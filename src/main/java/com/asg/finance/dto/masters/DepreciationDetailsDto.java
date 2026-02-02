package com.asg.finance.dto.masters;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepreciationDetailsDto {

	@Size(max = 1, message = "Depreciable must not exceed 1 character")
	private String depreciable = "Y";

	@Size(max = 20, message = "Depreciation Method must not exceed 20 characters")
	private String depreciationMethod;

	private LocalDate depreciationStartDate;

	@NotNull(message = "Depreciation Percent is mandatory")
	private BigDecimal depreciationPercent;

	@NotBlank(message = "Asset Life is mandatory")
	@Size(max = 20, message = "Asset Life must not exceed 20 characters")
	private String assetLife;

	private LocalDate scrapDate;

	private BigDecimal scrapValue;

	@NotNull(message = "FA GL Account is mandatory")
	private Long faGlAccount;

	@NotNull(message = "FA Accumulation Account is mandatory")
	private Long faAccumulationAc;

	@NotNull(message = "FA Depreciation Account is mandatory")
	private Long faDepreciationAc;

	@Size(max = 20, message = "Cost Center must not exceed 20 characters")
	private String costCenter;
}
