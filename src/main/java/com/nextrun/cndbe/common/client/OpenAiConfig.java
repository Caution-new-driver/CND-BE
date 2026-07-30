package com.nextrun.cndbe.common.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiConfig {

	@Bean
	public RestClient openAiRestClient(@Value("${openai.api-key}") String apiKey) {
		return RestClient.builder()
				.baseUrl("https://api.openai.com/v1")
				.defaultHeader("Authorization", "Bearer " + apiKey)
				.defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.build();
	}
}
