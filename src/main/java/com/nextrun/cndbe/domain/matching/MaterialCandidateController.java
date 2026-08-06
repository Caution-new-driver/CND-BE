package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.domain.matching.dto.MaterialCandidateListResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// b9+b10의 "카운터 직원". 프론트엔드의 후보 계산·조회 요청을 받아서
// 실제 필터링, 점수 계산, AI 추천을 담당하는 Service로 전달함.
@Tag(name = "Material Candidate")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drops/{dropId}/material-candidates")
public class MaterialCandidateController {

    private final MaterialCandidateService materialCandidateService;

    // [후보 계산/재계산] POST /api/drops/{dropId}/material-candidates
    // 저장된 디자인 조건을 기준으로 후보를 새로 계산하고, 이전 결과를 교체함.
    @PostMapping
    public MaterialCandidateListResponse calculateCandidates(
            @PathVariable UUID dropId
    ) {
        return materialCandidateService.calculateCandidates(dropId);
    }

    // [계산 결과 조회] GET /api/drops/{dropId}/material-candidates
    // POST에서 DB에 저장한 후보를 순위순으로 반환함.
    @GetMapping
    public MaterialCandidateListResponse getCandidates(
            @PathVariable UUID dropId
    ) {
        return materialCandidateService.getCandidates(dropId);
    }
}
