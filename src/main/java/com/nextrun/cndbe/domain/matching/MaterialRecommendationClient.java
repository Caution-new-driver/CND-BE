package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.domain.drop.DesignRequirement;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.MaterialType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

// b10: 백엔드가 뽑은 최대 3개 후보를 OpenAI에 전달해
// 한국어 추천 이유와 주의사항만 받아오는 외부 API 담당자.
@Component
@RequiredArgsConstructor
public class MaterialRecommendationClient {

    private static final String MODEL = "gpt-5.6-terra";

    private final RestClient openAiRestClient;
    private final JsonMapper jsonMapper;

    public MaterialRecommendationResult recommend(
            DesignRequirement requirement,
            List<MaterialCandidate> candidates
    ) {
        // 필수조건을 통과한 소재가 없으면 OpenAI를 호출할 필요 없이 빈 결과를 반환.
        if (candidates.isEmpty()) {
            return new MaterialRecommendationResult(List.of());
        }

        String prompt = buildPrompt(requirement, candidates);

        return requestRecommendations(prompt, candidates);
    }

    private String buildPrompt(
            DesignRequirement requirement,
            List<MaterialCandidate> candidates
    ) {
        // 디자인 조건과 후보 정보를 JSON 문자열로 만들어 AI가 비교하기 쉽게 전달.
        RecommendationPrompt prompt = new RecommendationPrompt(
                new RequirementPrompt(
                        requirement.getMaterialType(),
                        requirement.getColor(),
                        requirement.getPattern(),
                        requirement.getMinGrade()
                ),
                candidates.stream()
                        .map(this::toCandidatePrompt)
                        .toList()
        );

        return jsonMapper.writeValueAsString(prompt);
    }

    private CandidatePrompt toCandidatePrompt(
            MaterialCandidate candidate
    ) {
        Material material = candidate.getMaterial();

        return new CandidatePrompt(
                material.getId(),
                material.getMaterialCode(),
                material.getMaterialType().name(),
                material.getColor().name(),
                material.getPattern().name(),
                material.getGrade().name(),
                material.getTexture(),
                material.getSurfaceNotes(),
                candidate.getMatchScore(),
                candidate.getRank()
        );
    }

    private MaterialRecommendationResult requestRecommendations(
            String prompt,
            List<MaterialCandidate> candidates
    ) {
        int candidateCount = candidates.size();

        try {
            // 1. AI의 역할을 제한하고, 순위·점수는 수정하지 말라고 명시.
            Map<String, Object> requestBody = Map.of(
                    "model", MODEL,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content",
                                    """
                                    너는 럭셔리 업사이클링 소재 추천 전문가다.
                                    입력된 후보의 점수와 순위는 변경하지 않는다.
                                    각 후보에 대해 한국어 추천 이유와 주의사항만 작성한다.
                                    추천 이유와 주의사항은 각각 100자 이내로 작성한다.
                                    입력받은 materialId를 그대로 반환한다.
                                    제작 가능 수량은 계산하지 않는다.
                                    """
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    ),
                    "response_format",
                    // 2. Structured Outputs로 후보 수와 응답 필드를 강제함.
                    createResponseFormat(candidateCount)
            );

            // 3. OpenAI Chat Completions API에 실제 요청 전송.
            ChatCompletionResponse response =
                    openAiRestClient.post()
                            .uri("/chat/completions")
                            .body(requestBody)
                            .retrieve()
                            .body(ChatCompletionResponse.class);

            String content = extractContent(response);

            // 4. 응답 JSON 문자열을 자바 객체로 변환하고 후보 ID가 맞는지 검증.
            MaterialRecommendationResult result =
                    jsonMapper.readValue(
                            content,
                            MaterialRecommendationResult.class
                    );

            validateRecommendations(result, candidates);

            return result;

        } catch (IllegalStateException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "AI 추천 이유 생성에 실패했습니다.",
                    exception
            );
        }
    }

    private Map<String, Object> createResponseFormat(
            int candidateCount
    ) {
        return Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "material_recommendations",
                        "strict", true,
                        "schema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "recommendations", Map.of(
                                                "type", "array",
                                                "minItems", candidateCount,
                                                "maxItems", candidateCount,
                                                "items", Map.of(
                                                        "type", "object",
                                                        "properties", Map.of(
                                                                "materialId", Map.of(
                                                                        "type", "string"
                                                                ),
                                                                "aiReasons", Map.of(
                                                                        "type", "string",
                                                                        "maxLength", 100
                                                                ),
                                                                "aiCautions", Map.of(
                                                                        "type", "string",
                                                                        "maxLength", 100
                                                                )
                                                        ),
                                                        "required", List.of(
                                                                "materialId",
                                                                "aiReasons",
                                                                "aiCautions"
                                                        ),
                                                        "additionalProperties", false
                                                )
                                        )
                                ),
                                "required", List.of("recommendations"),
                                "additionalProperties", false
                        )
                )
        );
    }

    private String extractContent(
            ChatCompletionResponse response
    ) {
        // 응답 껍데기 안에 실제 JSON 문자열이 없으면 잘못된 AI 응답으로 처리.
        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()
                || response.choices().get(0).message() == null
                || !StringUtils.hasText(
                response.choices().get(0).message().content()
        )) {
            throw new IllegalStateException(
                    "OpenAI가 추천 결과를 반환하지 않았습니다."
            );
        }

        return response.choices()
                .get(0)
                .message()
                .content();
    }

    private void validateRecommendations(
            MaterialRecommendationResult result,
            List<MaterialCandidate> candidates
    ) {
        // 후보 누락·중복·다른 materialId 반환을 막아서 잘못된 설명이
        // 엉뚱한 소재에 저장되지 않도록 방어함.
        if (result == null || result.recommendations() == null) {
            throw new IllegalStateException(
                    "AI 추천 결과가 올바르지 않습니다."
            );
        }

        if (result.recommendations().size() != candidates.size()) {
            throw new IllegalStateException(
                    "AI 추천 결과 개수가 후보 개수와 일치하지 않습니다."
            );
        }

        Set<UUID> expectedMaterialIds =
                candidates.stream()
                        .map(candidate ->
                                candidate.getMaterial().getId()
                        )
                        .collect(Collectors.toSet());

        Set<UUID> returnedMaterialIds =
                result.recommendations().stream()
                        .map(MaterialRecommendationResult.Recommendation::materialId)
                        .collect(Collectors.toSet());

        if (!expectedMaterialIds.equals(returnedMaterialIds)) {
            throw new IllegalStateException(
                    "AI 추천 결과의 소재 ID가 요청한 후보와 일치하지 않습니다."
            );
        }
    }

    private record ChatCompletionResponse(
            List<Choice> choices
    ) {
        // OpenAI 응답 전체 중 우리에게 필요한 choices 부분만 담는 최소 껍데기.
    }

    private record Choice(
            Message message
    ) {
    }

    private record Message(
            String content
    ) {
    }

    private record RecommendationPrompt(
            RequirementPrompt designRequirement,
            List<CandidatePrompt> candidates
    ) {
        // OpenAI에 보내는 디자인 조건과 후보 목록의 전체 모양.
    }

    private record RequirementPrompt(
            MaterialType materialType,
            MaterialColor color,
            MaterialPattern pattern,
            MaterialGrade minGrade
    ) {
    }

    private record CandidatePrompt(
            UUID materialId,
            String materialCode,
            String materialType,
            String color,
            String pattern,
            String grade,
            String texture,
            String surfaceNotes,
            Integer matchScore,
            Integer rank
    ) {
    }
}
