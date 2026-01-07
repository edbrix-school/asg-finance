package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.finance.dto.GeneralReceiptRequest;
import com.asg.finance.dto.GeneralReceiptResponse;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.service.GeneralReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;
@Slf4j
@RestController
@RequestMapping("/v1/general-receipts")
@RequiredArgsConstructor
@Tag(name = "General Receipt", description = "General Receipt Management APIs")
public class GeneralReceiptController {

    private final GeneralReceiptService generalReceiptService;

    @Operation(
            summary = "Create General Receipt",
            description = """
                    Create a new General Receipt entry for cash/cheque/TT/card payments.
                    System performs all validations and auto-posts to GL or initiates approval 
                    based on configuration parameter GENERAL_RECEIPT_APPROVAL_SUBMISSION.
                    
                    ### Request Parameters
                    - **documentId:** Document identifier (`300-105`)
                    - **actionRequested:** Action being performed (`CREATE`)
                    
                    ### Request Body
                    Provide receipt details including:
                    - **header:** Receipt header (credit GL, received from, currency, amount, narration, etc.)
                    - **payments:** List of payment details (type, amount, cheque info, bank info)
                    - **bills:** List of bill details (for settlement against invoices)
                    - **extraCharges:** List of extra charges (bank charges, round-off, exchange gain/loss)
                    
                    ### Validations
                    - Mandatory: Credit GL, Currency, Ref Type, Amount, Narration
                    - Amount match: Receipt = Sum(Payment amounts)
                    - Cheque date validation (post/back-date limit parameters)
                    - Cost center required for Bank Charges / Round Off / Gain-Loss
                    - RoundOff ≤ ROUNDING_LIMIT parameter
                    - Multi-company validation on General Receipt payment
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Receipt created successfully",
                    content = @Content(schema = @Schema(implementation = GeneralReceiptResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Validation error"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createGeneralReceipt(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "General Receipt create payload",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Sample General Receipt Create Payload",
                                    value = """
                                    {
                                      "header": {
                                        "creditGL": "GL-1201",
                                        "receivedFrom": "ABC TRADING CO",
                                        "currency": "USD",
                                        "rate": 0.376,
                                        "receiptAmount": 1000,
                                        "refType": "AGAINST",
                                        "narration": "Payment against invoice INV-0045",
                                        "companyPoid": 101,
                                        "multicompany": "N",
                                        "ttBankPoid": 250,
                                        "costCenterPoid": "FIN-001"
                                      },
                                      "payments": [
                                        {
                                          "type": "CHEQUE",
                                          "amount": 1000,
                                          "chequeNo": "100245",
                                          "chequeDate": "2025-09-10",
                                          "bank": "NATIONAL BANK OF BAHRAIN",
                                          "bankPoid": 150,
                                          "accountNumber": "001123456789",
                                          "accountName": "ABC Trading Co",
                                          "accountPoid": 2500,
                                          "ttBankPoid": 250,
                                          "ttRef": "TT-REF-12345",
                                          "creditCardRef": "CC-REF-12345",
                                          "cardType": "VISA",
                                          "cardPoid": 300
                                        }
                                      ],
                                      "bills": [
                                        {
                                          "billReference": "INV-0045",
                                          "amount": 1000,
                                          "drCr": "Cr",
                                          "remarks": "Invoice INV-0045 full payment",
                                          "billRefType": "AGAINST",
                                          "billDueDate": "2025-11-30",
                                          "glPoid": 5001,
                                          "glCompanyPoid": 101,
                                          "description": "Payment for invoice"
                                        }
                                      ],
                                      "extraCharges": [
                                        {
                                          "chargeType": "BANK_CHARGES",
                                          "gl": "GL-5110",
                                          "amount": 2.5,
                                          "taxSlab": "VAT5",
                                          "taxPoid": 101,
                                          "taxPercent": 5,
                                          "taxAmount": 0.125,
                                          "totalAmount": 2.625,
                                          "costCenter": "FIN-001",
                                          "remarks": "Bank processing charges"
                                        }
                                      ],
                                      "advances": [
                                        {
                                          "advanceRefDocId": "string",
                                          "advanceRefPoid": 0,
                                          "amount": 0.01,
                                          "remarks": "string"
                                        }
                                      ]
                                    }
                                    """
                            )
                    )
            )
            @Valid @RequestBody GeneralReceiptRequest request) {
        try {
            GeneralReceiptResponse response = generalReceiptService.createGeneralReceipt(request);
            return success(response.getMessage(), response);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to create General Receipt: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get General Receipt by Transaction POID",
            description = """
                    Fetch a specific General Receipt using its Transaction POID (Primary Key).
                    
                    This endpoint queries the following tables:
                    - AR_GEN_RECEIPT_HDR
                    - AR_GEN_RECEIPT_PYMT_DETAILS
                    - AR_GEN_RECEIPT_BILL_DTL
                    - AR_GEN_RECEIPT_CHARGES_DTL
                    - AR_GEN_RECEIPT_ADVANCE_DTL
                    
                    Additionally calls the following procedures:
                    - PROC_GEN_REC_BILLWISE_PENDING → fetch pending bills
                    - PROC_AR_GEN_RCP_FETCH_CUST_AC → fetch customer bank details for cheque mode
                    - PROC_AR_GEN_RCPT_FTCH_GLBAL → load billwise details when Ref Type = AGAINST
                    
                    ### Request Parameters
                    - **transactionPoid:** Transaction POID (Primary Key), example: `234830`
                    - **documentId:** Document identifier (`300-105`)
                    - **actionRequested:** Action being performed (`VIEW`)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Receipt fetched successfully",
                    content = @Content(schema = @Schema(implementation = GeneralReceiptResponse.class))),
            @ApiResponse(responseCode = "404", description = "Receipt not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getGeneralReceiptByTransactionPoid(
            @Parameter(description = "Transaction POID (Primary Key)", required = true, example = "234830")
            @PathVariable Long transactionPoid) {
        try {
            GeneralReceiptResponse response = generalReceiptService.getGeneralReceiptByTransactionPoid(transactionPoid);
            return success("General Receipt fetched successfully", response);
        } catch (ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to fetch General Receipt: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get General Receipt by Document Reference",
            description = """
                    Fetch a specific General Receipt using its Document Reference (Receipt Number).
                    
                    This endpoint queries the following tables:
                    - AR_GEN_RECEIPT_HDR
                    - AR_GEN_RECEIPT_PYMT_DETAILS
                    - AR_GEN_RECEIPT_BILL_DTL
                    - AR_GEN_RECEIPT_CHARGES_DTL
                    - AR_GEN_RECEIPT_ADVANCE_DTL
                    
                    Additionally calls the following procedures:
                    - PROC_GEN_REC_BILLWISE_PENDING → fetch pending bills
                    - PROC_AR_GEN_RCP_FETCH_CUST_AC → fetch customer bank details for cheque mode
                    - PROC_AR_GEN_RCPT_FTCH_GLBAL → load billwise details when Ref Type = AGAINST
                    
                    ### Request Parameters
                    - **docRef:** Document Reference (Receipt Number), example: `ASGGEN72675`
                    - **documentId:** Document identifier (`300-105`)
                    - **actionRequested:** Action being performed (`VIEW`)
                    
                    ### Note
                    This endpoint uses the unique DOC_REF field for lookup. For faster lookups using primary key, use `GET /{transactionPoid}`.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Receipt fetched successfully",
                    content = @Content(schema = @Schema(implementation = GeneralReceiptResponse.class))),
            @ApiResponse(responseCode = "404", description = "Receipt not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/by-ref/{docRef}")
    public ResponseEntity<?> getGeneralReceiptByDocRef(
            @Parameter(description = "Document Reference (Receipt Number)", required = true, example = "ASGGEN72675")
            @PathVariable String docRef) {
        try {
            GeneralReceiptResponse response = generalReceiptService.getGeneralReceiptByDocRef(docRef);
            return success("General Receipt fetched successfully", response);
        } catch (ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to fetch General Receipt: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Update General Receipt",
            description = """
                    Update an existing General Receipt.
                    Note: Cannot update receipts that have been verified/posted to GL.

                    ### Request Parameters
                    - **transactionPoid:** Transaction POID reference identifier
                    - **documentId:** Document identifier (`300-105`)
                    - **actionRequested:** Action being performed (`EDIT`)

                    ### Request Body
                    Provide updated receipt details (same structure as create)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Receipt updated successfully",
                    content = @Content(schema = @Schema(implementation = GeneralReceiptResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Validation error or receipt already verified"),
            @ApiResponse(responseCode = "404", description = "Receipt not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateGeneralReceipt(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid,
            @Valid @RequestBody GeneralReceiptRequest request) {
        try {
            GeneralReceiptResponse response = generalReceiptService.updateGeneralReceipt(transactionPoid, request);
            return success(response.getMessage(), response);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to update General Receipt: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Delete General Receipt",
            description = """
                    Delete a General Receipt (soft delete on parent, hard delete on child tables).
                    
                    ### Request Parameters
                    - **transactionPoid:** Transaction POID reference identifier
                    - **documentId:** Document identifier (`300-105`)
                    - **actionRequested:** Action being performed (`DELETE`)
                    
                    ### Validation Rules:
                    - Cannot delete receipts that have been verified/posted to GL
                    - Cannot delete receipts that have downstream linkages (e.g., GL Ledger entries)
                    - Cannot delete receipts that are already deleted
                    
                    ### Deletion Behavior:
                    - **Parent Table (AR_GEN_RECEIPT_HDR):** Soft delete (sets DELETED = 'Y')
                    - **Child Tables:** Hard delete (permanently removed):
                      - AR_GEN_RECEIPT_ADVANCE_DTL
                      - AR_GEN_RECEIPT_PYMT_DETAILS
                      - AR_GEN_RECEIPT_BILL_DTL
                      - AR_GEN_RECEIPT_CHARGES_DTL
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Receipt deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Receipt cannot be deleted (verified, has linkages, or already deleted)"),
            @ApiResponse(responseCode = "404", description = "Receipt not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> deleteGeneralReceipt(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid) {
        try {
            generalReceiptService.deleteGeneralReceipt(transactionPoid);
            return success("General Receipt deleted successfully", null);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to delete General Receipt: " + ex.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listOfRecordsWithGenericSearch(
            @ParameterObject
            @Parameter(
                    description = "Pagination and sorting configuration",
                    example = "page=0&size=10&sort=transactionPoid,desc"
            )
            Pageable pageable,

            @RequestBody(required = false)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Filter criteria for General Receipt records",
                    required = false,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Search by Document Reference",
                                    value = """
                                    {
                                             "operator": "AND",
                                              "isDeleted": "N",
                                              "filters": [
                                                {
                                                  "searchField": "TRANSACTION_POID",
                                                  "searchValue": "8"
                                                },
                                                {
                                                  "searchField": "DOC_REF",
                                                  "searchValue": "ASGGEN32879"
                                                }
                                              ]
                                    }"""
                            )
                    )
            )
            FilterRequestDto filters,

            @RequestParam
            @Parameter(description = "Document identifier for General Receipt", required = true, example = "300-105")
            String documentId,

            @RequestParam
            @Parameter(description = "Type of action to perform (e.g., VIEW)", required = true, example = "VIEW")
            String actionRequested
    ) {
        Map<String, Object> result = generalReceiptService.listOfRecordsAndGenericSearch(documentId, filters, pageable);
        return success("General Receipts fetched successfully", result);
    }


    @Operation(
            summary = "Get Pending Bills",
            description = "Fetch pending bills for a GL account to select for settlement"
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/pending-bills/{glPoid}")
    public ResponseEntity<?> getPendingBills(
            @Parameter(description = "GL POID", required = true, example = "5001")
            @PathVariable Long glPoid,
            @Parameter(description = "As on date", example = "2025-01-15")
            @RequestParam(required = false) java.time.LocalDate asOnDate) {
        try {
            Map<String, Object> result = generalReceiptService.getPendingBills(glPoid, asOnDate);
            return success("Pending bills fetched successfully", result);
        } catch (Exception ex) {
            return internalServerError("Failed to fetch pending bills: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get GL Account for Charge Type",
            description = "Fetch the GL account POID configured for a specific charge type (e.g., BANK_CHARGES, ROUND_OFF, EXCHANGE_GAIN_LOSS)"
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/charge-gl")
    public ResponseEntity<?> getChargeGLAccount(
            @Parameter(description = "Charge Type", required = true, example = "BANK_CHARGES")
            @RequestParam String chargeType) {
        try {
            String glPoid = generalReceiptService.getChargeGLAccount(chargeType);
            return success("Charge GL account fetched successfully", glPoid);
        } catch (Exception ex) {
            return internalServerError("Failed to fetch charge GL account: " + ex.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for General Receipt",
            description = "Generate PDF report for a specific General Receipt transaction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "General Receipt not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "8")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = generalReceiptService.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=general-receipt-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for General Receipt: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }
}

