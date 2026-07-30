package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.drop.dto.DesignRequirementResponse;
import com.nextrun.cndbe.domain.drop.dto.DropResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Drop")
@RestController
@RequiredArgsConstructor
public class DropController {

	private final DropService dropService;

	// b7: 새 RUN Drop 기획 시작 - draft 상태 Drop 생성, 고정 미니백 템플릿 정보 함께 반환
	@PostMapping("/api/drops")
	@ResponseStatus(HttpStatus.CREATED)
	public DropResponse createDraftDrop() {
		Drop drop = dropService.createDraftDrop();
		return DropResponse.from(drop);
	}

	// b8: 디자인 조건 저장 (스케치 이미지 첨부 포함, 선택사항)
	@PostMapping(value = "/api/drops/{dropId}/design-requirement")
	public DesignRequirementResponse saveDesignRequirement(
			@PathVariable UUID dropId,
			@RequestParam(required = false) String materialType,
			@RequestParam(required = false) String color,
			@RequestParam(required = false) String pattern,
			@RequestParam(required = false) String minGrade,
			@RequestParam(required = false) String accessoryColor,
			@RequestParam(required = false) Boolean usePointMaterial,
			@RequestParam(required = false) MultipartFile sketchImage) {
		DesignRequirement requirement = dropService.saveDesignRequirement(
				dropId, materialType, color, pattern, minGrade, accessoryColor, usePointMaterial, sketchImage);
		return DesignRequirementResponse.from(requirement);
	}
}
