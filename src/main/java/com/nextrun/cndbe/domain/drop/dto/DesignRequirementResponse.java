package com.nextrun.cndbe.domain.drop.dto;

import com.nextrun.cndbe.domain.drop.DesignRequirement;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DesignRequirementResponse {

	private UUID id;
	private UUID dropId;
	private String materialType;
	private String color;
	private String pattern;
	private String minGrade;
	private String accessoryColor;
	private Boolean usePointMaterial;

	public static DesignRequirementResponse from(DesignRequirement requirement) {
		return DesignRequirementResponse.builder()
				.id(requirement.getId())
				.dropId(requirement.getDrop().getId())
				.materialType(requirement.getMaterialType())
				.color(requirement.getColor())
				.pattern(requirement.getPattern())
				.minGrade(requirement.getMinGrade())
				.accessoryColor(requirement.getAccessoryColor())
				.usePointMaterial(requirement.getUsePointMaterial())
				.build();
	}
}
