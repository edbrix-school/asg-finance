package com.asg.finance.repository;

import com.asg.finance.dto.BillDetailDto;
import com.asg.finance.dto.ChequeDetailDto;
import com.asg.finance.dto.ImcoRefundLoadResponseDto;
import com.asg.common.lib.security.util.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Repository
public class ImcoChequeDetailsRepositoryImpl implements ImcoChequeDetailsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ImcoRefundLoadResponseDto fetchChequeAndBillDetails(
            Long groupPoid,
            Long companyPoid,
            String loginUser,
            Long receiptPoid,
            String receiptNumber) throws SQLException {
        ImcoRefundLoadResponseDto response = new ImcoRefundLoadResponseDto();

        StoredProcedureQuery loadBlNumberQuery = entityManager.createStoredProcedureQuery("PROC_IMCO_RTN_DETAILS");
        loadBlNumberQuery.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
        loadBlNumberQuery.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        loadBlNumberQuery.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        loadBlNumberQuery.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
        loadBlNumberQuery.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);
        loadBlNumberQuery.setParameter("P_GROUP_POID", groupPoid);
        loadBlNumberQuery.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
        loadBlNumberQuery.setParameter("P_LOGIN_USER_POID", getUserPoid());
        loadBlNumberQuery.setParameter("P_TRANSACTION_POID", receiptPoid);
        loadBlNumberQuery.execute();
        ResultSet blQueryRS = (ResultSet) loadBlNumberQuery.getOutputParameterValue("OUTDATA");
        if (blQueryRS != null && blQueryRS.next()) {
            String blNumber = blQueryRS.getString("BL_NUMBER");
            String accountName = blQueryRS.getString("ACCOUNT_NAME");

            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_IMCO_REFUND_LOAD");
            query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RECEIPT_NUM", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_BL_NUMBER", String.class, ParameterMode.IN);

            query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);
            query.registerStoredProcedureParameter("OUTDATA1", ResultSet.class, ParameterMode.REF_CURSOR);
            query.registerStoredProcedureParameter("P_PAYING_TO", String.class, ParameterMode.OUT);

            query.setParameter("P_GROUP_POID", groupPoid);
            query.setParameter("P_COMPANY_POID", companyPoid);
            query.setParameter("P_LOGIN_USER", loginUser);
            query.setParameter("P_RECEIPT_NUM", receiptNumber);
            query.setParameter("P_BL_NUMBER", blNumber);
            query.execute();

            ResultSet rsBills = (ResultSet) query.getOutputParameterValue("OUTDATA");
            ResultSet rsCheques = (ResultSet) query.getOutputParameterValue("OUTDATA1");
            String payingTo = (String) query.getOutputParameterValue("P_PAYING_TO");

            response.setBills(mapToBillDetails(rsBills));
            response.setCheques(mapToChequeDetails(rsCheques));
            response.setPayingTo(accountName);
            response.setBlNumber(blNumber);
        }
        return response;
    }

    private List<BillDetailDto> mapToBillDetails(ResultSet rs) {
        List<BillDetailDto> bills = new ArrayList<>();
        try {
            while (rs.next()) {
                BillDetailDto dto = new BillDetailDto();
                dto.setDetRowId(rs.getLong("DET_ROW_ID"));
                dto.setBillRef(rs.getString("BILL_REF"));
                dto.setRemarks(rs.getString("REMARKS"));
                dto.setBillAmount(rs.getBigDecimal("BILL_AMOUNT"));
                dto.setCreatedBy(rs.getString("CREATED_BY"));
                dto.setCreatedDate(convertToLocalDate(rs.getDate("CREATED_DATE")));
                bills.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping bill details", e);
        }
        return bills;
    }

    private List<ChequeDetailDto> mapToChequeDetails(ResultSet rs) {
        List<ChequeDetailDto> cheques = new ArrayList<>();
        try {
            while (rs.next()) {
                ChequeDetailDto dto = new ChequeDetailDto();
                dto.setPaymentMainPoid(rs.getLong("PAYMENT_MAIN_POID"));
                dto.setAmount(rs.getBigDecimal("AMOUNT"));
                dto.setChoPoid(rs.getLong("CHO_POID"));
                dto.setChoDate(convertToLocalDate(rs.getDate("CHO_DATE")));
                dto.setPymtType(rs.getString("PYMT_TYPE"));
                dto.setChqCardno(rs.getString("CHQ_CARDNO"));
                dto.setChqDate(convertToLocalDate(rs.getDate("CHQ_DATE")));
                dto.setBankPoid(rs.getLong("BANK_POID"));
                dto.setAddressPoid(rs.getLong("ADDRESS_POID"));
                dto.setChqAcName(rs.getString("CHQ_AC_NAME"));
                dto.setRcpDate(convertToLocalDate(rs.getDate("RCP_DATE")));
                dto.setChqAcNo(rs.getString("CHQ_AC_NO"));
                dto.setRemarks(rs.getString("REMARKS"));
                dto.setRefDocRef(rs.getString("REF_DOC_REF"));
                dto.setRefDocId(rs.getString("REF_DOC_ID"));
                dto.setRefDocPoid(rs.getLong("REF_DOC_POID"));
                cheques.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping cheque details", e);
        }
        return cheques;
    }

    private LocalDate convertToLocalDate(Date date) {
        return date != null ? date.toLocalDate() : null;
    }

    private Long getUserPoid() {
        return UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;
    }

}

