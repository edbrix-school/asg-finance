package com.asg.finance.validation;

import com.asg.finance.dto.ChequeReturnRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class ChequeHeaderValidator implements ConstraintValidator<ValidChequeHeader, ChequeReturnRequest.ChequeHeaderDto> {

    @Override
    public boolean isValid(ChequeReturnRequest.ChequeHeaderDto chequeHeader, ConstraintValidatorContext context) {
        if (chequeHeader == null) {
            return true;
        }

        if ("CLOSED".equalsIgnoreCase(chequeHeader.getStatus())) {
            return StringUtils.isNotBlank(chequeHeader.getCloseDetail());
        }

        return true;
    }
}