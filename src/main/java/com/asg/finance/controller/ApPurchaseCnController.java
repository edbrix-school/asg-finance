package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.ApPurchaseCnHdrDto;
import com.asg.finance.service.ApPurchaseCnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;
import static com.asg.common.lib.dto.response.ApiResponse.*;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/ap-purchase-credit-note")
@Tag(name = "ap-purchase-credit-note-controller", description = "Manage AP Purchase Credit Note (Supplier Credit Note)")
public class ApPurchaseCnController {

    private final ApPurchaseCnService service;

    @Operation(summary = "Create Supplier Credit Note", description = "Creates supplier credit note as per SRS")
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ApPurchaseCnHdrDto dto) {
        ApPurchaseCnHdrDto result = service.create(dto);
        return success("Supplier credit note created successfully", result);
    }

    @Operation(summary = "Get Supplier Credit Note by ID")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getById(@PathVariable Long transactionPoid) {
        ApPurchaseCnHdrDto result = service.getById(transactionPoid);
        return success("Supplier credit note fetched successfully", result);
    }

    @Operation(summary = "List Supplier Credit Notes")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        try {
            if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
                return badRequest("Both startDate and endDate should be specified or both should be empty.");
            }
            Map<String, Object> result = service.list(UserContext.getDocumentId(), filters, startDate, endDate, pageable);
            return success("Supplier credit note list fetched successfully", result);
        } catch (Exception e) {
            log.error("Error fetching supplier credit note list", e);
            return internalServerError("Failed to fetch list: " + e.getMessage());
        }
    }

    @Operation(summary = "Update Supplier Credit Note")
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> update(
            @PathVariable Long transactionPoid,
            @Valid @RequestBody ApPurchaseCnHdrDto dto) {
        ApPurchaseCnHdrDto result = service.update(transactionPoid, dto);
        return success("Supplier credit note updated successfully", result);
    }

    @Operation(summary = "Delete Supplier Credit Note")
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> delete(
            @PathVariable Long transactionPoid,
            @Parameter(description = "Document identifier") @RequestParam String documentId,
            @Parameter(description = "Action requested") @RequestParam String actionRequested) {
        service.delete(transactionPoid);
        return success("Supplier credit note deleted successfully", null);
    }

    @Operation(summary = "Get PJ Reversal Details", description = "Calls PROC_AP_CN_PJ_REF_DETAILS")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/pj-ref-details/{pjPoid}")
    public ResponseEntity<?> getPjRefDetails(@PathVariable Long pjPoid) {
        Map<String, Object> result = service.getPjRefDetails(pjPoid);
        return success("PJ reference details fetched successfully", result);
    }

    @Operation(summary = "Get Party Details", description = "Calls PROC_AP_CN_PJ_PARTY_DTLS")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/party-details/{partyType}/{partyPoid}")
    public ResponseEntity<?> getPartyDetails(
            @PathVariable String partyType,
            @PathVariable Long partyPoid) {
        Map<String, Object> result = service.getPartyDetails(partyType, partyPoid);
        return success("Party details fetched successfully", result);
    }
}
