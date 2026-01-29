package com.asg.finance.dto;

import com.asg.finance.config.ThreeDecimalSerializer;
import com.asg.common.lib.dto.LovGetListDto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ApPurchaseCnHdrDto {
    
    private Long transactionPoid;
    
    @NotNull(message = "Date is required")
    private LocalDate transactionDate;
    
    private String docRef;
    private String poRef;
    private Long groupPoid;
    private Long companyPoid;
    
    @NotBlank(message = "Party type is required")
    private String partyType; // SUPPLIER or PRINCIPAL
    
    private Long supplierPoid; // When party = SUPPLIER
    private Long principalPoid; // When party = PRINCIPAL
    
    @NotBlank(message = "Currency is required")
    private String currencyCode;
    
    @NotNull(message = "Rate is required")
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal currencyRate;
    
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal bhdAmount; // Auto-calculated
    
    @NotNull(message = "Credit note amount is required")
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal supplierCnAmount;
    
    private String supplierCnNo;
    private LocalDate supplierCnDate;
    private String supplierCnRemark;
    private String partyTinNumber;

    @NotBlank(message = "Reference type is required")
    private String refType; // GENERAL, FF, PJ_REVERSAL, GENERAL_PO
    
    private Long ffRef; // When refType = FF
    private Long pjReversalRef; // When refType = PJ_REVERSAL
    private String pjReversalRefType; // MTA/FF/FDA
    private String pjReversalRefDetails;
    private String fdaCoveringRef;
    private Boolean provisionalInvoice;
    
    @NotBlank(message = "Narration is required")
    private String narration;
    
    private String remarks;
    private String description;
    private String type;
    
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal subTotal;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal discount;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal grandTotal;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal itemTotal;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal chargeTotal;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal glTotal;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal roundingAmount;
    
    private Long creditPeriod;
    private LocalDate dueDate;
    
    private Boolean multiCompany;
    
    private String createdBy;
    private Timestamp createdDate;
    private String lastModifiedBy;
    private Timestamp lastModifiedDate;
    
    @Valid
    private List<ApPurchaseCnItemDtlDto> itemDetails; // For MTA PO
    
    @Valid
    private List<ApPurchaseCnChargeDtlDto> chargeDetails; // For FF/FDA
    
    @Valid
    private List<ApPurchaseCnGlDtlDto> glDetails; // For GENERAL/GENERAL_PO
    
    private LovGetListDto partyDet;
}
