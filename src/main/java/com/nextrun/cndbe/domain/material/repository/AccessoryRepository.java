package com.nextrun.cndbe.domain.material.repository;

import com.nextrun.cndbe.domain.material.Accessory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// DB에 등록된 부자재를 조회하고 b11 선택 결과와 연결할 때 사용하는 창구.
public interface AccessoryRepository extends JpaRepository<Accessory, UUID> {

    // f4 선택 목록이 종류와 색상 순서로 일정하게 보이도록 정렬해서 조회함.
    List<Accessory> findAllByOrderByAccessoryTypeAscColorAsc();

    // Seeder가 서버 재시작 때 같은 종류·색상 조합을 중복으로 만들지 않도록 확인함.
    boolean existsByAccessoryTypeAndColor(
            String accessoryType,
            String color
    );
}
