package com.nextrun.cndbe.domain.matching;

import com.nextrun.cndbe.common.BaseEntity;
import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.material.Accessory;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

// 어떤 부자재(지퍼/링 색상 등)를 선택했는지 기록하는 연결 테이블
@Entity
@Table(name = "drop_accessory_selection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DropAccessorySelection extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "drop_id")
	private Drop drop;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "accessory_id")
	private Accessory accessory;
}
