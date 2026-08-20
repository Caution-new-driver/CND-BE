package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.drop.dto.DesignRequirementResponse;
import com.nextrun.cndbe.domain.drop.dto.DropConfirmRequest;
import com.nextrun.cndbe.domain.drop.dto.DropConfirmResponse;
import com.nextrun.cndbe.domain.drop.dto.DropIntroTextRequest;
import com.nextrun.cndbe.domain.drop.dto.DropIntroTextResponse;
import com.nextrun.cndbe.domain.drop.dto.DropResponse;
import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.MaterialType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Drop", description = "Drop 기획 시작·디자인 조건 저장·확정·소개문 수정 API")
@RestController
@RequiredArgsConstructor
public class DropController {

    private final DropService dropService;
    private final DropConfirmationService dropConfirmationService;
    private final DropIntroTextService dropIntroTextService;
    private final DropDeletionService dropDeletionService;

    // b7: 새 RUN Drop 기획 시작 - draft 상태 Drop 생성, 고정 미니백 템플릿 정보 함께 반환
    @Operation(
            summary = "새 Drop 생성",
            description = "draft 상태의 Drop을 새로 생성하고, 고정 미니백 템플릿(패턴 조각·필요 부자재 포함) 정보를 함께 반환합니다."
    )
    @PostMapping("/api/drops")
    @ResponseStatus(HttpStatus.CREATED)
    public DropResponse createDraftDrop() {
        Drop drop = dropService.createDraftDrop();
        return DropResponse.from(drop);
    }

    // Drop 목록 조회 API - 내부(관리자/담당자)용 조회. 고객 공개용이 아님.
    @Operation(
            summary = "Drop 목록 조회 (내부용)",
            description = "등록된 Drop 목록을 조회합니다. status로 필터링할 수 있고, 생략하면 전체를 반환합니다. "
                    + "고객에게 공개하는 화면이 아니라 담당자가 내부에서 확인하는 용도입니다."
    )
    @GetMapping("/api/drops")
    public List<DropResponse> list(
            @Parameter(description = "Drop 상태로 필터링 (생략 시 전체 조회)") @RequestParam(required = false) DropStatus status) {
        return dropService.list(status).stream()
                .map(DropResponse::from)
                .toList();
    }

    // 진입 경로(새로 확정 vs 탭 이동·재진입)와 무관하게 "이 Drop이 지금 CONFIRMED인지"를
    // 프론트가 항상 조회할 수 있도록 하는 단건 조회. 없으면 404.
    @Operation(
            summary = "Drop 단건 조회",
            description = "Drop의 현재 상태(DRAFT/CONFIRMED)를 포함한 정보를 조회합니다."
    )
    @GetMapping("/api/drops/{dropId}")
    public DropResponse get(
            @Parameter(description = "조회할 Drop ID") @PathVariable UUID dropId) {
        return DropResponse.from(dropService.get(dropId));
    }

    // b8: 디자인 조건 저장 (스케치 이미지 첨부 포함, 선택사항)
    @Operation(
            summary = "디자인 조건 저장",
            description = "소재 타입·색상·패턴·최소 등급을 저장합니다. "
                    + "같은 Drop으로 재호출하면 새로 생기지 않고 기존 조건을 덮어씁니다(upsert)."
    )
    @PostMapping(value = "/api/drops/{dropId}/design-requirement")
    public DesignRequirementResponse saveDesignRequirement(
            @Parameter(description = "디자인 조건을 저장할 Drop ID") @PathVariable UUID dropId,
            @RequestParam(required = false) MaterialType materialType,
            @RequestParam(required = false) MaterialColor color,
            @RequestParam(required = false) MaterialPattern pattern,
            @RequestParam(required = false) MaterialGrade minGrade) {
        DesignRequirement requirement = dropService.saveDesignRequirement(
                dropId, materialType, color, pattern, minGrade);
        return DesignRequirementResponse.from(requirement);
    }

