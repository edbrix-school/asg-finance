package com.asg.finance.dto;

import com.asg.common.lib.dto.DetailsDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CostCenterRequestDTO {
    @NotBlank(message = "Cost Center Code is required")
    private String costCenterCode;

    @NotBlank(message = "Cost Center Description is required")
    private String costCenterDescription;

    private String costCenterDescription2;

    @NotBlank(message = "Cost Center Type is required")
    private String costCenterType;

    private Long parentCostCenterPoid;

    private DetailsDto parentCostCenterPoidDtl;

    private Long groupPoid;

    @Size(max = 100, message = "Remarks must be at most 100 characters")
    private String remarks;

    @Size(max = 1, message = "active must be at most 1 character")
    private String active = "Y";

    @Max(value = 99999, message = "Seq.No cannot be more than 5 digits")
    private Integer seqNo;

}
