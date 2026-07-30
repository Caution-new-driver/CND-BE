package com.nextrun.cndbe.domain.material;

import com.nextrun.cndbe.common.BaseEntity;
import jakarta.persistence.Column;
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

// 미니백/러기지 태그 고정 패턴. 사용자가 만드는 데이터가 아니라 서비스 시작 전
// 시드 데이터로 한 번만 들어가는 상수에 가까움 (b1 나머지 작업: 시드 데이터 삽입).
@Entity
@Table(name = "template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	private String name;

	// [MVP 단순화] jsonb 타입 매핑 라이브러리 없이, JSON 문자열을 TEXT로 저장.
	// 필요한 곳에서 ObjectMapper로 직접 파싱. 패턴 조각 치수/부자재 수량은 읽기 전용.
	@Column(columnDefinition = "TEXT")
	private String patternPieces;

	@Column(columnDefinition = "TEXT")
	private String requiredAccessories;
}
