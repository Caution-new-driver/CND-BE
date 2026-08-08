package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.domain.drop.DesignRequirement;
import com.nextrun.cndbe.domain.drop.DesignRequirementRepository;
import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.drop.DropRepository;
import com.nextrun.cndbe.domain.matching.dto.MaterialCandidateListResponse;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.repository.MaterialRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// b9+b10의 핵심 흐름을 담당함.
// 필수조건 필터링 -> 점수·순위 계산 -> AI 설명 생성 -> 결과 저장을 한 번에 처리함.
@Service
@RequiredArgsConstructor
public class MaterialCandidateService {

    private final DropRepository dropRepository;
    private final DesignRequirementRepository designRequirementRepository;
    private final MaterialRepository materialRepository;
    private final MaterialCandidateRepository materialCandidateRepository;
    private final TemplateAreaCalculator templateAreaCalculator;
    private final MaterialCandidateFilter materialCandidateFilter;
    private final MaterialMatchScorer materialMatchScorer;
    private final MaterialRecommendationClient materialRecommendationClient;

    @Transactional
    public MaterialCandidateListResponse calculateCandidates(UUID dropId) {
        // 1. 추천의 기준이 되는 Drop과 디자인 조건이 실제로 저장돼 있는지 확인.
        Drop drop = findDrop(dropId);
        DesignRequirement requirement = findDesignRequirement(dropId);

        // 2. 미니백 1개 제작에 필요한 패턴 조각의 전체 면적을 mm²로 계산.
        double requiredAreaMm2 =
                templateAreaCalculator.calculateRequiredArea(drop.getTemplate());

        // 3. 전체 소재에서 필수조건을 통과한 것만 점수화하고,
        // 점수·등급·AI 신뢰도 순으로 정렬한 뒤 최대 3개만 선택.
        List<ScoredMaterial> scoredMaterials =
                materialRepository.findAll().stream()
                        .filter(material ->
                                materialCandidateFilter.isEligible(
                                        material,
                                        requirement,
                                        requiredAreaMm2,
                                        drop.getTemplate()
                                )
                        )
                        .map(material ->
                                new ScoredMaterial(
                                        material,
                                        materialMatchScorer.calculate(
                                                material,
                                                requirement
                                        )
                                )
                        )
                        .sorted(scoredMaterialComparator())
                        .limit(3)
                        .toList();

        List<MaterialCandidate> candidates =
                createCandidates(drop, scoredMaterials);

        // 4. 순위는 백엔드 계산 결과로 고정하고, AI는 각 후보의
        // 추천 이유와 주의사항만 작성함.
        MaterialRecommendationResult recommendationResult =
                materialRecommendationClient.recommend(
                        requirement,
                        candidates
                );

        applyRecommendations(candidates, recommendationResult);

        // 5. "조건을 수정해 다시 검색"하는 경우를 위해 과거 후보를 지우고
        // 이번 계산 결과로 교체. AI 호출 실패 시에는 여기까지 오지 않아 기존 결과가 유지됨.
        materialCandidateRepository.deleteAllByDrop_Id(dropId);

        List<MaterialCandidate> savedCandidates =
                materialCandidateRepository.saveAll(candidates);

        return MaterialCandidateListResponse.from(
                dropId,
                savedCandidates
        );
    }

    @Transactional(readOnly = true)
    public MaterialCandidateListResponse getCandidates(UUID dropId) {
        // POST에서 이미 계산·저장한 결과만 읽음. 여기서는 AI를 다시 호출하지 않음.
        findDrop(dropId);

        List<MaterialCandidate> candidates =
                materialCandidateRepository
                        .findAllByDrop_IdOrderByRankAsc(dropId);

        return MaterialCandidateListResponse.from(
                dropId,
                candidates
        );
    }

    private Drop findDrop(UUID dropId) {
        return dropRepository.findById(dropId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Drop을 찾을 수 없습니다: " + dropId
                        )
                );
    }

    private DesignRequirement findDesignRequirement(UUID dropId) {
        return designRequirementRepository.findByDrop_Id(dropId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "디자인 조건을 찾을 수 없습니다: " + dropId
                        )
                );
    }

    private List<MaterialCandidate> createCandidates(
            Drop drop,
            List<ScoredMaterial> scoredMaterials
    ) {
        // 정렬된 리스트의 위치를 1부터 시작하는 사용자용 rank로 변환.
        return java.util.stream.IntStream
                .range(0, scoredMaterials.size())
                .mapToObj(index -> {
                    ScoredMaterial scoredMaterial =
                            scoredMaterials.get(index);

                    return MaterialCandidate.builder()
                            .drop(drop)
                            .material(scoredMaterial.material())
                            .matchScore(scoredMaterial.score())
                            .rank(index + 1)
                            .build();
                })
                .toList();
    }

    private void applyRecommendations(
            List<MaterialCandidate> candidates,
            MaterialRecommendationResult result
    ) {
        // AI 응답 순서에 의존하지 않도록 materialId를 key로 바꾼 뒤
        // 원래 후보에 추천 이유·주의사항을 정확히 연결함.
        Map<UUID, MaterialRecommendationResult.Recommendation>
                recommendationByMaterialId =
                result.recommendations().stream()
                        .collect(Collectors.toMap(
                                MaterialRecommendationResult.Recommendation::materialId,
                                Function.identity()
                        ));

        for (MaterialCandidate candidate : candidates) {
            MaterialRecommendationResult.Recommendation recommendation =
                    recommendationByMaterialId.get(
                            candidate.getMaterial().getId()
                    );

            if (recommendation == null) {
                throw new IllegalStateException(
                        "AI 추천 결과에서 소재 정보를 찾을 수 없습니다."
                );
            }

            candidate.setAiReasons(
                    recommendation.aiReasons()
            );

            candidate.setAiCautions(
                    recommendation.aiCautions()
            );
        }
    }

    private Comparator<ScoredMaterial> scoredMaterialComparator() {
        // 우선순위: 매칭 점수 높은 순 -> 좋은 등급(A 우선) -> AI 신뢰도 높은 순
        // -> 결과가 매번 같도록 마지막에는 소재 코드 오름차순.
        return Comparator
                .comparingInt(ScoredMaterial::score)
                .reversed()
                .thenComparingInt(scoredMaterial ->
                        scoredMaterial.material()
                                .getGrade()
                                .ordinal()
                )
                .thenComparing(
                        scoredMaterial ->
                                scoredMaterial.material()
                                        .getAiConfidence(),
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
                .thenComparing(
                        scoredMaterial ->
                                scoredMaterial.material()
                                        .getMaterialCode(),
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                );
    }

    private record ScoredMaterial(
            Material material,
            int score
    ) {
        // 필터를 통과한 소재와 백엔드가 계산한 매칭 점수를 잠시 묶어두는 내부 전용 상자.
    }
}
