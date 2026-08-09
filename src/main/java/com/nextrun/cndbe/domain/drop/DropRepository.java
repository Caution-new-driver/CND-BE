package com.nextrun.cndbe.domain.drop;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DropRepository extends JpaRepository<Drop, UUID> {

	// 트랜잭션 밖에서 제작 계산을 이어갈 수 있도록 템플릿을 함께 조회한다.
	@EntityGraph(attributePaths = "template")
	@Query("select d from Drop d where d.id = :id")
	Optional<Drop> findByIdWithTemplate(@Param("id") UUID id);
}
