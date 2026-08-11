package com.nextrun.cndbe.common.calculation;

import com.nextrun.cndbe.domain.material.Template;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

// Template에 TEXT로 저장된 패턴 JSON을 계산용 값 객체로 변환한다.
@Component
public class TemplatePatternParser {

    private final JsonMapper jsonMapper;

    public TemplatePatternParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public List<PatternPiece> parse(Template template) {
        if (template == null || template.getPatternPieces() == null
                || template.getPatternPieces().isBlank()) {
            throw new IllegalStateException(
                    "템플릿 패턴 조각 정보가 없습니다."
            );
        }
        try {
            RawPatternPiece[] rawPieces = jsonMapper.readValue(
                    template.getPatternPieces(),
                    RawPatternPiece[].class
            );
            if (rawPieces.length == 0) {
                throw new IllegalStateException(
                        "템플릿 패턴 조각 정보가 없습니다."
                );
            }
            return Arrays.stream(rawPieces)
                    .map(this::toPatternPiece)
                    .toList();
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "템플릿 패턴 조각 정보를 읽을 수 없습니다.",
                    exception
            );
        }
    }

    private PatternPiece toPatternPiece(RawPatternPiece raw) {
        if (raw == null) {
            throw new IllegalStateException(
                    "템플릿 패턴 조각 정보가 올바르지 않습니다."
            );
        }
        double widthMm = resolveMillimeter(raw.widthMm(), raw.widthCm());
        double heightMm = resolveMillimeter(raw.heightMm(), raw.heightCm());
        int quantity = raw.quantity() == null ? 0 : raw.quantity();
        if (raw.pieceName() == null
                || raw.pieceName().isBlank()
                || raw.role() == null
                || !Double.isFinite(widthMm)
                || !Double.isFinite(heightMm)
                || widthMm <= 0
                || heightMm <= 0
                || quantity <= 0) {
            throw new IllegalStateException(
                    "템플릿 패턴 조각의 역할, 치수 또는 수량이 올바르지 않습니다: "
                            + raw.pieceName()
            );
        }
        return new PatternPiece(
                raw.pieceName(),
                widthMm,
                heightMm,
                quantity,
                raw.role()
        );
    }

    private double resolveMillimeter(Double millimeter, Double centimeter) {
        if (millimeter != null) {
            return millimeter;
        }
        if (centimeter != null) {
            return centimeter * 10;
        }
        return 0;
    }

    private record RawPatternPiece(
            String pieceName,
            Double widthMm,
            Double heightMm,
            Double widthCm,
            Double heightCm,
            Integer quantity,
            PatternPieceRole role
    ) {
    }
}
