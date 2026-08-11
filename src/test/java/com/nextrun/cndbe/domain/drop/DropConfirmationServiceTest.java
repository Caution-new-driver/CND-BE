package com.nextrun.cndbe.domain.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.domain.drop.dto.DropConfirmRequest;
import com.nextrun.cndbe.domain.drop.dto.DropConfirmResponse;
import com.nextrun.cndbe.domain.matching.DropMaterialSelection;
import com.nextrun.cndbe.domain.matching.DropMaterialSelectionRepository;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.MaterialStatus;
import com.nextrun.cndbe.domain.material.repository.MaterialRepository;
import com.nextrun.cndbe.domain.production.ProductType;
import com.nextrun.cndbe.domain.production.ProductionScenarioItem;
import com.nextrun.cndbe.domain.production.ProductionScenarioItemRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 실제 DB 없이 b13의 확정 조건 검증과 소재 DEPLETED 전환 규칙을 검증함.
@ExtendWith(MockitoExtension.class)
class DropConfirmationServiceTest {

    @Mock
    private DropRepository dropRepository;

    @Mock
    private DropMaterialSelectionRepository materialSelectionRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private ProductionScenarioItemRepository scenarioItemRepository;

    @InjectMocks
    private DropConfirmationService service;

    private UUID dropId;
    private UUID scenarioId;
    private Drop drop;

    @BeforeEach
    void setUp() {
        dropId = UUID.randomUUID();
        scenarioId = UUID.randomUUID();
        drop = Drop.builder()
                .id(dropId)
                .status(DropStatus.DRAFT)
                .selectedScenarioId(scenarioId)
                .build();
    }

    @Test
    void 제작안과_소재가_확정된_Drop을_CONFIRMED로_전환하고_소재를_DEPLETED로_바꾼다() {
        Material main = material(MaterialStatus.RESERVED);
        Material point = material(MaterialStatus.RESERVED);
        DropMaterialSelection selection = DropMaterialSelection.builder()
                .drop(drop)
                .mainMaterial(main)
                .pointMaterial(point)
                .build();

        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));
        when(materialSelectionRepository.findByDrop_Id(dropId)).thenReturn(Optional.of(selection));
        when(materialRepository.findAllByIdForUpdate(any())).thenReturn(List.of(main, point));
        when(scenarioItemRepository.findAllByScenario_IdOrderByProductTypeAsc(scenarioId))
                .thenReturn(List.of(scenarioItem(ProductType.MINI_BAG, 8)));

        DropConfirmResponse response = service.confirm(
                dropId,
                new DropConfirmRequest("첫 번째 RUN Drop", 14)
        );

        assertEquals(DropStatus.CONFIRMED, drop.getStatus());
        assertEquals("첫 번째 RUN Drop", drop.getName());
        assertEquals(14, drop.getExpectedProductionDays());
        assertEquals(MaterialStatus.DEPLETED, main.getStatus());
        assertEquals(MaterialStatus.DEPLETED, point.getStatus());
        assertEquals("CONFIRMED", response.getStatus());
        assertEquals(1, response.getItems().size());
    }

    @Test
    void 제작안을_선택하지_않았으면_확정할_수_없다() {
        drop.setSelectedScenarioId(null);
        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));

        assertThrows(
                IllegalStateException.class,
                () -> service.confirm(dropId, new DropConfirmRequest("이름", null))
        );
    }

    @Test
    void 소재_조합이_확정되지_않았으면_확정할_수_없다() {
        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));
        when(materialSelectionRepository.findByDrop_Id(dropId)).thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.confirm(dropId, new DropConfirmRequest("이름", null))
        );
    }

    @Test
    void 이미_확정된_Drop은_다시_확정할_수_없다() {
        drop.setStatus(DropStatus.CONFIRMED);
        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));

        assertThrows(
                IllegalStateException.class,
                () -> service.confirm(dropId, new DropConfirmRequest("이름", null))
        );
    }

    @Test
    void 존재하지_않는_Drop이면_404_예외를_던진다() {
        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.confirm(dropId, new DropConfirmRequest("이름", null))
        );
    }

    private Material material(MaterialStatus status) {
        return Material.builder()
                .id(UUID.randomUUID())
                .status(status)
                .build();
    }

    private ProductionScenarioItem scenarioItem(ProductType type, int quantity) {
        return ProductionScenarioItem.builder()
                .productType(type)
                .quantity(quantity)
                .numberingStart(1)
                .numberingEnd(quantity)
                .build();
    }
}
