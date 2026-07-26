package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.DocumentDownloadHeaderService;
import com.asg.finance.dto.PurchaseOrderRequest;

import com.asg.finance.dto.PurchaseOrderResponse;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/purchase-orders")
@Slf4j
public class PurchaseOrderController {

    private final PurchaseOrderService service;
    private final LoggingService loggingService;
    private final DocumentDownloadHeaderService downloadHeaderService;


    @Operation(
            summary = "Create a new Purchase Order",
            description = """
                    Creates a new Purchase Order based on RefType:
                    • GENERAL → Uses PurchaseOrderItem table
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created the Purchase Order",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PurchaseOrderResponse.class)
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
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createPurchaseOrder(

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Purchase Order request payload",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PurchaseOrderRequest.class)
                    )
            )
            @Parameter(description = "Purchase Order details", required = true)
            @Valid @RequestBody PurchaseOrderRequest request
    ) {

        try {
            PurchaseOrderResponse response = service.createGeneralPurchaseOrder(UserContext.getDocumentId(), request);
            return success("Purchase Order created successfully", response);

        } catch (Exception e) {
            log.error("Error creating purchase order: {}", e.getMessage(), e);
            return internalServerError("Failed to create purchase order : " + e.getMessage());
        }
    }


    @Operation(
            summary = "Update an existing Purchase Order",
            description = "Updates Purchase Order header and item details."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated Purchase Order",
                    content = @Content(schema = @Schema(implementation = PurchaseOrderResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Purchase Order not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updatePurchaseOrder(
            @Parameter(description = "transactionPoid reference identifier", required = true)
            @PathVariable Long transactionPoid,
            @Parameter(description = "Purchase Order details", required = true)
            @Valid @RequestBody PurchaseOrderRequest request
    ) {

        try {
            PurchaseOrderResponse response = service.updatePurchaseOrder(UserContext.getDocumentId(), transactionPoid, request);
            return success("Purchase Order updated successfully", response);

        } catch (Exception e) {
            log.error("Error updating purchase order: {}", e.getMessage(), e);
            return internalServerError("Failed to update purchase order: " + e.getMessage());
        }
    }


    @Operation(
            summary = "Get Purchase Order by ID",
            description = "Retrieve Purchase Order with all details."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully fetched Purchase Order",
                    content = @Content(
                            schema = @Schema(implementation = PurchaseOrderResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Purchase Order not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> findById(

            @Parameter(description = "transactionPoid reference identifier", required = true)
            @PathVariable Long transactionPoid
    ) {

        try {
            PurchaseOrderResponse response = service.findById(transactionPoid);
            loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
            return success("Purchase Order fetched successfully", response);

        } catch (Exception e) {
            log.error("Error fetching purchase order: {}", e.getMessage(), e);
            return internalServerError("Failed to fetch purchase order: " + e.getMessage());
        }
    }


    @Operation(
            summary = "Soft delete a Purchase Order",
            description = "Marks a Purchase Order as deleted without permanently removing its data",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully soft deleted the Purchase Order",
                            content = @Content(
                                    mediaType = "application/json"
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Purchase Order not found",
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
    public ResponseEntity<?> deletePurchaseOrder(
            @Parameter(description = "Transaction POID of the Purchase Order", required = true, example = "1001")
            @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        service.deletePurchaseOrder(transactionPoid, deleteReasonDto);
        return success("Purchase Order deleted successfully");
    }

    @Operation(
            summary = "List Purchase Order with Search and Sort (DocId: 200-101)",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (TRANSACTION_POID, TRANSACTION_DATE, DOC_REF, REF_TYPE). " +
                    "Will be searched in all available fields given in list_of_records_sql or main_table field in doc_master table. " +
                    "Sorting will be applied as specified in list_of_records_sql in doc_master table. " +
                    "Display fields for showing columns can be customized through list_of_display_columns_and_types field in doc_master."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields (TRANSACTION_POID, TRANSACTION_DATE, DOC_REF, REF_TYPE).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                         not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                      4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                      5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      No operator need to send in this case.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "36290" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      • { "searchField": "DOC_REF", "searchValue": "ASGOP113" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "REF_TYPE", "searchValue": "GENERAL" }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "DOC_REF", "searchValue": "ASGOP113" },
                      • { "searchField": "REF_TYPE", "searchValue": "GENERAL" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=TRANSACTION_DATE,ASC
                    
                    
                    - ### Authorization Parameters (handled by interceptor)
                        - **documentId:** Unique identifier for the document (`200-101`)
                        - **actionRequested:** Action being performed (`VIEW`)
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Purchase Order Filters",
                                    value = """
                                            {
                                              "operator": "AND",
                                              "isDeleted": "N",
                                              "filters": [
                                                 { "searchField": "TRANSACTION_POID", "searchValue": "36290" },
                                                 { "searchField": "REF_TYPE", "searchValue": "GENERAL" },
                                                 { "searchField": "DOC_REF", "searchValue": "ASGOP113" },
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
    public ResponseEntity<?> getListPurchaseOrder(@ParameterObject Pageable pageable,
                                                  @RequestBody(required = false) FilterRequestDto filters,
                                                  @Parameter(description = "Start date for filtering")
                                                  @RequestParam(required = false) String startDate,
                                                  @Parameter(description = "End date for filtering")
                                                  @RequestParam(required = false) String endDate) {
        try {
            java.time.LocalDate startDateValue = startDate != null ? java.time.LocalDate.parse(startDate) : null;
            java.time.LocalDate endDateValue = endDate != null ? java.time.LocalDate.parse(endDate) : null;
            Map<String, Object> data = service.listPurchaseOrder(UserContext.getDocumentId(), filters, startDateValue, endDateValue, pageable);
            return success("Purchase Order fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch Purchase Order list: " + ex.getMessage());
        }
    }


    @Operation(
            summary = "Create Purchase Order from RFQ (Stored Procedure)",
            description = """
                    Calls stored procedure **PROC_AP_PO_CREATE_FROM_RFQ** to generate
                    Purchase Order details from an existing RFQ.
                    
                    ### Required Inputs:
                    - loginGroupPoid → Logged-in user's group
                    - loginUserPoid → Logged-in user's ID
                    - loginCompanyPoid → Company context
                    - poPoid → Purchase Order POID (header)
                    - supplierPoid → Supplier identifier from RFQ
                    - rfqPoid → RFQ POID
                    
                    ### Output:
                    Returns the result string returned by the stored procedure.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "PO created successfully from RFQ",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = String.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/create-from-rfq")
    public ResponseEntity<?> createPOFromRFQ(

            @Parameter(description = "Purchase Order POID", required = true, example = "50001")
            @RequestParam Long poPoid,

            @Parameter(description = "Supplier POID from RFQ", required = true, example = "3001")
            @RequestParam String supplierPoid,

            @Parameter(description = "RFQ POID", required = true, example = "7001")
            @RequestParam String rfqPoid
    ) {

        try {
            String result = service.createPOFromRFQ(
                    UserContext.getGroupPoid(),
                    UserContext.getUserPoid(),
                    UserContext.getCompanyPoid(),
                    poPoid,
                    supplierPoid,
                    rfqPoid
            );

            return success("PO created successfully from RFQ", result);

        } catch (Exception e) {
            log.error("Error creating PO from RFQ: {}", e.getMessage(), e);
            return internalServerError("Failed to create PO from RFQ: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for Purchase Order",
            description = "Generate PDF report for a specific Purchase Order transaction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Purchase Order not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "21")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = service.print(transactionPoid);
            return ResponseEntity.ok()
                    .headers(downloadHeaderService.buildAttachmentHeaders(
                            UserContext.getDocumentId(),
                            transactionPoid,
                            "purchase-order",
                            "pdf"))
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Purchase Order: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

}
