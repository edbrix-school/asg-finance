package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.BankDebitVoucherRequest;
import com.asg.finance.dto.BankDebitVoucherResponse;
import com.asg.finance.dto.PayGLValidationRequest;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.service.BankDebitVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;


@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/bank-debit-voucher")
public class BankDebitVoucherController {

    private final BankDebitVoucherService bankDebitVoucherService;
    private final LoggingService loggingService;

    @Operation(summary = "Create Bank Debit Voucher")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            name = "Bank Debit Voucher Creation",
                            value = """
                                    {
                                      "bankPoid": 1008,
                                      "currencyCode": "USD",
                                      "currencyRate": 1.0,
                                      "currencyAmt": 1000.00,
                                      "amount": 1000.00,
                                      "gainLoss": 0.00,
                                      "gainLossType": "GAIN",
                                      "bankCharges": 10.00,
                                      "taxPoid": 6,
                                      "taxPercentage": 5.0,
                                      "taxAmount": 0.50,
                                      "longNarration": "Payment for office supplies and services",
                                      "payingType": "4",
                                      "bankPurposePoid": 72,
                                      "ttSpecialRate": 1.0,
                                      "rateDealNo": "RD001",
                                      "refType": "GENERAL",
                                      "ffRef": "1",
                                      "fdaRef": 12345,
                                      "payingToName": "John Doe",
                                      "payingTo": "G15",
                                      "beneficiaryBankPoid": 1008,
                                      "beneficiaryIban": "",
                                      "remarks": "Test payment",
                                      "confidentialRemarks": "Internal use only",
                                      "docRef": "BDV-004",
                                      "documentDate": "2025-01-20",
                                      "paymentGlDetails": [
                                        {
                                          "detRowId": 1,
                                          "type": "DR",
                                          "companyPoid": 1,
                                          "glPoid": 97907,
                                          "drAmt": 1000.00,
                                          "crAmt": 0.00,
                                          "taxPoid": 6,
                                          "taxPercentage": 5.0,
                                          "taxAmount": 50.00,
                                          "totalAmount": 1050.00,
                                          "partyInvNumber": "INV-001",
                                          "partyInvDate": "2024-12-15",
                                          "remarks": "Office supplies expense",
                                          "actionType": "isCreated"
                                          "breakupList": [
                                            {
                                              "billDetRowId": 1,
                                              "billRefType": "INVOICE",
                                              "billRef": "INV-1001",
                                              "billDueDate": "2025-12-31",
                                              "amount": 1500.75,
                                              "billRemarks": "Payment for services"
                                            }
                                          ],
                                          "costCenterList": [
                                            {
                                              "costDetRowId": 1,
                                              "costGroup": 1,
                                              "costPoid": 1,
                                              "amount": 10
                                            }
                                          ]
                                        },
                                        {
                                          "detRowId": 2,
                                          "type": "CR",
                                          "companyPoid": 1,
                                          "glPoid": 97906,
                                          "drAmt": 0.00,
                                          "crAmt": 1000.00,
                                          "taxPoid": 6,
                                          "taxPercentage": 7.0,
                                          "taxAmount": 70.00,
                                          "totalAmount": 1070.00,
                                          "remarks": "Bank account",
                                          "breakupList": [],
                                          "costCenterList": []
                                        }
                                      ],
                                      "chargeDetails": [],
                                      "termsAndConditionDtoList": [
                                        {
                                          "termsPoid": 1,
                                          "detRowId": 1,
                                          "rowSeqNo": 1,
                                          "clauseNo": "Test",
                                          "clauseDetails": "Test"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createBankDebitVoucher(
            @Valid @RequestBody BankDebitVoucherRequest request) {
        BankDebitVoucherResponse response = bankDebitVoucherService.createBankDebitVoucher(request, UserContext.getDocumentId());
        return success("Bank Debit Voucher created successfully", response);
    }

    @Operation(summary = "Get Bank Debit Voucher")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getBankDebitVoucher(
            @PathVariable @NotNull @Min(1) Long transactionPoid) {
        BankDebitVoucherResponse response = bankDebitVoucherService.getBankDebitVoucher(transactionPoid, UserContext.getDocumentId());
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        return success("Bank Debit Voucher retrieved successfully", response);
    }

    @Operation(summary = "List Bank Debit Vouchers")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            name = "Bank Debit Voucher List",
                            value = """
                                    {
                                       "operator": "OR",
                                       "isDeleted": "N",
                                       "filters": [
                                         {
                                           "searchField": "DOC_REF",
                                           "searchValue": "BDV-003"
                                         },
                                         {
                                           "searchField": "BANK_DESCRIPTION ",
                                           "searchValue": "Bank Desc BNKCD006"
                                         }
                                       ]
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<?> listBankDebitVouchers(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            return badRequest("Both startDate and endDate should be specified or both dates should be empty.");
        }
        Map<String, Object> response = bankDebitVoucherService.listBankDebitVouchers(UserContext.getDocumentId(), filters, startDate, endDate, pageable);

            return success("Bank Debit Vouchers list retrieved successfully", response);
    }

