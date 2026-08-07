package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.domain.matching.dto.MaterialSelectionRequest;
import com.nextrun.cndbe.domain.matching.dto.MaterialSelectionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// f4에서 사용자가 직접 고른 주 소재와 선택적 포인트 소재를 확정 저장하는 b11 API.
@Tag(name = "Material Selection")
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
            @PathVariable UUID dropId,
            @Valid @RequestBody MaterialSelectionRequest request
    ) {
        return materialSelectionService.selectMaterials(dropId, request);
    }
}
