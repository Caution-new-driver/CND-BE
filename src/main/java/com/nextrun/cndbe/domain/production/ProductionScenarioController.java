package com.nextrun.cndbe.domain.production;

import com.nextrun.cndbe.domain.production.dto.ProductionScenarioListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Production Scenario")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drops/{dropId}/production-scenarios")
public class ProductionScenarioController {

    private final ProductionScenarioService productionScenarioService;

    @Operation(summary = "제작 가능 수량 계산 및 제작안 2건 저장")
    @PostMapping
    public ProductionScenarioListResponse calculate(
            @PathVariable UUID dropId
    ) {
        return productionScenarioService.calculate(dropId);
    }

    @Operation(summary = "계산된 제작안 조회")
    @GetMapping
    public ProductionScenarioListResponse get(
            @PathVariable UUID dropId
    ) {
        return productionScenarioService.get(dropId);
    }

    @Operation(summary = "최종 제작안 선택")
    @PostMapping("/{scenarioId}/select")
    public ProductionScenarioListResponse select(
            @PathVariable UUID dropId,
            @PathVariable UUID scenarioId
    ) {
        return productionScenarioService.select(dropId, scenarioId);
    }
}
