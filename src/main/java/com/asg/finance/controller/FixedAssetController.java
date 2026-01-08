package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.masters.FixedAssetRequestDto;
import com.asg.finance.dto.masters.FixedAssetResponseDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.service.FixedAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.internalServerError;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/v1/fixed-asset")
public class FixedAssetController {
    private final FixedAssetService fixedAssetService;

    public FixedAssetController(FixedAssetService fixedAssetService) {
        this.fixedAssetService = fixedAssetService;
    }
    @Operation(
            summary = "Create a new Fixed Asset",
            description = "Creates a new Fixed Asset with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created the Fixed Asset",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FixedAssetResponseDto.class)
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
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createFixedAsset(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Fixed Asset object that needs to be created",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FixedAssetRequestDto.class)
                    )
            )
            @Parameter(description = "Fixed Asset details to be created", required = true)
            @Valid @RequestBody FixedAssetRequestDto request
    ) {
        try {
            FixedAssetResponseDto response = fixedAssetService.createFixedAsset(request);
            return success("Fixed Asset created successfully", response);
        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to create Fixed Asset " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Update an existing Fixed Asset",
            description = "Updates the Fixed Asset with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated the Fixed Asset",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FixedAssetResponseDto.class)
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
                            description = "Fixed Asset not found",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Duplicate Fixed Asset exists",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{faPoid}")
    public ResponseEntity<?> updateFixedAsset(
            @Parameter(description = "Fixed Asset faPoid to be updated", required = true)
            @PathVariable Long faPoid,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated Fixed Asset details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FixedAssetRequestDto.class)
                    )
            )
            @Valid @RequestBody FixedAssetRequestDto requestDTO
    ) {
        try {
            FixedAssetResponseDto response = fixedAssetService.updateFixedAsset(faPoid, requestDTO);
            return success("Fixed Asset updated successfully", response);

        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to update Fixed Asset " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get Fixed Asset by faPoid",
            description = "Retrieves Fixed Asset details based on the provided Fixed Asset faPoid",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the Fixed Asset details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FixedAssetResponseDto.class)
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
                            description = "Fixed Asset not found",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{faPoid}")
    public ResponseEntity<?> getFixedAssetById(
            @Parameter(description = "refTypePoid reference identifier", required = true)
            @PathVariable Long faPoid) {

        FixedAssetResponseDto responseDto = fixedAssetService.getFixedAssetById(faPoid);
        return success("Fixed Asset fetched successfully", responseDto);
    }

    @Operation(
            summary = "Soft delete a Fixed Asset",
            description = "Marks a Fixed Asset as deleted without permanently removing its data",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully soft deleted the Fixed Asset",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FixedAssetRequestDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Fixed Asset not found",
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
    @DeleteMapping("/{faPoid}")
    public ResponseEntity<?> softDeleteFixedAsset(
            @Parameter(description = "faPoid reference identifier", required = true)
            @PathVariable Long faPoid,
            @Valid @RequestBody(required = false) com.asg.common.lib.dto.DeleteReasonDto deleteReasonDto) {

        fixedAssetService.softDeleteFixedAsset(faPoid, deleteReasonDto);
        return success("Fixed Asset has been soft deleted successfully");
    }

    @Operation(
            summary = "Create multiple copies of an existing Fixed Asset",
            description = "Generates the specified number of copies for a given Fixed Asset. Each copy will have a new unique FA Code generated by the database.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created asset copies",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FixedAssetResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid FA POID or number of copies",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal Server Error while creating asset copies",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/{faPoid}/{noOfCopies}")
    public ResponseEntity<?> createAssetCopies(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated Fixed Asset details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FixedAssetRequestDto.class)
                    )
            )
            @Parameter(description = "Fixed Asset POID for which copies are to be created", required = true, example = "101")
            @PathVariable Long faPoid,

            @Parameter(description = "Number of copies to create", required = true, example = "3")
            @PathVariable int noOfCopies
    ) {
        try {
            List<Long> newPoidList = fixedAssetService.createMultipleCopies(faPoid, noOfCopies);
            return success("Fixed Asset Copies created successfully", newPoidList);
        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to create multiple Fixed Asset copies" + ex.getMessage());
        }
    }


    @Operation(
            summary = "List Fixed Assets with Search and Sort (DocId: 600-002)",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (FA_CODE, FA_DESCRIPTION, ASSET_TYPE, COMPANY_NAME, LOCATION_NAME, ACTIVE, CREATED_BY). " +
                    "Sorting defaults to faPoid descending. " +
                    "The search will be performed across all fields listed in `list_of_records_sql` or the main table field in `GLOBAL_DOC_MASTER` for DocId 600-002. " +
                    "Sorting behavior and displayed columns can be configured through `list_of_display_columns_and_types` in the document master configuration."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                - ### Filters:
                  Use either:
                  1. A single `GLOBALSEARCH` filter, OR
                  2. Any combination of specific fields (FA_CODE, FA_DESCRIPTION, ASSET_TYPE, COMPANY_NAME, LOCATION_NAME, ACTIVE, CREATED_BY).
                  3. `operator` field will either have "AND" or "OR". If not provided, it will default to "OR".
                     Not required for GLOBALSEARCH. For non-global searches, provide it only once.
                  4. `isDeleted` when 'N' or null will search and return non-deleted records; 'Y' will include deleted ones.
                  5. `sort` will default to the primary key ascending if specified in DB, otherwise, you can override it by providing field name and direction.
                
                - #### Global Search:
                  Apply one search term across multiple fields.
                  No operator needs to be sent in this case.
                  • { "searchField": "GLOBALSEARCH", "searchValue": "Laptop" }
                
                - #### Single Field, Single Value:
                  Search one field with one value.
                  • { "searchField": "FA_CODE", "searchValue": "FA001" },
                
                - #### Single Field, Multiple Values:
                  Provide multiple values for the same field, separated by `|`.
                  • { "searchField": "FA_CODE", "searchValue": "FA001|FA002" }
                
                - #### Multiple Different Fields, Single or Multiple Value:
                  Provide one or multiple search values across multiple fields.
                  • { "searchField": "FA_CODE", "searchValue": "FA001" },
                  • { "searchField": "FA_DESCRIPTION", "searchValue": "Computer|Printer" },
                  • "operator": "AND"/"OR"
                
                - ### Sorting:
                  Defaults to whatever specified in list_of_records_sql field (mostly primary key ascending).
                  To override, pass `sort=<field>,ASC|DESC` in query params.
                  Examples:
                  • sort=FA_DESCRIPTION,ASC
                  • sort=FA_CODE,DESC
                
                - ### Authorization Parameters (handled by interceptor)
                    - **documentId:** Unique identifier for the document (`600-002`)
                    - **actionRequested:** Action being performed (`VIEW`)
                """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Fixed Asset Filters",
                                    value = """
                                        {
                                          "operator": "AND",
                                          "isDeleted": "N",
                                          "filters": [
                                             { "searchField": "GLOBALSEARCH", "searchValue": "Computer" },
                                             { "searchField": "FA_CODE", "searchValue": "FA001|FA002" },
                                             { "searchField": "FA_DESCRIPTION", "searchValue": "Office Laptop" },
                                             { "searchField": "ASSET_TYPE", "searchValue": "IT Equipment" },
                                             { "searchField": "COMPANY_NAME", "searchValue": "TechCorp" },
                                             { "searchField": "LOCATION_NAME", "searchValue": "Head Office" },
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
    public ResponseEntity<?> getFixedAssetCategories(@ParameterObject Pageable pageable,
                                                     @RequestBody(required = false) FilterRequestDto filters) {
        try {
            Map<String, Object> data = fixedAssetService.listFixedAssetCategories(UserContext.getDocumentId(), filters, pageable);
            return success("Fixed Asset Categories fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch Fixed Asset Category list: " + ex.getMessage());
        }
    }
}
