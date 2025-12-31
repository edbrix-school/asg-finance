package com.asg.finance.repository;

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

    @Override
    public void createSchedule(Long groupPoid, Long userPoid, Long companyPoid, Long transactionPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_RJV_CREATE_SCHEDULE");
        
        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RJV_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
        
        query.setParameter("P_LOGIN_GROUP_POID", groupPoid);
        query.setParameter("P_LOGIN_USER_POID", userPoid);
        query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
        query.setParameter("P_RJV_POID", transactionPoid);
        
        query.execute();
        
        String result = (String) query.getOutputParameterValue("P_RESULT");
        log.info("Schedule created for recurring JV: {}, Result: {}", transactionPoid, result);
    }

    @Override
    public void deleteSchedule(Long groupPoid, Long userPoid, Long companyPoid, Long transactionPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_RJV_DELETE_SCHEDULE");
        
        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RJV_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
        
        query.setParameter("P_LOGIN_GROUP_POID", groupPoid);
        query.setParameter("P_LOGIN_USER_POID", userPoid);
        query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
        query.setParameter("P_RJV_POID", transactionPoid);
        
        query.execute();
        
        String result = (String) query.getOutputParameterValue("P_RESULT");
        log.info("Schedule deleted for recurring JV: {}, Result: {}", transactionPoid, result);
    }
}
