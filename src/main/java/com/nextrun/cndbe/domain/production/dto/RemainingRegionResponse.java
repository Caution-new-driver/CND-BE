package com.nextrun.cndbe.domain.production.dto;

import com.nextrun.cndbe.common.calculation.RemainingRegion;

public record RemainingRegionResponse(
        double widthMm,
        double heightMm
) {
    public static RemainingRegionResponse from(RemainingRegion region) {
        return new RemainingRegionResponse(
                region.widthMm(),
                region.heightMm()
        );
    }
}
