package com.nextrun.cndbe.domain.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.nextrun.cndbe.domain.production.ProductionScenario;
import com.nextrun.cndbe.domain.production.ProductionScenarioItem;
import com.nextrun.cndbe.domain.production.ProductionScenarioItemRepository;
import com.nextrun.cndbe.domain.production.ProductionScenarioRepository;
import com.nextrun.cndbe.domain.production.ScenarioType;
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

// 실제 DB/OpenAI 호출 없이 b13의 확정 조건 검증, 소재 DEPLETED 전환, AI 소개문 흡수 흐름을 검증함.
@ExtendWith(MockitoExtension.class)
class DropConfirmationServiceTest {

    @Mock
    private DropRepository dropRepository;

    @Mock
    private DropMaterialSelectionRepository materialSelectionRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private ProductionScenarioRepository scenarioRepository;

    @Mock
    private ProductionScenarioItemRepository scenarioItemRepository;

    @Mock
    private DropIntroTextClient introTextClient;

    @InjectMocks
    private DropConfirmationService service;

    private UUID dropId;
    private UUID scenarioId;
    private Drop drop;
    private ProductionScenario scenario;

    @BeforeEach
    void setUp() {
        dropId = UUID.randomUUID();
        scenarioId = UUID.randomUUID();
        drop = Drop.builder()
                .id(dropId)
                .status(DropStatus.DRAFT)
                .selectedScenarioId(scenarioId)
                .build();
        scenario = ProductionScenario.builder()
                .id(scenarioId)
                .drop(drop)
                .scenarioType(ScenarioType.MAIN_ONLY)
                .build();
    }

    @Test
    void 제작안과_소재가_확정된_Drop을_CONFIRMED로_전환하고_소재를_DEPLETED로_바꾸고_소개문을_생성한다() {
        Material main = material(MaterialStatus.RESERVED);
        Material point = material(MaterialStatus.RESERVED);
        DropMaterialSelection selection = DropMaterialSelection.builder()
                .drop(drop)
                .mainMaterial(main)
                .pointMaterial(point)
                .build();
        List<ProductionScenarioItem> items = List.of(scenarioItem(ProductType.MINI_BAG, 8));

        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));
        when(materialSelectionRepository.findByDrop_Id(dropId)).thenReturn(Optional.of(selection));
        when(scenarioRepository.findByIdAndDrop_Id(scenarioId, dropId)).thenReturn(Optional.of(scenario));
        when(materialRepository.findAllByIdForUpdate(any())).thenReturn(List.of(main, point));
        when(scenarioItemRepository.findAllByScenario_IdOrderByProductTypeAsc(scenarioId)).thenReturn(items);
        when(introTextClient.generate(any(), any(), any(), any(), any()))
                .thenReturn("업사이클링으로 태어난 미니백입니다.");

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
        assertEquals("업사이클링으로 태어난 미니백입니다.", response.getIntroText());
    }

    @Test
    void AI_소개문_생성이_실패해도_Drop_확정_자체는_성공한다() {
        Material main = material(MaterialStatus.RESERVED);
        DropMaterialSelection selection = DropMaterialSelection.builder()
                .drop(drop)
                .mainMaterial(main)
                .build();

        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));
        when(materialSelectionRepository.findByDrop_Id(dropId)).thenReturn(Optional.of(selection));
        when(scenarioRepository.findByIdAndDrop_Id(scenarioId, dropId)).thenReturn(Optional.of(scenario));
        when(materialRepository.findAllByIdForUpdate(any())).thenReturn(List.of(main));
        when(scenarioItemRepository.findAllByScenario_IdOrderByProductTypeAsc(scenarioId))
                .thenReturn(List.of(scenarioItem(ProductType.MINI_BAG, 8)));
        when(introTextClient.generate(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("AI 소개문 생성에 실패했습니다."));

        DropConfirmResponse response = service.confirm(
                dropId,
                new DropConfirmRequest("첫 번째 RUN Drop", null)
        );

        assertEquals(DropStatus.CONFIRMED, drop.getStatus());
        assertEquals(MaterialStatus.DEPLETED, main.getStatus());
        assertNull(response.getIntroText());
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
