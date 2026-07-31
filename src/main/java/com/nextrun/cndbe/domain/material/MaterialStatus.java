package com.nextrun.cndbe.domain.material;

// 김재현·팀 확정(2026-07-31): 등록됨(AVAILABLE) → Drop에서 주/포인트 소재로
// 선택됨(RESERVED, b11) → Drop 확정됨(DEPLETED, b13)의 3단계 흐름.
// RESERVED인 소재가 속한 Drop이 재검색되거나 선택이 취소되면 AVAILABLE로 자동 복귀시킬 것.
// quantity(재고 수량)는 지금은 분할 관리하지 않고 한 소재를 통째로 한 Drop에만 배정
// (부분 사용/재고 차감 로직은 MVP 이후 필요해지면 추가 — 스키마 변경 없이 가능).
public enum MaterialStatus {
	AVAILABLE,
	RESERVED,
	DEPLETED
}
