package com.asg.finance.repository;

import com.asg.common.lib.security.util.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class DebitNoteCustomRepositoryImpl implements DebitNoteCustomRepository {

    private final EntityManager em;

    @Override
    public Map<String, Object> loadFdaCharges(Long fdaPoid) {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("PROC_AR_DEBIT_CREATE_FROM_FDA_V2");
        
        sp.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_FDA_POID", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_PRINCIPAL_POID", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_TRANSACTION_DATE", java.sql.Date.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_FDA_REF", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        sp.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        sp.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        sp.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        sp.setParameter("P_FDA_POID", String.valueOf(fdaPoid));
        sp.setParameter("P_PRINCIPAL_POID", null);
        sp.setParameter("P_TRANSACTION_DATE", new java.sql.Date(System.currentTimeMillis()));
        sp.setParameter("P_FDA_REF", null);
        
        sp.execute();

        String result = (String) sp.getOutputParameterValue("P_RESULT");
        
        if (result != null && result.contains("WARNING")) {
            return Map.of("warning", result);
        }

        Object cursor = sp.getOutputParameterValue("OUTDATA");
        if (cursor instanceof java.sql.ResultSet rs) {
            try {
                java.util.List<Map<String, Object>> charges = new java.util.ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> charge = new java.util.HashMap<>();
                    charge.put("chargePoid", rs.getObject("CHARGE_POID"));
                    charge.put("chargeAmount", rs.getObject("CHARGE_AMOUNT"));
                    charge.put("pdaAmount", rs.getObject("PDA_AMOUNT"));
                    charge.put("remarks", rs.getObject("REMARKS"));
                    charge.put("refDocId", rs.getObject("REF_DOC_ID"));
                    charge.put("refDocPoid", rs.getObject("REF_DOC_POID"));
                    charge.put("fdaDetRowId", rs.getObject("FDA_DET_ROW_ID"));
                    charge.put("taxPercentage", rs.getObject("TAX_PERCENTAGE"));
                    charge.put("taxPoid", rs.getObject("TAX_POID"));
                    charges.add(charge);
                }
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("charges", charges);
                response.put("message", result);
                return response;
            } catch (Exception e) {
                throw new RuntimeException("Error reading FDA charges", e);
            }
        }
        return Map.of("message", result);
    }

    @Override
    public Map<String, Object> getTaxPercentage(Long chargeId, String partyType, Long partyPoid) {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("PROC_GET_CHARGE_TAX_PER_V3");
        
        sp.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_TRANSACTION_DATE", java.sql.Date.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_PARTY_TYPE", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_PARTY_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_CHARGE_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        sp.setParameter("P_COMPANY_POID", UserContext.getCompanyPoid());
        sp.setParameter("P_TRANSACTION_DATE", new java.sql.Date(System.currentTimeMillis()));
        sp.setParameter("P_PARTY_TYPE", partyType);
        sp.setParameter("P_PARTY_POID", partyPoid);
        sp.setParameter("P_CHARGE_POID", chargeId);
        
        sp.execute();

        Object cursor = sp.getOutputParameterValue("OUTDATA");
        if (cursor instanceof java.sql.ResultSet rs) {
            try {
                if (rs.next()) {
                    return Map.of(
                        "taxPoid", rs.getObject("TAX_POID"),
                        "percentage", rs.getObject("PERCENTAGE")
                    );
                }
            } catch (Exception e) {
                throw new RuntimeException("Error reading tax percentage", e);
            }
        }
        return Map.of();
    }

    @Override
    public void updateCostAmount(Long transactionPoid) {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("PROC_AR_DEBIT_UPDATE_COST_AMT");
        
        sp.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_DEBIT_POID", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

        sp.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        sp.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        sp.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        sp.setParameter("P_DEBIT_POID", String.valueOf(transactionPoid));
        
        sp.execute();
        
        String result = (String) sp.getOutputParameterValue("P_RESULT");
        if (result != null && result.contains("ERROR")) {
            throw new RuntimeException("Update cost amount failed: " + result);
        }
    }

    @Override
    public Map<String, Object> checkSailDate(Long fdaPoid) {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("PROC_DN_GET_FDA_SAIL_DATE");
        
        sp.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_FDA_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_SAIL_DATE", java.sql.Date.class, ParameterMode.OUT);

        sp.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        sp.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        sp.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        sp.setParameter("P_FDA_POID", fdaPoid);
        
        sp.execute();

        return Map.of("sailDate", sp.getOutputParameterValue("P_SAIL_DATE"));
    }

    @Override
    public Map<String, Object> getPartyDefaults(Long partyPoid, String partyType) {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("PROC_PI_SET_DEFAULT_CREDIT");

        sp.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOV_NAME", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOV_VALUE", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        sp.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        sp.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        sp.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        sp.setParameter("P_DOC_ID", "300-110");
        sp.setParameter("P_DOC_KEY_POID", null);
        sp.setParameter("P_LOV_NAME", partyType);
        sp.setParameter("P_LOV_VALUE", partyPoid);

        sp.execute();

        Object cursor = sp.getOutputParameterValue("OUTDATA");
        if (cursor instanceof java.sql.ResultSet rs) {
            try {
                if (rs.next()) {
                    return Map.of(
                        "creditPeriod", rs.getObject("CREDIT_PERIOD"),
                        "bankPoid", rs.getObject("BANK_POID"),
                        "tinNumber", rs.getObject("TIN_NUMBER"),
                        "currencyCode", rs.getObject("CURRENCY_CODE"),
                        "currencyRate", rs.getObject("CURRENCY_RATE")
                    );
                }
            } catch (Exception e) {
                throw new RuntimeException("Error reading party defaults", e);
            }
        }
        return Map.of();
    }

    @Override
    public void validateDebitNote(String refType, String partyType, Long partyPoid, Long fdaPoid, String poRef) {

        StoredProcedureQuery sp = em.createStoredProcedureQuery("PROC_GL_VOUCHERS_VALIDATIONS");

        sp.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_REF_TYPE", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_REF_POID", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

        sp.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        sp.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        sp.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        sp.setParameter("P_DOC_ID", "300-110");  // debit note doc id
        sp.setParameter("P_REF_TYPE", refType);

        // P_REF_POID is VARCHAR2 in procedure
        sp.setParameter("P_REF_POID", fdaPoid != null ? fdaPoid.toString() : null);

        sp.execute();

        String result = (String) sp.getOutputParameterValue("P_RESULT");

        // Throw error only if result is not SUCCESS
        if ("CLOSED".equalsIgnoreCase(result)) {
            throw new RuntimeException("Corresponding FDA is closed, can not edit...");
        }
    }

}

