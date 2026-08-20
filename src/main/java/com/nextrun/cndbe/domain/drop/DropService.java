package com.nextrun.cndbe.domain.drop;

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
            return dropRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return dropRepository.findAllByOrderByCreatedAtDesc();
    }

    // 단건 조회. 프론트가 "이 Drop이 CONFIRMED인지"를 진입 경로와 무관하게 항상 알 수 있도록
    // (예: FlowFrame의 탭 비활성화, f6 재진입 시 확정 상태 복원) 쓰인다.
    public Drop get(java.util.UUID dropId) {
        return dropRepository.findById(dropId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 Drop입니다: " + dropId));
    }

	// "이어서 제작" 재진입 시 f3 폼을 채우기 위한 조회. 아직 저장한 적 없으면 404.
	public DesignRequirement getDesignRequirement(java.util.UUID dropId) {
		return designRequirementRepository.findByDrop_Id(dropId)
				.orElseThrow(() -> new NoSuchElementException(
						"디자인 조건을 찾을 수 없습니다: " + dropId));
	}

	@Transactional
	public DesignRequirement saveDesignRequirement(
			java.util.UUID dropId,
			MaterialType materialType,
			MaterialColor color,
			MaterialPattern pattern,
			MaterialGrade minGrade) {

		Drop drop = dropRepository.findById(dropId)
				.orElseThrow(() -> new NoSuchElementException("존재하지 않는 Drop입니다: " + dropId));

		// 다른 하위 단계(소재 선택·부자재 선택·제작안 선택)는 이미 확정된 Drop의 수정을
		// 막고 있는데, 여기만 빠져있으면 탭바를 확정 후에도 클릭해 조건을 몰래 바꿀 수 있다.
		if (drop.getStatus() != DropStatus.DRAFT) {
			throw new IllegalStateException("확정된 Drop의 디자인 조건은 변경할 수 없습니다.");
		}

		// f4 분기 B "조건 수정해 다시 검색" 시 재입력 범위가 아직 미확정이라,
		// 재호출 시 기존 것을 덮어쓰는 upsert로 처리 (중복 row 방지)
		DesignRequirement requirement = designRequirementRepository.findByDrop_Id(dropId)
				.orElseGet(() -> DesignRequirement.builder().drop(drop).build());

		requirement.setMaterialType(materialType);
		requirement.setColor(color);
		requirement.setPattern(pattern);
		requirement.setMinGrade(minGrade);

		return designRequirementRepository.save(requirement);
	}
}
