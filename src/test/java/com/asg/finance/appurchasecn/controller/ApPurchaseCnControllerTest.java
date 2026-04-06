package com.asg.finance.appurchasecn.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.controller.ApPurchaseCnController;
import com.asg.finance.dto.ApPurchaseCnGlDtlDto;
import com.asg.finance.dto.ApPurchaseCnHdrDto;
import com.asg.finance.service.ApPurchaseCnService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ApPurchaseCnController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.asg.finance.aspect.*"))
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {ApPurchaseCnController.class, com.asg.finance.exceptions.GlobalExceptionHandler.class})
class ApPurchaseCnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApPurchaseCnService service;

    @MockBean
    private LoggingService loggingService;

    private ApPurchaseCnHdrDto headerDto;

    @BeforeEach
    void setUp() {
        ApPurchaseCnGlDtlDto glDtlDto = new ApPurchaseCnGlDtlDto();
        glDtlDto.setDetRowId(1L);
        glDtlDto.setGlPoid(200L);
        glDtlDto.setCompanyPoid(1L);
        glDtlDto.setType("DR");
        glDtlDto.setDrAmount(new BigDecimal("100.00"));
        glDtlDto.setCrAmount(BigDecimal.ZERO);
        glDtlDto.setTotalAmount(new BigDecimal("100.00"));

        headerDto = new ApPurchaseCnHdrDto();
        headerDto.setTransactionPoid(10L);
        headerDto.setTransactionDate(LocalDate.of(2026, 4, 3));
        headerDto.setPartyType("SUPPLIER");
        headerDto.setSupplierPoid(101L);
        headerDto.setCurrencyCode("BHD");
        headerDto.setCurrencyRate(BigDecimal.ONE);
        headerDto.setSupplierCnAmount(new BigDecimal("100.00"));
        headerDto.setRefType("GENERAL");
        headerDto.setNarration("Narration");
        headerDto.setGlDetails(List.of(glDtlDto));
    }

    @Test
    void create_Success() throws Exception {
        when(service.create(any(ApPurchaseCnHdrDto.class))).thenReturn(headerDto);

        mockMvc.perform(post("/v1/supplier-credit-note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(headerDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.transactionPoid").value(10L));
    }

    @Test
    void create_ValidationException() throws Exception {
        when(service.create(any(ApPurchaseCnHdrDto.class))).thenThrow(new ValidationException("invalid"));

        mockMvc.perform(post("/v1/supplier-credit-note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(headerDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_Success() throws Exception {
        when(service.getById(10L)).thenReturn(headerDto);

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getDocumentId).thenReturn("200-103");

            mockMvc.perform(get("/v1/supplier-credit-note/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(10L));
        }

        verify(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), eq("200-103"), eq("10"));
    }

    @Test
    void list_Success() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("totalElements", 1);

        when(service.list(anyString(), any(), any(), any(), any(Pageable.class))).thenReturn(result);

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getDocumentId).thenReturn("200-103");

            mockMvc.perform(post("/v1/supplier-credit-note/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new FilterRequestDto("AND", "N", List.of()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.totalElements").value(1));
        }
    }

    @Test
    void list_InvalidDateRange() throws Exception {
        mockMvc.perform(post("/v1/supplier-credit-note/list")
                        .param("startDate", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Both startDate and endDate")));
    }

    @Test
    void list_Exception() throws Exception {
        when(service.list(anyString(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("failure"));

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getDocumentId).thenReturn("200-103");

            mockMvc.perform(post("/v1/supplier-credit-note/list"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value(containsString("Failed to fetch list")));
        }
    }

    @Test
    void update_Success() throws Exception {
        when(service.update(eq(10L), any(ApPurchaseCnHdrDto.class))).thenReturn(headerDto);

        mockMvc.perform(put("/v1/supplier-credit-note/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(headerDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.transactionPoid").value(10L));
    }

    @Test
    void delete_Success() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("test");
        doNothing().when(service).delete(eq(10L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/supplier-credit-note/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk());
    }

    @Test
    void getPjRefDetails_Success() throws Exception {
        when(service.getPjRefDetails(55L)).thenReturn(Map.of("pjRefType", "GENERAL"));

        mockMvc.perform(get("/v1/supplier-credit-note/pj-ref-details/55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.pjRefType").value("GENERAL"));
    }

    @Test
    void getPartyDetails_Success() throws Exception {
        when(service.getPartyDetails("SUPPLIER", 101L)).thenReturn(Map.of("NAME", "ABC"));

        mockMvc.perform(get("/v1/supplier-credit-note/party-details/SUPPLIER/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.NAME").value("ABC"));
    }

    /*@Test
    void print_Success() throws Exception {
        byte[] pdf = "pdf".getBytes();
        when(service.print(10L)).thenReturn(pdf);

        mockMvc.perform(get("/v1/supplier-credit-note/print/10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("purchase-journal-10.pdf")));
    }*/

    /*@Test
    void print_Exception() throws Exception {
        when(service.print(anyLong())).thenThrow(new RuntimeException("print failure"));

        mockMvc.perform(get("/v1/supplier-credit-note/print/10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(containsString("Failed to generate PDF")));
    }*/

    @Test
    void delete_ServiceValidationException() throws Exception {
        doThrow(new ValidationException("delete validation")).when(service).delete(eq(10L), any());

        mockMvc.perform(delete("/v1/supplier-credit-note/10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_ServiceException() throws Exception {
        when(service.getById(10L)).thenThrow(new RuntimeException("failure"));

        mockMvc.perform(get("/v1/supplier-credit-note/10"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void create_InvalidJson() throws Exception {
        mockMvc.perform(post("/v1/supplier-credit-note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_ValidationException() throws Exception {
        when(service.update(eq(10L), any(ApPurchaseCnHdrDto.class)))
                .thenThrow(new ValidationException("update validation"));

        mockMvc.perform(put("/v1/supplier-credit-note/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(headerDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_ServiceException() throws Exception {
        when(service.update(eq(10L), any(ApPurchaseCnHdrDto.class)))
                .thenThrow(new RuntimeException("update failure"));

        mockMvc.perform(put("/v1/supplier-credit-note/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(headerDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getPjRefDetails_ServiceException() throws Exception {
        when(service.getPjRefDetails(55L)).thenThrow(new RuntimeException("pj ref failure"));

        mockMvc.perform(get("/v1/supplier-credit-note/pj-ref-details/55"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getPartyDetails_ServiceException() throws Exception {
        when(service.getPartyDetails("SUPPLIER", 101L))
                .thenThrow(new RuntimeException("party details failure"));

        mockMvc.perform(get("/v1/supplier-credit-note/party-details/SUPPLIER/101"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void delete_ServiceException() throws Exception {
        doThrow(new RuntimeException("delete failure")).when(service).delete(eq(10L), any());

        mockMvc.perform(delete("/v1/supplier-credit-note/10"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void list_WithDateRange_Success() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("totalElements", 1);

        when(service.list(anyString(), any(), any(), any(), any(Pageable.class))).thenReturn(result);

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getDocumentId).thenReturn("200-103");

            mockMvc.perform(post("/v1/supplier-credit-note/list")
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-31")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new FilterRequestDto("AND", "N", List.of()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.totalElements").value(1));
        }
    }

    @Test
    void list_OnlyStartDate() throws Exception {
        mockMvc.perform(post("/v1/supplier-credit-note/list")
                        .param("startDate", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Both startDate and endDate")));
    }

    @Test
    void list_OnlyEndDate() throws Exception {
        mockMvc.perform(post("/v1/supplier-credit-note/list")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Both startDate and endDate")));
    }

    @Test
    void create_NullRequestBody() throws Exception {
        mockMvc.perform(post("/v1/supplier-credit-note")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_NullRequestBody() throws Exception {
        mockMvc.perform(put("/v1/supplier-credit-note/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_WithDeleteReason() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Business requirement");
        doNothing().when(service).delete(eq(10L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/supplier-credit-note/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk());
    }

    @Test
    void getById_WithLogging() throws Exception {
        when(service.getById(10L)).thenReturn(headerDto);

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getDocumentId).thenReturn("200-103");

            mockMvc.perform(get("/v1/supplier-credit-note/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(10L));
        }

        verify(loggingService).createLogSummaryEntry(
                any(com.asg.common.lib.enums.LogDetailsEnum.class),
                eq("200-103"),
                eq("10")
        );
    }

    @Test
    void list_EmptyFilters() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("totalElements", 0);

        when(service.list(anyString(), any(), any(), any(), any(Pageable.class))).thenReturn(result);

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getDocumentId).thenReturn("200-103");

            mockMvc.perform(post("/v1/supplier-credit-note/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.totalElements").value(0));
        }
    }

    @Test
    void getPjRefDetails_NotFound() throws Exception {
        when(service.getPjRefDetails(999L))
                .thenThrow(new com.asg.common.lib.exception.ResourceNotFoundException("PJ Reference", "pjPoid", 999L));

        mockMvc.perform(get("/v1/supplier-credit-note/pj-ref-details/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPartyDetails_NotFound() throws Exception {
        when(service.getPartyDetails("SUPPLIER", 999L))
                .thenThrow(new com.asg.common.lib.exception.ResourceNotFoundException("Party", "partyPoid", 999L));

        mockMvc.perform(get("/v1/supplier-credit-note/party-details/SUPPLIER/999"))
                .andExpect(status().isNotFound());
    }
}
