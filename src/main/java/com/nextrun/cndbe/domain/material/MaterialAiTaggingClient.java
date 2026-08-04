package com.nextrun.cndbe.domain.material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

// b6: 소재 사진 2장(전체샷, 클로즈업)을 OpenAI한테 보내서
// 색상·패턴·질감·신뢰도·특이사항을 자동으로 채워달라고 부탁하는 담당자.
@Component
@RequiredArgsConstructor
public class MaterialAiTaggingClient {

    private static final String MODEL = "gpt-5.6-luna";

    private final RestClient openAiRestClient; // OpenAiConfig에서 만들어둔 그 빈(bean)
    private final JsonMapper jsonMapper;        // TemplateSeeder에서 쓰던 것과 같은 도구

    public MaterialAiTagResult tag(String imageUrlFull, String imageUrlCloseup) {

        // 1. OpenAI한테 보낼 요청 내용 만들기
        // Map.of(...)는 null 값을 절대 못 받아서, 사진이 없을 수도 있는 imageUrlCloseup은
        // 미리 다 Map.of에 박아넣지 않고, null이 아닐 때만 하나씩 추가하는 방식으로 만듦.
        List<Object> userContent = new ArrayList<>();
        userContent.add(Map.of("type", "text", "text", "이 소재 사진을 분석해줘."));
        if (imageUrlFull != null) {
            userContent.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrlFull)));
        }
        if (imageUrlCloseup != null) {
            userContent.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrlCloseup)));
        }

        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "너는 원단·가죽 소재 사진을 보고 특징을 분석하는 전문가야. "
                                        + "주어진 색상/패턴 선택지 중에서만 골라 답하고, "
                                        + "특이사항(surfaceNotes)은 100자 이내로 요약해. "
                                        + "특별히 눈에 띄는 게 없으면 빈 문자열로 답해."),
                        Map.of("role", "user", "content", userContent)
                ),
                // 2. "이 형식으로만 답해" 강제하기 (Structured Outputs)
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", "material_tag",
                                "strict", true,
                                "schema", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "color", Map.of("type", "string",
                                                        "enum", List.of("BLACK", "BROWN", "BEIGE", "WHITE", "RED", "BLUE", "MULTI", "OTHER")),
                                                "pattern", Map.of("type", "string",
                                                        "enum", List.of("MONOGRAM", "SOLID", "GEOMETRIC", "STRIPE", "OTHER")),
                                                "texture", Map.of("type", "string"),
                                                "aiConfidence", Map.of("type", "number"),
                                                "surfaceNotes", Map.of("type", "string")
                                        ),
                                        "required", List.of("color", "pattern", "texture", "aiConfidence", "surfaceNotes"),
                                        "additionalProperties", false
                                )
                        )
                )
        );

        // 3. 진짜 요청 보내기
        ChatCompletionResponse response = openAiRestClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(ChatCompletionResponse.class);

        // 4. 응답 안의 JSON 문자열을, 우리가 쓸 수 있는 객체(MaterialAiTagResult)로 변환
        String content = response.choices().get(0).message().content();
        return jsonMapper.readValue(content, MaterialAiTagResult.class);
    }

    // OpenAI가 돌려주는 응답 전체의 모양(껍데기). 우리한테 필요한 부분만 최소한으로 담음.
    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}