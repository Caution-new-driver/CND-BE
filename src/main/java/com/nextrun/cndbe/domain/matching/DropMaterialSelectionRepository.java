package com.nextrun.cndbe.domain.matching;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// Drop별로 담당자가 최종 확정한 주 소재와 포인트 소재를 저장하고 조회하는 창구.
public interface DropMaterialSelectionRepository
        extends JpaRepository<DropMaterialSelection, UUID> {

    // 같은 Drop에서 다시 선택하면 새 행을 만들지 않고 기존 선택을 교체하기 위해 사용함.
    Optional<DropMaterialSelection> findByDrop_Id(UUID dropId);
}
