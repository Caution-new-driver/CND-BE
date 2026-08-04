package com.nextrun.cndbe.domain.material;

import com.nextrun.cndbe.domain.material.dto.MaterialCreateRequest;
import com.nextrun.cndbe.domain.material.dto.MaterialResponse;
import com.nextrun.cndbe.domain.material.dto.MaterialUpdateRequest;
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
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    // [등록] POST /api/materials
    // 사진 파일이 껴 있어서 JSON이 아니라 multipart/form-data로 받음.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse create(@ModelAttribute MaterialCreateRequest request) {
        return MaterialResponse.from(materialService.create(request));
    }

    // [단건 조회] GET /api/materials/{id}
    @GetMapping("/{id}")
    public MaterialResponse getById(@PathVariable UUID id) {
        return MaterialResponse.from(materialService.getById(id));
    }

    // [목록 + 필터] GET /api/materials?status=AVAILABLE&materialType=LEATHER
    // status, materialType 둘 다 생략 가능 (안 주면 전체 조회)
    @GetMapping
    public List<MaterialResponse> list(
            @RequestParam(required = false) MaterialStatus status,
            @RequestParam(required = false) MaterialType materialType) {
        return materialService.list(status, materialType).stream()
                .map(MaterialResponse::from)
                .toList();
    }

    // [AI 태깅] POST /api/materials/{id}/ai-tag
    // body 없이 그냥 호출만 하면 됨. 이미 저장된 사진 URL을 갖고 AI한테 물어봄.
    @PostMapping("/{id}/ai-tag")
    public MaterialResponse tagWithAi(@PathVariable UUID id) {
        return MaterialResponse.from(materialService.tagWithAi(id));
    }


    // 새 사진을 올릴 수도 있어서 이것도 multipart/form-data.
    // 바꾸고 싶은 key만 보내면 됨 (예: grade만 보내면 grade만 바뀜)
    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MaterialResponse update(@PathVariable UUID id, @ModelAttribute MaterialUpdateRequest request) {
        return MaterialResponse.from(materialService.update(id, request));
    }
}