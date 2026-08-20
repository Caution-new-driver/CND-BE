package com.nextrun.cndbe.domain.production;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.domain.drop.Drop;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 소재 조합이 바뀌었을 때 이전 소재로 계산한 b12 결과가 남지 않는지 검증한다.
@ExtendWith(MockitoExtension.class)
class ProductionScenarioInvalidatorTest {

    @Mock
    private ProductionScenarioRepository scenarioRepository;
    @Mock
    private ProductionScenarioItemRepository itemRepository;
    @Mock
    private ProductionMaterialResultRepository materialResultRepository;

    private ProductionScenarioInvalidator invalidator;

    @BeforeEach
    void setUp() {
        invalidator = new ProductionScenarioInvalidator(
                scenarioRepository,
                itemRepository,
                materialResultRepository
        );
    }

    @Test
    void 기존_시나리오와_하위_결과를_삭제하고_선택을_초기화한다() {
        Drop drop = Drop.builder()
                .id(UUID.randomUUID())
                .selectedScenarioId(UUID.randomUUID())
                .build();
        ProductionScenario first = scenario(drop);
        ProductionScenario second = scenario(drop);
        ProductionScenarioItem item = ProductionScenarioItem.builder()
                .id(UUID.randomUUID())
                .scenario(first)
                .build();
        ProductionMaterialResult materialResult =
                ProductionMaterialResult.builder()
                        .id(UUID.randomUUID())
                        .scenario(first)
                        .build();

        when(scenarioRepository.findAllByDrop_IdOrderByScenarioTypeAsc(
                drop.getId()
        )).thenReturn(List.of(first, second));
        when(itemRepository.findAllByScenario_IdOrderByProductTypeAsc(
                first.getId()
        )).thenReturn(List.of(item));
        when(itemRepository.findAllByScenario_IdOrderByProductTypeAsc(
                second.getId()
        )).thenReturn(List.of());
        when(materialResultRepository
                .findAllByScenario_IdOrderByMaterialRoleAsc(first.getId()))
                .thenReturn(List.of(materialResult));
        when(materialResultRepository
                .findAllByScenario_IdOrderByMaterialRoleAsc(second.getId()))
                .thenReturn(List.of());

        invalidator.invalidate(drop);

        verify(materialResultRepository).deleteAll(List.of(materialResult));
        verify(itemRepository).deleteAll(List.of(item));
        verify(scenarioRepository).deleteAll(List.of(first, second));
        assertNull(drop.getSelectedScenarioId());
    }

    @Test
    void 저장된_시나리오가_없어도_Drop의_선택_ID를_초기화한다() {
        Drop drop = Drop.builder()
                .id(UUID.randomUUID())
                .selectedScenarioId(UUID.randomUUID())
                .build();
        when(scenarioRepository.findAllByDrop_IdOrderByScenarioTypeAsc(
                drop.getId()
        )).thenReturn(List.of());

        invalidator.invalidate(drop);

        verify(scenarioRepository).deleteAll(List.of());
        assertNull(drop.getSelectedScenarioId());
    }

    private ProductionScenario scenario(Drop drop) {
        return ProductionScenario.builder()
                .id(UUID.randomUUID())
                .drop(drop)
                .build();
    }
}
