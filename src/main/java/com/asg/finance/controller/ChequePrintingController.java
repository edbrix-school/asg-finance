package com.asg.finance.controller;

import static com.asg.common.lib.dto.response.ApiResponse.success;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.asg.finance.dto.ChequeStockResponse;
import com.asg.finance.dto.PendingChequeResponse;
import com.asg.finance.service.ChequePrintingService;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/cheque-printing")
@RequiredArgsConstructor
@Slf4j
public class ChequePrintingController {

	private final ChequePrintingService service;

	@GetMapping("/pending")
	public ResponseEntity<?> pendingCheques() {
		List<PendingChequeResponse> responses = service.getPendingCheques();
		return success("Pending Cheques fetched successfully", responses);
	}

	@GetMapping("/stock")
	public ResponseEntity<?> chequeStock(
			@Parameter(name = "bankCode", description = "Optional bank code", required = false, example = "NBP") @RequestParam(required = false) String bankCode,
			@Parameter(name = "signType", description = "Signature type filter", required = false, example = "Manual") @RequestParam(required = false) String signType) {
		List<ChequeStockResponse> responses = service.getChequeStock(bankCode, signType);
		return success("Cheque stocks fetched successfully", responses);
	}

}