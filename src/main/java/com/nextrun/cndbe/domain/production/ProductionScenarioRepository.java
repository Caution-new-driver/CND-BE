package com.nextrun.cndbe.domain.production;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionScenarioRepository
        extends JpaRepository<ProductionScenario, UUID> {

    List<ProductionScenario> findAllByDrop_IdOrderByScenarioTypeAsc(UUID dropId);

    Optional<ProductionScenario> findByIdAndDrop_Id(UUID scenarioId, UUID dropId);
}
