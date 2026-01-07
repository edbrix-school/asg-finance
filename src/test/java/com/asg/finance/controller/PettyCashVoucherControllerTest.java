//package com.asg.finance.controller;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.finance.dto.PettyCashRequestDto;
//import com.asg.finance.dto.PettyCashResponseDto;
//import com.asg.finance.exceptions.GlobalExceptionHandler;
//import com.asg.finance.service.PettyCashVoucherService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.math.BigDecimal;
//import java.util.*;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(MockitoExtension.class)
//class PettyCashVoucherControllerTest {
//
//    private MockMvc mockMvc;
//
//    @Mock
//    private PettyCashVoucherService pettyCashVoucherService;
//
//    @InjectMocks
//    private PettyCashVoucherController pettyCashVoucherController;
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    private PettyCashRequestDto requestDto;
//    private PettyCashResponseDto responseDto;
//
//    @BeforeEach
//    void setUp() {
//        mockMvc = MockMvcBuilders.standaloneSetup(pettyCashVoucherController)
//                .setControllerAdvice(new GlobalExceptionHandler())
//                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
//                .build();
//
//        requestDto = createSampleRequestDto();
//        responseDto = createSampleResponseDto();
//    }
//
//    @Test
//    void testCreatePettyCashVoucher_Success() throws Exception {
//        PettyCashRequestDto requestDto = PettyCashRequestDto.builder()
//                .transactionDate(new Date())
//                .pettyCashGlPoid(100L)
//                .amount(BigDecimal.TEN)
//                .payingTo("John Doe")
//                .refType("REFERENCE_TYPE")
//                .status("ACTIVE")
//                .build();
//
//        when(pettyCashVoucherService.createPettyCash(any(), any()))
//                .thenReturn(responseDto);
//
//        mockMvc.perform(post("/v1/petty-cash-voucher")
//                        .header("groupPoid", "1")
//                        .header("companyPoid", "1")
//                        .header("userPoid", "1")
//                        .param("documentId", "DOC001")
//                        .param("actionRequested", "create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.result.data.transactionPoid").value(1))
//                .andExpect(jsonPath("$.result.data.docRef").value("PC001"));
//
//        verify(pettyCashVoucherService, times(1))
//                .createPettyCash(any(), any());
//    }
//
//
//    @Test
//    void testListTaxMaster_Success() throws Exception {
//        Map<String, Object> data = new HashMap<>();
//        data.put("content", List.of(responseDto));
//        data.put("totalElements", 1);
//
//        when(pettyCashVoucherService.listPettyCashVoucher(anyString(), any(FilterRequestDto.class), any(), any(), any(Pageable.class)))
//                .thenReturn(data);
//
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());
//
//        mockMvc.perform(post("/v1/petty-cash-voucher/list")
//                        .param("documentId", "DOC001")
//                        .param("actionRequested", "VIEW")
//                        .param("startDate", "2024-01-01")
//                        .param("endDate", "2024-12-31")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(filters)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.result.data.totalElements").value(1));
//
//        verify(pettyCashVoucherService, times(1)).listPettyCashVoucher(anyString(), any(FilterRequestDto.class), any(), any(), any(Pageable.class));
//    }
//
//
//    @Test
//    void testCreatePettyCashVoucher_MissingDocumentId() throws Exception {
//        mockMvc.perform(post("/v1/petty-cash-voucher")
//                        .header("groupPoid", "1")
//                        .header("companyPoid", "1")
//                        .header("userPoid", "1")
//                        .param("actionRequested", "create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testCreatePettyCashVoucher_InvalidJson() throws Exception {
//        mockMvc.perform(post("/v1/petty-cash-voucher")
//                        .header("groupPoid", "1")
//                        .header("companyPoid", "1")
//                        .header("userPoid", "1")
//                        .param("documentId", "DOC001")
//                        .param("actionRequested", "create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{invalid json}"))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testUpdatePettyCash_MissingParams() throws Exception {
//        mockMvc.perform(put("/v1/petty-cash-voucher/{transactionPoid}", 1L)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testUpdatePettyCash_InvalidId() throws Exception {
//        mockMvc.perform(put("/v1/petty-cash-voucher/{transactionPoid}", "invalid")
//                        .param("loginGroupPoid", "1")
//                        .param("loginCompanyPoid", "1")
//                        .param("loginUserPoid", "1")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testListTaxMaster_MissingDocumentId() throws Exception {
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());
//
//        mockMvc.perform(post("/v1/petty-cash-voucher/list")
//                        .param("actionRequested", "VIEW")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(filters)))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testListTaxMaster_ServiceException() throws Exception {
//        when(pettyCashVoucherService.listPettyCashVoucher(anyString(), any(FilterRequestDto.class), any(), any(), any(Pageable.class)))
//                .thenThrow(new RuntimeException("List failed"));
//
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());
//
//        mockMvc.perform(post("/v1/petty-cash-voucher/list")
//                        .param("documentId", "DOC001")
//                        .param("actionRequested", "VIEW")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(filters)))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success").value(false));
//
//        verify(pettyCashVoucherService, times(1)).listPettyCashVoucher(anyString(), any(FilterRequestDto.class), any(), any(), any(Pageable.class));
//    }
//
//    @Test
//    void testCreatePettyCashVoucher_EmptyRequestBody() throws Exception {
//        mockMvc.perform(post("/v1/petty-cash-voucher")
//                        .header("groupPoid", "1")
//                        .header("companyPoid", "1")
//                        .header("userPoid", "1")
//                        .param("documentId", "DOC001")
//                        .param("actionRequested", "create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(""))
//                .andExpect(status().isBadRequest());
//    }
//
//
//
//    private PettyCashRequestDto createSampleRequestDto() {
//        PettyCashRequestDto dto = new PettyCashRequestDto();
//        dto.setTransactionDate(new Date());
//        dto.setCurrencyCode("USD");
//        dto.setCurrencyRate(BigDecimal.ONE);
//        dto.setAmount(BigDecimal.valueOf(1000));
//        dto.setPayingTo("Test Payee");
//        dto.setNarration("Test narration");
//        dto.setRefType("GENERAL");
//        dto.setDocId("DOC001");
//        dto.setGlPettyCashPaymentDtlRequestDtos(Collections.emptyList());
//        dto.setGlPettyCashChargeDtlRequestDtos(Collections.emptyList());
//        dto.setGlPettyCashItemDtlRequestDtos(Collections.emptyList());
//        return dto;
//    }
//
//    private PettyCashResponseDto createSampleResponseDto() {
//        return PettyCashResponseDto.builder()
//                .transactionPoid(1L)
//                .docRef("PC001")
//                .transactionDate(new Date())
//                .currencyCode("USD")
//                .amount(BigDecimal.valueOf(1000))
//                .payingTo("Test Payee")
//                .refType("GENERAL")
//                .paymentDtls(Collections.emptyList())
//                .chargeDtls(Collections.emptyList())
//                .itemDtls(Collections.emptyList())
//                .build();
//    }
//}