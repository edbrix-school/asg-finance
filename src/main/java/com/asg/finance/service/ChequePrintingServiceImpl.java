package com.asg.finance.service;

import java.util.List;

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
