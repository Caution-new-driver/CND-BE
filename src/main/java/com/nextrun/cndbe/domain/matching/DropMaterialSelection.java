package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.common.BaseEntity;
import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.material.Material;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

// 후보(MaterialCandidate) 중 담당자가 실제로 확정한 것만 담음.
// pointMaterial은 선택사항이라 null일 수 있음.
@Entity
@Table(name = "drop_material_selection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DropMaterialSelection extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "drop_id")
	private Drop drop;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "main_material_id")
	private Material mainMaterial;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "point_material_id")
	private Material pointMaterial;
}
