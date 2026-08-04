package com.nextrun.cndbe.domain.material;

import com.nextrun.cndbe.domain.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 개발·시연용 가짜 소재 데이터를 한 번에 채워넣는 지름길 (b5).
 * material 테이블이 비어있을 때만 1회 삽입 (재시작해도 중복 삽입 안 됨).
 * 정식 등록 경로(b4 POST /api/materials)를 대체하는 게 아니라, 개발 편의를 위한 임시 수단.
 */
@Component
@RequiredArgsConstructor
public class MaterialSeeder implements ApplicationRunner {

    private final MaterialRepository materialRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (materialRepository.count() > 0) {
            return;
        }

        materialRepository.save(sampleMaterial(
                "LTH-001", MaterialType.LEATHER, MaterialColor.BLACK, MaterialPattern.SOLID, MaterialGrade.A,
                300f, 200f, 1.2f, "부드러움", "유연함", 2));
        materialRepository.save(sampleMaterial(
                "LTH-002", MaterialType.LEATHER, MaterialColor.BROWN, MaterialPattern.MONOGRAM, MaterialGrade.B,
                250f, 180f, 1.5f, "약간 거침", "보통", 1));
        materialRepository.save(sampleMaterial(
                "CVS-001", MaterialType.COATED_CANVAS, MaterialColor.BEIGE, MaterialPattern.STRIPE, MaterialGrade.A,
                400f, 300f, 0.8f, "매끈함", "유연함", 3));
        materialRepository.save(sampleMaterial(
                "FAB-001", MaterialType.FABRIC, MaterialColor.RED, MaterialPattern.GEOMETRIC, MaterialGrade.C,
                200f, 150f, 0.5f, "부드러움", "매우 유연함", 1));
        materialRepository.save(sampleMaterial(
                "SYN-001", MaterialType.SYNTHETIC, MaterialColor.WHITE, MaterialPattern.SOLID, MaterialGrade.B,
                350f, 250f, 1.0f, "매끈함", "보통", 2));
    }

    // 반복되는 "소재 하나 만들기" 부분을 헬퍼 메서드로 뽑아서, 위에서 값만 바꿔서 5번 호출
    private Material sampleMaterial(String materialCode, MaterialType materialType, MaterialColor color,
                                    MaterialPattern pattern, MaterialGrade grade, Float widthMm, Float heightMm, Float thicknessMm,
                                    String handFeel, String flexibility, Integer quantity) {
        return Material.builder()
                .materialCode(materialCode)
                .materialType(materialType)
                .color(color)
                .pattern(pattern)
                .grade(grade)
                .widthMm(widthMm)
                .heightMm(heightMm)
                .thicknessMm(thicknessMm)
                .handFeel(handFeel)
                .flexibility(flexibility)
                .quantity(quantity)
                .status(MaterialStatus.AVAILABLE)
                .build();
    }
}