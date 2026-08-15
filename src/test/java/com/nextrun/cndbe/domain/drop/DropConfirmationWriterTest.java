package com.nextrun.cndbe.domain.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.domain.drop.dto.DropConfirmRequest;
import com.nextrun.cndbe.domain.matching.DropAccessorySelection;
import com.nextrun.cndbe.domain.matching.DropAccessorySelectionRepository;
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

// b13 확정의 DB 반영(짧은 트랜잭션)만 검증함. AI 호출은 DropConfirmationService가 트랜잭션 밖에서 처리하므로 여기선 다루지 않음.
@ExtendWith(MockitoExtension.class)
class DropConfirmationWriterTest {

    @Mock
    private DropRepository dropRepository;

    @Mock
    private DropMaterialSelectionRepository materialSelectionRepository;

    @Mock
    private DropAccessorySelectionRepository accessorySelectionRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private ProductionScenarioRepository scenarioRepository;

    @Mock
    private ProductionScenarioItemRepository scenarioItemRepository;

    @InjectMocks
    private DropConfirmationWriter writer;

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
                .template(com.nextrun.cndbe.domain.material.Template.builder()
                        .name("미니백")
                        .build())
                .build();
        scenario = ProductionScenario.builder()
                .id(scenarioId)
                .drop(drop)
                .scenarioType(ScenarioType.MAIN_ONLY)
                .build();
    }

    @Test
    void 제작안과_소재와_부자재가_확정된_Drop을_CONFIRMED로_전환하고_소재를_DEPLETED로_바꾼다() {
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
        when(accessorySelectionRepository.findAllByDrop_Id(dropId))
                .thenReturn(List.of(DropAccessorySelection.builder().drop(drop).build()));
        when(scenarioRepository.findByIdAndDrop_Id(scenarioId, dropId)).thenReturn(Optional.of(scenario));
        when(materialRepository.findAllByIdForUpdate(any())).thenReturn(List.of(main, point));
        when(scenarioItemRepository.findAllByScenario_IdOrderByProductTypeAsc(scenarioId)).thenReturn(items);

        DropConfirmationCoreResult result = writer.confirmCore(
                dropId,
                new DropConfirmRequest("첫 번째 RUN Drop", 14)
        );

        assertEquals(DropStatus.CONFIRMED, drop.getStatus());
        assertEquals("첫 번째 RUN Drop", drop.getName());
        assertEquals(14, drop.getExpectedProductionDays());
        assertEquals(MaterialStatus.DEPLETED, main.getStatus());
        assertEquals(MaterialStatus.DEPLETED, point.getStatus());
        assertEquals("CONFIRMED", result.status());
        assertEquals(1, result.items().size());

        assertEquals("첫 번째 RUN Drop", result.introTextPromptData().dropName());
        assertEquals("미니백", result.introTextPromptData().templateName());
        assertEquals(ScenarioType.MAIN_ONLY, result.introTextPromptData().scenarioType());
        assertEquals(main.getMaterialType(), result.introTextPromptData().mainMaterial().materialType());

        assertEquals(1, drop.getIntroTextGenerationCount());
        assertEquals(1, result.introTextGenerationsUsed());
    }

    @Test
    void 제작안을_선택하지_않았으면_확정할_수_없다() {
        drop.setSelectedScenarioId(null);
        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));

        assertThrows(
                IllegalStateException.class,
                () -> writer.confirmCore(dropId, new DropConfirmRequest("이름", null))
        );
    }

    @Test
    void 소재_조합이_확정되지_않았으면_확정할_수_없다() {
        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));
        when(materialSelectionRepository.findByDrop_Id(dropId)).thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> writer.confirmCore(dropId, new DropConfirmRequest("이름", null))
        );
    }

    @Test
    void 부자재_조합이_확정되지_않았으면_확정할_수_없다() {
        DropMaterialSelection selection = DropMaterialSelection.builder()
                .drop(drop)
                .mainMaterial(material(MaterialStatus.RESERVED))
                .build();

        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));
        when(materialSelectionRepository.findByDrop_Id(dropId)).thenReturn(Optional.of(selection));
        when(accessorySelectionRepository.findAllByDrop_Id(dropId)).thenReturn(List.of());

        assertThrows(
                IllegalStateException.class,
                () -> writer.confirmCore(dropId, new DropConfirmRequest("이름", null))
        );
    }

    @Test
    void 이미_확정된_Drop은_다시_확정할_수_없다() {
        drop.setStatus(DropStatus.CONFIRMED);
        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));

        assertThrows(
                IllegalStateException.class,
                () -> writer.confirmCore(dropId, new DropConfirmRequest("이름", null))
        );
    }

    @Test
    void 존재하지_않는_Drop이면_404_예외를_던진다() {
        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> writer.confirmCore(dropId, new DropConfirmRequest("이름", null))
        );
    }

    @Test
    void saveIntroText가_Drop의_소개문을_저장한다() {
        when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));

        writer.saveIntroText(dropId, "생성된 소개문");

        assertEquals("생성된 소개문", drop.getIntroText());
    }

    @Test
    void 재생성_예약이_성공하면_카운트를_1_늘리고_프롬프트데이터를_반환한다() {
        drop.setStatus(DropStatus.CONFIRMED);
        drop.setIntroTextGenerationCount(1);
        Material main = material(MaterialStatus.DEPLETED);
        DropMaterialSelection selection = DropMaterialSelection.builder()
                .drop(drop)
                .mainMaterial(main)
                .build();

        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));
        when(materialSelectionRepository.findByDrop_Id(dropId)).thenReturn(Optional.of(selection));
        when(scenarioRepository.findByIdAndDrop_Id(scenarioId, dropId)).thenReturn(Optional.of(scenario));
        when(scenarioItemRepository.findAllByScenario_IdOrderByProductTypeAsc(scenarioId))
                .thenReturn(List.of(scenarioItem(ProductType.MINI_BAG, 8)));

        DropConfirmationWriter.IntroTextRegenerationReservation reservation =
                writer.reserveRegenerationAttempt(dropId);

        assertEquals(2, drop.getIntroTextGenerationCount());
        assertEquals(2, reservation.usedCount());
        assertEquals("미니백", reservation.promptData().templateName());
    }

    @Test
    void DRAFT_상태의_Drop은_소개문을_재생성할_수_없다() {
        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));

        assertThrows(
                IllegalStateException.class,
                () -> writer.reserveRegenerationAttempt(dropId)
        );
    }

    @Test
    void 재생성_가능_횟수를_모두_썼으면_예약할_수_없다() {
        drop.setStatus(DropStatus.CONFIRMED);
        drop.setIntroTextGenerationCount(DropConfirmationWriter.MAX_INTRO_TEXT_GENERATION_COUNT);
        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));

        assertThrows(
                IllegalStateException.class,
                () -> writer.reserveRegenerationAttempt(dropId)
        );
    }

    private Material material(MaterialStatus status) {
        return Material.builder()
                .id(UUID.randomUUID())
                .materialType(com.nextrun.cndbe.domain.material.MaterialType.LEATHER)
                .color(com.nextrun.cndbe.domain.material.MaterialColor.BLACK)
                .pattern(com.nextrun.cndbe.domain.material.MaterialPattern.SOLID)
                .grade(com.nextrun.cndbe.domain.material.MaterialGrade.A)
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
