package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.common.BaseEntity;
import com.nextrun.cndbe.domain.material.AccessoryColor;
import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.MaterialType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	@Enumerated(EnumType.STRING)
	private MaterialType materialType;

	@Enumerated(EnumType.STRING)
	private MaterialColor color;

	@Enumerated(EnumType.STRING)
	private MaterialPattern pattern;

	@Enumerated(EnumType.STRING)
	private MaterialGrade minGrade;

	@Enumerated(EnumType.STRING)
	private AccessoryColor accessoryColor;
}
