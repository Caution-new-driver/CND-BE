package com.nextrun.cndbe.domain.material.repository;

import com.nextrun.cndbe.domain.material.Material;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialRepository extends JpaRepository<Material, UUID>, JpaSpecificationExecutor<Material> {

	// 두 Drop이 같은 소재를 동시에 선택하는 상황을 막기 위해 선택 처리 동안 소재 행을 잠금.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from Material m where m.id in :ids order by m.id")
	List<Material> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);
}
