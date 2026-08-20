package com.nextrun.cndbe.common.calculation;

import java.util.List;

public record SheetPlacementResult(
        int supportedProductQuantity,
        List<RemainingRegion> remainingRegions
) {
}
