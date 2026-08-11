package com.asg.finance.recurringjv.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.*;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GlRecurringJvDtl;
import com.asg.finance.entity.GlRecurringJvHdr;
import com.asg.finance.entity.GlRecurringJvMonthDtl;
import com.asg.finance.repository.*;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import com.asg.finance.service.impl.GlRecurringJvServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlRecurringJvServiceImplTest {

    @Mock
    private DocumentSearchService documentService;
    @Mock
    private GlRecurringJvHdrRepository hdrRepository;
    @Mock
    private GlRecurringJvDtlRepository dtlRepository;
    @Mock
    private GlRecurringJvMonthDtlRepository monthDtlRepository;
    @Mock
    private GLMasterRepository glMasterRepository;
    @Mock
    private CostCenterBreakupService costCenterBreakupService;
    @Mock
    private BillwiseBreakupService billwiseBreakupService;
    @Mock
    private GlRecurringJvProcRepository procRepository;
    @Mock
    private LovDataService lovService;
    @Mock
    private PrintService printService;
    @Mock
    private DataSource dataSource;
    @Mock
    private LoggingService loggingService;
    @Mock
    private DocumentDeleteService documentDeleteService;

    @InjectMocks
    private GlRecurringJvServiceImpl glRecurringJvService;

    private MockedStatic<ASGHelperUtils> mockedASGHelperUtils;
    private MockedStatic<UserContext> mockedUserContext;

    @BeforeEach
    void setUp() {
        mockedASGHelperUtils = Mockito.mockStatic(ASGHelperUtils.class);
        mockedUserContext = Mockito.mockStatic(UserContext.class);
        
        mockedASGHelperUtils.when(ASGHelperUtils::getGroupId).thenReturn(1L);
        mockedASGHelperUtils.when(ASGHelperUtils::getCompanyId).thenReturn(1L);
        mockedASGHelperUtils.when(ASGHelperUtils::getUserPoid).thenReturn(100L);
        
        mockedUserContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");
    }

    @AfterEach
    void tearDown() {
        mockedASGHelperUtils.close();
        mockedUserContext.close();
    }

    @Test
    void listRecurringJvs_ShouldReturnPaginatedData() {
        FilterRequestDto filters = new FilterRequestDto("AND", "N", Collections.emptyList());
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult rawResult = new RawSearchResult(Collections.emptyList(), Collections.emptyMap(), 0L);

        when(documentService.resolveOperator(filters)).thenReturn("AND");
        when(documentService.resolveIsDeleted(filters)).thenReturn("N");
        when(documentService.resolveDateFilters(any(), any(), any(), any())).thenReturn(new ArrayList<>());
        when(documentService.search(anyString(), anyList(), anyString(), any(Pageable.class), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = glRecurringJvService.listRecurringJvs("400-102", filters, null, null, pageable);

        assertNotNull(result);
        verify(documentService).search(eq("400-102"), anyList(), eq("AND"), eq(pageable), eq("N"), eq("NARRATION"), eq("TRANSACTION_POID"));
    }

    @Test
    void getRecurringJvById_ShouldReturnResponse() {
        Long poid = 1L;
        GlRecurringJvHdr hdr = new GlRecurringJvHdr();
        hdr.setTransactionPoid(poid);
        hdr.setDocRef("REF-001");

        when(hdrRepository.findByTransactionPoid(poid)).thenReturn(Optional.of(hdr));
        when(dtlRepository.findByTransactionPoid(poid)).thenReturn(Collections.emptyList());
        when(monthDtlRepository.findByTransactionPoid(poid)).thenReturn(Collections.emptyList());

        RecurringJvResponse response = glRecurringJvService.getRecurringJvById(poid);

        assertNotNull(response);
        assertEquals(poid, response.getTransactionPoid());
    }

    @Test
    void getRecurringJvById_ShouldThrowException_WhenNotFound() {
        when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> glRecurringJvService.getRecurringJvById(1L));
    }

    @Test
    void createRecurringJv_ShouldCreateSuccessfully() {
        RecurringJvRequest request = new RecurringJvRequest();
        request.setNarration("Test Narration");
        request.setTotalAmount(new BigDecimal("10.000"));
        request.setNoOfMonths(1);
        request.setStartDate(LocalDate.now());
        request.setRefType("GENERAL");
        
        RecurringJvDetailRequest dtl1 = new RecurringJvDetailRequest();
        dtl1.setType("Dr");
        dtl1.setDrAmt(new BigDecimal("10.000"));
        dtl1.setGlPoid(123L);
        dtl1.setCompanyPoid(1L);
        
        RecurringJvDetailRequest dtl2 = new RecurringJvDetailRequest();
        dtl2.setType("Cr");
        dtl2.setCrAmt(new BigDecimal("10.000"));
        dtl2.setGlPoid(456L);
        dtl2.setCompanyPoid(1L);
        
        request.setDetails(Arrays.asList(dtl1, dtl2));

        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(true);
        when(hdrRepository.save(any(GlRecurringJvHdr.class))).thenAnswer(invocation -> {
            GlRecurringJvHdr h = invocation.getArgument(0);
            h.setTransactionPoid(1L);
            return h;
        });

        RecurringJvCreateResponse response = glRecurringJvService.createRecurringJv(request);

        assertNotNull(response);
        assertEquals(Long.valueOf(1L), response.getTransactionPoid());
    }

    @Test
    void createRecurringJv_ShouldThrowException_WhenDrNotEqualCr() {
        RecurringJvRequest request = new RecurringJvRequest();
        request.setTotalAmount(new BigDecimal("1000"));
        request.setNoOfMonths(10);
        
        RecurringJvDetailRequest dtl1 = new RecurringJvDetailRequest();
        dtl1.setType("Dr");
        dtl1.setDrAmt(new BigDecimal("100"));
        
        RecurringJvDetailRequest dtl2 = new RecurringJvDetailRequest();
        dtl2.setType("Cr");
        dtl2.setCrAmt(new BigDecimal("90"));
        
        request.setDetails(Arrays.asList(dtl1, dtl2));

        assertThrows(IllegalArgumentException.class, () -> glRecurringJvService.createRecurringJv(request));
    }

    @Test
    void deleteRecurringJv_ShouldDeleteSuccessfully() {
        Long poid = 1L;
        GlRecurringJvHdr hdr = new GlRecurringJvHdr();
        hdr.setTransactionPoid(poid);
        hdr.setTransactionDate(LocalDate.now());

        when(hdrRepository.findById(poid)).thenReturn(Optional.of(hdr));
        when(monthDtlRepository.countCreatedSchedulesByTransactionPoid(poid)).thenReturn(0L);

        glRecurringJvService.deleteRecurringJv(poid, new DeleteReasonDto());

        verify(documentDeleteService).deleteDocument(any(), any(), any(), any(), any());
    }

    @Test
    void deleteRecurringJv_ShouldThrowException_WhenSchedulesExist() {
        Long poid = 1L;
        GlRecurringJvHdr hdr = new GlRecurringJvHdr();
        when(hdrRepository.findById(poid)).thenReturn(Optional.of(hdr));
        when(monthDtlRepository.countCreatedSchedulesByTransactionPoid(poid)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> glRecurringJvService.deleteRecurringJv(poid, new DeleteReasonDto()));
    }

    @Test
    void createSchedule_ShouldCallStoredProcedure() {
        Long poid = 1L;
        GlRecurringJvHdr hdr = new GlRecurringJvHdr();
        hdr.setTransactionPoid(poid);
        hdr.setDeleted("N");
        hdr.setGroupPoid(1L);
        hdr.setCompanyPoid(1L);

        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setTotalAmount(new BigDecimal("100"));
        request.setNoOfMonths(1);
        
        RecurringJvDetailRequest dtlDr = new RecurringJvDetailRequest();
        dtlDr.setType("Dr");
        dtlDr.setDrAmt(new BigDecimal("100"));
        
        RecurringJvDetailRequest dtlCr = new RecurringJvDetailRequest();
        dtlCr.setType("Cr");
        dtlCr.setCrAmt(new BigDecimal("100"));
        
        request.setDetails(Arrays.asList(dtlDr, dtlCr));

        when(hdrRepository.findById(poid)).thenReturn(Optional.of(hdr));
        when(dtlRepository.findByTransactionPoid(poid)).thenReturn(Arrays.asList(
            GlRecurringJvDtl.builder().type("Dr").drAmt(new BigDecimal("100")).build(),
            GlRecurringJvDtl.builder().type("Cr").crAmt(new BigDecimal("100")).build()
        ));
        when(monthDtlRepository.findByTransactionPoid(poid)).thenReturn(Collections.emptyList());

        glRecurringJvService.createSchedule(poid, request);

        verify(procRepository).createSchedule(anyLong(), anyLong(), anyLong(), eq(poid));
    }

    @Test
    void updateRecurringJv_ShouldUpdateSuccessfully() {
        Long poid = 1L;
        GlRecurringJvHdr hdr = new GlRecurringJvHdr();
        hdr.setTransactionPoid(poid);
        hdr.setDocRef("REF-001");
        hdr.setCompanyPoid(1L);

        RecurringJvRequest request = new RecurringJvRequest();
        request.setNarration("Updated Narration");
        request.setTotalAmount(new BigDecimal("10.000"));
        request.setNoOfMonths(1);
        request.setStartDate(LocalDate.now());
        request.setRefType("GENERAL");

        RecurringJvDetailRequest dtl1 = new RecurringJvDetailRequest();
        dtl1.setDetRowId(1L);
        dtl1.setActionType("UPDATED");
        dtl1.setType("Dr");
        dtl1.setDrAmt(new BigDecimal("10.000"));
        dtl1.setGlPoid(123L);

        RecurringJvDetailRequest dtl2 = new RecurringJvDetailRequest();
        dtl2.setDetRowId(2L);
        dtl2.setActionType("NOCHANGES");
        dtl2.setType("Cr");
        dtl2.setCrAmt(new BigDecimal("10.000"));
        dtl2.setGlPoid(456L);

        request.setDetails(Arrays.asList(dtl1, dtl2));

        when(hdrRepository.findById(poid)).thenReturn(Optional.of(hdr));
        when(monthDtlRepository.countCreatedSchedulesByTransactionPoid(poid)).thenReturn(0L);
        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(true);
        when(dtlRepository.findById(any())).thenReturn(Optional.of(new GlRecurringJvDtl()));

        RecurringJvCreateResponse response = glRecurringJvService.updateRecurringJv(poid, request, "400-102");

        assertNotNull(response);
        verify(hdrRepository).save(any(GlRecurringJvHdr.class));
    }

    @Test
    void processDetailAction_ShouldHandleNoChangeWithBreakups() {
        Long poid = 1L;
        RecurringJvRequest request = new RecurringJvRequest();
        request.setTotalAmount(new BigDecimal("100"));
        request.setNoOfMonths(1);
        
        RecurringJvDetailRequest dtl1 = new RecurringJvDetailRequest();
        dtl1.setDetRowId(1L);
        dtl1.setActionType("NOCHANGES");
        dtl1.setType("Dr");
        dtl1.setDrAmt(new BigDecimal("100"));
        dtl1.setGlPoid(123L);
        dtl1.setCompanyPoid(1L);

        CostCenterBreakupPopupRequestDto cc = new CostCenterBreakupPopupRequestDto();
        cc.setAmount(new BigDecimal("100"));
        cc.setActionType("isCreated");
        dtl1.setCostCenter(Collections.singletonList(cc));

        RecurringJvDetailRequest dtl2 = new RecurringJvDetailRequest();
        dtl2.setDetRowId(2L);
        dtl2.setActionType("NOCHANGES");
        dtl2.setType("Cr");
        dtl2.setCrAmt(new BigDecimal("100"));
        dtl2.setGlPoid(456L);
        dtl2.setCompanyPoid(1L);

        request.setDetails(Arrays.asList(dtl1, dtl2));

        GlRecurringJvHdr hdr = new GlRecurringJvHdr();
        hdr.setTransactionPoid(poid);
        hdr.setCompanyPoid(1L);
        
        when(hdrRepository.save(any(GlRecurringJvHdr.class))).thenReturn(hdr);
        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(true);

        glRecurringJvService.createRecurringJv(request);

        verify(costCenterBreakupService).saveCostCenterBreakups(anyList());
    }

    @Test
    void processDetailAction_ShouldHandleDeletedDetail() {
        Long poid = 1L;
        GlRecurringJvHdr hdr = new GlRecurringJvHdr();
        hdr.setTransactionPoid(poid);
        hdr.setCompanyPoid(1L);

        RecurringJvRequest request = new RecurringJvRequest();
        request.setTotalAmount(new BigDecimal("100"));
        request.setNoOfMonths(1);

        RecurringJvDetailRequest dtl1 = new RecurringJvDetailRequest();
        dtl1.setDetRowId(1L);
        dtl1.setActionType("DELETED");
        dtl1.setType("Dr");
        dtl1.setDrAmt(new BigDecimal("100"));
        dtl1.setGlPoid(123L);

        RecurringJvDetailRequest dtl2 = new RecurringJvDetailRequest();
        dtl2.setDetRowId(2L);
        dtl2.setActionType("NOCHANGES");
        dtl2.setType("Cr");
        dtl2.setCrAmt(new BigDecimal("100"));
        dtl2.setGlPoid(456L);

        request.setDetails(Arrays.asList(dtl1, dtl2));

        when(hdrRepository.findById(poid)).thenReturn(Optional.of(hdr));
        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(true);

        glRecurringJvService.updateRecurringJv(poid, request, "400-102");

        verify(dtlRepository).deleteById(any());
        verify(loggingService).logDelete(any(), any(), any());
    }

    @Test
    void validateDetailLine_ShouldThrowException_WhenTypeIsInvalid() {
        RecurringJvRequest request = new RecurringJvRequest();
        request.setTotalAmount(new BigDecimal("100"));
        request.setNoOfMonths(1);

        RecurringJvDetailRequest dtl = new RecurringJvDetailRequest();
        dtl.setType("Invalid");
        request.setDetails(Collections.singletonList(dtl));

        assertThrows(IllegalArgumentException.class, () -> glRecurringJvService.createRecurringJv(request));
    }

    @Test
    void validateDetailLine_ShouldThrowException_WhenDrAmtValueIsWrongForCrType() {
        RecurringJvRequest request = new RecurringJvRequest();
        request.setTotalAmount(new BigDecimal("100"));
        request.setNoOfMonths(1);

        RecurringJvDetailRequest dtl = new RecurringJvDetailRequest();
        dtl.setType("Cr");
        dtl.setCrAmt(new BigDecimal("100"));
        dtl.setDrAmt(new BigDecimal("10"));
        request.setDetails(Collections.singletonList(dtl));

        assertThrows(IllegalArgumentException.class, () -> glRecurringJvService.createRecurringJv(request));
    }

    @Test
    void print_ShouldReturnBytes() throws Exception {
        Long poid = 1L;
        byte[] content = "PDF".getBytes();
        when(printService.buildBaseParams(eq(poid), anyString())).thenReturn(new HashMap<>());
        when(printService.fillReportToPdf(any(), any(), any())).thenReturn(content);

        byte[] result = glRecurringJvService.print(poid);

        assertArrayEquals(content, result);
    }

    @Test
    void createSchedule_ShouldThrowException_WhenDeleted() {
        Long poid = 1L;
        GlRecurringJvHdr hdr = new GlRecurringJvHdr();
        hdr.setDeleted("Y");
        when(hdrRepository.findById(poid)).thenReturn(Optional.of(hdr));

        assertThrows(IllegalStateException.class, () -> glRecurringJvService.createSchedule(poid, new CreateScheduleRequest()));
    }

    @Test
    void createSchedule_ShouldThrowException_WhenNoDetails() {
        Long poid = 1L;
        GlRecurringJvHdr hdr = new GlRecurringJvHdr();
        hdr.setDeleted("N");
        when(hdrRepository.findById(poid)).thenReturn(Optional.of(hdr));
        when(dtlRepository.findByTransactionPoid(poid)).thenReturn(Collections.emptyList());

        assertThrows(IllegalStateException.class, () -> glRecurringJvService.createSchedule(poid, new CreateScheduleRequest()));
    }

    @Test
    void createSchedule_ShouldThrowException_WhenMissingDrOrCr() {
        Long poid = 1L;
        GlRecurringJvHdr hdr = new GlRecurringJvHdr();
        hdr.setDeleted("N");
        when(hdrRepository.findById(poid)).thenReturn(Optional.of(hdr));
        when(dtlRepository.findByTransactionPoid(poid)).thenReturn(Collections.singletonList(
            GlRecurringJvDtl.builder().type("Dr").drAmt(BigDecimal.TEN).build()
        ));

        assertThrows(IllegalStateException.class, () -> glRecurringJvService.createSchedule(poid, new CreateScheduleRequest()));
    }

    @Test
    void createSchedule_ShouldThrowException_WhenScheduleAlreadyExists() {
        Long poid = 1L;
        GlRecurringJvHdr hdr = new GlRecurringJvHdr();
        hdr.setDeleted("N");
        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setTotalAmount(BigDecimal.TEN);
        request.setNoOfMonths(1);
        
        RecurringJvDetailRequest dtlDr = new RecurringJvDetailRequest();
        dtlDr.setType("Dr");
        dtlDr.setDrAmt(BigDecimal.TEN);
        
        RecurringJvDetailRequest dtlCr = new RecurringJvDetailRequest();
        dtlCr.setType("Cr");
        dtlCr.setCrAmt(BigDecimal.TEN);
        
        request.setDetails(Arrays.asList(dtlDr, dtlCr));

        when(hdrRepository.findById(poid)).thenReturn(Optional.of(hdr));
        when(dtlRepository.findByTransactionPoid(poid)).thenReturn(Arrays.asList(
            GlRecurringJvDtl.builder().type("Dr").drAmt(BigDecimal.TEN).build(),
            GlRecurringJvDtl.builder().type("Cr").crAmt(BigDecimal.TEN).build()
        ));
        when(monthDtlRepository.findByTransactionPoid(poid)).thenReturn(Collections.singletonList(new GlRecurringJvMonthDtl()));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> glRecurringJvService.createSchedule(poid, request));
        assertEquals("Warning: Scheduled JV already created. Please verify.", ex.getMessage());
    }

    @Test
    void buildResponse_ShouldIncludeBreakups() {
        Long poid = 1L;
        GlRecurringJvHdr hdr = new GlRecurringJvHdr();
        hdr.setTransactionPoid(poid);
        
        GlRecurringJvDtl dtl = new GlRecurringJvDtl();
        dtl.setDetRowId(1L);
        dtl.setGlPoid(123L);

        GlVoucherCostCenterBreakupResponseDto ccResp = new GlVoucherCostCenterBreakupResponseDto();
        CostCenterBreakupResponseDto ccDtl = new CostCenterBreakupResponseDto();
        ccDtl.setMainDetRowId(1L);
        ccDtl.setCostGroup("GROUP");
        ccDtl.setCostPoid("ABC"); // Test non-numeric POID for catch block
        ccResp.setCostBreakupList(Collections.singletonList(ccDtl));

        when(hdrRepository.findByTransactionPoid(poid)).thenReturn(Optional.of(hdr));
        when(dtlRepository.findByTransactionPoid(poid)).thenReturn(Collections.singletonList(dtl));
        when(costCenterBreakupService.loadCostCenterData(any(), any(), any(), any(), any())).thenReturn(ccResp);
        when(lovService.getDetailsByCodeAndLovName(any(), any())).thenReturn(null);

        RecurringJvResponse response = glRecurringJvService.getRecurringJvById(poid);

        assertNotNull(response);
        assertEquals(1, response.getDetails().get(0).getCostCenter().size());
        verify(lovService).getDetailsByCodeAndLovName(eq("ABC"), eq("GROUP"));
    }

    @Test
    void validateRequest_ShouldThrowException_WhenAmtMismatch() {
        RecurringJvRequest request = new RecurringJvRequest();
        request.setTotalAmount(new BigDecimal("100"));
        request.setNoOfMonths(2); // monthly should be 50
        
        RecurringJvDetailRequest dtl1 = new RecurringJvDetailRequest();
        dtl1.setType("Dr");
        dtl1.setDrAmt(new BigDecimal("40")); // mismatch (40 != 50)
        
        RecurringJvDetailRequest dtl2 = new RecurringJvDetailRequest();
        dtl2.setType("Cr");
        dtl2.setCrAmt(new BigDecimal("40"));
        
        request.setDetails(Arrays.asList(dtl1, dtl2));

        assertThrows(IllegalArgumentException.class, () -> glRecurringJvService.createRecurringJv(request));
    }

    @Test
    void updateRecurringJv_ShouldHandleNestedUpdates() {
        Long poid = 1L;
        GlRecurringJvHdr hdr = new GlRecurringJvHdr();
        hdr.setTransactionPoid(poid);
        hdr.setCompanyPoid(1L);

        RecurringJvRequest request = new RecurringJvRequest();
        request.setTotalAmount(BigDecimal.TEN);
        request.setNoOfMonths(1);
        request.setStartDate(LocalDate.now());
        request.setRefType("REF");

        RecurringJvDetailRequest dtl1 = new RecurringJvDetailRequest();
        dtl1.setDetRowId(1L);
        dtl1.setActionType("UPDATED");
        dtl1.setType("Dr");
        dtl1.setDrAmt(BigDecimal.TEN);
        dtl1.setGlPoid(123L);
        
        CostCenterBreakupPopupRequestDto cc = new CostCenterBreakupPopupRequestDto();
        cc.setActionType("isUpdated");
        cc.setAmount(BigDecimal.TEN);
        dtl1.setCostCenter(Collections.singletonList(cc));

        RecurringJvDetailRequest dtl2 = new RecurringJvDetailRequest();
        dtl2.setDetRowId(2L);
        dtl2.setActionType("NOCHANGES");
        dtl2.setType("Cr");
        dtl2.setCrAmt(BigDecimal.TEN);
        dtl2.setGlPoid(456L);
        
        request.setDetails(Arrays.asList(dtl1, dtl2));

        when(hdrRepository.findById(poid)).thenReturn(Optional.of(hdr));
        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(true);
        when(dtlRepository.findById(any())).thenReturn(Optional.of(new GlRecurringJvDtl()));

        glRecurringJvService.updateRecurringJv(poid, request, "400-102");

        verify(costCenterBreakupService).updateCostCenterBreakups(anyList(), anyLong());
    }

    @Test
    void updateScheduleDetails_ShouldHandleUpdates() {
        Long poid = 1L;
        GlRecurringJvHdr hdr = new GlRecurringJvHdr();
        hdr.setTransactionPoid(poid);
        hdr.setCompanyPoid(1L);
        when(hdrRepository.findById(poid)).thenReturn(Optional.of(hdr));

        RecurringJvRequest request = new RecurringJvRequest();
        request.setNarration("nar");
        request.setTotalAmount(BigDecimal.TEN);
        request.setNoOfMonths(1);
        request.setStartDate(LocalDate.now());
        request.setRefType("REF");
        
        RecurringJvDetailRequest dtl1 = new RecurringJvDetailRequest();
        dtl1.setType("Dr");
        dtl1.setDrAmt(BigDecimal.TEN);
        dtl1.setGlPoid(123L);
        
        RecurringJvDetailRequest dtl2 = new RecurringJvDetailRequest();
        dtl2.setType("Cr");
        dtl2.setCrAmt(BigDecimal.TEN);
        dtl2.setGlPoid(456L);
        
        request.setDetails(Arrays.asList(dtl1, dtl2));

        RecurringJVScheduleWiseDetailsDto schedule = new RecurringJVScheduleWiseDetailsDto();
        schedule.setDetRowId(1L);
        schedule.setActionType("ISUPDATED");
        schedule.setMonthWiseDate(LocalDate.now());
        request.setScheduleDetails(Collections.singletonList(schedule));

        mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC");
        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(true);
        when(monthDtlRepository.findById(any())).thenReturn(Optional.of(new GlRecurringJvMonthDtl()));

        glRecurringJvService.updateRecurringJv(poid, request, "400-102");

        verify(monthDtlRepository).saveAll(anyList());
        verify(loggingService).createLogBatch(anyList());
    }

    @Test
    void createRecurringJv_ShouldPreserveSpacesBetweenWords() {
        RecurringJvRequest request = new RecurringJvRequest();
        request.setNarration("  Monthly salary entry with spaces  ");
        request.setRemarks("  Valid multi word remarks  ");
        request.setRefType("  EMPLOYEE  ");
        request.setStartDate(LocalDate.now());
        request.setTotalAmount(BigDecimal.TEN);
        request.setNoOfMonths(1);
        request.setMonthWiseAmount(BigDecimal.TEN);

        RecurringJvDetailRequest dtl1 = new RecurringJvDetailRequest();
        dtl1.setType("Dr");
        dtl1.setDrAmt(BigDecimal.TEN);
        dtl1.setGlPoid(123L);
        dtl1.setRemarks("  Dr detail remarks  ");

        RecurringJvDetailRequest dtl2 = new RecurringJvDetailRequest();
        dtl2.setType("Cr");
        dtl2.setCrAmt(BigDecimal.TEN);
        dtl2.setGlPoid(456L);

        request.setDetails(Arrays.asList(dtl1, dtl2));

        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(true);
        when(hdrRepository.save(any(GlRecurringJvHdr.class))).thenAnswer(invocation -> {
            GlRecurringJvHdr hdr = invocation.getArgument(0);
            hdr.setTransactionPoid(1L);
            hdr.setDocRef("RJV-001");
            return hdr;
        });

        glRecurringJvService.createRecurringJv(request);

        verify(hdrRepository).save(argThat(hdr -> 
            "Monthly salary entry with spaces".equals(hdr.getNarration()) &&
            "Valid multi word remarks".equals(hdr.getRemarks()) &&
            "EMPLOYEE".equals(hdr.getRefType())
        ));
    }
}
