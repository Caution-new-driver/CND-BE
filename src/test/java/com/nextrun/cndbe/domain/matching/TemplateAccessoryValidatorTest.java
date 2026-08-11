package com.nextrun.cndbe.domain.matching;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nextrun.cndbe.domain.material.Accessory;
import com.nextrun.cndbe.domain.material.AccessoryColor;
import com.nextrun.cndbe.domain.material.Template;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

// 템플릿의 필수 종류는 모두 있어야 하고, 누락·중복·불필요한 종류는 막는지 검증함.
class TemplateAccessoryValidatorTest {

    private final TemplateAccessoryValidator validator =
            new TemplateAccessoryValidator(JsonMapper.builder().build());

    private final Template miniBagTemplate = Template.builder()
            .requiredAccessories(
                    """
                    [
                      {"accessoryType":"지퍼","quantity":1},
                      {"accessoryType":"링","quantity":2}
                    ]
                    """
            )
            .build();

    @Test
    void 지퍼와_링을_각각_선택하면_통과한다() {
        assertDoesNotThrow(() -> validator.validate(
                miniBagTemplate,
                List.of(
                        accessory("지퍼", AccessoryColor.GOLD),
                        accessory("링", AccessoryColor.GOLD)
                ),
                AccessoryColor.GOLD
        ));
    }

    @Test
    void 필수_부자재가_빠지면_예외가_발생한다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        miniBagTemplate,
                        List.of(accessory("지퍼", AccessoryColor.GOLD)),
                        AccessoryColor.GOLD
                )
        );
    }

    @Test
    void 같은_종류를_서로_다른_색상으로_중복_선택할_수_없다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        miniBagTemplate,
                        List.of(
                                accessory("지퍼", AccessoryColor.GOLD),
                                accessory("지퍼", AccessoryColor.SILVER),
                                accessory("링", AccessoryColor.GOLD)
                        ),
                        AccessoryColor.GOLD
                )
        );
    }

    @Test
    void 템플릿_부자재_JSON이_잘못되면_예외가_발생한다() {
        Template broken = Template.builder()
                .requiredAccessories("not-json")
                .build();

        assertThrows(
                IllegalStateException.class,
                () -> validator.validate(
                        broken,
                        List.of(),
                        AccessoryColor.GOLD
                )
        );
    }

    @Test
    void 서로_다른_색상의_부자재를_한_세트로_선택할_수_없다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        miniBagTemplate,
                        List.of(
                                accessory("지퍼", AccessoryColor.GOLD),
                                accessory("링", AccessoryColor.BLACK)
                        ),
                        null
                )
        );
    }

    @Test
    void 디자인_조건과_다른_색상의_세트를_선택할_수_없다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        miniBagTemplate,
                        List.of(
                                accessory("지퍼", AccessoryColor.SILVER),
                                accessory("링", AccessoryColor.SILVER)
                        ),
                        AccessoryColor.GOLD
                )
        );
    }

    private Accessory accessory(String type, AccessoryColor color) {
        return Accessory.builder()
                .id(UUID.randomUUID())
                .accessoryType(type)
                .color(color)
                .build();
    }
}
