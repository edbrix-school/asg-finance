package com.asg.finance.dto;

import com.asg.common.lib.dto.AddressTypeMapDTO;
import com.asg.common.lib.dto.CountryDto;
import com.asg.finance.annotation.ValidateVatCrNo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@ValidateVatCrNo
public class SupplierMasterDto {
    private Long supplierPoid;
    @NotNull(message = "Group poid is required")
    private Long groupPoid;
    private String supplierCode;
    @NotBlank(message = "Supplier name is required")
    @Size(max = 100, message = "Supplier name must be at most 100 characters")
    private String supplierName;
    @Size(max = 100, message = "Supplier name2 must be at most 100 characters")
    private String supplierName2;
    @Size(max = 100, message = "Supplier type must be at most 100 characters")
    private String supplierType;
    @NotNull(message = "Supplier category poid is required")
    private Long supplierCategoryPoid;
    private SupplierCategoryDto supplierCategory;
    private Long countryPoid;
    private CountryDto country;
    private Long creditLimit;
    private Long creditPeriod;
    @Size(max = 50, message = "Cr no must be at most 50 characters")
    private String crNo;
    @Size(max = 100, message = "Contact person must be at most 50 characters")
    private String contactPerson;
    private Long addressPoid;
    private String addressName;
    private AddressMasterLightDto address;
    @Size(max = 1, message = "Active must be at most 1 characters")
    private String active;
    private Long seqNo;
    @Size(max = 250, message = "General remarks must be at most 250 characters")
    private String generalRemarks;
    @Size(max = 1, message = "Deleted must be at most 1 characters")
    private String deleted;
    @Size(max = 100, message = "Temp payment name must be at most 100 characters")
    private String tempPaymentName;
    @Size(max = 20, message = "Currency code must be at most 20 characters")
    private String currencyCode;
    private Long currencyRate;
    private CurrencyLightDto currency;
    private LocalDate rateExpiryDate;
    @NotNull(message = "Gl poid is required")
    private Long glPoid;
    private GLMasterResponseDto glMaster;
    @Size(max = 100, message = "Default weight selection method must be at most 100 characters")
    private String defaultWeightSelectionMethod;
    @Size(max = 1000, message = "Product info must be at most 1000 characters")
    private String productInfo;
    @Size(max = 100, message = "Tin number must be at most 100 characters")
    private String tinNumber;
    @Size(max = 100, message = "Tax slab must be at most 100 characters")
    private String taxSlab;
    @Size(max = 300, message = "Exemption reason must be at most 300 characters")
    private String exemptionReason;
    private LocalDate taxRegisteredDate;
    private Long purchaserPoid;
    private HrEmployeeDto purchaser;
    private Long grnCreditGl;
    private LocalDate auditedYear;
    @Size(max = 1000, message = "Auditing firm must be at most 1000 characters")
    private String auditingFirm;
    @Size(max = 1000, message = "Iso certification must be at most 1000 characters")
    private String isoCertification;
    @Size(max = 100, message = "Profile updated must be at most 100 characters")
    private String profileUpdated;
    @Size(max = 100, message = "Profile vat cr mismatch must be at most 100 characters")
    private String profileVatCrMismatch;
    private Long customerPoid;
    private SalesCustomerMasterDto customer;
    @Valid
    private AddressTypeMapDTO addressTypeMap;
    @Valid
    private List<SupplierMasterQstnDtlDto> questionaries;
    @Valid
    private List<SupplierMasterPaymentDtlDto> paymentDtl;
    @Valid
    private List<SupplierMasterManagementDtlDto> managementDtl;
    @Valid
    private List<SupplierMasterServiceDtlDto> serviceDtl;
}
