package com.nextrun.cndbe.common.calculation;

// 패턴을 배치하고 남은, 다른 영역과 겹치지 않는 직사각형 한 조각.
public record RemainingRegion(double widthMm, double heightMm) {
    public double areaMm2() {
        return widthMm * heightMm;
    }
}
