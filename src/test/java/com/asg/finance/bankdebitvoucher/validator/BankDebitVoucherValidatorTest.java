package com.asg.finance.bankdebitvoucher.validator;

import com.asg.common.lib.client.ParameterServiceClient;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.*;
import com.asg.finance.repository.BankDebitVoucherCustomRepository;
import com.asg.finance.repository.BankPaymentVoucherSpRepository;
import com.asg.finance.validator.BankDebitVoucherValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BankDebitVoucherValidatorTest {

    @Mock
    private ParameterServiceClient parameterServiceClient;

    @Mock
    private BankDebitVoucherCustomRepository bankDebitVoucherCustomRepository;

    @Mock
    private BankPaymentVoucherSpRepository spRepository;

    @InjectMocks
    private BankDebitVoucherValidator validator;

    private MockedStatic<UserContext> mockedUserContext;

    private static final Long BANK_GL_POID = 999L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(1000);
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @BeforeEach
    void setUpUserContext() {
        mockedUserContext = mockStatic(UserContext.class);
        mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
        mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
        mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
    }

    @AfterEach
    void tearDownUserContext() {
        mockedUserContext.close();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Minimal valid request: payingType=4 + GENERAL (simplest passing case). */
    private BankDebitVoucherRequest validGeneralType4Request() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setBankPoid(1L);
        req.setPayingType("4");
        req.setRefType("GENERAL");
        req.setCurrencyCode("BHD");
        req.setAmount(AMOUNT);
        req.setCurrencyAmt(AMOUNT);
        req.setLongNarration("Test narration");
        req.setTtDate(LocalDateTime.now());
        req.setPaymentGlDetails(List.of(
                glRow("DR", 100L, AMOUNT, ZERO),
                glRow("CR", BANK_GL_POID, ZERO, AMOUNT)
        ));
        return req;
    }

    /** Minimal valid request: payingType=1 (TT) + GENERAL. */
    private BankDebitVoucherRequest validGeneralType1Request() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setBankPoid(1L);
        req.setPayingType("1");
        req.setRefType("GENERAL");
        req.setCurrencyCode("BHD");
        req.setAmount(AMOUNT);
        req.setCurrencyAmt(AMOUNT);
        req.setLongNarration("Test narration");
        req.setTtDate(LocalDateTime.now().plusDays(1)); // future for new TT
        req.setPayingToName("Beneficiary Name");
        req.setPayingTo("BENE01");
        req.setTtChargeType("OUR");
        req.setPayGlPoid(200L);
        req.setPaymentGlDetails(List.of(
                glRow("DR", 200L, AMOUNT, ZERO),
                glRow("CR", BANK_GL_POID, ZERO, AMOUNT)
        ));
        return req;
    }

    private PaymentGlDetails glRow(String type, Long glPoid, BigDecimal dr, BigDecimal cr) {
        PaymentGlDetails d = new PaymentGlDetails();
        d.setType(type);
        d.setGlPoid(glPoid);
        d.setDrAmt(dr);
        d.setCrAmt(cr);
        d.setTotalAmount(type.equalsIgnoreCase("DR") ? dr : cr);
        d.setActionType("ISCREATED");
        return d;
    }

    private ChargeDetailDto chargeRow(BigDecimal amount, String checkAll) {
        ChargeDetailDto c = new ChargeDetailDto();
        c.setChargeAmount(amount);
        c.setTaxAmount(ZERO);
        c.setCheckAll(checkAll);
        c.setDetRowId(1L);
        return c;
    }

    private ItemDetailDto itemRow(BigDecimal total) {
        ItemDetailDto i = new ItemDetailDto();
        i.setTotal(total);
        i.setDetRowId(1L);
        return i;
    }

    // ── 1. Null request ───────────────────────────────────────────────────────

    @Test
    void validate_nullRequest_throwsValidationException() {
        assertThrows(ValidationException.class, () -> validator.validate(null, true));
    }

    // ── 2. Paying Type rules ──────────────────────────────────────────────────

    @Test
    void validate_blankPayingType_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setPayingType("");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("Paying Type is required", ex.getMessage());
    }

    @Test
    void validate_payingTypeNotFour_missingPayingToName_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setPayingType("2");
        req.setPayingToName(null);
        req.setPayGlPoid(200L);
        req.setTtChargeType(null);
        req.setPaymentGlDetails(List.of(
                glRow("DR", 200L, AMOUNT, ZERO),
                glRow("CR", BANK_GL_POID, ZERO, AMOUNT)
        ));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("Paying To Name"));
    }

    @Test
    void validate_payingTypeOne_missingBankPoid_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setPayingType("1");
        req.setBankPoid(null);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("Bank is required for TT payments", ex.getMessage());
    }

    // ── 3. Ref Type rules ─────────────────────────────────────────────────────

    @Test
    void validate_blankRefType_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setRefType("");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("Reference Type is required", ex.getMessage());
    }

    @Test
    void validate_payingTypeThree_jobRefType_withPayGl_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setPayingType("3");
        req.setRefType("FF JOBS");
        req.setPayGlPoid(200L);
        req.setFfRef("FF-001");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("PayGL must not be provided"));
    }

    // ── 4. Reference presence ─────────────────────────────────────────────────

    @Test
    void validate_ffJobsRefType_missingFfRef_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setRefType("FF JOBS");
        req.setFfRef(null);
        req.setChargeDetails(List.of(chargeRow(AMOUNT, "Y")));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("FF Reference is required for FF JOBS", ex.getMessage());
    }

    @Test
    void validate_fdaJobsRefType_missingFdaRef_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setRefType("FDA JOBS");
        req.setFdaRef(null);
        req.setChargeDetails(List.of(chargeRow(AMOUNT, "Y")));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("FDA Reference is required for FDA JOBS", ex.getMessage());
    }

    @Test
    void validate_mtaRfqRefType_missingSalesQtnRef_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setRefType("MTA RFQ");
        req.setSalesQtnRef(null);
        req.setItemDetails(List.of(itemRow(AMOUNT)));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("Sales Quotation Reference is required for MTA RFQ", ex.getMessage());
    }

    // ── 5. Beneficiary rules ──────────────────────────────────────────────────

    @Test
    void validate_payingTypeOne_missingPayingTo_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType1Request();
        req.setPayingTo(null);

        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("payingTo") || ex.getMessage().contains("Beneficiary A/C"));
    }

    // ── 6. PayGL rules ────────────────────────────────────────────────────────

    @Test
    void validate_payingTypeOne_missingPayGl_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType1Request();
        req.setPayGlPoid(null);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("PayGL is required"));
    }

    @Test
    void validate_payingTypeFour_withPayGl_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setPayGlPoid(200L);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("PayGL must not be provided for PayingType=4", ex.getMessage());
    }

    // ── 7. TT Charge Type rules ───────────────────────────────────────────────

    @Test
    void validate_payingTypeOne_missingTtChargeType_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType1Request();
        req.setTtChargeType(null);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("TT Charge Type is required for TT payments", ex.getMessage());
    }

    @Test
    void validate_payingTypeNotOne_hasTtChargeType_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setTtChargeType("OUR");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("TT Charge Type not allowed for this Paying Type", ex.getMessage());
    }

    // ── 8. Bank Purpose rules ─────────────────────────────────────────────────

    @Test
    void validate_payingTypeOne_nonBhdCurrency_missingBankPurpose_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType1Request();
        req.setCurrencyCode("USD");
        req.setBankPurposePoid(null);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("Bank Purpose Code required for TT in non-BHD currency", ex.getMessage());
    }

    // ── 9. Detail section presence ────────────────────────────────────────────

    @Test
    void validate_generalRefType_noPaymentGlDetails_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setPaymentGlDetails(null);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("Payment GL Detail is required"));
    }

    @Test
    void validate_generalRefType_emptyPaymentGlDetails_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setPaymentGlDetails(List.of());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("Payment GL Detail is required"));
    }

    @Test
    void validate_ffJobsRefType_noChargeDetails_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setPayingType("4");
        req.setRefType("FF JOBS");
        req.setFfRef("FF-001");
        req.setChargeDetails(null);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("Charge Details are required"));
    }

    @Test
    void validate_mtaRfqRefType_noItemDetails_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setRefType("MTA RFQ");
        req.setSalesQtnRef(500L);
        req.setItemDetails(null);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("Item Details are required"));
    }

    // ── 10. Amount totals ─────────────────────────────────────────────────────

    @Test
    void validate_general_drNotEqualCr_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setPaymentGlDetails(List.of(
                glRow("DR", 100L, BigDecimal.valueOf(1200), ZERO),
                glRow("CR", BANK_GL_POID, ZERO, AMOUNT)
        ));

        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("Total DR") && ex.getMessage().contains("CR"));
    }

    @Test
    void validate_general_crNotEqualHeaderAmount_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        // DR=CR=500, but headerAmount=1000
        req.setPaymentGlDetails(List.of(
                glRow("DR", 100L, BigDecimal.valueOf(500), ZERO),
                glRow("CR", BANK_GL_POID, ZERO, BigDecimal.valueOf(500))
        ));

        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("Header amount"));
    }

    // ── 11. CUSTOM specific checks (GAP-B) ────────────────────────────────────

    @Test
    void validate_customRefType_noDrEntries_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setRefType("CUSTOM");
        // Only CR rows, no DR rows
        req.setPaymentGlDetails(List.of(
                glRow("CR", BANK_GL_POID, ZERO, AMOUNT)
        ));

        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("No Debit Entries Entered...", ex.getMessage());
    }

    @Test
    void validate_customRefType_noCrEntries_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setRefType("CUSTOM");
        // Only DR rows, no CR rows
        req.setPaymentGlDetails(List.of(
                glRow("DR", 100L, AMOUNT, ZERO)
        ));

        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("No Credit Entries Entered...", ex.getMessage());
    }

    @Test
    void validate_customRefType_drNotEqualCr_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setRefType("CUSTOM");
        req.setPaymentGlDetails(List.of(
                glRow("DR", 100L, BigDecimal.valueOf(1500), ZERO),
                glRow("CR", BANK_GL_POID, ZERO, AMOUNT)
        ));

        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("Total DR") && ex.getMessage().contains("CR"));
    }

    // ── 12. VAT / Input Tax rules ─────────────────────────────────────────────

    @Test
    void validate_detailVatMismatch_exceedsAllowedDifference_throwsValidationException() {
        when(parameterServiceClient.getParameterValueByNameAsDecimal("USER", "VAT_DIFFERENCE_AMOUNT_ALLOWED"))
                .thenReturn(BigDecimal.valueOf(0.01));

        BankDebitVoucherRequest req = validGeneralType4Request();
        // DR row with 10% tax but wrong amount
        PaymentGlDetails drRow = glRow("DR", 100L, BigDecimal.valueOf(1000), ZERO);
        drRow.setTaxPercentage(BigDecimal.valueOf(10));
        drRow.setTaxAmount(BigDecimal.valueOf(200)); // should be 100, difference = 100 > 0.01

        req.setPaymentGlDetails(List.of(drRow, glRow("CR", BANK_GL_POID, ZERO, AMOUNT)));

        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("Tax amount mismatch"));
    }

    @Test
    void validate_inputTaxVariance_drRow_exceedsLimit_throwsValidationException() {
        when(parameterServiceClient.getParameterValueByNameAsDecimal("USER", "INPUT_TAX_VARIANCE_LIMIT"))
                .thenReturn(BigDecimal.valueOf(0.01));

        BankDebitVoucherRequest req = validGeneralType4Request();
        PaymentGlDetails drRow = glRow("DR", 100L, BigDecimal.valueOf(500), ZERO);
        drRow.setTaxPercentage(BigDecimal.valueOf(5));
        drRow.setTaxAmount(BigDecimal.valueOf(50)); // expected = 500 * 5% = 25; diff = 25 > 0.01

        req.setPaymentGlDetails(List.of(drRow, glRow("CR", BANK_GL_POID, ZERO, AMOUNT)));
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("WARNING") && ex.getMessage().contains("Input tax difference"));
    }

    // ── 13. Charge/Item amount reconciliation ─────────────────────────────────

    @Test
    void validate_ffJobs_chargeAmountMismatch_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setPayingType("4");
        req.setRefType("FF JOBS");
        req.setFfRef("FF-001");
        req.setPaymentGlDetails(null);
        // Total of checked charges = 500, but header amount = 1000
        req.setChargeDetails(List.of(chargeRow(BigDecimal.valueOf(500), "Y")));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("Total charge amount"));
    }

    @Test
    void validate_ffJobs_checkAllNRows_areIgnored_passesWhenCheckedRowsMatchAmount() {
        // checkAll='N' rows should NOT count toward the total
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setPayingType("4");
        req.setRefType("FF JOBS");
        req.setFfRef("FF-001");
        req.setPaymentGlDetails(null);
        ChargeDetailDto checkedRow = chargeRow(AMOUNT, "Y");
        ChargeDetailDto uncheckedRow = chargeRow(BigDecimal.valueOf(500), "N");
        req.setChargeDetails(List.of(checkedRow, uncheckedRow));
        req.setTtDate(LocalDateTime.now());

        // No exception expected — only the 'Y' row (1000) matches header amount (1000)
        assertDoesNotThrow(() -> validator.validate(req, true));
    }

    @Test
    void validate_mtaRfq_itemAmountMismatch_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setPayingType("4");
        req.setRefType("MTA RFQ");
        req.setSalesQtnRef(500L);
        req.setPaymentGlDetails(null);
        req.setItemDetails(List.of(itemRow(BigDecimal.valueOf(500)))); // 500 != 1000

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("Total item amount"));
    }

    // ── 14. Bank GL integrity (GAP-18) ────────────────────────────────────────

    @Test
    void validate_bankGlIntegrity_noBankGlPoid_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(null);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("no associated GL account"));
    }

    @Test
    void validate_bankGlIntegrity_noBankCrRow_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        // DR row uses glPoid=999L (bank), CR row uses a different GL
        req.setPaymentGlDetails(List.of(
                glRow("DR", BANK_GL_POID, AMOUNT, ZERO),
                glRow("CR", 100L, ZERO, AMOUNT)
        ));
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("credit entry to the bank GL"));
    }

    @Test
    void validate_bankGlIntegrity_bankCrTotalMismatch_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        // Bank CR row has only 500, but expected is 1000 (amount+charges+tax)
        req.setPaymentGlDetails(List.of(
                glRow("DR", 100L, AMOUNT, ZERO),
                glRow("CR", BANK_GL_POID, ZERO, BigDecimal.valueOf(500))
        ));
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("Bank GL credit total"));
    }

    @Test
    void validate_bankGlIntegrity_general_noPayGlDrRow_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType1Request();
        // payGlPoid=200L, but no DR row for glPoid=200
        req.setPaymentGlDetails(List.of(
                glRow("DR", 300L, AMOUNT, ZERO), // wrong GL
                glRow("CR", BANK_GL_POID, ZERO, AMOUNT)
        ));
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("debit entry for the Pay GL"));
    }

    // ── 15. TT Date validations (GAP-A: required for ALL paying types) ─────────

    @Test
    void validate_ttDateNull_payingTypeFour_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setTtDate(null);
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("Value Date is a required field...", ex.getMessage());
    }

    @Test
    void validate_ttDateNull_payingTypeOne_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType1Request();
        req.setTtDate(null);
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("Value Date is a required field...", ex.getMessage());
    }

    @Test
    void validate_ttDateNull_payingTypeTwo_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setPayingType("2");
        req.setPayingToName("Beneficiary");
        req.setPayGlPoid(200L);
        req.setTtDate(null);
        req.setPaymentGlDetails(List.of(
                glRow("DR", 200L, AMOUNT, ZERO),
                glRow("CR", BANK_GL_POID, ZERO, AMOUNT)
        ));
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("Value Date is a required field...", ex.getMessage());
    }

    @Test
    void validate_payingTypeOne_isNew_ttDateBeforeSystemDate_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType1Request();
        req.setTtDate(LocalDateTime.now().minusDays(1)); // yesterday
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertEquals("Value Date should not be before the System date...", ex.getMessage());
    }

    @Test
    void validate_payingTypeOne_isNew_postDateExceeded_throwsValidationException() {
        when(parameterServiceClient.getParameterValueByName("USER", "TT_POST_DATE_VALIDATION_DAYS"))
                .thenReturn(5);

        BankDebitVoucherRequest req = validGeneralType1Request();
        req.setTtDate(LocalDateTime.now().plusDays(10)); // 10 days > 5 day limit
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("Postdated entries more than 5 days"));
    }

    @Test
    void validate_payingTypeOne_isNew_ttDateToday_passes() {
        BankDebitVoucherRequest req = validGeneralType1Request();
        req.setTtDate(LocalDateTime.now()); // today (not before system date)
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        assertDoesNotThrow(() -> validator.validate(req, true));
    }

    @Test
    void validate_payingTypeOne_isEdit_ttDateBeforeDocDate_throwsValidationException() {
        BankDebitVoucherRequest req = validGeneralType1Request();
        req.setDocumentDate(java.time.LocalDate.now());
        req.setTtDate(LocalDateTime.now().minusDays(1)); // before doc date
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, false)); // isNew=false
        assertEquals("Value Date should not be before document date...", ex.getMessage());
    }

    @Test
    void validate_payingTypeTwo_isNew_backDateExceeded_throwsValidationException() {
        when(parameterServiceClient.getParameterValueByName("USER", "DIRECT_TRANSFER_BACK_DATE_VALIDATION_DAYS"))
                .thenReturn(-3); // max 3 days back

        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setPayingType("2");
        req.setPayingToName("Beneficiary");
        req.setPayGlPoid(200L);
        req.setTtDate(LocalDateTime.now().minusDays(10)); // 10 days before today
        req.setPaymentGlDetails(List.of(
                glRow("DR", 200L, AMOUNT, ZERO),
                glRow("CR", BANK_GL_POID, ZERO, AMOUNT)
        ));
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(req, true));
        assertTrue(ex.getMessage().contains("less than") && ex.getMessage().contains("Direct Transfer"));
    }

    // ── 16. Full valid request (success) ──────────────────────────────────────

    @Test
    void validate_fullValidGeneralType4Request_doesNotThrow() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        assertDoesNotThrow(() -> validator.validate(req, true));
    }

    @Test
    void validate_fullValidGeneralType1Request_doesNotThrow() {
        BankDebitVoucherRequest req = validGeneralType1Request();
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        assertDoesNotThrow(() -> validator.validate(req, true));
    }

    @Test
    void validate_ffJobs_validRequest_doesNotThrow() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setRefType("FF JOBS");
        req.setFfRef("FF-001");
        req.setPaymentGlDetails(null);
        req.setChargeDetails(List.of(chargeRow(AMOUNT, "Y")));

        assertDoesNotThrow(() -> validator.validate(req, true));
    }

    @Test
    void validate_mtaRfq_validRequest_doesNotThrow() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setRefType("MTA RFQ");
        req.setSalesQtnRef(500L);
        req.setPaymentGlDetails(null);
        req.setItemDetails(List.of(itemRow(AMOUNT)));

        assertDoesNotThrow(() -> validator.validate(req, true));
    }

    // ── 17. resolveReferenceValue (public) ────────────────────────────────────

    @Test
    void resolveReferenceValue_ffJobs_returnsFfRef() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setRefType("FF JOBS");
        req.setFfRef("FF-001");

        assertEquals("FF-001", validator.resolveReferenceValue(req));
    }

    @Test
    void resolveReferenceValue_fdaJobs_returnsFdaRef() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setRefType("FDA JOBS");
        req.setFdaRef(123L);

        assertEquals("123", validator.resolveReferenceValue(req));
    }

    @Test
    void resolveReferenceValue_mtaRfq_returnsSalesQtnRef() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setRefType("MTA RFQ");
        req.setSalesQtnRef(456L);

        assertEquals("456", validator.resolveReferenceValue(req));
    }

    @Test
    void resolveReferenceValue_general_returnsNull() {
        BankDebitVoucherRequest req = new BankDebitVoucherRequest();
        req.setRefType("GENERAL");

        assertNull(validator.resolveReferenceValue(req));
    }

    // ── 18. SP interactions ───────────────────────────────────────────────────

    @Test
    void validate_ffJobsWithFfRef_callsJobStatusSp() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        req.setRefType("FF JOBS");
        req.setFfRef("FF-001");
        req.setPaymentGlDetails(null);
        req.setChargeDetails(List.of(chargeRow(AMOUNT, "Y")));

        validator.validate(req, true);

        verify(bankDebitVoucherCustomRepository).procGlJobValBeforeSave(
                eq(1L), eq(1L), eq(1L), eq("400-111"), eq("FF JOBS"), eq("FF-001"));
    }

    @Test
    void validate_alwaysCallsPayGlBeneficiarySp() {
        BankDebitVoucherRequest req = validGeneralType4Request();
        when(bankDebitVoucherCustomRepository.getBankGlPoid(1L)).thenReturn(BANK_GL_POID);

        validator.validate(req, true);

        verify(bankDebitVoucherCustomRepository).procGlBankPayGlBenVal(
                eq(1L), eq(1L), eq(1L), eq("400-111"), isNull(),
                eq("4"), eq("GENERAL"), isNull(), isNull(), eq(1L));
    }
}
