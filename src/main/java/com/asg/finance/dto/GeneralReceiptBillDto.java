package com.asg.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bill details for General Receipt (against settlement)")
public class GeneralReceiptBillDto {

    @Schema(description = "Detail row ID (for updates)", example = "1")
    private Long detRowId;

    @NotBlank(message = "Bill reference is required")
    @Schema(description = "Bill reference number", example = "INV-0045", required = true)
    private String billReference;

    @NotNull(message = "Amount is required")

    @Schema(description = "Bill amount", example = "1000", required = true)
    private BigDecimal amount;

    @NotBlank(message = "Dr/Cr type is required")
    @Pattern(regexp = "Dr|Cr|DEBIT|CREDIT", message = "Dr/Cr type must be Dr, Cr, DEBIT, or CREDIT")
    @Schema(description = "Debit or Credit", example = "Cr", required = true, allowableValues = {"Dr", "Cr", "DEBIT", "CREDIT"})
    private String drCr;

    @Schema(description = "Bill remarks", example = "Invoice INV-0045 full payment")
    private String remarks;

    @Schema(description = "Bill reference type (NEW, AGAINST, ON-ACCOUNT)", example = "AGAINST")
    private String billRefType;

    @Schema(description = "Bill due date", example = "2025-11-30")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate billDueDate;

    @Schema(description = "GL POID for the bill", example = "5001")
    private Long glPoid;

    @Schema(description = "GL Company POID (for multi-company)", example = "101")
    private Long glCompanyPoid;

    @Schema(description = "Description", example = "Payment for invoice")
    private String description;

    @Schema(description = "Action type for update operations", example = "ISCREATED", allowableValues = {"ISCREATED", "ISUPDATED", "ISDELETED"})
    private String actionType;

    private BigDecimal billOriginalAmount;
}

