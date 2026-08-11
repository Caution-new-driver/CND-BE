package com.nextrun.cndbe.domain.drop.dto;

import jakarta.validation.constraints.NotBlank;

// b14: 담당자가 AI 초안을 직접 고쳐 최종 저장할 때 쓰는 요청 바디.
public record DropIntroTextRequest(
        @NotBlank(message = "소개문은 비어 있을 수 없습니다.")
        String introText
) {
}
