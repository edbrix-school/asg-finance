package com.asg.finance.journalvoucher.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.controller.JournalVoucherController;
import com.asg.finance.dto.*;
import com.asg.finance.service.JournalVoucherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = JournalVoucherController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.asg.finance.aspect.*"))
@ContextConfiguration(classes = {JournalVoucherController.class})
class JournalVoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JournalVoucherService journalVoucherService;

    @MockBean
    private LoggingService loggingService;

    private MockedStatic<UserContext> mockedUserContext;

    private JournalVoucherRequest journalVoucherRequest;
    private JournalVoucherResponse journalVoucherResponse;

    @BeforeEach
    void setUp() {
        mockedUserContext = mockStatic(UserContext.class);
        mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

        journalVoucherRequest = JournalVoucherRequest.builder()
                .transactionDate(LocalDate.now())
                .refType("GENERAL")
                .amount(BigDecimal.valueOf(1000))
                .postingNarration("Test Narration")
                .confidentialRemarks("Secret Notes")
                .build();

        journalVoucherResponse = JournalVoucherResponse.builder()
                .transactionPoid(1L)
                .docRef("JV-2025-001")
                .message("Success")
                .build();
    }

    @AfterEach
    void tearDown() {
        mockedUserContext.close();
    }

    @Test
    void createJournalVoucher_Success() throws Exception {
        when(journalVoucherService.createJournalVoucher(any(JournalVoucherRequest.class), anyString()))
                .thenReturn(journalVoucherResponse);

        mockMvc.perform(post("/v1/journal-voucher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(journalVoucherRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Journal Voucher created successfully"))
                .andExpect(jsonPath("$.result.data.transactionPoid").value(1L));

        verify(journalVoucherService, times(1)).createJournalVoucher(any(JournalVoucherRequest.class), eq("DOC123"));
    }

    @Test
    void listJournalVouchers_Success() throws Exception {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("content", Collections.singletonList(journalVoucherResponse));
        responseData.put("totalElements", 1);

        when(journalVoucherService.listJournalVouchers(anyString(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(responseData);

        mockMvc.perform(post("/v1/journal-voucher/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Journal Vouchers list retrieved successfully"))
                .andExpect(jsonPath("$.result.data.totalElements").value(1));

        verify(journalVoucherService, times(1)).listJournalVouchers(eq("DOC123"), any(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void listJournalVouchers_DateRangeError() throws Exception {
        mockMvc.perform(post("/v1/journal-voucher/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("startDate", "2025-01-01")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Both startDate and endDate should be specified or both dates should be empty."));
    }

    @Test
    void updateJournalVoucher_Success() throws Exception {
        when(journalVoucherService.updateJournalVoucher(eq(1L), any(JournalVoucherRequest.class), anyString()))
                .thenReturn(journalVoucherResponse);

        mockMvc.perform(put("/v1/journal-voucher/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(journalVoucherRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Journal Voucher updated successfully"))
                .andExpect(jsonPath("$.result.data.transactionPoid").value(1L));

        verify(journalVoucherService, times(1)).updateJournalVoucher(eq(1L), any(JournalVoucherRequest.class), eq("DOC123"));
    }

    @Test
    void getJournalVoucherById_Success() throws Exception {
        JournalVoucherDetailResponse detailResponse = JournalVoucherDetailResponse.builder()
                .transactionPoid(1L)
                .docRef("JV-2025-001")
                .build();

        when(journalVoucherService.getJournalVoucherById(1L)).thenReturn(detailResponse);

        mockMvc.perform(get("/v1/journal-voucher/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Journal Voucher retrieved successfully"))
                .andExpect(jsonPath("$.result.data.transactionPoid").value(1L));

        verify(journalVoucherService, times(1)).getJournalVoucherById(1L);
        verify(loggingService, times(1)).createLogSummaryEntry(eq(LogDetailsEnum.VIEWED), eq("DOC123"), eq("1"));
    }

    @Test
    void deleteJournalVoucher_Success() throws Exception {
        DeleteReasonDto deleteReason = new DeleteReasonDto();
        deleteReason.setDeleteReason("Testing");

        doNothing().when(journalVoucherService).deleteJournalVoucher(eq(1L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/journal-voucher/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReason)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Journal Voucher deleted successfully"));

        verify(journalVoucherService, times(1)).deleteJournalVoucher(eq(1L), any(DeleteReasonDto.class));
    }

    @Test
    void getAssetDepreciationDetails_Success() throws Exception {
        JournalVoucherAssetDetailDto assetDto = new JournalVoucherAssetDetailDto();

        when(journalVoucherService.getAssetDepreciationDetails(100L)).thenReturn(assetDto);

        mockMvc.perform(get("/v1/journal-voucher/asset-depreciation-details/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Asset depreciation details retrieved successfully"));

        verify(journalVoucherService, times(1)).getAssetDepreciationDetails(100L);
    }

    @Test
    void getAssetCapitalizationDetails_Success() throws Exception {
        JournalVoucherAssetCapitalizationResponseDto capDto = new JournalVoucherAssetCapitalizationResponseDto();

        when(journalVoucherService.getAssetCapitalizationDetails(200L)).thenReturn(capDto);

        mockMvc.perform(get("/v1/journal-voucher/asset-capitalization-details/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Asset capitalization details retrieved successfully"));

        verify(journalVoucherService, times(1)).getAssetCapitalizationDetails(200L);
    }

    @Test
    void print_Success() throws Exception {
        byte[] pdfContent = "PDF Content".getBytes();
        when(journalVoucherService.print(1L)).thenReturn(pdfContent);

        mockMvc.perform(get("/v1/journal-voucher/print/1"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=journal-voucher-1.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfContent));

        verify(journalVoucherService, times(1)).print(1L);
    }
}
