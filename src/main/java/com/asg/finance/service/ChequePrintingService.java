package com.asg.finance.service;

import java.util.List;

import com.asg.finance.dto.ChequeStockResponse;
import com.asg.finance.dto.PendingChequeResponse;

public interface ChequePrintingService {
	List<PendingChequeResponse> getPendingCheques();
	List<ChequeStockResponse> getChequeStock(String bankCode, String signType);
}
