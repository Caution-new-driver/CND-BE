package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.domain.material.Accessory;
import com.nextrun.cndbe.domain.material.Template;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

// 선택한 부자재 종류가 템플릿의 필수 구성(미니백: 지퍼·링)과 정확히 맞는지 확인함.
@Component
@RequiredArgsConstructor
public class TemplateAccessoryValidator {

    private final JsonMapper jsonMapper;

    public void validate(
            Template template,
            List<Accessory> selectedAccessories
    ) {
        RequiredAccessory[] requirements = readRequirements(template);

        Map<String, Long> selectedTypeCounts = selectedAccessories.stream()
                .map(Accessory::getAccessoryType)
                .filter(StringUtils::hasText)
                .map(this::normalize)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));

        if (selectedTypeCounts.size() != selectedAccessories.size()) {
            throw new IllegalArgumentException(
                    "부자재 종류가 없거나 같은 종류를 중복 선택했습니다."
            );
        }

        Set<String> requiredTypes = Arrays.stream(requirements)
                .map(RequiredAccessory::accessoryType)
                .map(this::normalize)
                .collect(Collectors.toSet());
        Set<String> selectedTypes = selectedTypeCounts.keySet();

        if (!selectedTypes.equals(requiredTypes)) {
            throw new IllegalArgumentException(
                    "템플릿에 필요한 부자재 종류를 모두 선택해야 합니다: "
                            + Arrays.stream(requirements)
                                    .map(requirement ->
                                            requirement.accessoryType()
                                                    + " "
                                                    + requirement.quantity()
                                                    + "개"
                                    )
                                    .collect(Collectors.joining(", "))
            );
        }
    }

    private RequiredAccessory[] readRequirements(Template template) {
        if (template == null
                || !StringUtils.hasText(template.getRequiredAccessories())) {
            throw new IllegalStateException(
                    "템플릿 필수 부자재 정보가 없습니다."
            );
        }

        try {
            RequiredAccessory[] requirements = jsonMapper.readValue(
                    template.getRequiredAccessories(),
                    RequiredAccessory[].class
            );
            if (requirements.length == 0) {
                throw new IllegalStateException(
                        "템플릿 필수 부자재 정보가 비어 있습니다."
                );
            }
            for (RequiredAccessory requirement : requirements) {
                if (!StringUtils.hasText(requirement.accessoryType())
                        || requirement.quantity() == null
                        || requirement.quantity() <= 0) {
                    throw new IllegalStateException(
                            "템플릿 필수 부자재 정보가 올바르지 않습니다."
                    );
                }
            }
            return requirements;

        } catch (IllegalStateException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "템플릿 필수 부자재 정보를 읽을 수 없습니다.",
                    exception
            );
        }
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private record RequiredAccessory(
            String accessoryType,
            Integer quantity
    ) {
    }
}
