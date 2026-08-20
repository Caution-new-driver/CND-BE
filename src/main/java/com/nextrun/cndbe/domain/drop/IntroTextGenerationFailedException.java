package com.nextrun.cndbe.domain.drop;

import lombok.Getter;

// b14 재생성 시도가 실패했을 때 던진다. reserveRegenerationAttempt에서 시도 횟수는 이미
// 차감된 뒤라, 프론트가 화면에 표시 중인 "남은 횟수"가 뒤늦게 어긋나지 않도록 최신 값을
// 응답에 함께 실어 보낸다(GlobalExceptionHandler가 처리).
@Getter
public class IntroTextGenerationFailedException extends RuntimeException {

    private final int regenerationsRemaining;

    public IntroTextGenerationFailedException(String message, int regenerationsRemaining) {
        super(message);
        this.regenerationsRemaining = regenerationsRemaining;
    }
}
