package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.drop.dto.DesignRequirementResponse;
import com.nextrun.cndbe.domain.drop.dto.DropResponse;
import com.nextrun.cndbe.domain.material.AccessoryColor;
import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.MaterialType;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
			@RequestParam(required = false) MaterialType materialType,
			@RequestParam(required = false) MaterialColor color,
			@RequestParam(required = false) MaterialPattern pattern,
			@RequestParam(required = false) MaterialGrade minGrade,
			@RequestParam(required = false) AccessoryColor accessoryColor,
			@RequestParam(required = false) Boolean usePointMaterial) {
		DesignRequirement requirement = dropService.saveDesignRequirement(
				dropId, materialType, color, pattern, minGrade, accessoryColor, usePointMaterial);
		return DesignRequirementResponse.from(requirement);
	}
}
