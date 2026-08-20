package com.nextrun.cndbe.domain.production;

import com.nextrun.cndbe.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

// 시나리오 하나 안에서도 제품별(미니백/러기지 태그)로 수량·넘버링이 다르게 나와서
// 한 단계 더 쪼갠 테이블 (예: WITH_LUGGAGE_TAG 시나리오 -> 로우 2개: 미니백 8개, 러기지 태그 13개)
@Entity
@Table(
        name = "production_scenario_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_production_scenario_item_type",
                columnNames = {"scenario_id", "product_type"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionScenarioItem extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scenario_id", nullable = false)
	private ProductionScenario scenario;

	@Enumerated(EnumType.STRING)
	@Column(name = "product_type", nullable = false)
	private ProductType productType;

	@Column(nullable = false)
	private Integer quantity;
	private Integer numberingStart;
	private Integer numberingEnd;
}
