package com.nextrun.cndbe.domain.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.domain.drop.DesignRequirement;
import com.nextrun.cndbe.domain.drop.DesignRequirementRepository;
import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.drop.DropRepository;
import com.nextrun.cndbe.domain.matching.dto.MaterialCandidateListResponse;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.MaterialStatus;
import com.nextrun.cndbe.domain.material.MaterialType;
import com.nextrun.cndbe.domain.material.Template;
import com.nextrun.cndbe.domain.material.repository.MaterialRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 실제 Neon DB와 OpenAI를 호출하지 않고 Mock으로 b9+b10 전체 흐름을 검증함.
// 필터 결과의 정렬·최대 3개 제한·AI 설명 연결·교체 저장·조회·예외를 확인함.
@ExtendWith(MockitoExtension.class)
class MaterialCandidateServiceTest {

    private static final double MINI_BAG_AREA_MM2 = 84_000;

    @Mock
    private DropRepository dropRepository;

    @Mock
    private DesignRequirementRepository designRequirementRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MaterialCandidateRepository materialCandidateRepository;

    @Mock
    private TemplateAreaCalculator templateAreaCalculator;

    @Mock
    private MaterialCandidateFilter materialCandidateFilter;

    @Mock
    private MaterialMatchScorer materialMatchScorer;

    @Mock
    private MaterialRecommendationClient materialRecommendationClient;

    @InjectMocks
    private MaterialCandidateService service;

    private UUID dropId;
    private Drop drop;
    private DesignRequirement requirement;

    @BeforeEach
    void setUp() {
        dropId = UUID.randomUUID();
        drop = Drop.builder()
                .id(dropId)
                .template(Template.builder().name("미니백").build())
                .build();
        requirement = DesignRequirement.builder()
                .drop(drop)
                .materialType("COATED_CANVAS")
                .color("BEIGE")
                .pattern("STRIPE")
                .minGrade("A")
                .build();
    }

    @Test
    void 후보를_점수와_등급순으로_최대_3개_저장한다() {
        Material gradeB100 = material("M-001", MaterialGrade.B, 0.95F);
        Material gradeA100 = material("M-002", MaterialGrade.A, 0.80F);
        Material score50 = material("M-003", MaterialGrade.A, 0.90F);
        Material score0 = material("M-004", MaterialGrade.A, 0.99F);

        prepareDropAndRequirement();
        when(materialRepository.findAll()).thenReturn(
                List.of(gradeB100, gradeA100, score50, score0)
        );
        when(materialCandidateFilter.isEligible(
                any(Material.class),
                any(DesignRequirement.class),
                anyDouble()
        )).thenReturn(true);
        when(materialMatchScorer.calculate(gradeB100, requirement))
                .thenReturn(100);
        when(materialMatchScorer.calculate(gradeA100, requirement))
                .thenReturn(100);
        when(materialMatchScorer.calculate(score50, requirement))
                .thenReturn(50);
        when(materialMatchScorer.calculate(score0, requirement))
                .thenReturn(0);
        mockAiRecommendations();
        mockSaveAll();

        MaterialCandidateListResponse response =
                service.calculateCandidates(dropId);

        assertEquals(dropId, response.dropId());
        assertEquals(3, response.candidates().size());
        assertEquals("M-002", response.candidates().get(0)
                .material().materialCode());
        assertEquals(1, response.candidates().get(0).rank());
        assertEquals("M-001", response.candidates().get(1)
                .material().materialCode());
        assertEquals(2, response.candidates().get(1).rank());
        assertEquals("M-003", response.candidates().get(2)
                .material().materialCode());
        assertEquals(3, response.candidates().get(2).rank());
        assertEquals("추천 이유", response.candidates().get(0).aiReasons());
        assertEquals("주의사항", response.candidates().get(0).aiCautions());

        InOrder persistenceOrder = inOrder(materialCandidateRepository);
        persistenceOrder.verify(materialCandidateRepository)
                .deleteAllByDrop_Id(dropId);
        persistenceOrder.verify(materialCandidateRepository)
                .saveAll(anyList());
    }

