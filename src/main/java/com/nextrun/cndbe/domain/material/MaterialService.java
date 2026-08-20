package com.nextrun.cndbe.domain.material;

import com.nextrun.cndbe.common.client.CloudinaryImageUploader;
import com.nextrun.cndbe.common.client.CloudinaryImageUploader.UploadResult;
import com.nextrun.cndbe.domain.matching.MaterialCandidateRepository;
import com.nextrun.cndbe.domain.material.dto.MaterialAiTagPreviewRequest;
import com.nextrun.cndbe.domain.material.dto.MaterialCreateRequest;
import com.nextrun.cndbe.domain.material.dto.MaterialUpdateRequest;
import com.nextrun.cndbe.domain.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final CloudinaryImageUploader imageUploader;
    private final MaterialAiTaggingClient aiTaggingClient;
    private final MaterialCandidateRepository materialCandidateRepository;

    @Transactional
    public Material create(MaterialCreateRequest request) {

        // 사진이 안 왔을 수도 있으니(선택사항) null/empty 체크부터.
        UploadResult imageFull = (request.getImageFull() != null && !request.getImageFull().isEmpty())
                ? imageUploader.upload(request.getImageFull())
                : null;

        UploadResult imageCloseup = (request.getImageCloseup() != null && !request.getImageCloseup().isEmpty())
                ? imageUploader.upload(request.getImageCloseup())
                : null;

        Material material = Material.builder()
                .materialCode(request.getMaterialCode())
                .materialType(request.getMaterialType())
                .grade(request.getGrade())
                .widthMm(request.getWidthMm())
                .heightMm(request.getHeightMm())
                .thicknessMm(request.getThicknessMm())
                .handFeel(request.getHandFeel())
                .flexibility(request.getFlexibility())
                .quantity(request.getQuantity())
                .imageUrlFull(imageFull != null ? imageFull.url() : null)
                .imagePublicIdFull(imageFull != null ? imageFull.publicId() : null)
                .imageUrlCloseup(imageCloseup != null ? imageCloseup.url() : null)
                .imagePublicIdCloseup(imageCloseup != null ? imageCloseup.publicId() : null)
                .status(MaterialStatus.AVAILABLE)
                .build();

        // 저장하고 결과 반환.
        return materialRepository.save(material);
    }

    public Material getById(UUID id) {
        return findMaterialOrThrow(id);
    }

    public List<Material> list(MaterialStatus status, MaterialType materialType) {
        Specification<Material> spec = Specification.where(hasStatus(status))
                .and(hasMaterialType(materialType));

        return materialRepository.findAll(spec);
    }

    @Transactional
    public Material update(UUID id, MaterialUpdateRequest request) {
        Material material = findMaterialOrThrow(id);

        // 담당자가 직접 수정하는 값들 - null이 아닌 것만 덮어쓰기
        if (request.getMaterialCode() != null) material.setMaterialCode(request.getMaterialCode());
        if (request.getMaterialType() != null) material.setMaterialType(request.getMaterialType());
        if (request.getGrade() != null) material.setGrade(request.getGrade());
        if (request.getWidthMm() != null) material.setWidthMm(request.getWidthMm());
        if (request.getHeightMm() != null) material.setHeightMm(request.getHeightMm());
        if (request.getThicknessMm() != null) material.setThicknessMm(request.getThicknessMm());
        if (request.getHandFeel() != null) material.setHandFeel(request.getHandFeel());
        if (request.getFlexibility() != null) material.setFlexibility(request.getFlexibility());
        if (request.getQuantity() != null) material.setQuantity(request.getQuantity());

        // AI가 채우거나, 담당자가 확인 후 고칠 수 있는 값들
        if (request.getColor() != null) material.setColor(request.getColor());
        if (request.getPattern() != null) material.setPattern(request.getPattern());
        if (request.getTexture() != null) material.setTexture(request.getTexture());
        if (request.getAiConfidence() != null) material.setAiConfidence(request.getAiConfidence());
        if (request.getSurfaceNotes() != null) material.setSurfaceNotes(request.getSurfaceNotes());

        // 새 사진이 왔을 때만 기존 사진 교체. 새 업로드가 실패하면 기존 사진이 그대로
        // 남아있어야 하므로, 반드시 "새로 올리기 성공 -> 옛 것 Cloudinary에서 지우기 ->
        // 필드 교체" 순서를 지킨다.
        if (request.getImageFull() != null && !request.getImageFull().isEmpty()) {
            UploadResult newImageFull = imageUploader.upload(request.getImageFull());
            imageUploader.delete(material.getImagePublicIdFull());
            material.setImageUrlFull(newImageFull.url());
            material.setImagePublicIdFull(newImageFull.publicId());
        }
        if (request.getImageCloseup() != null && !request.getImageCloseup().isEmpty()) {
            UploadResult newImageCloseup = imageUploader.upload(request.getImageCloseup());
            imageUploader.delete(material.getImagePublicIdCloseup());
            material.setImageUrlCloseup(newImageCloseup.url());
            material.setImagePublicIdCloseup(newImageCloseup.publicId());
        }

        // materialRepository.save() 안 불러도 됨 -> JPA 더티 체킹이 알아서 UPDATE 쿼리를 날려줌
        return material;
    }

    // b6: 사진 보고 AI가 색상·패턴·질감·신뢰도·특이사항을 자동으로 채워줌
    @Transactional
    public Material tagWithAi(UUID id) {
        Material material = findMaterialOrThrow(id);

        // 이미 태깅된 소재면 OpenAI를 또 호출하지 않고 바로 반환 (중복 호출 방지 = 비용 절약)
        if (material.getColor() != null) {
            return material;
        }

        MaterialAiTagResult result = aiTaggingClient.tag(
                material.getImageUrlFull(), material.getImageUrlCloseup());

        material.setColor(result.color());
        material.setPattern(result.pattern());
        material.setTexture(result.texture());
        material.setAiConfidence(result.aiConfidence());
        material.setSurfaceNotes(result.surfaceNotes());

        return material;
    }

    // b6확장: 소재를 등록하기 전, 사진만 올린 상태에서 미리 AI 태깅 결과를 보여주기 위한 무상태(stateless) 태깅.
    // Material을 저장하지 않고 사진만 업로드해서 AI한테 물어보고 결과만 돌려준다.
    public MaterialAiTagResult tagPreview(MaterialAiTagPreviewRequest request) {
        if (request.getImageFull() == null || request.getImageFull().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageFull은 필수입니다.");
        }

        // 저장하지 않는 미리보기용 업로드라 public_id는 쓸 일이 없어 URL만 꺼내 씀.
        String imageUrlFull = imageUploader.upload(request.getImageFull()).url();
        String imageUrlCloseup = (request.getImageCloseup() != null && !request.getImageCloseup().isEmpty())
                ? imageUploader.upload(request.getImageCloseup()).url()
                : null;

        return aiTaggingClient.tag(imageUrlFull, imageUrlCloseup);
    }

    // id로 소재를 찾고, 없으면 404 에러를 던지는 부분을 한 곳으로 모아둠 (중복 제거)
    private Material findMaterialOrThrow(UUID id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "id: " + id + "인 소재를 찾을 수 없습니다."));
    }

    // 필터 블록 1: status
    private static Specification<Material> hasStatus(MaterialStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    // 필터 블록 2: materialType
    private static Specification<Material> hasMaterialType(MaterialType materialType) {
        return (root, query, cb) -> materialType == null ? null : cb.equal(root.get("materialType"), materialType);
    }

    // AVAILABLE 소재만 삭제를 허용함. RESERVED/DEPLETED는 drop_material_selection·
    // production_material_result가 참조 중인 상태라 FK 위반 없이는 지울 수 없어서 막음.
    // AVAILABLE이면 이 두 참조는 항상 0건이라(재선택 시 옛 참조가 실제로 지워짐),
    // 남아있을 수 있는 material_candidate(탈락 후보 이력)만 같이 정리하고 삭제함.
    @Transactional
    public void delete(UUID id) {
        Material material = findMaterialOrThrow(id);

        if (material.getStatus() != MaterialStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "AVAILABLE 상태의 소재만 삭제할 수 있습니다. 현재 상태: " + material.getStatus()
            );
        }

        materialCandidateRepository.deleteByMaterial_Id(id);
        imageUploader.delete(material.getImagePublicIdFull());
        imageUploader.delete(material.getImagePublicIdCloseup());
        materialRepository.delete(material);
    }

}