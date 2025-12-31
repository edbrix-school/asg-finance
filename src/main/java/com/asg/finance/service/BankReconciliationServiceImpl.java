package com.asg.finance.service;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.asg.finance.dto.BankReconcHoldAndUholdRequest;
import com.asg.finance.dto.BankReconcileReportRequest;
import com.asg.finance.dto.BankReconcileReportResponse;
import com.asg.finance.dto.BankReconciliationRequest;
import com.asg.finance.dto.BankReconciliationResponse;
import com.asg.finance.dto.BankRenconciliationBankInfoDTO;
import com.asg.finance.repository.BankReconciliationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BankReconciliationServiceImpl implements BankReconciliationService {

	private final BankReconciliationRepository repository;

	@Override
	public List<BankReconciliationResponse> getReconciliationView(Long groupPoid, Long companyPoid, Long bankPoid,
			Date dateFrom, Date dateTill, String chequeNo, String reconcileCheque, String brType) {
		return repository.callReconcileView(groupPoid, companyPoid, bankPoid, dateFrom, dateTill, chequeNo,
				reconcileCheque, brType);
	}

	@Override
	public BankRenconciliationBankInfoDTO getBankInfo(Long glPoid) {
		return repository.getBankPoid(glPoid);
	}

	@Override
	public String saveReconciliation(List<BankReconciliationRequest> dto) {
		return repository.saveReconciliation(dto);
	}

	@Override
	public String holdCheque(List<BankReconcHoldAndUholdRequest> req) {
		return repository.holdCheque(req);
	}
	

	@Override
	public String unholdCheque(List<BankReconcHoldAndUholdRequest> req) {
		return repository.unholdCheque(req);
	}

	@Override
	public String updateStatementDate(Long companyPoid, Long postedBy, Long bankPoid, Date statementDate) {
		return repository.updateStatementDate(companyPoid, postedBy, bankPoid, statementDate);
	}

	@Override
	public String pollAutoRefresh(String userId, Long companyPoid, String loginUrl) {
		return repository.pollAutoRefresh(userId, companyPoid, loginUrl);
	}

	@Override
	public String revertReconciliation(String docId, String transactionPoid, Long loginUserPoid, Long loginGroupPoid,
			Long loginCompanyPoid, String mailAlert) {
		return repository.revertReconciliation(docId, transactionPoid, loginUserPoid, loginGroupPoid, loginCompanyPoid,
				mailAlert);
	}

	@Override
	public BankReconcileReportResponse fetchReport(BankReconcileReportRequest request) {
		return repository.getBankReconcileReport(request);
	}
}
