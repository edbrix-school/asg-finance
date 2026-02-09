package com.asg.finance.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateCostCenterCodeRequest {
	private String costCenterCode;
	private String costCenterDescription;

	@AssertTrue(message = "Either costCenterCode or costCenterDescription must be provided")
    public boolean isAtLeastOnePresent() {
        return (costCenterCode != null && !costCenterCode.trim().isEmpty()) ||
               (costCenterDescription != null && !costCenterDescription.trim().isEmpty());
    }
}
