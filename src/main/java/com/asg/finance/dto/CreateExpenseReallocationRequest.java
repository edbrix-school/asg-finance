package com.asg.finance.dto;

import java.sql.Timestamp;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpenseReallocationRequest {

	@NotNull(message = "Transaction Date is required")
	private Timestamp transactionDate;

	@NotNull(message = "Company POID is required")
	private Long companyPoid;

	@NotNull(message = "Expense Group GL is required")
	private Long expenseGroupGlId;

	@NotNull(message = "From Company is required")
	private Long fromCompanyId;

	@Size(max = 1000, message = "Narration must not exceed 1000 characters")
	private String narration;

	private Timestamp fromDate;

	private Timestamp toDate;

	@Size(max = 100, message = "Allocation Type must not exceed 100 characters")
	private String allocationType;

	@Size(max = 100, message = "Cost POID must not exceed 100 characters")
	private String costPoid;

	@Size(max = 1000, message = "Remarks must not exceed 1000 characters")
	private String remarks;

	@Size(max = 25, message = "Doc Ref must not exceed 25 characters")
	private String docRef; // Optional - auto-generated if not provided

	@Valid
	@NotNull(message = "Details are required")
	private List<ExpenseReallocationDetailRequest> details;

	@Valid
	private List<ExpenseReallocationXlDetailRequest> xlDetails; // Optional
}
