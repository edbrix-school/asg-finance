package com.asg.finance.hsbcapisync.repository;

import com.asg.finance.dto.HsbcApiSyncResponseDto;
import com.asg.finance.repository.HsbcApiSyncRepositoryImpl;
import oracle.jdbc.OracleTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HsbcApiSyncRepositoryImplTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private ResultSet balanceResultSet;

    @Mock
    private ResultSet transactionResultSet;

    @InjectMocks
    private HsbcApiSyncRepositoryImpl repository;

    @BeforeEach
    void setUp() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
        when(callableStatement.getObject(3)).thenReturn(balanceResultSet);
        when(callableStatement.getObject(4)).thenReturn(transactionResultSet);
    }

    @Test
    void loadHsbcApiData_Success_EmptyResultSets() throws Exception {
        when(balanceResultSet.next()).thenReturn(false);
        when(transactionResultSet.next()).thenReturn(false);

        HsbcApiSyncResponseDto result = repository.loadHsbcApiData("1234567890", "15-JAN-2025");

        assertNotNull(result);
        assertTrue(result.getBalances().isEmpty());
        assertTrue(result.getTransactions().isEmpty());

        verify(callableStatement).setString(1, "1234567890");
        verify(callableStatement).setString(2, "15-JAN-2025");
        verify(callableStatement).registerOutParameter(3, OracleTypes.CURSOR);
        verify(callableStatement).registerOutParameter(4, OracleTypes.CURSOR);
        verify(callableStatement).execute();
    }

    @Test
    void loadHsbcApiData_WithBalances() throws Exception {
        when(balanceResultSet.next()).thenReturn(true, false);
        when(balanceResultSet.getString("ACCOUNT_NUMBER")).thenReturn("1234567890");
        when(balanceResultSet.getString("BALANCE_DATETIME")).thenReturn("2025-01-15T10:00:00");
        when(balanceResultSet.getString("BALANCE_TYPE")).thenReturn("CLBD");
        when(balanceResultSet.getString("BALANCE_AMOUNT")).thenReturn("50000.00");
        when(balanceResultSet.getString("BALANCE_CURRENCY")).thenReturn("USD");
        when(balanceResultSet.getString("CREDIT_AMOUNT")).thenReturn("1000.00");
        when(balanceResultSet.getString("UPLOADED_DATETIME")).thenReturn("2025-01-15T12:00:00");
        when(transactionResultSet.next()).thenReturn(false);

        HsbcApiSyncResponseDto result = repository.loadHsbcApiData("1234567890", "15-JAN-2025");

        assertNotNull(result);
        assertEquals(1, result.getBalances().size());
        assertEquals("1234567890", result.getBalances().get(0).getAccountNumber());
        assertEquals("CLBD", result.getBalances().get(0).getBalanceType());
        assertEquals(50000.0, result.getBalances().get(0).getBalanceAmount());
        assertEquals("USD", result.getBalances().get(0).getBalanceCurrency());
    }

    @Test
    void loadHsbcApiData_WithTransactions() throws Exception {
        when(balanceResultSet.next()).thenReturn(false);
        when(transactionResultSet.next()).thenReturn(true, false);
        when(transactionResultSet.getString("ACCOUNT_NUMBER")).thenReturn("1234567890");
        when(transactionResultSet.getString("BOOKING_DATETIME")).thenReturn("2025-01-15T09:00:00");
        when(transactionResultSet.getString("VALUE_DATETIME")).thenReturn("2025-01-15T09:00:00");
        when(transactionResultSet.getString("STATEMENT_REFERENCE")).thenReturn("STMT-001");
        when(transactionResultSet.getString("TRANSACTION_REFERENCE")).thenReturn("TXN-001");
        when(transactionResultSet.getString("TRANS_INFO")).thenReturn("Payment");
        when(transactionResultSet.getString("TRANSACTION_AMOUNT_DR")).thenReturn("500.00");
        when(transactionResultSet.getString("TRANSACTION_AMOUNT_CR")).thenReturn(null);
        when(transactionResultSet.getString("UPLOADED_DATETIME")).thenReturn("2025-01-15T12:00:00");
        when(transactionResultSet.getString("NEW_TRANSACTION")).thenReturn("Y");

        HsbcApiSyncResponseDto result = repository.loadHsbcApiData("1234567890", "15-JAN-2025");

        assertNotNull(result);
        assertEquals(1, result.getTransactions().size());
        assertEquals("STMT-001", result.getTransactions().get(0).getStatementReference());
        assertEquals(500.0, result.getTransactions().get(0).getTransactionAmountDr());
        assertNull(result.getTransactions().get(0).getTransactionAmountCr());
        assertEquals("Y", result.getTransactions().get(0).getNewTransaction());
    }

    @Test
    void loadHsbcApiData_WithMultipleBalancesAndTransactions() throws Exception {
        when(balanceResultSet.next()).thenReturn(true, true, false);
        when(balanceResultSet.getString("ACCOUNT_NUMBER")).thenReturn("1234567890");
        when(balanceResultSet.getString("BALANCE_DATETIME")).thenReturn("2025-01-15T10:00:00");
        when(balanceResultSet.getString("BALANCE_TYPE")).thenReturn("CLBD");
        when(balanceResultSet.getString("BALANCE_AMOUNT")).thenReturn("50000.00");
        when(balanceResultSet.getString("BALANCE_CURRENCY")).thenReturn("USD");
        when(balanceResultSet.getString("CREDIT_AMOUNT")).thenReturn(null);
        when(balanceResultSet.getString("UPLOADED_DATETIME")).thenReturn("2025-01-15T12:00:00");

        when(transactionResultSet.next()).thenReturn(true, true, false);
        when(transactionResultSet.getString("ACCOUNT_NUMBER")).thenReturn("1234567890");
        when(transactionResultSet.getString("BOOKING_DATETIME")).thenReturn("2025-01-15T09:00:00");
        when(transactionResultSet.getString("VALUE_DATETIME")).thenReturn("2025-01-15T09:00:00");
        when(transactionResultSet.getString("STATEMENT_REFERENCE")).thenReturn("STMT-001");
        when(transactionResultSet.getString("TRANSACTION_REFERENCE")).thenReturn("TXN-001");
        when(transactionResultSet.getString("TRANS_INFO")).thenReturn("Payment");
        when(transactionResultSet.getString("TRANSACTION_AMOUNT_DR")).thenReturn("500.00");
        when(transactionResultSet.getString("TRANSACTION_AMOUNT_CR")).thenReturn("200.00");
        when(transactionResultSet.getString("UPLOADED_DATETIME")).thenReturn("2025-01-15T12:00:00");
        when(transactionResultSet.getString("NEW_TRANSACTION")).thenReturn("N");

        HsbcApiSyncResponseDto result = repository.loadHsbcApiData("1234567890", "15-JAN-2025");

        assertEquals(2, result.getBalances().size());
        assertEquals(2, result.getTransactions().size());
    }

    @Test
    void loadHsbcApiData_ConnectionFails_ThrowsRuntimeException() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB connection failed"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> repository.loadHsbcApiData("1234567890", "15-JAN-2025"));

        assertEquals("Failed to load HSBC API data", ex.getMessage());
    }

    @Test
    void loadHsbcApiData_NullAmountValues_MappedAsNull() throws Exception {
        when(balanceResultSet.next()).thenReturn(true, false);
        when(balanceResultSet.getString("ACCOUNT_NUMBER")).thenReturn("1234567890");
        when(balanceResultSet.getString("BALANCE_DATETIME")).thenReturn(null);
        when(balanceResultSet.getString("BALANCE_TYPE")).thenReturn(null);
        when(balanceResultSet.getString("BALANCE_AMOUNT")).thenReturn(null);
        when(balanceResultSet.getString("BALANCE_CURRENCY")).thenReturn(null);
        when(balanceResultSet.getString("CREDIT_AMOUNT")).thenReturn("");
        when(balanceResultSet.getString("UPLOADED_DATETIME")).thenReturn(null);
        when(transactionResultSet.next()).thenReturn(false);

        HsbcApiSyncResponseDto result = repository.loadHsbcApiData("1234567890", "15-JAN-2025");

        assertNull(result.getBalances().get(0).getBalanceAmount());
        assertNull(result.getBalances().get(0).getCreditAmount());
    }
}
