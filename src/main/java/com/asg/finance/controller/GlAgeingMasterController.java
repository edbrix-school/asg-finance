package com.asg.finance.controller;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.GlAgeingMasterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.GlAgeingMasterResponseDto;
import com.asg.finance.service.GlAgeingMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/ageing")
public class GlAgeingMasterController {

    private final GlAgeingMasterService ageingMasterService;

    @Operation(
            summary = "Create Ageing Master",
            description = "Creates a new ageing master record with ageing breakup details. The ageing description must be unique.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Ageing master created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = GlAgeingMasterResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data or validation errors"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Ageing description already exists"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Ageing master data with breakup details",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GlAgeingMasterDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Customer Ageing Example",
                                    summary = "Create customer ageing with monthly breakup",
                                    value = """
                                            {
                                              "groupPoid": 1,
                                              "description": "Customer Ageing",
                                              "description2": "Monthly ageing breakup",
                                              "ageingBreakupType": "MONTHLY",
                                              "seqno": 1,
                                              "active": true,
                                              "ageingDetails": [
                                                {
                                                  "breakupTitle": "0-30 Days",
                                                  "breakupFrom": 0,
                                                  "breakupTo": 30
                                                },
                                                {
                                                  "breakupTitle": "31-60 Days",
                                                  "breakupFrom": 31,
                                                  "breakupTo": 60
                                                },
                                                {
                                                  "breakupTitle": "61-90 Days",
                                                  "breakupFrom": 61,
                                                  "breakupTo": 90
                                                },
                                                {
                                                  "breakupTitle": "90-120 Days",
                                                  "breakupFrom": 91,
                                                  "breakupTo": 120
                                                }
                                              ]
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping
    public ResponseEntity<?> createAgeingMaster(
            @Valid @RequestBody GlAgeingMasterDto ageingMasterDto
    ) {
        try {
            GlAgeingMasterResponseDto response = ageingMasterService.createAgeingMaster(ageingMasterDto);
            return success("Ageing Master created successfully", response);
        } catch (RuntimeException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to create ageing master: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get Ageing Master Details",
            description = "Retrieves detailed information about a specific ageing master record including all breakup details",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved ageing master details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = GlAgeingMasterDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Ageing master record not found"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )
    @GetMapping("/{ageingPoid}")
    public ResponseEntity<?> getAgeingMasterDetails(
            @Parameter(
                    description = "Unique identifier of the ageing master record",
                    required = true,
                    example = "12"
            )
            @PathVariable Long ageingPoid
    ) {
        try {
            GlAgeingMasterDto ageingMaster = ageingMasterService.fetchAgeingMaster(ageingPoid);
            // Return raw DTO as per acceptance criteria
            return success("Ageing Master Details fetched successfully", ageingMaster);
        } catch (ResourceNotFoundException rnfe) {
            return notFound(rnfe.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to fetch ageing master: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Update Ageing Master",
            description = "Updates an existing ageing master record with ageing breakup details. For existing details, provide detRowId. For new details, set detRowId to null. Details not included will be deleted. The ageing description must remain unique.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Ageing master updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = GlAgeingMasterDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data or validation errors"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Ageing master not found"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Ageing description already exists"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )
    @PutMapping("/{ageingPoid}")
    public ResponseEntity<?> updateAgeingMaster(
            @Parameter(
                    description = "Unique identifier of the ageing master record to update",
                    required = true,
                    example = "12"
            )
            @PathVariable Long ageingPoid,
            @Valid @RequestBody GlAgeingMasterDto ageingMasterDto
    ) {
        try {
            GlAgeingMasterDto updatedAgeingMaster = ageingMasterService.updateAgeingMaster(ageingPoid, ageingMasterDto);
            
            return success("Ageing Master updated successfully", updatedAgeingMaster);
        } catch (RuntimeException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to update ageing master: " + ex.getMessage());
        }
    }
    @Operation(
            summary = "Soft delete an Ageing Master",
            description = "Marks an Ageing Master as deleted without permanently removing its data",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully soft deleted the Ageing Master",
                            content = @Content(
                                    mediaType = "application/json"
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Ageing Master not found",
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
    @DeleteMapping("/{ageingPoid}")
    public ResponseEntity<?> softDeleteAgeingMaster(
            @Parameter(description = "Unique identifier of the ageing record", required = true)
            @PathVariable Long ageingPoid
    ) {
        try {
            ageingMasterService.softDeleteAgeingMaster(ageingPoid);
            return success("Ageing Master has been soft deleted successfully");
        } catch (ResourceNotFoundException ex) {
            return notFound("Ageing Master not found with ID: " + ageingPoid);
        } catch (Exception ex) {
            return internalServerError("Failed to delete Ageing Master: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "List Ageing Masters with Search and Sort",
            description ="Provide search filters. Valid `searchField` values: GLOBALSEARCH or (DESCRIPTION, DESCRIPTION2, AGEING_BREAKUP_TYPE). Sorting default on ageingPoid, desc." +
                    "Will be searched in all available fields given in list_of_records_sql or main_table field in doc_master table." +
                    "Sorting will be applied as specified in list_of_records_sql in doc_master table." +
                    "Display fields for showing columns can be customized through list_of_display_columns_and_types field in doc_master."

            ,security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields (DESCRIPTION, DESCRIPTION2, AGEING_BREAKUP_TYPE).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR"
                      4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                    
                    ### Authorization Parameters (handled by interceptor)
                                - **documentId:** Unique identifier for the document (`400-003`) \s
                                - **actionRequested:** Action being performed (`VIEW`)""",
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Ageing Master Filters",
                                    value = """
                                {
                                  "operator": "OR",
                                  "isDeleted": "N",
                                  "filters": [
                                    { "searchField": "DESCRIPTION", "searchValue": "Audit" },
                                    { "searchField": "DESCRIPTION", "searchValue": "Monthly ageing breakup" },
                                    { "searchField": "AGEING_BREAKUP_TYPE", "searchValue": "DAYS" }
                                  ]
                                }
                                """
                            )
                    }
            )
    )
    @PostMapping("/list")
    public ResponseEntity<?> listAgeingMasters(@ParameterObject Pageable pageable,
                                               @RequestBody(required = false) FilterRequestDto filters) {
        try {
            Map<String, Object> data = ageingMasterService.listAgeingMasters(UserContext.getDocumentId(), filters, pageable);
            return success("Ageing Masters fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch ageing master list: " + ex.getMessage());
        }
    }
}
