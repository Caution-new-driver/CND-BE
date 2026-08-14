package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.drop.dto.DropConfirmRequest;
import com.nextrun.cndbe.domain.drop.dto.DropConfirmResponse;
import com.nextrun.cndbe.domain.matching.DropAccessorySelection;
import com.nextrun.cndbe.domain.matching.DropAccessorySelectionRepository;
import com.nextrun.cndbe.domain.matching.DropMaterialSelection;
import com.nextrun.cndbe.domain.matching.DropMaterialSelectionRepository;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.MaterialStatus;
import com.nextrun.cndbe.domain.material.repository.MaterialRepository;
import com.nextrun.cndbe.domain.production.ProductionScenario;
import com.nextrun.cndbe.domain.production.ProductionScenarioItem;
import com.nextrun.cndbe.domain.production.ProductionScenarioItemRepository;
import com.nextrun.cndbe.domain.production.ProductionScenarioRepository;
import com.nextrun.cndbe.domain.production.dto.ProductionScenarioItemResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// b13: Drop을 CONFIRMED로 전환하는 흐름을 담당함.
// 제작안 선택(b12) -> 소재·부자재 확정(b11)이 끝났는지 확인 -> Drop 확정 -> 소재 DEPLETED 전환 -> AI 소개문 초안(b14) 생성까지 한 번에 처리.
// f6·f7이 화면상 버튼 하나("Drop 확정하기")로 묶여 있어서, 소개문 생성을 별도 단계로 쪼개지 않고 확정 흐름에 흡수함.
@Service
@RequiredArgsConstructor
public class DropConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(DropConfirmationService.class);

    private final DropRepository dropRepository;
    private final DropMaterialSelectionRepository materialSelectionRepository;
    private final DropAccessorySelectionRepository accessorySelectionRepository;
    private final MaterialRepository materialRepository;
    private final ProductionScenarioRepository scenarioRepository;
    private final ProductionScenarioItemRepository scenarioItemRepository;
    private final DropIntroTextClient introTextClient;

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
        List<DropAccessorySelection> accessorySelections =
                accessorySelectionRepository.findAllByDrop_Id(dropId);
        if (accessorySelections.isEmpty()) {
            throw new IllegalStateException("부자재 조합을 먼저 확정해야 Drop을 확정할 수 있습니다.");
        }
        ProductionScenario scenario = scenarioRepository
                .findByIdAndDrop_Id(drop.getSelectedScenarioId(), dropId)
                .orElseThrow(() -> new IllegalStateException("확정된 제작안을 찾을 수 없습니다."));

        drop.setName(request.name());
        drop.setExpectedProductionDays(request.expectedProductionDays());
        drop.setStatus(DropStatus.CONFIRMED);

        depleteSelectedMaterials(selection);

        List<ProductionScenarioItem> scenarioItems = scenarioItemRepository
                .findAllByScenario_IdOrderByProductTypeAsc(scenario.getId());

        // AI 소개문은 부가정보라, 생성이 실패해도 Drop 확정 자체(재고 반영 포함)는 그대로 성공시킨다.
        // 실패 시 introText는 비워두고, 담당자가 PATCH /intro-text로 직접 채워 넣을 수 있다.
        try {
            String introText = introTextClient.generate(
                    drop,
                    selection.getMainMaterial(),
                    selection.getPointMaterial(),
                    scenario.getScenarioType(),
                    scenarioItems
            );
            drop.setIntroText(introText);
        } catch (RuntimeException exception) {
            log.warn("Drop 확정 중 AI 소개문 생성에 실패했습니다. dropId={}", dropId, exception);
        }
        dropRepository.save(drop);

        List<ProductionScenarioItemResponse> items = scenarioItems.stream()
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
