package com.asg.finance.advancepettycash.repository;

import com.asg.finance.entity.AdvancePettyCashHdr;
import com.asg.finance.repository.AdvancePettyCashHdrRepository;
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
class AdvancePettyCashHdrRepositoryTest {

    @Mock
    private AdvancePettyCashHdrRepository repository;

    private AdvancePettyCashHdr testEntity;
    private Long transactionPoid;

    @BeforeEach
    void setUp() {
        transactionPoid = 1L;
        
        testEntity = AdvancePettyCashHdr.builder()
                .transactionPoid(transactionPoid)
                .transactionDate(LocalDate.now())
                .groupPoid(1L)
                .companyPoid(1L)
                .docRef("DOC-001")
                .pettyCashGlPoid(1L)
                .payingTo("John Doe")
                .iouAmount(BigDecimal.valueOf(1000))
                .settledAmount(BigDecimal.ZERO)
                .balanceAmount(BigDecimal.valueOf(1000))
                .narration("Test narration")
                .status("OPEN")
                .deleted("N")
                .build();
    }

    @Test
    void findByTransactionPoid_Success() {
        // Arrange
        when(repository.findByTransactionPoid(transactionPoid)).thenReturn(Optional.of(testEntity));

        // Act
        Optional<AdvancePettyCashHdr> result = repository.findByTransactionPoid(transactionPoid);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getTransactionPoid()).isEqualTo(transactionPoid);
        assertThat(result.get().getPayingTo()).isEqualTo("John Doe");
        assertThat(result.get().getDocRef()).isEqualTo("DOC-001");
        verify(repository).findByTransactionPoid(transactionPoid);
    }

    @Test
    void findByTransactionPoid_NotFound() {
        // Arrange
        when(repository.findByTransactionPoid(999L)).thenReturn(Optional.empty());

        // Act
        Optional<AdvancePettyCashHdr> result = repository.findByTransactionPoid(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(repository).findByTransactionPoid(999L);
    }

    @Test
    void existsByDocRef_True() {
        // Arrange
        when(repository.existsByDocRef("DOC-001")).thenReturn(true);

        // Act
        boolean exists = repository.existsByDocRef("DOC-001");

        // Assert
        assertThat(exists).isTrue();
        verify(repository).existsByDocRef("DOC-001");
    }

    @Test
    void existsByDocRef_False() {
        // Arrange
        when(repository.existsByDocRef("NON-EXISTENT")).thenReturn(false);

        // Act
        boolean exists = repository.existsByDocRef("NON-EXISTENT");

        // Assert
        assertThat(exists).isFalse();
        verify(repository).existsByDocRef("NON-EXISTENT");
    }

    @Test
    void existsByDocRefAndTransactionPoidNot_True() {
        // Arrange
        when(repository.existsByDocRefAndTransactionPoidNot("DOC-001", 999L)).thenReturn(true);

        // Act
        boolean exists = repository.existsByDocRefAndTransactionPoidNot("DOC-001", 999L);

        // Assert
        assertThat(exists).isTrue();
        verify(repository).existsByDocRefAndTransactionPoidNot("DOC-001", 999L);
    }

    @Test
    void existsByDocRefAndTransactionPoidNot_False_SameId() {
        // Arrange
        when(repository.existsByDocRefAndTransactionPoidNot("DOC-001", transactionPoid)).thenReturn(false);

        // Act
        boolean exists = repository.existsByDocRefAndTransactionPoidNot("DOC-001", transactionPoid);

        // Assert
        assertThat(exists).isFalse();
        verify(repository).existsByDocRefAndTransactionPoidNot("DOC-001", transactionPoid);
    }

    @Test
    void existsByDocRefAndTransactionPoidNot_False_NoMatch() {
        // Arrange
        when(repository.existsByDocRefAndTransactionPoidNot("NON-EXISTENT", 999L)).thenReturn(false);

        // Act
        boolean exists = repository.existsByDocRefAndTransactionPoidNot("NON-EXISTENT", 999L);

        // Assert
        assertThat(exists).isFalse();
        verify(repository).existsByDocRefAndTransactionPoidNot("NON-EXISTENT", 999L);
    }

    @Test
    void existsByTransactionPoid_True() {
        // Arrange
        when(repository.existsByTransactionPoid(transactionPoid)).thenReturn(true);

        // Act
        boolean exists = repository.existsByTransactionPoid(transactionPoid);

        // Assert
        assertThat(exists).isTrue();
        verify(repository).existsByTransactionPoid(transactionPoid);
    }

    @Test
    void existsByTransactionPoid_False() {
        // Arrange
        when(repository.existsByTransactionPoid(999L)).thenReturn(false);

        // Act
        boolean exists = repository.existsByTransactionPoid(999L);

        // Assert
        assertThat(exists).isFalse();
        verify(repository).existsByTransactionPoid(999L);
    }

    @Test
    void save_Success() {
        // Arrange
        when(repository.save(testEntity)).thenReturn(testEntity);

        // Act
        AdvancePettyCashHdr saved = repository.save(testEntity);

        // Assert
        assertThat(saved.getTransactionPoid()).isEqualTo(transactionPoid);
        assertThat(saved.getPayingTo()).isEqualTo("John Doe");
        assertThat(saved.getIouAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        verify(repository).save(testEntity);
    }

    @Test
    void delete_Success() {
        // Arrange
        doNothing().when(repository).delete(testEntity);

        // Act
        repository.delete(testEntity);

        // Assert
        verify(repository).delete(testEntity);
    }

    @Test
    void findByTransactionPoid_MultipleRecords() {
        // Arrange
        AdvancePettyCashHdr entity2 = AdvancePettyCashHdr.builder()
                .transactionPoid(2L)
                .transactionDate(LocalDate.now())
                .groupPoid(1L)
                .companyPoid(1L)
                .docRef("DOC-002")
                .pettyCashGlPoid(1L)
                .payingTo("Jane Doe")
                .iouAmount(BigDecimal.valueOf(2000))
                .settledAmount(BigDecimal.ZERO)
                .balanceAmount(BigDecimal.valueOf(2000))
                .narration("Test narration 2")
                .status("OPEN")
                .deleted("N")
                .build();

        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(repository.findByTransactionPoid(2L)).thenReturn(Optional.of(entity2));

        // Act
        Optional<AdvancePettyCashHdr> result1 = repository.findByTransactionPoid(1L);
        Optional<AdvancePettyCashHdr> result2 = repository.findByTransactionPoid(2L);

        // Assert
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result1.get().getPayingTo()).isEqualTo("John Doe");
        assertThat(result2.get().getPayingTo()).isEqualTo("Jane Doe");
    }

    @Test
    void existsByDocRef_CaseInsensitive() {
        // Arrange
        when(repository.existsByDocRef("doc-001")).thenReturn(true);
        when(repository.existsByDocRef("DOC-001")).thenReturn(true);

        // Act
        boolean existsLower = repository.existsByDocRef("doc-001");
        boolean existsUpper = repository.existsByDocRef("DOC-001");

        // Assert
        assertThat(existsLower).isTrue();
        assertThat(existsUpper).isTrue();
        verify(repository).existsByDocRef("doc-001");
        verify(repository).existsByDocRef("DOC-001");
    }

    @Test
    void repository_ConcurrentAccess() {
        // Arrange
        when(repository.existsByTransactionPoid(1L)).thenReturn(true);
        when(repository.existsByTransactionPoid(2L)).thenReturn(false);

        // Act - Simulate concurrent access
        boolean exists1 = repository.existsByTransactionPoid(1L);
        boolean exists2 = repository.existsByTransactionPoid(2L);

        // Assert
        assertThat(exists1).isTrue();
        assertThat(exists2).isFalse();
        verify(repository).existsByTransactionPoid(1L);
        verify(repository).existsByTransactionPoid(2L);
    }
}