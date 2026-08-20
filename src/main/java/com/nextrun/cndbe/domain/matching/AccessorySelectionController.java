package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.domain.matching.dto.AccessorySelectionRequest;
import com.nextrun.cndbe.domain.matching.dto.AccessorySelectionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// f4에서 사용자가 직접 고른 부자재 세트를 확정 저장하는 b11 API.
@Tag(
        name = "Accessory Selection",
        description = "b11 부자재 세트 확정 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drops/{dropId}/accessory-selections")
public class AccessorySelectionController {

    private final AccessorySelectionService accessorySelectionService;

    @Operation(
            summary = "부자재 세트 선택 저장",
            description = "GET /api/accessories에서 조회한 부자재 ID 목록으로 기존 선택을 교체합니다."
    )
    @PostMapping
    public AccessorySelectionResponse selectAccessories(
            @Parameter(description = "부자재 세트를 확정할 Drop ID")
            @PathVariable UUID dropId,
            @Valid @RequestBody AccessorySelectionRequest request
    ) {
        return accessorySelectionService.selectAccessories(dropId, request);
    }

    // "이어서 제작" 재진입 시 f4에서 이전에 선택한 부자재 세트를 복원하기 위한 조회.
    @Operation(
            summary = "부자재 세트 선택 조회",
            description = "이 Drop에 저장된 부자재 선택을 조회합니다. 하나도 선택한 적이 없으면 빈 목록을 반환합니다."
    )
    @GetMapping
    public AccessorySelectionResponse getSelections(
            @Parameter(description = "부자재 선택을 조회할 Drop ID")
            @PathVariable UUID dropId
    ) {
        return accessorySelectionService.getSelections(dropId);
    }
}
