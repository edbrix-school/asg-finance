package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.dto.DebitNoteHeaderDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.service.DebitNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/debit-note")
@Tag(
        name = "debit-note-controller",
        description = "Manage Debit Note records including GL/Charge tabs, FDA validation, tax, cost updates and listing with filters."
)
public class DebitNoteController {

    private final DebitNoteService debitNoteService;
    private final LoggingService loggingService;
    // -------------------------------------------------------
    // CREATE
    // -------------------------------------------------------
    @Operation(
            summary = "Create Debit Note",
            description = """
                    Creates a new Debit Note along with GL or Charge details depending on Ref Type.
                    
                    ### Business Rules
                    - GENERAL → Requires GL Details
                    - FDA → Requires Charge Details  
                    - Currency, Credit Period & Posting Narration mandatory  
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Debit note created successfully",
            content = @Content(schema = @Schema(implementation = DebitNoteHeaderDto.class))
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createDebitNote(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Debit Note Header + GL / Charge Details",
                    content = @Content(
                            schema = @Schema(implementation = DebitNoteHeaderDto.class),
                            examples = @ExampleObject(
                                    name = "Create Debit Note Example",
                                    value = """
                                            {
                                              "partyType": "SUPPLIER",
                                              "partyPoid": 201,
                                              "currencyCode": "USD",
                                              "currencyRate": 0.376,
                                              "grandTotal": 5000,
                                              "postingNarration": "Debit note for freight adjustment",
                                              "creditPeriod": 7,
                                              "voucherType": "DEBIT_NOTE",
                                              "refType": "GENERAL",
                                              "bankPoid": 11,
                                              "glDetails": [
                                                {
                                                  "glId": 10001,
                                                  "type": "CR",
                                                  "amount": 5000,
                                                  "taxId": 4,
                                                  "remarks": "Adjustment entry",
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
                                                }
                                              ],
                                              "chargeDetails": [
                                                {
                                                  "chargeId": 2001,
                                                  "costAmount": 2000,
                                                  "chargeAmount": 2500,
                                                  "taxId": 3
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody DebitNoteHeaderDto dto
    ) {
        DebitNoteHeaderDto response = debitNoteService.createDebitNote(dto);
        return success("Debit Note created successfully", response);
    }

    // -------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------
    @Operation(
            summary = "Update Debit Note",
            description = "Updates debit note only when it is still in draft (not approved)."
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateDebitNote(
            @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Debit Note fields to update",
                    content = @Content(
                            schema = @Schema(implementation = DebitNoteHeaderDto.class),
                            examples = @ExampleObject(
                                    name = "Update Debit Note Example",
                                    value = """
                                            {
                                              "partyType": "CUSTOMER",
                                              "partyPoid": 150,
                                              "currencyCode": "BHD",
                                              "currencyRate": 1.0,
                                              "grandTotal": 2500,
                                              "postingNarration": "Updated debit note for service charges",
                                              "creditPeriod": 15,
                                              "voucherType": "DEBIT_NOTE",
                                              "refType": "FDA",
                                              "fdaRefPoid": 1001,
                                              "glDetails": [
                                                {
                                                  "glId": 10001,
                                                  "type": "CR",
                                                  "amount": 5000,
                                                  "taxId": 4,
                                                  "remarks": "Adjustment entry",
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
                                                }
                                              ],
                                              "chargeDetails": [
                                                {
                                                  "chargeId": 2001,
                                                  "costAmount": 2000,
                                                  "chargeAmount": 2500,
                                                  "taxId": 3
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody DebitNoteHeaderDto dto
    ) {
        return success("Debit Note updated successfully",
                debitNoteService.updateDebitNote(transactionPoid, dto));
    }

    // -------------------------------------------------------
    // DELETE
    // -------------------------------------------------------
    @Operation(
            summary = "Delete Debit Note",
            description = "Soft deletes the debit note using internal DB procedure."
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> deleteDebitNote(@PathVariable Long transactionPoid,
                                             @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        debitNoteService.deleteDebitNote(transactionPoid, deleteReasonDto);
        return success("Debit Note deleted successfully");
    }

    // -------------------------------------------------------
    // GET DETAILS
    // -------------------------------------------------------
    @Operation(
            summary = "Get Debit Note Details",
            description = "Fetch debit note header + GL / Charge tabs based on Ref Type."
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getDebitNote(@PathVariable Long transactionPoid) {
        DebitNoteHeaderDto result = debitNoteService.getDebitNote(transactionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        return success("Debit Note details fetched successfully", result);
    }

    // -------------------------------------------------------
    // LIST + FILTERS
    // -------------------------------------------------------
    @Operation(
            summary = "List Debit Notes with Filters & Pagination",
            description = """
                    Fetch Debit Notes using pagination + dynamic filters.

                    ### Allowed searchField values:
                    TRANSACTION_POID, PARTY_TYPE, PARTY_POID, REF_TYPE,
                    VOUCHER_TYPE, DOC_REF, CREATED_DATE, GRAND_TOTAL
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Search Filters for Debit Notes",
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = @ExampleObject(
                            name = "Debit Note Filters",
                            value = """
                                    {
                                      "operator": "AND",
                                      "isDeleted": "N",
                                      "filters": [
                                        { "searchField": "PARTY_TYPE", "searchValue": "SUPPLIER" },
                                        { "searchField": "REF_TYPE", "searchValue": "FDA" },
                                        { "searchField": "VOUCHER_TYPE", "searchValue": "DEBIT_NOTE" }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listDebitNotes(
            @ParameterObject Pageable pageable,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestBody(required = false) FilterRequestDto filterRequest
    ) {
        try {
            if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
                return badRequest("Both startDate and endDate should be specified or both dates should be empty.");
            }
            Map<String, Object> result = debitNoteService.listDebitNotes(filterRequest, startDate, endDate, pageable);

            return success("Debit Notes fetched successfully", result);
        } catch (Exception e) {
            return internalServerError("Unable to fetch Debit Notes: " + e.getMessage());
        }
    }

    @Operation(summary = "Load FDA Charges", description = "Fetch FDA charge details for a given FDA POID.")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/fda/{fdaPoid}/charges")
    public ResponseEntity<?> loadFdaCharges(@PathVariable Long fdaPoid) {
        Map<String, Object> result = debitNoteService.loadFdaCharges(fdaPoid);
        return success("FDA charges loaded successfully", result);
    }

    @Operation(summary = "Get Tax Percentage for Charge", description = "Returns tax percentage for the given charge ID.")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/tax/{chargeId}")
    public ResponseEntity<?> getChargeTax(@PathVariable Long chargeId,
                                          @Parameter(description = "Party Type", required = true, example = "100")
                                          @RequestParam String partyType,
                                          @Parameter(description = "Party Poid", required = true, example = "SUPPLIER")
                                          @RequestParam Long partyPoid) {
        Map<String, Object> result = debitNoteService.getChargeTax(chargeId, partyType, partyPoid);
        return success("Tax details fetched successfully", result);
    }

    @Operation(summary = "Update Cost Amount", description = "Recalculates cost amounts for FDA/Other Charges.")
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{transactionPoid}/update-cost-amount")
    public ResponseEntity<?> updateCostAmount(
            @PathVariable Long transactionPoid
    ) {
        debitNoteService.updateCostAmount(transactionPoid);
        return success("Cost amount updated successfully");
    }

    @Operation(summary = "Validate Sail Date", description = "Validates sail date for the given FDA reference.")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/fda/{fdaPoid}/sail-date-check")
    public ResponseEntity<?> checkSailDate(@PathVariable Long fdaPoid) {
        return success("Sail date validation completed",
                debitNoteService.checkSailDate(fdaPoid));
    }

    @Operation(summary = "Get Party Defaults", description = "Returns default bank + credit period for Supplier/Customer.")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/party-defaults/{partyPoid}")
    public ResponseEntity<?> getPartyDefaults(
            @PathVariable Long partyPoid,
            @RequestParam String partyType
    ) {
        return success("Party default values loaded",
                debitNoteService.getPartyDefaults(partyPoid, partyType));
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for Debit Note",
            description = "Generate PDF report for a specific Debit Note",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Debit Note not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "35657")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = debitNoteService.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=debit-note-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Debit Note: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

    @Operation(summary = "Validate Edit Request", description = "Validates debit note edit request data without saving.")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}/validate-edit")
    public ResponseEntity<?> validateEditRequest(@PathVariable Long transactionPoid) {
        Map<String, Object> result = debitNoteService.validateEditRequest(transactionPoid);
        return success("Validation completed", result);
    }

    @Operation(
            summary = "Get Custom LOV List",
            description = "Returns custom LOV configuration based on selected cost group."
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/cost-group/{costGroup}/custom-lov")
    public ResponseEntity<?> getCustomLovList(
            @Parameter(description = "Cost Group Code", example = "GL_SH_BLS")
            @PathVariable String costGroup
    ) {
        Map<String, String> result = debitNoteService.getCustomLovList(costGroup);
        return success("Custom LOV list retrieved successfully", result);
    }

}
