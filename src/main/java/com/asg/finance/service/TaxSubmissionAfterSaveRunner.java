package com.asg.finance.service;

import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.entity.GlobalTaxSubmissionHdr;
import com.asg.finance.repository.GlobalTaxSubmissionHdrRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
@Slf4j
public class TaxSubmissionAfterSaveRunner {

    private final GlobalTaxSubmissionHdrRepository hdrRepository;
    private final TaxSubmissionStoredProcedureHelper storedProcedureHelper;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GlobalTaxSubmissionHdr persistHeader(GlobalTaxSubmissionHdr header) {
        GlobalTaxSubmissionHdr saved = hdrRepository.save(header);
        entityManager.flush();
        log.debug("persistHeader committed transactionPoid={}", saved.getTransactionPoid());
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GlobalTaxSubmissionHdr runAfterSaveAndReload(
            Long transactionPoid, Long groupPoid, Long companyPoid, String userId) {
        String afterSaveStatus = storedProcedureHelper.processAfterSave(
                groupPoid, companyPoid, userId, transactionPoid);
        if (afterSaveStatus != null
                && (afterSaveStatus.contains("ERROR") || afterSaveStatus.contains("WARNING"))) {
            throw new ValidationException(afterSaveStatus);
        }
        entityManager.clear();
        return hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ValidationException(
                        "Tax submission not found after save: " + transactionPoid));
    }
}
