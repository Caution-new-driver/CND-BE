package com.nextrun.cndbe.domain.material.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.nextrun.cndbe.domain.material.Template;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TemplateResponse {

	private UUID templateId;
	private String templateName;

	// Template에 JSON 문자열로 저장돼 있는 값을 그대로 raw JSON으로 내려줌 (이중 이스케이프 방지)
	@JsonRawValue
	private String patternPieces;

	@JsonRawValue
	private String requiredAccessories;

	public static TemplateResponse from(Template template) {
		return TemplateResponse.builder()
				.templateId(template.getId())
				.templateName(template.getName())
				.patternPieces(template.getPatternPieces())
				.requiredAccessories(template.getRequiredAccessories())
				.build();
	}
}
