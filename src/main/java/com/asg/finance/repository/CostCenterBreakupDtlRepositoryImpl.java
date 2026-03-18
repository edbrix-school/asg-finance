package com.asg.finance.repository;

import com.asg.finance.dto.CostCenterBreakupRequestDto;
import com.asg.finance.dto.CostCenterBreakupResponseDto;
import com.asg.finance.dto.GlVoucherCostCenterBreakupResponseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class CostCenterBreakupDtlRepositoryImpl implements CostCenterBreakupDtlRepository{

    @PersistenceContext
    private EntityManager entityManager;

    public GlVoucherCostCenterBreakupResponseDto loadCostCenters(
            Long groupPoid,
            Long companyPoid,
            String docId,
            Long transactionPoid) {

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_VOUCH_COSTBREAK_LOAD");

            query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

            query.setParameter("P_GROUP_POID", groupPoid);
            query.setParameter("P_COMPANY_POID", companyPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_TRANSACTION_POID", transactionPoid);

            query.execute();

            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
        List<CostCenterBreakupResponseDto> costBreakupList = mapToCostBreakup(rs);

        GlVoucherCostCenterBreakupResponseDto response = new GlVoucherCostCenterBreakupResponseDto();
        response.setCostBreakupList(costBreakupList);


        return response;
    }

    private List<CostCenterBreakupResponseDto> mapToCostBreakup(ResultSet rs) {
        List<CostCenterBreakupResponseDto> costBreakupList = new ArrayList<>();
        try {
            while (rs.next()) {
                CostCenterBreakupResponseDto dto = new CostCenterBreakupResponseDto();
                dto.setMainDetRowId(rs.getLong("MAIN_DET_ROW_ID"));
                dto.setGlPoid(rs.getLong("GL_POID"));
                dto.setCostDetRowId(rs.getLong("COST_DET_ROW_ID"));
                dto.setCostGroup(rs.getString("COST_GROUP"));
                dto.setCostPoid(rs.getString("COST_POID"));
                dto.setAmount(rs.getBigDecimal("AMOUNT"));
                dto.setDescription(rs.getString("DESCRIPTION"));
                costBreakupList.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping cost breakup", e);
        }
        return costBreakupList;
    }

    @Override
    public void deleteCostCenters(Long groupPoid,
                                  Long companyPoid,
                                  String docId,
                                  Long transactionPoid,
                                  Long userPoid) {

        log.info("Executing PROC_GL_VOUCH_COSTBREAK_DELETE for docId={}, transactionPoid={}", docId, transactionPoid);

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_VOUCH_COSTBREAK_DELETE");

        query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);

        query.setParameter("P_GROUP_POID", groupPoid);
        query.setParameter("P_COMPANY_POID", companyPoid);
        query.setParameter("P_DOC_ID", docId);
        query.setParameter("P_TRANSACTION_POID", transactionPoid);
        query.setParameter("P_LOGIN_USER_POID", userPoid);

        query.execute();
    }

    @Override
    public void insertCostBreakup(List<CostCenterBreakupRequestDto> breakupList) {
        if (breakupList == null || breakupList.isEmpty()) return;

        for (CostCenterBreakupRequestDto breakup : breakupList) {
            try {
                StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_VOUCH_COSTBREAK_INSERT");

                query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_MAIN_DET_ROW_ID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_GL_POID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_COST_DET_ROW_ID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_COST_GROUP", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_COST_POID", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_AMOUNT", BigDecimal.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);

                query.setParameter("P_GROUP_POID", breakup.getGroupPoid());
                query.setParameter("P_COMPANY_POID", breakup.getCompanyPoid());
                query.setParameter("P_DOC_ID", breakup.getDocId());
                query.setParameter("P_TRANSACTION_POID", breakup.getTransactionPoid());
                query.setParameter("P_MAIN_DET_ROW_ID", breakup.getMainDetRowId());
                query.setParameter("P_GL_POID", breakup.getGlPoid());
                query.setParameter("P_COST_DET_ROW_ID", breakup.getCostDetRowId());
                query.setParameter("P_COST_GROUP", breakup.getCostGroup());
                query.setParameter("P_COST_POID", breakup.getCostPoid());
                query.setParameter("P_AMOUNT", breakup.getAmount());
                query.setParameter("P_LOGIN_USER_POID", breakup.getLoginUserPoid());

                query.execute();
            } catch (Exception e) {
                log.error("Error inserting breakup for DOC_ID {} and TRANSACTION_POID {}: {}", breakup.getDocId(), breakup.getTransactionPoid(), e.getMessage());
                throw new RuntimeException("Failed to insert cost breakup", e);
            }
        }
    }



}
