package com.asg.finance.pdcchqbatch.repository;

import com.asg.finance.entity.PdcChqBatchHdrEntity;
import com.asg.finance.repository.PdcChqBatchHdrRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdcChqBatchHdrRepositoryTest {

    @Mock
    private PdcChqBatchHdrRepository repository;

    private PdcChqBatchHdrEntity entity;

    @BeforeEach
    void setUp() {
        entity = PdcChqBatchHdrEntity.builder()
                .transactionPoid(999L)
                .transactionDate(LocalDate.of(2026, 4, 1).atStartOfDay())
                .docRef("PDC-001")
                .payGlPoid(101L)
                .payingTo("Vendor A")
                .bankPoid(202L)
                .chqStartNo("123456")
                .chqStartDate(LocalDate.of(2026, 4, 1))
                .chqAmount(1000.0)
                .noOfChqs(1L)
                .totalAmount(1000.0)
                .narration("Test")
                .deleted("N")
                .build();
    }

    @Test
    void findById_Success() {
        when(repository.findById(999L)).thenReturn(Optional.of(entity));

        Optional<PdcChqBatchHdrEntity> result = repository.findById(999L);

        assertThat(result).isPresent();
        assertThat(result.get().getDocRef()).isEqualTo("PDC-001");
        verify(repository).findById(999L);
    }

    @Test
    void findById_NotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        Optional<PdcChqBatchHdrEntity> result = repository.findById(404L);

        assertThat(result).isEmpty();
        verify(repository).findById(404L);
    }

    @Test
    void existsByTransactionPoid_True() {
        when(repository.existsByTransactionPoid(999L)).thenReturn(true);

        boolean result = repository.existsByTransactionPoid(999L);

        assertThat(result).isTrue();
        verify(repository).existsByTransactionPoid(999L);
    }

    @Test
    void existsByTransactionPoid_False() {
        when(repository.existsByTransactionPoid(404L)).thenReturn(false);

        boolean result = repository.existsByTransactionPoid(404L);

        assertThat(result).isFalse();
        verify(repository).existsByTransactionPoid(404L);
    }

    @Test
    void save_Success() {
        when(repository.save(entity)).thenReturn(entity);

        PdcChqBatchHdrEntity saved = repository.save(entity);

        assertThat(saved.getTransactionPoid()).isEqualTo(999L);
        assertThat(saved.getPayingTo()).isEqualTo("Vendor A");
        verify(repository).save(entity);
    }
}
