package com.nextrun.cndbe.domain.matching.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

// f4에서 선택한 b9 추천 후보 ID를 받음. 포인트 소재는 선택사항이라 null을 허용함.
public record MaterialSelectionRequest(
        @NotNull(message = "주 소재 후보 ID는 필수입니다.")
        UUID mainCandidateId,
        UUID pointCandidateId
) {
}
