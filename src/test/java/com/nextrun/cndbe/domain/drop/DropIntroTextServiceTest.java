package com.nextrun.cndbe.domain.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.domain.drop.dto.DropIntroTextRequest;
import com.nextrun.cndbe.domain.drop.dto.DropIntroTextResponse;
import com.nextrun.cndbe.domain.matching.DropMaterialSelection;
import com.nextrun.cndbe.domain.matching.DropMaterialSelectionRepository;
import com.nextrun.cndbe.domain.material.Material;
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

// 실제 DB/OpenAI 호출 없이 b14의 확정 상태 검증과 저장 흐름을 검증함.
@ExtendWith(MockitoExtension.class)
class DropIntroTextServiceTest {

    @Mock
    private DropRepository dropRepository;

    @Mock
    private DropMaterialSelectionRepository materialSelectionRepository;

    @Mock
    private ProductionScenarioRepository scenarioRepository;

    @Mock
    private ProductionScenarioItemRepository scenarioItemRepository;

    @Mock
    private DropIntroTextClient introTextClient;

    @InjectMocks
    private DropIntroTextService service;

    private UUID dropId;
    private UUID scenarioId;
    private Drop drop;

    @BeforeEach
    void setUp() {
        dropId = UUID.randomUUID();
        scenarioId = UUID.randomUUID();
        drop = Drop.builder()
                .id(dropId)
                .status(DropStatus.CONFIRMED)
                .name("첫 번째 RUN Drop")
                .selectedScenarioId(scenarioId)
                .build();
    }

    @Test
    void 확정된_Drop의_소개문_초안을_생성해_저장한다() {
        Material main = Material.builder().id(UUID.randomUUID()).build();
        DropMaterialSelection selection = DropMaterialSelection.builder()
                .drop(drop)
                .mainMaterial(main)
                .build();
        ProductionScenario scenario = ProductionScenario.builder()
                .id(scenarioId)
                .drop(drop)
                .scenarioType(ScenarioType.MAIN_ONLY)
                .build();
        List<ProductionScenarioItem> items = List.of(
                ProductionScenarioItem.builder()
                        .productType(ProductType.MINI_BAG)
                        .quantity(8)
                        .build()
        );

        when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));
        when(materialSelectionRepository.findByDrop_Id(dropId)).thenReturn(Optional.of(selection));
        when(scenarioRepository.findByIdAndDrop_Id(scenarioId, dropId)).thenReturn(Optional.of(scenario));
        when(scenarioItemRepository.findAllByScenario_IdOrderByProductTypeAsc(scenarioId)).thenReturn(items);
        when(introTextClient.generate(eq(drop), eq(main), isNull(), eq(ScenarioType.MAIN_ONLY), eq(items)))
                .thenReturn("업사이클링으로 태어난 미니백입니다.");

        DropIntroTextResponse response = service.generate(dropId);

        assertEquals(dropId, response.dropId());
        assertEquals("업사이클링으로 태어난 미니백입니다.", response.introText());
        assertEquals("업사이클링으로 태어난 미니백입니다.", drop.getIntroText());
    }

    @Test
    void DRAFT_상태의_Drop은_소개문을_생성할_수_없다() {
        drop.setStatus(DropStatus.DRAFT);
        when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));

        assertThrows(IllegalStateException.class, () -> service.generate(dropId));
    }

    @Test
    void 존재하지_않는_Drop이면_404_예외를_던진다() {
        when(dropRepository.findById(dropId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.generate(dropId));
    }

    @Test
    void 담당자_수정본을_그대로_저장한다() {
        when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));

        DropIntroTextResponse response = service.update(
                dropId,
                new DropIntroTextRequest("담당자가 직접 다듬은 소개문")
        );

        assertEquals("담당자가 직접 다듬은 소개문", drop.getIntroText());
        assertEquals("담당자가 직접 다듬은 소개문", response.introText());
    }

    @Test
    void DRAFT_상태의_Drop은_소개문을_수정할_수_없다() {
        drop.setStatus(DropStatus.DRAFT);
        when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));

        assertThrows(
                IllegalStateException.class,
                () -> service.update(dropId, new DropIntroTextRequest("수정본"))
        );
    }
}
