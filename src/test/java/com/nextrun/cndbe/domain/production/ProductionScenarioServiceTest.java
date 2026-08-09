package com.nextrun.cndbe.domain.production;

import com.nextrun.cndbe.common.calculation.RemainingRegion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.drop.DropRepository;
import com.nextrun.cndbe.domain.drop.DropStatus;
import com.nextrun.cndbe.domain.matching.DropMaterialSelection;
import com.nextrun.cndbe.domain.matching.DropMaterialSelectionRepository;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.Template;
import com.nextrun.cndbe.domain.material.TemplateRepository;
import com.nextrun.cndbe.domain.production.ProductionCalculationResult.MaterialCalculation;
import com.nextrun.cndbe.domain.production.ProductionCalculationResult.ScenarioCalculation;
import com.nextrun.cndbe.domain.production.dto.ProductionScenarioListResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ProductionScenarioServiceTest {

    @Mock
    private DropRepository dropRepository;
    @Mock
    private DropMaterialSelectionRepository materialSelectionRepository;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private ProductionScenarioRepository scenarioRepository;
    @Mock
    private ProductionScenarioItemRepository itemRepository;
    @Mock
    private ProductionMaterialResultRepository materialResultRepository;
    @Mock
    private ProductionScenarioCalculator scenarioCalculator;
    @Mock
    private ProductionScenarioInvalidator scenarioInvalidator;

    private ProductionScenarioService service;
    private UUID dropId;
    private Drop drop;

    @BeforeEach
    void setUp() {
        service = new ProductionScenarioService(
                dropRepository,
                materialSelectionRepository,
                templateRepository,
                scenarioRepository,
                itemRepository,
                materialResultRepository,
                scenarioCalculator,
                scenarioInvalidator,
                JsonMapper.builder().build()
        );
        dropId = UUID.randomUUID();
        drop = Drop.builder()
                .id(dropId)
                .status(DropStatus.DRAFT)
                .template(Template.builder().id(UUID.randomUUID()).build())
                .build();
    }

    @Test
    void 다시_계산하면_기존_선택을_초기화하고_시나리오_두_건을_저장한다() {
        Material main = Material.builder()
                .id(UUID.randomUUID())
                .materialCode("MAIN")
                .build();
        DropMaterialSelection selection = DropMaterialSelection.builder()
                .mainMaterial(main)
                .build();
        Template tagTemplate = Template.builder()
                .id(UUID.randomUUID())
                .name("러기지 태그")
                .build();
        MaterialCalculation materialCalculation = new MaterialCalculation(
                main,
                MaterialRole.MAIN,
                3,
                0,
                100_000,
                60_000,
                40_000,
                List.of(new RemainingRegion(200, 200))
        );
        ScenarioCalculation mainOnly = new ScenarioCalculation(
                ScenarioType.MAIN_ONLY,
                0,
                100_000,
                60_000,
                40_000,
                60,
                List.of(materialCalculation)
        );
        ScenarioCalculation withTags = new ScenarioCalculation(
                ScenarioType.WITH_LUGGAGE_TAG,
                2,
                100_000,
                80_000,
                20_000,
                80,
                List.of(materialCalculation)
        );
        ProductionCalculationResult calculation =
                new ProductionCalculationResult(
                        3,
                        List.of(mainOnly, withTags)
                );
        List<ProductionScenario> saved = new ArrayList<>();

        drop.setSelectedScenarioId(UUID.randomUUID());
        when(dropRepository.findByIdForUpdate(dropId))
                .thenReturn(Optional.of(drop));
        when(materialSelectionRepository.findByDrop_Id(dropId))
                .thenReturn(Optional.of(selection));
        when(templateRepository.findByName("러기지 태그"))
                .thenReturn(Optional.of(tagTemplate));
        when(scenarioCalculator.calculate(selection, drop.getTemplate(), tagTemplate))
                .thenReturn(calculation);
        doAnswer(invocation -> {
            Drop target = invocation.getArgument(0);
            target.setSelectedScenarioId(null);
            return null;
        }).when(scenarioInvalidator).invalidate(drop);
        when(scenarioRepository.findAllByDrop_IdOrderByScenarioTypeAsc(dropId))
                .thenAnswer(invocation -> List.copyOf(saved));
        when(scenarioRepository.save(any(ProductionScenario.class)))
                .thenAnswer(invocation -> {
                    ProductionScenario scenario = invocation.getArgument(0);
                    scenario.setId(UUID.randomUUID());
                    saved.add(scenario);
                    return scenario;
                });
        when(itemRepository.findAllByScenario_IdOrderByProductTypeAsc(any()))
                .thenReturn(List.of());
        when(materialResultRepository
                .findAllByScenario_IdOrderByMaterialRoleAsc(any()))
                .thenReturn(List.of());

        ProductionScenarioListResponse response = service.calculate(dropId);

        assertNull(response.selectedScenarioId());
        assertEquals(2, response.scenarios().size());
        verify(scenarioInvalidator).invalidate(drop);
        verify(scenarioRepository, times(2))
                .save(any(ProductionScenario.class));
        verify(itemRepository, times(3))
                .save(any(ProductionScenarioItem.class));
        verify(materialResultRepository, times(2))
                .save(any(ProductionMaterialResult.class));
    }

    @Test
    void 제작안을_선택하면_다른_안은_해제하고_Drop에_ID를_저장한다() {
        ProductionScenario mainOnly = scenario(ScenarioType.MAIN_ONLY);
        ProductionScenario withTags = scenario(
                ScenarioType.WITH_LUGGAGE_TAG
        );
        mainOnly.setIsSelected(true);

        when(dropRepository.findByIdForUpdate(dropId))
                .thenReturn(Optional.of(drop));
        when(scenarioRepository.findByIdAndDrop_Id(withTags.getId(), dropId))
                .thenReturn(Optional.of(withTags));
        when(scenarioRepository.findAllByDrop_IdOrderByScenarioTypeAsc(dropId))
                .thenReturn(List.of(mainOnly, withTags));
        when(itemRepository.findAllByScenario_IdOrderByProductTypeAsc(any()))
                .thenReturn(List.of());
        when(materialResultRepository
                .findAllByScenario_IdOrderByMaterialRoleAsc(any()))
                .thenReturn(List.of());

        ProductionScenarioListResponse response = service.select(
                dropId,
                withTags.getId()
        );

        assertFalse(mainOnly.getIsSelected());
        assertTrue(withTags.getIsSelected());
        assertEquals(withTags.getId(), drop.getSelectedScenarioId());
        assertEquals(withTags.getId(), response.selectedScenarioId());
    }

    @Test
    void 제작안_항목은_미니백_다음_러기지_태그_순서로_반환한다() {
        ProductionScenario withTags = scenario(
                ScenarioType.WITH_LUGGAGE_TAG
        );
        ProductionScenarioItem tagItem = ProductionScenarioItem.builder()
                .id(UUID.randomUUID())
                .scenario(withTags)
                .productType(ProductType.LUGGAGE_TAG)
                .quantity(4)
                .build();
        ProductionScenarioItem bagItem = ProductionScenarioItem.builder()
                .id(UUID.randomUUID())
                .scenario(withTags)
                .productType(ProductType.MINI_BAG)
                .quantity(2)
                .build();

        when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));
        when(scenarioRepository.findAllByDrop_IdOrderByScenarioTypeAsc(dropId))
                .thenReturn(List.of(withTags));
        when(itemRepository.findAllByScenario_IdOrderByProductTypeAsc(
                withTags.getId()
        )).thenReturn(List.of(tagItem, bagItem));
        when(materialResultRepository
                .findAllByScenario_IdOrderByMaterialRoleAsc(withTags.getId()))
                .thenReturn(List.of());

        ProductionScenarioListResponse response = service.get(dropId);

        assertEquals(
                ProductType.MINI_BAG,
                response.scenarios().getFirst().items().getFirst().productType()
        );
        assertEquals(
                ProductType.LUGGAGE_TAG,
                response.scenarios().getFirst().items().getLast().productType()
        );
    }

    @Test
    void 존재하지_않는_Drop의_제작안_조회는_404_예외를_던진다() {
        when(dropRepository.findById(dropId)).thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.get(dropId)
        );
    }

    @Test
    void 해당_Drop에_없는_제작안_선택은_404_예외를_던진다() {
        UUID scenarioId = UUID.randomUUID();
        when(dropRepository.findByIdForUpdate(dropId))
                .thenReturn(Optional.of(drop));
        when(scenarioRepository.findByIdAndDrop_Id(scenarioId, dropId))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.select(dropId, scenarioId)
        );
    }

    private ProductionScenario scenario(ScenarioType type) {
        return ProductionScenario.builder()
                .id(UUID.randomUUID())
                .drop(drop)
                .scenarioType(type)
                .materialUtilizationRate(50f)
                .totalAvailableAreaMm2(100_000d)
                .usedAreaMm2(50_000d)
                .remainingAreaMm2(50_000d)
                .isSelected(false)
                .build();
    }
}
