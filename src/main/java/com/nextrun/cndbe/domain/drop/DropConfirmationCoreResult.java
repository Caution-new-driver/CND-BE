package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.production.dto.ProductionScenarioItemResponse;
import java.util.List;
import java.util.UUID;

// DropConfirmationWriter.confirmCore()의 짧은 트랜잭션이 끝난 뒤 넘겨주는 결과.
// AI 소개문 생성(트랜잭션 밖)과 최종 응답 조립에 필요한 값만 담는다.
public record DropConfirmationCoreResult(
        UUID dropId,
        String status,
        String name,
        Integer expectedProductionDays,
        UUID selectedScenarioId,
        List<ProductionScenarioItemResponse> items,
        DropIntroTextPromptData introTextPromptData
) {
}
