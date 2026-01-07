package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.BankDepositVoucherDtlDto;
import com.asg.finance.dto.BankDepositVoucherRequestDto;
import com.asg.finance.dto.BankDepositVoucherResponseDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.service.BankDepositVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@Slf4j
@RestController
@RequestMapping("/v1/bank-deposit-voucher")
@Tag(name = "Bank Deposit Voucher", description = "APIs for managing Bank Deposit Voucher (Doc ID: 400-109)")
@RequiredArgsConstructor
public class BankDepositVoucherController {

    private final BankDepositVoucherService service;

    @Operation(
            summary = "Create Bank Deposit Voucher",
            description = "Creates a new Bank Deposit Voucher with cheque/cash details",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully created",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BankDepositVoucherResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = BankDepositVoucherRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Bank Voucher Create Example",
                                    value = """
                                            {
                                               "bankPoid": 61,
                                               "type": "CHEQUE",
                                               "bankFilter": "OWN BANK",
                                               "groupPosting": true,
                                               "postingNarration": "Bank deposit for cheques received",
                                               "transactionDate": "2025-01-15",
                                               "companyPoid": 1,
                                               "groupPoid": 1,
                                               "remarks": "Monthly cheque deposit",
                                               "details": [
                                                 {
                                                   "bankPoid": 61,
                                                   "pymtType": "CHEQUE",
                                                   "paymentMainPoid": 768393,
                                                   "refDocPoid": 2085,
                                                   "refDocRef": "ASGDR858626",
                                                   "chqAcName": "BABASONS",
                                                   "chqAcNo": "2002623964080",
                                                   "chqCardNo": "034588",
                                                   "chqDate": "2016-04-07",
                                                   "amount": 74.89,
                                                   "remarks": "Cheque deposit",
                                                   "selected": "Y",
                                                   "chqSeqNum": 1
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
    public ResponseEntity<?> create(
            @Valid @RequestBody BankDepositVoucherRequestDto request
    ) {
        try {
            BankDepositVoucherResponseDto response = service.createBankDepositVoucher(request);
            return success("Bank Deposit Voucher created successfully", response);
        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to create Bank Deposit Voucher: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Update Bank Deposit Voucher",
            description = "Updates an existing Bank Deposit Voucher",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully updated"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = BankDepositVoucherRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "Bank Voucher Update Example",
                                    value = """
                                            {
                                               "bankPoid": 61,
                                               "type": "CHEQUE",
                                               "bankFilter": "OWN BANK",
                                               "groupPosting": true,
                                               "postingNarration": "Bank deposit for cheques received",
                                               "transactionDate": "2025-01-15",
                                               "companyPoid": 1,
                                               "groupPoid": 1,
                                               "remarks": "Monthly cheque deposit",
                                               "details": [
                                                 {
                                                   "bankPoid": 61,
                                                   "pymtType": "CHEQUE",
                                                   "paymentMainPoid": 768393,
                                                   "refDocPoid": 2085,
                                                   "refDocRef": "ASGDR858626",
                                                   "chqAcName": "BABASONS",
                                                   "chqAcNo": "2002623964080",
                                                   "chqCardNo": "034588",
                                                   "chqDate": "2016-04-07",
                                                   "amount": 74.89,
                                                   "remarks": "Cheque deposit",
                                                   "selected": "Y",
                                                   "chqSeqNum": 1
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
    public ResponseEntity<?> update(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Valid @RequestBody BankDepositVoucherRequestDto request
    ) {
        try {
            BankDepositVoucherResponseDto response = service.updateBankDepositVoucher(transactionPoid, request);
            return success("Bank Deposit Voucher updated successfully", response);
        } catch (ValidationException ex) {
            return internalServerError(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Failed to update Bank Deposit Voucher: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get Bank Deposit Voucher by ID",
            description = "Retrieves Bank Deposit Voucher details",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getById(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid
    ) {
        BankDepositVoucherResponseDto response = service.getBankDepositVoucherById(transactionPoid);
        return success("Bank Deposit Voucher fetched successfully", response);
    }

    @Operation(
            summary = "Soft delete Bank Deposit Voucher",
            description = "Marks a Bank Deposit Voucher as deleted",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully deleted"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> softDelete(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid
    ) {
        service.softDeleteBankDepositVoucher(transactionPoid);
        return success("Bank Deposit Voucher has been soft deleted successfully");
    }

    @Operation(
            summary = "List Bank Deposit Vouchers with Search and Sort",
            description = "Retrieve a paginated list of Bank Deposit Vouchers with optional filtering and sorting"
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        try {
            if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
                return badRequest("Both startDate and endDate should be specified or both dates should be empty.");
            }
            Map<String, Object> data = service.listBankDepositVouchers(UserContext.getDocumentId(), filters, startDate, endDate, pageable);
            return success("Bank Deposit Vouchers fetched successfully", data);
        } catch (Exception ex) {
            return internalServerError("Unable to fetch Bank Deposit Voucher list: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Load pending payments for Bank Deposit Voucher",
            description = "Loads pending payments/cheques for a specific bank",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully loaded",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BankDepositVoucherDtlDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/load/pending-payments")
    public ResponseEntity<?> loadPendingPayments(
            @Parameter(description = "Bank POID", required = true, example = "15631")
            @RequestParam Long bankPoid,
            @Parameter(description = "Payment type (CHEQUE/CASH)", required = true, example = "CHEQUE")
            @RequestParam String type,
            @Parameter(description = "Bank filter criteria", required = false, example = "OTHER BANK")
            @RequestParam(required = false) String bankFilter
    ) {
        List<BankDepositVoucherDtlDto> pendingPayments = service.loadPendingPayments(bankPoid, type, bankFilter);
        return success("Pending payments loaded successfully", pendingPayments);
    }


    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for Bank Deposit Voucher",
            description = "Generate PDF report for a specific Bank Deposit Voucher transaction",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Bank Deposit Voucher not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "69789")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = service.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=bank-deposit-voucher-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for Bank Deposit Voucher: {}", transactionPoid, e);
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }
}
