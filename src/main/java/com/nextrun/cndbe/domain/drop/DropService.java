package com.nextrun.cndbe.domain.drop;

import com.nextrun.cndbe.domain.material.AccessoryColor;
import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.MaterialType;
import com.nextrun.cndbe.domain.material.Template;
import com.nextrun.cndbe.domain.material.TemplateRepository;

import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DropService {

	private static final String MINI_BAG_TEMPLATE_NAME = "미니백";

	private final DropRepository dropRepository;
	private final DesignRequirementRepository designRequirementRepository;
	private final TemplateRepository templateRepository;

	@Transactional
	public Drop createDraftDrop() {
		Template miniBagTemplate = templateRepository.findByName(MINI_BAG_TEMPLATE_NAME)
				.orElseThrow(() -> new IllegalStateException(
						"'" + MINI_BAG_TEMPLATE_NAME + "' 템플릿 시드 데이터가 없습니다. TemplateSeeder 실행 여부를 확인하세요."));

		Drop drop = Drop.builder()
				.template(miniBagTemplate)
				.status(DropStatus.DRAFT)
				.build();
		return dropRepository.save(drop);
	}

    public List<Drop> list(DropStatus status) {
        if (status != null) {
            return dropRepository.findByStatus(status);
        }
        return dropRepository.findAll();
    }

	@Transactional
	public DesignRequirement saveDesignRequirement(
			java.util.UUID dropId,
			MaterialType materialType,
			MaterialColor color,
			MaterialPattern pattern,
			MaterialGrade minGrade,
			AccessoryColor accessoryColor) {

		Drop drop = dropRepository.findById(dropId)
				.orElseThrow(() -> new NoSuchElementException("존재하지 않는 Drop입니다: " + dropId));

		// f4 분기 B "조건 수정해 다시 검색" 시 재입력 범위가 아직 미확정이라,
		// 재호출 시 기존 것을 덮어쓰는 upsert로 처리 (중복 row 방지)
		DesignRequirement requirement = designRequirementRepository.findByDrop_Id(dropId)
				.orElseGet(() -> DesignRequirement.builder().drop(drop).build());

		requirement.setMaterialType(materialType);
		requirement.setColor(color);
		requirement.setPattern(pattern);
		requirement.setMinGrade(minGrade);
		requirement.setAccessoryColor(accessoryColor);

		return designRequirementRepository.save(requirement);
	}
}
