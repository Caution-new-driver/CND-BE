package com.nextrun.cndbe.domain.production;

import com.nextrun.cndbe.domain.drop.Drop;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 상위 단계에서 소재가 바뀌면 이전 소재 기준 B12 결과를 한 번에 폐기한다.
@Component
@RequiredArgsConstructor
public class ProductionScenarioInvalidator {

    private final ProductionScenarioRepository scenarioRepository;
    private final ProductionScenarioItemRepository itemRepository;
    private final ProductionMaterialResultRepository materialResultRepository;

    @Transactional
    public void invalidate(Drop drop) {
        List<ProductionScenario> existing = scenarioRepository
                .findAllByDrop_IdOrderByScenarioTypeAsc(drop.getId());

        for (ProductionScenario scenario : existing) {
            materialResultRepository.deleteAll(
                    materialResultRepository
                            .findAllByScenario_IdOrderByMaterialRoleAsc(
                                    scenario.getId()
                            )
            );
            itemRepository.deleteAll(
                    itemRepository
                            .findAllByScenario_IdOrderByProductTypeAsc(
                                    scenario.getId()
                            )
            );
        }
        scenarioRepository.deleteAll(existing);
        drop.setSelectedScenarioId(null);
    }
}
