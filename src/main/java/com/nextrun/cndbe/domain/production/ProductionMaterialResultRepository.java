package com.nextrun.cndbe.domain.production;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionMaterialResultRepository
        extends JpaRepository<ProductionMaterialResult, UUID> {

    List<ProductionMaterialResult> findAllByScenario_IdOrderByMaterialRoleAsc(
            UUID scenarioId
    );
}
