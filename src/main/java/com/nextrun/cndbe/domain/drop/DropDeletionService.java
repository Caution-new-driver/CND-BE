package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.matching.DropAccessorySelectionRepository;
import com.nextrun.cndbe.domain.matching.MaterialCandidateRepository;
import com.nextrun.cndbe.domain.matching.MaterialSelectionService;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// DRAFT Drop을 하위 데이터까지 통째로 지우는 흐름을 담당함.
// f3~f5에서 저장된 데이터를 FK 순서에 맞게 먼저 지우고(대부분 기존 로직 재사용),
// 예약해둔 소재는 다시 AVAILABLE로 되돌린 뒤 마지막에 Drop 자체를 지운다.
@Service
@RequiredArgsConstructor
public class DropDeletionService {

    private final DropRepository dropRepository;
    private final DesignRequirementRepository designRequirementRepository;
    private final MaterialCandidateRepository materialCandidateRepository;
    private final DropAccessorySelectionRepository accessorySelectionRepository;
    private final MaterialSelectionService materialSelectionService;

    @Transactional
    public void delete(UUID dropId) {
        Drop drop = dropRepository.findByIdForUpdate(dropId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Drop을 찾을 수 없습니다: " + dropId
                ));

        if (drop.getStatus() != DropStatus.DRAFT) {
            throw new IllegalStateException(
                    "확정된 Drop은 삭제할 수 없습니다."
            );
        }

        // 소재 예약(RESERVED) 해제 + drop_material_selection 삭제 +
        // production_scenario 트리(scenario/item/material_result) 삭제까지 한 번에 처리됨.
        materialSelectionService.releaseSelectionForResearch(dropId);

        accessorySelectionRepository.deleteAllByDrop_Id(dropId);
        materialCandidateRepository.deleteAllByDrop_Id(dropId);
        designRequirementRepository.deleteByDrop_Id(dropId);

        dropRepository.delete(drop);
    }
}
