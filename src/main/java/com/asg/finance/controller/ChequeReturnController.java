package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;

import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.ChequeReturnEditRequest;
import com.asg.finance.dto.ChequeReturnRequest;
import com.asg.finance.dto.ChequeReturnResponse;
import com.asg.finance.dto.ChequeReturnLoadResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
 import com.asg.finance.service.ChequeReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.time.LocalDate;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/cheque-returns")
@SecurityRequirement(name = "bearerAuth")
public class ChequeReturnController {

    private final ChequeReturnService service;

    @Operation(
            summary = "Create Cheque Return",
            description = "Creates a cheque return header with detail rows and GL rows in a single transaction. " +
                    "DET_ROW_IDs are auto-generated starting from 1 for each header.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cheque Return created",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChequeReturnResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Cheque Return payload with header, details and GL",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ChequeReturnRequest.class)
            )
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createChequeReturn(
            @Valid @RequestBody ChequeReturnRequest request
    ) {
        try {
            ChequeReturnResponse resp = service.createChequeReturn(request);
            return success("Cheque Return created successfully", resp);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to create Cheque Return: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Partially update Cheque Return (Status / Close)",
            description = """
                Performs a partial update on cheque return header fields:
                - Updates **status** and **close details**.
                - If `status = CLOSED`, it also performs cheque closing logic via stored procedure `PROC_CHEQUE_RETURN_LOAD`
                  and GL reposting.
                """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cheque Return updated or closed successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChequeReturnResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "404", description = "Cheque Return not found")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Partially update Cheque Return (Status: Close) and close details (when applicable). On Close Button",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ChequeReturnEditRequest.class)
            )
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PatchMapping("/{transactionPoid}")
    public ResponseEntity<?> partiallyUpdateChequeReturn(
            @Parameter(description = "TRANSACTION_POID", required = true)
            @PathVariable Long transactionPoid,

            @Valid @RequestBody ChequeReturnEditRequest request
    ) {
        try {
            // Only support minimal updates (status/remarks/close)
            ChequeReturnResponse resp = service.updateChequeReturnMinimal(transactionPoid, request);
            return success("Cheque Return status updated successfully", resp);
        } catch (jakarta.persistence.EntityNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to update Cheque Return status: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Update Cheque Return",
            description = "Complete update of cheque return with full payload. Deletes existing child records and inserts new ones. " +
                    "All fields except docRef can be updated. Runs in transaction with rollback on failure.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cheque Return updated",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChequeReturnResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "404", description = "Cheque Return not found")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Full Cheque Return payload (docRef will be ignored)",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ChequeReturnRequest.class)
            )
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> fullUpdateChequeReturn(
            @Parameter(description = "TRANSACTION_POID", required = true)
            @PathVariable Long transactionPoid,

            @Valid @RequestBody ChequeReturnRequest request
    ) {
        try {
            ChequeReturnResponse resp = service.updateChequeReturnV2(transactionPoid, request);
            return success("Cheque Return updated successfully", resp);
        } catch (jakarta.persistence.EntityNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to update Cheque Return: " + ex.getMessage());
        }
    }


