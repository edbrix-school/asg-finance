package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.AssetInformationMasterRequest;
import com.asg.finance.dto.AssetInformationMasterResponse;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.service.AssetInformationService;
import com.asg.finance.validation.OnUpdate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.success;


@RestController
@RequestMapping("/v1/asset-information")
@Slf4j
public class AssetInformationController {

    @Autowired
    private AssetInformationService assetInformationService;

    @Operation(
            summary = "List Asset Information with Search and Sort",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (IA_CODE, IA_NAME, IA_DESCRIPTION, ASSET_CUSTODIAN, OPERATING_UNIT, TYPE_OF_INFORMATION_ASSET, STATUS). Sorting default on iaPoid, desc." +
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
                      2. Any combination of specific fields (IA_CODE, IA_NAME, IA_DESCRIPTION, ASSET_CUSTODIAN, OPERATING_UNIT, TYPE_OF_INFORMATION_ASSET, STATUS).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                         not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                      4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                      5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      No operator need to send in this case.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "asset" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      • { "searchField": "IA_CODE", "searchValue": "AST001" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "IA_CODE", "searchValue": "AST001|AST002" }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "IA_CODE", "searchValue": "AST001" },
                      • { "searchField": "IA_NAME", "searchValue": "Database|Server" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=IA_NAME,ASC
                      • sort=IA_CODE,DESC
                    
                    - ### Authorization Parameters (handled by interceptor)
                        - **documentId:** Unique identifier for the document
                        - **actionRequested:** Action being performed (`VIEW`)
                    """,
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = FilterDto.class)),
                    examples = {
                            @ExampleObject(
                                    name = "Asset Information Filters",
                                    value = """
                                            {
                                            "operator": "AND",
                                            "isDeleted": "N",
                                            "filters": [
                                               { "searchField": "GLOBALSEARCH", "searchValue": "database" },
                                               { "searchField": "IA_CODE", "searchValue": "AST001|AST002" },
                                               { "searchField": "IA_NAME", "searchValue": "Database"},
                                               { "searchField": "ASSET_CUSTODIAN", "searchValue": "IT Department"},
                                               { "searchField": "OPERATING_UNIT", "searchValue": "Finance"}
                                            ]
                                            }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listAssetInformation(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters
    ) {
        Map<String, Object> assetInformation = assetInformationService.listAssetInformation(UserContext.getDocumentId(), filters, pageable);
        return success("Asset Information list fetched successfully", assetInformation);
    }

    @Operation(
            summary = "Create Information Asset Master",
            description = "Create a new Information Asset Master record with metadata about ownership, classification, data sensitivity, retention, and accessibility.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Information Asset Master created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AssetInformationMasterResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data or validation errors"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Information Asset Master data",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AssetInformationMasterRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "Create Information Asset Master",
                                    summary = "Sample request to create a new information asset master",
                                    value = """
                                            {
                                              "iaCode": "AST001",
                                              "iaName": "Customer Database",
                                              "iaDescription": "Primary customer information database",
                                              "operatingUnit": "IT Department",
                                              "typeOfInformationAsset": "Database",
                                              "personalData": "Y",
                                              "personalSensitiveData": "N",
                                              "sensitiveCustomerData": "Y",
                                              "assetClassification": "Confidential",
                                              "integrity": "High",
                                              "availability": "24/7",
                                              "dataRetentionPeriod": "7 years",
                                              "assetCustodian": "IT Security Team",
                                              "active": "Y"
                                            }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createAssetInformation(
            @Valid @RequestBody AssetInformationMasterRequest request
    ) {
        AssetInformationMasterResponse response = assetInformationService.createAssetInformation(request);
        Map<String, Object> data = Map.of("iaPoid", response.getIaPoid());
        return success("Information Asset Master created successfully", data);
    }


    @Operation(
            summary = "Update Information Asset Master",
            description = "Update an existing Information Asset Master record with new metadata and classification information.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Information Asset Master updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AssetInformationMasterResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data or validation errors"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Information Asset Master not found"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{iaPoid}")
    public ResponseEntity<?> updateAssetInformation(
            @Parameter(description = "Unique identifier of the information asset master", required = true, example = "1")
            @PathVariable Long iaPoid,
            @Validated(OnUpdate.class) @RequestBody AssetInformationMasterRequest request
    ) {
        AssetInformationMasterResponse response = assetInformationService.updateAssetInformation(iaPoid, request);
        return success("Information Asset Master updated successfully", response);
    }

    @Operation(
            summary = "Get Asset Information by ID",
            description = "Returns an Asset Information record by its POID, including audit metadata"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Record found"),
            @ApiResponse(responseCode = "404", description = "Record not found")
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{iaPoid}")
    public ResponseEntity<?> getAssetInformationById(@PathVariable Long iaPoid) {

        AssetInformationMasterResponse response = assetInformationService.getAssetInformationByPoidId(iaPoid);
        return success("Asset Information fetched successfully", response);
    }

    @Operation(
            summary = "Soft delete Asset Information by ID",
            description = "Marks an Asset Information record as inactive instead of removing it from the database."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Record successfully soft deleted"),
            @ApiResponse(responseCode = "404", description = "Record not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized – JWT token missing or invalid"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{iaPoid}")
    public ResponseEntity<?> softDeleteAssetInformation(@PathVariable Long iaPoid) {

        assetInformationService.softDeleteAssetInformationByPoidId(iaPoid);
        return success("Successfully deleted the Assert Information", Map.of("message", "Asset Information with ID " + iaPoid + " has been soft deleted"));
    }

}
