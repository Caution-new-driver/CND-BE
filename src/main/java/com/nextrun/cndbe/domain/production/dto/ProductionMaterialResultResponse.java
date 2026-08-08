package com.nextrun.cndbe.domain.production.dto;

import com.nextrun.cndbe.domain.production.MaterialRole;
import com.nextrun.cndbe.domain.production.ProductionMaterialResult;
import com.nextrun.cndbe.common.calculation.RemainingRegion;
import java.util.List;
import java.util.UUID;

public record ProductionMaterialResultResponse(
        UUID materialId,
        String materialCode,
        MaterialRole materialRole,
        int supportedMiniBagQuantity,
        int luggageTagQuantity,
        double availableAreaMm2,
        double usedAreaMm2,
        double remainingAreaMm2,
        List<RemainingRegionResponse> remainingRegions
) {
    public static ProductionMaterialResultResponse from(
            ProductionMaterialResult result,
            List<RemainingRegion> regions
    ) {
        return new ProductionMaterialResultResponse(
                result.getMaterial().getId(),
                result.getMaterial().getMaterialCode(),
                result.getMaterialRole(),
                result.getSupportedMiniBagQuantity(),
                result.getLuggageTagQuantity(),
                result.getAvailableAreaMm2(),
                result.getUsedAreaMm2(),
                result.getRemainingAreaMm2(),
                regions.stream()
                        .map(RemainingRegionResponse::from)
                        .toList()
        );
    }
}
