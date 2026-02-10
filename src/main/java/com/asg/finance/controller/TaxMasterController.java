package com.asg.finance.controller;


import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.dto.TaxMasterRequestDTO;
import com.asg.finance.dto.TaxMasterResponseDTO;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.service.TaxMasterService;
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

import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.internalServerError;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/v1/tax-master")
@RequiredArgsConstructor
public class TaxMasterController {
    private final TaxMasterService service;
    private final LoggingService loggingService;

    @Operation(
            summary = "Create a new Tax Master",
            description = "Creates a new tax master with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created the Tax Master",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxMasterResponseDTO.class)
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
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Tax Master with the same code already exists",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createTaxMaster(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Tax Master object that needs to be created",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaxMasterRequestDTO.class)
                    )
            )
            @Parameter(description = "Tax Master details to be created", required = true)
            @Valid @RequestBody TaxMasterRequestDTO requestDTO
    ) {
        try {
            TaxMasterResponseDTO response = service.createTaxMaster(requestDTO);
            return success("Tax Master created successfully", response);

        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to create tax master " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Update an existing Tax Master",
            description = "Updates the tax master with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated the Tax Master",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxMasterResponseDTO.class)
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
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Tax Master not found",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Duplicate Tax Code exists",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{taxPoid}")
    public ResponseEntity<?> updateTaxMaster(
            @Parameter(description = "Tax Master taxPoid to be updated", required = true)
            @PathVariable Long taxPoid,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated Tax Master details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaxMasterRequestDTO.class)
                    )
            )
            @Valid @RequestBody TaxMasterRequestDTO requestDTO
    ) {
        try {
            TaxMasterResponseDTO response = service.updateTaxMaster(taxPoid, requestDTO);
            return success("Tax Master updated successfully", response);

        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to update tax master " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get Tax Master by taxPoid",
            description = "Retrieves tax master details based on the provided tax master POID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the tax master details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxMasterResponseDTO.class)
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
                            description = "Tax Master not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{taxPoid}")
    public ResponseEntity<?> getTaxMasterByTaxMasterPoid(
            @Parameter(description = "taxPoid reference identifier", required = true)
            @PathVariable Long taxPoid) {

        TaxMasterResponseDTO taxMasterResponseDTO = service.getTaxMasterById(taxPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), taxPoid.toString());
        return success("Tax Master fetched successfully", taxMasterResponseDTO);
    }

    @Operation(
            summary = "Get Tax Master by taxPoid (Simple)",
            description = "Retrieves tax master details without auth parameters - for internal service calls"
    )
    @GetMapping("/simple/{taxPoid}")
    public ResponseEntity<?> getTaxMasterSimple(@PathVariable Long taxPoid) {
        com.asg.common.lib.dto.TaxMasterDto taxMasterDto = service.getTaxMasterDtoById(taxPoid);
        return success("Tax Master fetched successfully", taxMasterDto);
    }

    @Operation(
            summary = "Get multiple Tax Masters by taxPoids (Batch)",
            description = "Retrieves multiple tax master details - for internal service calls"
    )
    @PostMapping("/batch")
    public ResponseEntity<?> getTaxMastersBatch(@RequestBody List<Long> taxPoids) {
        List<com.asg.common.lib.dto.TaxMasterDto> taxMasters = service.getTaxMasterDtosByIds(taxPoids);
        return success("Tax Masters fetched successfully", taxMasters);
    }

    @Operation(
            summary = "Soft delete a TaxMaster",
            description = "Marks a TaxMaster as deleted without permanently removing its data",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully soft deleted the TaxMaster",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxMasterRequestDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "TaxMaster not found",
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
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{taxPoid}")
    public ResponseEntity<?> softDeleteTaxMaster(
            @Parameter(description = "taxPoid reference identifier", required = true)
            @PathVariable Long taxPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {

        service.softDeleteTaxMaster(taxPoid, deleteReasonDto);
        return success("Tax Master has been deleted successfully");
    }

    @Operation(
            summary = "List Petty Cash User Role with Search and Sort (DocId: 400-008)",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (TAX_CODE, TAX_TYPE, ACTIVE, CREATED_BY). Sorting default on taxPoid, desc." +
                    "Will be searched in all available fields given in list_of_records_sql or main_table field in doc_master table." +
                    "Sorting will be applied as specified in list_of_records_sql in doc_master table." +
                    "Display fields for showing columns can be customized through list_of_display_columns_and_types field in doc_master."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields (TAX_CODE, TAX_TYPE, ACTIVE, CREATED_BY).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                         not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                      4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                      5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      No operator need to send in this case.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "TX16" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      • { "searchField": "TAX_CODE", "searchValue": "TX16" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "TAX_CODE", "searchValue": "TX16|TX17" }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "TAX_CODE", "searchValue": "TX16" },
                      • { "searchField": "TAX_TYPE", "searchValue": "INPUT_VAT|OUTPUT_VAT" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=TAX_CODE,ASC
                      • sort=TAX_TYPE,DESC
                    
                    - ### Authorization Parameters (handled by interceptor)
                        - **documentId:** Unique identifier for the document (`400-010`)
                        - **actionRequested:** Action being performed (`VIEW`)
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "TaxMaster Filters",
                                    value = """
                                            {
                                            "operator": "AND",
                                            "isDeleted": "N",
                                            "filters": [
                                               { "searchField": "GLOBALSEARCH", "searchValue": "TX16" },
                                               { "searchField": "TAX_CODE", "searchValue": "TX16" },
                                               { "searchField": "TAX_TYPE", "searchValue": "INPUT_VAT"},
                                               { "searchField": "ACTIVE", "searchValue": "Y"},
                                               { "searchField": "CREATED_BY", "searchValue": "Admin"}
                                            ]
                                            }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listTaxMaster(@ParameterObject Pageable pageable,
                                           @RequestBody(required = false) FilterRequestDto filters) {
        try {
            Map<String, Object> data = service.listTaxMaster(UserContext.getDocumentId(), filters, pageable);
            return success("Tax Master fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch taxMaster list: " + ex.getMessage());
        }

    }

}
