package com.nextrun.cndbe.domain.matching.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

// GET /api/accessories에서 받은 부자재 ID를 목록으로 전달함.
public record AccessorySelectionRequest(
        @NotEmpty(message = "부자재를 한 개 이상 선택해야 합니다.")
        List<@NotNull(message = "부자재 ID에는 null을 넣을 수 없습니다.") UUID> accessoryIds
) {
}
