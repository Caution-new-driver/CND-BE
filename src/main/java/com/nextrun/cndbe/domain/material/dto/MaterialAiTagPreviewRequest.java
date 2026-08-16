package com.nextrun.cndbe.domain.material.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class MaterialAiTagPreviewRequest {

	private MultipartFile imageFull; // 전체 사진 (필수)
	private MultipartFile imageCloseup; // 클로즈업 한 사진 (선택)
}
