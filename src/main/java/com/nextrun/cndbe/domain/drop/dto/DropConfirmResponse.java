package com.nextrun.cndbe.domain.drop.dto;

import com.nextrun.cndbe.domain.drop.DropConfirmationCoreResult;
import com.nextrun.cndbe.domain.drop.DropConfirmationWriter;
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

    // b13 최초 생성을 포함해 Drop당 총 6회 중 남은 AI 재생성 가능 횟수.
    private int regenerationsRemaining;

    // introText는 core가 아니라 별도 인자로 받는다 — AI 생성이 트랜잭션 밖에서
    // 일어나고 core가 만들어진 시점엔 아직 결과를 모르기 때문(DropConfirmationService 참고).
    public static DropConfirmResponse of(
            DropConfirmationCoreResult core,
            String introText
    ) {
        return DropConfirmResponse.builder()
                .id(core.dropId())
                .status(core.status())
                .name(core.name())
                .expectedProductionDays(core.expectedProductionDays())
                .selectedScenarioId(core.selectedScenarioId())
                .items(core.items())
                .introText(introText)
                .regenerationsRemaining(
                        DropConfirmationWriter.MAX_INTRO_TEXT_GENERATION_COUNT
                                - core.introTextGenerationsUsed()
                )
                .build();
    }
}
