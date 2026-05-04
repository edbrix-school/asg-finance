package com.asg.finance.repository;

import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class ApPurchaseCnProcRepositoryImpl implements ApPurchaseCnProcRepository {

    private static final String P_LOGIN_GROUP_POID = "P_LOGIN_GROUP_POID";
    private static final String P_LOGIN_COMPANY_POID = "P_LOGIN_COMPANY_POID";
    private static final String P_LOGIN_USER_POID = "P_LOGIN_USER_POID";
    private static final String P_RESULT = "P_RESULT";
    private static final String OUTDATA = "OUTDATA";
    private static final String P_PJ_POID = "P_PJ_POID";
    private static final String P_PJ_REF_TYPE = "P_PJ_REF_TYPE";
    private static final String P_PJ_REF_DETAILS = "P_PJ_REF_DETAILS";
    private static final String P_CN_PJ_PARTY_TYPE = "P_CN_PJ_PARTY_TYPE";
    private static final String P_CN_PJ_PARTY_POID = "P_CN_PJ_PARTY_POID";
    private static final String P_CN_PJ_REF_TYPE = "P_CN_PJ_REF_TYPE";
    private static final String ERROR = "ERROR";
    private static final String WARNING = "WARNING";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<String, Object> getPjRefDetails(Long pjPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_AP_CN_PJ_REF_DETAILS");
        query.registerStoredProcedureParameter(P_LOGIN_GROUP_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_COMPANY_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_USER_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_PJ_POID, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_PJ_REF_TYPE, String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter(P_PJ_REF_DETAILS, String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter(P_RESULT, String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter(OUTDATA, void.class, ParameterMode.REF_CURSOR);
        
        setBaseParameters(query);
        query.setParameter(P_PJ_POID, String.valueOf(pjPoid));
        query.execute();

        validateResult(query);

        String pjRefType = (String) query.getOutputParameterValue(P_PJ_REF_TYPE);
        String pjRefDetails = (String) query.getOutputParameterValue(P_PJ_REF_DETAILS);
        ResultSet rs = (ResultSet) query.getOutputParameterValue(OUTDATA);

        Map<String, Object> response = new HashMap<>();
        response.put("pjRefType", pjRefType);
        response.put("pjRefDetails", pjRefDetails);
        response.put("lineItems", parseResultSet(rs));

        return response;
    }

    @Override
    public Map<String, Object> getPartyDetails(String partyType, Long partyPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_AP_CN_PJ_PARTY_DTLS");
        query.registerStoredProcedureParameter(P_LOGIN_GROUP_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_COMPANY_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_USER_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_CN_PJ_PARTY_TYPE, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_CN_PJ_PARTY_POID, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_RESULT, String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter(OUTDATA, void.class, ParameterMode.REF_CURSOR);

        setBaseParameters(query);
        query.setParameter(P_CN_PJ_PARTY_TYPE, partyType);
        query.setParameter(P_CN_PJ_PARTY_POID, String.valueOf(partyPoid));
        query.execute();

        validateResult(query);

        ResultSet rs = (ResultSet) query.getOutputParameterValue(OUTDATA);
        List<Map<String, Object>> data = parseResultSet(rs);

        Map<String, Object> response = new HashMap<>();
        if (!data.isEmpty()) {
            response.putAll(data.get(0));
        }
        return response;
    }

    @Override
    public void beforeSaveValidation(String partyType, Long partyPoid, String refType, Long pjPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_AP_CN_PJ_BEFORE_SAVE");
        query.registerStoredProcedureParameter(P_LOGIN_GROUP_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_COMPANY_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_USER_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_CN_PJ_PARTY_TYPE, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_CN_PJ_PARTY_POID, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_CN_PJ_REF_TYPE, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_PJ_POID, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_RESULT, String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter(OUTDATA, void.class, ParameterMode.REF_CURSOR);

        setBaseParameters(query);
        query.setParameter(P_CN_PJ_PARTY_TYPE, partyType);
        query.setParameter(P_CN_PJ_PARTY_POID, partyPoid != null ? String.valueOf(partyPoid) : null);
        query.setParameter(P_CN_PJ_REF_TYPE, refType);
        query.setParameter(P_PJ_POID, pjPoid != null ? String.valueOf(pjPoid) : null);
        query.execute();

        validateResult(query);
    }

    private StoredProcedureQuery createBaseQuery(String procedureName) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery(procedureName);
        query.registerStoredProcedureParameter(P_LOGIN_GROUP_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_COMPANY_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_USER_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_RESULT, String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter(OUTDATA, void.class, ParameterMode.REF_CURSOR);
        return query;
    }

    private void setBaseParameters(StoredProcedureQuery query) {
        query.setParameter(P_LOGIN_GROUP_POID, UserContext.getGroupPoid());
        query.setParameter(P_LOGIN_COMPANY_POID, UserContext.getCompanyPoid());
        query.setParameter(P_LOGIN_USER_POID, UserContext.getUserPoid());
    }

    private void validateResult(StoredProcedureQuery query) {
        String result = (String) query.getOutputParameterValue(P_RESULT);
        if (result != null && (result.contains(ERROR) || result.contains(WARNING))) {
            throw new ValidationException(result);
        }
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
            log.error("Error parsing result set", e);
            throw new ValidationException("Error parsing procedure result: " + e.getMessage());
        }
        return results;
    }
}
