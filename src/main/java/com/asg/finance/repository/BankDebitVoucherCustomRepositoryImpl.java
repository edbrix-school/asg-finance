package com.asg.finance.repository;

import java.math.BigDecimal;
import java.sql.*;

import com.asg.finance.dto.ChargeDetailDto;
import com.asg.finance.dto.ChargeFDADto;
import com.asg.finance.dto.ChargeFFDto;
import com.asg.finance.dto.ItemDetailDto;
import com.asg.common.lib.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.internal.OracleTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BankDebitVoucherCustomRepositoryImpl implements BankDebitVoucherCustomRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void procGlJobValBeforeSave(Long groupPoid, Long userPoid, Long companyPoid,
                                       String docId, String refType, String ref) {
        jdbcTemplate.execute((CallableStatementCreator) con -> {
            CallableStatement cs = con.prepareCall("BEGIN PROC_GL_JOB_VAL_BEFORE_SAVE(?,?,?,?,?,?,?); END;");
            cs.setObject(1, groupPoid);
            cs.setObject(2, userPoid);
            cs.setObject(3, companyPoid);
            cs.setObject(4, docId);
            cs.setObject(5, refType);
            cs.setObject(6, ref);
            cs.registerOutParameter(7, Types.VARCHAR);
            return cs;
        }, (CallableStatementCallback<Void>) cs -> {
            cs.execute();
            String status = trimOrNull(cs.getString(7));

            // Match legacy error handling: lines 452-459
            if (status != null) {
                if (status.contains("CLOSED")) {
                    throw new ValidationException("This " + refType + " Status is 'Closed', could not save...");
                }
                if (status.contains("ERROR") || status.contains("WARNING")) {
                    throw new ValidationException(status);
                }
            }
            return null;
        });
    }

    @Override
    public void procGlBankPayGlBenVal(Long groupPoid, Long userPoid, Long companyPoid,
                                      String docId, Long docKeyPoid, String payingType,
                                      String refType, Long payGlPoid, String beneficiaryId, Long bankPoid) {
        log.debug("Calling PROC_GL_BANK_PAYGL_BEN_VAL_V2 with params: " +
                        "groupPoid={}, userPoid={}, companyPoid={}, docId={}, docKeyPoid={}, " +
                        "payingType={}, refType={}, payGlPoid={}, beneficiaryId={}, bankPoid={}",
                groupPoid, userPoid, companyPoid, docId, docKeyPoid,
                payingType, refType, payGlPoid, beneficiaryId, bankPoid);
        jdbcTemplate.execute((CallableStatementCreator) con -> {
            CallableStatement cs = con.prepareCall("BEGIN PROC_GL_BANK_PAYGL_BEN_VAL_V2(?,?,?,?,?,?,?,?,?,?,?); END;");
            cs.setObject(1, groupPoid);
            cs.setObject(2, userPoid);
            cs.setObject(3, companyPoid);
            cs.setObject(4, docId);
            cs.setObject(5, docKeyPoid);
            cs.setObject(6, payingType);
            cs.setObject(7, refType);
            cs.setObject(8, payGlPoid);
            cs.setObject(9, beneficiaryId);
            cs.setObject(10, bankPoid);
            cs.registerOutParameter(11, Types.VARCHAR);
            return cs;
        }, (CallableStatementCallback<Void>) cs -> {
            cs.execute();
            String status = trimOrNull(cs.getString(11));

            // Match legacy error handling: lines 616-619
            if (status != null && (status.contains("ERROR") || status.contains("WARNING"))) {
                throw new ValidationException(status);
            }
            return null;
        });
    }

    @Override
    public void procGlJobRelOldValues(Long groupPoid, Long userPoid, Long companyPoid,
                                      String docId, Long docKeyPoid) {
        jdbcTemplate.execute((CallableStatementCreator) con -> {
            CallableStatement cs = con.prepareCall("BEGIN PROC_GL_JOB_REL_OLD_VALUES(?,?,?,?,?,?,?); END;");
            cs.setObject(1, groupPoid);
            cs.setObject(2, userPoid);
            cs.setObject(3, companyPoid);
            cs.setObject(4, docId);
            cs.setObject(5, docKeyPoid);
            cs.registerOutParameter(6, Types.VARCHAR);
            cs.registerOutParameter(7, Types.VARCHAR);
            return cs;
        }, (CallableStatementCallback<Void>) cs -> {
            cs.execute();
            // Old values are stored but not used in validation - just need to call it
            return null;
        });
    }

    private String trimOrNull(String value) {
        return value == null ? null : value.trim();
    }

    @Override
    public List<ChargeFFDto> procLoadFFCharges(Long groupPoid, Long userPoid, Long companyPoid, Long ffRefPoid) {
        return jdbcTemplate.execute((CallableStatementCreator) con -> {
            CallableStatement cs = con.prepareCall("{call PROC_BANK_DEB_CREATE_FROM_FF(?,?,?,?,?,?)}");

            cs.setLong(1, groupPoid);
            cs.setLong(2, companyPoid);
            cs.setLong(3, userPoid);
            cs.setLong(4, ffRefPoid);

            cs.registerOutParameter(5, Types.VARCHAR);
            cs.registerOutParameter(6, OracleTypes.CURSOR);

            return cs;
        }, (CallableStatementCallback<List<ChargeFFDto>>) cs -> {
            cs.execute();

            String status = trimOrNull(cs.getString(5));
            if (status != null && (status.contains("ERROR") || status.contains("WARNING"))) {
                throw new ValidationException(status);
            }

            try (ResultSet rs = (ResultSet) cs.getObject(6)) {
                return mapFromResultSetToChargeFFDto(rs);
            }
        });
    }

    @Override
    public List<ChargeFDADto> procLoadFDACharges(Long groupPoid, Long userPoid, Long companyPoid, Long fdaRefPoid) {
        return jdbcTemplate.execute((CallableStatementCreator) con -> {
            CallableStatement cs = con.prepareCall("{call PROC_BANK_DEB_CREATE_FROM_FDA(?,?,?,?,?,?)}");

            cs.setLong(1, groupPoid);
            cs.setLong(2, companyPoid);
            cs.setLong(3, userPoid);
            cs.setLong(4, fdaRefPoid);

            cs.registerOutParameter(5, Types.VARCHAR);
            cs.registerOutParameter(6, OracleTypes.CURSOR);

            return cs;
        }, (CallableStatementCallback<List<ChargeFDADto>>) cs -> {
            cs.execute();

            String status = trimOrNull(cs.getString(5));
            if (status != null && (status.contains("ERROR") || status.contains("WARNING"))) {
                throw new ValidationException(status);
            }

            try (ResultSet rs = (ResultSet) cs.getObject(6)) {
                return mapFromResultSetToChargeFDADto(rs);
            }
        });
    }

    @Override
    public List<ItemDetailDto> procLoadMTAItems(Long groupPoid, Long userPoid, Long companyPoid, Long mtaRefPoid) {
        return jdbcTemplate.execute((CallableStatementCreator) con -> {
            CallableStatement cs = con.prepareCall("BEGIN PROC_BANK_DEB_CREATE_FROM_MTA(?,?,?,?,?,?); END;");
            cs.setObject(1, groupPoid);
            cs.setObject(2, companyPoid);
            cs.setObject(3, userPoid);
            cs.setObject(4, mtaRefPoid);
            cs.registerOutParameter(5, Types.VARCHAR);
            cs.registerOutParameter(6, OracleTypes.CURSOR);
            return cs;
        }, (CallableStatementCallback<List<ItemDetailDto>>) cs -> {
            cs.execute();

            String status = trimOrNull(cs.getString(5));
            if (status != null && (status.contains("ERROR") || status.contains("WARNING")))
                throw new ValidationException(status);

            ResultSet rs = (ResultSet) cs.getObject(6);
            return mapFromResultSetToItemDetailDto(rs);
        });
    }

    @Override
    public BigDecimal procGetBankBalance(Long groupPoid, Long userPoid, Long companyPoid, String documentId, LocalDate docDate, Long bankPoid) {
        return jdbcTemplate.execute((CallableStatementCreator) con -> {

            CallableStatement cs =
                    con.prepareCall("BEGIN PROC_GL_GET_BANK_BALANCE(?,?,?,?,?,?,?,?,?); END;");

            // IN params
            cs.setLong(1, groupPoid);              // P_LOGIN_GROUP_POID
            cs.setLong(2, companyPoid);              // P_LOGIN_COMPANY_POID
            cs.setLong(3, userPoid);              // P_LOGIN_USER_POID
            cs.setString(4, documentId);              // P_DOC_ID
            cs.setNull(5, Types.NUMERIC);              // P_DOC_KEY_POID
            cs.setDate(6, Date.valueOf(docDate));                 // P_DOC_DATE
            cs.setObject(7, bankPoid);                 // P_BANK_POID

            // OUT params
            cs.registerOutParameter(8, Types.NUMERIC); // P_CURRENT_BALANCE
            cs.registerOutParameter(9, Types.NUMERIC); // P_AVAILABLE_BALANCE

            return cs;

        }, (CallableStatementCallback<BigDecimal>) cs -> {

            cs.execute();

            // You return CURRENT balance
            return cs.getBigDecimal(8);
        });
    }


    @Override
    public String procGetBeneficiaryName(Long groupPoid,Long userPoid, Long companyPoid,String documentId, Long beneficiaryId) {
        return jdbcTemplate.execute((CallableStatementCreator) con -> {

            // 7 parameters => 6 IN + 1 OUT
            CallableStatement cs =
                    con.prepareCall("BEGIN PROC_GL_GET_BENEFICIARY_NAME(?,?,?,?,?,?,?); END;");

            // 1. P_LOGIN_GROUP_POID
            cs.setLong(1, groupPoid);

            // 2. P_LOGIN_COMPANY_POID
            cs.setLong(2, companyPoid);

            // 3. P_LOGIN_USER_POID
            cs.setLong(3, userPoid);

            // 4. P_DOC_ID
            cs.setString(4, documentId);

            // 5. P_PAY_GL_POID
            cs.setNull(5, Types.NUMERIC);

            // 6. P_BENEFICIARY_ID (your IBAN)
            cs.setObject(6, beneficiaryId);

            // 7. OUT param => P_BENEFICIARY_NAME
            cs.registerOutParameter(7, Types.VARCHAR);

            return cs;

        }, (CallableStatementCallback<String>) cs -> {

            cs.execute();

            // OUT param value
            return trimOrNull(cs.getString(7));
        });
    }

    List<ChargeDetailDto> mapFromResultSetToChargeDetailDto(ResultSet rs) throws SQLException {
        List<ChargeDetailDto> list = new ArrayList<>();

        while (rs.next()) {

            ChargeDetailDto dto = new ChargeDetailDto();

            dto.setTransactionPoid(rs.getLong("TRANSACTION_POID"));
            if (rs.wasNull()) dto.setTransactionPoid(null);

            dto.setDetRowId(rs.getLong("DET_ROW_ID"));
            if (rs.wasNull()) dto.setDetRowId(null);

            dto.setChargePoid(rs.getLong("CHARGE_POID"));
            if (rs.wasNull()) dto.setChargePoid(null);

            dto.setChargeAmount(rs.getBigDecimal("CHARGE_AMOUNT"));
            dto.setDescription(rs.getString("DESCRIPTION"));
            dto.setRemarks(rs.getString("REMARKS"));
            dto.setRefDocId(rs.getString("REF_DOC_ID"));

            dto.setRefDocPoid(rs.getLong("REF_DOC_POID"));
            if (rs.wasNull()) dto.setRefDocPoid(null);

            dto.setFdaDetRowId(rs.getLong("FDA_DET_ROW_ID"));
            if (rs.wasNull()) dto.setFdaDetRowId(null);

            dto.setCheckAll(rs.getString("CHECK_ALL"));

            dto.setPdaAmount(rs.getBigDecimal("PDA_AMOUNT"));
            dto.setFfAmount(rs.getBigDecimal("FF_AMOUNT"));

            dto.setTaxPoid(rs.getLong("TAX_POID"));
            if (rs.wasNull()) dto.setTaxPoid(null);

            dto.setTaxPercentage(rs.getBigDecimal("TAX_PERCENTAGE"));
            dto.setTaxAmount(rs.getBigDecimal("TAX_AMOUNT"));
            dto.setChargeBaseAmount(rs.getBigDecimal("CHARGE_BASE_AMOUNT"));

            list.add(dto);
        }
        return list;
    }

    List<ChargeFFDto> mapFromResultSetToChargeFFDto(ResultSet rs) throws SQLException {
        List<ChargeFFDto> list = new ArrayList<>();

        while (rs.next()) {

            ChargeFFDto dto = new ChargeFFDto();

            dto.setChargePoid(rs.getLong("CHARGE_POID"));
            dto.setChargeAmount(rs.getBigDecimal("CHARGE_AMOUNT"));
            dto.setFfAmount(rs.getBigDecimal("FF_AMOUNT"));
            dto.setRefDocId(rs.getString("REF_DOC_ID"));
            dto.setRefDocPoid(rs.getLong("REF_DOC_POID"));
            dto.setFdaDetRowId(rs.getLong("FDA_DET_ROW_ID"));

            list.add(dto);
        }
        return list;
    }

    List<ChargeFDADto> mapFromResultSetToChargeFDADto(ResultSet rs) throws SQLException {
        List<ChargeFDADto> list = new ArrayList<>();

        while (rs.next()) {

            ChargeFDADto dto = new ChargeFDADto();

            dto.setChargePoid(rs.getLong("CHARGE_POID"));
            dto.setPdaAmount(rs.getBigDecimal("PDA_AMOUNT"));
            dto.setRemarks(rs.getString("REMARKS"));
            dto.setRefDocId(rs.getString("REF_DOC_ID"));
            dto.setRefDocPoid(rs.getLong("REF_DOC_POID"));
            dto.setFdaDetRowId(rs.getLong("FDA_DET_ROW_ID"));

            list.add(dto);
        }
        return list;
    }

    private List<ItemDetailDto> mapFromResultSetToItemDetailDto(ResultSet rs) throws SQLException {
        List<ItemDetailDto> list = new ArrayList<>();

        while (rs.next()) {
            ItemDetailDto dto = new ItemDetailDto();

            dto.setTransactionPoid(rs.getLong("TRANSACTION_POID"));
            if (rs.wasNull()) dto.setTransactionPoid(null);

            dto.setDetRowId(rs.getLong("DET_ROW_ID"));
            if (rs.wasNull()) dto.setDetRowId(null);

            dto.setStockPoid(rs.getLong("STOCK_POID"));
            if (rs.wasNull()) dto.setStockPoid(null);

            dto.setStockUnitPoid(rs.getLong("STOCK_UNIT_POID"));
            if (rs.wasNull()) dto.setStockUnitPoid(null);

            dto.setPoQty(rs.getBigDecimal("PO_QTY"));
            dto.setDnQty(rs.getBigDecimal("DN_QTY"));
            dto.setQtyReceived(rs.getBigDecimal("QTY_RECEIVED"));
            dto.setPrice(rs.getBigDecimal("PRICE"));
            dto.setDiscount(rs.getBigDecimal("DISCOUNT"));
            dto.setTotal(rs.getBigDecimal("TOTAL"));

            dto.setRemarks(rs.getString("REMARKS"));
            dto.setRefDocId(rs.getString("REF_DOC_ID"));

            dto.setRefDocPoid(rs.getLong("REF_DOC_POID"));
            if (rs.wasNull()) dto.setRefDocPoid(null);

            dto.setRefDetRowId(rs.getLong("REF_DET_ROW_ID"));
            if (rs.wasNull()) dto.setRefDetRowId(null);

            list.add(dto);
        }
        return list;
    }
}