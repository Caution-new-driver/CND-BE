package com.nextrun.cndbe.common.client;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

// 다른 도메인(소재 사진, 추천 카드 이미지 등)에서도 재사용할 수 있는 공용 업로드 헬퍼.
@Component
@RequiredArgsConstructor
public class CloudinaryImageUploader {

	// 프론트가 목록/상세 팝업에서 쓰는 축소본과 동일한 변환. 여기서 eager로 미리 만들어두지
	// 않으면 프론트가 이 크기를 처음 요청할 때 Cloudinary가 그 자리에서 원본을 변환하느라
	// 응답이 느려진다(src/lib/cloudinary-image.ts의 THUMBNAIL_TRANSFORM과 반드시 일치해야 함).
	private static final Transformation THUMBNAIL_TRANSFORMATION = new Transformation()
			.width(160).height(160).crop("fill").quality("auto").fetchFormat("auto");

	private final Cloudinary cloudinary;

	public String upload(MultipartFile file) {
		try {
			Object result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
					"eager", List.of(THUMBNAIL_TRANSFORMATION)
			));
			return (String) ((java.util.Map<?, ?>) result).get("secure_url");
		} catch (IOException e) {
			throw new UncheckedIOException("이미지 업로드 실패: " + file.getOriginalFilename(), e);
		}
	}
}
