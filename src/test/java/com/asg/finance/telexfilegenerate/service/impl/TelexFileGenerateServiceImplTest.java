package com.asg.finance.telexfilegenerate.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.TelexFileDtlDto;
import com.asg.finance.dto.TelexFileGenerateRequestDto;
import com.asg.finance.dto.TelexFileGenerateResponseDto;
import com.asg.finance.entity.GlBankDebitHdr;
import com.asg.finance.entity.GlBankFileDtl;
import com.asg.finance.entity.GlBankFileHdr;
import com.asg.finance.repository.GlBankDebitHdrRepository;
import com.asg.finance.repository.GlBankFileDtlRepository;
import com.asg.finance.repository.GlBankFileHdrRepository;
import com.asg.finance.repository.TelexFileGenerateProcRepository;
import com.asg.finance.service.BankFileBatchService;
import com.asg.finance.service.impl.TelexFileGenerateServiceImpl;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelexFileGenerateServiceImplTest {

    @Mock
    private GlBankFileHdrRepository hdrRepository;

    @Mock
    private GlBankFileDtlRepository dtlRepository;

    @Mock
    private TelexFileGenerateProcRepository procRepository;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private LovDataService lovService;

    @Mock
    private GlBankDebitHdrRepository glBankDebitHdrRepository;

    @Mock
    private BankFileBatchService bankFileBatchService;

    @Mock
    private LoggingService loggingService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @InjectMocks
    private TelexFileGenerateServiceImpl service;

    private TelexFileGenerateRequestDto requestDto;
    private GlBankFileHdr hdrEntity;
    private GlBankFileDtl dtlEntity;
    private TelexFileDtlDto detailDto;
    private LovGetListDto lovDto;

    @BeforeEach
    void setUp() {
        detailDto = TelexFileDtlDto.builder()
                .detRowId(1L)
                .debitTransactionPoid(1001L)
                .debitTransactionDate(LocalDate.of(2026, 1, 28))
                .debitCompanyPoid(1L)
                .debitDocRef("BD-2025-001")
                .debitPayingToName("ABC Suppliers Ltd")
                .debitPayingType("1")
                .debitLongNarration("Payment for invoice")
                .debitTtDate(LocalDate.of(2026, 1, 29))
                .debitCurrencyCode("USD")
                .debitCurrencyRate(new BigDecimal("3.75"))
                .debitCurrencyAmt(new BigDecimal("10000.00"))
                .debitAmount(new BigDecimal("37500.00"))
                .deleted("N")
                .selected("N")
                .drilldownLinkInfo("TARGET_DOC_ID=400-111,DOC_KEY_POID=1001")
                .debitTtChargeType("OUR")
                .build();

        requestDto = TelexFileGenerateRequestDto.builder()
                .bankPoid(1L)
                .bankList("Y")
                .transactionDate(LocalDate.of(2026, 1, 29))
                .remarks("Test remarks")
                .approvalOnly(false)
                .suppressBalanceCheck(false)
                .details(Arrays.asList(detailDto))
                .build();

        hdrEntity = new GlBankFileHdr();
        hdrEntity.setTransactionPoid(42418L);
        hdrEntity.setTransactionDate(LocalDate.of(2026, 1, 29));
        hdrEntity.setGroupPoid(1L);
        hdrEntity.setCompanyPoid(1L);
        hdrEntity.setDocRef("ASGFILE3378");
        hdrEntity.setBankPoid(1L);
        hdrEntity.setBankList("Y");
        hdrEntity.setLongNarration("Test remarks");
        hdrEntity.setOnlyApproval("N");
        hdrEntity.setTtSuppressBalanceCheck("N");
        hdrEntity.setDeleted("N");

        dtlEntity = GlBankFileDtl.builder()
                .transactionPoid(42418L)
                .detRowId(1L)
                .debitTransactionPoid(1001L)
                .debitTransactionDate(LocalDate.of(2026, 1, 28))
                .debitCompanyPoid(1L)
                .debitDocRef("BD-2025-001")
                .debitPayingToName("ABC Suppliers Ltd")
                .debitPayingType("1")
                .debitLongNarration("Payment for invoice")
                .debitTtDate(LocalDate.of(2026, 1, 29))
                .debitCurrencyCode("USD")
                .debitCurrencyRate(new BigDecimal("3.75"))
                .debitCurrencyAmt(new BigDecimal("10000.00"))
                .debitAmount(new BigDecimal("37500.00"))
                .deleted("N")
                .selected("N")
                .drilldownLinkInfo("TARGET_DOC_ID=400-111,DOC_KEY_POID=1001")
                .debitTtChargeType("OUR")
                .build();

        lovDto = new LovGetListDto();
        lovDto.setPoid(1L);
        lovDto.setCode("TEST");
        lovDto.setLabel("Test Label");
    }

    @Test
    void createTelexFile_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");
            mockedTxManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);
            when(dtlRepository.saveAll(anyList())).thenReturn(Arrays.asList(dtlEntity));
            when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
            when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Arrays.asList(dtlEntity));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);
            doNothing().when(loggingService).createLogSummaryEntry(anyString(), anyString(), anyString());
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            TelexFileGenerateResponseDto result = service.createTelexFile(requestDto);

            assertNotNull(result);
            assertEquals(42418L, result.getTransactionPoid());
            assertEquals("ASGFILE3378", result.getDocRef());
            verify(hdrRepository, times(1)).saveAndFlush(any(GlBankFileHdr.class));
            verify(dtlRepository, times(1)).saveAll(anyList());
            verify(loggingService, times(1)).createLogSummaryEntry(any(LogDetailsEnum.class), eq("100-153"), eq("42418"));
        }
    }

    @Test
    void createTelexFile_WithoutDetails() {
        requestDto.setDetails(null);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");
            mockedTxManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);
            when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
            when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Collections.emptyList());
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);

            TelexFileGenerateResponseDto result = service.createTelexFile(requestDto);

            assertNotNull(result);
            verify(dtlRepository, never()).saveAll(anyList());
        }
    }

    @Test
    void createTelexFile_TriggerError_FinancialPeriod() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class)))
                    .thenThrow(new DataIntegrityViolationException("Changes allowed only within current Financial Period"));

            ValidationException exception = assertThrows(ValidationException.class, () -> {
                service.createTelexFile(requestDto);
            });

            assertEquals("Changes allowed only within current Financial Period", exception.getMessage());
        }
    }

    @Test
    void createTelexFile_TriggerError_TransactionDate() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class)))
                    .thenThrow(new DataIntegrityViolationException("Transaction date can not update"));

            ValidationException exception = assertThrows(ValidationException.class, () -> {
                service.createTelexFile(requestDto);
            });

            assertEquals("Transaction date cannot be updated", exception.getMessage());
        }
    }

    @Test
    void createTelexFile_TriggerError_TransactionPeriod() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class)))
                    .thenThrow(new DataIntegrityViolationException("Changes allowed only within current Transaction Period"));

            ValidationException exception = assertThrows(ValidationException.class, () -> {
                service.createTelexFile(requestDto);
            });

            assertEquals("Changes allowed only within current Transaction Period", exception.getMessage());
        }
    }

    @Test
    void updateTelexFile_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedTxManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);
            when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Arrays.asList(dtlEntity));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);

            TelexFileGenerateResponseDto result = service.updateTelexFile(42418L, requestDto);

            assertNotNull(result);
            verify(hdrRepository, times(1)).saveAndFlush(any(GlBankFileHdr.class));
            verify(loggingService, times(1)).logChanges(any(), any(), any(), eq("100-153"), eq("42418"), any(LogDetailsEnum.class), eq("TRANSACTION_POID"));
        }
    }

    @Test
    void updateTelexFile_WithDetailsCreated() {
        detailDto.setActionType("ISCREATED");
        detailDto.setDetRowId(null);
        requestDto.setDetails(Arrays.asList(detailDto));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedTxManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);
            when(dtlRepository.findMaxDetRowIdByTransactionPoid(42418L)).thenReturn(null);
            when(dtlRepository.saveAll(anyList())).thenReturn(Arrays.asList(dtlEntity));
            when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Arrays.asList(dtlEntity));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);
            lenient().doNothing().when(loggingService).createLogSummaryEntry(anyString(), anyString(), anyString());
            lenient().doNothing().when(loggingService).logChanges(any(), any(), any(), anyString(), anyString(), any(), anyString());

            TelexFileGenerateResponseDto result = service.updateTelexFile(42418L, requestDto);

            assertNotNull(result);
            verify(dtlRepository, times(1)).saveAll(anyList());
        }
    }

    @Test
    void updateTelexFile_WithDetailsUpdated() {
        detailDto.setActionType("ISUPDATED");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedTxManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);
            when(dtlRepository.findByTransactionPoidAndDetRowId(42418L, 1L)).thenReturn(Optional.of(dtlEntity));
            when(dtlRepository.saveAll(anyList())).thenReturn(Arrays.asList(dtlEntity));
            when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Arrays.asList(dtlEntity));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);
            doNothing().when(loggingService).createLogBatch(anyList());
            doNothing().when(loggingService).logChanges(any(), any(), any(), anyString(), anyString(), any(), anyString());

            TelexFileGenerateResponseDto result = service.updateTelexFile(42418L, requestDto);

            assertNotNull(result);
            verify(dtlRepository, times(1)).saveAll(anyList());
        }
    }

    @Test
    void updateTelexFile_WithDetailsDeleted() {
        detailDto.setActionType("ISDELETED");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedTxManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);
            doNothing().when(dtlRepository).deleteByTransactionPoidAndDetRowId(42418L, 1L);
            when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Collections.emptyList());
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);
            doNothing().when(loggingService).logDelete(any(), anyString(), anyString());
            doNothing().when(loggingService).logChanges(any(), any(), any(), anyString(), anyString(), any(), anyString());

            TelexFileGenerateResponseDto result = service.updateTelexFile(42418L, requestDto);

            assertNotNull(result);
            verify(dtlRepository, times(1)).deleteByTransactionPoidAndDetRowId(42418L, 1L);
        }
    }

    @Test
    void updateTelexFile_WithDetailsNoChanges_NullDetRowId() {
        detailDto.setActionType("NOCHANGES");
        detailDto.setDetRowId(null);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedTxManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);
            when(dtlRepository.findMaxDetRowIdByTransactionPoid(42418L)).thenReturn(5L);
            when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Arrays.asList(dtlEntity));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);
            doNothing().when(loggingService).logChanges(any(), any(), any(), anyString(), anyString(), any(), anyString());

            TelexFileGenerateResponseDto result = service.updateTelexFile(42418L, requestDto);

            assertNotNull(result);
            verify(dtlRepository, never()).saveAll(anyList());
        }
    }

    @Test
    void updateTelexFile_NotFound() {
        when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.empty());

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            service.updateTelexFile(42418L, requestDto);
        });

        assertTrue(exception.getMessage().contains("Telex File not found"));
    }

    @Test
    void getTelexFileById_Success() {
        when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
        when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Arrays.asList(dtlEntity));
        when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
        when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);

        TelexFileGenerateResponseDto result = service.getTelexFileById(42418L);

        assertNotNull(result);
        assertEquals(42418L, result.getTransactionPoid());
        assertEquals(1, result.getDetails().size());
    }

    @Test
    void getTelexFileById_WithoutDetails() {
        when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
        when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Collections.emptyList());
        when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
        when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);

        TelexFileGenerateResponseDto result = service.getTelexFileById(42418L);

        assertNotNull(result);
        assertEquals(0, result.getDetails().size());
    }

    @Test
    void getTelexFileById_NotFound() {
        when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            service.getTelexFileById(42418L);
        });

        assertTrue(exception.getMessage().contains("Telex File not found"));
    }

    @Test
    void softDeleteTelexFile_Success() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Test deletion");

        when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
        when(documentDeleteService.deleteDocument(anyLong(), anyString(), anyString(), any(), any())).thenReturn("SUCCESS");
        when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);

        service.softDeleteTelexFile(42418L, deleteReasonDto);

        verify(documentDeleteService, times(1)).deleteDocument(
                eq(42418L),
                eq("GL_BANK_FILE_HDR"),
                eq("TRANSACTION_POID"),
                eq(deleteReasonDto),
                eq(hdrEntity.getTransactionDate())
        );
        verify(hdrRepository, times(1)).saveAndFlush(any(GlBankFileHdr.class));
    }

    @Test
    void softDeleteTelexFile_NotFound() {
        when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.empty());

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            service.softDeleteTelexFile(42418L, null);
        });

        assertTrue(exception.getMessage().contains("Telex File not found"));
    }

    @Test
    void listTelexFiles_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        Map<String, Object> recordData = new HashMap<>();
        recordData.put("transactionPoid", "42418");
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(recordData);
        Map<String, String> displayFields = new HashMap<>();
        displayFields.put("transactionPoid", "Transaction ID");

        RawSearchResult rawResult = new RawSearchResult(records, displayFields, 1L);

        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveDateFilters(any(), eq("TRANSACTION_DATE"), eq(startDate), eq(endDate)))
                .thenReturn(new ArrayList<>());
        when(documentService.search(anyString(), anyList(), anyString(), any(Pageable.class), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.listTelexFiles("100-153", null, startDate, endDate, pageable);

        assertNotNull(result);
        verify(documentService, times(1)).search(eq("100-153"), anyList(), eq("AND"), eq(pageable), eq("N"), eq("DOC_REF"), eq("TRANSACTION_POID"));
    }

    @Test
    void listTelexFiles_WithoutDateRange() {
        Pageable pageable = PageRequest.of(0, 10);

        List<Map<String, Object>> emptyRecords = new ArrayList<>();
        Map<String, String> emptyDisplayFields = new HashMap<>();
        RawSearchResult rawResult = new RawSearchResult(emptyRecords, emptyDisplayFields, 0L);

        when(documentService.resolveOperator(any())).thenReturn("OR");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveDateFilters(any(), eq("TRANSACTION_DATE"), isNull(), isNull()))
                .thenReturn(new ArrayList<>());
        when(documentService.search(anyString(), anyList(), anyString(), any(Pageable.class), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.listTelexFiles("100-153", null, null, null, pageable);

        assertNotNull(result);
    }

    @Test
    void loadTelexTransferData_Success() {
        List<TelexFileDtlDto> mockData = Arrays.asList(detailDto);
        when(procRepository.loadTelexTransferData("Y")).thenReturn(mockData);

        List<TelexFileDtlDto> result = service.loadTelexTransferData("Y");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(procRepository, times(1)).loadTelexTransferData("Y");
    }

    @Test
    void loadTelexTransferData_EmptyResult() {
        when(procRepository.loadTelexTransferData("Y")).thenReturn(Collections.emptyList());

        List<TelexFileDtlDto> result = service.loadTelexTransferData("Y");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void regenerateTelexFile_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            GlBankDebitHdr debitHdr = new GlBankDebitHdr();
            debitHdr.setTransactionPoid(1001L);

            when(glBankDebitHdrRepository.findByTransactionPoid(1001L)).thenReturn(Optional.of(debitHdr));
            when(procRepository.regenerateTelexFile(1L, 1L, 1L, 1001L)).thenReturn("SUCCESS");

            String result = service.regenerateTelexFile(1001L);

            assertEquals("SUCCESS", result);
            verify(procRepository, times(1)).regenerateTelexFile(1L, 1L, 1L, 1001L);
        }
    }

    @Test
    void regenerateTelexFile_NotFound() {
        when(glBankDebitHdrRepository.findByTransactionPoid(1001L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            service.regenerateTelexFile(1001L);
        });

        assertTrue(exception.getMessage().contains("Telex File not found"));
    }

    @Test
    void generateBankFileButton_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            when(bankFileBatchService.createBankFileBatch(42418L, 1L)).thenReturn("SUCCESS");

            String result = service.generateBankFileButton(42418L);

            assertEquals("SUCCESS", result);
            verify(bankFileBatchService, times(1)).createBankFileBatch(42418L, 1L);
        }
    }

    @Test
    void createTelexFile_TriggerError_GenericError() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class)))
                    .thenThrow(new RuntimeException());

            ValidationException exception = assertThrows(ValidationException.class, () -> {
                service.createTelexFile(requestDto);
            });

            assertEquals("Database operation failed", exception.getMessage());
        }
    }

    @Test
    void updateTelexFile_WithDetailsNoChanges_WithDetRowId() {
        detailDto.setActionType("NOCHANGES");
        detailDto.setDetRowId(1L);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedTxManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);
            when(dtlRepository.findMaxDetRowIdByTransactionPoid(42418L)).thenReturn(5L);
            when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Arrays.asList(dtlEntity));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);
            doNothing().when(loggingService).logChanges(any(), any(), any(), anyString(), anyString(), any(), anyString());

            TelexFileGenerateResponseDto result = service.updateTelexFile(42418L, requestDto);

            assertNotNull(result);
            verify(dtlRepository, never()).saveAll(anyList());
        }
    }

    @Test
    void createTelexFile_WithNullBankList() {
        requestDto.setBankList(null);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");
            mockedTxManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);
            when(dtlRepository.saveAll(anyList())).thenReturn(Arrays.asList(dtlEntity));
            when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
            when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Arrays.asList(dtlEntity));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);
            doNothing().when(loggingService).createLogSummaryEntry(anyString(), anyString(), anyString());
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            TelexFileGenerateResponseDto result = service.createTelexFile(requestDto);

            assertNotNull(result);
            assertEquals("Y", result.getBankList());
        }
    }

    @Test
    void updateTelexFile_WithNullBankList() {
        requestDto.setBankList(null);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedTxManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);
            when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Arrays.asList(dtlEntity));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);

            TelexFileGenerateResponseDto result = service.updateTelexFile(42418L, requestDto);

            assertNotNull(result);
        }
    }

    @Test
    void createTelexFile_WithDetailsHavingNullDeletedAndSelected() {
        detailDto.setDeleted(null);
        detailDto.setSelected(null);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");
            mockedTxManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);
            when(dtlRepository.saveAll(anyList())).thenReturn(Arrays.asList(dtlEntity));
            when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
            when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Arrays.asList(dtlEntity));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);
            doNothing().when(loggingService).createLogSummaryEntry(anyString(), anyString(), anyString());
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            TelexFileGenerateResponseDto result = service.createTelexFile(requestDto);

            assertNotNull(result);
        }
    }

    @Test
    void createTelexFile_UserPoidNull() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(null);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");
            mockedTxManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);
            when(dtlRepository.saveAll(anyList())).thenReturn(Arrays.asList(dtlEntity));
            when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
            when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Arrays.asList(dtlEntity));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);
            doNothing().when(loggingService).createLogSummaryEntry(anyString(), anyString(), anyString());
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            TelexFileGenerateResponseDto result = service.createTelexFile(requestDto);

            assertNotNull(result);
        }
    }

    @Test
    void updateTelexFile_UserPoidNull() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(null);
            mockedTxManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);
            when(dtlRepository.findByTransactionPoid(42418L)).thenReturn(Arrays.asList(dtlEntity));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(lovDto);

            TelexFileGenerateResponseDto result = service.updateTelexFile(42418L, requestDto);

            assertNotNull(result);
        }
    }

    @Test
    void regenerateTelexFile_UserPoidNull() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(null);

            GlBankDebitHdr debitHdr = new GlBankDebitHdr();
            debitHdr.setTransactionPoid(1001L);

            when(glBankDebitHdrRepository.findByTransactionPoid(1001L)).thenReturn(Optional.of(debitHdr));
            when(procRepository.regenerateTelexFile(1L, 1L, 1L, 1001L)).thenReturn("SUCCESS");

            String result = service.regenerateTelexFile(1001L);

            assertEquals("SUCCESS", result);
        }
    }

    @Test
    void generateBankFileButton_UserPoidNull() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(null);

            when(bankFileBatchService.createBankFileBatch(42418L, 1L)).thenReturn("SUCCESS");

            String result = service.generateBankFileButton(42418L);

            assertEquals("SUCCESS", result);
        }
    }

    @Test
    void updateTelexFile_WithDetailsUpdatedNotFound() {
        detailDto.setActionType("ISUPDATED");
        detailDto.setDetRowId(999L);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedTxManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            when(hdrRepository.findByTransactionPoid(42418L)).thenReturn(Optional.of(hdrEntity));
            when(hdrRepository.saveAndFlush(any(GlBankFileHdr.class))).thenReturn(hdrEntity);
            when(dtlRepository.findByTransactionPoidAndDetRowId(42418L, 999L)).thenReturn(Optional.empty());

            ValidationException exception = assertThrows(ValidationException.class, () -> {
                service.updateTelexFile(42418L, requestDto);
            });

            assertTrue(exception.getMessage().contains("Detail not found"));
        }
    }
}
