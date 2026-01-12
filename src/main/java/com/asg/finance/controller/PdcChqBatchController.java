package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.*;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.service.PdcChqBatchService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/pdc-batch")
@Slf4j
public class PdcChqBatchController {

    private final PdcChqBatchService service;

    @Operation(
            summary = "Create PDC Cheque Batch",
            description = """
                    Creates a new PDC Cheque Batch (Header + Details).
                    
                    Business Rules:
                    • PRE_PRINTED = Y → CHQ_START_NO must be 6 digits
                    • Debit total must equal Credit total
                    • DET_ROW_ID auto-generated (1,2,3…)
                    
                    Trigger will auto-generate:
                    • TRANSACTION_POID
                    • DOC_REF
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Batch created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PdcChqBatchHdrResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation or business rule error",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unexpected server error",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createPdcBatch(

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "PDC Cheque Batch creation payload",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PdcChqBatchHdrRequestDto.class)
                    )
            )
            @Parameter(description = "Pdc Batch details", required = true)
            @Valid @RequestBody PdcChqBatchHdrRequestDto request
    ) {
        try {
            PdcChqBatchHdrResponseDto response = service.createBatch(request);
            return success("Pdc Batch created successfully", response);

        } catch (ValidationException e) {
            return internalServerError(e.getMessage());

        } catch (Exception ex) {
            return internalServerError("Failed to create Pdc Batch : " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Update PDC Cheque Batch",
            description = """
                    Updates existing PDC Cheque Batch.
                    Old detail rows are deleted and new ones are inserted.
                    All business validations (DR=CR, cheque no rules) apply.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Batch updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "404", description = "Batch not found"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updatePdcBatch(
            @Parameter(description = "Transaction POID to update", required = true)
            @PathVariable Long transactionPoid,
            @Parameter(description = "Pdc Batch details", required = true)
            @Valid @RequestBody PdcChqBatchHdrRequestDto request
    ) {
        try {
            PdcChqBatchHdrResponseDto response =
                    service.updateBatch(transactionPoid, request);

            return success("PDC Batch updated successfully", response);

        } catch (ValidationException e) {
            return internalServerError(e.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to update PDC Batch: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Fetch PDC Cheque Batch by TransactionPoid",
            description = """
                    Returns Header + Detail information for the given TRANSACTION_POID.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Batch fetched successfully"),
                    @ApiResponse(responseCode = "404", description = "Batch not found"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getPdcBatchById(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid
    ) {
        try {
            PdcChqBatchHdrResponseDto response = service.findById(transactionPoid);
            return success("PDC Batch fetched successfully", response);
        } catch (Exception ex) {
            return internalServerError("Failed to fetch PDC Batch: " + ex.getMessage());
        }
    }


    @Operation(
            summary = "Delete PDC Cheque Batch",
            description = """
                    Deletes a PDC Cheque Batch including all detail rows.
                    Detail rows are deleted first due to FK constraint.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Batch deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Batch not found"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> deletePdcBatch(
            @Parameter(description = "Transaction POID to delete", required = true)
            @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        try {
            service.deletePdcBatch(transactionPoid, deleteReasonDto);
            return success("PDC Batch deleted successfully", null);

        } catch (Exception ex) {
            return internalServerError("Failed to delete PDC Batch: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "List PDC Cheque Batches with Search and Sort (DocId: 400-113)",
            description = """
                    Provides advanced search and sorting on PDC Cheque Batch records.
                    
                    ## Search Rules:
                    1. Use either:
                       - `GLOBALSEARCH` (single value searched across multiple fields), OR
                       - Specific search fields such as: TRANSACTION_POID, PAY_GL_POID, PAYING_TO, CREATED_BY, DOC_REF.
                    2. If using specific fields, `operator` (AND/OR) defines how the filters combine.
                    3. `isDeleted` = 'N' returns active records; 'Y' returns deleted ones.
                    4. Multiple values can be separated using `|` (OR condition inside the field).
                    
                    ## Examples:
                    - Global Search:
                      `{ "searchField": "GLOBALSEARCH", "searchValue": "March" }`
                    
                    - Specific Field Search:
                      `{ "searchField": "TRANSACTION_POID", "searchValue": "1001|1002" }`
                    
                    ## Sorting:
                    - Default sort is applied based on doc_master.list_of_records_sql
                    - Override by passing query parameter:
                      `sort=TRANSACTION_POID,DESC`
                    
                    ## Authorization (handled by interceptor):
                    - `documentId` = Unique document identifier (400-113)
                    - `actionRequested` = VIEW
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Filter criteria for listing PDC Cheque Batches",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = FilterRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "PDC Batch Filters",
                                            value = """
                                                    {
                                                      "operator": "AND",
                                                      "isDeleted": "N",
                                                      "filters": [
                                                          { "searchField": "GLOBALSEARCH", "searchValue": "ABC" },
                                                          { "searchField": "TRANSACTION_POID", "searchValue": "101|102" },
                                                          { "searchField": "PAY_GL_POID", "searchValue": "5001" },
                                                          { "searchField": "PAYING_TO", "searchValue": "VendorX" }
                                                      ]
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDC Cheque Batch list fetched successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listPdcBatch(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @Parameter(description = "Start date for filtering")
            @RequestParam(required = false) String startDate,

            @Parameter(description = "End date for filtering")
            @RequestParam(required = false) String endDate
    ) {
        try {

            LocalDate startDateValue = startDate != null ? LocalDate.parse(startDate) : null;
            LocalDate endDateValue = endDate != null ? LocalDate.parse(endDate) : null;

            Map<String, Object> data =
                    service.listPdcBatchCreation(UserContext.getDocumentId(), filters, startDateValue, endDateValue, pageable);

            return success("PDC Cheque Batch list fetched successfully", data);

        } catch (Exception ex) {
            return internalServerError("Unable to fetch PDC Cheque Batch list: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Validate Pay GL Breakup",
            description = """
                    Calls Oracle procedure PROC_PDC_PAY_GL_BREAKUP_CHECK.
                    
                    Returns:
                    • BILL_WISE → If GL requires bill reference  
                    • COST_GROUP → If GL requires cost centre  
                    • NODATA → No configuration found  
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Validation result returned successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid GL"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/validate-paygl")
    public ResponseEntity<?> validatePayGlBreakup(
            @Parameter(description = "GL POID to validate", required = true)
            @RequestParam Long payGlPoid
    ) {
        try {
            PayGlBreakupCheckResponseDto resp = service.validatePayGl(payGlPoid);
            return success("Pay GL breakup check completed", resp);

        } catch (Exception ex) {
            return internalServerError("Pay GL validation failed: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Run PDC Batch Creation Procedure",
            description = """
                    Executes Oracle procedure PROC_PDC_CHQ_BATCH_CREATION using the data provided in the request DTO.
                    
                    The procedure auto-generates cheque detail rows based on:
                    • Number of Cheques
                    • Cheque Amount
                    • Start Cheque No
                    • Start Date
                    • Pre-Printed flag
                    
                    Returns:
                    • SUCCESS : Batch created successfully  
                    • WARNING : Issues detected  
                    • ERROR / NODATA : Procedure error  
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Procedure executed successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "500", description = "Error executing procedure")
            }
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/{transactionPoid}/run-batch-creation")
    public ResponseEntity<?> runBatchCreationProcedure(
            @Parameter(description = "Transaction POID for this batch", required = true)
            @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Header fields required for running the batch creation procedure",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PdcBatchCreationProcRequest.class))
            )
            @Valid @RequestBody PdcBatchCreationProcRequest request
    ) {
        try {
            request.setTransactionPoid(transactionPoid);
            PdcBatchCreationProcResponse status = service.processBatch(request);
            return success("Batch creation procedure executed", status);

        } catch (Exception ex) {
            return internalServerError("Failed to run batch creation procedure: " + ex.getMessage());
        }
    }


    @Operation(
            summary = "Run Bank Posting Procedure for PDC Cheque Batch",
            description = """
                    Executes Oracle procedure `PROC_PDC_CHQ_BANK_POSTING` for the given PDC Cheque Batch.
                    
                    This procedure:
                    • Validates Batch Details
                    • Posts Bank Payment Header + Detail GL Rows
                    • Creates Bill-wise & Cost-Center breakups
                    • Creates Bank Payment Entry (BPV)
                    • Updates PDC detail rows with BPV_POID & BPV_REF
                    
                    Returns:
                    • SUCCESS : Bank payment posted successfully
                    • WARNING : Validation issue
                    • NODATA / ERROR : Failure during posting
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bank Posting Procedure executed successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation or input error"),
                    @ApiResponse(responseCode = "404", description = "Batch not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/{transactionPoid}/run-bank-posting")
    public ResponseEntity<?> runBankPostingProcedure(
            @Parameter(description = "Transaction POID for this batch", required = true)
            @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Header fields required for running the batch creation procedure",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PdcBankPostingProcRequest.class))
            )
            @Valid @RequestBody PdcBankPostingProcRequest request
    ) {
        try {
            request.setTransactionPoid(transactionPoid);
            PdcBatchCreationProcResponse status = service.runBankPostingProcedure(request);
            return success("Bank Posting Procedure executed successfully", status);

        } catch (RuntimeException ex) {
            return internalServerError("Failed to run Bank Posting Procedure: " + ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Unexpected error during Bank Posting Procedure: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Run Excel-Based PDC Cheque Batch Creation",
            description = """
                    Executes Oracle procedure `PROC_PDC_CHQ_BATCH_CREATION_XL` to create 
                    PDC cheque batch details from Excel-uploaded temporary table.
                    
                    This procedure:
                    • Reads data from PDC_BATCH_EXCEL_UPLOAD_TEMP
                    • Validates cheque details, GL accounts, cost centers
                    • Generates batch detail rows in GL_PDC_CHQ_BATCH_DTL
                    • Handles cheque numbering & date auto-increment
                    • Logs system activity for auditing
                    
                    Returns:
                    • SUCCESS : Batch Created successfully
                    • WARNING : Any validation issue
                    • NODATA  : Missing required data
                    • ERROR   : Failure during processing
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Excel Batch Creation Procedure executed successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation or input error"),
                    @ApiResponse(responseCode = "404", description = "Batch not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/{transactionPoid}/run-excel-batch-creation")
    public ResponseEntity<?> runExcelBatchCreationProcedure(
            @Parameter(description = "Transaction POID for this batch", required = true)
            @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Fields required to run the Excel Batch Creation Procedure",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PdcBatchCreationExcelProcRequest.class))
            )
            @Valid @RequestBody PdcBatchCreationExcelProcRequest request
    ) {
        try {
            request.setTransactionPoid(transactionPoid);
            PdcBatchCreationProcResponse status = service.createBatchFromExcel(request);
            return success("Excel Batch Creation Procedure executed successfully", status);

        } catch (RuntimeException ex) {
            return internalServerError("Failed to run Excel Batch Creation Procedure: " + ex.getMessage());

        } catch (Exception ex) {
            return internalServerError("Unexpected error during Excel Batch Creation Procedure: " + ex.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping(value = "/upload-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadExcel(
            @RequestPart("file") MultipartFile file
    ) {

        try {
            if (file == null || file.isEmpty()) {
                return badRequest("Uploaded file is empty or missing");
            }
            String message = service.uploadExcel(file);
            return success("File Upload Successfully", message);
        } catch (Exception e) {
            return internalServerError("Unexpected error during Excel Uploading: " + e.getMessage());

        }
    }

}
