package com.nextrun.cndbe.domain.material;

import com.nextrun.cndbe.domain.material.dto.AccessoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// b11에서 선택 가능한 기존 부자재의 ID·종류·색상 목록을 제공함.
@Tag(
        name = "Accessory",
        description = "b11에서 선택 가능한 부자재 조회 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accessories")
public class AccessoryController {

    private final AccessoryService accessoryService;

    @Operation(
            summary = "부자재 목록 조회",
            description = "DB에 등록된 부자재를 종류·색상 순으로 반환합니다."
    )
    @GetMapping
    public List<AccessoryResponse> getAccessories() {
        return accessoryService.getAccessories();
    }
}
