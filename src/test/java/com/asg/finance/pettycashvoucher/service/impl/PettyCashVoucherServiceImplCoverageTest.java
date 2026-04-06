package com.asg.finance.pettycashvoucher.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.ShowPendingBillwiseBreakupResponseDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.model.CustomAuthDetails;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.GlobalParameterService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.dto.*;
import com.asg.finance.entity.AdvancePettyCashHdr;
import com.asg.finance.entity.GLMaster;
import com.asg.finance.entity.GLPettyCashItemDtl;
import com.asg.finance.entity.GlPettyCashChargeDtl;
import com.asg.finance.entity.GlPettyCashPaymentDtl;
import com.asg.finance.entity.GlPettyCashPaymentGrnDtl;
import com.asg.finance.entity.GlPettyCashPaymentHdr;
import com.asg.finance.entity.StockMasterEntity;
import com.asg.finance.entity.SupplierMasterEntity;
import com.asg.finance.entity.TaxMaster;
import com.asg.finance.entity.master.ShipChargeEntity;
import com.asg.finance.entity.master.UnitMaster;
import com.asg.finance.repository.*;
import com.asg.finance.repository.master.ShipChargeRepository;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import com.asg.finance.service.impl.PettyCashVoucherServiceImpl;
import jakarta.persistence.EntityManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PettyCashVoucherServiceImplCoverageTest {

    @Mock private GlPettyCashPaymentHdrRepository glPettyCashPaymentHdrRepository;
    @Mock private GlPettyCashPaymentDtlRepository glPettyCashPaymentDtlRepository;
    @Mock private GLPettyCashItemDtlRepository glPettyCashItemDtlRepository;
    @Mock private GlPettyCashChargeDtlRepository glPettyCashChargeDtlRepository;
    @Mock private GlPettyCashPaymentGrnDtlRepository glPettyCashPaymentGrnDtlRepository;
    @Mock private GLMasterRepository glMasterRepository;
    @Mock private StockMasterRepository stockMasterRepository;
    @Mock private ShipChargeRepository shipChargeRepository;
    @Mock private UnitMasterRepository unitMasterRepository;
    @Mock private com.asg.common.lib.service.DocumentSearchService documentService;
    @Mock private TaxMasterRepository taxMasterRepository;
    @Mock private com.asg.common.lib.service.LovDataService lovService;
    @Mock private CostCenterBreakupService costCenterBreakupService;
    @Mock private BillwiseBreakupService billwiseBreakupService;
    @Mock private DocumentDeleteService documentDeleteService;
    @Mock private PettyCashLoadByRefTypeRepository pettyCashLoadByRefTypeRepository;
    @Mock private PettyCashPaymentVoucherCustomRepository pettyCashPaymentVoucherCustomRepository;
    @Mock private SupplierMasterRepository supplierMasterRepository;
    @Mock private AdvancePettyCashHdrRepository advancePettyCashHdrRepository;
    @Mock private AssetLocationMasterRepository assetLocationMasterRepository;
    @Mock private PrintService printService;
    @Mock private EntityManager entityManager;
    @Mock private LoggingService loggingService;
    @Mock private GlobalParameterService globalParameterService;

    @InjectMocks
    private PettyCashVoucherServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        UserContext.setCurrentUser(currentUser());
        lenient().when(globalParameterService.getParameterValue(anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(3));
        lenient().when(pettyCashLoadByRefTypeRepository.getPettyGlBalance(any(), any(), any(), anyString(), any(), any(), any()))
                .thenReturn(balanceResponse(new BigDecimal("999.000")));
        lenient().doNothing().when(pettyCashPaymentVoucherCustomRepository).validateGlVouchers(any(), any(), any(), anyString(), anyString(), anyString(), any());
        lenient().doNothing().when(pettyCashPaymentVoucherCustomRepository).validateBeforeSave(any(), any(), any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), any(), any());
        lenient().doNothing().when(pettyCashPaymentVoucherCustomRepository).validateJobBeforeSave(any(), any(), any(), anyString(), anyString(), anyString(), any());
        lenient().doNothing().when(pettyCashPaymentVoucherCustomRepository).loadAdvanceDetails(any(), any(), any(), any(), anyString(), any(), anyList());
        lenient().doNothing().when(pettyCashPaymentVoucherCustomRepository).updateSalesGrnStatus(any(), any(), any(), anyString(), any(), any());
        lenient().doNothing().when(pettyCashPaymentVoucherCustomRepository).updateCostFF(any(), any(), any(), anyString(), any(), any());
        lenient().doNothing().when(pettyCashPaymentVoucherCustomRepository).updateCostFDA(any(), any(), any(), anyString(), any(), any());
        lenient().doNothing().when(pettyCashPaymentVoucherCustomRepository).updateRfqPurchasePrice(any(), any(), any(), anyString(), any());
        lenient().doNothing().when(pettyCashPaymentVoucherCustomRepository).updatePurchaseOrderStatus(any(), any(), any(), anyString(), any(), any());
        lenient().when(pettyCashPaymentVoucherCustomRepository.validateVoucherBeforeDelete(any(), any(), any(), anyString(), anyString(), anyString()))
                .thenReturn("OK");
        lenient().when(glPettyCashPaymentHdrRepository.save(any())).thenAnswer(inv -> {
            GlPettyCashPaymentHdr hdr = inv.getArgument(0);
            if (hdr.getTransactionPoid() == null) {
                hdr.setTransactionPoid(1000L);
            }
            return hdr;
        });
        lenient().when(glPettyCashPaymentDtlRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(glPettyCashChargeDtlRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(glPettyCashItemDtlRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(glPettyCashPaymentGrnDtlRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(glPettyCashPaymentDtlRepository.findByTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(glPettyCashChargeDtlRepository.findByTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(glPettyCashItemDtlRepository.findByTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(glPettyCashPaymentGrnDtlRepository.findByTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(billwiseBreakupService.loadBillwiseBreakup(any(), any(), anyString(), any())).thenReturn(null);
        lenient().when(costCenterBreakupService.loadCostCenterData(anyString(), any(), any(), any(), any())).thenReturn(null);
        lenient().when(printService.buildBaseParams(anyLong(), anyString())).thenReturn(new HashMap<>());
        lenient().when(printService.load(anyString())).thenReturn(mock(JasperReport.class));
        lenient().when(printService.fillReportToPdf(any(), anyMap(), any())).thenReturn(new byte[]{1, 2, 3});
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createPettyCash_generalPath_coversHeaderDetailAndBreakupLoading() {
        PettyCashCreateRequestDto request = baseCreateRequest("GENERAL");
        request.setStatus("AGAINST_ADVANCE");
        request.setGlPettyCashPaymentDtlRequestDtos(List.of(
                        GlPettyCashPaymentDtlRequestDto.builder()
                                .detRowId(1L)
                                .type("Dr")
                                .companyPoid(77L)
                                .glPoid(10L)
                                .drAmt(new BigDecimal("100"))
                                .totalAmount(new BigDecimal("100"))
                                .actionType("isCreated")
                                .billwiseBreakupList(List.of(
                                        BillwiseBreakupPopupRequestDto.builder()
                                                .billDetRowId(11L)
                                                .billRefType("PO")
                                                .billRef("B-1")
                                                .billDueDate(LocalDate.now())
                                                .type("DR")
                                                .amount(new BigDecimal("60"))
                                                .billRemarks("b1")
                                                .actionType("isCreated")
                                                .build(),
                                        BillwiseBreakupPopupRequestDto.builder()
                                                .billDetRowId(12L)
                                                .billRefType("PO")
                                                .billRef("B-2")
                                                .billDueDate(LocalDate.now())
                                                .type("CR")
                                                .amount(new BigDecimal("40"))
                                                .billRemarks("b2")
                                                .actionType("isCreated")
                                                .build()
                                ))
                                .costCenterBreakupList(List.of(
                                        CostCenterBreakupPopupRequestDto.builder()
                                                .costDetRowId(21L)
                                                .costGroup("LOCATION")
                                                .costPoid("55")
                                                .amount(new BigDecimal("100"))
                                                .actionType("isCreated")
                                                .build()
                                ))
                                .build()
                ));

        GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse = new GlVoucherLoadBillwiseBreakupResponseDto();
        LoadBillwiseBreakupResponseDto billwise1 = new LoadBillwiseBreakupResponseDto();
        billwise1.setMainDetRowId(1L);
        billwise1.setBillDetRowId(11L);
        billwise1.setBillRefType("PO");
        billwise1.setBillRef("B-1");
        billwise1.setBillDueDate(LocalDate.now());
        billwise1.setDrAmt(new BigDecimal("60"));
        billwise1.setBillRemarks("b1");
        LoadBillwiseBreakupResponseDto billwise2 = new LoadBillwiseBreakupResponseDto();
        billwise2.setMainDetRowId(2L);
        billwise2.setBillDetRowId(12L);
        billwise2.setBillRefType("PO");
        billwise2.setBillRef("B-2");
        billwise2.setBillDueDate(LocalDate.now());
        billwise2.setCrAmt(new BigDecimal("40"));
        billwise2.setBillRemarks("b2");
        billwiseResponse.setLoadBillwiseBreakupResponseDtoList(List.of(billwise1, billwise2));

        GlVoucherCostCenterBreakupResponseDto costCenterResponse = new GlVoucherCostCenterBreakupResponseDto();
        CostCenterBreakupResponseDto costCenter = CostCenterBreakupResponseDto.builder()
                .mainDetRowId(1L)
                .costDetRowId(21L)
                .costGroup("LOCATION")
                .costPoid("55")
                .amount(new BigDecimal("100"))
                .description("cc")
                .build();
        costCenterResponse.setCostBreakupList(List.of(costCenter));

        when(billwiseBreakupService.loadBillwiseBreakup(any(), any(), anyString(), any())).thenReturn(billwiseResponse);
        when(costCenterBreakupService.loadCostCenterData(anyString(), any(), any(), any(), any())).thenReturn(costCenterResponse);
        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<AdvanceDetailDto> out = inv.getArgument(6);
            out.add(new AdvanceDetailDto());
            return null;
        }).when(pettyCashPaymentVoucherCustomRepository).loadAdvanceDetails(any(), any(), any(), any(), anyString(), any(), anyList());

        when(glMasterRepository.findByGlPoid(anyLong())).thenAnswer(inv -> Optional.of(gl((Long) inv.getArgument(0))));
        when(shipChargeRepository.findByChargePoid(anyLong())).thenAnswer(inv -> Optional.of(charge((Long) inv.getArgument(0))));
        when(stockMasterRepository.findByStockPoid(anyLong())).thenAnswer(inv -> Optional.of(stock((Long) inv.getArgument(0))));
        when(unitMasterRepository.findByUnitPoid(anyLong())).thenAnswer(inv -> Optional.of(unit((Long) inv.getArgument(0))));
        when(taxMasterRepository.findByTaxPoid(anyLong())).thenAnswer(inv -> Optional.of(tax((Long) inv.getArgument(0))));
        when(supplierMasterRepository.findBySupplierPoid(anyLong())).thenAnswer(inv -> supplier((Long) inv.getArgument(0)));
        when(advancePettyCashHdrRepository.findByTransactionPoid(anyLong())).thenAnswer(inv -> Optional.of(advanceHeader((Long) inv.getArgument(0))));
        when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenAnswer(inv -> lov((Long) inv.getArgument(0), (String) inv.getArgument(1)));
        when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenAnswer(inv -> lovByCode((String) inv.getArgument(0), (String) inv.getArgument(1)));

        PettyCashResponseDto response = service.createPettyCash(request, "DOC123");

        assertNotNull(response);
        assertEquals(1000L, response.getTransactionPoid());
        assertNotNull(response.getPettyCashGlPoidDtl());
        assertNotNull(response.getGrnSupplierPoidDtl());
        assertNotNull(response.getSupplierGlPoidDtl());
        assertNotNull(response.getCustomerGlPoidDtl());
        assertNotNull(response.getAdvancePettyCashPoidDtl());
        assertNotNull(response.getFfRefDtl());
        assertNotNull(response.getFdaRefDtl());
        assertNotNull(response.getSalesQtnRefDtl());
        assertNotNull(response.getPaymentDtls());
        assertEquals(2, response.getPaymentDtls().size());
        verify(billwiseBreakupService).insertBillwiseBreakup(anyList());
        verify(costCenterBreakupService).saveCostCenterBreakups(anyList());
        verify(billwiseBreakupService).loadBillwiseBreakup(any(), any(), anyString(), any());
        verify(costCenterBreakupService).loadCostCenterData(anyString(), any(), any(), any(), any());
    }

    @Test
    void createPettyCash_ffJobsPath_coversChargeBranch() {
        PettyCashCreateRequestDto request = baseCreateRequest("FF JOBS");
        request.setFfRef("FF-001");
        request.setAmount(new BigDecimal("60"));
        request.setGlPettyCashChargeDtlRequestDtos(List.of(
                        GlPettyCashChargeDtlRequestDto.builder()
                                .detRowId(1L)
                                .chargePoid(31L)
                                .chargeAmount(new BigDecimal("60"))
                                .checkAll("Y")
                                .actionType("isCreated")
                                .build(),
                        GlPettyCashChargeDtlRequestDto.builder()
                                .detRowId(2L)
                                .chargePoid(32L)
                                .chargeAmount(new BigDecimal("40"))
                                .checkAll("N")
                                .actionType("isCreated")
                                .build()
                ));

        when(shipChargeRepository.findByChargePoid(anyLong())).thenAnswer(inv -> Optional.of(charge((Long) inv.getArgument(0))));
        when(taxMasterRepository.findByTaxPoid(anyLong())).thenAnswer(inv -> Optional.of(tax((Long) inv.getArgument(0))));

        PettyCashResponseDto response = service.createPettyCash(request, "DOC123");

        assertNotNull(response);
        assertEquals(1, response.getChargeDtls().size());
        assertEquals("FF", response.getChargeDtls().get(0).getChargeFrom());
        verify(pettyCashPaymentVoucherCustomRepository).updateCostFF(any(), any(), any(), eq("FF-001"), eq(1000L), any());
    }

    @Test
    void createPettyCash_mtaRfqPath_coversItemBranch() {
        PettyCashCreateRequestDto request = baseCreateRequest("MTA RFQ");
        request.setSalesQtnRef("RFQ-001");
        request.setGlPettyCashItemDtlRequestDtos(List.of(
                        GlPettyCashItemDtlRequestDto.builder()
                                .detRowId(1L)
                                .stockPoid(41L)
                                .stockUnitPoid(42L)
                                .poQty(new BigDecimal("2"))
                                .price(new BigDecimal("10"))
                                .total(new BigDecimal("20"))
                                .checkAll("Y")
                                .actionType("isCreated")
                                .build()
                ));

        when(stockMasterRepository.findByStockPoid(anyLong())).thenAnswer(inv -> Optional.of(stock((Long) inv.getArgument(0))));
        when(unitMasterRepository.findByUnitPoid(anyLong())).thenAnswer(inv -> Optional.of(unit((Long) inv.getArgument(0))));
        when(taxMasterRepository.findByTaxPoid(anyLong())).thenAnswer(inv -> Optional.of(tax((Long) inv.getArgument(0))));

        PettyCashResponseDto response = service.createPettyCash(request, "DOC123");

        assertNotNull(response);
        assertEquals(1, response.getItemDtls().size());
        assertNotNull(response.getItemDtls().get(0).getStockPoidDtl());
        assertNotNull(response.getItemDtls().get(0).getStockUnitPoidDtl());
        verify(pettyCashPaymentVoucherCustomRepository).updateRfqPurchasePrice(any(), any(), any(), eq("RFQ-001"), any());
    }

    @Test
    void createPettyCash_grnJobsPath_coversGrnBranch() {
        PettyCashCreateRequestDto request = baseCreateRequest("GRN_JOBS");
        request.setPoRef("PO-001");
        request.setAmount(new BigDecimal("30"));
        request.setGlPettyCashGrnDtlRequestDtos(List.of(
                        GlPettyCashPaymentGrnDtlRequestDto.builder()
                                .detRowId(1L)
                                .grnPoid(51L)
                                .amount(new BigDecimal("30"))
                                .checkAll("Y")
                                .actionType("isCreated")
                                .build(),
                        GlPettyCashPaymentGrnDtlRequestDto.builder()
                                .detRowId(2L)
                                .grnPoid(52L)
                                .amount(new BigDecimal("10"))
                                .checkAll("N")
                                .actionType("isCreated")
                                .build()
                ));

        PettyCashResponseDto response = service.createPettyCash(request, "DOC123");

        assertNotNull(response);
        verify(pettyCashPaymentVoucherCustomRepository).updateSalesGrnStatus(any(), any(), any(), eq("DOC123"), eq(1000L), any());
    }

    @Test
    void updatePettyCash_generalPath_coversMergeAndBreakupUpdates() {
        GlPettyCashPaymentHdr existingHdr = savedHeader(2000L, "GENERAL");
        existingHdr.setDocRef("DOC-OLD");
        when(glPettyCashPaymentHdrRepository.findByTransactionPoid(2000L)).thenReturn(Optional.of(existingHdr));
        doAnswer(inv -> {
            ((StringBuilder) inv.getArgument(5)).append("FF JOBS");
            ((StringBuilder) inv.getArgument(6)).append("FF-OLD");
            return null;
        }).when(pettyCashPaymentVoucherCustomRepository).getOldJobReferences(any(), any(), any(), anyString(), anyString(), any(), any());

        GlPettyCashPaymentDtl existing1 = paymentEntity(2000L, 1L, "Dr", 10L, new BigDecimal("60"), null);
        GlPettyCashPaymentDtl existing3 = paymentEntity(2000L, 3L, "Dr", 11L, new BigDecimal("40"), null);
        existing1.setCreatedDate(LocalDateTime.now().minusMinutes(2));
        existing3.setCreatedDate(LocalDateTime.now().minusMinutes(2));
        when(glPettyCashPaymentDtlRepository.findByTransactionPoid(2000L)).thenReturn(List.of(existing1, existing3));

        PettyCashUpdateRequestDto request = baseUpdateRequest("GENERAL");
        request.setGlPettyCashPaymentDtlRequestDtos(List.of(
                        GlPettyCashPaymentDtlRequestDto.builder()
                                .detRowId(1L)
                                .type("Dr")
                                .companyPoid(77L)
                                .glPoid(10L)
                                .drAmt(new BigDecimal("60"))
                                .totalAmount(new BigDecimal("60"))
                                .actionType("isUpdated")
                                .billwiseBreakupList(List.of(billwiseDrivenRequest(11L, "isUpdated", new BigDecimal("60"))))
                                .costCenterBreakupList(List.of(CostCenterBreakupPopupRequestDto.builder()
                                        .costDetRowId(21L)
                                        .costGroup("LOCATION")
                                        .costPoid("55")
                                        .amount(new BigDecimal("60"))
                                        .actionType("isUpdated")
                                        .build()))
                                .build(),
                        GlPettyCashPaymentDtlRequestDto.builder()
                                .detRowId(2L)
                                .type("Dr")
                                .companyPoid(77L)
                                .glPoid(12L)
                                .drAmt(new BigDecimal("40"))
                                .totalAmount(new BigDecimal("40"))
                                .actionType("isCreated")
                                .billwiseBreakupList(List.of(billwiseDrivenRequest(12L, null, new BigDecimal("40"))))
                                .costCenterBreakupList(List.of(CostCenterBreakupPopupRequestDto.builder()
                                        .costDetRowId(22L)
                                        .costGroup("LOCATION")
                                        .costPoid("56")
                                        .amount(new BigDecimal("40"))
                                        .actionType("isCreated")
                                        .build()))
                                .build(),
                        GlPettyCashPaymentDtlRequestDto.builder()
                                .detRowId(3L)
                                .type("Dr")
                                .companyPoid(77L)
                                .glPoid(13L)
                                .drAmt(new BigDecimal("0"))
                                .totalAmount(new BigDecimal("0"))
                                .actionType("isDeleted")
                                .build()
                ));

        GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse = new GlVoucherLoadBillwiseBreakupResponseDto();
        LoadBillwiseBreakupResponseDto row = new LoadBillwiseBreakupResponseDto();
        row.setMainDetRowId(1L);
        row.setBillDetRowId(11L);
        row.setBillRefType("PO");
        row.setBillRef("B-1");
        row.setBillDueDate(LocalDate.now());
        row.setDrAmt(new BigDecimal("60"));
        billwiseResponse.setLoadBillwiseBreakupResponseDtoList(List.of(row));
        GlVoucherCostCenterBreakupResponseDto costCenterResponse = new GlVoucherCostCenterBreakupResponseDto();
        costCenterResponse.setCostBreakupList(List.of(CostCenterBreakupResponseDto.builder()
                .mainDetRowId(1L)
                .costDetRowId(21L)
                .costGroup("LOCATION")
                .costPoid("55")
                .amount(new BigDecimal("60"))
                .build()));

        when(billwiseBreakupService.loadBillwiseBreakup(any(), any(), anyString(), any())).thenReturn(billwiseResponse);
        when(costCenterBreakupService.loadCostCenterData(anyString(), any(), any(), any(), any())).thenReturn(costCenterResponse);
        when(glMasterRepository.findByGlPoid(anyLong())).thenAnswer(inv -> Optional.of(gl((Long) inv.getArgument(0))));
        when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenAnswer(inv -> lov((Long) inv.getArgument(0), (String) inv.getArgument(1)));

        PettyCashResponseDto response = service.updatePettyCash(2000L, request, "DOC123");

        assertNotNull(response);
        assertEquals(3, response.getPaymentDtls().size());
        verify(billwiseBreakupService).updateBillwiseBreakups(anyList(), eq(33L));
        verify(costCenterBreakupService).updateCostCenterBreakups(anyList(), eq(33L));
        verify(pettyCashPaymentVoucherCustomRepository).updateCostFF(any(), any(), any(), eq("FF-OLD"), eq(2000L), any());
    }

    @Test
    void updatePettyCash_ffJobsPath_coversChargeMergeAndNewProcedure() {
        GlPettyCashPaymentHdr existingHdr = savedHeader(3000L, "FF JOBS");
        when(glPettyCashPaymentHdrRepository.findByTransactionPoid(3000L)).thenReturn(Optional.of(existingHdr));
        doAnswer(inv -> {
            ((StringBuilder) inv.getArgument(5)).append("MTA RFQ");
            ((StringBuilder) inv.getArgument(6)).append("RFQ-OLD");
            return null;
        }).when(pettyCashPaymentVoucherCustomRepository).getOldJobReferences(any(), any(), any(), anyString(), anyString(), any(), any());

        GlPettyCashChargeDtl existing1 = chargeEntity(3000L, 1L, 61L, new BigDecimal("60"), "FF");
        GlPettyCashChargeDtl existing3 = chargeEntity(3000L, 3L, 62L, new BigDecimal("40"), "FF");
        existing1.setCreatedDate(LocalDateTime.now().minusMinutes(2));
        existing3.setCreatedDate(LocalDateTime.now().minusMinutes(2));
        when(glPettyCashChargeDtlRepository.findByTransactionPoid(3000L)).thenReturn(List.of(existing1, existing3));
        when(taxMasterRepository.findByTaxPoid(anyLong())).thenAnswer(inv -> Optional.of(tax((Long) inv.getArgument(0))));

        PettyCashUpdateRequestDto request = baseUpdateRequest("FF JOBS");
        request.setFfRef("FF-001");
        request.setGlPettyCashChargeDtlRequestDtos(List.of(
                        GlPettyCashChargeDtlRequestDto.builder().detRowId(1L).chargePoid(61L).chargeAmount(new BigDecimal("60")).checkAll("Y").actionType("isUpdated").build(),
                        GlPettyCashChargeDtlRequestDto.builder().detRowId(2L).chargePoid(63L).chargeAmount(new BigDecimal("40")).checkAll("Y").actionType("isCreated").build(),
                        GlPettyCashChargeDtlRequestDto.builder().detRowId(3L).chargePoid(62L).chargeAmount(new BigDecimal("0")).checkAll("Y").actionType("isDeleted").build()
                ));

        when(shipChargeRepository.findByChargePoid(anyLong())).thenAnswer(inv -> Optional.of(charge((Long) inv.getArgument(0))));

        PettyCashResponseDto response = service.updatePettyCash(3000L, request, "DOC123");

        assertNotNull(response);
        assertEquals(2, response.getChargeDtls().size());
        verify(pettyCashPaymentVoucherCustomRepository).updateRfqPurchasePrice(any(), any(), any(), eq("RFQ-OLD"), any());
        verify(pettyCashPaymentVoucherCustomRepository).updateCostFF(any(), any(), any(), eq("FF-001"), eq(3000L), any());
    }

    @Test
    void updatePettyCash_mtaRfqPath_coversItemMergeAndNewProcedure() {
        GlPettyCashPaymentHdr existingHdr = savedHeader(4000L, "MTA RFQ");
        when(glPettyCashPaymentHdrRepository.findByTransactionPoid(4000L)).thenReturn(Optional.of(existingHdr));
        doAnswer(inv -> {
            ((StringBuilder) inv.getArgument(5)).append("FF JOBS");
            ((StringBuilder) inv.getArgument(6)).append("FF-OLD");
            return null;
        }).when(pettyCashPaymentVoucherCustomRepository).getOldJobReferences(any(), any(), any(), anyString(), anyString(), any(), any());

        GLPettyCashItemDtl existing1 = itemEntity(4000L, 1L, 71L, 72L, new BigDecimal("20"));
        GLPettyCashItemDtl existing3 = itemEntity(4000L, 3L, 73L, 74L, new BigDecimal("80"));
        existing1.setCreatedDate(LocalDateTime.now().minusMinutes(2));
        existing3.setCreatedDate(LocalDateTime.now().minusMinutes(2));
        when(glPettyCashItemDtlRepository.findByTransactionPoid(4000L)).thenReturn(List.of(existing1, existing3));
        when(taxMasterRepository.findByTaxPoid(anyLong())).thenAnswer(inv -> Optional.of(tax((Long) inv.getArgument(0))));

        PettyCashUpdateRequestDto request = baseUpdateRequest("MTA RFQ");
        request.setSalesQtnRef("RFQ-001");
        request.setGlPettyCashItemDtlRequestDtos(List.of(
                        GlPettyCashItemDtlRequestDto.builder().detRowId(1L).stockPoid(71L).stockUnitPoid(72L)
                                .poQty(new BigDecimal("1")).price(new BigDecimal("20")).total(new BigDecimal("20"))
                                .checkAll("Y").actionType("isUpdated").build(),
                        GlPettyCashItemDtlRequestDto.builder().detRowId(2L).stockPoid(75L).stockUnitPoid(76L)
                                .poQty(new BigDecimal("2")).price(new BigDecimal("40")).total(new BigDecimal("80"))
                                .checkAll("Y").actionType("isCreated").build(),
                        GlPettyCashItemDtlRequestDto.builder().detRowId(3L).stockPoid(73L).stockUnitPoid(74L)
                                .poQty(BigDecimal.ZERO).price(BigDecimal.ZERO).total(BigDecimal.ZERO)
                                .checkAll("Y").actionType("isDeleted").build()
                ));

        when(stockMasterRepository.findByStockPoid(anyLong())).thenAnswer(inv -> Optional.of(stock((Long) inv.getArgument(0))));
        when(unitMasterRepository.findByUnitPoid(anyLong())).thenAnswer(inv -> Optional.of(unit((Long) inv.getArgument(0))));

        PettyCashResponseDto response = service.updatePettyCash(4000L, request, "DOC123");

        assertNotNull(response);
        assertEquals(2, response.getItemDtls().size());
        verify(pettyCashPaymentVoucherCustomRepository).updateCostFF(any(), any(), any(), eq("FF-OLD"), eq(4000L), any());
        verify(pettyCashPaymentVoucherCustomRepository).updateRfqPurchasePrice(any(), any(), any(), eq("RFQ-001"), any());
    }

    @Test
    void updatePettyCash_grnJobsPath_coversGrnMerge() {
        GlPettyCashPaymentHdr existingHdr = savedHeader(5000L, "GRN_JOBS");
        when(glPettyCashPaymentHdrRepository.findByTransactionPoid(5000L)).thenReturn(Optional.of(existingHdr));
        doAnswer(inv -> {
            ((StringBuilder) inv.getArgument(5)).append("GENERAL PO");
            ((StringBuilder) inv.getArgument(6)).append("PO-OLD");
            return null;
        }).when(pettyCashPaymentVoucherCustomRepository).getOldJobReferences(any(), any(), any(), anyString(), anyString(), any(), any());

        GlPettyCashPaymentGrnDtl existing1 = grnEntity(5000L, 1L, 81L, new BigDecimal("30"));
        GlPettyCashPaymentGrnDtl existing3 = grnEntity(5000L, 3L, 82L, new BigDecimal("20"));
        when(glPettyCashPaymentGrnDtlRepository.findByTransactionPoid(5000L)).thenReturn(List.of(existing1, existing3));

        PettyCashUpdateRequestDto request = baseUpdateRequest("GRN_JOBS");
        request.setPoRef("PO-001");
        request.setAmount(new BigDecimal("50"));
        request.setGlPettyCashGrnDtlRequestDtos(List.of(
                        GlPettyCashPaymentGrnDtlRequestDto.builder().detRowId(1L).grnPoid(81L).amount(new BigDecimal("30")).checkAll("Y").actionType("isUpdated").build(),
                        GlPettyCashPaymentGrnDtlRequestDto.builder().detRowId(2L).grnPoid(83L).amount(new BigDecimal("20")).checkAll("Y").actionType("isCreated").build(),
                        GlPettyCashPaymentGrnDtlRequestDto.builder().detRowId(3L).grnPoid(82L).amount(BigDecimal.ZERO).checkAll("Y").actionType("isDeleted").build()
                ));

        PettyCashResponseDto response = service.updatePettyCash(5000L, request, "DOC123");

        assertNotNull(response);
        verify(pettyCashPaymentVoucherCustomRepository).updatePurchaseOrderStatus(any(), any(), any(), eq("PO-OLD"), eq(5000L), any());
        verify(pettyCashPaymentVoucherCustomRepository).updateSalesGrnStatus(any(), any(), any(), eq("DOC123"), eq(5000L), any());
    }

    @Test
    void findById_delete_list_loads_print_andParams_coverRemainingPublicMethods() throws Exception {
        GlPettyCashPaymentHdr header = savedHeader(6000L, "GENERAL");
        header.setDocRef("DOC-6000");
        header.setPettyCashGlPoid(900L);
        header.setGrnSupplierPoid(903L);
        header.setSupplierGlPoid(901L);
        header.setCustomerGlPoid(902L);
        header.setAdvancePettyCashPoid(904L);
        header.setFfRef("905");
        header.setFdaRef("906");
        header.setSalesQtnRef("907");
        when(glPettyCashPaymentHdrRepository.findByTransactionPoid(6000L)).thenReturn(Optional.of(header));
        when(glPettyCashPaymentDtlRepository.findByTransactionPoid(6000L)).thenReturn(List.of(paymentEntity(6000L, 1L, "Dr", 10L, new BigDecimal("50"), 20L)));
        when(glPettyCashChargeDtlRepository.findByTransactionPoid(6000L)).thenReturn(List.of(chargeEntity(6000L, 1L, 31L, new BigDecimal("60"), "FF")));
        when(glPettyCashItemDtlRepository.findByTransactionPoid(6000L)).thenReturn(List.of(itemEntity(6000L, 1L, 41L, 42L, new BigDecimal("20"))));
        when(glMasterRepository.findByGlPoid(anyLong())).thenAnswer(inv -> Optional.of(gl((Long) inv.getArgument(0))));
        when(shipChargeRepository.findByChargePoid(anyLong())).thenAnswer(inv -> Optional.of(charge((Long) inv.getArgument(0))));
        when(stockMasterRepository.findByStockPoid(anyLong())).thenAnswer(inv -> Optional.of(stock((Long) inv.getArgument(0))));
        when(unitMasterRepository.findByUnitPoid(anyLong())).thenAnswer(inv -> Optional.of(unit((Long) inv.getArgument(0))));
        when(taxMasterRepository.findByTaxPoid(anyLong())).thenAnswer(inv -> Optional.of(tax((Long) inv.getArgument(0))));
        when(supplierMasterRepository.findBySupplierPoid(anyLong())).thenAnswer(inv -> supplier((Long) inv.getArgument(0)));
        when(advancePettyCashHdrRepository.findByTransactionPoid(anyLong())).thenAnswer(inv -> Optional.of(advanceHeader((Long) inv.getArgument(0))));
        when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenAnswer(inv -> lov((Long) inv.getArgument(0), (String) inv.getArgument(1)));

        GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse = new GlVoucherLoadBillwiseBreakupResponseDto();
        LoadBillwiseBreakupResponseDto loadRow = new LoadBillwiseBreakupResponseDto();
        loadRow.setMainDetRowId(1L);
        loadRow.setBillDetRowId(11L);
        loadRow.setBillRefType("PO");
        loadRow.setBillRef("B-1");
        loadRow.setBillDueDate(LocalDate.now());
        loadRow.setDrAmt(new BigDecimal("50"));
        billwiseResponse.setLoadBillwiseBreakupResponseDtoList(List.of(loadRow));
        when(billwiseBreakupService.loadBillwiseBreakup(any(), any(), anyString(), any())).thenReturn(billwiseResponse);

        GlVoucherCostCenterBreakupResponseDto costCenterResponse = new GlVoucherCostCenterBreakupResponseDto();
        costCenterResponse.setCostBreakupList(List.of(CostCenterBreakupResponseDto.builder()
                .mainDetRowId(1L)
                .costDetRowId(21L)
                .costGroup("LOCATION")
                .costPoid("55")
                .amount(new BigDecimal("50"))
                .build()));
        when(costCenterBreakupService.loadCostCenterData(anyString(), any(), any(), any(), any())).thenReturn(costCenterResponse);

        PettyCashResponseDto found = service.findById(6000L, "DOC123");
        assertNotNull(found);
        assertNotNull(found.getPaymentDtls());
        assertNotNull(found.getChargeDtls());
        assertNotNull(found.getItemDtls());
        assertNotNull(found.getPettyCashGlPoidDtl());
        assertNotNull(found.getGrnSupplierPoidDtl());
        assertNotNull(found.getAdvancePettyCashPoidDtl());

        DeleteReasonDto deleteReason = new DeleteReasonDto();
        deleteReason.setDeleteReason("Duplicate");
        service.deletePettyCashVoucher(6000L, "DOC123", "GENERAL", deleteReason);
        verify(documentDeleteService).deleteDocument(eq(6000L), eq("GL_PETTY_CASH_PAYMENT_HDR"), eq("TRANSACTION_POID"), eq(deleteReason), any());

        FilterRequestDto filters = new FilterRequestDto(null, null, null);
        RawSearchResult raw = new RawSearchResult(List.of(Map.of("transactionPoid", 6000L)), Map.of("transactionPoid", "Transaction Poid"), 1L);
        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveDateFilters(any(), eq("TRANSACTION_DATE"), any(), any())).thenReturn(Collections.emptyList());
        when(documentService.search(eq("DOC123"), anyList(), eq("AND"), any(), eq("N"), eq("REF_TYPE"), eq("TRANSACTION_POID"))).thenReturn(raw);
        Map<String, Object> listResult = service.listPettyCashVoucher("DOC123", filters, LocalDate.now().minusDays(1), LocalDate.now(), PageRequest.of(0, 10));
        assertNotNull(listResult);
        verify(documentService).search(eq("DOC123"), anyList(), eq("AND"), any(), eq("N"), eq("REF_TYPE"), eq("TRANSACTION_POID"));

        when(pettyCashPaymentVoucherCustomRepository.getRefTypeWhereClause(33L)).thenReturn("'GENERAL','FF JOBS','FDA JOBS'");
        assertEquals(List.of("GENERAL", "FF JOBS", "FDA JOBS"), service.getAllowedRefTypes(33L));

        when(globalParameterService.getParameterValue(eq("PETTY_CASH_DEFAULT_PAYING_TO"), anyString(), anyString(), anyString())).thenReturn("CASHIER");
        when(globalParameterService.getParameterValue(eq("DEFAULT_PETTY_CASH_REF_TYPE"), anyString(), anyString(), anyString())).thenReturn("GENERAL");
        when(globalParameterService.getParameterValue(eq("PETTY_CASH_GL_VAT_RELATED_FIELDS"), anyString(), anyString(), anyString())).thenReturn("TRUE");
        when(globalParameterService.getParameterValue(eq("ROUNDING_LIMIT"), anyString(), anyString(), anyString())).thenReturn("5");
        when(globalParameterService.getParameterValue(eq("PETTY_CASH_VAT_AMOUNT_LIMIT"), anyString(), anyString(), anyString())).thenReturn("7");
        when(globalParameterService.getParameterValue(eq("INPUT_TAX_VARIANCE_LIMIT"), anyString(), anyString(), anyString())).thenReturn("0.25");
        when(globalParameterService.getParameterValue(eq("MTA_PETTY_CASH_GL_CODE"), anyString(), anyString(), anyString())).thenReturn("MTA-GL");
        when(globalParameterService.getParameterValue(eq("PETTY_CASH_ADVANCE_LEDGER"), anyString(), anyString(), anyString())).thenReturn("123");
        when(globalParameterService.getParameterValue(eq("PETTY_CASH_LEDGER"), anyString(), anyString(), anyString())).thenReturn("456");
        when(globalParameterService.getParameterValue(eq("PETTY_CASH_ADV_REFND_APPR_SUBMN"), anyString(), anyString(), anyString())).thenReturn("Y");
        PettyCashGlobalParamsDto params = service.getPettyCashGlobalParams(900L);
        assertEquals("CASHIER", params.getDefaultPayingTo());
        assertEquals("GENERAL", params.getDefaultRefType());
        assertTrue(params.isVatRelatedFieldsVisible());

        byte[] pdf = service.print(6000L);
        assertArrayEquals(new byte[]{1, 2, 3}, pdf);
        verify(printService).fillReportToPdf(any(), anyMap(), any());
    }

    @Test
    void privateHelpers_coverDefaultsValidationsAndMapHelpers() {
        PettyCashCreateRequestDto request = baseCreateRequest("GENERAL");
        GlPettyCashPaymentHdr header = (GlPettyCashPaymentHdr) ReflectionTestUtils.invokeMethod(service, "buildPettyCashHeader", request);
        assertEquals("GENERAL", header.getRefType());
        assertEquals("Vendor", header.getPayingTo());

        GlPettyCashPaymentHdr defaultsHeader = GlPettyCashPaymentHdr.builder().build();
        when(globalParameterService.getParameterValue(eq("PETTY_CASH_DEFAULT_PAYING_TO"), anyString(), anyString(), any())).thenReturn("CASHIER");
        when(globalParameterService.getParameterValue(eq("DEFAULT_PETTY_CASH_REF_TYPE"), anyString(), anyString(), any())).thenReturn("GENERAL");
        ReflectionTestUtils.invokeMethod(service, "applyNewDocumentDefaults", defaultsHeader);
        assertEquals("CASHIER", defaultsHeader.getPayingTo());
        assertEquals("GENERAL", defaultsHeader.getRefType());

        when(globalParameterService.getParameterValue(eq("PETTY_CASH_VALIDATION_DAYS"), anyString(), anyString(), anyString())).thenReturn("1");
        when(globalParameterService.getParameterValue(eq("PETTY_CASH_BACK_DATE_VALIDATION_DAYS"), anyString(), anyString(), anyString())).thenReturn("1");
        assertThrows(ValidationException.class, () -> ReflectionTestUtils.invokeMethod(service, "validateTransactionDate", LocalDate.now().plusDays(3)));
        assertThrows(ValidationException.class, () -> ReflectionTestUtils.invokeMethod(service, "validateTransactionDate", LocalDate.now().minusDays(3)));

        PettyCashCreateRequestDto ffReq = baseCreateRequest("FF JOBS");
        ffReq.setFfRef(null);
        assertThrows(ValidationException.class, () -> ReflectionTestUtils.invokeMethod(service, "validateRefPoidRequired", ffReq));
        PettyCashCreateRequestDto fdaReq = baseCreateRequest("FDA JOBS");
        fdaReq.setFdaRef(null);
        assertThrows(ValidationException.class, () -> ReflectionTestUtils.invokeMethod(service, "validateRefPoidRequired", fdaReq));
        PettyCashCreateRequestDto mtaReq = baseCreateRequest("MTA RFQ");
        mtaReq.setSalesQtnRef(null);
        assertThrows(ValidationException.class, () -> ReflectionTestUtils.invokeMethod(service, "validateRefPoidRequired", mtaReq));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateRefPoidRequired", baseCreateRequest("GENERAL")));

        when(globalParameterService.getParameterValue(eq("ROUNDING_LIMIT"), anyString(), anyString(), anyString())).thenReturn("1");
        assertThrows(ValidationException.class, () -> ReflectionTestUtils.invokeMethod(service, "validateRoundingAmount", new BigDecimal("2")));

        PettyCashUpdateRequestDto supplierReq = baseUpdateRequest("SUPPLIER");
        supplierReq.setSupplierGlPoid(500L);
        supplierReq.setAmount(new BigDecimal("100"));
        supplierReq.setGlPettyCashPaymentDtlRequestDtos(List.of(
                        GlPettyCashPaymentDtlRequestDto.builder().type("Dr").glPoid(500L).drAmt(new BigDecimal("100")).build()
                ));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateSupplierCustomerGlMatch", supplierReq, "SUPPLIER"));
        PettyCashUpdateRequestDto supplierBadReq = baseUpdateRequest("SUPPLIER");
        supplierBadReq.setSupplierGlPoid(500L);
        supplierBadReq.setAmount(new BigDecimal("100"));
        supplierBadReq.setGlPettyCashPaymentDtlRequestDtos(List.of(
                        GlPettyCashPaymentDtlRequestDto.builder().type("Dr").glPoid(501L).drAmt(new BigDecimal("100")).build()
                ));
        assertThrows(ValidationException.class, () -> ReflectionTestUtils.invokeMethod(service, "validateSupplierCustomerGlMatch", supplierBadReq, "SUPPLIER"));

        PettyCashCreateRequestDto mapRequest = baseCreateRequest("FF JOBS");
        mapRequest.setGlPettyCashPaymentDtlRequestDtos(List.of(
                        GlPettyCashPaymentDtlRequestDto.builder().actionType("isCreated").glPoid(10L).chargePoid(31L).drAmt(new BigDecimal("5")).build(),
                        GlPettyCashPaymentDtlRequestDto.builder().actionType(null).glPoid(11L).chargePoid(32L).drAmt(new BigDecimal("6")).build(),
                        GlPettyCashPaymentDtlRequestDto.builder().actionType("isDeleted").glPoid(12L).build()
                ));
        List<GlPettyCashPaymentDtl> paymentEntities = (List<GlPettyCashPaymentDtl>) ReflectionTestUtils.invokeMethod(service, "mapPaymentDtls", mapRequest, 7000L);
        assertEquals(2, paymentEntities.size());
        assertNotNull(paymentEntities.get(0).getChargeMaster());
        assertEquals(31L, paymentEntities.get(0).getChargeMaster().getChargePoid());

        List<PendingBillwiseBreakupDto> pending = (List<PendingBillwiseBreakupDto>) ReflectionTestUtils.invokeMethod(service, "mapToPendingDto", List.of(
                pendingRow("B-1", new BigDecimal("10"))
        ));
        assertEquals(1, pending.size());
        assertEquals("B-1", pending.get(0).getBillRef());

        BigDecimal drAmt = (BigDecimal) ReflectionTestUtils.invokeMethod(service, "resolveBillwiseDrAmt",
                BillwiseBreakupPopupRequestDto.builder().type("DR").amount(new BigDecimal("9")).build());
        BigDecimal crAmt = (BigDecimal) ReflectionTestUtils.invokeMethod(service, "resolveBillwiseCrAmt",
                BillwiseBreakupPopupRequestDto.builder().type("CR").amount(new BigDecimal("9")).build());
        assertEquals(new BigDecimal("9"), drAmt);
        assertEquals(new BigDecimal("9"), crAmt);
    }

    private CustomAuthDetails currentUser() {
        CustomAuthDetails user = new CustomAuthDetails();
        user.setUserId("USR-1");
        user.setUserName("Unit Test");
        user.setUserPoid(33L);
        user.setGroupPoid(11L);
        user.setCompanyPoid(22L);
        user.setDocumentId("DOC123");
        user.setTimeZoneCode("Asia/Kolkata");
        return user;
    }

    private PettyCashCreateRequestDto baseCreateRequest(String refType) {
        return PettyCashCreateRequestDto.builder()
                .transactionDate(null)
                .currencyCode("INR")
                .currencyRate(new BigDecimal("1.000"))
                .pettyCashGlPoid(900L)
                .balance(new BigDecimal("500"))
                .amount(new BigDecimal("100"))
                .payingTo("Vendor")
                .narration("Narration")
                .advance("N")
                .refType(refType)
                .fdaRef("906")
                .ffRef("905")
                .settledDate(LocalDate.now())
                .remarks("Remarks")
                .status("OPEN")
                .grandTotal(new BigDecimal("100"))
                .mtaRef("MTA-1")
                .multiCompany("N")
                .poRef("904")
                .salesQtnRef("907")
                .crTotal(new BigDecimal("0"))
                .drTotal(new BigDecimal("0"))
                .roundingAmount(BigDecimal.ZERO)
                .grnSupplierPoid(903L)
                .supplierGlPoid(901L)
                .customerGlPoid(902L)
                .advancePettyCashPoid(904L)
                .advanceStatus("OPEN")
                .advanceAmount(new BigDecimal("10"))
                .companyDivPoid(44L)
                .docId("DOC123")
                .build();
    }

    private PettyCashUpdateRequestDto baseUpdateRequest(String refType) {
        return PettyCashUpdateRequestDto.builder()
                .transactionDate(null)
                .currencyCode("INR")
                .currencyRate(new BigDecimal("1.000"))
                .pettyCashGlPoid(900L)
                .balance(new BigDecimal("500"))
                .amount(new BigDecimal("100"))
                .payingTo("Vendor")
                .narration("Narration")
                .advance("N")
                .refType(refType)
                .fdaRef("906")
                .ffRef("905")
                .settledDate(LocalDate.now())
                .remarks("Remarks")
                .status("OPEN")
                .grandTotal(new BigDecimal("100"))
                .mtaRef("MTA-1")
                .multiCompany("N")
                .poRef("PO-001")
                .salesQtnRef("907")
                .crTotal(new BigDecimal("0"))
                .drTotal(new BigDecimal("0"))
                .roundingAmount(BigDecimal.ZERO)
                .grnSupplierPoid(903L)
                .supplierGlPoid(901L)
                .customerGlPoid(902L)
                .advancePettyCashPoid(904L)
                .advanceStatus("OPEN")
                .advanceAmount(new BigDecimal("10"))
                .companyDivPoid(44L)
                .docId("DOC123")
                .build();
    }

    private GlPettyCashPaymentHdr savedHeader(Long txPoid, String refType) {
        return GlPettyCashPaymentHdr.builder()
                .transactionPoid(txPoid)
                .docRef("DOC-" + txPoid)
                .transactionDate(LocalDate.now())
                .groupPoid(11L)
                .companyPoid(22L)
                .currencyCode("INR")
                .currencyRate(new BigDecimal("1.000"))
                .pettyCashGlPoid(900L)
                .balance(new BigDecimal("500"))
                .amount(new BigDecimal("100"))
                .payingTo("Vendor")
                .narration("Narration")
                .advance("N")
                .refType(refType)
                .fdaRef("906")
                .ffRef("905")
                .settledDate(LocalDate.now())
                .remarks("Remarks")
                .settledTotal(new BigDecimal("0"))
                .status("OPEN")
                .grandTotal(new BigDecimal("100"))
                .mtaRef("MTA-1")
                .multiCompany("N")
                .poRef("PO-001")
                .salesQtnRef("907")
                .crTotal(new BigDecimal("0"))
                .drTotal(new BigDecimal("0"))
                .roundingAmount(BigDecimal.ZERO)
                .grnSupplierPoid(903L)
                .supplierGlPoid(901L)
                .customerGlPoid(902L)
                .advancePettyCashPoid(904L)
                .advanceStatus("OPEN")
                .advanceAmount(new BigDecimal("10"))
                .companyDivPoid(44L)
                .build();
    }

    private GlPettyCashPaymentDtl paymentEntity(Long tx, Long detRowId, String type, Long glPoid, BigDecimal amount, Long taxPoid) {
        GlPettyCashPaymentDtl entity = GlPettyCashPaymentDtl.builder()
                .transactionPoid(tx)
                .detRowId(detRowId)
                .type(type)
                .companyPoid(77L)
                .drAmt("Dr".equalsIgnoreCase(type) ? amount : BigDecimal.ZERO)
                .crAmt("Cr".equalsIgnoreCase(type) ? amount : BigDecimal.ZERO)
                .remarks("payment-" + detRowId)
                .vatAmount(BigDecimal.ZERO)
                .totalAmount(amount)
                .taxPoid(taxPoid)
                .build();
        entity.setGlMaster(gl(glPoid));
        entity.setCreatedDate(LocalDateTime.now());
        return entity;
    }

    private GlPettyCashChargeDtl chargeEntity(Long tx, Long detRowId, Long chargePoid, BigDecimal amount, String chargeFrom) {
        GlPettyCashChargeDtl entity = GlPettyCashChargeDtl.builder()
                .transactionPoid(tx)
                .detRowId(detRowId)
                .chargePoid(chargePoid)
                .chargeAmount(amount)
                .description("Charge " + detRowId)
                .remarks("charge-" + detRowId)
                .refDocId("REF-" + detRowId)
                .refDocPoid(700L + detRowId)
                .checkAll("Y")
                .pdaAmount(BigDecimal.ZERO)
                .ffAmount(BigDecimal.ZERO)
                .chargeFrom(chargeFrom)
                .taxPoid(20L)
                .build();
        entity.setCreatedDate(LocalDateTime.now());
        return entity;
    }

    private GLPettyCashItemDtl itemEntity(Long tx, Long detRowId, Long stockPoid, Long stockUnitPoid, BigDecimal amount) {
        GLPettyCashItemDtl entity = GLPettyCashItemDtl.builder()
                .transactionPoid(tx)
                .detRowId(detRowId)
                .stockPoid(stockPoid)
                .stockUnitPoid(stockUnitPoid)
                .poQty(BigDecimal.ONE)
                .dnQty(BigDecimal.ZERO)
                .qtyReceived(BigDecimal.ONE)
                .price(amount)
                .discount(BigDecimal.ZERO)
                .total(amount)
                .remarks("item-" + detRowId)
                .refDocId("REF-" + detRowId)
                .refDocPoid(710L + detRowId)
                .checkAll("Y")
                .refDetRowId(1L)
                .taxPoid(30L)
                .build();
        entity.setCreatedDate(LocalDateTime.now());
        return entity;
    }

    private GlPettyCashPaymentGrnDtl grnEntity(Long tx, Long detRowId, Long grnPoid, BigDecimal amount) {
        GlPettyCashPaymentGrnDtl entity = GlPettyCashPaymentGrnDtl.builder()
                .transactionPoid(tx)
                .detRowId(detRowId)
                .grnPoid(grnPoid)
                .checkAll("Y")
                .amount(amount)
                .remarks("grn-" + detRowId)
                .refDocId("REF-" + detRowId)
                .refDocPoid(720L + detRowId)
                .refDetRowId(1L)
                .build();
        return entity;
    }

    private AdvancePettyCashHdr advanceHeader(Long txPoid) {
        AdvancePettyCashHdr hdr = new AdvancePettyCashHdr();
        hdr.setTransactionPoid(txPoid);
        hdr.setDocRef("ADV-" + txPoid);
        hdr.setGroupPoid(11L);
        return hdr;
    }

    private GLMaster gl(Long poid) {
        return GLMaster.builder()
                .glPoid(poid)
                .glCode("GL-" + poid)
                .glDescription("GL Desc " + poid)
                .groupPoid(11L)
                .glDescription2("GL Desc 2 " + poid)
                .seqno(poid.intValue())
                .build();
    }

    private ShipChargeEntity charge(Long poid) {
        ShipChargeEntity entity = new ShipChargeEntity();
        entity.setChargePoid(poid);
        entity.setChargeCode("CH-" + poid);
        entity.setChargeName("Charge " + poid);
        entity.setGroupPoid(11L);
        entity.setChargeName2("Charge 2 " + poid);
        entity.setSeqNo(poid.intValue());
        return entity;
    }

    private StockMasterEntity stock(Long poid) {
        return StockMasterEntity.builder()
                .stockPoid(poid)
                .stockCode("ST-" + poid)
                .stockName("Stock " + poid)
                .groupPoid(11L)
                .stockDescription("Stock Desc " + poid)
                .seqNo(poid.intValue())
                .build();
    }

    private UnitMaster unit(Long poid) {
        UnitMaster entity = new UnitMaster();
        entity.setUnitPoid(poid);
        entity.setUnitCode("U-" + poid);
        entity.setUnitName("Unit " + poid);
        entity.setGroupPoid(11L);
        entity.setUnitName2("Unit 2 " + poid);
        entity.setSeqNo(poid.intValue());
        return entity;
    }

    private TaxMaster tax(Long poid) {
        return TaxMaster.builder()
                .taxPoid(poid)
                .taxCode("T-" + poid)
                .taxName("Tax " + poid)
                .groupPoid(11L)
                .taxName2("Tax 2 " + poid)
                .seqNo(poid.intValue())
                .build();
    }

    private SupplierMasterEntity supplier(Long poid) {
        SupplierMasterEntity entity = new SupplierMasterEntity();
        entity.setSupplierPoid(poid);
        entity.setSupplierCode("S-" + poid);
        entity.setSupplierName("Supplier " + poid);
        entity.setGroupPoid(11L);
        entity.setSupplierName2("Supplier 2 " + poid);
        entity.setSeqNo(poid);
        return entity;
    }

    private LovGetListDto lov(Long poid, String name) {
        LovGetListDto dto = new LovGetListDto();
        dto.setPoid(poid);
        dto.setCode(name + "-CODE");
        dto.setLabel(name + "-LABEL");
        dto.setValue(11L);
        dto.setDescription(name + "-DESC");
        dto.setSeqNo(poid.intValue());
        return dto;
    }

    private LovGetListDto lovByCode(String code, String name) {
        LovGetListDto dto = new LovGetListDto();
        dto.setPoid(1L);
        dto.setCode(code);
        dto.setLabel(name + "-LABEL");
        dto.setValue(11L);
        dto.setDescription(name + "-DESC");
        dto.setSeqNo(1);
        return dto;
    }

    private PettyRefTypeResponse<PettyGlBalanceDto> balanceResponse(BigDecimal balance) {
        return PettyRefTypeResponse.<PettyGlBalanceDto>builder()
                .responseList(List.of(PettyGlBalanceDto.builder().balance(balance).build()))
                .build();
    }

    private BillwiseBreakupPopupRequestDto billwiseDrivenRequest(Long detRowId, String actionType, BigDecimal amount) {
        return BillwiseBreakupPopupRequestDto.builder()
                .billDetRowId(detRowId)
                .billRefType("PO")
                .billRef("B-" + detRowId)
                .billDueDate(LocalDate.now())
                .type("DR")
                .amount(amount)
                .billRemarks("bill-" + detRowId)
                .actionType(actionType)
                .build();
    }

    private ShowPendingBillwiseBreakupResponseDto pendingRow(String billRef, BigDecimal balance) {
        ShowPendingBillwiseBreakupResponseDto row = new ShowPendingBillwiseBreakupResponseDto();
        row.setBillRef(billRef);
        row.setBillDueDate(LocalDate.now());
        row.setRemarks("pending");
        row.setBalance(balance);
        return row;
    }
}
