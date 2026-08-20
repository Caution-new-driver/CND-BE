package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.drop.dto.DropIntroTextRequest;
import com.nextrun.cndbe.domain.drop.dto.DropIntroTextResponse;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// b14: 확정된 Drop의 소개문을 관리함.
// AI 재생성(regenerate)은 DropConfirmationWriter가 관리하는 Drop당 총 6회 제한(b13 최초 생성 포함)을
// 공유해서 소모하고, 담당자의 수동 저장(update)은 이 제한과 무관하게 언제든 가능함.
@Service
@RequiredArgsConstructor
public class DropIntroTextService {

    private final DropRepository dropRepository;
    private final DropConfirmationWriter confirmationWriter;
    private final DropIntroTextClient introTextClient;

    // 트랜잭션 없이 오케스트레이션만 함 (DropConfirmationService.confirm()과 같은 이유:
    // OpenAI 호출 동안 Drop 행 락을 잡고 있지 않기 위해).
    public DropIntroTextResponse regenerate(UUID dropId) {
        DropConfirmationWriter.IntroTextRegenerationReservation reservation =
                confirmationWriter.reserveRegenerationAttempt(dropId);
        int remaining = DropConfirmationWriter.MAX_INTRO_TEXT_GENERATION_COUNT
                - reservation.usedCount();

        // 실패하면 여기서 예외가 그대로 던져진다 — confirm()과 달리, 재생성은
        // 사용자가 명시적으로 새 결과를 기다리는 액션이라 실패를 조용히 삼키지 않는다.
        // 이미 위에서 시도 횟수는 차감됐으므로 실패한 시도도 정상적으로 6회에 포함된다.
        // 프론트가 보여주는 "남은 횟수"가 그 차감을 놓치지 않도록, 실패 응답에도 최신
        // remaining을 실어 보낸다(IntroTextGenerationFailedException).
        String introText;
        try {
            introText = introTextClient.generate(reservation.promptData());
        } catch (RuntimeException exception) {
            throw new IntroTextGenerationFailedException(exception.getMessage(), remaining);
        }
        confirmationWriter.saveIntroText(dropId, introText);

        return new DropIntroTextResponse(dropId, introText, remaining);
    }

    @Transactional
    public DropIntroTextResponse update(UUID dropId, DropIntroTextRequest request) {
        Drop drop = findConfirmedDrop(dropId);

        drop.setIntroText(request.introText());
        dropRepository.save(drop);

        return new DropIntroTextResponse(drop.getId(), drop.getIntroText(), remainingRegenerations(drop));
    }

    private int remainingRegenerations(Drop drop) {
        int used = drop.getIntroTextGenerationCount() == null ? 0 : drop.getIntroTextGenerationCount();
        return Math.max(0, DropConfirmationWriter.MAX_INTRO_TEXT_GENERATION_COUNT - used);
    }

    private Drop findConfirmedDrop(UUID dropId) {
        Drop drop = dropRepository.findById(dropId)
                .orElseThrow(() -> new NoSuchElementException("Drop을 찾을 수 없습니다: " + dropId));
        if (drop.getStatus() != DropStatus.CONFIRMED) {
            throw new IllegalStateException("Drop이 확정된 후에만 소개문을 작성할 수 있습니다.");
        }
        return drop;
    }
}
