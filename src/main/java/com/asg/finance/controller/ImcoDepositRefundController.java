package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.response.GlPostingViewResponseDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.ImcoDepositRefundRequestDTO;
import com.asg.finance.dto.ImcoDepositRefundResponseDTO;
import com.asg.finance.dto.ImcoRefundLoadResponseDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.service.ImcoDepositRefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@Slf4j
@RestController
@RequestMapping("/v1/imco-deposit-refund")
@RequiredArgsConstructor
public class ImcoDepositRefundController {

    private final ImcoDepositRefundService service;

    @Operation(
            summary = "Create IMCO Deposit Refund Record",
            description = "Creates a new IMCO deposit refund record with cheque and bill details",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "IMCO Deposit Refund created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ImcoDepositRefundResponseDTO.class)
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
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createImcoDepositRefund(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "IMCO Deposit Refund details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ImcoDepositRefundRequestDTO.class)
                    )
            )
            @Valid @RequestBody ImcoDepositRefundRequestDTO request
    ) {
        try {
            ImcoDepositRefundResponseDTO response = service.createImcoDepositRefund(request);
            return success("IMCO Deposit Refund created successfully", response);

        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to create IMCO deposit refund: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get IMCO Deposit Refund by transactionPoid",
            description = "Retrieves IMCO Deposit Refund details based on the provided Tax Period transactionPoid",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the IMCO Deposit Refund details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ImcoDepositRefundResponseDTO.class)
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
                            description = "IMCO Deposit Refund not found",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getImcoDepositRefundById(
            @Parameter(description = "transactionPoid reference identifier", required = true)
            @PathVariable Long transactionPoid) {

        ImcoDepositRefundResponseDTO responseDto = service.getImcoDepositRefundById(transactionPoid);
        return success("IMCO Deposit Refund fetched successfully", responseDto);
    }


    @Operation(
            summary = "Soft delete a IMCO Deposit Refund",
            description = "Marks a IMCO Deposit Refund  as deleted without permanently removing its data",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully soft deleted the IMCO Deposit Refund",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ImcoDepositRefundRequestDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "IMCO Deposit Refund  not found",
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
    public ResponseEntity<?> softDeleteImcoDepositRefund(
            @Parameter(description = "transactionPoid reference identifier", required = true)
            @PathVariable Long transactionPoid) {

        service.softDeleteImcoDepositRefund(transactionPoid);
        return success("IMCO Deposit Refund has been soft deleted successfully");
    }

    @Operation(
            summary = "List IMCO Deposit Refund with Search and Sort (DocId: 400-108)",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (TRANSACTION_POID, DOC_REF, CREATED_BY). Sorting default on locationPoid, desc." +
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
                      2. Any combination of specific fields (TRANSACTION_POID, DOC_REF, CREATED_BY).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                         not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                      4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                      5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      No operator need to send in this case.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "Warehouse" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      • { "searchField": "TRANSACTION_POID", "searchValue": "LOC01" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "TRANSACTION_POID", "searchValue": "LOC01|LOC02" }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "TRANSACTION_POID", "searchValue": "LOC01" },
                      • { "searchField": "DOC_REF", "searchValue": "Head Office|Branch" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=TRANSACTION_POID,ASC
                      • sort=DOC_REF,DESC
                    
                    - ### Authorization Parameters (handled by interceptor)
                        - **documentId:** Unique identifier for the document (`400-108`)
                        - **actionRequested:** Action being performed (`VIEW`)
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "IMCO Deposit Refund Filters",
                                    value = """
                                            {
                                              "operator": "AND",
                                              "isDeleted": "N",
                                              "filters": [
                                                 { "searchField": "GLOBALSEARCH", "searchValue": "Warehouse" },
                                                 { "searchField": "TRANSACTION_POID", "searchValue": "LOC01|LOC02" },
                                                 { "searchField": "DOC_REF", "searchValue": "Main Office" },
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
    public ResponseEntity<?> listImcoDepositRefund(@ParameterObject Pageable pageable,
                                                   @RequestBody(required = false) FilterRequestDto filters,
                                                   @RequestParam(required = false) @Parameter(description = "Start date (inclusive) for TRANSACTION_DATE filter") LocalDate startDate,
                                                   @RequestParam(required = false) @Parameter(description = "End date (inclusive) for TRANSACTION_DATE filter") LocalDate endDate) {
        try {
            if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
                return badRequest("Both startDate and endDate should be specified or both should be empty.");
            }
            Map<String, Object> data = service.listImcoDepositRefund(UserContext.getDocumentId(), filters, startDate, endDate, pageable);
            return success("IMCO Deposit Refund successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch IMCO Deposit Refund list: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get IMCO Deposit Refund Cheque Details",
            description = "Retrieves IMCO Bill and Cheque details using the provided Receipt Number and BL Number.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved IMCO Cheque and Bill details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ImcoRefundLoadResponseDto.class)
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
                            description = "No cheque or bill details found for the given inputs",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/cheque-details")
    public ResponseEntity<?> getChequeDetails(
            @Parameter(description = "Receipt POID used to filter cheque details", required = true, example = "2001")
            @RequestParam(required = false) Long receiptPoid,

            @Parameter(description = "Receipt Number used to filter cheque details", required = true, example = "RCP-2025-001")
            @RequestParam(required = false) String receiptNumber
    ) throws SQLException {
        ImcoRefundLoadResponseDto responseDto = service.getChequeDetails(receiptPoid, receiptNumber);
        return success("IMCO Cheque and Bill details fetched successfully", responseDto);
    }

    @Operation(
            summary = "Get IMCO Deposit Refund GL Posting Details",
            description = "Fetches General Ledger (GL) Posting details including Ledger Entries, Billwise Breakup, Cost Breakup, and VAT Breakup for the provided Document ID and Transaction POID.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved GL Posting details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = GlPostingViewResponseDto.class)
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
                            description = "No GL Posting details found for the given inputs",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/gl-posting")
    public ResponseEntity<?> getGlPostingDetails(
            @Parameter(description = "Transaction POID linked to the document", required = true, example = "1001")
            @RequestParam Long transactionPoid
    ) {
        GlPostingViewResponseDto responseDto = service.getGlPostingDetails(UserContext.getUserId(), transactionPoid);
        return success("IMCO GL Posting details fetched successfully", responseDto);
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for IMCO Deposit Refund",
            description = "Generate PDF report for a specific IMCO Deposit Refund transaction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "IMCO Deposit Refund not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "281")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = service.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=imco-deposit-refund-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for IMCO Deposit Refund: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

}