package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.GlFavAcMasterRequest;
import com.asg.finance.dto.GlFavAcMasterResponse;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.service.GlFavAcMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;


/**
 * REST Controller for Key Favorite Account Master operations
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/favorite-accounts")
public class GlFavAcMasterController {

    private final GlFavAcMasterService service;

    @Operation(
            summary = "Create Key Favorite Account Master",
            description = "Create a new Favorite Account Group by entering Fav Ac Code, Description, and mapping GL Accounts for financial reporting (e.g., Cash Position Reports).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Favorite Account Group created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = GlFavAcMasterResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data or validation errors"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Key Favorite Account Master data with GL accounts and user roles",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GlFavAcMasterRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "Create Favorite Account Group",
                                    summary = "Sample request to create a new favorite account group",
                                    value = """
                                            {
                                              "favAcCode": "BANKS",
                                              "description": "Bank Accounts Group",
                                              "description2": "For treasury reporting",
                                              "seqNo": 1,
                                              "active": "Y",
                                              "groupPoid": 1,
                                              "userRolePoids": [1, 2],
                                              "glAccounts": [
                                                {
                                                  "glAccountPoId": 10010001,
                                                  "companyPoId": 1,
                                                  "viewCategoryPoid": "BANK_ACC",
                                                  "seqNo": 1,
                                                  "remarks": "Primary bank account"
                                                }
                                              ]
                                            }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createFavoriteAccount(
            @Valid @RequestBody GlFavAcMasterRequest request
    ) {
        try {
            GlFavAcMasterResponse response = service.createFavoriteAccount(request);
            Map<String, Object> data = Map.of("favAcId", response.getFavAcPoid());
            return success("Favorite Account Group created successfully", data);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to create Favorite Account Group: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Update Key Favorite Account Master",
            description = "Update an existing Favorite Account Group with new GL accounts and user role mappings.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Favorite Account Group updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = GlFavAcMasterResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data or validation errors"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Favorite Account Group not found"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{favAcPoid}")
    public ResponseEntity<?> updateFavoriteAccount(
            @Parameter(description = "Unique identifier of the favorite account group", required = true, example = "1")
            @PathVariable Long favAcPoid,
            @Valid @RequestBody GlFavAcMasterRequest request
    ) {
        try {
            GlFavAcMasterResponse result = service.updateFavoriteAccount(favAcPoid, request);
            return success("Favorite Account Group updated successfully", result);
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to update Favorite Account Group: " + ex.getMessage());
        }
    }


    @Operation(
            summary = "Get Favorite Account by ID",
            description = "Retrieves a specific Favorite Account Group by its unique identifier along with its associated GL accounts and user roles.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved Favorite Account Group",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = GlFavAcMasterResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request parameters",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "ErrorResponse",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "message": "Invalid request parameters",
                                                      "statusCode": "400"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Favorite Account not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "NotFoundResponse",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "message": "Favorite Account not found with id: 999",
                                                      "statusCode": "404"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Invalid or missing authentication",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content
                    )
            }
    )
    @Parameter(
            name = "favAcPoid",
            description = "Unique identifier of the Favorite Account Group",
            required = true,
            example = "1"
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{favAcPoid}")
    public ResponseEntity<?> getGlFavAc(@PathVariable Long favAcPoid) {
        GlFavAcMasterResponse result = service.getFavoriteAccountById(favAcPoid);
        return success("Favorite Account Records fetched successfully", result);
    }

    @Operation(
            summary = "Delete a Favorite Account by ID",
            description = "Soft deletes a favorite account by marking it as inactive and deleted.\n\n" +
                    "**Required Permissions:**\n" +
                    "- GL_FAV_AC_DELETE",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Favorite Account successfully deleted",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(
                                            name = "successResponse",
                                            value = "{\"status\": 200, \"message\": \"Favorite Account Record deleted successfully\"}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Favorite Account not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(
                                            name = "notFoundResponse",
                                            value = "{\"status\": 404, \"message\": \"Favorite Account not found with favAcPoid: 999\"}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(
                                            name = "accessDeniedResponse",
                                            value = "{\"status\": 403, \"message\": \"Access Denied\"}"
                                    )
                            )
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{favAcPoid}")
    public ResponseEntity<?> deleteFavoriteAccount(
            @Parameter(
                    description = "ID of the favorite account to be deleted",
                    required = true,
                    example = "116"
            )
            @PathVariable Long favAcPoid) {

        service.softDeleteFavoriteAccount(favAcPoid);
        return success("Favorite Account Record deleted successfully");
    }


    @Operation(
            summary = "List Favorite Accounts with Search and Sort",
            description = "Search and sort favorite accounts with flexible filtering. " +
                    "Valid search fields: GLOBALSEARCH or (FAC_AC_POID, DESCRIPTION). " +
                    "Sorting defaults to FAC_AC_POID in ascending order."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields (FAC_AC_POID, DESCRIPTION).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR".
                      4. isDeleted when 'N' or null, will search and return non-deleted records, 'Y' will check and return deleted records.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      - { "searchField": "GLOBALSEARCH", "searchValue": "bank" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      - { "searchField": "DESCRIPTION", "searchValue": "Bank Accounts" }
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|` .
                      - { "searchField": "DESCRIPTION", "searchValue": "Bank|Cash" }
                    
                    - #### Multiple Different Fields:
                      Combine multiple search conditions.
                      - { "searchField": "DESCRIPTION", "searchValue": "Bank" }
                      - "operator" : "AND"
                    
                    - ### Sorting:
                      Defaults to FAC_AC_POID ascending.
                      To override, use `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      - sort=DESCRIPTION,ASC
                      - sort=FAC_AC_POID,DESC
                    """,
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "Favorite Account Filters",
                                    value = """
                                            {
                                                "operator": "AND",
                                                "isDeleted": "N",
                                                "filters": [
                                                    {
                                                        "searchField": "FAC_AC_POID",
                                                        "searchValue": "64"
                                                    },
                                                    {
                                                        "searchField": "DESCRIPTION",
                                                        "searchValue": "CUSTOMER PERFORMANCE"
                                                    }
                                                ]
                                            }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listOfRecordsWithGenericSearch(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters
    ) {

        Map<String, Object> favAcMaster = service.listOfRecordsAndGenericSearch(UserContext.getDocumentId(), filters, pageable);

        return success("Favorite Account list fetched successfully", favAcMaster);

    }

}

