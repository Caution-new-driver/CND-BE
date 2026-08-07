package com.nextrun.cndbe.domain.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nextrun.cndbe.domain.material.repository.AccessoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

// 여섯 선택지를 한 번만 만들고 기존 데이터는 중복 저장하지 않는지 검증함.
@ExtendWith(MockitoExtension.class)
class AccessorySeederTest {

    @Mock
    private AccessoryRepository accessoryRepository;

    @InjectMocks
    private AccessorySeeder seeder;

    @Test
    void 없는_지퍼와_링_색상_조합_여섯_개를_등록한다() {
        when(accessoryRepository.existsByAccessoryTypeAndColor(
                any(String.class),
                any(String.class)
        )).thenReturn(false);

        seeder.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<Accessory> captor =
                ArgumentCaptor.forClass(Accessory.class);
        verify(accessoryRepository, org.mockito.Mockito.times(6))
                .save(captor.capture());
        assertEquals(6, captor.getAllValues().size());
    }

    @Test
    void 모든_조합이_이미_있으면_다시_저장하지_않는다() {
        when(accessoryRepository.existsByAccessoryTypeAndColor(
                any(String.class),
                any(String.class)
        )).thenReturn(true);

        seeder.run(new DefaultApplicationArguments(new String[0]));

        verify(accessoryRepository, never()).save(any(Accessory.class));
    }
}
