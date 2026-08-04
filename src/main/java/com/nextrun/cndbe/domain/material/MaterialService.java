package com.nextrun.cndbe.domain.material;

import com.nextrun.cndbe.common.client.CloudinaryImageUploader;
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

    @Transactional
    public Material create(MaterialCreateRequest request) {

        // 전체샷/클로즈업 사진을 각각 Cloudinary에 업로드하고 URL 받기.
        // 사진이 안 왔을 수도 있으니(선택사항) null/empty 체크부터.
        String imageUrlFull = (request.getImageFull() != null && !request.getImageFull().isEmpty())
                ? imageUploader.upload(request.getImageFull())
                : null;

        String imageUrlCloseup = (request.getImageCloseup() != null && !request.getImageCloseup().isEmpty())
                ? imageUploader.upload(request.getImageCloseup())
                : null;

        // AI 값(color/pattern/texture 등)은 아직 안 넣음 -> b6에서 나중에 채워짐.
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
                .imageUrlFull(imageUrlFull)
                .imageUrlCloseup(imageUrlCloseup)
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

        // 새 사진이 왔을 때만 기존 사진 URL 교체 (안 왔으면 기존 것 그대로 유지)
        if (request.getImageFull() != null && !request.getImageFull().isEmpty()) {
            material.setImageUrlFull(imageUploader.upload(request.getImageFull()));
        }
        if (request.getImageCloseup() != null && !request.getImageCloseup().isEmpty()) {
            material.setImageUrlCloseup(imageUploader.upload(request.getImageCloseup()));
        }

        // materialRepository.save() 안 불러도 됨 -> JPA 더티 체킹이 알아서 UPDATE 쿼리를 날려줌
        return material;
    }

    // b6: 사진 보고 AI가 색상·패턴·질감·신뢰도·특이사항을 자동으로 채워줌
    @Transactional
    public Material tagWithAi(UUID id) {
        Material material = findMaterialOrThrow(id);

        MaterialAiTagResult result = aiTaggingClient.tag(
                material.getImageUrlFull(), material.getImageUrlCloseup());

        material.setColor(result.color());
        material.setPattern(result.pattern());
        material.setTexture(result.texture());
        material.setAiConfidence(result.aiConfidence());
        material.setSurfaceNotes(result.surfaceNotes());

        return material;
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
}