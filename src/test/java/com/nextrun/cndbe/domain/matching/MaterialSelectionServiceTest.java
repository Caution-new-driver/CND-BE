package com.nextrun.cndbe.domain.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.drop.DropRepository;
import com.nextrun.cndbe.domain.drop.DropStatus;
import com.nextrun.cndbe.domain.matching.dto.MaterialSelectionRequest;
import com.nextrun.cndbe.domain.matching.dto.MaterialSelectionResponse;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.MaterialStatus;
import com.nextrun.cndbe.domain.material.repository.MaterialRepository;
import com.nextrun.cndbe.domain.production.ProductionScenarioInvalidator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 실제 DB 없이 b11의 예약·교체·중복 선택 방지 규칙을 검증함.
@ExtendWith(MockitoExtension.class)
class MaterialSelectionServiceTest {

    @Mock
    private DropRepository dropRepository;

    @Mock
    private MaterialCandidateRepository materialCandidateRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private DropMaterialSelectionRepository selectionRepository;

    @Mock
    private ProductionScenarioInvalidator scenarioInvalidator;

    @InjectMocks
    private MaterialSelectionService service;

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
    void 주_소재와_포인트_소재를_예약하고_선택을_저장한다() {
        Material main = material("MAIN", MaterialStatus.AVAILABLE);
        Material point = material("POINT", MaterialStatus.AVAILABLE);
        MaterialCandidate mainCandidate = candidate(main);
        MaterialCandidate pointCandidate = candidate(point);

        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));
        when(materialCandidateRepository.findByIdAndDrop_Id(
                mainCandidate.getId(), dropId
        )).thenReturn(Optional.of(mainCandidate));
        when(materialCandidateRepository.findByIdAndDrop_Id(
                pointCandidate.getId(), dropId
        )).thenReturn(Optional.of(pointCandidate));
        when(selectionRepository.findByDrop_Id(dropId))
                .thenReturn(Optional.empty());
        when(materialRepository.findAllByIdForUpdate(any()))
                .thenReturn(List.of(main, point));
        mockSelectionSave();

        MaterialSelectionResponse response = service.selectMaterials(
                dropId,
                new MaterialSelectionRequest(
                        mainCandidate.getId(),
                        pointCandidate.getId()
                )
        );

        assertEquals(MaterialStatus.RESERVED, main.getStatus());
        assertEquals(MaterialStatus.RESERVED, point.getStatus());
        assertEquals(main.getId(), response.mainMaterial().id());
        assertEquals(point.getId(), response.pointMaterial().id());
        verify(selectionRepository).save(any(DropMaterialSelection.class));
        verify(scenarioInvalidator).invalidate(drop);
    }

    @Test
    void 다시_선택하면_기존_소재를_해제하고_새_소재를_예약한다() {
        Material oldMain = material("OLD-MAIN", MaterialStatus.RESERVED);
        Material oldPoint = material("OLD-POINT", MaterialStatus.RESERVED);
        Material newMain = material("NEW-MAIN", MaterialStatus.AVAILABLE);
        MaterialCandidate newMainCandidate = candidate(newMain);
        DropMaterialSelection existing = DropMaterialSelection.builder()
                .id(UUID.randomUUID())
                .drop(drop)
                .mainMaterial(oldMain)
                .pointMaterial(oldPoint)
                .build();

        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));
        when(materialCandidateRepository.findByIdAndDrop_Id(
                newMainCandidate.getId(), dropId
        )).thenReturn(Optional.of(newMainCandidate));
        when(selectionRepository.findByDrop_Id(dropId))
                .thenReturn(Optional.of(existing));
        when(materialRepository.findAllByIdForUpdate(any()))
                .thenReturn(List.of(oldMain, oldPoint, newMain));
        when(selectionRepository.save(existing)).thenReturn(existing);

        MaterialSelectionResponse response = service.selectMaterials(
                dropId,
                new MaterialSelectionRequest(
                        newMainCandidate.getId(),
                        null
                )
        );

        assertEquals(MaterialStatus.AVAILABLE, oldMain.getStatus());
        assertEquals(MaterialStatus.AVAILABLE, oldPoint.getStatus());
        assertEquals(MaterialStatus.RESERVED, newMain.getStatus());
        assertSame(newMain, existing.getMainMaterial());
        assertNull(existing.getPointMaterial());
        assertNull(response.pointMaterial());
        verify(scenarioInvalidator).invalidate(drop);
    }

    @Test
    void 다른_Drop에서_예약한_소재는_선택할_수_없다() {
        Material reserved = material("RESERVED", MaterialStatus.RESERVED);
        MaterialCandidate candidate = candidate(reserved);

        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));
        when(materialCandidateRepository.findByIdAndDrop_Id(
                candidate.getId(), dropId
        )).thenReturn(Optional.of(candidate));
        when(selectionRepository.findByDrop_Id(dropId))
                .thenReturn(Optional.empty());
        when(materialRepository.findAllByIdForUpdate(any()))
                .thenReturn(List.of(reserved));

        assertThrows(
                IllegalStateException.class,
                () -> service.selectMaterials(
                        dropId,
                        new MaterialSelectionRequest(candidate.getId(), null)
                )
        );
    }

    @Test
    void 같은_소재를_주_소재와_포인트_소재로_동시에_선택할_수_없다() {
        Material material = material("SAME", MaterialStatus.AVAILABLE);
        MaterialCandidate mainCandidate = candidate(material);
        MaterialCandidate pointCandidate = candidate(material);

        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));
        when(materialCandidateRepository.findByIdAndDrop_Id(
                mainCandidate.getId(), dropId
        )).thenReturn(Optional.of(mainCandidate));
        when(materialCandidateRepository.findByIdAndDrop_Id(
                pointCandidate.getId(), dropId
        )).thenReturn(Optional.of(pointCandidate));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.selectMaterials(
                        dropId,
                        new MaterialSelectionRequest(
                                mainCandidate.getId(),
                                pointCandidate.getId()
                        )
                )
        );
    }

    @Test
    void 확정된_Drop의_소재_선택은_변경할_수_없다() {
        drop.setStatus(DropStatus.CONFIRMED);
        when(dropRepository.findByIdForUpdate(dropId)).thenReturn(Optional.of(drop));

        assertThrows(
                IllegalStateException.class,
                () -> service.selectMaterials(
                        dropId,
                        new MaterialSelectionRequest(UUID.randomUUID(), null)
                )
        );
    }

    @Test
    void 존재하지_않는_Drop이면_404_예외를_던진다() {
        when(dropRepository.findByIdForUpdate(dropId))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.selectMaterials(
                        dropId,
                        new MaterialSelectionRequest(UUID.randomUUID(), null)
                )
        );
    }

    @Test
    void 해당_Drop에_없는_후보이면_404_예외를_던진다() {
        UUID candidateId = UUID.randomUUID();
        when(dropRepository.findByIdForUpdate(dropId))
                .thenReturn(Optional.of(drop));
        when(materialCandidateRepository.findByIdAndDrop_Id(candidateId, dropId))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.selectMaterials(
                        dropId,
                        new MaterialSelectionRequest(candidateId, null)
                )
        );
    }

    @Test
    void 재검색하면_기존_선택을_삭제하고_예약을_해제한다() {
        Material main = material("MAIN", MaterialStatus.RESERVED);
        Material point = material("POINT", MaterialStatus.RESERVED);
        DropMaterialSelection selection = DropMaterialSelection.builder()
                .id(UUID.randomUUID())
                .drop(drop)
                .mainMaterial(main)
                .pointMaterial(point)
                .build();

        when(dropRepository.findByIdForUpdate(dropId))
                .thenReturn(Optional.of(drop));
        when(selectionRepository.findByDrop_Id(dropId))
                .thenReturn(Optional.of(selection));
        when(materialRepository.findAllByIdForUpdate(any()))
                .thenReturn(List.of(main, point));

        service.releaseSelectionForResearch(dropId);

        assertEquals(MaterialStatus.AVAILABLE, main.getStatus());
        assertEquals(MaterialStatus.AVAILABLE, point.getStatus());
        verify(selectionRepository).delete(selection);
        verify(scenarioInvalidator).invalidate(drop);
    }

    @Test
    void 현재_Drop이_선택한_소재_ID를_재검색용으로_조회한다() {
        Material main = material("MAIN", MaterialStatus.RESERVED);
        Material point = material("POINT", MaterialStatus.RESERVED);
        DropMaterialSelection selection = DropMaterialSelection.builder()
                .drop(drop)
                .mainMaterial(main)
                .pointMaterial(point)
                .build();
        when(selectionRepository.findByDrop_Id(dropId))
                .thenReturn(Optional.of(selection));

        Set<UUID> selectedIds = service.findSelectedMaterialIds(dropId);

        assertEquals(Set.of(main.getId(), point.getId()), selectedIds);
    }

    private Material material(String code, MaterialStatus status) {
        return Material.builder()
                .id(UUID.randomUUID())
                .materialCode(code)
                .status(status)
                .build();
    }

    private MaterialCandidate candidate(Material material) {
        return MaterialCandidate.builder()
                .id(UUID.randomUUID())
                .drop(drop)
                .material(material)
                .build();
    }

    private void mockSelectionSave() {
        when(selectionRepository.save(any(DropMaterialSelection.class)))
                .thenAnswer(invocation -> {
                    DropMaterialSelection selection = invocation.getArgument(0);
                    selection.setId(UUID.randomUUID());
                    return selection;
                });
    }
}
