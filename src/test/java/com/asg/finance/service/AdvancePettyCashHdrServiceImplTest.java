package com.asg.finance.service;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.finance.dto.AdvancePettyCashHdrRequestDTO;
import com.asg.finance.dto.AdvancePettyCashHdrResponseDTO;
import com.asg.finance.entity.AdvancePettyCashHdr;
import com.asg.finance.entity.GLMaster;
import com.asg.finance.repository.AdvancePettyCashDtlRepository;
import com.asg.finance.repository.AdvancePettyCashHdrRepository;
import com.asg.finance.repository.GLMasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvancePettyCashHdrServiceImplTest {

    @Mock
    private AdvancePettyCashHdrRepository repository;

    @Mock
    private AdvancePettyCashDtlRepository detailRepository;

    @Mock
    private GLMasterRepository glMasterRepository;

    @Mock
    private DocumentSearchService documentService;

    @InjectMocks
    private AdvancePettyCashHdrServiceImpl service;

    private AdvancePettyCashHdrRequestDTO requestDTO;
    private AdvancePettyCashHdr entity;
    private GLMaster glMaster;

    @BeforeEach
    void setUp() {
        requestDTO = AdvancePettyCashHdrRequestDTO.builder()
                .transactionDate(LocalDate.now())
                .pettyCashGlPoid(1001L)
                .payingTo("John Doe")
                .iouAmount(BigDecimal.valueOf(500))
                .narration("Test advance")
                .status("OPEN")
                .build();

        entity = AdvancePettyCashHdr.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.now())
                .docRef("ASGIOU001")
                .pettyCashGlPoid(1001L)
                .payingTo("John Doe")
                .iouAmount(BigDecimal.valueOf(500))
                .settledAmount(BigDecimal.ZERO)
                .balanceAmount(BigDecimal.valueOf(500))
                .narration("Test advance")
                .status("OPEN")
                .deleted("N")
                .createdBy("SYSTEM")
                .createdDate(LocalDateTime.now())
                .build();

        glMaster = GLMaster.builder()
                .glPoid(1001L)
                .glCode("1001")
                .glDescription("Petty Cash Account")
                .build();
    }

    @Test
    void createAdvancePettyCash_Success() {
        when(repository.save(any(AdvancePettyCashHdr.class))).thenReturn(entity);
        when(glMasterRepository.findByGlPoid(1001L)).thenReturn(Optional.of(glMaster));
        when(detailRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());

        AdvancePettyCashHdrResponseDTO result = service.createAdvancePettyCash(requestDTO);

        assertThat(result.getTransactionPoid()).isEqualTo(1L);
        assertThat(result.getPayingTo()).isEqualTo("John Doe");
        assertThat(result.getStatus()).isEqualTo("OPEN");
        verify(repository).save(any(AdvancePettyCashHdr.class));
    }

    @Test
    void createAdvancePettyCash_FutureDateValidation() {
        requestDTO.setTransactionDate(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class, () -> service.createAdvancePettyCash(requestDTO));
        verify(repository, never()).save(any());
    }

    @Test
    void createAdvancePettyCash_ClosedStatusWithoutReason() {
        requestDTO.setStatus("CLOSED");
        requestDTO.setClosedReason(null);

        assertThrows(ValidationException.class, () -> service.createAdvancePettyCash(requestDTO));
        verify(repository, never()).save(any());
    }

    @Test
    void updateAdvancePettyCash_Success() {
        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(AdvancePettyCashHdr.class))).thenReturn(entity);
        when(glMasterRepository.findByGlPoid(1001L)).thenReturn(Optional.of(glMaster));
        when(detailRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());

        AdvancePettyCashHdrResponseDTO result = service.updateAdvancePettyCash(1L, requestDTO);

        assertThat(result.getTransactionPoid()).isEqualTo(1L);
        verify(repository).save(any(AdvancePettyCashHdr.class));
    }

    @Test
    void updateAdvancePettyCash_RecordNotFound() {
        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateAdvancePettyCash(1L, requestDTO));
        verify(repository, never()).save(any());
    }

    @Test
    void updateAdvancePettyCash_ClosedRecord() {
        entity.setStatus("CLOSED");
        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.of(entity));

        assertThrows(ValidationException.class, () -> service.updateAdvancePettyCash(1L, requestDTO));
        verify(repository, never()).save(any());
    }

    /*@Test
    void getAdvancePettyCashById_Success() {
        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.of(entity));
        when(glMasterRepository.findByGlPoid(1001L)).thenReturn(Optional.of(glMaster));
        when(detailRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());

        AdvancePettyCashHdrResponseDTO result = service.getAdvancePettyCashById(1L);

        assertThat(result.getTransactionPoid()).isEqualTo(1L);
        assertThat(result.getGlLedger().getGlCode()).isEqualTo("1001");
    }*/

    @Test
    void getAdvancePettyCashById_NotFound() {
        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getAdvancePettyCashById(1L));
    }

    @Test
    void softDeleteAdvancePettyCash_Success() {
        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.of(entity));

        service.softDeleteAdvancePettyCash(1L);

        verify(repository).save(argThat(e -> "Y".equals(e.getDeleted())));
    }
}