    @Test
    void 후보가_없으면_빈_목록으로_기존_결과를_교체한다() {
        prepareDropAndRequirement();
        when(materialRepository.findAll()).thenReturn(List.of());
        when(materialRecommendationClient.recommend(
                requirement,
                List.of()
        )).thenReturn(new MaterialRecommendationResult(List.of()));
        when(materialCandidateRepository.saveAll(anyList()))
                .thenReturn(List.of());

        MaterialCandidateListResponse response =
                service.calculateCandidates(dropId);

        assertEquals(List.of(), response.candidates());
        verify(materialCandidateRepository).deleteAllByDrop_Id(dropId);
        verify(materialCandidateRepository).saveAll(List.of());
    }

    @Test
    void 저장된_후보를_순위순으로_조회한다() {
        Material firstMaterial = material("M-001", MaterialGrade.A, 0.9F);
        Material secondMaterial = material("M-002", MaterialGrade.B, 0.8F);
        MaterialCandidate first = candidate(firstMaterial, 1, 100);
        MaterialCandidate second = candidate(secondMaterial, 2, 50);

        when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));
        when(materialCandidateRepository
                .findAllByDrop_IdOrderByRankAsc(dropId))
                .thenReturn(List.of(first, second));

        MaterialCandidateListResponse response =
                service.getCandidates(dropId);

        assertEquals(2, response.candidates().size());
        assertEquals(1, response.candidates().get(0).rank());
        assertEquals(2, response.candidates().get(1).rank());
    }

    @Test
    void 존재하지_않는_Drop이면_예외가_발생한다() {
        when(dropRepository.findById(dropId)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateCandidates(dropId)
        );
    }

    @Test
    void 디자인_조건이_없으면_예외가_발생한다() {
        when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));
        when(designRequirementRepository.findByDrop_Id(dropId))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateCandidates(dropId)
        );
    }

    private void prepareDropAndRequirement() {
        when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));
        when(designRequirementRepository.findByDrop_Id(dropId))
                .thenReturn(Optional.of(requirement));
        when(templateAreaCalculator.calculateRequiredArea(drop.getTemplate()))
                .thenReturn(MINI_BAG_AREA_MM2);
    }

    private void mockAiRecommendations() {
        when(materialRecommendationClient.recommend(
                any(DesignRequirement.class),
                anyList()
        )).thenAnswer(invocation -> {
            List<MaterialCandidate> candidates = invocation.getArgument(1);
            List<MaterialRecommendationResult.Recommendation> recommendations =
                    candidates.stream()
                            .map(candidate ->
                                    new MaterialRecommendationResult.Recommendation(
                                            candidate.getMaterial().getId(),
                                            "추천 이유",
                                            "주의사항"
                                    )
                            )
                            .toList();

            return new MaterialRecommendationResult(recommendations);
        });
    }

    private void mockSaveAll() {
        when(materialCandidateRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    List<MaterialCandidate> candidates =
                            invocation.getArgument(0);
                    candidates.forEach(candidate ->
                            candidate.setId(UUID.randomUUID())
                    );
                    return candidates;
                });
    }

    private Material material(
            String materialCode,
            MaterialGrade grade,
            Float aiConfidence
    ) {
        return Material.builder()
                .id(UUID.randomUUID())
                .materialCode(materialCode)
                .materialType(MaterialType.COATED_CANVAS)
                .color(MaterialColor.BEIGE)
                .pattern(MaterialPattern.STRIPE)
                .grade(grade)
                .aiConfidence(aiConfidence)
                .widthMm(400F)
                .heightMm(300F)
                .quantity(1)
                .status(MaterialStatus.AVAILABLE)
                .build();
    }

    private MaterialCandidate candidate(
            Material material,
            int rank,
            int matchScore
    ) {
        return MaterialCandidate.builder()
                .id(UUID.randomUUID())
                .drop(drop)
                .material(material)
                .rank(rank)
                .matchScore(matchScore)
                .aiReasons("추천 이유")
                .aiCautions("주의사항")
                .build();
    }
}
