package com.asg.finance.repository;

import com.asg.common.lib.dto.BillwiseBreakupDto;
import com.asg.common.lib.dto.CostBreakupDto;
import com.asg.common.lib.dto.LedgerEntryDto;
import com.asg.common.lib.dto.VatBreakupDto;
import com.asg.common.lib.dto.response.GlPostingViewResponseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Repository
public class ImcoDepositRefundRepositoryImpl implements  ImcoDepositRefundRepository{

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public GlPostingViewResponseDto fetchGlPostingDetails(
            Long groupPoid,
            Long companyPoid,
            String docId,
            Long transactionPoid) {

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_POSTING_VIEW_LOAD_V2");

        query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);

        query.registerStoredProcedureParameter("OUTDATA1", void.class, ParameterMode.REF_CURSOR);
        query.registerStoredProcedureParameter("OUTDATA2", void.class, ParameterMode.REF_CURSOR);
        query.registerStoredProcedureParameter("OUTDATA3", void.class, ParameterMode.REF_CURSOR);
        query.registerStoredProcedureParameter("OUTDATA4", void.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_GROUP_POID", groupPoid);
        query.setParameter("P_COMPANY_POID", companyPoid);
        query.setParameter("P_DOC_ID", docId);
        query.setParameter("P_TRANSACTION_POID", transactionPoid);

        query.execute();

        ResultSet ledgerEntries = (ResultSet) query.getOutputParameterValue("OUTDATA1");
        ResultSet billwiseBreakup = (ResultSet) query.getOutputParameterValue("OUTDATA2");
        ResultSet costBreakup = (ResultSet) query.getOutputParameterValue("OUTDATA3");
        ResultSet vatBreakup = (ResultSet) query.getOutputParameterValue("OUTDATA4");

        GlPostingViewResponseDto response = new GlPostingViewResponseDto();
        response.setLedgerEntries(mapToLedgerEntries(ledgerEntries));
        response.setBillwiseBreakup(mapToBillwiseBreakup(billwiseBreakup));
        response.setCostBreakup(mapToCostBreakup(costBreakup));
        response.setVatBreakup(mapToVatBreakup(vatBreakup));

        return response;
    }

