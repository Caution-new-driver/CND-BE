package com.nextrun.cndbe.domain.preorder;

import com.nextrun.cndbe.common.BaseEntity;
import com.nextrun.cndbe.domain.drop.Drop;
import com.nextrun.cndbe.domain.production.ProductType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

// 고객의 신청 한 건 = 로우 하나. 신청 1건당 수량 1개 고정(v4 확정 사항)이라 수량 필드 없음.
// assignedNumber는 신청 시점엔 비어있다가, 신청 확정 후 채워짐.
@Entity
@Table(name = "preorder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Preorder extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "drop_id")
	private Drop drop;

	@Enumerated(EnumType.STRING)
	private ProductType productType;

	private String customerName;
	private String customerContact;
	private Integer assignedNumber;

	// [추측] b17/b18(신청 저장·자동 마감, 이수현) 구현 시 실제 상태값 목록 확정 필요.
	// 현재 문서엔 "성공하면 바로 신청 완료"만 명시돼 있고 별도 상태 흐름이 정의돼
	// 있지 않아, 우선 자유 문자열로 남겨둠 (예: CONFIRMED 하나만 쓰게 될 수도 있음).
	private String status;
}
