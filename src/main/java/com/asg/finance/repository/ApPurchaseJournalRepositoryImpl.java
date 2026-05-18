package com.asg.finance.repository;

import com.asg.finance.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import com.asg.common.lib.security.util.UserContext;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Repository
@RequiredArgsConstructor
public class ApPurchaseJournalRepositoryImpl implements ApPurchaseJournalRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final DataSource dataSource;

    @Override
    public List<ApPurchaseJournalResponseDto> createFromFda(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String fdaPoid,
            StringBuilder result
    ) {

        List<ApPurchaseJournalResponseDto> responseList = new ArrayList<>();

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_AP_PI_CREATE_FROM_FDA_NEW");

            // Register IN parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_FDA_POID", String.class, ParameterMode.IN);

            // Register OUT parameters
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

            // Assign IN parameter values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_FDA_POID", fdaPoid);

            // Execute procedure
            query.execute();

            // Capture output message
            String resultMsg = (String) query.getOutputParameterValue("P_RESULT");
            if (result != null) result.append(resultMsg);

            // Process OUTDATA cursor
            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
            responseList = mapToApPiDto(rs);

            log.info("PROC_AP_PI_CREATE_FROM_FDA_NEW executed successfully. Message: {}", resultMsg);

        } catch (Exception e) {
            log.error("Error executing PROC_AP_PI_CREATE_FROM_FDA_NEW: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load PI charges from FDA: " + e.getMessage(), e);
        }

        return responseList;
    }

    // Mapper Method
    private List<ApPurchaseJournalResponseDto> mapToApPiDto(ResultSet rs) throws SQLException {
        List<ApPurchaseJournalResponseDto> list = new ArrayList<>();

        while (rs != null && rs.next()) {
            ApPurchaseJournalResponseDto dto = new ApPurchaseJournalResponseDto();

            dto.setChargePoid(rs.getLong("CHARGE_POID"));
            dto.setChargeBaseAmount(rs.getDouble("CHARGE_BASE_AMOUNT"));
            dto.setPdaAmount(rs.getDouble("PDA_AMOUNT"));
            dto.setRemarks(rs.getString("REMARKS"));
            dto.setRefDocId(rs.getString("REF_DOC_ID"));
            dto.setRefDocPoid(rs.getString("REF_DOC_POID"));
            dto.setFdaDetRowId(rs.getLong("FDA_DET_ROW_ID"));
            dto.setTaxPoid(rs.getLong("TAX_POID"));
            dto.setTaxPercentage(rs.getDouble("TAX_PERCENTAGE"));
            dto.setTaxAmount(rs.getDouble("TAX_AMOUNT"));
            dto.setChargeAmount(rs.getDouble("CHARGE_AMOUNT"));

            list.add(dto);
        }

        return list;
    }

    @Override
    public List<ApPurchaseJournalResponseDto> createFromFf(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid,
            StringBuilder result
    ) {

        List<ApPurchaseJournalResponseDto> responseList = new ArrayList<>();

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_AP_PI_CREATE_FROM_FF_NEW");

            // Register IN params
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_FF_POID", String.class, ParameterMode.IN);

            // Register OUT params
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

            // Set values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_FF_POID", ffPoid);

            // Execute
            query.execute();

            // Capture result message
            String resultMsg = (String) query.getOutputParameterValue("P_RESULT");
            if (result != null) result.append(resultMsg);

            // Process cursor
            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
            responseList = mapToDto(rs);

            log.info("PROC_AP_PI_CREATE_FROM_FF_NEW executed successfully. Message: {}", resultMsg);

        } catch (Exception e) {
            log.error("Error executing PROC_AP_PI_CREATE_FROM_FF_NEW: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load PI charges from FF: " + e.getMessage(), e);
        }

        return responseList;
    }

    private List<ApPurchaseJournalResponseDto> mapToDto(ResultSet rs) throws SQLException {
        List<ApPurchaseJournalResponseDto> list = new ArrayList<>();

        while (rs != null && rs.next()) {
            ApPurchaseJournalResponseDto dto = new ApPurchaseJournalResponseDto();

            dto.setChargePoid(rs.getLong("CHARGE_POID"));
            dto.setChargeBaseAmount(rs.getDouble("CHARGE_BASE_AMOUNT"));
            dto.setFfAmount(rs.getDouble("FF_AMOUNT"));
            dto.setRefDocId(rs.getString("REF_DOC_ID"));
            dto.setRefDocPoid(rs.getString("REF_DOC_POID"));
            dto.setFdaDetRowId(rs.getLong("FDA_DET_ROW_ID"));
            dto.setTaxPoid(rs.getLong("TAX_POID"));
            dto.setTaxPercentage(rs.getDouble("TAX_PERCENTAGE"));
            dto.setTaxAmount(rs.getDouble("TAX_AMOUNT"));
            dto.setChargeAmount(rs.getDouble("CHARGE_AMOUNT"));

            list.add(dto);
        }

        return list;
    }




    @Override
    public String updateFfCost(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid,
            Long piPoid
    ) {

        String resultMessage = "";

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_AP_PI_FF_UPDATE_COST");

            // Register IN parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_FF_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PI_POID", Long.class, ParameterMode.IN);

            // Register OUT parameter
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Assign IN values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_FF_POID", ffPoid);
            query.setParameter("P_PI_POID", piPoid);

            // Execute stored procedure
            query.execute();

            // Capture the OUT message
            resultMessage = (String) query.getOutputParameterValue("P_RESULT");

            log.info("PROC_AP_PI_FF_UPDATE_COST executed. Result: {}", resultMessage);

        } catch (Exception e) {
            log.error("Error executing PROC_AP_PI_FF_UPDATE_COST: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update FF cost: " + e.getMessage(), e);
        }

        return resultMessage;
    }

    @Override
    public String updateFdaCost(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String fdaPoid,
            Long piPoid
    ) {

        String resultMsg = "";

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_AP_PI_FDA_UPDATE_COST");

            // Register IN parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_FDA_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PI_POID", Long.class, ParameterMode.IN);

            // Register OUT parameter
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Assign IN values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_FDA_POID", fdaPoid);
            query.setParameter("P_PI_POID", piPoid);

            // Execute
            query.execute();

            // Capture OUT result
            resultMsg = (String) query.getOutputParameterValue("P_RESULT");

            log.info("PROC_AP_PI_FDA_UPDATE_COST executed. Result: {}", resultMsg);

        } catch (Exception e) {
            log.error("Error executing PROC_AP_PI_FDA_UPDATE_COST: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update FDA cost: " + e.getMessage(), e);
        }

        return resultMsg;
    }

    @Override
    public String validateVoucher(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            String docId,
            String refType,
            String refPoid
    ) {
        String result = "";

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_GL_VOUCHERS_VALIDATIONS");

            // Register IN params
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_REF_TYPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_REF_POID", String.class, ParameterMode.IN);

            // OUT param
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Set values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_REF_TYPE", refType);
            query.setParameter("P_REF_POID", refPoid);

            // Execute
            query.execute();

            result = (String) query.getOutputParameterValue("P_RESULT");

            log.info("PROC_GL_VOUCHERS_VALIDATIONS executed → {}", result);

        } catch (Exception e) {
            log.error("Error executing PROC_GL_VOUCHERS_VALIDATIONS: {}", e.getMessage(), e);
            throw new RuntimeException("Voucher validation failed: " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public String getSupplierPoidFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid
    ) {

        String supplierPoid = null;

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_GET_PO_SUPPLIER_POID");

            // Register IN parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PO_POID", String.class, ParameterMode.IN);

            // Register OUT parameter
            query.registerStoredProcedureParameter("P_SUPPLEIR_POID", String.class, ParameterMode.OUT);

            // Set input params
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_PO_POID", poPoid);

            // Execute procedure
            query.execute();

            // Read OUT value
            supplierPoid = (String) query.getOutputParameterValue("P_SUPPLEIR_POID");

            log.info("PROC_GET_PO_SUPPLIER_POID executed. SupplierPoid: {}", supplierPoid);

        } catch (Exception e) {
            log.error("Error executing PROC_GET_PO_SUPPLIER_POID: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch Supplier POID: " + e.getMessage(), e);
        }

        return supplierPoid;
    }

    @Override
    public String validateBeforeSave(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            String docId,
            String refType,
            String refPoid
    ) {

        String result = "";

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_GL_JOB_VAL_BEFORE_SAVE");

            // IN params
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_REF_TYPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_REF_POID", String.class, ParameterMode.IN);

            // OUT param
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Set values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_REF_TYPE", refType);
            query.setParameter("P_REF_POID", refPoid);

            // Execute
            query.execute();

            result = (String) query.getOutputParameterValue("P_RESULT");

            log.info("PROC_GL_JOB_VAL_BEFORE_SAVE executed → {}", result);

        } catch (Exception e) {
            log.error("Error executing PROC_GL_JOB_VAL_BEFORE_SAVE: {}", e.getMessage(), e);
            throw new RuntimeException("Job validation before save failed: " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public String updateMtaPoBookingDetails(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid,
            Long bookPoid
    ) {

        String result = "";

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_AP_MTA_PO_UPDATE_BKNG_DTL");

            // Register IN params
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PO_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_BOOK_POID", Long.class, ParameterMode.IN);

            // OUT param
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Set input values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_PO_POID", poPoid);
            query.setParameter("P_BOOK_POID", bookPoid);

            // Execute
            query.execute();

            // Output message
            result = (String) query.getOutputParameterValue("P_RESULT");

            log.info("PROC_AP_MTA_PO_UPDATE_BKNG_DTL executed → {}", result);

        } catch (Exception e) {
            log.error("Error executing PROC_AP_MTA_PO_UPDATE_BKNG_DTL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update MTA PO booking details: " + e.getMessage(), e);
        }

        return result;
    }


    @Override
    public String updateGeneralPoStatus(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid,
            Long bookPoid
    ) {

        String result = "";

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_AP_GEN_PO_UPDATE_STATUS");

            // Register IN params
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PO_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_BOOK_POID", Long.class, ParameterMode.IN);

            // Register OUT param
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Set params
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_PO_POID", poPoid);
            query.setParameter("P_BOOK_POID", bookPoid);

            // Execute
            query.execute();

            // Read result
            result = (String) query.getOutputParameterValue("P_RESULT");

            log.info("PROC_AP_GEN_PO_UPDATE_STATUS executed → {}", result);

        } catch (Exception e) {
            log.error("Error executing PROC_AP_GEN_PO_UPDATE_STATUS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update general PO status: " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public List<ApPiFromPoResponseDto> createPiFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid,
            StringBuilder resultMsg
    ) {

        List<ApPiFromPoResponseDto> responseList = new ArrayList<>();

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_AP_PI_CREATE_FROM_PO");

            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PO_POID", String.class, ParameterMode.IN);

            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_PO_POID", poPoid);

            query.execute();

          /*  String result = (String) query.getOutputParameterValue("P_RESULT");
            if (resultMsg != null) {
                resultMsg.append(result);
            }

            // 🔑 THIS LINE FIXES EVERYTHING
            @SuppressWarnings("unchecked")
            List<Object[]> rows = query.getResultList();*/
            String result = (String) query.getOutputParameterValue("P_RESULT");

            if (resultMsg != null) {
                resultMsg.append(result);
            }

            if (result != null &&
                    (result.startsWith("WARNING") || result.startsWith("ERROR"))) {

                log.warn("Procedure returned message: {}", result);
                return responseList; // return empty list
            }


            List<Object[]> rows = query.getResultList();

            for (Object[] row : rows) {
                responseList.add(
                        ApPiFromPoResponseDto.builder()
                                .stockPoid(row[0] != null ? ((Number) row[0]).longValue() : null)
                                .stockUnitPoid(row[1] != null ? ((Number) row[1]).longValue() : null)
                                .poQty((BigDecimal) row[2])
                                .price((BigDecimal) row[3])
                                .discount((BigDecimal) row[4])
                                .baseAmount((BigDecimal) row[5])
                                .taxPoid(row[6] != null ? ((Number) row[6]).longValue() : null)
                                .taxPercentage((BigDecimal) row[7])
                                .taxAmount((BigDecimal) row[8])
                                .amount((BigDecimal) row[9])
                                .remarks((String) row[10])
                                .refDocId((String) row[11])
                                .refDocPoid((String) row[12])
                                .refDetRowId(row[13] != null ? ((Number) row[13]).longValue() : null)
                                .build()
                );
            }

            log.info("PROC_AP_PI_CREATE_FROM_PO executed. Result={}, rows={}", result, responseList.size());

        } catch (Exception e) {
            log.error("Error executing PROC_AP_PI_CREATE_FROM_PO", e);
            throw new RuntimeException("Failed to create PI items from PO", e);
        }

        return responseList;
    }


    private List<ApPiFromPoResponseDto> mapToPoDto(ResultSet rs) throws SQLException {
        List<ApPiFromPoResponseDto> list = new ArrayList<>();

        while (rs != null && rs.next()) {
            ApPiFromPoResponseDto dto = new ApPiFromPoResponseDto();

            dto.setStockPoid(rs.getLong("STOCK_POID"));
            dto.setStockUnitPoid(rs.getLong("STOCK_UNIT_POID"));
            dto.setPoQty(rs.getBigDecimal("PO_QTY"));
            dto.setPrice(rs.getBigDecimal("PRICE"));
            dto.setDiscount(rs.getBigDecimal("DISCOUNT"));
            dto.setBaseAmount(rs.getBigDecimal("BASE_AMOUNT"));
            dto.setTaxPoid(rs.getLong("TAX_POID"));
            dto.setTaxPercentage(rs.getBigDecimal("TAX_PERCENTAGE"));
            dto.setTaxAmount(rs.getBigDecimal("TAX_AMOUNT"));
            dto.setAmount(rs.getBigDecimal("AMOUNT"));
            dto.setRemarks(rs.getString("REMARKS"));
            dto.setRefDocId(rs.getString("REF_DOC_ID"));
            dto.setRefDocPoid(rs.getString("REF_DOC_POID"));
            dto.setRefDetRowId(rs.getLong("REF_DET_ROW_ID"));

            list.add(dto);
        }

        return list;
    }

    @Override
    public List<ApPiFromGeneralPoResponseDto> createPiFromGeneralPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid,
            StringBuilder resultMsg
    ) {

        List<ApPiFromGeneralPoResponseDto> responseList = new ArrayList<>();

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_AP_PI_CREATE_FROM_GEN_PO");

            // IN params
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PO_POID", String.class, ParameterMode.IN);

            // OUT params
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

            // Set params
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_PO_POID", poPoid);

            // Execute
            query.execute();

            // Result message
            String result = (String) query.getOutputParameterValue("P_RESULT");
            if (resultMsg != null) resultMsg.append(result);

            // Read cursor
            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
            responseList = mapToGenPoDto(rs);

            log.info("PROC_AP_PI_CREATE_FROM_GEN_PO executed → {}", result);

        } catch (Exception e) {
            log.error("Error executing PROC_AP_PI_CREATE_FROM_GEN_PO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load PI GL details from General PO: " + e.getMessage(), e);
        }

        return responseList;
    }


    private List<ApPiFromGeneralPoResponseDto> mapToGenPoDto(ResultSet rs) throws SQLException {

        List<ApPiFromGeneralPoResponseDto> list = new ArrayList<>();

        while (rs != null && rs.next()) {
            ApPiFromGeneralPoResponseDto dto = new ApPiFromGeneralPoResponseDto();

            dto.setType(rs.getString("TYPE"));
            dto.setCompanyPoid(rs.getLong("COMPANY_POID"));
            dto.setGlPoid(rs.getLong("GL_POID"));
            dto.setDrAmount(rs.getBigDecimal("DR_AMOUNT"));
            dto.setCrAmount(rs.getBigDecimal("CR_AMOUNT"));
            dto.setRemarks(rs.getString("REMARKS"));
            dto.setRefDocId(rs.getString("REF_DOC_ID"));
            dto.setRefDocPoid(rs.getString("REF_DOC_POID"));

            list.add(dto);
        }

        return list;
    }

    @Override
    public List<ApPiFaDefaultDetailsDto> getFaDefaultDetails(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String faPoid
    ) {

        List<ApPiFaDefaultDetailsDto> responseList = new ArrayList<>();

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_AP_PI_FA_DEFAULT_DTLS");

            // Register params
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_FA_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

            // Set params
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_FA_POID", faPoid);

            // Execute
            query.execute();

            // Read cursor data
            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
            responseList = mapToAssetDto(rs);

        } catch (Exception e) {
            log.error("Error executing PROC_AP_PI_FA_DEFAULT_DTLS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch FA default details: " + e.getMessage(), e);
        }

        return responseList;
    }

    private List<ApPiFaDefaultDetailsDto> mapToAssetDto(ResultSet rs) throws SQLException {

        List<ApPiFaDefaultDetailsDto> list = new ArrayList<>();

        while (rs != null && rs.next()) {

            ApPiFaDefaultDetailsDto dto = new ApPiFaDefaultDetailsDto();

            dto.setFaDescription(rs.getString("FA_DESCRIPTION"));
            dto.setFaCategoryPoid(rs.getLong("FA_CATEGORY_POID"));
            dto.setCategoryDescription(rs.getString("CATEGORY_DESCRIPTION"));
            dto.setAssetType(rs.getString("ASSET_TYPE"));
            dto.setGrossValue(rs.getBigDecimal("GROSS_VALUE"));

            list.add(dto);
        }

        return list;
    }

    @Override
    public String checkDuplicatePi(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String partyType,
            Long partyPoid,
            String supplierInvNo,
            Long piPoid,
            String billType
    ) {

        String result = "";
        String remark = "";

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_AP_PI_CHECK_DUPLICATE_V2");

            // IN parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PARTY_TYPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PARTY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_SUP_INV_NO", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PI_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_BILL_TYPE", String.class, ParameterMode.IN);

            // OUT parameters
            query.registerStoredProcedureParameter("P_REMARK", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Set IN param values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_PARTY_TYPE", partyType);
            query.setParameter("P_PARTY_POID", partyPoid);
            query.setParameter("P_SUP_INV_NO", supplierInvNo);
            query.setParameter("P_PI_POID", piPoid);
            query.setParameter("P_BILL_TYPE", billType);

            // Execute
            query.execute();

            // OUT results
            remark = (String) query.getOutputParameterValue("P_REMARK");
            result = (String) query.getOutputParameterValue("P_RESULT");

            log.info("PROC_AP_PI_CHECK_DUPLICATE_V2 executed → RESULT={}, REMARK={}", result, remark);

        } catch (Exception e) {
            log.error("Error executing PROC_AP_PI_CHECK_DUPLICATE_V2: {}", e.getMessage(), e);
            throw new RuntimeException("Duplicate PI check failed: " + e.getMessage(), e);
        }

        // Return only the actual validation result message
        return result;
    }

    @Override
    public String validateInputVat(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String docId,
            Long docKeyPoid,
            String partyType,
            Long partyPoid,
            Double taxAmount
    ) {

        String result = "";

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_AP_PI_INPUT_VAT_VALTN");

            // Register IN parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PARTY_TYPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PARTY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TAX_AMOUNT", Double.class, ParameterMode.IN);

            // OUT param
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Set input values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_DOC_KEY_POID", docKeyPoid);
            query.setParameter("P_PARTY_TYPE", partyType);
            query.setParameter("P_PARTY_POID", partyPoid);
            query.setParameter("P_TAX_AMOUNT", taxAmount);

            // Execute
            query.execute();

            // Read output
            result = (String) query.getOutputParameterValue("P_RESULT");

            log.info("PROC_AP_PI_INPUT_VAT_VALTN executed → {}", result);

        } catch (Exception e) {
            log.error("Error executing PROC_AP_PI_INPUT_VAT_VALTN: {}", e.getMessage(), e);
            throw new RuntimeException("Input VAT validation failed: " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public Map<String, String> validateGlDetailBeforeSave(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            String docId,
            String refType,
            String glRefPoid,
            String glRefPoid2,
            String glRefPoid3,
            String partyType,
            Long partyPoid
    ) {

        Map<String, String> output = new HashMap<>();

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_GL_DTL_BEFORE_SAVE_VAL_V2");

            // IN params
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_REF_TYPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_GL_REF_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_GL_REF_POID2", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_GL_REF_POID3", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PARTY_TYPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PARTY_POID", Long.class, ParameterMode.IN);

            // OUT params
            query.registerStoredProcedureParameter("P_TAX_INPUT_GL_POID", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Set values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_REF_TYPE", refType);
            query.setParameter("P_GL_REF_POID", glRefPoid);
            query.setParameter("P_GL_REF_POID2", glRefPoid2);
            query.setParameter("P_GL_REF_POID3", glRefPoid3);
            query.setParameter("P_PARTY_TYPE", partyType);
            query.setParameter("P_PARTY_POID", partyPoid);

            // Execute
            query.execute();

            // Read output params
            String taxInputGlPoid = (String) query.getOutputParameterValue("P_TAX_INPUT_GL_POID");
            String result = (String) query.getOutputParameterValue("P_RESULT");

            output.put("taxInputGlPoid", taxInputGlPoid);
            output.put("result", result);

            log.info("PROC_GL_DTL_BEFORE_SAVE_VAL_V2 executed → RESULT={}, TAX_GL={}",
                    result, taxInputGlPoid);

        } catch (Exception e) {
            log.error("Error executing PROC_GL_DTL_BEFORE_SAVE_VAL_V2: {}", e.getMessage(), e);
            throw new RuntimeException("GL detail validation failed: " + e.getMessage(), e);
        }

        return output;
    }

    @Override
    public String checkOutstandingPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            Long supplierPoid) {

        String result;

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery(
                            "PROC_AP_PI_CHECK_OUTSTAND_PO");

            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_SUPPLIER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_SUPPLIER_POID", supplierPoid);

            query.execute();

            result = (String) query.getOutputParameterValue("P_RESULT");

            // 🔥 FIX: handle NULL from procedure
            if (result == null || result.trim().isEmpty()) {
                result = "No outstanding General PO found for this supplier.";
            }

            log.info("Outstanding PO check result: {}", result);

        } catch (Exception e) {
            log.error("Error executing PROC_AP_PI_CHECK_OUTSTAND_PO", e);
            throw new RuntimeException("Failed to check outstanding PO", e);
        }

        return result;
    }

    @Override
    public List<ApPurchaseInvRjvDefaultDto> fetchRjvDefaultDetails(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String rjvPoid) {

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        "PROC_AP_PI_RJV_DEFAULT_DTLS");

        // IN params
        query.registerStoredProcedureParameter(
                "P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(
                "P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(
                "P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(
                "P_RJV_POID", String.class, ParameterMode.IN);

        // OUT REF CURSOR
        query.registerStoredProcedureParameter(
                "OUTDATA", void.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_LOGIN_GROUP_POID", groupPoid);
        query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
        query.setParameter("P_LOGIN_USER_POID", userPoid);
        query.setParameter("P_RJV_POID", rjvPoid);

        query.execute();

        @SuppressWarnings("unchecked")
        List<Object[]> rows =
                query.getResultList();

        List<ApPurchaseInvRjvDefaultDto> result = new ArrayList<>();

        for (Object[] row : rows) {
            ApPurchaseInvRjvDefaultDto dto =
                    new ApPurchaseInvRjvDefaultDto();

            dto.setDrilldownLinkInfo((String) row[0]);
            dto.setRjvTrnDate(
                    row[1] != null
                            ? ((Timestamp) row[1]).toLocalDateTime()
                            : null);
            dto.setRjvDocRef((String) row[2]);
            dto.setRjvCompanyPoid(
                    row[3] != null ? ((Number) row[3]).longValue() : null);
            dto.setRjvRefType((String) row[4]);
            dto.setRjvAmount((BigDecimal) row[5]);
            dto.setRjvRemarks((String) row[6]);

            result.add(dto);
        }

        return result;
    }

    @Override
    public String getSupplierGlPoid(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String partyType,
            Long partyPoid
    ) {

        String glPoid = null;

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery(
                            "PROC_GL_GET_SUPPLIER_GLPOID_V2");

            // IN parameters
            query.registerStoredProcedureParameter(
                    "P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(
                    "P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(
                    "P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(
                    "P_PARTY_TYPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(
                    "P_PARTY_POID", Long.class, ParameterMode.IN);

            // OUT parameter
            query.registerStoredProcedureParameter(
                    "P_PARTY_GLPOID", String.class, ParameterMode.OUT);

            // Set values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_PARTY_TYPE", partyType);
            query.setParameter("P_PARTY_POID", partyPoid);

            // Execute
            query.execute();

            // Get OUT value
            glPoid = (String) query.getOutputParameterValue("P_PARTY_GLPOID");

            log.info("PROC_GL_GET_SUPPLIER_GLPOID_V2 executed → GL_POID={}", glPoid);

        } catch (Exception e) {
            log.error("Error executing PROC_GL_GET_SUPPLIER_GLPOID_V2: {}", e.getMessage(), e);
            throw new RuntimeException(
                    "Failed to fetch Supplier/Principal GL POID: " + e.getMessage(), e);
        }

        return glPoid;
    }

    @Override
    public Map<String, String> getGlJobRelOldValues(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            String docId,
            String transactionPoid
    ) {

        Map<String, String> output = new HashMap<>();

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_GL_JOB_REL_OLD_VALUES");

            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TRANSACTION_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_REF_TYPE", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("P_REF_POID", String.class, ParameterMode.OUT);

            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_TRANSACTION_POID", transactionPoid);

            query.execute();

            output.put("refType", (String) query.getOutputParameterValue("P_REF_TYPE"));
            output.put("refPoid", (String) query.getOutputParameterValue("P_REF_POID"));

            log.info("PROC_GL_JOB_REL_OLD_VALUES executed → refType={}, refPoid={}",
                    output.get("refType"), output.get("refPoid"));

        } catch (Exception e) {
            log.error("Error executing PROC_GL_JOB_REL_OLD_VALUES: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch GL job related old values: " + e.getMessage(), e);
        }

        return output;
    }

    private Long getLong(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }


}
