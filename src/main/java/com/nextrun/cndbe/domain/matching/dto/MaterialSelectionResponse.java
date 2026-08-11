package com.nextrun.cndbe.domain.matching.dto;

import com.nextrun.cndbe.domain.matching.DropMaterialSelection;
import com.nextrun.cndbe.domain.material.dto.MaterialResponse;
import java.util.UUID;

// 저장 직후 f4 요약 카드에 사용할 수 있도록 주 소재와 포인트 소재 상세 정보를 함께 반환함.
public record MaterialSelectionResponse(
        UUID selectionId,
        UUID dropId,
        MaterialResponse mainMaterial,
        MaterialResponse pointMaterial
) {

    public static MaterialSelectionResponse from(
            DropMaterialSelection selection
    ) {
        return new MaterialSelectionResponse(
                selection.getId(),
                selection.getDrop().getId(),
                MaterialResponse.from(selection.getMainMaterial()),
                selection.getPointMaterial() == null
                        ? null
                        : MaterialResponse.from(selection.getPointMaterial())
        );
    }
}
