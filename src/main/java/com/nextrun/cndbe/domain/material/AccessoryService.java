package com.nextrun.cndbe.domain.material;

import com.nextrun.cndbe.domain.material.dto.AccessoryResponse;
import com.nextrun.cndbe.domain.material.repository.AccessoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 프론트가 임의 문자열을 보내지 않고 DB에 등록된 부자재 중 하나를 고를 수 있게 목록을 제공함.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessoryService {

    private final AccessoryRepository accessoryRepository;

    public List<AccessoryResponse> getAccessories() {
        return accessoryRepository
                .findAllByOrderByAccessoryTypeAscColorAsc()
                .stream()
                .map(AccessoryResponse::from)
                .toList();
    }
}
