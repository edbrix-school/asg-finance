package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.dto.ApPaymentRequestDetailResponse;
import com.asg.finance.dto.ApPaymentRequestHdrRequestDto;
import com.asg.finance.dto.ApPaymentRequestHdrResponseDto;
import com.asg.finance.dto.ApPaymentRequestResponse;
import com.asg.finance.service.ApPaymentRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.internalServerError;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/v1/payment-request")
@RequiredArgsConstructor
public class ApPaymentRequestHdrController {

    private final ApPaymentRequestService service;
    private final LoggingService loggingService;

    /* ================= CREATE ================= */

    @Operation(
            summary = "Create AP Payment Request",
            description = "Creates a new AP Payment Request with header and detail records",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created AP Payment Request",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApPaymentRequestHdrResponseDto.class)
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
            }
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createApPaymentRequest(
            @Valid @RequestBody ApPaymentRequestHdrRequestDto requestDto
    ) {
        ApPaymentRequestHdrResponseDto response = service.create(requestDto);
        return success("AP Payment Request created successfully", response);
    }

    /* ================= UPDATE ================= */

    @Operation(
            summary = "Update AP Payment Request",
            description = "Updates an existing AP Payment Request",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated AP Payment Request",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApPaymentRequestHdrResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "AP Payment Request not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateApPaymentRequest(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Valid @RequestBody ApPaymentRequestHdrRequestDto requestDto
    ) {
        ApPaymentRequestHdrResponseDto response =
                service.update(transactionPoid, requestDto);

        return success("AP Payment Request updated successfully", response);
    }

    /* ================= GET BY ID ================= */

    @Operation(
            summary = "Get AP Payment Request by ID",
            description = "Fetches AP Payment Request details by transaction POID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully fetched AP Payment Request",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApPaymentRequestHdrResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "AP Payment Request not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getApPaymentRequestById(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid
    ) {
        ApPaymentRequestHdrResponseDto response =
                service.findById(transactionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        return success("AP Payment Request fetched successfully", response);
    }

    /* ================= SOFT DELETE ================= */

    @Operation(
            summary = "Soft delete AP Payment Request",
            description = "Marks AP Payment Request as deleted",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully soft deleted AP Payment Request",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "AP Payment Request not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> softDeleteApPaymentRequest(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        service.delete(transactionPoid, deleteReasonDto);
        return success("AP Payment Request has been soft deleted successfully");
    }

    @Operation(
            summary = "List AP Payment Requests with Search and Sort",
            description = """
                Provide search filters.
                Valid searchField values:
                - GLOBALSEARCH
                - transactionPoid
                - docRef
                - refType
                - currencyCode

                Default sorting: transactionPoid DESC
                """
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listApPaymentRequest(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestParam(required = false) LocalDate periodFrom,
            @RequestParam(required = false) LocalDate periodTo
    ) {
        try {
            Map<String, Object> data = service.listPaymentRequest(
                    UserContext.getDocumentId(),
                    filters,
                    pageable,
                    periodFrom,
                    periodTo
            );
            return success("AP Payment Request fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError(
                    "Unable to fetch AP Payment Request list: " + ex.getMessage()
            );
        }
    }

    @Operation(
            summary = "Create AP Payment Request from PO",
            description = "Creates AP Payment Request based on Purchase Order (PO)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully processed"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/po")
    public ResponseEntity<?> createFromPo(
            @Parameter(description = "PO POID", required = true)
            @RequestParam String poPoid
    ) {
        ApPaymentRequestResponse response = service.createFromPo(poPoid);
        return success(response.getMessage(), response.getRecords());
    }

    @Operation(
            summary = "Create AP Payment Request from FF",
            description = "Creates AP Payment Request based on Freight Forwarder (FF)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully processed"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/ff")
    public ResponseEntity<?> createFromFf(
            @Parameter(description = "FF POID", required = true)
            @RequestParam String ffPoid
    ) {
        ApPaymentRequestResponse response = service.createFromFf(ffPoid);
        return success(response.getMessage(), response.getRecords());
    }


    @Operation(
            summary = "Create AP Payment Request from FDA",
            description = "Creates AP Payment Request based on FDA",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully processed"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/fda")
    public ResponseEntity<?> createFromFda(
            @Parameter(description = "FDA POID", required = true)
            @RequestParam String fdaPoid
    ) {
        ApPaymentRequestResponse response = service.createFromFda(fdaPoid);
        return success(response.getMessage(), response.getRecords());
    }

    @Operation(
            summary = "Create AP Payment Request from MTA",
            description = "Creates AP Payment Request based on MTA Purchase Orders",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully processed"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/mta")
    public ResponseEntity<?> createFromMta(
            @Parameter(description = "PO POID", required = true)
            @RequestParam String poPoid
    ) {
        ApPaymentRequestResponse response = service.createFromMta(poPoid);
        return success(response.getMessage(), response.getRecords());
    }

}
