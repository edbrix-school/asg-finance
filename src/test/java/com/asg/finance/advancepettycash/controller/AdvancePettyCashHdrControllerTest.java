package com.asg.finance.advancepettycash.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.controller.AdvancePettyCashHdrController;
import com.asg.finance.dto.AdvancePettyCashHdrRequestDTO;
import com.asg.finance.dto.AdvancePettyCashHdrResponseDTO;
import com.asg.finance.service.AdvancePettyCashHdrService;
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
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AdvancePettyCashHdrController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.asg.finance.aspect.*"))
@ContextConfiguration(classes = {AdvancePettyCashHdrController.class})
class AdvancePettyCashHdrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdvancePettyCashHdrService service;

    @MockBean
    private LoggingService loggingService;

    private AdvancePettyCashHdrRequestDTO requestDTO;
    private AdvancePettyCashHdrResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = AdvancePettyCashHdrRequestDTO.builder()
                .transactionDate(LocalDate.now())
                .pettyCashGlPoid(1L)
                .payingTo("John Doe")
                .iouAmount(BigDecimal.valueOf(1000))
                .narration("Test narration")
                .status("OPEN")
                .build();

        responseDTO = AdvancePettyCashHdrResponseDTO.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.now())
                .docRef("DOC-001")
                .pettyCashGlPoid(1L)
                .payingTo("John Doe")
                .iouAmount(BigDecimal.valueOf(1000))
                .settledAmount(BigDecimal.ZERO)
                .balanceAmount(BigDecimal.valueOf(1000))
                .narration("Test narration")
                .status("OPEN")
                .build();
    }

    @Test
    void createAdvancePettyCash_Success() throws Exception {
        when(service.createAdvancePettyCash(any(AdvancePettyCashHdrRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/v1/advance-petty-cash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.transactionPoid").value(1L))
                .andExpect(jsonPath("$.result.data.payingTo").value("John Doe"));

        verify(service, times(1)).createAdvancePettyCash(any(AdvancePettyCashHdrRequestDTO.class));
    }

    @Test
    void updateAdvancePettyCash_Success() throws Exception {
        when(service.updateAdvancePettyCash(eq(1L), any(AdvancePettyCashHdrRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/v1/advance-petty-cash/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.transactionPoid").value(1L));

        verify(service, times(1)).updateAdvancePettyCash(eq(1L), any(AdvancePettyCashHdrRequestDTO.class));
    }

    @Test
    void getAdvancePettyCashById_Success() throws Exception {
        when(service.getAdvancePettyCashById(1L)).thenReturn(responseDTO);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            mockMvc.perform(get("/v1/advance-petty-cash/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(1L))
                    .andExpect(jsonPath("$.result.data.payingTo").value("John Doe"));

            verify(service, times(1)).getAdvancePettyCashById(1L);
            verify(loggingService, times(1)).createLogSummaryEntry(any(LogDetailsEnum.class), eq("DOC123"), eq("1"));
        }
    }

    @Test
    void softDeleteAdvancePettyCash_Success() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Test deletion");

        doNothing().when(service).softDeleteAdvancePettyCash(eq(1L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/advance-petty-cash/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk());

        verify(service, times(1)).softDeleteAdvancePettyCash(eq(1L), any(DeleteReasonDto.class));
    }

    @Test
    void softDeleteAdvancePettyCash_WithoutDeleteReason() throws Exception {
        doNothing().when(service).softDeleteAdvancePettyCash(eq(1L), isNull());

        mockMvc.perform(delete("/v1/advance-petty-cash/1"))
                .andExpect(status().isOk());

        verify(service, times(1)).softDeleteAdvancePettyCash(eq(1L), isNull());
    }

    @Test
    void listAdvancePettyCash_Success() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("content", responseDTO);
        data.put("totalElements", 1);

        when(service.listAdvancePettyCash(anyString(), any(), any(Pageable.class), any(), any())).thenReturn(data);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            mockMvc.perform(post("/v1/advance-petty-cash/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.totalElements").value(1));

            verify(service, times(1)).listAdvancePettyCash(eq("DOC123"), any(), any(Pageable.class), isNull(), isNull());
        }
    }

    @Test
    void listAdvancePettyCash_WithDateRange() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("content", responseDTO);

        when(service.listAdvancePettyCash(anyString(), any(), any(Pageable.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(data);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            mockMvc.perform(post("/v1/advance-petty-cash/list")
                            .param("periodFrom", "2024-01-01")
                            .param("periodTo", "2024-12-31")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());

            verify(service, times(1)).listAdvancePettyCash(eq("DOC123"), isNull(), any(Pageable.class), any(LocalDate.class), any(LocalDate.class));
        }
    }

    @Test
    void listAdvancePettyCash_Exception() throws Exception {
        when(service.listAdvancePettyCash(anyString(), any(), any(Pageable.class), any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            mockMvc.perform(post("/v1/advance-petty-cash/list")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isInternalServerError());
        }
    }
}
