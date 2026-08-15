package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.MaterialType;
import com.nextrun.cndbe.domain.production.ProductType;
import com.nextrun.cndbe.domain.production.ScenarioType;
import java.util.List;

// b13 확정 트랜잭션이 끝나기 전에 미리 꺼내둔 순수 값만 담는 상자.
// DropIntroTextClient는 트랜잭션이 끝난 뒤(OpenAI 호출 시점)에 호출되므로,
// Drop.template 같은 지연 로딩 필드를 다시 읽을 수 없다 — 그래서 엔티티 대신 이 값만 전달한다.
public record DropIntroTextPromptData(
        String dropName,
        String templateName,
        ScenarioType scenarioType,
        MaterialSummary mainMaterial,
        MaterialSummary pointMaterial,
        List<ProductSummary> items
) {

    public record MaterialSummary(
            MaterialType materialType,
            MaterialColor color,
            MaterialPattern pattern,
            MaterialGrade grade
    ) {
    }

    public record ProductSummary(
            ProductType productType,
            int quantity
    ) {
    }
}
