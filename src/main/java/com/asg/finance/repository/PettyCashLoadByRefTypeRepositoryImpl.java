package com.asg.finance.repository;

import com.asg.finance.dto.PettyCashFromFdaDto;
import com.asg.finance.dto.PettyCashFromFfDto;
import com.asg.finance.dto.PettyCashFromPoDto;
import com.asg.finance.dto.PettyGlBalanceDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository

public class PettyCashLoadByRefTypeRepositoryImpl implements PettyCashLoadByRefTypeRepository{

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<PettyCashFromPoDto> loadPettyCashFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String rfqPoid,
            StringBuilder result
    ) {
        List<PettyCashFromPoDto> responseList = new ArrayList<>();

        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_PETTY_CREATE_FROM_PO");

            // Register procedure parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RFQ_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

            // Set input values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_RFQ_POID", rfqPoid);

            // Execute the stored procedure
            query.execute();

            // Capture output message
            String resultValue = (String) query.getOutputParameterValue("P_RESULT");
            if (result != null) result.append(resultValue);

            // Process result set
            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
            responseList = mapToPODto(rs);

            log.info("PROC_GL_PETTY_CREATE_FROM_PO executed successfully. Message: {}", resultValue);

        } catch (Exception e) {
            log.error("Error executing PROC_GL_PETTY_CREATE_FROM_PO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load petty cash data from PO: " + e.getMessage(), e);
        }

        return responseList;
    }

    private List<PettyCashFromPoDto> mapToPODto(ResultSet rs) {
        List<PettyCashFromPoDto> list = new ArrayList<>();
        try {
            while (rs.next()) {
                PettyCashFromPoDto dto = PettyCashFromPoDto.builder()
                        .stockPoid(rs.getLong("STOCK_POID"))
                        .stockUnitPoid(rs.getLong("STOCK_UNIT_POID"))
                        .poQty(rs.getBigDecimal("PO_QTY"))
                        .price(rs.getBigDecimal("PRICE"))
                        .taxPoid(rs.getLong("TAX_POID"))
                        .remarks(rs.getString("REMARKS"))
                        .refDocId(rs.getString("REF_DOC_ID"))
                        .refDocPoid(rs.getString("REF_DOC_POID"))
                        .refDetRowId(rs.getLong("REF_DET_ROW_ID"))
                        .build();
                list.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping OUTDATA to PettyCashFromPoDto: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<PettyCashFromFfDto> loadPettyCashFromFf(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid,
            StringBuilder result
    ) {
        List<PettyCashFromFfDto> responseList = new ArrayList<>();

        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_PETTY_CREATE_FROM_FF");


            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_FF_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);


            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_FF_POID", ffPoid);


            query.execute();

            String resultValue = (String) query.getOutputParameterValue("P_RESULT");
            if (result != null) result.append(resultValue);


            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
            responseList = mapToFFDto(rs);

            log.info("PROC_GL_PETTY_CREATE_FROM_FF executed successfully, message: {}", resultValue);

        } catch (Exception e) {
            log.error("Error executing PROC_GL_PETTY_CREATE_FROM_FF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load petty cash from FF: " + e.getMessage(), e);
        }

        return responseList;
    }

    private List<PettyCashFromFfDto> mapToFFDto(ResultSet rs) {
        List<PettyCashFromFfDto> list = new ArrayList<>();
        try {
            while (rs.next()) {
                PettyCashFromFfDto dto = PettyCashFromFfDto.builder()
                        .chargePoid(rs.getLong("CHARGE_POID"))
                        .chargeAmount(rs.getBigDecimal("CHARGE_AMOUNT"))
                        .ffAmount(rs.getBigDecimal("FF_AMOUNT"))
                        .taxPoid(rs.getLong("TAX_POID"))
                        .refDocId(rs.getString("REF_DOC_ID"))
                        .refDocPoid(rs.getString("REF_DOC_POID"))
                        .fdaDetRowId(rs.getLong("FDA_DET_ROW_ID"))
                        .build();
                list.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping OUTDATA to DTO: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<PettyCashFromFdaDto> loadPettyCashFromFda(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String fdaPoid,
            StringBuilder result
    ) {
        List<PettyCashFromFdaDto> responseList = new ArrayList<>();

        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_PETTY_CREATE_FROM_FDA");

            // Register parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_FDA_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

            // Set inputs
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_FDA_POID", fdaPoid);

            // Execute
            query.execute();

            // Capture result message
            String resultValue = (String) query.getOutputParameterValue("P_RESULT");
            if (result != null) result.append(resultValue);

            // Process cursor
            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
            responseList = mapToFDADto(rs);

            log.info("PROC_GL_PETTY_CREATE_FROM_FDA executed successfully. Message: {}", resultValue);

        } catch (Exception e) {
            log.error("Error executing PROC_GL_PETTY_CREATE_FROM_FDA: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load petty cash from FDA: " + e.getMessage(), e);
        }

        return responseList;
    }

    private List<PettyCashFromFdaDto> mapToFDADto(ResultSet rs) {
        List<PettyCashFromFdaDto> list = new ArrayList<>();
        try {
            while (rs.next()) {
                PettyCashFromFdaDto dto = PettyCashFromFdaDto.builder()
                        .chargePoid(rs.getLong("CHARGE_POID"))
                        .pdaAmount(rs.getBigDecimal("PDA_AMOUNT"))
                        .taxPoid(rs.getLong("TAX_POID"))
                        .remarks(rs.getString("REMARKS"))
                        .refDocId(rs.getString("REF_DOC_ID"))
                        .refDocPoid(rs.getString("REF_DOC_POID"))
                        .fdaDetRowId(rs.getLong("FDA_DET_ROW_ID"))
                        .build();
                list.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping OUTDATA to DTO: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<PettyGlBalanceDto> getPettyGlBalance(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String docId,
            Long docKeyPoid,
            String lovName,
            Long lovValue
    ) {
        List<PettyGlBalanceDto> responseList = new ArrayList<>();

        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_PETTY_GL_DEFAULT_BALANCE");

            // Register IN parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOV_NAME", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOV_VALUE", Long.class, ParameterMode.IN);

            // Register OUT parameter
            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

            // Set parameter values
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_DOC_KEY_POID", docKeyPoid);
            query.setParameter("P_LOV_NAME", lovName);
            query.setParameter("P_LOV_VALUE", lovValue);

            // Execute procedure
            query.execute();

            // Read REF_CURSOR output
            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
            responseList = mapToBalanceDto(rs);

            log.info("PROC_PETTY_GL_DEFAULT_BALANCE executed successfully");

        } catch (Exception e) {
            log.error("Error executing PROC_PETTY_GL_DEFAULT_BALANCE: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch petty GL balance: " + e.getMessage(), e);
        }

        return responseList;
    }

    private List<PettyGlBalanceDto> mapToBalanceDto(ResultSet rs) {
        List<PettyGlBalanceDto> list = new ArrayList<>();

        try {
            while (rs.next()) {
                PettyGlBalanceDto dto = PettyGlBalanceDto.builder()
                        .balance(rs.getBigDecimal("BALANCE"))
                        .build();
                list.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping OUTDATA to PettyGlBalanceDto: " + e.getMessage(), e);
        }

        return list;
    }

}
