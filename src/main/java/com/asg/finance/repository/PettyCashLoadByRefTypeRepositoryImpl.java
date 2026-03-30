package com.asg.finance.repository;

import com.asg.finance.dto.PettyCashFromFdaDto;
import com.asg.finance.dto.PettyCashFromFfDto;
import com.asg.finance.dto.PettyCashFromGenrlPoDto;
import com.asg.finance.dto.PettyCashFromGrnDto;
import com.asg.finance.dto.PettyCashFromPoDto;
import com.asg.finance.dto.PettyGlBalanceDto;
import com.asg.finance.dto.PettyRefTypeResponse;
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
    public PettyRefTypeResponse<PettyCashFromPoDto> loadPettyCashFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String rfqPoid
    ) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_PETTY_CREATE_FROM_PO");

            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RFQ_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_RFQ_POID", rfqPoid);

            query.execute();

            String resultValue = (String) query.getOutputParameterValue("P_RESULT");
            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");

            log.info("PROC_GL_PETTY_CREATE_FROM_PO executed successfully. Message: {}", resultValue);

            return PettyRefTypeResponse.<PettyCashFromPoDto>builder()
                    .message(resultValue)
                    .responseList(mapToPODto(rs))
                    .build();

        } catch (Exception e) {
            log.error("Error executing PROC_GL_PETTY_CREATE_FROM_PO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load petty cash data from PO: " + e.getMessage(), e);
        }
    }

    private List<PettyCashFromPoDto> mapToPODto(ResultSet rs) {
        List<PettyCashFromPoDto> list = new ArrayList<>();
        try {
            while (rs != null && rs.next()) {
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
    public PettyRefTypeResponse<PettyCashFromFfDto> loadPettyCashFromFf(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid
    ) {
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
            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");

            log.info("PROC_GL_PETTY_CREATE_FROM_FF executed successfully, message: {}", resultValue);

            return PettyRefTypeResponse.<PettyCashFromFfDto>builder()
                    .message(resultValue)
                    .responseList(mapToFFDto(rs))
                    .build();

        } catch (Exception e) {
            log.error("Error executing PROC_GL_PETTY_CREATE_FROM_FF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load petty cash from FF: " + e.getMessage(), e);
        }
    }

    private List<PettyCashFromFfDto> mapToFFDto(ResultSet rs) {
        List<PettyCashFromFfDto> list = new ArrayList<>();
        if (rs == null) return list;
        try {
            while (rs != null && rs.next()) {
                PettyCashFromFfDto dto = PettyCashFromFfDto.builder()
                        .chargePoid(rs.getLong("CHARGE_POID"))
                        .chargeAmount(rs.getBigDecimal("CHARGE_AMOUNT"))
                        .ffAmount(rs.getBigDecimal("FF_AMOUNT"))
                        .taxPoid(rs.getLong("TAX_POID"))
                        .refDocId(rs.getString("REF_DOC_ID"))
                        .refDocPoid(rs.getLong("REF_DOC_POID"))
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
    public PettyRefTypeResponse<PettyCashFromFdaDto> loadPettyCashFromFda(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String fdaPoid
    ) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_PETTY_CREATE_FROM_FDA");

            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_FDA_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_FDA_POID", fdaPoid);

            query.execute();

            String resultValue = (String) query.getOutputParameterValue("P_RESULT");
            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");

            log.info("PROC_GL_PETTY_CREATE_FROM_FDA executed successfully. Message: {}", resultValue);

            return PettyRefTypeResponse.<PettyCashFromFdaDto>builder()
                    .message(resultValue)
                    .responseList(mapToFDADto(rs))
                    .build();

        } catch (Exception e) {
            log.error("Error executing PROC_GL_PETTY_CREATE_FROM_FDA: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load petty cash from FDA: " + e.getMessage(), e);
        }
    }

    private List<PettyCashFromFdaDto> mapToFDADto(ResultSet rs) {
        List<PettyCashFromFdaDto> list = new ArrayList<>();
        try {
            while (rs != null && rs.next()) {
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
    public PettyRefTypeResponse<PettyGlBalanceDto> getPettyGlBalance(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String docId,
            Long docKeyPoid,
            String lovName,
            Long lovValue
    ) {
        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_PETTY_GL_DEFAULT_BALANCE");

            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOV_NAME", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOV_VALUE", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_DOC_KEY_POID", docKeyPoid);
            query.setParameter("P_LOV_NAME", lovName);
            query.setParameter("P_LOV_VALUE", lovValue);

            query.execute();

            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");

            log.info("PROC_PETTY_GL_DEFAULT_BALANCE executed successfully");

            return PettyRefTypeResponse.<PettyGlBalanceDto>builder()
                    .responseList(mapToBalanceDto(rs))
                    .build();

        } catch (Exception e) {
            log.error("Error executing PROC_PETTY_GL_DEFAULT_BALANCE: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch petty GL balance: " + e.getMessage(), e);
        }
    }

    private List<PettyGlBalanceDto> mapToBalanceDto(ResultSet rs) {
        List<PettyGlBalanceDto> list = new ArrayList<>();

        try {
            while (rs != null && rs.next()) {
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

    @Override
    public PettyRefTypeResponse<PettyCashFromGrnDto> loadPettyCashFromGrn(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String transactionDate,
            String grnSupplierPoid
    ) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_PETTY_INSERT_GRN_JOBS");

            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TRANSACTION_DATE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_GRN_SUPPLIER_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_TRANSACTION_DATE", transactionDate);
            query.setParameter("P_GRN_SUPPLIER_POID", grnSupplierPoid);

            query.execute();

            String resultValue = (String) query.getOutputParameterValue("P_RESULT");
            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");

            log.info("PROC_GL_PETTY_INSERT_GRN_JOBS executed successfully. Message: {}", resultValue);

            return PettyRefTypeResponse.<PettyCashFromGrnDto>builder()
                    .message(resultValue)
                    .responseList(mapToGrnDto(rs))
                    .build();

        } catch (Exception e) {
            log.error("Error executing PROC_GL_PETTY_INSERT_GRN_JOBS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load petty cash from GRN: " + e.getMessage(), e);
        }
    }

    private List<PettyCashFromGrnDto> mapToGrnDto(ResultSet rs) {
        List<PettyCashFromGrnDto> list = new ArrayList<>();
        try {
            while (rs != null && rs.next()) {
                PettyCashFromGrnDto dto = PettyCashFromGrnDto.builder()
                        .transactionPoid(rs.getLong("TRANSACTION_POID"))
                        .transactionDate(rs.getString("TRANSACTION_DATE"))
                        .docRef(rs.getString("DOC_REF"))
                        .companyPoid(rs.getLong("COMPANY_POID"))
                        .supplierPoid(rs.getLong("SUPPLIER_POID"))
                        .locationPoid(rs.getLong("LOCATION_POID"))
                        .remarks(rs.getString("REMARKS"))
                        .grandTotal(rs.getBigDecimal("GRAND_TOTAL"))
                        .drilldownLinkInfo(rs.getString("DRILLDOWN_LINK_INFO"))
                        .build();
                list.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping OUTDATA to PettyCashFromGrnDto: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public PettyRefTypeResponse<PettyCashFromGenrlPoDto> loadPettyCashFromCompletedPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid
    ) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_PETTY_CREATE_GENRL_PO");

            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PO_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_PO_POID", poPoid);

            query.execute();

            String resultValue = (String) query.getOutputParameterValue("P_RESULT");
            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");

            log.info("PROC_GL_PETTY_CREATE_GENRL_PO executed successfully. Message: {}", resultValue);

            return PettyRefTypeResponse.<PettyCashFromGenrlPoDto>builder()
                    .message(resultValue)
                    .responseList(mapToGenrlPoDto(rs))
                    .build();

        } catch (Exception e) {
            log.error("Error executing PROC_GL_PETTY_CREATE_GENRL_PO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load petty cash from completed PO: " + e.getMessage(), e);
        }
    }

    private List<PettyCashFromGenrlPoDto> mapToGenrlPoDto(ResultSet rs) {
        List<PettyCashFromGenrlPoDto> list = new ArrayList<>();
        try {
            while (rs != null && rs.next()) {
                PettyCashFromGenrlPoDto dto = PettyCashFromGenrlPoDto.builder()
                        .stockPoid(rs.getLong("STOCK_POID"))
                        .stockUnitPoid(rs.getLong("STOCK_UNIT_POID"))
                        .poQty(rs.getBigDecimal("PO_QTY"))
                        .price(rs.getBigDecimal("PRICE"))
                        .discount(rs.getBigDecimal("DISCOUNT"))
                        .total(rs.getBigDecimal("TOTAL"))
                        .remarks(rs.getString("REMARKS"))
                        .refDocId(rs.getString("REF_DOC_ID"))
                        .refDocPoid(rs.getString("REF_DOC_POID"))
                        .build();
                list.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping OUTDATA to PettyCashFromGenrlPoDto: " + e.getMessage(), e);
        }
        return list;
    }

}
