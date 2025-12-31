package com.asg.finance.controller;

import static com.asg.common.lib.dto.response.ApiResponse.error;
import static com.asg.common.lib.dto.response.ApiResponse.success;

import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.asg.finance.dto.BankReconcHoldAndUholdRequest;
import com.asg.finance.dto.BankReconcileReportRequest;
import com.asg.finance.dto.BankReconcileReportResponse;
import com.asg.finance.dto.BankReconciliationRequest;
import com.asg.finance.dto.BankReconciliationResponse;
import com.asg.finance.dto.BankRenconciliationBankInfoDTO;
import com.asg.finance.service.BankReconciliationService;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/bank-reconciliation")
@RequiredArgsConstructor
@Slf4j
public class BankReconciliationController {

	private final BankReconciliationService service;

	@GetMapping("/view")
	public ResponseEntity<?> getReconciliationView(
			@Parameter(description = "Group POID", required = true, example = "1") @RequestParam Long groupPoid,

			@Parameter(description = "Company POID", required = true, example = "1") @RequestParam Long companyPoid,

			@Parameter(description = "Bank POID", required = true, example = "101") @RequestParam Long bankPoid,

			@Parameter(description = "Start date for reconciliation", required = true, example = "2025-12-01") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,

			@Parameter(description = "End date for reconciliation", required = true, example = "2025-12-05") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTill,

			@Parameter(description = "Cheque number (optional)", required = false, example = "CHQ12345") @RequestParam(required = false) String chequeNo,

			@Parameter(description = "Reconcile cheque filter (optional)", required = false, example = "Y") @RequestParam(required = false) String reconcileCheque,

			@Parameter(description = "Bank reconciliation type (optional)", required = false, example = "TYPE1") @RequestParam(required = false) String brType) {
		List<BankReconciliationResponse> responses = service.getReconciliationView(groupPoid, companyPoid, bankPoid,
				dateFrom, dateTill, chequeNo, reconcileCheque, brType);

		return success("Bank reconciliation details fetched successfully", responses);

	}

	@GetMapping("/bank-info/{glPoid}")
	public ResponseEntity<?> getBankInfo(
			@Parameter(description = "GL transaction POID", required = true, example = "5001") @PathVariable Long glPoid){
		BankRenconciliationBankInfoDTO responses = service.getBankInfo(glPoid);

		if (responses.getBank() == null)
			return error(String.format("No bank data found for POID: %s", glPoid), 404);

		return success("Bank information fetched successfully", responses);
	}

	@PostMapping("/save")
	public ResponseEntity<?> saveReconciliation(
			@Parameter(description = "Bank Reconciliation request payload", required = true) @RequestBody List<BankReconciliationRequest> dto) {
		String response = service.saveReconciliation(dto);
		if (response.toLowerCase().startsWith("error"))
			return error(response, 500);

		return success(response);

	}

	@PostMapping("/hold")
	public ResponseEntity<?> holdCheque(
			@Parameter(description = "Bank Reconciliation request payload", required = true) @RequestBody List<BankReconcHoldAndUholdRequest> req) {
		String response = service.holdCheque(req);
		if (response.toLowerCase().startsWith("error"))
			return error(response, 500);

		return success(response);

	}

	@PostMapping("/unhold")
	public ResponseEntity<?> unholdCheque(
			@Parameter(description = "Bank Reconciliation request payload", required = true) @RequestBody List<BankReconcHoldAndUholdRequest> req) {
		String response = service.unholdCheque(req);
		if (response.toLowerCase().startsWith("error"))
			return error(response, 500);

		return success(response);

	}

	@PostMapping("/update-statement-date")
	public ResponseEntity<?> updateStatementDate(
			@Parameter(description = "Company POID", required = true, example = "1") @RequestParam Long companyPoid,

			@Parameter(description = "Bank POID", required = true, example = "101") @RequestParam Long bankPoid,

			@Parameter(description = "Posted by user POID", required = true, example = "1001") @RequestParam Long postedBy,

			@Parameter(description = "Statement date to update", required = true, example = "2025-12-05") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date statementDate) {
		String response = service.updateStatementDate(companyPoid, postedBy, bankPoid, statementDate);
		if (response.toLowerCase().startsWith("error"))
			return error(response, 500);

		return success(response);

	}

	@PostMapping("/poll-refresh")
	public ResponseEntity<?> pollAutoRefresh(
			@Parameter(description = "User ID for polling", required = true) @RequestParam String userId,

			@Parameter(description = "Company POID", required = true, example = "101") @RequestParam Long companyPoid,

			@Parameter(description = "Login URL", required = true) @RequestParam String loginUrl) {
		String response = service.pollAutoRefresh(userId, companyPoid, loginUrl);
		if (response.toLowerCase().startsWith("error"))
			return error(response, 500);

		return success(response);
	}

	@PostMapping("/revert")
	public ResponseEntity<?> revertReconciliation(
			@Parameter(description = "Document ID of the transaction", required = true, example = "101") @RequestParam String docId,

			@Parameter(description = "Transaction POID", required = true, example = "5001") @RequestParam String transactionPoid,

			@Parameter(description = "Login user POID", required = true, example = "1001") @RequestParam Long loginUserPoid,

			@Parameter(description = "Login group POID", required = true, example = "1") @RequestParam Long loginGroupPoid,

			@Parameter(description = "Login company POID", required = true, example = "101") @RequestParam Long loginCompanyPoid,

			@Parameter(description = "Mail alert flag", required = true, example = "Y") @RequestParam String mailAlert) {
		String response = service.revertReconciliation(docId, transactionPoid, loginUserPoid, loginGroupPoid,
				loginCompanyPoid, mailAlert);
		if (response.toLowerCase().startsWith("error"))
			return error(response, 500);

		return success(response);
	}

	@PostMapping("/report")
	public ResponseEntity<?> getReport(
			@Parameter(description = "Bank Reconciliation request payload", required = true) @RequestBody BankReconcileReportRequest request) {

		BankReconcileReportResponse response = service.fetchReport(request);

		return success("Report fetched successfully", response);
	}
}
