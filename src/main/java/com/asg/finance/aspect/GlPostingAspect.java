package com.asg.finance.aspect;

import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.annotation.PerformGlPosting;
import com.asg.finance.service.GlPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class GlPostingAspect {

    private final GlPostingService glPostingService;

    @AfterReturning(pointcut = "@annotation(performGlPosting)", returning = "result")
    public void performGlPosting(JoinPoint joinPoint, PerformGlPosting performGlPosting, Object result) {
        try {
            if (result == null) {
                log.warn("PerformGlPosting: Method result is null, skipping GL posting.");
                return;
            }

            String docId = performGlPosting.docId();
            if (!StringUtils.hasText(docId)) {
                docId = UserContext.getDocumentId();
            }

            Long transactionPoid = extractLongValue(result, "getPoid", "getTransactionPoid");
            String docRef = extractStringValue(result, "getDocRef");

            if (transactionPoid != null && StringUtils.hasText(docId)) {
                glPostingService.performGlPosting(docId, transactionPoid, docRef);
            } else {
                log.warn("PerformGlPosting: Could not extract necessary information. DocId: {}, Poid: {}, DocRef: {}", 
                        docId, transactionPoid, docRef);
            }

        } catch (Exception e) {
            log.error("Error in GlPostingAspect: {}", e.getMessage(), e);
            if (e instanceof RuntimeException) {
                throw e;
            }
            throw new RuntimeException("Unexpected error during automatic GL Posting", e);
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
