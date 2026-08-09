package com.nextrun.cndbe.domain.production;

import com.nextrun.cndbe.common.calculation.RemainingRegion;
import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.drop.DropRepository;
import com.nextrun.cndbe.domain.drop.DropStatus;
import com.nextrun.cndbe.domain.matching.DropMaterialSelection;
import com.nextrun.cndbe.domain.matching.DropMaterialSelectionRepository;
import com.nextrun.cndbe.domain.material.Template;
import com.nextrun.cndbe.domain.material.TemplateRepository;
import com.nextrun.cndbe.domain.production.ProductionCalculationResult.MaterialCalculation;
import com.nextrun.cndbe.domain.production.ProductionCalculationResult.ScenarioCalculation;
import com.nextrun.cndbe.domain.production.dto.ProductionMaterialResultResponse;
import com.nextrun.cndbe.domain.production.dto.ProductionScenarioItemResponse;
import com.nextrun.cndbe.domain.production.dto.ProductionScenarioListResponse;
import com.nextrun.cndbe.domain.production.dto.ProductionScenarioResponse;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

// b12 제작 가능성 계산의 흐름을 담당한다: 입력 조회 -> 계산 -> 결과 교체 저장 -> 조회/선택.
@Service
@RequiredArgsConstructor
public class ProductionScenarioService {

    private static final String LUGGAGE_TAG_TEMPLATE_NAME = "러기지 태그";

    private final DropRepository dropRepository;
    private final DropMaterialSelectionRepository materialSelectionRepository;
    private final TemplateRepository templateRepository;
    private final ProductionScenarioRepository scenarioRepository;
    private final ProductionScenarioItemRepository itemRepository;
    private final ProductionMaterialResultRepository materialResultRepository;
    private final ProductionScenarioCalculator scenarioCalculator;
    private final ProductionScenarioInvalidator scenarioInvalidator;
    private final JsonMapper jsonMapper;

    @Transactional
    public ProductionScenarioListResponse calculate(UUID dropId) {
        Drop drop = findEditableDrop(dropId);
        DropMaterialSelection selection = materialSelectionRepository
                .findByDrop_Id(dropId)
                .orElseThrow(() -> new IllegalStateException(
                        "제작 가능성을 계산하려면 소재 조합을 먼저 선택해야 합니다."
                ));
        Template luggageTagTemplate = templateRepository
                .findByName(LUGGAGE_TAG_TEMPLATE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "러기지 태그 템플릿을 찾을 수 없습니다."
                ));

        ProductionCalculationResult calculation = scenarioCalculator.calculate(
                selection,
                drop.getTemplate(),
                luggageTagTemplate
        );

        // 디자인·소재를 수정한 뒤 재호출할 수 있으므로 이전 결과와 선택을 모두 교체한다.
        scenarioInvalidator.invalidate(drop);
        for (ScenarioCalculation scenarioCalculation
                : calculation.scenarios()) {
            saveScenario(
                    drop,
                    calculation.miniBagQuantity(),
                    scenarioCalculation
            );
        }

