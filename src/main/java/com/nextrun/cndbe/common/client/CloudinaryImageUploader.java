package com.nextrun.cndbe.common.client;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

// 다른 도메인(소재 사진, 추천 카드 이미지 등)에서도 재사용할 수 있는 공용 업로드 헬퍼.
@Component
@RequiredArgsConstructor
public class CloudinaryImageUploader {

	private final Cloudinary cloudinary;

	public String upload(MultipartFile file) {
		try {
			Object result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
			return (String) ((java.util.Map<?, ?>) result).get("secure_url");
		} catch (IOException e) {
			throw new UncheckedIOException("이미지 업로드 실패: " + file.getOriginalFilename(), e);
		}
	}
}
