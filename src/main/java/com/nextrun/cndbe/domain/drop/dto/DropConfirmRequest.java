package com.nextrun.cndbe.domain.drop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

// b13: Drop 이름은 AI 자동 생성이 아니라 담당자가 직접 입력한다.
public record DropConfirmRequest(
        @NotBlank(message = "Drop 이름은 필수입니다.")
        String name,

        @Positive(message = "예상 제작기간은 1일 이상이어야 합니다.")
        Integer expectedProductionDays
) {
}
