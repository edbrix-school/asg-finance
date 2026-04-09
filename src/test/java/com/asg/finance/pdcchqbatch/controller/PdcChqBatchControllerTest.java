package com.asg.finance.pdcchqbatch.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.controller.PdcChqBatchController;
import com.asg.finance.dto.PayGlBreakupCheckResponseDto;
import com.asg.finance.dto.PdcBankPostingProcRequest;
import com.asg.finance.dto.PdcBatchCreationExcelProcRequest;
import com.asg.finance.dto.PdcBatchCreationProcRequest;
import com.asg.finance.dto.PdcBatchCreationProcResponse;
import com.asg.finance.dto.PdcChqBatchHdrRequestDto;
import com.asg.finance.dto.PdcChqBatchHdrResponseDto;
import com.asg.finance.service.PdcChqBatchService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = PdcChqBatchController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.asg.finance.aspect.*"))
@ContextConfiguration(classes = {PdcChqBatchController.class})
class PdcChqBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PdcChqBatchService service;

    @MockBean
    private LoggingService loggingService;

    private PdcChqBatchHdrRequestDto requestDto;
    private PdcChqBatchHdrResponseDto responseDto;
    private PdcBatchCreationProcRequest batchCreationRequest;
    private PdcBankPostingProcRequest bankPostingRequest;
    private PdcBatchCreationExcelProcRequest excelProcRequest;
    private PdcBatchCreationProcResponse procResponse;

    @BeforeEach
    void setUp() {
        requestDto = PdcChqBatchHdrRequestDto.builder()
                .payGlPoid(101L)
                .payingTo("Test Vendor")
                .bankPoid(202L)
                .chqStartDate(LocalDate.of(2026, 4, 1))
                .chqAmount(1500.0)
                .noOfChqs(2L)
                .narration("Test narration")
                .prePrinted("Y")
                .build();

        responseDto = PdcChqBatchHdrResponseDto.builder()
                .transactionPoid(999L)
                .docRef("PDC-0001")
                .payGlPoid(101L)
                .payingTo("Test Vendor")
                .bankPoid(202L)
                .narration("Test narration")
                .build();

        batchCreationRequest = PdcBatchCreationProcRequest.builder()
                .noOfCheques(2)
                .chequeAmount(1500.0)
                .startChequeNo("100001")
                .startDate(LocalDate.of(2026, 4, 1))
                .prePrinted("Y")
                .narration("Test narration")
                .billRef("BILL-1")
                .build();

        bankPostingRequest = PdcBankPostingProcRequest.builder()
                .payGlPoid(101L)
                .payingTo("Test Vendor")
                .bankPoid(202L)
                .noOfChqs(2L)
                .chequeAmount(1500.0)
                .startChequeNo("100001")
                .startDate("2026-04-01")
                .build();

        excelProcRequest = new PdcBatchCreationExcelProcRequest();
        excelProcRequest.setNoOfCheques(2);
        excelProcRequest.setChequeAmount(java.math.BigDecimal.valueOf(1500));
        excelProcRequest.setStartChequeNo("100001");
        excelProcRequest.setStartDate(LocalDate.of(2026, 4, 1));
        excelProcRequest.setPrePrinted("Y");
        excelProcRequest.setNarration("Test narration");
        excelProcRequest.setBillRef("BILL-1");

        procResponse = PdcBatchCreationProcResponse.builder()
                .status("SUCCESS")
                .chequeDetails(Collections.emptyList())
                .build();
    }

    @Test
    void createPdcBatch_Success() throws Exception {
        when(service.createBatch(any(PdcChqBatchHdrRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/v1/pdc-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.transactionPoid").value(999L))
                .andExpect(jsonPath("$.result.data.docRef").value("PDC-0001"));

        verify(service).createBatch(any(PdcChqBatchHdrRequestDto.class));
    }

    @Test
    void createPdcBatch_ValidationException_Returns500() throws Exception {
        when(service.createBatch(any(PdcChqBatchHdrRequestDto.class)))
                .thenThrow(new ValidationException("Validation failed"));

        mockMvc.perform(post("/v1/pdc-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createPdcBatch_RuntimeException_Returns500() throws Exception {
        when(service.createBatch(any(PdcChqBatchHdrRequestDto.class)))
                .thenThrow(new RuntimeException("Create failed"));

        mockMvc.perform(post("/v1/pdc-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createPdcBatch_InvalidPayload_Returns400() throws Exception {
        mockMvc.perform(post("/v1/pdc-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePdcBatch_Success() throws Exception {
        when(service.updateBatch(eq(999L), any(PdcChqBatchHdrRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/v1/pdc-batch/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.transactionPoid").value(999L));

        verify(service).updateBatch(eq(999L), any(PdcChqBatchHdrRequestDto.class));
    }

    @Test
    void updatePdcBatch_ValidationException_Returns500() throws Exception {
        when(service.updateBatch(eq(999L), any(PdcChqBatchHdrRequestDto.class)))
                .thenThrow(new ValidationException("Validation failed"));

        mockMvc.perform(put("/v1/pdc-batch/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updatePdcBatch_RuntimeException_Returns500() throws Exception {
        when(service.updateBatch(eq(999L), any(PdcChqBatchHdrRequestDto.class)))
                .thenThrow(new RuntimeException("Update failed"));

        mockMvc.perform(put("/v1/pdc-batch/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updatePdcBatch_InvalidPayload_Returns400() throws Exception {
        mockMvc.perform(put("/v1/pdc-batch/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPdcBatchById_Success() throws Exception {
        when(service.findById(999L)).thenReturn(responseDto);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("400-113");

            mockMvc.perform(get("/v1/pdc-batch/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(999L));

            verify(service).findById(999L);
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "400-113", "999");
        }
    }

    @Test
    void getPdcBatchById_Exception_Returns500() throws Exception {
        when(service.findById(999L)).thenThrow(new RuntimeException("Fetch failed"));

        mockMvc.perform(get("/v1/pdc-batch/999"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void deletePdcBatch_WithReason_Success() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("No longer required");
        doNothing().when(service).deletePdcBatch(eq(999L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/pdc-batch/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk());

        verify(service).deletePdcBatch(eq(999L), any(DeleteReasonDto.class));
    }

    @Test
    void deletePdcBatch_WithoutReason_Success() throws Exception {
        doNothing().when(service).deletePdcBatch(eq(999L), isNull());

        mockMvc.perform(delete("/v1/pdc-batch/999"))
                .andExpect(status().isOk());

        verify(service).deletePdcBatch(eq(999L), isNull());
    }

    @Test
    void deletePdcBatch_Exception_Returns500() throws Exception {
        doThrow(new RuntimeException("Delete failed")).when(service).deletePdcBatch(eq(999L), isNull());

        mockMvc.perform(delete("/v1/pdc-batch/999"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void listPdcBatch_Success() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("content", Collections.singletonList(responseDto));
        data.put("totalElements", 1);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("400-113");
            when(service.listPdcBatchCreation(eq("400-113"), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(data);

            mockMvc.perform(post("/v1/pdc-batch/list")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.totalElements").value(1));
        }
    }

    @Test
    void listPdcBatch_WithDateRange_Success() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("content", Collections.emptyList());

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("400-113");
            when(service.listPdcBatchCreation(eq("400-113"), isNull(),
                    eq(LocalDate.of(2026, 4, 1)), eq(LocalDate.of(2026, 4, 30)), any(Pageable.class)))
                    .thenReturn(data);

            mockMvc.perform(post("/v1/pdc-batch/list")
                            .param("startDate", "2026-04-01")
                            .param("endDate", "2026-04-30")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void listPdcBatch_InvalidDate_Returns500() throws Exception {
        mockMvc.perform(post("/v1/pdc-batch/list")
                        .param("startDate", "invalid-date")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void validatePayGlBreakup_Success() throws Exception {
        PayGlBreakupCheckResponseDto response = PayGlBreakupCheckResponseDto.builder()
                .result("BILL_WISE")
                .costGroup("CG-1")
                .build();
        when(service.validatePayGl(101L)).thenReturn(response);

        mockMvc.perform(get("/v1/pdc-batch/validate-paygl")
                        .param("payGlPoid", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.result").value("BILL_WISE"));

        verify(service).validatePayGl(101L);
    }

    @Test
    void validatePayGlBreakup_Exception_Returns500() throws Exception {
        when(service.validatePayGl(101L)).thenThrow(new RuntimeException("Validation failed"));

        mockMvc.perform(get("/v1/pdc-batch/validate-paygl")
                        .param("payGlPoid", "101"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void validatePayGlBreakup_MissingParam_Returns400() throws Exception {
        mockMvc.perform(get("/v1/pdc-batch/validate-paygl"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void runBatchCreationProcedure_Success() throws Exception {
        when(service.processBatch(any(PdcBatchCreationProcRequest.class))).thenReturn(procResponse);

        mockMvc.perform(post("/v1/pdc-batch/999/run-batch-creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchCreationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.status").value("SUCCESS"));
    }

    @Test
    void runBatchCreationProcedure_Exception_Returns500() throws Exception {
        when(service.processBatch(any(PdcBatchCreationProcRequest.class)))
                .thenThrow(new RuntimeException("Procedure failed"));

        mockMvc.perform(post("/v1/pdc-batch/999/run-batch-creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchCreationRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void runBankPostingProcedure_Success() throws Exception {
        when(service.runBankPostingProcedure(any(PdcBankPostingProcRequest.class))).thenReturn(procResponse);

        mockMvc.perform(post("/v1/pdc-batch/999/run-bank-posting")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bankPostingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.status").value("SUCCESS"));
    }

    @Test
    void runBankPostingProcedure_RuntimeException_Returns500() throws Exception {
        when(service.runBankPostingProcedure(any(PdcBankPostingProcRequest.class)))
                .thenThrow(new RuntimeException("Bank posting failed"));

        mockMvc.perform(post("/v1/pdc-batch/999/run-bank-posting")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bankPostingRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void runBankPostingProcedure_CheckedException_Returns500() throws Exception {
        doAnswer(invocation -> sneakyThrow(new Exception("Checked failure")))
                .when(service).runBankPostingProcedure(any(PdcBankPostingProcRequest.class));

        mockMvc.perform(post("/v1/pdc-batch/999/run-bank-posting")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bankPostingRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void runExcelBatchCreationProcedure_Success() throws Exception {
        when(service.createBatchFromExcel(any(PdcBatchCreationExcelProcRequest.class))).thenReturn(procResponse);

        mockMvc.perform(post("/v1/pdc-batch/999/run-excel-batch-creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excelProcRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.status").value("SUCCESS"));
    }

    @Test
    void runExcelBatchCreationProcedure_RuntimeException_Returns500() throws Exception {
        when(service.createBatchFromExcel(any(PdcBatchCreationExcelProcRequest.class)))
                .thenThrow(new RuntimeException("Excel batch failed"));

        mockMvc.perform(post("/v1/pdc-batch/999/run-excel-batch-creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excelProcRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void runExcelBatchCreationProcedure_CheckedException_Returns500() throws Exception {
        doAnswer(invocation -> sneakyThrow(new Exception("Checked failure")))
                .when(service).createBatchFromExcel(any(PdcBatchCreationExcelProcRequest.class));

        mockMvc.perform(post("/v1/pdc-batch/999/run-excel-batch-creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excelProcRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void uploadExcel_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pdc.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "test".getBytes());
        when(service.uploadExcel(any())).thenReturn("Uploaded");

        mockMvc.perform(multipart("/v1/pdc-batch/upload-excel").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data").value("Uploaded"));
    }

    @Test
    void uploadExcel_EmptyFile_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

        mockMvc.perform(multipart("/v1/pdc-batch/upload-excel").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadExcel_Exception_Returns500() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pdc.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "test".getBytes());
        when(service.uploadExcel(any())).thenThrow(new RuntimeException("Upload failed"));

        mockMvc.perform(multipart("/v1/pdc-batch/upload-excel").file(file))
                .andExpect(status().isInternalServerError());
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable, R> R sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
