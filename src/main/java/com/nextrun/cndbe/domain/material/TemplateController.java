package com.nextrun.cndbe.domain.material;

import com.nextrun.cndbe.domain.material.dto.TemplateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Drop을 생성하지 않고도 고정 템플릿 정보(패턴 조각/부자재)만 미리 보여주기 위한 조회 전용 API.
// f2 화면 진입 시점에 POST /api/drops를 매번 호출하면 안 쓰는 DRAFT Drop이 계속 쌓이는 문제를 막기 위함.
@Tag(name = "Template", description = "고정 템플릿(미니백/러기지 태그) 조회 API")
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

	private final TemplateRepository templateRepository;

	// [단건 조회] GET /api/templates/{name}
	@Operation(summary = "템플릿 단건 조회", description = "이름으로 고정 템플릿 하나를 조회합니다. 없으면 404를 반환합니다.")
	@GetMapping("/{name}")
	public TemplateResponse getByName(@Parameter(description = "템플릿 이름 (예: 미니백)") @PathVariable String name) {
		Template template = templateRepository.findByName(name)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "name: " + name + "인 템플릿을 찾을 수 없습니다."));
		return TemplateResponse.from(template);
	}
}
