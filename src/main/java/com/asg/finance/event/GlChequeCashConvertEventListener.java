package com.asg.finance.event;

import com.asg.finance.repository.ChequeCashConvertCustomRepository;
import com.asg.common.lib.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlChequeCashConvertEventListener {

    private final ChequeCashConvertCustomRepository customRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGlChequeCashConvertSaved(GlChequeCashConvertSaveEvent event) {
        log.info("GlChequeCashConvertEventListener triggered for transaction: {}", event.getEntity().getTransactionPoid());
        
        String status = customRepository.convertChequeAfterSave(
                event.getGroupPoid(),
                event.getCompanyPoid(),
                event.getEntity().getTransactionPoid(),
                event.getEntity().getDocRef(),
                event.getUserPoid(),
                event.getCurrentUser()
        );
        
        if (status != null && status.contains("ERROR")) {
            log.error("Some error occured in PROC_CHEQUE_CONVERT_AFTER_SAVE : {}", status);
            throw new ValidationException("Some error occured in PROC_CHEQUE_CONVERT_AFTER_SAVE : " + status);
        }
    }
}
