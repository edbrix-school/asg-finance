package com.asg.finance.repository;


import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.GlVoucherPendingBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.ShowPendingBillwiseBreakupResponseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class BillwiseBreakupDtlRepositoryImpl implements BillwiseBreakupDtlRepository{

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public GlVoucherLoadBillwiseBreakupResponseDto loadBillwiseBreakup(
            Long groupPoid,
            Long companyPoid,
            String docId,
            Long transactionPoid) {

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_VOUCH_BILLWISE_LOAD");

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
        List<LoadBillwiseBreakupResponseDto> billwiseList = mapToBillwiseBreakup(rs);

        GlVoucherLoadBillwiseBreakupResponseDto response = new GlVoucherLoadBillwiseBreakupResponseDto();
        response.setLoadBillwiseBreakupResponseDtoList(billwiseList);

        return response;
    }

    private List<LoadBillwiseBreakupResponseDto> mapToBillwiseBreakup(ResultSet rs) {
        List<LoadBillwiseBreakupResponseDto> list = new ArrayList<>();
        try {
            while (rs.next()) {
                LoadBillwiseBreakupResponseDto dto = new LoadBillwiseBreakupResponseDto();
                dto.setMainDetRowId(rs.getLong("MAIN_DET_ROW_ID"));
                dto.setBillDetRowId(rs.getLong("BILL_DET_ROW_ID"));
                dto.setGlPoid(rs.getLong("GL_POID"));
                dto.setBillRefType(rs.getString("BILL_REF_TYPE"));
                dto.setBillRef(rs.getString("BILL_REF"));
                dto.setBillDueDate(rs.getObject("BILL_DUE_DATE", LocalDate.class));
                dto.setDrAmt(rs.getBigDecimal("DR_AMT"));
                dto.setCrAmt(rs.getBigDecimal("CR_AMT"));
                dto.setBillRemarks(rs.getString("BILL_REMARKS"));
                list.add(dto);
            }
        } catch (SQLException e) {
            log.error("Error mapping ResultSet to DTO", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public GlVoucherPendingBillwiseBreakupResponseDto showPendingBillwiseBreakup(
            Long groupPoid,
            Long companyPoid,
            Long glPoid,
            LocalDate asOnDate) {

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_GET_BILLWISE_PENDING");

        query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_GL_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_ASON_DATE", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_GROUP_POID", groupPoid);
        query.setParameter("P_COMPANY_POID", companyPoid);
        query.setParameter("P_GL_POID", glPoid);
       // query.setParameter("P_ASON_DATE", asOnDate);
        query.setParameter(
                "P_ASON_DATE",
                asOnDate != null ? java.sql.Date.valueOf(asOnDate) : null
        );

        query.execute();

        ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
        List<ShowPendingBillwiseBreakupResponseDto> pendingList = mapToPendingBills(rs);
        GlVoucherPendingBillwiseBreakupResponseDto response = new GlVoucherPendingBillwiseBreakupResponseDto();
        response.setShowPendingBillwiseBreakupResponseDtoList(pendingList);

        return response;
    }

    @Override
    public GlVoucherPendingBillwiseBreakupResponseDto showAllPendingBillwiseBreakup(
            Long groupPoid,
            Long companyPoid,
            Long glPoid,
            LocalDate asOnDate) {

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_GET_BILLWISE_PENDING2");

        query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_GL_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_ASON_DATE", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_GROUP_POID", groupPoid);
        query.setParameter("P_COMPANY_POID", companyPoid);
        query.setParameter("P_GL_POID", glPoid);
        query.setParameter(
                "P_ASON_DATE",
                asOnDate != null ? java.sql.Date.valueOf(asOnDate) : null
        );

        query.execute();

        ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
        List<ShowPendingBillwiseBreakupResponseDto> pendingList = mapToPendingBills(rs);
        GlVoucherPendingBillwiseBreakupResponseDto response = new GlVoucherPendingBillwiseBreakupResponseDto();
        response.setShowPendingBillwiseBreakupResponseDtoList(pendingList);

        return response;
    }

    private List<ShowPendingBillwiseBreakupResponseDto> mapToPendingBills(ResultSet rs) {
        List<ShowPendingBillwiseBreakupResponseDto> list = new ArrayList<>();
        try {
            while (rs.next()) {
                ShowPendingBillwiseBreakupResponseDto dto = new ShowPendingBillwiseBreakupResponseDto();
                dto.setGlCompanyPoid(rs.getLong("GL_COMPANY_POID"));
                dto.setBillRef(rs.getString("BILL_REF"));
                dto.setBillDueDate(rs.getObject("BILL_DUE_DATE", LocalDate.class));
                dto.setRemarks(rs.getString("REMARKS"));
                dto.setBalance(rs.getBigDecimal("BALANCE"));
                list.add(dto);
            }
        } catch (SQLException e) {
            log.error("Error mapping ResultSet to PendingBillwiseResponseDto", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public void insertBillwiseBreakup(List<BillwiseBreakupRequestDto> breakupList) {
        if (breakupList == null || breakupList.isEmpty()) return;

        for (BillwiseBreakupRequestDto breakup : breakupList) {
            try {
                StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_VOUCH_BILLWISE_INSERT");

                query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_MAIN_DET_ROW_ID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_GL_POID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_GL_COMPANY_POID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_BILL_DET_ROW_ID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_BILL_REF_TYPE", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_BILL_REF", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_BILL_DUE_DATE", Date.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_DR_AMT", BigDecimal.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_CR_AMT", BigDecimal.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_BILL_REMARKS", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);

                query.setParameter("P_GROUP_POID", breakup.getGroupPoid());
                query.setParameter("P_COMPANY_POID", breakup.getCompanyPoid());
                query.setParameter("P_DOC_ID", breakup.getDocId());
                query.setParameter("P_TRANSACTION_POID", breakup.getTransactionPoid());
                query.setParameter("P_MAIN_DET_ROW_ID", breakup.getMainDetRowId());
                query.setParameter("P_GL_POID", breakup.getGlPoid());
                query.setParameter("P_GL_COMPANY_POID", breakup.getGlCompanyPoid());
                query.setParameter("P_BILL_DET_ROW_ID", breakup.getBillDetRowId());
                query.setParameter("P_BILL_REF_TYPE", breakup.getBillRefType());
                query.setParameter("P_BILL_REF", breakup.getBillRef());
                query.setParameter(
                        "P_BILL_DUE_DATE",
                        breakup.getBillDueDate() != null
                                ? java.sql.Date.valueOf(breakup.getBillDueDate())
                                : null
                );
                query.setParameter("P_DR_AMT", breakup.getDrAmt());
                query.setParameter("P_CR_AMT", breakup.getCrAmt());
                query.setParameter("P_BILL_REMARKS", breakup.getBillRemarks());
                query.setParameter("P_LOGIN_USER_POID", breakup.getLoginUserPoid());

                query.execute();
            } catch (Exception e) {
                log.error("Error inserting billwise breakup for DOC_ID {} and TRANSACTION_POID {}: {}",
                        breakup.getDocId(), breakup.getTransactionPoid(), e.getMessage());
                throw new RuntimeException("Failed to insert billwise breakup", e);
            }
        }
    }

    @Override
    public void deleteBillwiseBreakup(Long groupPoid, Long companyPoid, String docId, Long transactionPoid, Long loginUserPoid) {

            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_VOUCH_BILLWISE_DELETE");

            // Register input parameters
            query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);

            // Set parameter values
            query.setParameter("P_GROUP_POID", groupPoid);
            query.setParameter("P_COMPANY_POID", companyPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_TRANSACTION_POID", transactionPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);

            // Execute the stored procedure
            query.execute();

    }
}
