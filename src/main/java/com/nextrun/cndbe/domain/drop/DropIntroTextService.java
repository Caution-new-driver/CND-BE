package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.drop.dto.DropIntroTextRequest;
import com.nextrun.cndbe.domain.drop.dto.DropIntroTextResponse;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// b14: AI 초안은 b13 확정(DropConfirmationService) 흐름에서 함께 생성됨.
// 이 서비스는 확정 후 담당자가 초안을 직접 고친 최종본을 저장하는 것만 담당함.
@Service
@RequiredArgsConstructor
public class DropIntroTextService {

    private final DropRepository dropRepository;

    @Transactional
    public DropIntroTextResponse update(UUID dropId, DropIntroTextRequest request) {
        Drop drop = findConfirmedDrop(dropId);

        drop.setIntroText(request.introText());
        dropRepository.save(drop);

        return new DropIntroTextResponse(drop.getId(), drop.getIntroText());
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
