package com.asg.finance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.asg.finance.dto.BankReconcHoldAndUholdRequest;
import com.asg.finance.dto.BankReconcileReportRequest;
import com.asg.finance.dto.BankReconcileReportResponse;
import com.asg.finance.dto.BankReconciliationRequest;
import com.asg.finance.dto.BankReconciliationResponse;
import com.asg.finance.dto.BankRenconciliationBankInfoDTO;

public interface BankReconciliationService {

	List<BankReconciliationResponse> getReconciliationView(Long groupPoid, Long companyPoid, Long bankPoid,
			LocalDate dateFrom, LocalDate dateTill, String chequeNo, String reconcileCheque, String brType);

	BankRenconciliationBankInfoDTO getBankInfo(Long glPoid);

	String saveReconciliation(List<BankReconciliationRequest> dto);

	String holdCheque(List<BankReconcHoldAndUholdRequest> req);

	String unholdCheque(List<BankReconcHoldAndUholdRequest> req);

	String updateStatementDate(Long companyPoid, Long postedBy, Long bankPoid, LocalDate statementDate);

	String pollAutoRefresh(String userId, Long companyPoid, String loginUrl);

	String revertReconciliation(String docId, String transactionPoid, Long loginUserPoid, Long loginGroupPoid,
			Long loginCompanyPoid, String mailAlert);

	BankReconcileReportResponse fetchReport(BankReconcileReportRequest request);

    byte[] print(Long transactionPoid, Long bankPoid, LocalDate dateFrom, LocalDate dateTill, String balanceAsPerBank) throws Exception;
}
