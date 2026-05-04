package com.asg.finance.repository;

import com.asg.finance.dto.ApPaymentRequestResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Transactional
public class ApPaymentRequestCustomRepositoryImpl implements ApPaymentRequestCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private static final String REMARKS = "REMARKS";
    private static final String TAX_AMOUNT = "TAX_AMOUNT";
    private static final String REF_DOC_ID = "REF_DOC_ID";
    private static final String TAX_POID = "TAX_POID";
    private static final String REF_DOC_POID = "REF_DOC_POID";
    private static final String TAX_PERCENTAGE = "TAX_PERCENTAGE";
    private static final String P_LOGIN_COMPANY_POID = "P_LOGIN_COMPANY_POID";
    private static final String P_LOGIN_GROUP_POID = "P_LOGIN_GROUP_POID";
    private static final String P_LOGIN_USER_POID = "P_LOGIN_USER_POID";

    private ApPaymentRequestResponse executeProcedure(
            String procedureName,
            Map<String, Object> inParams,
            List<String> outColumns
    ) {

        StoredProcedureQuery sp = entityManager.createStoredProcedureQuery(procedureName);

        // Register IN parameters
        inParams.forEach((key, value) ->
                sp.registerStoredProcedureParameter(key, value.getClass(), ParameterMode.IN)
        );

        // Register OUT parameters
        sp.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        // Set IN parameters
        inParams.forEach(sp::setParameter);

        sp.execute();

        String resultMessage = (String) sp.getOutputParameterValue("P_RESULT");

        List<Map<String, Object>> records = new ArrayList<>();

        if (resultMessage != null && resultMessage.startsWith("Successfully")) {

            @SuppressWarnings("unchecked")
            List<Object[]> rows = sp.getResultList();

            for (Object[] row : rows) {
                Map<String, Object> map = new LinkedHashMap<>();

                for (int i = 0; i < outColumns.size(); i++) {
                    map.put(outColumns.get(i), row[i]);
                }

                records.add(map);
            }
        }

        return ApPaymentRequestResponse
                .builder()
                .message(resultMessage)
                .records(records)
                .build();
    }

    // ================= CREATE FROM PO =================
    @Override
    public ApPaymentRequestResponse createFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid
    ) {

        Map<String, Object> inParams = Map.of(
                P_LOGIN_GROUP_POID, loginGroupPoid,
                P_LOGIN_COMPANY_POID, loginCompanyPoid,
                P_LOGIN_USER_POID, loginUserPoid,
                "P_PO_POID", poPoid
        );

        // column names based on SP
        List<String> columns = List.of(
                "STOCK_POID", "STOCK_UNIT_POID", "PO_QTY", "PRICE",
                "DISCOUNT", "BASE_AMOUNT", TAX_POID, TAX_PERCENTAGE,
                TAX_AMOUNT, "AMOUNT", REMARKS, REF_DOC_ID,
                REF_DOC_POID, "REF_DET_ROW_ID"
        );

        return executeProcedure("PROC_AP_PR_CREATE_FROM_PO", inParams, columns);
    }

    // ================= CREATE FROM MTA =================
    @Override
    public ApPaymentRequestResponse createFromMta(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String poPoid
    ) {

        Map<String, Object> inParams = Map.of(
                P_LOGIN_GROUP_POID, groupPoid,
                P_LOGIN_COMPANY_POID, companyPoid,
                P_LOGIN_USER_POID, userPoid,
                "P_PO_POID", poPoid
        );

        // column names based on SP
        List<String> columns = List.of(
                "STOCK_POID", "STOCK_UNIT_POID", "PO_QTY", "PRICE",
                "DISCOUNT", "BASE_AMOUNT", TAX_POID, TAX_PERCENTAGE,
                TAX_AMOUNT, "AMOUNT", REMARKS, REF_DOC_ID,
                REF_DOC_POID, "REF_DET_ROW_ID"
        );

        return executeProcedure("PROC_AP_PR_CREATE_FROM_MTA", inParams, columns);
    }

    // ================= CREATE FROM FF =================
    @Override
    public ApPaymentRequestResponse createFromFf(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid
    ) {

        Map<String, Object> inParams = Map.of(
                P_LOGIN_GROUP_POID, loginGroupPoid,
                P_LOGIN_COMPANY_POID, loginCompanyPoid,
                P_LOGIN_USER_POID, loginUserPoid,
                "P_FF_POID", ffPoid
        );

        // column name based on SP
        List<String> columns = List.of(
                "CHARGE_POID", "CHARGE_BASE_AMOUNT", "FF_AMOUNT",
                REF_DOC_ID, REF_DOC_POID, "FDA_DET_ROW_ID",
                TAX_POID, TAX_PERCENTAGE, TAX_AMOUNT, "CHARGE_AMOUNT"
        );

        return executeProcedure("PROC_AP_PR_CREATE_FROM_FF", inParams, columns);
    }

    // ================= CREATE FROM FDA =================
    @Override
    public ApPaymentRequestResponse createFromFda(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String fdaPoid
    ) {

        Map<String, Object> inParams = Map.of(
                P_LOGIN_GROUP_POID, groupPoid,
                P_LOGIN_COMPANY_POID, companyPoid,
                P_LOGIN_USER_POID, userPoid,
                "P_FDA_POID", fdaPoid
        );

        // column name based on SP
        List<String> columns = List.of(
                "CHARGE_POID", "CHARGE_BASE_AMOUNT", "PDA_AMOUNT",
                REMARKS, REF_DOC_ID, REF_DOC_POID,
                "FDA_DET_ROW_ID", TAX_POID, TAX_PERCENTAGE,
                TAX_AMOUNT, "CHARGE_AMOUNT"
        );

        return executeProcedure("PROC_AP_PR_CREATE_FROM_FDA", inParams, columns);
    }
}