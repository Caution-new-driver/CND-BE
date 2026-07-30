package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

// Drop 1개당 디자인 조건 1개 (ERD: DROP ||--|| DESIGN_REQUIREMENT)
@Entity
@Table(name = "design_requirement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignRequirement extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "drop_id")
	private Drop drop;

	private String materialType;
	private String color;
	private String pattern;
	private String minGrade;
	private String accessoryColor;
	private Boolean usePointMaterial;

	// 참고용 첨부. 수량 계산에는 영향 없음.
	private String sketchImageUrl;
}
