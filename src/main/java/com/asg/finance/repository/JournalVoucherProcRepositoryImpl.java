package com.asg.finance.repository;

import com.asg.finance.dto.JournalVoucherAssetDetailDto;
import com.asg.finance.dto.JournalVoucherCapitalizationDto;
import com.asg.finance.entity.GlJournalVoucherHdr;
import com.asg.finance.exception.DataAccessException;
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

import static com.asg.common.lib.security.util.UserContext.getUserPoid;
import static com.asg.common.lib.utility.ASGHelperUtils.getCompanyId;
import static com.asg.common.lib.utility.ASGHelperUtils.getGroupId;


@Slf4j
@Repository
public class JournalVoucherProcRepositoryImpl implements JournalVoucherProcRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public String postJournalVoucher(Long transactionPoid,GlJournalVoucherHdr glJournalVoucherHdr,String documentId) {

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GL_LEDGER_POSTING_JV");

        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_REF", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);

        query.setParameter("P_LOGIN_GROUP_POID", glJournalVoucherHdr.getGroupPoid());
        query.setParameter("P_LOGIN_COMPANY_POID", glJournalVoucherHdr.getCompanyPoid());
        query.setParameter("P_LOGIN_USER_POID", getUserPoid());
        query.setParameter("P_DOC_ID", documentId);
        query.setParameter("P_TRANSACTION_POID", transactionPoid);
        query.setParameter("P_DOC_REF", glJournalVoucherHdr.getDocRef());

        query.execute();

        String result = (String) query.getOutputParameterValue("P_STATUS");
        log.info("Result: {}", result);

        if (result != null && result.contains("ERROR")) {
            handlePostingError(result);
        }

        return result;
    }


    @Override
    public List<JournalVoucherAssetDetailDto> fetchAssetDepreciationDetails(Long faPoid) {


        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_FA_DEPRE_DTL_FOR_DIS_JV");

        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DEP_YEAR", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_FA_POID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_COMPANY_POID", getCompanyId());
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);

        query.setParameter(
                "P_DEP_YEAR",
                java.sql.Date.valueOf(currentMonth)
        );

        query.setParameter("P_FA_POID", faPoid.toString());

        query.execute();

        ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
        log.info("Result Set: {}", rs);
        List<JournalVoucherAssetDetailDto> resultList = new ArrayList<>();

        try {
            while (rs != null && rs.next()) {
                JournalVoucherAssetDetailDto dto = JournalVoucherAssetDetailDto.builder()
                        .faPoid(faPoid)
                        .lifeYear(rs.getInt("LIFE_YEAR"))
                        .purchaseDate(rs.getDate("PURCHASE_DATE") != null ? rs.getDate("PURCHASE_DATE").toLocalDate() : null)
                        .depreciationStartDate(rs.getDate("DEPRECIATION_START_DATE") != null ? rs.getDate("DEPRECIATION_START_DATE").toLocalDate() : null)
                        .assetValue(rs.getBigDecimal("GROSS_BLOCK_START"))
                        .depreciatedAmt(rs.getBigDecimal("ACCUM_DEPRICIATION"))
                        .wdvValue(rs.getBigDecimal("WDV_VALUE"))
                        .build();
                resultList.add(dto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching asset depreciation details: " + e.getMessage(), e);
        }

        return resultList;
    }

    @Override
    public List<JournalVoucherCapitalizationDto> fetchFixedAssetDetails(Long faPoid) throws SQLException {

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_AP_PI_FA_DEFAULT_DTLS");

        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_FA_POID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_LOGIN_GROUP_POID", getGroupId());
        query.setParameter("P_LOGIN_COMPANY_POID", getCompanyId());
        query.setParameter("P_LOGIN_USER_POID", getUserPoid());
        query.setParameter("P_FA_POID", faPoid.toString());

        query.execute();

        ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
        List<JournalVoucherCapitalizationDto> list = new ArrayList<>();

        while (rs != null && rs.next()) {
            list.add(JournalVoucherCapitalizationDto.builder()
                    .faPoid(faPoid)
                    .faDescription(rs.getString("FA_DESCRIPTION"))
                    .faCategory(rs.getLong("FA_CATEGORY_POID"))
                    .assetType(rs.getString("ASSET_TYPE"))
                    .assetValue(rs.getBigDecimal("GROSS_VALUE"))
                    .build()
            );
        }

        return list;
    }

    private void handlePostingError(String errorMessage) {
        if (errorMessage.contains("ORA-20001")) {
            throw new DataAccessException("Changes allowed only within current Financial Period");
        }
        if (errorMessage.contains("ORA-20002")) {
            throw new DataAccessException("Changes allowed only within current Transaction Period");
        }
        throw new DataAccessException(errorMessage);
    }

    @Override
    public void updateAssetDetail(Long transactionPoid) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_JV_UPDATE_ASSET_DTL");

        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_TRANSACTION_POID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

        query.setParameter("P_LOGIN_GROUP_POID", getGroupId());
        query.setParameter("P_LOGIN_COMPANY_POID", getCompanyId());
        query.setParameter("P_LOGIN_USER_POID", getUserPoid());
        query.setParameter("P_TRANSACTION_POID", transactionPoid.toString());

        query.execute();

        String result = (String) query.getOutputParameterValue("P_RESULT");
        if (result != null && result.contains("ERROR")) {
            throw new RuntimeException(result);
        }
    }
}