package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;

import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.TelexFileDtlDto;
import com.asg.finance.dto.TelexFileGenerateRequestDto;
import com.asg.finance.dto.TelexFileGenerateResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.service.TelexFileGenerateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.internalServerError;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/v1/telex-file-generate")
@Tag(name = "Telex File Generate", description = "APIs for Telex Transfer / Bank File Generation (Doc ID: 100-153)")
@RequiredArgsConstructor
public class TelexFileGenerateController {

    private final TelexFileGenerateService service;
    private final LoggingService loggingService;

    @Operation(
            summary = "Create Telex File",
            description = "Creates a new Telex File for Transfer",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully created",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TelexFileGenerateResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody TelexFileGenerateRequestDto request
    ) {
        try {
            TelexFileGenerateResponseDto response = service.createTelexFile(request);
            return success("Telex File created successfully", response);
        } catch (Exception ex) {
            return internalServerError("Failed to create Telex File: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Update Telex File",
            description = "Updates an existing Telex File",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully updated"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> update(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Valid @RequestBody TelexFileGenerateRequestDto request
    ) {
        try {
            TelexFileGenerateResponseDto response = service.updateTelexFile(transactionPoid, request);
            return success("Telex File updated successfully", response);
        } catch (Exception ex) {
            return internalServerError("Failed to update Telex File: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get Telex File by ID",
            description = "Retrieves Telex File details",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getById(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid
    ) {
        TelexFileGenerateResponseDto response = service.getTelexFileById(transactionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        return success("Telex File fetched successfully", response);
    }

    @Operation(
            summary = "Soft delete Telex File",
            description = "Marks a Telex File as deleted",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully deleted"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> softDelete(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        service.softDeleteTelexFile(transactionPoid, deleteReasonDto);
        return success("SUCCESS =: Record is Marked as deleted and deactivated");
    }

    @Operation(
            summary = "List Telex Files with Search and Sort (DocId: 100-153)",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (TRANSACTION_POID, DOC_REF, BANK_POID, CREATED_BY). Sorting default on transactionPoid, desc." +
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
                      2. Any combination of specific fields (TRANSACTION_POID, DOC_REF, BANK_POID, CREATED_BY).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                         not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                      4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                      5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      No operator need to send in this case.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "Bank" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      • { "searchField": "TRANSACTION_POID", "searchValue": "1001" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "TRANSACTION_POID", "searchValue": "1001|1002" }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "TRANSACTION_POID", "searchValue": "1001" },
                      • { "searchField": "DOC_REF", "searchValue": "TLX-001|TLX-002" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=TRANSACTION_POID,ASC
                      • sort=DOC_REF,DESC
                    
                    - ### Authorization Parameters (handled by interceptor)
                        - **documentId:** Unique identifier for the document (`100-153`)
                        - **actionRequested:** Action being performed (`VIEW`)
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Telex File Filters",
                                    value = """
                                            {
                                              "operator": "AND",
                                              "isDeleted": "N",
                                              "filters": [
                                                 { "searchField": "GLOBALSEARCH", "searchValue": "Bank" },
                                                 { "searchField": "TRANSACTION_POID", "searchValue": "1001|1002" },
                                                 { "searchField": "DOC_REF", "searchValue": "TLX-001" },
                                                 { "searchField": "BANK_POID", "searchValue": "5001" },
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
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        try {
            if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
                return internalServerError("Both startDate and endDate should be specified or both dates should be empty.");
            }
            Map<String, Object> data = service.listTelexFiles(UserContext.getDocumentId(), filters, startDate, endDate, pageable);
            
            return success("Telex Files fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch Telex File list: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Load Telex Transfer Data",
            description = "Loads pending telex transfer data based on bank selection"
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/load-telex-data")
    public ResponseEntity<?> loadTelexData(
            @Parameter(description = "Bank List Filter", required = false, example = "Y")
            @RequestParam(required = false, defaultValue = "Y") String bankList
    ) {
        try {
            List<TelexFileDtlDto> data = service.loadTelexTransferData(bankList);
            return success("Telex transfer data loaded successfully", data);
        } catch (Exception ex) {
            return internalServerError("Failed to load telex data: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Regenerate Telex File",
            description = "Regenerates the telex file for a given transaction"
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/{debitVoucherPoid}/regenerate")
    public ResponseEntity<?> regenerateTelexFile(
            @Parameter(description = "Debit Voucher POID", required = true)
            @PathVariable Long debitVoucherPoid
    ) {
        try {
            String result = service.regenerateTelexFile(debitVoucherPoid);
            if (result != null && result.contains("ERROR")) {
                return internalServerError("Error regenerating telex file: " + result);
            }
            return success("Telex file regenerated successfully");
        } catch (Exception ex) {
            return internalServerError("Failed to regenerate telex file: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Generate Telex File",
            description = "Generate the telex file for a given transaction"
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/{id}/generate")
    public ResponseEntity<?> generateBankFileButton(
            @Parameter(description = "Telex file generate", required = true)
            @PathVariable Long id
    ) {
        try {
            String result = service.generateBankFileButton(id);
            if (result != null && result.contains("ERROR")) {
                return internalServerError("Error generating telex file: " + result);
            }
            return success("Telex file regenerated successfully");
        } catch (Exception ex) {
            return internalServerError("Failed to generate telex file: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Check Bank Balance",
            description = "Check if sufficient bank balance is available for the telex transfer using PROC_BANK_FILE_CHECK_OD_V2"
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}/check-balance")
    public ResponseEntity<?> checkBankBalance(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid
    ) {
        try {
            String result = service.checkBankBalance(transactionPoid);
            return success("Balance check completed", result);
        } catch (Exception ex) {
            return internalServerError("Failed to check bank balance: " + ex.getMessage());
        }
    }

}
