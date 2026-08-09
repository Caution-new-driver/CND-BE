package com.nextrun.cndbe.domain.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nextrun.cndbe.domain.drop.DesignRequirement;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import org.junit.jupiter.api.Test;

// 색상·패턴 일치 개수에 따라 100점, 50점, 0점이 나오는지 확인함.
class MaterialMatchScorerTest {

    private final MaterialMatchScorer scorer = new MaterialMatchScorer();

    @Test
    void 색상과_패턴이_모두_일치하면_100점이다() {
        int score = scorer.calculate(
                material(MaterialColor.BEIGE, MaterialPattern.STRIPE),
                requirement(MaterialColor.BEIGE, MaterialPattern.STRIPE)
        );

        assertEquals(100, score);
    }

    @Test
    void 색상과_패턴_중_하나만_일치하면_50점이다() {
        int score = scorer.calculate(
                material(MaterialColor.BEIGE, MaterialPattern.SOLID),
                requirement(MaterialColor.BEIGE, MaterialPattern.STRIPE)
        );

        assertEquals(50, score);
    }

    @Test
    void 색상과_패턴이_모두_다르면_0점이다() {
        int score = scorer.calculate(
                material(MaterialColor.BLACK, MaterialPattern.SOLID),
                requirement(MaterialColor.BEIGE, MaterialPattern.STRIPE)
        );

        assertEquals(0, score);
    }

    @Test
    void 선호_조건이_없으면_0점이다() {
        int score = scorer.calculate(
                material(MaterialColor.BEIGE, MaterialPattern.STRIPE),
                requirement(null, null)
        );

        assertEquals(0, score);
    }

    private Material material(
            MaterialColor color,
            MaterialPattern pattern
    ) {
        return Material.builder()
                .color(color)
                .pattern(pattern)
                .build();
    }

    private DesignRequirement requirement(
            MaterialColor color,
            MaterialPattern pattern
    ) {
        return DesignRequirement.builder()
                .color(color)
                .pattern(pattern)
                .build();
    }
}
