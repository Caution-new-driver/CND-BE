package com.nextrun.cndbe.domain.drop.dto;

import java.util.UUID;

public record DropIntroTextResponse(
        UUID dropId,
        String introText,
        // b13 최초 생성을 포함해 Drop당 총 6회 중 남은 AI 생성 가능 횟수. 0이면 재생성 불가, 직접 입력만 가능.
        int regenerationsRemaining
) {
}
