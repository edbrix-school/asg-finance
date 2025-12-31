package com.asg.finance.scheduler;

import com.asg.common.lib.client.ParameterServiceClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.asg.finance.utility.HsbcPaymentClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class HsbcPaymentScheduler {

    private final HsbcPaymentClient paymentClient;
    private final ParameterServiceClient parameterServiceClient;

    @Scheduled(fixedDelayString = "${hsbc.payment.process.interval:300000}")
    public void processPayments() {
        try {
            String enabled = parameterServiceClient.findParameterValueByName("HSBC_PAYMENT_SCHEDULER_ENABLED")
                    .orElse("N");
            
            if ("Y".equalsIgnoreCase(enabled)) {
                log.info("Starting HSBC payment scheduler");
                paymentClient.processPendingPayments();
            }
        } catch (Exception e) {
            log.error("Error in HSBC payment scheduler: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelayString = "${hsbc.payment.status.interval:600000}")
    public void checkPaymentStatus() {
        try {
            String enabled = parameterServiceClient.findParameterValueByName("HSBC_PAYMENT_SCHEDULER_ENABLED")
                    .orElse("N");
            
            if ("Y".equalsIgnoreCase(enabled)) {
                log.info("Starting HSBC payment status check");
                paymentClient.checkPaymentStatus();
            }
        } catch (Exception e) {
            log.error("Error in HSBC payment status scheduler: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "${hsbc.balance.mismatch.cron:0 0 8 * * ?}")
    public void checkBalanceMismatch() {
        try {
            String enabled = parameterServiceClient.findParameterValueByName("HSBC_ALERT_SCHEDULER_ENABLED")
                    .orElse("N");
            
            if ("Y".equalsIgnoreCase(enabled)) {
                log.info("Starting HSBC balance mismatch check");
                paymentClient.checkBalanceMismatch();
            }
        } catch (Exception e) {
            log.error("Error in HSBC balance mismatch scheduler: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "${hsbc.ledger.mismatch.cron:0 30 8 * * ?}")
    public void checkLedgerMismatch() {
        try {
            String enabled = parameterServiceClient.findParameterValueByName("HSBC_ALERT_SCHEDULER_ENABLED")
                    .orElse("N");
            
            if ("Y".equalsIgnoreCase(enabled)) {
                log.info("Starting HSBC ledger mismatch check");
                paymentClient.checkLedgerMismatch();
            }
        } catch (Exception e) {
            log.error("Error in HSBC ledger mismatch scheduler: {}", e.getMessage(), e);
        }
    }
}
