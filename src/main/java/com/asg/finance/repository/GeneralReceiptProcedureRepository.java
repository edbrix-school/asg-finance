package com.asg.finance.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleTypes;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for General Receipt stored procedure calls
 * Handles all database procedure interactions for General Receipt operations
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class GeneralReceiptProcedureRepository {

    private static final Long DEFAULT_GROUP_POID = 1L;
    private static final String DOC_ID = "300-105";

    private final EntityManager entityManager;

    /**
     * Call PROC_GL_LEDGER_POSTING_MAIN to post receipt to General Ledger
     * 
     * @param companyPoid Company POID
     * @param userPoid User POID
     * @param transactionPoid Transaction POID
     * @param docRefNumber Document reference number (numeric part)
     * @return Status message from procedure
     */
    public String callGLPostingProcedure(Long companyPoid, Long userPoid, Long transactionPoid, Long docRefNumber) {
        log.info("Calling PROC_GL_LEDGER_POSTING_MAIN for transaction: {}", transactionPoid);

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GL_LEDGER_POSTING_MAIN")
                .registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_REF", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT)
                .registerStoredProcedureParameter("P_SEND_ALERT", String.class, ParameterMode.IN)
                .setParameter("P_LOGIN_GROUP_POID", DEFAULT_GROUP_POID)
                .setParameter("P_LOGIN_COMPANY_POID", companyPoid)
                .setParameter("P_LOGIN_USER_POID", userPoid)
                .setParameter("P_DOC_ID", DOC_ID)
                .setParameter("P_TRANSACTION_POID", transactionPoid)
                .setParameter("P_DOC_REF", docRefNumber)
                .setParameter("P_SEND_ALERT", "N");

        query.execute();
        return (String) query.getOutputParameterValue("P_STATUS");
    }

    /**
     * Call PROC_GLOB_APPROVAL_ACTION to submit receipt for approval
     * 
     * @param companyPoid Company POID
     * @param userPoid User POID
     * @param transactionPoid Transaction POID
     * @param docRefNumber Document reference number (numeric part)
     * @param transactionDate Transaction date
     * @return Status message from procedure
     */
    public String callApprovalProcedure(Long companyPoid, Long userPoid, Long transactionPoid, 
                                       Long docRefNumber, java.time.LocalDate transactionDate) {
        log.info("Calling PROC_GLOB_APPROVAL_ACTION for transaction: {}", transactionPoid);

        Date docDate = transactionDate != null 
                ? Date.valueOf(transactionDate) 
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
                .registerStoredProcedureParameter("P_DOC_REF", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_DATE", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_SUBMIT_TO_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_ACTION_RESULT", String.class, ParameterMode.OUT)
                .registerStoredProcedureParameter("P_ACTION_RESULT_USER_POID_LIST", String.class, ParameterMode.OUT)
                .registerStoredProcedureParameter("P_APPROVAL_POID", Long.class, ParameterMode.OUT)
                .setParameter("P_LOGIN_GROUP_POID", DEFAULT_GROUP_POID)
                .setParameter("P_LOGIN_COMPANY_POID", companyPoid)
                .setParameter("P_LOGIN_USER_POID", userPoid)
                .setParameter("P_DOC_ID", DOC_ID)
                .setParameter("P_DOC_KEY_POID", transactionPoid.toString())
                .setParameter("P_ACTION", "SUBMIT")
                .setParameter("P_ACTION_MESSAGE", "General Receipt submitted for approval")
                .setParameter("P_DOC_SUMMARY_INFO", null)
                .setParameter("P_DOC_REF", docRefNumber)
                .setParameter("P_DOC_DATE", docDate.toString())
                .setParameter("P_SUBMIT_TO_USER_POID", null);

        query.execute();
        return (String) query.getOutputParameterValue("P_ACTION_RESULT");
    }

    /**
     * Call PROC_GEN_RECE_NEW_BILLREF_CHK to validate bill references
     * 
     * @param companyPoid Company POID
     * @param userPoid User POID
     * @param glPoid GL POID
     * @param billRefNo Bill reference number
     * @param genPoid General receipt POID (null for new receipts)
     * @param billType Bill type (NEW or AGAINST)
     * @return Result message from procedure
     */
    public String validateBillReference(Long companyPoid, Long userPoid, Long glPoid, 
                                       String billRefNo, Long genPoid, String billType) {
        log.debug("Calling PROC_GEN_RECE_NEW_BILLREF_CHK for bill reference: {}", billRefNo);

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GEN_RECE_NEW_BILLREF_CHK")
                .registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_GL_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_BILL_REF_NO", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_GEN_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_BILL_TYPE", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT)
                .setParameter("P_LOGIN_GROUP_POID", DEFAULT_GROUP_POID)
                .setParameter("P_LOGIN_COMPANY_POID", companyPoid)
                .setParameter("P_LOGIN_USER_POID", userPoid)
                .setParameter("P_GL_POID", glPoid)
                .setParameter("P_BILL_REF_NO", billRefNo)
                .setParameter("P_GEN_POID", genPoid)
                .setParameter("P_BILL_TYPE", billType);

        query.execute();
        return (String) query.getOutputParameterValue("P_RESULT");
    }

    /**
     * Call PROC_GEN_RECEIPT_BILLWISE_CHK to format bill references
     * Trims bill references at pipe delimiter (e.g., "INV-001|Info" -> "INV-001")
     * 
     * @param companyPoid Company POID
     * @param userPoid User POID
     * @param transactionPoid Transaction POID
     */
    public void formatBillReferences(Long companyPoid, Long userPoid, Long transactionPoid) {
        log.debug("Calling PROC_GEN_RECEIPT_BILLWISE_CHK for transaction: {}", transactionPoid);

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GEN_RECEIPT_BILLWISE_CHK")
                .registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN)
                .setParameter("P_GROUP_POID", DEFAULT_GROUP_POID)
                .setParameter("P_LOGIN_USER_POID", userPoid)
                .setParameter("P_COMPANY_POID", companyPoid)
                .setParameter("P_TRANSACTION_POID", transactionPoid);

        query.execute();
        log.debug("PROC_GEN_RECEIPT_BILLWISE_CHK executed successfully");
    }

    /**
     * Call PROC_GLOB_DOC_DELETE to mark receipt as deleted
     * Procedure sets DELETED = 'Y' and also deletes related GL ledger records
     * 
     * @param companyPoid Company POID
     * @param userPoid User POID
     * @param transactionPoid Transaction POID
     * @param transactionDate Transaction date
     * @return Status message from procedure
     */
    public String callDeleteProcedure(Long companyPoid, Long userPoid, Long transactionPoid, 
                                     java.time.LocalDate transactionDate) {
        log.info("Calling PROC_GLOB_DOC_DELETE for transaction: {}", transactionPoid);

        Date docDate = transactionDate != null 
                ? Date.valueOf(transactionDate) 
                : Date.valueOf(java.time.LocalDate.now());

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GLOB_DOC_DELETE")
                .registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_POID_FIELD", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_POID_VALUE", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_TABLE_NAME", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_REQUEST_TYPE", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_DATE", Date.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_DOC_TYPE", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT)
                .setParameter("P_LOGIN_GROUP_POID", DEFAULT_GROUP_POID)
                .setParameter("P_LOGIN_COMPANY_POID", companyPoid)
                .setParameter("P_LOGIN_USER_POID", userPoid)
                .setParameter("P_DOC_ID", DOC_ID)
                .setParameter("P_DOC_POID_FIELD", "TRANSACTION_POID")
                .setParameter("P_DOC_POID_VALUE", transactionPoid)
                .setParameter("P_TABLE_NAME", "AR_GEN_RECEIPT_HDR")
                .setParameter("P_REQUEST_TYPE", "MARK_AS_DELETE")
                .setParameter("P_DOC_DATE", docDate)
                .setParameter("P_DOC_TYPE", "TRANSACTION");

        query.execute();
        return (String) query.getOutputParameterValue("P_STATUS");
    }

    /**
     * Call PROC_GEN_REC_BILLWISE_PENDING to fetch pending bills
     * 
     * @param groupPoid Group POID
     * @param companyPoid Company POID
     * @param glPoid GL POID
     * @param asOnDate As on date for bill calculation
     * @return List of pending bills (returns cursor result set)
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> fetchPendingBills(Long groupPoid, Long companyPoid, Long glPoid, Date asOnDate) {
        log.debug("Calling PROC_GEN_REC_BILLWISE_PENDING for GL: {}", glPoid);

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GEN_REC_BILLWISE_PENDING")
                .registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_GL_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_ASON_DATE", Date.class, ParameterMode.IN)
                .registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR)
                .setParameter("P_GROUP_POID", groupPoid)
                .setParameter("P_COMPANY_POID", companyPoid)
                .setParameter("P_GL_POID", glPoid)
                .setParameter("P_ASON_DATE", asOnDate);

        query.execute();
        return query.getResultList();
    }

    /**
     * Call PROC_AR_GEN_RCP_FETCH_CUST_AC to fetch customer bank details for cheque mode
     * Returns account number, account name, and bank POID from the last cheque payment for the given GL
     * 
     * @param glPoid GL POID (customer GL - RCVD_OTH_POID)
     * @return Customer bank account details (returns cursor result set with columns: ACCOUNT_NO, ACCOUNT_NAME, BANK_POID)
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> fetchCustomerBankDetails(Long glPoid) {
        log.debug("Calling PROC_AR_GEN_RCP_FETCH_CUST_AC for GL: {}", glPoid);

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_AR_GEN_RCP_FETCH_CUST_AC")
                .registerStoredProcedureParameter("P_GLPOID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("ATT_REC", void.class, ParameterMode.REF_CURSOR)
                .setParameter("P_GLPOID", glPoid);

        query.execute();
        return query.getResultList();
    }

    /**
     * Call PROC_AR_GEN_RCPT_FTCH_GLBAL to load billwise balance details for a specific bill reference
     * Returns balance, account type (CREDIT/DEBIT), bill due date, check_all flag, and remarks
     * 
     * @param glPoid GL POID
     * @param billRef Bill reference number
     * @return Billwise balance details (returns cursor result set with columns: BALANCE, ACTYPE, BILL_DUE_DATE, CHECK_ALL, REMARKS)
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> fetchBillwiseDetails(Long glPoid, String billRef) {
        log.debug("Calling PROC_AR_GEN_RCPT_FTCH_GLBAL for GL: {} and bill reference: {}", glPoid, billRef);

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_AR_GEN_RCPT_FTCH_GLBAL")
                .registerStoredProcedureParameter("P_GLPOID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_BL_REF", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("ATT_REC", void.class, ParameterMode.REF_CURSOR)
                .setParameter("P_GLPOID", glPoid)
                .setParameter("P_BL_REF", billRef);

        query.execute();
        return query.getResultList();
    }

    /**
     * Call PROC_GEN_REC_FECH_LEDGER_OF_BC to fetch GL account for charge types
     * Returns GL POID for specific charge types from global parameters
     * 
     * @param groupPoid Group POID
     * @param companyPoid Company POID
     * @param userPoid User POID
     * @param chargeType Charge type (e.g., BANK_CHARGES, ROUND_OFF, EXCHANGE_GAIN_LOSS, COURIER_CHARGES)
     * @return GL POID as string
     */
    public String fetchChargeGLAccount(Long groupPoid, Long companyPoid, Long userPoid, String chargeType) {
        log.debug("Calling PROC_GEN_REC_FECH_LEDGER_OF_BC for charge type: {}", chargeType);

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GEN_REC_FECH_LEDGER_OF_BC")
                .registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_CHARGE_TYPE", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_GL_POID", String.class, ParameterMode.OUT)
                .setParameter("P_LOGIN_GROUP_POID", groupPoid)
                .setParameter("P_LOGIN_COMPANY_POID", companyPoid)
                .setParameter("P_LOGIN_USER_POID", userPoid)
                .setParameter("P_CHARGE_TYPE", chargeType);

        query.execute();
        return (String) query.getOutputParameterValue("P_GL_POID");
    }

    /**
     * Call PROC_DOC_LIST_OF_RECORDS to fetch receipt list with filters
     * Procedure returns two cursors:
     * - OUTDATA_TABLE: Empty record for column structure
     * - OUTDATA_LIST: Actual filtered data
     * 
     * @param groupPoid Group POID
     * @param companyPoid Company POID
     * @param userPoid User POID
     * @param docId Document ID (300-105)
     * @param docType Document type filter (optional)
     * @param whereClause WHERE clause string built from filters
     * @param orderBy ORDER BY clause string
     * @return ProcedureResult containing both cursors and status
     */
    public ProcedureResult fetchReceiptList(Long groupPoid, Long companyPoid, Long userPoid, String docId,
                                            String docType, String whereClause, String orderBy) throws SQLException {
        log.debug("Calling PROC_DOC_LIST_OF_RECORDS for docId: {}, docType: {}, whereClause: {}", 
                docId, docType, whereClause);

        String sql = "{ call PROC_DOC_LIST_OF_RECORDS(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }";

        List<Map<String, Object>> tableStructure = new ArrayList<>();
        List<Map<String, Object>> dataList = new ArrayList<>();
        String status = null;

        // Use EntityManager's connection to stay within transaction context
        Connection conn = null;
        CallableStatement cs = null;
        
        try {
            // Get connection from EntityManager via Hibernate Session to ensure we're in the same transaction
            Session session = entityManager.unwrap(Session.class);
            conn = session.doReturningWork(connection -> connection);
            cs = conn.prepareCall(sql);

            // Set input parameters
            cs.setLong(1, groupPoid);                    // P_LOGIN_GROUP_POID
            cs.setLong(2, companyPoid);                  // P_LOGIN_COMPANY_POID
            cs.setLong(3, userPoid);                     // P_LOGIN_USER_POID
            cs.setString(4, docId);                      // P_DOC_ID
            cs.setString(5, docType != null ? docType : ""); // P_DOC_TYPE
            cs.setString(6, orderBy != null ? orderBy : "");  // P_ORDER_BY
            cs.setString(7, whereClause != null ? whereClause : "1=1"); // P_WHERE_CLAUSE

            // Register output parameters
            cs.registerOutParameter(8, OracleTypes.CURSOR);  // OUTDATA_TABLE
            cs.registerOutParameter(9, OracleTypes.CURSOR);  // OUTDATA_LIST
            cs.registerOutParameter(10, Types.VARCHAR);      // P_STATUS

            log.debug("Executing PROC_DOC_LIST_OF_RECORDS with params: groupPoid={}, companyPoid={}, userPoid={}, docId={}, docType={}, orderBy={}, whereClause={}",
                    groupPoid, companyPoid, userPoid, docId, docType, orderBy, whereClause);

            cs.execute();

            // Get status
            status = cs.getString(10);
            log.debug("Procedure returned status: {}", status);

            // Get first cursor (OUTDATA_TABLE) - table structure
            try (ResultSet rs = (ResultSet) cs.getObject(8)) {
                if (rs != null) {
                    tableStructure = convertResultSetToList(rs);
                }
            }

            // Get second cursor (OUTDATA_LIST) - actual data
            try (ResultSet rs = (ResultSet) cs.getObject(9)) {
                if (rs != null) {
                    dataList = convertResultSetToList(rs);
                }
            }

            log.debug("Procedure returned status: {}, table structure rows: {}, data rows: {}", 
                    status, tableStructure.size(), dataList.size());

        } catch (SQLException e) {
            log.error("Error calling PROC_DOC_LIST_OF_RECORDS: {}", e.getMessage(), e);
            throw e;
        } finally {
            // Close resources manually (don't use try-with-resources as EntityManager manages the connection)
            if (cs != null) {
                try {
                    cs.close();
                } catch (SQLException e) {
                    log.warn("Error closing CallableStatement", e);
                }
            }
            // Don't close connection - EntityManager manages it
        }

        // Return Map structure to preserve column names for mapping
        return new ProcedureResult(tableStructure, dataList, status);
    }

    /**
     * Convert ResultSet to List of Maps
     */
    private List<Map<String, Object>> convertResultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                row.put(columnName, rs.getObject(i));
            }
            result.add(row);
        }
        return result;
    }

    /**
     * Result class for procedure with multiple cursors
     */
    public static class ProcedureResult {
        private final List<Map<String, Object>> tableStructure;
        private final List<Map<String, Object>> dataList;
        private final String status;

        public ProcedureResult(List<Map<String, Object>> tableStructure, 
                              List<Map<String, Object>> dataList, 
                              String status) {
            this.tableStructure = tableStructure;
            this.dataList = dataList;
            this.status = status;
        }

        public List<Map<String, Object>> getTableStructure() {
            return tableStructure;
        }

        public List<Map<String, Object>> getDataList() {
            return dataList;
        }

        public String getStatus() {
            return status;
        }
    }
}

