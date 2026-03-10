package com.asg.finance.repository;


import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.BankDepositVoucherDtlDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class BankDepositVoucherProcRepositoryImpl implements BankDepositVoucherProcRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LovDataService lovService;

    @Override
    public void callBeforeSaveValidation(Long companyPoid, Long bankPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_BANK_DEP_BEFORE_SAVE");
        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_BANK_GL_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

        query.setParameter("P_LOGIN_GROUP_POID", 1L);
        query.setParameter("P_LOGIN_USER_POID", 1L);
        query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
        query.setParameter("P_BANK_GL_POID", bankPoid);
        query.execute();

        String result = (String) query.getOutputParameterValue("P_RESULT");
        if (result != null && result.contains("ERROR")) {
            throw new RuntimeException(result);
        }
    }

    @Override
    public void callChequeStatusValidation(String refDocRef, Long refDocPoid) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GET_CHEQUE_CURRENT_STATUS");
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("P_RESULT2", String.class, ParameterMode.OUT);

        query.setParameter("P_DOC_ID", refDocRef);
        query.setParameter("P_DOC_KEY_POID", refDocPoid);
        query.execute();

        String result = (String) query.getOutputParameterValue("P_RESULT");
        if (result != null && result.equalsIgnoreCase("RECONCILED")) {
            throw new RuntimeException("Cheque already reconciled for ref " + refDocRef);
        }
    }

    @Override
    public List<BankDepositVoucherDtlDto> loadPendingPayments(Long bankPoid, String type, String bankFilter) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_BANK_DEPOSIT_LOAD_PYMT");
        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_PAYMENT_TYPE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_BANK_FILTER", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_BANK_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_DATE", java.sql.Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LINE_TYPE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid() != null ? UserContext.getUserPoid(): null);
        query.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid() != null ? UserContext.getUserPoid() : null);
        query.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid()!= null ? UserContext.getCompanyPoid() : null);
        query.setParameter("P_PAYMENT_TYPE", type);
        query.setParameter("P_BANK_FILTER", bankFilter);
        query.setParameter("P_BANK_POID", bankPoid);
        query.setParameter("P_DOC_DATE", new java.sql.Date(System.currentTimeMillis()));
        query.setParameter("P_LINE_TYPE", "OTHERS");
        query.execute();

        String result = (String) query.getOutputParameterValue("P_RESULT");

        if (result != null) {
            if (result.contains("Bank Filter is a required field") || result.contains("Bank Filter")) {
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

            if (result.contains("ERROR") || result.contains("WARNING")) {
                throw new ValidationException(result);
            }
        }
        ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
        List<BankDepositVoucherDtlDto> resultList = new ArrayList<>();

        if (rs == null) {
            log.info("No pending payments found");
            return resultList;
        }

        try {
            long detRowIdCounter = 1L;
            while (rs.next()) {
                BankDepositVoucherDtlDto dto = BankDepositVoucherDtlDto.builder()
                        .paymentMainPoid(rs.getLong("PAYMENT_MAIN_POID"))
                        .refDocPoid(rs.getLong("REF_DOC_POID"))
                        .refDocRef(rs.getString("REF_DOC_REF"))
                        .refDocId(rs.getString("REF_DOC_ID"))
                        .rcpDate(rs.getDate("RCP_DATE") != null ? rs.getDate("RCP_DATE").toLocalDate() : null)
                        .bankPoid(rs.getLong("BANK_POID"))
                        .chqAcName(rs.getString("CHQ_AC_NAME"))
                        .chqAcNo(rs.getString("CHQ_AC_NO"))
                        .chqCardNo(rs.getString("CHQ_CARDNO"))
                        .chqDate(rs.getDate("CHQ_DATE") != null ? rs.getDate("CHQ_DATE").toLocalDate() : null)
                        .amount(rs.getBigDecimal("AMOUNT"))
                        .pymtType(rs.getString("PYMT_TYPE"))
                        .detRowId(detRowIdCounter++)
                        .build();

                setBankDetailsForDto(dto);

                resultList.add(dto);
            }
        } catch (Exception e) {
            log.error("Error reading result set: {}", e.getMessage(), e);
            throw new ValidationException("Error loading pending payments: " + e.getMessage());
        }

        return resultList;
    }

    private void setBankDetailsForDto(BankDepositVoucherDtlDto dto) {
        if (dto.getBankPoid() != null) {
            try {
                dto.setBankDet(lovService.getDetailsByPoidAndLovName(dto.getBankPoid(), "CUSTOMER_BANK_MASTER"));
            } catch (Exception e) {
                log.warn("Failed to fetch bank details for bankPoid: {}", dto.getBankPoid(), e);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPaymentsCompleted(Long transactionPoid, Long groupPoid, Long companyPoid, String paymentType) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_BANK_DEPOSIT_UPDT_PYMT");
        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_BDV_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_PAYMENT_TYPE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

        query.setParameter("P_LOGIN_GROUP_POID", groupPoid);
        query.setParameter("P_LOGIN_USER_POID", 1L);
        query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
        query.setParameter("P_BDV_POID", transactionPoid);
        query.setParameter("P_PAYMENT_TYPE", paymentType);
        query.execute();

        String result = (String) query.getOutputParameterValue("P_RESULT");
        if (result != null && result.contains("ERROR")) {
            throw new ValidationException("PROC_GL_BANK_DEPOSIT_UPDT_PYMT returned error: " + result);
        }
    }
}
