package com.asg.finance.dto.masters;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceMasterRequestDto {

    @NotBlank(message = "Insurance Type is mandatory")
    private String insuranceType;

    @NotBlank(message = "Category is mandatory")
    private String category;

    @NotBlank(message = "Policy No is mandatory")
    private String policyNo;

    @NotBlank(message = "Insurance Provider is mandatory")
    private String insuranceProvider;

    @NotNull(message = "From Date is mandatory")
    private LocalDate fromDate;

    @NotNull(message = "Expiry Date is mandatory")
    private LocalDate expiryDate;

    @NotNull(message = "Currency is mandatory")
    private Long currency;

    @NotNull(message = "Rate is mandatory")
    private BigDecimal rate;

    @NotNull(message = "Insurance Amount is mandatory")
    @DecimalMin(value = "0.01", message = "Insurance Amount format is not correct")
    @Digits(integer = 15, fraction = 2, message = "Insurance Amount format is not correct")
    private BigDecimal insuranceAmount;

    @NotNull(message = "Premium Amount is mandatory")
    @DecimalMin(value = "0.01", message = "Premium Amount format is not correct")
    @Digits(integer = 15, fraction = 2, message = "Premium Amount format is not correct")
    private BigDecimal premiumAmount;

    private String paymentFrequency;

    private String oneTime;

    private String description;

    private String active;

    @Valid
    private List<InsuranceEmployeeDetailRequestDto> employeeDetails;

    @Valid
    private List<InsurancePropertyDetailRequestDto> propertyDetails;

    @Valid
    private List<InsurancePicDetailRequestDto> picDetails;
}