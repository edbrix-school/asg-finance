package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.CostCenterListResponseDto;
import com.asg.finance.dto.CostCenterRequestDTO;
import com.asg.finance.dto.CostCenterResponseDTO;
import com.asg.finance.dto.CostCenterTreeRequest;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.service.CostCenterServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;


@RestController
@RequestMapping("/v1/cost-center")
@RequiredArgsConstructor
public class CostCenterController {
    private final CostCenterServiceImpl costCenterServiceImpl;

    @Operation(
            summary = "Create a new CostCenter",
            description = "Creates a new costCenter with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created the costCenter",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CostCenterRequestDTO.class)
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
                            responseCode = "409",
                            description = "CostCenter with the same code already exists",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createCostCenter(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "CostCenter object that needs to be created",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CostCenterRequestDTO.class)
                    )
            )
            @Parameter(description = "CostCenter details to be created", required = true)
            @Valid @RequestBody CostCenterRequestDTO costCenterDto) {
        try {
            Long costCenterPoid = costCenterServiceImpl.createCostCenter(costCenterDto);

            CostCenterResponseDTO response = new CostCenterResponseDTO("success", costCenterPoid);
            return success("Cost center created successfully", response);

        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError(ex.getMessage());
        }
    }


    @Operation(
            summary = "Soft delete a country",
            description = "Marks a country as deleted without permanently removing its data",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully soft deleted the country",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CostCenterRequestDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Country not found",
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
    @DeleteMapping("/{costCenterPoid}")
    public ResponseEntity<?> softDeleteCountry(
            @Parameter(description = "CostCenterPoid reference identifier", required = true)
            @PathVariable Long costCenterPoid) {

        costCenterServiceImpl.softDeleteCountry(costCenterPoid);
        return success("Cost Center has been soft deleted successfully");
    }

    @Operation(
            summary = "Update CostCenter details",
            description = "Updates the details of an existing CostCenter identified by its ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "CostCenter updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CostCenterRequestDTO.class),
                                    examples = @ExampleObject(
                                            value = "{\"status\": 200, \"message\": \"CostCenter updated successfully\", \"data\": {...}}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input or validation error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"status\": 400, \"message\": \"Validation error: [field] is required\"}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "CostCenter not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"status\": 404, \"message\": \"CostCenter not found with countryPoid: 123\"}"
                                    )
                            )
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCostCenter(
            @Parameter(description = "CostCenter ID (Poid) to update", required = true, example = "51001")
            @PathVariable Long id,

            @Parameter(description = "Updated CostCenter details", required = true)
            @Valid @RequestBody CostCenterRequestDTO costCenterDto) {

        try {
            Long updatedPoid = costCenterServiceImpl.updateCostCenter(id, costCenterDto);
            return success("Updated cost center successfully", new CostCenterResponseDTO("success", updatedPoid));
        } catch (ValidationException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError(ex.getMessage());
        }
    }

    @Operation(
            summary = "Get cost center by ID",
            description = "Retrieves cost center details based on the provided cost center POID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the cost center details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CostCenterRequestDTO.class)
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
                            description = "Cost Center not found",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{costCenterPoid}")
    public ResponseEntity<?> getCostCenterByCostCenterPoid(
            @Parameter(description = "costCenterPoid reference identifier", required = true)
            @PathVariable Long costCenterPoid) {
        CostCenterRequestDTO costCenterDto = costCenterServiceImpl.getCostCenterById(costCenterPoid);
        return success("Cost Center fetched successfully", costCenterDto);
    }

    @Operation(
            summary = "Get Cost Center Tree Structure",
            description = """
                    Retrieve a hierarchical tree structure of Cost Center records.
                    
                    ### Behavior:
                    - **includeDeleted = false**: Returns only active Cost Center records (default)
                    - **includeDeleted = true**: Includes soft-deleted records
                    - **filterValue**: Optional filter to search by Cost Center description or code
                    
                    ### Use Cases:
                    - **All Records**: `GET /api/v1/cost-center/tree?documentId=400-012&actionRequested=VIEW`
                        Returns: Complete tree structure with all active Cost Centers
                        
                    - **With Filter**: `GET /api/v1/cost-center/tree?documentId=400-012&actionRequested=VIEW&filterValue=Admin`
                        Returns: Tree structure filtered by Cost Centers containing "Admin"
                        
                    - **Include Deleted**: `GET /api/v1/cost-center/tree?documentId=400-012&actionRequested=VIEW&includeDeleted=true`
                        Returns: Tree structure including soft-deleted records
                    
                    ### Response Format:
                    - Hierarchical JSON structure with nested children arrays
                    - Root level contains only top-level MAIN_GROUP Cost Centers
                    - Each node includes costCenterPoid, costCenterCode, costCenterDescription, type, level
                    - Sorted by Cost Center code for consistent ordering
                    - Empty array returned if no records exist
                    
                    ### Required Parameters:
                    - **documentId**: Document identifier (400-012 for Cost Center Master)
                    - **actionRequested**: Action being performed (VIEW)
                    
                    ### Optional Parameters:
                    - **includeDeleted**: Include soft-deleted records (default: false)
                    - **groupPoid**: Group POID for filtering (default: 1)
                    - **companyPoid**: Company POID for filtering (default: 1)
                    - **userPoid**: User POID for filtering (default: 100)
                    - **filterValue**: Filter by Cost Center description or code
                    """
    )
    @ApiResponse(responseCode = "200", description = "Cost Center tree structure retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid parameters")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/tree")
    public ResponseEntity<?> getCostCenterTree(
            @Parameter(description = "Include soft-deleted records", example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean includeDeleted,
            @Parameter(description = "Group POID for filtering", example = "1")
            @RequestParam(required = false) Long groupPoid,
            @Parameter(description = "Company POID for filtering", example = "1")
            @RequestParam(required = false) Long companyPoid,
            @Parameter(description = "User POID for filtering", example = "100")
            @RequestParam(required = false) Long userPoid,
            @Parameter(description = "Filter value for searching Cost Center description or code", example = "Admin")
            @RequestParam(required = false) String filterValue) {

        try {
            // Build request object from parameters
            CostCenterTreeRequest request = CostCenterTreeRequest.builder()
                    .includeDeleted(includeDeleted)
                    .groupPoid(groupPoid)
                    .companyPoid(companyPoid)
                    .userPoid(userPoid)
                    .filterValue(filterValue)
                    .build();

            var treeNodes = costCenterServiceImpl.getCostCenterTree(UserContext.getDocumentId(), UserContext.getActionRequested(), request);

            if (treeNodes.isEmpty()) {
                return success("No Cost Center records found", new ArrayList<>());
            }

            // Return the tree structure directly as an array
            return success("Cost Center tree structure retrieved successfully", treeNodes);

        } catch (Exception e) {
            return internalServerError("An error occurred while retrieving Cost Center tree structure: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Get Cost Center List",
            description = """
                    Retrieve a flat list of Cost Center records based on parent-child relationship.
                    
                    ### Behavior:
                    - **parentPoid = null**: Returns main groups (top-level cost centers)
                    - **parentPoid = specific value**: Returns direct children of that parent (not complete hierarchy)
                    
                    ### Use Cases:
                    - **Main Groups**: `GET /api/v1/cost-center/list?documentId=400-012&actionRequested=VIEW`
                      Returns: Top-level cost center categories
                      
                    - **Sub-Groups**: `GET /api/v1/cost-center/list?documentId=400-012&actionRequested=VIEW&parentPoid=1000`
                      Returns: Direct children of cost center 1000
                      
                    - **Individual Records**: `GET /api/v1/cost-center/list?documentId=400-012&actionRequested=VIEW&parentPoid=10100`
                      Returns: Direct children of cost center 10100
                    
                    ### Response Format:
                    - Flat array of Cost Center items with parent-child relationship info
                    - Each item includes costCenterPoid, parentCostCenterPoid, costCenterCode, description, type, level
                    - Sorted by sequence number (seqNo) first, then by cost center code for consistent ordering
                    - Only active (non-deleted) records are returned
                    
                    ### Required Parameters:
                    - **documentId**: Document identifier (400-012 for Cost Center Master)
                    - **actionRequested**: Action being performed (VIEW)
                    
                    ### Optional Parameters:
                    - **parentPoid**: Parent POID (null for main groups, specific value for children)
                    """
    )
    @ApiResponse(responseCode = "200", description = "Cost Center list retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid parameters")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/list")
    public ResponseEntity<?> getCostCenterList(
            @Parameter(description = "Parent POID (null for main groups)", example = "1000")
            @RequestParam(required = false) Long parentPoid) {

        try {
            List<CostCenterListResponseDto> listItems = costCenterServiceImpl.getCostCenterList(UserContext.getDocumentId(), UserContext.getActionRequested(), parentPoid);

            if (listItems.isEmpty()) {
                return success("No Cost Center records found", new ArrayList<>());
            }

            // Create response with list and count
            Map<String, Object> response = Map.of(
                "content", listItems,
                "totalElements", listItems.size()
            );
            return success("Cost Center list retrieved successfully", response);

        } catch (Exception e) {
            return internalServerError("An error occurred while retrieving Cost Center list: " + e.getMessage());
        }
    }
}

