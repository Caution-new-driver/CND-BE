package com.nextrun.cndbe.common.calculation;

// Template.patternPieces JSON을 계산기가 사용하기 쉬운 형태로 바꾼 값 객체.
public record PatternPiece(
        String pieceName,
        double widthMm,
        double heightMm,
        int quantity,
        PatternPieceRole role
) {
    public double areaMm2() {
        return widthMm * heightMm * quantity;
    }
}
