package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;

import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.BankPayeeRequest;
import com.asg.finance.dto.BankPayeeResponse;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.service.IBankPayeeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/bank-payees")
@lombok.RequiredArgsConstructor
public class BankPayeeController {

    private final IBankPayeeService bankPayeeService;
    private final LoggingService loggingService;

    @Operation(
            summary = "Create Bank Payee",
            description = "Creates a new payee entry in the Bank Payee Master so it becomes available for cheque printing.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Payee created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BankPayeeResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input or missing headers",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized – JWT token missing or invalid",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Forbidden – Access denied by RBAC",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Conflict – Payee name already exists",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createPayee(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payee object that needs to be created",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BankPayeeRequest.class)
                    )
            )
            @Parameter(description = "Payee details to be created", required = true)
            @Valid @RequestBody BankPayeeRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return handleValidationErrors(bindingResult);
        }

        BankPayeeResponse response = bankPayeeService.createPayee(request);
        return success("Bank Payee created successfully", response);
    }


    @Operation(
            summary = "Get Bank Payee by ID",
            description = "Returns a Bank Payee by its POID, including audit metadata"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payee found"),
            @ApiResponse(responseCode = "404", description = "Payee not found")
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{payeePoid}")
    public ResponseEntity<?> getPayee(@PathVariable Long payeePoid) {

        BankPayeeResponse response = bankPayeeService.getPayeeById(payeePoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), payeePoid.toString());
        return success("Bank Payee found", response);
    }

    @Operation(
            summary = "Soft delete payee by ID",
            description = "Marks a bank payee as inactive instead of removing the record from the database."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payee successfully soft deleted"),
            @ApiResponse(responseCode = "404", description = "Payee not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized – JWT token missing or invalid"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{payingPoid}")
    public ResponseEntity<?> softDeleteById(
            @Parameter(description = "Bank Payee POID", required = true)
            @PathVariable Long payingPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {

        bankPayeeService.softDeleteBypPayingPoid(payingPoid, deleteReasonDto);
        return success("Bank Payee has been soft deleted successfully", null);
    }

    @Operation(
            summary = "Update Bank Payee (DocId: 800-320)",
            description = """
                    Updates an existing bank payee record in the master table.
                    
                    ### Authorization Parameters
                    - **documentId:** Unique identifier for the document (`800-320`)
                    - **actionRequested:** Action being performed (`EDIT`)
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated the payee",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BankPayeeResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad Request – Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized – JWT token missing or invalid"),
                    @ApiResponse(responseCode = "404", description = "Payee not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict – Payee name already exists"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Provide the updated bank payee details.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BankPayeeRequest.class),
                    examples = @ExampleObject(
                            name = "Bank Payee Update Example",
                            value = """
                                    {
                                      "payingName": "Johnathan Doe",
                                      "payingName2": "J. Doe",
                                      "remarks": "Updated cheque name",
                                       "active": "Y"
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{payingPoid}")
    public ResponseEntity<?> updatePayee(
            @PathVariable
            @Parameter(description = "Bank Payee POID", required = true)
            Long payingPoid,
            @Valid @RequestBody BankPayeeRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return handleValidationErrors(bindingResult);
        }

        BankPayeeResponse response = bankPayeeService.updatePayee(payingPoid, request);
        return success("Updated bank payee details", response);
    }

    @Operation(
            summary = "List Bank Payees with Search and Sort (DocId: 800-320)",
            description = """
                        Provide search filters. Valid `searchField` values: GLOBALSEARCH or (PAYING_NAME, PAYING_NAME2, ACTIVE, CREATED_BY).
                        Sorting default on payingPoid descending unless overridden using `sort` query param.
                    
                        - ### Filters:
                          Use either:
                          1. A single `GLOBALSEARCH` filter, OR
                          2. Any combination of specific fields (PAYING_NAME, PAYING_NAME2, ACTIVE, CREATED_BY).
                          3. `operator` field supports "AND" or "OR", default is "OR" (optional for GLOBALSEARCH).
                          4. `isDeleted` filter with 'N' or null to get active records; 'Y' to get deleted ones.
                          5. Use `sort` param to override sorting. Default is primary key descending.
                    
                        - ### Examples:
                          • { "searchField": "GLOBALSEARCH", "searchValue": "John" }
                          • { "searchField": "PAYING_NAME", "searchValue": "Doe|Smith" }
                          • sort=PAYING_NAME,ASC
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Search filter configuration for listing Bank Payees",
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Bank Payee Filter Example",
                                    value = """
                                            {
                                              "operator": "AND",
                                              "isDeleted": "N",
                                              "filters": [
                                                { "searchField": "GLOBALSEARCH", "searchValue": "John" },
                                                { "searchField": "PAYING_NAME", "searchValue": "Doe|Smith" },
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
    public ResponseEntity<?> getBankPayees(@ParameterObject Pageable pageable,
                                           @RequestBody(required = false) FilterRequestDto filters) {
        try {
            Map<String, Object> data = bankPayeeService.listPayees(UserContext.getDocumentId(), filters, pageable);

            return success("Bank payees fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch bank payee list: " + ex.getMessage());
        }
    }

    private ResponseEntity<?> handleValidationErrors(BindingResult bindingResult) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error("Validation error in request payload", HttpStatus.BAD_REQUEST.value(), errors);
    }
}

