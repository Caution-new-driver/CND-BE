package com.nextrun.cndbe.domain.material.dto;

import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.MaterialType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class MaterialUpdateRequest {

    // AI가 채우거나, 담당자가 수정 확인할 수 있는 값
    private MaterialColor color;
    private MaterialPattern pattern;
    private String texture;
    private Float aiConfidence;
    private String surfaceNotes;


    // 담당자가 직접 수정하는 값
    private String materialCode;
    private MaterialType materialType;

    private MaterialGrade grade;
    private Float widthMm;
    private Float heightMm;
    private Float thicknessMm;
    private String handFeel;
    private String flexibility;
    private Integer quantity;


    // 새 사진을 올린 경우에만 기존 사진을 교체할 값
    private MultipartFile imageFull;
    private MultipartFile imageCloseup;
}
