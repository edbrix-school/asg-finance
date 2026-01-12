package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;

import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.PropertyCostCenterRequest;
import com.asg.finance.dto.PropertyCostCenterResponse;
import com.asg.finance.dto.PropertyCostCenterTreeRequest;
import com.asg.finance.service.IPropertyCostCenterService;
import com.asg.common.lib.dto.DeleteReasonDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/property-cost-centers")
public class PropertyCostCenterController {

    private final IPropertyCostCenterService propertyCostCenterService;
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyCostCenterController.class);

    @Autowired
    public PropertyCostCenterController(IPropertyCostCenterService propertyCostCenterService) {
        this.propertyCostCenterService = propertyCostCenterService;
    }

    // ------------------- CREATE -------------------
    @Operation(summary = "Create Property Cost Center", description = "Adds a new property cost center record")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Property Cost Center created successfully",
                    content = @Content(schema = @Schema(implementation = PropertyCostCenterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "Conflict – Cost Center already exists")
    })
    @AllowedAction(UserRolesRightsEnum.CREATE)

    @PostMapping
    public ResponseEntity<?> createPropertyCostCenter(
            @Valid @RequestBody PropertyCostCenterRequest request) {

        try {
            PropertyCostCenterResponse response = propertyCostCenterService.createPropertyCostCenter(request);
            return success("Property Cost Center created successfully", response);
        } catch (DataIntegrityViolationException ex) {
            return handleConstraintViolation(ex);
        }
    }

    // ------------------- READ BY ID -------------------
    @Operation(summary = "Get Property Cost Center by ID", description = "Returns a Property Cost Center by its POID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Property Cost Center found"),
            @ApiResponse(responseCode = "404", description = "Property Cost Center not found")
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)

    @GetMapping("/{costCenterPoid}")
    public ResponseEntity<?> getPropertyCostCenter(
            @PathVariable Long costCenterPoid) {

        PropertyCostCenterResponse response = propertyCostCenterService.getPropertyCostCenterById(costCenterPoid);
        return success("Property Cost Center found", response);
    }

    // ------------------- UPDATE -------------------
    @Operation(summary = "Update Property Cost Center", description = "Updates an existing property cost center")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Property Cost Center updated successfully",
                    content = @Content(schema = @Schema(implementation = PropertyCostCenterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Property Cost Center not found"),
            @ApiResponse(responseCode = "409", description = "Conflict – Cost Center already exists")
    })
    @AllowedAction(UserRolesRightsEnum.EDIT)

    @PutMapping("/{costCenterPoid}")
    public ResponseEntity<?> updatePropertyCostCenter(
            @PathVariable Long costCenterPoid,
            @Valid @RequestBody PropertyCostCenterRequest request) {

        try {
            PropertyCostCenterResponse response = propertyCostCenterService.updatePropertyCostCenter(costCenterPoid, request);
            return success("Property Cost Center updated successfully", response);
        } catch (DataIntegrityViolationException ex) {
            return handleConstraintViolation(ex);
        }
    }

    // ------------------- SOFT DELETE -------------------
    @Operation(summary = "Soft delete Property Cost Center", description = "Marks a property cost center as inactive instead of deleting")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Property Cost Center soft deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Property Cost Center not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @AllowedAction(UserRolesRightsEnum.DELETE)

    @DeleteMapping("/{costCenterPoid}")
    public ResponseEntity<?> softDeletePropertyCostCenter(
            @PathVariable Long costCenterPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {

        propertyCostCenterService.softDeleteByPoid(costCenterPoid, deleteReasonDto);
        return success("Property Cost Center with ID " + costCenterPoid + " has been soft deleted successfully", null);
    }

    // ------------------- TREE VIEW -------------------
    @Operation(
            summary = "Get Property Cost Center Tree View (DocId: 400-013)",
            description = """
                    Returns hierarchical tree structure of Property Cost Centers using stored procedure.
                    
                    ### Features:
                    - Hierarchical parent-child relationships
                    - Filtered by Property Cost Center name or code
                    - Nested children arrays for tree display
                    - Excludes soft-deleted records by default
                    
                    ### Behavior:
                    - **filterValue = null**: Returns complete tree structure
                    - **filterValue = specific text**: Returns filtered tree matching name or code
                    
                    ### Use Cases:
                    - **All Records**: `GET /api/v1/property-cost-centers/tree?documentId=400-013&actionRequested=VIEW`
                        Returns: Complete tree structure with all active Property Cost Centers
                        
                    - **With Filter**: `GET /api/v1/property-cost-centers/tree?documentId=400-013&actionRequested=VIEW&filterValue=Office`
                        Returns: Tree structure filtered by Property Cost Centers containing "Office"
                    
                    ### Response Format:
                    - Hierarchical JSON structure with nested children arrays
                    - Root level contains only top-level MAIN_GROUP Property Cost Centers
                    - Each node includes: poid, description, parentPoid, level, itemType, propertyType
                    - Sorted by propertyType: MAIN_GROUP → SUB_GROUP → CHILD
                    
                    ### Required Parameters:
                    - **documentId**: Document identifier (400-013 for Property Cost Center Master)
                    - **actionRequested**: Action being performed (VIEW)
                    - **filterValue**: Text to search in Property Cost Center name or code
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved tree structure",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request parameters",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)

    @GetMapping("/tree")
    public ResponseEntity<?> getPropertyCostCenterTree(
            @Parameter(description = "Filter value to search by name or code", example = "Office")
            @RequestParam(required = false) String filterValue,

            @Parameter(description = "Include deleted records", example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean includeDeleted) {

        try {
            PropertyCostCenterTreeRequest request = PropertyCostCenterTreeRequest.builder()
                    .filterValue(filterValue)
                    .includeDeleted(includeDeleted)
                    .groupPoid(UserContext.getGroupPoid())
                    .companyPoid(UserContext.getCompanyPoid())
                    .userPoid(UserContext.getUserPoid())
                    .build();

            var treeNodes = propertyCostCenterService.getPropertyCostCenterTree(UserContext.getDocumentId(), UserContext.getActionRequested(), request);

            if (treeNodes.isEmpty()) {
                return success("No Property Cost Center records found", new java.util.ArrayList<>());
            }

            return success("Property Cost Center tree retrieved successfully", treeNodes);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("An error occurred while retrieving Property Cost Center tree: " + e.getMessage());
        }
    }

    // ------------------- LIST VIEW -------------------
    @Operation(
            summary = "Get Property Cost Center List View (DocId: 400-013)",
            description = """
                    Returns flat list of Property Cost Centers with parent-child relationship information.
                    
                    ### Features:
                    - Flat array structure (not hierarchical)
                    - Direct database access (no stored procedures)
                    - Parent-child relationship via parentPropertyPoid field
                    - Level indicator (0 for main groups, 1 for direct children)
                    - Excludes soft-deleted records
                    
                    ### Behavior:
                    - **parentPoid = null**: Returns main groups (top-level property cost centers)
                    - **parentPoid = specific value**: Returns direct children of that parent (not complete hierarchy)
                    
                    ### Use Cases:
                    - **Main Groups**: `GET /api/v1/property-cost-centers/list?documentId=400-013&actionRequested=VIEW`
                      Returns: Top-level property cost center categories
                      
                    - **Sub-Groups**: `GET /api/v1/property-cost-centers/list?documentId=400-013&actionRequested=VIEW&parentPoid=1000`
                      Returns: Direct children of property cost center 1000
                      
                    - **Individual Records**: `GET /api/v1/property-cost-centers/list?documentId=400-013&actionRequested=VIEW&parentPoid=10100`
                      Returns: Direct children of property cost center 10100
                    
                    ### Response Format:
                    - Flat array of Property Cost Center items with parent-child relationship info
                    - Each item includes propertyCostCenterPoid, parentPropertyPoid, propertyCostCenterCode, propertyCostCenterName, propertyType, level
                    - Sorted by seqNo (if available) then propertyCostCenterCode
                    - No nested children arrays (use tree endpoint for hierarchical structure)
                    
                    ### Required Parameters:
                    - **documentId**: Document identifier (400-013 for Property Cost Center Master)
                    - **actionRequested**: Action being performed (VIEW)
                    
                    ### Optional Parameters:
                    - **parentPoid**: Parent Property Cost Center POID (null for main groups)
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved list",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request parameters",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)

    @GetMapping("/list")
    public ResponseEntity<?> getPropertyCostCenterList(
            @Parameter(description = "Parent Property Cost Center POID (null for main groups)", example = "1000")
            @RequestParam(required = false) Long parentPoid) {

        try {
            var listItems = propertyCostCenterService.getPropertyCostCenterList(UserContext.getDocumentId(), UserContext.getActionRequested(), parentPoid);

            if (listItems.isEmpty()) {
                return success("No Property Cost Center records found", new java.util.ArrayList<>());
            }

            // Create response with list and count
            Map<String, Object> response = Map.of(
                "content", listItems,
                "totalElements", listItems.size()
            );
            return success("Property Cost Center list retrieved successfully", response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("An error occurred while retrieving Property Cost Center list: " + e.getMessage());
        }
    }

    private ResponseEntity<?> handleConstraintViolation(DataIntegrityViolationException ex) {
        LOGGER.error("Foreign key constraint violated for Property Cost Center", ex);

        String friendlyMessage = extractForeignKeyViolationMessage(ex);
        if (friendlyMessage != null) {
            return badRequest(friendlyMessage);
        }

        return badRequest("Database constraint violated. Please verify referenced entities.");
    }

    private String extractForeignKeyViolationMessage(DataIntegrityViolationException ex) {
        String message = Optional.ofNullable(ex.getMostSpecificCause())
                .map(Throwable::getMessage)
                .orElse(ex.getMessage());

        if (message == null) {
            return null;
        }

        String normalized = message.toUpperCase();

        if (normalized.contains("PROPERTY_COST_CENTER_MASTER_FK2")) {
                return "Referenced Company (companyPoid) does not exist. Please provide a valid companyPoid.";
        }
        if (normalized.contains("PROPERTY_COST_CENTER_MASTER_FK")) {
            return "Referenced GL Cost Center (costCenterPoid) does not exist. Please provide a valid costCenterPoid.";
        }

        if (normalized.contains("ORA-02291")) {
            return "Referenced parent record is missing. Please verify the related POID values.";
        }

        if (normalized.contains("ORA-02292")) {
            return "Operation prevented by existing child records. Remove dependent records first.";
        }

        return null;
    }

}
