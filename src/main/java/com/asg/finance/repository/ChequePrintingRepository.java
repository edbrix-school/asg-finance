package com.asg.finance.repository;

import java.util.List;

import com.asg.finance.dto.ChequeStockResponse;
import com.asg.finance.dto.PendingChequeResponse;

public interface ChequePrintingRepository {
	List<PendingChequeResponse> fetchPendingCheques();

	List<ChequeStockResponse> fetchChequeStock(String bankCode, String signType);

}
