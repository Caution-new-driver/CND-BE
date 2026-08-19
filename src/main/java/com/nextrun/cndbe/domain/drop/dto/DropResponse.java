package com.nextrun.cndbe.domain.drop.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.nextrun.cndbe.domain.drop.Drop;
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
                .templateId(drop.getTemplate().getId())
                .templateName(drop.getTemplate().getName())
                .patternPieces(drop.getTemplate().getPatternPieces())
                .requiredAccessories(drop.getTemplate().getRequiredAccessories())
                .build();
    }
}