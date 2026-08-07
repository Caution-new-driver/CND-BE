package com.nextrun.cndbe.domain.matching.dto;

import com.nextrun.cndbe.domain.matching.DropAccessorySelection;
import java.util.List;
import java.util.UUID;

// 저장된 부자재 세트를 f4 요약 카드에서 바로 사용할 수 있는 형태로 반환함.
public record AccessorySelectionResponse(
        UUID dropId,
        List<SelectedAccessory> selections
) {

    public static AccessorySelectionResponse from(
            UUID dropId,
            List<DropAccessorySelection> selections
    ) {
        return new AccessorySelectionResponse(
                dropId,
                selections.stream()
                        .map(selection -> new SelectedAccessory(
                                selection.getId(),
                                selection.getAccessory().getId(),
                                selection.getAccessory().getAccessoryType(),
                                selection.getAccessory().getColor()
                        ))
                        .toList()
        );
    }

    public record SelectedAccessory(
            UUID selectionId,
            UUID accessoryId,
            String accessoryType,
            String color
    ) {
    }
}
