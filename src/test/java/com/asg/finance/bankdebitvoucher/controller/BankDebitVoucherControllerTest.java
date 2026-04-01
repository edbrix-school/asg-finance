package com.asg.finance.bankdebitvoucher.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.controller.BankDebitVoucherController;
import com.asg.finance.dto.*;
import com.asg.finance.service.BankDebitVoucherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = BankDebitVoucherController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.asg.finance.aspect.*"))
@ContextConfiguration(classes = {BankDebitVoucherController.class})
class BankDebitVoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BankDebitVoucherService bankDebitVoucherService;

    @MockBean
    private LoggingService loggingService;

    private BankDebitVoucherRequest request;
    private BankDebitVoucherResponse response;

    @BeforeEach
    void setUp() {
        request = new BankDebitVoucherRequest();
        request.setBankPoid(1008L);
        request.setCurrencyCode("USD");
        request.setCurrencyRate(BigDecimal.ONE);
        request.setCurrencyAmt(BigDecimal.valueOf(1000));
        request.setAmount(BigDecimal.valueOf(1000));
        request.setLongNarration("Test payment");
        request.setPayingType("4");
        request.setRefType("GENERAL");

        response = new BankDebitVoucherResponse();
        response.setTransactionPoid(1L);
        response.setBankPoid(1008L);
        response.setAmount(BigDecimal.valueOf(1000));
        response.setPayingType("4");
        response.setRefType("GENERAL");
    }

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @Test
    void createBankDebitVoucher_Success() throws Exception {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("DOC123");
            when(bankDebitVoucherService.createBankDebitVoucher(any(BankDebitVoucherRequest.class), eq("DOC123")))
                    .thenReturn(response);

            mockMvc.perform(post("/v1/bank-debit-voucher")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(1L));

            verify(bankDebitVoucherService).createBankDebitVoucher(any(BankDebitVoucherRequest.class), eq("DOC123"));
        }
    }

    @Test
    void createBankDebitVoucher_MissingRequired_Returns400() throws Exception {
        BankDebitVoucherRequest bad = new BankDebitVoucherRequest();
        // bankPoid, currencyCode, amount, longNarration, payingType, refType all missing

        mockMvc.perform(post("/v1/bank-debit-voucher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    // ─── GET ─────────────────────────────────────────────────────────────────

    @Test
    void getBankDebitVoucher_Success() throws Exception {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("DOC123");
            when(bankDebitVoucherService.getBankDebitVoucher(1L, "DOC123")).thenReturn(response);

            mockMvc.perform(get("/v1/bank-debit-voucher/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(1L));

            verify(bankDebitVoucherService).getBankDebitVoucher(1L, "DOC123");
            verify(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), eq("DOC123"), eq("1"));
        }
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    @Test
    void updateBankDebitVoucher_Success() throws Exception {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("DOC123");
            when(bankDebitVoucherService.updateBankDebitVoucher(eq(1L), any(BankDebitVoucherRequest.class), eq("DOC123")))
                    .thenReturn(response);

            mockMvc.perform(put("/v1/bank-debit-voucher/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(1L));

            verify(bankDebitVoucherService).updateBankDebitVoucher(eq(1L), any(BankDebitVoucherRequest.class), eq("DOC123"));
        }
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @Test
    void deleteBankDebitVoucher_Success() throws Exception {
        DeleteReasonDto deleteDto = new DeleteReasonDto();
        deleteDto.setDeleteReason("No longer required");

        doNothing().when(bankDebitVoucherService).softDeleteBankDebitVoucher(eq(1L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/bank-debit-voucher/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data").value(1));

        verify(bankDebitVoucherService).softDeleteBankDebitVoucher(eq(1L), any(DeleteReasonDto.class));
    }

    @Test
    void deleteBankDebitVoucher_WithoutBody_Success() throws Exception {
        doNothing().when(bankDebitVoucherService).softDeleteBankDebitVoucher(eq(1L), isNull());

        mockMvc.perform(delete("/v1/bank-debit-voucher/1"))
                .andExpect(status().isOk());

        verify(bankDebitVoucherService).softDeleteBankDebitVoucher(eq(1L), isNull());
    }

    // ─── LIST ─────────────────────────────────────────────────────────────────

    @Test
    void listBankDebitVouchers_Success() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("content", Collections.emptyList());
        data.put("totalElements", 0);

        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("DOC123");
            when(bankDebitVoucherService.listBankDebitVouchers(eq("DOC123"), any(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(data);

            mockMvc.perform(post("/v1/bank-debit-voucher/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.totalElements").value(0));

            verify(bankDebitVoucherService).listBankDebitVouchers(eq("DOC123"), any(), isNull(), isNull(), any(Pageable.class));
        }
    }

    @Test
    void listBankDebitVouchers_OnlyStartDate_Returns400() throws Exception {
        // startDate provided but endDate is missing → controller returns badRequest
        mockMvc.perform(post("/v1/bank-debit-voucher/list")
                        .param("startDate", "2025-01-01")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listBankDebitVouchers_OnlyEndDate_Returns400() throws Exception {
        mockMvc.perform(post("/v1/bank-debit-voucher/list")
                        .param("endDate", "2025-12-31")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listBankDebitVouchers_WithDateRange_Success() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("content", Collections.emptyList());

        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("DOC123");
            when(bankDebitVoucherService.listBankDebitVouchers(eq("DOC123"), any(),
                    eq(LocalDate.of(2025, 1, 1)), eq(LocalDate.of(2025, 12, 31)), any(Pageable.class)))
                    .thenReturn(data);

            mockMvc.perform(post("/v1/bank-debit-voucher/list")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-12-31")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());
        }
    }

    // ─── MTA ITEMS ────────────────────────────────────────────────────────────

    @Test
    void getMTAItems_Success() throws Exception {
        List<ItemDetailDto> items = Collections.emptyList();
        when(bankDebitVoucherService.loadMTAItems(5L)).thenReturn(items);

        mockMvc.perform(get("/v1/bank-debit-voucher/mta-items")
                        .param("salesQtnRefPoid", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data").isArray());

        verify(bankDebitVoucherService).loadMTAItems(5L);
    }

    // ─── MTA REF ─────────────────────────────────────────────────────────────

    @Test
    void getMtaRef_Success() throws Exception {
        when(bankDebitVoucherService.getMtaRef(5L)).thenReturn("MTA-001");

        mockMvc.perform(get("/v1/bank-debit-voucher/mta-ref")
                        .param("salesQtnRefPoid", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data").value("MTA-001"));

        verify(bankDebitVoucherService).getMtaRef(5L);
    }

    // ─── DEFAULT GL ROWS ──────────────────────────────────────────────────────

    @Test
    void generateDefaultGlRows_Success() throws Exception {
        PaymentGlDetails dr = new PaymentGlDetails();
        dr.setDetRowId(1L);
        dr.setType("DR");
        dr.setDrAmt(BigDecimal.valueOf(1000));

        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("DOC123");
            when(bankDebitVoucherService.generateDefaultGlRows(any(BankDebitVoucherRequest.class)))
                    .thenReturn(List.of(dr));

            mockMvc.perform(post("/v1/bank-debit-voucher/default-gl-rows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data[0].type").value("DR"));

            verify(bankDebitVoucherService).generateDefaultGlRows(any(BankDebitVoucherRequest.class));
        }
    }

    // ─── FF CHARGES ───────────────────────────────────────────────────────────

    @Test
    void getFFCharges_Success() throws Exception {
        when(bankDebitVoucherService.loadFFCharges(10L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/bank-debit-voucher/ff-charges")
                        .param("ffRefPoid", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data").isArray());

        verify(bankDebitVoucherService).loadFFCharges(10L);
    }

    // ─── FDA CHARGES ──────────────────────────────────────────────────────────

    @Test
    void getFDACharges_Success() throws Exception {
        when(bankDebitVoucherService.loadFDACharges(20L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/bank-debit-voucher/fda-charges")
                        .param("fdaRefPoid", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data").isArray());

        verify(bankDebitVoucherService).loadFDACharges(20L);
    }

    // ─── BANK BALANCE ─────────────────────────────────────────────────────────

    @Test
    void getBankBalance_Success() throws Exception {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("DOC123");
            when(bankDebitVoucherService.getBankBalance(1008L, "DOC123", LocalDate.of(2025, 1, 20)))
                    .thenReturn(BigDecimal.valueOf(50000));

            mockMvc.perform(get("/v1/bank-debit-voucher/bank-balance")
                            .param("bankPoid", "1008")
                            .param("docDate", "2025-01-20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data").value(50000));

            verify(bankDebitVoucherService).getBankBalance(1008L, "DOC123", LocalDate.of(2025, 1, 20));
        }
    }

    // ─── BENEFICIARY NAME ─────────────────────────────────────────────────────

    @Test
    void getBeneficiaryName_Success() throws Exception {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("DOC123");
            when(bankDebitVoucherService.getBeneficiaryName(42L, "DOC123")).thenReturn("John Doe");

            mockMvc.perform(get("/v1/bank-debit-voucher/beneficiary-name")
                            .param("beneficiaryId", "42"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data").value("John Doe"));

            verify(bankDebitVoucherService).getBeneficiaryName(42L, "DOC123");
        }
    }

    // ─── VALIDATE PAY GL ──────────────────────────────────────────────────────

    @Test
    void validatePayGL_Success() throws Exception {
        PayGLValidationRequest valReq = new PayGLValidationRequest();
        valReq.setTransactionPoid(1L);
        valReq.setPayingType("4");
        valReq.setRefType("GENERAL");
        valReq.setBankPoid(1008L);

        doNothing().when(bankDebitVoucherService).validatePayGLAndBeneficiary(any(PayGLValidationRequest.class));

        mockMvc.perform(post("/v1/bank-debit-voucher/validate-paygl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valReq)))
                .andExpect(status().isOk());

        verify(bankDebitVoucherService).validatePayGLAndBeneficiary(any(PayGLValidationRequest.class));
    }

    // ─── REVERT RECONCILIATION ────────────────────────────────────────────────

    @Test
    void revertReconciliation_Success() throws Exception {
        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("DOC123");
            doNothing().when(bankDebitVoucherService).revertReconciliation(1L, "Test comment");

            mockMvc.perform(post("/v1/bank-debit-voucher/1/revert-reconciliation")
                            .param("comments", "Test comment"))
                    .andExpect(status().isOk());

            verify(bankDebitVoucherService).revertReconciliation(1L, "Test comment");
        }
    }

    @Test
    void revertReconciliation_ServiceThrows_Returns500() throws Exception {
        doThrow(new RuntimeException("Reconciliation error"))
                .when(bankDebitVoucherService).revertReconciliation(1L, null);

        mockMvc.perform(post("/v1/bank-debit-voucher/1/revert-reconciliation"))
                .andExpect(status().isInternalServerError());
    }

    // ─── PRINT ────────────────────────────────────────────────────────────────

    @Test
    void print_Success() throws Exception {
        byte[] pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF magic bytes
        when(bankDebitVoucherService.print(1L)).thenReturn(pdfBytes);

        mockMvc.perform(get("/v1/bank-debit-voucher/print/1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=bank-debit-voucher-1.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        verify(bankDebitVoucherService).print(1L);
    }

    @Test
    void print_ServiceThrows_Returns500() throws Exception {
        when(bankDebitVoucherService.print(1L)).thenThrow(new RuntimeException("Jasper error"));

        mockMvc.perform(get("/v1/bank-debit-voucher/print/1"))
                .andExpect(status().isInternalServerError());
    }

    // ─── GET – not found ──────────────────────────────────────────────────────



    // ─── UPDATE – missing required fields ─────────────────────────────────────

    @Test
    void updateBankDebitVoucher_MissingRequired_Returns400() throws Exception {
        BankDebitVoucherRequest bad = new BankDebitVoucherRequest();

        mockMvc.perform(put("/v1/bank-debit-voucher/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    // ─── LIST – no body ────────────────────────────────────────────────────────

    @Test
    void listBankDebitVouchers_NoBody_Success() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("content", Collections.emptyList());

        try (MockedStatic<UserContext> muc = mockStatic(UserContext.class)) {
            muc.when(UserContext::getDocumentId).thenReturn("DOC123");
            when(bankDebitVoucherService.listBankDebitVouchers(eq("DOC123"), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(data);

            mockMvc.perform(post("/v1/bank-debit-voucher/list")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());
        }
    }

    // ─── BANK BALANCE – missing required params ───────────────────────────────

    @Test
    void getBankBalance_MissingDocDate_Returns400() throws Exception {
        mockMvc.perform(get("/v1/bank-debit-voucher/bank-balance")
                        .param("bankPoid", "1008"))
                .andExpect(status().isBadRequest());
    }

    // ─── MTA ITEMS – missing required param ─────────────────────────────────────

    @Test
    void getMTAItems_MissingParam_Returns400() throws Exception {
        mockMvc.perform(get("/v1/bank-debit-voucher/mta-items"))
                .andExpect(status().isBadRequest());
    }

    // ─── VALIDATE PAY GL – service throws ──────────────────────────────────────


}
