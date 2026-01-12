package com.asg.finance.service.impl;

import java.util.List;

import com.asg.finance.service.ChequePrintingService;
import org.springframework.stereotype.Service;

import com.asg.finance.dto.ChequeStockResponse;
import com.asg.finance.dto.PendingChequeResponse;
import com.asg.finance.repository.ChequePrintingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChequePrintingServiceImpl implements ChequePrintingService {

	private final ChequePrintingRepository repo;

	@Override
	public List<PendingChequeResponse> getPendingCheques() {
		return repo.fetchPendingCheques();
	}

	@Override
	public List<ChequeStockResponse> getChequeStock(String bankCode, String signType) {
		return repo.fetchChequeStock(bankCode, signType);
	}

}
