package com.nextrun.cndbe.domain.matching;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// Drop에서 최종 선택한 지퍼·링 등의 부자재 연결 정보를 저장하는 창구.
public interface DropAccessorySelectionRepository
        extends JpaRepository<DropAccessorySelection, UUID> {

    List<DropAccessorySelection> findAllByDrop_Id(UUID dropId);

    // 같은 Drop에서 다시 선택하면 과거 선택을 지우고 새 세트로 교체함.
    void deleteAllByDrop_Id(UUID dropId);
}
