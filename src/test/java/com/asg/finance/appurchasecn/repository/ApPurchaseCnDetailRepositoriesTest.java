package com.asg.finance.appurchasecn.repository;

import com.asg.finance.entity.ApPurchaseCnChargeDtl;
import com.asg.finance.entity.ApPurchaseCnGlDtl;
import com.asg.finance.entity.key.ApPurchaseCnChargeDtlKey;
import com.asg.finance.entity.key.ApPurchaseCnGlDtlKey;
import com.asg.finance.repository.ApPurchaseCnChargeDtlRepository;
import com.asg.finance.repository.ApPurchaseCnGlDtlRepository;
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
class ApPurchaseCnDetailRepositoriesTest {

    @Mock
    private ApPurchaseCnChargeDtlRepository chargeDtlRepository;

    @Mock
    private ApPurchaseCnGlDtlRepository glDtlRepository;

    private ApPurchaseCnChargeDtl chargeEntity;
    private ApPurchaseCnGlDtl glEntity;
    private Long transactionPoid;

    @BeforeEach
    void setUp() {
        transactionPoid = 100L;
        
        chargeEntity = ApPurchaseCnChargeDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(1L)
                .chargePoid(21L)
                .chargeAmount(new BigDecimal("10"))
                .description("Test charge")
                .build();

        glEntity = ApPurchaseCnGlDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(1L)
                .type("DR")
                .companyPoid(1L)
                .glPoid(31L)
                .drAmount(new BigDecimal("100"))
                .crAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("100"))
                .build();
    }

    @Test
    void chargeDtlRepository_FindById_Success() {
        ApPurchaseCnChargeDtlKey key = new ApPurchaseCnChargeDtlKey(transactionPoid, 1L);
        when(chargeDtlRepository.findById(key)).thenReturn(Optional.of(chargeEntity));

        Optional<ApPurchaseCnChargeDtl> result = chargeDtlRepository.findById(key);

        assertThat(result).isPresent();
        assertThat(result.get().getTransactionPoid()).isEqualTo(transactionPoid);
        verify(chargeDtlRepository).findById(key);
    }

    @Test
    void chargeDtlRepository_FindByTransactionPoid_Success() {
        when(chargeDtlRepository.findByTransactionPoid(transactionPoid)).thenReturn(List.of(chargeEntity));

        List<ApPurchaseCnChargeDtl> result = chargeDtlRepository.findByTransactionPoid(transactionPoid);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionPoid()).isEqualTo(transactionPoid);
        verify(chargeDtlRepository).findByTransactionPoid(transactionPoid);
    }

    @Test
    void chargeDtlRepository_DeleteByTransactionPoid_Success() {
        doNothing().when(chargeDtlRepository).deleteByTransactionPoid(transactionPoid);

        chargeDtlRepository.deleteByTransactionPoid(transactionPoid);

        verify(chargeDtlRepository).deleteByTransactionPoid(transactionPoid);
    }

    @Test
    void glDtlRepository_FindById_Success() {
        ApPurchaseCnGlDtlKey key = new ApPurchaseCnGlDtlKey(transactionPoid, 1L);
        when(glDtlRepository.findById(key)).thenReturn(Optional.of(glEntity));

        Optional<ApPurchaseCnGlDtl> result = glDtlRepository.findById(key);

        assertThat(result).isPresent();
        assertThat(result.get().getTransactionPoid()).isEqualTo(transactionPoid);
        verify(glDtlRepository).findById(key);
    }

    @Test
    void glDtlRepository_FindByTransactionPoid_Success() {
        when(glDtlRepository.findByTransactionPoid(transactionPoid)).thenReturn(List.of(glEntity));

        List<ApPurchaseCnGlDtl> result = glDtlRepository.findByTransactionPoid(transactionPoid);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionPoid()).isEqualTo(transactionPoid);
        verify(glDtlRepository).findByTransactionPoid(transactionPoid);
    }

    @Test
    void glDtlRepository_DeleteByTransactionPoid_Success() {
        doNothing().when(glDtlRepository).deleteByTransactionPoid(transactionPoid);

        glDtlRepository.deleteByTransactionPoid(transactionPoid);

        verify(glDtlRepository).deleteByTransactionPoid(transactionPoid);
    }

    @Test
    void chargeDtlRepository_Save_Success() {
        when(chargeDtlRepository.save(chargeEntity)).thenReturn(chargeEntity);

        ApPurchaseCnChargeDtl saved = chargeDtlRepository.save(chargeEntity);

        assertThat(saved.getTransactionPoid()).isEqualTo(transactionPoid);
        assertThat(saved.getChargeAmount()).isEqualByComparingTo(new BigDecimal("10"));
        verify(chargeDtlRepository).save(chargeEntity);
    }

    @Test
    void glDtlRepository_Save_Success() {
        when(glDtlRepository.save(glEntity)).thenReturn(glEntity);

        ApPurchaseCnGlDtl saved = glDtlRepository.save(glEntity);

        assertThat(saved.getTransactionPoid()).isEqualTo(transactionPoid);
        assertThat(saved.getDrAmount()).isEqualByComparingTo(new BigDecimal("100"));
        verify(glDtlRepository).save(glEntity);
    }
}