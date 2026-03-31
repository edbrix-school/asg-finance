package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.model.CustomAuthDetails;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.*;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GLPaymentVoucherDtlGLEntity;
import com.asg.finance.entity.GLPaymentVoucherHDREntity;
import com.asg.finance.entity.GlBankPaymentChargeDtlEntity;
import com.asg.finance.entity.GlBankPaymentItemDtlEntity;
import com.asg.finance.repository.*;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class BankPaymentVoucherServiceImplTest {

    // ======================== MOCKS ========================

    @Mock private BankPaymentVoucherRepository paymentVoucherRepository;
    @Mock private BankPaymentVoucherDetailsRepository paymentVoucherDetailsRepository;
    @Mock private GlBankPaymentChargeDtlRepository chargeDtlRepository;
    @Mock private GlBankPaymentItemDtlRepository itemRepository;
    @Mock private BankPaymentVoucherSpRepository spRepository;
    @Mock private BankPaymentLoadDataRepository loadDataRepository;
    @Mock private DocumentSearchService documentService;
    @Mock private LovDataService lovService;
    @Mock private BillwiseBreakupDtlRepository billwiseBreakupRepository;
    @Mock private CostCenterBreakupDtlRepository costCenterBreakupDtlRepository;
    @Mock private BillwiseBreakupService billwiseBreakupService;
    @Mock private CostCenterBreakupService costCenterBreakupService;
    @Mock private PrintService printService;
    @Mock private DataSource dataSource;
    @Mock private LoggingService loggingService;
    @Mock private DocumentDeleteService documentDeleteService;
    @Mock private EntityManager entityManager;
    @Mock private GlobalParameterService globalParameterService;

    @InjectMocks
    private BankPaymentVoucherServiceImpl service;

    // ======================== CONSTANTS ========================

    private static final Long TRANS_POID = 100L;
    private static final Long GROUP_POID = 1L;
    private static final Long COMPANY_POID = 2L;
    private static final Long USER_POID = 3L;
    private static final Long BANK_POID = 10L;
    private static final String DOC_ID = "400-107";
    private static final String USER_NAME = "testuser";

    // ======================== TEST DATA ========================

    private GLPaymentVoucherHDREntity headerEntity;
    private BankPaymentVoucherRequest generalRequest;
    private BankPaymentGLDetailRequest glDetailRequest;
    private BankPaymentChargeDetailRequest chargeDetailRequest;
    private BankPaymentItemDetailRequest itemDetailRequest;

    @BeforeEach
    void setUp() {
        // Set up UserContext via ThreadLocal (no MockedStatic needed)
        CustomAuthDetails auth = new CustomAuthDetails();
        auth.setGroupPoid(GROUP_POID);
        auth.setCompanyPoid(COMPANY_POID);
        auth.setUserPoid(USER_POID);
        auth.setDocumentId(DOC_ID);
        auth.setUserName(USER_NAME);
        auth.setTimeZoneCode("UTC");
        UserContext.setCurrentUser(auth);

        // Base header entity
        headerEntity = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(TRANS_POID)
                .transactionDate(LocalDate.now())
                .groupPoid(GROUP_POID)
                .companyPoid(COMPANY_POID)
                .docRef("BPV-100")
                .bankPoid(BANK_POID)
                .payingTo("Test Supplier")
                .refType("GENERAL")
                .deleted("N")
                .released("N")
                .prePrinted("N")
                .chqPrinted("N")
                .build();

        // GL detail for GENERAL ref type
        glDetailRequest = new BankPaymentGLDetailRequest();
        glDetailRequest.setDetRowId(1L);
        glDetailRequest.setType("DR");
        glDetailRequest.setGlPoid(20L);
        glDetailRequest.setCompanyPoid(COMPANY_POID);
        glDetailRequest.setDrAmt(500.0);
        glDetailRequest.setCrAmt(0.0);
        glDetailRequest.setActionType("isCreated");

        // Charge detail for FDA JOBS / FF JOBS
        chargeDetailRequest = new BankPaymentChargeDetailRequest();
        chargeDetailRequest.setDetRowId(1L);
        chargeDetailRequest.setChargePoid(30L);
        chargeDetailRequest.setChargeAmount(250L);
        chargeDetailRequest.setActionType("isCreated");

        // Item detail for MTA RFQ
        itemDetailRequest = new BankPaymentItemDetailRequest();
        itemDetailRequest.setDetRowId(1L);
        itemDetailRequest.setStockPoid(40L);
        itemDetailRequest.setPoQty(10L);
        itemDetailRequest.setPrice(25L);
        itemDetailRequest.setActionType("isCreated");

        // Default GENERAL request
        generalRequest = buildGeneralRequest();
    }

    @AfterEach
    void tearDown() {
        UserContext.setCurrentUser(null);
    }

    // ======================== HELPERS ========================

    private BankPaymentVoucherRequest buildGeneralRequest() {
        BankPaymentVoucherRequest req = new BankPaymentVoucherRequest();
        req.setRefType("GENERAL");
        req.setPayGlPoid(20L);
        req.setBankPoid(BANK_POID);
        req.setPayingTo("Test Supplier");
        req.setRemarks("Test remark");
        req.setGlDetails(List.of(glDetailRequest));
        return req;
    }

    private BankPaymentVoucherRequest buildFdaRequest() {
        BankPaymentVoucherRequest req = new BankPaymentVoucherRequest();
        req.setRefType("FDA JOBS");
        req.setFdaRefId(50L);
        req.setBankPoid(BANK_POID);
        req.setPayingTo("FDA Supplier");
        req.setChargeDetailRequests(List.of(chargeDetailRequest));
        return req;
    }

    private BankPaymentVoucherRequest buildFfRequest() {
        BankPaymentVoucherRequest req = new BankPaymentVoucherRequest();
        req.setRefType("FF JOBS");
        req.setFfRefId(60L);
        req.setBankPoid(BANK_POID);
        req.setPayingTo("FF Supplier");
        req.setChargeDetailRequests(List.of(chargeDetailRequest));
        return req;
    }

    private BankPaymentVoucherRequest buildMtaRequest() {
        BankPaymentVoucherRequest req = new BankPaymentVoucherRequest();
        req.setRefType("MTA RFQ");
        req.setMtaRfqId("MTA-001");
        req.setBankPoid(BANK_POID);
        req.setPayingTo("MTA Supplier");
        req.setItemDetailRequests(List.of(itemDetailRequest));
        return req;
    }

    /**
     * Stubs the minimum calls needed when getVoucherById is invoked internally
     * (e.g. after create/update) for a GENERAL voucher.
     */
    private void stubGetVoucherByIdGeneral(GLPaymentVoucherHDREntity header) {
        GLPaymentVoucherDtlGLEntity glDtl = new GLPaymentVoucherDtlGLEntity();
        glDtl.setTransactionPoid(header.getTransactionPoid());
        glDtl.setDetRowId(1L);

        when(paymentVoucherRepository.findById(header.getTransactionPoid()))
                .thenReturn(Optional.of(header));
        when(paymentVoucherDetailsRepository.findByTransactionPoid(header.getTransactionPoid()))
                .thenReturn(List.of(glDtl));
        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(new GlVoucherLoadBillwiseBreakupResponseDto());
        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new GlVoucherCostCenterBreakupResponseDto());
        lenient().when(lovService.getDetailsByPoidAndLovName(any(), anyString())).thenReturn(null);
    }

    // ========================================================
    // getVoucherById tests
    // ========================================================

    @Test
    @DisplayName("getVoucherById – GENERAL ref type returns response with GL details")
    void getVoucherById_GeneralRefType_ReturnsGlDetails() {
        GLPaymentVoucherDtlGLEntity glDtl = new GLPaymentVoucherDtlGLEntity();
        glDtl.setTransactionPoid(TRANS_POID);
        glDtl.setDetRowId(1L);

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(paymentVoucherDetailsRepository.findByTransactionPoid(TRANS_POID)).thenReturn(List.of(glDtl));
        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(new GlVoucherLoadBillwiseBreakupResponseDto());
        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new GlVoucherCostCenterBreakupResponseDto());

        BankPaymentVoucherResponse result = service.getVoucherById(TRANS_POID, DOC_ID);

        assertNotNull(result);
        assertEquals(TRANS_POID, result.getTransactionPoid());
        assertNotNull(result.getGlDetails());
        assertFalse(result.getGlDetails().isEmpty());
        verify(paymentVoucherDetailsRepository).findByTransactionPoid(TRANS_POID);
    }

    @Test
    @DisplayName("getVoucherById – CUSTOM ref type loads GL details same as GENERAL")
    void getVoucherById_CustomRefType_ReturnsGlDetails() {
        headerEntity.setRefType("CUSTOM");
        GLPaymentVoucherDtlGLEntity glDtl = new GLPaymentVoucherDtlGLEntity();
        glDtl.setTransactionPoid(TRANS_POID);
        glDtl.setDetRowId(1L);

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(paymentVoucherDetailsRepository.findByTransactionPoid(TRANS_POID)).thenReturn(List.of(glDtl));
        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(new GlVoucherLoadBillwiseBreakupResponseDto());
        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new GlVoucherCostCenterBreakupResponseDto());

        BankPaymentVoucherResponse result = service.getVoucherById(TRANS_POID, DOC_ID);

        assertNotNull(result);
        verify(paymentVoucherDetailsRepository).findByTransactionPoid(TRANS_POID);
    }

    @Test
    @DisplayName("getVoucherById – FDA JOBS ref type loads charge details")
    void getVoucherById_FdaJobsRefType_ReturnsChargeDetails() {
        headerEntity.setRefType("FDA JOBS");
        GlBankPaymentChargeDtlEntity chargeDtl = new GlBankPaymentChargeDtlEntity();
        chargeDtl.setTransactionPoid(TRANS_POID);
        chargeDtl.setDetRowId(1L);

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(chargeDtlRepository.findByTransactionPoid(TRANS_POID)).thenReturn(List.of(chargeDtl));
        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(new GlVoucherLoadBillwiseBreakupResponseDto());
        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new GlVoucherCostCenterBreakupResponseDto());

        BankPaymentVoucherResponse result = service.getVoucherById(TRANS_POID, DOC_ID);

        assertNotNull(result);
        assertNotNull(result.getChargeDetails());
        assertFalse(result.getChargeDetails().isEmpty());
        verify(chargeDtlRepository).findByTransactionPoid(TRANS_POID);
    }

    @Test
    @DisplayName("getVoucherById – FF JOBS ref type loads charge details")
    void getVoucherById_FfJobsRefType_ReturnsChargeDetails() {
        headerEntity.setRefType("FF JOBS");
        GlBankPaymentChargeDtlEntity chargeDtl = new GlBankPaymentChargeDtlEntity();
        chargeDtl.setTransactionPoid(TRANS_POID);
        chargeDtl.setDetRowId(1L);

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(chargeDtlRepository.findByTransactionPoid(TRANS_POID)).thenReturn(List.of(chargeDtl));
        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(new GlVoucherLoadBillwiseBreakupResponseDto());
        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new GlVoucherCostCenterBreakupResponseDto());

        BankPaymentVoucherResponse result = service.getVoucherById(TRANS_POID, DOC_ID);

        assertNotNull(result);
        verify(chargeDtlRepository).findByTransactionPoid(TRANS_POID);
    }

    @Test
    @DisplayName("getVoucherById – MTA RFQ ref type loads item details")
    void getVoucherById_MtaRfqRefType_ReturnsItemDetails() {
        headerEntity.setRefType("MTA RFQ");
        GlBankPaymentItemDtlEntity itemDtl = new GlBankPaymentItemDtlEntity();
        itemDtl.setTransactionPoid(TRANS_POID);
        itemDtl.setDetRowId(1L);

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(itemRepository.findByTransactionPoid(TRANS_POID)).thenReturn(List.of(itemDtl));
        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(new GlVoucherLoadBillwiseBreakupResponseDto());
        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new GlVoucherCostCenterBreakupResponseDto());

        BankPaymentVoucherResponse result = service.getVoucherById(TRANS_POID, DOC_ID);

        assertNotNull(result);
        assertNotNull(result.getItemDetails());
        assertFalse(result.getItemDetails().isEmpty());
        verify(itemRepository).findByTransactionPoid(TRANS_POID);
    }

    @Test
    @DisplayName("getVoucherById – unknown ref type still returns response with empty lists")
    void getVoucherById_UnknownRefType_ReturnsEmptyDetails() {
        headerEntity.setRefType("UNKNOWN");

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(new GlVoucherLoadBillwiseBreakupResponseDto());
        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new GlVoucherCostCenterBreakupResponseDto());

        BankPaymentVoucherResponse result = service.getVoucherById(TRANS_POID, DOC_ID);

        assertNotNull(result);
        assertTrue(result.getGlDetails().isEmpty());
        assertTrue(result.getChargeDetails().isEmpty());
        assertTrue(result.getItemDetails().isEmpty());
    }

    @Test
    @DisplayName("getVoucherById – voucher not found throws ValidationException")
    void getVoucherById_NotFound_ThrowsValidationException() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.empty());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.getVoucherById(TRANS_POID, DOC_ID));
        assertTrue(ex.getMessage().contains(TRANS_POID.toString()));
    }

    // ========================================================
    // createBankPaymentVoucher tests
    // ========================================================

    @Test
    @DisplayName("createBankPaymentVoucher – GENERAL ref type saves header and GL details")
    void createBankPaymentVoucher_GeneralRefType_SavesAndReturnsResponse() {
        when(globalParameterService.getParameterValue(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("999");
        when(paymentVoucherRepository.save(any(GLPaymentVoucherHDREntity.class))).thenReturn(headerEntity);
        doNothing().when(paymentVoucherRepository).flush();

        GLPaymentVoucherDtlGLEntity savedGlDtl = new GLPaymentVoucherDtlGLEntity();
        savedGlDtl.setTransactionPoid(TRANS_POID);
        savedGlDtl.setDetRowId(1L);
        when(paymentVoucherDetailsRepository.saveAll(anyList())).thenReturn(List.of(savedGlDtl));
        stubGetVoucherByIdGeneral(headerEntity);

        BankPaymentVoucherResponse result = service.createBankPaymentVoucher(generalRequest, DOC_ID);

        assertNotNull(result);
        assertEquals(TRANS_POID, result.getTransactionPoid());
        verify(paymentVoucherRepository, atLeast(2)).save(any(GLPaymentVoucherHDREntity.class));
        verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), eq(DOC_ID), eq(TRANS_POID.toString()));
    }

    @Test
    @DisplayName("createBankPaymentVoucher – CUSTOM ref type saves GL details")
    void createBankPaymentVoucher_CustomRefType_SavesGlDetails() {
        BankPaymentVoucherRequest customReq = buildGeneralRequest();
        customReq.setRefType("CUSTOM");

        when(globalParameterService.getParameterValue(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("999");
        when(paymentVoucherRepository.save(any(GLPaymentVoucherHDREntity.class))).thenReturn(headerEntity);
        doNothing().when(paymentVoucherRepository).flush();

        GLPaymentVoucherDtlGLEntity savedGlDtl = new GLPaymentVoucherDtlGLEntity();
        savedGlDtl.setDetRowId(1L);
        when(paymentVoucherDetailsRepository.saveAll(anyList())).thenReturn(List.of(savedGlDtl));

        GLPaymentVoucherHDREntity customHeader = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(TRANS_POID).groupPoid(GROUP_POID).companyPoid(COMPANY_POID)
                .refType("CUSTOM").deleted("N").released("N").prePrinted("N").chqPrinted("N").build();
        stubGetVoucherByIdGeneral(customHeader);
        // override to return the custom header
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(customHeader));

        BankPaymentVoucherResponse result = service.createBankPaymentVoucher(customReq, DOC_ID);

        assertNotNull(result);
        verify(paymentVoucherDetailsRepository, atLeast(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("createBankPaymentVoucher – FDA JOBS ref type saves charge details")
    void createBankPaymentVoucher_FdaJobsRefType_SavesChargeDetails() {
        BankPaymentVoucherRequest fdaReq = buildFdaRequest();
        GLPaymentVoucherHDREntity fdaHeader = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(TRANS_POID).groupPoid(GROUP_POID).companyPoid(COMPANY_POID)
                .refType("FDA JOBS").fdaRef(50L).deleted("N").released("N").prePrinted("N").build();

        when(globalParameterService.getParameterValue(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("999");
        when(paymentVoucherRepository.save(any(GLPaymentVoucherHDREntity.class))).thenReturn(fdaHeader);
        doNothing().when(paymentVoucherRepository).flush();
        when(chargeDtlRepository.findByTransactionPoid(TRANS_POID)).thenReturn(Collections.emptyList());
        when(chargeDtlRepository.saveAll(anyList())).thenReturn(List.of(new GlBankPaymentChargeDtlEntity()));

        // stub getVoucherById internal call for FDA
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(fdaHeader));
        GlBankPaymentChargeDtlEntity chDtl = new GlBankPaymentChargeDtlEntity();
        chDtl.setTransactionPoid(TRANS_POID);
        chDtl.setDetRowId(1L);
        when(chargeDtlRepository.findByTransactionPoid(TRANS_POID)).thenReturn(List.of(chDtl));
        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(new GlVoucherLoadBillwiseBreakupResponseDto());
        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new GlVoucherCostCenterBreakupResponseDto());

        BankPaymentVoucherResponse result = service.createBankPaymentVoucher(fdaReq, DOC_ID);

        assertNotNull(result);
        verify(chargeDtlRepository, atLeast(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("createBankPaymentVoucher – FF JOBS ref type saves charge details")
    void createBankPaymentVoucher_FfJobsRefType_SavesChargeDetails() {
        BankPaymentVoucherRequest ffReq = buildFfRequest();
        GLPaymentVoucherHDREntity ffHeader = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(TRANS_POID).groupPoid(GROUP_POID).companyPoid(COMPANY_POID)
                .refType("FF JOBS").ffRef("60").deleted("N").released("N").prePrinted("N").build();

        when(globalParameterService.getParameterValue(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("999");
        when(paymentVoucherRepository.save(any(GLPaymentVoucherHDREntity.class))).thenReturn(ffHeader);
        doNothing().when(paymentVoucherRepository).flush();
        when(chargeDtlRepository.findByTransactionPoid(TRANS_POID)).thenReturn(Collections.emptyList());
        when(chargeDtlRepository.saveAll(anyList())).thenReturn(List.of(new GlBankPaymentChargeDtlEntity()));
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(ffHeader));
        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(new GlVoucherLoadBillwiseBreakupResponseDto());
        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new GlVoucherCostCenterBreakupResponseDto());

        BankPaymentVoucherResponse result = service.createBankPaymentVoucher(ffReq, DOC_ID);

        assertNotNull(result);
        verify(chargeDtlRepository, atLeast(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("createBankPaymentVoucher – MTA RFQ ref type saves item details")
    void createBankPaymentVoucher_MtaRfqRefType_SavesItemDetails() {
        BankPaymentVoucherRequest mtaReq = buildMtaRequest();
        GLPaymentVoucherHDREntity mtaHeader = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(TRANS_POID).groupPoid(GROUP_POID).companyPoid(COMPANY_POID)
                .refType("MTA RFQ").mtaRef("MTA-001").deleted("N").released("N").prePrinted("N").build();

        when(globalParameterService.getParameterValue(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("999");
        when(paymentVoucherRepository.save(any(GLPaymentVoucherHDREntity.class))).thenReturn(mtaHeader);
        doNothing().when(paymentVoucherRepository).flush();
        when(itemRepository.findByTransactionPoid(TRANS_POID)).thenReturn(Collections.emptyList());
        when(itemRepository.saveAll(anyList())).thenReturn(List.of(new GlBankPaymentItemDtlEntity()));
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(mtaHeader));
        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(new GlVoucherLoadBillwiseBreakupResponseDto());
        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new GlVoucherCostCenterBreakupResponseDto());

        BankPaymentVoucherResponse result = service.createBankPaymentVoucher(mtaReq, DOC_ID);

        assertNotNull(result);
        verify(itemRepository, atLeast(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("createBankPaymentVoucher – invalid ref type throws ValidationException")
    void createBankPaymentVoucher_InvalidRefType_ThrowsValidationException() {
        BankPaymentVoucherRequest badReq = new BankPaymentVoucherRequest();
        badReq.setRefType("INVALID");
        badReq.setGlDetails(List.of(glDetailRequest));

        assertThrows(ValidationException.class,
                () -> service.createBankPaymentVoucher(badReq, DOC_ID));
    }

    @Test
    @DisplayName("createBankPaymentVoucher – GENERAL without payGlPoid throws ValidationException")
    void createBankPaymentVoucher_GeneralWithoutPayGl_ThrowsValidationException() {
        BankPaymentVoucherRequest req = buildGeneralRequest();
        req.setPayGlPoid(null);

        assertThrows(ValidationException.class,
                () -> service.createBankPaymentVoucher(req, DOC_ID));
    }

    @Test
    @DisplayName("createBankPaymentVoucher – GENERAL without glDetails throws ValidationException")
    void createBankPaymentVoucher_GeneralWithoutGlDetails_ThrowsValidationException() {
        BankPaymentVoucherRequest req = buildGeneralRequest();
        req.setGlDetails(Collections.emptyList());

        assertThrows(ValidationException.class,
                () -> service.createBankPaymentVoucher(req, DOC_ID));
    }

    @Test
    @DisplayName("createBankPaymentVoucher – FF JOBS without ffRefId throws ValidationException")
    void createBankPaymentVoucher_FfJobsWithoutFfRef_ThrowsValidationException() {
        BankPaymentVoucherRequest req = buildFfRequest();
        req.setFfRefId(null);

        assertThrows(ValidationException.class,
                () -> service.createBankPaymentVoucher(req, DOC_ID));
    }

    @Test
    @DisplayName("createBankPaymentVoucher – FDA JOBS without fdaRefId throws ValidationException")
    void createBankPaymentVoucher_FdaJobsWithoutFdaRef_ThrowsValidationException() {
        BankPaymentVoucherRequest req = buildFdaRequest();
        req.setFdaRefId(null);

        assertThrows(ValidationException.class,
                () -> service.createBankPaymentVoucher(req, DOC_ID));
    }

    @Test
    @DisplayName("createBankPaymentVoucher – MTA RFQ without mtaRfqId throws ValidationException")
    void createBankPaymentVoucher_MtaRfqWithoutMtaId_ThrowsValidationException() {
        BankPaymentVoucherRequest req = buildMtaRequest();
        req.setMtaRfqId(null);

        assertThrows(ValidationException.class,
                () -> service.createBankPaymentVoucher(req, DOC_ID));
    }

    @Test
    @DisplayName("createBankPaymentVoucher – post-dated cheque for MTA RFQ throws ValidationException")
    void createBankPaymentVoucher_PostDatedChequeForMtaRfq_ThrowsValidationException() {
        BankPaymentVoucherRequest req = buildMtaRequest();
        req.setChequeDate(LocalDate.now().plusDays(5).toString());

        assertThrows(ValidationException.class,
                () -> service.createBankPaymentVoucher(req, DOC_ID));
    }

    @Test
    @DisplayName("createBankPaymentVoucher – auto-generates cheque number when bankPoid set and no chequeNo")
    void createBankPaymentVoucher_AutoGeneratesChequeNumber() {
        when(globalParameterService.getParameterValue(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("999");
        when(spRepository.getNextChequeNumber(BANK_POID)).thenReturn("CHQ-001");
        when(paymentVoucherRepository.save(any(GLPaymentVoucherHDREntity.class))).thenReturn(headerEntity);
        doNothing().when(paymentVoucherRepository).flush();
        GLPaymentVoucherDtlGLEntity savedGl = new GLPaymentVoucherDtlGLEntity();
        savedGl.setDetRowId(1L);
        when(paymentVoucherDetailsRepository.saveAll(anyList())).thenReturn(List.of(savedGl));
        stubGetVoucherByIdGeneral(headerEntity);

        service.createBankPaymentVoucher(generalRequest, DOC_ID);

        verify(spRepository).getNextChequeNumber(BANK_POID);
    }

    // ========================================================
    // updateBankPaymentVoucher tests
    // ========================================================

    @Test
    @DisplayName("updateBankPaymentVoucher – happy path updates existing voucher")
    void updateBankPaymentVoucher_HappyPath_UpdatesAndReturnsResponse() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(globalParameterService.getParameterValue(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("999");
        when(paymentVoucherRepository.save(any(GLPaymentVoucherHDREntity.class))).thenReturn(headerEntity);

        // For updateGLDetails
        when(paymentVoucherDetailsRepository.findByTransactionPoid(TRANS_POID)).thenReturn(Collections.emptyList());
        when(paymentVoucherDetailsRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // For getVoucherById after update (called twice due to find + after-update)
        stubGetVoucherByIdGeneral(headerEntity);

        BankPaymentVoucherResponse result = service.updateBankPaymentVoucher(TRANS_POID, generalRequest, DOC_ID);

        assertNotNull(result);
        verify(paymentVoucherRepository, atLeast(1)).save(any(GLPaymentVoucherHDREntity.class));
        verify(loggingService).logChanges(any(), any(), eq(GLPaymentVoucherHDREntity.class),
                eq(DOC_ID), eq(TRANS_POID.toString()), eq(LogDetailsEnum.MODIFIED), anyString());
    }

    @Test
    @DisplayName("updateBankPaymentVoucher – voucher not found throws ValidationException")
    void updateBankPaymentVoucher_NotFound_ThrowsValidationException() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.updateBankPaymentVoucher(TRANS_POID, generalRequest, DOC_ID));
    }

    @Test
    @DisplayName("updateBankPaymentVoucher – invalid ref type throws ValidationException")
    void updateBankPaymentVoucher_InvalidRefType_ThrowsValidationException() {
        BankPaymentVoucherRequest badReq = buildGeneralRequest();
        badReq.setRefType("INVALID_TYPE");

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(globalParameterService.getParameterValue(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("999");

        assertThrows(ValidationException.class,
                () -> service.updateBankPaymentVoucher(TRANS_POID, badReq, DOC_ID));
    }

    @Test
    @DisplayName("updateBankPaymentVoucher – FDA JOBS updates charge details")
    void updateBankPaymentVoucher_FdaJobs_UpdatesChargeDetails() {
        GLPaymentVoucherHDREntity fdaHeader = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(TRANS_POID).groupPoid(GROUP_POID).companyPoid(COMPANY_POID)
                .refType("FDA JOBS").fdaRef(50L).deleted("N").released("N").prePrinted("N").build();

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(fdaHeader));
        when(globalParameterService.getParameterValue(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("999");
        when(paymentVoucherRepository.save(any(GLPaymentVoucherHDREntity.class))).thenReturn(fdaHeader);
        when(chargeDtlRepository.findByTransactionPoid(TRANS_POID)).thenReturn(Collections.emptyList());
        when(chargeDtlRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Internal getVoucherById after update
        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(new GlVoucherLoadBillwiseBreakupResponseDto());
        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new GlVoucherCostCenterBreakupResponseDto());

        BankPaymentVoucherResponse result = service.updateBankPaymentVoucher(TRANS_POID, buildFdaRequest(), DOC_ID);

        assertNotNull(result);
        verify(chargeDtlRepository, atLeast(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("updateBankPaymentVoucher – MTA RFQ updates item details")
    void updateBankPaymentVoucher_MtaRfq_UpdatesItemDetails() {
        GLPaymentVoucherHDREntity mtaHeader = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(TRANS_POID).groupPoid(GROUP_POID).companyPoid(COMPANY_POID)
                .refType("MTA RFQ").mtaRef("MTA-001").deleted("N").released("N").prePrinted("N").build();

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(mtaHeader));
        when(globalParameterService.getParameterValue(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("999");
        when(paymentVoucherRepository.save(any(GLPaymentVoucherHDREntity.class))).thenReturn(mtaHeader);
        when(itemRepository.findByTransactionPoid(TRANS_POID)).thenReturn(Collections.emptyList());
        when(itemRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(new GlVoucherLoadBillwiseBreakupResponseDto());
        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new GlVoucherCostCenterBreakupResponseDto());

        BankPaymentVoucherResponse result = service.updateBankPaymentVoucher(TRANS_POID, buildMtaRequest(), DOC_ID);

        assertNotNull(result);
        verify(itemRepository, atLeast(1)).saveAll(anyList());
    }

    // ========================================================
    // softDeleteVoucher tests
    // ========================================================

    @Test
    @DisplayName("softDeleteVoucher – happy path delegates to documentDeleteService")
    void softDeleteVoucher_HappyPath_CallsDocumentDeleteService() {
        DeleteReasonDto deleteReason = new DeleteReasonDto();
        deleteReason.setDeleteReason("Test delete");

        headerEntity.setRefType("GENERAL");
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(documentDeleteService.deleteDocument(anyLong(), anyString(), anyString(),
                any(DeleteReasonDto.class), any(LocalDate.class))).thenReturn("DELETED");

        service.softDeleteVoucher(TRANS_POID, DOC_ID, deleteReason);

        verify(documentDeleteService).deleteDocument(eq(TRANS_POID), eq("GL_BANK_PAYMENT_HDR"),
                eq("TRANSACTION_POID"), eq(deleteReason), any(LocalDate.class));
    }

    @Test
    @DisplayName("softDeleteVoucher – voucher not found throws ValidationException")
    void softDeleteVoucher_NotFound_ThrowsValidationException() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.softDeleteVoucher(TRANS_POID, DOC_ID, new DeleteReasonDto()));
    }

    @Test
    @DisplayName("softDeleteVoucher – FDA JOBS releases old job values before deleting")
    void softDeleteVoucher_FdaJobs_ReleasesOldJobValues() {
        headerEntity.setRefType("FDA JOBS");
        headerEntity.setFdaRef(50L);
        DeleteReasonDto deleteReason = new DeleteReasonDto();

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(documentDeleteService.deleteDocument(anyLong(), anyString(), anyString(),
                any(DeleteReasonDto.class), any(LocalDate.class))).thenReturn("DELETED");

        service.softDeleteVoucher(TRANS_POID, DOC_ID, deleteReason);

        verify(documentDeleteService).deleteDocument(eq(TRANS_POID), anyString(), anyString(),
                any(DeleteReasonDto.class), any(LocalDate.class));
    }

    // ========================================================
    // getBankBalance tests
    // ========================================================

    @Test
    @DisplayName("getBankBalance – delegates to spRepository and returns map")
    void getBankBalance_HappyPath_ReturnsBankBalanceMap() {
        Map<String, BigDecimal> balanceMap = new HashMap<>();
        balanceMap.put("AVAILABLE_BALANCE", new BigDecimal("50000.00"));
        balanceMap.put("BANK_BALANCE", new BigDecimal("45000.00"));

        when(spRepository.getBankBalance(DOC_ID, TRANS_POID, LocalDate.now(), BANK_POID))
                .thenReturn(balanceMap);

        Map<String, BigDecimal> result = service.getBankBalance(DOC_ID, TRANS_POID, LocalDate.now(), BANK_POID);

        assertNotNull(result);
        assertEquals(balanceMap, result);
        verify(spRepository).getBankBalance(DOC_ID, TRANS_POID, LocalDate.now(), BANK_POID);
    }

    @Test
    @DisplayName("getBankBalance – returns empty map when no balance found")
    void getBankBalance_NoBalance_ReturnsEmptyMap() {
        when(spRepository.getBankBalance(anyString(), anyLong(), any(), anyLong()))
                .thenReturn(Collections.emptyMap());

        Map<String, BigDecimal> result = service.getBankBalance(DOC_ID, TRANS_POID, LocalDate.now(), BANK_POID);

        assertTrue(result.isEmpty());
    }

    // ========================================================
    // createBankPayFromFf tests
    // ========================================================

    @Test
    @DisplayName("createBankPayFromFf – delegates to loadDataRepository")
    void createBankPayFromFf_HappyPath_ReturnsFfResponse() {
        BankPayCreateFromFfResponse expected = new BankPayCreateFromFfResponse();
        expected.setItems(Collections.emptyList());
        when(loadDataRepository.executeBankPayFromFf("FF-001")).thenReturn(expected);

        BankPayCreateFromFfResponse result = service.createBankPayFromFf("FF-001");

        assertNotNull(result);
        assertEquals(expected, result);
        verify(loadDataRepository).executeBankPayFromFf("FF-001");
    }

    @Test
    @DisplayName("createBankPayFromFf – null ffPoid still delegates to repository")
    void createBankPayFromFf_NullPoid_DelegatesToRepository() {
        when(loadDataRepository.executeBankPayFromFf(null)).thenReturn(new BankPayCreateFromFfResponse());

        BankPayCreateFromFfResponse result = service.createBankPayFromFf(null);

        assertNotNull(result);
        verify(loadDataRepository).executeBankPayFromFf(null);
    }

    // ========================================================
    // createBankPayFromFda tests
    // ========================================================

    @Test
    @DisplayName("createBankPayFromFda – delegates to loadDataRepository")
    void createBankPayFromFda_HappyPath_ReturnsFdaResponse() {
        BankPayCreateFromFdaResponse expected = new BankPayCreateFromFdaResponse();
        expected.setItems(Collections.emptyList());
        when(loadDataRepository.executeBankPayFromFda("FDA-001")).thenReturn(expected);

        BankPayCreateFromFdaResponse result = service.createBankPayFromFda("FDA-001");

        assertNotNull(result);
        assertEquals(expected, result);
        verify(loadDataRepository).executeBankPayFromFda("FDA-001");
    }

    @Test
    @DisplayName("createBankPayFromFda – null fdaPoid still delegates to repository")
    void createBankPayFromFda_NullPoid_DelegatesToRepository() {
        when(loadDataRepository.executeBankPayFromFda(null)).thenReturn(new BankPayCreateFromFdaResponse());

        BankPayCreateFromFdaResponse result = service.createBankPayFromFda(null);

        assertNotNull(result);
    }

    // ========================================================
    // createBankPayment (MTA) tests
    // ========================================================

    @Test
    @DisplayName("createBankPayment – delegates to loadDataRepository")
    void createBankPayment_HappyPath_ReturnsMtaResponse() {
        BankPayCreateFromMtaResponse expected = new BankPayCreateFromMtaResponse();
        expected.setResultMessage("SUCCESS");
        when(loadDataRepository.executeBankPayProc("RFQ-001")).thenReturn(expected);

        BankPayCreateFromMtaResponse result = service.createBankPayment("RFQ-001");

        assertNotNull(result);
        assertEquals("SUCCESS", result.getResultMessage());
        verify(loadDataRepository).executeBankPayProc("RFQ-001");
    }

    @Test
    @DisplayName("createBankPayment – null rfqPoid still delegates to repository")
    void createBankPayment_NullPoid_DelegatesToRepository() {
        when(loadDataRepository.executeBankPayProc(null)).thenReturn(new BankPayCreateFromMtaResponse());

        BankPayCreateFromMtaResponse result = service.createBankPayment(null);

        assertNotNull(result);
    }

    // ========================================================
    // validateChequePrint tests
    // ========================================================

    @Test
    @DisplayName("validateChequePrint – happy path calls spRepository validateBeforeChequePrint")
    void validateChequePrint_HappyPath_CallsSpRepository() {
        headerEntity.setBankPoid(BANK_POID);
        headerEntity.setChqSignType("SINGLE");
        headerEntity.setSuppressValidation("N");

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(spRepository.validateBeforeChequePrint(anyLong(), anyLong(), anyLong(), anyLong(),
                anyString(), anyLong(), anyString())).thenReturn(Collections.emptyMap());

        assertDoesNotThrow(() -> service.validateChequePrint(TRANS_POID));

        verify(spRepository).validateBeforeChequePrint(
                eq(GROUP_POID), eq(USER_POID), eq(COMPANY_POID),
                eq(BANK_POID), eq("SINGLE"), eq(TRANS_POID), eq("N"));
    }

    @Test
    @DisplayName("validateChequePrint – voucher not found throws ValidationException")
    void validateChequePrint_NotFound_ThrowsValidationException() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.validateChequePrint(TRANS_POID));
    }

    // ========================================================
    // markChequePrinted tests
    // ========================================================

    @Test
    @DisplayName("markChequePrinted – happy path calls afterChequePrint and logs changes")
    void markChequePrinted_HappyPath_CallsSpRepositoryAndLogs() {
        headerEntity.setBankPoid(BANK_POID);
        headerEntity.setChqSignType("DUAL");
        headerEntity.setChqPrinted("N");

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        doNothing().when(spRepository).afterChequePrint(anyLong(), anyString(), anyLong(),
                anyLong(), anyLong(), anyString(), anyLong());

        service.markChequePrinted(TRANS_POID);

        verify(spRepository).afterChequePrint(eq(GROUP_POID), eq(USER_NAME), eq(COMPANY_POID),
                eq(TRANS_POID), eq(BANK_POID), eq("DUAL"), eq(USER_POID));
        verify(loggingService).logChanges(any(), any(), eq(GLPaymentVoucherHDREntity.class),
                eq(DOC_ID), eq(TRANS_POID.toString()), eq(LogDetailsEnum.MODIFIED), anyString());
    }

    @Test
    @DisplayName("markChequePrinted – voucher not found throws ValidationException")
    void markChequePrinted_NotFound_ThrowsValidationException() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.markChequePrinted(TRANS_POID));
    }

    // ========================================================
    // releaseCheque tests
    // ========================================================

    @Test
    @DisplayName("releaseCheque – happy path releases cheque and logs changes")
    void releaseCheque_HappyPath_CallsSpRepositoryAndLogs() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        doNothing().when(spRepository).releaseCheque(anyLong(), anyString(), anyLong(), anyLong(),
                anyString(), anyString());

        service.releaseCheque(TRANS_POID, "John Doe", "555-1234");

        verify(spRepository).releaseCheque(eq(GROUP_POID), eq(USER_NAME), eq(COMPANY_POID),
                eq(TRANS_POID), eq("John Doe"), eq("555-1234"));
        verify(loggingService).logChanges(any(), any(), eq(GLPaymentVoucherHDREntity.class),
                eq(DOC_ID), eq(TRANS_POID.toString()), eq(LogDetailsEnum.MODIFIED), anyString());
    }

    @Test
    @DisplayName("releaseCheque – voucher not found throws ValidationException")
    void releaseCheque_NotFound_ThrowsValidationException() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.releaseCheque(TRANS_POID, "John", "555"));
    }

    // ========================================================
    // unReleaseCheque tests
    // ========================================================

    @Test
    @DisplayName("unReleaseCheque – happy path calls unReleaseCheque sp and logs changes")
    void unReleaseCheque_HappyPath_CallsSpRepositoryAndLogs() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        doNothing().when(spRepository).unReleaseCheque(anyLong(), anyLong(), anyLong(), anyLong());

        service.unReleaseCheque(TRANS_POID);

        verify(spRepository).unReleaseCheque(eq(GROUP_POID), eq(USER_POID), eq(COMPANY_POID), eq(TRANS_POID));
        verify(loggingService).logChanges(any(), any(), eq(GLPaymentVoucherHDREntity.class),
                eq(DOC_ID), eq(TRANS_POID.toString()), eq(LogDetailsEnum.MODIFIED), anyString());
    }

    @Test
    @DisplayName("unReleaseCheque – voucher not found throws ValidationException")
    void unReleaseCheque_NotFound_ThrowsValidationException() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.unReleaseCheque(TRANS_POID));
    }

    // ========================================================
    // resetChequeStatus tests
    // ========================================================

    @Test
    @DisplayName("resetChequeStatus – happy path calls resetChequeStatus sp and logs changes")
    void resetChequeStatus_HappyPath_CallsSpRepositoryAndLogs() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        doNothing().when(spRepository).resetChequeStatus(anyLong(), anyLong(), any(), anyLong());

        service.resetChequeStatus(TRANS_POID);

        verify(spRepository).resetChequeStatus(eq(GROUP_POID), eq(COMPANY_POID), isNull(), eq(TRANS_POID));
        verify(loggingService).logChanges(any(), any(), eq(GLPaymentVoucherHDREntity.class),
                eq(DOC_ID), eq(TRANS_POID.toString()), eq(LogDetailsEnum.MODIFIED), anyString());
    }

    @Test
    @DisplayName("resetChequeStatus – voucher not found throws ValidationException")
    void resetChequeStatus_NotFound_ThrowsValidationException() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.resetChequeStatus(TRANS_POID));
    }

    // ========================================================
    // revertReconciliation tests
    // ========================================================

    @Test
    @DisplayName("revertReconciliation – happy path calls sp and logs changes")
    void revertReconciliation_HappyPath_CallsSpRepositoryAndReturnsStatus() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(spRepository.revertReconciliation(anyLong(), anyLong(), anyLong(), anyString(),
                anyString(), anyString())).thenReturn("SUCCESS");

        String result = service.revertReconciliation(TRANS_POID, DOC_ID);

        assertEquals("SUCCESS", result);
        verify(spRepository).revertReconciliation(eq(GROUP_POID), eq(COMPANY_POID), eq(USER_POID),
                eq(DOC_ID), eq(TRANS_POID.toString()), eq("Y"));
        verify(loggingService).logChanges(any(), any(), eq(GLPaymentVoucherHDREntity.class),
                eq(DOC_ID), eq(TRANS_POID.toString()), eq(LogDetailsEnum.MODIFIED), anyString());
    }

    @Test
    @DisplayName("revertReconciliation – voucher not found throws ValidationException")
    void revertReconciliation_NotFound_ThrowsValidationException() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.revertReconciliation(TRANS_POID, DOC_ID));
    }

    @Test
    @DisplayName("revertReconciliation – sp returns non-SUCCESS status is returned as-is")
    void revertReconciliation_SpReturnsError_ReturnsErrorMessage() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(spRepository.revertReconciliation(anyLong(), anyLong(), anyLong(), anyString(),
                anyString(), anyString())).thenReturn("ALREADY_REVERTED");

        String result = service.revertReconciliation(TRANS_POID, DOC_ID);

        assertEquals("ALREADY_REVERTED", result);
    }

    // ========================================================
    // listBankPaymentVouchers tests
    // ========================================================

    @Test
    @DisplayName("listBankPaymentVouchers – happy path returns paginated map")
    void listBankPaymentVouchers_HappyPath_ReturnsPaginatedResult() {
        FilterRequestDto filters = new FilterRequestDto(null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        List<Map<String, Object>> records = List.of(
                Map.of("TRANSACTION_POID", 100L, "DOC_REF", "BPV-100")
        );
        Map<String, String> displayFields = Map.of("TRANSACTION_POID", "ID", "DOC_REF", "Reference");
        RawSearchResult rawResult = new RawSearchResult(records, displayFields, 1L);

        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveDateFilters(any(), anyString(), any(), any())).thenReturn(Collections.emptyList());
        when(documentService.search(anyString(), anyList(), anyString(), any(Pageable.class),
                anyString(), anyString(), anyString())).thenReturn(rawResult);

        Map<String, Object> result = service.listBankPaymentVouchers(DOC_ID, filters, null, null, pageable);

        assertNotNull(result);
        verify(documentService).search(eq(DOC_ID), anyList(), eq("AND"), eq(pageable), eq("N"),
                eq("TRANSACTION_POID"), eq("DOC_REF"));
    }

    @Test
    @DisplayName("listBankPaymentVouchers – with date filters delegates to documentService")
    void listBankPaymentVouchers_WithDateFilters_PassesDatesToDocumentService() {
        FilterRequestDto filters = new FilterRequestDto(null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        RawSearchResult rawResult = new RawSearchResult(Collections.emptyList(), Collections.emptyMap(), 0L);

        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveDateFilters(any(), anyString(), eq(startDate), eq(endDate)))
                .thenReturn(Collections.emptyList());
        when(documentService.search(anyString(), anyList(), anyString(), any(Pageable.class),
                anyString(), anyString(), anyString())).thenReturn(rawResult);

        Map<String, Object> result = service.listBankPaymentVouchers(DOC_ID, filters, startDate, endDate, pageable);

        assertNotNull(result);
        verify(documentService).resolveDateFilters(any(), eq("TRANSACTION_DATE"), eq(startDate), eq(endDate));
    }

    @Test
    @DisplayName("listBankPaymentVouchers – empty results returns wrapped empty page")
    void listBankPaymentVouchers_EmptyResult_ReturnsEmptyPage() {
        FilterRequestDto filters = new FilterRequestDto(null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult rawResult = new RawSearchResult(Collections.emptyList(), Collections.emptyMap(), 0L);

        when(documentService.resolveOperator(any())).thenReturn("OR");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveDateFilters(any(), anyString(), any(), any())).thenReturn(Collections.emptyList());
        when(documentService.search(anyString(), anyList(), anyString(), any(Pageable.class),
                anyString(), anyString(), anyString())).thenReturn(rawResult);

        Map<String, Object> result = service.listBankPaymentVouchers(DOC_ID, filters, null, null, pageable);

        assertNotNull(result);
    }

    // ========================================================
    // getReconciledDate tests
    // ========================================================

    @Test
    @DisplayName("getReconciledDate – happy path returns ReconcileResultDto with date")
    void getReconciledDate_HappyPath_ReturnsResult() {
        StoredProcedureQuery spQuery = mock(StoredProcedureQuery.class);
        when(entityManager.createStoredProcedureQuery("PROC_DEBIT_PAYMENT_RECON_DATE")).thenReturn(spQuery);
        when(spQuery.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(spQuery);
        when(spQuery.setParameter(anyString(), any())).thenReturn(spQuery);
        when(spQuery.execute()).thenReturn(true);
        // cursor coming back as null → should return ReconcileResultDto(null, null)
        when(spQuery.getOutputParameterValue("OUTDATA")).thenReturn(null);

        ReconcileResultDto result = service.getReconciledDate(DOC_ID, TRANS_POID);

        assertNotNull(result);
        assertNull(result.getReconcileDate());
        assertNull(result.getHold());
    }

    @Test
    @DisplayName("getReconciledDate – registers correct parameters and executes procedure")
    void getReconciledDate_RegistersCorrectParams() {
        StoredProcedureQuery spQuery = mock(StoredProcedureQuery.class);
        when(entityManager.createStoredProcedureQuery("PROC_DEBIT_PAYMENT_RECON_DATE")).thenReturn(spQuery);
        when(spQuery.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(spQuery);
        when(spQuery.execute()).thenReturn(true);
        when(spQuery.getOutputParameterValue("OUTDATA")).thenReturn(null);

        service.getReconciledDate(DOC_ID, TRANS_POID);

        verify(entityManager).createStoredProcedureQuery("PROC_DEBIT_PAYMENT_RECON_DATE");
        verify(spQuery).execute();
    }

    // ========================================================
    // print tests
    // ========================================================

    @Test
    @DisplayName("print – header without pre-printed uses 'WithOutCheque' report")
    void print_WithoutPrePrinted_UsesWithoutChequeReport() throws Exception {
        headerEntity.setPrePrinted("N");
        headerEntity.setRefType("GENERAL");

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(printService.buildBaseParams(eq(TRANS_POID), eq(DOC_ID))).thenReturn(new HashMap<>());
        net.sf.jasperreports.engine.JasperReport mockReport = mock(net.sf.jasperreports.engine.JasperReport.class);
        when(printService.load("Finance/BankPayments/BankPaymentVoucher_WithOutCheque.jrxml")).thenReturn(mockReport);
        byte[] pdfBytes = new byte[]{1, 2, 3};
        when(printService.fillReportToPdf(eq(mockReport), anyMap(), eq(dataSource))).thenReturn(pdfBytes);

        byte[] result = service.print(TRANS_POID);

        assertNotNull(result);
        assertArrayEquals(pdfBytes, result);
        verify(printService).load("Finance/BankPayments/BankPaymentVoucher_WithOutCheque.jrxml");
    }

    @Test
    @DisplayName("print – header with pre-printed uses 'ManualCheque' report and sub-detail")
    void print_WithPrePrinted_UsesManualChequeReport() throws Exception {
        headerEntity.setPrePrinted("Y");
        headerEntity.setRefType("GENERAL");

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(printService.buildBaseParams(eq(TRANS_POID), eq(DOC_ID))).thenReturn(new HashMap<>());
        net.sf.jasperreports.engine.JasperReport subReport = mock(net.sf.jasperreports.engine.JasperReport.class);
        net.sf.jasperreports.engine.JasperReport mainReport = mock(net.sf.jasperreports.engine.JasperReport.class);
        when(printService.load("Finance/BankPayments/BankPaymentVoucher_ManualCheque1_subreport1.jrxml"))
                .thenReturn(subReport);
        when(printService.load("Finance/BankPayments/BankPaymentVoucher_ManualCheque.jrxml"))
                .thenReturn(mainReport);
        byte[] pdfBytes = new byte[]{4, 5, 6};
        when(printService.fillReportToPdf(eq(mainReport), anyMap(), eq(dataSource))).thenReturn(pdfBytes);

        byte[] result = service.print(TRANS_POID);

        assertArrayEquals(pdfBytes, result);
        verify(printService).load("Finance/BankPayments/BankPaymentVoucher_ManualCheque.jrxml");
    }

    @Test
    @DisplayName("print – CUSTOM ref type sets P_PRINT_WITHOUT_BILL to Y")
    void print_CustomRefType_SetsPrintWithoutBillParamToY() throws Exception {
        headerEntity.setPrePrinted("N");
        headerEntity.setRefType("CUSTOM");

        Map<String, Object> params = new HashMap<>();
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(headerEntity));
        when(printService.buildBaseParams(eq(TRANS_POID), eq(DOC_ID))).thenReturn(params);
        net.sf.jasperreports.engine.JasperReport mockReport = mock(net.sf.jasperreports.engine.JasperReport.class);
        when(printService.load(anyString())).thenReturn(mockReport);
        when(printService.fillReportToPdf(any(), anyMap(), eq(dataSource))).thenReturn(new byte[0]);

        service.print(TRANS_POID);

        assertEquals("Y", params.get("P_PRINT_WITHOUT_BILL"));
    }

    @Test
    @DisplayName("print – voucher not found throws ValidationException")
    void print_NotFound_ThrowsValidationException() {
        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> service.print(TRANS_POID));
    }

    // ========================================================
    // Edge-case / internal validation tests
    // ========================================================

    @Test
    @DisplayName("createBankPaymentVoucher – cheque date back-dated throws ValidationException")
    void createBankPaymentVoucher_BackDatedCheque_ThrowsValidationException() {
        BankPaymentVoucherRequest req = buildGeneralRequest();
        // back-dated 10 days, with limit of only 5 days allowed
        req.setChequeDate(LocalDate.now().minusDays(10).toString());

        when(globalParameterService.getParameterValue("CHEQUE_DATE_VALIDATION_DAYS", "GROUP", "1", "0"))
                .thenReturn("-5");

        assertThrows(ValidationException.class,
                () -> service.createBankPaymentVoucher(req, DOC_ID));
    }

    @Test
    @DisplayName("createBankPaymentVoucher – non-GENERAL empty glDetails throws ValidationException")
    void createBankPaymentVoucher_FdaWithNoChargeDetails_ThrowsValidationException() {
        BankPaymentVoucherRequest req = buildFdaRequest();
        req.setChargeDetailRequests(Collections.emptyList());

        assertThrows(ValidationException.class,
                () -> service.createBankPaymentVoucher(req, DOC_ID));
    }

    @Test
    @DisplayName("createBankPaymentVoucher – MTA with null itemDetailRequests throws ValidationException")
    void createBankPaymentVoucher_MtaWithNullItems_ThrowsValidationException() {
        BankPaymentVoucherRequest req = buildMtaRequest();
        req.setItemDetailRequests(null);

        assertThrows(ValidationException.class,
                () -> service.createBankPaymentVoucher(req, DOC_ID));
    }

    @Test
    @DisplayName("createBankPaymentVoucher – validates job for FF JOBS ref type")
    void createBankPaymentVoucher_FfJobs_ValidatesJobViaSpRepository() {
        BankPaymentVoucherRequest ffReq = buildFfRequest();
        GLPaymentVoucherHDREntity ffHeader = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(TRANS_POID).groupPoid(GROUP_POID).companyPoid(COMPANY_POID)
                .refType("FF JOBS").ffRef("60").deleted("N").released("N").prePrinted("N").build();

        when(globalParameterService.getParameterValue(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("999");
        when(spRepository.validateJob(anyLong(), any(), anyLong(), any(), eq("FF JOBS"), anyString()))
                .thenReturn("SUCCESS");
        when(paymentVoucherRepository.save(any(GLPaymentVoucherHDREntity.class))).thenReturn(ffHeader);
        doNothing().when(paymentVoucherRepository).flush();
        when(chargeDtlRepository.findByTransactionPoid(any())).thenReturn(Collections.emptyList());
        when(chargeDtlRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(paymentVoucherRepository.findById(any())).thenReturn(Optional.of(ffHeader));
        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(new GlVoucherLoadBillwiseBreakupResponseDto());
        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new GlVoucherCostCenterBreakupResponseDto());

        BankPaymentVoucherResponse result = service.createBankPaymentVoucher(ffReq, DOC_ID);

        assertNotNull(result);
        verify(spRepository).validateJob(anyLong(), any(), anyLong(), any(), eq("FF JOBS"), eq("60"));
    }

    @Test
    @DisplayName("createBankPaymentVoucher – job validation failure throws ValidationException")
    void createBankPaymentVoucher_JobValidationFails_ThrowsValidationException() {
        BankPaymentVoucherRequest ffReq = buildFfRequest();

        when(globalParameterService.getParameterValue(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("999");
        when(spRepository.validateJob(anyLong(), any(), anyLong(), any(), eq("FF JOBS"), anyString()))
                .thenReturn("JOB_NOT_VALID");

        assertThrows(ValidationException.class,
                () -> service.createBankPaymentVoucher(ffReq, DOC_ID));
    }

    @Test
    @DisplayName("updateBankPaymentVoucher – FF JOBS updates charge details")
    void updateBankPaymentVoucher_FfJobs_UpdatesChargeDetails() {
        GLPaymentVoucherHDREntity ffHeader = GLPaymentVoucherHDREntity.builder()
                .transactionPoid(TRANS_POID).groupPoid(GROUP_POID).companyPoid(COMPANY_POID)
                .refType("FF JOBS").ffRef("60").deleted("N").released("N").prePrinted("N").build();

        when(paymentVoucherRepository.findById(TRANS_POID)).thenReturn(Optional.of(ffHeader));
        when(globalParameterService.getParameterValue(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("999");
        when(paymentVoucherRepository.save(any(GLPaymentVoucherHDREntity.class))).thenReturn(ffHeader);
        when(chargeDtlRepository.findByTransactionPoid(TRANS_POID)).thenReturn(Collections.emptyList());
        when(chargeDtlRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(new GlVoucherLoadBillwiseBreakupResponseDto());
        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new GlVoucherCostCenterBreakupResponseDto());

        BankPaymentVoucherResponse result = service.updateBankPaymentVoucher(TRANS_POID, buildFfRequest(), DOC_ID);

        assertNotNull(result);
        verify(chargeDtlRepository, atLeast(1)).saveAll(anyList());
    }
}
