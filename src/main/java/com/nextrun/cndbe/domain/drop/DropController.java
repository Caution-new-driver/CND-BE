package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.drop.dto.DesignRequirementResponse;
import com.nextrun.cndbe.domain.drop.dto.DropConfirmRequest;
import com.nextrun.cndbe.domain.drop.dto.DropConfirmResponse;
import com.nextrun.cndbe.domain.drop.dto.DropIntroTextRequest;
import com.nextrun.cndbe.domain.drop.dto.DropIntroTextResponse;
import com.nextrun.cndbe.domain.drop.dto.DropResponse;
import com.nextrun.cndbe.domain.material.AccessoryColor;
import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.MaterialType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Drop")
@RestController
@RequiredArgsConstructor
public class DropController {

	private final DropService dropService;
	private final DropConfirmationService dropConfirmationService;
	private final DropIntroTextService dropIntroTextService;

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

	// b13: 선택된 제작안(b12)과 소재 조합(b11)을 확정하고 Drop 상태를 CONFIRMED로 전환
	@PatchMapping("/api/drops/{dropId}/confirm")
	public DropConfirmResponse confirm(
			@PathVariable UUID dropId,
			@Valid @RequestBody DropConfirmRequest request) {
		return dropConfirmationService.confirm(dropId, request);
	}

	// b14: 확정된 Drop 정보를 기반으로 AI 소개문 초안 생성
	@PostMapping("/api/drops/{dropId}/intro-text")
	public DropIntroTextResponse generateIntroText(@PathVariable UUID dropId) {
		return dropIntroTextService.generate(dropId);
	}

	// b14: 담당자가 수정한 소개문 최종본 저장
	@PatchMapping("/api/drops/{dropId}/intro-text")
	public DropIntroTextResponse updateIntroText(
			@PathVariable UUID dropId,
			@Valid @RequestBody DropIntroTextRequest request) {
		return dropIntroTextService.update(dropId, request);
	}
}
