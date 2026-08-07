package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.drop.DropRepository;
import com.nextrun.cndbe.domain.drop.DropStatus;
import com.nextrun.cndbe.domain.matching.dto.MaterialSelectionRequest;
import com.nextrun.cndbe.domain.matching.dto.MaterialSelectionResponse;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.MaterialStatus;
import com.nextrun.cndbe.domain.material.repository.MaterialRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// b11 소재 선택의 전체 흐름을 담당함.
// 추천 후보 확인 -> 재고 상태 검증 -> 기존 예약 해제 -> 새 소재 예약 -> 확정본 저장 순서로 처리함.
@Service
@RequiredArgsConstructor
public class MaterialSelectionService {

    private final DropRepository dropRepository;
    private final MaterialCandidateRepository materialCandidateRepository;
    private final MaterialRepository materialRepository;
    private final DropMaterialSelectionRepository selectionRepository;

    @Transactional
    public MaterialSelectionResponse selectMaterials(
            UUID dropId,
            MaterialSelectionRequest request
    ) {
        Drop drop = findEditableDrop(dropId);

        MaterialCandidate mainCandidate = findCandidate(
                dropId,
                request.mainCandidateId(),
                "주 소재"
        );
        MaterialCandidate pointCandidate = request.pointCandidateId() == null
                ? null
                : findCandidate(
                        dropId,
                        request.pointCandidateId(),
                        "포인트 소재"
                );

        UUID mainMaterialId = getMaterialId(mainCandidate, "주 소재");
        UUID pointMaterialId = pointCandidate == null
                ? null
                : getMaterialId(pointCandidate, "포인트 소재");

        if (mainMaterialId.equals(pointMaterialId)) {
            throw new IllegalArgumentException(
                    "주 소재와 포인트 소재는 서로 달라야 합니다."
            );
        }

        DropMaterialSelection selection = selectionRepository
                .findByDrop_Id(dropId)
                .orElseGet(() -> DropMaterialSelection.builder()
                        .drop(drop)
                        .build());

        Set<UUID> oldMaterialIds = selectedMaterialIds(selection);
        Set<UUID> newMaterialIds = new LinkedHashSet<>();
        newMaterialIds.add(mainMaterialId);
        if (pointMaterialId != null) {
            newMaterialIds.add(pointMaterialId);
        }

        // 기존 소재와 새 소재를 한 번에 잠가서 두 Drop이 같은 소재를 동시에 예약하지 못하게 함.
        Set<UUID> materialIdsToLock = new LinkedHashSet<>(oldMaterialIds);
        materialIdsToLock.addAll(newMaterialIds);
        Map<UUID, Material> lockedMaterials = lockMaterials(materialIdsToLock);

        validateAvailable(newMaterialIds, oldMaterialIds, lockedMaterials);
        releaseRemovedMaterials(oldMaterialIds, newMaterialIds, lockedMaterials);
        reserveMaterials(newMaterialIds, lockedMaterials);

        selection.setMainMaterial(lockedMaterials.get(mainMaterialId));
        selection.setPointMaterial(
                pointMaterialId == null
                        ? null
                        : lockedMaterials.get(pointMaterialId)
        );

        return MaterialSelectionResponse.from(
                selectionRepository.save(selection)
        );
    }

    // f4에서 조건을 수정해 후보를 다시 계산하면 이전 확정본을 지우고
    // 해당 Drop이 잡고 있던 RESERVED 소재를 다시 AVAILABLE로 돌려놓음.
    @Transactional
    public void releaseSelectionForResearch(UUID dropId) {
        findEditableDrop(dropId);

        DropMaterialSelection selection = selectionRepository
                .findByDrop_Id(dropId)
                .orElse(null);
        if (selection == null) {
            return;
        }

        Set<UUID> materialIds = selectedMaterialIds(selection);
        if (materialIds.isEmpty()) {
            throw new IllegalStateException(
                    "기존 소재 선택 정보에 연결된 소재가 없습니다."
            );
        }
        Map<UUID, Material> lockedMaterials = lockMaterials(materialIds);
        lockedMaterials.values().stream()
                .filter(material ->
                        material.getStatus() == MaterialStatus.RESERVED
                )
                .forEach(material ->
                        material.setStatus(MaterialStatus.AVAILABLE)
                );

        selectionRepository.delete(selection);
    }

