package com.nextrun.cndbe.domain.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.domain.drop.dto.DropIntroTextRequest;
import com.nextrun.cndbe.domain.drop.dto.DropIntroTextResponse;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// b14 중 담당자 수정본 저장(update)만 검증함. AI 초안 생성은 b13 확정 흐름(DropConfirmationService)에 흡수되어 그쪽에서 검증함.
@ExtendWith(MockitoExtension.class)
class DropIntroTextServiceTest {

    @Mock
    private DropRepository dropRepository;

    @InjectMocks
    private DropIntroTextService service;

    private UUID dropId;
    private Drop drop;

    @BeforeEach
    void setUp() {
        dropId = UUID.randomUUID();
        drop = Drop.builder()
                .id(dropId)
                .status(DropStatus.CONFIRMED)
                .name("첫 번째 RUN Drop")
                .build();
    }

    @Test
    void 담당자_수정본을_그대로_저장한다() {
        when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));

        DropIntroTextResponse response = service.update(
                dropId,
                new DropIntroTextRequest("담당자가 직접 다듬은 소개문")
        );

        assertEquals("담당자가 직접 다듬은 소개문", drop.getIntroText());
        assertEquals("담당자가 직접 다듬은 소개문", response.introText());
    }

    @Test
    void DRAFT_상태의_Drop은_소개문을_수정할_수_없다() {
        drop.setStatus(DropStatus.DRAFT);
        when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));

        assertThrows(
                IllegalStateException.class,
                () -> service.update(dropId, new DropIntroTextRequest("수정본"))
        );
    }

    @Test
    void 존재하지_않는_Drop이면_404_예외를_던진다() {
        when(dropRepository.findById(dropId)).thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.update(dropId, new DropIntroTextRequest("수정본"))
        );
    }
}
