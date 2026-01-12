package com.asg.finance.service;

import java.util.Map;

import org.springframework.data.domain.Pageable;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.ComputeTotalsRequest;
import com.asg.finance.dto.ComputeTotalsResponse;
import com.asg.finance.dto.CreateExpenseReallocationRequest;
import com.asg.finance.dto.ExpenseReallocationConfigResponse;
import com.asg.finance.dto.ExpenseReallocationResponse;
import com.asg.finance.dto.UpdateExpenseReallocationRequest;
import com.asg.finance.dto.ValidateAllocationResponse;

public interface ExpenseReallocationService {

	ExpenseReallocationResponse createExpenseReallocation(CreateExpenseReallocationRequest request, Long groupPoid,
			Long companyPoid, String userId);

	ExpenseReallocationResponse getExpenseReallocationById(Long transactionPoid, Long groupPoid);

	ExpenseReallocationResponse updateExpenseReallocation(Long transactionPoid,
			UpdateExpenseReallocationRequest request, Long groupPoid, String userId);

	void deleteExpenseReallocation(Long transactionPoid, Long groupPoid);

	Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, Pageable pageable);

	Map<String, String> createJv(Long transactionPoid, Long groupPoid, Long companyPoid, Long userId);

	String generateReport(Long transactionPoid, Long groupPoid, String userId);

	ValidateAllocationResponse validateAllocation(Long transactionPoid, Long groupPoid);

	ComputeTotalsResponse computeTotals(ComputeTotalsRequest request);

	ExpenseReallocationConfigResponse getConfig();
}
