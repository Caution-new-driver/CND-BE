package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.common.BaseEntity;
import com.nextrun.cndbe.domain.material.Template;
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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

// 테이블명을 "drop"이 아니라 "run_drop"으로 둠: DROP은 SQL 예약어(DROP TABLE 등)라
// 그대로 쓰면 매번 따옴표로 감싸야 하는 번거로움이 생겨서 처음부터 피함.
@Entity
@Table(name = "run_drop")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Drop extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_id")
	private Template template;

	// ProductionScenario -> Drop 참조와 서로 마주보는 순환 관계라, 객체 참조가 아니라
	// UUID 값만 저장. Stage 4에서 제작안이 확정되기 전까지는 null.
	@Column(name = "selected_scenario_id")
	private UUID selectedScenarioId;

	private String name;

	@Enumerated(EnumType.STRING)
	private DropStatus status;

	private Integer expectedProductionDays;

	@Column(columnDefinition = "TEXT")
	private String introText;
}
