package com.asg.finance.service.impl;

import com.asg.common.lib.client.ParameterServiceClient;
import com.asg.finance.dto.BankFileDetailProjection;
import com.asg.finance.repository.TelexFileGenerateProcRepository;
import com.asg.finance.service.BankFileAubService;
import com.asg.finance.service.BankFileBatchService;
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
public class BankFileBatchServiceImpl implements BankFileBatchService {

    private final TelexFileGenerateProcRepository procRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BankFileAubService aubService;
    private final ParameterServiceClient parameterServiceClient;
    
    @Value("${bank.file.pp.directory:FAX_EMAIL}")
    private String ppFileDirectoryParameterName;

    @Override
    @Transactional
    public String createBankFileBatch(Long transactionPoid, Long userPoid) {
        try {
            // Gap 1+5: DISTINCT added; fetched before branching only
            String bankList = jdbcTemplate.queryForObject(
                "SELECT DISTINCT NVL(BANK_LIST, 'Y') FROM GL_BANK_FILE_HDR WHERE TRANSACTION_POID = ?",
                String.class, transactionPoid);

            // Handle AUB file generation
            if ("A".equals(bankList)) {
                return processAubFiles(transactionPoid, userPoid);
            }

            // Handle NBB file generation
            if ("B".equals(bankList)) {
                return processNbbFiles(transactionPoid, userPoid);
            }

            // Gap 5: fetch only after AUB/NBB branch (matches proc flow)
            String ttSuppressBalanceCheck = jdbcTemplate.queryForObject(
                "SELECT NVL(TT_SUPPRESS_BALANCE_CHECK, 'N') FROM GL_BANK_FILE_HDR WHERE TRANSACTION_POID = ? AND NVL(DELETED, 'N') = 'N'",
                String.class, transactionPoid);

            // Check overdraft if not suppressed
            if ("N".equals(ttSuppressBalanceCheck)) {
                String odStatus = procRepository.checkOverdraft(transactionPoid);
                if (odStatus != null && odStatus.toUpperCase().contains("WARNING : AMOUNT IS GREATER THAN OUR")) {
                    return odStatus;
                }
            }

            // Process bank file details
            List<BankFileDetailProjection> details = fetchBankFileDetails(transactionPoid);
            List<String> statusMessages = new ArrayList<>();
            int seqNo = 0;
            // Gap 3: track last onlyApproval to match proc's post-loop early return
            String lastOnlyApproval = "N";

            for (BankFileDetailProjection detail : details) {
                String onlyApproval = detail.getOnlyApproval() != null ? detail.getOnlyApproval() : "N";
                lastOnlyApproval = onlyApproval;

                if ("Y".equals(onlyApproval)) {
                    String approvalStatus = procRepository.linkBankApproval(
                            1L, detail.getDebitCompanyPoid(), userPoid, "400-111", "xx",
                            "400-111-" + detail.getDebitTransactionPoid());
                    statusMessages.add(detail.getDebitDocRef() + "," + (approvalStatus != null ? approvalStatus.substring(0, Math.min(100, approvalStatus.length())) : ""));
                } else {
                    String validationError = validatePaymentDetails(detail);
                    if (validationError != null) {
                        updateDetailDeleted(transactionPoid, detail.getDebitDocRef(), "N");
                        statusMessages.add(validationError + ",FILE NOT GENERATED FOR " + detail.getDebitDocRef());
                        continue;
                    }

                    String paymentStatus = processPayment(detail, transactionPoid, userPoid, ++seqNo);
                    if (paymentStatus != null && paymentStatus.toUpperCase().contains("WARNING")) {
                        statusMessages.add(paymentStatus);
                    }
                }
            }

            // Update total record count
            int totalRecords = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM GL_BANK_FILE_GENERATE WHERE MAIN_TRANSACTION_POID = ?",
                Integer.class, transactionPoid);
            jdbcTemplate.update(
                "UPDATE GL_BANK_FILE_GENERATE SET TXT_FILE = REPLACE(TXT_FILE, '<TOTALRECORDline>', ?) WHERE MAIN_TRANSACTION_POID = ? AND DET_ROW_ID = 1",
                totalRecords, transactionPoid);

            // Delete non-deleted details
            jdbcTemplate.update("DELETE FROM GL_BANK_FILE_DTL WHERE TRANSACTION_POID = ? AND DELETED = 'N'", transactionPoid);

            // Gap 3: skip file generation and email for approval-only batches (mirrors proc line 427-430)
            if ("Y".equals(lastOnlyApproval)) {
                return String.join(",", statusMessages);
            }

            // Generate and send file
            generateAndSendFile(transactionPoid, userPoid);

            // Trigger mail job
            triggerMailJob();

            return String.join(",", statusMessages) + "SUCCESS: FILE SENT BY MAIL/API";

        } catch (Exception e) {
            log.error("Error in createBankFileBatch", e);
            return "ERROR : " + e.getMessage().substring(0, Math.min(200, e.getMessage().length()));
        }
    }

    private List<BankFileDetailProjection> fetchBankFileDetails(Long transactionPoid) {
        String sql = "SELECT DEBIT_COMPANY_POID, DEBIT_TRANSACTION_POID, DEBIT_DOC_REF, DEBIT_CURRENCY_CODE, " +
                     "NVL(ONLY_APPROVAL, 'N') ONLY_APPROVAL, TT_SUPPRESS_BALANCE_CHECK, GLDTL.DET_ROW_ID " +
                     "FROM GL_BANK_FILE_HDR GLHDR " +
                     "INNER JOIN GL_BANK_FILE_DTL GLDTL ON GLHDR.TRANSACTION_POID = GLDTL.TRANSACTION_POID " +
                     "WHERE GLDTL.TRANSACTION_POID = ? AND GLDTL.DELETED = 'Y' " +
                     "AND NVL(GLHDR.DELETED, 'N') = 'N' AND NVL(DEBIT_TRANSACTION_POID, 0) <> 0";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> new BankFileDetailProjection(
            rs.getLong("DEBIT_COMPANY_POID"),
            rs.getLong("DEBIT_TRANSACTION_POID"),
            rs.getString("DEBIT_DOC_REF"),
            rs.getString("DEBIT_CURRENCY_CODE"),
            rs.getString("ONLY_APPROVAL"),
            rs.getString("TT_SUPPRESS_BALANCE_CHECK"),
            rs.getLong("DET_ROW_ID")
        ), transactionPoid);
    }

    private List<Long> fetchDistinctCompanyPoids(Long transactionPoid) {
        return jdbcTemplate.query(
            "SELECT DISTINCT DEBIT_COMPANY_POID FROM GL_BANK_FILE_DTL WHERE TRANSACTION_POID = ? AND NVL(DEBIT_TRANSACTION_POID, 0) <> 0",
            (rs, rowNum) -> rs.getLong(1), transactionPoid);
    }

    private void updateDetailDeleted(Long transactionPoid, String docRef, String deleted) {
        jdbcTemplate.update(
            "UPDATE GL_BANK_FILE_DTL SET DELETED = ? WHERE TRANSACTION_POID = ? AND DELETED = 'Y' AND DEBIT_DOC_REF = ?",
            deleted, transactionPoid, docRef);
    }

    private String processAubFiles(Long transactionPoid, Long userPoid) {
        try {
            List<Long> companyPoids = fetchDistinctCompanyPoids(transactionPoid);
            for (Long companyPoid : companyPoids) {
                aubService.createBankFileBatchAub(transactionPoid, userPoid, companyPoid);
            }
            return "SUCCESS: FILE Generated....";
        } catch (Exception e) {
            return "ERROR: Aub File Generation-->" + e.getMessage();
        }
    }

    private String processNbbFiles(Long transactionPoid, Long userPoid) {
        try {
            List<Long> companyPoids = fetchDistinctCompanyPoids(transactionPoid);
            for (Long companyPoid : companyPoids) {
                procRepository.createBankFileBatchNbb(transactionPoid, userPoid, companyPoid);
            }
            return "SUCCESS: FILE Generated....";
        } catch (Exception e) {
            return "ERROR: NBB File Generation-->" + e.getMessage();
        }
    }

    private String validatePaymentDetails(BankFileDetailProjection detail) {
        // Check employee IBAN if paying to employee
        String payingTo = procRepository.getPayingTo(detail.getDebitDocRef());
        if (payingTo != null && payingTo.startsWith("EMP-")) {
            if (!procRepository.checkEmployeeIban(detail.getDebitDocRef())) {
                return "ERROR: Employee Record Check IBAN/Others details ...." + detail.getDebitDocRef();
            }
        }

        // Validate foreign currency details
        if (!"BHD".equals(detail.getDebitCurrencyCode())) {
            Map<String, String> beneficiaryDetails = procRepository.getBeneficiaryDetails(detail.getDebitTransactionPoid());

            String ttChargeType = procRepository.getTtChargeType(detail.getDebitTransactionPoid());
            if (ttChargeType == null) {
                return "ERROR: TT CHARGE NOT ENTERED (OUR/SHARE) ...." + detail.getDebitDocRef();
            }

            String intermediaryBank = beneficiaryDetails.get("INTERMEDIARY_BANK");
            String intermediaryCountry = beneficiaryDetails.get("INTERMEDIARY_COUNTRY_POID");
            if (intermediaryBank != null && intermediaryCountry == null) {
                return "ERROR: INTERMEDIARY BANK COUNTRY NOT MAP...." + detail.getDebitDocRef();
            }

            String beneficiaryCountry = beneficiaryDetails.get("BENEFICIARY_COUNTRY");
            if (beneficiaryCountry == null) {
                return "ERROR: BENEFICIARY COUNTRY NOT MAP...." + detail.getDebitDocRef();
            }
        }

        return null;
    }

    private String processPayment(BankFileDetailProjection detail, Long transactionPoid, Long userPoid, int seqNo) {
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
        String countryCode = "BHD".equals(detail.getDebitCurrencyCode()) ? "BH" : procRepository.getCountryCode(detail.getDebitTransactionPoid());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        procRepository.createBankFilePayment(
                detail.getDebitCompanyPoid(), detail.getDebitTransactionPoid(), userPoid,
                companyDetails.get("COMPANY_NAME"), companyDetails.get("ADDRESS"), "BAHRAIN",
                detail.getDebitDocRef(), "OUR", countryCode, transactionPoid, seqNo);

        try {
            procRepository.generateHsbcApiXml(
                    detail.getDebitCompanyPoid(), detail.getDebitTransactionPoid(), userPoid,
                    companyDetails.get("COMPANY_NAME"), companyDetails.get("ADDRESS"), "BAHRAIN",
                    detail.getDebitDocRef(), "OUR", countryCode, transactionPoid, seqNo, detail.getDetRowId());
        } catch (Exception e) {
            log.warn("Error generating HSBC API XML", e);
        }

        return null;
    }

    private void generateAndSendFile(Long transactionPoid, Long userPoid) {
        try {
            String timestamp = new java.text.SimpleDateFormat("ddMMMyyyy").format(new java.util.Date()).toUpperCase();
            String random = String.valueOf(Math.round(Math.random() * 1000));
            String filename = "PPFILE" + transactionPoid + random + timestamp + ".TXT";

            final String directory = parameterServiceClient.findParameterValueByName(ppFileDirectoryParameterName).orElseThrow();

            Path directoryPath = Paths.get(directory);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            Path filePath = Paths.get(directory, filename);
            List<String> lines = jdbcTemplate.query(
                "SELECT TXT_FILE FROM GL_BANK_FILE_GENERATE " +
                "WHERE MAIN_TRANSACTION_POID = ? " +
                "ORDER BY MAIN_DET_ROW_ID, TRANSACTION_POID, DET_ROW_ID",
                (rs, rowNum) -> rs.getString("TXT_FILE"),
                transactionPoid);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            jdbcTemplate.update(
                "UPDATE GL_BANK_DEBIT_HDR SET FILE_GENERATED = 'Y', FILE_NAME = ?, FILE_UNIQUE_ID = ?, " +
                "FILE_GENERATED_BY = ?, FILE_GENERATED_DATE = SYSDATE " +
                "WHERE TRANSACTION_POID IN (SELECT TRANSACTION_POID FROM GL_BANK_FILE_GENERATE WHERE MAIN_TRANSACTION_POID = ?)",
                filename, transactionPoid, userPoid, transactionPoid);

            String emailId = getEmailForUser(userPoid);
            if (emailId != null) {
                queueEmail(emailId, userPoid, transactionPoid);
            }

            log.info("Bank file created: {}", filename);
        } catch (Exception e) {
            log.error("Error generating bank file", e);
            throw new RuntimeException("Failed to generate bank file", e);
        }
    }

    private String getEmailForUser(Long userPoid) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT LISTAGG(USER_EMAIL, ';') WITHIN GROUP (ORDER BY USER_EMAIL) " +
                "FROM GLOBAL_USERS GUSR " +
                "INNER JOIN GLOBAL_USERS_AUTH_ROLES_DTL GUARD ON GUSR.USER_POID = GUARD.USER_POID " +
                "WHERE GUSR.USER_POID = ? AND USER_ROLE_POID IN " +
                "(SELECT USER_ROLE_POID FROM GLOBAL_USER_ROLES WHERE USER_ROLE_ID = 'TT_FILE_EMAIL')",
                String.class, userPoid);
        } catch (Exception e) {
            return null;
        }
    }

    private void queueEmail(String emailId, Long userPoid, Long transactionPoid) {
        try {
            String subject = "BANK FILE " + new java.text.SimpleDateFormat("dd-MMM-yyyy").format(new java.util.Date()).toUpperCase();
            jdbcTemplate.update(
                "INSERT INTO GLOBAL_MAIL_SENDING_QUEUE (RECEVER_EMAIL_ID, MESSAGE_SUBJECT, MESSGE_BODY, " +
                "SENT_STATUS, QUEUE_DATE, SENDING_POID, ATTACHED_FILE_NAME, LINK_TRANSACTION_POID) " +
                "VALUES (?, ?, ?, 'N', SYSDATE, ?, NULL, NULL)",
                emailId, subject, subject, userPoid);
        } catch (Exception e) {
            log.warn("Error queuing email", e);
        }
    }

    private void triggerMailJob() {
        try {
            jdbcTemplate.execute("BEGIN DBMS_SCHEDULER.RUN_JOB(job_name => 'JOB_GLOBAL_MAIL_SENDING_QUEUE', USE_CURRENT_SESSION => FALSE); END;");
        } catch (Exception e) {
            log.warn("Error triggering mail job", e);
        }
    }
}
