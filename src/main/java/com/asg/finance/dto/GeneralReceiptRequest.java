package com.asg.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "General Receipt creation request")
public class GeneralReceiptRequest {

    @Valid
    @NotNull(message = "Header information is required")
    @Schema(description = "Receipt header information")
    private GeneralReceiptHeaderDto header;

    @Valid
    @NotNull(message = "At least one payment is required")
    @Schema(description = "List of payment details")
    private List<GeneralReceiptPaymentDto> payments;

    @Valid
    @Schema(description = "List of bill details (for settlement)")
    private List<GeneralReceiptBillDto> bills;

    @Valid
    @Schema(description = "List of extra charges (bank charges, round-off, etc.)")
    private List<GeneralReceiptChargeDto> extraCharges;

    @Valid
    @Schema(description = "List of advance details (advance adjustments)")
    private List<GeneralReceiptAdvanceDto> advances;
}

