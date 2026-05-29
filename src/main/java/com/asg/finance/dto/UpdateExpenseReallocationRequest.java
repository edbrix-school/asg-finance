package com.asg.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExpenseReallocationRequest {

    @NotNull(message = "Transaction Date is required")
    private LocalDate transactionDate;

    @NotNull(message = "Company POID is required")
    private Long companyPoid;

    @NotNull(message = "Expense Group GL is required")
    private Long expenseGroupGlId;

    @NotNull(message = "From Company is required")
    private Long fromCompanyId;

    @Size(max = 1000, message = "Narration must not exceed 1000 characters")
    private String narration;

    private LocalDate fromDate;

    private LocalDate toDate;

    @Size(max = 100, message = "Allocation Type must not exceed 100 characters")
    private String allocationType;

    @Size(max = 100, message = "Cost POID must not exceed 100 characters")
    private String costPoid;

    @Size(max = 100, message = "Cost Code must not exceed 100 characters")
    private String costCode;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String remarks;

    @Valid
    @NotNull(message = "Details are required")
    private List<ExpenseReallocationXlDetailRequest> details;

    private String reportGeneration;

}
