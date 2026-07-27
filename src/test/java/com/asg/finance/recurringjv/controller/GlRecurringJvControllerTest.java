package com.asg.finance.recurringjv.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDownloadHeaderService;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.controller.GlRecurringJvController;
import com.asg.finance.dto.RecurringJvCreateResponse;
import com.asg.finance.dto.RecurringJvRequest;
import com.asg.finance.dto.RecurringJvResponse;
import com.asg.finance.service.GlRecurringJvService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = GlRecurringJvController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.asg.finance.aspect.*"))
@ContextConfiguration(classes = {GlRecurringJvController.class, DocumentDownloadHeaderService.class})
class GlRecurringJvControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean

    private JdbcTemplate jdbcTemplate;


    @MockBean
    private GlRecurringJvService recurringJvService;

    @MockBean
    private LoggingService loggingService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Setup initial mocks if necessary
    }

    @Test
    void createRecurringJv_ShouldReturnSuccess() throws Exception {
        RecurringJvRequest request = new RecurringJvRequest();
        // Set necessary fields to pass validation
        request.setNarration("Test Narration");
        request.setTotalAmount(java.math.BigDecimal.valueOf(100));
        request.setNoOfMonths(2);
        request.setStartDate(java.time.LocalDate.now());
        request.setRefType("ASSET");
        request.setDetails(List.of()); // Assuming minimal validation passes or mock ignores it

        RecurringJvCreateResponse response = new RecurringJvCreateResponse(1L, "Created");
        when(recurringJvService.createRecurringJv(any(RecurringJvRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/recurring-jvs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Recurring JV created successfully"));
    }

    @Test
    void listRecurringJvs_ShouldReturnSuccess() throws Exception {
        FilterRequestDto filterRequest = new FilterRequestDto(null, null, null);
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", List.of());

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("DOC-123");
            when(recurringJvService.listRecurringJvs(eq("DOC-123"), any(), any(), any(), any()))
                    .thenReturn(responseMap);

            mockMvc.perform(post("/v1/recurring-jvs/list")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(filterRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Recurring JVs list retrieved successfully"));
        }
    }

    @Test
    void getRecurringJvById_ShouldReturnSuccess() throws Exception {
        Long transactionPoid = 1L;
        RecurringJvResponse response = new RecurringJvResponse();
        response.setTransactionPoid(transactionPoid);

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("DOC-123");
            when(recurringJvService.getRecurringJvById(transactionPoid)).thenReturn(response);

            mockMvc.perform(get("/v1/recurring-jvs/{transactionPoid}", transactionPoid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.data.transactionPoid").value(transactionPoid));

            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "DOC-123", "1");
        }
    }

    @Test
    void updateRecurringJv_ShouldReturnSuccess() throws Exception {
        Long transactionPoid = 1L;
        RecurringJvRequest request = new RecurringJvRequest();
        request.setNarration("Update Narration");
        request.setTotalAmount(java.math.BigDecimal.valueOf(200));
        request.setNoOfMonths(3);
        request.setStartDate(java.time.LocalDate.now());
        request.setRefType("ASSET");
        request.setDetails(List.of());

        RecurringJvCreateResponse response = new RecurringJvCreateResponse(transactionPoid, "Updated");

        try (MockedStatic<UserContext> userContext = Mockito.mockStatic(UserContext.class)) {
            userContext.when(UserContext::getDocumentId).thenReturn("DOC-123");
            when(recurringJvService.updateRecurringJv(eq(transactionPoid), any(RecurringJvRequest.class), eq("DOC-123")))
                    .thenReturn(response);

            mockMvc.perform(put("/v1/recurring-jvs/{transactionPoid}", transactionPoid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Recurring JV updated successfully"));
        }
    }

    @Test
    void deleteRecurringJv_ShouldReturnSuccess() throws Exception {
        Long transactionPoid = 1L;
        DeleteReasonDto deleteReason = new DeleteReasonDto();
        deleteReason.setDeleteReason("Not needed");

        mockMvc.perform(delete("/v1/recurring-jvs/{transactionPoid}", transactionPoid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteReason)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Recurring JV deleted successfully"));

        verify(recurringJvService).deleteRecurringJv(eq(transactionPoid), any(DeleteReasonDto.class));
    }

    @Test
    void print_ShouldReturnPdf() throws Exception {
        Long transactionPoid = 1L;
        byte[] pdfBytes = "Sample PDF Content".getBytes();

        when(recurringJvService.print(transactionPoid)).thenReturn(pdfBytes);

        mockMvc.perform(get("/v1/recurring-jvs/print/{transactionPoid}", transactionPoid))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recurring-jv-1.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfBytes));
    }
}
