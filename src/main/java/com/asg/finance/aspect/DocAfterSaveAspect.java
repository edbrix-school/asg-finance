package com.asg.finance.aspect;

import com.asg.finance.annotation.DocAfterSave;
import com.asg.finance.service.DocAfterSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

import static com.asg.common.lib.security.util.UserContext.getDocumentId;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class DocAfterSaveAspect {

    private final DocAfterSaveService docAfterSaveService;

    @AfterReturning(pointcut = "@annotation(docAfterSave)", returning = "result")
    public void runDocAfterSave(JoinPoint joinPoint, DocAfterSave docAfterSave, Object result) {
        if (result == null) {
            log.warn("DocAfterSaveAspect: method returned null, skipping PROC_DOC_AFTER_SAVE.");
            return;
        }

        try {
            String docId = docAfterSave.docId();
            if (!StringUtils.hasText(docId)) {
                docId = getDocumentId();
            }

            Long transactionPoid = extractLongValue(result, "getTransactionPoid", "getPoid");

            if (transactionPoid == null || !StringUtils.hasText(docId)) {
                log.warn("DocAfterSaveAspect: cannot determine docId or transactionPoid — docId={}, poid={}. Skipping.",
                        docId, transactionPoid);
                return;
            }

            String finalDocId = docId;
            Long   finalPoid  = transactionPoid;

            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            docAfterSaveService.callDocAfterSave(finalDocId, finalPoid);
                        } catch (Exception e) {
                            log.error("DocAfterSaveAspect afterCommit failed — docId={}, poid={}: {}",
                                    finalDocId, finalPoid, e.getMessage(), e);
                        }
                    }
                });
            } else {
                docAfterSaveService.callDocAfterSave(finalDocId, finalPoid);
            }

        } catch (Exception e) {
            log.error("DocAfterSaveAspect error: {}", e.getMessage(), e);
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
}