    // "이어서 제작" 재진입 시 f3 폼을 이전에 저장한 값으로 채우기 위한 조회.
    @Operation(
            summary = "디자인 조건 조회",
            description = "이 Drop에 저장된 디자인 조건을 조회합니다. 아직 저장한 적이 없으면 404를 반환합니다."
    )
    @GetMapping("/api/drops/{dropId}/design-requirement")
    public DesignRequirementResponse getDesignRequirement(
            @Parameter(description = "디자인 조건을 조회할 Drop ID") @PathVariable UUID dropId) {
        return DesignRequirementResponse.from(dropService.getDesignRequirement(dropId));
    }

    // DRAFT Drop과 그동안 저장된 하위 데이터(디자인 조건·추천 후보·소재/부자재 선택·제작안)를
    // 통째로 삭제. 예약해둔 소재는 다시 AVAILABLE로 돌아감.
    @Operation(
            summary = "Drop 삭제",
            description = "아직 확정하지 않은(DRAFT) Drop과 그 하위 데이터를 모두 삭제합니다. "
                    + "예약해둔 소재는 다시 AVAILABLE로 돌아갑니다. "
                    + "이미 확정된(CONFIRMED) Drop은 삭제할 수 없습니다(409)."
    )
    @DeleteMapping("/api/drops/{dropId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "삭제할 Drop ID") @PathVariable UUID dropId) {
        dropDeletionService.delete(dropId);
    }

    // b13: 선택된 제작안(b12)과 소재 조합(b11)을 확정하고 Drop 상태를 CONFIRMED로 전환
    @Operation(
            summary = "Drop 확정",
            description = "선택된 제작안(b12)과 소재 조합(b11)이 모두 끝난 Drop을 CONFIRMED로 전환하고, "
                    + "이름·예상 제작기간을 저장하며, 확정된 소재를 DEPLETED로 전환합니다. "
                    + "같은 흐름 안에서 AI 소개문 초안(b14)도 함께 생성해 반환합니다(실패해도 확정 자체는 성공). "
                    + "이 최초 생성도 Drop당 총 " + DropConfirmationWriter.MAX_INTRO_TEXT_GENERATION_COUNT
                    + "회 한도에 포함됩니다."
    )
    @PatchMapping("/api/drops/{dropId}/confirm")
    public DropConfirmResponse confirm(
            @Parameter(description = "확정할 Drop ID") @PathVariable UUID dropId,
            @Valid @RequestBody DropConfirmRequest request) {
        return dropConfirmationService.confirm(dropId, request);
    }

    // b14: 담당자가 확정된 소개문을 다시 AI로 생성 요청 (b13 최초 생성 포함 Drop당 총 6회까지)
    @Operation(
            summary = "소개문 AI 재생성",
            description = "b13 확정 시 생성된 AI 소개문 초안이 마음에 들지 않을 때 다시 요청합니다. "
                    + "Drop이 CONFIRMED 상태가 아니면 호출할 수 없고, "
                    + "b13 최초 생성을 포함해 Drop당 총 " + DropConfirmationWriter.MAX_INTRO_TEXT_GENERATION_COUNT
                    + "회까지만 호출 가능합니다(초과 시 409). 실패한 시도도 횟수에 포함됩니다."
    )
    @PostMapping("/api/drops/{dropId}/intro-text")
    public DropIntroTextResponse regenerateIntroText(
            @Parameter(description = "소개문을 재생성할 Drop ID") @PathVariable UUID dropId) {
        return dropIntroTextService.regenerate(dropId);
    }

    // b14: 담당자가 수정한 소개문 최종본 저장 (AI 재호출 없음, 횟수 제한과 무관)
    @Operation(
            summary = "소개문 수정본 저장",
            description = "AI가 생성한 소개문을 담당자가 직접 고친 최종본으로 덮어씁니다. "
                    + "Drop이 CONFIRMED 상태가 아니면 저장할 수 없습니다."
    )
    @PatchMapping("/api/drops/{dropId}/intro-text")
    public DropIntroTextResponse updateIntroText(
            @Parameter(description = "소개문을 수정할 Drop ID") @PathVariable UUID dropId,
            @Valid @RequestBody DropIntroTextRequest request) {
        return dropIntroTextService.update(dropId, request);
    }
}