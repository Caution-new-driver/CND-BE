package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.common.BaseEntity;
import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.material.Material;
import jakarta.persistence.Column;
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

// AI가 추천한 후보 (최대 3개). 탈락한 후보도 그대로 남겨서 "왜 이걸 추천했었는지"
// 나중에 되짚어볼 수 있게 함 — 최종 확정본은 DropMaterialSelection에 별도 저장.
@Entity
@Table(name = "material_candidate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialCandidate extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "drop_id")
	private Drop drop;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "material_id")
	private Material material;

	private Integer matchScore;

	@Column(columnDefinition = "TEXT")
	private String aiReasons;

	@Column(columnDefinition = "TEXT")
	private String aiCautions;

	private Integer rank;
}
