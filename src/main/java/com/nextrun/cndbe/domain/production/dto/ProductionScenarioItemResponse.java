package com.nextrun.cndbe.domain.production.dto;

import com.nextrun.cndbe.domain.production.ProductType;
import com.nextrun.cndbe.domain.production.ProductionScenarioItem;

public record ProductionScenarioItemResponse(
        ProductType productType,
        int quantity,
        Integer numberingStart,
        Integer numberingEnd
) {
    public static ProductionScenarioItemResponse from(
            ProductionScenarioItem item
    ) {
        return new ProductionScenarioItemResponse(
                item.getProductType(),
                item.getQuantity(),
                item.getNumberingStart(),
                item.getNumberingEnd()
        );
    }
}