    @Operation(summary = "Update Bank Debit Voucher")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            name = "Bank Debit Voucher Update",
                            value = """
                                    {
                                        "bankPoid": 1008,
                                        "currencyCode": "USD",
                                        "currencyRate": 1.0,
                                        "currencyAmt": 1000.00,
                                        "amount": 1000.00,
                                        "gainLoss": 0.00,
                                        "gainLossType": "GAIN",
                                        "bankCharges": 10.00,
                                        "taxPoid": 6,
                                        "taxPercentage": 5.0,
                                        "taxAmount": 0.50,
                                        "longNarration": "Payment for office supplies and services Update",
                                        "payingType": "4",
                                        "bankPurposePoid": 72,
                                        "ttSpecialRate": 1.0,
                                        "rateDealNo": "RD001",
                                        "refType": "GENERAL",
                                        "ffRef": 1,
                                        "fdaRef": 12345,
                                        "payingToName": "John Doe",
                                        "payingTo": "G15",
                                        "beneficiaryBankPoid": 1008,
                                        "beneficiaryIban": "",
                                        "remarks": "Test payment",
                                        "confidentialRemarks": "Internal use only",
                                        "docRef": "BDV-004",
                                        "documentDate": "2025-01-20",
                                        "paymentGlDetails": [
                                            {
                                                "detRowId": 1,
                                                "type": "DR",
                                                "companyPoid": 1,
                                                "glPoid": 97907,
                                                "drAmt": 1000.00,
                                                "crAmt": 0.00,
                                                "taxPoid": 6,
                                                "taxPercentage": 5.0,
                                                "taxAmount": 50.00,
                                                "totalAmount": 1050.00,
                                                "partyInvNumber": "INV-001",
                                                "partyInvDate": "2024-12-15",
                                                "remarks": "Office supplies expense",
                                                "actionType": "isUpdated"                                
                                                "breakupList": [
                                                    {
                                                        "billDetRowId": 1,
                                                        "billRefType": "INVOICE",
                                                        "billRef": "INV-1001",
                                                        "billDueDate": "2025-12-31",
                                                        "amount": 1500.75,
                                                        "billRemarks": "Payment for services"
                                                    }
                                                ],
                                                "costCenterList": [
                                                    {
                                                        "costDetRowId": 1,
                                                        "costGroup": 1,
                                                        "costPoid": 1,
                                                        "amount": 10
                                                    }
                                                ]
                                            },
                                            {
                                                "detRowId": 2,
                                                "type": "CR",
                                                "companyPoid": 1,
                                                "glPoid": 97906,
                                                "drAmt": 0.00,
                                                "crAmt": 1000.00,
                                                "taxPoid": 6,
                                                "taxPercentage": 7.0,
                                                "taxAmount": 70.00,
                                                "totalAmount": 1070.00,
                                                "remarks": "Bank account",
                                                "breakupList": [],
                                                "costCenterList": []
                                            }
                                        ],
                                        "chargeDetails": [],
                                        "termsAndConditionDtoList": [
                                            {
                                                "termsPoid": 1,
                                                "detRowId": 1,
                                                "rowSeqNo": 1,
                                                "clauseNo": "Test",
                                                "clauseDetails": "Test"
                                            }
                                        ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateBankDebitVoucher(
            @PathVariable @NotNull @Min(1) Long transactionPoid,
            @Valid @RequestBody BankDebitVoucherRequest request) {
        BankDebitVoucherResponse response = bankDebitVoucherService.updateBankDebitVoucher(transactionPoid, request, UserContext.getDocumentId());
        return success("Bank Debit Voucher updated successfully", response);
    }

    @Operation(summary = "Delete Bank Debit Voucher")
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> deleteBankDebitVoucher(
            @PathVariable @NotNull @Min(1) Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        bankDebitVoucherService.softDeleteBankDebitVoucher(transactionPoid, deleteReasonDto);
        return success("Bank Debit Voucher soft deleted successfully", transactionPoid);
    }

    @Operation(summary = "Get FF Charges")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/ff-charges")
    public ResponseEntity<?> getFFCharges(
            @Parameter(description = "FF reference POID", required = true)
            @RequestParam @NotNull @Min(1) Long ffRefPoid) {
        Object response = bankDebitVoucherService.loadFFCharges(ffRefPoid);
        return success("FF charges retrieved successfully", response);
    }

    @Operation(summary = "Get FDA Charges")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/fda-charges")
    public ResponseEntity<?> getFDACharges(
            @Parameter(description = "FDA reference POID", required = true)
            @RequestParam @NotNull @Min(1) Long fdaRefPoid) {
        Object response = bankDebitVoucherService.loadFDACharges(fdaRefPoid);
        return success("FDA charges retrieved successfully", response);
    }

    @Operation(summary = "Get Bank Balance")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/bank-balance")
    public ResponseEntity<?> getBankBalance(
            @Parameter(description = "Bank POID", required = true)
            @RequestParam @NotNull @Min(1) Long bankPoid,
            @Parameter(description = "Document Date", required = true)
            @RequestParam @NotNull Date docDate) {
        Object response = bankDebitVoucherService.getBankBalance(bankPoid, UserContext.getDocumentId(), docDate);
        return success("Bank balance retrieved successfully", response);
    }

    @Operation(summary = "Get Beneficiary Name by Beneficiary Id")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/beneficiary-name")
    public ResponseEntity<?> getBeneficiaryName(
            @Parameter(description = "Beneficiary Id", required = true)
            @RequestParam @NotNull @Min(1) Long beneficiaryId) {
        Object response = bankDebitVoucherService.getBeneficiaryName(beneficiaryId, UserContext.getDocumentId());
        return success("Beneficiary name retrieved successfully", response);
    }

    @Operation(summary = "Validate Pay GL and Beneficiary")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            name = "Validate Pay GL and Beneficiary",
                            value = """
                                    {
                                      "transactionPoid": 1,
                                      "payingType": "4",
                                      "refType": "GENERAL",
                                      "payGlPoid": 0,
                                      "payingTo": "G15",
                                      "bankPoid": 1008
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/validate-paygl")
    public ResponseEntity<?> validatePayGL(
            @Valid @RequestBody PayGLValidationRequest request) {
        bankDebitVoucherService.validatePayGLAndBeneficiary(request);
        return success("Pay GL validation completed successfully", null);
    }

    @Operation(summary = "Revert Reconciliation", description = "Reverts bank reconciliation")
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/{transactionPoid}/revert-reconciliation")
    public ResponseEntity<?> revertReconciliation(
            @PathVariable Long transactionPoid,
            @RequestParam(required = false) String comments) {
        try {
            bankDebitVoucherService.revertReconciliation(transactionPoid, comments);
            return success("Reconciliation reverted successfully", null);
        } catch (Exception ex) {
            return internalServerError("Failed to revert reconciliation: " + ex.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for IMCO Deposit Refund",
            description = "Generate PDF report for a specific Bank Debit Voucher transaction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Bank Debit Voucher not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "21")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = bankDebitVoucherService.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=bank-debit-voucher-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Bank Debit Voucher: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

}