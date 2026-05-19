package com.asg.finance.service.impl;

import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.dto.*;
import com.asg.finance.repository.BankReconciliationRepository;
import com.asg.finance.service.BankReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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
                                                                  LocalDate dateFrom, LocalDate dateTill, String chequeNo, String reconcileCheque, String brType) {
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
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), key, String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), req.getDocRef()));
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
                                      LocalDate statementDate) {

        return repository.updateStatementDate(
                companyPoid,
                postedBy,
                bankPoid,
                statementDate
        );
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

    @Override
    public byte[] print(Long transactionPoid, Long bankPoid, LocalDate dateFrom, LocalDate dateTill, String balanceAsPerBank) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "400-150");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        params.put("BANK_POID", bankPoid);
        params.put("DOC_FROM_DATE", dateFrom.format(formatter));
        params.put("DOC_TO_DATE", dateTill.format(formatter));
        params.put("BALANCE_BANK", balanceAsPerBank);
        params.put("SUBREPORT1", printService.load("Finance/GL/BankReconciliationSubreport1.jrxml"));
        params.put("SUBREPORT2", printService.load("Finance/GL/BankReconciliationSubreport2.jrxml"));
        params.put("SUBREPORT3", printService.load("Finance/GL/BankReconciliationSubreport3.jrxml"));
        JasperReport mainReport = printService.load("Finance/GL/BankReconciliationReport.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }
}
