package com.asg.finance.annotation;

import com.asg.finance.validator.PaymentDetailsValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PaymentDetailsValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPaymentDetails {

    String message() default "Invalid payment details";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}