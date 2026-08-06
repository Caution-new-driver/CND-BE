package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.domain.material.Template;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

// b9 필터링에 필요한 "미니백 1개 패턴의 전체 면적" 계산기.
// 공유 DB의 기존 cm 데이터와 신규 Seeder의 mm 데이터를 모두 읽을 수 있게 호환 처리함.
@Component
@RequiredArgsConstructor
public class TemplateAreaCalculator {

    private final JsonMapper jsonMapper;

    public double calculateRequiredArea(Template template) {
        if (template == null
                || !StringUtils.hasText(template.getPatternPieces())) {
            throw new IllegalStateException(
                    "템플릿 패턴 조각 정보가 없습니다."
            );
        }

        try {
            // Template에는 패턴 조각이 JSON 문자열로 저장돼 있어 계산용 객체 배열로 변환.
            PatternPiece[] patternPieces = jsonMapper.readValue(
                    template.getPatternPieces(),
                    PatternPiece[].class
            );

            if (patternPieces.length == 0) {
                throw new IllegalStateException(
                        "템플릿 패턴 조각이 비어 있습니다."
                );
            }

            return Arrays.stream(patternPieces)
                    .mapToDouble(this::calculatePieceArea)
                    .sum();

        } catch (IllegalStateException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "템플릿 패턴 조각 정보를 읽을 수 없습니다.",
                    exception
            );
        }
    }

    private double calculatePieceArea(PatternPiece piece) {
        // 각 조각의 가로 × 세로 × 필요 수량을 더해서 제품 1개의 필요 면적을 구함.
        double widthMm = resolveWidthMm(piece);
        double heightMm = resolveHeightMm(piece);

        if (widthMm <= 0
                || heightMm <= 0
                || piece.quantity() == null
                || piece.quantity() <= 0) {
            throw new IllegalStateException(
                    "템플릿 패턴 조각의 치수 또는 수량이 올바르지 않습니다: "
                            + piece.pieceName()
            );
        }

        return widthMm
                * heightMm
                * piece.quantity();
    }

    private double resolveWidthMm(PatternPiece piece) {
        // 신규 데이터의 widthMm를 우선 사용하고, 기존 widthCm만 있으면 10을 곱해 변환.
        if (piece.widthMm() != null) {
            return piece.widthMm();
        }

        if (piece.widthCm() != null) {
            return piece.widthCm() * 10;
        }

        throw new IllegalStateException(
                "템플릿 패턴 조각의 너비 정보가 없습니다: "
                        + piece.pieceName()
        );
    }

    private double resolveHeightMm(PatternPiece piece) {
        // 너비와 동일하게 mm 우선, 기존 cm 데이터는 mm로 변환.
        if (piece.heightMm() != null) {
            return piece.heightMm();
        }

        if (piece.heightCm() != null) {
            return piece.heightCm() * 10;
        }

        throw new IllegalStateException(
                "템플릿 패턴 조각의 높이 정보가 없습니다: "
                        + piece.pieceName()
        );
    }

    private record PatternPiece(
            String pieceName,
            Double widthMm,
            Double heightMm,
            Double widthCm,
            Double heightCm,
            Integer quantity
    ) {
        // DB JSON의 신규 mm 필드와 기존 cm 필드를 함께 받을 수 있는 내부 전용 모양.
    }
}
