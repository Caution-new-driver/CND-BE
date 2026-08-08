package com.nextrun.cndbe.domain.production.dto;

import java.util.List;
import java.util.UUID;

public record ProductionScenarioListResponse(
        UUID dropId,
        UUID selectedScenarioId,
        List<ProductionScenarioResponse> scenarios
) {
}
