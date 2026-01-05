package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;

import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.GlChequeCashConvertHdrDto;
import com.asg.finance.dto.GlChequeConversionLoadResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.service.GlChequeCashConvertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/gl-cheque-cash-conversion")
public class GlChequeCashConvertController {

    private final GlChequeCashConvertService service;

    @Operation(
            summary = "Fetch GL Cheque Cash Convert Record by Transaction POID",
            description = "This API retrieves a specific GL Cheque Cash Convert Header record based on the provided Transaction POID. "
                    + "It also requires the document ID and action requested as input parameters for context validation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "GL Cheque Cash Convert Record fetched successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GlChequeCashConvertHdrDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or missing parameters"),
            @ApiResponse(responseCode = "404", description = "Record not found for the given Transaction POID"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getGlChequeCashConvert(
            @Parameter(
                    description = "Unique Transaction POID of the GL Cheque Cash Convert Header record",
                    required = true,
                    example = "201"
            )
            @PathVariable Long transactionPoid) {
        GlChequeCashConvertHdrDto result = service.getGlChequeCashConvert(transactionPoid);
        return success("GL Cheque Cash Convert Records fetched successfully", result);
    }


    @Operation(
            summary = "Soft delete a GL Cheque Cash Convert record",
            description = """
                    This API performs a soft delete operation for a specific GL Cheque Cash Convert record 
                    based on the provided Transaction POID. 
                    The record is not permanently removed but marked as deleted in the system.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "GL Cheque Cash Convert record deleted successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input parameters",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Record not found for the given Transaction POID",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> deleteGlChequeCashConvert(
            @Parameter(description = "ID of the  to be deleted", required = true, example = "301")
            @PathVariable Long transactionPoid) {

        service.softDeleteByTransactionPoid(transactionPoid);
        return success("GL Cheque Cash Convert Record deleted successfully");
    }

    @Operation(
            summary = "Get paginated list of GL Cheque Cash Convert records with filtering",
            description = "Retrieves a paginated list of GL Cheque Cash Convert records with support for dynamic filtering and search parameters."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved GL Cheque Cash Convert records",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input parameters",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listOfRecordsWithGenericSearch(
            @ParameterObject
            @Parameter(
                    description = "Pagination and sorting configuration",
                    example = "page=0&size=10&sort=transactionPoid,desc"
            )
            Pageable pageable,

            @RequestBody(required = false)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Filter criteria for GL Cheque Cash Convert records",
                    required = false,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Search by Document Reference",
                                    value = """
                                            {
                                                 "operator": "AND",
                                                     "isDeleted": "N",
                                                     "filters": [
                                                       {
                                                         "searchField": "TRANSACTION_POID",
                                                         "searchValue": "301"
                                                       },
                                                       {
                                                         "searchField": "DOC_REF",
                                                         "searchValue": "ASG7"
                                                       }
                                                     ]
                                            }"""
                            )
                    )
            )
            FilterRequestDto filters,
            @Parameter(description = "Start date for filtering")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "End date for filtering")
            @RequestParam(required = false) String endDate
    ) {
        java.time.LocalDate startDateValue = startDate != null ? java.time.LocalDate.parse(startDate) : null;
        java.time.LocalDate endDateValue = endDate != null ? java.time.LocalDate.parse(endDate) : null;
        Map<String, Object> result = service.listOfRecordsAndGenericSearch(UserContext.getDocumentId(), filters, startDateValue, endDateValue, pageable);
        return success("GL Cheque Cash Convert list fetched successfully", result);
    }


    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create GL Cheque Cash Conversion",
            description = "Creates a new GL Cheque Cash Conversion record for cheque-to-cash transactions.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "GL Cheque Cash Conversion request payload",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = GlChequeCashConvertHdrDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Sample Request",
                                            value = """
                                                    {
                                                     "transactionDate": "2025-11-14T10:00:00",
                                                                                         "groupPoid": 1,
                                                                                         "companyPoid": 1,
                                                                                         "docRef": "GLC-0002",
                                                                                         "type": "CHQ2CASH",
                                                                                         "postingNarration": "Split cheques to cash",
                                                                                         "cash": 3000,
                                                                                         "remarks": "Multiple cheques",
                                                                                         "createdBy": "api.user",
                                                                                         "chqAcNo": "AX-009988",
                                                                                         "chqCardNo": "NA",
                                                                                         "roundingAmt": 0,
                                                                                         "inDtls": [
                                                                                           {
                                                                                             "bankPoid": 501,
                                                                                             "chqAcName": "Axis Bank - Main",
                                                                                             "chqAcNo": "AX-009988",
                                                                                             "chqCardNo": "111122223333",
                                                                                             "chqDate": "2025-11-13",
                                                                                             "amount": 1000,
                                                                                             "remarks": "Cheque A",
                                                                                             "voucherType": "CHQ",
                                                                                             "chequeCompanyPoid": 301,
                                                                                             "paymentMainPoid": 7002001,
                                                                                             "lineType": "IN",
                                                                                             "pymtType": "CHEQUE"
                                                                                           }
                                                    
                                                                                         ],
                                                                                         "outDtls": [
                                                                                           {
                                                                                             "paymentMainPoid": 772300,
                                                                                             "amount": 3000,
                                                                                             "remarks": "Cash issued",
                                                                                             "bankPoid": 50,
                                                                                             "chqAcName": "Cash Account",
                                                                                             "chqAcNo": "CASH-001",
                                                                                             "chqCardNo": "NA",
                                                                                             "chqDate": "2025-11-14",
                                                                                             "selected": "Y",
                                                                                             "voucherType": "CASH",
                                                                                             "lineType": "OUT"
                                                                                           }
                                                                                         ]
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @ApiResponse(
            responseCode = "200",
            description = "GL Cheque Cash Convert created successfully",
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "status": "success",
                                              "message": "GL Cheque Cash Convert created successfully",
                                              "data": {
                                                "transactionPoid": 1001,
                                                "groupPoid": 1,
                                                "companyPoid": 1,
                                                "docRef": "ASG78",
                                                "postingNarration": "Cheque conversion for cash",
                                                "cash": 2000,
                                                "createdBy": "MOHAMMED",
                                                "inDtls": [],
                                                "outDtls": []
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    public ResponseEntity<?> createGlChequeCashConvert(
            @Valid @RequestBody GlChequeCashConvertHdrDto glChequeCashConvertHdrDto) {
        GlChequeCashConvertHdrDto result = service.createGlChequeCashConvert(glChequeCashConvertHdrDto);
        return success("GL Cheque Cash Convert created successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    @Operation(
            summary = "Update GL Cheque Cash Conversion",
            description = "Updates an existing GL Cheque Cash Conversion record identified by transactionPoid.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "GL Cheque Cash Conversion update payload",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = GlChequeCashConvertHdrDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Sample Request",
                                            value = """
                                                                                      {
                                                                                         "transactionDate": "2025-11-14T10:00:00",
                                                                                         "postingNarration": "Revised posting narration",
                                                                                         "cash": 3500,
                                                                                         "remarks": "Edited: adjust totals",
                                                                                         "chqAcNo": "AX-009988",
                                                                                         "chqCardNo": "NA",
                                                                                         "roundingAmt": 0,
                                                                                         "lastModifiedBy": "api.user",
                                                                                         "deleted": "N",
                                                    
                                                                                         "inDtls": [
                                                                                           {
                                                    
                                                                                             "bankPoid": 501,
                                                                                             "chqAcName": "Axis Bank - Main",
                                                                                             "chqAcNo": "AX-009988",
                                                                                             "chqCardNo": "111122223333",
                                                                                             "chqDate": "2025-11-13",
                                                                                             "amount": 1500,
                                                                                             "remarks": "Cheque A (edited)",
                                                                                             "voucherType": "CHQ",
                                                                                             "chequeCompanyPoid": 301,
                                                                                             "paymentMainPoid": 7002001,
                                                                                             "lineType": "IN",
                                                                                             "pymtType": "CHEQUE"
                                                                                           }
                                                                                         ],
                                                    
                                                                                         "outDtls": [
                                                                                           {
                                                    
                                                                                             "paymentMainPoid": 772300,
                                                                                             "amount": 3500,
                                                                                             "remarks": "Cash issued (edited)",
                                                                                             "bankPoid": 50,
                                                                                             "chqAcName": "Cash Account",
                                                                                             "chqAcNo": "CASH-001",
                                                                                             "chqCardNo": "NA",
                                                                                             "chqDate": "2025-11-14",
                                                                                             "selected": "Y",
                                                                                             "voucherType": "CASH",
                                                                                             "lineType": "OUT"
                                                                                           }
                                                                                         ]
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "GL Cheque Cash Conversion updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Success Response",
                                            value = """
                                                    {
                                                      "status": "success",
                                                      "message": "GL Cheque Cash Conversion updated successfully",
                                                      "data": {
                                                        "transactionPoid": 1001,
                                                        "groupPoid": 1,
                                                        "companyPoid": 1,
                                                        "docRef": "ASG7",
                                                        "postingNarration": "Cheque conversion",
                                                        "cash": 2000,
                                                        "createdBy": "MOHAM",
                                                        "inDtls": [],
                                                        "outDtls": []
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Record not found for the provided transactionPoid"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or missing required fields"
            )
    })

    public ResponseEntity<?> updateGlChequeCashConvert(
            @Parameter(
                    description = "Transaction POID of the record to be updated",
                    required = true
            )
            @PathVariable Long transactionPoid,

            @Valid @RequestBody GlChequeCashConvertHdrDto glChequeCashConvertHdrDto) {

        GlChequeCashConvertHdrDto result = service.updateGlChequeCashConvert(transactionPoid, glChequeCashConvertHdrDto);
        return success("GLCheque Cash Conversion updated successfully", result);
    }


    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/load")
    @Operation(
            summary = "Load Source Records for GL Cheque and Cash Conversion"

    )
    public ResponseEntity<?> loadGlChequeConversion(
            @RequestParam(name = "chequeNum", required = false) @Parameter(name = "chequeNum", example = "716380") String chequeNumber,
            @RequestParam(name = "chqAcNo", required = false) @Parameter(name = "chqAcNo", example = "0100000007343") String chequeAccNumber,
            @RequestParam(required = true) @Parameter(example = "CHEQUE_TO_CHEQUE") String type
    ) {
        java.util.List<GlChequeConversionLoadResponseDto> data = service.loadGlChequeConversion(chequeNumber, chequeAccNumber, type);
        return success("GL Cheque Cash Conversion load fetched successfully", data);
    }

}
