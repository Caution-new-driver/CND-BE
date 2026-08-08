package com.nextrun.cndbe.domain.production;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionScenarioItemRepository
        extends JpaRepository<ProductionScenarioItem, UUID> {

    List<ProductionScenarioItem> findAllByScenario_IdOrderByProductTypeAsc(
            UUID scenarioId
    );
}
