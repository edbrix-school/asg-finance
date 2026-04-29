package com.asg.finance.validator;

import com.asg.common.lib.client.ParameterServiceClient;
import com.asg.finance.dto.BankDebitVoucherRequest;
import com.asg.finance.dto.ChargeDetailDto;
import com.asg.finance.dto.ItemDetailDto;
import com.asg.finance.dto.PaymentGlDetails;
import com.asg.finance.entity.GlBankDebitHdr;
import com.asg.finance.repository.BankDebitVoucherCustomRepository;
import com.asg.finance.repository.BankPaymentVoucherSpRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class BankDebitVoucherValidator {

    private final ParameterServiceClient parameterServiceClient;
    private final BankDebitVoucherCustomRepository bankDebitVoucherCustomRepository;
    private final BankPaymentVoucherSpRepository spRepository;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal DEFAULT_VAT_DIFF = BigDecimal.valueOf(0.01);

    /**
     * Main entry point.
     *
     * @param req   request DTO
     * @param isNew true if new record, false if update
     */
    public void validate(BankDebitVoucherRequest req, boolean isNew) {
        if (req == null) {
            throw new ValidationException("Request cannot be null");
        }

        // Basic dynamic rules
        validatePayingTypeRules(req);
        validateRefTypeRules(req);

        // Reference presence for job-related refTypes must be checked before job-status SP
        validateReferenceNotNull(req);

        // Call job-status validation SP for job related ref types
        callJobStatusValidationIfRequired(req);

        // Cross validation of PayGL & Beneficiary (DB-level SP) - always run (legacy ran this when refType present)
        callPayGlBeneficiaryValidation(req, isNew ? null : req.getTransactionPoid());

        // Other validations
        validateBeneficiaryRules(req);
        validatePayGlRules(req);
        validateChargeTypeRules(req);
        validateBankPurposeRules(req);
        validateDetailSections(req);

        // Amount totals only for GENERAL / CUSTOM
        validateAmountTotals(req);

        // VAT / Tax validations
        validateVatRules(req);
        validateInputTaxVariance(req);

        // Charge vs header amount reconciliation for FF/FDA/MTA (GAP-17)
        validateChargeAmountReconciliation(req);

        // Bank GL credit row integrity for GENERAL/CUSTOM (GAP-18)
       // validateBankGlIntegrity(req);

        // TT / value-date validations (based on paying type and system params)
        validateTtDate(req, isNew);
    }

    // -------------------------
    // 1. PAYING TYPE rules
    // -------------------------
    private void validatePayingTypeRules(BankDebitVoucherRequest req) {
        String payingType = trim(req.getPayingType());
        if (isBlank(payingType)) {
            throw new ValidationException("Paying Type is required");
        }

        // payingTo (Beneficiary A/C ID) required when PayingType = 1 or 3 (with conditions)
        // payingToName required when PayingType != 4
        if (!"4".equals(payingType) && isBlank(req.getPayingToName())) {
            throw new ValidationException("Beneficiary Name is a required field...");
        }

        // For TT (1) bank must be present (also checked in service, but double-check here)
        if ("1".equals(payingType) && req.getBankPoid() == null) {
            throw new ValidationException("Bank is required for TT payments");
        }
    }

    // -------------------------
    // 2. REF TYPE rules
    // -------------------------
    private void validateRefTypeRules(BankDebitVoucherRequest req) {
        String refType = trim(req.getRefType());
        if (isBlank(refType)) {
            throw new ValidationException("Reference Type is required");
        }

        // If payingType=3 (credit card) and refType in [FF JOBS, FDA JOBS, MTA RFQ, CUSTOM] then PayGL must NOT be provided
        String payingType = trim(req.getPayingType());
        if ("3".equals(payingType) && isRefTypeNoPayGl(refType) && req.getPayGlPoid() != null) {
            throw new ValidationException("PayGL must not be provided for this RefType with PayingType=3");
        }
    }

    private boolean isRefTypeNoPayGl(String refType) {
        if (isBlank(refType)) return false;
        String rf = refType.trim().toUpperCase();
        return rf.equals("FF JOBS") || rf.equals("FDA JOBS") || rf.equals("MTA RFQ") || rf.equals("CUSTOM");
    }

    // -------------------------
    // 3. REFERENCE presence (job refs)
    // -------------------------
    public void validateReferenceNotNull(BankDebitVoucherRequest req) {
        String refType = trim(req.getRefType());
        if ("FF JOBS".equalsIgnoreCase(refType) && isBlank(req.getFfRef())) {
            throw new ValidationException("Select a FF Ref...");
        }
        if ("FDA JOBS".equalsIgnoreCase(refType) && req.getFdaRef() == null) {
            throw new ValidationException("Select a FDA Ref...");
        }
        if ("MTA RFQ".equalsIgnoreCase(refType) && req.getSalesQtnRef() == null) {
            throw new ValidationException("Select a MTA RFQ Ref...");
        }
    }

    // -------------------------
    // 4. JOB STATUS validation (DB SP)
    // -------------------------
    private void callJobStatusValidationIfRequired(BankDebitVoucherRequest req) {
        String refType = trim(req.getRefType());
        if (req == null || refType == null) return;

        if ("FF JOBS".equalsIgnoreCase(refType) ||
                "FDA JOBS".equalsIgnoreCase(refType) ||
                "MTA RFQ".equalsIgnoreCase(refType)) {

            String refValue = resolveReferenceValue(req);
            if (isBlank(refValue)) {
                // reference presence should already have been validated above
                return;
            }

            // Call stored procedure: PROC_GL_JOB_VAL_BEFORE_SAVE
            bankDebitVoucherCustomRepository.procGlJobValBeforeSave(
                    UserContext.getGroupPoid(),
                    UserContext.getUserPoid(),
                    UserContext.getCompanyPoid(),
                    "400-111",   // docId per SRS
                    req.getRefType(),
                    refValue
            );
            // proc throws ValidationException inside implementation when status contains ERROR/WARNING/CLOSED
        }
    }

    // -------------------------
    // 5. PAYGL + BENEFICIARY cross validation (DB SP)
    // -------------------------
    private void callPayGlBeneficiaryValidation(BankDebitVoucherRequest req, Long existingTransactionPoid) {
        // Always call as legacy does when refType present (refType is required)
        bankDebitVoucherCustomRepository.procGlBankPayGlBenVal(
                UserContext.getGroupPoid(),
                UserContext.getUserPoid(),
                UserContext.getCompanyPoid(),
                "400-111",
                existingTransactionPoid,
                req.getPayingType(),
                req.getRefType(),
                req.getPayGlPoid(),
                req.getPayingTo(),    // beneficiary id / payingTo
                req.getBankPoid()
        );
        // proc implementation throws ValidationException on ERROR/WARNING
    }

    // -------------------------
    // 6. BENEFICIARY rules
    // -------------------------
    private void validateBeneficiaryRules(BankDebitVoucherRequest req) {
        String payingType = trim(req.getPayingType());

        // payingTo (Beneficiary A/C ID) required when PayingType = 1 or 3 (with conditions)
        if ("1".equals(payingType)) {
            if (isBlank(req.getPayingTo())) {
                throw new ValidationException("Beneficiary A/C (payingTo) is required for TT (PayingType=1)");
            }
        }

        if ("3".equals(payingType)) {
            // For payingType 3 (credit card), payingTo required except when refType is FF/FDA/MTA/CUSTOM
            String refType = trim(req.getRefType());
            if (!isRefTypeNoPayGl(refType) && isBlank(req.getPayingTo())) {
                throw new ValidationException("Beneficiary A/C (payingTo) is required for PayingType=3 for this RefType");
            }
        }

        // payingToName (Beneficiary Name) required when PayingType != 4
        if (!"4".equals(payingType) && isBlank(req.getPayingToName())) {
            throw new ValidationException("Beneficiary Name is a required field...");
        }

        // If payingType = 2 or 4, beneficiary IBAN not allowed (per clarifications)
        if ("2".equals(payingType) || "4".equals(payingType)) {
            if (!isBlank(req.getBeneficiaryIban())) {
                throw new ValidationException("Beneficiary IBAN is not allowed for this Paying Type");
            }
        }
    }

    // -------------------------
    // 7. PAYGL rules (header-level)
    // -------------------------
    private void validatePayGlRules(BankDebitVoucherRequest req) {
        String payingType = trim(req.getPayingType());
        String refType = trim(req.getRefType());
        Long payGl = req.getPayGlPoid();

        if ("1".equals(payingType) || "2".equals(payingType)) {
            if (payGl == null) {
                throw new ValidationException("PayGL is required for selected PayingType");
            }
        }

        if ("3".equals(payingType)) {
            if (isRefTypeNoPayGl(refType)) {
                if (payGl != null) {
                    throw new ValidationException("PayGL must not be provided for this RefType with PayingType=3");
                }
            } else {
                if (payGl == null) {
                    throw new ValidationException("PayGL is required for PayingType=3 for this RefType");
                }
            }
        }

        if ("4".equals(payingType) && payGl != null) {
            throw new ValidationException("PayGL must not be provided for PayingType=4");
        }
    }

    // -------------------------
    // 8. TT charge type rules
    // -------------------------
    private void validateChargeTypeRules(BankDebitVoucherRequest req) {
        String payingType = trim(req.getPayingType());
        if ("1".equals(payingType)) {
            if (isBlank(req.getTtChargeType())) {
                throw new ValidationException("TT Charge Type is required for TT payments");
            }
        } else {
            if (!isBlank(req.getTtChargeType())) {
                throw new ValidationException("TT Charge Type not allowed for this Paying Type");
            }
        }
    }

    // -------------------------
    // 9. Bank purpose rules
    // -------------------------
    private void validateBankPurposeRules(BankDebitVoucherRequest req) {
        String payingType = trim(req.getPayingType());
        String currency = trim(req.getCurrencyCode());

        if ("1".equals(payingType) && !"BHD".equalsIgnoreCase(currency)) {
            if (req.getBankPurposePoid() == null) {
                throw new ValidationException("Bank Purpose Code required for TT in non-BHD currency");
            }
        }
    }

    // -------------------------
    // 10. Detail sections (which tabs are required)
    // -------------------------
    private void validateDetailSections(BankDebitVoucherRequest req) {
        String refType = trim(req.getRefType());

        if ("GENERAL".equalsIgnoreCase(refType) || "CUSTOM".equalsIgnoreCase(refType)) {
            if (req.getPaymentGlDetails() == null || req.getPaymentGlDetails().isEmpty()) {
                throw new ValidationException("No Details in this Transaction...");
            }
        }

        if ("FF JOBS".equalsIgnoreCase(refType) || "FDA JOBS".equalsIgnoreCase(refType)) {
            if (req.getChargeDetails() == null || req.getChargeDetails().isEmpty()) {
                throw new ValidationException("No Details in this Transaction...");
            }
        }

        if ("MTA RFQ".equalsIgnoreCase(refType)) {
            if (req.getItemDetails() == null || req.getItemDetails().isEmpty()) {
                throw new ValidationException("No Details in this Transaction...");
            }
        }
    }

    // -------------------------
    // 11. AMOUNT totals (GENERAL / CUSTOM only)
    // -------------------------
    private void validateAmountTotals(BankDebitVoucherRequest req) {
        String refType = trim(req.getRefType());
        if (!"GENERAL".equalsIgnoreCase(refType) && !"CUSTOM".equalsIgnoreCase(refType)) {
            return; // only validate for GENERAL/CUSTOM
        }

        BigDecimal headerAmount = nvl(req.getAmount());
        if(req.getBankCharges() != null) {
            headerAmount = headerAmount.add(nvl(req.getBankCharges()));
        }
        if(req.getTaxAmount() != null) {
            headerAmount = headerAmount.add(nvl(req.getTaxAmount()));
        }
        BigDecimal totalGlDr = ZERO;
        BigDecimal totalGlCr = ZERO;

        List<PaymentGlDetails> list = req.getPaymentGlDetails();
        if (list == null || list.isEmpty()) {
            // detail presence is validated elsewhere
            return;
        }

        for (PaymentGlDetails det : list) {
            totalGlDr = totalGlDr.add(nvl(det.getDrAmt()));
            if("DR".equals(det.getType()) && det.getTaxAmount() != null) {
                totalGlDr = totalGlDr.add(det.getTaxAmount());
            }
            totalGlCr = totalGlCr.add(nvl(det.getCrAmt()));
            if("CR".equals(det.getType()) && det.getTaxAmount() != null) {
                totalGlCr = totalGlCr.add(det.getTaxAmount());
            }
        }

        // For CUSTOM: separately validate that DR entries and CR entries are both present
        if ("CUSTOM".equalsIgnoreCase(refType)) {
            if (totalGlDr.compareTo(ZERO) == 0) {
                throw new ValidationException("No Debit Entries Entered...");
            }
            if (totalGlCr.compareTo(ZERO) == 0) {
                throw new ValidationException("No Credit Entries Entered...");
            }
        }

        if (totalGlDr.compareTo(totalGlCr) != 0) {
            throw new ValidationException(String.format("Total Debit(%s) Amounts and Credit(%s) Amounts are not tallying...", totalGlDr.stripTrailingZeros().toPlainString(), totalGlCr.stripTrailingZeros().toPlainString()));
        }
    }

    // -------------------------
    // 12. VAT / Header tax & Detail-level input tax checks
    // -------------------------
    private void validateVatRules(BankDebitVoucherRequest req) {
        BigDecimal allowedDifference = parameterServiceClient.getParameterValueByNameAsDecimal("USER", "VAT_DIFFERENCE_AMOUNT_ALLOWED");
        if (allowedDifference == null) allowedDifference = DEFAULT_VAT_DIFF;

        // Detail-level VAT: for each payment GL detail, validate tax amount vs percentage on DR or CR base
        if (req.getPaymentGlDetails() != null) {
            for (PaymentGlDetails det : req.getPaymentGlDetails()) {
                BigDecimal taxPerc = det.getTaxPercentage();
                BigDecimal taxAmt = det.getTaxAmount();
                BigDecimal base = (det.getCrAmt() != null && det.getCrAmt().compareTo(ZERO) > 0) ? det.getCrAmt() : det.getDrAmt();
                if (taxPerc != null && taxAmt != null && base != null && base.compareTo(ZERO) > 0) {
                    BigDecimal expected = base.multiply(taxPerc).divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
                    BigDecimal diff = expected.subtract(taxAmt).abs();
                    if (diff.compareTo(allowedDifference) > 0) {
                        throw new ValidationException(String.format("Tax amount mismatch above allowed limit for GL POID %s", det.getGlPoid()));
                    }
                }
            }
        }

        // Header VAT validation uses bankCharges as base
        if (req.getBankCharges() != null && req.getTaxPercentage() != null && req.getTaxAmount() != null) {
            BigDecimal base = nvl(req.getBankCharges());
            if (base.compareTo(ZERO) > 0) {
                BigDecimal expected = base.multiply(req.getTaxPercentage()).divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
                BigDecimal diff = expected.subtract(req.getTaxAmount()).abs();
                if (diff.compareTo(allowedDifference) > 0) {
                    throw new ValidationException("Actual VAT amount (" + expected + "/-) and entered VAT amount (" + req.getTaxAmount() + "/-) difference between should be less than " + allowedDifference + "...");
                }
            }
        }
    }

    // Input tax variance validation (GENERAL/CUSTOM only) - legacy shows WARNING with row numbers
    private void validateInputTaxVariance(BankDebitVoucherRequest req) {
        String refType = trim(req.getRefType());
        if (!"GENERAL".equalsIgnoreCase(refType) && !"CUSTOM".equalsIgnoreCase(refType)) {
            return;
        }

        BigDecimal inputTaxLimit = parameterServiceClient.getParameterValueByNameAsDecimal("USER", "INPUT_TAX_VARIANCE_LIMIT");
        if (inputTaxLimit == null) {
            inputTaxLimit = DEFAULT_VAT_DIFF;
        }

        List<PaymentGlDetails> list = req.getPaymentGlDetails();
        if (list == null) return;

        for (int i = 0; i < list.size(); i++) {
            PaymentGlDetails det = list.get(i);
            int rowNum = i + 1;
            BigDecimal taxPerc = det.getTaxPercentage();
            BigDecimal taxAmt = det.getTaxAmount();

            if (taxPerc == null || taxAmt == null) continue;

            // DR check
            if (det.getDrAmt() != null && det.getDrAmt().compareTo(ZERO) > 0) {
                BigDecimal expected = det.getDrAmt().multiply(taxPerc).divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
                BigDecimal diff = taxAmt.subtract(expected).abs();
                if (diff.compareTo(inputTaxLimit) > 0) {
                    throw new ValidationException("WARNING : Input tax difference (" + diff + "/-) should be within " + inputTaxLimit + "/- Please note the row number " + rowNum);
                }
            }

            // CR check
            if (det.getCrAmt() != null && det.getCrAmt().compareTo(ZERO) > 0) {
                BigDecimal expected = det.getCrAmt().multiply(taxPerc).divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
                BigDecimal diff = taxAmt.subtract(expected).abs();
                if (diff.compareTo(inputTaxLimit) > 0) {
                    throw new ValidationException("WARNING : Input tax difference (" + diff + "/-) should be within " + inputTaxLimit + "/- Please note the row number " + rowNum);
                }
            }
        }
    }

    // -------------------------
    // 13. TT / Value Date validations
    // -------------------------
    private void validateTtDate(BankDebitVoucherRequest req, boolean isNew) {
        String payingType = trim(req.getPayingType());
        if (isBlank(payingType)) return;

        // TT Date is mandatory for ALL paying types — mirrors legacy DocumentBeforeSaveBillwiseCostGroups
        // (unconditional check at the end of that method regardless of paying type)
        if (req.getTtDate() == null) {
            throw new ValidationException("Value Date is a required field...");
        }

        // Compare to system date and/or existing document date
        LocalDate ttDate = req.getTtDate().toLocalDate();
        LocalDate systemDate = LocalDate.now();
        // documentDate (transaction date) is generated on backend; frontend may send documentDate optionally - doc clarified it's not used
        LocalDate docDate = req.getDocumentDate() != null ? req.getDocumentDate() : systemDate;

        if ("1".equals(payingType)) { // TT
            if (isNew) {
                // cannot be before system date
                if (ttDate.isBefore(systemDate)) {
                    throw new ValidationException("Value Date should not be before the System date...");
                }
                // post-dated limit
                Integer postDays = parameterServiceClient.getParameterValueByName("USER", "TT_POST_DATE_VALIDATION_DAYS");
                if (postDays != null) {
                    long daysDiff = ChronoUnit.DAYS.between(systemDate, ttDate);
                    if (daysDiff > postDays) {
                        throw new ValidationException(String.format("Postdated entries more than %d days is not allowed for TT in the Bank Debit Voucher, Please verify the Value Date...", postDays));
                    }
                }
            } else {
                // edit -> cannot be before document date
                if (ttDate.isBefore(docDate)) {
                    throw new ValidationException("Value Date should not be before document date...");
                }
            }
        } else if ("2".equals(payingType)) { // Direct Transfer
            if (isNew) {
                if (ttDate.isBefore(docDate)) {
                    throw new ValidationException("Value Date should not be before the document date...");
                }
                Integer backDays = parameterServiceClient.getParameterValueByName("USER", "DIRECT_TRANSFER_BACK_DATE_VALIDATION_DAYS");
                if (backDays != null) {
                    long daysDiff = ChronoUnit.DAYS.between(systemDate, ttDate);
                    if (daysDiff < -Math.abs(backDays)) {
                        throw new ValidationException(String.format("Value Date is less than %d days is not allowed for Direct Transfer.", Math.abs(backDays)));
                    }
                }
                Integer postDays = parameterServiceClient.getParameterValueByName("USER", "DIRECT_TRANSFER_POST_DATE_VALIDATION_DAYS");
                if (postDays != null) {
                    long daysDiff = ChronoUnit.DAYS.between(systemDate, ttDate);
                    if (daysDiff > postDays) {
                        throw new ValidationException(String.format("Postdated entries more than %d days is not allowed for Direct Transfer in the Bank Debit Voucher, Please verify the Value Date...", postDays));
                    }
                }
            } else {
                if (ttDate.isBefore(docDate)) {
                    throw new ValidationException("Value Date should not be before document date...");
                }
            }
        } else if ("3".equals(payingType)) { // Credit Card
            Integer backDays = parameterServiceClient.getParameterValueByName("USER", "CREDIT_CARD_BACK_DATE_VALIDATION_DAYS");
            if (backDays != null) {
                LocalDate compareDate = isNew ? systemDate : docDate;
                long daysDiff = ChronoUnit.DAYS.between(compareDate, ttDate);
                if (daysDiff < -Math.abs(backDays)) {
                    throw new ValidationException(String.format("Value Date is less than %d days is not allowed for Credit Card.", Math.abs(backDays)));
                }
            }
            Integer postDays = parameterServiceClient.getParameterValueByName("USER", "CREDIT_CARD_POST_DATE_VALIDATION_DAYS");
            if (postDays != null) {
                long daysDiff = ChronoUnit.DAYS.between(systemDate, ttDate);
                if (daysDiff > postDays) {
                    throw new ValidationException(String.format("Postdated entries more than %d days is not allowed for Credit Card in the Bank Debit Voucher, Please verify the Value Date...", postDays));
                }
            }
        }
    }

    // -------------------------
    // 15. Bank GL integrity — credit row and amount balance (GAP-18)
    // -------------------------
    private void validateBankGlIntegrity(BankDebitVoucherRequest req) {
        String refType = trim(req.getRefType());
        String payingType = trim(req.getPayingType());

        // Only applies to GENERAL and CUSTOM with payment GL details present
        if (!"GENERAL".equalsIgnoreCase(refType) && !"CUSTOM".equalsIgnoreCase(refType)) return;
        if (req.getPaymentGlDetails() == null || req.getPaymentGlDetails().isEmpty()) return;
        if (req.getBankPoid() == null) return;

        // Resolve bank's associated GL POID — must not be null (GAP-18: bank must have a GL account)
        Long bankGlPoid = bankDebitVoucherCustomRepository.getBankGlPoid(req.getBankPoid());
        if (bankGlPoid == null) {
            throw new ValidationException("The selected bank has no associated GL account. Please configure the bank GL account before proceeding.");
        }

        // For GENERAL: enforce that at least one CR row maps to the bank GL
        // For CUSTOM: also requires a bank CR row
        boolean hasBankCrRow = req.getPaymentGlDetails().stream()
                .anyMatch(d -> bankGlPoid.equals(d.getGlPoid())
                        && d.getCrAmt() != null
                        && d.getCrAmt().compareTo(ZERO) > 0);

        if (!hasBankCrRow) {
            throw new ValidationException("A credit entry to the bank GL account is required in the Payment GL Details.");
        }

        // For GENERAL: enforce presence of a payGL debit row when PayingType != 4
        if ("GENERAL".equalsIgnoreCase(refType) && !"4".equals(payingType) && req.getPayGlPoid() != null) {
            boolean hasPayGlDrRow = req.getPaymentGlDetails().stream()
                    .anyMatch(d -> req.getPayGlPoid().equals(d.getGlPoid())
                            && d.getDrAmt() != null
                            && d.getDrAmt().compareTo(ZERO) > 0);

            if (!hasPayGlDrRow) {
                throw new ValidationException("A debit entry for the Pay GL account is required in the Payment GL Details.");
            }
        }

        // Validate bank GL credit total == amount + bankCharges + taxAmount (GAP-18)
        BigDecimal expectedTotal = nvl(req.getAmount())
                .add(nvl(req.getBankCharges()))
                .add(nvl(req.getTaxAmount()));

        BigDecimal bankGlCrTotal = req.getPaymentGlDetails().stream()
                .filter(d -> bankGlPoid.equals(d.getGlPoid())
                        && d.getCrAmt() != null
                        && d.getCrAmt().compareTo(ZERO) > 0)
                .map(d -> nvl(d.getCrAmt()))
                .reduce(ZERO, BigDecimal::add);

        // Allow a small rounding difference (1 unit in 3rd decimal)
        BigDecimal diff = bankGlCrTotal.subtract(expectedTotal).abs();
        if (diff.compareTo(BigDecimal.valueOf(0.001)) > 0) {
            throw new ValidationException(String.format(
                    "Bank GL credit total (%.3f) must equal Amount + Bank Charges + Tax (%.3f)",
                    bankGlCrTotal, expectedTotal));
        }
    }

    // -------------------------
    // 14. Charge/Item vs header amount reconciliation (GAP-17)
    // -------------------------
    private void validateChargeAmountReconciliation(BankDebitVoucherRequest req) {
        String refType = trim(req.getRefType());
        BigDecimal headerAmount = nvl(req.getAmount());

        if ("FF JOBS".equalsIgnoreCase(refType) || "FDA JOBS".equalsIgnoreCase(refType)) {
            if (req.getChargeDetails() == null || req.getChargeDetails().isEmpty()) return;

            // Only rows with checkAll != 'N' are counted
            BigDecimal totalCharges = req.getChargeDetails().stream()
                    .filter(c -> !"N".equalsIgnoreCase(c.getCheckAll()))
                    .map(c -> nvl(c.getChargeAmount()))
                    .reduce(ZERO, BigDecimal::add);

            if (totalCharges.compareTo(ZERO) > 0 && totalCharges.compareTo(headerAmount) != 0) {
                throw new ValidationException("Total charge amount (" + totalCharges + ") is not matched with debit voucher Amount (" + headerAmount + ")");
            }
        }

        if ("MTA RFQ".equalsIgnoreCase(refType)) {
            if (req.getItemDetails() == null || req.getItemDetails().isEmpty()) return;

            BigDecimal totalItems = req.getItemDetails().stream()
                    .map(i -> nvl(i.getTotal()))
                    .reduce(ZERO, BigDecimal::add);

            if (totalItems.compareTo(ZERO) > 0 && totalItems.compareTo(headerAmount) != 0) {
                throw new ValidationException("Total charge amount (" + totalItems + ") is not matched with debit voucher Amount (" + headerAmount + ")");
            }
        }
    }

    // -------------------------
    // Helpers & simple util
    // -------------------------
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? ZERO : v;
    }

    private LocalDate toLocalDate(java.util.Date d) {
        return d == null ? null : new java.sql.Date(d.getTime()).toLocalDate();
    }

    public String resolveReferenceValue(BankDebitVoucherRequest req) {
        String refType = trim(req.getRefType());
        if ("FF JOBS".equalsIgnoreCase(refType)) return req.getFfRef();
        if ("FDA JOBS".equalsIgnoreCase(refType)) return req.getFdaRef() != null ? req.getFdaRef().toString() : null;
        if ("MTA RFQ".equalsIgnoreCase(refType)) return req.getSalesQtnRef() != null ? req.getSalesQtnRef().toString() : null;
        return null;
    }

    private Long getOldJobPoid(GlBankDebitHdr entity, String refType) {
        return switch (refType != null ? refType.toUpperCase() : "") {
            case "FDA JOBS" -> entity.getFdaRef();
            case "FF JOBS" -> entity.getFfRef() != null ? Long.parseLong(entity.getFfRef()) : null;
            case "MTA RFQ" -> entity.getMtaRef() != null ? Long.parseLong(entity.getMtaRef()) : null;
            default -> null;
        };
    }

    public void validateVoucherStatusInNewTransaction(GlBankDebitHdr header) {
        try {
            Long refPoid = getOldJobPoid(header, header.getRefType());
            String result = spRepository.validateVoucherStatus(
                    UserContext.getGroupPoid(),
                    UserContext.getCompanyPoid(),
                    UserContext.getUserPoid(),
                    header.getDocRef(),
                    header.getRefType(),
                    refPoid != null ? String.valueOf(refPoid) : null
            );
            if (result != null && !result.equals("SUCCESS")) {
                throw new ValidationException(result);
            }
        } catch (Exception e) {
            log.error("Voucher status validation failed: {}", e.getMessage());
            throw new ValidationException("Voucher cannot be modified: " + e.getMessage());
        }
    }
}
