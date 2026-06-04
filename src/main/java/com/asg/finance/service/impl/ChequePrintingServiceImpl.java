package com.asg.finance.service.impl;

import java.util.List;
import java.util.Map;

import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.GlobalParameterService;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.dto.ChequePrintBatchRequest;
import com.asg.finance.dto.ChequePrintBatchResponse;
import com.asg.finance.dto.ChequePrintPendingRequest;
import com.asg.finance.dto.ChequePrintStockRequest;
import com.asg.finance.service.ChequePrintingService;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.asg.finance.dto.ChequeStockResponse;
import com.asg.finance.dto.PendingChequeResponse;
import com.asg.finance.repository.ChequePrintingRepository;

import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;

@Service
@RequiredArgsConstructor
public class ChequePrintingServiceImpl implements ChequePrintingService {

	private static final String STATUS_SUCCESS = "SUCCESS";
	private static final String STATUS_ERROR = "ERROR";
	private static final String STATUS_INFO = "INFO:";
	private static final String CHEQUE_PRINT_BANK = "CHEQUE_PRINT_BANK";

	private final ChequePrintingRepository repo;
	private final PrintService printService;
	private final DataSource dataSource;
	private final GlobalParameterService globalParameterService;

	@Override
	public List<PendingChequeResponse> getPendingCheques() {
		return repo.fetchPendingCheques();
	}

	@Override
	public List<ChequeStockResponse> getChequeStock(String bankCode, String signType) {
		String normalizedBankCode = StringUtils.defaultIfBlank(
				StringUtils.trimToNull(bankCode),
				getDefaultChequePrintBank());
		return repo.fetchChequeStock(normalizedBankCode, signType);
	}

	@Override
	public String getDefaultChequePrintBank() {
		return globalParameterService.getParameterValue(CHEQUE_PRINT_BANK, "GROUP", "-", "");
	}


	@Override
	public ChequePrintBatchResponse print(ChequePrintBatchRequest request) throws Exception {
		if (request.getStocks() == null || request.getStocks().isEmpty()) {
			throw new ValidationException("Cheque Stationery type is not selected");
		}
		// Legacy behavior: if any stock row is not selected, stop immediately.
		boolean anyStockNotSelected = request.getStocks().stream()
				.anyMatch(stock -> !Boolean.TRUE.equals(stock.getSelected()));
		if (anyStockNotSelected) {
			throw new ValidationException("Cheque Stationery type is not selected...");
		}

		List<ChequePrintPendingRequest> selectedCheques = request.getPendingCheques() == null ? List.of()
				: request.getPendingCheques().stream()
				.filter(chq -> Boolean.TRUE.equals(chq.getSelected()))
				.toList();
		if (selectedCheques.isEmpty()) {
			throw new ValidationException("No Cheques are selected...");
		}

		ChequePrintBatchResponse response = new ChequePrintBatchResponse();
		response.setRequestedCount(selectedCheques.size());

		for (ChequePrintPendingRequest cheque : selectedCheques) {
			ChequePrintStockRequest matchedStock = request.getStocks().stream()
					.filter(stock -> Boolean.TRUE.equals(stock.getSelected()))
					.filter(stock -> stock.getCompanyPoid().equals(cheque.getCompanyPoid()))
					.findFirst()
					.orElse(null);

			if (matchedStock == null) {
				response.setSkippedCount(response.getSkippedCount() + 1);
				continue;
			}

			PrintOneResult result = printInternal(
					cheque.getTransactionPoid(),
					cheque.getCompanyPoid(),
					cheque.getPvNo(),
					matchedStock.getBankPoid(),
					matchedStock.getStockType(),
					cheque.getAccountPayee(),
					request.getSuppressBalanceCheck());

			if (result.isPrinted()) {
				response.setPrintedCount(response.getPrintedCount() + 1);
				response.getPrintedTransactions().add(String.valueOf(cheque.getTransactionPoid()));
			}
			if (StringUtils.isNotBlank(result.userMessage())) {
				response.getMessages().add("TXN " + cheque.getTransactionPoid() + ": " + result.userMessage());
			}
		}
		if (response.getPrintedCount() == 0 && response.getMessages().isEmpty()) {
			throw new ValidationException("No matching cheque stock found for selected cheques");
		}
		return response;
	}

	private PrintOneResult printInternal(Long transactionPoid, Long companyPoid, String pvNo, Long bankPoid, String stockType,
			String accountPayee, String suppressBalanceCheckValue) throws Exception {
		String suppressBalanceCheck = StringUtils.defaultIfBlank(suppressBalanceCheckValue, "N");
		Map<String, String> beforePrintResult = repo.validateBeforeChequePrint(
				UserContext.getGroupPoid(),
				UserContext.getUserName(),
				String.valueOf(companyPoid),
				bankPoid,
				stockType,
				transactionPoid,
				suppressBalanceCheck
		);

		String status = beforePrintResult.get("result");
		if (StringUtils.isBlank(status)) {
			throw new ValidationException("No response from before cheque print validation");
		}
		if (status.contains(STATUS_ERROR)) {
			throw new ValidationException(status);
		}
		if (!status.contains(STATUS_SUCCESS) && !status.contains(STATUS_INFO)) {
			throw new ValidationException(status);
		}
		if (status.contains(STATUS_INFO)) {
			return PrintOneResult.withInfo(status);
		}

		String chequeNumber = beforePrintResult.get("nextChequeNumber");
		String defaultPrinter = beforePrintResult.get("defaultPrinter");

		if (StringUtils.isBlank(chequeNumber)) {
			throw new ValidationException("Cheque number was not generated by before cheque print validation");
		}
		if (StringUtils.isBlank(defaultPrinter)) {
			throw new ValidationException("No Printers configured for selected Cheque Stationery");
		}

		Map<String, Object> params = printService.buildBaseParams(transactionPoid, UserContext.getDocumentId());
		params.put("CURRENT_CHEQUE_NO", chequeNumber);
		params.put("BANK_POID", bankPoid);
		params.put("ACCOUNT_PAYEE", accountPayee);
		params.put("ACCOUNT_PAYEE_IMG", "jasper/Finance/BankPayments/AccountsPayeeOnly.png");
		JasperReport mainReport = printService.load("Finance/BankPayments/BankPaymentVoucher.jrxml");
		printService.printReportToPrinter(mainReport, params, dataSource, defaultPrinter, 1);

		String afterStatus = repo.afterChequePrint(
				UserContext.getGroupPoid(),
				UserContext.getUserName(),
				companyPoid,
				transactionPoid,
				bankPoid,
				stockType,
				UserContext.getUserPoid()
		);
		if (StringUtils.isNotBlank(afterStatus) && afterStatus.contains(STATUS_ERROR)) {
			throw new ValidationException("Some error occured after check print: pv_no " + pvNo + " " + afterStatus);
		}
		if (StringUtils.isNotBlank(afterStatus) && afterStatus.contains(STATUS_INFO)) {
			return PrintOneResult.withInfo(afterStatus);
		}
		return PrintOneResult.successPrinted();
	}

	private record PrintOneResult(boolean isPrinted, String userMessage) {
		static PrintOneResult successPrinted() {
			return new PrintOneResult(true, null);
		}
		static PrintOneResult withInfo(String message) {
			return new PrintOneResult(false, message);
		}
	}
}
