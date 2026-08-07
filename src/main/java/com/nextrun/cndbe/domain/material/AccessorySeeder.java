package com.nextrun.cndbe.domain.material;

import com.nextrun.cndbe.domain.material.repository.AccessoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 디자이너가 f4에서 고를 수 있는 고정 부자재 선택지를 준비함.
 * AI가 고르는 값이 아니라 메뉴판 역할의 시드 데이터이며, 최종 선택은 항상 사용자가 수행함.
 */
@Component
@RequiredArgsConstructor
public class AccessorySeeder implements ApplicationRunner {

    private static final List<String> ACCESSORY_TYPES =
            List.of("지퍼", "링");
    private static final List<String> COLORS =
            List.of("GOLD", "SILVER", "BLACK");

    private final AccessoryRepository accessoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (String accessoryType : ACCESSORY_TYPES) {
            for (String color : COLORS) {
                saveIfMissing(accessoryType, color);
            }
        }
    }

    private void saveIfMissing(String accessoryType, String color) {
        if (accessoryRepository.existsByAccessoryTypeAndColor(
                accessoryType,
                color
        )) {
            return;
        }

        accessoryRepository.save(Accessory.builder()
                .accessoryType(accessoryType)
                .color(color)
                .build());
    }
}
