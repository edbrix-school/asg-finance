package com.asg.finance.repository;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GlBankEntity;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BankReconciliationRepositoryImpl implements BankReconciliationRepository {

    @PersistenceContext
    private EntityManager em;

    private final GlBankRepository bankRepository;

    private static final String P_GROUP_POID = "P_GROUP_POID";
    private static final String P_DATE_TILL = "P_DATE_TILL";
    private static final String P_DATE_FROM = "P_DATE_FROM";
    private static final String P_TRANSACTION_POID = "P_TRANSACTION_POID";
    private static final String P_RESULT = "P_RESULT";
    private static final String P_TRANSACTION_GROUP_POID = "P_TRANSACTION_GROUP_POID";
    private static final String P_TRANSACTION_COMPANY_POID = "P_TRANSACTION_COMPANY_POID";
    private static final String P_LOGIN_USER_POID = "P_LOGIN_USER_POID";
    private static final String P_LOGIN_COMPANY_POID = "P_LOGIN_COMPANY_POID";
    private static final String P_LOGIN_GROUP_POID = "P_LOGIN_GROUP_POID";
    private static final String P_LOGIN_URL = "P_LOGIN_URL";
    private static final String P_CHEQUE_TYPE = "P_CHEQUE_TYPE";
    private static final String P_CHEQUE_FILTER = "P_CHEQUE_FILTER";
    private static final String P_DOC_ID = "P_DOC_ID";
    private static final String P_DOC_REF = "P_DOC_REF";
    private static final String P_BANK_POID = "P_BANK_POID";
    private static final String P_COMPANY_POID = "P_COMPANY_POID";
    private static final String P_POSTED_BY = "P_POSTED_BY";
    private static final String P_CHEQUE_NO = "P_CHEQUE_NO";
    private static final String P_RECONCILE_CHEQUE = "P_RECONCILE_CHEQUE";
    private static final String P_BR_TYPE = "P_BR_TYPE";
    private static final String P_GL_POID = "P_GL_POID";

    @Override
    public List<BankReconciliationResponse> callReconcileView(Long groupPoid, Long companyPoid, Long bankPoid,
                                                              LocalDate dateFrom, LocalDate dateTill, String chequeNo, String reconcileCheque, String brType) {

        StoredProcedureQuery sp = createSP("PROC_GL_BANK_RECONCILE_VIEW");

        regIn(sp, P_GROUP_POID, Long.class);
        regIn(sp, P_COMPANY_POID, Long.class);
        regIn(sp, P_BANK_POID, Long.class);
        regIn(sp, P_DATE_FROM, java.sql.Date.class);
        regIn(sp, P_DATE_TILL, java.sql.Date.class);
        regIn(sp, P_CHEQUE_NO, String.class);
        regIn(sp, P_RECONCILE_CHEQUE, String.class);
        regIn(sp, P_BR_TYPE, String.class);
        regRefCursor(sp, "OUTDATA", void.class);
        regOut(sp, "P_OPEN_BALANCE", String.class);
        regOut(sp, "P_CLOSING_BALANCE", String.class);

        set(sp, P_GROUP_POID, groupPoid);
        set(sp, P_COMPANY_POID, companyPoid);
        set(sp, P_BANK_POID, bankPoid);
        set(sp, P_DATE_FROM, dateFrom != null ? java.sql.Date.valueOf(dateFrom) : null);
        set(sp, P_DATE_TILL, dateTill != null ? java.sql.Date.valueOf(dateTill) : null);
        set(sp, P_CHEQUE_NO, chequeNo);
        set(sp, P_RECONCILE_CHEQUE, reconcileCheque);
        set(sp, P_BR_TYPE, brType);

        sp.execute();

        @SuppressWarnings("unchecked")
        List<Object[]> rows = sp.getResultList();

        return rows.stream().map(this::mapRowToDtoView).toList();
    }

    @Override
    public BankRenconciliationBankInfoDTO getBankPoid(Long glPoid) {
        StoredProcedureQuery sp = createSP("PROC_GL_BANK_REC_GET_BANK_POID");
        regIn(sp, P_GL_POID, Long.class);
        regOut(sp, "P_BANK", String.class);
        regOut(sp, "P_COMPANY", String.class);
        set(sp, P_GL_POID, glPoid);
        sp.execute();
        BankRenconciliationBankInfoDTO dto = new BankRenconciliationBankInfoDTO();
        dto.setBank((String) sp.getOutputParameterValue("P_BANK"));
        dto.setCompany((String) sp.getOutputParameterValue("P_COMPANY"));
        return dto;
    }

    @Override
    public String saveReconciliation(List<BankReconciliationRequest> req) {
        if (req == null || req.isEmpty()) {
            throw new ValidationException("Insufficient Data");
        }

        StoredProcedureQuery sp = createSP("PROC_GL_BANK_RECONCILE_SAVE");
        regIn(sp, P_TRANSACTION_GROUP_POID, Long.class);
        regIn(sp, P_TRANSACTION_COMPANY_POID, Long.class);
        regIn(sp, P_DOC_ID, String.class);
        regIn(sp, P_TRANSACTION_POID, Long.class);
        regIn(sp, "P_TRANSACTION_DATE", java.sql.Date.class);
        regIn(sp, P_DOC_REF, String.class);
        regIn(sp, "P_CHEQUE_REF", String.class);
        regIn(sp, "P_DET_ROW_ID", Long.class);
        regIn(sp, "P_NARRATION", String.class);
        regIn(sp, "P_GL_COMPANY_POID", Long.class);
        regIn(sp, P_GL_POID, Long.class);
        regIn(sp, "P_DR_AMT", Double.class);
        regIn(sp, "P_CR_AMT", Double.class);
        regIn(sp, P_POSTED_BY, Long.class);
        regIn(sp, "P_CLEARANCE_DATE", java.sql.Date.class);
        regOut(sp, P_RESULT, String.class);
        regIn(sp, "p_user_auto", String.class);

        String response = "Successfully Updated.";

        for (BankReconciliationRequest dto : req) {
            set(sp, P_TRANSACTION_GROUP_POID, dto.getTransactionGroupPoid());
            set(sp, P_TRANSACTION_COMPANY_POID, dto.getTransactionCompanyPoid());
            set(sp, P_DOC_ID, dto.getDocId());
            set(sp, P_TRANSACTION_POID, dto.getTransactionPoid());
            set(sp, "P_TRANSACTION_DATE", dto.getTransactionDate() != null ? java.sql.Date.valueOf(dto.getTransactionDate()) : null);
            set(sp, P_DOC_REF, dto.getDocRef());
            set(sp, "P_CHEQUE_REF", dto.getChequeRef());
            set(sp, "P_DET_ROW_ID", dto.getDetRowId());
            set(sp, "P_NARRATION", dto.getNarration());
            set(sp, "P_GL_COMPANY_POID", dto.getGlCompanyPoid());
            set(sp, P_GL_POID, dto.getGlPoid());
            set(sp, "P_DR_AMT", dto.getDrAmt());
            set(sp, "P_CR_AMT", dto.getCrAmt());
            set(sp, P_POSTED_BY, dto.getPostedBy());
            set(sp, "P_CLEARANCE_DATE", dto.getClearanceDate() != null ? java.sql.Date.valueOf(dto.getClearanceDate()) : null);
            set(sp, "p_user_auto", dto.getUserAuto());
            sp.execute();
            String output = outStr(sp, P_RESULT);

            if (output == null || !output.toLowerCase().startsWith("success")) {
                return output;
            }
        }
        return response;
    }

    @Override
    public String holdCheque(List<BankReconcHoldAndUholdRequest> reqList) {
        for (BankReconcHoldAndUholdRequest req : reqList) {

            if(req.getDocId()==null || !req.getDocId().equalsIgnoreCase("400-107")){
                String errorMessage=req.getDocId()==null?"The selected item cannot be hold":String.format("DocRef: %s cannot be hold",req.getDocRef());
                throw new IllegalStateException(errorMessage);
            }

            StoredProcedureQuery sp = createSP("PROC_GL_BANK_RECONCILE_HOLD");

            regIn(sp, P_TRANSACTION_GROUP_POID, Long.class);
            regIn(sp, P_TRANSACTION_COMPANY_POID, Long.class);
            regIn(sp, P_LOGIN_USER_POID, Long.class);
            regIn(sp, P_DOC_ID, String.class);
            regIn(sp, P_TRANSACTION_POID, Long.class);
            regIn(sp, P_DOC_REF, String.class);
            regOut(sp, P_RESULT, String.class);

            set(sp, P_TRANSACTION_GROUP_POID, req.getTransactionGroupPoid());
            set(sp, P_TRANSACTION_COMPANY_POID, req.getTransactionCompanyPoid());
            set(sp, P_LOGIN_USER_POID, req.getUserPoid());
            set(sp, P_DOC_ID, req.getDocId());
            set(sp, P_TRANSACTION_POID, req.getTransactionPoid());
            set(sp, P_DOC_REF, req.getDocRef());

            sp.execute();

            String result = outStr(sp, P_RESULT);

            if (isError(result)) {
                throw new IllegalStateException(result);
            }
        }

        return reqList.size() + " cheques hold successfully";
    }

    @Override
    public String unholdCheque(List<BankReconcHoldAndUholdRequest> reqList) {

        if (reqList == null || reqList.isEmpty()) {
            return "No cheques to unhold";
        }

        return em.unwrap(Session.class).doReturningWork(connection -> {
            String sql = "{ call PROC_GL_BANK_RECONCILE_UNHOLD(?, ?, ?, ?, ?, ?, ?) }";

            try (CallableStatement cs = connection.prepareCall(sql)) {

                cs.registerOutParameter(7, Types.VARCHAR);

                for (BankReconcHoldAndUholdRequest req : reqList) {
                    if(req.getDocId()==null || !req.getDocId().equalsIgnoreCase("400-107")){
                        String errorMessage=req.getDocId()==null?"The selected item cannot be unhold":String.format("DocRef: %s cannot be unhold",req.getDocRef());
                        throw new IllegalStateException(errorMessage);
                    }
                    String result = executeUnhold(cs, req);

                    if (isError(result)) {
                        throw new IllegalStateException(result);
                    }
                }

                return reqList.size() + " cheques unheld successfully";

            }
            catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Error calling PROC_GL_BANK_RECONCILE_UNHOLD", e);
            }
        });
    }

    private String executeUnhold(CallableStatement cs, BankReconcHoldAndUholdRequest req)
            throws SQLException {

        cs.setLong(1, req.getTransactionGroupPoid());
        cs.setLong(2, req.getTransactionCompanyPoid());
        cs.setLong(3, req.getUserPoid());
        cs.setString(4, req.getDocId());
        cs.setLong(5, req.getTransactionPoid());
        cs.setString(6, req.getDocRef());

        cs.execute();

        String result = cs.getString(7);
        cs.clearParameters();

        return result;
    }

    private boolean isError(String result) {
        return result != null && !result.toLowerCase().contains("success");
    }

    @Override
    public String updateStatementDate(Long companyPoid, Long postedBy, Long bankPoid, LocalDate statementDate) {

        if (!validateBank(bankPoid)) {
            throw new ResourceNotFoundException("Bank", "bank poid", bankPoid);
        }

        StoredProcedureQuery sp = createSP("PROC_GL_BANK_RECON_STMT_DATE");

        regIn(sp, 1, Long.class);
        regIn(sp, 2, Long.class);
        regIn(sp, 3, Long.class);
        regIn(sp, 4, java.sql.Date.class);
        regOut(sp, 5, String.class);

        set(sp, 1, companyPoid);
        set(sp, 2, postedBy);
        set(sp, 3, bankPoid);
        set(sp, 4, statementDate != null ? java.sql.Date.valueOf(statementDate) : null);

        sp.execute();

        return (String) sp.getOutputParameterValue(5);
    }

    @Override
    public String pollAutoRefresh(String userId, Long companyPoid, String loginUrl) {

        StoredProcedureQuery sp = createSP("PROC_BANK_REC_POLL_AUTOREFRESH");

        regIn(sp, "P_USER_ID", String.class);
        regIn(sp, P_LOGIN_COMPANY_POID, Long.class);
        regIn(sp, P_LOGIN_URL, String.class);

        set(sp, "P_USER_ID", userId);
        set(sp, P_LOGIN_COMPANY_POID, companyPoid);
        set(sp, P_LOGIN_URL, loginUrl);

        sp.execute();

        return "SUCCESS";
    }

    @Override
    public String revertReconciliation(String docId, String transactionPoid, Long loginUserPoid, Long loginGroupPoid,
                                       Long loginCompanyPoid, String mailAlert) {

        StoredProcedureQuery sp = createSP("PROC_GL_BANK_RECONCILE_REVERT");

        regIn(sp, P_LOGIN_GROUP_POID, Long.class);
        regIn(sp, P_LOGIN_COMPANY_POID, Long.class);
        regIn(sp, P_LOGIN_USER_POID, Long.class);
        regIn(sp, P_DOC_ID, String.class);
        regIn(sp, P_TRANSACTION_POID, String.class);
        regOut(sp, "P_STATUS", String.class);
        regIn(sp, "P_MAIL_ALERT", String.class);

        set(sp, P_LOGIN_GROUP_POID, loginGroupPoid);
        set(sp, P_LOGIN_COMPANY_POID, loginCompanyPoid);
        set(sp, P_LOGIN_USER_POID, loginUserPoid);
        set(sp, P_DOC_ID, docId);
        set(sp, P_TRANSACTION_POID, transactionPoid);
        set(sp, "P_MAIL_ALERT", mailAlert == null ? "Y" : mailAlert);

        sp.execute();

        return outStr(sp, "P_STATUS");
    }

    @Override
    public BankReconcileReportResponse getBankReconcileReport(BankReconcileReportRequest req) {

        StoredProcedureQuery sp = createSP("PROC_GL_BANK_RECONCILE_REPORT");

        regIn(sp, P_GROUP_POID, Long.class);
        regIn(sp, P_COMPANY_POID, Long.class);
        regIn(sp, P_BANK_POID, Long.class);
        regIn(sp, P_DATE_FROM, java.sql.Date.class);
        regIn(sp, P_DATE_TILL, java.sql.Date.class);
        regIn(sp, P_CHEQUE_NO, String.class);
        regIn(sp, P_RECONCILE_CHEQUE, String.class);
        regIn(sp, P_BR_TYPE, String.class);
        regIn(sp, P_CHEQUE_TYPE, String.class);
        regIn(sp, P_CHEQUE_FILTER, String.class);
        regRefCursor(sp, "OUTDATA", void.class);

        regOut(sp, "OUTDATA1", String.class);
        regOut(sp, "OUTDATA2", String.class);
        regOut(sp, "OUTDATA3", String.class);
        regOut(sp, "OUTDATA4", String.class);
        regOut(sp, "OUTDATA5", String.class);
        regOut(sp, "OUTDATA6", String.class);
        regOut(sp, "OUTDATA7", String.class);

        set(sp, P_GROUP_POID, req.getGroupPoid());
        set(sp, P_COMPANY_POID, req.getCompanyPoid());
        set(sp, P_BANK_POID, req.getBankPoid());
        set(sp, P_DATE_FROM, Optional.ofNullable(req.getDateFrom()).map(java.sql.Date::valueOf).orElse(null));
        set(sp, P_DATE_TILL, Optional.ofNullable(req.getDateTill()).map(java.sql.Date::valueOf).orElse(null));
        set(sp, P_CHEQUE_NO, req.getChequeNo());
        set(sp, P_RECONCILE_CHEQUE, req.getReconcileCheque());
        set(sp, P_BR_TYPE, req.getBrType());
        set(sp, P_CHEQUE_TYPE, req.getChequeType());
        set(sp, P_CHEQUE_FILTER, StringUtils.defaultIfBlank(req.getChequeFilter(), "ALL"));

        sp.execute();

        BankReconcileReportResponse resp = new BankReconcileReportResponse();

        List<Object[]> cursorList = sp.getResultList();
        List<BankReconcileReportRow> reportRows = cursorList.stream()
                .map(cursorData -> mapReportRow(
                        cursorData,
                        req.getChequeType(),
                        StringUtils.defaultIfBlank(req.getChequeFilter(), "ALL")
                ))
                .toList();

        resp.setOpeningBalance((String) sp.getOutputParameterValue("OUTDATA1"));
        resp.setClosingBalance((String) sp.getOutputParameterValue("OUTDATA2"));
        resp.setCreditTotal((String) sp.getOutputParameterValue("OUTDATA3"));
        resp.setDebitTotal((String) sp.getOutputParameterValue("OUTDATA4"));
        resp.setUnclearBalance((String) sp.getOutputParameterValue("OUTDATA5"));
        resp.setDebitTotal2((String) sp.getOutputParameterValue("OUTDATA6"));
        resp.setExtraValue((String) sp.getOutputParameterValue("OUTDATA7"));

        reportRows.forEach(row -> {
            if (row.getDocId1() != null) {
                row.setDocTitle(getDocumentTitle(row.getDocId1()));
            }
        });
        resp.setReportData(reportRows);

        return resp;
    }

    private String getDocumentTitle(String docId) {
        try {
            return (String) em
                    .createNativeQuery(
                            "SELECT DOC_NAME FROM GLOBAL_DOC_MASTER " +
                                    "WHERE DOC_ID = :docId " +
                                    "AND ACTIVE = 'Y' " +
                                    "AND (DELETED = 'N' OR DELETED IS NULL)")
                    .setParameter("docId", docId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return "";
        }
    }

    private boolean existsByTransactionPoid(Long transactionPoid) {
        String sql = "SELECT 1 FROM GL_BANK_RECONCILIATION_TABLE WHERE TRANSACTION_POID = ? FETCH FIRST 1 ROWS ONLY";

        return em.unwrap(Session.class).doReturningWork(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, transactionPoid);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Error checking transaction poid existence", e);
            }
        });
    }

    private boolean existsByDocref(Long transactionPoid, String docRef) {
        String sql = "SELECT 1 FROM GL_BANK_RECONCILIATION_TABLE WHERE TRANSACTION_POID = ? AND DOC_REF = ? FETCH FIRST 1 ROWS ONLY";

        return em.unwrap(Session.class).doReturningWork(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, transactionPoid);
                ps.setString(2, docRef);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Error checking transaction poid existence", e);
            }
        });
    }

    private BankReconciliationResponse mapRowToDtoView(Object[] row) {
        BankReconciliationResponse dto = new BankReconciliationResponse();

        dto.setTransactionGroupPoid(getLong(row, 0));
        dto.setTransactionCompanyPoid(getLong(row, 1));
        dto.setDocId(getString(row, 2));
        dto.setDocId1(getString(row, 3));
        dto.setTransactionPoid(getLong(row, 4));
        dto.setTransactionDate(getDate(row, 5));
        dto.setDocRef(getString(row, 6));
        dto.setChequeRef(getString(row, 7));
        dto.setDetRowId(getLong(row, 8));
        dto.setNarration(getString(row, 9));
        dto.setGlCompanyPoid(getLong(row, 10));
        dto.setGlPoid(getLong(row, 11));
        dto.setCrAmt(getBigDecimal(row, 12));
        dto.setDrAmt(getBigDecimal(row, 13));

        if (dto.getDocId() != null) {
            dto.setDocTitle(getDocumentTitle(dto.getDocId()));
        }

        return dto;
    }

    private Long toLong(Object val) {
        if (val == null)
            return null;
        if (val instanceof BigDecimal bd)
            return bd.longValue();
        if (val instanceof Number num)
            return num.longValue();
        if (val instanceof String s)
            return s.isBlank() ? null : Long.valueOf(s.trim());
        throw new IllegalArgumentException("Unsupported type for Long conversion: " + val.getClass());
    }

    private String getString(Object[] row, int index) {
        return row.length > index ? (String) row[index] : null;
    }

    private Long getLong(Object[] row, int index) {
        return row.length > index ? toLong(row[index]) : null;
    }

    private BigDecimal getBigDecimal(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return null;
        }

        Object val = row[index];

        if (val instanceof BigDecimal bd) {
            return bd;
        }
        if (val instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        if (val instanceof String s && !s.isBlank()) {
            return new BigDecimal(s);
        }

        return null;
    }

    private LocalDate getDate(Object[] row, int index) {
        if (row.length <= index || row[index] == null) {
            return null;
        }

        Object val = row[index];

        if (val instanceof java.sql.Date) {
            return ((java.sql.Date) val).toLocalDate();
        }

        if (val instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) val).toLocalDateTime().toLocalDate();
        }

        if (val instanceof String s) {
            try {
                return LocalDate.parse(s);
            } catch (Exception e) {
                return null;
            }
        }

        throw new IllegalArgumentException("Unsupported date type at index " + index + ": " + val.getClass());
    }

    private StoredProcedureQuery createSP(String name) {
        return em.createStoredProcedureQuery(name);
    }

    private void regIn(StoredProcedureQuery sp, String name, Class<?> type) {
        sp.registerStoredProcedureParameter(name, type, ParameterMode.IN);
    }

    private void regIn(StoredProcedureQuery sp, int position, Class<?> type) {
        sp.registerStoredProcedureParameter(position, type, ParameterMode.IN);
    }

    private void regOut(StoredProcedureQuery sp, String name, Class<?> type) {
        sp.registerStoredProcedureParameter(name, type, ParameterMode.OUT);
    }

    private void regOut(StoredProcedureQuery sp, int position, Class<?> type) {
        sp.registerStoredProcedureParameter(position, type, ParameterMode.OUT);
    }

    private void regRefCursor(StoredProcedureQuery sp, String name, Class<?> type) {
        sp.registerStoredProcedureParameter(name, type, ParameterMode.REF_CURSOR);
    }

    private void set(StoredProcedureQuery sp, String name, Object value) {
        sp.setParameter(name, value);
    }

    private void set(StoredProcedureQuery sp, int position, Object value) {
        sp.setParameter(position, value);
    }

    private String outStr(StoredProcedureQuery sp, String name) {
        Object o = sp.getOutputParameterValue(name);
        return (o != null) ? o.toString() : null;
    }

    private BankReconcileReportRow mapReportRow(Object[] row, String chequeType, String chequeFilter) {

        boolean isFiltered = chequeType != null && !chequeType.isEmpty() && chequeFilter != null
                && !chequeFilter.isEmpty();

        int offset = isFiltered ? 1 : 0;

        return new BankReconcileReportRow(getLong(row, 0), getLong(row, 1), getString(row, 2), getLong(row, 3),
                getDate(row, 4), getString(row, 5), getString(row, 6), getLong(row, 7), getString(row, 8),
                getLong(row, 9), getLong(row, 10), getBigDecimal(row, 11), isFiltered ? null : getBigDecimal(row, 12),
                getDate(row, 13 - offset), getString(row, 14 - offset), getString(row, 15 - offset), null);
    }

    private boolean validateBank(Long bankPoid) {
        GlBankEntity bankEntity = bankRepository.findByBankPoid(bankPoid);
        return bankEntity != null;
    }

}
