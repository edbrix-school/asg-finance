package com.asg.finance.telexfilegenerate.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.controller.TelexFileGenerateController;
import com.asg.finance.dto.TelexFileDtlDto;
import com.asg.finance.dto.TelexFileGenerateRequestDto;
import com.asg.finance.dto.TelexFileGenerateResponseDto;
import com.asg.finance.service.TelexFileGenerateService;
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
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TelexFileGenerateController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.asg.finance.aspect.*"))
@ContextConfiguration(classes = {TelexFileGenerateController.class})
class TelexFileGenerateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TelexFileGenerateService service;

    @MockBean
    private LoggingService loggingService;

    private TelexFileGenerateRequestDto requestDto;
    private TelexFileGenerateResponseDto responseDto;
    private List<TelexFileDtlDto> detailsList;

    @BeforeEach
    void setUp() {
        TelexFileDtlDto detail = TelexFileDtlDto.builder()
                .detRowId(1L)
                .debitTransactionPoid(1001L)
                .debitTransactionDate(LocalDate.of(2026, 1, 28))
                .debitCompanyPoid(1L)
                .debitDocRef("BD-2025-001")
                .debitPayingToName("ABC Suppliers Ltd")
                .debitPayingType("1")
                .debitLongNarration("Payment for invoice")
                .debitTtDate(LocalDate.of(2026, 1, 29))
                .debitCurrencyCode("USD")
                .debitCurrencyRate(new BigDecimal("3.75"))
                .debitCurrencyAmt(new BigDecimal("10000.00"))
                .debitAmount(new BigDecimal("37500.00"))
                .deleted("N")
                .selected("N")
                .drilldownLinkInfo("TARGET_DOC_ID=400-111,DOC_KEY_POID=1001")
                .debitTtChargeType("OUR")
                .build();

        detailsList = Arrays.asList(detail);

        requestDto = TelexFileGenerateRequestDto.builder()
                .bankPoid(1L)
                .bankList("Y")
                .transactionDate(LocalDate.of(2026, 1, 29))
                .remarks("Test remarks")
                .approvalOnly(false)
                .suppressBalanceCheck(false)
                .details(detailsList)
                .build();

        responseDto = TelexFileGenerateResponseDto.builder()
                .transactionPoid(42418L)
                .transactionDate(LocalDate.of(2026, 1, 29))
                .groupPoid(1L)
                .companyPoid(1L)
                .docRef("ASGFILE3378")
                .bankPoid(1L)
                .bankList("Y")
                .remarks("Test remarks")
                .approvalOnly(false)
                .suppressBalanceCheck(false)
                .createdBy("HEXAUSER1")
                .createdDate(LocalDateTime.now())
                .lastModifiedBy("HEXAUSER1")
                .lastModifiedDate(LocalDateTime.now())
                .details(detailsList)
                .build();
    }

    @Test
    void create_Success() throws Exception {
        when(service.createTelexFile(any(TelexFileGenerateRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/v1/telex-file-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.transactionPoid").value(42418L))
                .andExpect(jsonPath("$.result.data.docRef").value("ASGFILE3378"));

        verify(service, times(1)).createTelexFile(any(TelexFileGenerateRequestDto.class));
    }

    @Test
    void create_Exception() throws Exception {
        when(service.createTelexFile(any(TelexFileGenerateRequestDto.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/v1/telex-file-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError());

        verify(service, times(1)).createTelexFile(any(TelexFileGenerateRequestDto.class));
    }

    @Test
    void update_Success() throws Exception {
        when(service.updateTelexFile(eq(42418L), any(TelexFileGenerateRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/v1/telex-file-generate/42418")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.transactionPoid").value(42418L));

        verify(service, times(1)).updateTelexFile(eq(42418L), any(TelexFileGenerateRequestDto.class));
    }

    @Test
    void update_Exception() throws Exception {
        when(service.updateTelexFile(eq(42418L), any(TelexFileGenerateRequestDto.class)))
                .thenThrow(new RuntimeException("Update failed"));

        mockMvc.perform(put("/v1/telex-file-generate/42418")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError());

        verify(service, times(1)).updateTelexFile(eq(42418L), any(TelexFileGenerateRequestDto.class));
    }

    @Test
    void getById_Success() throws Exception {
        when(service.getTelexFileById(42418L)).thenReturn(responseDto);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");

            mockMvc.perform(get("/v1/telex-file-generate/42418"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(42418L))
                    .andExpect(jsonPath("$.result.data.docRef").value("ASGFILE3378"));

            verify(service, times(1)).getTelexFileById(42418L);
            verify(loggingService, times(1)).createLogSummaryEntry(
                    eq(LogDetailsEnum.VIEWED), eq("100-153"), eq("42418"));
        }
    }

    @Test
    void softDelete_Success() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Test deletion");

        doNothing().when(service).softDeleteTelexFile(eq(42418L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/telex-file-generate/42418")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk());

        verify(service, times(1)).softDeleteTelexFile(eq(42418L), any(DeleteReasonDto.class));
    }

    @Test
    void softDelete_WithoutDeleteReason() throws Exception {
        doNothing().when(service).softDeleteTelexFile(eq(42418L), isNull());

        mockMvc.perform(delete("/v1/telex-file-generate/42418"))
                .andExpect(status().isOk());

        verify(service, times(1)).softDeleteTelexFile(eq(42418L), isNull());
    }

    @Test
    void list_Success() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("content", Collections.singletonList(responseDto));
        data.put("totalElements", 1);

        when(service.listTelexFiles(anyString(), any(), any(), any(), any(Pageable.class))).thenReturn(data);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");

            mockMvc.perform(post("/v1/telex-file-generate/list")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.totalElements").value(1));

            verify(service, times(1)).listTelexFiles(eq("100-153"), isNull(), isNull(), isNull(), any(Pageable.class));
        }
    }

    @Test
    void list_WithDateRange() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("content", Collections.singletonList(responseDto));

        when(service.listTelexFiles(anyString(), any(), any(), any(), any(Pageable.class))).thenReturn(data);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");

            mockMvc.perform(post("/v1/telex-file-generate/list")
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-31")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());

            verify(service, times(1)).listTelexFiles(
                    eq("100-153"), isNull(),
                    eq(LocalDate.of(2026, 1, 1)),
                    eq(LocalDate.of(2026, 1, 31)),
                    any(Pageable.class));
        }
    }

    @Test
    void list_InvalidDateRange_OnlyStartDate() throws Exception {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");

            mockMvc.perform(post("/v1/telex-file-generate/list")
                            .param("startDate", "2026-01-01")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isInternalServerError());

            verify(service, never()).listTelexFiles(anyString(), any(), any(), any(), any(Pageable.class));
        }
    }

    @Test
    void list_InvalidDateRange_OnlyEndDate() throws Exception {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");

            mockMvc.perform(post("/v1/telex-file-generate/list")
                            .param("endDate", "2026-01-31")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isInternalServerError());

            verify(service, never()).listTelexFiles(anyString(), any(), any(), any(), any(Pageable.class));
        }
    }

    @Test
    void list_Exception() throws Exception {
        when(service.listTelexFiles(anyString(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-153");

            mockMvc.perform(post("/v1/telex-file-generate/list")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Test
    void loadTelexData_Success() throws Exception {
        when(service.loadTelexTransferData("Y")).thenReturn(detailsList);

        mockMvc.perform(get("/v1/telex-file-generate/load-telex-data")
                        .param("bankList", "Y"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data").isArray());

        verify(service, times(1)).loadTelexTransferData("Y");
    }

    @Test
    void loadTelexData_DefaultBankList() throws Exception {
        when(service.loadTelexTransferData("Y")).thenReturn(detailsList);

        mockMvc.perform(get("/v1/telex-file-generate/load-telex-data"))
                .andExpect(status().isOk());

        verify(service, times(1)).loadTelexTransferData("Y");
    }

    @Test
    void loadTelexData_Exception() throws Exception {
        when(service.loadTelexTransferData("Y")).thenThrow(new RuntimeException("Load failed"));

        mockMvc.perform(get("/v1/telex-file-generate/load-telex-data")
                        .param("bankList", "Y"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void regenerateTelexFile_WithTelexPoid_Success() throws Exception {
        when(service.regenerateTelexFile(42418L, 1001L)).thenReturn("SUCCESS : Bank telex file removed");

        mockMvc.perform(post("/v1/telex-file-generate/42418/regenerate/1001"))
                .andExpect(status().isOk());

        verify(service, times(1)).regenerateTelexFile(42418L, 1001L);
    }

    @Test
    void regenerateTelexFile_WithTelexPoid_Error() throws Exception {
        when(service.regenerateTelexFile(42418L, 1001L)).thenReturn("ERROR: Failed to regenerate");

        mockMvc.perform(post("/v1/telex-file-generate/42418/regenerate/1001"))
                .andExpect(status().isInternalServerError());

        verify(service, times(1)).regenerateTelexFile(42418L, 1001L);
    }

    @Test
    void regenerateTelexFile_WithTelexPoid_Exception() throws Exception {
        when(service.regenerateTelexFile(42418L, 1001L)).thenThrow(new RuntimeException("Exception occurred"));

        mockMvc.perform(post("/v1/telex-file-generate/42418/regenerate/1001"))
                .andExpect(status().isInternalServerError());

        verify(service, times(1)).regenerateTelexFile(42418L, 1001L);
    }

    @Test
    void generateBankFileButton_Success() throws Exception {
        when(service.generateBankFileButton(42418L)).thenReturn("SUCCESS");

        mockMvc.perform(post("/v1/telex-file-generate/42418/generate"))
                .andExpect(status().isOk());

        verify(service, times(1)).generateBankFileButton(42418L);
    }

    @Test
    void generateBankFileButton_Error() throws Exception {
        when(service.generateBankFileButton(42418L)).thenReturn("ERROR: Generation failed");

        mockMvc.perform(post("/v1/telex-file-generate/42418/generate"))
                .andExpect(status().isInternalServerError());

        verify(service, times(1)).generateBankFileButton(42418L);
    }

    @Test
    void generateBankFileButton_Exception() throws Exception {
        when(service.generateBankFileButton(42418L)).thenThrow(new RuntimeException("Exception"));

        mockMvc.perform(post("/v1/telex-file-generate/42418/generate"))
                .andExpect(status().isInternalServerError());

        verify(service, times(1)).generateBankFileButton(42418L);
    }

    @Test
    void checkBankBalance_Success() throws Exception {
        when(service.checkBankBalance(42418L)).thenReturn("Balance check passed");

        mockMvc.perform(get("/v1/telex-file-generate/42418/check-balance"))
                .andExpect(status().isOk());

        verify(service, times(1)).checkBankBalance(42418L);
    }

    @Test
    void checkBankBalance_Exception() throws Exception {
        when(service.checkBankBalance(42418L)).thenThrow(new RuntimeException("Balance check failed"));

        mockMvc.perform(get("/v1/telex-file-generate/42418/check-balance"))
                .andExpect(status().isInternalServerError());

        verify(service, times(1)).checkBankBalance(42418L);
    }

}
