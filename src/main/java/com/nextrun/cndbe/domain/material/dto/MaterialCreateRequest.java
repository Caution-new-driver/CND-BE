package com.nextrun.cndbe.domain.material.dto;

import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile; //이미지 파일 받을 때 필요

@Getter
@Setter
@NoArgsConstructor
public class MaterialCreateRequest {

	private String materialCode;
	private MaterialType materialType;

	// 담당자가 직접 입력하는 값
	private MaterialGrade grade;
	private Float widthMm;
	private Float heightMm;
	private Float thicknessMm;
	private String handFeel;
	private String flexibility;
	private Integer quantity;

	// 등록 화면에서 받는 사진 파일
	private MultipartFile imageFull; // 전체 사진
	private MultipartFile imageCloseup; // 클로즈업 한 사진
}
