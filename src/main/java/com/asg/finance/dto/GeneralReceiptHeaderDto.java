package com.asg.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "General Receipt header information")
public class GeneralReceiptHeaderDto {

    @Schema(description = "Transaction POID (for updates, null for new)", example = "12345")
    private Long transactionPoid;

    @Schema(description = "Transaction date", example = "2025-10-27")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;

    @NotBlank(message = "Credit GL is required")
    @Schema(description = "Credit GL Account Code", example = "GL-1201", required = true)
    private String creditGL;

    
    @Schema(description = "Received from (customer/party name)", example = "ABC TRADING CO", required = true)
    private String receivedFrom;

    @NotBlank(message = "Currency is required")
    @Schema(description = "Currency code", example = "USD", required = true)
    private String currency;

    @NotNull(message = "Currency rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Currency rate must be greater than 0")
    @Schema(description = "Currency exchange rate", example = "0.376", required = true)
    private BigDecimal rate;

    @NotNull(message = "Receipt amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Receipt amount must be greater than 0")
    @Schema(description = "Total receipt amount", example = "1000", required = true)
    private BigDecimal receiptAmount;

    @NotBlank(message = "Ref Type is required")
    @Pattern(regexp = "GENERAL|FDA_ADVANCE", message = "Ref Type must be GENERAL, FDA_ADVANCE")
    @Schema(description = "Reference type", example = "GENERAL", required = true, allowableValues = {"GENERAL", "FDA_ADVANCE"})
    private String refType;

    @NotBlank(message = "Narration is required")
    @Schema(description = "Receipt narration/remarks", example = "Payment against invoice INV-0045", required = true)
    private String narration;

    @NotNull(message = "Company POID is required")
    @Schema(description = "Company POID", example = "101", required = true)
    private Long companyPoid;

    @Schema(description = "Multi-company flag", example = "N")
    private String multicompany;

    @Schema(description = "TT Bank POID (for TT payments)", example = "250")
    private Long ttBankPoid;

    @Schema(description = "Cost center POID", example = "FIN-001")
    private String costCenterPoid;

    @Schema(description = "Extra charges flag (Y/N)", example = "Y")
    private String extraCharges;

    @Schema(description = "printDocCompId", example = "Y")
    private Long printDocCompId;

}

