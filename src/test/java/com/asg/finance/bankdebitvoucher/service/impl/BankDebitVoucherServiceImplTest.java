package com.asg.finance.bankdebitvoucher.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.*;
import com.asg.finance.client.GlobalTermsServiceClient;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GlBankDebitHdr;
import com.asg.finance.repository.*;
import com.asg.finance.repository.master.ShipChargeRepository;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import com.asg.finance.service.GlPostingService;
import com.asg.finance.service.impl.BankDebitVoucherServiceImpl;
import com.asg.finance.validator.BankDebitVoucherValidator;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankDebitVoucherServiceImplTest {

    // ─── Repositories ────────────────────────────────────────────────────────

    @Mock private GlBankDebitHdrRepository headerRepository;
    @Mock private GlBankDebitDtlGlRepository paymentGlRepository;
    @Mock private GlBankDebitChargeDtlRepository chargeDetailRepository;
    @Mock private GlBankDebitItemDtlRepository itemDetailRepository;
    @Mock private GlBankRepository glBankRepository;
    @Mock private TaxMasterRepository taxMasterRepository;
    @Mock private GLMasterRepository glMasterRepository;
    @Mock private ShipChargeRepository shipChargeRepository;
    @Mock private BankPurposeCodeMasterRepository bankPurposeCodeMasterRepository;
    @Mock private BankDebitVoucherCustomRepository bankDebitVoucherCustomRepository;
    @Mock private BankPaymentVoucherSpRepository bankPaymentVoucherSpRepository;
    @Mock private GlobalTermsCustomChangesRepository globalTermsCustomChangesRepository;
    @Mock private GlChequeCashConvertRepository glChequeCashConvertRepository;
    @Mock private BankDebitVoucherProcedureRepository bankDebitVoucherProcedureRepository;

    // ─── Services ─────────────────────────────────────────────────────────────

    @Mock private DocumentSearchService documentService;
    @Mock private LovDataService lovService;
    @Mock private BankDebitVoucherValidator validator;
    @Mock private BillwiseBreakupService billwiseBreakupService;
    @Mock private CostCenterBreakupService costCenterBreakupService;
    @Mock private GlobalTermsServiceClient globalTermsServiceClient;
    @Mock private PrintService printService;
    @Mock private LoggingService loggingService;
    @Mock private DocumentDeleteService documentDeleteService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ApprovalService approvalService;
    @Mock private GlPostingService glPostingService;
    @Mock private ApplicationContext applicationContext;
    @Mock private GlobalParameterService globalParameterService;
    @Mock private EntityManager entityManager;
    @Mock private DataSource dataSource;

    @InjectMocks
    private BankDebitVoucherServiceImpl service;

    private GlBankDebitHdr header;

    @BeforeEach
    void setUp() {
        header = new GlBankDebitHdr();
        header.setTransactionPoid(1L);
        header.setGroupPoid(1L);
        header.setCompanyPoid(1L);
        header.setDocRef("BDV-001");
        header.setBankPoid(1008L);
        header.setAmount(BigDecimal.valueOf(1000));
        header.setPayingType("4");
        header.setRefType("GENERAL");
        header.setTransactionDate(LocalDateTime.now());
        header.setDeleted("N");
    }

    // ─── getBankDebitVoucher ──────────────────────────────────────────────────

    @Test
    void getBankDebitVoucher_NotFound_ThrowsException() {
        when(headerRepository.findByTransactionPoidAndNotDeleted(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBankDebitVoucher(99L, "DOC123"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── softDeleteBankDebitVoucher ───────────────────────────────────────────

    @Test
    void softDeleteBankDebitVoucher_Success() {
        header.setTransactionDate(LocalDateTime.of(2025, 1, 20, 0, 0));
        when(headerRepository.findByTransactionPoidAndNotDeleted(1L)).thenReturn(Optional.of(header));
        doNothing().when(validator).validateVoucherStatusInNewTransaction(header);

        DeleteReasonDto deleteDto = new DeleteReasonDto();
        deleteDto.setDeleteReason("No longer required");

        service.softDeleteBankDebitVoucher(1L, deleteDto);

        verify(documentDeleteService).deleteDocument(
                eq(1L),
                eq("GL_BANK_DEBIT_HDR"),
                eq("TRANSACTION_POID"),
                eq(deleteDto),
                eq(LocalDate.of(2025, 1, 20))
        );
    }

    @Test
    void softDeleteBankDebitVoucher_NotFound_ThrowsException() {
        when(headerRepository.findByTransactionPoidAndNotDeleted(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDeleteBankDebitVoucher(99L, null))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(documentDeleteService, never()).deleteDocument(any(), any(), any(), any(), any());
    }

    // ─── listBankDebitVouchers ────────────────────────────────────────────────

    @Test
    void listBankDebitVouchers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Map<String, Object>> records = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("transactionPoid", "1");
        records.add(row);
        Map<String, String> displayFields = new HashMap<>();
        displayFields.put("transactionPoid", "Transaction ID");
        RawSearchResult rawResult = new RawSearchResult(records, displayFields, 1L);

        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveDateFilters(any(), eq("TRANSACTION_DATE"), isNull(), isNull()))
                .thenReturn(new ArrayList<>());
        when(documentService.search(eq("DOC123"), anyList(), eq("AND"), eq(pageable), eq("N"),
                eq("LONG_NARRATION"), eq("TRANSACTION_POID")))
                .thenReturn(rawResult);

        Map<String, Object> result = service.listBankDebitVouchers("DOC123", null, null, null, pageable);

        assertNotNull(result);
        verify(documentService).search(eq("DOC123"), anyList(), eq("AND"), eq(pageable), eq("N"),
                eq("LONG_NARRATION"), eq("TRANSACTION_POID"));
    }

    @Test
    void listBankDebitVouchers_WithDateRange_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);
        RawSearchResult rawResult = new RawSearchResult(new ArrayList<>(), new HashMap<>(), 0L);

        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveDateFilters(any(), eq("TRANSACTION_DATE"), eq(from), eq(to)))
                .thenReturn(new ArrayList<>());
        when(documentService.search(anyString(), anyList(), anyString(), any(Pageable.class), anyString(),
                anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.listBankDebitVouchers("DOC123", null, from, to, pageable);

        assertNotNull(result);
    }

    // ─── getBankBalance ───────────────────────────────────────────────────────

    @Test
    void getBankBalance_DelegatesToRepository() {
        LocalDate docDate = LocalDate.of(2025, 1, 20);
        BigDecimal expected = BigDecimal.valueOf(50000);

        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getGroupPoid).thenReturn(1L);
            muc.when(UserContext::getUserPoid).thenReturn(10L);
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(bankDebitVoucherCustomRepository.procGetBankBalance(1L, 10L, 1L, "DOC123", docDate, 1008L))
                    .thenReturn(expected);

            BigDecimal result = service.getBankBalance(1008L, "DOC123", docDate);

            assertThat(result).isEqualByComparingTo(expected);
            verify(bankDebitVoucherCustomRepository).procGetBankBalance(1L, 10L, 1L, "DOC123", docDate, 1008L);
        }
    }

    // ─── getBeneficiaryName ───────────────────────────────────────────────────

    @Test
    void getBeneficiaryName_DelegatesToRepository() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getGroupPoid).thenReturn(1L);
            muc.when(UserContext::getUserPoid).thenReturn(10L);
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(bankDebitVoucherCustomRepository.procGetBeneficiaryName(1L, 10L, 1L, "DOC123", 42L))
                    .thenReturn("John Doe");

            String result = service.getBeneficiaryName(42L, "DOC123");

            assertThat(result).isEqualTo("John Doe");
        }
    }

    // ─── getMtaRef ────────────────────────────────────────────────────────────

    @Test
    void getMtaRef_DelegatesToRepository() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getGroupPoid).thenReturn(1L);
            muc.when(UserContext::getUserPoid).thenReturn(10L);
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(bankDebitVoucherCustomRepository.procSetMtaRef(1L, 10L, 1L, 5L)).thenReturn("MTA-2025-001");

            String result = service.getMtaRef(5L);

            assertThat(result).isEqualTo("MTA-2025-001");
            verify(bankDebitVoucherCustomRepository).procSetMtaRef(1L, 10L, 1L, 5L);
        }
    }

    // ─── loadMTAItems ─────────────────────────────────────────────────────────

    @Test
    void loadMTAItems_DelegatesToRepository() {
        ItemDetailDto item = new ItemDetailDto();
        item.setDetRowId(1L);

        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getGroupPoid).thenReturn(1L);
            muc.when(UserContext::getUserPoid).thenReturn(10L);
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(bankDebitVoucherCustomRepository.procLoadMTAItems(1L, 10L, 1L, 5L))
                    .thenReturn(List.of(item));

            List<ItemDetailDto> result = service.loadMTAItems(5L);

            assertThat(result).hasSize(1);
            verify(bankDebitVoucherCustomRepository).procLoadMTAItems(1L, 10L, 1L, 5L);
        }
    }

    // ─── loadFFCharges ────────────────────────────────────────────────────────

    @Test
    void loadFFCharges_DelegatesToRepository() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getGroupPoid).thenReturn(1L);
            muc.when(UserContext::getUserPoid).thenReturn(10L);
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(bankDebitVoucherCustomRepository.procLoadFFCharges(1L, 10L, 1L, 10L))
                    .thenReturn(Collections.emptyList());

            List<ChargeFFDto> result = service.loadFFCharges(10L);

            assertThat(result).isEmpty();
            verify(bankDebitVoucherCustomRepository).procLoadFFCharges(1L, 10L, 1L, 10L);
        }
    }

    // ─── loadFDACharges ───────────────────────────────────────────────────────

    @Test
    void loadFDACharges_DelegatesToRepository() {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getGroupPoid).thenReturn(1L);
            muc.when(UserContext::getUserPoid).thenReturn(10L);
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(bankDebitVoucherCustomRepository.procLoadFDACharges(1L, 10L, 1L, 20L))
                    .thenReturn(Collections.emptyList());

            List<ChargeFDADto> result = service.loadFDACharges(20L);

            assertThat(result).isEmpty();
            verify(bankDebitVoucherCustomRepository).procLoadFDACharges(1L, 10L, 1L, 20L);
        }
    }

    // ─── validatePayGLAndBeneficiary ──────────────────────────────────────────

    @Test
    void validatePayGLAndBeneficiary_DelegatesToRepository() {
        PayGLValidationRequest req = new PayGLValidationRequest();
        req.setTransactionPoid(1L);
        req.setPayingType("4");
        req.setRefType("GENERAL");
        req.setPayGlPoid(null);
        req.setPayingTo(null);
        req.setBankPoid(1008L);

        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getGroupPoid).thenReturn(1L);
            muc.when(UserContext::getUserPoid).thenReturn(10L);
            muc.when(UserContext::getCompanyPoid).thenReturn(1L);

            doNothing().when(bankDebitVoucherCustomRepository).procGlBankPayGlBenVal(
                    eq(1L), eq(10L), eq(1L), eq("400-111"),
                    eq(1L), eq("4"), eq("GENERAL"), isNull(), isNull(), eq(1008L));

            service.validatePayGLAndBeneficiary(req);

            verify(bankDebitVoucherCustomRepository).procGlBankPayGlBenVal(
                    eq(1L), eq(10L), eq(1L), eq("400-111"),
                    eq(1L), eq("4"), eq("GENERAL"), isNull(), isNull(), eq(1008L));
        }
    }

    // ─── revertReconciliation ─────────────────────────────────────────────────

    @Test
    void revertReconciliation_Success() {
        when(headerRepository.findById(1L)).thenReturn(Optional.of(header));

        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getUserPoid).thenReturn(10L);

            when(bankPaymentVoucherSpRepository.revertReconciliation(
                    eq(1L), eq(1L), eq(10L), eq("BDV-001"), eq("1"), eq("Y")))
                    .thenReturn("SUCCESS");

            service.revertReconciliation(1L, "Test comment");

            verify(bankPaymentVoucherSpRepository).revertReconciliation(
                    eq(1L), eq(1L), eq(10L), eq("BDV-001"), eq("1"), eq("Y"));
        }
    }

    @Test
    void revertReconciliation_NotFound_ThrowsException() {
        when(headerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revertReconciliation(99L, null))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(bankPaymentVoucherSpRepository, never())
                .revertReconciliation(any(), any(), any(), any(), any(), any());
    }

    // ─── generateDefaultGlRows ────────────────────────────────────────────────

    @Test
    void generateDefaultGlRows_NoBankPoid_ReturnsEmpty() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setBankPoid(null);

        List<PaymentGlDetails> result = service.generateDefaultGlRows(req);

        assertThat(result).isEmpty();
        verifyNoInteractions(bankDebitVoucherCustomRepository);
    }

    @Test
    void generateDefaultGlRows_NoBankGlPoid_ReturnsEmpty() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setBankPoid(1008L);
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1008L)).thenReturn(null);

        List<PaymentGlDetails> result = service.generateDefaultGlRows(req);

        assertThat(result).isEmpty();
    }

    @Test
    void generateDefaultGlRows_PayingType4_ReturnsDrCrRows() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setBankPoid(1008L);
        req.setPayingType("4");
        req.setAmount(BigDecimal.valueOf(1000));

        when(bankDebitVoucherCustomRepository.getBankGlPoid(1008L)).thenReturn(999L);
        when(globalParameterService.getParameterValue("BANK INTEREST CHARGES SHIPPING", "GROUP", "1", null))
                .thenReturn("888");

        List<PaymentGlDetails> rows = service.generateDefaultGlRows(req);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getType()).isEqualTo("DR");
        assertThat(rows.get(0).getGlPoid()).isEqualTo(888L);
        assertThat(rows.get(1).getType()).isEqualTo("CR");
        assertThat(rows.get(1).getGlPoid()).isEqualTo(999L);
    }

    @Test
    void generateDefaultGlRows_PayingType4_NoBankChargesParam_ReturnsEmpty() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setBankPoid(1008L);
        req.setPayingType("4");
        req.setAmount(BigDecimal.valueOf(1000));

        when(bankDebitVoucherCustomRepository.getBankGlPoid(1008L)).thenReturn(999L);
        when(globalParameterService.getParameterValue("BANK INTEREST CHARGES SHIPPING", "GROUP", "1", null))
                .thenReturn(null);

        List<PaymentGlDetails> rows = service.generateDefaultGlRows(req);

        assertThat(rows).isEmpty();
    }

    @Test
    void generateDefaultGlRows_GeneralWithPayGl_ReturnsDrCr() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setBankPoid(1008L);
        req.setPayingType("1");
        req.setRefType("GENERAL");
        req.setPayGlPoid(777L);
        req.setAmount(BigDecimal.valueOf(1000));

        when(bankDebitVoucherCustomRepository.getBankGlPoid(1008L)).thenReturn(999L);

        List<PaymentGlDetails> rows = service.generateDefaultGlRows(req);

        // DR(PayGL=777) + CR(BankGL=999) — no bank charges, no gain/loss
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getType()).isEqualTo("DR");
        assertThat(rows.get(0).getGlPoid()).isEqualTo(777L);
        assertThat(rows.get(0).getDrAmt()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(rows.get(1).getType()).isEqualTo("CR");
        assertThat(rows.get(1).getGlPoid()).isEqualTo(999L);
        assertThat(rows.get(1).getCrAmt()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void generateDefaultGlRows_GeneralWithBankCharges_AddsChargeRows() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setBankPoid(1008L);
        req.setPayingType("1");
        req.setRefType("GENERAL");
        req.setPayGlPoid(777L);
        req.setAmount(BigDecimal.valueOf(1000));
        req.setBankCharges(BigDecimal.valueOf(50));
        req.setTaxAmount(BigDecimal.valueOf(5));

        when(bankDebitVoucherCustomRepository.getBankGlPoid(1008L)).thenReturn(999L);
        when(globalParameterService.getParameterValue("BANK INTEREST CHARGES SHIPPING", "GROUP", "1", null))
                .thenReturn("888");

        List<PaymentGlDetails> rows = service.generateDefaultGlRows(req);

        // DR(PayGL) + CR(BankGL) + DR(ChargesGL) + CR(BankGL for charges)
        assertThat(rows).hasSize(4);
        assertThat(rows.get(2).getType()).isEqualTo("DR");
        assertThat(rows.get(2).getGlPoid()).isEqualTo(888L);
        assertThat(rows.get(3).getType()).isEqualTo("CR");
        // charge + tax = 55
        assertThat(rows.get(3).getCrAmt()).isEqualByComparingTo(BigDecimal.valueOf(55));
    }

    @Test
    void generateDefaultGlRows_CustomRefType_ReturnsOnlyCrRow() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setBankPoid(1008L);
        req.setPayingType("1");
        req.setRefType("CUSTOM");
        req.setAmount(BigDecimal.valueOf(1000));

        when(bankDebitVoucherCustomRepository.getBankGlPoid(1008L)).thenReturn(999L);

        List<PaymentGlDetails> rows = service.generateDefaultGlRows(req);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getType()).isEqualTo("CR");
        assertThat(rows.get(0).getGlPoid()).isEqualTo(999L);
    }

    @Test
    void generateDefaultGlRows_GeneralNoPayGl_ReturnsEmpty() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setBankPoid(1008L);
        req.setPayingType("1");
        req.setRefType("GENERAL");
        req.setPayGlPoid(null); // no payGL → no rows generated for GENERAL
        req.setAmount(BigDecimal.valueOf(1000));

        when(bankDebitVoucherCustomRepository.getBankGlPoid(1008L)).thenReturn(999L);

        List<PaymentGlDetails> rows = service.generateDefaultGlRows(req);

        assertThat(rows).isEmpty();
    }

    @Test
    void generateDefaultGlRows_GainLoss_CrType_AdjustsPayGlAndAddsGainLossRow() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setBankPoid(1008L);
        req.setPayingType("1");
        req.setRefType("GENERAL");
        req.setPayGlPoid(777L);
        req.setAmount(BigDecimal.valueOf(1000));
        req.setGainLoss(BigDecimal.valueOf(20));
        req.setGainLossType("CR"); // gain: payGL DR = amount + gainLoss = 1020

        when(bankDebitVoucherCustomRepository.getBankGlPoid(1008L)).thenReturn(999L);
        when(globalParameterService.getParameterValue("EXCHANGE GAIN LOSS ACCT", "GROUP", "1", null))
                .thenReturn("666");

        List<PaymentGlDetails> rows = service.generateDefaultGlRows(req);

        // DR(PayGL=777, 1020) + CR(BankGL=999, 1000) + DR(GainLossGL=666, 20)
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).getDrAmt()).isEqualByComparingTo(BigDecimal.valueOf(1020));
        assertThat(rows.get(2).getGlPoid()).isEqualTo(666L);
        assertThat(rows.get(2).getType()).isEqualTo("DR"); // CR gainLossType → DR row for gain/loss
    }
}
