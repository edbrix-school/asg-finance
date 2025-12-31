package com.asg.finance.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ChequeHeaderValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidChequeHeader {
    String message() default "closeDetail is mandatory when status is CLOSED";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}