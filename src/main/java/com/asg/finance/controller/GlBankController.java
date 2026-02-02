package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;

import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.GlBankDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.entity.GlBankEntity;
import com.asg.finance.service.GlBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/bank-master")
public class GlBankController {

    @Autowired
    GlBankService bankService;
    
    private final LoggingService loggingService;

    @Operation(
            summary = "Update Bank Master",
            description = "Updates an existing bank master record with bank details, commission settings, and cheque configurations. Supports CRUD operations on related cheque and commission details through actionType field.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Bank master updated successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GlBankDto.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data or validation errors"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Bank master not found"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Bank master data with optional cheque and commission details",
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GlBankDto.class),
                    examples = {
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Complete Bank Update",
                                    summary = "Update bank with cheque and commission details",
                                    value = """
                                            {
                                                "groupPoid": 1,
                                                "bankCode": "HDFC005",
                                                "bankDescription": "HDFC Bank Main Branch",
                                                "bankDescription2": "Mumbai Central",
                                                "glPoid": 19761,
                                                "bankAccountNo": "1212",
                                                "iban": "IN12345678901234567890",
                                                "swiftCode": "HDFCINBBXXX",
                                                "bankAddress": "123, MG Road, Mumbai, Maharashtra, India",
                                                "currencyCode": "INR",
                                                "buyingRate": 83.25,
                                                "sellingRate": 83.5,
                                                "periodStart": "2025-01-01",
                                                "periodEnd": "2025-12-31",
                                                "cardCommision": 2.5,
                                                "chequePrintingYn": "Y",
                                                "seqno": 1,
                                                "remarks": "Preferred banking partner. Integrates with ERP.",
                                                "active": "Y",
                                                "deleted": "N",
                                                "odLimit": 500000.00,
                                                "companyPoid": 2,
                                                "oldGlAccNo": "GL789456123",
                                                "bankPrefix": "HDFC",
                                                "onlineFileTt": "Y",
                                                "bankStatementDate": "2025-09-01",
                                                "currencyRate": 1.0,
                                                "vatTinNumber": "27ABCDE1234F1Z5",
                                                "accountType": "Current Account",
                                                "correspondantSwiftCode": "CORRINBBXXX",
                                                "correspondantBank": "Correspondent Bank, NY Branch",
                                                "ediBankAccountNo": "EDI1234567890987654321",
                                                "chequeDetails": [
                                                    {
                                                        "detRowId": "3",
                                                        "chqSignType": "Authorized Sign",
                                                        "totalCheques": 150,
                                                        "startChqNo": "0001004",
                                                        "endChqNo": "0001150",
                                                        "reorderLevel": 50,
                                                        "currentChqNo": "0001100",
                                                        "defaultPrinterAddr": "192.168.1.50",
                                                        "defaultPrinterTray": "Tray 1",
                                                        "stockFinishedYn": "N",
                                                        "remarks": "Cheque stock sufficient for next quarter.",
                                                        "lastChqNo": "0001150",
                                                        "actionType": "isUpdated"
                                                    }
                                                ],
                                                "commissionDetails": [
                                                    {
                                                        "detRowId": "2",
                                                        "periodFrom": "2025-01-01",
                                                        "periodTo": "2025-12-31",
                                                        "commissionGlPoid": 19761,
                                                        "commissionPercent": 2.75,
                                                        "taxPoid": 281,
                                                        "taxPercentage": 18.00,
                                                        "remarks": "Commission applicable for all credit card transactions during the fiscal year 2025.",
                                                        "cardType": "Visa, MasterCard, Amex",
                                                        "actionType": "isUpdated"
                                                    }
                                                ]
                                            }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{bankPoid}")
    public ResponseEntity<?> updateBankMaster(
            @Parameter(
                    description = "Unique identifier of the bank master record to update",
                    required = true,
                    example = "807"
            )
            @PathVariable Long bankPoid,
            @RequestBody @Valid GlBankDto glBankDto
    ) {
        GlBankDto updateGlBank = bankService.updateGlBank(bankPoid, glBankDto);
        return success("Bank Master updated successfully", updateGlBank);
    }


    @Operation(
            summary = "Create Bank Master",
            description = "Create bank master record with bank details, commission settings, and cheque configurations. Supports CRUD operations on related cheque and commission details through actionType field.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Bank master created successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GlBankDto.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data or validation errors"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Bank master not found"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Bank master data with optional cheque and commission details",
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GlBankDto.class),
                    examples = {
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Create Bank Master",
                                    summary = "Create bank with cheque and commission details",
                                    value = """
                                            {
                                                 "groupPoid": 1,
                                                 "bankCode": "HDFC005",
                                                 "bankDescription": "HDFC Bank Main Branch",
                                                 "bankDescription2": "Mumbai Central",
                                                 "glPoid": 19761,
                                                 "bankAccountNo": "1212",
                                                 "iban": "IN12345678901234567890",
                                                 "swiftCode": "HDFCINBBXXX",
                                                 "bankAddress": "123, MG Road, Mumbai, Maharashtra, India",
                                                 "currencyCode": "INR",
                                                 "buyingRate": 83.25,
                                                 "sellingRate": 83.5,
                                                 "periodStart": "2025-01-01",
                                                 "periodEnd": "2025-12-31",
                                                 "cardCommision": 2.5,
                                                 "chequePrintingYn": "Y",
                                                 "seqno": 1,
                                                 "remarks": "Preferred banking partner. Integrates with ERP.",
                                                 "active": "Y",
                                                 "deleted": "N",
                                                 "odLimit": 500000.00,
                                                 "companyPoid": 3,
                                                 "oldGlAccNo": "GL789456123",
                                                 "bankPrefix": "HDFC",
                                                 "onlineFileTt": "Y",
                                                 "bankStatementDate": "2025-09-01",
                                                 "currencyRate": 1.0,
                                                 "vatTinNumber": "27ABCDE1234F1Z5",
                                                 "accountType": "Current Account",
                                                 "correspondantSwiftCode": "CORRINBBXXX",
                                                 "correspondantBank": "Correspondent Bank, NY Branch",
                                                 "ediBankAccountNo": "EDI1234567890987654321",
                                                 "chequeDetails": [
                                                     {
                                                         "detRowId": "3",
                                                         "chqSignType": "Authorized Sign",
                                                         "totalCheques": 150,
                                                         "startChqNo": "0001004",
                                                         "endChqNo": "0001150",
                                                         "reorderLevel": 50,
                                                         "currentChqNo": "0001100",
                                                         "defaultPrinterAddr": "192.168.1.50",
                                                         "defaultPrinterTray": "Tray 1",
                                                         "stockFinishedYn": "N",
                                                         "remarks": "Cheque stock sufficient for next quarter.",
                                                         "lastChqNo": "0001150"
                                                     }
                                                 ],
                                                 "commissionDetails": [
                                                     {
                                                         "detRowId": "2",
                                                         "periodFrom": "2025-01-01",
                                                         "periodTo": "2025-12-31",
                                                         "commissionGlPoid": 19761,
                                                         "commissionPercent": 2.75,
                                                         "taxPoid": 281,
                                                         "taxPercentage": 18.00,
                                                         "remarks": "Commission applicable for all credit card transactions during the fiscal year 2025.",
                                                         "cardType": "Visa, MasterCard, Amex"
                                                     }
                                                 ]
                                             }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/createEntry")
    public ResponseEntity<?> createNewEntry(
            @Valid @RequestBody GlBankDto bankDto) {
        GlBankDto data = bankService.createEntry(bankDto);
        Map<String, String> response = new HashMap<>();
        response.put("bankPoid", data.getBankPoid().toString());
        return success("Bank Master Created successfully", response);
    }


    @Operation(
            summary = "Get Bank Master Details",
            description = "Retrieves detailed information about a specific bank master record including cheque and commission details",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved bank master details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = GlBankDto.class),
                                    examples = @ExampleObject(
                                            name = "BankMasterResponse",
                                            value = """
                                                    {
                                                      "bankPoid": 1,
                                                      "groupPoid": 1,
                                                      "bankCode": "HDFC001",
                                                      "bankDescription": "HDFC Bank Main Branch",
                                                      "bankDescription2": "Mumbai Central",
                                                      "glPoid": 3001,
                                                      "bankAccountNo": "1234567890123456",
                                                      "iban": "IN12345678901234567890",
                                                      "swiftCode": "HDFCINBBXXX",
                                                      "bankAddress": "123, MG Road, Mumbai, Maharashtra, India",
                                                      "currencyCode": "INR",
                                                      "buyingRate": 83.25,
                                                      "sellingRate": 83.5,
                                                      "periodStart": "2025-01-01",
                                                      "periodEnd": "2025-12-31",
                                                      "cardCommision": 2.5,
                                                      "chequePrintingYn": "Y",
                                                      "seqno": 1,
                                                      "remarks": "Primary bank for corporate transactions",
                                                      "active": "Y",
                                                      "deleted": "N",
                                                      "chequeDetails": [
                                                        {
                                                          "detRowId": 1,
                                                          "chqSignType": "SINGLE",
                                                          "totalCheques": 100,
                                                          "startChqNo": "0000001",
                                                          "endChqNo": "0000100",
                                                          "reorderLevel": 20,
                                                          "currentChqNo": "0000050",
                                                          "defaultPrinterAddr": "PRN001",
                                                          "defaultPrinterTray": "TRAY1",
                                                          "stockFinishedYn": "N",
                                                          "remarks": "Cheque book issued on 2025-01-01",
                                                          "lastChqNo": "0000050"
                                                        }
                                                      ],
                                                      "commissionDetails": [
                                                        {
                                                          "detRowId": 1,
                                                          "periodFrom": "2025-01-01",
                                                          "periodTo": "2025-12-31",
                                                          "commissionPercent": 1.5,
                                                          "taxPoid": 1,
                                                          "taxPercentage": 18.0,
                                                          "remarks": "Standard commission for all transactions",
                                                          "cardType": "VISA,MASTERCARD"
                                                        }
                                                      ]
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request parameters",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "ErrorResponse",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "message": "Invalid bank ID format"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Bank master record not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "NotFoundResponse",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "message": "Bank not found with ID: 999"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "ErrorResponse",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "message": "An unexpected error occurred while processing your request"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{bankPoid}")
    public ResponseEntity<?> getBankDetails(
            @Parameter(
                    description = "Unique identifier of the bank master record",
                    required = true,
                    example = "47"
            )
            @PathVariable Long bankPoid
    ) {
        GlBankDto data = bankService.fetchGlBank(bankPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), bankPoid.toString());
        return success("Bank Master Fetched successfully", data);
    }


    @Operation(
            summary = "Soft delete a bank master record",
            description = "Performs a soft delete of a bank master record by setting its active status to 'N' and deleted status to 'Y'." +
                    " Also deletes all associated cheque and commission details.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Bank master record deleted successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Bank master record not found with the given ID",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input parameters",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{bankPoid}")
    public ResponseEntity<?> softDeleteBankMaster(
            @Parameter(
                    description = "Unique identifier of the bank master record to be soft deleted",
                    required = true,
                    example = "181"
            )
            @PathVariable Long bankPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {

        bankService.deleteBankMaster(bankPoid, deleteReasonDto);
        return success("Bank Master soft deleted and related details removed");
    }


    @Operation(
            summary = "Get paginated list of bank records with filtering and sorting",
            description = "Retrieves a paginated list of bank records with support for filtering, sorting, and searching.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved bank records",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "BankRecordsResponse",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "Bank records fetched successfully",
                                                      "data": {
                                                        "content": [
                                                          {
                                                            "bankPoid": 1,
                                                            "bankCode": "BANK001",
                                                            "bankDescription": "Main Branch",
                                                            "active": "Y",
                                                            "deleted": "N"
                                                          }
                                                        ],
                                                        "totalElements": 1,
                                                        "totalPages": 1,
                                                        "size": 10,
                                                        "number": 0
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request parameters",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "ErrorResponse",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "message": "Invalid request parameters",
                                                      "errors": [
                                                        {
                                                          "field": "bankCode",
                                                          "message": "must not be empty"
                                                        }
                                                      ]
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Invalid or missing authentication",
                            content = @Content
                    )
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listOfRecordsWithGenericSearch(
            @ParameterObject
            @Parameter(
                    description = "Pagination and sorting parameters",
                    example = "{\"page\": 0, \"size\": 10, \"sort\": \"bankCode,asc\"}"
            )
            Pageable pageable,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Filter criteria for bank records",
                    content = @Content(
                            schema = @Schema(implementation = FilterRequestDto.class),
                            examples = @ExampleObject(
                                    name = "FilterExample",
                                    value = """
                                            {
                                                 "operator":"AND",
                                                 "isDeleted":"Y",
                                                 "filters":[
                                                     {
                                                         "searchField":"BANK_DESCRIPTION",
                                                         "searchValue":"AMEX -374490013331005- BALA"
                                                     },
                                                     {
                                                         "searchField":"BANK_CODE",
                                                         "searchValue":"AMEX000002"
                                                     }
                                                 ]
                                            }
                                            """
                            )
                    )
            )
            @RequestBody(required = false) FilterRequestDto filters
    ) {
        Map<String, Object> bankRecords = bankService.listOfRecordsAndGenericSearch(UserContext.getDocumentId(), filters, pageable);

            return success("Bank records fetched successfully", bankRecords);
    }
}