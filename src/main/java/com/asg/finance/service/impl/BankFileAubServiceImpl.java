package com.asg.finance.service.impl;

import com.asg.common.lib.client.ParameterServiceClient;
import com.asg.finance.dto.BankFileDetailProjection;
import com.asg.finance.repository.TelexFileGenerateProcRepository;
import com.asg.finance.service.BankFileAubService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankFileAubServiceImpl implements BankFileAubService {

    private final TelexFileGenerateProcRepository procRepository;
    private final ParameterServiceClient parameterServiceClient;
    private final JdbcTemplate jdbcTemplate;
    
    @Value("${bank.file.directory:AUB_FTP_FILE_LOAD}")
    private String aubFileDirectoryParameterName;

    @Override
    @Transactional
    public String createBankFileBatchAub(Long transactionPoid, Long userPoid, Long companyPoid) {
        try {
            String ttSuppressBalanceCheck = jdbcTemplate.queryForObject(
                "SELECT NVL(TT_SUPPRESS_BALANCE_CHECK, 'N') FROM GL_BANK_FILE_HDR WHERE TRANSACTION_POID = ? AND NVL(DELETED, 'N') = 'N'",
                String.class, transactionPoid);

            if ("N".equals(ttSuppressBalanceCheck)) {
                String odStatus = procRepository.checkOverdraft(transactionPoid);
                if (odStatus != null && odStatus.toUpperCase().contains("WARNING : AMOUNT IS GREATER THAN OUR")) {
                    return odStatus;
                }
            }

            List<BankFileDetailProjection> details = fetchBankFileDetails(transactionPoid, companyPoid);
            List<String> statusMessages = new ArrayList<>();
            int seqNo = 0;
            String onlyApproval = "N";

            for (BankFileDetailProjection detail : details) {
                onlyApproval = detail.getOnlyApproval() != null ? detail.getOnlyApproval() : "N";

                if ("Y".equals(onlyApproval)) {
                    String approvalStatus = procRepository.linkBankApproval(
                            1L, detail.getDebitCompanyPoid(), userPoid, "400-111", "xx",
                            "400-111-" + detail.getDebitTransactionPoid());
                    statusMessages.add(detail.getDebitDocRef() + "," + (approvalStatus != null ? approvalStatus.substring(0, Math.min(100, approvalStatus.length())) : ""));
                } else {
                    String validationError = validatePaymentDetails(detail, transactionPoid);
                    if (validationError != null) {
                        statusMessages.add(validationError + ",FILE NOT GENERATED FOR " + detail.getDebitDocRef());
                        continue;
                    }

                    String paymentStatus = processAubPayment(detail, transactionPoid, userPoid, ++seqNo);
                    if (paymentStatus != null) {
                        statusMessages.add(paymentStatus);
                    }
                }
            }

            Map<String, Object> stats = getFileStats(transactionPoid, companyPoid);
            int s2Count = countS2Records(transactionPoid, companyPoid);
            deleteS3Records(transactionPoid, companyPoid);
            
            int detRowId = ((Number) stats.get("MAX_DET_ROW_ID")).intValue();
            int mainDetRowId = ((Number) stats.get("MAX_MAIN_DET_ROW_ID")).intValue();
            
            insertS3Trailer(transactionPoid, detRowId + 1, s2Count, userPoid, mainDetRowId + 1, companyPoid);
            
            jdbcTemplate.update("DELETE FROM GL_BANK_FILE_DTL WHERE TRANSACTION_POID = ? AND DELETED = 'N'", transactionPoid);

            int advRecordCount = countAdvRecords(transactionPoid, companyPoid);
            updateAdvRecordCount(transactionPoid, companyPoid, advRecordCount);

            if (!"Y".equals(onlyApproval)) {
                generateAndSendAubFiles(transactionPoid, userPoid, companyPoid);
            }

            return String.join(",", statusMessages) + "SUCCESS: FILE SENT BY MAIL";

        } catch (Exception e) {
            log.error("Error in createBankFileBatchAub", e);
            return "ERROR : " + e.getMessage().substring(0, Math.min(200, e.getMessage().length()));
        }
    }

    private List<BankFileDetailProjection> fetchBankFileDetails(Long transactionPoid, Long companyPoid) {
        String sql = "SELECT DEBIT_COMPANY_POID, DEBIT_TRANSACTION_POID, DEBIT_DOC_REF, DEBIT_CURRENCY_CODE, " +
                     "NVL(ONLY_APPROVAL, 'N') ONLY_APPROVAL, TT_SUPPRESS_BALANCE_CHECK, GLDTL.DET_ROW_ID " +
                     "FROM GL_BANK_FILE_HDR GLHDR " +
                     "INNER JOIN GL_BANK_FILE_DTL GLDTL ON GLHDR.TRANSACTION_POID = GLDTL.TRANSACTION_POID " +
                     "WHERE GLDTL.TRANSACTION_POID = ? AND GLDTL.DELETED = 'Y' " +
                     "AND NVL(GLHDR.DELETED, 'N') = 'N' AND NVL(DEBIT_TRANSACTION_POID, 0) <> 0 " +
                     "AND DEBIT_COMPANY_POID = ?";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> new BankFileDetailProjection(
            rs.getLong("DEBIT_COMPANY_POID"),
            rs.getLong("DEBIT_TRANSACTION_POID"),
            rs.getString("DEBIT_DOC_REF"),
            rs.getString("DEBIT_CURRENCY_CODE"),
            rs.getString("ONLY_APPROVAL"),
            rs.getString("TT_SUPPRESS_BALANCE_CHECK"),
            rs.getLong("DET_ROW_ID")
        ), transactionPoid, companyPoid);
    }

    private String validatePaymentDetails(BankFileDetailProjection detail, Long transactionPoid) {
        String payingTo = procRepository.getPayingTo(detail.getDebitDocRef());
        if (payingTo != null && payingTo.startsWith("EMP-")) {
            if (!procRepository.checkEmployeeIban(detail.getDebitDocRef())) {
                updateDetailDeleted(transactionPoid, detail.getDebitDocRef(), "N");
                return "ERROR: Employee Record Check IBAN/Others details ...." + detail.getDebitDocRef();
            }
        }

        if (!"BHD".equals(detail.getDebitCurrencyCode())) {
            Map<String, String> beneficiaryDetails = procRepository.getBeneficiaryDetails(
                    detail.getDebitTransactionPoid(), detail.getDebitDocRef());

            String ttChargeType = procRepository.getTtChargeType(detail.getDebitTransactionPoid());
            if (ttChargeType == null) {
                updateDetailDeleted(transactionPoid, detail.getDebitDocRef(), "N");
                return "ERROR: TT CHARGE NOT ENTERED (OUR/SHARE) ...." + detail.getDebitDocRef();
            }

            String intermediaryBank = beneficiaryDetails.get("INTERMEDIARY_BANK");
            String intermediaryCountry = beneficiaryDetails.get("INTERMEDIARY_COUNTRY_POID");
            if (intermediaryBank != null && intermediaryCountry == null) {
                updateDetailDeleted(transactionPoid, detail.getDebitDocRef(), "N");
                return "ERROR: INTERMEDIARY BANK COUNTRY NOT MAP...." + detail.getDebitDocRef();
            }

            String beneficiaryCountry = beneficiaryDetails.get("BENEFICIARY_COUNTRY");
            if (beneficiaryCountry == null) {
                updateDetailDeleted(transactionPoid, detail.getDebitDocRef(), "N");
                return "ERROR: BENEFICIARY COUNTRY NOT MAP...." + detail.getDebitDocRef();
            }
        }

        return null;
    }

    private String processAubPayment(BankFileDetailProjection detail, Long transactionPoid, Long userPoid, int seqNo) {
        String approvalStatus = procRepository.linkBankApproval(
                1L, detail.getDebitCompanyPoid(), userPoid, "400-111",
                "SUPPRESS_BALANCE_CHECK=" + detail.getTtSuppressBalanceCheck(),
                "400-111-" + detail.getDebitTransactionPoid());

        if (approvalStatus != null && approvalStatus.toUpperCase().contains("WARNING : AMOUNT IS GREATER THAN OUR")
                && "N".equals(detail.getTtSuppressBalanceCheck())) {
            updateDetailDeleted(transactionPoid, detail.getDebitDocRef(), "N");
            return approvalStatus + ",FILE NOT GENERATED FOR " + detail.getDebitDocRef();
        }

        Map<String, String> companyDetails = procRepository.getCompanyDetails(detail.getDebitCompanyPoid());
        String countryCode = "BHD".equals(detail.getDebitCurrencyCode())
                ? "BH"
                : procRepository.getCountryCode(detail.getDebitTransactionPoid(), detail.getDebitDocRef());

        procRepository.createBankFilePaymentAub(
                detail.getDebitCompanyPoid(), detail.getDebitTransactionPoid(), userPoid,
                companyDetails.get("COMPANY_NAME"), companyDetails.get("ADDRESS"), "BAHRAIN",
                detail.getDebitDocRef(), "OUR", countryCode, transactionPoid, seqNo);

        return null;
    }

    private void updateDetailDeleted(Long transactionPoid, String docRef, String deleted) {
        jdbcTemplate.update(
            "UPDATE GL_BANK_FILE_DTL SET DELETED = ? WHERE TRANSACTION_POID = ? AND DELETED = 'Y' AND DEBIT_DOC_REF = ?",
            deleted, transactionPoid, docRef);
    }

    private Map<String, Object> getFileStats(Long transactionPoid, Long companyPoid) {
        return jdbcTemplate.queryForMap(
            "SELECT COUNT(*) as TOTAL_COUNT, NVL(MAX(DET_ROW_ID), 0) as MAX_DET_ROW_ID, NVL(MAX(MAIN_DET_ROW_ID), 0) as MAX_MAIN_DET_ROW_ID " +
            "FROM GL_BANK_FILE_GENERATE WHERE MAIN_TRANSACTION_POID = ? AND FILE_COMPANY_POID = ?",
            transactionPoid, companyPoid);
    }

    private int countS2Records(Long transactionPoid, Long companyPoid) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM GL_BANK_FILE_GENERATE WHERE MAIN_TRANSACTION_POID = ? AND FILE_COMPANY_POID = ? AND TXT_FILE LIKE 'S2%'",
            Integer.class, transactionPoid, companyPoid);
    }

    private void deleteS3Records(Long transactionPoid, Long companyPoid) {
        jdbcTemplate.update(
            "DELETE FROM GL_BANK_FILE_GENERATE WHERE MAIN_TRANSACTION_POID = ? AND FILE_COMPANY_POID = ? AND TXT_FILE LIKE 'S3%'",
            transactionPoid, companyPoid);
    }

    private void insertS3Trailer(Long transactionPoid, int detRowId, int recordCount, Long userPoid, int mainDetRowId, Long companyPoid) {
        jdbcTemplate.update(
            "INSERT INTO GL_BANK_FILE_GENERATE (TRANSACTION_POID, DET_ROW_ID, TXT_FILE, IFILE_TYPE, CREATED_BY, CREATED_DATE, MAIN_TRANSACTION_POID, MAIN_DET_ROW_ID, FILE_COMPANY_POID) " +
            "VALUES (?, ?, ?, 'ACH', ?, SYSDATE, ?, ?, ?)",
            transactionPoid, detRowId, "S3," + recordCount, userPoid, transactionPoid, mainDetRowId, companyPoid);
    }

    private int countAdvRecords(Long transactionPoid, Long companyPoid) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(TRANSACTION_POID) FROM GL_BANK_FILE_GENERATE WHERE MAIN_TRANSACTION_POID = ? AND FILE_COMPANY_POID = ? AND TXT_FILE LIKE 'ADVR-D%'",
            Integer.class, transactionPoid, companyPoid);
    }

    private void updateAdvRecordCount(Long transactionPoid, Long companyPoid, int recordCount) {
        jdbcTemplate.update(
            "UPDATE GL_BANK_FILE_GENERATE SET TXT_FILE = REPLACE(TXT_FILE, '<<NO_OF_ADV_RECORD>>', ?) " +
            "WHERE MAIN_TRANSACTION_POID = ? AND TXT_FILE LIKE 'ADVR-T%' AND FILE_COMPANY_POID = ?",
            String.valueOf(recordCount), transactionPoid, companyPoid);
    }

    private void generateAndSendAubFiles(Long transactionPoid, Long userPoid, Long companyPoid) {
        try {
            String companyCode = procRepository.getCompanyCode(companyPoid);
            String timestamp = new java.text.SimpleDateFormat("ddMMyyHHmmss").format(new java.util.Date());
            String filename = companyCode + "_PAY_" + timestamp + ".TXT";
            String filenameAdv = companyCode + "_ADV_" + timestamp + ".TXT";

            jdbcTemplate.update(
                "UPDATE GL_BANK_FILE_GENERATE SET TXT_FILE = REPLACE(TXT_FILE, '<<FILEPAYNAME>>', ?) " +
                "WHERE MAIN_TRANSACTION_POID = ? AND FILE_COMPANY_POID = ? AND TXT_FILE LIKE 'ADVR-H%'",
                filename, transactionPoid, companyPoid);

            final String aubFileDirectory = parameterServiceClient.findParameterValueByName(aubFileDirectoryParameterName).orElse("/win3mount/WinSCP/AUB_SFTP_LOAD");

            Path directory = Paths.get(aubFileDirectory);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            writePaymentFile(transactionPoid, companyPoid, filename, directory.toString());
            writeAdviceFile(transactionPoid, companyPoid, filenameAdv, directory.toString());

            jdbcTemplate.update(
                "UPDATE GL_BANK_DEBIT_HDR SET FILE_GENERATED = 'Y', FILE_NAME = ?, FILE_UNIQUE_ID = ?, " +
                "FILE_GENERATED_BY = ?, FILE_GENERATED_DATE = SYSDATE " +
                "WHERE TRANSACTION_POID IN (SELECT TRANSACTION_POID FROM GL_BANK_FILE_GENERATE " +
                "WHERE MAIN_TRANSACTION_POID = ? AND NVL(AMOUNT, 0) <> 0)",
                filename, transactionPoid, userPoid, transactionPoid);

            jdbcTemplate.update(
                "UPDATE GL_BANK_DEBIT_HDR SET FILE_NAME = FILE_NAME || ',' || ? " +
                "WHERE TRANSACTION_POID IN (SELECT TRANSACTION_POID FROM GL_BANK_FILE_GENERATE " +
                "WHERE MAIN_TRANSACTION_POID = ? AND NVL(AMOUNT, 0) <> 0)",
                filenameAdv, transactionPoid);

            log.info("AUB files created: {} and {}", filename, filenameAdv);
        } catch (Exception e) {
            log.error("Error generating AUB files", e);
            throw new RuntimeException("Failed to generate AUB files", e);
        }
    }

    private void writePaymentFile(Long transactionPoid, Long companyPoid, String filename, String directory) throws Exception {
        Path filePath = Paths.get(directory, filename);
        String sql = companyPoid != null
            ? "SELECT TXT_FILE FROM GL_BANK_FILE_GENERATE WHERE MAIN_TRANSACTION_POID = ? AND FILE_COMPANY_POID = ? AND TXT_FILE NOT LIKE 'ADVR-%' ORDER BY MAIN_DET_ROW_ID, TRANSACTION_POID, DET_ROW_ID"
            : "SELECT TXT_FILE FROM GL_BANK_FILE_GENERATE WHERE MAIN_TRANSACTION_POID = ? AND FILE_COMPANY_POID IS NULL AND TXT_FILE NOT LIKE 'ADVR-%' ORDER BY MAIN_DET_ROW_ID, TRANSACTION_POID, DET_ROW_ID";
        
        List<String> lines = companyPoid != null
            ? jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("TXT_FILE"), transactionPoid, companyPoid)
            : jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("TXT_FILE"), transactionPoid);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private void writeAdviceFile(Long transactionPoid, Long companyPoid, String filename, String directory) throws Exception {
        Path filePath = Paths.get(directory, filename);
        String sql = companyPoid != null
            ? "SELECT TXT_FILE FROM GL_BANK_FILE_GENERATE WHERE MAIN_TRANSACTION_POID = ? AND FILE_COMPANY_POID = ? AND TXT_FILE LIKE 'ADVR-%' ORDER BY DET_ROW_ID, TRANSACTION_POID, MAIN_DET_ROW_ID"
            : "SELECT TXT_FILE FROM GL_BANK_FILE_GENERATE WHERE MAIN_TRANSACTION_POID = ? AND FILE_COMPANY_POID IS NULL AND TXT_FILE LIKE 'ADVR-%' ORDER BY DET_ROW_ID, TRANSACTION_POID, MAIN_DET_ROW_ID";
        
        List<String> lines = companyPoid != null
            ? jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("TXT_FILE").replace("ADVR-", ""), transactionPoid, companyPoid)
            : jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("TXT_FILE").replace("ADVR-", ""), transactionPoid);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
}
