package com.asg.finance.repository;

import com.asg.finance.dto.*;
import com.asg.common.lib.security.util.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
@RequiredArgsConstructor
public class PdcBatchCreationRepositoryImpl implements PdcBatchCreationRepository{

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public PayGlBreakupCheckResponseDto checkPayGlBreakup(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            Long payGlPoid
    ) {

        try {
            StoredProcedureQuery query = entityManager
                    .createStoredProcedureQuery("PROC_PDC_PAY_GL_BREAKUP_CHECK");

            // Input parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PAY_GL_POID", String.class, ParameterMode.IN);

            // Output parameters
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("P_COST_GROUP", String.class, ParameterMode.OUT);

            // Assign values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_PAY_GL_POID", String.valueOf(payGlPoid));

            // Execute
            query.execute();

            // Read output
            String result = (String) query.getOutputParameterValue("P_RESULT");
            String costGroup = (String) query.getOutputParameterValue("P_COST_GROUP");

            return PayGlBreakupCheckResponseDto.builder()
                    .result(result)
                    .costGroup(costGroup)
                    .build();

        } catch (Exception ex) {
            throw new RuntimeException("Error executing PROC_PDC_PAY_GL_BREAKUP_CHECK: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional
    public PdcBatchCreationProcResponse runBatchCreation(PdcBatchCreationProcRequest request) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_PDC_CHQ_BATCH_CREATION");

        Long companyPoid = UserContext.getCompanyPoid();
        Long loginUserPoid = UserContext.getUserPoid();
        String loginUser = String.valueOf(loginUserPoid);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

        String formattedDate = request.getStartDate()
                .format(formatter)
                .toUpperCase();



        // Register IN parameters
        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_NO_OF_CHQ", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_CHQ_AMT", Double.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_START_CHQ_NO", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_START_DATE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_PRE_PRINTED", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_NARRATION", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_BILL_REF", String.class, ParameterMode.IN);

        // Register OUT parameter
        query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);

        // Set parameter values
        query.setParameter("P_COMPANY_POID", companyPoid);
        query.setParameter("P_TRANSACTION_POID", request.getTransactionPoid());
        query.setParameter("P_NO_OF_CHQ", request.getNoOfCheques());
        query.setParameter("P_CHQ_AMT", request.getChequeAmount());
        query.setParameter("P_START_CHQ_NO", request.getStartChequeNo());
        query.setParameter("P_START_DATE", formattedDate);
        query.setParameter("P_PRE_PRINTED", request.getPrePrinted());
        query.setParameter("P_LOGIN_USER", loginUser);
        query.setParameter("P_NARRATION", request.getNarration());
        query.setParameter("P_BILL_REF", request.getBillRef());

        // Execute stored procedure
        query.execute();
        entityManager.flush();
        entityManager.clear();

        // Retrieve response
        String status = (String) query.getOutputParameterValue("P_STATUS");


        // RETURN BOTH STATUS + DATA
         return new PdcBatchCreationProcResponse(status, null);
    }

    @Override
    public PdcBatchCreationProcResponse runBankPosting(PdcBankPostingProcRequest request) {

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_PDC_CHQ_BANK_POSTING");

        Long companyPoid = UserContext.getCompanyPoid();
        Long groupPoid = UserContext.getGroupPoid();
        Long loginUserPoid = UserContext.getUserPoid();
        String loginUser = String.valueOf(loginUserPoid);



        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_PAY_GL_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_PAYING_TO", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_BANK_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_NO_OF_CHQ", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_CHQ_AMT", Double.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_START_CHQ_NO", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_START_DATE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER", String.class, ParameterMode.IN);

        query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);

        // PARAM VALUES
        query.setParameter("P_COMPANY_POID", companyPoid);
        query.setParameter("P_TRANSACTION_POID", request.getTransactionPoid());
        query.setParameter("P_GROUP_POID",groupPoid);
        query.setParameter("P_PAY_GL_POID", request.getPayGlPoid());
        query.setParameter("P_PAYING_TO", request.getPayingTo());
        query.setParameter("P_BANK_POID", request.getBankPoid());
        query.setParameter("P_NO_OF_CHQ", request.getNoOfChqs());
        query.setParameter("P_CHQ_AMT", request.getChequeAmount());
        query.setParameter("P_START_CHQ_NO", request.getStartChequeNo());
        query.setParameter("P_START_DATE", request.getStartDate());
        query.setParameter("P_LOGIN_USER", loginUser);

        // EXECUTE
        query.execute();

        // RESPONSE
        String status = (String) query.getOutputParameterValue("P_STATUS");

        return new PdcBatchCreationProcResponse(status, null);
    }

    @Override
    public PdcBatchCreationProcResponse runBatchCreationXL(PdcBatchCreationExcelProcRequest request) {

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_PDC_CHQ_BATCH_CREATION_XL");

        Long companyPoid = UserContext.getCompanyPoid();
        Long loginUserPoid = UserContext.getUserPoid();
        String loginUser = String.valueOf(loginUserPoid);

        query.registerStoredProcedureParameter("P_COMPANY_POID",   Long.class,    ParameterMode.IN);
        query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class,  ParameterMode.IN);
        query.registerStoredProcedureParameter("P_NO_OF_CHQ",      Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_CHQ_AMT",        BigDecimal.class,  ParameterMode.IN);
        query.registerStoredProcedureParameter("P_START_CHQ_NO",   String.class,  ParameterMode.IN);
        query.registerStoredProcedureParameter("P_START_DATE",     String.class,  ParameterMode.IN);
        query.registerStoredProcedureParameter("P_PRE_PRINTED",    String.class,  ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER",     String.class,  ParameterMode.IN);
        query.registerStoredProcedureParameter("P_NARRATION",      String.class,  ParameterMode.IN);
        query.registerStoredProcedureParameter("P_BILL_REF",       String.class,  ParameterMode.IN);

        query.registerStoredProcedureParameter("P_STATUS",         String.class,  ParameterMode.OUT);

        query.setParameter("P_COMPANY_POID",    companyPoid);
        query.setParameter("P_TRANSACTION_POID", request.getTransactionPoid());
        query.setParameter("P_NO_OF_CHQ",       request.getNoOfCheques());
        query.setParameter("P_CHQ_AMT",         request.getChequeAmount());
        query.setParameter("P_START_CHQ_NO",    request.getStartChequeNo());
        query.setParameter("P_START_DATE",      request.getStartDate());
        query.setParameter("P_PRE_PRINTED",     request.getPrePrinted());
        query.setParameter("P_LOGIN_USER",      loginUser);
        query.setParameter("P_NARRATION",       request.getNarration());
        query.setParameter("P_BILL_REF",        request.getBillRef());

        query.execute();

        String status = (String) query.getOutputParameterValue("P_STATUS");

        return new PdcBatchCreationProcResponse(status, null);
    }
}
