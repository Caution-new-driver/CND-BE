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
	private String materialType;

	// AI가 사진 분석으로 채우는 값
	private String color;
	private String pattern;
	private String texture;
	private Float aiConfidence;
	private String surfaceNotes;

	// 사람이 직접 입력하는 값
	private String grade;
	private Float widthCm;
	private Float heightCm;
	private Float thicknessMm;
	private String handFeel;
	private String flexibility;
	private Integer quantity;

	private String imageUrlFull;
	private String imageUrlCloseup;

	@Enumerated(EnumType.STRING)
	private MaterialStatus status;
}
