package com.asg.finance.dto;

import com.asg.finance.config.ThreeDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class DebitNoteHeaderDto {

    private Long transactionPoid;

    private LocalDate transactionDate;

    // These come from token, not payload
    private Long groupPoid;

    private Long companyPoid;

    @NotBlank(message = "Party type is required")
    private String partyType;

    @NotNull(message = "Party POID is required")
    private Long partyPoid;

    @NotBlank(message = "Currency code is required")
    private String currencyCode;

    @NotNull(message = "Currency rate is required")
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal currencyRate;

    @NotNull(message = "Grand total is required")
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal grandTotal;

    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal drTotal;

    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal crTotal;

    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal bhdAmount;

    @NotBlank(message = "Posting narration is mandatory")
    private String postingNarration;

    private Integer creditPeriod;

    private LocalDate dueDate;

    private Boolean multiCompany;

    private Boolean showBankDetailsInPrint;

    @NotBlank(message = "Voucher type is required")
    private String voucherType;

    @NotBlank(message = "Ref type is required")
    private String refType;

    private String docRef;

    private Long bankPoid;

    private String poRef;

    private String costRefNumber;

    private String tinNumber;

    private Long printDivisionPoid;

    private Boolean remarksPrintable;

    // FDA/FDA Direct specific fields
    private Long fdaRefPoid; // PROCESS_FDA_IN_PI
    
    private Long fdaDirectRefPoid; // PROCESS_FDA_DIRECT_IN_DN
    
    // Other Charges specific fields
    private Long costGroupPoid; // CREDIT_PARTY_TYPE
    
    private Long disposalJvRefPoid; // DISPOSAL_JV_REF_FOR_DN
    
    private String active;

    private String deleted;

    private String voyageRef;
    
    // Conditional detail lists based on refType
    @Valid
    private List<DebitNoteGlDetailDto> glDetails; // For GENERAL/CUSTOM

    @Valid
    private List<DebitNoteChargeDetailDto> chargeDetails; // For FDA/FDA_DIRECT
    
    @Valid
    private List<DebitNoteOtherChargeDto> otherChargeDetails; // For OTHER_CHARGES
}