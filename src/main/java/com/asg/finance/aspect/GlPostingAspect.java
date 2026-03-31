package com.asg.finance.aspect;

import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.ApprovalService;
import com.asg.finance.annotation.PerformGlPosting;
import com.asg.finance.service.GlPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class GlPostingAspect {

    private final GlPostingService glPostingService;
    private final ApprovalService approvalService;

    @AfterReturning(pointcut = "@annotation(performGlPosting)", returning = "result")
    public void performGlPosting(JoinPoint joinPoint, PerformGlPosting performGlPosting, Object result) {

        if (result == null) {
            log.warn("PerformGlPosting: Method result is null, skipping GL posting.");
            return;
        }

        try {
            String docId = performGlPosting.docId();
            if (!StringUtils.hasText(docId)) {
                docId = UserContext.getDocumentId();
            }

            Long transactionPoid = extractLongValue(result, "getPoid", "getTransactionPoid");
            String docRef = extractStringValue(result, "getDocRef");

            if (transactionPoid == null || !StringUtils.hasText(docId)) {
                log.warn("PerformGlPosting: Could not extract necessary information. DocId: {}, Poid: {}, DocRef: {}",
                        docId, transactionPoid, docRef);
                return;
            }

            // 🔥 KEY CHANGE: run AFTER COMMIT
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                String finalDocId = docId;
                Long finalTransactionPoid = transactionPoid;
                String finalDocRef = docRef;

                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                try {
                                    String approvalStatus = approvalService.getApprovalStatus(documentId, savedHeader.getTransactionPoid());
                                    if ("APPROVAL_NOT_APPLICABLE".equalsIgnoreCase(approvalStatus)) {
                                        glPostingService.performGlPosting(finalDocId, finalTransactionPoid, finalDocRef);
                                    }
                                } catch (Exception e) {
                                    log.error("Error during GL Posting after commit: {}", e.getMessage(), e);
                                    UserContext.setGlPostingError(e.getMessage());
                                }
                            }
                        }
                );
            } else {
                // fallback if no transaction
                String approvalStatus = approvalService.getApprovalStatus(documentId, savedHeader.getTransactionPoid());
                if ("APPROVAL_NOT_APPLICABLE".equalsIgnoreCase(approvalStatus)) {
                        glPostingService.performGlPosting(docId, transactionPoid, docRef);
                }
            }

        } catch (Exception e) {
            log.error("Error in GlPostingAspect: {}", e.getMessage(), e);
            UserContext.setGlPostingError(e.getMessage());
        }
    }

    private Long extractLongValue(Object result, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = result.getClass().getMethod(methodName);
                Object val = method.invoke(result);
                if (val instanceof Long) {
                    return (Long) val;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String extractStringValue(Object result, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = result.getClass().getMethod(methodName);
                Object val = method.invoke(result);
                if (val instanceof String) {
                    return (String) val;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}