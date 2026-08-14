package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.drop.dto.DropConfirmRequest;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// b13 확정의 DB 반영만 짧은 트랜잭션으로 전담함. OpenAI 호출(DropConfirmationService)은
// 이 클래스 밖, 트랜잭션이 끝난 뒤에 이뤄진다.
// b9의 MaterialCandidateWriter와 같은 이유로 별도 컴포넌트로 분리함: 같은 클래스 안에서
// @Transactional 메서드를 또 호출하면(self-invocation) 스프링 프록시를 안 거쳐서
// 트랜잭션이 실제로는 분리되지 않기 때문.
@Component
@RequiredArgsConstructor
public class DropConfirmationWriter {

    private final DropRepository dropRepository;
    private final DropMaterialSelectionRepository materialSelectionRepository;
    private final DropAccessorySelectionRepository accessorySelectionRepository;
    private final MaterialRepository materialRepository;
    private final ProductionScenarioRepository scenarioRepository;
    private final ProductionScenarioItemRepository scenarioItemRepository;

    @Transactional
    public DropConfirmationCoreResult confirmCore(UUID dropId, DropConfirmRequest request) {
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

        // 트랜잭션이 끝나면 Drop.template 같은 지연 로딩 필드를 더 이상 못 읽으므로,
        // AI 프롬프트에 필요한 값은 여기서 순수 값으로 미리 꺼내둔다.
        DropIntroTextPromptData promptData =
                buildPromptData(drop, selection, scenario, scenarioItems);

        List<ProductionScenarioItemResponse> items = scenarioItems.stream()
                // 화면 표시 순서(미니백 -> 러기지 태그)로 반환 (b12 응답 조립과 동일한 정렬 규칙)
                .sorted(Comparator.comparingInt(item ->
                        item.getProductType().name().equals("MINI_BAG") ? 0 : 1))
                .map(ProductionScenarioItemResponse::from)
                .toList();

        dropRepository.save(drop);

        return new DropConfirmationCoreResult(
                drop.getId(),
                drop.getStatus().name(),
                drop.getName(),
                drop.getExpectedProductionDays(),
                drop.getSelectedScenarioId(),
                items,
                promptData
        );
    }

    // OpenAI 호출이 끝난 뒤 introText 한 필드만 저장하는 별도의 짧은 트랜잭션.
    @Transactional
    public void saveIntroText(UUID dropId, String introText) {
        Drop drop = dropRepository.findById(dropId)
                .orElseThrow(() -> new NoSuchElementException("Drop을 찾을 수 없습니다: " + dropId));
        drop.setIntroText(introText);
        dropRepository.save(drop);
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

    private DropIntroTextPromptData buildPromptData(
            Drop drop,
            DropMaterialSelection selection,
            ProductionScenario scenario,
            List<ProductionScenarioItem> scenarioItems
    ) {
        return new DropIntroTextPromptData(
                drop.getName(),
                drop.getTemplate().getName(),
                scenario.getScenarioType(),
                toMaterialSummary(selection.getMainMaterial()),
                selection.getPointMaterial() == null
                        ? null
                        : toMaterialSummary(selection.getPointMaterial()),
                scenarioItems.stream()
                        .map(item -> new DropIntroTextPromptData.ProductSummary(
                                item.getProductType(),
                                item.getQuantity()
                        ))
                        .toList()
        );
    }

    private DropIntroTextPromptData.MaterialSummary toMaterialSummary(Material material) {
        return new DropIntroTextPromptData.MaterialSummary(
                material.getMaterialType(),
                material.getColor(),
                material.getPattern(),
                material.getGrade()
        );
    }
}
