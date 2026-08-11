package com.nextrun.cndbe.domain.material;

import com.nextrun.cndbe.common.calculation.PatternPieceRole;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 서비스가 켜지기 전에 항상 존재해야 하는 고정 템플릿(미니백/러기지 태그) 시드 데이터.
 * 이름으로 기존 데이터를 찾아 ID는 유지하고, 고정 JSON 값은 최신 정의와 동기화한다.
 *
 * 아래 widthMm/heightMm는 MVP에서 합의한 고정 템플릿 치수다.
 * 실제 생산에 적용하기 전에는 패턴 실측값과 다시 대조해야 한다.
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
		synchronizeTemplate(miniBagTemplate());
		synchronizeTemplate(luggageTagTemplate());
	}

	// 기존 Drop이 참조하는 Template ID는 유지하면서 고정 JSON 데이터만 최신 값으로 맞춘다.
	// 따라서 이미 데이터가 있는 Neon DB에도 새 role 필드가 애플리케이션 시작 시 반영된다.
	private void synchronizeTemplate(Template desired) {
		templateRepository.findByName(desired.getName())
				.ifPresentOrElse(existing -> {
					if (Objects.equals(existing.getPatternPieces(), desired.getPatternPieces())
							&& Objects.equals(existing.getRequiredAccessories(), desired.getRequiredAccessories())) {
						return;
					}
					existing.setPatternPieces(desired.getPatternPieces());
					existing.setRequiredAccessories(desired.getRequiredAccessories());
					templateRepository.save(existing);
				}, () -> templateRepository.save(desired));
	}

	private Template miniBagTemplate() {
		List<PatternPieceSeed> patternPieces = List.of(
				new PatternPieceSeed("앞판", 200, 150, 1, PatternPieceRole.MAIN),
				new PatternPieceSeed("뒷판", 200, 150, 1, PatternPieceRole.MAIN),
				new PatternPieceSeed("옆판/바닥", 400, 60, 1, PatternPieceRole.POINT));
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
				new PatternPieceSeed("태그 몸체", 100, 60, 1, PatternPieceRole.MAIN));
		List<AccessoryRequirementSeed> accessories = List.of(
				new AccessoryRequirementSeed("링", 1));

		return Template.builder()
				.name("러기지 태그")
				.patternPieces(jsonMapper.writeValueAsString(patternPieces))
				.requiredAccessories(jsonMapper.writeValueAsString(accessories))
				.build();
	}

	private record PatternPieceSeed(
			String pieceName,
			double widthMm,
			double heightMm,
			int quantity,
			PatternPieceRole role
	) {
	}

	private record AccessoryRequirementSeed(String accessoryType, int quantity) {
	}
}
