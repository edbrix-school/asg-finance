package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.DocumentDownloadHeaderService;
import com.asg.finance.dto.ContraVoucherRequest;
import com.asg.finance.dto.ContraVoucherFullResponse;
import com.asg.finance.entity.GlContraVoucherHdr;
import com.asg.finance.service.ContraVoucherService;
import com.asg.common.lib.security.util.UserContext;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/contra-vouchers")
@RequiredArgsConstructor
@Slf4j
public class ContraVoucherController {

    private final ContraVoucherService contraVoucherService;
    private final LoggingService loggingService;
    private final DocumentDownloadHeaderService downloadHeaderService;

    @Operation(
            summary = "Get contra vouchers list",  
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    description = "Search filter configuration for listing Contra Vouchers",
                    content = @Content(
                            schema = @Schema(implementation = FilterRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Contra Voucher Filters",
                                            value = """
                                                    {
                                                      "operator": "AND",
                                                      "isDeleted": "N",
                                                      "filters": [
                                                        { "searchField": "GLOBALSEARCH", "searchValue": "ASG" },
                                                        { "searchField": "DOC_REF", "searchValue": "ASG10001" },
                                                        { "searchField": "POSTING_NARRATION", "searchValue": "Contra Entry" },
                                                        { "searchField": "TRANSACTION_DATE", "searchValue": ">=2025-01-01" }
                                                      ]
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved contra vouchers list",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listContraVouchers(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @Parameter(description = "Period From date (inclusive) for TRANSACTION_DATE filter", required = false, example = "2025-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @Parameter(description = "Period To date (inclusive) for TRANSACTION_DATE filter", required = false, example = "2025-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo) {

        log.info("listContraVouchers started for groupPoid={}", UserContext.getGroupPoid());

        // Validate period dates
        if ((periodFrom == null && periodTo != null) || (periodFrom != null && periodTo == null)) {
            return badRequest("Both periodFrom and periodTo should be specified or both should be empty.");
        }

        Map<String, Object> response = contraVoucherService.listContraVouchers(UserContext.getDocumentId(), filters, pageable, periodFrom, periodTo);

        log.info("listContraVouchers completed for groupPoid={}", UserContext.getGroupPoid());

            return success("Contra vouchers fetched successfully", response);
    }

    @Operation(
            summary = "Create contra voucher",
            description = "Creates a new contra voucher with header and detail data. actionType must be 'isCreated'",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully created contra voucher",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ContraVoucherFullResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request - Invalid actionType or missing required fields",
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
    public ResponseEntity<?> createContraVoucher(
            @RequestBody ContraVoucherRequest request) {

        log.info("createContraVoucher started for groupPoid={}", UserContext.getGroupPoid());

        // Set groupPoid from context if not provided
        if (request.getGroupPoid() == null) {
            request.setGroupPoid(UserContext.getGroupPoid());
        }

        ContraVoucherFullResponse response = contraVoucherService.createContraVoucher(request);

        log.info("createContraVoucher completed for transactionPoid={}", response.getTransactionPoid());
        
        return success("Contra voucher created successfully", response);
    }

    @Operation(
            summary = "Update contra voucher",
            description = "Updates an existing contra voucher with header and detail data. actionType must be 'isUpdated'. Details can have actionType: isCreated, isUpdated, isDeleted, or noChange/noChanges",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated contra voucher",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ContraVoucherFullResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request - Invalid actionType or missing required fields",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Contra voucher not found",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateContraVoucher(
            @Parameter(description = "Transaction POID", required = true, example = "261")
            @PathVariable Long transactionPoid,
            @RequestBody ContraVoucherRequest request) {

        log.info("updateContraVoucher started for transactionPoid={}, groupPoid={}", 
                transactionPoid, UserContext.getGroupPoid());

        // Set transactionPoid from path variable
        request.setTransactionPoid(transactionPoid);

        ContraVoucherFullResponse response = contraVoucherService.updateContraVoucher(request);

        log.info("updateContraVoucher completed for transactionPoid={}", response.getTransactionPoid());
        
        return success("Contra voucher updated successfully", response);
    }

    @Operation(
            summary = "Get contra voucher by ID",
            description = "Retrieves a contra voucher by transactionPoid",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved contra voucher",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ContraVoucherFullResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Contra voucher not found",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getContraVoucherById(
            @Parameter(description = "Transaction POID", required = true, example = "261")
            @PathVariable Long transactionPoid) {

        log.info("getContraVoucherById started for transactionPoid={}, groupPoid={}", 
                transactionPoid, UserContext.getGroupPoid());

        ContraVoucherFullResponse response = contraVoucherService.getContraVoucherById(transactionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());

        log.info("getContraVoucherById completed for transactionPoid={}", transactionPoid);
        
        return success("Contra voucher fetched successfully", response);
    }

    @Operation(
            summary = "Delete contra voucher",
            description = "Soft deletes a contra voucher by setting deleted flag to 'Y'",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully deleted contra voucher",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Contra voucher not found",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> deleteContraVoucher(
            @Parameter(description = "Transaction POID", required = true, example = "261")
            @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {

        log.info("deleteContraVoucher started for transactionPoid={}, groupPoid={}", 
                transactionPoid, UserContext.getGroupPoid());

        contraVoucherService.deleteContraVoucher(transactionPoid, deleteReasonDto);

        log.info("deleteContraVoucher completed for transactionPoid={}", transactionPoid);
        
        return success("Contra voucher deleted successfully", null);
    }

    @Operation(
            summary = "Check GL Nature",
            description = "Checks GL nature for a given credit GL ID using stored procedure PROC_CHECK_GL_NATURE. Returns status string from stored procedure.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "GL nature checked successfully",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid input provided"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access"),
                    @ApiResponse(responseCode = "404", description = "GL record not found")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/check-gl-nature/{creditGlId}")
    public ResponseEntity<?> checkGlNature(
            @Parameter(description = "Credit GL ID reference identifier", required = true, example = "201")
            @PathVariable Long creditGlId) {
        
        log.info("checkGlNature started for creditGlId={}, groupPoid={}", 
                creditGlId, UserContext.getGroupPoid());
        
        String result = contraVoucherService.checkGlNature(creditGlId);
        log.info("checkGlNature completed for creditGlId={}", creditGlId);
        
        return success("GL nature checked successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for Contra Voucher",
            description = "Generate PDF report for a specific Contra Voucher transaction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Contra Voucher not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "21")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = contraVoucherService.print(transactionPoid);
            return ResponseEntity.ok()
                    .headers(downloadHeaderService.buildAttachmentHeaders(
                            GlContraVoucherHdr.class,
                            transactionPoid,
                            "contra-voucher",
                            "pdf"))
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Contra Voucher: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }
}
