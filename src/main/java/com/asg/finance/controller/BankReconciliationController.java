package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;

import static com.asg.common.lib.dto.response.ApiResponse.error;
import static com.asg.common.lib.dto.response.ApiResponse.success;

import java.util.Date;
import java.util.List;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/bank-reconciliation")
@RequiredArgsConstructor
@Slf4j
public class BankReconciliationController {

	private final BankReconciliationService service;
    private final LoggingService loggingService;

	@AllowedAction(UserRolesRightsEnum.VIEW)
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

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@GetMapping("/bank-info/{glPoid}")
	public ResponseEntity<?> getBankInfo(
			@Parameter(description = "GL transaction POID", required = true, example = "5001") @PathVariable Long glPoid){
		BankRenconciliationBankInfoDTO responses = service.getBankInfo(glPoid);

        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), glPoid.toString());
		if (responses.getBank() == null)
			return error(String.format("No bank data found for POID: %s", glPoid), 404);

		return success("Bank information fetched successfully", responses);
	}

	@AllowedAction(UserRolesRightsEnum.CREATE)
	@PostMapping("/save")
	public ResponseEntity<?> saveReconciliation(
			@Parameter(description = "Bank Reconciliation request payload", required = true) @RequestBody @Valid List<BankReconciliationRequest> dto) {
		String response = service.saveReconciliation(dto);
		if (response.toLowerCase().startsWith("error"))
			return error(response, 500);

		return success(response);

	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PostMapping("/hold")
	public ResponseEntity<?> holdCheque(
			@Parameter(description = "Bank Reconciliation request payload", required = true) @RequestBody @Valid List<BankReconcHoldAndUholdRequest> req) {
		String response = service.holdCheque(req);
		if (response.toLowerCase().startsWith("error"))
			return error(response, 500);

		return success(response);

	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PostMapping("/unhold")
	public ResponseEntity<?> unholdCheque(
			@Parameter(description = "Bank Reconciliation request payload", required = true) @RequestBody @Valid List<BankReconcHoldAndUholdRequest> req) {
		String response = service.unholdCheque(req);
		if (response.toLowerCase().startsWith("error"))
			return error(response, 500);

		return success(response);

	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
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

	@AllowedAction(UserRolesRightsEnum.CREATE)
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

	@AllowedAction(UserRolesRightsEnum.EDIT)
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

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@PostMapping("/report")
	public ResponseEntity<?> getReport(
			@Parameter(description = "Bank Reconciliation request payload", required = true) @RequestBody BankReconcileReportRequest request) {

		BankReconcileReportResponse response = service.fetchReport(request);

		return success("Report fetched successfully", response);
	}


    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for Bank Reconciliation",
            description = "Generate PDF report for a specific Bank Reconciliation transaction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Bank Reconciliation not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print")
    public ResponseEntity<?> print(
            @Parameter(description = "Bank POID", required = true, example = "101") 
            @RequestParam Long bankPoid,
            
            @Parameter(description = "Start date for reconciliation", required = true, example = "2025-12-01") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
            
            @Parameter(description = "End date for reconciliation", required = true, example = "2025-12-05") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTill,
            
            @Parameter(description = "Balance as per bank", required = false, example = "1000.00") 
            @RequestParam(required = false, defaultValue = "0") String balanceAsPerBank) {
        try {
            byte[] pdf = service.print(1L, bankPoid, dateFrom, dateTill, balanceAsPerBank);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=bank-reconciliation-" + 1 + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Bank Reconciliation: {}", 1, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }
}