    @Operation(
            summary = "Get Cheque Return",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fetched",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChequeReturnResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getChequeReturn(
            @Parameter(description = "TRANSACTION_POID", required = true) @PathVariable Long transactionPoid
    ) {
        try {
            ChequeReturnResponse resp = service.getChequeReturn(transactionPoid);
            return success("Cheque Return fetched successfully", resp);
        } catch (jakarta.persistence.EntityNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to fetch Cheque Return: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Soft delete Cheque Return",
            description = "Marks header as inactive and deleted using JPA (DELETED='Y', ACTIVE='N').",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Soft deleted"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> softDeleteChequeReturn(
            @Parameter(description = "TRANSACTION_POID", required = true) @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        try {
            service.softDeleteChequeReturn(transactionPoid, deleteReasonDto);
            return success("Cheque Return soft deleted successfully");
        } catch (jakarta.persistence.EntityNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to delete Cheque Return: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "List Cheque Returns with Search and Sort",
            description = """
                - Filters:
                  Use either:
                  1. A single `GLOBALSEARCH` filter, OR
                  2. Any combination of specific fields (e.g., CHEQUE_NUMBER, TRANSACTION_POID, TRANSACTION_DATE).
                  3. `operator` will either have "AND" or "OR"; if not given it defaults to "OR".
                  4. `isDeleted` when 'N' or null will return non-deleted records; 'Y' returns deleted records.

                - Global Search:
                  Apply one search term across multiple fields.
                  - { "searchField": "GLOBALSEARCH", "searchValue": "CHQ" }

                - Single Field, Single Value:
                  - { "searchField": "CHEQUE_NUMBER", "searchValue": "710211" }

                - Single Field, Multiple Values:
                  Provide multiple values separated by `|`.
                  - { "searchField": "CHEQUE_NUMBER", "searchValue": "710211|710212" }

                - Multiple Different Fields:
                  Combine multiple search conditions.
                  - { "searchField": "CHEQUE_NUMBER", "searchValue": "710211" }
                  - { "searchField": "TRANSACTION_POID", "searchValue": "282" }
                  - "operator" : "AND"

                - Date Fields:
                  For exact or range queries on date fields, send comparisons in value.
                  - { "searchField": "TRANSACTION_DATE", "searchValue": ">=2025-01-01" }
                  - { "searchField": "TRANSACTION_DATE", "searchValue": "<=2025-12-31" }

                - Sorting:
                  Use `sort=<FIELD>,ASC|DESC` in query params.
                  Examples:
                  - sort=TRANSACTION_DATE,DESC
                  - sort=CHEQUE_NUMBER,ASC
            """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "List fetched successfully",
                            content = @Content(mediaType = "application/json"))
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping(value = "/list", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> listOfRecordsWithGenericSearch(
            @ParameterObject
            @Parameter(
                    description = "Pagination and sorting configuration",
                    example = "page=0&size=10&sort=createdDate,desc"
            )
            Pageable pageable,

            @RequestBody(required = false)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    description = "Filter criteria for cheque return records (see examples).",
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "Global Search",
                                            value = """
                                            {
                                              "operator": "OR",
                                              "isDeleted": "N",
                                              "filters": [
                                                { "searchField": "GLOBALSEARCH", "searchValue": "CHQ" }
                                              ]
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "Multiple Fields (AND)",
                                            value = """
                                            {
                                              "operator": "AND",
                                              "isDeleted": "N",
                                              "filters": [
                                                { "searchField": "CHEQUE_NUMBER", "searchValue": "710211" },
                                                { "searchField": "TRANSACTION_POID", "searchValue": "282" }
                                              ]
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "Date Range",
                                            value = """
                                            {
                                              "operator": "AND",
                                              "isDeleted": "N",
                                              "filters": [
                                                { "searchField": "TRANSACTION_DATE", "searchValue": ">=2025-01-01" },
                                                { "searchField": "TRANSACTION_DATE", "searchValue": "<=2025-12-31" }
                                              ]
                                            }
                                            """
                                    )
                            }
                    )
            )
            FilterRequestDto filters,

            @RequestParam(required = false)
            @Parameter(description = "Start date (inclusive) for TRANSACTION_DATE filter")
            LocalDate startDate,

            @RequestParam(required = false)
            @Parameter(description = "End date (inclusive) for TRANSACTION_DATE filter")
            LocalDate endDate
    ) {
        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            return badRequest("Both startDate and endDate should be specified or both should be empty.");
        }

        Map<String, Object> ChequeReturnList = service.listOfRecordsAndGenericSearch(UserContext.getDocumentId(), filters, startDate, endDate, pageable);
        return success("Cheque Return list fetched successfully", ChequeReturnList);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/load")
    public ResponseEntity<?> loadChequeReturn(
            @RequestParam @Parameter(description = "Cheque number", required = true, example = "348138") String chequeNumber,
            @RequestParam(required = false) @Parameter(description = "Receipt number(Optional)", required = false, example = "") String receiptNo
    ) {
        ChequeReturnLoadResponseDto data = service.loadChequeData(chequeNumber, receiptNo);
        return success("Cheque Return load fetched successfully", data);
    }
}
