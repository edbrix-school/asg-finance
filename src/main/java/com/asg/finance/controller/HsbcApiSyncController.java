package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;

import static com.asg.common.lib.dto.response.ApiResponse.success;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.asg.finance.dto.HsbcApiSyncResponseDto;
import com.asg.finance.service.HsbcApiSyncService;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/hsbc-api-sync")
@RequiredArgsConstructor
public class HsbcApiSyncController {

    private final HsbcApiSyncService service;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/refresh")
    public ResponseEntity<?> refreshHsbcData(
            @Parameter(description = "Bank account number", required = true, example = "1234567890")
            @RequestParam String accountNumber,

            @Parameter(description = "Date in format yyyy-MM-dd", required = true, example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        HsbcApiSyncResponseDto response = service.refreshHsbcData(accountNumber, date);
        return success("HSBC API data fetched successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/sync")
    public ResponseEntity<?> syncHsbcData(
            @Parameter(description = "Bank account number", required = true, example = "1234567890")
            @RequestParam String accountNumber,

            @Parameter(description = "Date in format yyyy-MM-dd", required = true, example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) throws Exception {

        String message = service.syncHsbcApiData(accountNumber, date);
        return success(message);
    }
}
