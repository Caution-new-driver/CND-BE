package com.nextrun.cndbe.domain.drop;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DropRepository extends JpaRepository<Drop, UUID> {

	// 같은 Drop의 선택 정보를 동시에 수정해서 중복 행이 생기지 않도록 작업 동안 잠금.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from Drop d where d.id = :id")
	Optional<Drop> findByIdForUpdate(@Param("id") UUID id);

	// 트랜잭션 밖에서 제작 계산을 이어갈 수 있도록 템플릿을 함께 조회한다.
	@EntityGraph(attributePaths = "template")
	@Query("select d from Drop d where d.id = :id")
	Optional<Drop> findByIdWithTemplate(@Param("id") UUID id);
}
