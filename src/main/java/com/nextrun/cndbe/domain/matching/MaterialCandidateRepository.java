package com.nextrun.cndbe.domain.matching;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// 계산된 추천 후보(MaterialCandidate)를 DB에 저장하고 조회하는 창구.
public interface MaterialCandidateRepository
        extends JpaRepository<MaterialCandidate, UUID> {

    // GET 응답에서 추천 순위가 1, 2, 3 순서로 나오도록 조회함.
    List<MaterialCandidate> findAllByDrop_IdOrderByRankAsc(UUID dropId);

    // 같은 Drop을 재검색하면 과거 계산 결과를 지우고 새 결과로 교체함.
    void deleteAllByDrop_Id(UUID dropId);
}
