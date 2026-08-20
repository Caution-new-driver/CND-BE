package com.nextrun.cndbe.domain.production;

import com.nextrun.cndbe.domain.production.dto.ProductionScenarioListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Production Scenario",
        description = "b12 제작 가능 수량 계산·제작안 조회·선택 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drops/{dropId}/production-scenarios")
public class ProductionScenarioController {

    private final ProductionScenarioService productionScenarioService;

    @Operation(
            summary = "제작 가능 수량 계산 및 제작안 2건 저장",
            description = "확정된 소재 조합을 기준으로 실제 배치 가능한 보수적 제작 수량을 계산하고, "
                    + "미니백 단독안과 러기지 태그 추가안을 저장합니다."
    )
    @PostMapping
    public ProductionScenarioListResponse calculate(
            @Parameter(description = "제작 가능성을 계산할 Drop ID")
            @PathVariable UUID dropId
    ) {
        return productionScenarioService.calculate(dropId);
    }

    @Operation(
            summary = "계산된 제작안 조회",
            description = "저장된 두 제작안과 소재별 배치 결과, 활용률, 남은 영역을 반환합니다."
    )
    @GetMapping
    public ProductionScenarioListResponse get(
            @Parameter(description = "제작안을 조회할 Drop ID")
            @PathVariable UUID dropId
    ) {
        return productionScenarioService.get(dropId);
    }

    @Operation(
            summary = "최종 제작안 선택",
            description = "지정한 제작안을 최종안으로 표시하고 Drop의 selectedScenarioId를 갱신합니다."
    )
    @PostMapping("/{scenarioId}/select")
    public ProductionScenarioListResponse select(
            @Parameter(description = "제작안을 선택할 Drop ID")
            @PathVariable UUID dropId,
            @Parameter(description = "최종 선택할 제작안 ID")
            @PathVariable UUID scenarioId
    ) {
        return productionScenarioService.select(dropId, scenarioId);
    }
}
