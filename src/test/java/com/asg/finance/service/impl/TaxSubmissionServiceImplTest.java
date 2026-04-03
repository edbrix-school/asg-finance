package com.asg.finance.service.impl;

import com.asg.common.lib.dto.CompanyDto;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.entity.DocumentEntity;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.repository.DocumentCommonRepository;
import com.asg.common.lib.repository.TableMetaRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.DateUtil;
import com.asg.finance.client.CompanyServiceClient;
import com.asg.finance.dto.CreateTaxSubmissionRequest;
import com.asg.finance.dto.LoadVatDetailsResponse;
import com.asg.finance.dto.SubmitTaxSubmissionRequest;
import com.asg.finance.dto.SubmitTaxSubmissionResponse;
import com.asg.finance.dto.TaxSubmissionResponse;
import com.asg.finance.dto.UpdateTaxSubmissionRequest;
import com.asg.finance.dto.ValidatePeriodRequest;
import com.asg.finance.dto.ValidatePeriodResponse;
import com.asg.finance.entity.GlobalTaxSubmissionDtl;
import com.asg.finance.entity.GlobalTaxSubmissionHdr;
import com.asg.finance.repository.GlobalTaxSubmissionDtlRepository;
import com.asg.finance.repository.GlobalTaxSubmissionHdrRepository;
import com.asg.finance.service.PeriodValidationHelper;
import com.asg.finance.service.TaxSubmissionStoredProcedureHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaxSubmissionServiceImplTest {

    @Mock
    private GlobalTaxSubmissionHdrRepository hdrRepository;
    @Mock
    private GlobalTaxSubmissionDtlRepository dtlRepository;
    @Mock
    private TaxSubmissionStoredProcedureHelper storedProcedureHelper;
    @Mock
    private PeriodValidationHelper periodValidationHelper;
    @Mock
    private DocumentSearchService documentService;
    @Mock
    private TableMetaRepository tableMetaRepository;
    @Mock
    private DocumentCommonRepository documentRepository;
    @Mock
    private CompanyServiceClient companyServiceClient;
    @Mock
    private LoggingService loggingService;
    @Mock
    private DocumentDeleteService documentDeleteService;

    @InjectMocks
    private TaxSubmissionServiceImpl service;

    @Test
    void createTaxSubmission_Success() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 1, 8, 30);
        CreateTaxSubmissionRequest request = new CreateTaxSubmissionRequest(
                null,
                now,
                now.plusDays(30),
                "remarks",
                "TS-001");

        CompanyDto company = new CompanyDto();
        company.setVatFilingPeriod("1");

        GlobalTaxSubmissionHdr saved = new GlobalTaxSubmissionHdr();
        saved.setTransactionPoid(101L);
        saved.setCompanyPoid(2L);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class);
             MockedStatic<DateUtil> dateUtil = mockStatic(DateUtil.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getCompanyPoid).thenReturn(2L);
            userContext.when(UserContext::getUserId).thenReturn("tester");
            userContext.when(UserContext::getDocumentId).thenReturn("400-118");
            dateUtil.when(DateUtil::getCurrentDateTimeInUserTimeZone).thenReturn(now);

            when(companyServiceClient.findById(2L)).thenReturn(company);
            when(periodValidationHelper.validatePeriodRules(any(), any(), eq(1))).thenReturn(List.of());
            when(hdrRepository.findOverlappingPeriods(anyLong(), anyLong(), any(), any())).thenReturn(List.of());
            when(storedProcedureHelper.validateBeforeSave(anyLong(), anyLong(), any(), any(), any(), any())).thenReturn("Success");
            when(hdrRepository.save(any(GlobalTaxSubmissionHdr.class))).thenReturn(saved);

            TaxSubmissionResponse response = service.createTaxSubmission(request);

            assertEquals(101L, response.getTransactionPoid());
            verify(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), eq("400-118"), eq("101"));
        }
    }

    @Test
    void createTaxSubmission_ThrowsWhenBeforeSaveHasError() {
        CreateTaxSubmissionRequest request = new CreateTaxSubmissionRequest(
                2L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(10),
                "remarks",
                null);

        CompanyDto company = new CompanyDto();
        company.setVatFilingPeriod("1");

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getCompanyPoid).thenReturn(2L);
            userContext.when(UserContext::getUserId).thenReturn("tester");

            when(companyServiceClient.findById(2L)).thenReturn(company);
            when(periodValidationHelper.validatePeriodRules(any(), any(), eq(1))).thenReturn(List.of());
            when(hdrRepository.findOverlappingPeriods(anyLong(), anyLong(), any(), any())).thenReturn(List.of());
            when(storedProcedureHelper.validateBeforeSave(anyLong(), anyLong(), any(), any(), any(), any()))
                    .thenReturn("ERROR: invalid");

            assertThrows(ValidationException.class, () -> service.createTaxSubmission(request));
        }
    }

    @Test
    void getTaxSubmissionById_ThrowsWhenMissing() {
        when(hdrRepository.findByTransactionPoid(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getTaxSubmissionById(999L));
    }

    @Test
    void updateTaxSubmission_SuccessAndClearsDetailsWhenPeriodChanges() {
        GlobalTaxSubmissionHdr header = new GlobalTaxSubmissionHdr();
        header.setTransactionPoid(10L);
        header.setCompanyPoid(2L);
        header.setGroupPoid(1L);
        header.setApprovalStatus("PENDING");
        header.setStatus("DRAFT");
        header.setPeriodFrom(LocalDateTime.of(2026, 1, 1, 0, 0));
        header.setPeriodTo(LocalDateTime.of(2026, 1, 31, 0, 0));

        UpdateTaxSubmissionRequest request = new UpdateTaxSubmissionRequest(
                LocalDateTime.of(2026, 2, 1, 10, 0),
                LocalDateTime.of(2026, 2, 28, 16, 0),
                "updated");

        CompanyDto company = new CompanyDto();
        company.setVatFilingPeriod("1");

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserId).thenReturn("tester");
            userContext.when(UserContext::getDocumentId).thenReturn("400-118");

            when(hdrRepository.findByTransactionPoidAndGroupPoid(10L, 1L)).thenReturn(Optional.of(header));
            when(companyServiceClient.findById(2L)).thenReturn(company);
            when(periodValidationHelper.validatePeriodRules(any(), any(), eq(1))).thenReturn(List.of());
            when(hdrRepository.findOverlappingPeriodsExcluding(anyLong(), anyLong(), any(), any(), anyLong())).thenReturn(List.of());
            when(storedProcedureHelper.validateBeforeSave(anyLong(), anyLong(), any(), any(), any(), anyLong())).thenReturn("Success");
            when(hdrRepository.save(any(GlobalTaxSubmissionHdr.class))).thenAnswer(inv -> inv.getArgument(0));
            when(dtlRepository.findByTransactionPoid(10L)).thenReturn(List.of());

            TaxSubmissionResponse response = service.updateTaxSubmission(10L, request);

            assertNotNull(response);
            verify(dtlRepository).deleteByTransactionPoid(10L);
            verify(loggingService).logChanges(any(), any(), any(), eq("400-118"), eq("10"), any(), eq("TRANSACTION_POID"));
        }
    }

    @Test
    void deleteTaxSubmission_Success() {
        GlobalTaxSubmissionHdr header = new GlobalTaxSubmissionHdr();
        header.setTransactionPoid(12L);
        header.setGroupPoid(1L);
        header.setApprovalStatus("PENDING");
        header.setStatus("DRAFT");
        header.setTransactionDate(LocalDateTime.now());

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getDocumentId).thenReturn("400-118");

            when(hdrRepository.findByTransactionPoidAndGroupPoid(12L, 1L)).thenReturn(Optional.of(header));

            service.deleteTaxSubmission(12L, new DeleteReasonDto());

            verify(documentDeleteService).deleteDocument(eq(12L), eq("GLOBAL_TAX_SUBMISSION_HDR"), eq("TRANSACTION_POID"), any(), any());
            verify(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), eq("400-118"), eq("12"));
        }
    }

    @Test
    void listTaxSubmission_ThrowsWhenOnlyOnePeriodProvided() {
        assertThrows(ValidationException.class,
                () -> service.listTaxSubmission(null, PageRequest.of(0, 10), LocalDate.now(), null));
    }

    @Test
    void listTaxSubmission_ThrowsWhenPeriodFromAfterPeriodTo() {
        assertThrows(ValidationException.class,
                () -> service.listTaxSubmission(null, PageRequest.of(0, 10),
                        LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)));
    }

    @Test
    void listTaxSubmission_Success_WithSearchAndSorting() {
        FilterRequestDto filters = new FilterRequestDto(
                "OR",
                "N",
                List.of(
                        new FilterDto("GLOBALSEARCH", "2026"),
                        new FilterDto("TRANSACTION_POID", ">=10"),
                        new FilterDto("DOC_REF", "TS-1|TS-2")
                )
        );

        PageRequest pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "DOC_REF"));

        when(documentService.resolveOperator(filters)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filters)).thenReturn("N");
        when(documentService.resolveDateFilters(eq(filters), eq("TRANSACTION_DATE"), any(), any()))
                .thenReturn(new ArrayList<>(filters.filters()));

        when(tableMetaRepository.getColumnsFromTable("GLOBAL_TAX_SUBMISSION_HDR"))
                .thenReturn(List.of("TRANSACTION_POID", "DOC_REF", "CREATED_BY", "TRANSACTION_DATE", "GROUP_POID", "COMPANY_POID"));
        HashMap<String, Object> mutableRow = new HashMap<>();
        mutableRow.put("TRANSACTION_POID", 1L);
        mutableRow.put("DOC_REF", "TS-001");
        mutableRow.put("CREATED_BY", "tester");
        when(tableMetaRepository.executeDynamicQuery(any(), any(), any()))
            .thenReturn(List.of(mutableRow));
        when(tableMetaRepository.executeCountQuery(any(), any())).thenReturn(1L);

        DocumentEntity doc = new DocumentEntity();
        doc.setDocId("400-118");
        doc.setListOfDisplayColumnsAndTypes("<DOC_REF,Doc Ref>|<CREATED_BY,Created By>");
        when(documentRepository.findByDocId("400-118")).thenReturn(doc);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("400-118");

            Map<String, Object> result = service.listTaxSubmission(filters, pageable,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

            assertNotNull(result);
            verify(tableMetaRepository).executeDynamicQuery(any(), any(), any());
        }
    }

    @Test
    void loadVatDetails_Success() {
        GlobalTaxSubmissionHdr header = new GlobalTaxSubmissionHdr();
        header.setTransactionPoid(20L);
        header.setCompanyPoid(2L);
        header.setGroupPoid(1L);
        header.setApprovalStatus("PENDING");
        header.setStatus("DRAFT");
        header.setPeriodFrom(LocalDateTime.of(2026, 1, 1, 0, 0));
        header.setPeriodTo(LocalDateTime.of(2026, 1, 30, 0, 0));

        Map<String, Object> detailRow = Map.of(
                "DET_ROW_ID", 1L,
                "TAX_TYPE", "VAT",
                "TAX_POID", 100L,
                "TAX_CODE", "VAT-01",
                "TAX_DESCRIPTION", "Value Added Tax",
                "TAX_PERCENTAGE", "5",
                "TAX_BASE_AMOUNT", new BigDecimal("100.00"),
                "TAX_AMOUNT", new BigDecimal("5.00"),
                "TOTAL_AMOUNT", new BigDecimal("105.00"),
                "REMARKS", "ok"
        );

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserPoid).thenReturn(999L);
            userContext.when(UserContext::getUserId).thenReturn("tester");

            when(hdrRepository.findByTransactionPoidAndGroupPoid(20L, 1L)).thenReturn(Optional.of(header));
            when(storedProcedureHelper.loadVatDetails(anyLong(), anyLong(), anyLong(), anyLong(), any(), any()))
                    .thenReturn(List.of(detailRow));
            when(dtlRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            LoadVatDetailsResponse response = service.loadVatDetails(20L);

            assertEquals("SUCCESS", response.getStatus());
            assertEquals(1, response.getDetails().size());
            verify(dtlRepository).deleteByTransactionPoid(20L);
        }
    }

    @Test
    void submitTaxSubmission_Approve_SetsApprovedStatus() {
        GlobalTaxSubmissionHdr header = new GlobalTaxSubmissionHdr();
        header.setTransactionPoid(30L);
        header.setGroupPoid(1L);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(hdrRepository.findByTransactionPoidAndGroupPoid(30L, 1L)).thenReturn(Optional.of(header));
            when(dtlRepository.findByTransactionPoid(30L)).thenReturn(List.of(new GlobalTaxSubmissionDtl()));

            SubmitTaxSubmissionResponse response = service.submitTaxSubmission(30L, new SubmitTaxSubmissionRequest("APPROVE", "ok"));

            assertEquals("APPROVED", response.getStatus());
            assertEquals("APPROVED", response.getApprovalStatus());
        }
    }

    @Test
    void submitTaxSubmission_ThrowsWhenNoDetails() {
        GlobalTaxSubmissionHdr header = new GlobalTaxSubmissionHdr();

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            when(hdrRepository.findByTransactionPoidAndGroupPoid(31L, 1L)).thenReturn(Optional.of(header));
            when(dtlRepository.findByTransactionPoid(31L)).thenReturn(List.of());

            assertThrows(ValidationException.class,
                    () -> service.submitTaxSubmission(31L, new SubmitTaxSubmissionRequest("SUBMIT", "x")));
        }
    }

    @Test
    void runAfterSave_ThrowsWhenProcedureReturnsWarning() {
        GlobalTaxSubmissionHdr header = new GlobalTaxSubmissionHdr();
        header.setTransactionPoid(40L);
        header.setCompanyPoid(2L);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserId).thenReturn("tester");

            when(hdrRepository.findByTransactionPoidAndGroupPoid(40L, 1L)).thenReturn(Optional.of(header));
            when(storedProcedureHelper.processAfterSave(1L, 2L, "tester", 40L)).thenReturn("WARNING: data");

            assertThrows(ValidationException.class, () -> service.runAfterSave(40L));
        }
    }

    @Test
    void runAfterSave_Success() {
        GlobalTaxSubmissionHdr header = new GlobalTaxSubmissionHdr();
        header.setTransactionPoid(41L);
        header.setCompanyPoid(2L);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserId).thenReturn("tester");

            when(hdrRepository.findByTransactionPoidAndGroupPoid(41L, 1L))
                    .thenReturn(Optional.of(header), Optional.of(header));
            when(storedProcedureHelper.processAfterSave(1L, 2L, "tester", 41L)).thenReturn("Success");
            when(dtlRepository.findByTransactionPoid(41L)).thenReturn(List.of());

            TaxSubmissionResponse response = service.runAfterSave(41L);

            assertNotNull(response);
            assertEquals(41L, response.getTransactionPoid());
        }
    }

    @Test
    void validatePeriod_ReturnsInvalidWhenOverlapExists() {
        ValidatePeriodRequest request = new ValidatePeriodRequest(
                2L,
                LocalDateTime.of(2026, 1, 1, 11, 0),
                LocalDateTime.of(2026, 1, 31, 16, 0));

        CompanyDto company = new CompanyDto();
        company.setVatFilingPeriod("1");

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            when(companyServiceClient.findById(2L)).thenReturn(company);
            when(periodValidationHelper.validatePeriodRules(any(), any(), eq(1))).thenReturn(List.of("period problem"));
            when(hdrRepository.findOverlappingPeriods(anyLong(), anyLong(), any(), any()))
                    .thenReturn(List.of(new GlobalTaxSubmissionHdr()));

            ValidatePeriodResponse response = service.validatePeriod(request);

            assertFalse(response.getValid());
            assertEquals(2, response.getErrors().size());
        }
    }

    @Test
    void validatePeriod_ReturnsValidWhenNoErrors() {
        ValidatePeriodRequest request = new ValidatePeriodRequest(
                2L,
                LocalDateTime.of(2026, 3, 1, 11, 0),
                LocalDateTime.of(2026, 3, 31, 16, 0));

        CompanyDto company = new CompanyDto();
        company.setVatFilingPeriod("1");

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            when(companyServiceClient.findById(2L)).thenReturn(company);
            when(periodValidationHelper.validatePeriodRules(any(), any(), eq(1))).thenReturn(List.of());
            when(hdrRepository.findOverlappingPeriods(anyLong(), anyLong(), any(), any())).thenReturn(List.of());

            ValidatePeriodResponse response = service.validatePeriod(request);

            assertTrue(response.getValid());
            assertEquals("Period is valid", response.getMessage());
        }
    }

    @Test
    void loadVatDetails_ThrowsWhenPeriodTooLong() {
        GlobalTaxSubmissionHdr header = new GlobalTaxSubmissionHdr();
        header.setTransactionPoid(50L);
        header.setCompanyPoid(2L);
        header.setGroupPoid(1L);
        header.setApprovalStatus("PENDING");
        header.setStatus("DRAFT");
        header.setPeriodFrom(LocalDateTime.of(2026, 1, 1, 0, 0));
        header.setPeriodTo(LocalDateTime.of(2026, 2, 15, 0, 0));

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserPoid).thenReturn(999L);
            userContext.when(UserContext::getUserId).thenReturn("tester");
            when(hdrRepository.findByTransactionPoidAndGroupPoid(50L, 1L)).thenReturn(Optional.of(header));

            assertThrows(ValidationException.class, () -> service.loadVatDetails(50L));
        }
    }

    @Test
    void updateTaxSubmission_ThrowsWhenClosedPeriod() {
        GlobalTaxSubmissionHdr header = new GlobalTaxSubmissionHdr();
        header.setTransactionPoid(60L);
        header.setCompanyPoid(2L);
        header.setGroupPoid(1L);
        header.setPeriodClosedDate(LocalDateTime.now());

        UpdateTaxSubmissionRequest request = new UpdateTaxSubmissionRequest(
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                "x");

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserId).thenReturn("tester");
            when(hdrRepository.findByTransactionPoidAndGroupPoid(60L, 1L)).thenReturn(Optional.of(header));

            assertThrows(ValidationException.class, () -> service.updateTaxSubmission(60L, request));
        }
    }

    @Test
    void createTaxSubmission_ThrowsWhenCompanyMissingInRequestAndContext() {
        CreateTaxSubmissionRequest request = new CreateTaxSubmissionRequest(
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                "remarks",
                null);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getCompanyPoid).thenReturn(null);
            userContext.when(UserContext::getUserId).thenReturn("tester");

            assertThrows(ValidationException.class, () -> service.createTaxSubmission(request));
        }
    }

    @Test
    void createTaxSubmission_ThrowsWhenPeriodFromMissing() {
        CreateTaxSubmissionRequest request = new CreateTaxSubmissionRequest(
                2L,
                null,
                LocalDateTime.now().plusDays(1),
                "remarks",
                null);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getCompanyPoid).thenReturn(2L);
            userContext.when(UserContext::getUserId).thenReturn("tester");

            assertThrows(ValidationException.class, () -> service.createTaxSubmission(request));
        }
    }

    @Test
    void loadVatDetails_ThrowsWhenPeriodMissing() {
        GlobalTaxSubmissionHdr header = new GlobalTaxSubmissionHdr();
        header.setTransactionPoid(70L);
        header.setCompanyPoid(2L);
        header.setGroupPoid(1L);
        header.setApprovalStatus("PENDING");
        header.setStatus("DRAFT");
        header.setPeriodFrom(null);
        header.setPeriodTo(null);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserPoid).thenReturn(999L);
            userContext.when(UserContext::getUserId).thenReturn("tester");
            when(hdrRepository.findByTransactionPoidAndGroupPoid(70L, 1L)).thenReturn(Optional.of(header));

            assertThrows(ValidationException.class, () -> service.loadVatDetails(70L));
        }
    }

    @Test
    void privateHelpers_ParseAndNormalize_AreCovered() {
        String[] cmp = ReflectionTestUtils.invokeMethod(service, "parseComparison", ">= 2026-01-01");
        assertEquals(">=", cmp[0]);
        assertEquals("2026-01-01", cmp[1]);

        LocalDateTime normalized = ReflectionTestUtils.invokeMethod(service, "normalizeToMidnight",
                LocalDateTime.of(2026, 4, 1, 11, 22, 33));
        assertEquals(LocalDateTime.of(2026, 4, 1, 0, 0), normalized);

        LocalDateTime normalizedNull = ReflectionTestUtils.invokeMethod(service, "normalizeToMidnight", new Object[]{null});
        assertEquals(null, normalizedNull);
    }

    @Test
    void privateHelpers_ParseDisplayAndCondition_AreCovered() {
        Map<String, String> display = ReflectionTestUtils.invokeMethod(service, "parseDisplayColumns",
                "<DOC_REF,Doc Ref>|<CREATED_BY,Created By>|invalid");
        assertEquals("Doc Ref", display.get("DOC_REF"));
        assertEquals("Created By", display.get("CREATED_BY"));

        List<Object> params = new ArrayList<>();
        String cond = ReflectionTestUtils.invokeMethod(service,
                "buildTaxSubmissionFieldCondition",
                "TRANSACTION_POID", "=", "100|200", false, params);
        assertTrue(cond.contains("TRANSACTION_POID = ?"));
        assertEquals(2, params.size());
    }

    @Test
    void privateHelpers_GetVatFilingPeriod_HandlesInvalidNumber() {
        CompanyDto company = new CompanyDto();
        company.setVatFilingPeriod("x1");
        when(companyServiceClient.findById(55L)).thenReturn(company);

        Integer filingPeriod = ReflectionTestUtils.invokeMethod(service, "getVatFilingPeriod", 55L);

        assertEquals(null, filingPeriod);
    }
}
