package com.asg.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class BankPaymentGLDetailRequest {

    // === Action Type ===
    private String actionType;          // "isCreated", "isUpdated", "isDeleted", "noChanges"

    // === Link fields ===
    private Long transactionPoid;       // FK → GL_BANK_PAYMENT_HDR.TRANSACTION_POID
    private Long detRowId;              // Row number (auto/incremental in UI)

    // === GL & Company Info ===
    private String type;                // DR / CR
    private Long companyPoid;           // COMPANY_POID (from Company dropdown)
    private Long glPoid;                // GL_POID (from GL search field)

    // === Financial values ===
    private BigDecimal drAmt;               // Debit Amount
    private BigDecimal crAmt;               // Credit Amount
    private Long taxPoid;               // TAX_POID (Tax Slab selected)
    private BigDecimal taxPercentage;       // TAX_PERCENTAGE
    private BigDecimal taxAmount;           // TAX_AMOUNT
    private BigDecimal totalAmount;         // TOTAL_AMOUNT (DR/CR + tax)

    // === Invoice Info ===
    private String partyInvNumber;      // VAT Inv Number
    private LocalDate partyInvDate;     // Invoice Date

    // === Metadata ===
    private String remarks;// Remarks (text field)

    private List<BillwiseBreakupPopupRequestDto> billWiseBreakup;
    private List<CostCenterBreakupPopupRequestDto> costCenterBreakup;

}
