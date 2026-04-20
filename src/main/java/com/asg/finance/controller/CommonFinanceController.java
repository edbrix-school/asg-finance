package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.finance.service.CreditNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.asg.common.lib.dto.response.ApiResponse.internalServerError;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/common")
@Tag(name = "common-finance", description = "Shared finance utilities used across AR/AP modules")
public class CommonFinanceController {

    private final CreditNoteService creditNoteService;

    @Operation(
            summary = "Get Party GL POID",
            description = """
                    Fetches the GL POID for a given party using PROC_GL_GET_DR_PARTY_GLPOID.
                    Used by Debit Note, Credit Note, Supplier Credit Note, and Purchase Journal
                    when Ref Type is GENERAL.

                    ### Input
                    - **partyPoid** — Party identifier
                    - **partyType** — SUPPLIER, CUSTOMER, or PRINCIPAL

                    ### Output
                    - Party GL POID
                    """
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/party-gl/{partyPoid}")
    public ResponseEntity<?> getPartyGLPoid(
            @Parameter(description = "Party POID", example = "1001")
            @PathVariable Long partyPoid,
            @Parameter(description = "Party Type: SUPPLIER, CUSTOMER, PRINCIPAL", example = "SUPPLIER")
            @RequestParam String partyType) {
        try {
            Long result = creditNoteService.getPartyGLPoid(partyPoid, partyType);
            return success("Party GL POID fetched successfully", result);
        } catch (Exception e) {
            log.error("Error fetching party GL POID for partyPoid: {}, partyType: {}", partyPoid, partyType, e);
            return internalServerError("Failed to fetch party GL POID: " + e.getMessage());
        }
    }
}
