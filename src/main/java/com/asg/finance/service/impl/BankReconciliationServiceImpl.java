package com.asg.finance.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.entity.BankReconciliation;
import com.asg.finance.service.BankReconciliationService;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Service;

import com.asg.finance.dto.BankReconcHoldAndUholdRequest;
import com.asg.finance.dto.BankReconcileReportRequest;
import com.asg.finance.dto.BankReconcileReportResponse;
import com.asg.finance.dto.BankReconciliationRequest;
import com.asg.finance.dto.BankReconciliationResponse;
import com.asg.finance.dto.BankRenconciliationBankInfoDTO;
import com.asg.finance.repository.BankReconciliationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankReconciliationServiceImpl implements BankReconciliationService {

	private final BankReconciliationRepository repository;
    private final PrintService printService;
    private final DataSource dataSource;
    private final LoggingService loggingService;

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
		String result = repository.saveReconciliation(dto);
		
		// Log the creation if successful
		if (result != null && !result.toLowerCase().startsWith("error")) {
			for (BankReconciliationRequest req : dto) {
				String key = req.getTransactionPoid() != null ? req.getTransactionPoid().toString() : "unknown";
				loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), key);
			}
		}
		
		return result;
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
    public String updateStatementDate(Long companyPoid,
                                      Long postedBy,
                                      Long bankPoid,
                                      Date statementDate) {

        return repository.updateStatementDate(
                companyPoid,
                postedBy,
                bankPoid,
                statementDate
        );
    }

    @Override
	public String pollAutoRefresh(String userId, Long companyPoid, String loginUrl) {
		String result = repository.pollAutoRefresh(userId, companyPoid, loginUrl);
		
		return result;
	}

	@Override
	public String revertReconciliation(String docId, String transactionPoid, Long loginUserPoid, Long loginGroupPoid,
			Long loginCompanyPoid, String mailAlert) {
		String result = repository.revertReconciliation(docId, transactionPoid, loginUserPoid, loginGroupPoid, loginCompanyPoid,
				mailAlert);
		
		// Log the modification if successful
		if (result != null && !result.toLowerCase().startsWith("error")) {
			loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, UserContext.getDocumentId(), transactionPoid);
			
			// Create objects for detailed logging
			Map<String, Object> oldData = Map.of("transactionPoid", transactionPoid, "status", "reconciled");
			Map<String, Object> newData = Map.of("transactionPoid", transactionPoid, "status", "reverted", "docId", docId, "loginUserPoid", loginUserPoid, "mailAlert", mailAlert);
			loggingService.logChanges(oldData, newData, Map.class, 
					UserContext.getDocumentId(), transactionPoid, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
		}
		
		return result;
	}

	@Override
	public BankReconcileReportResponse fetchReport(BankReconcileReportRequest request) {
		return repository.getBankReconcileReport(request);
	}

    @Override
    public byte[] print(Long transactionPoid, Long bankPoid, Date dateFrom, Date dateTill, String balanceAsPerBank) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "400-150");
        SimpleDateFormat format1 = new SimpleDateFormat("dd-MMM-yyyy");
        params.put("BANK_POID", bankPoid);
        params.put("DOC_FROM_DATE", format1.format(dateFrom));
        params.put("DOC_TO_DATE", format1.format(dateTill));
        params.put("BALANCE_BANK", balanceAsPerBank);
        params.put("SUBREPORT1", printService.load("Finance/GL/BankReconciliationSubreport1.jrxml"));
        params.put("SUBREPORT2", printService.load("Finance/GL/BankReconciliationSubreport2.jrxml"));
        params.put("SUBREPORT3", printService.load("Finance/GL/BankReconciliationSubreport3.jrxml"));
        JasperReport mainReport = printService.load("Finance/GL/BankReconciliationReport.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }
}
