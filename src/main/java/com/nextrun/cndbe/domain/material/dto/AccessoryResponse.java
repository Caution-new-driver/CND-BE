package com.nextrun.cndbe.domain.material.dto;

import com.nextrun.cndbe.domain.material.Accessory;
import com.nextrun.cndbe.domain.material.AccessoryColor;
import java.util.UUID;

// f4가 부자재 선택 항목을 만드는 데 필요한 ID·종류·색상만 반환함.
public record AccessoryResponse(
        UUID id,
        String accessoryType,
        AccessoryColor color
) {

    public static AccessoryResponse from(Accessory accessory) {
        return new AccessoryResponse(
                accessory.getId(),
                accessory.getAccessoryType(),
                accessory.getColor()
        );
    }
}
