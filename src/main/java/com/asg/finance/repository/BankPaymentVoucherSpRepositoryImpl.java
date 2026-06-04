package com.asg.finance.repository;

import com.asg.common.lib.security.util.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class BankPaymentVoucherSpRepositoryImpl implements BankPaymentVoucherSpRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void validateBeforeSave(Long transactionPoid, Long groupPoid, Long companyPoid, String userCode, String suppressBalanceCheck) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_GL_BANK_PAY_BEF_SAVE_VAL");

        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(7, String.class, ParameterMode.OUT);

        query.setParameter(1, groupPoid);
        query.setParameter(2, companyPoid);
        query.setParameter(3, transactionPoid);
        query.setParameter(4, suppressBalanceCheck != null ? suppressBalanceCheck : "N");
        query.setParameter(5, null);
        query.setParameter(6, transactionPoid);

        query.execute();

        String result = (String) query.getOutputParameterValue(7);
        if (result != null && !result.equals("SUCESS")) {
            throw new RuntimeException(result);
        }
    }

    @Override
    public String validateJob(Long groupPoid, Long userPoid, Long companyPoid, String docId, String refType, String refPoid) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_GL_JOB_VAL_BEFORE_SAVE");
        
        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(7, String.class, ParameterMode.OUT);
        
        query.setParameter(1, groupPoid);
        query.setParameter(2, userPoid);
        query.setParameter(3, companyPoid);
        query.setParameter(4, docId);
        query.setParameter(5, refType);
        query.setParameter(6, refPoid);
        
        query.execute();
        
        return (String) query.getOutputParameterValue(7);
    }

    @Override
    public Map<String, BigDecimal> getBankBalance(String docId,
                                                  Long docKeyPoid,
                                                  LocalDate docDate,
                                                  Long bankPoid) {

        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_GL_GET_BANK_BALANCE");

        // Register parameters
        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_DATE", java.sql.Date.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_BANK_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_CURRENT_BALANCE", BigDecimal.class, jakarta.persistence.ParameterMode.OUT);
        query.registerStoredProcedureParameter("P_AVAILABLE_BALANCE", BigDecimal.class, jakarta.persistence.ParameterMode.OUT);

        // Set input parameters
        query.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        query.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        query.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        query.setParameter("P_DOC_ID", docId);
        query.setParameter("P_DOC_KEY_POID", docKeyPoid);
        query.setParameter("P_DOC_DATE", docDate);
        query.setParameter("P_BANK_POID", bankPoid);

        // Execute the procedure
        query.execute();

        // Fetch output
        BigDecimal currentBalance = (BigDecimal) query.getOutputParameterValue("P_CURRENT_BALANCE");
        BigDecimal availableBalance = (BigDecimal) query.getOutputParameterValue("P_AVAILABLE_BALANCE");

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("currentBalance", currentBalance);
        result.put("availableBalance", availableBalance);

        return result;
    }

    @Override
    public String getNextChequeNumber(Long bankPoid) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("GET_NEXT_CHEQUE_NUMBER");
        
        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, String.class, ParameterMode.OUT);
        
        query.setParameter(1, bankPoid);
        query.execute();
        
        return (String) query.getOutputParameterValue(2);
    }

    @Override
    public void updateFdaCost(Long groupPoid, Long companyPoid, Long userPoid, String fdaPoid, Long piPoid) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_AP_PI_FDA_UPDATE_COST");
        
        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, String.class, ParameterMode.OUT);
        
        query.setParameter(1, groupPoid);
        query.setParameter(2, companyPoid);
        query.setParameter(3, userPoid);
        query.setParameter(4, fdaPoid);
        query.setParameter(5, piPoid);
        
        query.execute();
        
        String result = (String) query.getOutputParameterValue(6);
        if (result != null && !result.contains("SUCESS")) {
            throw new RuntimeException(result);
        }
    }

    @Override
    public void updateFfCost(Long groupPoid, Long companyPoid, Long userPoid, String ffPoid, Long piPoid) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_AP_PI_FF_UPDATE_COST");
        
        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, String.class, ParameterMode.OUT);
        
        query.setParameter(1, groupPoid);
        query.setParameter(2, companyPoid);
        query.setParameter(3, userPoid);
        query.setParameter(4, ffPoid);
        query.setParameter(5, piPoid);
        
        query.execute();
        
        String result = (String) query.getOutputParameterValue(6);
        if (result != null && !result.contains("SUCCESS")) {
            throw new RuntimeException(result);
        }
    }

    @Override
    public void updateMtaCost(Long groupPoid, Long companyPoid, Long userPoid, Long transactionPoid, String rfqPoid) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_BANK_MTA_UPDATE");
        
        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, String.class, ParameterMode.OUT);
        
        query.setParameter(1, groupPoid);
        query.setParameter(2, companyPoid);
        query.setParameter(3, userPoid);
        query.setParameter(4, transactionPoid);
        query.setParameter(5, rfqPoid);
        
        query.execute();
        
        String result = (String) query.getOutputParameterValue(6);
        if (result != null && !result.contains("SUCCESS")) {
            throw new RuntimeException(result);
        }
    }

    @Override
    public Map<String, String> validateBeforeChequePrint(Long groupPoid, String loginUser, Long companyPoid, Long bankPoid, String chqSignType, Long transactionPoid, String suppressBalanceCheck) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_GL_BANK_BEFORE_CHEQ_PRINT");
        
        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(7, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(8, String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter(9, String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter(10, String.class, ParameterMode.OUT);
        
        query.setParameter(1, groupPoid);
        query.setParameter(2, loginUser);
        query.setParameter(3, companyPoid != null ? String.valueOf(companyPoid) : null);
        query.setParameter(4, bankPoid);
        query.setParameter(5, chqSignType);
        query.setParameter(6, transactionPoid);
        query.setParameter(7, suppressBalanceCheck);
        
        query.execute();
        
        Map<String, String> result = new HashMap<>();
        result.put("result", (String) query.getOutputParameterValue(8));
        result.put("nextChequeNumber", (String) query.getOutputParameterValue(9));
        result.put("defaultPrinter", (String) query.getOutputParameterValue(10));
        
        String validationResult = result.get("result");
        if (validationResult != null && !validationResult.equals("SUCCESS")) {
            throw new RuntimeException(validationResult);
        }
        
        return result;
    }

    @Override
    public void afterChequePrint(Long groupPoid, String loginUser, Long companyPoid, Long transactionPoid, Long bankPoid, String chqSignType, Long userPoid) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_GL_BANK_AFTER_CHEQ_PRINT");
        
        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(7, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(8, String.class, ParameterMode.OUT);
        
        query.setParameter(1, groupPoid);
        query.setParameter(2, loginUser);
        query.setParameter(3, companyPoid);
        query.setParameter(4, transactionPoid);
        query.setParameter(5, bankPoid);
        query.setParameter(6, chqSignType);
        query.setParameter(7, userPoid);
        
        query.execute();
        
        String result = (String) query.getOutputParameterValue(8);
        if (result != null && !result.equals("SUCCESS")) {
            throw new RuntimeException(result);
        }
    }

    @Override
    public void releaseCheque(Long groupPoid, String loginUser, Long companyPoid, Long transactionPoid, String releasedTo, String contact) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_GL_BANK_CHEQUE_RELEASE");
        
        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(7, String.class, ParameterMode.OUT);
        
        query.setParameter(1, groupPoid);
        query.setParameter(2, loginUser);
        query.setParameter(3, companyPoid);
        query.setParameter(4, transactionPoid);
        query.setParameter(5, releasedTo);
        query.setParameter(6, contact);
        
        query.execute();
        
        String result = (String) query.getOutputParameterValue(7);
        if (result != null && !result.equals("SUCCESS")) {
            throw new RuntimeException(result);
        }
    }

    @Override
    public void unReleaseCheque(Long groupPoid, Long loginUser, Long companyPoid, Long transactionPoid) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_GL_BANK_CHEQUE_UN_RELEASE");
        
        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, String.class, ParameterMode.OUT);
        
        query.setParameter(1, groupPoid);
        query.setParameter(2, loginUser);
        query.setParameter(3, companyPoid);
        query.setParameter(4, transactionPoid);
        
        query.execute();
        
        String result = (String) query.getOutputParameterValue(5);
        if (result != null && !result.equals("SUCCESS")) {
            throw new RuntimeException(result);
        }
    }

    @Override
    public void resetChequeStatus(Long groupPoid, Long companyPoid, Long userPoid, Long docKeyPoid) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_GL_BANK_CHQ_STATUS_RESET");
        
        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, String.class, ParameterMode.OUT);
        
        query.setParameter(1, groupPoid);
        query.setParameter(2, companyPoid);
        query.setParameter(3, userPoid);
        query.setParameter(4, docKeyPoid);
        
        query.execute();
        
        String result = (String) query.getOutputParameterValue(5);
        if (result != null && !result.equals("SUCCESS")) {
            throw new RuntimeException(result);
        }
    }

    @Override
    public String revertReconciliation(Long groupPoid, Long companyPoid, Long userPoid, String docId, String transactionPoid, String mailAlert, String comments) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_GL_BANK_RECON_REVERT_V2");

        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter(7, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(8, String.class, ParameterMode.IN);

        query.setParameter(1, groupPoid);
        query.setParameter(2, companyPoid);
        query.setParameter(3, userPoid);
        query.setParameter(4, docId);
        query.setParameter(5, transactionPoid);
        query.setParameter(7, mailAlert != null ? mailAlert : "Y");
        query.setParameter(8, comments);

        query.execute();

        String result = (String) query.getOutputParameterValue(6);


        if (result != null && result.startsWith("SUCCESS")) {
            return result;
        } else {
            throw new RuntimeException(result);
        }
    }

    @Override
    public void releaseOldJobValues(Long groupPoid, Long userPoid, Long companyPoid, String docId, String transactionPoid) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_GL_JOB_REL_OLD_VALUES");
        
        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter(7, String.class, ParameterMode.OUT);
        
        query.setParameter(1, groupPoid);
        query.setParameter(2, userPoid);
        query.setParameter(3, companyPoid);
        query.setParameter(4, docId);
        query.setParameter(5, transactionPoid);
        
        query.execute();
    }

    @Override
    public String validateVoucherStatus(Long groupPoid, Long userPoid, Long companyPoid, String docId, String refType, String refPoid) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_GL_VOUCHERS_VALIDATIONS");
        
        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(7, String.class, ParameterMode.OUT);
        
        query.setParameter(1, groupPoid);
        query.setParameter(2, userPoid);
        query.setParameter(3, companyPoid);
        query.setParameter(4, docId);
        query.setParameter(5, refType);
        query.setParameter(6, refPoid);
        
        query.execute();
        
        return (String) query.getOutputParameterValue(7);
    }

    @Override
    public Map<String, Object> getBankBeneficiary(String docId, Long docKeyPoid, String selectedPayGlPoid) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_GL_GET_BANK_BENEFICIARY");

        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(7, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(8, void.class, ParameterMode.REF_CURSOR);

        query.setParameter(1, UserContext.getGroupPoid());
        query.setParameter(2, UserContext.getCompanyPoid());
        query.setParameter(3, UserContext.getUserPoid());
        query.setParameter(4, docId);
        query.setParameter(5, docKeyPoid);
        query.setParameter(6, "GL_MASTER_LEDGERS");
        query.setParameter(7, selectedPayGlPoid);

        query.execute();

        ResultSet rs = (ResultSet) query.getOutputParameterValue(8);
        List<Map<String, Object>> data = parseResultSet(rs);

        Map<String, Object> response = new HashMap<>();
        if (!data.isEmpty()) {
            Map<String, Object> firstRow = data.get(0);
            for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
                response.put(toCamelCase(entry.getKey()), entry.getValue());
            }
        }
        return response;
    }

    private List<Map<String, Object>> parseResultSet(ResultSet rs) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (rs == null) return results;

        try {
            int columnCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rs.getMetaData().getColumnName(i);
                    row.put(columnName, rs.getObject(i));
                }
                results.add(row);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing procedure result: " + e.getMessage());
        }
        return results;
    }

    private String toCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return snakeCase;
        }
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (int i = 0; i < snakeCase.length(); i++) {
            char c = snakeCase.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    sb.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    sb.append(Character.toLowerCase(c));
                }
            }
        }
        return sb.toString();
    }
}
