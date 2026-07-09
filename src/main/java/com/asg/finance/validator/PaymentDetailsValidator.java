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

        // Skip validation for deleted or unchanged rows
        if (payment.getActionType() != null) {
            String action = payment.getActionType().toUpperCase();
            if ("ISDELETED".equals(action) || "NOCHANGE".equals(action)) {
                return true;
            }
        }

        String type = payment.getType();

        context.disableDefaultConstraintViolation();

        // Validate chequeNo format regardless of payment type (if provided)
        if (StringUtils.hasText(payment.getChequeNo()) && !payment.getChequeNo().matches("^\\d{1,6}$")) {
            addError(context, "Cheque/Card number must be numeric and max 6 digits", "chequeNo");
            return false;
        }

        switch (type) {

            case "CHEQUE":
                if (!StringUtils.hasText(payment.getChequeNo())) {
                    addError(context, "Cheque number is required for CHEQUE payment type", "chequeNo");
                    return false;
                }
                if (payment.getBankPoid() == null) {
                    addError(context, "Bank is required for CHEQUE payment type", "bankPoid");
                    return false;
                }
                if (payment.getChequeDate() == null) {
                    addError(context, "Cheque date is required for CHEQUE payment type", "chequeDate");
                    return false;
                }
                if (payment.getAccountName() == null || payment.getAccountName().isEmpty()) {
                    addError(context, "Account name is required for CHEQUE payment type", "accountName");
                    return false;
                }
                if (payment.getAccountNumber() == null || payment.getAccountNumber().isEmpty()) {
                    addError(context, "Account number is required for CHEQUE payment type", "accountNumber");
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
                if (payment.getCardPoid() == null) {
                    addError(context, "Card is required for CARD payment type", "cardPoid");
                    return false;
                }
                if (payment.getCardType() == null|| payment.getCardType().isEmpty()) {
                    addError(context, "Card type is required for CARD payment type", "cardType");
                    return false;
                }
                if (!StringUtils.hasText(payment.getCreditCardRef())) {
                    addError(context, "Card reference is required for CARD payment type", "creditCardRef");
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
