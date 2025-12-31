package com.asg.finance.controller;

import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.FixedAssetCategoryRequestDto;
import com.asg.finance.dto.FixedAssetCategoryResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.service.FixedAssetCategoryService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/asset-category")
@RequiredArgsConstructor
public class FixedAssetCategoryController {


    @Autowired
    private FixedAssetCategoryService fixedAssetCategoryService;

    @Operation(
            summary = "Create a new Fixed Asset Category",
            description = "Creates a new Fixed Asset Category with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created the Fixed Asset Category",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FixedAssetCategoryResponseDto.class)
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
    public ResponseEntity<?> createFixedAssetCategory(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Fixed Asset Category object that needs to be created",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FixedAssetCategoryRequestDto.class)
                    )
            )
            @Parameter(description = "Fixed Asset Category details to be created", required = true)
            @Valid @RequestBody FixedAssetCategoryRequestDto request
    ) {
        try {
            FixedAssetCategoryResponseDto response = fixedAssetCategoryService.createFixedAssetCategory(request);
            return success("Fixed Asset Category created successfully", response);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed To Create Fixed Asset Category: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Update an existing Fixed Asset Category",
            description = "Updates the Fixed Asset Category with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated the Fixed Asset Category",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FixedAssetCategoryResponseDto.class)
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
                            description = "Fixed Asset Category not found",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Duplicate Fixed Asset Category exists",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )

    @PutMapping("/{faCategoryPoid}")
    public ResponseEntity<?> updateFixedAssetCategory(
            @Parameter(description = "Fixed Asset Category faCategoryPoid to be updated", required = true)
            @PathVariable Long faCategoryPoid,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated Fixed Asset Category details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FixedAssetCategoryRequestDto.class)
                    )
            )
            @Valid @RequestBody FixedAssetCategoryRequestDto requestDTO
    ) {
        try {
            FixedAssetCategoryResponseDto response = fixedAssetCategoryService.updateFixedAssetCategory(faCategoryPoid, requestDTO);
            return success("Fixed Asset Category updated successfully", response);

        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to update Fixed Asset Category: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get Fixed Asset Category by faCategoryPoid",
            description = "Retrieves Fixed Asset Category details based on the provided Fixed Asset Category faCategoryPoid",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the Fixed Asset Category details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FixedAssetCategoryResponseDto.class)
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
                            description = "Fixed Asset Category not found",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{faCategoryPoid}")
    public ResponseEntity<?> getFixedAssetCategory(
            @Parameter(description = "faCategoryPoid reference identifier", required = true)
            @PathVariable Long faCategoryPoid
    ) {

        FixedAssetCategoryResponseDto responseDto = fixedAssetCategoryService.getFixedAssetCategory(faCategoryPoid);
        return success("Fixed Asset Category fetched successfully", responseDto);
    }

    @Operation(
            summary = "Soft delete a Fixed Asset Category",
            description = "Marks a Fixed Asset Category as deleted without permanently removing its data",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully soft deleted the Fixed Asset Category",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FixedAssetCategoryRequestDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Fixed Asset Category not found",
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
    @DeleteMapping("/{faCategoryPoid}")
    public ResponseEntity<?> softDeleteFixedAssetCategory(
            @Parameter(description = "faCategoryPoid reference identifier", required = true)
            @PathVariable Long faCategoryPoid
    ) {

        fixedAssetCategoryService.softDeleteFixedAssetCategory(faCategoryPoid);
        return success("Fixed Asset Category has been soft deleted successfully");
    }


    @Operation(
            summary = "List Fixed Asset Categories with Search and Sort (DocId: 600-003)",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (CATEGORY_CODE, CATEGORY_NAME, ACTIVE, CREATED_BY). " +
                    "Will be searched in all available fields given in list_of_records_sql or main_table field in doc_master table. " +
                    "Sorting will be applied as specified in list_of_records_sql in doc_master table. " +
                    "Display fields for showing columns can be customized through list_of_display_columns_and_types field in doc_master."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields (CATEGORY_CODE, CATEGORY_NAME, ACTIVE, CREATED_BY).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                         not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                      4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                      5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      No operator need to send in this case.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "Electronics" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      • { "searchField": "CATEGORY_CODE", "searchValue": "CAT01" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "CATEGORY_CODE", "searchValue": "CAT01|CAT02" }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "CATEGORY_CODE", "searchValue": "CAT01" },
                      • { "searchField": "CATEGORY_NAME", "searchValue": "Electronics|Furniture" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=CATEGORY_NAME,ASC
                      • sort=CATEGORY_CODE,DESC
                    
                    - ### Authorization Parameters (handled by interceptor)
                        - **documentId:** Unique identifier for the document (`600-003`)
                        - **actionRequested:** Action being performed (`VIEW`)
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Fixed Asset Category Filters",
                                    value = """
                                            {
                                              "operator": "AND",
                                              "isDeleted": "N",
                                              "filters": [
                                                 { "searchField": "GLOBALSEARCH", "searchValue": "Electronics" },
                                                 { "searchField": "CATEGORY_CODE", "searchValue": "CAT01|CAT02" },
                                                 { "searchField": "CATEGORY_NAME", "searchValue": "Computers" },
                                                 { "searchField": "ACTIVE", "searchValue": "Y" },
                                                 { "searchField": "CREATED_BY", "searchValue": "Admin" }
                                              ]
                                            }
                                            """
                            )
                    }
            )
    )

    @PostMapping("/list")
    public ResponseEntity<?> getFixedAssetCategories(@ParameterObject Pageable pageable,
                                                     @RequestBody(required = false) FilterRequestDto filters) {
        try {
            Map<String, Object> data = fixedAssetCategoryService.listFixedAssetCategories(UserContext.getDocumentId(), filters, pageable);
            return success("Fixed Asset Categories fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch Fixed Asset Category list: " + ex.getMessage());
        }
    }
}


