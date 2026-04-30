package com.asg.finance.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;

@Repository
@RequiredArgsConstructor
public class DebitNoteProcedureRepository {

    private final EntityManager entityManager;
    private final DataSource dataSource;

    // Signature: PROC_GL_VOUCHERS_VALIDATIONS(groupPoid, userPoid, companyPoid, docId, refType, refPoid, P_RESULT OUT)
    // Note: userPoid comes before companyPoid; refPoid is VARCHAR2
    public String validateGlVouchers(Long groupPoid, Long companyPoid, Long userPoid,
                                     String docId, String refType, String refPoid) {
        String sql = "BEGIN PROC_GL_VOUCHERS_VALIDATIONS(?,?,?,?,?,?,?); END;";
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setObject(1, groupPoid);
            cs.setObject(2, userPoid);   // user before company — matches DB signature
            cs.setObject(3, companyPoid);
            cs.setObject(4, docId);
            cs.setObject(5, refType);
            cs.setObject(6, refPoid);
            cs.registerOutParameter(7, Types.VARCHAR);
            cs.execute();
            return cs.getString(7);
        } catch (Exception e) {
            throw new RuntimeException("Error calling PROC_GL_VOUCHERS_VALIDATIONS: " + e.getMessage(), e);
        }
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

    /**
     * Signature: PROC_DN_CHECK_GL_L_A(groupPoid, companyPoid, userPoid, glList, P_STATUS OUT, P_CURSOR OUT CURSOR)
     * glList — "0~gl1~gl2..." tilde-delimited GL POIDs from the current GL details.
     * Returns an error message if any invalid ledger is found, otherwise "SUCCESS".
     */
    public String checkGlLedgerAccount(Long groupPoid, Long companyPoid, Long userPoid, String glList) {
        String sql = "BEGIN PROC_DN_CHECK_GL_L_A(?,?,?,?,?,?); END;";
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setObject(1, groupPoid);
            cs.setObject(2, companyPoid);
            cs.setObject(3, userPoid);
            cs.setObject(4, glList);
            cs.registerOutParameter(5, Types.VARCHAR);
            cs.registerOutParameter(6, -10); // OracleTypes.CURSOR

            cs.execute();

            // STATUS ERROR is non-blocking in legacy (only logged); cursor rows are the actual block
            ResultSet rs = (ResultSet) cs.getObject(6);
            if (rs != null) {
                StringBuilder ledgers = new StringBuilder();
                while (rs.next()) {
                    if (ledgers.length() > 0) ledgers.append("; ");
                    ledgers.append(rs.getObject("GL_POID")).append(" - ").append(rs.getObject("GL_DESCRIPTION"));
                }
                if (ledgers.length() > 0) {
                    return "ERROR: Booking Expense GL and Revenue GL in General/Custom is not allowed. Remove: " + ledgers;
                }
            }
            return "SUCCESS";
        } catch (com.asg.common.lib.exception.ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new RuntimeException("Error calling PROC_DN_CHECK_GL_L_A: " + e.getMessage(), e);
        }
    }

    // Signature: PROC_AR_DN_BEFORE_SAVE_VAL(groupPoid, companyPoid, userPoid, docId, transactionPoid,
    //            partyPoid, taxAmount, transactionDate, P_STATUS OUT)
    public String validateBeforeSave(Long groupPoid, Long companyPoid, Long userPoid,
                                     String docId, Long transactionPoid,
                                     Long partyPoid, java.math.BigDecimal taxAmount,
                                     java.time.LocalDate transactionDate) {
        String sql = "BEGIN PROC_AR_DN_BEFORE_SAVE_VAL(?,?,?,?,?,?,?,?,?); END;";
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setObject(1, groupPoid);
            cs.setObject(2, companyPoid);
            cs.setObject(3, userPoid);
            cs.setObject(4, docId);
            cs.setObject(5, transactionPoid);
            cs.setObject(6, partyPoid);
            cs.setObject(7, taxAmount != null ? taxAmount : java.math.BigDecimal.ZERO);
            cs.setObject(8, transactionDate != null ? java.sql.Date.valueOf(transactionDate) : null);
            cs.registerOutParameter(9, Types.VARCHAR);
            cs.execute();
            return cs.getString(9);
        } catch (Exception e) {
            throw new RuntimeException("Error calling PROC_AR_DN_BEFORE_SAVE_VAL: " + e.getMessage(), e);
        }
    }

    public String updateBillReference(Long groupPoid, Long companyPoid, Long userPoid, Long transactionPoid, String docRef, String docId, String refType, String partyType) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_DR_CR_BILL_REF_UPDATE")
                .registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_REF", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_REF_TYPE", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_PARTY_TYPE", String.class, ParameterMode.IN)
                .setParameter("P_LOGIN_GROUP_POID", groupPoid)
                .setParameter("P_LOGIN_USER_POID", userPoid)
                .setParameter("P_LOGIN_COMPANY_POID", companyPoid)
                .setParameter("P_TRANSACTION_POID", transactionPoid)
                .setParameter("P_DOC_REF", docRef)
                .setParameter("P_DOC_ID", docId)
                .setParameter("P_REF_TYPE", refType)
                .setParameter("P_PARTY_TYPE", partyType);

        query.execute();
        return "SUCCESS";
    }

    public String updateFdaAmount(Long groupPoid, Long companyPoid, Long userPoid, String fdaPoid) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_AR_UPDATE_FDA_AMOUNT")
                .registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_FDA_POID", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT)
                .setParameter("P_LOGIN_GROUP_POID", groupPoid)
                .setParameter("P_LOGIN_COMPANY_POID", companyPoid)
                .setParameter("P_LOGIN_USER_POID", userPoid)
                .setParameter("P_FDA_POID", fdaPoid);

        query.execute();
        return (String) query.getOutputParameterValue("P_RESULT");
    }

    public String validateJobBeforeSave(Long groupPoid, Long companyPoid, Long userPoid, Long transactionPoid, String refType, Long refPoid) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GL_JOB_VAL_BEFORE_SAVE")
                .registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_REF_TYPE", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_REF_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT)
                .setParameter("P_GROUP_POID", groupPoid)
                .setParameter("P_COMPANY_POID", companyPoid)
                .setParameter("P_USER_POID", userPoid)
                .setParameter("P_TRANSACTION_POID", transactionPoid)
                .setParameter("P_REF_TYPE", refType)
                .setParameter("P_REF_POID", refPoid);

        query.execute();
        return (String) query.getOutputParameterValue("P_STATUS");
    }

    public String releaseJobOldValues(Long groupPoid, Long companyPoid, Long userPoid, Long transactionPoid, String refType, Long refPoid) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GL_JOB_REL_OLD_VALUES")
                .registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_REF_TYPE", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_REF_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT)
                .setParameter("P_GROUP_POID", groupPoid)
                .setParameter("P_COMPANY_POID", companyPoid)
                .setParameter("P_USER_POID", userPoid)
                .setParameter("P_TRANSACTION_POID", transactionPoid)
                .setParameter("P_REF_TYPE", refType)
                .setParameter("P_REF_POID", refPoid);

        query.execute();
        return (String) query.getOutputParameterValue("P_STATUS");
    }

    // 4th param is PropertyInvoice flag (Y/N), not transactionPoid — matches legacy signature
    public String validateDebitNotePartyCompany(Long groupPoid, Long companyPoid, Long userPoid, String propertyInvoice) {
        String sql = "BEGIN PROC_GL_DEBIT_NOTE_PTY_CMP_CHK(?,?,?,?,?); END;";
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setObject(1, groupPoid);
            cs.setObject(2, companyPoid);
            cs.setObject(3, userPoid);
            cs.setObject(4, propertyInvoice);
            cs.registerOutParameter(5, Types.VARCHAR);
            cs.execute();
            return cs.getString(5);
        } catch (Exception e) {
            throw new RuntimeException("Error calling PROC_GL_DEBIT_NOTE_PTY_CMP_CHK: " + e.getMessage(), e);
        }
    }
}