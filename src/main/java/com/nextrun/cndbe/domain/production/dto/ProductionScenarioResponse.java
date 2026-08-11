package com.nextrun.cndbe.domain.production.dto;

import com.nextrun.cndbe.domain.production.ScenarioType;
import java.util.List;
import java.util.UUID;

public record ProductionScenarioResponse(
        UUID scenarioId,
        ScenarioType scenarioType,
        float materialUtilizationRate,
        double totalAvailableAreaMm2,
        double usedAreaMm2,
        double remainingAreaMm2,
        boolean selected,
        List<ProductionScenarioItemResponse> items,
        List<ProductionMaterialResultResponse> materialResults
) {
}
