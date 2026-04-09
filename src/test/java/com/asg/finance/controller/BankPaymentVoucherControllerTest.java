package com.asg.finance.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.ReconcileResultDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.model.CustomAuthDetails;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.dto.*;
import com.asg.finance.service.BankPaymentVoucherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankPaymentVoucherController.class)
@AutoConfigureMockMvc(addFilters = false)
class BankPaymentVoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BankPaymentVoucherService service;

    @MockBean
    private LoggingService loggingService;

    private static final String BASE_URL = "/v1/bank-payment-vouchers";
    private static final String DOC_ID = "400-107";

    @BeforeEach
    void setUp() {
        CustomAuthDetails authDetails = CustomAuthDetails.builder()
                .documentId(DOC_ID)
                .userId("testUser")
                .build();
        UserContext.setCurrentUser(authDetails);
    }

    @Test
    @DisplayName("GET /v1/bank-payment-vouchers/{id} - Success")
    void getVoucherById_givenValidId_whenGetVoucher_thenReturnsSuccess() throws Exception {
        Long transactionPoid = 1L;
        BankPaymentVoucherResponse response = new BankPaymentVoucherResponse();
        response.setTransactionPoid(transactionPoid);

        when(service.getVoucherById(eq(transactionPoid), anyString())).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/{transactionPoid}", transactionPoid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Voucher fetched successfully"))
                .andExpect(jsonPath("$.result.data.transactionPoid").value(transactionPoid));

        verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.VIEWED), anyString(), eq(transactionPoid.toString()));
    }

    @Test
    @DisplayName("GET /v1/bank-payment-vouchers/{id} - Validation Exception")
    void getVoucherById_givenInvalidId_whenGetVoucher_thenReturnsInternalServerError() throws Exception {
        Long transactionPoid = 1L;
        when(service.getVoucherById(eq(transactionPoid), anyString())).thenThrow(new ValidationException("Voucher not found"));

        mockMvc.perform(get(BASE_URL + "/{transactionPoid}", transactionPoid))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Voucher not found"));
    }

    @Test
    @DisplayName("POST /v1/bank-payment-vouchers - Success")
    void createVoucher_givenValidRequest_whenCreateVoucher_thenReturnsCreated() throws Exception {
        BankPaymentVoucherRequest request = new BankPaymentVoucherRequest();
        BankPaymentVoucherResponse response = new BankPaymentVoucherResponse();
        response.setTransactionPoid(100L);

        when(service.createBankPaymentVoucher(any(BankPaymentVoucherRequest.class), anyString())).thenReturn(response);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Bank Payment Voucher created successfully"));
    }

    @Test
    @DisplayName("PUT /v1/bank-payment-vouchers/{id} - Success")
    void updateVoucher_givenValidRequest_whenUpdateVoucher_thenReturnsSuccess() throws Exception {
        Long transactionPoid = 1L;
        BankPaymentVoucherRequest request = new BankPaymentVoucherRequest();
        BankPaymentVoucherResponse response = new BankPaymentVoucherResponse();
        response.setTransactionPoid(transactionPoid);

        when(service.updateBankPaymentVoucher(eq(transactionPoid), any(BankPaymentVoucherRequest.class), anyString())).thenReturn(response);

        mockMvc.perform(put(BASE_URL + "/{transactionPoid}", transactionPoid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Bank Payment Voucher updated successfully"));
    }

    @Test
    @DisplayName("POST /v1/bank-payment-vouchers/list - Success")
    void listBankPaymentVouchers_givenFilters_whenList_thenReturnsData() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("vouchers", java.util.Collections.emptyList());

        when(service.listBankPaymentVouchers(anyString(), any(), any(), any(), any(Pageable.class))).thenReturn(data);

        mockMvc.perform(post(BASE_URL + "/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FilterRequestDto(null, null, null)))
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Bank Payment Vouchers fetched successfully"));
    }

    @Test
    @DisplayName("DELETE /v1/bank-payment-vouchers/{id}/delete - Success")
    void softDeleteVoucher_givenId_whenDelete_thenReturnsSuccess() throws Exception {
        Long transactionPoid = 1L;
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Deletion reason");

        doNothing().when(service).softDeleteVoucher(eq(transactionPoid), anyString(), any(DeleteReasonDto.class));

        mockMvc.perform(delete(BASE_URL + "/{transactionPoid}/delete", transactionPoid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Bank Payment Voucher soft deleted successfully"));
    }

    @Test
    @DisplayName("GET /bank-balance/{bankPoid} - Exception")
    void getBankBalance_whenException_thenReturnsError() throws Exception {
        Long bankPoid = 1L;
        when(service.getBankBalance(anyString(), any(), any(), anyLong())).thenThrow(new RuntimeException("Balance error"));

        mockMvc.perform(get(BASE_URL + "/bank-balance/{bankPoid}", bankPoid))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Failed to fetch bank balance")));
    }

    @Test
    @DisplayName("POST /validate-cheque-print - Exception")
    void validateChequePrint_whenException_thenReturnsError() throws Exception {
        Long transactionPoid = 1L;
        doThrow(new RuntimeException("Validation failed")).when(service).validateChequePrint(transactionPoid);

        mockMvc.perform(post(BASE_URL + "/{transactionPoid}/validate-cheque-print", transactionPoid))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Validation failed")));
    }

    @Test
    @DisplayName("POST /release-cheque - Exception")
    void releaseCheque_whenException_thenReturnsError() throws Exception {
        Long transactionPoid = 1L;
        doThrow(new RuntimeException("Release error")).when(service).releaseCheque(anyLong(), anyString(), anyString());

        mockMvc.perform(post(BASE_URL + "/{transactionPoid}/release-cheque", transactionPoid)
                        .param("releasedTo", "Test")
                        .param("contact", "123"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Failed to release cheque")));
    }

    @Test
    @DisplayName("POST /revert-reconciliation - Success")
    void revertReconciliation_whenServiceReturnsSuccess_thenReturnsSuccess() throws Exception {
        Long transactionPoid = 1L;
        when(service.revertReconciliation(eq(transactionPoid), anyString())).thenReturn("SUCCESS: Done");

        mockMvc.perform(post(BASE_URL + "/{transactionPoid}/revert-reconciliation", transactionPoid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("SUCCESS: Done"));
    }

    @Test
    @DisplayName("POST /revert-reconciliation - Failure")
    void revertReconciliation_whenServiceReturnsFailure_thenReturnsError() throws Exception {
        Long transactionPoid = 1L;
        when(service.revertReconciliation(eq(transactionPoid), anyString())).thenReturn("ERROR: Failed");

        mockMvc.perform(post(BASE_URL + "/{transactionPoid}/revert-reconciliation", transactionPoid))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Failed to revert reconciliation")));
    }

    @Test
    @DisplayName("POST /load-from-ff - Success")
    void createBankPayFromFf_whenValid_thenReturnsSuccess() throws Exception {
        String ffPoid = "FF123";
        BankPayCreateFromFfResponse response = new BankPayCreateFromFfResponse();
        when(service.createBankPayFromFf(ffPoid)).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/load-from-ff")
                        .param("ffPoid", ffPoid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /print/{id} - Success")
    void print_whenValidId_thenReturnsPdf() throws Exception {
        Long transactionPoid = 1L;
        byte[] pdf = "pdf content".getBytes();
        when(service.print(transactionPoid)).thenReturn(pdf);

        mockMvc.perform(get(BASE_URL + "/print/{transactionPoid}", transactionPoid))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("attachment")));
    }
}

