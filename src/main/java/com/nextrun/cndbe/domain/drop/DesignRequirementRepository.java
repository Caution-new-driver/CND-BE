package com.nextrun.cndbe.domain.drop;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignRequirementRepository extends JpaRepository<DesignRequirement, UUID> {

	Optional<DesignRequirement> findByDrop_Id(UUID dropId);

	// Drop 삭제 시 하위 데이터를 함께 정리하기 위해 사용. 저장한 적 없으면 아무 일도 안 함.
	void deleteByDrop_Id(UUID dropId);
}
