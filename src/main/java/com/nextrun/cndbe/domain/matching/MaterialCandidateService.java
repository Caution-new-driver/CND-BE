package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.common.calculation.PatternPiece;
import com.nextrun.cndbe.common.calculation.TemplatePatternParser;
import com.nextrun.cndbe.domain.drop.DesignRequirement;
import com.nextrun.cndbe.domain.drop.DesignRequirementRepository;
import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.drop.DropRepository;
import com.nextrun.cndbe.domain.drop.DropStatus;
import com.nextrun.cndbe.domain.matching.dto.MaterialCandidateListResponse;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.repository.MaterialRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// b9+b10의 핵심 흐름을 담당함.
// 필수조건 필터링 -> 점수·순위 계산 -> AI 설명 생성 -> 성공 시 결과 교체 순서로 처리함.
@Service
@RequiredArgsConstructor
public class MaterialCandidateService {

    private final DropRepository dropRepository;
    private final DesignRequirementRepository designRequirementRepository;
    private final MaterialRepository materialRepository;
    private final MaterialCandidateRepository materialCandidateRepository;
    private final MaterialCandidateWriter materialCandidateWriter;
    private final TemplatePatternParser templatePatternParser;
    private final MaterialCandidateFilter materialCandidateFilter;
    private final MaterialMatchScorer materialMatchScorer;
    private final MaterialRecommendationClient materialRecommendationClient;
    private final MaterialSelectionService materialSelectionService;

    public MaterialCandidateListResponse calculateCandidates(UUID dropId) {
        // 1. 짧은 DB 조회가 끝난 뒤에도 템플릿을 읽을 수 있도록 함께 조회한다.
        Drop drop = findDropWithTemplate(dropId);
        DesignRequirement requirement = findDesignRequirement(dropId);

        // AI 호출이 실패해도 기존 선택을 보존하기 위해 예약은 아직 해제하지 않는다.
        // 대신 이 Drop이 이미 선택한 RESERVED 소재 ID만 필터에서 재사용 가능하게 처리한다.
        Set<UUID> reusableMaterialIds =
                materialSelectionService.findSelectedMaterialIds(dropId);

        // 2. 템플릿 JSON은 한 번만 파싱한 뒤 면적 계산과 모든 소재 필터에서 재사용.
        List<PatternPiece> patternPieces = templatePatternParser.parse(
                drop.getTemplate()
        );
        double requiredAreaMm2 = patternPieces.stream()
                .mapToDouble(PatternPiece::areaMm2)
                .sum();

        // 3. 전체 소재에서 필수조건을 통과한 것만 점수화하고,
        // 점수·등급·AI 신뢰도 순으로 정렬한 뒤 최대 3개만 선택.
        List<ScoredMaterial> scoredMaterials =
                materialRepository.findAll().stream()
                        .filter(material ->
                                materialCandidateFilter.isEligible(
                                        material,
                                        requirement,
                                        requiredAreaMm2,
                                        patternPieces,
                                        reusableMaterialIds
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

        // 5. 외부 API 호출이 끝난 뒤 삭제+저장만 짧은 트랜잭션으로 처리한다.
        // AI 호출 실패 시 writer가 실행되지 않아 기존 결과가 유지된다.
        List<MaterialCandidate> savedCandidates =
                materialCandidateWriter.replaceAfterResearch(
                        dropId,
                        candidates
                );

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
                        new NoSuchElementException(
                                "Drop을 찾을 수 없습니다: " + dropId
                        )
                );
    }

    private Drop findDropWithTemplate(UUID dropId) {
        Drop drop = dropRepository.findByIdWithTemplate(dropId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Drop을 찾을 수 없습니다: " + dropId
                        )
                );

        // 실제 OpenAI 호출(materialRecommendationClient.recommend) 전에 막아서, 확정된
        // Drop에 대해 재계산을 시도해도 AI 비용이 나가지 않도록 한다. 이 체크가 없으면
        // AI 호출까지 다 끝난 뒤에야 뒷단(releaseSelectionForResearch)에서 실패했다.
        if (drop.getStatus() != DropStatus.DRAFT) {
            throw new IllegalStateException(
                    "확정된 Drop의 소재 후보는 다시 계산할 수 없습니다."
            );
        }

        return drop;
    }

    private DesignRequirement findDesignRequirement(UUID dropId) {
        return designRequirementRepository.findByDrop_Id(dropId)
                .orElseThrow(() ->
                        new NoSuchElementException(
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
