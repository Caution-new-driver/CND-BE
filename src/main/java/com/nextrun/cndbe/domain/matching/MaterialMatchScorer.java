package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.domain.drop.DesignRequirement;
import com.nextrun.cndbe.domain.material.Material;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// b10 후보의 표시 순서를 정하기 위한 단순 점수 계산기.
// AI가 순위를 임의로 바꾸지 못하도록 색상·패턴 일치 여부를 코드로 계산함.
@Component
public class MaterialMatchScorer {

    public int calculate(
            Material material,
            DesignRequirement requirement
    ) {
        // 입력된 선호조건만 분모에 포함함.
        // 색상·패턴 모두 일치하면 100점, 하나만 일치하면 50점, 모두 다르면 0점.
        int criteriaCount = 0;
        int matchedCount = 0;

        if (StringUtils.hasText(requirement.getColor())) {
            criteriaCount++;

            if (material.getColor() != null
                    && material.getColor().name()
                    .equalsIgnoreCase(requirement.getColor().trim())) {
                matchedCount++;
            }
        }

        if (StringUtils.hasText(requirement.getPattern())) {
            criteriaCount++;

            if (material.getPattern() != null
                    && material.getPattern().name()
                    .equalsIgnoreCase(requirement.getPattern().trim())) {
                matchedCount++;
            }
        }

        if (criteriaCount == 0) {
            // 선호 색상과 패턴이 모두 비어 있으면 비교할 기준이 없으므로 0점 처리.
            return 0;
        }

        return matchedCount * 100 / criteriaCount;
    }
}
