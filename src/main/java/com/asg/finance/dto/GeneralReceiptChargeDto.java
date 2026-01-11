package com.asg.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Extra charges for General Receipt (bank charges, round-off, etc.)")
public class GeneralReceiptChargeDto {

    @Schema(description = "Detail row ID (for updates)", example = "1")
    private Long detRowId;

    @NotBlank(message = "Charge type is required")
    @Schema(description = "Charge type (BANK_CHARGES, ROUND_OFF, EXCHANGE_GAIN_LOSS)", 
            example = "BANK_CHARGES", required = true)
    private String chargeType;

    @Schema(description = "GL Account Code (auto-fetched from stored procedure based on charge type)", example = "GL-5110")
    private String gl;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", message = "Amount must be non-negative")
    @Schema(description = "Charge amount", example = "2.50", required = true)
    private BigDecimal amount;

    @Schema(description = "Amount in BHD (auto-calculated: amount × currency rate)", example = "0.94")
    private BigDecimal bhdAmount;

    @Schema(description = "Tax slab code", example = "VAT5")
    private String taxSlab;

    @Schema(description = "Tax POID", example = "101")
    private Long taxPoid;

    @Schema(description = "Tax percentage", example = "5")
    private BigDecimal taxPercent;

    @Schema(description = "Tax amount", example = "0.125")
    private BigDecimal taxAmount;

    @Schema(description = "Total amount (amount + tax)", example = "2.625")
    private BigDecimal totalAmount;

    @Schema(description = "Cost center code", example = "FIN-001")
    private String costCenter;

    @Schema(description = "Remarks", example = "Bank processing charges")
    private String remarks;
}

