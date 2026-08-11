package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.drop.dto.DropConfirmRequest;
import com.nextrun.cndbe.domain.drop.dto.DropConfirmResponse;
import com.nextrun.cndbe.domain.matching.DropMaterialSelection;
import com.nextrun.cndbe.domain.matching.DropMaterialSelectionRepository;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.MaterialStatus;
import com.nextrun.cndbe.domain.material.repository.MaterialRepository;
import com.nextrun.cndbe.domain.production.ProductionScenarioItemRepository;
import com.nextrun.cndbe.domain.production.dto.ProductionScenarioItemResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// b13: Drop을 CONFIRMED로 전환하는 흐름을 담당함.
// 제작안 선택(b12) -> 소재 확정(b11)이 끝났는지 확인 -> Drop 확정 -> 확정된 소재를 DEPLETED로 전환.
@Service
@RequiredArgsConstructor
public class DropConfirmationService {

    private final DropRepository dropRepository;
    private final DropMaterialSelectionRepository materialSelectionRepository;
    private final MaterialRepository materialRepository;
    private final ProductionScenarioItemRepository scenarioItemRepository;

    @Transactional
    public DropConfirmResponse confirm(UUID dropId, DropConfirmRequest request) {
        Drop drop = dropRepository.findByIdForUpdate(dropId)
                .orElseThrow(() -> new NoSuchElementException("Drop을 찾을 수 없습니다: " + dropId));

        if (drop.getStatus() != DropStatus.DRAFT) {
            throw new IllegalStateException("이미 확정되었거나 확정할 수 없는 Drop입니다.");
        }
        if (drop.getSelectedScenarioId() == null) {
            throw new IllegalStateException("제작안을 먼저 선택해야 Drop을 확정할 수 있습니다.");
        }
        DropMaterialSelection selection = materialSelectionRepository.findByDrop_Id(dropId)
                .orElseThrow(() -> new IllegalStateException("소재 조합을 먼저 확정해야 Drop을 확정할 수 있습니다."));

        drop.setName(request.name());
        drop.setExpectedProductionDays(request.expectedProductionDays());
        drop.setStatus(DropStatus.CONFIRMED);
        dropRepository.save(drop);

        depleteSelectedMaterials(selection);

        List<ProductionScenarioItemResponse> items = scenarioItemRepository
                .findAllByScenario_IdOrderByProductTypeAsc(drop.getSelectedScenarioId())
                .stream()
                // 화면 표시 순서(미니백 -> 러기지 태그)로 반환 (b12 응답 조립과 동일한 정렬 규칙)
                .sorted(Comparator.comparingInt(item ->
                        item.getProductType().name().equals("MINI_BAG") ? 0 : 1))
                .map(ProductionScenarioItemResponse::from)
                .toList();

        return DropConfirmResponse.of(drop, items);
    }

    private void depleteSelectedMaterials(DropMaterialSelection selection) {
        List<UUID> materialIds = new ArrayList<>();
        materialIds.add(selection.getMainMaterial().getId());
        if (selection.getPointMaterial() != null) {
            materialIds.add(selection.getPointMaterial().getId());
        }

        List<Material> materials = materialRepository.findAllByIdForUpdate(materialIds);
        materials.forEach(material -> material.setStatus(MaterialStatus.DEPLETED));
        materialRepository.saveAll(materials);
    }
}
