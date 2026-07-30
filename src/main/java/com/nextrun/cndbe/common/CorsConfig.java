package com.nextrun.cndbe.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 프론트(Vercel)와 백엔드(Railway)가 서로 다른 origin이라 필요.
// app.cors.allowed-origins 로 콤마 구분된 목록을 받음 (prod는 CORS_ALLOWED_ORIGINS 환경변수로 주입).
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	@Value("${app.cors.allowed-origins:http://localhost:5173}")
	private String allowedOrigins;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedOrigins(allowedOrigins.split(","))
				.allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("*");
	}
}
