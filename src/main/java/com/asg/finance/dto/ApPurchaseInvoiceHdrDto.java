package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApPurchaseInvoiceHdrDto {
    private Long transactionPoid;
    private LocalDate transactionDate; 
    private Long groupPoid;
    private String docRef;
    private String poRef;
    private String fdaRef;
    private String ffRef;
    private String shipRef;
    private Long companyPoid;
    private String currencyCode;
    private Long currencyRate;
    @NotNull(message = "Supplier Poid cannot be null")
    private Long supplierPoid;
    private Long locationPoid;
    private Long subTotal;
    private Long discount;
    private Long expenseBySupplier;
    private Long grandTotal;
    @NotBlank(message = "Remarks cannot be blank")
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String deleted;
    private Long itemTotal;
    private Long chargeTotal;
    private Long glTotal;
    private String type;
    private String description;
    private Long creditPeriod;
    private LocalDate dueDate;
    private String invnoOld;
    private String modcodeOld;
    private String refType;
    private String salesQtnPoid;
    @NotBlank(message = "Narration cannot be blank")
    private String narration;
    @NotNull(message = "Supplier Invoice Date cannot be null")
    private LocalDate supplierInvDate;
    private String supplierInvNo;
    private String supplierInvRemark;
    private String mtaRef;
    private String multiCompany;
    private Long bhdAmount;
    private Long supplierInvAmount;
    private Long roundingAmount;
    private String billType;
    private String provisionalInvoice;
    @NotBlank(message = "Party Type cannot be blank")
    private String partyType;
    private Long grnSupplierPoid;
    private String partyTinNumber;
    private String paidAgainst;
    private String fdaCoveringRef;
    private String ConfidentialRemarks;
    private List<ApPurchaseInvoiceItemDtlDto> itemDtls;
    private List<ApPurchaseInvoiceGlDtlDto> glDtls;
    private List<ApPurchaseInvoiceAssetDtlDto> assetDtls;
    private List<ApPurchaseInvRjvDetailsDto> rjvDtls;
    private List<PurchaseInvoiceChargeDtlRequestDto> chargeDtls;


    private LovGetListDto groupDet;
    private LovGetListDto companyDet;
    private LovGetListDto supplierDet;
    private LovGetListDto locationDet;
    private LovGetListDto salesQtnDet;
    private LovGetListDto grnSupplierDet;
    private ApPurchaseJournalResponseDto ffDetails;
    private ApPurchaseJournalResponseDto fdaDetails;

}
