package com.nextrun.cndbe.common.client;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiConfig {

	@Bean
	public RestClient openAiRestClient(@Value("${openai.api-key}") String apiKey) {
		// 타임아웃이 없으면 OpenAI 응답이 지연될 때 호출 스레드가 무한정 묶일 수 있어 명시적으로 설정.
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(30));

		return RestClient.builder()
				.baseUrl("https://api.openai.com/v1")
				.requestFactory(requestFactory)
				.defaultHeader("Authorization", "Bearer " + apiKey)
				.defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.build();
	}
}
