package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.domain.matching.dto.MaterialCandidateListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Material Candidate", description = "Drop별 소재 후보 계산 및 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drops/{dropId}/material-candidates")
public class MaterialCandidateController {

    private final MaterialCandidateService materialCandidateService;

    // [후보 계산/재계산] POST /api/drops/{dropId}/material-candidates
    // 저장된 디자인 조건을 기준으로 후보를 새로 계산하고, 이전 결과를 교체함.
    @Operation(
            summary = "소재 후보 계산 및 저장",
            description = "Drop의 디자인 조건과 미니백 템플릿을 기준으로 소재를 필터링하고, "
                    + "AI 추천 결과를 반영한 최대 3개의 후보를 계산하여 저장합니다. "
                    + "기존에 계산된 후보가 있으면 새 결과로 교체합니다."
    )
    @PostMapping
    public MaterialCandidateListResponse calculateCandidates(
            @Parameter(description = "소재 후보를 계산할 Drop ID") @PathVariable UUID dropId
    ) {
        return materialCandidateService.calculateCandidates(dropId);
    }

    // [계산 결과 조회] GET /api/drops/{dropId}/material-candidates
    // POST에서 DB에 저장한 후보를 순위순으로 반환함.
    @Operation(
            summary = "계산된 소재 후보 조회",
            description = "해당 Drop에 마지막으로 계산하여 저장한 소재 후보를 추천 순위대로 조회합니다."
    )
    @GetMapping
    public MaterialCandidateListResponse getCandidates(
            @Parameter(description = "소재 후보를 조회할 Drop ID") @PathVariable UUID dropId
    ) {
        return materialCandidateService.getCandidates(dropId);
    }
}
