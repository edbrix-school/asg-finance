package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;
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
        private final LoggingService loggingService;

        @Operation(summary = "Create Journal Voucher", description = "Creates a new journal voucher with GL details, asset disposal, or asset capitalization")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Journal Voucher created successfully"),
                        @ApiResponse(responseCode = "400", description = "Validation errors"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "500", description = "Database error")
        })
        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                        @ExampleObject(name = "GENERAL Type with GL Details", value = """
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
                                              "costCenterList": [
                                                {
                                                  "costGroup": "DEPT",
                                                  "costPoid": "CC001",
                                                  "amount": 600.00,
                                                  "actionType": "isCreated"
                                                }
                                              ],
                                              "breakupList": [
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
                                        """),
                        @ExampleObject(name = "ASSET_DISPOSAL Type", value = """
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
                                        """),
                        @ExampleObject(name = "ASSET_CAPITALIZATION Type", value = """
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
                                        """)
        }))
        @AllowedAction(UserRolesRightsEnum.CREATE)
        @PostMapping
        public ResponseEntity<?> createJournalVoucher(
                        @Valid @RequestBody JournalVoucherRequest request) {
                JournalVoucherResponse response = journalVoucherService.createJournalVoucher(request,
                                UserContext.getDocumentId());
                return success("Journal Voucher created successfully", response);

        }

        @Operation(summary = "List Journal Vouchers")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Journal Vouchers list retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Validation errors"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "500", description = "Database error") })
        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(name = "Journal Voucher List", value = """
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
                        """)))
        @AllowedAction(UserRolesRightsEnum.VIEW)
        @PostMapping("/list")
        public ResponseEntity<?> listJournalVouchers(
                        @ParameterObject Pageable pageable,
                        @RequestBody(required = false) FilterRequestDto filters,
                        @RequestParam(required = false) LocalDate startDate,
                        @RequestParam(required = false) LocalDate endDate) {
                if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
                        return badRequest(
                                        "Both startDate and endDate should be specified or both dates should be empty.");
                }
                Map<String, Object> response = journalVoucherService.listJournalVouchers(UserContext.getDocumentId(),
                                filters, startDate, endDate, pageable);

                return success("Journal Vouchers list retrieved successfully", response);
        }

        @Operation(summary = "Update Journal Voucher", description = "Updates an existing journal voucher")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Journal Voucher updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Validation errors"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "500", description = "Database error")
        })
        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(name = "Update Journal Voucher", value = """
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
                        """)))
        @AllowedAction(UserRolesRightsEnum.EDIT)
        @PutMapping("/{transactionPoid}")
        public ResponseEntity<?> updateJournalVoucher(
                        @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
                        @Valid @RequestBody JournalVoucherRequest request) {
                JournalVoucherResponse response = journalVoucherService.updateJournalVoucher(transactionPoid, request,
                                UserContext.getDocumentId());
                return success("Journal Voucher updated successfully", response);
        }

        @Operation(summary = "Get Journal Voucher by ID", description = "Retrieve a single journal voucher with all details based on refType")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Journal Voucher retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "500", description = "Database error")
        })
        @AllowedAction(UserRolesRightsEnum.VIEW)
        @GetMapping("/{transactionPoid}")
        public ResponseEntity<?> getJournalVoucherById(
                        @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid) {
                JournalVoucherDetailResponse response = journalVoucherService.getJournalVoucherById(transactionPoid);
                loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(),
                                transactionPoid.toString());
                return success("Journal Voucher retrieved successfully", response);

        }

        @Operation(summary = "Delete Journal Voucher", description = "Soft deletes a journal voucher (must not be posted)")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Journal Voucher deleted successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "500", description = "Database error")
        })
        @AllowedAction(UserRolesRightsEnum.DELETE)
        @DeleteMapping("/{transactionPoid}")
        public ResponseEntity<?> deleteJournalVoucher(
                        @Parameter(description = "Transaction POID", required = true) @PathVariable Long transactionPoid,
                        @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
                journalVoucherService.deleteJournalVoucher(transactionPoid, deleteReasonDto);
                return success("Journal Voucher deleted successfully", null);

        }

        @Operation(summary = "Get Asset Depreciation Details", description = "Fetch asset depreciation details for disposal")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Asset depreciation details retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "404", description = "Asset not found"),
                        @ApiResponse(responseCode = "500", description = "Database error")
        })
        @GetMapping("/asset-depreciation-details/{faPoid}")
        public ResponseEntity<?> getAssetDepreciationDetails(
                        @Parameter(description = "Fixed Asset POID", required = true) @PathVariable Long faPoid) {
                JournalVoucherAssetDetailDto response = journalVoucherService.getAssetDepreciationDetails(faPoid);
                return success("Asset depreciation details retrieved successfully", response);

        }

        @Operation(summary = "Get Asset Capitalization Details", description = "Fetch fixed asset details for capitalization")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Asset capitalization details retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "404", description = "Asset not found"),
                        @ApiResponse(responseCode = "500", description = "Database error")
        })
        @GetMapping("/asset-capitalization-details/{faPoid}")
        public ResponseEntity<?> getAssetCapitalizationDetails(
                        @Parameter(description = "Fixed Asset POID", required = true) @PathVariable Long faPoid) {
                JournalVoucherCapitalizationDto response = journalVoucherService.getAssetCapitalizationDetails(faPoid);
                return success("Asset capitalization details retrieved successfully", response);

        }

        @AllowedAction(UserRolesRightsEnum.PRINT)
        @Operation(summary = "Generate PDF for Journal Voucher", description = "Generate PDF report for a specific Journal Voucher transaction", responses = {
                        @ApiResponse(responseCode = "200", description = "PDF generated successfully", content = @Content(mediaType = "application/pdf")),
                        @ApiResponse(responseCode = "404", description = "Journal Voucher not found"),
                        @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
        })
        @GetMapping("/print/{transactionPoid}")
        public ResponseEntity<?> print(
                        @Parameter(description = "Transaction POID", example = "92170") @PathVariable Long transactionPoid) {
                try {
                        byte[] pdf = journalVoucherService.print(transactionPoid);
                        return ResponseEntity.ok()
                                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                                        "attachment; filename=journal-voucher-" + transactionPoid
                                                                        + ".pdf")
                                        .contentType(MediaType.APPLICATION_PDF)
                                        .body(pdf);
                } catch (Exception e) {
                        log.error("Failed to generate PDF for Journal Voucher: {}", transactionPoid, e);
                        return error("Failed to generate PDF: " + e.getMessage(), 500);
                }
        }
}