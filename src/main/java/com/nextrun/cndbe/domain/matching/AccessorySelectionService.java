package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.drop.DropRepository;
import com.nextrun.cndbe.domain.drop.DropStatus;
import com.nextrun.cndbe.domain.matching.dto.AccessorySelectionRequest;
import com.nextrun.cndbe.domain.matching.dto.AccessorySelectionResponse;
import com.nextrun.cndbe.domain.material.Accessory;
import com.nextrun.cndbe.domain.material.repository.AccessoryRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// b11 부자재 선택의 검증과 교체 저장을 담당함.
@Service
@RequiredArgsConstructor
public class AccessorySelectionService {

    private final DropRepository dropRepository;
    private final AccessoryRepository accessoryRepository;
    private final DropAccessorySelectionRepository selectionRepository;
    private final TemplateAccessoryValidator templateAccessoryValidator;

    @Transactional
    public AccessorySelectionResponse selectAccessories(
            UUID dropId,
            AccessorySelectionRequest request
    ) {
        Drop drop = findEditableDrop(dropId);
        List<UUID> requestedIds = validateRequest(request);

        List<Accessory> accessories = accessoryRepository
                .findAllById(requestedIds);
        if (accessories.size() != requestedIds.size()) {
            throw new NoSuchElementException(
                    "존재하지 않는 부자재가 포함되어 있습니다."
            );
        }

        // 미니백 템플릿이 요구하는 지퍼·링 등이 빠지거나 중복되지 않았는지 확인함.
        templateAccessoryValidator.validate(
                drop.getTemplate(),
                accessories
        );

        // findAllById의 반환 순서는 보장되지 않으므로 요청 순서대로 다시 정렬함.
        Map<UUID, Accessory> accessoryById = accessories.stream()
                .collect(Collectors.toMap(
                        Accessory::getId,
                        Function.identity()
                ));

        List<DropAccessorySelection> newSelections = requestedIds.stream()
                .map(accessoryById::get)
                .map(accessory -> DropAccessorySelection.builder()
                        .drop(drop)
                        .accessory(accessory)
                        .build())
                .toList();

        selectionRepository.deleteAllByDrop_Id(dropId);
        List<DropAccessorySelection> savedSelections =
                selectionRepository.saveAll(newSelections);

        return AccessorySelectionResponse.from(
                dropId,
                savedSelections
        );
    }

    private Drop findEditableDrop(UUID dropId) {
        Drop drop = dropRepository.findByIdForUpdate(dropId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Drop을 찾을 수 없습니다: " + dropId
                ));

        if (drop.getStatus() != DropStatus.DRAFT) {
            throw new IllegalStateException(
                    "확정된 Drop의 부자재 선택은 변경할 수 없습니다."
            );
        }
        return drop;
    }

    private List<UUID> validateRequest(
            AccessorySelectionRequest request
    ) {
        if (request == null
                || request.accessoryIds() == null
                || request.accessoryIds().isEmpty()) {
            throw new IllegalArgumentException(
                    "부자재를 한 개 이상 선택해야 합니다."
            );
        }
        if (request.accessoryIds().stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException(
                    "부자재 ID에는 null을 넣을 수 없습니다."
            );
        }

        Set<UUID> uniqueIds = new LinkedHashSet<>(request.accessoryIds());
        if (uniqueIds.size() != request.accessoryIds().size()) {
            throw new IllegalArgumentException(
                    "같은 부자재를 중복 선택할 수 없습니다."
            );
        }
        return List.copyOf(uniqueIds);
    }
}
