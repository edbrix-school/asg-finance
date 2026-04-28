package com.asg.finance.service.impl;

import com.asg.finance.dto.BankFileBatchResult;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankFileBatchServiceImpl implements BankFileBatchService {

    private final TelexFileGenerateProcRepository procRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BankFileAubService aubService;

    @Value("${bank.file.pp.directory:FAX_EMAIL}")
    private String ppFileDirectoryName;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    @Transactional
    public BankFileBatchResult createBankFileBatch(Long transactionPoid, Long userPoid) {
        List<Map<String, String>> messages = new ArrayList<>();
        try {
            addMessage(messages, "Starting bank file batch creation...", "INFO");

            Integer recordCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM GL_BANK_FILE_HDR WHERE TRANSACTION_POID = ?",
                Integer.class, transactionPoid);

            if (recordCount == 0) {
                String errorMsg = "No bank file header record found for transaction POID: " + transactionPoid;
                addMessage(messages, errorMsg, "ERROR");
                return new BankFileBatchResult("ERROR : " + errorMsg, messages);
            }

            addMessage(messages, "Bank file header record found. Validating...", "INFO");

            String deletedStatus = jdbcTemplate.queryForObject(
                "SELECT NVL(DELETED, 'N') FROM GL_BANK_FILE_HDR WHERE TRANSACTION_POID = ?",
                String.class, transactionPoid);

            if ("Y".equals(deletedStatus)) {
                String errorMsg = "Bank file header record is deleted for transaction POID: " + transactionPoid;
                addMessage(messages, errorMsg, "ERROR");
                return new BankFileBatchResult("ERROR : " + errorMsg, messages);
            }

            // Gap 1+5: DISTINCT added; fetched before branching only
            String bankList = jdbcTemplate.queryForObject(
                "SELECT DISTINCT NVL(BANK_LIST, 'Y') FROM GL_BANK_FILE_HDR WHERE TRANSACTION_POID = ?",
                String.class, transactionPoid);

            addMessage(messages, "Bank list type: " + bankList, "INFO");

            if ("A".equals(bankList)) {
                addMessage(messages, "Processing AUB file generation...", "INFO");
                String result = processAubFiles(transactionPoid, userPoid);
                boolean isSuccess = result.contains("SUCCESS");
                addMessage(messages, result, isSuccess ? "COMPLETED_SUCCESS" : "COMPLETED_ERROR");
                return new BankFileBatchResult(result, messages);
            }

            if ("B".equals(bankList)) {
                addMessage(messages, "Processing NBB file generation...", "INFO");
                String result = processNbbFiles(transactionPoid, userPoid);
                boolean isSuccess = result.contains("SUCCESS");
                addMessage(messages, result, isSuccess ? "COMPLETED_SUCCESS" : "COMPLETED_ERROR");
                return new BankFileBatchResult(result, messages);
            }

            // Gap 5: fetch only after AUB/NBB branch (matches proc flow)
            String ttSuppressBalanceCheck = jdbcTemplate.queryForObject(
                "SELECT NVL(TT_SUPPRESS_BALANCE_CHECK, 'N') FROM GL_BANK_FILE_HDR WHERE TRANSACTION_POID = ? AND NVL(DELETED, 'N') = 'N'",
                String.class, transactionPoid);

            if ("N".equals(ttSuppressBalanceCheck)) {
                addMessage(messages, "Checking bank balance...", "INFO");
                String odStatus = procRepository.checkOverdraft(transactionPoid);
                if (odStatus != null && odStatus.toUpperCase().contains("WARNING : AMOUNT IS GREATER THAN OUR")) {
                    addMessage(messages, "Insufficient balance: " + odStatus, "WARNING");
                    addMessage(messages, odStatus, "COMPLETED_ERROR");
                    return new BankFileBatchResult(odStatus, messages);
                }
                addMessage(messages, "Bank balance check passed", "SUCCESS");
            } else {
                addMessage(messages, "Balance check suppressed", "INFO");
            }

            addMessage(messages, "Fetching bank file details...", "INFO");
            List<BankFileDetailProjection> details = fetchBankFileDetails(transactionPoid);

            if (details.isEmpty()) {
                String errorMsg = "No bank file details found for transaction POID: " + transactionPoid;
                addMessage(messages, errorMsg, "ERROR");
                return new BankFileBatchResult("ERROR : " + errorMsg, messages);
            }

            addMessage(messages, "Found " + details.size() + " records to process", "INFO");

            List<String> statusMessages = new ArrayList<>();
            int seqNo = 0;
            // Gap 3: track last onlyApproval to match proc's post-loop early return
            String lastOnlyApproval = "N";

            for (int i = 0; i < details.size(); i++) {
                BankFileDetailProjection detail = details.get(i);
                addMessage(messages,
                    String.format("Processing record %d/%d: %s", i + 1, details.size(), detail.getDebitDocRef()),
                    "INFO");

                String onlyApproval = detail.getOnlyApproval() != null ? detail.getOnlyApproval() : "N";
                lastOnlyApproval = onlyApproval;

                if ("Y".equals(onlyApproval)) {
                    addMessage(messages, "Processing approval for: " + detail.getDebitDocRef(), "INFO");
                    String approvalStatus = procRepository.linkBankApproval(
                            1L, detail.getDebitCompanyPoid(), userPoid, "400-111", "xx",
                            "400-111-" + detail.getDebitTransactionPoid());
                    statusMessages.add(detail.getDebitDocRef() + "," + (approvalStatus != null ? approvalStatus.substring(0, Math.min(100, approvalStatus.length())) : ""));
                    addMessage(messages, "Approval processed for: " + detail.getDebitDocRef(), "SUCCESS");
                } else {
                    addMessage(messages, "Validating payment details for: " + detail.getDebitDocRef(), "INFO");
                    String validationError = validatePaymentDetails(detail);
                    if (validationError != null) {
                        updateDetailDeleted(transactionPoid, detail.getDebitDocRef(), "N");
                        statusMessages.add(validationError + ",FILE NOT GENERATED FOR " + detail.getDebitDocRef());
                        addMessage(messages, "Validation failed for " + detail.getDebitDocRef() + ": " + validationError, "WARNING");
                        continue;
                    }

                    addMessage(messages, "Processing payment for: " + detail.getDebitDocRef(), "INFO");
                    String paymentStatus = processPayment(detail, transactionPoid, userPoid, ++seqNo);
                    if (paymentStatus != null && paymentStatus.toUpperCase().contains("WARNING")) {
                        statusMessages.add(paymentStatus);
                        addMessage(messages, "Payment warning for " + detail.getDebitDocRef() + ": " + paymentStatus, "WARNING");
                    } else {
                        addMessage(messages, "Payment processed successfully for: " + detail.getDebitDocRef(), "SUCCESS");
                    }
                }
            }

            addMessage(messages, "Updating record counts and cleaning up...", "INFO");

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
                String result = String.join(",", statusMessages);
                addMessage(messages, "Approval-only batch completed: " + result, "COMPLETED_SUCCESS");
                return new BankFileBatchResult(result, messages);
            }

            // Mirror proc COMMIT at line 425: payment work must persist even if
            // file write / email fails. Catch here instead of letting Spring roll back.
            String fileEmailStatus = "SUCCESS: FILE SENT BY MAIL/API";
            try {
                addMessage(messages, "Generating and sending bank file...", "INFO");
                generateAndSendFile(transactionPoid, userPoid);
                addMessage(messages, "Triggering email notification...", "INFO");
                triggerMailJob();
                addMessage(messages, "Bank file generated and email sent successfully", "SUCCESS");
            } catch (Exception e) {
                log.error("Bank file write/email failed; payment work preserved", e);
                String msg = e.getMessage() == null ? "" : e.getMessage();
                fileEmailStatus = "ERROR : " + msg.substring(0, Math.min(200, msg.length()));
                addMessage(messages, "File generation/email failed: " + fileEmailStatus, "ERROR");
            }

            String finalResult = String.join(",", statusMessages) + fileEmailStatus;
            boolean isSuccess = !fileEmailStatus.startsWith("ERROR");
            addMessage(messages, finalResult, isSuccess ? "COMPLETED_SUCCESS" : "COMPLETED_ERROR");
            return new BankFileBatchResult(finalResult, messages);

        } catch (Exception e) {
            log.error("Error in createBankFileBatch for transactionPoid: {}", transactionPoid, e);
            String errorMsg = "ERROR : " + e.getMessage().substring(0, Math.min(200, e.getMessage().length()));
            addMessage(messages, errorMsg, "COMPLETED_ERROR");
            return new BankFileBatchResult(errorMsg, messages);
        }
    }

    private void addMessage(List<Map<String, String>> messages, String message, String status) {
        Map<String, String> entry = new HashMap<>();
        entry.put("message", message);
        entry.put("status", status);
        entry.put("timestamp", LocalDateTime.now().format(TIME_FORMATTER));
        messages.add(entry);
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
        String payingTo = procRepository.getPayingTo(detail.getDebitDocRef());
        if (payingTo != null && payingTo.startsWith("EMP-")) {
            if (!procRepository.checkEmployeeIban(detail.getDebitDocRef())) {
                return "ERROR: Employee Record Check IBAN/Others details ...." + detail.getDebitDocRef();
            }
        }

        if (!"BHD".equals(detail.getDebitCurrencyCode())) {
            Map<String, String> beneficiaryDetails = procRepository.getBeneficiaryDetails(
                    detail.getDebitTransactionPoid(), detail.getDebitDocRef());

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
        String countryCode = "BHD".equals(detail.getDebitCurrencyCode())
                ? "BH"
                : procRepository.getCountryCode(detail.getDebitTransactionPoid(), detail.getDebitDocRef());

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
            String timestamp = new java.text.SimpleDateFormat("ddMMMyyyy", java.util.Locale.ENGLISH).format(new java.util.Date()).toUpperCase();
            String random = String.valueOf(Math.round(Math.random() * 1000));
            String filename = "PPFILE" + transactionPoid + random + timestamp + ".TXT";

            final String directory = jdbcTemplate.queryForObject(
                "SELECT DIRECTORY_PATH FROM ALL_DIRECTORIES WHERE DIRECTORY_NAME = ?",
                String.class, ppFileDirectoryName);

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
            String subject = "BANK FILE " + new java.text.SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.ENGLISH).format(new java.util.Date()).toUpperCase();
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
//        try {
//            jdbcTemplate.execute("BEGIN DBMS_SCHEDULER.RUN_JOB(job_name => 'JOB_GLOBAL_MAIL_SENDING_QUEUE', USE_CURRENT_SESSION => FALSE); END;");
//        } catch (Exception e) {
//            log.warn("Error triggering mail job", e);
//        }
    }
}
