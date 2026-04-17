package com.asg.finance.repository;

import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.dto.TelexFileDtlDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TelexFileGenerateProcRepositoryImpl implements TelexFileGenerateProcRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final LoggingService loggingService;
    private final DataSource dataSource;

    @Override
    public List<TelexFileDtlDto> loadTelexTransferData(String bankList) {
        String sql = """
            SELECT TRANSACTION_POID, TO_DATE(TRANSACTION_DATE), COMPANY_POID, DOC_REF,
                   SUBSTR(NVL(PAYING_TO_NAME, GET_GL_NAME(PAY_GL_POID)), 1, 50) AS PAYING_TO_NAME,
                   PAYING_TYPE, SUBSTR(LONG_NARRATION, 1, 50) AS LONG_NARRATION,
                   TT_DATE, CURRENCY_CODE, CURRENCY_RATE, CURRENCY_AMT, AMOUNT,
                   'N' AS DELETED,
                   'N' AS SELECTED,
                   'TARGET_DOC_ID=400-111,DOC_KEY_POID=' || TRANSACTION_POID AS DRILLDOWN_LINK_INFO,
                   TT_CHARGE_TYPE
              FROM GL_BANK_DEBIT_HDR
             WHERE PAYING_TYPE = '1'
               AND NVL(BANK_POID, 0) IN (SELECT NVL(BANK_POID, 1)
                                           FROM GL_BANK_MASTER
                                          WHERE NVL(ONLINE_FILE_TT, 'N') = ?)
               AND FILE_GENERATED = 'N'
               AND NVL(DELETED, 'N') = 'N'
               AND NVL(FILE_GENERATED, 'N') = 'N'
               AND ('400-111', TRANSACTION_POID) IN
                   (SELECT APS.DOC_ID, APS.DOC_KEY_POID
                      FROM (SELECT ROW_NUMBER() OVER (PARTITION BY DOC_ID, DOC_KEY_POID ORDER BY APPROVAL_POID DESC) ROW_NUM, APS.*
                              FROM GLOBAL_APPROVAL_STATUS APS) APS
                     INNER JOIN GLOBAL_USERS_AUTH_ROLES_DTL URD ON APS.USER_ROLE_POID = URD.USER_ROLE_POID
                     INNER JOIN GLOBAL_USERS UM ON URD.USER_POID = UM.USER_POID
                     WHERE ROW_NUM = 1
                       AND (ACTION_STATUS = 'SUBMIT_FOR_APPROVAL' OR ACTION_STATUS = 'SUBMIT_FOR_SPECIAL_APPROVAL'))
             ORDER BY CURRENCY_CODE, TT_DATE, AMOUNT
            """;

        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter(1, bankList)
                .getResultList();

        List<TelexFileDtlDto> dtoList = new ArrayList<>();
        long rowId = 1;
        for (Object[] row : results) {
            TelexFileDtlDto dto = TelexFileDtlDto.builder()
                    .detRowId(rowId++)
                    .debitTransactionPoid(((BigDecimal) row[0]).longValue())
                    .debitTransactionDate(row[1] != null ? ((java.sql.Timestamp) row[1]).toLocalDateTime().toLocalDate() : null)
                    .debitCompanyPoid(row[2] != null ? ((BigDecimal) row[2]).longValue() : null)
                    .debitDocRef((String) row[3])
                    .debitPayingToName((String) row[4])
                    .debitPayingType(row[5] != null ? row[5].toString() : null)
                    .debitLongNarration((String) row[6])
                    .debitTtDate(row[7] != null ? ((java.sql.Timestamp) row[7]).toLocalDateTime().toLocalDate() : null)
                    .debitCurrencyCode((String) row[8])
                    .debitCurrencyRate(row[9] != null ? (BigDecimal) row[9] : null)
                    .debitCurrencyAmt(row[10] != null ? (BigDecimal) row[10] : null)
                    .debitAmount(row[11] != null ? (BigDecimal) row[11] : null)
                    .deleted(row[12] != null ? row[12].toString() : null)
                    .selected(row[13] != null ? row[13].toString() : null)
                    .drilldownLinkInfo((String) row[14])
                    .debitTtChargeType(row[15] != null ? row[15].toString() : null)
                    .build();
            dtoList.add(dto);
        }
        return dtoList;
    }

    @Override
    public String regenerateTelexFile(Long groupPoid, Long companyPoid, Long userPoid, Long docKeyPoid) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_BANK_DEBIT_FILE_REGENT_V2");
            query.registerStoredProcedureParameter(1, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(3, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(4, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(5, String.class, ParameterMode.OUT);

            query.setParameter(1, new BigDecimal(groupPoid));
            query.setParameter(2, new BigDecimal(companyPoid));
            query.setParameter(3, new BigDecimal(userPoid));
            query.setParameter(4, new BigDecimal(docKeyPoid));

            query.execute();
            String result = (String) query.getOutputParameterValue(5);

            if (result.isEmpty() || result.contains("ERROR")) {
                return result;
            }

            loggingService.createLogSummaryEntry(UserContext.getDocumentId(), docKeyPoid.toString(), "SUCCESS : Bank telex file removed for recreation.");
            return "SUCCESS: Bank telex file removed";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public String createBankFileBatchNbb(Long transactionPoid, Long userPoid, Long companyPoid) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_BANK_FILE_CREATE_BCH_NBB");
            query.registerStoredProcedureParameter(1, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(3, String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter(4, BigDecimal.class, ParameterMode.IN);

            query.setParameter(1, new BigDecimal(transactionPoid));
            query.setParameter(2, new BigDecimal(userPoid));
            query.setParameter(4, new BigDecimal(companyPoid));

            query.execute();
            return (String) query.getOutputParameterValue(3);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public String checkOverdraft(Long transactionPoid) {
        return callProcBankFileCheckOdV2(transactionPoid);
    }

    private String callProcBankFileCheckOdV2(Long transactionPoid) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_BANK_FILE_CHECK_OD_V2");
            query.registerStoredProcedureParameter(1, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, String.class, ParameterMode.OUT);

            query.setParameter(1, new BigDecimal(transactionPoid));
            query.execute();
            return (String) query.getOutputParameterValue(2);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String linkBankApproval(Long param1, Long companyPoid, Long userPoid, String docId, String param5, String docKey) {
        return callProcDynrptLinkBankApproval(param1, companyPoid, userPoid, docId, param5, docKey);
    }
    
    private String callProcDynrptLinkBankApproval(Long loginGroupPoid, Long loginCompanyPoid, Long loginUserPoid, 
                                                   String rptDocId, String rptFiltersList, String rptSelectedValuesList) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_DYNRPT_LINK_BANKAPPROVAL");
            query.registerStoredProcedureParameter(1, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(3, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(7, String.class, ParameterMode.OUT);

            query.setParameter(1, new BigDecimal(loginGroupPoid));
            query.setParameter(2, new BigDecimal(loginCompanyPoid));
            query.setParameter(3, new BigDecimal(loginUserPoid));
            query.setParameter(4, rptDocId);
            query.setParameter(5, rptFiltersList);
            query.setParameter(6, rptSelectedValuesList);

            query.execute();
            return (String) query.getOutputParameterValue(7);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getPayingTo(String docRef) {
        try {
            String sql = "SELECT DISTINCT PAYING_TO FROM GL_BANK_DEBIT_HDR WHERE DOC_REF = ? AND PAYING_TO LIKE 'EMP-%'";
            List<String> results = entityManager.createNativeQuery(sql)
                    .setParameter(1, docRef)
                    .getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean checkEmployeeIban(String docRef) {
        try {
            String sql = """
                SELECT COUNT(*) FROM GL_BANK_DEBIT_HDR glbkhdr
                INNER JOIN TEST_PAYROLL.HR_EMPLOYEE_MASTER PRNHDR ON REPLACE(glbkhdr.PAYING_TO, 'EMP-') = employee_code
                INNER JOIN TEST_PAYROLL.HR_EMPLOYEE_SALARY_MASTER EMPSLRY ON EMPSLRY.EMPLOYEE_POID = PRNHDR.EMPLOYEE_POID
                INNER JOIN GLOBAL_CUSTOMER_BANK_MASTER GBLCUST ON GBLCUST.BANK_POID = EMPSLRY.BANK_POID
                WHERE IBAN_ACCOUNT_NO IS NOT NULL AND EMPSLRY.PAYMENT_METHOD = 'BANK'
                AND DOC_REF = ? AND glbkhdr.PAYING_TO LIKE 'EMP-%'
                """;
            BigDecimal count = (BigDecimal) entityManager.createNativeQuery(sql)
                    .setParameter(1, docRef)
                    .getSingleResult();
            return count.intValue() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public java.util.Map<String, String> getBeneficiaryDetails(Long debitTransactionPoid, String docRef) {
        try {
            String sql = """
                SELECT BENEFICIARY_ID, INTERMEDIARY_COUNTRY_POID, BENEFICIARY_COUNTRY,
                       INTERMEDIARY_ACCT, INTERMEDIARY_BANK
                FROM (
                    SELECT BENEFICIARY_ID, INTERMEDIARY_COUNTRY_POID, BENEFICIARY_COUNTRY,
                           INTERMEDIARY_ACCT, INTERMEDIARY_BANK
                    FROM SHIP_PRINCIPAL_MASTER_PYMT_DTL
                    UNION ALL
                    SELECT BENEFICIARY_ID, INTERMEDIARY_COUNTRY_POID, BENEFICIARY_COUNTRY,
                           INTERMEDIARY_ACCT, INTERMEDIARY_BANK
                    FROM AP_SUPPLIER_MASTER_PYMT_DTL
                    UNION ALL
                    SELECT BENEFICIARY_ID, INTERMEDIARY_COUNTRY_POID, BENEFICIARY_COUNTRY,
                           INTERMEDIARY_ACCT, INTERMEDIARY_BANK
                    FROM gl_master_pymt_dtl
                )
                WHERE BENEFICIARY_ID IN (
                    SELECT PAYING_TO FROM GL_BANK_DEBIT_HDR
                     WHERE TRANSACTION_POID = ?
                       AND DOC_REF = ?
                )
                """;
            List<Object[]> results = entityManager.createNativeQuery(sql)
                    .setParameter(1, new BigDecimal(debitTransactionPoid))
                    .setParameter(2, docRef)
                    .getResultList();
            
            if (!results.isEmpty()) {
                Object[] row = results.get(0);
                java.util.Map<String, String> map = new java.util.HashMap<>();
                map.put("BENEFICIARY_ID", row[0] != null ? row[0].toString() : null);
                map.put("INTERMEDIARY_COUNTRY_POID", row[1] != null ? row[1].toString() : null);
                map.put("BENEFICIARY_COUNTRY", row[2] != null ? row[2].toString() : null);
                map.put("INTERMEDIARY_ACCT", row[3] != null ? row[3].toString() : null);
                map.put("INTERMEDIARY_BANK", row[4] != null ? row[4].toString() : null);
                return map;
            }
            return new java.util.HashMap<>();
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    @Override
    public String getTtChargeType(Long debitTransactionPoid) {
        try {
            String sql = "SELECT TT_CHARGE_TYPE FROM GL_BANK_DEBIT_HDR WHERE TRANSACTION_POID = ?";
            return (String) entityManager.createNativeQuery(sql)
                    .setParameter(1, new BigDecimal(debitTransactionPoid))
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public java.util.Map<String, String> getCompanyDetails(Long companyPoid) {
        try {
            String sql = "SELECT COMPANY_NAME, ADDRESS FROM GLOBAL_COMPANY_MASTER WHERE COMPANY_POID = ?";
            Object[] result = (Object[]) entityManager.createNativeQuery(sql)
                    .setParameter(1, new BigDecimal(companyPoid))
                    .getSingleResult();
            
            java.util.Map<String, String> map = new java.util.HashMap<>();
            map.put("COMPANY_NAME", result[0] != null ? result[0].toString() : null);
            map.put("ADDRESS", result[1] != null ? result[1].toString() : null);
            return map;
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    @Override
    public String getCountryCode(Long debitTransactionPoid, String docRef) {
        try {
            String sql = """
                SELECT GET_COUNTRY_CODE(BENEFICIARY_COUNTRY) FROM (
                    SELECT BENEFICIARY_COUNTRY FROM SHIP_PRINCIPAL_MASTER_PYMT_DTL
                    WHERE BENEFICIARY_ID IN (SELECT PAYING_TO FROM GL_BANK_DEBIT_HDR WHERE TRANSACTION_POID = ? AND DOC_REF = ?)
                    UNION ALL
                    SELECT BENEFICIARY_COUNTRY FROM AP_SUPPLIER_MASTER_PYMT_DTL
                    WHERE BENEFICIARY_ID IN (SELECT PAYING_TO FROM GL_BANK_DEBIT_HDR WHERE TRANSACTION_POID = ? AND DOC_REF = ?)
                    UNION ALL
                    SELECT BENEFICIARY_COUNTRY FROM gl_master_pymt_dtl
                    WHERE BENEFICIARY_ID IN (SELECT PAYING_TO FROM GL_BANK_DEBIT_HDR WHERE TRANSACTION_POID = ? AND DOC_REF = ?)
                ) WHERE ROWNUM = 1
                """;
            return (String) entityManager.createNativeQuery(sql)
                    .setParameter(1, new BigDecimal(debitTransactionPoid))
                    .setParameter(2, docRef)
                    .setParameter(3, new BigDecimal(debitTransactionPoid))
                    .setParameter(4, docRef)
                    .setParameter(5, new BigDecimal(debitTransactionPoid))
                    .setParameter(6, docRef)
                    .getSingleResult();
        } catch (Exception e) {
            return "BH";
        }
    }

    @Override
    public void createBankFilePayment(Long companyPoid, Long transactionPoid, Long userPoid, String companyName,
                                     String address, String country, String docRef, String chargeType,
                                     String countryCode, Long mainTransactionPoid, int seqNo) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_BANK_FILE_CREATE_PAYMENT");
            query.registerStoredProcedureParameter(1, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(3, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(7, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(8, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(9, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(10, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(11, BigDecimal.class, ParameterMode.IN);

            query.setParameter(1, new BigDecimal(companyPoid));
            query.setParameter(2, new BigDecimal(transactionPoid));
            query.setParameter(3, new BigDecimal(userPoid));
            query.setParameter(4, companyName);
            query.setParameter(5, address);
            query.setParameter(6, country);
            query.setParameter(7, docRef);
            query.setParameter(8, chargeType);
            query.setParameter(9, countryCode);
            query.setParameter(10, new BigDecimal(mainTransactionPoid));
            query.setParameter(11, new BigDecimal(seqNo));

            query.execute();
        } catch (Exception e) {
            // Ignore errors
        }
    }

    @Override
    public void generateHsbcApiXml(Long companyPoid, Long transactionPoid, Long userPoid, String companyName,
                                  String address, String country, String docRef, String chargeType,
                                  String countryCode, Long mainTransactionPoid, int seqNo, Long detRowId) {
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall(
                     "{call PROC_HSBC_API_GENERATE_XML_V2(?,?,?,?,?,?,?,?,?,?,?,?)}")) {
            stmt.setBigDecimal(1, new BigDecimal(companyPoid));
            stmt.setBigDecimal(2, new BigDecimal(transactionPoid));
            stmt.setBigDecimal(3, new BigDecimal(userPoid));
            stmt.setString(4, companyName);
            stmt.setString(5, address);
            stmt.setString(6, country);
            stmt.setString(7, docRef);
            stmt.setString(8, chargeType);
            stmt.setString(9, countryCode);
            stmt.setBigDecimal(10, new BigDecimal(mainTransactionPoid));
            stmt.setBigDecimal(11, new BigDecimal(seqNo));
            stmt.setBigDecimal(12, new BigDecimal(detRowId));
            stmt.execute();
        } catch (Exception e) {
            log.warn("Error generating HSBC API XML for docRef={}: {}", docRef, e.getMessage());
        }
    }

    @Override
    public void createBankFilePaymentAub(Long companyPoid, Long transactionPoid, Long userPoid, String companyName,
                                        String address, String country, String docRef, String chargeType,
                                        String countryCode, Long mainTransactionPoid, int seqNo) {
        callProcBankFileCreatePmtAub(companyPoid, transactionPoid, userPoid, companyName, address, 
                                     country, docRef, chargeType, countryCode, mainTransactionPoid, seqNo);
    }
    
    private void callProcBankFileCreatePmtAub(Long companyPoid, Long trnNo, Long loginUserPoid, 
                                              String orderPartyDr, String orderPartyDrAd1, String orderPartyDrAd2,
                                              String docRef, String detailsCharges, String countryCode, 
                                              Long mainPoid, int sentSeqNo) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_BANK_FILE_CREATE_PMT_AUB");
            query.registerStoredProcedureParameter(1, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(3, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(7, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(8, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(9, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(10, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(11, BigDecimal.class, ParameterMode.IN);

            query.setParameter(1, new BigDecimal(companyPoid));
            query.setParameter(2, new BigDecimal(trnNo));
            query.setParameter(3, new BigDecimal(loginUserPoid));
            query.setParameter(4, orderPartyDr);
            query.setParameter(5, orderPartyDrAd1);
            query.setParameter(6, orderPartyDrAd2);
            query.setParameter(7, docRef);
            query.setParameter(8, detailsCharges);
            query.setParameter(9, countryCode);
            query.setParameter(10, new BigDecimal(mainPoid));
            query.setParameter(11, new BigDecimal(sentSeqNo));

            query.execute();
        } catch (Exception e) {
            // Ignore errors as per original procedure
        }
    }
    @Override
    public String getCompanyCode(Long companyPoid) {
        try {
            String sql = "SELECT GET_COMPANY_CODE(?) FROM DUAL";
            return (String) entityManager.createNativeQuery(sql)
                    .setParameter(1, new BigDecimal(companyPoid))
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
}
