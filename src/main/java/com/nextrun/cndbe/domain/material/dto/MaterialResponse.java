package com.nextrun.cndbe.domain.material.dto;

import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.MaterialStatus;
import com.nextrun.cndbe.domain.material.MaterialType;


import java.time.LocalDateTime;
import java.util.UUID;

public record MaterialResponse(

        UUID id,

        String materialCode,
        MaterialType materialType,

        //AI가 채운 필드
        MaterialColor color,
        MaterialPattern pattern,
        String texture,
        Float aiConfidence,
        String surfaceNotes,

        //담당자가 입력한 필드

        MaterialGrade grade,
        Float widthMm,
        Float heightMm,
        Float thicknessMm,
        String handFeel,
        String flexibility,
        Integer quantity,

        //사진 URL
        String imageUrlFull,
        String imageUrlCloseup,

        MaterialStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.getId(),

                material.getMaterialCode(),
                material.getMaterialType(),

                material.getColor(),
                material.getPattern(),
                material.getTexture(),
                material.getAiConfidence(),
                material.getSurfaceNotes(),

                material.getGrade(),
                material.getWidthMm(),
                material.getHeightMm(),
                material.getThicknessMm(),
                material.getHandFeel(),
                material.getFlexibility(),
                material.getQuantity(),

                material.getImageUrlFull(),
                material.getImageUrlCloseup(),

                material.getStatus(),
                material.getCreatedAt(),
                material.getUpdatedAt()


        );
    }
}
