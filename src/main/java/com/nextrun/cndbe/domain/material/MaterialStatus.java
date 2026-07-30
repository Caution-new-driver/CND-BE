package com.nextrun.cndbe.domain.material;

// [추측] ERD엔 status가 단순 string으로만 명시되어 실제 상태값 목록은 미확정.
// 등록됨(AVAILABLE) → Drop에서 선택됨(RESERVED) → 제작에 소진됨(DEPLETED)의
// 자연스러운 흐름으로 가정해 정의함. b4(소재 CRUD, 이수현) 작업 시 확정 필요.
public enum MaterialStatus {
	AVAILABLE,
	RESERVED,
	DEPLETED
}
