package com.asg.finance.pdcchqbatch.repository;

import com.asg.finance.entity.PdcChqBatchDtlEntity;
import com.asg.finance.repository.PdcChqBatchDtlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdcChqBatchDtlRepositoryTest {

    @Mock
    private PdcChqBatchDtlRepository repository;

    private PdcChqBatchDtlEntity detail1;
    private PdcChqBatchDtlEntity detail2;

    @BeforeEach
    void setUp() {
        detail1 = PdcChqBatchDtlEntity.builder()
                .transactionPoid(999L)
                .detRowId(1L)
                .pdcChqDate(LocalDate.of(2026, 4, 1))
                .chqNumber("100001")
                .chqAmount(1000.0)
                .drGlPoid1(11L)
                .drAmt1(1000.0)
                .crGlPoid(21L)
                .crAmt(1000.0)
                .build();

        detail2 = PdcChqBatchDtlEntity.builder()
                .transactionPoid(999L)
                .detRowId(2L)
                .pdcChqDate(LocalDate.of(2026, 4, 2))
                .chqNumber("100002")
                .chqAmount(1000.0)
                .drGlPoid1(12L)
                .drAmt1(1000.0)
                .crGlPoid(22L)
                .crAmt(1000.0)
                .build();
    }

    @Test
    void findTopByTransactionPoidOrderByDetRowIdDesc_Success() {
        when(repository.findTopByTransactionPoidOrderByDetRowIdDesc(999L)).thenReturn(detail2);

        PdcChqBatchDtlEntity result = repository.findTopByTransactionPoidOrderByDetRowIdDesc(999L);

        assertThat(result.getDetRowId()).isEqualTo(2L);
        verify(repository).findTopByTransactionPoidOrderByDetRowIdDesc(999L);
    }

    @Test
    void findByTransactionPoidOrderByDetRowIdAsc_Success() {
        when(repository.findByTransactionPoidOrderByDetRowIdAsc(999L)).thenReturn(List.of(detail1, detail2));

        List<PdcChqBatchDtlEntity> result = repository.findByTransactionPoidOrderByDetRowIdAsc(999L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDetRowId()).isEqualTo(1L);
        assertThat(result.get(1).getDetRowId()).isEqualTo(2L);
        verify(repository).findByTransactionPoidOrderByDetRowIdAsc(999L);
    }

    @Test
    void findByTransactionPoidOrderByDetRowIdAsc_Empty() {
        when(repository.findByTransactionPoidOrderByDetRowIdAsc(404L)).thenReturn(List.of());

        List<PdcChqBatchDtlEntity> result = repository.findByTransactionPoidOrderByDetRowIdAsc(404L);

        assertThat(result).isEmpty();
        verify(repository).findByTransactionPoidOrderByDetRowIdAsc(404L);
    }

    @Test
    void save_Success() {
        when(repository.save(detail1)).thenReturn(detail1);

        PdcChqBatchDtlEntity saved = repository.save(detail1);

        assertThat(saved.getChqNumber()).isEqualTo("100001");
        verify(repository).save(detail1);
    }

    @Test
    void deleteByTransactionPoid_Success() {
        doNothing().when(repository).deleteByTransactionPoid(999L);

        repository.deleteByTransactionPoid(999L);

        verify(repository).deleteByTransactionPoid(999L);
    }

    @Test
    void deleteByTransactionPoidAndDetRowId_Success() {
        doNothing().when(repository).deleteByTransactionPoidAndDetRowId(999L, 1L);

        repository.deleteByTransactionPoidAndDetRowId(999L, 1L);

        verify(repository).deleteByTransactionPoidAndDetRowId(999L, 1L);
    }
}
