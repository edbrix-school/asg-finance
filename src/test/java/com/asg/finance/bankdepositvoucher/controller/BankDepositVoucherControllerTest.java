package com.asg.finance.bankdepositvoucher.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDownloadHeaderService;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.controller.BankDepositVoucherController;
import com.asg.finance.dto.BankDepositVoucherDtlDto;
import com.asg.finance.dto.BankDepositVoucherRequestDto;
import com.asg.finance.dto.BankDepositVoucherResponseDto;
import com.asg.finance.service.BankDepositVoucherService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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

@WebMvcTest(value = BankDepositVoucherController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.asg.finance.aspect.*"))
@ContextConfiguration(classes = {BankDepositVoucherController.class, DocumentDownloadHeaderService.class})
class BankDepositVoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean

    private JdbcTemplate jdbcTemplate;


    @MockBean
    private BankDepositVoucherService service;

    @MockBean
    private LoggingService loggingService;

    private BankDepositVoucherRequestDto requestDto;
    private BankDepositVoucherResponseDto responseDto;

    @BeforeEach
    void setUp() {
        BankDepositVoucherDtlDto detail = BankDepositVoucherDtlDto.builder()
                .bankPoid(61L)
                .pymtType("CHEQUE")
                .amount(new BigDecimal("74.89"))
                .build();

        requestDto = BankDepositVoucherRequestDto.builder()
                .bankPoid(61L)
                .type("CHEQUE")
                .details(Collections.singletonList(detail))
                .build();

        responseDto = BankDepositVoucherResponseDto.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.now())
                .docRef("DOC-001")
                .bankPoid(61L)
                .grandTotal(new BigDecimal("74.89"))
                .build();
    }

    @Test
    void create_Success() throws Exception {
        when(service.createBankDepositVoucher(any(BankDepositVoucherRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/v1/bank-deposit-voucher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.transactionPoid").value(1L))
                .andExpect(jsonPath("$.message").value("Bank Deposit Voucher created successfully"));

        verify(service, times(1)).createBankDepositVoucher(any(BankDepositVoucherRequestDto.class));
    }

    @Test
    void create_Exception() throws Exception {
        when(service.createBankDepositVoucher(any(BankDepositVoucherRequestDto.class)))
                .thenThrow(new RuntimeException("Error creating"));

        mockMvc.perform(post("/v1/bank-deposit-voucher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void update_Success() throws Exception {
        when(service.updateBankDepositVoucher(eq(1L), any(BankDepositVoucherRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/v1/bank-deposit-voucher/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.transactionPoid").value(1L))
                .andExpect(jsonPath("$.message").value("Bank Deposit Voucher updated successfully"));

        verify(service, times(1)).updateBankDepositVoucher(eq(1L), any(BankDepositVoucherRequestDto.class));
    }

    @Test
    void getById_Success() throws Exception {
        when(service.getBankDepositVoucherById(1L)).thenReturn(responseDto);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            mockMvc.perform(get("/v1/bank-deposit-voucher/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(1L));

            verify(service, times(1)).getBankDepositVoucherById(1L);
            verify(loggingService, times(1)).createLogSummaryEntry(any(LogDetailsEnum.class), eq("DOC123"), eq("1"));
        }
    }

    @Test
    void softDelete_Success() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Deleted for testing");

        doNothing().when(service).softDeleteBankDepositVoucher(eq(1L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/bank-deposit-voucher/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bank Deposit Voucher has been soft deleted successfully"));

        verify(service, times(1)).softDeleteBankDepositVoucher(eq(1L), any(DeleteReasonDto.class));
    }

    @Test
    void list_Success() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("content", Collections.singletonList(responseDto));
        data.put("totalElements", 1);

        when(service.listBankDepositVouchers(anyString(), any(), any(), any(), any(Pageable.class))).thenReturn(data);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            mockMvc.perform(post("/v1/bank-deposit-voucher/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.totalElements").value(1));

            verify(service, times(1)).listBankDepositVouchers(eq("DOC123"), any(), isNull(), isNull(), any(Pageable.class));
        }
    }

    @Test
    void list_DateRangeError() throws Exception {
        mockMvc.perform(post("/v1/bank-deposit-voucher/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("startDate", "2024-01-01")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Both startDate and endDate should be specified or both dates should be empty."));
    }

    @Test
    void list_Exception() throws Exception {
        when(service.listBankDepositVouchers(anyString(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Search failed"));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            mockMvc.perform(post("/v1/bank-deposit-voucher/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Test
    void loadPendingPayments_Success() throws Exception {
        List<BankDepositVoucherDtlDto> details = Collections.singletonList(new BankDepositVoucherDtlDto());
        when(service.loadPendingPayments(15631L, "CHEQUE", "OTHER BANK")).thenReturn(details);

        mockMvc.perform(get("/v1/bank-deposit-voucher/load/pending-payments")
                        .param("bankPoid", "15631")
                        .param("type", "CHEQUE")
                        .param("bankFilter", "OTHER BANK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data").isArray());

        verify(service, times(1)).loadPendingPayments(15631L, "CHEQUE", "OTHER BANK");
    }

    @Test
    void print_Success() throws Exception {
        byte[] pdf = "dummy pdf".getBytes();
        when(service.print(1L)).thenReturn(pdf);

        mockMvc.perform(get("/v1/bank-deposit-voucher/print/1"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bank-deposit-voucher-1.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));

        verify(service, times(1)).print(1L);
    }

    @Test
    void print_Exception() throws Exception {
        when(service.print(1L)).thenThrow(new RuntimeException("Print failed"));

        mockMvc.perform(get("/v1/bank-deposit-voucher/print/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Failed to generate PDF: Print failed"));
    }
}
