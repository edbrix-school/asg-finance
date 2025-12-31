package com.asg.finance.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CreditNoteValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCreditNote {
    String message() default "Credit note validation failed";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}