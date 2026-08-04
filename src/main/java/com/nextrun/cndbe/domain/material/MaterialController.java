package com.nextrun.cndbe.domain.material;

import com.nextrun.cndbe.domain.material.dto.MaterialCreateRequest;
import com.nextrun.cndbe.domain.material.dto.MaterialResponse;
import com.nextrun.cndbe.domain.material.dto.MaterialUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// "카운터 직원". 손님(프론트엔드)이 보낸 웹 요청을 받아서, 알맞은 Service 메서드로 전달함.
@Tag(name = "Material", description = "소재 등록/조회/필터/수정 + AI 태깅 API")
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    // [등록] POST /api/materials
    @Operation(summary = "소재 등록", description = "소재 정보와 사진(전체샷 필수, 클로즈업 선택)을 등록합니다. "
            + "등록 직후 status는 AVAILABLE로 시작하며, AI 태깅 값(color/pattern/texture 등)은 비어있습니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse create(@ModelAttribute MaterialCreateRequest request) {
        return MaterialResponse.from(materialService.create(request));
    }

    // [단건 조회] GET /api/materials/{id}
    @Operation(summary = "소재 단건 조회", description = "id로 소재 하나를 조회합니다. 없으면 404를 반환합니다.")
    @GetMapping("/{id}")
    public MaterialResponse getById(@Parameter(description = "소재 id (UUID)") @PathVariable UUID id) {
        return MaterialResponse.from(materialService.getById(id));
    }

    // [목록 + 필터] GET /api/materials?status=AVAILABLE&materialType=LEATHER
    // status, materialType 둘 다 생략 가능 (안 주면 전체 조회)
    @Operation(summary = "소재 목록 조회 (필터 가능)",
            description = "status, materialType으로 필터링해서 소재 목록을 조회합니다. 둘 다 생략하면 전체 조회됩니다.")
    @GetMapping
    public List<MaterialResponse> list(
            @Parameter(description = "소재 상태로 필터링") @RequestParam(required = false) MaterialStatus status,
            @Parameter(description = "소재 종류로 필터링") @RequestParam(required = false) MaterialType materialType) {
        return materialService.list(status, materialType).stream()
                .map(MaterialResponse::from)
                .toList();
    }

    // [AI 태깅] POST /api/materials/{id}/ai-tag
    // body 없이 그냥 호출만 하면 됨. 이미 저장된 사진 URL을 갖고 AI한테 물어봄.
    @Operation(summary = "AI 이미지 태깅", description = "저장된 사진 URL로 OpenAI를 호출해 color/pattern/texture/aiConfidence/surfaceNotes를 채웁니다. "
            + "이미 태깅된 소재(color가 있는 경우)는 재호출하지 않고 기존 값을 그대로 반환합니다.")
    @PostMapping("/{id}/ai-tag")
    public MaterialResponse tagWithAi(@Parameter(description = "소재 id (UUID)") @PathVariable UUID id) {
        return MaterialResponse.from(materialService.tagWithAi(id));
    }

    // [수정] PATCH /api/materials/{id}
    // 새 사진을 올릴 수도 있어서 이것도 multipart/form-data.
    // 바꾸고 싶은 key만 보내면 됨 (예: grade만 보내면 grade만 바뀜)
    @Operation(summary = "소재 수정", description = "값을 보낸 필드만 부분 수정됩니다 (보내지 않은 필드는 그대로 유지). "
            + "새 사진을 보내면 기존 사진 URL을 교체합니다.")
    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MaterialResponse update(@Parameter(description = "소재 id (UUID)") @PathVariable UUID id,
                                   @ModelAttribute MaterialUpdateRequest request) {
        return MaterialResponse.from(materialService.update(id, request));
    }
}