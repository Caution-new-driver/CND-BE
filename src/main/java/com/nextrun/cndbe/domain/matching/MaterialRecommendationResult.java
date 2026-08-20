package com.nextrun.cndbe.domain.matching;

import java.util.List;
import java.util.UUID;

// OpenAI가 반환한 추천 결과를 담는 상자.
// 후보별 materialId를 기준으로 추천 이유와 주의사항을 원래 소재에 연결함.
public record MaterialRecommendationResult(
        List<Recommendation> recommendations
) {

    public record Recommendation(
            UUID materialId,
            String aiReasons,
            String aiCautions
    ) {
        // 후보 하나에 대한 AI 작성 결과. 점수와 순위는 AI가 반환하지 않음.
    }
}
