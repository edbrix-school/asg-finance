package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.*;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.service.ApPurchaseServiceJournal;
import com.asg.finance.service.CreditNoteService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.catalina.User;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/purchase-journal")
public class ApPurchaseJournalController {

    private final ApPurchaseServiceJournal service;
    private final CreditNoteService creditNoteService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    @Operation(
            summary = "Get AP Purchase Journal by Transaction POID",
            description = "Retrieves the AP Purchase Journal (Invoice Header) along with related item, GL, and asset details for the given Transaction POID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "AP Purchase Journal fetched successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApPurchaseInvoiceHdrDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "AP Purchase Journal not found for the given transactionPoid"
            )
    })
    public ResponseEntity<?> getApPurchaseInvoiceHdr(
            @Parameter(
                    description = "Unique Transaction POID of the AP Purchase Invoice Header record",
                    required = true,
                    example = "71031"
            )
            @PathVariable Long transactionPoid
    ) {
        ApPurchaseInvoiceHdrDto result = service.fetchApPurchaseInvoiceHdr(transactionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        return success("AP Purchase Journal fetched successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create AP Purchase Journal",
            description = "Creates a new AP Purchase Journal (Invoice) header with optional item, GL, and asset details."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "AP Purchase Journal created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApPurchaseInvoiceHdrDto.class)
                    )
            )
    })
    public ResponseEntity<?> createApPurchaseJournal(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "AP Purchase Journal payload",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApPurchaseInvoiceHdrDto.class)
                    )
            )
            @RequestBody ApPurchaseInvoiceHdrDto apPurchaseInvoiceHdrDto) {
        try {
            ApPurchaseInvoiceHdrDto result = service.createApPurchaseInvoice(apPurchaseInvoiceHdrDto, UserContext.getDocumentId());
            return success("Purchase Journal created successfully", result);
        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception e) {
            log.error("Error creating Purchase Journal: {}", e.getMessage(), e);
            return internalServerError("Failed to create Purchase Journal: " + e.getMessage());
        }

    }

    @Operation(
            summary = "Update an existing Purchase Journal",
            description = "Partially or fully updates an existing Purchase Journal transaction, including its header and child details (payments, charges, items).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated the Purchase Journal record",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApPurchaseInvoiceHdrDto.class)
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
                            description = "Purchase Journal record not found",
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
    public ResponseEntity<?> updateApPurchaseJournal(
            @Parameter(
                    description = "Purchase Journal Transaction Poid to update", required = true)
            @PathVariable Long transactionPoid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "AP Purchase Journal payload",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApPurchaseInvoiceHdrDto.class)

                    )
            )

            @RequestBody ApPurchaseInvoiceHdrDto apPurchaseInvoiceHdrDto) {
        ApPurchaseInvoiceHdrDto result = service.updateApPurchaseInvoice(transactionPoid, apPurchaseInvoiceHdrDto);
        return success("Purchase Journal updated successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    @Operation(
            summary = "Delete (soft) AP Purchase Journal",
            description = "Marks the AP Purchase Journal header and its child details as deleted (soft delete). No content is removed physically."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "AP Purchase Journal deleted successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "AP Purchase Journal not found for the given transactionPoid"
            )
    })
    public ResponseEntity<?> softDeleteApPurchaseJournal(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {

        service.softDeleteApPurchaseInvoice(transactionPoid, deleteReasonDto);

        return success("Purchase Journal deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    @Operation(
            summary = "List AP Purchase Journal with Search and Sort",
            description = """
                    Provide search filters for AP Purchase Journal. Valid `searchField` values include: GLOBALSEARCH or specific columns such as TRANSACTION_POID, DOC_REF, SUPPLIER_POID, COMPANY_POID, TYPE, DESCRIPTION, CREATED_BY, etc.\n\n
                    Will be searched in all available fields given in `list_of_records_sql` or main table specified in the `doc_master` table.\n\n
                    Sorting will be applied as specified in `list_of_records_sql` in `doc_master`. Display fields for showing columns can be customized through `list_of_display_columns_and_types` in `doc_master`.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved AP Purchase Journal list",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> listOfRecordsWithGenericSearch(
            @ParameterObject
            @Parameter(
                    description = "Pagination and sorting configuration",
                    example = "page=0&size=10&sort=transactionPoid,asc"
            ) Pageable pageable,

            @RequestBody(required = false)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                            - ### Filters:\n
                              Use either:\n
                              1. A single `GLOBALSEARCH` filter, OR\n
                              2. Any combination of specific fields (e.g., TRANSACTION_POID, DOC_REF, SUPPLIER_POID).\n
                              3. `operator` will either have \"AND\" or \"OR\". If not given, it defaults to \"OR\" (not required for GLOBALSEARCH).\n
                              4. `isDeleted`: when 'N' or null, returns non-deleted records. 'Y' returns deleted records.\n
                              5. Sorting defaults to backend configuration; you can override via `sort=<FIELD>,ASC|DESC` in query params.\n
                            \n
                            - #### Global Search:\n
                              Apply one search term across multiple fields. `operator` is ignored.\n
                              • { \"searchField\": \"GLOBALSEARCH\", \"searchValue\": \"ASG54210\" }\n
                            \n
                            - #### Single Field, Single Value:\n
                              • { \"searchField\": \"DOC_REF\", \"searchValue\": \"ASG54210\" }\n
                            \n
                            - #### Single Field, Multiple Values:\n
                              Provide multiple values separated by `|`.\n
                              • { \"searchField\": \"TRANSACTION_POID\", \"searchValue\": \"71026|71031\" }\n
                            \n
                            - #### Multiple Different Fields:\n
                              Combine multiple fields with `operator`.\n
                              • { \"searchField\": \"DOC_REF\", \"searchValue\": \"ASG54210\" }\n
                              • { \"searchField\": \"SUPPLIER_POID\", \"searchValue\": \"1|2\" }\n
                              • \"operator\": \"AND\"/\"OR\"\n
                            \n
                            - ### Sorting:\n
                              Defaults to server config (often primary key ascending). Override using query param: \n
                              • sort=DOC_REF,ASC \n
                              • sort=TRANSACTION_POID,DESC\n
                            \n
                            - ### Authorization Parameters (handled by interceptor):\n
                              - **documentId:** AP Purchase Journal doc id (e.g., `200-103`)\n
                              - **actionRequested:** Action being performed (e.g., `VIEW`)
                            """,
                    required = false,
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = FilterDto.class)),
                            examples = {
                                    @ExampleObject(
                                            name = "Global Search",
                                            value = """
                                                    {
                                                      "operator": "OR",
                                                      "isDeleted": "N",
                                                      "filters": [
                                                        { "searchField": "GLOBALSEARCH", "searchValue": "ASG54210" }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Search by Doc Ref and Supplier",
                                            value = """
                                                    {
                                                      "operator": "AND",
                                                      "isDeleted": "N",
                                                      "filters": [
                                                        { "searchField": "DOC_REF", "searchValue": "ASG54210" },
                                                        { "searchField": "SUPPLIER_POID", "searchValue": "1|2" }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Multiple Transaction POIDs",
                                            value = """
                                                    {
                                                      "operator": "OR",
                                                      "isDeleted": "N",
                                                      "filters": [
                                                        { "searchField": "TRANSACTION_POID", "searchValue": "71026|71031" }
                                                      ]
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            FilterRequestDto filters,

            @RequestParam(required = false)
            @Parameter(description = "Start date (inclusive) for TRANSACTION_DATE filter")
            LocalDate startDate,

            @RequestParam(required = false)
            @Parameter(description = "End date (inclusive) for TRANSACTION_DATE filter")
            LocalDate endDate
    ) {
        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            return badRequest("Both startDate and endDate should be specified or both should be empty.");
        }

        Map<String, Object> result = service.listOfRecordsAndGenericSearch(UserContext.getDocumentId(), filters, startDate, endDate, pageable);

            return success("Purchase Journal list fetched successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/ff-charges-details")
    @Operation(
            summary = "Create AP Purchase details from FF Charge Details",
            description = "Calls PROC_AP_PI_CREATE_FROM_FF_NEW to load FF charges for AP Purchase Journal creation"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FF charges loaded successfully", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> createFromFf(
            @RequestParam String ffPoid
    ) {
        StringBuilder result = new StringBuilder();
        List<ApPurchaseJournalResponseDto> response = service.createFromFf(ffPoid, result);
        if (result.toString() != null && result.toString().toUpperCase().contains("WARNING")) {
            return success(result.toString(), null);
        }
        return success("FF charge details created successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/ff-charges-details/{ffPoid}")
    @Operation(
            summary = "Update FF manifest cost amounts from booked documents",
            description = "Invokes PROC_AP_PI_FF_UPDATE_COST to update FF manifest charge amounts from booked AP Purchase Journal, and Bank documents"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FF cost updated successfully", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateFfCost(
            @PathVariable String ffPoid,
            @RequestParam(required = false) Long piPoid
    ) {
        String result = service.updateFfCost(ffPoid, piPoid);
        return success("FF cost updated successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/fda-charges-details")
    @Operation(
            summary = "Create AP Purchase details from FDA charges",
            description = "Calls PROC_AP_PI_CREATE_FROM_FDA_NEW to load FDA charges for AP Purchase Journal creation"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FDA charges loaded successfully", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> createFromFda(
            @RequestParam String fdaPoid
    ) {
        StringBuilder result = new StringBuilder();
        List<ApPurchaseJournalResponseDto> response = service.createFromFda(fdaPoid, result);
        if (result.toString() != null && result.toString().toUpperCase().contains("WARNING")) {
            return success(result.toString(), null);
        }
        return success("FDA charges processed", response);
    }


    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/fda-charges-details/{fdaPoid}")
    @Operation(
            summary = "Update FDA cost amounts from booked documents",
            description = "Calls PROC_AP_PI_FDA_UPDATE_COST to synchronize FDA cost with AP Purchase Journal, Bank Payment/Debit charges"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FDA cost updated successfully", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateFdaCost(
            @PathVariable String fdaPoid,
            @RequestParam(required = false) Long piPoid
    ) {
        String result = service.updateFdaCost(fdaPoid, piPoid);
        return success("FDA cost updated successfully", result);
    }

    // mapping is incorrect
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @GetMapping("/create-pi-from-po")
    public ResponseEntity<?> createPiFromPo(

            @Parameter(description = "PO POID", required = true, example = "PO-5678")
            @RequestParam String poPoid
    ) {
        List<ApPiFromPoResponseDto> response =
                service.createPiFromPo(poPoid);

        return success("PI created from PO successfully", response);
    }

    // mapping is incorrect
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @GetMapping("/create-pi-from-general-po")
    public ResponseEntity<?> createPiFromGeneralPo(

            @Parameter(description = "PO POID", required = true, example = "PO-GEN-5678")
            @RequestParam String poPoid
    ) {

        List<ApPiFromGeneralPoResponseDto> response =
                service.createPiFromGeneralPo(poPoid);

        return success("PI created from General PO successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/load-fixed-asset-details")
    public ResponseEntity<?> getFaDefaultDetails(

            @Parameter(description = "FA POID", required = true, example = "FA-1001")
            @RequestParam String faPoid
    ) {
        List<ApPiFaDefaultDetailsDto> response = service.getFaDefaultDetails(faPoid);
        return success("FA default details fetched successfully", response);
    }

    // mapping is incorrect
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @GetMapping("/update-mta-po-booking")
    public ResponseEntity<?> updateMtaPoBookingDetails(

            @Parameter(description = "PO POID", required = true, example = "PO-7890")
            @RequestParam String poPoid,

            @Parameter(description = "Booking POID", required = true, example = "5001")
            @RequestParam Long bookPoid
    ) {
        String response = service.updateMtaPoBookingDetails(poPoid, bookPoid);
        return success("MTA PO Booking details updated successfully", response);
    }


    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/check-supplier-po")
    public ResponseEntity<?> validateOutstandingPo(
            @Parameter(description = "SUPPLIER POID", required = true, example = "7890")
            @RequestParam Long supplierPoid
    ) {
        String response = service.validateOutstandingPo(supplierPoid);
        return success("Supplier Poid details fetch successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/rjv-details")
    public ResponseEntity<?> getRjvDefaultDetails(
            @Parameter(description = "RJV POID ", required = true, example = "7890")
            @RequestParam String rjvPoid
    ) {
        List<ApPurchaseInvRjvDefaultDto> response = service.getRjvDefaultDetails(rjvPoid);
        return success("RJV Poid details fetch successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/general-po")
    public ResponseEntity<?> supplierPoidFromPo(
            @Parameter(description = "PO POID", required = true, example = "7890")
            @RequestParam String pOPoid
    ) {
        String response = service.supplierPoidFromPo(pOPoid);
        return success("General PO Poid details fetch successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Get Default Supplier Values",
            description = """
                Fetches default credit values for a selected party using:
                PROC_PI_SET_DEFAULT_CREDIT

                ### Input:
                - Party POID
                - Party Type (CUSTOMER, SUPPLIER, PRINCIPAL)

                ### Output:
                - Credit Period
                - Currency Code & Rate
                - TIN Number
                - Due Date (calculated)
                """
    )
    @GetMapping("/default-value/{partyPoid}")
    public ResponseEntity<?> getDefaultSupplierValues(
            @PathVariable Long partyPoid,
            @RequestParam String partyType) {
        try {
            DefaultCreditValuesDto result = creditNoteService.getDefaultCreditValues(partyPoid, partyType);
            return success("Default supplier values fetched successfully", result);
        } catch (Exception e) {
            log.error("Error fetching default supplier values for partyPoid: {}, partyType: {}", partyPoid, partyType, e);
            return internalServerError("Failed to fetch default credit values: " + e.getMessage());
        }
    }
    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for Purchase Journal",
            description = "Generate PDF report for a specific Purchase Journal transaction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Purchase Journal not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "71031")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = service.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=purchase-journal-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Purchase Journal: {}", transactionPoid, e);
            return internalServerError("Failed to generate PDF: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/validate-duplicate-invoice")
    public ResponseEntity<?> validateDuplicateInvoice(
            @RequestBody ApPurchaseInvoiceHdrDto dto,
            @RequestParam(required = false) Long transactionPoid
    ) {

        String response = service.validateDuplicateInvoice(dto,transactionPoid);

        return success("Duplicate invoice validation completed", response);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/validate-voucher")
    public ResponseEntity<?> validateVoucher(
            @RequestParam String docId,
            @RequestParam String refType,
            @RequestParam String refPoid
    ) {

        try {

            boolean valid = service.validateVoucher(docId, refType, refPoid);

            return success(
                    "Voucher validation completed successfully",
                    valid
            );

        } catch (RuntimeException ex) {

            // 🔹 CLOSED case → return warning message
            return success(
                    ex.getMessage(),
                    null
            );
        }
    }

    @GetMapping("/supplier-gl-poid")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    public ResponseEntity<?> getSupplierGlPoid(
            @RequestParam String partyType,
            @RequestParam Long partyPoid
    ) {

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        String glPoid = service.getSupplierGlPoid(
                groupPoid,
                companyPoid,
                userPoid,
                partyType,
                partyPoid
        );

        return success("Party GL POID fetched successfully", glPoid);
    }
}