        return buildResponse(drop);
    }

    @Transactional(readOnly = true)
    public ProductionScenarioListResponse get(UUID dropId) {
        Drop drop = dropRepository.findById(dropId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Drop을 찾을 수 없습니다: " + dropId
                ));
        if (scenarioRepository
                .findAllByDrop_IdOrderByScenarioTypeAsc(dropId)
                .isEmpty()) {
            throw new IllegalStateException(
                    "아직 계산된 제작 시나리오가 없습니다."
            );
        }
        return buildResponse(drop);
    }

    @Transactional
    public ProductionScenarioListResponse select(
            UUID dropId,
            UUID scenarioId
    ) {
        Drop drop = findEditableDrop(dropId);
        ProductionScenario selected = scenarioRepository
                .findByIdAndDrop_Id(scenarioId, dropId)
                .orElseThrow(() -> new NoSuchElementException(
                        "해당 Drop의 제작 시나리오를 찾을 수 없습니다: "
                                + scenarioId
                ));

        List<ProductionScenario> scenarios = scenarioRepository
                .findAllByDrop_IdOrderByScenarioTypeAsc(dropId);
        scenarios.forEach(scenario -> scenario.setIsSelected(false));
        selected.setIsSelected(true);
        drop.setSelectedScenarioId(selected.getId());

        scenarioRepository.saveAll(scenarios);
        return buildResponse(drop);
    }

    private void saveScenario(
            Drop drop,
            int miniBagQuantity,
            ScenarioCalculation calculation
    ) {
        ProductionScenario scenario = scenarioRepository.save(
                ProductionScenario.builder()
                        .drop(drop)
                        .scenarioType(calculation.scenarioType())
                        .materialUtilizationRate(calculation.utilizationRate())
                        .totalAvailableAreaMm2(
                                calculation.totalAvailableAreaMm2()
                        )
                        .usedAreaMm2(calculation.usedAreaMm2())
                        .remainingAreaMm2(calculation.remainingAreaMm2())
                        .isSelected(false)
                        .build()
        );

        itemRepository.save(item(
                scenario,
                ProductType.MINI_BAG,
                miniBagQuantity
        ));
        if (calculation.scenarioType()
                == ScenarioType.WITH_LUGGAGE_TAG) {
            itemRepository.save(item(
                    scenario,
                    ProductType.LUGGAGE_TAG,
                    calculation.luggageTagQuantity()
            ));
        }

        for (MaterialCalculation material : calculation.materials()) {
            materialResultRepository.save(
                    ProductionMaterialResult.builder()
                            .scenario(scenario)
                            .material(material.material())
                            .materialRole(material.role())
                            .supportedMiniBagQuantity(
                                    material.supportedMiniBagQuantity()
                            )
                            .luggageTagQuantity(
                                    material.luggageTagQuantity()
                            )
                            .availableAreaMm2(material.availableAreaMm2())
                            .usedAreaMm2(material.usedAreaMm2())
                            .remainingAreaMm2(material.remainingAreaMm2())
                            .remainingRegions(writeRegions(
                                    material.remainingRegions()
                            ))
                            .build()
            );
        }
    }

    private ProductionScenarioItem item(
            ProductionScenario scenario,
            ProductType productType,
            int quantity
    ) {
        return ProductionScenarioItem.builder()
                .scenario(scenario)
                .productType(productType)
                .quantity(quantity)
                .numberingStart(quantity > 0 ? 1 : null)
                .numberingEnd(quantity > 0 ? quantity : null)
                .build();
    }

    private ProductionScenarioListResponse buildResponse(Drop drop) {
        List<ProductionScenarioResponse> scenarios = scenarioRepository
                .findAllByDrop_IdOrderByScenarioTypeAsc(drop.getId())
                .stream()
                .map(this::scenarioResponse)
                .toList();
        return new ProductionScenarioListResponse(
                drop.getId(),
                drop.getSelectedScenarioId(),
                scenarios
        );
    }

    private ProductionScenarioResponse scenarioResponse(
            ProductionScenario scenario
    ) {
        List<ProductionScenarioItemResponse> items = itemRepository
                .findAllByScenario_IdOrderByProductTypeAsc(scenario.getId())
                .stream()
                // enum 이름의 알파벳순이 아니라 화면 표시 순서(미니백 -> 러기지 태그)로 반환한다.
                .sorted(Comparator.comparingInt(item ->
                        item.getProductType() == ProductType.MINI_BAG ? 0 : 1
                ))
                .map(ProductionScenarioItemResponse::from)
                .toList();
        List<ProductionMaterialResultResponse> materials =
                materialResultRepository
                        .findAllByScenario_IdOrderByMaterialRoleAsc(
                                scenario.getId()
                        )
                        .stream()
                        .map(result -> ProductionMaterialResultResponse.from(
                                result,
                                readRegions(result.getRemainingRegions())
                        ))
                        .toList();

        return new ProductionScenarioResponse(
                scenario.getId(),
                scenario.getScenarioType(),
                scenario.getMaterialUtilizationRate(),
                scenario.getTotalAvailableAreaMm2(),
                scenario.getUsedAreaMm2(),
                scenario.getRemainingAreaMm2(),
                Boolean.TRUE.equals(scenario.getIsSelected()),
                items,
                materials
        );
    }

    private String writeRegions(List<RemainingRegion> regions) {
        try {
            return jsonMapper.writeValueAsString(regions);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "남은 소재 영역을 저장할 수 없습니다.",
                    exception
            );
        }
    }

    private List<RemainingRegion> readRegions(String regionsJson) {
        try {
            RemainingRegion[] regions = jsonMapper.readValue(
                    regionsJson,
                    RemainingRegion[].class
            );
            return List.copyOf(Arrays.asList(regions));
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "남은 소재 영역을 읽을 수 없습니다.",
                    exception
            );
        }
    }

    private Drop findEditableDrop(UUID dropId) {
        Drop drop = dropRepository.findByIdForUpdate(dropId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Drop을 찾을 수 없습니다: " + dropId
                ));
        if (drop.getStatus() != DropStatus.DRAFT) {
            throw new IllegalStateException(
                    "확정된 Drop의 제작 시나리오는 변경할 수 없습니다."
            );
        }
        if (drop.getTemplate() == null) {
            throw new IllegalStateException(
                    "Drop에 연결된 제품 템플릿이 없습니다."
            );
        }
        return drop;
    }
}
