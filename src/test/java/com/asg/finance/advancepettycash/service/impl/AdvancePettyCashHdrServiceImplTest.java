package com.asg.finance.advancepettycash.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.dto.AdvancePettyCashHdrRequestDTO;
import com.asg.finance.dto.AdvancePettyCashHdrResponseDTO;
import com.asg.finance.entity.AdvancePettyCashDtl;
import com.asg.finance.entity.AdvancePettyCashHdr;
import com.asg.finance.entity.GLMaster;
import com.asg.finance.repository.AdvancePettyCashDtlRepository;
import com.asg.finance.repository.AdvancePettyCashHdrRepository;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.finance.service.impl.AdvancePettyCashHdrServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    private LoggingService loggingService;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @InjectMocks
    private AdvancePettyCashHdrServiceImpl service;

    private AdvancePettyCashHdrRequestDTO requestDTO;
    private AdvancePettyCashHdr entity;
    private GLMaster glMaster;

    @BeforeEach
    void setUp() {
        requestDTO = AdvancePettyCashHdrRequestDTO.builder()
                .transactionDate(LocalDate.now())
                .pettyCashGlPoid(1L)
                .payingTo("John Doe")
                .iouAmount(BigDecimal.valueOf(1000))
                .narration("Test narration")
                .status("OPEN")
                .build();

        entity = AdvancePettyCashHdr.builder()
                .transactionPoid(1L)
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

        glMaster = new GLMaster();
        glMaster.setGlPoid(1L);
        glMaster.setGlCode("GL001");
        glMaster.setGlDescription("Petty Cash");
    }

    @Test
    void createAdvancePettyCash_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(repository.save(any(AdvancePettyCashHdr.class))).thenReturn(entity);
            when(glMasterRepository.findByGlPoid(1L)).thenReturn(Optional.of(glMaster));
            when(detailRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());

            AdvancePettyCashHdrResponseDTO result = service.createAdvancePettyCash(requestDTO);

            assertNotNull(result);
            assertEquals(1L, result.getTransactionPoid());
            assertEquals("John Doe", result.getPayingTo());
            verify(repository, times(1)).save(any(AdvancePettyCashHdr.class));
            verify(loggingService, times(1)).createLogSummaryEntry(any(LogDetailsEnum.class), eq("DOC123"), eq("1"));
        }
    }

    @Test
    void createAdvancePettyCash_WithClosedStatus() {
        requestDTO.setStatus("CLOSED");
        requestDTO.setClosedReason("Completed");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            entity.setStatus("CLOSED");
            entity.setSettledAmount(BigDecimal.valueOf(1000));
            entity.setBalanceAmount(BigDecimal.ZERO);

            when(repository.save(any(AdvancePettyCashHdr.class))).thenReturn(entity);
            when(glMasterRepository.findByGlPoid(1L)).thenReturn(Optional.of(glMaster));
            when(detailRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());

            AdvancePettyCashHdrResponseDTO result = service.createAdvancePettyCash(requestDTO);

            assertNotNull(result);
            assertEquals("CLOSED", result.getStatus());
        }
    }

    @Test
    void createAdvancePettyCash_FutureDateValidation() {
        requestDTO.setTransactionDate(LocalDate.now().plusDays(1));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            service.createAdvancePettyCash(requestDTO);
        });

        assertEquals("Transaction date cannot be in future", exception.getMessage());
    }

    @Test
    void createAdvancePettyCash_ClosedStatusWithoutReason() {
        requestDTO.setStatus("CLOSED");
        requestDTO.setClosedReason(null);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            service.createAdvancePettyCash(requestDTO);
        });

        assertEquals("Closed reason is required !!", exception.getMessage());
    }

    @Test
    void createAdvancePettyCash_RefundedStatusWithoutReason() {
        requestDTO.setStatus("REFUNDED");
        requestDTO.setClosedReason("");

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            service.createAdvancePettyCash(requestDTO);
        });

        assertEquals("Closed reason is required !!", exception.getMessage());
    }

    @Test
    void createAdvancePettyCash_TriggerError_ORA20001() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(repository.save(any(AdvancePettyCashHdr.class)))
                    .thenThrow(new DataIntegrityViolationException("ORA-20001: Financial period error"));

            ValidationException exception = assertThrows(ValidationException.class, () -> {
                service.createAdvancePettyCash(requestDTO);
            });

            assertEquals("Changes allowed only within current Financial Period", exception.getMessage());
        }
    }

    @Test
    void createAdvancePettyCash_TriggerError_ORA20002() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(repository.save(any(AdvancePettyCashHdr.class)))
                    .thenThrow(new DataIntegrityViolationException("ORA-20002: Transaction date can not update"));

            ValidationException exception = assertThrows(ValidationException.class, () -> {
                service.createAdvancePettyCash(requestDTO);
            });

            assertEquals("Transaction date cannot be updated", exception.getMessage());
        }
    }

    @Test
    void createAdvancePettyCash_TriggerError_ORA20002_Other() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(repository.save(any(AdvancePettyCashHdr.class)))
                    .thenThrow(new DataIntegrityViolationException("ORA-20002: Transaction period error"));

            ValidationException exception = assertThrows(ValidationException.class, () -> {
                service.createAdvancePettyCash(requestDTO);
            });

            assertEquals("Changes allowed only within current Transaction Period", exception.getMessage());
        }
    }

    @Test
    void createAdvancePettyCash_GenericDatabaseError() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(repository.save(any(AdvancePettyCashHdr.class)))
                    .thenThrow(new DataIntegrityViolationException("Generic error"));

            ValidationException exception = assertThrows(ValidationException.class, () -> {
                service.createAdvancePettyCash(requestDTO);
            });

            assertTrue(exception.getMessage().contains("Database validation failed"));
        }
    }

    @Test
    void updateAdvancePettyCash_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(repository.findByTransactionPoid(1L)).thenReturn(Optional.of(entity));
            when(repository.save(any(AdvancePettyCashHdr.class))).thenReturn(entity);
            when(glMasterRepository.findByGlPoid(1L)).thenReturn(Optional.of(glMaster));
            when(detailRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());

            AdvancePettyCashHdrResponseDTO result = service.updateAdvancePettyCash(1L, requestDTO);

            assertNotNull(result);
            verify(repository, times(1)).save(any(AdvancePettyCashHdr.class));
            verify(loggingService, times(1)).logChanges(any(), any(), any(), eq("DOC123"), eq("1"), any(LogDetailsEnum.class), eq("TRANSACTION_POID"));
        }
    }

    @Test
    void updateAdvancePettyCash_ToClosedStatus() {
        requestDTO.setStatus("CLOSED");
        requestDTO.setClosedReason("Completed");

        entity.setSettledAmount(BigDecimal.valueOf(500));
        entity.setBalanceAmount(BigDecimal.valueOf(500));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(repository.findByTransactionPoid(1L)).thenReturn(Optional.of(entity));
            when(repository.save(any(AdvancePettyCashHdr.class))).thenAnswer(invocation -> {
                AdvancePettyCashHdr saved = invocation.getArgument(0);
                assertEquals(BigDecimal.valueOf(1000), saved.getSettledAmount());
                assertEquals(BigDecimal.ZERO, saved.getBalanceAmount());
                return saved;
            });
            when(glMasterRepository.findByGlPoid(1L)).thenReturn(Optional.of(glMaster));
            when(detailRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());

            AdvancePettyCashHdrResponseDTO result = service.updateAdvancePettyCash(1L, requestDTO);

            assertNotNull(result);
        }
    }

    @Test
    void updateAdvancePettyCash_ToOpenStatus() {
        requestDTO.setStatus("OPEN");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(repository.findByTransactionPoid(1L)).thenReturn(Optional.of(entity));
            when(repository.save(any(AdvancePettyCashHdr.class))).thenAnswer(invocation -> {
                AdvancePettyCashHdr saved = invocation.getArgument(0);
                assertEquals(BigDecimal.ZERO, saved.getSettledAmount());
                assertEquals(BigDecimal.valueOf(1000), saved.getBalanceAmount());
                return saved;
            });
            when(glMasterRepository.findByGlPoid(1L)).thenReturn(Optional.of(glMaster));
            when(detailRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());

            AdvancePettyCashHdrResponseDTO result = service.updateAdvancePettyCash(1L, requestDTO);

            assertNotNull(result);
        }
    }

    @Test
    void updateAdvancePettyCash_NotFound() {
        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            service.updateAdvancePettyCash(1L, requestDTO);
        });

        assertTrue(exception.getMessage().contains("Advance Petty Cash not found"));
    }

    @Test
    void updateAdvancePettyCash_AlreadyClosed() {
        entity.setStatus("CLOSED");
        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.of(entity));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            service.updateAdvancePettyCash(1L, requestDTO);
        });

        assertEquals("Cannot update a closed advance petty cash record", exception.getMessage());
    }

    @Test
    void getAdvancePettyCashById_Success() {
        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.of(entity));
        when(glMasterRepository.findByGlPoid(1L)).thenReturn(Optional.of(glMaster));
        when(detailRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());

        AdvancePettyCashHdrResponseDTO result = service.getAdvancePettyCashById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getTransactionPoid());
        assertEquals("John Doe", result.getPayingTo());
        assertNotNull(result.getPettyCashGlPoidDet());
        assertEquals("GL001", result.getPettyCashGlPoidDet().getCode());
    }

    @Test
    void getAdvancePettyCashById_WithDetails() {
        AdvancePettyCashDtl detail = AdvancePettyCashDtl.builder()
                .detRowId(1L)
                .transactionPoid(1L)
                .pettyCashTrnDate(LocalDate.now())
                .pettyCashRef("REF-001")
                .amount(BigDecimal.valueOf(500))
                .pettyCashRemarks("Test remark")
                .build();

        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.of(entity));
        when(glMasterRepository.findByGlPoid(1L)).thenReturn(Optional.of(glMaster));
        when(detailRepository.findByTransactionPoid(1L)).thenReturn(List.of(detail));

        AdvancePettyCashHdrResponseDTO result = service.getAdvancePettyCashById(1L);

        assertNotNull(result);
        assertNotNull(result.getDetails());
        assertEquals(1, result.getDetails().size());
        assertEquals("REF-001", result.getDetails().get(0).getPettyCashReference());
    }

    @Test
    void getAdvancePettyCashById_WithoutGLMaster() {
        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.of(entity));
        when(glMasterRepository.findByGlPoid(1L)).thenReturn(Optional.empty());
        when(detailRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());

        AdvancePettyCashHdrResponseDTO result = service.getAdvancePettyCashById(1L);

        assertNotNull(result);
        assertNull(result.getPettyCashGlPoidDet());
    }

    @Test
    void getAdvancePettyCashById_NotFound() {
        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            service.getAdvancePettyCashById(1L);
        });

        assertTrue(exception.getMessage().contains("Advance Petty Cash not found"));
    }

    @Test
    void softDeleteAdvancePettyCash_Success() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Test deletion");

        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.of(entity));
        when(documentDeleteService.deleteDocument(anyLong(), anyString(), anyString(), any(), any())).thenReturn(String.valueOf(true));

        service.softDeleteAdvancePettyCash(1L, deleteReasonDto);

        verify(documentDeleteService, times(1)).deleteDocument(
                eq(1L),
                eq("GL_ADVANCE_PETTY_CASH_HDR"),
                eq("TRANSACTION_POID"),
                eq(deleteReasonDto),
                eq(entity.getTransactionDate())
        );
    }

    @Test
    void softDeleteAdvancePettyCash_NotFound() {
        when(repository.findByTransactionPoid(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            service.softDeleteAdvancePettyCash(1L, null);
        });

        assertTrue(exception.getMessage().contains("Advance Petty Cash not found"));
    }

    @Test
    void listAdvancePettyCash_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate periodFrom = LocalDate.of(2024, 1, 1);
        LocalDate periodTo = LocalDate.of(2024, 12, 31);

        Map<String, Object> recordData = new HashMap<>();
        recordData.put("transactionPoid", "1");
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(recordData);
        Map<String, String> displayFields = new HashMap<>();
        displayFields.put("transactionPoid", "Transaction ID");
        
        RawSearchResult rawResult = new RawSearchResult(
                records,
                displayFields,
                1L
        );

        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveDateFilters(any(), eq("TRANSACTION_DATE"), eq(periodFrom), eq(periodTo)))
                .thenReturn(new ArrayList<>());
        when(documentService.search(anyString(), anyList(), anyString(), any(Pageable.class), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.listAdvancePettyCash("DOC123", null, pageable, periodFrom, periodTo);

        assertNotNull(result);
        verify(documentService, times(1)).search(eq("DOC123"), anyList(), eq("AND"), eq(pageable), eq("N"), eq("DOC_REF"), eq("TRANSACTION_POID"));
    }

    @Test
    void listAdvancePettyCash_WithoutDateRange() {
        Pageable pageable = PageRequest.of(0, 10);

        List<Map<String, Object>> emptyRecords = new ArrayList<>();
        Map<String, String> emptyDisplayFields = new HashMap<>();
        RawSearchResult rawResult = new RawSearchResult(
                emptyRecords,
                emptyDisplayFields,
                0L
        );

        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveDateFilters(any(), eq("TRANSACTION_DATE"), isNull(), isNull()))
                .thenReturn(new ArrayList<>());
        when(documentService.search(anyString(), anyList(), anyString(), any(Pageable.class), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.listAdvancePettyCash("DOC123", null, pageable, null, null);

        assertNotNull(result);
    }

    @Test
    void listAdvancePettyCash_InvalidDateRange_FromAfterTo() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate periodFrom = LocalDate.of(2024, 12, 31);
        LocalDate periodTo = LocalDate.of(2024, 1, 1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.listAdvancePettyCash("DOC123", null, pageable, periodFrom, periodTo);
        });

        assertEquals("Period From must not be after Period To", exception.getMessage());
    }

    @Test
    void listAdvancePettyCash_InvalidDateRange_OnlyFromProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate periodFrom = LocalDate.of(2024, 1, 1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.listAdvancePettyCash("DOC123", null, pageable, periodFrom, null);
        });

        assertEquals("Both startDate and endDate should be specified or both dates should be empty.", exception.getMessage());
    }

    @Test
    void listAdvancePettyCash_InvalidDateRange_OnlyToProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate periodTo = LocalDate.of(2024, 12, 31);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.listAdvancePettyCash("DOC123", null, pageable, null, periodTo);
        });

        assertEquals("Both startDate and endDate should be specified or both dates should be empty.", exception.getMessage());
    }
}
