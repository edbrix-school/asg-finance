package com.asg.finance.repository;

import com.asg.finance.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface BankReconciliationRepository {

    List<BankReconciliationResponse> callReconcileView(Long groupPoid, Long companyPoid, Long bankPoid, LocalDate dateFrom,
                                                       LocalDate dateTill, String chequeNo, String reconcileCheque, String brType);

    BankRenconciliationBankInfoDTO getBankPoid(Long glPoid);

    String saveReconciliation(List<BankReconciliationRequest> req);

    String holdCheque(List<BankReconcHoldAndUholdRequest> req);

    String unholdCheque(List<BankReconcHoldAndUholdRequest> req);

    String updateStatementDate(Long companyPoid, Long postedBy, Long bankPoid, LocalDate statementDate);

    String pollAutoRefresh(String userId, Long companyPoid, String loginUrl);

    String revertReconciliation(String docId, String transactionPoid, Long loginUserPoid, Long loginGroupPoid,
                                Long loginCompanyPoid, String mailAlert);

    BankReconcileReportResponse getBankReconcileReport(BankReconcileReportRequest req);

}
