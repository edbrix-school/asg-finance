package com.asg.finance.repository;

import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.ChequeReturnDataDto;
import com.asg.finance.dto.ChequeReturnGlEntryDto;
import com.asg.finance.dto.ChequeReturnLoadResponseDto;
import com.asg.common.lib.security.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleTypes;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ChequeReturnLoadRepository {

    private final DataSource dataSource;
    private final LovDataService lovService;

    public ChequeReturnLoadResponseDto loadChequeReturnDto(String chequeNum, String receiptNo) {

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        String loginUser = UserContext.getUserId();

        if (chequeNum == null || chequeNum.trim().isEmpty()) {
            throw new IllegalArgumentException("chequeNumber is required");
        }

        String sql = "{ call PROC_CHEQUE_RETURN_LOAD_V2(?, ?, ?, ?, ?, ?, ?, ?) }";

        try (Connection connection = dataSource.getConnection();
             CallableStatement cs = connection.prepareCall(sql)) {

            log.info("Calling PROC_CHEQUE_RETURN_LOAD_V2 groupPoid={}, companyPoid={}, loginUser={}, chequeNum={}, receiptNo={}",
                    groupPoid, companyPoid, loginUser, chequeNum, receiptNo);

            // IN parameters
            cs.setLong(1, groupPoid);
            cs.setLong(2, companyPoid);
            cs.setString(3, loginUser);
            cs.setString(4, chequeNum);

            // OUT cursors
            cs.registerOutParameter(5, OracleTypes.CURSOR);
            cs.registerOutParameter(6, OracleTypes.CURSOR);

            // IN receipt no (nullable)
            cs.setString(7, receiptNo);

            // NEW OUT result parameter
            cs.registerOutParameter(8, Types.VARCHAR);

            cs.execute();

            // FIRST check result message
            String resultMessage = cs.getString(8);
            if (resultMessage != null && !resultMessage.trim().isEmpty()) {
                log.warn("Procedure returned message: {}", resultMessage);
                throw new IllegalStateException(resultMessage);
            }

            // ==============================
            // Process Cheque Details Cursor
            // ==============================
            ChequeReturnDataDto chequeData = null;

            try (ResultSet rs = (ResultSet) cs.getObject(5)) {

                if (rs != null && rs.next()) {

                    chequeData = new ChequeReturnDataDto();
                    chequeData.setPaymentMainPoid(getLong(rs, 1));
                    chequeData.setAmount(getDouble(rs, 2));
                    chequeData.setChoPoid(getLong(rs, 3));
                    chequeData.setChoDate(toLocalDate(rs.getTimestamp(4)));
                    chequeData.setPymtType(rs.getString(5));
                    chequeData.setChqCardNo(rs.getString(6));
                    chequeData.setChqDate(toLocalDate(rs.getTimestamp(7)));
                    chequeData.setBankPoid(getLong(rs, 8));
                    chequeData.setAddressPoid(getLong(rs, 9));
                    chequeData.setChqAcName(rs.getString(10));
                    chequeData.setRcpDate(toLocalDate(rs.getTimestamp(11)));
                    chequeData.setChqAcNo(rs.getString(12));
                    chequeData.setRemarks(rs.getString(13));
                    chequeData.setRefDocRef(rs.getString(14));
                    chequeData.setRefDocId(rs.getString(15));
                    chequeData.setRefDocPoid(getLong(rs, 16));

                } else {
                    throw new IllegalStateException(
                            "No recent deposit details found for this cheque in the current login company."
                    );
                }
            }

            // ==============================
            // Process GL Details Cursor
            // ==============================
            List<ChequeReturnGlEntryDto> glEntries = new ArrayList<>();

            try (ResultSet rs2 = (ResultSet) cs.getObject(6)) {

                if (rs2 == null) {
                    throw new IllegalStateException(
                            "No GL entries returned for this cheque."
                    );
                }

                while (rs2.next()) {

                    ChequeReturnGlEntryDto gl = new ChequeReturnGlEntryDto();

                    String type = rs2.getString(1);
                    gl.setType(type);
                    gl.setCompanyPoid(getLong(rs2, 2));
                    gl.setCompanyDtl(
                            lovService.getDetailsByPoidAndLovName(
                                    gl.getCompanyPoid(), "COMPANY"));

                    gl.setGlPoid(getLong(rs2, 3));
                    gl.setGlDtl(
                            lovService.getDetailsByPoidAndLovName(
                                    gl.getGlPoid(), "GL_MASTER_LEDGERS"));

                    if ("DR".equalsIgnoreCase(type)) {
                        gl.setAmount(getDouble(rs2, 4));
                    } else {
                        gl.setAmount(getDouble(rs2, 5));
                    }

                    glEntries.add(gl);
                }

                if (glEntries.isEmpty()) {
                    throw new IllegalStateException(
                            "No GL entries returned for this cheque."
                    );
                }
            }

            // ==============================
            // Build Response
            // ==============================
            ChequeReturnLoadResponseDto resp = new ChequeReturnLoadResponseDto();
            resp.setChequeDetails(chequeData);
            resp.setGlDetails(glEntries);

            return resp;

        } catch (SQLException e) {
            log.error("Error executing PROC_CHEQUE_RETURN_LOAD_V2", e);
            throw new RuntimeException("Error executing PROC_CHEQUE_RETURN_LOAD_V2", e);
        }
    }

    private static Long getLong(ResultSet rs, int index) throws SQLException {
        long v = rs.getLong(index);
        return rs.wasNull() ? null : v;
    }

    private static Double getDouble(ResultSet rs, int index) throws SQLException {
        double v = rs.getDouble(index);
        return rs.wasNull() ? null : v;
    }

    private static LocalDate toLocalDate(Timestamp ts) {
        if (ts == null) return null;
        return ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}