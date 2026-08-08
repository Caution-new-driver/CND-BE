package com.nextrun.cndbe.domain.production;

import com.nextrun.cndbe.common.calculation.RemainingRegion;
import com.nextrun.cndbe.domain.material.Material;
import java.util.List;

public record ProductionCalculationResult(
        int miniBagQuantity,
        List<ScenarioCalculation> scenarios
) {
    public record ScenarioCalculation(
            ScenarioType scenarioType,
            int luggageTagQuantity,
            double totalAvailableAreaMm2,
            double usedAreaMm2,
            double remainingAreaMm2,
            float utilizationRate,
            List<MaterialCalculation> materials
    ) {
    }

    public record MaterialCalculation(
            Material material,
            MaterialRole role,
            int supportedMiniBagQuantity,
            int luggageTagQuantity,
            double availableAreaMm2,
            double usedAreaMm2,
            double remainingAreaMm2,
            List<RemainingRegion> remainingRegions
    ) {
    }
}
