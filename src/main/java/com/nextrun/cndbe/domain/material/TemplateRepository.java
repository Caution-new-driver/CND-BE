package com.nextrun.cndbe.domain.material;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<Template, UUID> {

	Optional<Template> findByName(String name);
}
