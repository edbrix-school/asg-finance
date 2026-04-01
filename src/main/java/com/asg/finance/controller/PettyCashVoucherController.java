package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.finance.dto.*;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.service.PettyCashVoucherService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/petty-cash-voucher")
@Slf4j
public class PettyCashVoucherController {

    private final PettyCashVoucherService pettyCashVoucherService;
    private final LoggingService loggingService;

    public PettyCashVoucherController(PettyCashVoucherService pettyCashVoucherService, LoggingService loggingService) {
        this.pettyCashVoucherService = pettyCashVoucherService;
        this.loggingService = loggingService;
    }

    @Operation(
            summary = "Create a new Petty Cash Voucher",
            description = "Creates a new petty cash voucher entry including header, GL details, charges, and items",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created the Petty Cash Voucher",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PettyCashVoucherResponseDto.class)
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
    public ResponseEntity<?> createPettyCashVoucher(

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Petty Cash Voucher object that needs to be created",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PettyCashCreateRequestDto.class)
                    )
            )
            @Parameter(description = "Petty Cash Voucher details", required = true)
            @Valid @RequestBody PettyCashCreateRequestDto request
    ) {
        try {
            PettyCashResponseDto response = pettyCashVoucherService.createPettyCash(request, UserContext.getDocumentId());
            
            String key = response.getTransactionPoid().toString();

            return success("Petty cash voucher created successfully", response);
        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception e) {
            log.error("Error creating petty cash voucher: {}", e.getMessage(), e);
            return internalServerError("Failed to create petty cash voucher: " + e.getMessage());
        }
    }


    @Operation(
            summary = "Update an existing Petty Cash record",
            description = "Partially or fully updates an existing Petty Cash transaction, including its header and child details (payments, charges, items).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated the Petty Cash record",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PettyCashResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input or validation error",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Petty Cash record not found",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error during update",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updatePettyCash(
            @Parameter(
                    description = "Petty Cash Transaction Poid to update", required = true)
            @PathVariable Long transactionPoid,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated Petty Cash details (can include header and/or child details)",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PettyCashUpdateRequestDto.class)
                    )
            )
            @Valid @RequestBody PettyCashUpdateRequestDto requestDto
    ) {
        try {
            PettyCashResponseDto response = pettyCashVoucherService.updatePettyCash(
                    transactionPoid,
                    requestDto,
                    UserContext.getDocumentId()
            );
            
            String key = transactionPoid.toString();

            return success("Petty Cash record updated successfully", response);

        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());

        } catch (Exception ex) {
            log.error("Error updating petty cash voucher ID {}: {}", transactionPoid, ex.getMessage(), ex);
            return internalServerError("Failed to update petty cash: " + ex.getMessage());
        }
    }


    @Operation(
            summary = "Load Petty Cash Details",
            description = "Fetch petty cash details based on the reference type (FF, FDA, PO) and reference POID. "
                    + "This endpoint dynamically executes the corresponding stored procedure to retrieve the data.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully loaded petty cash details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PettyCashResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input parameters or reference type",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error while fetching petty cash details",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getPettyCashDetailsById(
            @Parameter(description = "Transaction Poid reference identifier", required = true)
            @PathVariable Long transactionPoid

    ) {
        PettyCashResponseDto response = pettyCashVoucherService.findById(transactionPoid, UserContext.getDocumentId());
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        return success("Petty cash details fetched successfully", response);


    }
    @Operation(
            summary = "Soft delete a TaxMaster",
            description = "Marks a TaxMaster as deleted without permanently removing its data",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully soft deleted the TaxMaster",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxMasterRequestDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "TaxMaster not found",
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
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> softDeleteTaxMaster(
            @Parameter(description = "Transaction Poid reference identifier", required = true)
            @PathVariable Long transactionPoid,
            @Parameter(description = "Reference type for petty cash creation. Allowed values: FF, FDA, PO", required = false, example = "FF")
            @RequestParam String refType,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {

        pettyCashVoucherService.deletePettyCashVoucher(transactionPoid, UserContext.getDocumentId(), refType, deleteReasonDto);
        return success("Petty Cash Voucher Payment has been soft deleted successfully");
    }

    @Operation(
            summary = "List Petty Cash Payment Voucher with Search and Sort (DocId: 400-101)",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (TRANSACTION_POID, REF_TYPE, CREATED_BY). Sorting default on transactionPoid, desc." +
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
                      2. Any combination of specific fields (TRANSACTION_POID, REF_TYPE, CREATED_BY).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                         not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                      4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                      5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      No operator need to send in this case.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "16" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      • { "searchField": "TRANSACTION_POID", "searchValue": "16" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "REF_TYPE", "searchValue": "GENERAL|FF" }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "TRANSACTION_POID", "searchValue": "16" },
                      • { "searchField": "REF_TYPE", "searchValue": "GENERAL|FF" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=TRANSACTION_POID,ASC
                      • sort=REF_TYPE,DESC
                    
                    - ### Authorization Parameters (handled by interceptor)
                        - **documentId:** Unique identifier for the document (`400-010`)
                        - **actionRequested:** Action being performed (`VIEW`)
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "TaxMaster Filters",
                                    value = """
                                            {
                                            "operator": "AND",
                                            "isDeleted": "N",
                                            "filters": [
                                               { "searchField": "GLOBALSEARCH", "searchValue": "16" },
                                               { "searchField": "TRANSACTION_POID", "searchValue": "TX16" },
                                               { "searchField": "REF_TYPE", "searchValue": "INPUT_VAT"},
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
    public ResponseEntity<?> listTaxMaster(@ParameterObject Pageable pageable,
                                           @RequestBody(required = false) FilterRequestDto filters,
                                           @Parameter(description = "Start date for filtering")
                                           @RequestParam(required = false) String startDate,
                                           @Parameter(description = "End date for filtering")
                                           @RequestParam(required = false) String endDate) {
        try {
            java.time.LocalDate startDateValue = startDate != null ? java.time.LocalDate.parse(startDate) : null;
            java.time.LocalDate endDateValue = endDate != null ? java.time.LocalDate.parse(endDate) : null;
            Map<String, Object> data = pettyCashVoucherService.listPettyCashVoucher(UserContext.getDocumentId(), filters, startDateValue, endDateValue, pageable);
            return success("Petty Cash Payment fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch Petty Cash Payment list: " + ex.getMessage());
        }

    }

    @Operation(
            summary = "Load Petty Cash from PO",
            description = "Loads petty cash details using the provided PO information.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Petty cash from PO fetched successfully",
                            content = @Content(schema = @Schema(implementation = PettyCashFromPoDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "No petty cash found")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/po-change")
    public ResponseEntity<?> loadFromPo(
            @Parameter(description = "Group POID", example = "1001")
            @RequestParam Long groupPoid,

            @Parameter(description = "Company POID", example = "2001")
            @RequestParam Long companyPoid,

            @Parameter(description = "User POID", example = "3001")
            @RequestParam Long userPoid,

            @Parameter(description = "RFQ POID", example = "RFQ-3456")
            @RequestParam String rfqPoid
    ) {

        PettyRefTypeResponse<PettyCashFromPoDto> response =
                pettyCashVoucherService.loadPettyCashFromPo(groupPoid, companyPoid, userPoid, rfqPoid);

        String message = response.getMessage() != null ? response.getMessage() : "Petty Cash from PO fetched successfully";
        return success(message, response.getResponseList());
    }


    @Operation(
            summary = "Load Petty Cash from FF",
            description = "Loads petty cash charges from FF.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Petty cash from FF fetched successfully",
                            content = @Content(schema = @Schema(implementation = PettyCashFromFfDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "No petty cash found"),
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/ff-changes")
    public ResponseEntity<?> loadFromFf(
            @Parameter(description = "Group POID", example = "1001")
            @RequestParam Long groupPoid,

            @Parameter(description = "Company POID", example = "2001")
            @RequestParam Long companyPoid,

            @Parameter(description = "User POID", example = "3001")
            @RequestParam Long userPoid,

            @Parameter(description = "FF POID", example = "FF-7892")
            @RequestParam String ffPoid
    ) {

        PettyRefTypeResponse<PettyCashFromFfDto> response =
                pettyCashVoucherService.loadPettyCashFromFf(groupPoid, companyPoid, userPoid, ffPoid);

        String message = response.getMessage() != null ? response.getMessage() : "Petty Cash from FF fetched successfully";
        return success(message, response.getResponseList());
    }


    @Operation(
            summary = "Load Petty Cash from FDA",
            description = "Loads petty cash data from FDA (Final Delivery Advice).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Petty cash from FDA fetched successfully",
                            content = @Content(schema = @Schema(implementation = PettyCashFromFdaDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "No petty cash found"),
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/fda-change")
    public ResponseEntity<?> loadFromFda(
            @Parameter(description = "Group POID", example = "1001")
            @RequestParam Long groupPoid,

            @Parameter(description = "Company POID", example = "2001")
            @RequestParam Long companyPoid,

            @Parameter(description = "User POID", example = "3001")
            @RequestParam Long userPoid,

            @Parameter(description = "FDA POID", example = "FDA-7789")
            @RequestParam String fdaPoid
    ) {

        PettyRefTypeResponse<PettyCashFromFdaDto> response =
                pettyCashVoucherService.loadPettyCashFromFda(groupPoid, companyPoid, userPoid, fdaPoid);

        String message = response.getMessage() != null ? response.getMessage() : "Petty Cash from FDA fetched successfully";
        return success(message, response.getResponseList());
    }

    @Operation(
            summary = "Load Petty Cash GL Balance",
            description = "Fetches petty cash GL balance for a given document and LOV using stored procedure PROC_PETTY_GL_DEFAULT_BALANCE.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Petty cash GL balance fetched successfully",
                            content = @Content(schema = @Schema(implementation = PettyGlBalanceDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "No GL balance found"),
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/gl-balance")
    public ResponseEntity<?> loadPettyGlBalance(
            @Parameter(description = "Document Key POID", example = "5001")
            @RequestParam Long docKeyPoid,

            @Parameter(description = "LOV Name (e.g., 'PETTY_CASH_GL')", required = true)
            @RequestParam String lovName,

            @Parameter(description = "LOV Value (GL POID)", example = "20001")
            @RequestParam Long lovValue
    ) {

        PettyRefTypeResponse<PettyGlBalanceDto> response = pettyCashVoucherService.loadPettyGlBalance(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                UserContext.getDocumentId(),
                docKeyPoid,
                lovName,
                lovValue
        );

        String message = response.getMessage() != null ? response.getMessage() : "Balance fetched successfully";
        return success(message, response.getResponseList());
    }

    @Operation(
            summary = "Load Petty Cash from GRN",
            description = "Loads pending GRN details for a given supplier using PROC_GL_PETTY_INSERT_GRN_JOBS.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Petty cash from GRN fetched successfully",
                            content = @Content(schema = @Schema(implementation = PettyCashFromGrnDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/load-from-grn")
    public ResponseEntity<?> loadFromGrn(
            @Parameter(description = "Group POID", example = "1001") @RequestParam Long groupPoid,
            @Parameter(description = "Company POID", example = "2001") @RequestParam Long companyPoid,
            @Parameter(description = "User POID", example = "3001") @RequestParam Long userPoid,
            @Parameter(description = "Transaction date", example = "2024-01-01") @RequestParam String transactionDate,
            @Parameter(description = "GRN Supplier POID", example = "123") @RequestParam String grnSupplierPoid
    ) {
        PettyRefTypeResponse<PettyCashFromGrnDto> response =
                pettyCashVoucherService.loadPettyCashFromGrn(groupPoid, companyPoid, userPoid, transactionDate, grnSupplierPoid);
        String message = response.getMessage() != null ? response.getMessage() : "Petty Cash from GRN fetched successfully";
        return success(message, response.getResponseList());
    }

    @Operation(
            summary = "Load Petty Cash from Completed PO",
            description = "Loads items from a completed (non-RFQ/MTA) PO using PROC_GL_PETTY_CREATE_GENRL_PO.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Petty cash from completed PO fetched successfully",
                            content = @Content(schema = @Schema(implementation = PettyCashFromGenrlPoDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/load-from-completed-po")
    public ResponseEntity<?> loadFromCompletedPo(
            @Parameter(description = "Group POID", example = "1001") @RequestParam Long groupPoid,
            @Parameter(description = "Company POID", example = "2001") @RequestParam Long companyPoid,
            @Parameter(description = "User POID", example = "3001") @RequestParam Long userPoid,
            @Parameter(description = "PO POID", example = "456") @RequestParam String poPoid
    ) {
        PettyRefTypeResponse<PettyCashFromGenrlPoDto> response =
                pettyCashVoucherService.loadPettyCashFromCompletedPo(groupPoid, companyPoid, userPoid, poPoid);
        String message = response.getMessage() != null ? response.getMessage() : "Petty Cash from completed PO fetched successfully";
        return success(message, response.getResponseList());
    }

    @Operation(
            summary = "Get allowed ref types for user",
            description = "Returns the list of ref types the current user is allowed to see, via PROC_GL_PETTY_REF_WHERE_CLAUSE.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Allowed ref types fetched successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/allowed-ref-types")
    public ResponseEntity<?> getAllowedRefTypes(
            @Parameter(description = "User POID", example = "3001") @RequestParam Long userPoid
    ) {
        List<String> response = pettyCashVoucherService.getAllowedRefTypes(userPoid);
        return success("Allowed ref types fetched successfully", response);
    }

    @Operation(
            summary = "Get global parameters for Petty Cash Payment page",
            description = "Returns all UI configuration, default values, and validation limits needed by the frontend to initialise the Petty Cash Payment page.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Global parameters fetched successfully",
                            content = @Content(schema = @Schema(implementation = PettyCashGlobalParamsDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/global-params")
    public ResponseEntity<?> getGlobalParams(
            @Parameter(description = "Petty Cash GL POID (used to resolve ledger-specific parameters PETTY_CASH_ADVANCE_LEDGER and PETTY_CASH_LEDGER)")
            @RequestParam(required = false) Long pettyCashGlPoid
    ) {
        PettyCashGlobalParamsDto params = pettyCashVoucherService.getPettyCashGlobalParams(pettyCashGlPoid);
        return success("Global parameters fetched successfully", params);
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for Petty Cash Voucher",
            description = "Generate PDF report for a specific Petty Cash Voucher transaction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Petty Cash Voucher not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "21")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = pettyCashVoucherService.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=petty-cash-voucher-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Petty Cash Voucher: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

    private ResponseEntity<?> successs(String message, Object data) {
        return ResponseEntity.ok(
                Map.of(
                        "message", message,
                        "data", data
                )
        );
    }


}

