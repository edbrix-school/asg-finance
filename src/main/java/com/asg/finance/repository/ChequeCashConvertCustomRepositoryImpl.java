package com.asg.finance.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

@Repository
public class ChequeCashConvertCustomRepositoryImpl implements ChequeCashConvertCustomRepository{

    @PersistenceContext
    private EntityManager em;

    @Override
    public String convertChequeAfterSave(
            Long groupPoid,
            Long companyPoid,
            Long transactionPoid,
            String docRef,
            Long loginUserPoid,
            String loginUser
    ) {

        StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_CHEQUE_CONVERT_AFTER_SAVE");


        query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(5, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);

        // OUT parameter
        query.registerStoredProcedureParameter(7, String.class, ParameterMode.OUT); // STATUS OUT

        // Set input parameter values
        query.setParameter(1, groupPoid);
        query.setParameter(2, companyPoid);
        query.setParameter(3, transactionPoid);
        query.setParameter(4, docRef);
        query.setParameter(5, loginUserPoid);
        query.setParameter(6, loginUser);

        // Execute
        query.execute();

        // Read out result
        String status = (String) query.getOutputParameterValue(7);

        return status;
    }
}
