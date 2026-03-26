package com.asg.finance.repository;

import com.asg.common.lib.security.util.UserContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import oracle.jdbc.OracleTypes;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.asg.finance.dto.GlChequeConversionLoadResponseDto;

@Repository
@Slf4j
public class GlChequeCashConvertRepository {

    private final DataSource dataSource;
    @Autowired
    public GlChequeCashConvertRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<GlChequeConversionLoadResponseDto> loadGlChequeConversion(String chequeNumber, String chequeAcNumber, String type) {

        Long groupPoid= UserContext.getGroupPoid();
        Long companyPoid= UserContext.getCompanyPoid();
        Long loginUserPoid= UserContext.getUserPoid();

        String sql = "{ call PROC_CHEQUE_CONVERSION_LOAD(?, ?, ?, ?, ?, ?, ?) }";

        try (Connection connection = dataSource.getConnection();
             CallableStatement cs = connection.prepareCall(sql)) {

            log.info("Calling PROC_CHEQUE_CONVERSION_LOAD groupPoid={}, companyPoid={}, loginUserPoid={}, chequeNum={}, chqAcNo={}, type={}",
                    groupPoid,companyPoid,loginUserPoid,
                    chequeNumber, chequeAcNumber,type);

            cs.setObject(1, groupPoid);
            cs.setObject(2, companyPoid);
            cs.setObject(3,loginUserPoid);
            cs.setObject(4, chequeNumber);
            cs.setObject(5, chequeAcNumber);
            cs.setObject(6, type);
            cs.registerOutParameter(7, OracleTypes.CURSOR);

            log.debug("Executing stored procedure PROC_CHEQUE_CONVERSION_LOAD");
            cs.execute();
            try (ResultSet rs = (ResultSet) cs.getObject(7)) {
                log.debug("Stored procedure executed successfully");
                if (rs == null) {
                    log.warn("PROC_CHEQUE_CONVERSION_LOAD returned null cursor");
                    return Collections.emptyList();
                }

                boolean chequeMode = type!= null && (
                        type.equalsIgnoreCase("CHEQUE_TO_CHEQUE")
                                ||type.equalsIgnoreCase("CHEQUE_TO_CASH")
                                || type.equalsIgnoreCase("CHEQUE_TO_IMCOCHEQUE")
                                || type.equalsIgnoreCase("CHEQUE_TO_BANK")
                );
                List<GlChequeConversionLoadResponseDto> list = new ArrayList<>();
                int rowCount = 0;
                while (rs.next()) {
                    GlChequeConversionLoadResponseDto dto = new GlChequeConversionLoadResponseDto();

                    Long paymentMainPoid = null;
                    long pmp = rs.getLong(1);
                    if (!rs.wasNull()) paymentMainPoid = pmp;
                    dto.setPaymentMainPoid(paymentMainPoid);

                    Double amount = null;
                    double amt = rs.getDouble(2);
                    if (!rs.wasNull()) amount = amt;
                    dto.setAmount(amount);

                    if (chequeMode) {
                        java.util.Date choDate = null;
                        java.sql.Timestamp choTs = rs.getTimestamp(3);
                        if (choTs != null) choDate = new java.util.Date(choTs.getTime());
                        dto.setChoDate(choDate);

                        dto.setChqCardNo(rs.getString(4));

                        java.util.Date chqDate = null;
                        java.sql.Timestamp chqTs = rs.getTimestamp(5);
                        if (chqTs != null) chqDate = new java.util.Date(chqTs.getTime());
                        dto.setChqDate(chqDate);

                        Long bankPoid = null;
                        long bp = rs.getLong(6);
                        if (!rs.wasNull()) bankPoid = bp;
                        dto.setBankPoid(bankPoid);

                        dto.setChqAcNo(rs.getString(7));
                        dto.setChqAcName(rs.getString(8));
                        dto.setRemarks(rs.getString(9));
                        dto.setVoucherType(rs.getString(10));
                        dto.setLineType(rs.getString(11));
                    } else {
                        dto.setPaymentMainPoid(rs.getLong(1));
                        dto.setAmount(rs.getDouble(2));
                        dto.setRemarks(rs.getString(3));
                        dto.setVoucherType(rs.getString(4));
                        dto.setLineType(rs.getString(5));
                    }
                    list.add(dto);
                    rowCount++;
                }
                log.info("PROC_CHEQUE_CONVERSION_LOAD fetched {} row(s) for type={} and chequeNum={}", rowCount, type, chequeNumber);
                return list;
            }
        } catch (SQLException e) {
            log.error("Error executing stored procedure PROC_CHEQUE_CONVERSION_LOAD", e);
            throw new RuntimeException("Error executing PROC_CHEQUE_CONVERSION_LOAD", e);
        }
    }

