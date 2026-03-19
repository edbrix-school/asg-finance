package com.asg.finance.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.Date;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BankDebitVoucherProcedureRepository {

    private static final Long DEFAULT_GROUP_POID = 1L;

    private final EntityManager entityManager;

    public String callApprovalAction(Long companyPoid, Long userPoid, Long transactionPoid,
                                     String docId, String docRef, java.time.LocalDateTime transactionDate) {
        log.info("Calling PROC_GLOB_APPROVAL_ACTION for transaction: {}", transactionPoid);

        Date docDate = transactionDate != null
                ? Date.valueOf(transactionDate.toLocalDate())
                : Date.valueOf(java.time.LocalDate.now());

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GLOB_APPROVAL_ACTION")
                .registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_KEY_POID", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_ACTION", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_ACTION_MESSAGE", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_SUMMARY_INFO", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_REF", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_DATE", Date.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_SUBMIT_TO_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_ACTION_RESULT", String.class, ParameterMode.OUT)
                .registerStoredProcedureParameter("P_ACTION_RESULT_USER_POID_LIST", String.class, ParameterMode.OUT)
                .registerStoredProcedureParameter("P_APPROVAL_POID", Long.class, ParameterMode.OUT)
                .setParameter("P_LOGIN_GROUP_POID", DEFAULT_GROUP_POID)
                .setParameter("P_LOGIN_COMPANY_POID", companyPoid)
                .setParameter("P_LOGIN_USER_POID", userPoid)
                .setParameter("P_DOC_ID", docId)
                .setParameter("P_DOC_KEY_POID", transactionPoid.toString())
                .setParameter("P_ACTION", "SUBMIT")
                .setParameter("P_ACTION_MESSAGE", "Bank Debit Voucher submitted for approval")
                .setParameter("P_DOC_SUMMARY_INFO", null)
                .setParameter("P_DOC_REF", docRef)
                .setParameter("P_DOC_DATE", docDate)
                .setParameter("P_SUBMIT_TO_USER_POID", null);

        query.execute();
        return (String) query.getOutputParameterValue("P_ACTION_RESULT");
    }
}
