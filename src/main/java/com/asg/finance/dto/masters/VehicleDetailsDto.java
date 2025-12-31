package com.asg.finance.dto.masters;

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
public class VehicleDetailsDto {

    private Integer vehicleProductionYear;
    private Long registrationNo;
    private String faParentPoid;
    @Size(max = 100, message = "Model No must not exceed 100 characters")
    private String modelNo;
    @Size(max = 100, message = "Engine Chassis No must not exceed 100 characters")
    private String engineChassisNo;
    @Size(max = 100, message = "Vehicle Type must not exceed 100 characters")
    private String vehicleType;
    @Size(max = 100, message = "Insurance Coverage must not exceed 100 characters")
    private String insuranceCoverage;
    private LocalDate insuranceRenewalDate;
    @Size(max = 100, message = "Insurance Company must not exceed 100 characters")
    private String insuranceCompany;
    @Size(max = 100, message = "Insurance Policy No must not exceed 100 characters")
    private String insurancePolicyNo;
    private BigDecimal insuranceAmount;
}
