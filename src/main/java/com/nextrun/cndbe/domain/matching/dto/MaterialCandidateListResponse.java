package com.nextrun.cndbe.domain.matching.dto;

import com.nextrun.cndbe.domain.matching.MaterialCandidate;
import java.util.List;
import java.util.UUID;

// POST 계산 결과와 GET 조회 결과가 공통으로 사용하는 응답 상자.
// 어떤 Drop의 결과인지와 최대 3개의 후보 목록을 함께 반환함.
public record MaterialCandidateListResponse(
        UUID dropId,
        List<MaterialCandidateResponse> candidates
) {

    public static MaterialCandidateListResponse from(
            UUID dropId,
            List<MaterialCandidate> candidates
    ) {
        // JPA 엔티티를 API 전용 DTO로 바꿔서 내부 연관관계가 그대로 노출되지 않게 함.
        return new MaterialCandidateListResponse(
                dropId,
                candidates.stream()
                        .map(MaterialCandidateResponse::from)
                        .toList()
        );
    }
}
