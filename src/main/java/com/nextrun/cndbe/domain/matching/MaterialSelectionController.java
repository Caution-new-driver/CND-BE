package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.domain.matching.dto.MaterialSelectionRequest;
import com.nextrun.cndbe.domain.matching.dto.MaterialSelectionResponse;
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

// f4에서 사용자가 직접 고른 주 소재와 선택적 포인트 소재를 확정 저장하는 b11 API.
@Tag(
        name = "Material Selection",
        description = "b11 주 소재·포인트 소재 확정 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drops/{dropId}/material-selection")
public class MaterialSelectionController {

    private final MaterialSelectionService materialSelectionService;

    @Operation(
            summary = "주 소재·포인트 소재 선택 저장",
            description = "b9 추천 후보 ID를 받아 선택을 저장하고 소재 상태를 RESERVED로 변경합니다."
    )
    @PostMapping
    public MaterialSelectionResponse selectMaterials(
            @Parameter(description = "소재 조합을 확정할 Drop ID")
            @PathVariable UUID dropId,
            @Valid @RequestBody MaterialSelectionRequest request
    ) {
        return materialSelectionService.selectMaterials(dropId, request);
    }

    // "이어서 제작" 재진입 시 f4에서 이전에 확정한 조합을 복원하기 위한 조회.
    @Operation(
            summary = "주 소재·포인트 소재 선택 조회",
            description = "이 Drop에 저장된 소재 선택을 조회합니다. 아직 선택한 적이 없으면 404를 반환합니다."
    )
    @GetMapping
    public MaterialSelectionResponse getSelection(
            @Parameter(description = "소재 조합을 조회할 Drop ID")
            @PathVariable UUID dropId
    ) {
        return materialSelectionService.getSelection(dropId);
    }
}
