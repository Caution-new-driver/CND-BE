package com.nextrun.cndbe.domain.drop;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DropRepository extends JpaRepository<Drop, UUID> {
}
