package com.nextrun.cndbe.domain.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.domain.drop.dto.DropIntroTextRequest;
import com.nextrun.cndbe.domain.drop.dto.DropIntroTextResponse;
import com.nextrun.cndbe.domain.production.ProductType;
import com.nextrun.cndbe.domain.production.ScenarioType;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// b14의 재생성(regenerate)과 담당자 수정본 저장(update)을 검증함.
// confirmCore/reserveRegenerationAttempt 자체의 DB 검증 로직은 DropConfirmationWriterTest에서 다룸.
@ExtendWith(MockitoExtension.class)
class DropIntroTextServiceTest {

    @Mock
    private DropRepository dropRepository;

    @Mock
    private DropConfirmationWriter confirmationWriter;

    @Mock
    private DropIntroTextClient introTextClient;

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
    void 재생성이_성공하면_저장하고_남은_횟수를_반환한다() {
        DropIntroTextPromptData promptData = new DropIntroTextPromptData(
                "첫 번째 RUN Drop",
                "미니백",
                ScenarioType.MAIN_ONLY,
                new DropIntroTextPromptData.MaterialSummary(
                        com.nextrun.cndbe.domain.material.MaterialType.LEATHER,
                        com.nextrun.cndbe.domain.material.MaterialColor.BLACK,
                        com.nextrun.cndbe.domain.material.MaterialPattern.SOLID,
                        com.nextrun.cndbe.domain.material.MaterialGrade.A
                ),
                null,
                List.of(new DropIntroTextPromptData.ProductSummary(ProductType.MINI_BAG, 8))
        );
        when(confirmationWriter.reserveRegenerationAttempt(dropId))
                .thenReturn(new DropConfirmationWriter.IntroTextRegenerationReservation(promptData, 2));
        when(introTextClient.generate(promptData)).thenReturn("다시 생성된 소개문");

        DropIntroTextResponse response = service.regenerate(dropId);

        assertEquals("다시 생성된 소개문", response.introText());
        assertEquals(
                DropConfirmationWriter.MAX_INTRO_TEXT_GENERATION_COUNT - 2,
                response.regenerationsRemaining()
        );
        verify(confirmationWriter).saveIntroText(dropId, "다시 생성된 소개문");
    }

    @Test
    void 재생성_예약이_거부되면_저장을_시도하지_않고_예외가_그대로_전달된다() {
        when(confirmationWriter.reserveRegenerationAttempt(dropId))
                .thenThrow(new IllegalStateException("AI 소개문 생성 가능 횟수를 모두 사용했습니다. 직접 입력해주세요."));

        assertThrows(IllegalStateException.class, () -> service.regenerate(dropId));
    }

    @Test
    void AI_호출이_실패하면_남은_횟수를_담은_예외로_변환된다() {
        DropIntroTextPromptData promptData = new DropIntroTextPromptData(
                "이름", "미니백", ScenarioType.MAIN_ONLY, null, null, List.of()
        );
        when(confirmationWriter.reserveRegenerationAttempt(dropId))
                .thenReturn(new DropConfirmationWriter.IntroTextRegenerationReservation(promptData, 2));
        when(introTextClient.generate(promptData))
                .thenThrow(new IllegalStateException("AI 소개문 생성에 실패했습니다."));

        IntroTextGenerationFailedException exception = assertThrows(
                IntroTextGenerationFailedException.class,
                () -> service.regenerate(dropId)
        );
        assertEquals(
                DropConfirmationWriter.MAX_INTRO_TEXT_GENERATION_COUNT - 2,
                exception.getRegenerationsRemaining()
        );
    }

    @Test
    void 담당자_수정본을_그대로_저장하고_남은_횟수를_함께_반환한다() {
        drop.setIntroTextGenerationCount(3);
        when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));

        DropIntroTextResponse response = service.update(
                dropId,
                new DropIntroTextRequest("담당자가 직접 다듬은 소개문")
        );

        assertEquals("담당자가 직접 다듬은 소개문", drop.getIntroText());
        assertEquals("담당자가 직접 다듬은 소개문", response.introText());
        assertEquals(
                DropConfirmationWriter.MAX_INTRO_TEXT_GENERATION_COUNT - 3,
                response.regenerationsRemaining()
        );
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
