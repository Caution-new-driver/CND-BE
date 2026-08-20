package com.nextrun.cndbe.domain.drop;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

// b14: 확정된 Drop 정보(이름·소재·제작 수량)를 OpenAI에 전달해
// 한국어 소개문 초안을 받아오는 외부 API 담당자.
// b13 확정 트랜잭션이 끝난 뒤(락 없이) 호출되므로, JPA 엔티티가 아니라
// DropConfirmationWriter가 트랜잭션 안에서 미리 꺼내둔 순수 값(DropIntroTextPromptData)만 받는다.
@Component
@RequiredArgsConstructor
public class DropIntroTextClient {

    private static final String MODEL = "gpt-5.6-terra";
    private static final int MAX_LENGTH = 300;
    // 이 호출 한 번 자체가 재생성 시도 횟수를 소모한다(reserveRegenerationAttempt 참고).
    // 일시적인 타임아웃/빈 응답으로 사용자가 시도 횟수만 날리는 걸 줄이기 위해 실패 시 한 번만
    // 더 시도한다 — 그 이상 재시도하면 오히려 유효한 실패(잘못된 요청 등)까지 늦게 확정된다.
    private static final int MAX_ATTEMPTS = 2;

    private final RestClient openAiRestClient;
    private final JsonMapper jsonMapper;

    public String generate(DropIntroTextPromptData promptData) {
        String prompt = jsonMapper.writeValueAsString(promptData);
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return requestIntroText(prompt);
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }
        throw lastFailure;
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
                                    주어진 사실(Drop 이름, 템플릿, 소재 종류·색상·패턴·등급, 제품별 제작 수량)만 사용해 한국어 소개문을 작성한다.
                                    없는 사실을 지어내지 않는다 — 특히 수치(제작 수량), 소재 정보, 등급을 절대 임의로 바꾸거나 부풀리지 않는다.

                                    소비자 머릿속에 남는 소개문을 쓰기 위해 다음을 지킨다:
                                    1. 첫 문장에서 바로 시선을 붙잡는다. "이 가방은...", "저희 브랜드는..." 같은 상투적 도입으로 시작하지 않는다.
                                    2. 제작 수량이 이 소재가 가진 진짜 한계라는 점을 희소성으로 활용한다. "단 N개만 제작됩니다"처럼 주어진 숫자를 그대로 정확히 써서 강조하고, "한정판"처럼 숫자를 뭉개서 모호하게 표현하지 않는다.
                                    3. 주어진 소재 등급·패턴 같은 감각적 사실을 구체적으로 묘사해 소재가 손에 잡히듯 느껴지게 한다. 근거 없는 형용사("최고급의", "완벽한")는 쓰지 않는다.
                                    4. "당신만을 위한", "특별한 순간을 위한", "시간을 초월한" 같은 뻔한 럭셔리 클리셰 문구는 피한다.
                                    5. 마지막 문장은 여운을 남기는 한 줄로 끝맺는다.

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

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}
