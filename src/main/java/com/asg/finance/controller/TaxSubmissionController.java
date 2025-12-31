package com.asg.finance.controller;

import com.asg.finance.dto.*;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.service.TaxSubmissionService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/tax-submission")
@RequiredArgsConstructor
@Slf4j
public class TaxSubmissionController {

    private final TaxSubmissionService taxSubmissionService;

    @Operation(
            summary = "Create tax submission",
            description = "Creates a new tax submission with header. Company defaults to session company (read-only).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully created tax submission",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxSubmissionResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input parameters or validation error",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @PostMapping
    public ResponseEntity<?> createTaxSubmission(
            @Parameter(description = "Tax submission creation request", required = true)
            @Valid @RequestBody CreateTaxSubmissionRequest request) {

        log.info("createTaxSubmission started for groupPoid={} userId={}", 
                UserContext.getGroupPoid(), UserContext.getUserId());
        
        TaxSubmissionResponse response = taxSubmissionService.createTaxSubmission(request);
        
        log.info("createTaxSubmission completed for transactionPoid={}", 
                response != null ? response.getTransactionPoid() : null);
        
        return success("Tax submission created successfully", response);
    }

    @Operation(
            summary = "Get tax submission by ID",
            description = "Retrieves tax submission details including header and detail lines",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved tax submission",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxSubmissionResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Tax submission not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getTaxSubmissionById(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid) {

        log.info("getTaxSubmissionById started for transactionPoid={} groupPoid={}", 
                transactionPoid, UserContext.getGroupPoid());
        
        TaxSubmissionResponse response = taxSubmissionService.getTaxSubmissionById(transactionPoid);
        
        log.info("getTaxSubmissionById completed for transactionPoid={}", transactionPoid);
        return success("Tax submission fetched successfully", response);
    }

    @Operation(
            summary = "Update tax submission",
            description = "Updates an existing tax submission. Cannot update if period is closed or submission is approved/posted.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated tax submission",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxSubmissionResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input parameters or validation error",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateTaxSubmission(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Parameter(description = "Tax submission update request", required = true)
            @Valid @RequestBody UpdateTaxSubmissionRequest request) {

        log.info("updateTaxSubmission started for transactionPoid={} groupPoid={}", 
                transactionPoid, UserContext.getGroupPoid());
        
        TaxSubmissionResponse response = taxSubmissionService.updateTaxSubmission(transactionPoid, request);
        
        log.info("updateTaxSubmission completed for transactionPoid={}", transactionPoid);
        return success("Tax submission updated successfully", response);
    }

    @Operation(
            summary = "Delete tax submission",
            description = "Deletes a tax submission. Cannot delete if period is closed or submission is approved/posted.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully deleted tax submission"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Cannot delete closed/approved/posted tax submission",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> deleteTaxSubmission(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid) {

        log.info("deleteTaxSubmission started for transactionPoid={} groupPoid={}", 
                transactionPoid, UserContext.getGroupPoid());
        
        taxSubmissionService.deleteTaxSubmission(transactionPoid);
        
        log.info("deleteTaxSubmission completed for transactionPoid={}", transactionPoid);
        return success("Tax submission deleted successfully", null);
    }

    @Operation(
            summary = "List Tax Submission with Search and Sort (DocId: 400-118)",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (TRANSACTION_POID, DOC_REF, CREATED_BY). Sorting default on transactionPoid, desc." +
                    "Will be searched in all available fields given in list_of_records_sql or main_table field in doc_master table." +
                    "Sorting will be applied as specified in list_of_records_sql in doc_master table." +
                    "Display fields for showing columns can be customized through list_of_display_columns_and_types field in doc_master.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    description = """
                        - ### Filters:
                          Use either:
                          1. A single `GLOBALSEARCH` filter, OR
                          2. Any combination of specific fields (TRANSACTION_POID, DOC_REF, CREATED_BY).
                          3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                             not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                          4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                          5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                        
                        - #### Global Search:
                          Apply one search term across multiple fields.
                          No operator need to send in this case.
                          • { "searchField": "GLOBALSEARCH", "searchValue": "2024" }
                        
                        - #### Single Field, Single Value:
                          Search one field with one value.
                          • { "searchField": "TRANSACTION_POID", "searchValue": "12345" },
                        
                        - #### Single Field, Multiple Values:
                          Provide multiple values for the same field, separated by `|`.
                          • { "searchField": "TRANSACTION_POID", "searchValue": "12345|12346" }
                        
                        - #### Multiple Different Fields, Single or Multiple Value:
                          Provide one or multiple search values across multiple fields.
                          • { "searchField": "TRANSACTION_POID", "searchValue": "12345" },
                          • { "searchField": "DOC_REF", "searchValue": "TS-2024-001|TS-2024-002" }
                          • "operator" :  "AND"/"OR"
                        
                        - ### Sorting:
                          Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                          To override, pass `sort=<field>,ASC|DESC` in query params.
                          Examples:
                          • sort=TRANSACTION_POID,ASC
                          • sort=DOC_REF,DESC
                        
                        - ### Authorization Parameters (handled by interceptor)
                            - **documentId:** Unique identifier for the document (`400-118`)
                            - **actionRequested:** Action being performed (`VIEW`)
                        """,
                    content = @Content(
                            schema = @Schema(implementation = FilterRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Tax Submission Filters",
                                            value = """
                                                {
                                                  "operator": "AND",
                                                  "isDeleted": "N",
                                                  "filters": [
                                                     { "searchField": "GLOBALSEARCH", "searchValue": "2024" },
                                                     { "searchField": "DOC_REF", "searchValue": "TS-2024-001|TS-2024-002" },
                                                     { "searchField": "TRANSACTION_POID", "searchValue": "12345" }
                                                  ]
                                                }
                                                """
                                    )
                            }
                    )
            )
    )
    @PostMapping("/list")
    public ResponseEntity<?> listTaxSubmission(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @Parameter(description = "Period From date filter")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @Parameter(description = "Period To date filter")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo) {
        try {
            Map<String, Object> data = taxSubmissionService.listTaxSubmission(filters, pageable, periodFrom, periodTo);
            return success("Tax submissions fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch Tax Submission list: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Load VAT details",
            description = "Loads VAT summary details from GL ledger and saves to breakup tables (equivalent to 'Load VAT Details' button)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully loaded VAT details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = LoadVatDetailsResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation error or period not set",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @PostMapping("/{transactionPoid}/load-vat-details")
    public ResponseEntity<?> loadVatDetails(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid) {

        log.info("loadVatDetails started for transactionPoid={}", transactionPoid);
        
        LoadVatDetailsResponse response = taxSubmissionService.loadVatDetails(transactionPoid);
        
        log.info("loadVatDetails completed for transactionPoid={} loadedCount={}", 
                transactionPoid, response.getDetails() != null ? response.getDetails().size() : 0);
        return success("VAT details loaded successfully", response);
    }

    @Operation(
            summary = "Submit or approve tax submission",
            description = "Submits tax submission for approval or approves directly",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully submitted/approved tax submission",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = SubmitTaxSubmissionResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation error or VAT details not loaded",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @PostMapping("/{transactionPoid}/submit")
    public ResponseEntity<?> submitTaxSubmission(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Parameter(description = "Submit request", required = true)
            @Valid @RequestBody SubmitTaxSubmissionRequest request) {

        log.info("submitTaxSubmission started for transactionPoid={} action={}", 
                transactionPoid, request.getAction());
        
        SubmitTaxSubmissionResponse response = taxSubmissionService.submitTaxSubmission(transactionPoid, request);
        
        log.info("submitTaxSubmission completed for transactionPoid={}", transactionPoid);
        return success("Tax submission submitted/approved successfully", response);
    }

    @Operation(
            summary = "Run after-save processing for tax submission",
            description = "Executes after-save logic (PROC_TAX_SUBMIN_AFTER_SAVE) for the given tax submission. " +
                    "This endpoint is typically called after the header has been created or updated.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "After-save processing completed successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxSubmissionResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation error during after-save processing",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @PostMapping("/{transactionPoid}/after-save")
    public ResponseEntity<?> runAfterSave(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid) {

        log.info("runAfterSave endpoint called for transactionPoid={}", transactionPoid);

        TaxSubmissionResponse response = taxSubmissionService.runAfterSave(transactionPoid);

        return success("Tax submission after-save processing completed successfully", response);
    }

    @Operation(
            summary = "Validate period",
            description = "Validates period rules and checks for existing submissions",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Period validation result",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ValidatePeriodResponse.class)
                            )
                    )
            }
    )
    @PostMapping("/validate-period")
    public ResponseEntity<?> validatePeriod(
            @Parameter(description = "Validate period request", required = true)
            @Valid @RequestBody ValidatePeriodRequest request) {

        log.info("validatePeriod started for companyId={} periodFrom={} periodTo={}", 
                request.getCompanyId(), request.getPeriodFrom(), request.getPeriodTo());
        
        ValidatePeriodResponse response = taxSubmissionService.validatePeriod(request);
        
        log.info("validatePeriod completed valid={}", response.getValid());
        return success("Period validated successfully", response);
    }

}

