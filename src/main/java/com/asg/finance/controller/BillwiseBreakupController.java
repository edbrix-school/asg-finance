package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;

import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.dto.response.GlVoucherPendingBillwiseBreakupResponseDto;
import com.asg.finance.service.BillwiseBreakupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Date;

import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/v1/billwise-breakup")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class BillwiseBreakupController {

    private final BillwiseBreakupService service;
    private final LoggingService loggingService;

    @Operation(
            summary = "Get Pending Billwise Breakup Details",
            description = "Fetches GL Voucher Pending Billwise Breakup details based on provided parameters (includes all pending bills).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved pending billwise breakup details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = GlVoucherPendingBillwiseBreakupResponseDto.class)
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
                            description = "Data not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/showPending")
    public ResponseEntity<?> getPendingBillwiseBreakup(
            @Parameter(description = "GL POID", required = true, example = "12345")
            @RequestParam Long glPoid,
            @Parameter(description = "As on Date (format: yyyy-MM-dd)", required = true, example = "2025-11-11")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate asOnDate

    ) {

        GlVoucherPendingBillwiseBreakupResponseDto response =
                service.showPendingBillwiseBreakup(UserContext.getGroupPoid(), UserContext.getCompanyPoid(), glPoid, asOnDate);

        return success("Pending Billwise breakup details fetched successfully", response);
    }

    @Operation(
            summary = "Get All Pending Billwise Breakup Details",
            description = "Fetches GL Voucher All Pending Billwise Breakup details based on provided parameters (includes all pending bills).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved all pending billwise breakup details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = GlVoucherPendingBillwiseBreakupResponseDto.class)
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
                            description = "Data not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/showAllPending")
    public ResponseEntity<?> getAllPendingBillwiseBreakup(
            @Parameter(description = "GL POID", required = true, example = "12345")
            @RequestParam Long glPoid,
            @Parameter(description = "Company POID", required = true, example = "67890")
            @RequestParam Long companyPoid,
            @Parameter(description = "As on Date (format: yyyy-MM-dd)", required = true, example = "2025-11-11")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate asOnDate

    ) {

        GlVoucherPendingBillwiseBreakupResponseDto response =
                service.showAllPendingBillwiseBreakup(UserContext.getGroupPoid(), companyPoid, glPoid, asOnDate);

        return success("Pending All Billwise breakup details fetched successfully", response);
    }
}
