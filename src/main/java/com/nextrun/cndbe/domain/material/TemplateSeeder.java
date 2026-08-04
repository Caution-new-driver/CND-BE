package com.nextrun.cndbe.domain.material;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 서비스가 켜지기 전에 항상 존재해야 하는 고정 템플릿(미니백/러기지 태그) 시드 데이터.
 * template 테이블이 비어있을 때만 1회 삽입 (재시작해도 중복 삽입 안 됨).
 *
 * [TODO] 아래 widthMm/heightMm는 전부 임시 placeholder 숫자임.
 * 팀이 실제 자투리 원단·가죽을 실측한 뒤 반드시 실측값으로 교체할 것.
 *
 * 단위는 Material 엔티티(widthMm/heightMm)와 맞춰 mm로 통일함 (기존 cm 표기는
 * b12 제작가능성 계산에서 Material과 Template 면적을 비교할 때 단위 불일치를 유발할 수 있어 수정).
 */
@Component
@RequiredArgsConstructor
public class TemplateSeeder implements ApplicationRunner {

	private final TemplateRepository templateRepository;
	private final JsonMapper jsonMapper;

	@Override
	public void run(ApplicationArguments args) {
		if (templateRepository.count() > 0) {
			return;
		}
		templateRepository.save(miniBagTemplate());
		templateRepository.save(luggageTagTemplate());
	}

	private Template miniBagTemplate() {
		List<PatternPieceSeed> patternPieces = List.of(
				new PatternPieceSeed("앞판", 200, 150, 1),
				new PatternPieceSeed("뒷판", 200, 150, 1),
				new PatternPieceSeed("옆판/바닥", 400, 60, 1));
		List<AccessoryRequirementSeed> accessories = List.of(
				new AccessoryRequirementSeed("지퍼", 1),
				new AccessoryRequirementSeed("링", 2));

		return Template.builder()
				.name("미니백")
				.patternPieces(jsonMapper.writeValueAsString(patternPieces))
				.requiredAccessories(jsonMapper.writeValueAsString(accessories))
				.build();
	}

	private Template luggageTagTemplate() {
		List<PatternPieceSeed> patternPieces = List.of(
				new PatternPieceSeed("태그 몸체", 100, 60, 1));
		List<AccessoryRequirementSeed> accessories = List.of(
				new AccessoryRequirementSeed("링", 1));

		return Template.builder()
				.name("러기지 태그")
				.patternPieces(jsonMapper.writeValueAsString(patternPieces))
				.requiredAccessories(jsonMapper.writeValueAsString(accessories))
				.build();
	}

	private record PatternPieceSeed(String pieceName, double widthMm, double heightMm, int quantity) {
	}

	private record AccessoryRequirementSeed(String accessoryType, int quantity) {
	}
}
