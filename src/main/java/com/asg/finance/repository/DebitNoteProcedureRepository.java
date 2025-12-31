package com.asg.finance.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DebitNoteProcedureRepository {

    private final EntityManager entityManager;

    public String validateGlVouchers(Long groupPoid, Long companyPoid, Long userPoid, Long transactionPoid) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GL_VOUCHERS_VALIDATIONS")
                .registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT)
                .setParameter("P_GROUP_POID", groupPoid)
                .setParameter("P_COMPANY_POID", companyPoid)
                .setParameter("P_USER_POID", userPoid)
                .setParameter("P_TRANSACTION_POID", transactionPoid);

        query.execute();
        return (String) query.getOutputParameterValue("P_STATUS");
    }

    public String updateCostAmount(Long groupPoid, Long companyPoid, Long userPoid, Long transactionPoid) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_AR_DEBIT_UPDATE_COST_AMT")
                .registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT)
                .setParameter("P_GROUP_POID", groupPoid)
                .setParameter("P_COMPANY_POID", companyPoid)
                .setParameter("P_USER_POID", userPoid)
                .setParameter("P_TRANSACTION_POID", transactionPoid);

        query.execute();
        return (String) query.getOutputParameterValue("P_STATUS");
    }

    public String checkGlLedgerAccount(Long groupPoid, Long companyPoid, Long userPoid, Long transactionPoid) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_DN_CHECK_GL_L_A")
                .registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT)
                .setParameter("P_GROUP_POID", groupPoid)
                .setParameter("P_COMPANY_POID", companyPoid)
                .setParameter("P_USER_POID", userPoid)
                .setParameter("P_TRANSACTION_POID", transactionPoid);

        query.execute();
        return (String) query.getOutputParameterValue("P_STATUS");
    }

    public String validateBeforeSave(Long groupPoid, Long companyPoid, Long userPoid, Long transactionPoid) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_AR_DN_BEFORE_SAVE_VAL")
                .registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT)
                .setParameter("P_GROUP_POID", groupPoid)
                .setParameter("P_COMPANY_POID", companyPoid)
                .setParameter("P_USER_POID", userPoid)
                .setParameter("P_TRANSACTION_POID", transactionPoid);

        query.execute();
        return (String) query.getOutputParameterValue("P_STATUS");
    }

    public String updateBillReference(Long groupPoid, Long companyPoid, Long userPoid, Long transactionPoid) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_DR_CR_BILL_REF_UPDATE")
                .registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT)
                .setParameter("P_GROUP_POID", groupPoid)
                .setParameter("P_COMPANY_POID", companyPoid)
                .setParameter("P_USER_POID", userPoid)
                .setParameter("P_TRANSACTION_POID", transactionPoid);

        query.execute();
        return (String) query.getOutputParameterValue("P_STATUS");
    }

    public String updateFdaAmount(Long groupPoid, Long companyPoid, Long userPoid, Long transactionPoid) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_AR_UPDATE_FDA_AMOUNT")
                .registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT)
                .setParameter("P_GROUP_POID", groupPoid)
                .setParameter("P_COMPANY_POID", companyPoid)
                .setParameter("P_USER_POID", userPoid)
                .setParameter("P_TRANSACTION_POID", transactionPoid);

        query.execute();
        return (String) query.getOutputParameterValue("P_STATUS");
    }

    public String validateDebitNotePartyCompany(Long groupPoid, Long companyPoid, Long userPoid, Long transactionPoid) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GL_DEBIT_NOTE_PTY_CMP_CHK")
                .registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT)
                .setParameter("P_GROUP_POID", groupPoid)
                .setParameter("P_COMPANY_POID", companyPoid)
                .setParameter("P_USER_POID", userPoid)
                .setParameter("P_TRANSACTION_POID", transactionPoid);

        query.execute();
        return (String) query.getOutputParameterValue("P_STATUS");
    }
}