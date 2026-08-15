package com.nextrun.cndbe.domain.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.domain.drop.dto.DropConfirmRequest;
import com.nextrun.cndbe.domain.drop.dto.DropConfirmResponse;
import com.nextrun.cndbe.domain.production.ProductType;
import com.nextrun.cndbe.domain.production.ScenarioType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// b13 오케스트레이션(DropConfirmationWriter 짧은 트랜잭션 + DropIntroTextClient 트랜잭션 밖 호출)만 검증함.
// 확정 검증 로직 자체는 DropConfirmationWriterTest에서 다룸.
@ExtendWith(MockitoExtension.class)
class DropConfirmationServiceTest {

    @Mock
    private DropConfirmationWriter confirmationWriter;

    @Mock
    private DropIntroTextClient introTextClient;

    @InjectMocks
    private DropConfirmationService service;

    private UUID dropId;
    private DropConfirmationCoreResult core;

    @BeforeEach
    void setUp() {
        dropId = UUID.randomUUID();
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
        core = new DropConfirmationCoreResult(
                dropId,
                "CONFIRMED",
                "첫 번째 RUN Drop",
                14,
                UUID.randomUUID(),
                List.of(),
                promptData,
                1
        );
    }

    @Test
    void AI_생성이_성공하면_introText가_채워진_응답을_반환하고_결과를_저장한다() {
        when(confirmationWriter.confirmCore(eq(dropId), any())).thenReturn(core);
        when(introTextClient.generate(core.introTextPromptData()))
                .thenReturn("업사이클링으로 태어난 미니백입니다.");

        DropConfirmResponse response = service.confirm(
                dropId,
                new DropConfirmRequest("첫 번째 RUN Drop", 14)
        );

        assertEquals("CONFIRMED", response.getStatus());
        assertEquals("업사이클링으로 태어난 미니백입니다.", response.getIntroText());
        assertEquals(
                DropConfirmationWriter.MAX_INTRO_TEXT_GENERATION_COUNT - 1,
                response.getRegenerationsRemaining()
        );
        verify(confirmationWriter).saveIntroText(dropId, "업사이클링으로 태어난 미니백입니다.");
    }

    @Test
    void AI_생성이_실패해도_확정_결과는_반환되고_introText는_저장되지_않는다() {
        when(confirmationWriter.confirmCore(eq(dropId), any())).thenReturn(core);
        when(introTextClient.generate(core.introTextPromptData()))
                .thenThrow(new IllegalStateException("AI 소개문 생성에 실패했습니다."));

        DropConfirmResponse response = service.confirm(
                dropId,
                new DropConfirmRequest("첫 번째 RUN Drop", 14)
        );

        assertEquals("CONFIRMED", response.getStatus());
        assertNull(response.getIntroText());
        verify(confirmationWriter, never()).saveIntroText(any(), any());
    }
}
