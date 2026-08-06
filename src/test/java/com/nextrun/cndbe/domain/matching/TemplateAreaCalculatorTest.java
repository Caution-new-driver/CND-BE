package com.nextrun.cndbe.domain.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nextrun.cndbe.domain.material.Template;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

// 신규 mm 템플릿과 공유 DB의 기존 cm 템플릿이 같은 면적으로
// 계산되는지, 잘못된 데이터는 예외로 막는지 확인함.
class TemplateAreaCalculatorTest {

    private final TemplateAreaCalculator calculator =
            new TemplateAreaCalculator(JsonMapper.builder().build());

    @Test
    void mm_패턴_조각의_필요_면적을_계산한다() {
        Template template = Template.builder()
                .patternPieces(
                        """
                        [
                          {"pieceName":"앞판","widthMm":200,"heightMm":150,"quantity":1},
                          {"pieceName":"뒷판","widthMm":200,"heightMm":150,"quantity":1},
                          {"pieceName":"옆판/바닥","widthMm":400,"heightMm":60,"quantity":1}
                        ]
                        """
                )
                .build();

        double area = calculator.calculateRequiredArea(template);

        assertEquals(84_000, area);
    }

    @Test
    void 기존_cm_패턴_조각도_mm로_변환해_계산한다() {
        Template template = Template.builder()
                .patternPieces(
                        """
                        [
                          {"pieceName":"앞판","widthCm":20,"heightCm":15,"quantity":1},
                          {"pieceName":"뒷판","widthCm":20,"heightCm":15,"quantity":1},
                          {"pieceName":"옆판/바닥","widthCm":40,"heightCm":6,"quantity":1}
                        ]
                        """
                )
                .build();

        double area = calculator.calculateRequiredArea(template);

        assertEquals(84_000, area);
    }

    @Test
    void 패턴_조각_정보가_없으면_예외가_발생한다() {
        Template template = Template.builder().build();

        assertThrows(
                IllegalStateException.class,
                () -> calculator.calculateRequiredArea(template)
        );
    }
}
