package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.CreditNoteHeaderDto;
import com.asg.finance.dto.DefaultCreditValuesDto;
import com.asg.finance.dto.FdaRefResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.service.CreditNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import com.asg.common.lib.dto.FilterDto;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
@RequestMapping("/v1/credit-note")
@Tag(
        name = "credit-note-controller",
        description = "Manage Credit Note records, GL/Charge tabs, reference invoice fetch and listing."
)

public class CreditNoteController {
    private final CreditNoteService creditNoteService;
    private final LoggingService loggingService;

    @Operation(
            summary = "Create Credit Note",
            description = "Creates a new credit note with automatic ID generation, validation, and stored procedure integration. Supports FF_INVOICE, SH_INVOICE, DN_INVOICE, FDA, and GENERAL reference types.",
            tags = {"Credit Note Management"}
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    ### Request Body Fields
                    **Header Fields:**
                    - **partyType*** (string): CUSTOMER, SUPPLIER, PRINCIPAL
                    - **partyPoid*** (long): Party identifier
                    - **refType*** (string): FF_INVOICE, SH_INVOICE, DN_INVOICE, FDA, GENERAL
                    - **postingNarration*** (string): Posting description
                    - **transactionDate** (date): Transaction date
                    - **currencyCode** (string): USD, BHD, etc.
                    - **currencyRate** (decimal): Exchange rate
                    - **amount** (decimal): Credit amount
                    - **bhdAmount** (decimal): Amount in BHD
                    - **creditPeriod** (long): Credit period in days
                    - **dueDate** (date): Due date
                    - **tinNumber** (string): Tax identification number
                    - **billRefType** (string): Bill reference type
                    - **printableRemarks** (boolean): Print remarks flag
                    - **issueType** (string): Issue type Y/N
                    - **multiCompany** (boolean): Multi-company flag
                    - **remarks** (string): Additional remarks
                    - **grandTotal** (decimal): Grand total amount
                    - **bankPoid** (long): Bank identifier
                    
                    **Reference-specific Fields:**
                    - **shInvoicePoid** (long): For SH_INVOICE type
                    - **ffInvoicePoid** (long): For FF_INVOICE type
                    - **dnInvoicePoid** (long): For DN_INVOICE type
                    - **fdaRefPoid** (long): For FDA type
                    - **dnFdaReference** (string): DN FDA reference text
                    - **fdaDirect** (boolean): FDA direct flag
                    
                    **GL Details Array:**
                    - **companyPoid*** (long): Company identifier
                    - **type** (string): DR/CR
                    - **glPoid** (long): GL account identifier
                    - **drAmt** (decimal): Debit amount
                    - **crAmt** (decimal): Credit amount
                    - **taxPoid*** (long): Tax identifier
                    - **taxPercentage** (decimal): Tax percentage
                    - **taxAmount** (decimal): Tax amount
                    - **totalAmount** (decimal): Total amount
                    - **remarks** (string): GL remarks
                    - **breakupList** (array): Billwise breakup details
                    - **costCenterList** (array): Cost center breakup details
                    
                    **Charge Details Array:**
                    - **chargePoid** (long): Charge identifier
                    - **chargeAmount** (decimal): Charge amount
                    - **chargeCostAmount** (decimal): Charge cost amount
                    - **pdaAmount** (decimal): PDA amount
                    - **taxPoid** (long): Tax identifier
                    - **taxPercentage** (decimal): Tax percentage
                    - **taxAmount** (decimal): Tax amount
                    - **totalAmount** (decimal): Total amount
                    - **remarks** (string): Charge remarks
                    - **issueInvoice** (string): Y/N
                    - **selected** (boolean): Selection flag
                    
                    ### Authorization Parameters (handled by interceptor)
                    - **documentId:** Document identifier (300-111)
                    - **actionRequested:** Action being performed (CREATE)
                    """,
            content = @Content(
                    schema = @Schema(implementation = CreditNoteHeaderDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Credit Note Creation Example",
                                    value = """
                                            {
                                              "partyType": "CUSTOMER",
                                              "partyPoid": 12345,
                                              "refType": "FF_INVOICE",
                                              "ffInvoicePoid": 67890,
                                              "postingNarration": "Credit note against FF Invoice dated 25-May-2025",
                                              "transactionDate": "2025-05-25",
                                              "currencyCode": "BHD",
                                              "currencyRate": 1.000,
                                              "amount": 169.000,
                                              "bhdAmount": 169.000,
                                              "creditPeriod": 30,
                                              "dueDate": "2025-06-24",
                                              "tinNumber": "TIN-87456021",
                                              "billRefType": "AGAINST",
                                              "printableRemarks": true,
                                              "issueType": "Y",
                                              "multiCompany": false,
                                              "remarks": "Customs Duty Paid On Behalf Of Consignee",
                                              "grandTotal": 169.000,
                                              "glDetails": [
                                                {
                                                  "companyPoid": 5,
                                                  "type": "DR",
                                                  "glPoid": 4001,
                                                  "drAmt": 169.000,
                                                  "crAmt": 0.000,
                                                  "taxPoid": 14,
                                                  "taxPercentage": 0.00,
                                                  "taxAmount": 0.00,
                                                  "totalAmount": 169.000,
                                                  "remarks": "Credit reversal for customs duty"
                                                }
                                              ],
                                              "chargeDetails": [
                                                {
                                                  "chargePoid": 101,
                                                  "chargeAmount": 169.000,
                                                  "chargeCostAmount": 169.000,
                                                  "pdaAmount": 0.000,
                                                  "taxPoid": 14,
                                                  "taxPercentage": 0.00,
                                                  "taxAmount": 0.00,
                                                  "totalAmount": 169.000,
                                                  "remarks": "Custom duty paid",
                                                  "issueInvoice": "Y",
                                                  "selected": true
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
    public ResponseEntity<?> createCreditNote(
            @Valid @RequestBody CreditNoteHeaderDto creditNoteDto) {
        CreditNoteHeaderDto result = creditNoteService.createCreditNote(creditNoteDto);
        return success("Credit note created successfully", result);
    }

    @Operation(
            summary = "Get Credit Note by ID",
            description = "Retrieves complete credit note details including header, GL entries, and charge details by transaction POID.",
            tags = {"Credit Note Management"}
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getCreditNoteById(
            @PathVariable Long transactionPoid) {
        CreditNoteHeaderDto result = creditNoteService.getCreditNoteById(transactionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        return success("Credit note fetched successfully", result);
    }

    @Operation(
            summary = "List Credit Notes with Search and Sort",
            description = "Retrieves paginated list of credit notes with advanced filtering and sorting capabilities. Supports global search and field-specific filters.",
            tags = {"Credit Note Management"}
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    - ### Filters:
                      Use either:
                      1. A single `GLOBALSEARCH` filter, OR
                      2. Any combination of specific fields (PARTY_TYPE, REF_TYPE, CURRENCY_CODE, REMARKS).
                      3. operator field will either have "AND" or "OR", if not given will be considered as "OR",
                         not required for GLOBALSEARCH, for non GLOBALSEARCH need to give only 1 time
                      4. isDeleted when 'N' or null, will search and return non deleted records, 'Y' will check and return deleted records
                      5. sort will be default on primary key ascending if specified in db field otherwise you can override it giving the field name and the direction.
                    
                    - #### Global Search:
                      Apply one search term across multiple fields.
                      No operator need to send in this case.
                      • { "searchField": "GLOBALSEARCH", "searchValue": "FF" }
                    
                    - #### Single Field, Single Value:
                      Search one field with one value.
                      • { "searchField": "PARTY_TYPE", "searchValue": "PRINCIPAL" },
                    
                    - #### Single Field, Multiple Values:
                      Provide multiple values for the same field, separated by `|`.
                      • { "searchField": "REF_TYPE", "searchValue": "FF_INVOICE|SH_INVOICE" }
                    
                    - #### Multiple Different Fields, Single or Multiple Value:
                      Provide one or multiple search values across multiple fields.
                      • { "searchField": "PARTY_TYPE", "searchValue": "PRINCIPAL" },
                      • { "searchField": "REF_TYPE", "searchValue": "FF_INVOICE|SH_INVOICE" }
                      • "operator" :  "AND"/"OR"
                    
                    - ### Sorting:
                      Defaults to whatever specified in list_of_records_sql field mostly primary key ascending.
                      To override, pass `sort=<field>,ASC|DESC` in query params.
                      Examples:
                      • sort=TRANSACTION_DATE,ASC
                      • sort=GRAND_TOTAL,DESC
                    
                    - ### Authorization Parameters (handled by interceptor)
                        - **documentId:** Document identifier (300-111)
                        - **actionRequested:** Action being performed (VIEW)
                    """,
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = FilterDto.class)),
                    examples = {
                            @ExampleObject(
                                    name = "Credit Note Filters",
                                    value = """
                                            {
                                            "operator": "AND",
                                            "isDeleted": "N",
                                            "filters": [
                                               { "searchField": "GLOBALSEARCH", "searchValue": "FF" },
                                               { "searchField": "PARTY_TYPE", "searchValue": "PRINCIPAL|AGENT" },
                                               { "searchField": "REF_TYPE", "searchValue": "FF_INVOICE"},
                                               { "searchField": "CURRENCY_CODE", "searchValue": "USD"},
                                               { "searchField": "REMARKS", "searchValue": "credit"}
                                            ]
                                            }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> getCreditNoteList(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        try {
            if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
                return badRequest("Both startDate and endDate should be specified or both dates should be empty.");
            }
            Map<String, Object> result = creditNoteService.listCreditNotes(UserContext.getDocumentId(), filters, startDate, endDate, pageable);
            return success("Credit note list fetched successfully", result);
        } catch (Exception e) {
            log.error("Error fetching credit note list", e);
            return internalServerError("Failed to fetch credit note list: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Update Credit Note",
            description = "Updates existing credit note. All GL and charge details are replaced with new values. Validates business rules and maintains audit trail.",
            tags = {"Credit Note Management"}
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = """
                    ### Request Body Fields (Same as Create)
                    **Note:** transactionPoid in body will be ignored - path parameter takes precedence
                    
                    **Header Fields:**
                    - **partyType*** (string): CUSTOMER, SUPPLIER, PRINCIPAL
                    - **partyPoid*** (long): Party identifier
                    - **refType*** (string): FF_INVOICE, SH_INVOICE, DN_INVOICE, FDA, GENERAL
                    - **postingNarration*** (string): Posting description
                    - **transactionDate** (date): Transaction date
                    - **currencyCode** (string): USD, BHD, etc.
                    - **currencyRate** (decimal): Exchange rate
                    - **amount** (decimal): Credit amount
                    - **bhdAmount** (decimal): Amount in BHD
                    - **creditPeriod** (long): Credit period in days
                    - **dueDate** (date): Due date
                    - **tinNumber** (string): Tax identification number
                    - **billRefType** (string): Bill reference type
                    - **printableRemarks** (boolean): Print remarks flag
                    - **issueType** (string): Issue type Y/N
                    - **multiCompany** (boolean): Multi-company flag
                    - **remarks** (string): Additional remarks
                    - **grandTotal** (decimal): Grand total amount
                    - **bankPoid** (long): Bank identifier
                    
                    **Reference-specific Fields:**
                    - **shInvoicePoid** (long): For SH_INVOICE type
                    - **ffInvoicePoid** (long): For FF_INVOICE type
                    - **dnInvoicePoid** (long): For DN_INVOICE type
                    - **fdaRefPoid** (long): For FDA type
                    - **dnFdaReference** (string): DN FDA reference text
                    - **fdaDirect** (boolean): FDA direct flag
                    
                    **GL Details Array:** (All existing GL details will be replaced)
                    - **companyPoid*** (long): Company identifier
                    - **type** (string): DR/CR
                    - **glPoid** (long): GL account identifier
                    - **drAmt** (decimal): Debit amount
                    - **crAmt** (decimal): Credit amount
                    - **taxPoid*** (long): Tax identifier
                    - **taxPercentage** (decimal): Tax percentage
                    - **taxAmount** (decimal): Tax amount
                    - **totalAmount** (decimal): Total amount
                    - **remarks** (string): GL remarks
                    - **breakupList** (array): Billwise breakup details
                    - **costCenterList** (array): Cost center breakup details
                    
                    **Charge Details Array:** (All existing charge details will be replaced)
                    - **chargePoid** (long): Charge identifier
                    - **chargeAmount** (decimal): Charge amount
                    - **chargeCostAmount** (decimal): Charge cost amount
                    - **pdaAmount** (decimal): PDA amount
                    - **taxPoid** (long): Tax identifier
                    - **taxPercentage** (decimal): Tax percentage
                    - **taxAmount** (decimal): Tax amount
                    - **totalAmount** (decimal): Total amount
                    - **remarks** (string): Charge remarks
                    - **issueInvoice** (string): Y/N
                    - **selected** (boolean): Selection flag
                    
                    ### Authorization Parameters (handled by interceptor)
                    - **documentId:** Document identifier (300-111)
                    - **actionRequested:** Action being performed (EDIT)
                    """,
            content = @Content(
                    schema = @Schema(implementation = CreditNoteHeaderDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Credit Note Update Example",
                                    value = """
                                            {
                                              "partyType": "PRINCIPAL",
                                              "partyPoid": 54321,
                                              "refType": "FF_INVOICE",
                                              "ffInvoicePoid": 98765,
                                              "postingNarration": "Updated credit note against FF Invoice",
                                              "transactionDate": "2025-05-26",
                                              "currencyCode": "USD",
                                              "currencyRate": 0.3765,
                                              "amount": 2000.00,
                                              "bhdAmount": 753.00,
                                              "creditPeriod": 45,
                                              "dueDate": "2025-07-10",
                                              "remarks": "Updated credit note for FF charges",
                                              "grandTotal": 2000.00,
                                              "glDetails": [
                                                {
                                                  "companyPoid": 3,
                                                  "type": "DR",
                                                  "glPoid": 4002,
                                                  "drAmt": 2000.00,
                                                  "crAmt": 0.00,
                                                  "taxPoid": 15,
                                                  "taxPercentage": 0.00,
                                                  "taxAmount": 0.00,
                                                  "totalAmount": 2000.00,
                                                  "remarks": "Updated debit entry"
                                                }
                                              ],
                                              "chargeDetails": [
                                                {
                                                  "chargePoid": 102,
                                                  "chargeAmount": 2000.00,
                                                  "chargeCostAmount": 2000.00,
                                                  "pdaAmount": 0.00,
                                                  "taxPoid": 15,
                                                  "taxPercentage": 0.00,
                                                  "taxAmount": 0.00,
                                                  "totalAmount": 2000.00,
                                                  "remarks": "Updated credit note charge",
                                                  "issueInvoice": "Y",
                                                  "selected": true
                                                }
                                              ]
                                            }
                                            """
                            )
                    }
            )
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    public ResponseEntity<?> updateCreditNote(
            @PathVariable Long transactionPoid,
            @Valid @RequestBody CreditNoteHeaderDto creditNoteDto) {
        CreditNoteHeaderDto result = creditNoteService.updateCreditNote(transactionPoid, creditNoteDto);
        return success("Credit note updated successfully", result);
    }

    @Operation(
            summary = "Delete Credit Note",
            description = "Performs soft delete by setting DELETED='Y'. Validates deletion permissions and maintains referential integrity.",
            tags = {"Credit Note Management"}
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> deleteCreditNote(
            @PathVariable Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
            creditNoteService.deleteCreditNote(transactionPoid, deleteReasonDto);
            return success("Credit note deleted successfully", null);
    }

    @Operation(
            summary = "Get FF Invoice Charges",
            description = """
                Fetches charge details for FF Invoice using:
                PROC_CR_NOTE_CREATE_FROM_FF

                ### Input:
                - FF Invoice Ref No (refNo)

                ### Output:
                - Charge Amount
                - Tax Percent
                - Total Amount etc.
                """
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/ref/ff/{refNo}")
    public ResponseEntity<?> getFFInvoiceCharges(
            @PathVariable Long refNo,
            @RequestParam Long partyPoid) {
        var result = creditNoteService.getFFInvoiceCharges(refNo, partyPoid);

        return success("FF Invoice charges fetched successfully", result);
    }

    @Operation(
            summary = "Get Shipping Invoice Charges",
            description = """
                Fetch shipment invoice–based charges using:
                PROC_AR_CREDIT_NT_FROM_SH_INV

                Requires:
                - Invoice Ref No
                - Party POID

                Returns:
                - Charge lines
                - Tax details
                - Manifest references
                """
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/ref/sh/{refNo}")
    public ResponseEntity<?> getSHInvoiceCharges(
            @PathVariable Long refNo,
            @RequestParam Long partyPoid) {
        var result = creditNoteService.getSHInvoiceCharges(refNo, partyPoid);
        return success("Shipping invoice charges fetched successfully", result);
    }

    @Operation(
            summary = "Get DN Invoice Charges",
            description = """
                Loads charges for DN invoice using:
                PROC_AR_CN_CREATE_FROM_DN
                """
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/ref/dn/{refNo}")
    public ResponseEntity<?> getDNInvoiceCharges(
            @PathVariable Long refNo,
            @RequestParam Long partyPoid) {
        var result = creditNoteService.getDNInvoiceCharges(refNo, partyPoid);
        return success("DN invoice charges fetched successfully", result);
    }

    @Operation(
            summary = "Get FDA Reference Details",
            description = """
                Fetch FDA-related reference and charge details required for credit note creation.

                Validates:
                - Party mapping
                - FDA status
                """
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/ref/fda/{fdaRef}")
    public ResponseEntity<?> getFDADetails(
            @PathVariable Long fdaRef,
            @RequestParam Long partyPoid) {
        var result = creditNoteService.getFDADetails(fdaRef, partyPoid);
        return success("FDA details fetched successfully", result);
    }

    @Operation(
            summary = "Get Default Credit Values",
            description = """
                Fetches default credit values for a selected party using:
                PROC_PI_SET_DEFAULT_CREDIT

                ### Input:
                - Party POID
                - Party Type (CUSTOMER, SUPPLIER, PRINCIPAL)

                ### Output:
                - Credit Period
                - Currency Code & Rate
                - TIN Number
                - Due Date (calculated)
                """
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/default-credit/{partyPoid}")
    public ResponseEntity<?> getDefaultCreditValues(
            @PathVariable Long partyPoid,
            @RequestParam String partyType) {
        try {
            DefaultCreditValuesDto result = creditNoteService.getDefaultCreditValues(partyPoid, partyType);
            return success("Default credit values fetched successfully", result);
        } catch (Exception e) {
            log.error("Error fetching default credit values for partyPoid: {}, partyType: {}", partyPoid, partyType, e);
            return internalServerError("Failed to fetch default credit values: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Get Party GL POID",
            description = """
                Fetches party GL POID for a selected party using: When Ref Type is General or Custom
                PROC_GL_GET_DR_PARTY_GLPOID

                ### Input:
                - Party POID
                - Party Type (CUSTOMER, SUPPLIER, PRINCIPAL)
                

                ### Output:
                - Party GL POID
                """
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/party-gl/{partyPoid}")
    public ResponseEntity<?> getPartyGLPoid(
            @PathVariable Long partyPoid,
            @RequestParam String partyType) {
        try {
            Long result = creditNoteService.getPartyGLPoid(partyPoid, partyType);
            return success("Party GL POID fetched successfully", result);
        } catch (Exception e) {
            log.error("Error fetching party GL POID for partyPoid: {}, partyType: {}", partyPoid, partyType, e);
            return internalServerError("Failed to fetch party GL POID: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Get FDA Reference for Credit Note",
            description = """
                Fetches FDA reference details for credit note creation using:
                PROC_AR_CN_SET_FDAREF

                ### Input:
                - Doc Key POID (Transaction POID)
                - LOV Name (e.g., DN_INVOICE_FOR_CN)
                - LOV Value (e.g., Debit Note POID)

                ### Output:
                - FDA Reference POID
                """
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/fda-ref")
    public ResponseEntity<?> getFdaRefForCreditNote(
            @RequestParam Long docKeyPoid,
            @RequestParam String lovName,
            @RequestParam Long lovValue) {
        try {
            FdaRefResponseDto result = creditNoteService.getFdaRefForCreditNote(docKeyPoid, lovName, lovValue);
            return success("FDA reference fetched successfully", result);
        } catch (Exception e) {
            log.error("Error fetching FDA reference for docKeyPoid: {}, lovName: {}, lovValue: {}", docKeyPoid, lovName, lovValue, e);
            return internalServerError("Failed to fetch FDA reference: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate Credit Note PDF",
            description = """
            Generates PDF for Credit Note or Tax Credit Note based on transaction data.
            
            ### PDF Generation Logic:
            - Uses CreditNote.jrxml as main template
            - Automatically selects appropriate subreports based on:
              - REF_TYPE (GENERAL, CUSTOM, VOYAGE, FDA, FF, SH_INVOICE, DN_INVOICE, FF_INVOICE, MTA_INVOICE)
              - YEAR (before/after 2018 for VAT handling)
              - TAX_APPLICABLE flag
            
            ### Subreports Used:
            - DocHeaderSubReport - Company header
            - CreditNoteDtl_subreport1 - GL details (pre-2019)
            - CreditNoteDtlSubreportVAT2019 - GL details with VAT (2019+)
            - CreditNoteChargeSubreport1 - Charge details (pre-2019)
            - CrdeitNoteChargeSubreportVAT2019 - Charge details with VAT (2019+)
            - CrdeitNoteItemSubreport - Item details for MTA invoices
            
            ### Output:
            - PDF file with credit note details
            """,
            tags = {"Credit Note Management"}
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "155514")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = creditNoteService.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=credit-note-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Credit note: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }

}