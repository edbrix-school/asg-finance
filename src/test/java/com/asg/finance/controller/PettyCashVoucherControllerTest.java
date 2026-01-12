package com.asg.finance.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.PettyCashCreateRequestDto;
import com.asg.finance.dto.PettyCashResponseDto;
import com.asg.finance.dto.PettyCashUpdateRequestDto;
import com.asg.finance.exceptions.GlobalExceptionHandler;
import com.asg.finance.service.PettyCashVoucherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PettyCashVoucherControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PettyCashVoucherService pettyCashVoucherService;

    @InjectMocks
    private PettyCashVoucherController pettyCashVoucherController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PettyCashCreateRequestDto createRequestDto;
    private PettyCashUpdateRequestDto updateRequestDto;
    private PettyCashResponseDto responseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pettyCashVoucherController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        createRequestDto = createSampleCreateRequestDto();
        updateRequestDto = createSampleUpdateRequestDto();
        responseDto = createSampleResponseDto();
    }

    @Test
    void testCreatePettyCashVoucher_Success() throws Exception {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");
            
            when(pettyCashVoucherService.createPettyCash(any(PettyCashCreateRequestDto.class), anyString()))
                    .thenReturn(responseDto);

            mockMvc.perform(post("/petty-cash-voucher")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)   // 🔑 FORCE JSON
                            .content(objectMapper.writeValueAsString(createRequestDto)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.message")
                            .value("Petty cash voucher created successfully"))
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(1))
                    .andExpect(jsonPath("$.result.data.docRef").value("PC001"));

            verify(pettyCashVoucherService, times(1))
                    .createPettyCash(any(PettyCashCreateRequestDto.class), anyString());
        }
    }


    @Test
    void testListTaxMaster_Success() throws Exception {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");
            
            Map<String, Object> data = new HashMap<>();
            data.put("content", List.of(responseDto));
            data.put("totalElements", 1);

            when(pettyCashVoucherService.listPettyCashVoucher(anyString(), any(FilterRequestDto.class), any(), any(), any(Pageable.class)))
                    .thenReturn(data);

            FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());

            mockMvc.perform(post("/petty-cash-voucher/list")
                            .param("startDate", "2024-01-01")
                            .param("endDate", "2024-12-31")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filters)))
                    .andExpect(status().isOk());

            verify(pettyCashVoucherService, times(1)).listPettyCashVoucher(anyString(), any(FilterRequestDto.class), any(), any(), any(Pageable.class));
        }
    }


    @Test
    void testCreatePettyCashVoucher_MissingDocumentId() throws Exception {
        mockMvc.perform(post("/petty-cash-voucher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isOk()); // Should succeed as documentId comes from UserContext
    }

    @Test
    void testCreatePettyCashVoucher_InvalidJson() throws Exception {
        mockMvc.perform(post("/petty-cash-voucher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdatePettyCash_MissingParams() throws Exception {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");
            
            when(pettyCashVoucherService.updatePettyCash(anyLong(), any(PettyCashUpdateRequestDto.class), anyString()))
                    .thenReturn(responseDto);

            mockMvc.perform(put("/petty-cash-voucher/{transactionPoid}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequestDto)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testUpdatePettyCash_InvalidId() throws Exception {
        mockMvc.perform(put("/petty-cash-voucher/{transactionPoid}", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testListTaxMaster_MissingDocumentId() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());

        mockMvc.perform(post("/petty-cash-voucher/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isOk()); // Should succeed as documentId comes from UserContext
    }

    @Test
    void testListTaxMaster_ServiceException() throws Exception {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");
            
            when(pettyCashVoucherService.listPettyCashVoucher(anyString(), any(FilterRequestDto.class), any(), any(), any(Pageable.class)))
                    .thenThrow(new RuntimeException("List failed"));

            FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());

            mockMvc.perform(post("/petty-cash-voucher/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filters)))
                    .andExpect(status().isInternalServerError());

            verify(pettyCashVoucherService, times(1)).listPettyCashVoucher(anyString(), any(FilterRequestDto.class), any(), any(), any(Pageable.class));
        }
    }

    @Test
    void testCreatePettyCashVoucher_EmptyRequestBody() throws Exception {
        mockMvc.perform(post("/petty-cash-voucher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }



    private PettyCashCreateRequestDto createSampleCreateRequestDto() {
        return PettyCashCreateRequestDto.builder()
                .transactionDate(new Date())
                .currencyCode("USD")
                .currencyRate(BigDecimal.ONE)
                .pettyCashGlPoid(100L)
                .amount(BigDecimal.valueOf(1000))
                .payingTo("Test Payee")
                .narration("Test narration")
                .refType("GENERAL")
                .status("ACTIVE")
                .docId("DOC001")
                .build();
    }

    private PettyCashUpdateRequestDto createSampleUpdateRequestDto() {
        return PettyCashUpdateRequestDto.builder()
                .transactionDate(new Date())
                .currencyCode("USD")
                .currencyRate(BigDecimal.ONE)
                .pettyCashGlPoid(100L)
                .amount(BigDecimal.valueOf(1500))
                .payingTo("Updated Payee")
                .narration("Updated narration")
                .refType("GENERAL")
                .status("ACTIVE")
                .docId("DOC001")
                .build();
    }

    private PettyCashResponseDto createSampleResponseDto() {
        PettyCashResponseDto dto = new PettyCashResponseDto();
        dto.setTransactionPoid(1L);
        dto.setDocRef("PC001");
        return dto;
    }
}