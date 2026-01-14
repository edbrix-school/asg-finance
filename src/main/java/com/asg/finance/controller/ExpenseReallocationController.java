package com.asg.finance.controller;

import static com.asg.common.lib.dto.response.ApiResponse.badRequest;
import static com.asg.common.lib.dto.response.ApiResponse.success;

import java.time.LocalDate;
import java.util.Map;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.dto.ComputeTotalsRequest;
import com.asg.finance.dto.ComputeTotalsResponse;
import com.asg.finance.dto.CreateExpenseReallocationRequest;
import com.asg.finance.dto.ExpenseReallocationConfigResponse;
import com.asg.finance.dto.ExpenseReallocationResponse;
import com.asg.finance.dto.GenerateReportResponse;
import com.asg.finance.dto.UpdateExpenseReallocationRequest;
import com.asg.finance.dto.ValidateAllocationResponse;
import com.asg.finance.service.ExpenseReallocationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/expense-reallocations")
@RequiredArgsConstructor
@Slf4j
public class ExpenseReallocationController {

	private final ExpenseReallocationService expenseReallocationService;
	private final LoggingService loggingService;

	@Operation(summary = "Create expense reallocation", description = "Creates a new expense reallocation with header and detail lines", responses = {
			@ApiResponse(responseCode = "200", description = "Successfully created expense reallocation"),
			@ApiResponse(responseCode = "400", description = "Invalid input parameters or validation error") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping
	@AllowedAction(UserRolesRightsEnum.CREATE)
	public ResponseEntity<?> createExpenseReallocation(
			@Parameter(description = "Expense reallocation creation request", required = true) @Valid @RequestBody CreateExpenseReallocationRequest request) {

		log.info("createExpenseReallocation started for groupPoid={} userId={}", UserContext.getGroupPoid(),
				UserContext.getUserId());

		ExpenseReallocationResponse response = expenseReallocationService.createExpenseReallocation(request,
				UserContext.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserId());

		log.info("createExpenseReallocation completed for transactionPoid={}",
				response != null ? response.getTransactionPoid() : null);

		return success("Expense reallocation created successfully", response);
	}

	@Operation(summary = "Get expense reallocation by ID", description = "Retrieves expense reallocation details including header, detail lines, and XL detail lines", responses = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved expense reallocation"),
			@ApiResponse(responseCode = "404", description = "Expense reallocation not found") }, security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/{transactionPoid}")
	@AllowedAction(UserRolesRightsEnum.VIEW)
	public ResponseEntity<?> getExpenseReallocationById(
			@Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {

		log.info("getExpenseReallocationById started for transactionPoid={} groupPoid={}", transactionPoid,
				UserContext.getGroupPoid());
		loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(),
				transactionPoid.toString());

		ExpenseReallocationResponse response = expenseReallocationService.getExpenseReallocationById(transactionPoid,
				UserContext.getGroupPoid());

		log.info("getExpenseReallocationById completed for transactionPoid={}", transactionPoid);

		return success("Expense reallocation retrieved successfully", response);
	}

	@Operation(summary = "Update expense reallocation", description = "Updates an existing expense reallocation. Cannot update if JV is already created.", responses = {
			@ApiResponse(responseCode = "200", description = "Successfully updated expense reallocation"),
			@ApiResponse(responseCode = "400", description = "Invalid input parameters or validation error"),
			@ApiResponse(responseCode = "404", description = "Expense reallocation not found") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PutMapping("/{transactionPoid}")
	@AllowedAction(UserRolesRightsEnum.EDIT)
	public ResponseEntity<?> updateExpenseReallocation(
			@Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
			@Parameter(description = "Expense reallocation update request", required = true) @Valid @RequestBody UpdateExpenseReallocationRequest request) {

		log.info("updateExpenseReallocation started for transactionPoid={} groupPoid={} userId={}", transactionPoid,
				UserContext.getGroupPoid(), UserContext.getUserId());

		ExpenseReallocationResponse response = expenseReallocationService.updateExpenseReallocation(transactionPoid,
				request, UserContext.getGroupPoid(), UserContext.getUserId());

		log.info("updateExpenseReallocation completed for transactionPoid={}", transactionPoid);

		return success("Expense reallocation updated successfully", response);
	}

	@Operation(summary = "Delete expense reallocation", description = "Soft deletes an expense reallocation. Cannot delete if JV is already created.", responses = {
			@ApiResponse(responseCode = "200", description = "Successfully deleted expense reallocation"),
			@ApiResponse(responseCode = "400", description = "Cannot delete expense reallocation with JV created"),
			@ApiResponse(responseCode = "404", description = "Expense reallocation not found") }, security = @SecurityRequirement(name = "bearerAuth"))
	@DeleteMapping("/{transactionPoid}")
	@AllowedAction(UserRolesRightsEnum.DELETE)
	public ResponseEntity<?> deleteExpenseReallocation(
			@Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {

		log.info("deleteExpenseReallocation started for transactionPoid={} groupPoid={}", transactionPoid,
				UserContext.getGroupPoid());

		expenseReallocationService.deleteExpenseReallocation(transactionPoid, UserContext.getGroupPoid());

		log.info("deleteExpenseReallocation completed for transactionPoid={}", transactionPoid);
		loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, UserContext.getDocumentId(),
				transactionPoid.toString());

		return success("Expense reallocation deleted successfully");
	}

	@Operation(summary = "List expense reallocations", description = "Retrieves a paginated list of expense reallocations with optional filters", responses = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved expense reallocations)")}, security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/search")
	@AllowedAction(UserRolesRightsEnum.VIEW)
	public ResponseEntity<?> getExpenseReallocations(@ParameterObject Pageable pageable,
			@RequestBody(required = false) FilterRequestDto filters,
			@RequestParam(required = false) LocalDate startDate, @RequestParam(required = false) LocalDate endDate) {
		log.info("getExpenseReallocations started for filters={} startDate={} endDate={} page={}", filters.toString(),
				startDate, endDate, pageable.toString());

		if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
			return badRequest("Both startDate and endDate should be specified or both dates should be empty.");
		}

		Map<String, Object> response = expenseReallocationService
				.listOfRecordsAndGenericSearch(UserContext.getDocumentId(), filters, startDate, endDate, pageable);

		log.info("getExpenseReallocations completed for docId={} count={}", UserContext.getDocumentId());

		return success("Expense reallocations retrieved successfully", response);
	}

	@Operation(summary = "Create JV from expense reallocation", description = "Creates Journal Voucher entries from the expense reallocation via stored procedure", responses = {
			@ApiResponse(responseCode = "200", description = "Successfully created JV"),
			@ApiResponse(responseCode = "400", description = "Invalid input parameters or validation error"),
			@ApiResponse(responseCode = "404", description = "Expense reallocation not found") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/{transactionPoid}/create-jv")
	@AllowedAction(UserRolesRightsEnum.CREATE)
	public ResponseEntity<?> createJv(
			@Parameter(description = "Transaction POID", required = true, example = "161 or 301") @PathVariable Long transactionPoid) {

		log.info("createJv started for transactionPoid={} groupPoid={}", transactionPoid, UserContext.getGroupPoid());

		Map<String, String> response = expenseReallocationService.createJv(transactionPoid, UserContext.getGroupPoid(),
				UserContext.getCompanyPoid(), UserContext.getUserPoid());

		log.info("createJv completed for transactionPoid={} jvPoid={}", transactionPoid,
				response != null ? response : null);

		return success("JV created successfully", response);
	}

	@Operation(summary = "Generate report", description = "Generates dynamic report for expense allocation", responses = {
			@ApiResponse(responseCode = "200", description = "Successfully generated report"),
			@ApiResponse(responseCode = "400", description = "Invalid input parameters or validation error"),
			@ApiResponse(responseCode = "404", description = "Expense reallocation not found") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/{transactionPoid}/generate-report")
	@AllowedAction(UserRolesRightsEnum.VIEW)
	public ResponseEntity<?> generateReport(
			@Parameter(description = "Transaction POID", required = true, example = "161 or 301") @PathVariable Long transactionPoid) {

		log.info("generateReport started for transactionPoid={} groupPoid={}", transactionPoid,
				UserContext.getGroupPoid());

		String response = expenseReallocationService.generateReport(transactionPoid, UserContext.getGroupPoid(),
				UserContext.getUserId());

		log.info("generateReport completed for transactionPoid={} reportUrl={}", transactionPoid, response);

		return success(response);
	}

	@Operation(summary = "Validate allocations", description = "Validates allocation totals and business rules before saving/posting", responses = {
			@ApiResponse(responseCode = "200", description = "Validation completed"),
			@ApiResponse(responseCode = "404", description = "Expense reallocation not found") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/{transactionPoid}/validate")
	@AllowedAction(UserRolesRightsEnum.VIEW)
	public ResponseEntity<?> validateAllocation(
			@Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {

		log.info("validateAllocation started for transactionPoid={} groupPoid={}", transactionPoid,
				UserContext.getGroupPoid());

		ValidateAllocationResponse response = expenseReallocationService.validateAllocation(transactionPoid,
				UserContext.getGroupPoid());

		log.info("validateAllocation completed for transactionPoid={} valid={}", transactionPoid,
				response != null ? response.getValid() : null);

		return success("Validation completed", response);
	}

	@Operation(summary = "Compute totals", description = "Helper endpoint to compute totals from detail lines (client-side calculation helper)", responses = {
			@ApiResponse(responseCode = "200", description = "Totals computed successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input parameters") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/compute-totals")
	@AllowedAction(UserRolesRightsEnum.VIEW)
	public ResponseEntity<?> computeTotals(
			@Parameter(description = "Compute totals request", required = true) @Valid @RequestBody ComputeTotalsRequest request) {

		log.info("computeTotals started");

		ComputeTotalsResponse response = expenseReallocationService.computeTotals(request);

		log.info("computeTotals completed");

		return success("Totals computed successfully", response);
	}

	@Operation(summary = "Get screen config", description = "Fetches screen configuration and parameters", responses = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved config") }, security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/config")
	@AllowedAction(UserRolesRightsEnum.VIEW)
	public ResponseEntity<?> getConfig() {

		log.info("getConfig started");

		ExpenseReallocationConfigResponse response = expenseReallocationService.getConfig();

		log.info("getConfig completed");

		return success("Config retrieved successfully", response);
	}
}
