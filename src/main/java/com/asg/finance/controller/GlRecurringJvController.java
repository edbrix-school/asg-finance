package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.dto.CreateScheduleRequest;
import com.asg.finance.dto.RecurringJvRequest;
import com.asg.finance.dto.RecurringJvResponse;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.service.GlRecurringJvService;
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

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/recurring-jvs")
public class GlRecurringJvController {

    private final GlRecurringJvService recurringJvService;
    private final LoggingService loggingService;

    @Operation(summary = "Create Recurring JV")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = {
                            @ExampleObject(
                                    name = "EMPLOYEE Type Recurring JV",
                                    value = """
                                            {
                                              "transactionDate": "2025-12-16",
                                              "refType": "EMPLOYEE",
                                              "employeePoid": 1001,
                                              "assetPoid": null,
                                              "companyPoid": 1,
                                              "currencyCode": "BHD",
                                              "currencyRate": 1.0,
                                              "amount": 5000.00,
                                              "bhdAmount": 5000.00,
                                              "postingNarration": "Monthly salary processing",
                                              "multiCompany": false,
                                              "remarks": "Employee salary recurring entry",
                                              "confidentialRemarks": "HR confidential notes",
                                              "details": [
                                                {
                                                  "actionType": "ISCREATED",
                                                  "type": "DR",
                                                  "companyPoid": 1,
                                                  "glPoid": 5001,
                                                  "drAmt": 5000.00,
                                                  "crAmt": 0.00,
                                                  "remarks": "Salary expense",
                                                  "costCenterBreakup": [
                                                    {
                                                      "costGroup": "DEPT",
                                                      "costPoid": "HR001",
                                                      "amount": 5000.00,
                                                      "actionType": "isCreated"
                                                    }
                                                  ],
                                                  "billWiseBreakup": []
                                                },
                                                {
                                                  "actionType": "ISCREATED",
                                                  "type": "CR",
                                                  "companyPoid": 1,
                                                  "glPoid": 2001,
                                                  "drAmt": 0.00,
                                                  "crAmt": 5000.00,
                                                  "remarks": "Salary payable"
                                                }
                                              ]
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "ASSET Type Recurring JV",
                                    value = """
                                            {
                                              "transactionDate": "2025-12-16",
                                              "refType": "ASSET",
                                              "employeePoid": null,
                                              "assetPoid": 2001,
                                              "companyPoid": 1,
                                              "currencyCode": "BHD",
                                              "currencyRate": 1.0,
                                              "amount": 2000.00,
                                              "bhdAmount": 2000.00,
                                              "postingNarration": "Monthly depreciation",
                                              "multiCompany": false,
                                              "remarks": "Asset depreciation recurring entry",
                                              "confidentialRemarks": "Asset management notes",
                                              "details": [
                                                {
                                                  "actionType": "ISCREATED",
                                                  "type": "DR",
                                                  "companyPoid": 1,
                                                  "glPoid": 6001,
                                                  "drAmt": 2000.00,
                                                  "crAmt": 0.00,
                                                  "remarks": "Depreciation expense"
                                                },
                                                {
                                                  "actionType": "ISCREATED",
                                                  "type": "CR",
                                                  "companyPoid": 1,
                                                  "glPoid": 1501,
                                                  "drAmt": 0.00,
                                                  "crAmt": 2000.00,
                                                  "remarks": "Accumulated depreciation"
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
    public ResponseEntity<?> createRecurringJv(
            @Valid @RequestBody RecurringJvRequest request,
            @Parameter(description = "Document identifier", required = true, example = "800-321")
            @RequestParam String documentId,
            @Parameter(description = "Action requested", required = true, example = "create")
            @RequestParam String actionRequested) {
        return success("Recurring JV created successfully", recurringJvService.createRecurringJv(request, documentId));
    }

    @Operation(summary = "List Recurring JVs")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            name = "Recurring JV List",
                            value = """
                                    {
                                       "operator": "OR",
                                       "isDeleted": "N",
                                       "filters": [
                                         {
                                           "searchField": "DOC_REF",
                                           "searchValue": "RJV-2025-001"
                                         },
                                         {
                                           "searchField": "REF_TYPE",
                                           "searchValue": "EMPLOYEE"
                                         }
                                       ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listRecurringJvs(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "Document identifier", required = true, example = "800-321")
            @RequestParam String documentId,
            @Parameter(description = "Action requested", required = true, example = "view")
            @RequestParam String actionRequested) {
        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            return badRequest("Both startDate and endDate should be specified or both dates should be empty.");
        }
        Map<String, Object> response = recurringJvService.listRecurringJvs(documentId, filters, startDate, endDate, pageable);
            return success("Recurring JVs list retrieved successfully", response);
    }

    @Operation(
            summary = "Get recurring JV by ID",
            description = "Retrieves recurring JV details including header, detail lines, and schedule details",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved recurring JV",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = RecurringJvResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Recurring JV not found",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getRecurringJvById(
            @Parameter(description = "Transaction POID of the recurring JV", required = true)
            @PathVariable Long transactionPoid,
            @Parameter(description = "Document identifier", required = true, example = "400-102")
            @RequestParam String documentId,
            @Parameter(description = "Action requested", required = true, example = "view")
            @RequestParam String actionRequested) {

        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        return success("Recurring JV retrieved successfully", recurringJvService.getRecurringJvById(transactionPoid));
    }

    @Operation(summary = "Update Recurring JV")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            name = "Update Recurring JV",
                            value = """
                                    {
                                      "transactionDate": "2025-12-16",
                                      "refType": "EMPLOYEE",
                                      "employeePoid": 1001,
                                      "assetPoid": null,
                                      "companyPoid": 1,
                                      "amount": 5500.00,
                                      "bhdAmount": 5500.00,
                                      "postingNarration": "Updated monthly salary processing",
                                      "multiCompany": false,
                                      "remarks": "Updated salary amount",
                                      "confidentialRemarks": "Updated HR notes",
                                      "details": [
                                        {
                                          "lineId": 1,
                                          "actionType": "ISUPDATED",
                                          "type": "DR",
                                          "companyPoid": 1,
                                          "glPoid": 5001,
                                          "drAmt": 5500.00,
                                          "crAmt": 0.00,
                                          "remarks": "Updated salary expense"
                                        },
                                        {
                                          "lineId": 2,
                                          "actionType": "ISUPDATED",
                                          "type": "CR",
                                          "companyPoid": 1,
                                          "glPoid": 2001,
                                          "drAmt": 0.00,
                                          "crAmt": 5500.00,
                                          "remarks": "Updated salary payable"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateRecurringJv(
            @PathVariable Long transactionPoid,
            @Valid @RequestBody RecurringJvRequest request,
            @Parameter(description = "Document identifier", required = true, example = "400-102")
            @RequestParam String documentId,
            @Parameter(description = "Action requested", required = true, example = "edit")
            @RequestParam String actionRequested) {
        return success("Recurring JV updated successfully", recurringJvService.updateRecurringJv(transactionPoid, request, documentId));
    }

    @Operation(summary = "Delete Recurring JV")
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> deleteRecurringJv(
            @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        recurringJvService.deleteRecurringJv(transactionPoid, deleteReasonDto);
        return success("Recurring JV deleted successfully", null);
    }

    @Operation(summary = "Create Due Date Schedule")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            name = "Create Schedule",
                            value = """
                                    {
                                      "totalAmount": 60000.00,
                                      "noOfMonths": 12,
                                      "startDate": "2025-01-01",
                                      "details": [
                                        {
                                          "dueDate": "2025-01-31",
                                          "amount": 5000.00
                                        },
                                        {
                                          "dueDate": "2025-02-28",
                                          "amount": 5000.00
                                        },
                                        {
                                          "dueDate": "2025-03-31",
                                          "amount": 5000.00
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/{transactionPoid}/create-schedule")
    public ResponseEntity<?> createSchedule(
            @PathVariable Long transactionPoid,
            @Valid @RequestBody CreateScheduleRequest request,
            @Parameter(description = "Document identifier", required = true, example = "400-102")
            @RequestParam String documentId,
            @Parameter(description = "Action requested", required = true, example = "create")
            @RequestParam String actionRequested) {
        return success("Schedule created successfully", recurringJvService.createSchedule(transactionPoid, request));
    }

    @Operation(summary = "Delete Schedule")
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}/schedule")
    public ResponseEntity<?> deleteSchedule(
            @PathVariable Long transactionPoid,
            @Parameter(description = "Document identifier", required = true, example = "400-102")
            @RequestParam String documentId,
            @Parameter(description = "Action requested", required = true, example = "delete")
            @RequestParam String actionRequested) {
        recurringJvService.deleteSchedule(transactionPoid);
        return success("Schedule deleted successfully", null);
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for Recurring Jv",
            description = "Generate PDF report for a specific Recurring Jv transaction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Recurring Jv not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "21")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = recurringJvService.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=recurring-jv-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Recurring Jv: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

}