    private Drop findEditableDrop(UUID dropId) {
        Drop drop = dropRepository.findByIdForUpdate(dropId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Drop을 찾을 수 없습니다: " + dropId
                ));

        if (drop.getStatus() != DropStatus.DRAFT) {
            throw new IllegalStateException(
                    "확정된 Drop의 소재 선택은 변경할 수 없습니다."
            );
        }
        return drop;
    }

    private MaterialCandidate findCandidate(
            UUID dropId,
            UUID candidateId,
            String role
    ) {
        return materialCandidateRepository
                .findByIdAndDrop_Id(candidateId, dropId)
                .orElseThrow(() -> new IllegalArgumentException(
                        role + " 후보를 찾을 수 없습니다: " + candidateId
                ));
    }

    private UUID getMaterialId(
            MaterialCandidate candidate,
            String role
    ) {
        if (candidate.getMaterial() == null
                || candidate.getMaterial().getId() == null) {
            throw new IllegalStateException(
                    role + " 후보에 소재 정보가 없습니다."
            );
        }
        return candidate.getMaterial().getId();
    }

    private Set<UUID> selectedMaterialIds(
            DropMaterialSelection selection
    ) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (selection.getMainMaterial() != null) {
            ids.add(selection.getMainMaterial().getId());
        }
        if (selection.getPointMaterial() != null) {
            ids.add(selection.getPointMaterial().getId());
        }
        return ids;
    }

    private Map<UUID, Material> lockMaterials(Set<UUID> materialIds) {
        List<UUID> sortedIds = new ArrayList<>(materialIds);
        sortedIds.sort((left, right) -> left.toString()
                .compareTo(right.toString()));

        List<Material> materials = materialRepository
                .findAllByIdForUpdate(sortedIds);
        if (materials.size() != materialIds.size()) {
            throw new IllegalStateException(
                    "선택한 소재 정보를 읽을 수 없습니다."
            );
        }

        return materials.stream().collect(Collectors.toMap(
                Material::getId,
                Function.identity()
        ));
    }

    private void validateAvailable(
            Set<UUID> newMaterialIds,
            Set<UUID> oldMaterialIds,
            Map<UUID, Material> materials
    ) {
        for (UUID materialId : newMaterialIds) {
            Material material = materials.get(materialId);
            boolean alreadySelectedByThisDrop = oldMaterialIds
                    .contains(materialId);

            if (material.getStatus() == MaterialStatus.AVAILABLE) {
                continue;
            }
            if (alreadySelectedByThisDrop
                    && material.getStatus() == MaterialStatus.RESERVED) {
                continue;
            }

            throw new IllegalStateException(
                    "이미 다른 Drop에서 선택했거나 소진된 소재입니다: "
                            + material.getMaterialCode()
            );
        }
    }

    private void releaseRemovedMaterials(
            Set<UUID> oldMaterialIds,
            Set<UUID> newMaterialIds,
            Map<UUID, Material> materials
    ) {
        oldMaterialIds.stream()
                .filter(materialId -> !newMaterialIds.contains(materialId))
                .map(materials::get)
                .filter(material ->
                        material.getStatus() == MaterialStatus.RESERVED
                )
                .forEach(material ->
                        material.setStatus(MaterialStatus.AVAILABLE)
                );
    }

    private void reserveMaterials(
            Set<UUID> newMaterialIds,
            Map<UUID, Material> materials
    ) {
        newMaterialIds.stream()
                .map(materials::get)
                .forEach(material ->
                        material.setStatus(MaterialStatus.RESERVED)
                );
    }
}
