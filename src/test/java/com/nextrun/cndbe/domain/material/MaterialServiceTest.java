package com.nextrun.cndbe.domain.material;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.common.client.CloudinaryImageUploader;
import com.nextrun.cndbe.domain.matching.MaterialCandidateRepository;
import com.nextrun.cndbe.domain.material.repository.MaterialRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// AVAILABLE 소재만 삭제가 허용되고, 그 외 상태는 막히는지 DB 없이 검증함.
@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private CloudinaryImageUploader imageUploader;

    @Mock
    private MaterialAiTaggingClient aiTaggingClient;

    @Mock
    private MaterialCandidateRepository materialCandidateRepository;

    @InjectMocks
    private MaterialService service;

    @Test
    void AVAILABLE_소재는_탈락_후보_이력을_같이_지우고_삭제한다() {
        UUID materialId = UUID.randomUUID();
        Material material = Material.builder()
                .id(materialId)
                .status(MaterialStatus.AVAILABLE)
                .build();
        when(materialRepository.findById(materialId))
                .thenReturn(Optional.of(material));

        service.delete(materialId);

        verify(materialCandidateRepository).deleteByMaterial_Id(materialId);
        verify(materialRepository).delete(material);
    }

    @Test
    void RESERVED_소재는_삭제할_수_없다() {
        UUID materialId = UUID.randomUUID();
        Material material = Material.builder()
                .id(materialId)
                .status(MaterialStatus.RESERVED)
                .build();
        when(materialRepository.findById(materialId))
                .thenReturn(Optional.of(material));

        assertThrows(
                IllegalStateException.class,
                () -> service.delete(materialId)
        );

        verify(materialCandidateRepository, never()).deleteByMaterial_Id(materialId);
        verify(materialRepository, never()).delete(material);
    }

    @Test
    void DEPLETED_소재는_삭제할_수_없다() {
        UUID materialId = UUID.randomUUID();
        Material material = Material.builder()
                .id(materialId)
                .status(MaterialStatus.DEPLETED)
                .build();
        when(materialRepository.findById(materialId))
                .thenReturn(Optional.of(material));

        assertThrows(
                IllegalStateException.class,
                () -> service.delete(materialId)
        );

        verify(materialCandidateRepository, never()).deleteByMaterial_Id(materialId);
        verify(materialRepository, never()).delete(material);
    }
}
