package com.asg.finance.appurchasecn.repository;

import com.asg.finance.entity.ApPurchaseCnHdr;
import com.asg.finance.repository.ApPurchaseCnHdrRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApPurchaseCnHdrRepositoryTest {

    @Mock
    private ApPurchaseCnHdrRepository repository;

    private ApPurchaseCnHdr testEntity;
    private Long transactionPoid;

    @BeforeEach
    void setUp() {
        transactionPoid = 100L;
        
        testEntity = ApPurchaseCnHdr.builder()
                .transactionPoid(transactionPoid)
                .transactionDate(LocalDate.now())
                .groupPoid(1L)
                .companyPoid(1L)
                .docRef("CN-001")
                .currencyCode("BHD")
                .currencyRate(BigDecimal.ONE)
                .supplierPoid(101L)
                .supplierCnAmount(new BigDecimal("100.00"))
                .refType("GENERAL")
                .narration("Test narration")
                .partyType("SUPPLIER")
                .deleted("N")
                .build();
    }

    @Test
    void findById_Success() {
        when(repository.findById(transactionPoid)).thenReturn(Optional.of(testEntity));

        Optional<ApPurchaseCnHdr> result = repository.findById(transactionPoid);

        assertThat(result).isPresent();
        assertThat(result.get().getTransactionPoid()).isEqualTo(transactionPoid);
        assertThat(result.get().getDocRef()).isEqualTo("CN-001");
        verify(repository).findById(transactionPoid);
    }

    @Test
    void findById_NotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        Optional<ApPurchaseCnHdr> result = repository.findById(999L);

        assertThat(result).isEmpty();
        verify(repository).findById(999L);
    }

    @Test
    void save_Success() {
        when(repository.save(testEntity)).thenReturn(testEntity);

        ApPurchaseCnHdr saved = repository.save(testEntity);

        assertThat(saved.getTransactionPoid()).isEqualTo(transactionPoid);
        assertThat(saved.getDocRef()).isEqualTo("CN-001");
        assertThat(saved.getSupplierCnAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        verify(repository).save(testEntity);
    }

    @Test
    void delete_Success() {
        doNothing().when(repository).delete(testEntity);

        repository.delete(testEntity);

        verify(repository).delete(testEntity);
    }

    @Test
    void existsById_True() {
        when(repository.existsById(transactionPoid)).thenReturn(true);

        boolean exists = repository.existsById(transactionPoid);

        assertThat(exists).isTrue();
        verify(repository).existsById(transactionPoid);
    }

    @Test
    void existsById_False() {
        when(repository.existsById(999L)).thenReturn(false);

        boolean exists = repository.existsById(999L);

        assertThat(exists).isFalse();
        verify(repository).existsById(999L);
    }
}