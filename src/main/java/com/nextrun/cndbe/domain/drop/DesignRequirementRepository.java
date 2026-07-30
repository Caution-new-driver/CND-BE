package com.nextrun.cndbe.domain.drop;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignRequirementRepository extends JpaRepository<DesignRequirement, UUID> {

	Optional<DesignRequirement> findByDrop_Id(UUID dropId);
}
