package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.MaterialType;
import com.nextrun.cndbe.domain.production.ProductType;
import com.nextrun.cndbe.domain.production.ProductionScenarioItem;
import com.nextrun.cndbe.domain.production.ScenarioType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

// b14: 확정된 Drop 정보(이름·소재·제작 수량)를 OpenAI에 전달해
// 한국어 소개문 초안을 받아오는 외부 API 담당자.
@Component
@RequiredArgsConstructor
public class DropIntroTextClient {

    private static final String MODEL = "gpt-5.6-terra";
    private static final int MAX_LENGTH = 300;

    private final RestClient openAiRestClient;
    private final JsonMapper jsonMapper;

    public String generate(
            Drop drop,
            Material mainMaterial,
            Material pointMaterial,
            ScenarioType scenarioType,
            List<ProductionScenarioItem> items
    ) {
        String prompt = buildPrompt(drop, mainMaterial, pointMaterial, scenarioType, items);
        return requestIntroText(prompt);
    }

    private String buildPrompt(
            Drop drop,
            Material mainMaterial,
            Material pointMaterial,
            ScenarioType scenarioType,
            List<ProductionScenarioItem> items
    ) {
        IntroTextPrompt prompt = new IntroTextPrompt(
                drop.getName(),
                drop.getTemplate().getName(),
                scenarioType,
                toMaterialPrompt(mainMaterial),
                pointMaterial == null ? null : toMaterialPrompt(pointMaterial),
                items.stream().map(this::toProductPrompt).toList()
        );
        return jsonMapper.writeValueAsString(prompt);
    }

    private MaterialPrompt toMaterialPrompt(Material material) {
        return new MaterialPrompt(
                material.getMaterialType(),
                material.getColor(),
                material.getPattern(),
                material.getGrade()
        );
    }

    private ProductPrompt toProductPrompt(ProductionScenarioItem item) {
        return new ProductPrompt(item.getProductType(), item.getQuantity());
    }

    private String requestIntroText(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", MODEL,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content",
                                    """
                                    너는 럭셔리 업사이클링 브랜드의 카피라이터다.
                                    주어진 사실(소재, 색상, 패턴, 등급, 제작 수량)만 사용해 한국어 소개문을 작성한다.
                                    없는 사실을 지어내지 않는다.
                                    소개문은 %d자 이내로 작성한다.
                                    """.formatted(MAX_LENGTH)
                            ),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "response_format", createResponseFormat()
            );

            ChatCompletionResponse response = openAiRestClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            String content = extractContent(response);
            DropIntroTextResult result = jsonMapper.readValue(content, DropIntroTextResult.class);

            if (result == null || !StringUtils.hasText(result.introText())) {
                throw new IllegalStateException("AI 소개문 생성 결과가 올바르지 않습니다.");
            }
            return result.introText();

        } catch (IllegalStateException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("AI 소개문 생성에 실패했습니다.", exception);
        }
    }

    private Map<String, Object> createResponseFormat() {
        return Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "drop_intro_text",
                        "strict", true,
                        "schema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "introText", Map.of(
                                                "type", "string",
                                                "maxLength", MAX_LENGTH
                                        )
                                ),
                                "required", List.of("introText"),
                                "additionalProperties", false
                        )
                )
        );
    }

    private String extractContent(ChatCompletionResponse response) {
        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()
                || response.choices().get(0).message() == null
                || !StringUtils.hasText(response.choices().get(0).message().content())) {
            throw new IllegalStateException("OpenAI가 소개문을 반환하지 않았습니다.");
        }
        return response.choices().get(0).message().content();
    }

    private record IntroTextPrompt(
            String dropName,
            String templateName,
            ScenarioType scenarioType,
            MaterialPrompt mainMaterial,
            MaterialPrompt pointMaterial,
            List<ProductPrompt> products
    ) {
    }

    private record MaterialPrompt(
            MaterialType materialType,
            MaterialColor color,
            MaterialPattern pattern,
            MaterialGrade grade
    ) {
    }

    private record ProductPrompt(ProductType productType, int quantity) {
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}
