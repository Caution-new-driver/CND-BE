package com.nextrun.cndbe.domain.drop.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.drop.DropConfirmationWriter;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DropResponse {

    private UUID id;
    private String status;

    // b13 확정 시점에 채워짐. 확정 전(DRAFT)에는 null.
    private String name;
    private String introText;
    private Integer expectedProductionDays;
    private LocalDateTime createdAt;

    // f6 재진입(탭 이동·새로고침·"이어서 제작")했을 때도 소개문 재생성 잔여 횟수를
    // 다시 조회할 수 있도록 함께 내려준다. b13 최초 생성을 포함해 계산.
    private int regenerationsRemaining;

    private UUID templateId;
    private String templateName;

    // Template에 JSON 문자열로 저장돼 있는 값을 그대로 raw JSON으로 내려줌 (이중 이스케이프 방지)
    @JsonRawValue
    private String patternPieces;

    @JsonRawValue
    private String requiredAccessories;

    public static DropResponse from(Drop drop) {
        return DropResponse.builder()
                .id(drop.getId())
                .status(drop.getStatus().name())
                .name(drop.getName())
                .introText(drop.getIntroText())
                .expectedProductionDays(drop.getExpectedProductionDays())
                .createdAt(drop.getCreatedAt())
                .regenerationsRemaining(
                        DropConfirmationWriter.MAX_INTRO_TEXT_GENERATION_COUNT
                                - (drop.getIntroTextGenerationCount() == null
                                        ? 0
                                        : drop.getIntroTextGenerationCount())
                )
                .templateId(drop.getTemplate().getId())
                .templateName(drop.getTemplate().getName())
                .patternPieces(drop.getTemplate().getPatternPieces())
                .requiredAccessories(drop.getTemplate().getRequiredAccessories())
                .build();
    }
}