package com.nextrun.cndbe.domain.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.MaterialType;
import com.nextrun.cndbe.domain.material.Template;
import com.nextrun.cndbe.domain.material.TemplateRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Drop 단건 조회와 디자인 조건 저장(f3)의 확정 상태 보호를 확인함.
@ExtendWith(MockitoExtension.class)
class DropServiceTest {

	@Mock
	private DropRepository dropRepository;

	@Mock
	private DesignRequirementRepository designRequirementRepository;

	@Mock
	private TemplateRepository templateRepository;

	@InjectMocks
	private DropService service;

	private UUID dropId;
	private Drop drop;

	@BeforeEach
	void setUp() {
		dropId = UUID.randomUUID();
		drop = Drop.builder()
				.id(dropId)
				.template(Template.builder().build())
				.status(DropStatus.DRAFT)
				.build();
	}

	@Test
	void 단건_조회는_존재하는_Drop을_반환한다() {
		when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));

		assertEquals(drop, service.get(dropId));
	}

	@Test
	void 존재하지_않는_Drop을_단건_조회하면_404_예외를_던진다() {
		when(dropRepository.findById(dropId)).thenReturn(Optional.empty());

		assertThrows(NoSuchElementException.class, () -> service.get(dropId));
	}

	@Test
	void 확정된_Drop의_디자인_조건은_수정할_수_없다() {
		drop.setStatus(DropStatus.CONFIRMED);
		when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));

		assertThrows(
				IllegalStateException.class,
				() -> service.saveDesignRequirement(
						dropId,
						MaterialType.LEATHER,
						MaterialColor.BLACK,
						MaterialPattern.SOLID,
						MaterialGrade.A
				)
		);
	}

	@Test
	void DRAFT_Drop의_디자인_조건은_upsert로_저장된다() {
		when(dropRepository.findById(dropId)).thenReturn(Optional.of(drop));
		when(designRequirementRepository.findByDrop_Id(dropId)).thenReturn(Optional.empty());
		when(designRequirementRepository.save(org.mockito.ArgumentMatchers.any(DesignRequirement.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		DesignRequirement result = service.saveDesignRequirement(
				dropId,
				MaterialType.LEATHER,
				MaterialColor.BLACK,
				MaterialPattern.SOLID,
				MaterialGrade.A
		);

		assertEquals(MaterialType.LEATHER, result.getMaterialType());
	}
}
