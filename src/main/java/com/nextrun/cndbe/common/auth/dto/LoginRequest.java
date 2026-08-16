package com.nextrun.cndbe.common.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank(message = "비밀번호를 입력해야 합니다.")
		String password
) {
}
