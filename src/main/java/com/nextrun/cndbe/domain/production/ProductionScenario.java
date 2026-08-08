package com.nextrun.cndbe.domain.production;

import com.nextrun.cndbe.common.BaseEntity;
import com.nextrun.cndbe.domain.drop.Drop;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

// Drop 하나당 이 테이블에 로우가 2개 생김: MAIN_ONLY, WITH_LUGGAGE_TAG.
// isSelected로 둘 중 뭘 최종 선택했는지 표시. 확정되면 Drop.selectedScenarioId에 이 id가 채워짐.
@Entity
@Table(name = "production_scenario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionScenario extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "drop_id")
	private Drop drop;

	@Enumerated(EnumType.STRING)
	private ScenarioType scenarioType;

	private Float materialUtilizationRate;

	// 시나리오 계산 시점의 면적 스냅샷. 이후 소재 정보가 바뀌어도 결과가 흔들리지 않게 저장한다.
	private Double totalAvailableAreaMm2;
	private Double usedAreaMm2;
	private Double remainingAreaMm2;

	private Boolean isSelected;
}
