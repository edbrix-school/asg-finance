package com.asg.finance.repository;

import com.asg.finance.dto.ApPaymentRequestResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.dialect.OracleTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Repository
@Transactional
public class ApPaymentRequestCustomRepositoryImpl implements ApPaymentRequestCustomRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String REMARKS = "remarks";
    private static final String TAX_AMOUNT = "taxAmount";
    private static final String REF_DOC_ID = "refDocId";
    private static final String TAX_POID = "taxPoid";
    private static final String REF_DOC_POID = "refDocPoid";
    private static final String TAX_PERCENTAGE = "taxPercentage";

    private ApPaymentRequestResponse executeProcedure(
            String procedureName,
            Map<Integer, Object> inParams,
            List<String> outColumns,
            String detail
    ) {
        return jdbcTemplate.execute(
                (CallableStatementCreator) con -> {

                    CallableStatement cs =
                            con.prepareCall("{call " + procedureName + "(?, ?, ?, ?, ?, ?)}");

                    inParams.forEach((index, value) -> setSpValues(index, value, cs));

                    cs.registerOutParameter(5, Types.VARCHAR);
                    cs.registerOutParameter(6, OracleTypes.CURSOR);

                    return cs;
                },

                (CallableStatementCallback<ApPaymentRequestResponse>) cs -> {

                    ApPaymentRequestResponse response = new ApPaymentRequestResponse();
                    List<Map<String, Object>> records = new ArrayList<>();

                    cs.execute();

                    String resultMsg = cs.getString(5);
                    response.setMessage(resultMsg);

                    if (resultMsg != null &&
                            (resultMsg.toUpperCase().startsWith("WARNING")
                                    || resultMsg.toUpperCase().startsWith("ERROR"))) {

                        response.setRecords(Collections.emptyList());
                        return response;
                    }

                    ResultSet rs = (ResultSet) cs.getObject(6);

                    if (rs == null) {
                        response.setRecords(Collections.emptyList());
                        return response;
                    }
                    Map<String, String> columnMapping = outColumns.stream()
                            .collect(Collectors.toMap(
                                    col -> col,
                                    this::toDbColumn
                            ));

                    try (rs) {

                        while (rs.next()) {

                            Map<String, Object> row = new LinkedHashMap<>();

                            for (String column : outColumns) {
                                row.put(column, rs.getObject(columnMapping.get(column)));
                            }

                            records.add(row);
                        }
                    }

                    if(records.isEmpty()){
                        throw new RuntimeException(String.format("No %s to display",detail));
                    }

                    response.setRecords(records);
                    return response;
                }
        );
    }

    // ================= CREATE FROM PO =================
    @Override
    public ApPaymentRequestResponse createFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid
    ) {

        Map<Integer, Object> inParams = Map.of(
                1, loginGroupPoid,
                2, loginCompanyPoid,
                3, loginUserPoid,
                4, poPoid
        );

        // column names based on SP
        List<String> columns = List.of(
                "stockPoid", "stockUnitPoid", "PoQty", "price",
                "discount", "baseAmount", TAX_POID, TAX_PERCENTAGE,
                TAX_AMOUNT, "amount", REMARKS, REF_DOC_ID,
                REF_DOC_POID, "refDetRowId"
        );

        return executeProcedure("PROC_AP_PR_CREATE_FROM_PO", inParams, columns, "stocks");
    }

    // ================= CREATE FROM MTA =================
    @Override
    public ApPaymentRequestResponse createFromMta(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String mtaPoid
    ) {

        Map<Integer, Object> inParams = Map.of(
                1, groupPoid,
                2, companyPoid,
                3, userPoid,
                4, mtaPoid
        );

        // column names based on SP
        List<String> columns = List.of(
                "stockPoid", "stockUnitPoid", "PoQty", "price",
                "discount", "baseAmount", TAX_POID, TAX_PERCENTAGE,
                TAX_AMOUNT, "amount", REMARKS, REF_DOC_ID,
                REF_DOC_POID, "refDetRowId"
        );

        return executeProcedure("PROC_AP_PR_CREATE_FROM_MTA", inParams, columns, "stocks");
    }

    // ================= CREATE FROM FF =================
    @Override
    public ApPaymentRequestResponse createFromFf(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid
    ) {

        Map<Integer, Object> inParams = Map.of(
                1, loginGroupPoid,
                2, loginCompanyPoid,
                3, loginUserPoid,
                4, ffPoid
        );

        // column name based on SP
        List<String> columns = List.of(
                "chargePoid", "chargeBaseAmount", "ffAmount",
                REF_DOC_ID, REF_DOC_POID, "fdaDetRowId",
                TAX_POID, TAX_PERCENTAGE, TAX_AMOUNT, "chargeAmount"
        );

        return executeProcedure("PROC_AP_PR_CREATE_FROM_FF", inParams, columns, "charges");
    }

    // ================= CREATE FROM FDA =================
    @Override
    public ApPaymentRequestResponse createFromFda(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String fdaPoid
    ) {

        Map<Integer, Object> inParams = Map.of(
                1, groupPoid,
                2, companyPoid,
                3, userPoid,
                4, fdaPoid
        );

        // column name based on SP
        List<String> columns = List.of(
                "chargePoid", "chargeBaseAmount", "pdaAmount",
                REMARKS, REF_DOC_ID, REF_DOC_POID,
                "fdaDetRowId", TAX_POID, TAX_PERCENTAGE,
                TAX_AMOUNT, "chargeAmount"
        );

        return executeProcedure("PROC_AP_PR_CREATE_FROM_FDA", inParams, columns, "charges");
    }

    private void setSpValues(int index, Object value, CallableStatement cs) {
        try {
            if (value == null) {
                cs.setNull(index, Types.NULL);
            } else if (value instanceof Long l) {
                cs.setLong(index, l);
            } else if (value instanceof Integer i) {
                cs.setInt(index, i);
            } else if (value instanceof String s) {
                cs.setString(index, s);
            } else {
                cs.setObject(index, value);
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("Error setting SP parameter", e);
        }
    }


    private String toDbColumn(String camelCase) {
        return camelCase
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase();
    }
}