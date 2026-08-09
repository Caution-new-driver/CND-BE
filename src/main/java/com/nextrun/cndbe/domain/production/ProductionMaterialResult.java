package com.nextrun.cndbe.domain.production;

import com.nextrun.cndbe.common.BaseEntity;
import com.nextrun.cndbe.domain.material.Material;
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

// 시나리오별·소재별 계산 결과. GET이 재계산하지 않고 POST 당시 결과를 그대로 보여주기 위해 저장한다.
@Entity
@Table(
        name = "production_material_result",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_production_material_result_role",
                columnNames = {"scenario_id", "material_role"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionMaterialResult extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ProductionScenario scenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Enumerated(EnumType.STRING)
    @Column(name = "material_role", nullable = false)
    private MaterialRole materialRole;

    // 이 소재가 담당하는 패턴만 보았을 때 지원 가능한 미니백 최대 수량.
    private Integer supportedMiniBagQuantity;
    private Integer luggageTagQuantity;
    private Double availableAreaMm2;
    private Double usedAreaMm2;
    private Double remainingAreaMm2;

    // 서로 떨어진 잔여 사각형 목록을 JSON으로 보관한다.
    @Column(columnDefinition = "TEXT")
    private String remainingRegions;
}
