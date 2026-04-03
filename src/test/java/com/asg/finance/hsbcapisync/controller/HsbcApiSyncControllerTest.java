package com.asg.finance.hsbcapisync.controller;

import com.asg.common.lib.service.LoggingService;
import com.asg.finance.controller.HsbcApiSyncController;
import com.asg.finance.dto.HsbcApiBalanceDto;
import com.asg.finance.dto.HsbcApiSyncResponseDto;
import com.asg.finance.dto.HsbcApiTransactionDto;
import com.asg.finance.service.HsbcApiSyncService;
import com.asg.common.lib.exception.AsgException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = HsbcApiSyncController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.asg.finance.aspect.*"))
@ContextConfiguration(classes = {HsbcApiSyncController.class})
class HsbcApiSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HsbcApiSyncService service;

    @MockBean
    private LoggingService loggingService;

    private HsbcApiSyncResponseDto responseDto;

    @BeforeEach
    void setUp() {
        HsbcApiBalanceDto balance = new HsbcApiBalanceDto();
        balance.setAccountNumber("1234567890");
        balance.setBalanceAmount(50000.0);
        balance.setBalanceCurrency("USD");
        balance.setBalanceType("CLBD");

        HsbcApiTransactionDto transaction = new HsbcApiTransactionDto();
        transaction.setAccountNumber("1234567890");
        transaction.setStatementReference("STMT-001");
        transaction.setTransactionAmountDr(1000.0);

        responseDto = new HsbcApiSyncResponseDto();
        responseDto.setBalances(List.of(balance));
        responseDto.setTransactions(List.of(transaction));
    }

    @Test
    void refreshHsbcData_Success() throws Exception {
        when(service.refreshHsbcData(eq("1234567890"), any(LocalDate.class))).thenReturn(responseDto);

        mockMvc.perform(get("/v1/hsbc-api-sync/refresh")
                        .param("accountNumber", "1234567890")
                        .param("date", "2025-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data.balances[0].accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.result.data.transactions[0].statementReference").value("STMT-001"));

        verify(service).refreshHsbcData(eq("1234567890"), eq(LocalDate.of(2025, 1, 15)));
    }

    @Test
    void refreshHsbcData_MissingAccountNumber_Returns400() throws Exception {
        mockMvc.perform(get("/v1/hsbc-api-sync/refresh")
                        .param("date", "2025-01-15"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshHsbcData_MissingDate_Returns400() throws Exception {
        mockMvc.perform(get("/v1/hsbc-api-sync/refresh")
                        .param("accountNumber", "1234567890"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshHsbcData_ServiceThrows_Returns500() throws Exception {
        when(service.refreshHsbcData(any(), any())).thenThrow(new AsgException("Bank account must be selected", 400));

        mockMvc.perform(get("/v1/hsbc-api-sync/refresh")
                        .param("accountNumber", "1234567890")
                        .param("date", "2025-01-15"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void syncHsbcData_Success() throws Exception {
        when(service.syncHsbcApiData(eq("1234567890"), any(LocalDate.class))).thenReturn("HSBC data synced successfully");

        mockMvc.perform(get("/v1/hsbc-api-sync/sync")
                        .param("accountNumber", "1234567890")
                        .param("date", "2025-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data").value("HSBC data synced successfully"));

        verify(service).syncHsbcApiData(eq("1234567890"), eq(LocalDate.of(2025, 1, 15)));
    }

    @Test
    void syncHsbcData_MissingAccountNumber_Returns400() throws Exception {
        mockMvc.perform(get("/v1/hsbc-api-sync/sync")
                        .param("date", "2025-01-15"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void syncHsbcData_MissingDate_Returns400() throws Exception {
        mockMvc.perform(get("/v1/hsbc-api-sync/sync")
                        .param("accountNumber", "1234567890"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void syncHsbcData_ServiceThrows_Returns500() throws Exception {
        when(service.syncHsbcApiData(any(), any())).thenThrow(new AsgException("Date range must be within the last 3 months", 400));

        mockMvc.perform(get("/v1/hsbc-api-sync/sync")
                        .param("accountNumber", "1234567890")
                        .param("date", "2025-01-15"))
                .andExpect(status().isInternalServerError());
    }
}
