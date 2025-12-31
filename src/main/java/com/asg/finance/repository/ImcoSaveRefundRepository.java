package com.asg.finance.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

@Repository
public class ImcoSaveRefundRepository {

    @PersistenceContext
    private EntityManager em;

    public String callImcoRefundLoadProcedure(
            Long groupPoid,
            Long companyPoid,
            String loginUser,
            String receiptNum,
            String blNumber
    ) {
        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_IMCO_REFUND_LOAD");

        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, void.class, ParameterMode.REF_CURSOR);
        query.registerStoredProcedureParameter(7, void.class, ParameterMode.REF_CURSOR);
        query.registerStoredProcedureParameter(8, String.class, ParameterMode.OUT);

        query.setParameter(1, groupPoid);
        query.setParameter(2, companyPoid);
        query.setParameter(3, loginUser);
        query.setParameter(4, receiptNum);
        query.setParameter(5, blNumber);

        query.execute();

        return (String) query.getOutputParameterValue(8);
    }
}

