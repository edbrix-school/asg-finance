package com.asg.finance.dto.masters;

import com.asg.common.lib.dto.DetailsDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneralInfoDto {

    @NotBlank(message = "FA Owner is mandatory")
    @Size(max = 100, message = "FA Owner must not exceed 100 characters")
    private String faOwner;

    private Long employeePoid;
    private DetailsDto employeePoidDet;

    @NotBlank(message = "Model No is mandatory")
    @Size(max = 100, message = "Model No must not exceed 100 characters")
    private String modelNo;

    @Size(max = 100, message = "Serial No must not exceed 100 characters")
    private String serialNo;

    @Size(max = 100, message = "FA Color must not exceed 100 characters")
    private String faColor;

    @Size(max = 100, message = "FA Size must not exceed 100 characters")
    private String faSize;

    @Size(max = 100, message = "Country of Origin must not exceed 100 characters")
    private String countryOfOrgin;

    private Long supplierPoid;
    private DetailsDto supplierPoidDet;

    @Size(max = 3000, message = "System Specifications must not exceed 3000 characters")
    private String systemSpecifications;

    @NotNull(message = "Purchase Date is mandatory")
    private LocalDate purchaseDate;

    @NotBlank(message = "Invoice No is mandatory")
    @Size(max = 100, message = "Invoice No must not exceed 100 characters")
    private String invoiceNo;

    @Size(max = 100, message = "Warranty Period must not exceed 100 characters")
    private String warrantyPeriod;

    @Size(max = 300, message = "Maintenance Contract must not exceed 300 characters")
    private String maintenaceContract;

    @Size(max = 100, message = "AMC Supplier must not exceed 100 characters")
    private String amcSupplier;

    private LocalDate contractExpiry;

    @NotNull(message = "Gross Value is mandatory")
    private BigDecimal grossValue;

    @Size(max = 3000, message = "Software Details must not exceed 3000 characters")
    private String softwareDetails;

    @Size(max = 100, message = "Brand must not exceed 100 characters")
    private String brand;
    private LocalDate makeDate;
    
    private String pjDocRef;
    private Long pjTransactionPoid;
    private Long pjCompanyPoid;
    private String pjInvNo;
}
