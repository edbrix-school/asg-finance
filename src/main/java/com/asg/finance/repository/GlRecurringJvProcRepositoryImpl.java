package com.asg.finance.repository;

import com.asg.common.lib.exception.AsgException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class GlRecurringJvProcRepositoryImpl implements GlRecurringJvProcRepository {

    @PersistenceContext
    private EntityManager entityManager;


    private static final String P_LOGIN_GROUP_POID = "P_LOGIN_GROUP_POID";
    private static final String P_LOGIN_USER_POID = "P_LOGIN_USER_POID";
    private static final String P_LOGIN_COMPANY_POID = "P_LOGIN_COMPANY_POID";
    private static final String P_RESULT = "P_RESULT";

    private static final String PROC_RJV_CREATE_SCHEDULE = "PROC_GL_RJV_CREATE_SCHEDULE";
    private static final String PROC_RJV_DELETE_SCHEDULE = "PROC_GL_RJV_DELETE_SCHEDULE";

    @Override
    public void createSchedule(Long groupPoid, Long userPoid, Long companyPoid, Long transactionPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery(PROC_RJV_CREATE_SCHEDULE);
        
        query.registerStoredProcedureParameter(P_LOGIN_GROUP_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_USER_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_COMPANY_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RJV_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_RESULT, String.class, ParameterMode.OUT);
        
        query.setParameter(P_LOGIN_GROUP_POID, groupPoid);
        query.setParameter(P_LOGIN_USER_POID, userPoid);
        query.setParameter(P_LOGIN_COMPANY_POID, companyPoid);
        query.setParameter("P_RJV_POID", transactionPoid);
        
        query.execute();
        
        String result = (String) query.getOutputParameterValue(P_RESULT);
        log.info("Schedule created for recurring JV: {}, Result: {}", transactionPoid, result);
        if (result != null && result.toUpperCase().contains("ERROR")) {
            throw new AsgException("Failed to create schedule: " + result);
        }
    }

    @Override
    public void deleteSchedule(Long groupPoid, Long userPoid, Long companyPoid, Long transactionPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery(PROC_RJV_DELETE_SCHEDULE);
        
        query.registerStoredProcedureParameter(P_LOGIN_GROUP_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_USER_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_COMPANY_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RJV_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_RESULT, String.class, ParameterMode.OUT);
        
        query.setParameter(P_LOGIN_GROUP_POID, groupPoid);
        query.setParameter(P_LOGIN_USER_POID, userPoid);
        query.setParameter(P_LOGIN_COMPANY_POID, companyPoid);
        query.setParameter("P_RJV_POID", transactionPoid);
        
        query.execute();
        
        String result = (String) query.getOutputParameterValue(P_RESULT);
        log.info("Schedule deleted for recurring JV: {}, Result: {}", transactionPoid, result);
        if (result != null && result.toUpperCase().contains("ERROR")) {
            throw new AsgException("Failed to delete schedule: " + result);
        }
    }



}
