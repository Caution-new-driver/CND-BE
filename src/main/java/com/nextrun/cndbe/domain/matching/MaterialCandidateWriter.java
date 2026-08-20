package com.nextrun.cndbe.domain.matching;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// OpenAI 호출이 끝난 뒤 기존 후보 삭제와 새 후보 저장만 짧은 트랜잭션으로 처리한다.
@Component
@RequiredArgsConstructor
public class MaterialCandidateWriter {

    private final MaterialCandidateRepository materialCandidateRepository;
    private final MaterialSelectionService materialSelectionService;

    @Transactional
    public List<MaterialCandidate> replaceAfterResearch(
            UUID dropId,
            List<MaterialCandidate> candidates
    ) {
        // AI 추천까지 성공한 뒤에만 기존 선택·시나리오와 후보를 한 트랜잭션에서 교체한다.
        materialSelectionService.releaseSelectionForResearch(dropId);
        materialCandidateRepository.deleteAllByDrop_Id(dropId);
        return materialCandidateRepository.saveAll(candidates);
    }
}
