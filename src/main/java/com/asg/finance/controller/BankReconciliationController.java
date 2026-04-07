package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.excel.ExcelFileData;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.ExcelExportService;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.dto.*;
import com.asg.finance.service.BankReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.error;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/v1/bank-reconciliation")
@RequiredArgsConstructor
@Slf4j
public class BankReconciliationController {

    private final BankReconciliationService service;
    private final LoggingService loggingService;
    private final ExcelExportService excelExportService;

    private static final String ERROR = "error";

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/view")
    public ResponseEntity<?> getReconciliationView(
            @Parameter(description = "Group POID", required = true, example = "1") @RequestParam Long groupPoid,

            @Parameter(description = "Company POID", required = true, example = "1") @RequestParam Long companyPoid,

            @Parameter(description = "Bank POID", required = true, example = "101") @RequestParam Long bankPoid,

            @Parameter(description = "Start date for reconciliation", required = true, example = "2025-12-01") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,

            @Parameter(description = "End date for reconciliation", required = true, example = "2025-12-05") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateTill,

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
            @Parameter(description = "GL transaction POID", required = true, example = "5001") @PathVariable Long glPoid) {
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
        if (response.toLowerCase().startsWith(ERROR))
            return error(response, 500);

        return success(response);

    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/hold")
    public ResponseEntity<?> holdCheque(
            @Parameter(description = "Bank Reconciliation request payload", required = true) @RequestBody @Valid List<BankReconcHoldAndUholdRequest> req) {
        String response = service.holdCheque(req);
        if (response.toLowerCase().startsWith(ERROR))
            return error(response, 500);

        return success(response);

    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/unhold")
    public ResponseEntity<?> unholdCheque(
            @Parameter(description = "Bank Reconciliation request payload", required = true) @RequestBody @Valid List<BankReconcHoldAndUholdRequest> req) {
        String response = service.unholdCheque(req);
        if (response.toLowerCase().startsWith(ERROR))
            return error(response, 500);

        return success(response);

    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/update-statement-date")
    public ResponseEntity<?> updateStatementDate(
            @Parameter(description = "Company POID", required = true, example = "1") @RequestParam Long companyPoid,

            @Parameter(description = "Bank POID", required = true, example = "101") @RequestParam Long bankPoid,

            @Parameter(description = "Posted by user POID", required = true, example = "1001") @RequestParam Long postedBy,

            @Parameter(description = "Statement date to update", required = true, example = "2025-12-05") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate statementDate) {
        String response = service.updateStatementDate(companyPoid, postedBy, bankPoid, statementDate);
        if (response.toLowerCase().startsWith(ERROR))
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
        if (response.toLowerCase().startsWith(ERROR))
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
        if (response.toLowerCase().startsWith(ERROR))
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
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,

            @Parameter(description = "End date for reconciliation", required = true, example = "2025-12-05")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateTill,

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

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(summary = "Generate Excel for Bank Reconciliation")
    @GetMapping("/excel")
    public ResponseEntity<?> exportExcel(
            @Parameter(description = "End date", required = true, example = "2025-12-05")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            @Parameter(description = "Bank POID", required = true, example = "181")
            @RequestParam Long bankPoid,
            @Parameter(description = "Balance as per bank", required = true, example = "1000.00")
            @RequestParam BigDecimal balanceAsPerBank) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("P_DOC_TO_DATE", toDate);    //new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).format(toDate)
            parameters.put("P_BANK_POID", bankPoid);
            parameters.put("P_LOGIN_COMP_POID", UserContext.getCompanyPoid());
            parameters.put("P_BALANCE_BANK", balanceAsPerBank);
            ExcelFileData data = excelExportService.generateExcel("400-150", null, parameters, "BankReconciliation.xlsx");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=" + data.getFileName())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data.getContent());
        } catch (Exception e) {
            log.error("Failed to generate Excel for Bank Reconciliation", e);
            return error("Failed to generate Excel: " + e.getMessage(), 500);
        }
    }
}
