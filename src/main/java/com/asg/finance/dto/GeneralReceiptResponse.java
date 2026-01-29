package com.asg.finance.dto;

import com.asg.finance.config.ThreeDecimalSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "General Receipt response")
public class GeneralReceiptResponse {

    @Schema(description = "Receipt status", example = "SUCCESS")
    private String status;

    @Schema(description = "Status message", example = "General Receipt created successfully")
    private String message;

    @Schema(description = "Receipt number/document reference", example = "ASGGEN32904")
    private String receiptNo;

    @Schema(description = "Transaction POID", example = "12345")
    private Long transactionPoid;

    @Schema(description = "Transaction date", example = "2025-10-27")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;

    @Schema(description = "Company POID", example = "101")
    private Long companyPoid;

    @Schema(description = "Receipt amount", example = "1000")
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal receiptAmount;

    @Schema(description = "Currency code", example = "USD")
    private String currencyCode;

    @Schema(description = "Currency rate", example = "0.376")
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal currencyRate;

    @Schema(description = "BHD equivalent amount (receiptAmount * currencyRate)", example = "376.000")
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal bhdAmount;

    @Schema(description = "Received from", example = "ABC TRADING CO")
    private String receivedFrom;

    @Schema(description = "Credit GL details")
    private CreditGlDto creditGL;

    @Schema(description = "Reference type", example = "AGAINST")
    private String refType;

    @Schema(description = "doc ref", example = "ASGGEN33472")
    private String docRef;

    @Schema(description = "Narration", example = "Payment against invoice INV-0045")
    private String narration;

    @Schema(description = "Print Title/Print Document Company ID", example = "NSA")
    private String printTitle;

    @Schema(description = "Approval status", example = "Final Approval Completed")
    private String approvalStatus;

    @Schema(description = "Verified flag (Y/N)", example = "Y")
    private String verified;

    @Schema(description = "Multi-company flag (Y/N)", example = "N")
    private String multicompany;

    @Schema(description = "Created by user", example = "john.doe")
    private String createdBy;

    @Schema(description = "Created date", example = "2025-10-27T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    @Schema(description = "printDocCompId", example = "123")
    private Long printDocCompId;

    @Schema(description = "Payment details")
    private List<GeneralReceiptPaymentDto> payments;

    @Schema(description = "Bill details")
    private List<GeneralReceiptBillDto> bills;

    @Schema(description = "Extra charges")
    private List<GeneralReceiptChargeDto> extraCharges;

    @Schema(description = "Extra charges flag (Y/N)", example = "Y")
    private String extraChargesFlag;

    @Schema(description = "Advance details")
    private List<GeneralReceiptAdvanceDto> advances;
}

