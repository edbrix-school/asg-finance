package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;

import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.AssetLocationMasterRequestDto;
import com.asg.finance.dto.AssetLocationMasterResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.service.AssetLocationMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;


import java.util.*;

import static com.asg.common.lib.dto.response.ApiResponse.*;


@RestController
@RequestMapping("/v1/asset-location")
@Tag(name = "Asset Location Master", description = "APIs for managing and retrieving Asset Location Master records")
public class AssetLocationMasterController {

    @Autowired
    private AssetLocationMasterService service;

    @Autowired
    AssetLocationMasterService assetLocationService;

    @Operation(
            summary = "Create a new AssetLocation Master",
            description = "Creates a new AssetLocation with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created the AssetLocation Master",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AssetLocationMasterResponseDto.class)
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
                            description = "AssetLocation  with the same code already exists",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createAssetLocation(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "AssetLocation  Master object that needs to be created",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AssetLocationMasterRequestDto.class)
                    )
            )
            @Parameter(description = "AssetLocation  details to be created", required = true)
            @Valid @RequestBody AssetLocationMasterRequestDto requestDTO
    ) {

        try {
            AssetLocationMasterResponseDto response = service.createAssetLocationMaster(requestDTO);
            return success("Asset Location Master created successfully", response);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    @Operation(
            summary = "Update an existing Asset Location  Master",
            description = "Updates the AssetLocation  with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated the AssetLocation  Master",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AssetLocationMasterResponseDto.class)
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
                            description = "AssetLocation  Master not found",
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
    @PutMapping("/{locationPoid}")
    public ResponseEntity<?> updateAssetLocationMaster(
            @Parameter(description = "AssetLocation  ID (Poid) to update", required = true, example = "51001")
            @PathVariable Long locationPoid,

            @Parameter(description = "Updated CostCenter details", required = true)
            @Valid @RequestBody AssetLocationMasterRequestDto assetLocationMasterRequestDtoDto
    ) {
        try {
            AssetLocationMasterResponseDto response = service.updateAssetLocationMaster(locationPoid, assetLocationMasterRequestDtoDto);
            return success("Asset Location Master updated successfully", response);

        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError(ex.getMessage());
        }
    }

    @Operation(
            summary = "Soft delete a AssetLocation ",
            description = "Marks a AssetLocation  as deleted without permanently removing its data",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully soft deleted the AssetLocation ",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AssetLocationMasterRequestDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "AssetLocation  not found",
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
    @DeleteMapping("/{locationPoid}")
    public ResponseEntity<?> softDeleteAssetMaster(
            @Parameter(description = "locationPoid reference identifier", required = true)
            @PathVariable Long locationPoid) {

        service.softDeleteAssetLocationMaster(locationPoid);
        return success("Asset Location Master has been soft deleted successfully");
    }

    @Operation(
            summary = "Get AssetLocation  by locationPoid",
            description = "Retrieves AssetLocation  details based on the provided location POID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the AssetLocation  details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AssetLocationMasterResponseDto.class)
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
                            description = "AssetLocation  not found",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{locationPoid}")
    public ResponseEntity<?> getAssetMasterById(
            @Parameter(description = "locationPoid reference identifier", required = true)
            @PathVariable Long locationPoid) {

        AssetLocationMasterResponseDto assetLocationMasterResponseDto = service.getAssetLocationMasterById(locationPoid);
        return success("Asset Location Master fetched successfully", assetLocationMasterResponseDto);
    }


    @Operation(
            summary = "List Asset Locations with Search and Sort (DocId: 000-XXX)",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (LOCATION_CODE, LOCATION_NAME, ACTIVE, CREATED_BY). Sorting default on locationPoid, desc." +
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
                      2. Any combination of specific fields (LOCATION_CODE, LOCATION_NAME, ACTIVE, CREATED_BY).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                         not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                      4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                      5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      No operator need to send in this case.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "Warehouse" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      • { "searchField": "LOCATION_CODE", "searchValue": "LOC01" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "LOCATION_CODE", "searchValue": "LOC01|LOC02" }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "LOCATION_CODE", "searchValue": "LOC01" },
                      • { "searchField": "LOCATION_NAME", "searchValue": "Head Office|Branch" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=LOCATION_NAME,ASC
                      • sort=LOCATION_CODE,DESC
                    
                    - ### Authorization Parameters (handled by interceptor)
                        - **documentId:** Unique identifier for the document (`000-XXX`)
                        - **actionRequested:** Action being performed (`VIEW`)
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Asset Location Filters",
                                    value = """
                                            {
                                              "operator": "AND",
                                              "isDeleted": "N",
                                              "filters": [
                                                 { "searchField": "GLOBALSEARCH", "searchValue": "Warehouse" },
                                                 { "searchField": "LOCATION_CODE", "searchValue": "LOC01|LOC02" },
                                                 { "searchField": "LOCATION_NAME", "searchValue": "Main Office" },
                                                 { "searchField": "ACTIVE", "searchValue": "Y" },
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
    public ResponseEntity<?> getAssetLocations(@ParameterObject Pageable pageable,
                                               @RequestBody(required = false) FilterRequestDto filters) {
        try {

            Map<String, Object> data = assetLocationService.listAssetLocations(UserContext.getDocumentId(), filters, pageable);
            return success("Asset Location Master fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch Asset Location list: " + ex.getMessage());
        }
    }


}
