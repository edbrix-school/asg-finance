package com.asg.finance.appurchasecn.repository;

import com.asg.finance.entity.ApPurchaseCnItemDtl;
import com.asg.finance.entity.key.ApPurchaseCnItemDtlKey;
import com.asg.finance.repository.ApPurchaseCnItemDtlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApPurchaseCnItemDtlRepositoryTest {

    @Mock
    private ApPurchaseCnItemDtlRepository repository;

    private ApPurchaseCnItemDtl testEntity;
    private ApPurchaseCnItemDtlKey testKey;
    private Long transactionPoid;

    @BeforeEach
    void setUp() {
        transactionPoid = 100L;
        testKey = new ApPurchaseCnItemDtlKey(transactionPoid, 1L);
        
        testEntity = ApPurchaseCnItemDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(1L)
                .stockPoid(11L)
                .stockUnitPoid(12L)
                .quantity(new BigDecimal("2"))
                .price(new BigDecimal("20"))
                .amount(new BigDecimal("40"))
                .baseAmount(new BigDecimal("40"))
                .build();
    }

    @Test
    void findById_Success() {
        when(repository.findById(testKey)).thenReturn(Optional.of(testEntity));

        Optional<ApPurchaseCnItemDtl> result = repository.findById(testKey);

        assertThat(result).isPresent();
        assertThat(result.get().getTransactionPoid()).isEqualTo(transactionPoid);
        assertThat(result.get().getDetRowId()).isEqualTo(1L);
        verify(repository).findById(testKey);
    }

    @Test
    void findByTransactionPoid_Success() {
        when(repository.findByTransactionPoid(transactionPoid)).thenReturn(List.of(testEntity));

        List<ApPurchaseCnItemDtl> result = repository.findByTransactionPoid(transactionPoid);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionPoid()).isEqualTo(transactionPoid);
        verify(repository).findByTransactionPoid(transactionPoid);
    }

    @Test
    void deleteByTransactionPoid_Success() {
        doNothing().when(repository).deleteByTransactionPoid(transactionPoid);

        repository.deleteByTransactionPoid(transactionPoid);

        verify(repository).deleteByTransactionPoid(transactionPoid);
    }

    @Test
    void save_Success() {
        when(repository.save(testEntity)).thenReturn(testEntity);

        ApPurchaseCnItemDtl saved = repository.save(testEntity);

        assertThat(saved.getTransactionPoid()).isEqualTo(transactionPoid);
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("40"));
        verify(repository).save(testEntity);
    }
}