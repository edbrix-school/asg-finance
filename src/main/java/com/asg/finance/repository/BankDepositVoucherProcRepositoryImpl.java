package com.asg.finance.repository;


import com.asg.common.lib.exception.AsgException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.BankDepositVoucherDtlDto;
import com.asg.finance.dto.DrilldownLinkInfoDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BankDepositVoucherProcRepositoryImpl implements BankDepositVoucherProcRepository {

    private static final String DRILLDOWN_LINK_INFO = "DRILLDOWN_LINK_INFO";
    private static final String P_LOGIN_GROUP_POID = "P_LOGIN_GROUP_POID";
    private static final String P_LOGIN_USER_POID = "P_LOGIN_USER_POID";
    private static final String P_LOGIN_COMPANY_POID = "P_LOGIN_COMPANY_POID";
    private static final String P_RESULT = "P_RESULT";
    private static final String P_DOC_ID = "P_DOC_ID";
    private static final String P_DOC_KEY_POID = "P_DOC_KEY_POID";

    private static final String PROC_GL_BANK_DEP_BEFORE_SAVE = "PROC_GL_BANK_DEP_BEFORE_SAVE";
    private static final String PROC_GET_CHEQUE_CURRENT_STATUS = "PROC_GET_CHEQUE_CURRENT_STATUS";
    private static final String PROC_GL_BANK_DEPOSIT_LOAD_PYMT = "PROC_GL_BANK_DEPOSIT_LOAD_PYMT";
    private static final String PROC_GL_BANK_DEPOSIT_UPDT_PYMT = "PROC_GL_BANK_DEPOSIT_UPDT_PYMT";
    private static final String P_PAYMENT_TYPE = "P_PAYMENT_TYPE";
    private static final String P_BANK_FILTER = "P_BANK_FILTER";
    private static final String P_BANK_POID = "P_BANK_POID";
    private static final String P_DOC_DATE = "P_DOC_DATE";
    private static final String P_BDV_POID = "P_BDV_POID";
    private static final String P_LINE_TYPE = "P_LINE_TYPE";
    private static final String P_RESULT2 = "P_RESULT2";
    private static final String OUTDATA = "OUTDATA";
    private static final String CUSTOMER_BANK_MASTER = "CUSTOMER_BANK_MASTER";
    private static final String P_BANK_GL_POID = "P_BANK_GL_POID";
    private static final String ERROR = "ERROR";
    private static final String WARNING = "WARNING";

    @PersistenceContext
    private EntityManager entityManager;
    private final LovDataService lovService;
    private final GeneralReceiptProcedureRepository generalReceiptProcedureRepository;




    @Override
    public void callBeforeSaveValidation(Long companyPoid, Long bankPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery(PROC_GL_BANK_DEP_BEFORE_SAVE);
        query.registerStoredProcedureParameter(P_LOGIN_GROUP_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_USER_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_COMPANY_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_BANK_GL_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_RESULT, String.class, ParameterMode.OUT);

        query.setParameter(P_LOGIN_GROUP_POID, UserContext.getGroupPoid());
        query.setParameter(P_LOGIN_USER_POID, UserContext.getUserPoid());
        query.setParameter(P_LOGIN_COMPANY_POID, companyPoid != null ? companyPoid : UserContext.getCompanyPoid());
        query.setParameter(P_BANK_GL_POID, bankPoid);
        query.execute();

        String result = (String) query.getOutputParameterValue(P_RESULT);
        if (result != null && (result.contains(ERROR) || result.contains(WARNING))) {
            throw new ValidationException(result);
        }
    }

    @Override
    public void callChequeStatusValidation(String refDocRef, Long refDocPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery(PROC_GET_CHEQUE_CURRENT_STATUS);
        query.registerStoredProcedureParameter(P_DOC_ID, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_DOC_KEY_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_RESULT, String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter(P_RESULT2, String.class, ParameterMode.OUT);

        query.setParameter(P_DOC_ID, refDocRef);
        query.setParameter(P_DOC_KEY_POID, refDocPoid);
        query.execute();

        String status = (String) query.getOutputParameterValue(P_RESULT);
        String message = (String) query.getOutputParameterValue(P_RESULT2);

        if (status != null && (status.contains("RECONCILE") || status.contains("NOT_ALLOWED"))) {
            throw new ValidationException(message != null ? message : "Cheque already reconciled or action not allowed for ref " + refDocRef);
        }
    }

    @Override
    public List<BankDepositVoucherDtlDto> loadPendingPayments(Long bankPoid, String type, String bankFilter) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery(PROC_GL_BANK_DEPOSIT_LOAD_PYMT);

        registerParameters(query);
        setQueryParameters(query, bankPoid, type, bankFilter);
        query.execute();

        handleProcedureResult((String) query.getOutputParameterValue(P_RESULT), bankPoid);

        ResultSet rs = (ResultSet) query.getOutputParameterValue(OUTDATA);

        if (rs == null) {
            throw new ResourceNotFoundException("Pending payments", "bankPoid", bankPoid);
        }

        try {
            List<BankDepositVoucherDtlDto> results = processResultSet(rs);
            if (results.isEmpty()) {
                throw new ValidationException("No Pending Payments");}
            return results;
        }
        catch (ValidationException e){
            throw e;
        }
        catch (Exception e) {
            log.error("Error reading result set: {}", e.getMessage(), e);
            throw new ValidationException("Error loading pending payments: " + e.getMessage());
        }
    }
    private void setQueryParameters(StoredProcedureQuery query, Long bankPoid, String type, String bankFilter) {
        query.setParameter(P_LOGIN_GROUP_POID, UserContext.getGroupPoid());
        query.setParameter(P_LOGIN_USER_POID, UserContext.getUserPoid());
        query.setParameter(P_LOGIN_COMPANY_POID, UserContext.getCompanyPoid());
        query.setParameter(P_PAYMENT_TYPE, type);
        query.setParameter(P_BANK_FILTER, bankFilter);
        query.setParameter(P_BANK_POID, bankPoid);
        query.setParameter(P_DOC_DATE, new java.sql.Date(System.currentTimeMillis()));
        query.setParameter(P_LINE_TYPE, "OTHERS");
    }
    private List<BankDepositVoucherDtlDto> processResultSet(ResultSet rs) throws SQLException {
        List<BankDepositVoucherDtlDto> resultList = new ArrayList<>();
        long detRowIdCounter = 1L;

        while (rs.next()) {
            BankDepositVoucherDtlDto dto = mapRowToDto(rs, detRowIdCounter++);
            setBankDetailsForDto(dto);
            resultList.add(dto);
        }
        return resultList;
    }

    private BankDepositVoucherDtlDto mapRowToDto(ResultSet rs, long detRowId) throws SQLException {
        return BankDepositVoucherDtlDto.builder()
                .paymentMainPoid(rs.getLong("PAYMENT_MAIN_POID"))
                .refDocPoid(rs.getLong("REF_DOC_POID"))
                .refDocRef(rs.getString("REF_DOC_REF"))
                .refDocId(rs.getString("REF_DOC_ID"))
                .drilldownLinkInfo(
                        parseDrilldownLinkInfo(
                                rs.getString("DRILLDOWN_LINK_INFO")))
                .rcpDate(rs.getDate("RCP_DATE") != null
                        ? rs.getDate("RCP_DATE").toLocalDate()
                        : null)
                .rcpDate(rs.getDate("RCP_DATE") != null ? rs.getDate("RCP_DATE").toLocalDate() : null)
                .bankPoid(rs.getLong("BANK_POID"))
                .chqAcName(rs.getString("CHQ_AC_NAME"))
                .chqAcNo(rs.getString("CHQ_AC_NO"))
                .chqCardNo(rs.getString("CHQ_CARDNO"))
                .chqDate(rs.getDate("CHQ_DATE") != null ? rs.getDate("CHQ_DATE").toLocalDate() : null)
                .amount(rs.getBigDecimal("AMOUNT"))
                .pymtType(rs.getString("PYMT_TYPE"))
                .detRowId(detRowId)
                .build();
    }

    private void handleProcedureResult(String result, Long bankPoid) {
        if (result == null) return;

        if (result.contains("Bank Filter")) {
            log.error("Bank Filter is a required field");
            throw new ValidationException("Bank Filter is a required field.");
        }
        if (result.contains("Selected bank and login company")) {
            throw new ValidationException("Selected bank and login company are not matching.");
        }
        if (result.contains("Currently no pending")) {
            log.info(result);
            throw new ResourceNotFoundException("Pending payments", "bankPoid", bankPoid);
        }
        if (result.contains(ERROR) || result.contains(WARNING)) {
            throw new ValidationException(result);
        }
    }


    private void registerParameters(StoredProcedureQuery query) {
        query.registerStoredProcedureParameter(P_LOGIN_GROUP_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_USER_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_COMPANY_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_PAYMENT_TYPE, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_BANK_FILTER, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_BANK_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_DOC_DATE, java.sql.Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LINE_TYPE, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_RESULT, String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter(OUTDATA, void.class, ParameterMode.REF_CURSOR);
    }
    private void setBankDetailsForDto(BankDepositVoucherDtlDto dto) {
        if (dto.getBankPoid() != null) {
            try {
                dto.setBankDet(lovService.getDetailsByPoidAndLovName(dto.getBankPoid(), CUSTOMER_BANK_MASTER));
            } catch (Exception e) {
                log.warn("Failed to fetch bank details for bankPoid: {}", dto.getBankPoid(), e);
            }
        }
    }

    @Override
    public void markPaymentsCompleted(Long transactionPoid, String paymentType) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery(PROC_GL_BANK_DEPOSIT_UPDT_PYMT);
        query.registerStoredProcedureParameter(P_LOGIN_GROUP_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_USER_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_COMPANY_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_BDV_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_PAYMENT_TYPE, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_RESULT, String.class, ParameterMode.OUT);

        query.setParameter(P_LOGIN_GROUP_POID, UserContext.getGroupPoid());
        query.setParameter(P_LOGIN_USER_POID, UserContext.getUserPoid());
        query.setParameter(P_LOGIN_COMPANY_POID, UserContext.getCompanyPoid());
        query.setParameter(P_BDV_POID, transactionPoid);
        query.setParameter(P_PAYMENT_TYPE, paymentType);
        query.execute();

        String result = (String) query.getOutputParameterValue(P_RESULT);
        if (result != null && result.contains(ERROR)) {

            log.warn("PROC_GL_BANK_DEPOSIT_UPDT_PYMT returned error: {}", result);
        }
    }

    @Override
    public void callApprovalProcedure(Long companyPoid, Long userPoid, Long transactionPoid, String docRef, java.time.LocalDate transactionDate) {
        log.info("Calling PROC_GLOB_APPROVAL_ACTION for BDV transaction: {}", transactionPoid);
        generalReceiptProcedureRepository.callApprovalProcedure(companyPoid,userPoid,transactionPoid,Long.parseLong(docRef),transactionDate);
    }

    private DrilldownLinkInfoDto parseDrilldownLinkInfo(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        Map<String, String> map = Arrays.stream(value.split(","))
                .map(s -> s.split("=", 2))
                .filter(arr -> arr.length == 2)
                .collect(Collectors.toMap(
                        arr -> arr[0].trim(),
                        arr -> arr[1].trim()
                ));

        return DrilldownLinkInfoDto.builder()
                .companyPoid(
                        map.containsKey("COMPANY_POID")
                                ? Long.valueOf(map.get("COMPANY_POID"))
                                : null)
                .targetDocId(map.get("TARGET_DOC_ID"))
                .docKeyPoid(
                        map.containsKey("DOC_KEY_POID")
                                ? Long.valueOf(map.get("DOC_KEY_POID"))
                                : null)
                .build();
    }
}
