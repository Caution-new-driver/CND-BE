package com.nextrun.cndbe.domain.matching;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nextrun.cndbe.domain.drop.DesignRequirement;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.MaterialStatus;
import com.nextrun.cndbe.domain.material.MaterialType;
import org.junit.jupiter.api.Test;

// b9 필수조건 필터가 상태·AI 태깅·소재 종류·등급·면적을
// 기획한 규칙대로 통과시키거나 제외하는지 각각 확인함.
class MaterialCandidateFilterTest {

    private static final double MINI_BAG_AREA_MM2 = 84_000;

    private final MaterialCandidateFilter filter =
            new MaterialCandidateFilter();

    @Test
    void 필수_조건을_모두_충족하면_후보가_된다() {
        Material material = eligibleMaterial();
        DesignRequirement requirement = requirement(
                "COATED_CANVAS",
                "A"
        );

        boolean result = filter.isEligible(
                material,
                requirement,
                MINI_BAG_AREA_MM2
        );

        assertTrue(result);
    }

    @Test
    void AI_태깅값이_없으면_후보에서_제외한다() {
        Material material = eligibleMaterial();
        material.setColor(null);

        boolean result = filter.isEligible(
                material,
                requirement("COATED_CANVAS", "A"),
                MINI_BAG_AREA_MM2
        );

        assertFalse(result);
    }

    @Test
    void 사용_가능_상태가_아니면_후보에서_제외한다() {
        Material material = eligibleMaterial();
        material.setStatus(MaterialStatus.RESERVED);

        boolean result = filter.isEligible(
                material,
                requirement("COATED_CANVAS", "A"),
                MINI_BAG_AREA_MM2
        );

        assertFalse(result);
    }

    @Test
    void 소재_종류가_다르면_후보에서_제외한다() {
        boolean result = filter.isEligible(
                eligibleMaterial(),
                requirement("LEATHER", "A"),
                MINI_BAG_AREA_MM2
        );

        assertFalse(result);
    }

    @Test
    void 최소_등급보다_낮으면_후보에서_제외한다() {
        Material material = eligibleMaterial();
        material.setGrade(MaterialGrade.C);

        boolean result = filter.isEligible(
                material,
                requirement("COATED_CANVAS", "B"),
                MINI_BAG_AREA_MM2
        );

        assertFalse(result);
    }

    @Test
    void 최소_등급보다_좋은_등급은_후보가_된다() {
        Material material = eligibleMaterial();
        material.setGrade(MaterialGrade.A);

        boolean result = filter.isEligible(
                material,
                requirement("COATED_CANVAS", "B"),
                MINI_BAG_AREA_MM2
        );

        assertTrue(result);
    }

    @Test
    void 총면적이_부족하면_후보에서_제외한다() {
        Material material = eligibleMaterial();
        material.setWidthMm(200F);
        material.setHeightMm(200F);
        material.setQuantity(2);

        boolean result = filter.isEligible(
                material,
                requirement("COATED_CANVAS", "A"),
                MINI_BAG_AREA_MM2
        );

        assertFalse(result);
    }

    private Material eligibleMaterial() {
        return Material.builder()
                .materialType(MaterialType.COATED_CANVAS)
                .color(MaterialColor.BEIGE)
                .pattern(MaterialPattern.STRIPE)
                .grade(MaterialGrade.A)
                .widthMm(400F)
                .heightMm(300F)
                .quantity(1)
                .status(MaterialStatus.AVAILABLE)
                .build();
    }

    private DesignRequirement requirement(
            String materialType,
            String minGrade
    ) {
        return DesignRequirement.builder()
                .materialType(materialType)
                .minGrade(minGrade)
                .build();
    }
}
