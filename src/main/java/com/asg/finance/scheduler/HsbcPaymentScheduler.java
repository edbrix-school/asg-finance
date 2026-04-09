package com.asg.finance.scheduler;

import com.asg.finance.repository.HsbcSchedulerSettingsRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.asg.finance.utility.HsbcApiClient;
import com.asg.finance.utility.HsbcPaymentClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class HsbcPaymentScheduler {

    private static final String ACTION_PAYMNT_POST    = "HSBC_API_PAYMNT_POST";
    private static final String ACTION_PAY_ENQUIRY    = "HSBC_API_PAY_ENQUIRY";
    private static final String ACTION_STMT_SYNC      = "HSBC_API_STATEMENT_SYNC";
    private static final String ACTION_BAL_MISMATCH   = "HSBC_API_BAL_MISMATCH";
    private static final String ACTION_LEDGER_MISMATCH = "HSBC_API_LEDGER_MISMATCH";

    private final HsbcPaymentClient paymentClient;
    private final HsbcApiClient hsbcApiClient;
    private final HsbcSchedulerSettingsRepository settingsRepository;

    // Running status flags — mirrors legacy RUNNING_STATUS to prevent overlapping runs
    private final AtomicBoolean paymentProcessRunning  = new AtomicBoolean(false);
    private final AtomicBoolean paymentStatusRunning   = new AtomicBoolean(false);
    private final AtomicBoolean statementSyncRunning   = new AtomicBoolean(false);
    private final AtomicBoolean balanceMismatchRunning = new AtomicBoolean(false);
    private final AtomicBoolean ledgerMismatchRunning  = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "${hsbc.payment.process.interval:300000}")
    public void processPayments() {
        if (paymentProcessRunning.get()) {
            log.info("{} already running, skipping this cycle", ACTION_PAYMNT_POST);
            return;
        }
        if (!settingsRepository.isActive(ACTION_PAYMNT_POST)) return;
        try {
            System.gc();
            String lastExecutionTimeStr = currentDateTimeStr();
            settingsRepository.updateLastExecutionTime(ACTION_PAYMNT_POST, lastExecutionTimeStr);
            log.info("{} Last Execution Time : {}", ACTION_PAYMNT_POST, lastExecutionTimeStr);

            paymentProcessRunning.set(true);
            log.info("{} HsbcPaymentClient Initialized : {}", ACTION_PAYMNT_POST, lastExecutionTimeStr);

            paymentClient.processPendingPayments();
            log.info("{} processPaymentCalls Done : {}", ACTION_PAYMNT_POST, lastExecutionTimeStr);
        } catch (Exception e) {
            log.error("ERROR in {} : {}", ACTION_PAYMNT_POST, e.getMessage(), e);
        } finally {
            paymentProcessRunning.set(false);
        }
    }

    @Scheduled(fixedDelayString = "${hsbc.payment.status.interval:600000}")
    public void checkPaymentStatus() {
        if (paymentStatusRunning.get()) {
            log.info("{} already running, skipping this cycle", ACTION_PAY_ENQUIRY);
            return;
        }
        if (!settingsRepository.isActive(ACTION_PAY_ENQUIRY)) return;
        try {
            System.gc();
            String lastExecutionTimeStr = currentDateTimeStr();
            settingsRepository.updateLastExecutionTime(ACTION_PAY_ENQUIRY, lastExecutionTimeStr);
            log.info("{} Last Execution Time : {}", ACTION_PAY_ENQUIRY, lastExecutionTimeStr);

            paymentStatusRunning.set(true);
            log.info("{} HsbcPaymentClient Initialized : {}", ACTION_PAY_ENQUIRY, lastExecutionTimeStr);

            paymentClient.checkPaymentStatus();
            log.info("{} checkPaymentStatus Done : {}", ACTION_PAY_ENQUIRY, lastExecutionTimeStr);
        } catch (Exception e) {
            log.error("ERROR in {} : {}", ACTION_PAY_ENQUIRY, e.getMessage(), e);
        } finally {
            paymentStatusRunning.set(false);
        }
    }

    @Scheduled(cron = "${hsbc.statement.sync.cron:0 0 6 * * ?}")
    public void syncStatements() {
        if (statementSyncRunning.get()) {
            log.info("{} already running, skipping this cycle", ACTION_STMT_SYNC);
            return;
        }
        if (!settingsRepository.isActive(ACTION_STMT_SYNC)) return;
        try {
            System.gc();
            String dateVal = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
            String lastExecTimeStr = currentDateTimeStr();
            settingsRepository.updateLastExecutionTime(ACTION_STMT_SYNC, lastExecTimeStr);
            log.info("{} Last Execution Time : {}", ACTION_STMT_SYNC, lastExecTimeStr);

            statementSyncRunning.set(true);
            log.info("{} HsbcApiClient Initialized : {}", ACTION_STMT_SYNC, lastExecTimeStr);

            // syncAllAccounts internally calls performStmtReconcile after all accounts
            hsbcApiClient.syncAllAccounts(dateVal, 1L, 1L, 1L, "SCHEDULER");
            log.info("{} checkPgpHelper Done : {}", ACTION_STMT_SYNC, lastExecTimeStr);
            log.info("{} performStmtReconcile Done : {}", ACTION_STMT_SYNC, lastExecTimeStr);
        } catch (Exception e) {
            log.error("ERROR in {} : {}", ACTION_STMT_SYNC, e.getMessage(), e);
        } finally {
            statementSyncRunning.set(false);
        }
    }

    @Scheduled(cron = "${hsbc.balance.mismatch.cron:0 0 8 * * ?}")
    public void checkBalanceMismatch() {
        if (balanceMismatchRunning.get()) {
            log.info("{} already running, skipping this cycle", ACTION_BAL_MISMATCH);
            return;
        }
        if (!settingsRepository.isActive(ACTION_BAL_MISMATCH)) return;
        try {
            String lastExecutionTimeStr = currentDateTimeStr();
            settingsRepository.updateLastExecutionTime(ACTION_BAL_MISMATCH, lastExecutionTimeStr);
            log.info("{} Last Execution Time : {}", ACTION_BAL_MISMATCH, lastExecutionTimeStr);

            balanceMismatchRunning.set(true);
            log.info("{} HsbcPaymentClient Initialized : {}", ACTION_BAL_MISMATCH, lastExecutionTimeStr);

            paymentClient.checkBalanceMismatch();
            log.info("{} checkBalanceMismatch Done : {}", ACTION_BAL_MISMATCH, lastExecutionTimeStr);
        } catch (Exception e) {
            log.error("ERROR in {} : {}", ACTION_BAL_MISMATCH, e.getMessage(), e);
        } finally {
            balanceMismatchRunning.set(false);
        }
    }

    @Scheduled(cron = "${hsbc.ledger.mismatch.cron:0 30 8 * * ?}")
    public void checkLedgerMismatch() {
        if (ledgerMismatchRunning.get()) {
            log.info("{} already running, skipping this cycle", ACTION_LEDGER_MISMATCH);
            return;
        }
        if (!settingsRepository.isActive(ACTION_LEDGER_MISMATCH)) return;
        try {
            String lastExecutionTimeStr = currentDateTimeStr();
            settingsRepository.updateLastExecutionTime(ACTION_LEDGER_MISMATCH, lastExecutionTimeStr);
            log.info("{} Last Execution Time : {}", ACTION_LEDGER_MISMATCH, lastExecutionTimeStr);

            ledgerMismatchRunning.set(true);
            log.info("{} HsbcPaymentClient Initialized : {}", ACTION_LEDGER_MISMATCH, lastExecutionTimeStr);

            paymentClient.checkLedgerMismatch();
            log.info("{} checkLedgerMismatch Done : {}", ACTION_LEDGER_MISMATCH, lastExecutionTimeStr);
        } catch (Exception e) {
            log.error("ERROR in {} : {}", ACTION_LEDGER_MISMATCH, e.getMessage(), e);
        } finally {
            ledgerMismatchRunning.set(false);
        }
    }

    private String currentDateTimeStr() {
        return new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss").format(new Date());
    }
}
