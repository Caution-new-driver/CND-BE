package com.nextrun.cndbe.common.calculation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nextrun.cndbe.domain.material.Template;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

// 템플릿 JSON을 한 번 파싱한 결과로 면적 계산과 배치 계산을 함께 수행할 수 있는지 검증한다.
class TemplatePatternParserTest {

    private final TemplatePatternParser parser =
            new TemplatePatternParser(JsonMapper.builder().build());

    @Test
    void mm_패턴을_파싱하면_미니백_필요_면적이_계산된다() {
        List<PatternPiece> pieces = parser.parse(template(
                """
                [
                  {"pieceName":"앞판","widthMm":200,"heightMm":150,"quantity":1},
                  {"pieceName":"뒷판","widthMm":200,"heightMm":150,"quantity":1},
                  {"pieceName":"옆판/바닥","widthMm":400,"heightMm":60,"quantity":1}
                ]
                """
        ));

        assertEquals(
                84_000,
                pieces.stream().mapToDouble(PatternPiece::areaMm2).sum()
        );
    }

    @Test
    void 기존_cm_패턴도_mm로_변환한다() {
        List<PatternPiece> pieces = parser.parse(template(
                """
                [
                  {"pieceName":"앞판","widthCm":20,"heightCm":15,"quantity":1}
                ]
                """
        ));

        assertEquals(200, pieces.getFirst().widthMm());
        assertEquals(150, pieces.getFirst().heightMm());
    }

    @Test
    void 치수나_수량이_올바르지_않으면_예외가_발생한다() {
        Template invalid = template(
                """
                [
                  {"pieceName":"앞판","widthMm":0,"heightMm":150,"quantity":0}
                ]
                """
        );

        assertThrows(
                IllegalStateException.class,
                () -> parser.parse(invalid)
        );
    }

    private Template template(String patternPieces) {
        return Template.builder()
                .patternPieces(patternPieces)
                .build();
    }
}
