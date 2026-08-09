package com.nextrun.cndbe.domain.matching;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 과거 후보 삭제와 새 후보 저장이 정해진 순서로 한 작업 안에서 실행되는지 검증한다.
@ExtendWith(MockitoExtension.class)
class MaterialCandidateWriterTest {

    @Mock
    private MaterialCandidateRepository materialCandidateRepository;

    @Test
    void 기존_후보를_삭제한_뒤_새_후보를_저장한다() {
        UUID dropId = UUID.randomUUID();
        List<MaterialCandidate> candidates = List.of(
                MaterialCandidate.builder().rank(1).build()
        );
        when(materialCandidateRepository.saveAll(candidates))
                .thenReturn(candidates);
        MaterialCandidateWriter writer = new MaterialCandidateWriter(
                materialCandidateRepository
        );

        List<MaterialCandidate> saved = writer.replace(dropId, candidates);

        InOrder order = inOrder(materialCandidateRepository);
        order.verify(materialCandidateRepository).deleteAllByDrop_Id(dropId);
        order.verify(materialCandidateRepository).saveAll(candidates);
        assertSame(candidates, saved);
    }
}
