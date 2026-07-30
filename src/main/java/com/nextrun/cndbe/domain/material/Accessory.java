package com.nextrun.cndbe.domain.material;

import com.nextrun.cndbe.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "accessory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Accessory extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	private String accessoryType;
	private String color;
}
