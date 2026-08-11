package com.nextrun.cndbe.domain.drop.dto;

import java.util.UUID;

public record DropIntroTextResponse(
        UUID dropId,
        String introText
) {
}
