package com.asg.finance.event;

import com.asg.finance.dto.PettyCashRequestBase;
import com.asg.finance.repository.PettyCashPaymentVoucherCustomRepository;
import com.asg.common.lib.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class PettyCashVoucherEventListener {

    private final PettyCashPaymentVoucherCustomRepository pettyCashPaymentVoucherCustomRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPettyCashSaved(PettyCashVoucherSaveEvent event) {
        log.info("PettyCashVoucherEventListener triggered for transactionPoid: {}", event.getTransactionPoid());
        runAfterSaveReferenceProcedures(event);
    }

    private void runAfterSaveReferenceProcedures(PettyCashVoucherSaveEvent event) {
        PettyCashRequestBase requestDto = event.getRequestDto();
        Long transactionPoid = event.getTransactionPoid();
        String newRefType = normalizeRefType(requestDto.getRefType());

        // Re-apply old reference cost/status when reference changed.
        String oldRefType = event.getOldRefType();
        String oldRefPoid = event.getOldRefPoid();
        if (hasText(oldRefType) && hasText(oldRefPoid)) {
            String normalizedOldRefType = normalizeRefType(oldRefType);
            String newRefForOldType = resolveRefPoidByType(requestDto, normalizedOldRefType);
            boolean referenceChanged = !normalizedOldRefType.equals(newRefType) ||
                    !normalizeRefPoid(oldRefPoid).equals(normalizeRefPoid(newRefForOldType));
            if (referenceChanged) {
                executeAfterSaveProcedure(normalizedOldRefType, oldRefPoid, transactionPoid, event);
            }
        }

        String refPoid = resolveRefPoidByType(requestDto, newRefType);
        if (hasText(newRefType) && hasText(refPoid)) {
            executeAfterSaveProcedure(newRefType, refPoid, transactionPoid, event);
        }

        StringBuilder grnResult = new StringBuilder();
        pettyCashPaymentVoucherCustomRepository.updateSalesGrnStatus(
                event.getGroupPoid(),
                event.getCompanyPoid(),
                event.getUserPoid(),
                event.getDocId(),
                transactionPoid,
                grnResult
        );
        assertProcedureSuccess("PROC_SALES_GRN_UPDATE_STATUS", grnResult);
    }

    private void executeAfterSaveProcedure(String refType, String refPoid, Long transactionPoid,
                                           PettyCashVoucherSaveEvent event) {
        Long groupPoid = event.getGroupPoid();
        Long companyPoid = event.getCompanyPoid();
        Long userPoid = event.getUserPoid();
        StringBuilder procResult = new StringBuilder();

        switch (normalizeRefType(refType)) {
            case "FF JOBS" -> {
                pettyCashPaymentVoucherCustomRepository.updateCostFF(
                        groupPoid, companyPoid, userPoid, refPoid, transactionPoid, procResult);
                assertProcedureSuccess("PROC_AP_PI_FF_UPDATE_COST", procResult);
            }
            case "FDA JOBS" -> {
                pettyCashPaymentVoucherCustomRepository.updateCostFDA(
                        groupPoid, companyPoid, userPoid, refPoid, transactionPoid, procResult);
                assertProcedureSuccess("PROC_AP_PI_FDA_UPDATE_COST", procResult);
            }
            case "MTA RFQ" -> {
                pettyCashPaymentVoucherCustomRepository.updateRfqPurchasePrice(
                        groupPoid, companyPoid, userPoid, refPoid, procResult);
                assertProcedureSuccess("PROC_RFQ_UPDATE_PURCHASE_PRICE", procResult);
            }
            case "GENERAL PO" -> {
                pettyCashPaymentVoucherCustomRepository.updatePurchaseOrderStatus(
                        groupPoid, companyPoid, userPoid, refPoid, transactionPoid, procResult);
                assertProcedureSuccess("PROC_AP_PO_UPDATE_STATUS", procResult);
            }
            default -> {
                // No post-save procedure for other reference types.
            }
        }
    }

    private String resolveRefPoidByType(PettyCashRequestBase requestDto, String refType) {
        return switch (normalizeRefType(refType)) {
            case "FF JOBS" -> requestDto.getFfRef();
            case "FDA JOBS" -> requestDto.getFdaRef();
            case "MTA RFQ" -> requestDto.getSalesQtnRef();
            case "GENERAL PO" -> requestDto.getPoRef();
            case "GRN_JOBS" -> hasText(requestDto.getPoRef()) ? requestDto.getPoRef()
                    : (requestDto.getGrnSupplierPoid() != null
                    ? String.valueOf(requestDto.getGrnSupplierPoid()) : null);
            default -> null;
        };
    }

    private String normalizeRefType(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRefPoid(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void assertProcedureSuccess(String procedureName, StringBuilder result) {
        String response = result == null ? "" : result.toString();
        if (hasText(response)) {
            String normalized = response.toUpperCase(Locale.ROOT);
            if (normalized.contains("ERROR") || normalized.contains("WARNING")) {
                log.error("{} returned error response: {}", procedureName, response);
                throw new ValidationException(procedureName + " failed: " + response);
            }
        }
    }
}
