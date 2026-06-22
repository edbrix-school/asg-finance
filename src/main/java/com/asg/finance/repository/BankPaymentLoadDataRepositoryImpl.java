package com.asg.finance.repository;

import com.asg.finance.dto.*;
import com.asg.common.lib.security.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.internal.OracleTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class BankPaymentLoadDataRepositoryImpl implements BankPaymentLoadDataRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public BankPaymentLoadDataRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public BankPayCreateFromFfResponse executeBankPayFromFf(String ffPoid) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("PROC_BANK_PAY_CREATE_FROM_FF")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_LOGIN_GROUP_POID", OracleTypes.NUMBER),
                        new SqlParameter("P_LOGIN_COMPANY_POID", OracleTypes.NUMBER),
                        new SqlParameter("P_LOGIN_USER_POID", OracleTypes.NUMBER),
                        new SqlParameter("P_FF_POID", OracleTypes.VARCHAR),
                        new SqlOutParameter("P_RESULT", OracleTypes.VARCHAR),
                        new SqlOutParameter("OUTDATA", OracleTypes.CURSOR,
                                (rs, rowNum) -> mapFfItem(rs))
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        inParams.put("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        inParams.put("P_LOGIN_USER_POID", UserContext.getUserPoid());
        inParams.put("P_FF_POID", ffPoid);

        Map<String, Object> result = jdbcCall.execute(inParams);

        String resultMessage = (String) result.get("P_RESULT");
        log.info("PROC_BANK_PAY_CREATE_FROM_FF executed. ffPoid='{}' result='{}'", ffPoid, resultMessage);

        BankPayCreateFromFfResponse response = new BankPayCreateFromFfResponse();
        response.setResultMessage(resultMessage);
        response.setItems((List<BankPayFfItemDto>) result.get("OUTDATA"));
        return response;
    }

    private BankPayFfItemDto mapFfItem(ResultSet rs) throws SQLException {
        BankPayFfItemDto dto = new BankPayFfItemDto();
        dto.setChargePoid(rs.getLong("CHARGE_POID"));
        dto.setChargeAmount(rs.getBigDecimal("CHARGE_AMOUNT"));
        dto.setFfAmount(rs.getBigDecimal("FF_AMOUNT"));
        dto.setRefDocId(rs.getString("REF_DOC_ID"));
        dto.setRefDocPoid(rs.getString("REF_DOC_POID"));
        dto.setDetRowId(rs.getString("FDA_DET_ROW_ID"));
        return dto;
    }

   /* @Override
    public List<BankPaymentChargeDetailResponse> loadFdaCharges(Long fdaRefId) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_BANK_PAY_CREATE_FROM_FDA");

        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, void.class, ParameterMode.REF_CURSOR);

        query.setParameter(1, fdaRefId);
        query.execute();

        ResultSet rs = (ResultSet) query.getOutputParameterValue(2);
        List<Object[]> rows = query.getResultList();
        return mapRows(rows);
    }*/

    @Override
    public BankPayCreateFromFdaResponse executeBankPayFromFda(String fdaPoid) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("PROC_BANK_PAY_CREATE_FROM_FDA")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_LOGIN_GROUP_POID", OracleTypes.NUMBER),
                        new SqlParameter("P_LOGIN_COMPANY_POID", OracleTypes.NUMBER),
                        new SqlParameter("P_LOGIN_USER_POID", OracleTypes.NUMBER),
                        new SqlParameter("P_FDA_POID", OracleTypes.VARCHAR),
                        new SqlOutParameter("P_RESULT", OracleTypes.VARCHAR),
                        new SqlOutParameter("OUTDATA", OracleTypes.CURSOR,
                                (rs, rowNum) -> mapFdaItem(rs))
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        inParams.put("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        inParams.put("P_LOGIN_USER_POID", UserContext.getUserPoid());
        inParams.put("P_FDA_POID", fdaPoid);

        Map<String, Object> result = jdbcCall.execute(inParams);

        String resultMessage = (String) result.get("P_RESULT");
        log.info("PROC_BANK_PAY_CREATE_FROM_FDA executed. fdaPoid='{}' result='{}'", fdaPoid, resultMessage);

        BankPayCreateFromFdaResponse response = new BankPayCreateFromFdaResponse();
        response.setResultMessage(resultMessage);
        response.setItems((List<BankPayFdaItemDto>) result.get("OUTDATA"));
        return response;
    }

    private BankPayFdaItemDto mapFdaItem(ResultSet rs) throws SQLException {
        BankPayFdaItemDto dto = new BankPayFdaItemDto();
        dto.setChargePoid(rs.getLong("CHARGE_POID"));
        dto.setPdaAmount(rs.getBigDecimal("PDA_AMOUNT"));
        dto.setRemarks(rs.getString("REMARKS"));
        dto.setRefDocId(rs.getString("REF_DOC_ID"));
        dto.setRefDocPoid(rs.getString("REF_DOC_POID"));
        dto.setDetRowId(rs.getString("FDA_DET_ROW_ID"));
        return dto;
    }

   /* @Override
    public List<BankPaymentItemDetailResponse> loadMtaItems(Long mtaRfqId) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_BANK_PAY_CREATE_FROM_MTA");

        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, void.class, ParameterMode.REF_CURSOR);

        query.setParameter(1, mtaRfqId);
        query.execute();

        ResultSet rs = (ResultSet) query.getOutputParameterValue(2);
        return mapToItemDetails(rs);
    }*/

    @Override
    public BankPayCreateFromMtaResponse executeBankPayProc(String rfqPoid) {

        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("PROC_BANK_PAY_CREATE_FROM_MTA")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_LOGIN_GROUP_POID", OracleTypes.NUMBER),
                        new SqlParameter("P_LOGIN_COMPANY_POID", OracleTypes.NUMBER),
                        new SqlParameter("P_LOGIN_USER_POID", OracleTypes.NUMBER),
                        new SqlParameter("P_RFQ_POID", OracleTypes.VARCHAR),

                        new SqlOutParameter("P_RESULT", OracleTypes.VARCHAR),
                        new SqlOutParameter("OUTDATA", OracleTypes.CURSOR,
                                (rs, rowNum) -> mapBankPayItem(rs)
                        )
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        inParams.put("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        inParams.put("P_LOGIN_USER_POID", UserContext.getUserPoid());
        inParams.put("P_RFQ_POID", rfqPoid);

        Map<String, Object> result = jdbcCall.execute(inParams);

        BankPayCreateFromMtaResponse response = new BankPayCreateFromMtaResponse();
        response.setResultMessage((String) result.get("P_RESULT"));

        List<BankPayItemDto> items = (List<BankPayItemDto>) result.get("OUTDATA");
        response.setItems(items);

        return response;
    }

    private BankPayItemDto mapBankPayItem(ResultSet rs) throws SQLException {
        BankPayItemDto dto = new BankPayItemDto();
        dto.setStockPoid(rs.getLong("STOCK_POID"));
        dto.setStockUnitPoid(rs.getLong("STOCK_UNIT_POID"));
        dto.setPoQty(rs.getDouble("PO_QTY"));
        dto.setPrice(rs.getDouble("PRICE"));
        dto.setTotal(rs.getDouble("TOTAL"));
        dto.setRemarks(rs.getString("REMARKS"));
        dto.setRefDocId(rs.getString("REF_DOC_ID"));
        dto.setRefDocPoid(rs.getString("REF_DOC_POID"));
        dto.setRefDetRowId(rs.getString("REF_DET_ROW_ID"));
        return dto;
    }

    private List<BankPaymentChargeDetailResponse> mapRows(List<Object[]> rows) {

        List<BankPaymentChargeDetailResponse> list = new ArrayList<>();

        for (Object[] row : rows) {

            BankPaymentChargeDetailResponse dto = new BankPaymentChargeDetailResponse();

            dto.setChargePoid(((Number) row[0]).longValue());
            dto.setChargeAmount(BigDecimal.valueOf(((Number) row[1]).doubleValue()));
            dto.setFfAmount(BigDecimal.valueOf(((Number) row[2]).doubleValue()));

            dto.setRefDocId((String) row[3]);
            dto.setRefDocPoid(((Number) row[4]).longValue());

            dto.setDetRowId(((Number) row[5]).longValue());

            dto.setPdaAmount(BigDecimal.ZERO);
            dto.setRemarks(null);

            list.add(dto);
        }

        return list;
    }

    private List<BankPaymentItemDetailResponse> mapToItemDetails(ResultSet rs) {
        List<BankPaymentItemDetailResponse> list = new ArrayList<>();
        try {
            while (rs.next()) {
                BankPaymentItemDetailResponse dto = new BankPaymentItemDetailResponse();
                dto.setDetRowId(rs.getLong("DET_ROW_ID"));
                dto.setStockPoid(rs.getLong("STOCK_POID"));
                dto.setStockUnitPoid(rs.getLong("STOCK_UNIT_POID"));
                dto.setPrice(rs.getDouble("PRICE"));
                dto.setPoQty(rs.getDouble("PO_QTY"));
                dto.setTotal(rs.getDouble("TOTAL"));
                dto.setRemarks(rs.getString("REMARKS"));
                dto.setRefDocId(rs.getString("REF_DOC_ID"));
                dto.setRefDocPoid(rs.getLong("REF_DOC_POID"));
                list.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping item details", e);
        }
        return list;
    }
}
