package com.asg.finance.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import static com.asg.common.lib.dto.response.ApiResponse.error;
import static com.asg.common.lib.dto.response.ApiResponse.success;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import com.asg.finance.dto.ChequePrintBatchRequest;
import com.asg.finance.dto.ChequePrintBatchResponse;
import com.asg.finance.dto.ChequeStockResponse;
import com.asg.finance.dto.PendingChequeResponse;
import com.asg.finance.service.ChequePrintingService;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/cheque-printing")
@RequiredArgsConstructor
@Slf4j
public class ChequePrintingController {

	private final ChequePrintingService service;

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@GetMapping("/pending")
	public ResponseEntity<?> pendingCheques() {
		List<PendingChequeResponse> responses = service.getPendingCheques();
		return success("Pending Cheques fetched successfully", responses);
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@GetMapping("/stock")
	public ResponseEntity<?> chequeStock(
			@Parameter(name = "bankCode", description = "Optional bank code", required = false, example = "NBP") @RequestParam(required = false) String bankCode,
			@Parameter(name = "signType", description = "Signature type filter", required = false, example = "Manual") @RequestParam(required = false) String signType) {
		List<ChequeStockResponse> responses = service.getChequeStock(bankCode, signType);
		return success("Cheque stocks fetched successfully", responses);
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@Operation(summary = "Get default cheque print bank", description = "Returns default bank value from global parameters for cheque printing")
	@GetMapping("/default-bank")
	public ResponseEntity<?> getDefaultChequePrintBank() {
		String defaultBank = service.getDefaultChequePrintBank();
		return success("Default cheque print bank fetched successfully", Map.of("defaultBank", defaultBank));
	}

	@AllowedAction(UserRolesRightsEnum.PRINT)
	@Operation(summary = "Print selected cheques", description = "Processes selected pending cheques using selected stock rows")
	@PostMapping("/print")
	public ResponseEntity<?> print(@Valid @RequestBody ChequePrintBatchRequest request) {
		try {
			ChequePrintBatchResponse result = service.print(request);
			return success("Cheque printing completed", result);
		} catch (Exception e) {
			log.error("Failed to process cheque print", e);
			return error("Failed to process cheque print: " + e.getMessage(), 500);
		}
	}

}