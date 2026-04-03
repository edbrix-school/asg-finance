package com.asg.finance.hsbcapisync.service.impl;

import com.asg.common.lib.exception.AsgException;
import com.asg.finance.dto.HsbcApiBalanceDto;
import com.asg.finance.dto.HsbcApiSyncResponseDto;
import com.asg.finance.dto.HsbcApiTransactionDto;
import com.asg.finance.repository.HsbcApiSyncRepository;
import com.asg.finance.service.impl.HsbcApiSyncServiceImpl;
import com.asg.finance.utility.HsbcApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HsbcApiSyncServiceImplTest {

    @Mock
    private HsbcApiSyncRepository repository;

    @Mock
    private HsbcApiClient hsbcApiClient;

    @InjectMocks
    private HsbcApiSyncServiceImpl service;

    private HsbcApiSyncResponseDto responseDto;
    private final String accountNumber = "1234567890";
    private final LocalDate validDate = LocalDate.now().minusDays(5);

    @BeforeEach
    void setUp() {
        HsbcApiBalanceDto balance = new HsbcApiBalanceDto();
        balance.setAccountNumber(accountNumber);
        balance.setBalanceAmount(50000.0);
        balance.setBalanceCurrency("USD");

        HsbcApiTransactionDto transaction = new HsbcApiTransactionDto();
        transaction.setAccountNumber(accountNumber);
        transaction.setStatementReference("STMT-001");

        responseDto = new HsbcApiSyncResponseDto();
        responseDto.setBalances(List.of(balance));
        responseDto.setTransactions(List.of(transaction));
    }

    // ─── refreshHsbcData ─────────────────────────────────────────────────────

    @Test
    void refreshHsbcData_Success() {
        when(repository.loadHsbcApiData(eq(accountNumber), any())).thenReturn(responseDto);

        HsbcApiSyncResponseDto result = service.refreshHsbcData(accountNumber, validDate);

        assertNotNull(result);
        assertEquals(1, result.getBalances().size());
        assertEquals(accountNumber, result.getBalances().get(0).getAccountNumber());
        verify(repository).loadHsbcApiData(eq(accountNumber), any());
    }

    @Test
    void refreshHsbcData_FormatsDateCorrectly() {
        LocalDate date = LocalDate.of(2025, 1, 5);
        when(repository.loadHsbcApiData(eq(accountNumber), eq("05-JAN-2025"))).thenReturn(responseDto);

        HsbcApiSyncResponseDto result = service.refreshHsbcData(accountNumber, date);

        assertNotNull(result);
        verify(repository).loadHsbcApiData(accountNumber, "05-JAN-2025");
    }

    @Test
    void refreshHsbcData_NullAccountNumber_ThrowsAsgException() {
        AsgException ex = assertThrows(AsgException.class, () -> service.refreshHsbcData(null, validDate));
        assertEquals("Bank account must be selected", ex.getMessage());
    }

    @Test
    void refreshHsbcData_BlankAccountNumber_ThrowsAsgException() {
        AsgException ex = assertThrows(AsgException.class, () -> service.refreshHsbcData("  ", validDate));
        assertEquals("Bank account must be selected", ex.getMessage());
    }

    @Test
    void refreshHsbcData_NullDate_ThrowsAsgException() {
        AsgException ex = assertThrows(AsgException.class, () -> service.refreshHsbcData(accountNumber, null));
        assertEquals("Date must be selected", ex.getMessage());
    }

    @Test
    void refreshHsbcData_DateOlderThan3Months_ThrowsAsgException() {
        LocalDate oldDate = LocalDate.now().minusMonths(4);
        AsgException ex = assertThrows(AsgException.class, () -> service.refreshHsbcData(accountNumber, oldDate));
        assertEquals("Date range must be within the last 3 months", ex.getMessage());
    }

    // ─── syncHsbcApiData ─────────────────────────────────────────────────────

    @Test
    void syncHsbcApiData_Success() throws Exception {
        doNothing().when(hsbcApiClient).syncHsbcData(eq(accountNumber), any());

        String result = service.syncHsbcApiData(accountNumber, validDate);

        assertEquals("HSBC data synced successfully", result);
        verify(hsbcApiClient).syncHsbcData(eq(accountNumber), any());
    }

    @Test
    void syncHsbcApiData_FormatsDateCorrectly() throws Exception {
        LocalDate date = LocalDate.of(2025, 3, 10);
        doNothing().when(hsbcApiClient).syncHsbcData(eq(accountNumber), eq("10-MAR-2025"));

        service.syncHsbcApiData(accountNumber, date);

        verify(hsbcApiClient).syncHsbcData(accountNumber, "10-MAR-2025");
    }

    @Test
    void syncHsbcApiData_NullAccountNumber_ThrowsAsgException() {
        AsgException ex = assertThrows(AsgException.class, () -> service.syncHsbcApiData(null, validDate));
        assertEquals("Bank account must be selected", ex.getMessage());
    }

    @Test
    void syncHsbcApiData_NullDate_ThrowsAsgException() {
        AsgException ex = assertThrows(AsgException.class, () -> service.syncHsbcApiData(accountNumber, null));
        assertEquals("Date must be selected", ex.getMessage());
    }

    @Test
    void syncHsbcApiData_DateOlderThan3Months_ThrowsAsgException() {
        LocalDate oldDate = LocalDate.now().minusMonths(4);
        AsgException ex = assertThrows(AsgException.class, () -> service.syncHsbcApiData(accountNumber, oldDate));
        assertEquals("Date range must be within the last 3 months", ex.getMessage());
    }

    @Test
    void syncHsbcApiData_ClientThrows_WrapsInAsgException() throws Exception {
        doThrow(new Exception("Connection refused")).when(hsbcApiClient).syncHsbcData(any(), any());

        AsgException ex = assertThrows(AsgException.class, () -> service.syncHsbcApiData(accountNumber, validDate));
        assertEquals("Connection refused", ex.getMessage());
    }
}
