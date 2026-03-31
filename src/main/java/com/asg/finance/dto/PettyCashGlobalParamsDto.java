package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PettyCashGlobalParamsDto {
    // Defaults for new voucher
    private String defaultPayingTo;           // PETTY_CASH_DEFAULT_PAYING_TO
    private String defaultRefType;            // DEFAULT_PETTY_CASH_REF_TYPE
    // UI visibility
    private boolean vatRelatedFieldsVisible;  // PETTY_CASH_GL_VAT_RELATED_FIELDS
    // Validation limits
    private BigDecimal roundingLimit;         // ROUNDING_LIMIT
    private BigDecimal vatAmountLimit;        // PETTY_CASH_VAT_AMOUNT_LIMIT
    private BigDecimal inputTaxVarianceLimit; // INPUT_TAX_VARIANCE_LIMIT
    // Auto-behaviour
    private String mtaPettyCashGlCode;        // MTA_PETTY_CASH_GL_CODE
    private Long advanceLedgerGlPoid;         // PETTY_CASH_ADVANCE_LEDGER (per petty-cash GL)
    private Long pettyCashLedgerGlPoid;       // PETTY_CASH_LEDGER (per petty-cash GL)
    private boolean advRefundAutoApproval;    // PETTY_CASH_ADV_REFND_APPR_SUBMN
}
