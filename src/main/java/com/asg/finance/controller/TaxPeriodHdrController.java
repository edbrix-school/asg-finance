package com.asg.finance.controller;


import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.*;
import com.asg.finance.service.TaxPeriodHdrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;


@RestController
@RequestMapping("/v1/tax-period")
public class TaxPeriodHdrController {
    private final TaxPeriodHdrService taxPeriodHdrService;

    public TaxPeriodHdrController(TaxPeriodHdrService taxPeriodHdrService) {
        this.taxPeriodHdrService = taxPeriodHdrService;
    }

    @Operation(
            summary = "Create a new Tax Period",
            description = "Creates a new Tax Period with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created the Tax Period",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxPeriodHdrResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input, object invalid",
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
    @PostMapping
    public ResponseEntity<?> createTaxPeriodHdr(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Tax Period object that needs to be created",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaxPeriodHdrRequestDto.class)
                    )
            )
            @Parameter(description = "Tax Period details to be created", required = true)
            @Valid @RequestBody TaxPeriodHdrRequestDto request
    ) {
        try {
            TaxPeriodHdrResponseDto response = taxPeriodHdrService.createTaxPeriodHdr(request);
            return success("Tax Period created successfully", response);
        } catch (Exception ex) {
            return internalServerError("Failed to create Tax Period: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Update an existing Tax Period",
            description = "Updates the Tax Period with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated the Tax Period",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxPeriodHdrResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input, object invalid",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Tax Period not found",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Duplicate Tax Period exists",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateTaxPeriodHdr(
            @Parameter(description = "Tax Period transactionPoid to be updated", required = true)
            @PathVariable Long transactionPoid,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated Tax Period details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaxPeriodHdrRequestDto.class)
                    )
            )
            @Valid @RequestBody TaxPeriodHdrRequestDto requestDTO
    ) {
        try {
            TaxPeriodHdrResponseDto response = taxPeriodHdrService.updateTaxPeriodHdr(transactionPoid, requestDTO);
            return success("Tax Period updated successfully", response);
        } catch (Exception ex) {
            return internalServerError("Failed to update Tax Period" + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get Tax Period by transactionPoid",
            description = "Retrieves Tax Period details based on the provided Tax Period transactionPoid",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the Tax Period details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxPeriodHdrResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input parameters",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Tax Period not found",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getTaxPeriodHdrById(
            @Parameter(description = "transactionPoid reference identifier", required = true)
            @PathVariable Long transactionPoid) {

        TaxPeriodHdrResponseDto responseDto = taxPeriodHdrService.getTaxPeriodHdrById(transactionPoid);
        return success("Tax Period fetched successfully", responseDto);
    }

    @Operation(
            summary = "Soft delete a Tax Period ",
            description = "Marks a Tax Period  as deleted without permanently removing its data",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully soft deleted the Tax Period ",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxPeriodHdrRequestDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Tax Period  not found",
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
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> softDeleteTaxPeriodHdr(
            @Parameter(description = "transactionPoid reference identifier", required = true)
            @PathVariable Long transactionPoid) {

        taxPeriodHdrService.softDeleteTaxPeriodHdr(transactionPoid);
        return success("Tax Period has been soft deleted successfully");
    }


    @Operation(
            summary = "Get Tax Period Charges",
            description = "Retrieves paginated charges for a specific tax period",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved charges",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxPeriodChargeDtlRequestDto.class)
                            )
                    ),

                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input parameters",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Transaction Poid not found",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )

    @GetMapping("/{transactionPoid}/charges")
    public ResponseEntity<?> getTaxPeriodCharges(
            @Parameter(description = "Transaction Poid", required = true)
            @PathVariable Long transactionPoid,
            @Parameter(description = "Page number (default: 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 25)", example = "25")
            @RequestParam(defaultValue = "25") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TaxPeriodChargeDtlResponseDto> charges = taxPeriodHdrService.getTaxPeriodCharges(transactionPoid, pageable);
        return success("Transaction Poid charges fetched successfully", charges);
    }

    @Operation(
            summary = "Get Tax Period Stocks",
            description = "Retrieves paginated stocks for a specific tax period",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved stocks",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaxPeriodStockDtlRequestDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input parameters",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - Authentication required",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Transaction Poid not found",
                            content = @Content(mediaType = "application/json")
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )


    @GetMapping("/{transactionPoid}/stocks")
    public ResponseEntity<?> getTaxPeriodStocks(
            @Parameter(description = "Transaction Poid", required = true)
            @PathVariable Long transactionPoid,
            @Parameter(description = "Page number (default: 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 25)", example = "25")
            @RequestParam(defaultValue = "25") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TaxPeriodStockDtlResponseDto> stocks = taxPeriodHdrService.getTaxPeriodStocks(transactionPoid, pageable);
        return success("Transaction Poid stocks fetched successfully", stocks);
    }

    @Operation(
            summary = "Copy Tax Period",
            description = "Creates a copy of an existing tax period with updated period dates",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully copied the Tax Period",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Original Tax Period not found",
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
    @PostMapping("/{transactionPoid}/copy")
    public ResponseEntity<?> copyTaxPeriod(
            @Parameter(description = "Original Tax Period transactionPoid to copy", required = true)
            @PathVariable Long transactionPoid) {

        try {
            String result = taxPeriodHdrService.copyTaxPeriod(transactionPoid, UserContext.getCompanyPoid(), UserContext.getUserPoid());
            return success(result);
        } catch (Exception ex) {
            return internalServerError("Failed to copy Tax Period: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "List Tax Period with Search and Sort (DocId: 400-011)",
            description = "Provide search filters. Valid `searchField` values: GLOBALSEARCH or (TRANSACTION_POID, DOC_REF, CREATED_BY). Sorting default on locationPoid, desc." +
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
                      2. Any combination of specific fields (TRANSACTION_POID, DOC_REF, CREATED_BY).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                         not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                      4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                      5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      No operator need to send in this case.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "Warehouse" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      • { "searchField": "TRANSACTION_POID", "searchValue": "LOC01" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "TRANSACTION_POID", "searchValue": "LOC01|LOC02" }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "TRANSACTION_POID", "searchValue": "LOC01" },
                      • { "searchField": "DOC_REF", "searchValue": "Head Office|Branch" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=TRANSACTION_POID,ASC
                      • sort=DOC_REF,DESC
                    
                    - ### Authorization Parameters (handled by interceptor)
                        - **documentId:** Unique identifier for the document (`400-011`)
                        - **actionRequested:** Action being performed (`VIEW`)
                    """,
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Asset Location Filters",
                                    value = """
                                            {
                                              "operator": "AND",
                                              "isDeleted": "N",
                                              "filters": [
                                                 { "searchField": "GLOBALSEARCH", "searchValue": "Warehouse" },
                                                 { "searchField": "TRANSACTION_POID", "searchValue": "LOC01|LOC02" },
                                                 { "searchField": "DOC_REF", "searchValue": "Main Office" },
                                                 { "searchField": "ACTIVE", "searchValue": "Y" },
                                                 { "searchField": "CREATED_BY", "searchValue": "Admin" }
                                              ]
                                            }
                                            """
                            )
                    }
            )
    )


    @PostMapping("/list")
    public ResponseEntity<?> listTaxPeriod(@ParameterObject Pageable pageable,
                                           @RequestBody(required = false) FilterRequestDto filters,
                                           @RequestParam(required = false) LocalDate periodFrom,
                                           @RequestParam(required = false) LocalDate periodTo) {
        try {
            Map<String, Object> data = taxPeriodHdrService.listTaxPeriod(UserContext.getDocumentId(), filters, pageable, periodFrom, periodTo);
            return success("Tax Period successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch Tax Period list: " + ex.getMessage());
        }
    }
}