    private List<LedgerEntryDto> mapToLedgerEntries(ResultSet rs) {
        List<LedgerEntryDto> entries = new ArrayList<>();
        try {
            while (rs.next()) {
                LedgerEntryDto dto = new LedgerEntryDto();
                dto.setTransactionDate(convertToLocalDate(rs.getDate("TRANSACTION_DATE")));
                dto.setDocRef(rs.getString("DOC_REF"));
                dto.setNarration(rs.getString("NARRATION"));
                dto.setCompanyCode(rs.getString("COMPANY_CODE"));
                dto.setGlAcType(rs.getString("GL_AC_TYPE"));
                dto.setGlCode(rs.getString("GL_CODE"));
                dto.setGlDescription(rs.getString("GL_DESCRIPTION"));
                dto.setDrAmt(rs.getBigDecimal("DR_AMT"));
                dto.setCrAmt(rs.getBigDecimal("CR_AMT"));
                dto.setPostedBy(rs.getString("POSTED_BY"));
//                dto.setPostedDate(LocalDate.from(convertToLocalDate(rs.getDate("POSTED_DATE")).atStartOfDay()).atStartOfDay());
                entries.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping ledger entries", e);
        }
        return entries;
    }

    private List<BillwiseBreakupDto> mapToBillwiseBreakup(ResultSet rs) {
        List<BillwiseBreakupDto> entries = new ArrayList<>();
        try {
            while (rs.next()) {
                BillwiseBreakupDto dto = new BillwiseBreakupDto();
                dto.setTransactionDate(convertToLocalDate(rs.getDate("TRANSACTION_DATE")));
                dto.setDocRef(rs.getString("DOC_REF"));
                dto.setGlCode(rs.getString("GL_CODE"));
                dto.setGlDescription(rs.getString("GL_DESCRIPTION"));
                dto.setBillRefType(rs.getString("BILL_REF_TYPE"));
                dto.setBillRef(rs.getString("BILL_REF"));
                dto.setBillDueDate((rs.getDate("BILL_DUE_DATE")));
                dto.setRemarks(rs.getString("REMARKS"));
                dto.setDrAmt(rs.getBigDecimal("DR_AMT"));
                dto.setCrAmt(rs.getBigDecimal("CR_AMT"));
                dto.setGlCompany(rs.getString("GL_COMPANY"));
                entries.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping billwise breakup", e);
        }
        return entries;
    }

    private List<CostBreakupDto> mapToCostBreakup(ResultSet rs) {
        List<CostBreakupDto> entries = new ArrayList<>();
        try {
            while (rs.next()) {
                CostBreakupDto dto = new CostBreakupDto();
                dto.setTransactionDate(convertToLocalDate(rs.getDate("TRANSACTION_DATE")));
                dto.setDocRef(rs.getString("DOC_REF"));
                dto.setGlCode(rs.getString("GL_CODE"));
                dto.setGlDescription(rs.getString("GL_DESCRIPTION"));
                dto.setCostGroup(rs.getString("COST_GROUP"));
                dto.setCostPoid(rs.getLong("COST_POID"));
                dto.setAmt(rs.getBigDecimal("AMT"));
                dto.setGlCompany(rs.getString("GL_COMPANY"));
                entries.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping cost breakup", e);
        }
        return entries;
    }

    private List<VatBreakupDto> mapToVatBreakup(ResultSet rs) {
        List<VatBreakupDto> entries = new ArrayList<>();
        try {
            while (rs.next()) {
                VatBreakupDto dto = new VatBreakupDto();
                dto.setTransactionDate(convertToLocalDate(rs.getDate("TRANSACTION_DATE")));
                dto.setDocRef(rs.getString("DOC_REF"));
                dto.setGlCode(rs.getString("GL_CODE"));
                dto.setGlDescription(rs.getString("GL_DESCRIPTION"));
                dto.setTaxName(rs.getString("TAX_NAME"));
                dto.setTaxBaseAmount(rs.getBigDecimal("TAX_BASE_AMOUNT"));
                dto.setTaxAmount(rs.getBigDecimal("TAX_AMOUNT"));
                dto.setAmt(rs.getBigDecimal("AMT"));
                entries.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping VAT breakup", e);
        }
        return entries;
    }

    private LocalDate convertToLocalDate(Date date) {
        return date != null ? date.toLocalDate() : null;
    }

    @Override
    public boolean isValidReceipt(Long receiptPoid, String receiptNumber, Long companyPoid) {
        String sql = "SELECT COUNT(*) FROM GL_CHEQUE_CASH_PYMT_MAIN CCP " +
                     "WHERE NVL(TRUNC(CCP.CHQ_DATE), SYSDATE) <= SYSDATE " +
                     "AND CCP.STATUS = 'PENDING' " +
                     "AND CCP.REF_DOC_POID IS NOT NULL " +
                     "AND CCP.REF_DOC_POID = :receiptPoid " +
                     "AND DECODE(CCP.REF_DOC_REF, NULL, CCP.OLD_RCPVNO, CCP.REF_DOC_REF) = :receiptNumber " +
                     "AND CCP.PYMT_TYPE = 'CHEQUE' " +
                     "AND CCP.VOUCHER_TYPE = 'IMCOCHEQUE' " +
                     "AND CCP.COMPANY_POID = :companyPoid";

        Long count = (Long) entityManager.createNativeQuery(sql, Long.class)
                .setParameter("receiptPoid", receiptPoid)
                .setParameter("receiptNumber", receiptNumber)
                .setParameter("companyPoid", companyPoid)
                .getSingleResult();

        return count != null && count > 0;
    }
}
