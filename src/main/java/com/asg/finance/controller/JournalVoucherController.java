package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.*;
import jakarta.validation.Valid;
import com.asg.finance.service.JournalVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/journal-voucher")
public class JournalVoucherController {

    private final JournalVoucherService journalVoucherService;

    @Operation(
            summary = "Create Journal Voucher",
            description = "Creates a new journal voucher with GL details, asset disposal, or asset capitalization"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Journal Voucher created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation errors"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = {
                            @ExampleObject(
                                    name = "GENERAL Type with GL Details",
                                    value = """
                                            {
                                              "transactionDate": "2025-12-16",
                                              "refType": "GENERAL",
                                              "currencyCode": "USD",
                                              "currencyRate": 0.377,
                                              "amount": 1000.00,
                                              "bhdAmount": 377.00,
                                              "postingNarration": "Monthly journal adjustments",
                                              "wdvAccountGl": 1001,
                                              "multiCompany": false,
                                              "remarks": "General ledger adjustments",
                                              "confidentialRemarks": "Internal audit notes",
                                              "glDetails": [
                                                {
                                                  "actionType": "ISCREATED",
                                                  "type": "DR",
                                                  "companyPoid": 1,
                                                  "glPoid": 1001,
                                                  "drAmt": 1000.00,
                                                  "crAmt": 0.00,
                                                  "remarks": "Debit entry",
                                                  "costCenterBreakup": [
                                                    {
                                                      "costGroup": "DEPT",
                                                      "costPoid": "CC001",
                                                      "amount": 600.00,
                                                      "actionType": "isCreated"
                                                    }
                                                  ],
                                                  "billWiseBreakup": [
                                                    {
                                                      "billRefType": "INVOICE",
                                                      "billRef": "INV-001",
                                                      "billDueDate": "2025-12-30",
                                                      "type": "DR",
                                                      "amount": 1000.00,
                                                      "billRemarks": "Invoice payment",
                                                      "actionType": "isCreated"
                                                    }
                                                  ]
                                                },
                                                {
                                                  "actionType": "ISCREATED",
                                                  "type": "CR",
                                                  "companyPoid": 1,
                                                  "glPoid": 2001,
                                                  "drAmt": 0.00,
                                                  "crAmt": 1000.00,
                                                  "remarks": "Credit entry"
                                                }
                                              ]
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "ASSET_DISPOSAL Type",
                                    value = """
                                            {
                                              "transactionDate": "2025-12-16",
                                              "refType": "ASSET_DISPOSAL",
                                              "amount": 5000.00,
                                              "bhdAmount": 5000.00,
                                              "postingNarration": "Asset disposal - Old equipment",
                                              "wdvAccountGl": 3001,
                                              "multiCompany": false,
                                              "remarks": "Disposing old equipment",
                                              "confidentialRemarks": "Equipment beyond repair",
                                              "assetDetails": [
                                                {
                                                  "faPoid": 12345,
                                                  "process": "DISPOSAL",
                                                  "scrapSoldDate": "2025-12-16",
                                                  "scrapSoldValue": 500.00,
                                                  "remarks": "Sold as scrap"
                                                }
                                              ]
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "ASSET_CAPITALIZATION Type",
                                    value = """
                                            {
                                              "transactionDate": "2025-12-16",
                                              "refType": "ASSET_CAPITALIZATION",
                                              "amount": 25000.00,
                                              "bhdAmount": 25000.00,
                                              "postingNarration": "New equipment capitalization",
                                              "multiCompany": false,
                                              "remarks": "Capitalizing new machinery",
                                              "confidentialRemarks": "Strategic equipment purchase",
                                              "assetCapitalization": [
                                                {
                                                  "faPoid": 12346,
                                                  "faDescription": "Production Equipment",
                                                  "faCategory": 101,
                                                  "assetType": "MACHINERY",
                                                  "assetValue": 25000.00,
                                                  "remarks": "New production line"
                                                }
                                              ]
                                            }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createJournalVoucher(
            @Valid @RequestBody JournalVoucherRequest request
    ) {
        try {
            JournalVoucherResponse response = journalVoucherService.createJournalVoucher(request, UserContext.getDocumentId());
            return success("Journal Voucher created successfully", response);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return notFound("Journal Voucher not found: " + e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to create journal voucher: " + e.getMessage());
        }
    }

    @Operation(summary = "List Journal Vouchers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Journal Vouchers list retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation errors"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Database error")}
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            name = "Journal Voucher List",
                            value = """
                                    {
                                       "operator": "OR",
                                       "isDeleted": "N",
                                       "filters": [
                                         {
                                           "searchField": "DOC_REF",
                                           "searchValue": "JV-2025-001"
                                         },
                                         {
                                           "searchField": "REF_TYPE",
                                           "searchValue": "GENERAL"
                                         }
                                       ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listJournalVouchers(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            return badRequest("Both startDate and endDate should be specified or both dates should be empty.");
        }
        Map<String, Object> response = journalVoucherService.listJournalVouchers(UserContext.getDocumentId(), filters, startDate, endDate, pageable);
        return success("Journal Vouchers list retrieved successfully", response);
    }

    @Operation(
            summary = "Update Journal Voucher",
            description = "Updates an existing journal voucher"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Journal Voucher updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation errors"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            name = "Update Journal Voucher",
                            value = """
                                    {
                                      "transactionDate": "2025-12-16",
                                      "refType": "GENERAL",
                                      "amount": 1500.00,
                                      "bhdAmount": 1500.00,
                                      "postingNarration": "Updated journal entry",
                                      "multiCompany": false,
                                      "remarks": "Updated remarks",
                                      "confidentialRemarks": "Updated confidential notes",
                                      "glDetails": [
                                        {
                                          "detRowId": 1,
                                          "actionType": "ISUPDATED",
                                          "type": "DR",
                                          "companyPoid": 1,
                                          "glPoid": 1001,
                                          "drAmt": 1500.00,
                                          "crAmt": 0.00,
                                          "remarks": "Updated debit entry"
                                        },
                                        {
                                          "detRowId": 2,
                                          "actionType": "ISUPDATED",
                                          "type": "CR",
                                          "companyPoid": 1,
                                          "glPoid": 2001,
                                          "drAmt": 0.00,
                                          "crAmt": 1500.00,
                                          "remarks": "Updated credit entry"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateJournalVoucher(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Valid @RequestBody JournalVoucherRequest request
    ) {
        try {
            JournalVoucherResponse response = journalVoucherService.updateJournalVoucher(transactionPoid, request, UserContext.getDocumentId());
            return success("Journal Voucher updated successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to update journal voucher: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Create New Fixed Asset",
            description = "Creates a new fixed asset in master for use in asset capitalization"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fixed Asset created successfully"),
            @ApiResponse(responseCode = "400", description = "Duplicate asset code or invalid data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/create-fixed-asset")
    public ResponseEntity<?> createFixedAsset(
            @Valid @RequestBody CreateFixedAssetRequest request
    ) {
        try {
            CreateFixedAssetResponse response = journalVoucherService.createFixedAsset(request);
            return success("Fixed Asset created successfully", response);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to create fixed asset: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Get Journal Voucher by ID",
            description = "Retrieve a single journal voucher with all details based on refType"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Journal Voucher retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getJournalVoucherById(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid
    ) {
        try {
            JournalVoucherDetailResponse response = journalVoucherService.getJournalVoucherById(transactionPoid);
            return success("Journal Voucher retrieved successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to retrieve journal voucher: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Delete Journal Voucher",
            description = "Soft deletes a journal voucher (must not be posted)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Journal Voucher deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> deleteJournalVoucher(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        try {
            journalVoucherService.deleteJournalVoucher(transactionPoid, deleteReasonDto);
            return success("Journal Voucher deleted successfully", null);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (IllegalStateException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to delete journal voucher: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Post Journal Voucher to GL",
            description = "Posts journal voucher to general ledger"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Journal Voucher posted successfully"),
            @ApiResponse(responseCode = "400", description = "Debit/Credit mismatch or validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "409", description = "Already posted"),
            @ApiResponse(responseCode = "500", description = "Posting failed")
    })
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{transactionPoid}/post")
    public ResponseEntity<?> postJournalVoucher(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Valid @RequestBody PostJournalVoucherRequest request
    ) {
        try {
            PostJournalVoucherResponse response = journalVoucherService.postJournalVoucher(transactionPoid, request, UserContext.getDocumentId());
            return success("Journal Voucher posted successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to post journal voucher: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Get GL Detail Totals",
            description = "Calculate debit and credit totals for GENERAL type journal vouchers"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Totals calculated successfully"),
            @ApiResponse(responseCode = "400", description = "Not applicable for non-GENERAL type"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}/totals")
    public ResponseEntity<?> getGlDetailTotals(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid
    ) {
        try {
            JournalVoucherTotalsResponse response = journalVoucherService.getGlDetailTotals(transactionPoid);
            return success("Totals calculated successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to calculate totals: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Get Asset Depreciation Details",
            description = "Fetch asset depreciation details for disposal"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset depreciation details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Asset not found"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/asset-depreciation-details")
    public ResponseEntity<?> getAssetDepreciationDetails(
            @Valid @RequestBody AssetDepreciationRequest request
    ) {
        try {
            JournalVoucherAssetDetailDto response = journalVoucherService.getAssetDepreciationDetails(request.getFaPoid());
            return success("Asset depreciation details retrieved successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to retrieve asset details: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Get Asset Capitalization Details",
            description = "Fetch fixed asset details for capitalization"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset capitalization details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Asset not found"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/asset-capitalization-details")
    public ResponseEntity<?> getAssetCapitalizationDetails(
            @Valid @RequestBody AssetDepreciationRequest request
    ) {
        try {
            JournalVoucherCapitalizationDto response = journalVoucherService.getAssetCapitalizationDetails(request.getFaPoid());
            return success("Asset capitalization details retrieved successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to retrieve asset details: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Update Asset Detail",
            description = "Update asset disposal detail and call stored procedure"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset detail updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid process type"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Asset detail not found"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}/asset-details/{sn}")
    public ResponseEntity<?> updateAssetDetail(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Parameter(description = "Serial number", required = true)
            @PathVariable Long sn,
            @Valid @RequestBody UpdateAssetDetailRequest request
    ) {
        try {
            journalVoucherService.updateAssetDetail(transactionPoid, sn, request);
            return success("Asset detail updated successfully", null);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to update asset detail: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Calculate Currency Conversion",
            description = "Calculate BHD amount based on currency code, amount, and transaction date"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Currency conversion calculated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid currency code"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Currency rate not found"),
            @ApiResponse(responseCode = "500", description = "Database error")
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/calculate-currency")
    public ResponseEntity<?> calculateCurrencyConversion(
            @Valid @RequestBody CurrencyConversionRequest request
    ) {
        try {
            CurrencyConversionResponse response = journalVoucherService.calculateCurrencyConversion(request);
            return success("Currency conversion calculated successfully", response);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to calculate currency conversion: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for Journal Voucher",
            description = "Generate PDF report for a specific Journal Voucher transaction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Journal Voucher not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "92170")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = journalVoucherService.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=journal-voucher-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Journal Voucher: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }
}