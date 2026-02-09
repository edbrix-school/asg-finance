package com.asg.finance.repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.asg.finance.dto.HsbcApiBalanceDto;
import com.asg.finance.dto.HsbcApiSyncResponseDto;
import com.asg.finance.dto.HsbcApiTransactionDto;

import lombok.RequiredArgsConstructor;
import oracle.jdbc.OracleTypes;

@Repository
@RequiredArgsConstructor
public class HsbcApiSyncRepositoryImpl implements HsbcApiSyncRepository {

    private final DataSource dataSource;

    @Override
    public HsbcApiSyncResponseDto loadHsbcApiData(String accountNumber, String date) {
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_LOAD_HSBC_API_BAL(?,?,?,?)}")) {
            
            stmt.setString(1, accountNumber);
            stmt.setString(2, date);
            stmt.registerOutParameter(3, OracleTypes.CURSOR);
            stmt.registerOutParameter(4, OracleTypes.CURSOR);
            stmt.execute();
            
            List<HsbcApiBalanceDto> balances = new ArrayList<>();
            try (ResultSet rs = (ResultSet) stmt.getObject(3)) {
                while (rs.next()) {
                    balances.add(mapToBalanceDto(rs));
                }
            }
            
            List<HsbcApiTransactionDto> transactions = new ArrayList<>();
            try (ResultSet rs = (ResultSet) stmt.getObject(4)) {
                while (rs.next()) {
                    transactions.add(mapToTransactionDto(rs));
                }
            }
            
            HsbcApiSyncResponseDto response = new HsbcApiSyncResponseDto();
            response.setBalances(balances);
            response.setTransactions(transactions);
            return response;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load HSBC API data", e);
        }
    }

    private HsbcApiBalanceDto mapToBalanceDto(ResultSet rs) throws Exception {
        HsbcApiBalanceDto dto = new HsbcApiBalanceDto();
        dto.setAccountNumber(rs.getString("ACCOUNT_NUMBER"));
        dto.setBalanceDatetime(rs.getString("BALANCE_DATETIME"));
        dto.setBalanceType(rs.getString("BALANCE_TYPE"));
        dto.setBalanceAmount(getDoubleValue(rs, "BALANCE_AMOUNT"));
        dto.setBalanceCurrency(rs.getString("BALANCE_CURRENCY"));
        dto.setCreditAmount(getDoubleValue(rs, "CREDIT_AMOUNT"));
        dto.setUploadedDatetime(rs.getString("UPLOADED_DATETIME"));
        return dto;
    }

    private HsbcApiTransactionDto mapToTransactionDto(ResultSet rs) throws Exception {
        HsbcApiTransactionDto dto = new HsbcApiTransactionDto();
        dto.setAccountNumber(rs.getString("ACCOUNT_NUMBER"));
        dto.setBookingDatetime(rs.getString("BOOKING_DATETIME"));
        dto.setValueDatetime(rs.getString("VALUE_DATETIME"));
        dto.setStatementReference(rs.getString("STATEMENT_REFERENCE"));
        dto.setTransactionReference(rs.getString("TRANSACTION_REFERENCE"));
        dto.setTransInfo(rs.getString("TRANS_INFO"));
        dto.setTransactionAmountDr(getDoubleValue(rs, "TRANSACTION_AMOUNT_DR"));
        dto.setTransactionAmountCr(getDoubleValue(rs, "TRANSACTION_AMOUNT_CR"));
        dto.setUploadedDatetime(rs.getString("UPLOADED_DATETIME"));
        dto.setNewTransaction(rs.getString("NEW_TRANSACTION"));
        return dto;
    }
    
    private Double getDoubleValue(ResultSet rs, String columnName) throws Exception {
        String value = rs.getString(columnName);
        return value != null && !value.isEmpty() ? Double.parseDouble(value) : null;
    }
}
