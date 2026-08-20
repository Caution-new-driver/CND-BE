package com.nextrun.cndbe.domain.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.common.client.CloudinaryImageUploader;
import com.nextrun.cndbe.common.client.CloudinaryImageUploader.UploadResult;
import com.nextrun.cndbe.domain.matching.MaterialCandidateRepository;
import com.nextrun.cndbe.domain.material.dto.MaterialCreateRequest;
import com.nextrun.cndbe.domain.material.dto.MaterialUpdateRequest;
import com.nextrun.cndbe.domain.material.repository.MaterialRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

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

    @Test
    void 등록_시_사진의_public_id를_같이_저장한다() {
        MaterialCreateRequest request = new MaterialCreateRequest();
        request.setImageFull(new MockMultipartFile("imageFull", "full.jpg", "image/jpeg", new byte[]{1}));
        request.setImageCloseup(new MockMultipartFile("imageCloseup", "closeup.jpg", "image/jpeg", new byte[]{2}));
        when(imageUploader.upload(request.getImageFull()))
                .thenReturn(new UploadResult("https://cdn/full.jpg", "full-public-id"));
        when(imageUploader.upload(request.getImageCloseup()))
                .thenReturn(new UploadResult("https://cdn/closeup.jpg", "closeup-public-id"));
        when(materialRepository.save(any(Material.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Material saved = service.create(request);

        assertEquals("full-public-id", saved.getImagePublicIdFull());
        assertEquals("closeup-public-id", saved.getImagePublicIdCloseup());
    }

    @Test
    void 사진_교체_시_옛_public_id로_Cloudinary에서_옛_사진을_지운다() {
        UUID materialId = UUID.randomUUID();
        Material material = Material.builder()
                .id(materialId)
                .status(MaterialStatus.AVAILABLE)
                .imageUrlFull("https://cdn/old-full.jpg")
                .imagePublicIdFull("old-full-public-id")
                .build();
        when(materialRepository.findById(materialId))
                .thenReturn(Optional.of(material));

        MaterialUpdateRequest request = new MaterialUpdateRequest();
        request.setImageFull(new MockMultipartFile("imageFull", "new-full.jpg", "image/jpeg", new byte[]{1}));
        when(imageUploader.upload(request.getImageFull()))
                .thenReturn(new UploadResult("https://cdn/new-full.jpg", "new-full-public-id"));

        Material updated = service.update(materialId, request);

        verify(imageUploader).delete("old-full-public-id");
        assertEquals("new-full-public-id", updated.getImagePublicIdFull());
        assertEquals("https://cdn/new-full.jpg", updated.getImageUrlFull());
    }

    @Test
    void 소재_삭제_시_전체샷과_클로즈업_public_id로_Cloudinary_삭제를_호출한다() {
        UUID materialId = UUID.randomUUID();
        Material material = Material.builder()
                .id(materialId)
                .status(MaterialStatus.AVAILABLE)
                .imagePublicIdFull("full-public-id")
                .imagePublicIdCloseup("closeup-public-id")
                .build();
        when(materialRepository.findById(materialId))
                .thenReturn(Optional.of(material));

        service.delete(materialId);

        verify(imageUploader).delete("full-public-id");
        verify(imageUploader).delete("closeup-public-id");
    }

    @Test
    void public_id가_없는_레거시_소재도_에러_없이_삭제된다() {
        UUID materialId = UUID.randomUUID();
        Material material = Material.builder()
                .id(materialId)
                .status(MaterialStatus.AVAILABLE)
                .imagePublicIdFull(null)
                .imagePublicIdCloseup(null)
                .build();
        when(materialRepository.findById(materialId))
                .thenReturn(Optional.of(material));

        service.delete(materialId);

        verify(imageUploader, times(2)).delete(null);
        verify(materialRepository).delete(material);
    }
}
