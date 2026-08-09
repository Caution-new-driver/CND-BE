package com.nextrun.cndbe.common.calculation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatternPlacementCalculatorTest {

    private PatternPlacementCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PatternPlacementCalculator();
    }

    @Test
    void 패턴을_90도_회전해서_배치할_수_있다() {
        int capacity = calculator.calculateCapacityPerSheet(
                150,
                100,
                List.of(new PatternPiece("회전 패턴", 100, 150, 1))
        );

        assertEquals(1, capacity);
    }

    @Test
    void 여러_소재_장은_붙이지_않고_각각_배치한다() {
        SheetPlacementResult result = calculator.placeAcrossSheets(
                100,
                100,
                2,
                List.of(new PatternPiece("패턴", 100, 60, 1)),
                2
        );

        assertEquals(2, result.supportedProductQuantity());
        assertEquals(
                8_000,
                result.remainingRegions().stream()
                        .mapToDouble(RemainingRegion::areaMm2)
                        .sum(),
                0.001
        );
    }

    @Test
    void 남은_직사각형에_러기지_태그를_채운다() {
        PatternPlacementCalculator.TagPlacementResult result =
                calculator.fillWithTags(
                        List.of(new RemainingRegion(120, 100)),
                        new PatternPiece("태그", 100, 60, 1)
                );

        assertEquals(2, result.quantity());
        assertEquals(
                0,
                result.remainingRegions().stream()
                        .mapToDouble(RemainingRegion::areaMm2)
                        .sum(),
                0.001
        );
    }

    @Test
    void 실제_미니백_패턴을_400x300_소재에_한_세트_배치한다() {
        int capacity = calculator.calculateCapacityPerSheet(
                400,
                300,
                List.of(
                        new PatternPiece("앞판", 200, 150, 1),
                        new PatternPiece("뒷판", 200, 150, 1),
                        new PatternPiece("옆판/바닥", 400, 60, 1)
                )
        );

        assertEquals(1, capacity);
    }
}
