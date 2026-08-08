package com.nextrun.cndbe.domain.production;

import com.nextrun.cndbe.common.calculation.PatternPlacementCalculator;
import com.nextrun.cndbe.common.calculation.TemplatePatternParser;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nextrun.cndbe.domain.matching.DropMaterialSelection;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.Template;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ProductionScenarioCalculatorTest {

    private ProductionScenarioCalculator calculator;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        calculator = new ProductionScenarioCalculator(
                new TemplatePatternParser(jsonMapper),
                new PatternPlacementCalculator()
        );
    }

    @Test
    void 포인트_소재가_없으면_모든_미니백_패턴을_주_소재에_배치한다() {
        Material main = material("MAIN", 200, 150, 2);
        DropMaterialSelection selection = DropMaterialSelection.builder()
                .mainMaterial(main)
                .build();

        ProductionCalculationResult result = calculator.calculate(
                selection,
                miniBagTemplate(),
                luggageTagTemplate()
        );

        assertEquals(2, result.miniBagQuantity());
        assertEquals(2, result.scenarios().size());
        assertEquals(
                ScenarioType.MAIN_ONLY,
                result.scenarios().getFirst().scenarioType()
        );
        assertEquals(
                ScenarioType.WITH_LUGGAGE_TAG,
                result.scenarios().getLast().scenarioType()
        );
    }

    @Test
    void 포인트_소재는_옆판_바닥을_담당하고_남은_영역도_태그에_사용한다() {
        Material main = material("MAIN", 200, 100, 1);
        Material point = material("POINT", 100, 100, 1);
        DropMaterialSelection selection = DropMaterialSelection.builder()
                .mainMaterial(main)
                .pointMaterial(point)
                .build();

        ProductionCalculationResult result = calculator.calculate(
                selection,
                miniBagTemplate(),
                luggageTagTemplate()
        );
        ProductionCalculationResult.ScenarioCalculation withTags =
                result.scenarios().getLast();

        assertEquals(1, result.miniBagQuantity());
        assertEquals(2, withTags.luggageTagQuantity());
        assertEquals(100.0f, withTags.utilizationRate(), 0.001);
        assertEquals(2, withTags.materials().size());
        assertEquals(
                2,
                withTags.materials().stream()
                        .filter(material -> material.role() == MaterialRole.POINT)
                        .findFirst()
                        .orElseThrow()
                        .luggageTagQuantity()
        );
    }

    private Material material(
            String code,
            float width,
            float height,
            int quantity
    ) {
        return Material.builder()
                .id(UUID.randomUUID())
                .materialCode(code)
                .widthMm(width)
                .heightMm(height)
                .quantity(quantity)
                .build();
    }

    private Template miniBagTemplate() {
        return Template.builder()
                .id(UUID.randomUUID())
                .name("미니백")
                .patternPieces("""
                        [
                          {"pieceName":"앞판","widthMm":100,"heightMm":100,"quantity":1},
                          {"pieceName":"뒷판","widthMm":100,"heightMm":100,"quantity":1},
                          {"pieceName":"옆판/바닥","widthMm":100,"heightMm":50,"quantity":1}
                        ]
                        """)
                .build();
    }

    private Template luggageTagTemplate() {
        return Template.builder()
                .id(UUID.randomUUID())
                .name("러기지 태그")
                .patternPieces("""
                        [
                          {"pieceName":"태그 몸체","widthMm":50,"heightMm":50,"quantity":1}
                        ]
                        """)
                .build();
    }
}
