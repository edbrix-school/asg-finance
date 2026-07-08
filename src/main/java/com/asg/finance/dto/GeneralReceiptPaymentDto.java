package com.asg.finance.dto;

import com.asg.finance.annotation.ValidPaymentDetails;
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

@ValidPaymentDetails
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment details for General Receipt")
public class GeneralReceiptPaymentDto {

    @Schema(description = "Detail row ID (for updates)", example = "1")
    private Long detRowId;

    @NotBlank(message = "Payment type is required")
    @Pattern(regexp = "CASH|CHEQUE|TT|CARD|ROUNDOFF",
             message = "Payment type must be CASH, CHEQUE, TT, CARD, or ROUNDOFF")
    @Schema(description = "Payment type", example = "CHEQUE", required = true, 
            allowableValues = {"CASH", "CHEQUE", "TT","CARD", "ROUNDOFF"})
    private String type;

    @NotNull(message = "Payment amount is required")
    @Schema(description = "Payment amount", example = "1000", required = true)
    private BigDecimal amount;

    @Pattern(regexp = "^\\d{1,6}$", message = "Cheque/Card number must be numeric and max 6 digits")
    @Schema(description = "Cheque/Card number", example = "100245")
    private String chequeNo;

    @Schema(description = "Cheque date", example = "2025-09-10")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate chequeDate;

    @Schema(description = "Bank name/description", example = "NATIONAL BANK OF BAHRAIN")
    private String bank;

    @Schema(description = "Bank POID (for cheques/TT)", example = "150")
    private Long bankPoid;

    @Schema(description = "Account number", example = "001123456789")
    private String accountNumber;

    @Schema(description = "Account name", example = "ABC Trading Co")
    private String accountName;

    @Schema(description = "Account POID", example = "2500")
    private Long accountPoid;

    @Schema(description = "TT Bank POID (for TT payments)", example = "250")
    private Long ttBankPoid;

    @Schema(description = "TT Reference", example = "TT-REF-12345")
    private String ttRef;

    @Schema(description = "Credit card reference", example = "CC-REF-12345")
    private String creditCardRef;

    @Schema(description = "Card type (for card payments)", example = "VISA")
    private String cardType;

    @Schema(description = "Card POID (for card payments)", example = "300")
    private Long cardPoid;

    @Schema(description = "Action type for frontend operations", example = "ISCREATED", allowableValues = {"ISCREATED", "ISUPDATED", "ISDELETED"})
    private String actionType;
}

