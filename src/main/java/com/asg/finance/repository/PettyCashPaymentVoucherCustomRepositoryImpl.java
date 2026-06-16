package com.asg.finance.repository;

import com.asg.finance.dto.AdvanceDetailDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PettyCashPaymentVoucherCustomRepositoryImpl implements PettyCashPaymentVoucherCustomRepository{

    @PersistenceContext
    private EntityManager entityManager;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void validateGlVouchers(Long loginGroupPoid, Long loginUserPoid, Long loginCompanyPoid,
                                   String docId, String refType, String refPoid, StringBuilder result) {
        try {
            // Create StoredProcedureQuery to execute the validation procedure
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_VOUCHERS_VALIDATIONS");

            // Register input parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_REF_TYPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_REF_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Set the input parameters
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_REF_TYPE", refType);
            query.setParameter("P_REF_POID", refPoid);

            // Execute the procedure
            query.execute();

            // Get the result (output parameter)
            String resultValue = (String) query.getOutputParameterValue("P_RESULT");

            // Set the result to the StringBuilder
            if (result != null) {
                result.append(resultValue);
            }

        } catch (Exception e) {
            log.error("Error validating GL voucher for DOC_ID {} and REF_POID {}: {}", docId, refPoid, e.getMessage());
            throw new RuntimeException("Failed to validate GL voucher", e);
        }
    }

    @Override
    public String validateVoucherBeforeDelete(Long loginGroupPoid,
                                              Long loginUserPoid,
                                              Long loginCompanyPoid,
                                              String docId,
                                              String refType,
                                              String refPoid) {
        try {
            log.info("Executing PROC_GL_VOUCHERS_VALIDATIONS for docId={}, refType={}, refPoid={}",
                    docId, refType, refPoid);

            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_VOUCHERS_VALIDATIONS");

            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_REF_TYPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_REF_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_REF_TYPE", refType);
            query.setParameter("P_REF_POID", refPoid);

            query.execute();

            String result = (String) query.getOutputParameterValue("P_RESULT");

            log.info("Validation result from PROC_GL_VOUCHERS_VALIDATIONS: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Error executing PROC_GL_VOUCHERS_VALIDATIONS: {}", e.getMessage(), e);
            throw new RuntimeException("Error validating petty cash voucher: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateCostFF(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid,
            Long piPoid,
            StringBuilder result
    ) {
        try {
            // Create a stored procedure query for the PROC_AP_PI_FF_UPDATE_COST procedure
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_AP_PI_FF_UPDATE_COST");

            // Register input parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_FF_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PI_POID", Long.class, ParameterMode.IN);

            // Register output parameter
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Set input parameters
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_FF_POID", ffPoid);
            query.setParameter("P_PI_POID", piPoid);

            // Execute the stored procedure
            query.execute();

            // Get output parameter
            String resultOut = (String) query.getOutputParameterValue("P_RESULT");

            // Append result to StringBuilder
            result.append(resultOut);

        } catch (Exception e) {
            log.error("Error executing PROC_AP_PI_FF_UPDATE_COST", e);
            if (result != null) {
                result.setLength(0);
                result.append("ERROR : ").append(e.getMessage());
            }
        }
    }

    @Override
    public void updateCostFDA(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String fdaPoid,
            Long piPoid,
            StringBuilder result
    ) {
        try {
            // Create a stored procedure query for the PROC_AP_PI_FDA_UPDATE_COST procedure
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_AP_PI_FDA_UPDATE_COST");

            // Register input parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_FDA_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PI_POID", Long.class, ParameterMode.IN);

            // Register output parameter
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Set input parameters
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_FDA_POID", fdaPoid);
            query.setParameter("P_PI_POID", piPoid);

            // Execute the stored procedure
            query.execute();

            // Get output parameter
            String resultOut = (String) query.getOutputParameterValue("P_RESULT");

            // Append result to StringBuilder
            result.append(resultOut);

        } catch (Exception e) {
            log.error("Error executing PROC_AP_PI_FDA_UPDATE_COST", e);
            if (result != null) {
                result.setLength(0);
                result.append("ERROR : ").append(e.getMessage());
            }
        }
    }

    @Override
    public void updatePurchaseOrderStatus(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid,
            Long bookPoid,
            StringBuilder resultOut
    ) {
        try {
            // Create stored procedure query for PROC_AP_PO_UPDATE_STATUS
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_AP_PO_UPDATE_STATUS");

            // Register input parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PO_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_BOOK_POID", Long.class, ParameterMode.IN);

            // Register output parameter
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Set input parameters
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_PO_POID", poPoid);
            query.setParameter("P_BOOK_POID", bookPoid);

            // Execute the stored procedure
            query.execute();

            // Retrieve output
            String result = (String) query.getOutputParameterValue("P_RESULT");

            resultOut.append(result != null ? result : "");

        } catch (Exception e) {
            log.error("Error executing PROC_AP_PO_UPDATE_STATUS", e);
            if (resultOut != null) {
                resultOut.setLength(0);
                resultOut.append("ERROR : ").append(e.getMessage());
            }
        }
    }

    @Override
    public void validateBeforeSave(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            String docId,
            String refType,
            String glRefPoid,
            String glRefPoid2,
            String glRefPoid3,
            String partyType,
            Long partyPoid,
            StringBuilder taxInputGlPoid,
            StringBuilder result
    ) {
        try {
            // Create a stored procedure query to call the PL/SQL procedure
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_DTL_BEFORE_SAVE_VAL_V2");

            // Register the input parameters
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

            // Register the output parameters
            query.registerStoredProcedureParameter("P_TAX_INPUT_GL_POID", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Set the input parameters
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

            // Execute the stored procedure
            query.execute();

            // Get the output parameters and assign them to the provided variables
            String taxInputGlPoidOut = (String) query.getOutputParameterValue("P_TAX_INPUT_GL_POID");
            String resultOut = (String) query.getOutputParameterValue("P_RESULT");

            taxInputGlPoid.append(taxInputGlPoidOut);
            result.append(resultOut);

        } catch (Exception e) {

            throw new RuntimeException("Error executing PROC_GL_DTL_BEFORE_SAVE_VAL_V2", e);
        }
    }

    @Override
    public void getOldJobReferences(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            String docId,
            String transactionPoid,
            StringBuilder refTypeOut,
            StringBuilder refPoidOut
    ) {
        log.info("[OldJobRef] PROC_GL_JOB_REL_OLD_VALUES inputs: groupPoid={} userPoid={} companyPoid={} docId='{}' transactionPoid='{}'",
                loginGroupPoid, loginUserPoid, loginCompanyPoid, docId, transactionPoid);
        try {
            // Create stored procedure query for PROC_GL_JOB_REL_OLD_VALUES
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_JOB_REL_OLD_VALUES");

            // Register input parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TRANSACTION_POID", String.class, ParameterMode.IN);

            // Register output parameters
            query.registerStoredProcedureParameter("P_REF_TYPE", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("P_REF_POID", String.class, ParameterMode.OUT);

            // Set input parameter values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_TRANSACTION_POID", transactionPoid);

            // Execute the stored procedure
            query.execute();

            // Fetch output values
            String refType = (String) query.getOutputParameterValue("P_REF_TYPE");
            String refPoid = (String) query.getOutputParameterValue("P_REF_POID");
            log.info("[OldJobRef] PROC_GL_JOB_REL_OLD_VALUES raw outputs: P_REF_TYPE='{}' P_REF_POID='{}'", refType, refPoid);

            // Append output to provided StringBuilders
            refTypeOut.append(refType != null ? refType : "");
            refPoidOut.append(refPoid != null ? refPoid : "");

        } catch (Exception e) {

            throw new RuntimeException("Error executing PROC_GL_JOB_REL_OLD_VALUES", e);
        }
    }

    @Override
    public void validateJobBeforeSave(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            String docId,
            String refType,
            String refPoid,
            StringBuilder result
    ) {
        try {
            // Create a stored procedure query for PROC_GL_JOB_VAL_BEFORE_SAVE
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_JOB_VAL_BEFORE_SAVE");

            // Register all procedure parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_REF_TYPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_REF_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Set input parameters
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_REF_TYPE", refType);
            query.setParameter("P_REF_POID", refPoid);

            // Execute the stored procedure
            query.execute();

            // Fetch the OUT parameter
            String resultOut = (String) query.getOutputParameterValue("P_RESULT");
            result.append(resultOut != null ? resultOut : "");


        } catch (Exception e) {

            throw new RuntimeException("Error executing PROC_GL_JOB_VAL_BEFORE_SAVE", e);
        }
    }

    @Override
    public void loadAdvanceDetails(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            BigDecimal amount,
            String advancePoid,
            StringBuilder result,
            List<AdvanceDetailDto> outData
    ) {
        jdbcTemplate.execute(
                con -> {
                    java.sql.CallableStatement cs = con.prepareCall("BEGIN PROC_GL_PETTY_ADVANCE_DTLLOAD(?,?,?,?,?,?,?); END;");
                    cs.setObject(1, loginGroupPoid);
                    cs.setObject(2, loginCompanyPoid);
                    cs.setObject(3, loginUserPoid);
                    cs.setObject(4, amount);
                    cs.setObject(5, advancePoid);
                    cs.registerOutParameter(6, Types.VARCHAR);
                    cs.registerOutParameter(7, OracleTypes.CURSOR);
                    return cs;
                },
                (org.springframework.jdbc.core.CallableStatementCallback<Void>) cs -> {
                    cs.execute();
                    String resultOut = cs.getString(6);
                    if (resultOut != null) {
                        result.append(resultOut);
                    }
                    try (ResultSet rs = (ResultSet) cs.getObject(7)) {
                        if (rs != null) {
                            while (rs.next()) {
                                AdvanceDetailDto advanceDetail = new AdvanceDetailDto();
                                advanceDetail.setAdvanceAmount(rs.getBigDecimal("ADVANCE_AMOUNT"));
                                advanceDetail.setAdvanceStatus(rs.getString("ADVANCE_STATUS"));
                                outData.add(advanceDetail);
                            }
                        }
                    }
                    return null;
                }
        );
    }

    @Override
    public void updateRfqPurchasePrice(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String rfqPoid,
            StringBuilder resultOut
    ) {
        try {
            // Create stored procedure query
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_RFQ_UPDATE_PURCHASE_PRICE");

            // Register input parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RFQ_POID", String.class, ParameterMode.IN);

            // Register output parameter
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Set parameter values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_RFQ_POID", rfqPoid);

            // Execute stored procedure
            query.execute();

            // Get OUT parameter
            String result = (String) query.getOutputParameterValue("P_RESULT");
            resultOut.append(result != null ? result : "");
        } catch (Exception e) {
            log.error("Error executing PROC_RFQ_UPDATE_PURCHASE_PRICE", e);
            if (resultOut != null) {
                resultOut.setLength(0);
                resultOut.append("ERROR : ").append(e.getMessage());
            }
        }
    }

    @Override
    public void updateSalesGrnStatus(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String docId,
            Long bookPoid,
            StringBuilder resultOut
    ) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SALES_GRN_UPDATE_STATUS");

            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_BOOK_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_BOOK_POID", bookPoid);
            query.execute();

            String result = (String) query.getOutputParameterValue("P_RESULT");
            resultOut.append(result != null ? result : "");
        } catch (Exception e) {
            log.error("Error executing PROC_SALES_GRN_UPDATE_STATUS", e);
            if (resultOut != null) {
                resultOut.setLength(0);
                resultOut.append("ERROR : ").append(e.getMessage());
            }
        }
    }

    @Override
    public String getRefTypeWhereClause(Long loginUserPoid) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_PETTY_REF_WHERE_CLAUSE");

            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);

            query.execute();

            String result = (String) query.getOutputParameterValue("P_RESULT");
            log.info("PROC_GL_PETTY_REF_WHERE_CLAUSE executed successfully. Result: {}", result);
            return result != null ? result : "";

        } catch (Exception e) {
            log.error("Error executing PROC_GL_PETTY_REF_WHERE_CLAUSE: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get ref type where clause: " + e.getMessage(), e);
        }
    }
}
