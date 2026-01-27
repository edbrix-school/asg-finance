package com.asg.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralReceiptAdvanceDto {

    @Schema(description = "Detail row ID (for updates)", example = "1")
    private Long detRowId;

    private String advanceRefDocId;
    
    private Long advanceRefPoid;
    
    @NotNull(message = "Advance amount is required")
    @DecimalMin(value = "0.01", message = "Advance amount must be greater than 0")
    private BigDecimal amount;
    
    private String remarks;

    @Schema(description = "Action type for update operations", example = "ISCREATED", allowableValues = {"ISCREATED", "ISUPDATED", "ISDELETED"})
    private String actionType;
}

