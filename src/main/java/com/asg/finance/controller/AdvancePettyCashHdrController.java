package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.AdvancePettyCashHdrRequestDTO;
import com.asg.finance.dto.AdvancePettyCashHdrResponseDTO;
import com.asg.finance.service.AdvancePettyCashHdrService;
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
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.internalServerError;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequestMapping("/v1/advance-petty-cash")
@RequiredArgsConstructor
public class AdvancePettyCashHdrController {
    
    private final AdvancePettyCashHdrService service;

    @Operation(
            summary = "Create a new Advance Petty Cash",
            description = "Creates a new advance petty cash record with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created the Advance Petty Cash",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AdvancePettyCashHdrResponseDTO.class)
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
            }
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createAdvancePettyCash(
            @Valid @RequestBody AdvancePettyCashHdrRequestDTO requestDTO
    ) {
        AdvancePettyCashHdrResponseDTO response = service.createAdvancePettyCash(requestDTO);
        return success("Advance Petty Cash created successfully", response);
    }

    @Operation(
            summary = "Update an existing Advance Petty Cash",
            description = "Updates the advance petty cash record with the provided details",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated the Advance Petty Cash",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AdvancePettyCashHdrResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input, object invalid",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Advance Petty Cash not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateAdvancePettyCash(
            @Parameter(description = "Transaction POID to be updated", required = true)
            @PathVariable Long transactionPoid,
            @Valid @RequestBody AdvancePettyCashHdrRequestDTO requestDTO
    ) {
        AdvancePettyCashHdrResponseDTO response = service.updateAdvancePettyCash(transactionPoid, requestDTO);
        return success("Advance Petty Cash updated successfully", response);
    }

    @Operation(
            summary = "Get Advance Petty Cash by ID",
            description = "Retrieves advance petty cash details based on the provided transaction POID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the advance petty cash details",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AdvancePettyCashHdrResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Advance Petty Cash not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getAdvancePettyCashById(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid
    ) {
        AdvancePettyCashHdrResponseDTO response = service.getAdvancePettyCashById(transactionPoid);
        return success("Advance Petty Cash fetched successfully", response);
    }

    @Operation(
            summary = "Soft delete an Advance Petty Cash",
            description = "Marks an advance petty cash record as deleted without permanently removing its data",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully soft deleted the Advance Petty Cash",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Advance Petty Cash not found",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> softDeleteAdvancePettyCash(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        service.softDeleteAdvancePettyCash(transactionPoid, deleteReasonDto);
        return success("Advance Petty Cash has been soft deleted successfully");
    }

    @Operation(
            summary = "List Advance Petty Cash with Search and Sort",
            description = "Provide search filters. Valid searchField values: GLOBALSEARCH or specific field names. Sorting default on transactionPoid, desc."
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listAdvancePettyCash(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestParam(required = false) LocalDate periodFrom,
            @RequestParam(required = false) LocalDate periodTo
    ) {
        try {
            Map<String, Object> data = service.listAdvancePettyCash(UserContext.getDocumentId(), filters, pageable, periodFrom, periodTo);
            return success("Advance Petty Cash fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch advance petty cash list: " + ex.getMessage());
        }
    }
}