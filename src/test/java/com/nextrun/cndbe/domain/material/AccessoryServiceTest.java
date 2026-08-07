package com.nextrun.cndbe.domain.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.domain.material.dto.AccessoryResponse;
import com.nextrun.cndbe.domain.material.repository.AccessoryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 부자재 목록 API에 전달할 DTO 변환 결과를 DB 없이 검증함.
@ExtendWith(MockitoExtension.class)
class AccessoryServiceTest {

    @Mock
    private AccessoryRepository accessoryRepository;

    @InjectMocks
    private AccessoryService service;

    @Test
    void 등록된_부자재를_종류와_색상_정보로_반환한다() {
        Accessory zipper = Accessory.builder()
                .id(UUID.randomUUID())
                .accessoryType("지퍼")
                .color("GOLD")
                .build();
        Accessory ring = Accessory.builder()
                .id(UUID.randomUUID())
                .accessoryType("링")
                .color("GOLD")
                .build();
        when(accessoryRepository.findAllByOrderByAccessoryTypeAscColorAsc())
                .thenReturn(List.of(ring, zipper));

        List<AccessoryResponse> response = service.getAccessories();

        assertEquals(2, response.size());
        assertEquals(ring.getId(), response.get(0).id());
        assertEquals("링", response.get(0).accessoryType());
        assertEquals("GOLD", response.get(0).color());
    }
}
