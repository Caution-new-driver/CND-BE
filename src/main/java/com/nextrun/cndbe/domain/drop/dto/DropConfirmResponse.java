package com.nextrun.cndbe.domain.drop.dto;

import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.production.dto.ProductionScenarioItemResponse;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DropConfirmResponse {

    private UUID id;
    private String status;
    private String name;
    private Integer expectedProductionDays;
    private UUID selectedScenarioId;

    // 넘버링은 새로 계산하지 않고 b12에서 저장해둔 선택 시나리오 값을 그대로 확정해 보여줌.
    private List<ProductionScenarioItemResponse> items;

    public static DropConfirmResponse of(
            Drop drop,
            List<ProductionScenarioItemResponse> items
    ) {
        return DropConfirmResponse.builder()
                .id(drop.getId())
                .status(drop.getStatus().name())
                .name(drop.getName())
                .expectedProductionDays(drop.getExpectedProductionDays())
                .selectedScenarioId(drop.getSelectedScenarioId())
                .items(items)
                .build();
    }
}
