package com.asg.finance.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Transactional
public class ApPaymentRequestCustomRepositoryImpl implements ApPaymentRequestCustomRepository{

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<String, Object> createFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid
    ) {

        StoredProcedureQuery sp = entityManager
                .createStoredProcedureQuery("PROC_AP_PR_CREATE_FROM_PO");

        // ===== IN parameters =====
        sp.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_PO_POID", String.class, ParameterMode.IN);

        // ===== OUT parameters =====
        sp.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        // ===== Set IN values =====
        sp.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
        sp.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
        sp.setParameter("P_LOGIN_USER_POID", loginUserPoid);
        sp.setParameter("P_PO_POID", poPoid);

        // ===== Execute =====
        sp.execute();

        // ===== Read OUT params =====
        String resultMessage = (String) sp.getOutputParameterValue("P_RESULT");

        @SuppressWarnings("unchecked")
        List<Object[]> rows = sp.getResultList();

        List<Map<String, Object>> data = new ArrayList<>();

        for (Object[] row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("STOCK_POID", row[0]);
            map.put("STOCK_UNIT_POID", row[1]);
            map.put("PO_QTY", row[2]);
            map.put("PRICE", row[3]);
            map.put("DISCOUNT", row[4]);
            map.put("BASE_AMOUNT", row[5]);
            map.put("TAX_POID", row[6]);
            map.put("TAX_PERCENTAGE", row[7]);
            map.put("TAX_AMOUNT", row[8]);
            map.put("AMOUNT", row[9]);
            map.put("REMARKS", row[10]);
            map.put("REF_DOC_ID", row[11]);
            map.put("REF_DOC_POID", row[12]);
            map.put("REF_DET_ROW_ID", row[13]);

            data.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", resultMessage);
        response.put("records", data);

        return response;
    }

    @Override
    public Map<String, Object> createFromFf(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid
    ) {

        StoredProcedureQuery sp = entityManager
                .createStoredProcedureQuery("PROC_AP_PR_CREATE_FROM_FF");

        // ===== IN parameters =====
        sp.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_FF_POID", String.class, ParameterMode.IN);

        // ===== OUT parameters =====
        sp.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        // ===== Set IN values =====
        sp.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
        sp.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
        sp.setParameter("P_LOGIN_USER_POID", loginUserPoid);
        sp.setParameter("P_FF_POID", ffPoid);

        // ===== Execute =====
        sp.execute();

        // ===== Read OUT message =====
        String resultMessage =
                (String) sp.getOutputParameterValue("P_RESULT");

        // ===== Read cursor =====
        @SuppressWarnings("unchecked")
        List<Object[]> rows = sp.getResultList();

        List<Map<String, Object>> records = new ArrayList<>();

        for (Object[] row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("CHARGE_POID", row[0]);
            map.put("CHARGE_BASE_AMOUNT", row[1]);
            map.put("FF_AMOUNT", row[2]);
            map.put("REF_DOC_ID", row[3]);
            map.put("REF_DOC_POID", row[4]);
            map.put("FDA_DET_ROW_ID", row[5]);
            map.put("TAX_POID", row[6]);
            map.put("TAX_PERCENTAGE", row[7]);
            map.put("TAX_AMOUNT", row[8]);
            map.put("CHARGE_AMOUNT", row[9]);

            records.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", resultMessage);
        response.put("records", records);

        return response;
    }

    @Override
    public Map<String, Object> createFromFda(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String fdaPoid) {

        StoredProcedureQuery sp = entityManager
                .createStoredProcedureQuery("PROC_AP_PR_CREATE_FROM_FDA");

        sp.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_FDA_POID", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        sp.setParameter("P_LOGIN_GROUP_POID", groupPoid);
        sp.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
        sp.setParameter("P_LOGIN_USER_POID", userPoid);
        sp.setParameter("P_FDA_POID", fdaPoid);

        sp.execute();

        return buildResponse(sp);
    }

    // ================= COMMON RESPONSE BUILDER =================
    private Map<String, Object> buildResponse(StoredProcedureQuery sp) {

        Map<String, Object> response = new HashMap<>();

        String message = (String) sp.getOutputParameterValue("P_RESULT");
        List<Object[]> records = sp.getResultList();

        response.put("message", message);
        response.put("records", records == null ? Collections.emptyList() : records);

        return response;
    }

    @Override
    public Map<String, Object> createFromMta(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String poPoid) {

        StoredProcedureQuery sp = entityManager
                .createStoredProcedureQuery("PROC_AP_PR_CREATE_FROM_MTA");

        sp.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_PO_POID", String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        sp.setParameter("P_LOGIN_GROUP_POID", groupPoid);
        sp.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
        sp.setParameter("P_LOGIN_USER_POID", userPoid);
        sp.setParameter("P_PO_POID", poPoid);

        sp.execute();

        return buildResponse(sp);
    }
}