    public String checkChequeConvertStatus(
            Long groupPoid,
            Long companyPoid,
            Long transactionPoid,
            String documentId,
            Long userPoid,
            String username
    ) {

        String result = null;

        String sql = "{call PROC_CHEQUE_CONVERT_STATUS_CHK(?,?,?,?,?,?,?)}";

        try (Connection connection = dataSource.getConnection();
             CallableStatement callableStatement = connection.prepareCall(sql)) {

            // IN parameters
            callableStatement.setLong(1, groupPoid);
            callableStatement.setLong(2, companyPoid);
            callableStatement.setLong(3, transactionPoid);
            callableStatement.setString(4, documentId);
            callableStatement.setLong(5, userPoid);
            callableStatement.setString(6, username);

            // OUT parameter
            callableStatement.registerOutParameter(7, Types.VARCHAR);

            // Execute
            callableStatement.execute();

            // Get OUT value
            result = callableStatement.getString(7);

        } catch (SQLException e) {
            e.printStackTrace(); // Replace with proper logging
            throw new RuntimeException("Failed to check cheque convert status", e);
        }

        return result;
    }

    /**
     * Validates that the transaction date falls within the current financial year (mirrors trigger
     * FUNC_GLOB_FINANCIAL_YEAR_VALID). Returns false if the function result contains 'ERROR'.
     */
    public boolean isFinancialYearValid(Long companyPoid, LocalDate transactionDate) {
        if (companyPoid == null || transactionDate == null) {
            return true;
        }
        String sql = "SELECT FUNC_GLOB_FINANCIAL_YEAR_VALID(?, ?) FROM DUAL";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyPoid);
            ps.setDate(2, Date.valueOf(transactionDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String result = rs.getString(1);
                    return result == null || !result.toUpperCase().contains("ERROR");
                }
            }
        } catch (SQLException e) {
            log.warn("Financial year validation check failed (FUNC_GLOB_FINANCIAL_YEAR_VALID): {}", e.getMessage());
            return true; // allow flow if DB function unavailable; trigger will still run
        }
        return true;
    }

    /**
     * Validates that the transaction date falls within the current transaction period (mirrors trigger
     * FUNC_GLOB_TRANSACTN_YEAR_VALID). Returns false if the function result contains 'ERROR'.
     */
    public boolean isTransactionYearValid(Long companyPoid, LocalDate transactionDate) {
        if (companyPoid == null || transactionDate == null) {
            return true;
        }
        String sql = "SELECT FUNC_GLOB_TRANSACTN_YEAR_VALID(?, ?) FROM DUAL";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyPoid);
            ps.setDate(2, Date.valueOf(transactionDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String result = rs.getString(1);
                    return result == null || !result.toUpperCase().contains("ERROR");
                }
            }
        } catch (SQLException e) {
            log.warn("Transaction year validation check failed (FUNC_GLOB_TRANSACTN_YEAR_VALID): {}", e.getMessage());
            return true;
        }
        return true;
    }
}
