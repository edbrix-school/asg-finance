package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.ApPurchaseCnHdrDto;
import com.asg.finance.service.ApPurchaseCnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;
import static com.asg.common.lib.dto.response.ApiResponse.*;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/ap-purchase-credit-note")
@Tag(name = "ap-purchase-credit-note-controller", description = "Manage AP Purchase Credit Note (Supplier Credit Note)")
public class ApPurchaseCnController {

    private final ApPurchaseCnService service;

    @Operation(
            summary = "Create Supplier Credit Note",
            description = """
                Creates supplier credit note as per SRS.
                
                ### Authorization Parameters (handled by interceptor)
                - **documentId:** Document identifier (300-001)
                - **actionRequested:** Action being performed (CREATE)
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Supplier credit note created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation errors"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            name = "Supplier Credit Note Creation",
                            value = """
                                    {
                                      "partyType": "SUPPLIER",
                                      "supplierPoid": 12345,
                                      "refType": "PJ_REVERSAL",
                                      "pjReversalRef": 67890,
                                      "postingNarration": "Credit note for supplier payment reversal",
                                      "transactionDate": "2025-01-15",
                                      "currencyCode": "USD",
                                      "currencyRate": 0.377,
                                      "amount": 1000.00,
                                      "bhdAmount": 377.00,
                                      "remarks": "Supplier credit note for overpayment",
                                      "actionType": "isCreated",
                                      "itemDetails": [
                                        {
                                          "itemPoid": 101,
                                          "quantity": 10,
                                          "unitPrice": 50.00,
                                          "totalAmount": 500.00,
                                          "remarks": "Item credit",
                                          "actionType": "isCreated"
                                        }
                                      ],
                                      "chargeDetails": [
                                        {
                                          "chargePoid": 201,
                                          "chargeAmount": 100.00,
                                          "taxAmount": 5.00,
                                          "totalAmount": 105.00,
                                          "remarks": "Service charge credit",
                                          "actionType": "isCreated"
                                        }
                                      ],
                                      "glDetails": [
                                        {
                                          "companyPoid": 1,
                                          "type": "DR",
                                          "glPoid": 4001,
                                          "drAmt": 1000.00,
                                          "crAmt": 0.00,
                                          "remarks": "Supplier credit debit entry",
                                          "actionType": "isCreated"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ApPurchaseCnHdrDto dto) {
        ApPurchaseCnHdrDto result = service.create(dto);
        return success("Supplier credit note created successfully", result);
    }

    @Operation(
            summary = "Get Supplier Credit Note by ID",
            description = """
                Retrieves detailed information about a specific supplier credit note.
                
                ### Authorization Parameters (handled by interceptor)
                - **documentId:** Document identifier (300-001)
                - **actionRequested:** Action being performed (VIEW)
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Supplier credit note retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Supplier credit note not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getById(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid) {
        ApPurchaseCnHdrDto result = service.getById(transactionPoid);
        return success("Supplier credit note fetched successfully", result);
    }

    @Operation(
            summary = "List Supplier Credit Notes",
            description = """
                Retrieves paginated list of supplier credit notes with filtering capabilities.
                
                ### Authorization Parameters (handled by interceptor)
                - **documentId:** Document identifier (300-001)
                - **actionRequested:** Action being performed (VIEW)
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Supplier credit notes list retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date range"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            name = "Supplier Credit Note Filters",
                            value = """
                                    {
                                      "operator": "AND",
                                      "isDeleted": "N",
                                      "filters": [
                                        {
                                          "searchField": "PARTY_TYPE",
                                          "searchValue": "SUPPLIER"
                                        },
                                        {
                                          "searchField": "REF_TYPE",
                                          "searchValue": "PJ_REVERSAL"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        try {
            if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
                return badRequest("Both startDate and endDate should be specified or both should be empty.");
            }
            Map<String, Object> result = service.list(UserContext.getDocumentId(), filters, startDate, endDate, pageable);
            return success("Supplier credit note list fetched successfully", result);
        } catch (Exception e) {
            log.error("Error fetching supplier credit note list", e);
            return internalServerError("Failed to fetch list: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Update Supplier Credit Note",
            description = """
                Updates an existing supplier credit note with new information.
                
                ### Authorization Parameters (handled by interceptor)
                - **documentId:** Document identifier (300-001)
                - **actionRequested:** Action being performed (EDIT)
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Supplier credit note updated successfully"),
            @ApiResponse(responseCode = "404", description = "Supplier credit note not found"),
            @ApiResponse(responseCode = "400", description = "Validation errors"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> update(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid,
            @Valid @RequestBody ApPurchaseCnHdrDto dto) {
        ApPurchaseCnHdrDto result = service.update(transactionPoid, dto);
        return success("Supplier credit note updated successfully", result);
    }

    @Operation(
            summary = "Delete Supplier Credit Note",
            description = """
                Marks a supplier credit note as deleted. This is a soft delete operation.
                
                ### Authorization Parameters (handled by interceptor)
                - **documentId:** Document identifier (300-001)
                - **actionRequested:** Action being performed (DELETE)
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Supplier credit note deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Supplier credit note not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> delete(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        service.delete(transactionPoid);
        return success("Supplier credit note deleted successfully", null);
    }

    @Operation(
            summary = "Get PJ Reversal Details",
            description = """
                Calls PROC_AP_CN_PJ_REF_DETAILS to fetch Purchase Journal reversal details.
                
                ### Authorization Parameters (handled by interceptor)
                - **documentId:** Document identifier (300-001)
                - **actionRequested:** Action being performed (VIEW)
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PJ reference details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "PJ reference not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/pj-ref-details/{pjPoid}")
    public ResponseEntity<?> getPjRefDetails(
            @Parameter(description = "Purchase Journal POID", required = true, example = "67890")
            @PathVariable Long pjPoid) {
        Map<String, Object> result = service.getPjRefDetails(pjPoid);
        return success("PJ reference details fetched successfully", result);
    }

    @Operation(
            summary = "Get Party Details",
            description = """
                Calls PROC_AP_CN_PJ_PARTY_DTLS to fetch party details for supplier credit note.
                
                ### Authorization Parameters (handled by interceptor)
                - **documentId:** Document identifier (300-001)
                - **actionRequested:** Action being performed (VIEW)
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Party details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Party not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/party-details/{partyType}/{partyPoid}")
    public ResponseEntity<?> getPartyDetails(
            @Parameter(description = "Party type (SUPPLIER, PRINCIPAL)", required = true, example = "SUPPLIER")
            @PathVariable String partyType,
            @Parameter(description = "Party POID", required = true, example = "54321")
            @PathVariable Long partyPoid) {
        Map<String, Object> result = service.getPartyDetails(partyType, partyPoid);
        return success("Party details fetched successfully", result);
    }
}
