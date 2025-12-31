package com.asg.finance.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChequePrintRequest {
	@NotEmpty(message = "At least one pending cheque must be selected")
	private List<Long> pendingChequeIds;

	@Valid
	@NotEmpty(message = "At least one stock selection is required")
	private List<StockSelectionRequest> stockSelections;

	@Size(max = 1, message = "SuppressBalanceCheck must be Y or N")
	private String suppressBalanceCheck;
}
