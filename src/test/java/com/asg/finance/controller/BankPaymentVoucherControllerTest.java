//package com.asg.finance.controller;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.exception.ValidationException;
//import com.asg.finance.dto.BankPaymentChargeDetailRequest;
//import com.asg.finance.dto.BankPaymentVoucherRequest;
//import com.asg.finance.dto.BankPaymentVoucherResponse;
//import com.asg.finance.exceptions.GlobalExceptionHandler;
//import com.asg.finance.service.BankPaymentVoucherService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.hamcrest.Matchers.hasSize;
//import static org.hamcrest.Matchers.is;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//public class BankPaymentVoucherControllerTest {
//
//    private MockMvc mockMvc;
//
//    @Mock
//    private BankPaymentVoucherService service;
//
//    @InjectMocks
//    private BankPaymentVoucherController controller;
//
//    private ObjectMapper objectMapper;
//    private BankPaymentVoucherRequest request;
//    private BankPaymentVoucherResponse response;
//
//    @BeforeEach
//    void setUp() {
//        objectMapper = new ObjectMapper()
//                .registerModule(new JavaTimeModule())
//                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//
//        mockMvc = MockMvcBuilders.standaloneSetup(controller)
//                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
//                .setControllerAdvice(new GlobalExceptionHandler())
//                .build();
//
//        BankPaymentChargeDetailRequest chargeRequest = new BankPaymentChargeDetailRequest();
//        chargeRequest.setDetRowId(1L);
//        chargeRequest.setChargePoid(1L);
//        chargeRequest.setChargeAmount(20000L);
//        chargeRequest.setDescription("Freight Charge");
//        chargeRequest.setSelected(true);
//
//        request = new BankPaymentVoucherRequest();
//        request.setBankPoid(100L);
//        request.setRefType("FDA JOBS");
//        request.setFdaRefId(1122L);
//        request.setPayGlPoid(500L);
//        request.setPayingTo("XYZ Logistics");
//        request.setChequeNo("CHQ987654");
//        request.setChequeDate("2025-11-12");
//        request.setRemarks("Payment for freight");
//        request.setAccountPayee(true);
//        request.setChargeDetailRequests(List.of(chargeRequest));
//
//        response = new BankPaymentVoucherResponse();
//        response.setTransactionPoid(1L);
//        response.setDocRef("BPV-1");
//        response.setRefType("FDA JOBS");
//        response.setPayingTo("XYZ Logistics");
//    }
//
//    @Test
//    void createVoucher_ShouldCreateSuccessfully() throws Exception {
//        when(service.createBankPaymentVoucher(any(BankPaymentVoucherRequest.class), anyString())).thenReturn(response);
//
//        mockMvc.perform(post("/api/v1/bank-payment-vouchers")
//                        .param("documentId", "400-107")
//                        .param("actionRequested", "create")
//                        .param("userPoid", "101")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Bank Payment Voucher created successfully")))
//                .andExpect(jsonPath("$.result.data.transactionPoid", is(1)));
//
//        verify(service).createBankPaymentVoucher(any(BankPaymentVoucherRequest.class), anyString());
//    }
//
//    @Test
//    void createVoucher_ShouldReturnError_WhenValidationFails() throws Exception {
//        when(service.createBankPaymentVoucher(any(), anyString())).thenThrow(new ValidationException("FDA Ref Id is mandatory"));
//
//        mockMvc.perform(post("/api/v1/bank-payment-vouchers")
//                        .param("documentId", "400-107")
//                        .param("actionRequested", "create")
//                        .param("userPoid", "101")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success", is(false)))
//                .andExpect(jsonPath("$.message", is("FDA Ref Id is mandatory")));
//
//        verify(service).createBankPaymentVoucher(any(), anyString());
//    }
//
//    @Test
//    void getVoucherById_ShouldReturnSuccessfully() throws Exception {
//        when(service.getVoucherById(1L, "400-107")).thenReturn(response);
//
//        mockMvc.perform(get("/api/v1/bank-payment-vouchers/{transactionPoid}", 1L)
//                        .param("documentId", "400-107")
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message", is("Voucher fetched successfully")))
//                .andExpect(jsonPath("$.result.data.docRef", is("BPV-1")));
//
//        verify(service).getVoucherById(1L, "400-107");
//    }
//
//    @Test
//    void updateVoucher_ShouldUpdateSuccessfully() throws Exception {
//        when(service.updateBankPaymentVoucher(eq(1L), any(BankPaymentVoucherRequest.class), anyString())).thenReturn(response);
//
//        mockMvc.perform(put("/api/v1/bank-payment-vouchers/{transactionPoid}", 1L)
//                        .param("documentId", "400-107")
//                        .param("actionRequested", "update")
//                        .param("userPoid", "101")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Bank Payment Voucher updated successfully")));
//
//        verify(service).updateBankPaymentVoucher(eq(1L), any(BankPaymentVoucherRequest.class), anyString());
//    }
//
//    @Test
//    void softDeleteVoucher_ShouldDeleteSuccessfully() throws Exception {
//        when(service.softDeleteVoucher(1L, "400-107")).thenReturn(response);
//
//        mockMvc.perform(delete("/api/v1/bank-payment-vouchers/{transactionPoid}/delete", 1L)
//                        .param("documentId", "400-107")
//                        .param("actionRequested", "delete"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message", is("Bank Payment Voucher soft deleted successfully")));
//
//        verify(service).softDeleteVoucher(1L,"400-107");
//    }
//
//    @Test
//    void listBankPaymentVouchers_ShouldReturnFilteredList() throws Exception {
//        Pageable pageable = PageRequest.of(0, 10);
//        Map<String, Object> mockResponse = new HashMap<>();
//        mockResponse.put("records", List.of(
//                Map.of("TRANSACTION_POID", 1L, "DOC_REF", "BPV-1"),
//                Map.of("TRANSACTION_POID", 2L, "DOC_REF", "BPV-2")
//        ));
//        mockResponse.put("totalCount", 2);
//
//        when(service.listBankPaymentVouchers(eq("400-107"), any(FilterRequestDto.class), any(java.time.LocalDate.class), any(java.time.LocalDate.class), any(Pageable.class)))
//                .thenReturn(mockResponse);
//
//        String filterRequestJson = """
//        {
//          "operator": "AND",
//          "isDeleted": "N",
//          "filters": [
//            { "searchField": "GLOBALSEARCH", "searchValue": "BPV" }
//          ]
//        }
//        """;
//
//        mockMvc.perform(post("/api/v1/bank-payment-vouchers/list")
//                        .param("documentId", "400-107")
//                        .param("actionRequested", "VIEW")
//                        .param("page", "0")
//                        .param("size", "10")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(filterRequestJson))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.result.data.totalCount", is(2)))
//                .andExpect(jsonPath("$.result.data.records", hasSize(2)))
//                .andExpect(jsonPath("$.result.data.records[0].DOC_REF", is("BPV-1")));
//
//        verify(service).listBankPaymentVouchers(eq("400-107"), any(FilterRequestDto.class), any(java.time.LocalDate.class), any(java.time.LocalDate.class), any(Pageable.class));
//    }
//
//    @Test
//    void getBankBalance_ShouldReturnBalance() throws Exception {
//        Map<String, Object> balance = Map.of("currentBalance", 100000, "availableBalance", 95000);
//        when(service.getBankBalance(100L)).thenReturn(balance);
//
//        mockMvc.perform(get("/api/v1/bank-payment-vouchers/bank-balance/100")
//                        .param("documentId", "400-107")
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)));
//
//        verify(service).getBankBalance(100L);
//    }
//
//    @Test
//    void validateChequePrint_ShouldValidateSuccessfully() throws Exception {
//        doNothing().when(service).validateChequePrint(1L);
//
//        mockMvc.perform(post("/api/v1/bank-payment-vouchers/1/validate-cheque-print")
//                        .param("documentId", "400-107")
//                        .param("actionRequested", "VALIDATE"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)));
//
//        verify(service).validateChequePrint(1L);
//    }
//
//    @Test
//    void markChequePrinted_ShouldMarkSuccessfully() throws Exception {
//        doNothing().when(service).markChequePrinted(1L);
//
//        mockMvc.perform(post("/api/v1/bank-payment-vouchers/1/mark-cheque-printed")
//                        .param("documentId", "400-107")
//                        .param("actionRequested", "PRINT"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)));
//
//        verify(service).markChequePrinted(1L);
//    }
//
//    @Test
//    void releaseCheque_ShouldReleaseSuccessfully() throws Exception {
//        doNothing().when(service).releaseCheque(1L, "John Doe", "+971501234567");
//
//        mockMvc.perform(post("/api/v1/bank-payment-vouchers/1/release-cheque")
//                        .param("documentId", "400-107")
//                        .param("actionRequested", "RELEASE")
//                        .param("releasedTo", "John Doe")
//                        .param("contact", "+971501234567"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)));
//
//        verify(service).releaseCheque(1L, "John Doe", "+971501234567");
//    }
//
//    @Test
//    void unReleaseCheque_ShouldUnReleaseSuccessfully() throws Exception {
//        doNothing().when(service).unReleaseCheque(1L);
//
//        mockMvc.perform(post("/api/v1/bank-payment-vouchers/1/unrelease-cheque")
//                        .param("documentId", "400-107")
//                        .param("actionRequested", "UNRELEASE"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)));
//
//        verify(service).unReleaseCheque(1L);
//    }
//
//    @Test
//    void resetChequeStatus_ShouldResetSuccessfully() throws Exception {
//        doNothing().when(service).resetChequeStatus(1L);
//
//        mockMvc.perform(post("/api/v1/bank-payment-vouchers/1/reset-cheque-status")
//                        .param("documentId", "400-107")
//                        .param("actionRequested", "RESET"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)));
//
//        verify(service).resetChequeStatus(1L);
//    }
//
//    @Test
//    void revertReconciliation_ShouldRevertSuccessfully() throws Exception {
//        doNothing().when(service).revertReconciliation(1L, "Test comment");
//
//        mockMvc.perform(post("/api/v1/bank-payment-vouchers/1/revert-reconciliation")
//                        .param("documentId", "400-107")
//                        .param("actionRequested", "REVERT")
//                        .param("comments", "Test comment"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)));
//
//        verify(service).revertReconciliation(1L, "Test comment");
//    }
//}
//
