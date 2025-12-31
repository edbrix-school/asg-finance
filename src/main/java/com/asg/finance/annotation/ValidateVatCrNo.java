package com.asg.finance.annotation;

import com.asg.finance.validator.VatCrNoValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = VatCrNoValidator.class)
@Documented
public @interface ValidateVatCrNo {
    String message() default "CR_NO is mandatory when VAT (TIN or tax slab) is present";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

