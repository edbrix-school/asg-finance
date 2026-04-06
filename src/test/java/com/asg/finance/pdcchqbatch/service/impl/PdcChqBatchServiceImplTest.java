package com.asg.finance.pdcchqbatch.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.dto.PayGlBreakupCheckResponseDto;
import com.asg.finance.dto.PdcBankPostingProcRequest;
import com.asg.finance.dto.PdcBatchCreationExcelProcRequest;
import com.asg.finance.dto.PdcBatchCreationProcRequest;
import com.asg.finance.dto.PdcBatchCreationProcResponse;
import com.asg.finance.dto.PdcChqBatchDtlRequestDto;
import com.asg.finance.dto.PdcChqBatchHdrRequestDto;
import com.asg.finance.dto.PdcChqBatchHdrResponseDto;
import com.asg.finance.entity.PdcChqBatchDtlEntity;
import com.asg.finance.entity.PdcChqBatchHdrEntity;
import com.asg.finance.repository.PdcBatchCreationRepository;
import com.asg.finance.repository.PdcBatchExcelUploadTempRepository;
import com.asg.finance.repository.PdcChqBatchDtlRepository;
import com.asg.finance.repository.PdcChqBatchHdrRepository;
import com.asg.finance.service.impl.PdcChqBatchServiceImpl;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdcChqBatchServiceImplTest {

    @Mock
    private PdcChqBatchHdrRepository hdrRepo;

    @Mock
    private PdcChqBatchDtlRepository dtlRepo;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private PdcBatchCreationRepository pdcBatchCreationRepository;

    @Mock
    private PdcBatchExcelUploadTempRepository tempRepo;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private PdcChqBatchServiceImpl service;

    private PdcChqBatchHdrRequestDto requestDto;
    private PdcChqBatchDtlRequestDto createdDetail;
    private PdcChqBatchDtlRequestDto updatedDetail;
    private PdcChqBatchDtlRequestDto deletedDetail;
    private PdcChqBatchHdrEntity hdrEntity;
    private PdcChqBatchDtlEntity dtlEntity;

    @BeforeEach
    void setUp() {
        createdDetail = PdcChqBatchDtlRequestDto.builder()
                .actionType("ISCREATED")
                .pdcChqDate(LocalDate.of(2026, 4, 1))
                .chqNumber("100001")
                .chqAmount(1000.0)
                .remarks("created")
                .drGlPoid1(11L)
                .drAmt1(1000.0)
                .crGlPoid(21L)
                .crAmt(1000.0)
                .costPoid("C1")
                .build();

        updatedDetail = PdcChqBatchDtlRequestDto.builder()
                .detRowId(1L)
                .actionType("ISUPDATED")
                .pdcChqDate(LocalDate.of(2026, 4, 2))
                .chqNumber("100002")
                .chqAmount(1000.0)
                .remarks("updated")
                .drGlPoid1(12L)
                .drAmt1(1000.0)
                .crGlPoid(22L)
                .crAmt(1000.0)
                .costPoid("C2")
                .build();

        deletedDetail = PdcChqBatchDtlRequestDto.builder()
                .detRowId(2L)
                .actionType("ISDELETED")
                .drAmt1(0.0)
                .crAmt(0.0)
                .build();

        requestDto = PdcChqBatchHdrRequestDto.builder()
                .transactionDate(LocalDate.of(2026, 4, 1).atStartOfDay())
                .groupPoid(1L)
                .companyPoid(2L)
                .payGlPoid(101L)
                .payingTo("Vendor A")
                .payingType("SUPPLIER")
                .divisionCode("DIV")
                .bankPoid(202L)
                .chqStartNo("123456")
                .chqStartDate(LocalDate.of(2026, 4, 1))
                .chqAmount(1000.0)
                .noOfChqs(1L)
                .narration("Test batch")
                .billType("GENERAL")
                .billRef("BILL-1")
                .costGroup("CG")
                .costPoid("CP")
                .prePrinted("Y")
                .accountPayee("Y")
                .confidentialRemarks("note")
                .chequeDetails(new ArrayList<>(List.of(createdDetail)))
                .build();

        hdrEntity = PdcChqBatchHdrEntity.builder()
                .transactionPoid(999L)
                .transactionDate(LocalDate.of(2026, 4, 1).atStartOfDay())
                .groupPoid(1L)
                .companyPoid(2L)
                .docRef("PDC-001")
                .payGlPoid(101L)
                .payingTo("Vendor A")
                .payingType("SUPPLIER")
                .divisionCode("DIV")
                .bankPoid(202L)
                .chqStartNo("123456")
                .chqStartDate(LocalDate.of(2026, 4, 1))
                .chqAmount(1000.0)
                .noOfChqs(1L)
                .totalAmount(1000.0)
                .narration("Test batch")
                .deleted("N")
                .billType("GENERAL")
                .billRef("BILL-1")
                .costGroup("CG")
                .costPoid("CP")
                .prePrinted("Y")
                .accountPayee("Y")
                .confidentialRemarks("note")
                .build();

        dtlEntity = PdcChqBatchDtlEntity.builder()
                .transactionPoid(999L)
                .detRowId(1L)
                .pdcChqDate(LocalDate.of(2026, 4, 1))
                .chqNumber("100001")
                .chqAmount(1000.0)
                .remarks("created")
                .drGlPoid1(11L)
                .drAmt1(1000.0)
                .crGlPoid(21L)
                .crAmt(1000.0)
                .costPoid("C1")
                .build();
    }

    @Test
    void createBatch_Success() {
        when(hdrRepo.save(any(PdcChqBatchHdrEntity.class))).thenReturn(hdrEntity);
        when(dtlRepo.findByTransactionPoidOrderByDetRowIdAsc(999L)).thenReturn(Collections.emptyList());
        when(dtlRepo.save(any(PdcChqBatchDtlEntity.class))).thenReturn(dtlEntity);

        PdcChqBatchHdrResponseDto result = service.createBatch(requestDto);

        assertNotNull(result);
        assertEquals(999L, result.getTransactionPoid());
        assertEquals("PDC-001", result.getDocRef());
        assertEquals(1, result.getChequeDetails().size());
        verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "400-113", "999");
        verify(loggingService).createLogSummaryEntry(eq("400-113"), eq("999"), anyString());
    }

    @Test
    void createBatch_PrePrintedChequeNumberInvalid() {
        requestDto.setChqStartNo("123");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.createBatch(requestDto));

        assertEquals("Cheque Start No must be 6 digits when Manual Cheque is selected.", exception.getMessage());
    }

    @Test
    void createBatch_DrCrMismatch() {
        createdDetail.setCrAmt(500.0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.createBatch(requestDto));

        assertEquals("Debit and Credit total must be equal.", exception.getMessage());
    }

    @Test
    void createBatch_DefaultsNullActionAndDates() {
        createdDetail.setActionType(null);
        createdDetail.setPdcChqDate(null);
        requestDto.setTransactionDate(null);
        requestDto.setChqStartDate(null);

        when(hdrRepo.save(any(PdcChqBatchHdrEntity.class))).thenAnswer(invocation -> {
            PdcChqBatchHdrEntity saved = invocation.getArgument(0);
            assertNotNull(saved.getTransactionDate());
            assertNotNull(saved.getChqStartDate());
            return hdrEntity;
        });
        when(dtlRepo.findByTransactionPoidOrderByDetRowIdAsc(999L)).thenReturn(Collections.emptyList());
        when(dtlRepo.save(any(PdcChqBatchDtlEntity.class))).thenAnswer(invocation -> {
            PdcChqBatchDtlEntity saved = invocation.getArgument(0);
            assertNotNull(saved.getPdcChqDate());
            return dtlEntity;
        });

        PdcChqBatchHdrResponseDto result = service.createBatch(requestDto);

        assertNotNull(result);
        verify(dtlRepo).save(any(PdcChqBatchDtlEntity.class));
    }

    @Test
    void updateBatch_Success_WithCreateUpdateDeleteActions() {
        requestDto.setChequeDetails(List.of(createdDetail, updatedDetail, deletedDetail));

        PdcChqBatchDtlEntity existingForUpdate = PdcChqBatchDtlEntity.builder()
                .transactionPoid(999L)
                .detRowId(1L)
                .pdcChqDate(LocalDate.of(2026, 4, 1))
                .chqNumber("100001")
                .chqAmount(1000.0)
                .remarks("old")
                .drGlPoid1(11L)
                .drAmt1(1000.0)
                .crGlPoid(21L)
                .crAmt(1000.0)
                .build();
        PdcChqBatchDtlEntity existingDeleted = PdcChqBatchDtlEntity.builder()
                .transactionPoid(999L)
                .detRowId(2L)
                .build();

        when(hdrRepo.findById(999L)).thenReturn(java.util.Optional.of(hdrEntity));
        when(hdrRepo.save(any(PdcChqBatchHdrEntity.class))).thenReturn(hdrEntity);
        when(dtlRepo.findByTransactionPoidOrderByDetRowIdAsc(999L))
                .thenReturn(List.of(existingForUpdate, existingDeleted))
                .thenReturn(List.of(existingForUpdate, existingDeleted));
        when(dtlRepo.save(any(PdcChqBatchDtlEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PdcChqBatchHdrResponseDto result = service.updateBatch(999L, requestDto);

        assertNotNull(result);
        verify(dtlRepo).deleteByTransactionPoid(999L);
        verify(dtlRepo, times(2)).save(any(PdcChqBatchDtlEntity.class));
        verify(dtlRepo).deleteByTransactionPoidAndDetRowId(999L, 2L);
        verify(loggingService).logDelete(eq(deletedDetail), eq("400-113"), eq("999"));
        verify(loggingService).createLogBatch(anyList());
        verify(loggingService).logChanges(any(PdcChqBatchHdrEntity.class), any(PdcChqBatchHdrEntity.class),
                eq(PdcChqBatchHdrEntity.class), eq("400-113"), eq("999"), eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
    }

    @Test
    void updateBatch_HeaderNotFound() {
        when(hdrRepo.findById(999L)).thenReturn(java.util.Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.updateBatch(999L, requestDto));

        assertTrue(exception.getMessage().contains("PDC Batch not found"));
    }

    @Test
    void updateBatch_UpdatedDetailNotFound() {
        requestDto.setChequeDetails(List.of(updatedDetail));
        when(hdrRepo.findById(999L)).thenReturn(java.util.Optional.of(hdrEntity));
        when(hdrRepo.save(any(PdcChqBatchHdrEntity.class))).thenReturn(hdrEntity);
        when(dtlRepo.findByTransactionPoidOrderByDetRowIdAsc(999L)).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> service.updateBatch(999L, requestDto));
    }

    @Test
    void updateBatch_DefaultsNullHeaderDates() {
        requestDto.setTransactionDate(null);
        requestDto.setChqStartDate(null);

        when(hdrRepo.findById(999L)).thenReturn(java.util.Optional.of(hdrEntity));
        when(hdrRepo.save(any(PdcChqBatchHdrEntity.class))).thenAnswer(invocation -> {
            PdcChqBatchHdrEntity saved = invocation.getArgument(0);
            assertNotNull(saved.getTransactionDate());
            assertNotNull(saved.getChqStartDate());
            return hdrEntity;
        });
        when(dtlRepo.findByTransactionPoidOrderByDetRowIdAsc(999L))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());
        when(dtlRepo.save(any(PdcChqBatchDtlEntity.class))).thenReturn(dtlEntity);

        PdcChqBatchHdrResponseDto result = service.updateBatch(999L, requestDto);

        assertNotNull(result);
    }

    @Test
    void findById_Success() {
        when(hdrRepo.findById(999L)).thenReturn(java.util.Optional.of(hdrEntity));
        when(dtlRepo.findByTransactionPoidOrderByDetRowIdAsc(999L)).thenReturn(List.of(dtlEntity));

        PdcChqBatchHdrResponseDto result = service.findById(999L);

        assertNotNull(result);
        assertEquals("Vendor A", result.getPayingTo());
        assertEquals(1, result.getChequeDetails().size());
        assertEquals("100001", result.getChequeDetails().get(0).getChqNumber());
    }

    @Test
    void findById_NotFound() {
        when(hdrRepo.findById(999L)).thenReturn(java.util.Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.findById(999L));

        assertTrue(exception.getMessage().contains("PDC Batch not found"));
    }

    @Test
    void deletePdcBatch_Success() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("cleanup");
        when(hdrRepo.findById(999L)).thenReturn(java.util.Optional.of(hdrEntity));
        when(documentDeleteService.deleteDocument(anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn("true");

        service.deletePdcBatch(999L, deleteReasonDto);

        verify(documentDeleteService).deleteDocument(999L, "GL_PDC_CHQ_BATCH_HDR",
                "TRANSACTION_POID", deleteReasonDto, LocalDate.of(2026, 4, 1));
    }

    @Test
    void deletePdcBatch_NotFound() {
        when(hdrRepo.findById(999L)).thenReturn(java.util.Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.deletePdcBatch(999L, null));

        assertTrue(exception.getMessage().contains("PDC Batch not found"));
    }

    @Test
    void listPdcBatchCreation_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("transactionPoid", 999L)),
                Map.of("transactionPoid", "Transaction Poid"),
                1L
        );

        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveDateFilters(any(), eq("TRANSACTION_DATE"), eq(LocalDate.of(2026, 4, 1)), eq(LocalDate.of(2026, 4, 30))))
                .thenReturn(new ArrayList<>());
        when(documentService.search(eq("400-113"), anyList(), eq("AND"), eq(pageable), eq("N"), eq("REF_TYPE"), eq("TRANSACTION_POID")))
                .thenReturn(raw);

        Map<String, Object> result = service.listPdcBatchCreation("400-113", new FilterRequestDto("AND", "N", Collections.emptyList()),
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), pageable);

        assertNotNull(result);
        assertEquals(1L, result.get("totalElements"));
    }

    @Test
    void listPdcBatchCreation_WithNullFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult raw = new RawSearchResult(Collections.emptyList(), Collections.emptyMap(), 0L);

        when(documentService.resolveOperator(null)).thenReturn("OR");
        when(documentService.resolveIsDeleted(null)).thenReturn("N");
        when(documentService.resolveDateFilters(null, "TRANSACTION_DATE", null, null)).thenReturn(new ArrayList<>());
        when(documentService.search(eq("400-113"), anyList(), eq("OR"), eq(pageable), eq("N"), eq("REF_TYPE"), eq("TRANSACTION_POID")))
                .thenReturn(raw);

        Map<String, Object> result = service.listPdcBatchCreation("400-113", null, null, null, pageable);

        assertNotNull(result);
        assertEquals(0L, result.get("totalElements"));
    }

    @Test
    void validatePayGl_Success() {
        PayGlBreakupCheckResponseDto response = PayGlBreakupCheckResponseDto.builder()
                .result("BILL_WISE")
                .costGroup("CG")
                .build();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(2L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(3L);
            when(pdcBatchCreationRepository.checkPayGlBreakup(1L, 2L, 3L, 101L)).thenReturn(response);

            PayGlBreakupCheckResponseDto result = service.validatePayGl(101L);

            assertEquals("BILL_WISE", result.getResult());
        }
    }

    @Test
    void processBatch_SuccessStatusLoadsDetails() {
        PdcBatchCreationProcRequest request = PdcBatchCreationProcRequest.builder()
                .transactionPoid(999L)
                .build();
        PdcBatchCreationProcResponse proc = PdcBatchCreationProcResponse.builder().status("SUCCESS").build();

        when(hdrRepo.existsByTransactionPoid(999L)).thenReturn(true);
        when(pdcBatchCreationRepository.runBatchCreation(request)).thenReturn(proc);
        when(dtlRepo.findByTransactionPoidOrderByDetRowIdAsc(999L)).thenReturn(List.of(dtlEntity));

        PdcBatchCreationProcResponse result = service.processBatch(request);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals(1, result.getChequeDetails().size());
    }

    @Test
    void processBatch_NonSuccessStatusReturnsEmptyDetails() {
        PdcBatchCreationProcRequest request = PdcBatchCreationProcRequest.builder()
                .transactionPoid(999L)
                .build();
        PdcBatchCreationProcResponse proc = PdcBatchCreationProcResponse.builder().status("ERROR").build();

        when(hdrRepo.existsByTransactionPoid(999L)).thenReturn(true);
        when(pdcBatchCreationRepository.runBatchCreation(request)).thenReturn(proc);

        PdcBatchCreationProcResponse result = service.processBatch(request);

        assertEquals("ERROR", result.getStatus());
        assertTrue(result.getChequeDetails().isEmpty());
        verify(dtlRepo, never()).findByTransactionPoidOrderByDetRowIdAsc(anyLong());
    }

    @Test
    void processBatch_HeaderMissing() {
        PdcBatchCreationProcRequest request = PdcBatchCreationProcRequest.builder()
                .transactionPoid(999L)
                .build();
        when(hdrRepo.existsByTransactionPoid(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.processBatch(request));
    }

    @Test
    void runBankPostingProcedure_Success() {
        PdcBankPostingProcRequest request = PdcBankPostingProcRequest.builder().transactionPoid(999L).build();
        PdcBatchCreationProcResponse proc = PdcBatchCreationProcResponse.builder().status("SUCCESS: posted").build();

        when(hdrRepo.existsByTransactionPoid(999L)).thenReturn(true);
        when(pdcBatchCreationRepository.runBankPosting(request)).thenReturn(proc);
        when(dtlRepo.findByTransactionPoidOrderByDetRowIdAsc(999L)).thenReturn(List.of(dtlEntity));

        PdcBatchCreationProcResponse result = service.runBankPostingProcedure(request);

        assertTrue(result.getStatus().startsWith("SUCCESS"));
        assertEquals(1, result.getChequeDetails().size());
    }

    @Test
    void runBankPostingProcedure_NonSuccessStatusReturnsEmptyDetails() {
        PdcBankPostingProcRequest request = PdcBankPostingProcRequest.builder().transactionPoid(999L).build();
        PdcBatchCreationProcResponse proc = PdcBatchCreationProcResponse.builder().status("ERROR").build();

        when(hdrRepo.existsByTransactionPoid(999L)).thenReturn(true);
        when(pdcBatchCreationRepository.runBankPosting(request)).thenReturn(proc);

        PdcBatchCreationProcResponse result = service.runBankPostingProcedure(request);

        assertEquals("ERROR", result.getStatus());
        assertTrue(result.getChequeDetails().isEmpty());
        verify(dtlRepo, never()).findByTransactionPoidOrderByDetRowIdAsc(anyLong());
    }

    @Test
    void runBankPostingProcedure_HeaderMissing() {
        PdcBankPostingProcRequest request = PdcBankPostingProcRequest.builder().transactionPoid(999L).build();
        when(hdrRepo.existsByTransactionPoid(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.runBankPostingProcedure(request));
    }

    @Test
    void createBatchFromExcel_Success() {
        PdcBatchCreationExcelProcRequest request = new PdcBatchCreationExcelProcRequest();
        request.setTransactionPoid(999L);
        PdcBatchCreationProcResponse proc = PdcBatchCreationProcResponse.builder().status("SUCCESS").build();

        when(hdrRepo.existsByTransactionPoid(999L)).thenReturn(true);
        when(pdcBatchCreationRepository.runBatchCreationXL(request)).thenReturn(proc);
        when(dtlRepo.findByTransactionPoidOrderByDetRowIdAsc(999L)).thenReturn(List.of(dtlEntity));

        PdcBatchCreationProcResponse result = service.createBatchFromExcel(request);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals(1, result.getChequeDetails().size());
    }

    @Test
    void createBatchFromExcel_NonSuccessStatusReturnsEmptyDetails() {
        PdcBatchCreationExcelProcRequest request = new PdcBatchCreationExcelProcRequest();
        request.setTransactionPoid(999L);
        PdcBatchCreationProcResponse proc = PdcBatchCreationProcResponse.builder().status("WARNING").build();

        when(hdrRepo.existsByTransactionPoid(999L)).thenReturn(true);
        when(pdcBatchCreationRepository.runBatchCreationXL(request)).thenReturn(proc);

        PdcBatchCreationProcResponse result = service.createBatchFromExcel(request);

        assertEquals("WARNING", result.getStatus());
        assertTrue(result.getChequeDetails().isEmpty());
        verify(dtlRepo, never()).findByTransactionPoidOrderByDetRowIdAsc(anyLong());
    }

    @Test
    void createBatchFromExcel_HeaderMissing() {
        PdcBatchCreationExcelProcRequest request = new PdcBatchCreationExcelProcRequest();
        request.setTransactionPoid(999L);
        when(hdrRepo.existsByTransactionPoid(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.createBatchFromExcel(request));
    }

    @Test
    void uploadExcel_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pdc.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildWorkbookBytes(new String[][]{
                        {"SN", "Cheque Number", "Cheque Date", "Cheque Amount", "DR1", "DR2"},
                        {"1", "100001", "2026-04-01", "1000", "1000", "0"},
                        {"2", "100002", "2026-04-02", "1000", "1000", "0"}
                })
        );

        String result = service.uploadExcel(file);

        assertEquals("Excel uploaded successfully", result);
        verify(tempRepo).deleteAll();
        verify(tempRepo).saveAll(anyList());
    }

    @Test
    void uploadExcel_EmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.xlsx", "application/octet-stream", new byte[0]);

        String result = service.uploadExcel(file);

        assertEquals("File is empty", result);
        verify(tempRepo, never()).saveAll(anyList());
    }

    @Test
    void uploadExcel_MissingChequeNumber() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pdc.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildWorkbookBytes(new String[][]{
                        {"SN", "Cheque Number", "Cheque Date", "Cheque Amount", "DR1", "DR2"},
                        {"1", "", "2026-04-01", "1000", "1000", "0"}
                })
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.uploadExcel(file));

        assertTrue(exception.getMessage().contains("Cheque number is missing"));
    }

    @Test
    void uploadExcel_DuplicateChequeNumber() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pdc.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildWorkbookBytes(new String[][]{
                        {"SN", "Cheque Number", "Cheque Date", "Cheque Amount", "DR1", "DR2"},
                        {"1", "100001", "2026-04-01", "1000", "1000", "0"},
                        {"2", "100001", "2026-04-02", "1000", "1000", "0"}
                })
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.uploadExcel(file));

        assertTrue(exception.getMessage().contains("Duplicate cheque number in Excel"));
    }

    private byte[] buildWorkbookBytes(String[][] rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("PDC");
            for (int i = 0; i < rows.length; i++) {
                var row = sheet.createRow(i);
                for (int j = 0; j < rows[i].length; j++) {
                    row.createCell(j).setCellValue(rows[i][j]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
