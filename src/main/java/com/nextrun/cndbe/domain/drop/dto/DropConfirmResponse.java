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

    // b14가 확정 흐름에 흡수되어 함께 생성됨. AI 생성이 실패했을 경우 null일 수 있음(담당자가 직접 채워야 함).
    private String introText;

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
                .introText(drop.getIntroText())
                .build();
    }
}
