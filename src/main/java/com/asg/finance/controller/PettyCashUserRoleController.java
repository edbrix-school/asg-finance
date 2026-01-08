package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;

import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.PettyCashUserRoleRequestDto;
import com.asg.finance.dto.PettyCashUserroleResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.service.PettyCashUserRoleService;
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

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.internalServerError;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/v1/petty-cash-user-role")
public class PettyCashUserRoleController {
    private final PettyCashUserRoleService pettyCashUserRoleService;

    public PettyCashUserRoleController(PettyCashUserRoleService pettyCashUserRoleService) {
        this.pettyCashUserRoleService = pettyCashUserRoleService;
    }

    @Operation(
            summary = "Create a new Petty Cash User Role",
            description = "Creates a new Petty Cash User Role with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created the Petty Cash User Role",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PettyCashUserroleResponseDto.class)
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
    public ResponseEntity<?> createPettyCashUserRole(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Petty Cash User Role object that needs to be created",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PettyCashUserRoleRequestDto.class)
                    )
            )
            @Parameter(description = "Petty Cash User Role details to be created", required = true)
            @Valid @RequestBody PettyCashUserRoleRequestDto request,
            @Parameter(description = "User Poid", required = false, example = "101")
            @RequestParam(required = false, defaultValue = "0") Long userPoid
    ) {
        try {
            PettyCashUserroleResponseDto response = pettyCashUserRoleService.createPettyCashUserRole(request);
            return success("Petty Cash User Role created successfully", response);
        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to create Petty Cash User Role: " + ex.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{refTypePoid}")
    public ResponseEntity<?> updatePettyCashUserRole(
            @Parameter(description = "Petty Cash User Role refTypePoid to be updated", required = true)
            @PathVariable Long refTypePoid,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated Petty Cash User Role details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PettyCashUserRoleRequestDto.class)
                    )
            )
            @Valid @RequestBody PettyCashUserRoleRequestDto requestDTO,
            @Parameter(description = "User Poid", required = false, example = "101")
            @RequestParam(required = false, defaultValue = "0") Long userPoid
    ) {
        try {
            PettyCashUserroleResponseDto response = pettyCashUserRoleService.updatePettyCashUserRole(refTypePoid, requestDTO);
            return success("Petty Cash User Role updated successfully", response);

        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to update Petty Cash User Role: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get Petty Cash User Role by refTypePoid",
            description = "Retrieves Petty Cash User Role details based on the provided Petty Cash User Role refTypePoid",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the Petty Cash User Role details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PettyCashUserroleResponseDto.class)
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
                            description = "Petty Cash User Role not found",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{refTypePoid}")
    public ResponseEntity<?> getPettyCashUserRole(
            @Parameter(description = "refTypePoid reference identifier", required = true)
            @PathVariable Long refTypePoid) {

        PettyCashUserroleResponseDto responseDto = pettyCashUserRoleService.getPettyCashUserRole(refTypePoid);
        return success("Petty Cash User Role fetched successfully", responseDto);
    }

    @Operation(
            summary = "Soft delete a Petty Cash User Role",
            description = "Marks a Petty Cash User Role as deleted without permanently removing its data",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully soft deleted the Petty Cash User Role",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PettyCashUserRoleRequestDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Petty Cash User Role not found",
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
    @DeleteMapping("/{refTypePoid}")
    public ResponseEntity<?> softDeletePettyCashUserRole(
            @Parameter(description = "refTypePoid reference identifier", required = true)
            @PathVariable Long refTypePoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {

        pettyCashUserRoleService.softDeletePettyCashUserRole(refTypePoid, deleteReasonDto);
        return success("Petty Cash User Role has been soft deleted successfully");
    }

    @Operation(
            summary = "List Tax Master with Search and Sort (DocId: 400-010)",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (REF_TYPE, DESCRIPTION, ACTIVE, CREATED_BY). Sorting default on refTypePoid, desc." +
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
                      2. Any combination of specific fields (REF_TYPE, DESCRIPTION, ACTIVE, CREATED_BY).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                         not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                      4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                      5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      No operator need to send in this case.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "GENERAL" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      • { "searchField": "REF_TYPE", "searchValue": "GENERAL" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "REF_TYPE", "searchValue": "GENERAL|FF JOBS" }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "REF_TYPE", "searchValue": "GENERAL" },
                      • { "searchField": "DESCRIPTION", "searchValue": "GENERAL|FF JOBS" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=REF_TYPE,ASC
                      • sort=DESCRIPTION,DESC
                    
                    - ### Authorization Parameters (handled by interceptor)
                        - **documentId:** Unique identifier for the document (`400-008`)
                        - **actionRequested:** Action being performed (`VIEW`)
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "CostCenter Filters",
                                    value = """
                                            {
                                            "operator": "AND",
                                            "isDeleted": "N",
                                            "filters": [
                                               { "searchField": "GLOBALSEARCH", "searchValue": "GENERAL" },
                                               { "searchField": "REF_TYPE", "searchValue": "GENERAL" },
                                               { "searchField": "DESCRIPTION", "searchValue": "GENERAL"},
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
    public ResponseEntity<?> listPettyCashUserRole(@ParameterObject Pageable pageable,
                                                   @RequestBody(required = false) FilterRequestDto filters) {
        try {
            Map<String, Object> data = pettyCashUserRoleService.listPettyCashUserRole(UserContext.getDocumentId(), filters, pageable);
            return success("Petty Cash User Role fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch pettyCashUserRole list: " + ex.getMessage());
        }

    }


}
