package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.common.calculation.PatternPlacementCalculator;
import com.nextrun.cndbe.common.calculation.TemplatePatternParser;
import com.nextrun.cndbe.domain.drop.DesignRequirement;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialStatus;
import com.nextrun.cndbe.domain.material.MaterialType;
import com.nextrun.cndbe.domain.material.Template;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// b9의 필수조건 필터. 전체 재고 중에서 디자인 조건과 제작 최소조건을
// 모두 통과한 소재만 점수 계산 대상으로 넘김.
@Component
@RequiredArgsConstructor
public class MaterialCandidateFilter {

    private final TemplatePatternParser templatePatternParser;
    private final PatternPlacementCalculator patternPlacementCalculator;

    // 아래 조건은 AND 관계라서 하나라도 실패하면 추천 후보에서 제외됨.
    public boolean isEligible(
            Material material,
            DesignRequirement requirement,
            double requiredAreaMm2,
            Template template
    ) {
        return hasRequiredData(material)
                && matchesMaterialType(material, requirement)
                && meetsMinimumGrade(material, requirement)
                && hasEnoughArea(material, requiredAreaMm2)
                && canPlacePatternOnOneSheet(material, template);
    }

    // AVAILABLE 재고이면서 AI 태깅(color/pattern)과 제작 계산용 값이
    // 모두 채워져 있어야 함. null이나 0 이하 치수·수량은 잘못된 데이터로 봄.
    private boolean hasRequiredData(Material material) {
        return material.getStatus() == MaterialStatus.AVAILABLE
                && material.getMaterialType() != null
                && material.getGrade() != null
                && material.getColor() != null
                && material.getPattern() != null
                && material.getWidthMm() != null
                && material.getWidthMm() > 0
                && material.getHeightMm() != null
                && material.getHeightMm() > 0
                && material.getQuantity() != null
                && material.getQuantity() > 0;
    }

    // 소재 종류 조건을 입력하지 않았다면 모든 종류를 허용하고,
    // 입력했다면 프론트와 합의한 MaterialType enum 코드로 정확히 비교함.
    private boolean matchesMaterialType(
            Material material,
            DesignRequirement requirement
    ) {
        if (!StringUtils.hasText(requirement.getMaterialType())) {
            return true;
        }

        try {
            MaterialType requiredType = MaterialType.valueOf(
                    requirement.getMaterialType()
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );

            return material.getMaterialType() == requiredType;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "지원하지 않는 소재 종류입니다: "
                            + requirement.getMaterialType()
            );
        }
    }

    // 등급 순서는 A가 가장 좋고 C가 가장 낮음.
    // 예: 최소 B를 선택하면 A와 B는 통과하고 C는 제외됨.
    private boolean meetsMinimumGrade(
            Material material,
            DesignRequirement requirement
    ) {
        if (!StringUtils.hasText(requirement.getMinGrade())) {
            return true;
        }

        try {
            MaterialGrade minimumGrade = MaterialGrade.valueOf(
                    requirement.getMinGrade()
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );

            return material.getGrade().ordinal()
                    <= minimumGrade.ordinal();
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "지원하지 않는 소재 등급입니다: "
                            + requirement.getMinGrade()
            );
        }
    }

    // 소재 한 장의 면적에 재고 수량을 곱해서, 미니백 1개에 필요한
    // 전체 패턴 면적 이상을 보유했는지 확인하는 MVP 기준 계산.
    private boolean hasEnoughArea(
            Material material,
            double requiredAreaMm2
    ) {
        double availableAreaMm2 =
                material.getWidthMm()
                        * material.getHeightMm()
                        * material.getQuantity();

        return availableAreaMm2 >= requiredAreaMm2;
    }

    // 여러 장의 면적을 가상으로 붙이지 않는다. B12와 같은 2차원 배치 계산으로
    // 소재 한 장에 미니백 패턴 한 세트가 실제로 들어가는지도 확인한다.
    private boolean canPlacePatternOnOneSheet(
            Material material,
            Template template
    ) {
        return patternPlacementCalculator.calculateCapacityPerSheet(
                material.getWidthMm(),
                material.getHeightMm(),
                templatePatternParser.parse(template)
        ) >= 1;
    }
}
