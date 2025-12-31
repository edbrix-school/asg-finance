package com.asg.finance.validator;

import com.asg.finance.annotation.ValidateVatCrNo;
import com.asg.finance.dto.SupplierMasterDto;
import org.apache.commons.lang3.StringUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class VatCrNoValidator implements ConstraintValidator<ValidateVatCrNo, SupplierMasterDto> {

    @Override
    public boolean isValid(SupplierMasterDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        boolean vatPresent = StringUtils.isNotBlank(dto.getTinNumber()) || StringUtils.isNotBlank(dto.getTaxSlab());

        // Enforce CR_NO presence if VAT applies
        if (vatPresent && StringUtils.isBlank(dto.getCrNo())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "crNo is required when VAT (TIN or tax slab) is present")
                    .addPropertyNode("crNo")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}

