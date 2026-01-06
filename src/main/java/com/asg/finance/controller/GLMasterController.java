package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.request.DocReleaseLockRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.GLMasterRequestDto;
import com.asg.finance.dto.GLMasterResponseDto;
import com.asg.finance.dto.GlMasterTreeRequest;
import com.asg.finance.service.GLMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;


@RestController
@RequestMapping("/v1/gl-master")
public class GLMasterController {

    @Autowired
    private GLMasterService glMasterService;

    @Operation(
            summary = "Create GL Master",
            description = "Creates a new GL Master entry along with Company and Payment details.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "GL Master created successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GLMasterResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                    @ApiResponse(responseCode = "409", description = "Conflict – GL Code already exists", content = @Content)
            }
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "GL Master object with company and payment details to create",
                    required = true,
                    content = @Content(schema = @Schema(implementation = GLMasterRequestDto.class))
            )
            @Valid @RequestBody GLMasterRequestDto req) {

        GLMasterResponseDto resp = glMasterService.createGLMaster(req);
        return success("GL Master created successfully", resp);
    }

    @Operation(
            summary = "Get GL Master by ID",
            description = "Fetches GL Master details by POID including company details if available.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "GL Master found",
                            content = @Content(schema = @Schema(implementation = GLMasterResponseDto.class))),
                    @ApiResponse(responseCode = "404", description = "GL Master not found", content = @Content)
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{glPoid}")
    public ResponseEntity<?> get(
            @Parameter(description = "GL Master POID", required = true) @PathVariable Long glPoid) {

        GLMasterResponseDto resp = glMasterService.getGLMaster(glPoid);
        return success("GL Master found", resp);

    }

    @Operation(
            summary = "Get GL Master by ID (Simple)",
            description = "Fetches GL Master details without auth parameters - for internal service calls"
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/simple/{glPoid}")
    public ResponseEntity<?> getSimple(@PathVariable Long glPoid) {
        com.asg.common.lib.dto.GLMasterDto glMasterDto = glMasterService.getGLMasterDto(glPoid);
        return success("GL Master found", glMasterDto);
    }

    @Operation(
            summary = "Get multiple GL Masters by glPoids (Batch)",
            description = "Retrieves multiple GL Master details - for internal service calls"
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/batch")
    public ResponseEntity<?> getGLMastersBatch(@RequestBody List<Long> glPoids) {
        List<com.asg.common.lib.dto.GLMasterDto> glMasters = glMasterService.getGLMasterDtos(glPoids);
        return success("GL Masters fetched successfully", glMasters);
    }

    @Operation(
            summary = "Update GL Master",
            description = "Updates an existing GL Master entry including company and payment details.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "GL Master updated successfully",
                            content = @Content(schema = @Schema(implementation = GLMasterResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
                    @ApiResponse(responseCode = "404", description = "GL Master not found", content = @Content)
            }
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{glPoid}")
    public ResponseEntity<?> update(
            @Parameter(description = "GL Master POID", required = true) @PathVariable Long glPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated GL Master object including company details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = GLMasterRequestDto.class))
            )
            @Valid @RequestBody GLMasterRequestDto req) {

        GLMasterResponseDto resp = glMasterService.updateGLMaster(glPoid, req);
        return success("GL Master found", resp);
    }

    @Operation(
            summary = "Delete GL Master",
            description = "Deletes a GL Master entry by marking it inactive (soft delete).",
            responses = {
                    @ApiResponse(responseCode = "204", description = "GL Master deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "GL Master not found", content = @Content)
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{glPoid}")
    public ResponseEntity<?> delete(
            @Parameter(description = "GL Master POID", required = true) @PathVariable Long glPoid) {

        glMasterService.deleteGLMaster(glPoid);
        return success("GL Master has been soft deleted successfully");
    }

    @Operation(
            summary = "Get GL Master Tree Structure",
            description = """
                    Retrieve the complete GL Master tree structure with hierarchical parent-child relationships.
                    
                    ### Tree Structure:
                    - **MAIN_GROUP**: Top-level account groups
                    - **SUB_GROUP**: Sub-account groups under main groups
                    - **LEDGER**: Individual GL accounts (leaf nodes)
                    
                    ### Response Format:
                    - Nested JSON structure with children[] arrays
                    - Each item contains glCode, description, type, accountType, id, level, children
                    - Statistics include total accounts, group count, item count
                    
                    ### Authorization Parameters (handled by interceptor):
                    - **documentId**: Document identifier (400-001 for GL Master)
                    - **actionRequested**: Action being performed (VIEW)
                    
                    ### Example Usage:
                    - Get all active records: `/api/v1/gl-master/tree?documentId=400-001&actionRequested=VIEW`
                    - Include deleted records: `/api/v1/gl-master/tree?documentId=400-001&actionRequested=VIEW&includeDeleted=true`
                    - Filter by GL code: `/api/v1/gl-master/tree?documentId=400-001&actionRequested=VIEW&filterField1=GL_CODE&filterValue1=1001`
                    - Filter by GL type: `/api/v1/gl-master/tree?documentId=400-001&actionRequested=VIEW&filterField1=GL_TYPE&filterValue1=LEDGER`
                    """
    )
    @ApiResponse(responseCode = "200", description = "GL Master tree structure retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid parameters")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/tree")
    public ResponseEntity<?> getGlMasterTree(
            @Parameter(description = "Include deleted records in the response", example = "false")
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @Parameter(description = "Filter value", example = "1001")
            @RequestParam(required = true) String filterValue)
    {
        try {
            // Build request object
            GlMasterTreeRequest request = GlMasterTreeRequest.builder()
                    .includeDeleted(includeDeleted)
                    .groupPoid(UserContext.getGroupPoid())
                    .companyPoid(UserContext.getCompanyPoid())
                    .userPoid(UserContext.getUserPoid())
                    .filterValue(filterValue)
                    .build();

            var treeItems = glMasterService.getGlMasterTree(UserContext.getDocumentId(), UserContext.getActionRequested(), request);

            if (treeItems.isEmpty()) {
                return success("No GL Master records found", new ArrayList<>());
            }

            // Return the tree structure directly as an array
            return success("GL Master tree structure retrieved successfully", treeItems);

        } catch (Exception e) {
            return internalServerError("An error occurred while retrieving GL Master tree: " + e.getMessage());
        }
    }

    @Operation(summary = "Release GL Master Lock")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "GL lock released successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or stored proc returned warning/error"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/release-lock")
    public ResponseEntity<?> releaseLock(
            @Parameter(description = "GL Master request payload", required = true)
            @Valid @RequestBody DocReleaseLockRequestDto request
    ) {

        String status = glMasterService.acquireLock(request);
        return success("Lock released successfully", status);
    }

    @Operation(
            summary = "Acquire document lock",
            description = "Acquires a lock on a document to prevent concurrent modifications",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lock acquired successfully"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request parameters"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Document is locked by another user"
                    )
            }
    )

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/acquire-lock")
    public ResponseEntity<?> acquireLock(
            @Parameter(description = "GL Master request payload", required = true)
            @Valid @RequestBody DocReleaseLockRequestDto request
    ) {

        String status = glMasterService.acquireLock(request);
        return success("Lock acquired successfully", status);
    }

    @Operation(
            summary = "Retrieve paginated GL Master records with filtering and sorting",
            description = "Fetches a paginated list of GL Master records with support for filtering, sorting, and pagination",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved GL Master records",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Map.class)
                            )
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listOfRecordsWithGenericSearch(
            @Parameter(
                    description = "Pagination and sorting parameters",
                    example = "{\n" +
                            "  \"page\": 0,\n" +
                            "  \"size\": 10,\n" +
                            "  \"sort\": [\"GL_DESCRIPTION,asc\"]\n" +
                            "}"
            ) @ParameterObject Pageable pageable,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Filter criteria for the search",
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "Search by GL_POID and GL_DESCRIPTION",
                                            value = "{\n" +
                                                    "  \"operator\": \"AND\",\n" +
                                                    "  \"isDeleted\": \"N\",\n" +
                                                    "  \"filters\": [\n" +
                                                    "    {\n" +
                                                    "      \"searchField\": \"GL_POID\",\n" +
                                                    "      \"searchValue\": \"19760\"\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"searchField\": \"GL_DESCRIPTION\",\n" +
                                                    "      \"searchValue\": \"Jade Union Shipping Co.Ltd\"\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"
                                    ),
                                    @ExampleObject(
                                            name = "Simple Search Example",
                                            value = "{\n" +
                                                    "  \"operator\": \"AND\",\n" +
                                                    "  \"isDeleted\": \"false\",\n" +
                                                    "  \"filters\": [\n" +
                                                    "    {\n" +
                                                    "      \"searchField\": \"GL_CODE\",\n" +
                                                    "      \"searchValue\": \"1000\"\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"
                                    )
                            }
                    )
            ) @RequestBody(required = false) FilterRequestDto filters
    ) {
        Map<String, Object> result = glMasterService.listOfRecordsAndGenericSearch(UserContext.getDocumentId(), filters, pageable);
        return success("GL Master list fetched successfully", result);
    }

    @Operation(
            summary = "Get GL Master List"
    )
    @ApiResponse(responseCode = "200", description = "GL Master list retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid parameters")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/list")
    public ResponseEntity<?> getGlMasterList(
            @Parameter(description = "Parent POID (null for main groups)", example = "1000")
            @RequestParam(required = false) Long parentPoid) {

        try {
            var listItems = glMasterService.getGlMasterList(UserContext.getDocumentId(), UserContext.getActionRequested(), parentPoid);

            if (listItems.isEmpty()) {
                return success("No GL Master records found", new ArrayList<>());
            }

            // Create response with list and count
            Map<String, Object> response = Map.of(
                "content", listItems,
                "totalElements", listItems.size()
            );
            return success("GL Master list retrieved successfully", response);

        } catch (Exception e) {
            return internalServerError("An error occurred while retrieving GL Master list: " + e.getMessage());
        }
    }

}
