package com.asg.finance.dto.masters;

import com.asg.common.lib.dto.LovGetListDto;
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
public class InsuranceMasterResponseDto {
    private Long insurancePoid;
    private String docRef;
    private Long groupPoid;
    private Long companyPoid;
    private String insuranceType;
    private String insuranceCategory;
    private String policyNo;
    private String insuranceProvider;
    private LocalDate fromDate;
    private LocalDate expiryDate;
    private String status;
    private Long currency;
    private BigDecimal rate;
    private BigDecimal insuranceAmount;
    private BigDecimal premiumAmount;
    private String paymentFrequency;
    private String oneTime;
    private String description;
    private Long faPoid;
    private Long pjRefPoid;
    //private String active;
    private String deleted;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private LovGetListDto insuranceTypeDetails;
    private LovGetListDto insuranceCategoryDetails;
    private LovGetListDto insuranceProviderDetails;
    private LovGetListDto currencyDetails;
    private LovGetListDto paymentFrequencyDetails;

    private List<InsuranceEmployeeDetailResponseDto> employeeDetails;
    private List<InsurancePropertyDetailResponseDto> propertyDetails;
    private List<InsurancePicDetailResponseDto> picDetails;
    private List<InsuranceRenewalLogResponseDto> renewalLogs;
}