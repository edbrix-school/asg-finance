package com.asg.finance.validation;

import com.asg.finance.dto.CreditNoteHeaderDto;
import com.asg.finance.dto.CreditNoteGLDetailDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class CreditNoteValidator implements ConstraintValidator<ValidCreditNote, CreditNoteHeaderDto> {

    @Override
    public boolean isValid(CreditNoteHeaderDto creditNote, ConstraintValidatorContext context) {
        if (creditNote == null) {
            return true; // Let @NotNull handle null validation
        }

        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        // Validate FF Invoice reference when refType = FF_INVOICE
        if ("FF_INVOICE".equals(creditNote.getRefType())) {
            if (creditNote.getFfInvoicePoid() == null) {
                context.buildConstraintViolationWithTemplate("FF Invoice reference is mandatory for FF_INVOICE type")
                        .addPropertyNode("ffInvoicePoid")
                        .addConstraintViolation();
                isValid = false;
            }
        }

        // Validate SH Invoice reference when refType = SH_INVOICE
        if ("SH_INVOICE".equals(creditNote.getRefType())) {
            if (creditNote.getShInvoicePoid() == null) {
                context.buildConstraintViolationWithTemplate("SH Invoice reference is mandatory for SH_INVOICE type")
                        .addPropertyNode("shInvoicePoid")
                        .addConstraintViolation();
                isValid = false;
            }
        }

        // Validate DN Invoice reference when refType = DN_INVOICE
        if ("DN_INVOICE".equals(creditNote.getRefType())) {
            if (creditNote.getDnInvoicePoid() == null) {
                context.buildConstraintViolationWithTemplate("DN Invoice reference is mandatory for DN_INVOICE type")
                        .addPropertyNode("dnInvoicePoid")
                        .addConstraintViolation();
                isValid = false;
            }
            if (creditNote.getFdaRefPoid() == null) {
                context.buildConstraintViolationWithTemplate("FDA Ref Poid is mandatory for DN_INVOICE type")
                        .addPropertyNode("fdaRefPoid")
                        .addConstraintViolation();
                isValid = false;
            }
        }

        // Validate party based on party type
        if (StringUtils.hasText(creditNote.getPartyType())) {
            if (creditNote.getPartyPoid() == null) {
                String partyTypeLabel = creditNote.getPartyType().toLowerCase();
                context.buildConstraintViolationWithTemplate(partyTypeLabel + " reference is mandatory for " + creditNote.getPartyType() + " party type")
                        .addPropertyNode("partyPoid")
                        .addConstraintViolation();
                isValid = false;
            }
        }

        // Validate company when multiCompany = true
        if (null != creditNote.getMultiCompany() && creditNote.getMultiCompany()) {
            // Validate company code in GL details when multiCompany = true
            if (creditNote.getGlDetails() != null) {
                for (int i = 0; i < creditNote.getGlDetails().size(); i++) {
                    CreditNoteGLDetailDto glDetail = creditNote.getGlDetails().get(i);
                    if (null == glDetail.getCompanyPoid()) {
                        context.buildConstraintViolationWithTemplate("Company mandatory for multi-company")
                                .addPropertyNode("glDetails[" + i + "].companyCode")
                                .addConstraintViolation();
                        isValid = false;
                    }
                }
            }
        }
        
        return isValid;
    }
}