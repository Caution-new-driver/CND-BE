package com.nextrun.cndbe.domain.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class TemplateSeederTest {

    @Mock
    private TemplateRepository templateRepository;

    @Test
    void 기존_템플릿_ID를_유지하면서_패턴_역할을_동기화한다() {
        UUID existingId = UUID.randomUUID();
        Template existingMiniBag = Template.builder()
                .id(existingId)
                .name("미니백")
                .patternPieces("[]")
                .requiredAccessories("[]")
                .build();
        when(templateRepository.findByName("미니백"))
                .thenReturn(Optional.of(existingMiniBag));
        when(templateRepository.findByName("러기지 태그"))
                .thenReturn(Optional.empty());

        TemplateSeeder seeder = new TemplateSeeder(
                templateRepository,
                JsonMapper.builder().build()
        );
        seeder.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(existingId, existingMiniBag.getId());
        assertTrue(existingMiniBag.getPatternPieces()
                .contains("\"role\":\"MAIN\""));
        assertTrue(existingMiniBag.getPatternPieces()
                .contains("\"role\":\"POINT\""));
        verify(templateRepository).save(existingMiniBag);
        verify(templateRepository).save(argThat(template ->
                "러기지 태그".equals(template.getName())
                        && template.getPatternPieces()
                        .contains("\"role\":\"MAIN\"")
        ));
    }
}
