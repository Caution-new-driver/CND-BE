package com.nextrun.cndbe.domain.matching.dto;

import com.nextrun.cndbe.domain.matching.MaterialCandidate;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.MaterialType;
import java.util.UUID;

// 후보 카드 1개에 필요한 순위·점수·AI 설명과 소재 정보를 담는 응답 DTO.
public record MaterialCandidateResponse(
        UUID candidateId,
        Integer rank,
        Integer matchScore,
        String aiReasons,
        String aiCautions,
        MaterialDetail material
) {

    public static MaterialCandidateResponse from(MaterialCandidate candidate) {
        // MaterialCandidate와 연결된 Material에서 f4 화면 표시값을 한 번에 꺼내 담음.
        Material material = candidate.getMaterial();

        return new MaterialCandidateResponse(
                candidate.getId(),
                candidate.getRank(),
                candidate.getMatchScore(),
                candidate.getAiReasons(),
                candidate.getAiCautions(),
                new MaterialDetail(
                        material.getId(),
                        material.getMaterialCode(),
                        material.getMaterialType(),
                        material.getColor(),
                        material.getPattern(),
                        material.getGrade(),
                        material.getWidthMm(),
                        material.getHeightMm(),
                        material.getQuantity(),
                        material.getImageUrlFull(),
                        material.getImageUrlCloseup()
                )
        );
    }

    public record MaterialDetail(
            UUID id,
            String materialCode,
            MaterialType materialType,
            MaterialColor color,
            MaterialPattern pattern,
            MaterialGrade grade,
            Float widthMm,
            Float heightMm,
            Integer quantity,
            String imageUrlFull,
            String imageUrlCloseup
    ) {
        // 프론트 후보 카드에 보여줄 소재 사진·종류·색상·등급·크기·수량 정보.
    }
}
