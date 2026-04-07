package com.asg.finance.repository;

import com.asg.common.lib.utility.DateUtil;
import com.asg.finance.dto.JournalVoucherAssetDetailDto;

import com.asg.common.lib.exception.AsgException;
import com.asg.finance.dto.JournalVoucherCapitalizationDto;
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
import java.util.ArrayList;
import java.util.List;

import static com.asg.common.lib.security.util.UserContext.getUserPoid;
import static com.asg.common.lib.utility.ASGHelperUtils.getCompanyId;
import static com.asg.common.lib.utility.ASGHelperUtils.getGroupId;


@Slf4j
@Repository
public class JournalVoucherProcRepositoryImpl implements JournalVoucherProcRepository {

    private static final String PROC_FA_DEPRE_DTL = "PROC_FA_DEPRE_DTL_FOR_DIS_JV";
    private static final String PROC_FA_DEFAULT_DTLS = "PROC_AP_PI_FA_DEFAULT_DTLS";
    private static final String PROC_UPDATE_ASSET_DTL = "PROC_JV_UPDATE_ASSET_DTL";

    private static final String P_LOGIN_GROUP_POID = "P_LOGIN_GROUP_POID";
    private static final String P_LOGIN_COMPANY_POID = "P_LOGIN_COMPANY_POID";
    private static final String P_LOGIN_USER_POID = "P_LOGIN_USER_POID";
    private static final String P_FA_POID = "P_FA_POID";
    private static final String OUTDATA = "OUTDATA";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<JournalVoucherAssetDetailDto> fetchAssetDepreciationDetails(Long faPoid) {

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery(PROC_FA_DEPRE_DTL);

        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DEP_YEAR", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_FA_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(OUTDATA, void.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_COMPANY_POID", getCompanyId());
        LocalDate currentDate = DateUtil.getCurrentDateInUserTimeZone();

        query.setParameter("P_DEP_YEAR", java.sql.Date.valueOf(currentDate));
        query.setParameter(P_FA_POID, faPoid);

        query.execute();

        try (ResultSet rs = (ResultSet) query.getOutputParameterValue(OUTDATA)) {
            log.info("Result Set: {}", rs);
            List<JournalVoucherAssetDetailDto> resultList = new ArrayList<>();

            while (rs != null && rs.next()) {
                JournalVoucherAssetDetailDto dto = JournalVoucherAssetDetailDto.builder()
                        .faPoid(faPoid)
                        .lifeYear(rs.getInt("LIFE_YEAR"))
                        .purchaseDate(rs.getDate("PURCHASE_DATE") != null ? rs.getDate("PURCHASE_DATE").toLocalDate() : null)
                        .depreciationStartDate(rs.getDate("DEPRECIATION_START_DATE") != null ? rs.getDate("DEPRECIATION_START_DATE").toLocalDate() : null)
                        .assetValue(rs.getBigDecimal("GROSS_BLOCK_START"))
                        .depreciatedAmt(rs.getBigDecimal("ACCUM_DEPRICIATION"))
                        .wdvValue(rs.getBigDecimal("WDV_VALUE"))
                        .faDescription(rs.getString("DESCRIPTION"))
                        .faCategory(rs.getLong("FA_CATEGORY_POID"))
                        .assetType(rs.getString("ASSET_TYPE"))
                        .scrapSoldDate(LocalDate.now())
                        .build();
                resultList.add(dto);
            }
            return resultList;
        } catch (Exception e) {
            throw new AsgException("Error fetching asset depreciation details: " + e.getMessage(), e);
        }
    }

    @Override
    public List<JournalVoucherCapitalizationDto> fetchFixedAssetDetails(Long faPoid) {

        try {
            StoredProcedureQuery query = entityManager
                    .createStoredProcedureQuery(PROC_FA_DEFAULT_DTLS);

            query.registerStoredProcedureParameter(P_LOGIN_GROUP_POID, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(P_LOGIN_COMPANY_POID, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(P_LOGIN_USER_POID, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(P_FA_POID, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(OUTDATA, void.class, ParameterMode.REF_CURSOR);

            query.setParameter(P_LOGIN_GROUP_POID, getGroupId());
            query.setParameter(P_LOGIN_COMPANY_POID, getCompanyId());
            query.setParameter(P_LOGIN_USER_POID, getUserPoid());
            query.setParameter(P_FA_POID, faPoid);

            query.execute();

            List<JournalVoucherCapitalizationDto> list = new ArrayList<>();

            try (ResultSet rs = (ResultSet) query.getOutputParameterValue(OUTDATA)) {
                while (rs != null && rs.next()) {
                    list.add(JournalVoucherCapitalizationDto.builder()
                            .faPoid(faPoid)
                            .faDescription(rs.getString("FA_DESCRIPTION"))
                            .faCategory(rs.getLong("FA_CATEGORY_POID"))
                            .assetType(rs.getString("ASSET_TYPE"))
                            .assetValue(rs.getBigDecimal("GROSS_VALUE"))
                            .build());
                }
            }

            return list;
        } catch (SQLException e) {
            throw new AsgException("Error fetching fixed asset details: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateAssetDetail(Long transactionPoid) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery(PROC_UPDATE_ASSET_DTL);

        query.registerStoredProcedureParameter(P_LOGIN_GROUP_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_COMPANY_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(P_LOGIN_USER_POID, Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

        query.setParameter(P_LOGIN_GROUP_POID, getGroupId());
        query.setParameter(P_LOGIN_COMPANY_POID, getCompanyId());
        query.setParameter(P_LOGIN_USER_POID, getUserPoid());
        query.setParameter("P_TRANSACTION_POID", transactionPoid);

        query.execute();

        String result = (String) query.getOutputParameterValue("P_RESULT");
        if (result != null && result.contains("ERROR")) {
            throw new AsgException(result);
        }
    }
}