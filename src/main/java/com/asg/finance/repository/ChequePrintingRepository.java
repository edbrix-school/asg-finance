package com.asg.finance.repository;

import java.util.List;
import java.util.Map;

import com.asg.finance.dto.ChequeStockResponse;
import com.asg.finance.dto.PendingChequeResponse;

public interface ChequePrintingRepository {
	List<PendingChequeResponse> fetchPendingCheques();

	List<ChequeStockResponse> fetchChequeStock(String bankCode, String signType);

	Map<String, String> validateBeforeChequePrint(Long groupPoid, Long loginUserPoid, String companyPoid, Long bankPoid,
			String chqSignType, Long transactionPoid, String suppressBalanceCheck);

	String afterChequePrint(Long groupPoid, String loginUser, Long companyPoid, Long transactionPoid, Long bankPoid,
			String chqSignType, Long userPoid);
}
