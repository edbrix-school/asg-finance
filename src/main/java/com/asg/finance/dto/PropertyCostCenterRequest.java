package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class PropertyCostCenterRequest {

    private String propertyCostCenterCode;

    @NotBlank(message = "Property Cost Center Name cannot be null")
    private String propertyCostCenterName;

    private String propertyDescription;

    @NotNull(message = "Property Cost Center Name cannot be null")
    private String propertyType;

    private Long parentPropertyPoid;

    @NotNull(message = "Cost Center cannot be null")
    private Long costCenterPoid;

    private Long companyPoid;
    private String remarks;
    private Integer seqNo;

    @Size(max = 1, message = "active must be at most 1 character")
    private String active = "Y";
}
