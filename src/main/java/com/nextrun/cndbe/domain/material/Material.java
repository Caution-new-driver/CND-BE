package com.nextrun.cndbe.domain.material;

import com.nextrun.cndbe.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "material")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	private String materialCode;

	@Enumerated(EnumType.STRING)
	private MaterialType materialType;

	// AI가 사진 분석으로 채우는 값
	@Enumerated(EnumType.STRING)
	private MaterialColor color;

	@Enumerated(EnumType.STRING)
	private MaterialPattern pattern;

	private String texture;
	private Float aiConfidence;
	private String surfaceNotes;

	// 사람이 직접 입력하는 값
	@Enumerated(EnumType.STRING)
	private MaterialGrade grade;

	private Float widthMm;
	private Float heightMm;
	private Float thicknessMm;
	private String handFeel;
	private String flexibility;
	private Integer quantity;

	private String imageUrlFull;
	private String imageUrlCloseup;

	// Cloudinary 삭제 API(destroy)는 URL이 아니라 이 값으로만 동작해서,
	// 소재 삭제/사진 교체 시 Cloudinary 원본을 지우려면 따로 보관해둬야 함.
	private String imagePublicIdFull;
	private String imagePublicIdCloseup;

	@Enumerated(EnumType.STRING)
	private MaterialStatus status;
}
