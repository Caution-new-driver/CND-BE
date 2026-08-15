package com.nextrun.cndbe.common.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// MCM 관계자만 API를 쓸 수 있도록, 로그인 API를 제외한 모든 /api/** 요청에서
// Authorization: Bearer <token> 헤더를 검증한다.
@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

	private static final String AUTH_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final AuthTokenService authTokenService;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		// CORS 프리플라이트(OPTIONS)는 브라우저가 자동으로 보내는 요청이라 Authorization 헤더가
		// 없다. 여기서 막으면 Spring의 CORS 헤더 처리(필터 이후 단계)까지 못 가서 브라우저가
		// 프리플라이트 실패로 보고 실제 요청을 아예 안 보낸다.
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		String path = request.getRequestURI();
		return !path.startsWith("/api/") || path.equals("/api/auth/login");
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(AUTH_HEADER);
		String token = (header != null && header.startsWith(BEARER_PREFIX))
				? header.substring(BEARER_PREFIX.length())
				: null;

		if (!authTokenService.isValidToken(token)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"message\":\"인증이 필요합니다.\"}");
			return;
		}
		filterChain.doFilter(request, response);
	}
}
