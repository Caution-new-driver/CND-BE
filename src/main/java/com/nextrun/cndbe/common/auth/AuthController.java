package com.nextrun.cndbe.common.auth;

import com.nextrun.cndbe.common.auth.dto.LoginRequest;
import com.nextrun.cndbe.common.auth.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(
		name = "Auth",
		description = "MCM 관계자 공용 비밀번호 로그인 API"
)
@RestController
@RequiredArgsConstructor
public class AuthController {

	private final AuthTokenService authTokenService;

	@Operation(
			summary = "로그인",
			description = "팀 공유 비밀번호를 검증하고, 맞으면 이후 요청에 쓸 세션 토큰을 발급한다."
	)
	@PostMapping("/api/auth/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		if (!authTokenService.isCorrectPassword(request.password())) {
			throw new InvalidCredentialsException("비밀번호가 올바르지 않습니다.");
		}
		return new LoginResponse(authTokenService.issueToken());
	}
}
