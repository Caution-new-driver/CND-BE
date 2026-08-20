package com.nextrun.cndbe.domain.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.drop.DropRepository;
import com.nextrun.cndbe.domain.drop.DropStatus;
import com.nextrun.cndbe.domain.matching.dto.AccessorySelectionRequest;
import com.nextrun.cndbe.domain.matching.dto.AccessorySelectionResponse;
import com.nextrun.cndbe.domain.material.Accessory;
import com.nextrun.cndbe.domain.material.AccessoryColor;
import com.nextrun.cndbe.domain.material.repository.AccessoryRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 실제 DB 없이 b11 부자재 세트의 검증·교체 저장 흐름을 확인함.
@ExtendWith(MockitoExtension.class)
class AccessorySelectionServiceTest {

    @Mock
    private DropRepository dropRepository;

    @Mock
    private AccessoryRepository accessoryRepository;

    @Mock
    private DropAccessorySelectionRepository selectionRepository;

    @Mock
    private TemplateAccessoryValidator templateAccessoryValidator;

    @InjectMocks
    private AccessorySelectionService service;

    private UUID dropId;
    private Drop drop;

    @BeforeEach
    void setUp() {
        dropId = UUID.randomUUID();
        drop = Drop.builder()
                .id(dropId)
                .status(DropStatus.DRAFT)
                .build();
    }

    @Test
    void 선택한_부자재를_요청_순서대로_교체_저장한다() {
        Accessory zipper = accessory("지퍼", AccessoryColor.GOLD);
        Accessory ring = accessory("링", AccessoryColor.GOLD);

        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));
        // DB 반환 순서가 요청 순서와 달라도 응답은 요청 순서를 유지해야 함.
        when(accessoryRepository.findAllById(
                List.of(zipper.getId(), ring.getId())
        )).thenReturn(List.of(ring, zipper));
        when(selectionRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    List<DropAccessorySelection> selections =
                            invocation.getArgument(0);
                    selections.forEach(selection ->
                            selection.setId(UUID.randomUUID())
                    );
                    return selections;
                });

        AccessorySelectionResponse response = service.selectAccessories(
                dropId,
                new AccessorySelectionRequest(
                        List.of(zipper.getId(), ring.getId())
                )
        );

        assertEquals(2, response.selections().size());
        assertEquals(
                zipper.getId(),
                response.selections().get(0).accessoryId()
        );
        assertEquals(
                ring.getId(),
                response.selections().get(1).accessoryId()
        );
        verify(templateAccessoryValidator).validate(
                drop.getTemplate(),
                List.of(ring, zipper)
        );

        // flush가 delete와 saveAll 사이에서 반드시 호출돼야 한다 — 그렇지 않으면 Hibernate가
        // insert를 delete보다 먼저 flush해서, 안 바뀐 부자재의 (drop_id, accessory_id) 유니크
        // 제약을 옛 행이 아직 남아있는 상태에서 위반하게 된다.
        InOrder order = inOrder(selectionRepository);
        order.verify(selectionRepository).deleteAllByDrop_Id(dropId);
        order.verify(selectionRepository).flush();
        order.verify(selectionRepository).saveAll(anyList());
    }

    @Test
    void 같은_부자재를_중복_선택할_수_없다() {
        UUID accessoryId = UUID.randomUUID();
        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.selectAccessories(
                        dropId,
                        new AccessorySelectionRequest(
                                List.of(accessoryId, accessoryId)
                        )
                )
        );
    }

    @Test
    void 존재하지_않는_Drop이면_404_예외를_던진다() {
        when(dropRepository.findByIdForUpdate(dropId))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.selectAccessories(
                        dropId,
                        new AccessorySelectionRequest(
                                List.of(UUID.randomUUID())
                        )
                )
        );
    }

    @Test
    void 존재하지_않는_부자재가_포함되면_저장하지_않는다() {
        Accessory existing = accessory("지퍼", AccessoryColor.SILVER);
        UUID missingId = UUID.randomUUID();
        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));
        when(accessoryRepository.findAllById(
                List.of(existing.getId(), missingId)
        )).thenReturn(List.of(existing));

        assertThrows(
                NoSuchElementException.class,
                () -> service.selectAccessories(
                        dropId,
                        new AccessorySelectionRequest(
                                List.of(existing.getId(), missingId)
                        )
                )
        );
    }

    @Test
    void 확정된_Drop의_부자재_선택은_변경할_수_없다() {
        drop.setStatus(DropStatus.CONFIRMED);
        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));

        assertThrows(
                IllegalStateException.class,
                () -> service.selectAccessories(
                        dropId,
                        new AccessorySelectionRequest(
                                List.of(UUID.randomUUID())
                        )
                )
        );
    }

    private Accessory accessory(
            String type,
            AccessoryColor color
    ) {
        return Accessory.builder()
                .id(UUID.randomUUID())
                .accessoryType(type)
                .color(color)
                .build();
    }
}
