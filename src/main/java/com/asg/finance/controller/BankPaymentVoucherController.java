package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.dto.*;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.service.BankPaymentVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.Date;

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;
@Slf4j
@RestController
@RequestMapping("/v1/bank-payment-vouchers")
@RequiredArgsConstructor
public class BankPaymentVoucherController {

    private final BankPaymentVoucherService service;
    private final LoggingService loggingService;

    @Operation(
            summary = "Get Bank Payment Voucher by ID",
            description = "Fetches a Bank Payment Voucher with header and all related details (GL / Charge / Item) based on Ref Type.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the Bank Payment Voucher",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BankPaymentVoucherResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Voucher not found"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getVoucherById(
            @Parameter(description = "Unique ID of the voucher to fetch", required = true)
            @PathVariable Long transactionPoid) {

        try {
            loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
            return success("Voucher fetched successfully", service.getVoucherById(transactionPoid, UserContext.getDocumentId()));
        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to fetch Bank Payment Voucher: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Create a new Bank Payment Voucher",
            description = "Creates a new bank payment voucher along with its related GL, Charge, or Item details depending on the Ref Type.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created the Bank Payment Voucher",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BankPaymentVoucherResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input or missing required fields",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createVoucher(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Bank Payment Voucher details for creation",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BankPaymentVoucherRequest.class)
                    )
            )
            @Valid @RequestBody BankPaymentVoucherRequest request) {

        try {
            BankPaymentVoucherResponse response = service.createBankPaymentVoucher(request, UserContext.getDocumentId());
            return success("Bank Payment Voucher created successfully", response);
        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to create Bank Payment Voucher: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Update an existing Bank Payment Voucher",
            description = "Updates the header and related details of an existing bank payment voucher. " +
                    "Details (GL, Charge, or Item) will be updated based on the Ref Type.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated the Bank Payment Voucher",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BankPaymentVoucherResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Bank Payment Voucher not found",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateVoucher(
            @Parameter(description = "Unique ID of the Bank Payment Voucher to update", required = true)
            @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated Bank Payment Voucher details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BankPaymentVoucherRequest.class)
                    )
            )
            @Valid @RequestBody BankPaymentVoucherRequest request) {

        try {
            BankPaymentVoucherResponse response = service.updateBankPaymentVoucher(transactionPoid, request, UserContext.getDocumentId());
            return success("Bank Payment Voucher updated successfully", response);
        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to update Bank Payment Voucher: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "List Bank Payment Vouchers with Search and Sort (DocId: 400-107)",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (TRANSACTION_POID, DOC_REF, CREATED_BY). Sorting default on TRANSACTION_POID, desc."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields (TRANSACTION_POID, DOC_REF, CREATED_BY).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR"
                      4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                      5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "BPV-123" }
                    
                    - #### Single Field, Single Value:
                      • { "searchField": "TRANSACTION_POID", "searchValue": "1001" }
                    
                    - #### Single Field, Multiple Values:
                      • { "searchField": "TRANSACTION_POID", "searchValue": "1001|1002" }
                    
                    - #### Multiple Different Fields:
                      • { "searchField": "TRANSACTION_POID", "searchValue": "1001" },
                      • { "searchField": "DOC_REF", "searchValue": "BPV-123" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to TRANSACTION_POID ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=TRANSACTION_POID,DESC
                      • sort=DOC_REF,ASC
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Bank Payment Voucher Filters",
                                    value = """
                                            {
                                              "operator": "AND",
                                              "isDeleted": "N",
                                              "filters": [
                                                 { "searchField": "GLOBALSEARCH", "searchValue": "BPV" },
                                                 { "searchField": "TRANSACTION_POID", "searchValue": "1001|1002" },
                                                 { "searchField": "DOC_REF", "searchValue": "BPV-123" },
                                                 { "searchField": "CREATED_BY", "searchValue": "Admin" }
                                              ]
                                            }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listBankPaymentVouchers(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @Parameter(description = "Start date for filtering (YYYY-MM-DD)", example = "2025-01-01")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "End date for filtering (YYYY-MM-DD)", example = "2025-12-31")
            @RequestParam(required = false) LocalDate endDate) {

        try {
            Map<String, Object> data = service.listBankPaymentVouchers(UserContext.getDocumentId(), filters, startDate, endDate, pageable);
            return success("Bank Payment Vouchers fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch Bank Payment Vouchers list: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Soft delete a Bank Payment Voucher",
            description = "Marks a Bank Payment Voucher as deleted (sets DELETED = 'Y') without permanently removing it from the database.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully soft deleted the Bank Payment Voucher",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BankPaymentVoucherResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Bank Payment Voucher not found",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}/delete")
    public ResponseEntity<?> softDeleteVoucher(
            @Parameter(description = "Unique ID of the Bank Payment Voucher to soft delete", required = true)
            @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {

        try {
            service.softDeleteVoucher(transactionPoid, UserContext.getDocumentId(), deleteReasonDto);
            return success("Bank Payment Voucher soft deleted successfully");
        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to delete Bank Payment Voucher: " + ex.getMessage());
        }
    }

    @Operation(summary = "Get Bank Balance", description = "Retrieves bank balance for a given bank")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/bank-balance/{bankPoid}")
    public ResponseEntity<?> getBankBalance(
            @PathVariable Long bankPoid,
            @RequestParam(required = false) Long docKeyPoid,
            @RequestParam(required = false) Date docDate) {
        try {
            return success("Bank balance fetched successfully", service.getBankBalance(UserContext.getDocumentId(), docKeyPoid, docDate, bankPoid));
        } catch (Exception ex) {
            return internalServerError("Failed to fetch bank balance: " + ex.getMessage());
        }
    }

    @Operation(summary = "Validate Cheque Print", description = "Validates voucher before cheque printing")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/{transactionPoid}/validate-cheque-print")
    public ResponseEntity<?> validateChequePrint(
            @PathVariable Long transactionPoid) {
        try {
            service.validateChequePrint(transactionPoid);
            return success("Cheque print validation successful", null);
        } catch (Exception ex) {
            return internalServerError("Validation failed: " + ex.getMessage());
        }
    }

    @Operation(summary = "Mark Cheque as Printed", description = "Marks cheque as printed after successful print")
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{transactionPoid}/mark-cheque-printed")
    public ResponseEntity<?> markChequePrinted(
            @PathVariable Long transactionPoid) {
        try {
            service.markChequePrinted(transactionPoid);
            return success("Cheque marked as printed successfully", null);
        } catch (Exception ex) {
            return internalServerError("Failed to mark cheque as printed: " + ex.getMessage());
        }
    }

    @Operation(summary = "Release Cheque", description = "Releases cheque to a person")
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{transactionPoid}/release-cheque")
    public ResponseEntity<?> releaseCheque(
            @PathVariable Long transactionPoid,
            @RequestParam String releasedTo,
            @RequestParam String contact) {
        try {
            service.releaseCheque(transactionPoid, releasedTo, contact);
            return success("Cheque released successfully", null);
        } catch (Exception ex) {
            return internalServerError("Failed to release cheque: " + ex.getMessage());
        }
    }

    @Operation(summary = "Un-Release Cheque", description = "Reverts cheque release")
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{transactionPoid}/unrelease-cheque")
    public ResponseEntity<?> unReleaseCheque(
            @PathVariable Long transactionPoid) {
        try {
            service.unReleaseCheque(transactionPoid);
            return success("Cheque un-released successfully", null);
        } catch (Exception ex) {
            return internalServerError("Failed to un-release cheque: " + ex.getMessage());
        }
    }

    @Operation(summary = "Reset Cheque Status", description = "Resets cheque status")
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{transactionPoid}/reset-cheque-status")
    public ResponseEntity<?> resetChequeStatus(
            @PathVariable Long transactionPoid) {
        try {
            service.resetChequeStatus(transactionPoid);
            return success("Cheque status reset successfully", null);
        } catch (Exception ex) {
            return internalServerError("Failed to reset cheque status: " + ex.getMessage());
        }
    }

    @Operation(summary = "Revert Reconciliation", description = "Reverts bank reconciliation")
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{transactionPoid}/revert-reconciliation")
    public ResponseEntity<?> revertReconciliation(
            @PathVariable Long transactionPoid) {
        try {
            String status = service.revertReconciliation(transactionPoid, UserContext.getDocumentId());

            if (status.startsWith("SUCCESS")) {
                return success(status, null);
            } else {
                return internalServerError("Failed to revert reconciliation: " + status);
            }

        } catch (Exception ex) {
            return internalServerError("Exception: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Load FF Charges",
            description = """
                    Fetches charge details for FF Invoice using stored procedure.
                    ### Input:
                    - FF Invoice Ref No (refNo)
                    ### Output:
                    - Charge Amount
                    - Tax Percent
                    - Total Amount
                    - FF Row IDs
                    - Manifest Charge Details
                    """
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/load-from-ff")
    public ResponseEntity<?> createBankPayFromFf(
            @RequestParam String ffPoid) {
        BankPayCreateFromFfResponse response = service.createBankPayFromFf(ffPoid);
        try {
            return success("FF charges loaded successfully", response);
        } catch (Exception ex) {
            return internalServerError("Failed to load FF charges: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Load FDA Charges",
            description = """
                    Fetches charge details for FDA using stored procedure.
                    ### Input:
                    - FDA Ref No (refNo)
                    ### Output:
                    - Charge Amount
                    - Tax Percent
                    - Total Amount
                    - FDA Row IDs
                    - Charge Details
                    """
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/load-from-fda")
    public ResponseEntity<?> loadFdaCharges(
            @RequestParam String fdaPoid) {
        BankPayCreateFromFdaResponse response =
                service.createBankPayFromFda(fdaPoid);
        try {
            return success("FDA charges loaded successfully", response);
        } catch (Exception ex) {
            return internalServerError("Failed to load FDA charges: " + ex.getMessage());
        }
    }


    @Operation(
            summary = "Load MTA Items",
            description = """
                    Fetches item details for MTA using stored procedure.
                    ### Input:
                    - MTA RFQ Ref No (refNo)
                    ### Output:
                    - Item Amount
                    - Tax Percent
                    - Total Amount
                    - MTA Row IDs
                    - Item Details
                    """
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/load-from-mta")
    public ResponseEntity<?> createFromMta(
            @RequestParam String rfqPoid) {

        BankPayCreateFromMtaResponse response = service.createBankPayment(rfqPoid);
        try {
            return success("MTA charges loaded successfully", response);
        } catch (Exception ex) {
            return internalServerError("Failed to load MTA charges: " + ex.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for Bank Payment Voucher",
            description = "Generate PDF report for a specific Bank Payment Voucher transaction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Bank Payment Voucher not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "21")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = service.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=bank-payment-voucher-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Bank Payment Voucher: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }


}
