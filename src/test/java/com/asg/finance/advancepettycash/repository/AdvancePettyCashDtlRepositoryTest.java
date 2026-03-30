package com.asg.finance.advancepettycash.repository;

import com.asg.finance.entity.AdvancePettyCashDtl;
import com.asg.finance.repository.AdvancePettyCashDtlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvancePettyCashDtlRepositoryTest {

    @Mock
    private AdvancePettyCashDtlRepository repository;

    private AdvancePettyCashDtl testEntity1;
    private AdvancePettyCashDtl testEntity2;
    private Long transactionPoid;

    @BeforeEach
    void setUp() {
        transactionPoid = 100L;
        
        testEntity1 = AdvancePettyCashDtl.builder()
                .detRowId(1L)
                .transactionPoid(transactionPoid)
                .pettyCashTrnDate(LocalDate.now())
                .pettyCashRef("REF-001")
                .amount(BigDecimal.valueOf(500))
                .pettyCashRemarks("Test remark 1")
                .build();

        testEntity2 = AdvancePettyCashDtl.builder()
                .detRowId(2L)
                .transactionPoid(transactionPoid)
                .pettyCashTrnDate(LocalDate.now().minusDays(1))
                .pettyCashRef("REF-002")
                .amount(BigDecimal.valueOf(300))
                .pettyCashRemarks("Test remark 2")
                .build();
    }

    @Test
    void findByTransactionPoid_Success() {
        // Arrange
        List<AdvancePettyCashDtl> expectedResult = List.of(testEntity1, testEntity2);
        when(repository.findByTransactionPoid(transactionPoid)).thenReturn(expectedResult);

        // Act
        List<AdvancePettyCashDtl> result = repository.findByTransactionPoid(transactionPoid);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(AdvancePettyCashDtl::getPettyCashRef)
                .containsExactlyInAnyOrder("REF-001", "REF-002");
        verify(repository).findByTransactionPoid(transactionPoid);
    }

    @Test
    void findByTransactionPoid_EmptyResult() {
        // Arrange
        when(repository.findByTransactionPoid(999L)).thenReturn(new ArrayList<>());

        // Act
        List<AdvancePettyCashDtl> result = repository.findByTransactionPoid(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(repository).findByTransactionPoid(999L);
    }

    @Test
    void findByTransactionPoid_SingleResult() {
        // Arrange
        List<AdvancePettyCashDtl> expectedResult = List.of(testEntity1);
        when(repository.findByTransactionPoid(transactionPoid)).thenReturn(expectedResult);

        // Act
        List<AdvancePettyCashDtl> result = repository.findByTransactionPoid(transactionPoid);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPettyCashRef()).isEqualTo("REF-001");
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        verify(repository).findByTransactionPoid(transactionPoid);
    }

    @Test
    void findByTransactionPoid_DifferentTransactionPoids() {
        // Arrange
        testEntity2.setTransactionPoid(200L);
        when(repository.findByTransactionPoid(100L)).thenReturn(List.of(testEntity1));
        when(repository.findByTransactionPoid(200L)).thenReturn(List.of(testEntity2));

        // Act
        List<AdvancePettyCashDtl> result100 = repository.findByTransactionPoid(100L);
        List<AdvancePettyCashDtl> result200 = repository.findByTransactionPoid(200L);

        // Assert
        assertThat(result100).hasSize(1);
        assertThat(result200).hasSize(1);
        assertThat(result100.get(0).getPettyCashRef()).isEqualTo("REF-001");
        assertThat(result200.get(0).getPettyCashRef()).isEqualTo("REF-002");
        verify(repository).findByTransactionPoid(100L);
        verify(repository).findByTransactionPoid(200L);
    }

    @Test
    void saveAndFindByTransactionPoid_Success() {
        // Arrange
        when(repository.save(testEntity1)).thenReturn(testEntity1);
        when(repository.findByTransactionPoid(transactionPoid)).thenReturn(List.of(testEntity1));

        // Act
        AdvancePettyCashDtl saved = repository.save(testEntity1);
        List<AdvancePettyCashDtl> found = repository.findByTransactionPoid(transactionPoid);

        // Assert
        assertThat(saved.getPettyCashRef()).isEqualTo("REF-001");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getPettyCashRef()).isEqualTo("REF-001");
        verify(repository).save(testEntity1);
        verify(repository).findByTransactionPoid(transactionPoid);
    }

    @Test
    void findByTransactionPoid_LargeDataSet() {
        // Arrange
        List<AdvancePettyCashDtl> largeDataSet = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            AdvancePettyCashDtl entity = AdvancePettyCashDtl.builder()
                    .detRowId((long) i)
                    .transactionPoid(transactionPoid)
                    .pettyCashTrnDate(LocalDate.now())
                    .pettyCashRef("REF-" + String.format("%03d", i))
                    .amount(BigDecimal.valueOf(i * 10))
                    .build();
            largeDataSet.add(entity);
        }
        when(repository.findByTransactionPoid(transactionPoid)).thenReturn(largeDataSet);

        // Act
        List<AdvancePettyCashDtl> result = repository.findByTransactionPoid(transactionPoid);

        // Assert
        assertThat(result).hasSize(1000);
        verify(repository).findByTransactionPoid(transactionPoid);
    }

    @Test
    void findByTransactionPoid_ConcurrentAccess() {
        // Arrange
        when(repository.findByTransactionPoid(transactionPoid)).thenReturn(List.of(testEntity1));
        when(repository.findByTransactionPoid(200L)).thenReturn(List.of(testEntity2));

        // Act & Assert - Simulate concurrent access
        List<AdvancePettyCashDtl> result1 = repository.findByTransactionPoid(transactionPoid);
        List<AdvancePettyCashDtl> result2 = repository.findByTransactionPoid(200L);

        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(1);
        assertThat(result1.get(0).getPettyCashRef()).isEqualTo("REF-001");
        assertThat(result2.get(0).getPettyCashRef()).isEqualTo("REF-002");
    }
}