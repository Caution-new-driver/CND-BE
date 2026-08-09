package com.nextrun.cndbe.domain.material;

import com.nextrun.cndbe.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "accessory",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_accessory_type_color",
                columnNames = {"accessory_type", "color"}
        )
)
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

	@Column(name = "accessory_type", nullable = false)
	private String accessoryType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AccessoryColor color;
}
