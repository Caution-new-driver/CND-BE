package com.nextrun.cndbe.domain.drop.dto;

import com.nextrun.cndbe.domain.drop.DesignRequirement;
import com.nextrun.cndbe.domain.material.AccessoryColor;
import com.nextrun.cndbe.domain.material.MaterialColor;
import com.nextrun.cndbe.domain.material.MaterialGrade;
import com.nextrun.cndbe.domain.material.MaterialPattern;
import com.nextrun.cndbe.domain.material.MaterialType;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DesignRequirementResponse {

	private UUID id;
	private UUID dropId;
	private MaterialType materialType;
	private MaterialColor color;
	private MaterialPattern pattern;
	private MaterialGrade minGrade;
	private AccessoryColor accessoryColor;

	public static DesignRequirementResponse from(DesignRequirement requirement) {
		return DesignRequirementResponse.builder()
				.id(requirement.getId())
				.dropId(requirement.getDrop().getId())
				.materialType(requirement.getMaterialType())
				.color(requirement.getColor())
				.pattern(requirement.getPattern())
				.minGrade(requirement.getMinGrade())
				.accessoryColor(requirement.getAccessoryColor())
				.build();
	}
}
