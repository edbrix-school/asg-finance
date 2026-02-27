package com.asg.finance.validator;

import com.asg.finance.annotation.ValidPaymentDetails;
import com.asg.finance.dto.GeneralReceiptPaymentDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class PaymentDetailsValidator
        implements ConstraintValidator<ValidPaymentDetails, GeneralReceiptPaymentDto> {

    @Override
    public boolean isValid(GeneralReceiptPaymentDto payment,
                           ConstraintValidatorContext context) {

        if (payment == null || payment.getType() == null) {
            return true;
        }

        String type = payment.getType();

        context.disableDefaultConstraintViolation();

        switch (type) {

            case "CHEQUE":
                if (!StringUtils.hasText(payment.getChequeNo())) {
                    addError(context, "Cheque number is required for CHEQUE payment type", "chequeNo");
                    return false;
                }
                if (payment.getChequeDate() == null) {
                    addError(context, "Cheque date is required for CHEQUE payment type", "chequeDate");
                    return false;
                }
                if (payment.getBankPoid() == null) {
                    addError(context, "Bank is required for CHEQUE payment type", "bankPoid");
                    return false;
                }
                break;

            case "TT":
                if (!StringUtils.hasText(payment.getTtRef())) {
                    addError(context, "TT reference is required for TT payment type", "ttRef");
                    return false;
                }
                if (payment.getTtBankPoid() == null) {
                    addError(context, "TT Bank is required for TT payment type", "ttBankPoid");
                    return false;
                }
                break;

            case "CARD":
                if (!StringUtils.hasText(payment.getCreditCardRef())) {
                    addError(context, "Card reference is required for CARD payment type", "creditCardRef");
                    return false;
                }
                if (payment.getCardPoid() == null) {
                    addError(context, "Card POID is required for CARD payment type", "cardPoid");
                    return false;
                }
                break;
        }

        return true;
    }

    private void addError(ConstraintValidatorContext context,
                          String message,
                          String field) {

        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}