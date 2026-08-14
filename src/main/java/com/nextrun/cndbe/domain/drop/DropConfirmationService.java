package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.drop.dto.DropConfirmRequest;
import com.nextrun.cndbe.domain.drop.dto.DropConfirmResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// b13: Drop을 CONFIRMED로 전환하는 흐름을 담당함.
// DB 반영(검증·상태전환·소재 DEPLETED)은 DropConfirmationWriter가 짧은 트랜잭션으로 전담하고,
// 이 클래스는 그 사이에서 트랜잭션 없이 OpenAI 호출(b14 소개문 초안)만 이어붙이는 오케스트레이터다.
// DB 락을 잡은 채로 외부 API를 호출하지 않기 위해 일부러 트랜잭션을 얹지 않았다.
@Service
@RequiredArgsConstructor
public class DropConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(DropConfirmationService.class);

    private final DropConfirmationWriter confirmationWriter;
    private final DropIntroTextClient introTextClient;

    public DropConfirmResponse confirm(UUID dropId, DropConfirmRequest request) {
        // 1. 짧은 트랜잭션: 검증 + 상태전환 + 소재 DEPLETED 전환 + AI 프롬프트용 값 추출.
        DropConfirmationCoreResult core = confirmationWriter.confirmCore(dropId, request);

        // 2. 트랜잭션 밖: OpenAI 호출. 실패해도 확정 자체는 이미 끝났으니 introText만 비워둔다.
        String introText = null;
        try {
            introText = introTextClient.generate(core.introTextPromptData());
        } catch (RuntimeException exception) {
            log.warn("Drop 확정 중 AI 소개문 생성에 실패했습니다. dropId={}", dropId, exception);
        }

        // 3. 짧은 트랜잭션: 생성에 성공했을 때만 introText 한 필드를 저장.
        if (introText != null) {
            confirmationWriter.saveIntroText(dropId, introText);
        }

        return DropConfirmResponse.of(core, introText);
    }
}
