package com.nextrun.cndbe.domain.material;

// AI가 사진 분석하고 돌려주는 결과를 담는 상자.
// Material 엔티티의 "AI가 채우는 값" 5개랑 정확히 같은 모양.
public record MaterialAiTagResult(
        MaterialColor color,
        MaterialPattern pattern,
        String texture,
        Float aiConfidence,
        String surfaceNotes
) {
}