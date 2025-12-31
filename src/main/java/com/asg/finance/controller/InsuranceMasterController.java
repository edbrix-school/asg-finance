package com.asg.finance.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.masters.InsuranceMasterRequestDto;
import com.asg.finance.dto.masters.InsuranceMasterResponseDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.service.InsuranceMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.internalServerError;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/v1/insurance-master")
@Tag(name = "Insurance Master", description = "APIs for managing Insurance Master records")
@RequiredArgsConstructor
public class InsuranceMasterController {

    private final InsuranceMasterService insuranceMasterService;

    @Operation(
            summary = "Create a new Insurance Master",
            description = "Creates a new Insurance Master with the provided details including vehicle, employee, property, and PIC details",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created the Insurance Master",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = InsuranceMasterResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input, object invalid",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<?> createInsuranceMaster(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Insurance Master object that needs to be created",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InsuranceMasterRequestDto.class)
                    )
            )
            @Parameter(description = "Insurance Master details to be created", required = true)
            @Valid @RequestBody InsuranceMasterRequestDto request
    ) {
        try {
            InsuranceMasterResponseDto response = insuranceMasterService.createInsuranceMaster(request);
            return success("Insurance Master created successfully", response);
        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to create Insurance Master: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Soft delete Insurance Master",
            description = "Soft deletes an Insurance Master record if not linked to any project reference",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Successfully deleted the Insurance Master"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Cannot delete - Insurance linked with Project Reference"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Insurance Master not found"
                    )
            }
    )
    @DeleteMapping("/{insuranceId}")
    public ResponseEntity<?> softDeleteInsuranceMaster(
            @Parameter(description = "insuranceId reference identifier", required = true)
            @PathVariable Long insuranceId) {

        insuranceMasterService.softDeleteInsuranceMaster(insuranceId);
        return success("Insurance Master has been soft deleted successfully");
    }

    @Operation(
            summary = "Get Insurance Master by ID",
            description = "Retrieves Insurance Master details based on the provided Insurance Master ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the Insurance Master details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = InsuranceMasterResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input parameters",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Insurance Master not found",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{insuranceId}")
    public ResponseEntity<?> getInsuranceMasterById(
            @Parameter(description = "insuranceId reference identifier", required = true)
            @PathVariable Long insuranceId) {

        InsuranceMasterResponseDto responseDto = insuranceMasterService.getInsuranceMasterById(insuranceId);
        return success("Insurance Master fetched successfully", responseDto);
    }

    @Operation(
            summary = "Update Insurance Master",
            description = "Updates an existing Insurance Master record. PJ Reference field is preserved and read-only.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated the Insurance Master",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = InsuranceMasterResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input, object invalid",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Insurance Master not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @PutMapping("/{insuranceId}")
    public ResponseEntity<?> updateInsuranceMaster(
            @Parameter(description = "Insurance Master ID", required = true)
            @PathVariable Long insuranceId,
            @Valid @RequestBody InsuranceMasterRequestDto request
    ) {
        try {
            InsuranceMasterResponseDto response = insuranceMasterService.updateInsuranceMaster(insuranceId, request);
            return success("Insurance Master updated successfully", response);
        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to update Insurance Master: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "List Insurance Masters with Search and Sort",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (INSURANCE_TYPE, INSURANCE_CATEGORY, POLICY_NO, INSURANCE_PROVIDER, STATUS, EXPIRY_DATE). " +
                    "Sorting defaults to transactionPoid descending. " +
                    "The search will be performed across all fields listed in `list_of_records_sql` or the main table field in `GLOBAL_DOC_MASTER`."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields (INSURANCE_TYPE, INSURANCE_CATEGORY, POLICY_NO, INSURANCE_PROVIDER, STATUS, EXPIRY_DATE).
                      3. `operator` field will either have "AND" or "OR". If not provided, it will default to "OR".
                         Not required for GLOBALSEARCH. For non-global searches, provide it only once.
                      4. `isDeleted` when 'N' or null will search and return non-deleted records; 'Y' will include deleted ones.
                      5. `sort` will default to the primary key ascending if specified in DB, otherwise, you can override it by providing field name and direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      No operator needs to be sent in this case.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "Property" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      • { "searchField": "INSURANCE_TYPE", "searchValue": "Property Insurance" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "INSURANCE_CATEGORY", "searchValue": "Motor|Property" }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "INSURANCE_TYPE", "searchValue": "Property Insurance" },
                      • { "searchField": "STATUS", "searchValue": "Active|Expired" },
                      • "operator": "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field (mostly primary key ascending).
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=POLICY_NO,ASC
                      • sort=EXPIRY_DATE,DESC
                    
                    - ### Authorization Parameters (handled by interceptor)
                        - **documentId:** Unique identifier for the document (`600-001`)
                        - **actionRequested:** Action being performed (`VIEW`)
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Insurance Master Filters",
                                    value = """
                                            {
                                              "operator": "AND",
                                              "isDeleted": "N",
                                              "filters": [
                                                 { "searchField": "GLOBALSEARCH", "searchValue": "Property" },
                                                 { "searchField": "INSURANCE_TYPE", "searchValue": "Property Insurance|Motor Insurance" },
                                                 { "searchField": "INSURANCE_CATEGORY", "searchValue": "Motor" },
                                                 { "searchField": "POLICY_NO", "searchValue": "PROP456" },
                                                 { "searchField": "INSURANCE_PROVIDER", "searchValue": "ABC Insurance" },
                                                 { "searchField": "STATUS", "searchValue": "Active" },
                                                 { "searchField": "EXPIRY_DATE", "searchValue": "2024-01-01|2024-12-31" }
                                              ]
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping("/list")
    public ResponseEntity<?> getInsuranceMasters(@ParameterObject Pageable pageable,
                                                 @RequestBody(required = false) FilterRequestDto filters,
                                                 @Parameter(description = "Start date for filtering")
                                                 @RequestParam(required = false) String startDate,
                                                 @Parameter(description = "End date for filtering")
                                                 @RequestParam(required = false) String endDate) {
        try {
            java.time.LocalDate startDateValue = startDate != null ? java.time.LocalDate.parse(startDate) : null;
            java.time.LocalDate endDateValue = endDate != null ? java.time.LocalDate.parse(endDate) : null;
            Map<String, Object> data = insuranceMasterService.listInsuranceMasters(UserContext.getDocumentId(), filters, startDateValue, endDateValue, pageable);
            return success("Insurance Masters fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch Insurance Master list: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Renew Insurance",
            description = "Archives current insurance details to renewal log and updates with new renewal terms",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully renewed the Insurance",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = InsuranceMasterResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Insurance Master not found"
                    )
            }
    )
    @PostMapping("/{insuranceId}/renew")
    public ResponseEntity<?> renewInsurance(
            @Parameter(description = "Insurance Master ID", required = true)
            @PathVariable Long insuranceId,
            @Valid @RequestBody InsuranceMasterRequestDto request
    ) {
        try {
            InsuranceMasterResponseDto response = insuranceMasterService.renewInsurance(insuranceId, request);
            return success("Insurance renewed successfully", response);
        } catch (Exception ex) {
            return internalServerError("Failed to renew Insurance: " + ex.getMessage());
        }
    }
}