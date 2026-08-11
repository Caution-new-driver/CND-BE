package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.drop.dto.DropIntroTextRequest;
import com.nextrun.cndbe.domain.drop.dto.DropIntroTextResponse;
import com.nextrun.cndbe.domain.matching.DropMaterialSelection;
import com.nextrun.cndbe.domain.matching.DropMaterialSelectionRepository;
import com.nextrun.cndbe.domain.production.ProductionScenario;
import com.nextrun.cndbe.domain.production.ProductionScenarioItem;
import com.nextrun.cndbe.domain.production.ProductionScenarioItemRepository;
import com.nextrun.cndbe.domain.production.ProductionScenarioRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// b14: 확정된 Drop을 기준으로 AI 소개문 초안을 생성하고, 담당자 수정본을 저장하는 흐름을 담당함.
@Service
@RequiredArgsConstructor
public class DropIntroTextService {

    private final DropRepository dropRepository;
    private final DropMaterialSelectionRepository materialSelectionRepository;
    private final ProductionScenarioRepository scenarioRepository;
    private final ProductionScenarioItemRepository scenarioItemRepository;
    private final DropIntroTextClient introTextClient;

    @Transactional
    public DropIntroTextResponse generate(UUID dropId) {
        Drop drop = findConfirmedDrop(dropId);

        DropMaterialSelection selection = materialSelectionRepository.findByDrop_Id(dropId)
                .orElseThrow(() -> new IllegalStateException("확정된 소재 조합을 찾을 수 없습니다."));
        ProductionScenario scenario = scenarioRepository
                .findByIdAndDrop_Id(drop.getSelectedScenarioId(), dropId)
                .orElseThrow(() -> new IllegalStateException("확정된 제작안을 찾을 수 없습니다."));
        List<ProductionScenarioItem> items =
                scenarioItemRepository.findAllByScenario_IdOrderByProductTypeAsc(scenario.getId());

        String introText = introTextClient.generate(
                drop,
                selection.getMainMaterial(),
                selection.getPointMaterial(),
                scenario.getScenarioType(),
                items
        );

        drop.setIntroText(introText);
        dropRepository.save(drop);

        return new DropIntroTextResponse(drop.getId(), introText);
    }

    @Transactional
    public DropIntroTextResponse update(UUID dropId, DropIntroTextRequest request) {
        Drop drop = findConfirmedDrop(dropId);

        drop.setIntroText(request.introText());
        dropRepository.save(drop);

        return new DropIntroTextResponse(drop.getId(), drop.getIntroText());
    }

    private Drop findConfirmedDrop(UUID dropId) {
        Drop drop = dropRepository.findById(dropId)
                .orElseThrow(() -> new NoSuchElementException("Drop을 찾을 수 없습니다: " + dropId));
        if (drop.getStatus() != DropStatus.CONFIRMED) {
            throw new IllegalStateException("Drop이 확정된 후에만 소개문을 작성할 수 있습니다.");
        }
        return drop;
    }
}
