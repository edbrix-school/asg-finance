package com.asg.finance.bankreconciliation.controller;

import com.asg.common.lib.dto.excel.ExcelFileData;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.ExcelExportService;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.controller.BankReconciliationController;
import com.asg.finance.dto.*;
import com.asg.finance.service.BankReconciliationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BankReconciliationControllerTest {

    @Mock
    private BankReconciliationService service;

    @Mock
    private LoggingService loggingService;

    @Mock
    private ExcelExportService excelExportService;

    @InjectMocks
    private BankReconciliationController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private MockedStatic<UserContext> userContextMock;

    private static final String BASE_URL = "/v1/bank-reconciliation";
    private static final String DOC_ID = "400-150";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(objectMapper);

        ByteArrayHttpMessageConverter byteConverter = new ByteArrayHttpMessageConverter();
        byteConverter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_PDF,
                MediaType.APPLICATION_OCTET_STREAM,
                MediaType.ALL));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(jsonConverter, byteConverter)
                .build();
        userContextMock = Mockito.mockStatic(UserContext.class);
        userContextMock.when(UserContext::getDocumentId).thenReturn(DOC_ID);
        userContextMock.when(UserContext::getCompanyPoid).thenReturn(101L);
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    // ── getReconciliationView ──────────────────────────────────────────────────

    @Test
    void getReconciliationView_success() throws Exception {
        when(service.getReconciliationView(anyLong(), anyLong(), anyLong(),
                any(LocalDate.class), any(LocalDate.class), any(), any(), any()))
                .thenReturn(Collections.singletonList(new BankReconciliationResponse()));

        mockMvc.perform(get(BASE_URL + "/view")
                        .param("groupPoid", "1")
                        .param("companyPoid", "1")
                        .param("bankPoid", "101")
                        .param("dateFrom", "2025-12-01")
                        .param("dateTill", "2025-12-05")
                        .param("chequeNo", "CHQ123")
                        .param("reconcileCheque", "Y")
                        .param("brType", "TYPE1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Bank reconciliation details fetched successfully"));
    }

    // ── getBankInfo ────────────────────────────────────────────────────────────

    @Test
    void getBankInfo_whenBankExists_returnsSuccess() throws Exception {
        BankRenconciliationBankInfoDTO dto = new BankRenconciliationBankInfoDTO();
        dto.setBank("Test Bank");
        dto.setCompany("Test Company");
        when(service.getBankInfo(5001L)).thenReturn(dto);

        mockMvc.perform(get(BASE_URL + "/bank-info/{glPoid}", 5001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Bank information fetched successfully"));

        verify(loggingService).createLogSummaryEntry(
                eq(LogDetailsEnum.VIEWED), eq(DOC_ID), eq("5001"));
    }

    @Test
    void getBankInfo_whenBankNull_returns404() throws Exception {
        BankRenconciliationBankInfoDTO dto = new BankRenconciliationBankInfoDTO();
        dto.setBank(null);
        when(service.getBankInfo(9999L)).thenReturn(dto);

        mockMvc.perform(get(BASE_URL + "/bank-info/{glPoid}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── saveReconciliation ─────────────────────────────────────────────────────

    @Test
    void saveReconciliation_success() throws Exception {
        when(service.saveReconciliation(anyList())).thenReturn("Successfully Updated.");

        mockMvc.perform(post(BASE_URL + "/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Successfully Updated."));
    }

    @Test
    void saveReconciliation_errorResponse_returns500() throws Exception {
        when(service.saveReconciliation(anyList())).thenReturn("error: Save failed");

        mockMvc.perform(post(BASE_URL + "/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("error: Save failed"));
    }

    // ── holdCheque ─────────────────────────────────────────────────────────────

    @Test
    void holdCheque_success() throws Exception {
        when(service.holdCheque(anyList())).thenReturn("1 cheques hold successfully");

        mockMvc.perform(post(BASE_URL + "/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void holdCheque_errorResponse_returns500() throws Exception {
        when(service.holdCheque(anyList())).thenReturn("Error: Hold failed");

        mockMvc.perform(post(BASE_URL + "/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── unholdCheque ───────────────────────────────────────────────────────────

    @Test
    void unholdCheque_success() throws Exception {
        when(service.unholdCheque(anyList())).thenReturn("1 cheques unheld successfully");

        mockMvc.perform(post(BASE_URL + "/unhold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void unholdCheque_errorResponse_returns500() throws Exception {
        when(service.unholdCheque(anyList())).thenReturn("error processing");

        mockMvc.perform(post(BASE_URL + "/unhold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── updateStatementDate ────────────────────────────────────────────────────

    @Test
    void updateStatementDate_success() throws Exception {
        when(service.updateStatementDate(anyLong(), anyLong(), anyLong(), any(LocalDate.class)))
                .thenReturn("Statement updated");

        mockMvc.perform(post(BASE_URL + "/update-statement-date")
                        .param("companyPoid", "1")
                        .param("bankPoid", "101")
                        .param("postedBy", "1001")
                        .param("statementDate", "2025-12-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateStatementDate_errorResponse_returns500() throws Exception {
        when(service.updateStatementDate(anyLong(), anyLong(), anyLong(), any(LocalDate.class)))
                .thenReturn("ERROR: Update failed");

        mockMvc.perform(post(BASE_URL + "/update-statement-date")
                        .param("companyPoid", "1")
                        .param("bankPoid", "101")
                        .param("postedBy", "1001")
                        .param("statementDate", "2025-12-05"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── pollAutoRefresh ────────────────────────────────────────────────────────

    @Test
    void pollAutoRefresh_success() throws Exception {
        when(service.pollAutoRefresh(anyString(), anyLong(), anyString())).thenReturn("OK");

        mockMvc.perform(post(BASE_URL + "/poll-refresh")
                        .param("userId", "user1")
                        .param("companyPoid", "101")
                        .param("loginUrl", "http://login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void pollAutoRefresh_errorResponse_returns500() throws Exception {
        when(service.pollAutoRefresh(anyString(), anyLong(), anyString()))
                .thenReturn("Error: poll failed");

        mockMvc.perform(post(BASE_URL + "/poll-refresh")
                        .param("userId", "user1")
                        .param("companyPoid", "101")
                        .param("loginUrl", "http://login"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── revertReconciliation ───────────────────────────────────────────────────

    @Test
    void revertReconciliation_success() throws Exception {
        when(service.revertReconciliation(anyString(), anyString(), anyLong(), anyLong(), anyLong(), anyString()))
                .thenReturn("SUCCESS: Reverted");

        mockMvc.perform(post(BASE_URL + "/revert")
                        .param("docId", "101")
                        .param("transactionPoid", "5001")
                        .param("loginUserPoid", "1001")
                        .param("loginGroupPoid", "1")
                        .param("loginCompanyPoid", "101")
                        .param("mailAlert", "Y"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void revertReconciliation_errorResponse_returns500() throws Exception {
        when(service.revertReconciliation(anyString(), anyString(), anyLong(), anyLong(), anyLong(), anyString()))
                .thenReturn("Error: Revert failed");

        mockMvc.perform(post(BASE_URL + "/revert")
                        .param("docId", "101")
                        .param("transactionPoid", "5001")
                        .param("loginUserPoid", "1001")
                        .param("loginGroupPoid", "1")
                        .param("loginCompanyPoid", "101")
                        .param("mailAlert", "Y"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── getReport ──────────────────────────────────────────────────────────────

    @Test
    void getReport_success() throws Exception {
        when(service.fetchReport(any(BankReconcileReportRequest.class)))
                .thenReturn(new BankReconcileReportResponse());

        mockMvc.perform(post(BASE_URL + "/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BankReconcileReportRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report fetched successfully"));
    }

    // ── print ──────────────────────────────────────────────────────────────────

    @Test
    void print_success_returnsPdf() throws Exception {
        when(service.print(anyLong(), anyLong(), any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn("pdf-content".getBytes());

        mockMvc.perform(get(BASE_URL + "/print")
                        .param("bankPoid", "101")
                        .param("dateFrom", "2025-12-01")
                        .param("dateTill", "2025-12-05")
                        .param("balanceAsPerBank", "1000.00"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        containsString("attachment; filename=bank-reconciliation-1.pdf")));
    }

    @Test
    void print_whenException_returns500() throws Exception {
        when(service.print(anyLong(), anyLong(), any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenThrow(new RuntimeException("Print failed"));

        mockMvc.perform(get(BASE_URL + "/print")
                        .param("bankPoid", "101")
                        .param("dateFrom", "2025-12-01")
                        .param("dateTill", "2025-12-05")
                        .param("balanceAsPerBank", "1000.00"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Failed to generate PDF: Print failed")));
    }

    // ── exportExcel ────────────────────────────────────────────────────────────

    @Test
    void exportExcel_success_returnsOctetStream() throws Exception {
        ExcelFileData data = new ExcelFileData("excel".getBytes(), "BankReconciliation.xlsx");
        when(excelExportService.generateExcel(anyString(), isNull(), anyMap(), anyString()))
                .thenReturn(data);

        mockMvc.perform(get(BASE_URL + "/excel")
                        .param("toDate", "2025-12-05")
                        .param("bankPoid", "181")
                        .param("balanceAsPerBank", "1000.00"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Content-Disposition",
                        containsString("attachment; filename=BankReconciliation.xlsx")));
    }

    @Test
    void exportExcel_whenException_returns500() throws Exception {
        when(excelExportService.generateExcel(anyString(), isNull(), anyMap(), anyString()))
                .thenThrow(new RuntimeException("Excel failed"));

        mockMvc.perform(get(BASE_URL + "/excel")
                        .param("toDate", "2025-12-05")
                        .param("bankPoid", "181")
                        .param("balanceAsPerBank", "1000.00"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Failed to generate Excel: Excel failed")));
    }
}
