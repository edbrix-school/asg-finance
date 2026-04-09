package com.asg.finance.bankreconciliation.service;

import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.dto.*;
import com.asg.finance.repository.BankReconciliationRepository;
import com.asg.finance.service.impl.BankReconciliationServiceImpl;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankReconciliationServiceImplTest {

    @Mock
    private BankReconciliationRepository repository;
    @Mock
    private PrintService printService;
    @Mock
    private DataSource dataSource;
    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private BankReconciliationServiceImpl service;

    // ─── getReconciliationView ───────────────────────────────────────────────

    @Test
    void getReconciliationView_delegatesToRepository_andReturnsResult() {
        BankReconciliationResponse response = new BankReconciliationResponse();
        when(repository.callReconcileView(1L, 2L, 3L,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
                "CHQ001", "Y", "TYPE1"))
                .thenReturn(List.of(response));

        List<BankReconciliationResponse> result = service.getReconciliationView(
                1L, 2L, 3L,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
                "CHQ001", "Y", "TYPE1");

        assertThat(result).containsExactly(response);
        verify(repository).callReconcileView(1L, 2L, 3L,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
                "CHQ001", "Y", "TYPE1");
    }

    @Test
    void getReconciliationView_returnsEmptyList_whenRepositoryReturnsEmpty() {
        when(repository.callReconcileView(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<BankReconciliationResponse> result = service.getReconciliationView(
                1L, 2L, 3L,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
                null, null, null);

        assertThat(result).isEmpty();
    }

    // ─── getBankInfo ─────────────────────────────────────────────────────────

    @Test
    void getBankInfo_delegatesToRepository_andReturnsResult() {
        BankRenconciliationBankInfoDTO dto = new BankRenconciliationBankInfoDTO();
        dto.setBank("HSBC");
        dto.setCompany("ASG");
        when(repository.getBankPoid(101L)).thenReturn(dto);

        BankRenconciliationBankInfoDTO result = service.getBankInfo(101L);

        assertThat(result).isEqualTo(dto);
        assertThat(result.getBank()).isEqualTo("HSBC");
        verify(repository).getBankPoid(101L);
    }

    @Test
    void getBankInfo_returnsNull_whenRepositoryReturnsNull() {
        when(repository.getBankPoid(anyLong())).thenReturn(null);

        BankRenconciliationBankInfoDTO result = service.getBankInfo(999L);

        assertThat(result).isNull();
    }

    // ─── saveReconciliation ──────────────────────────────────────────────────

    @Test
    void saveReconciliation_success_logsEachRequest() {
        BankReconciliationRequest req = new BankReconciliationRequest();
        req.setTransactionPoid(500L);
        List<BankReconciliationRequest> requests = List.of(req);

        when(repository.saveReconciliation(requests)).thenReturn("SUCCESS");

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getDocumentId).thenReturn("400-150");

            String result = service.saveReconciliation(requests);

            assertThat(result).isEqualTo("SUCCESS");
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "400-150", "500");
        }
    }

    @Test
    void saveReconciliation_success_multipleRequests_logsAll() {
        BankReconciliationRequest req1 = new BankReconciliationRequest();
        req1.setTransactionPoid(100L);
        BankReconciliationRequest req2 = new BankReconciliationRequest();
        req2.setTransactionPoid(200L);
        List<BankReconciliationRequest> requests = List.of(req1, req2);

        when(repository.saveReconciliation(requests)).thenReturn("SUCCESS");

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getDocumentId).thenReturn("400-150");

            String result = service.saveReconciliation(requests);

            assertThat(result).isEqualTo("SUCCESS");
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "400-150", "100");
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "400-150", "200");
        }
    }

    @Test
    void saveReconciliation_errorResult_doesNotLog() {
        BankReconciliationRequest req = new BankReconciliationRequest();
        req.setTransactionPoid(500L);
        List<BankReconciliationRequest> requests = List.of(req);

        when(repository.saveReconciliation(requests)).thenReturn("error: something went wrong");

        String result = service.saveReconciliation(requests);

        assertThat(result).isEqualTo("error: something went wrong");
        verifyNoInteractions(loggingService);
    }

    @Test
    void saveReconciliation_errorResultCaseInsensitive_doesNotLog() {
        BankReconciliationRequest req = new BankReconciliationRequest();
        req.setTransactionPoid(500L);
        List<BankReconciliationRequest> requests = List.of(req);

        when(repository.saveReconciliation(requests)).thenReturn("ERROR: DB failure");

        String result = service.saveReconciliation(requests);

        assertThat(result).isEqualTo("ERROR: DB failure");
        verifyNoInteractions(loggingService);
    }

    @Test
    void saveReconciliation_nullResult_doesNotLog() {
        BankReconciliationRequest req = new BankReconciliationRequest();
        req.setTransactionPoid(500L);
        List<BankReconciliationRequest> requests = List.of(req);

        when(repository.saveReconciliation(requests)).thenReturn(null);

        String result = service.saveReconciliation(requests);

        assertThat(result).isNull();
        verifyNoInteractions(loggingService);
    }

    @Test
    void saveReconciliation_nullTransactionPoid_logsUnknown() {
        BankReconciliationRequest req = new BankReconciliationRequest();
        req.setTransactionPoid(null);
        List<BankReconciliationRequest> requests = List.of(req);

        when(repository.saveReconciliation(requests)).thenReturn("SUCCESS");

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getDocumentId).thenReturn("400-150");

            String result = service.saveReconciliation(requests);

            assertThat(result).isEqualTo("SUCCESS");
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "400-150", "unknown");
        }
    }

    // ─── holdCheque ──────────────────────────────────────────────────────────

    @Test
    void holdCheque_delegatesToRepository_andReturnsResult() {
        List<BankReconcHoldAndUholdRequest> req = List.of(new BankReconcHoldAndUholdRequest());
        when(repository.holdCheque(req)).thenReturn("SUCCESS: Held");

        String result = service.holdCheque(req);

        assertThat(result).isEqualTo("SUCCESS: Held");
        verify(repository).holdCheque(req);
    }

    @Test
    void holdCheque_returnsError_whenRepositoryReturnsError() {
        List<BankReconcHoldAndUholdRequest> req = List.of(new BankReconcHoldAndUholdRequest());
        when(repository.holdCheque(req)).thenReturn("Error: Hold failed");

        String result = service.holdCheque(req);

        assertThat(result).isEqualTo("Error: Hold failed");
    }

    // ─── unholdCheque ────────────────────────────────────────────────────────

    @Test
    void unholdCheque_delegatesToRepository_andReturnsResult() {
        List<BankReconcHoldAndUholdRequest> req = List.of(new BankReconcHoldAndUholdRequest());
        when(repository.unholdCheque(req)).thenReturn("SUCCESS: Unheld");

        String result = service.unholdCheque(req);

        assertThat(result).isEqualTo("SUCCESS: Unheld");
        verify(repository).unholdCheque(req);
    }

    @Test
    void unholdCheque_returnsError_whenRepositoryReturnsError() {
        List<BankReconcHoldAndUholdRequest> req = List.of(new BankReconcHoldAndUholdRequest());
        when(repository.unholdCheque(req)).thenReturn("Error: Unhold failed");

        String result = service.unholdCheque(req);

        assertThat(result).isEqualTo("Error: Unhold failed");
    }

    // ─── updateStatementDate ─────────────────────────────────────────────────

    @Test
    void updateStatementDate_delegatesToRepository_andReturnsResult() {
        LocalDate statementDate = LocalDate.of(2025, 6, 30);
        when(repository.updateStatementDate(1L, 1001L, 101L, statementDate))
                .thenReturn("SUCCESS: Statement updated");

        String result = service.updateStatementDate(1L, 1001L, 101L, statementDate);

        assertThat(result).isEqualTo("SUCCESS: Statement updated");
        verify(repository).updateStatementDate(1L, 1001L, 101L, statementDate);
    }

    @Test
    void updateStatementDate_returnsError_whenRepositoryReturnsError() {
        LocalDate statementDate = LocalDate.of(2025, 6, 30);
        when(repository.updateStatementDate(any(), any(), any(), any()))
                .thenReturn("ERROR: Update failed");

        String result = service.updateStatementDate(1L, 1001L, 101L, statementDate);

        assertThat(result).isEqualTo("ERROR: Update failed");
    }

    // ─── pollAutoRefresh ─────────────────────────────────────────────────────

    @Test
    void pollAutoRefresh_delegatesToRepository_andReturnsResult() {
        when(repository.pollAutoRefresh("user1", 101L, "http://login")).thenReturn("OK");

        String result = service.pollAutoRefresh("user1", 101L, "http://login");

        assertThat(result).isEqualTo("OK");
        verify(repository).pollAutoRefresh("user1", 101L, "http://login");
    }

    @Test
    void pollAutoRefresh_returnsError_whenRepositoryReturnsError() {
        when(repository.pollAutoRefresh(any(), any(), any())).thenReturn("Error: poll failed");

        String result = service.pollAutoRefresh("user1", 101L, "http://login");

        assertThat(result).isEqualTo("Error: poll failed");
    }

    // ─── revertReconciliation ────────────────────────────────────────────────

    @Test
    void revertReconciliation_delegatesToRepository_andReturnsResult() {
        when(repository.revertReconciliation("400-150", "5001", 1001L, 1L, 101L, "Y"))
                .thenReturn("SUCCESS: Reverted");

        String result = service.revertReconciliation("400-150", "5001", 1001L, 1L, 101L, "Y");

        assertThat(result).isEqualTo("SUCCESS: Reverted");
        verify(repository).revertReconciliation("400-150", "5001", 1001L, 1L, 101L, "Y");
    }

    @Test
    void revertReconciliation_returnsError_whenRepositoryReturnsError() {
        when(repository.revertReconciliation(any(), any(), any(), any(), any(), any()))
                .thenReturn("Error: Revert failed");

        String result = service.revertReconciliation("400-150", "5001", 1001L, 1L, 101L, "N");

        assertThat(result).isEqualTo("Error: Revert failed");
    }

    // ─── fetchReport ─────────────────────────────────────────────────────────

    @Test
    void fetchReport_delegatesToRepository_andReturnsResult() {
        BankReconcileReportRequest request = new BankReconcileReportRequest();
        request.setGroupPoid(1L);
        request.setCompanyPoid(2L);
        BankReconcileReportResponse response = new BankReconcileReportResponse();
        response.setOpeningBalance("1000.00");
        when(repository.getBankReconcileReport(request)).thenReturn(response);

        BankReconcileReportResponse result = service.fetchReport(request);

        assertThat(result).isEqualTo(response);
        assertThat(result.getOpeningBalance()).isEqualTo("1000.00");
        verify(repository).getBankReconcileReport(request);
    }

    @Test
    void fetchReport_returnsNull_whenRepositoryReturnsNull() {
        BankReconcileReportRequest request = new BankReconcileReportRequest();
        when(repository.getBankReconcileReport(request)).thenReturn(null);

        BankReconcileReportResponse result = service.fetchReport(request);

        assertThat(result).isNull();
    }

    // ─── print ───────────────────────────────────────────────────────────────

    @Test
    void print_buildsParamsAndReturnsPdf() throws Exception {
        Long transactionPoid = 1L;
        Long bankPoid = 101L;
        LocalDate dateFrom = LocalDate.of(2025, 1, 1);
        LocalDate dateTill = LocalDate.of(2025, 1, 31);
        String balanceAsPerBank = "50000.00";
        byte[] pdfBytes = "pdf-content".getBytes();

        JasperReport mainReport = mock(JasperReport.class);
        JasperReport subReport1 = mock(JasperReport.class);
        JasperReport subReport2 = mock(JasperReport.class);
        JasperReport subReport3 = mock(JasperReport.class);

        when(printService.buildBaseParams(transactionPoid, "400-150")).thenReturn(new java.util.HashMap<>());
        when(printService.load("Finance/GL/BankReconciliationSubreport1.jrxml")).thenReturn(subReport1);
        when(printService.load("Finance/GL/BankReconciliationSubreport2.jrxml")).thenReturn(subReport2);
        when(printService.load("Finance/GL/BankReconciliationSubreport3.jrxml")).thenReturn(subReport3);
        when(printService.load("Finance/GL/BankReconciliationReport.jrxml")).thenReturn(mainReport);
        when(printService.fillReportToPdf(eq(mainReport), any(Map.class), eq(dataSource))).thenReturn(pdfBytes);

        byte[] result = service.print(transactionPoid, bankPoid, dateFrom, dateTill, balanceAsPerBank);

        assertThat(result).isEqualTo(pdfBytes);
        verify(printService).buildBaseParams(transactionPoid, "400-150");
        verify(printService).load("Finance/GL/BankReconciliationSubreport1.jrxml");
        verify(printService).load("Finance/GL/BankReconciliationSubreport2.jrxml");
        verify(printService).load("Finance/GL/BankReconciliationSubreport3.jrxml");
        verify(printService).load("Finance/GL/BankReconciliationReport.jrxml");
        verify(printService).fillReportToPdf(eq(mainReport), any(Map.class), eq(dataSource));
    }

    @Test
    void print_formatsDateParams_correctly() throws Exception {
        LocalDate dateFrom = LocalDate.of(2025, 3, 5);
        LocalDate dateTill = LocalDate.of(2025, 3, 31);
        java.util.Map<String, Object> capturedParams = new java.util.HashMap<>();

        JasperReport mainReport = mock(JasperReport.class);
        when(printService.buildBaseParams(any(), any())).thenReturn(capturedParams);
        when(printService.load(anyString())).thenReturn(mainReport);
        when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[0]);

        service.print(1L, 101L, dateFrom, dateTill, "1000.00");

        assertThat(capturedParams.get("DOC_FROM_DATE")).isEqualTo("05-Mar-2025");
        assertThat(capturedParams.get("DOC_TO_DATE")).isEqualTo("31-Mar-2025");
        assertThat(capturedParams.get("BANK_POID")).isEqualTo(101L);
        assertThat(capturedParams.get("BALANCE_BANK")).isEqualTo("1000.00");
    }

    @Test
    void print_throwsException_whenPrintServiceFails() throws Exception {
        JasperReport mainReport = mock(JasperReport.class);
        when(printService.buildBaseParams(any(), any())).thenReturn(new java.util.HashMap<>());
        when(printService.load(anyString())).thenReturn(mainReport);
        when(printService.fillReportToPdf(any(), any(), any()))
                .thenThrow(new RuntimeException("PDF generation failed"));

        assertThatThrownBy(() -> service.print(1L, 101L,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), "1000.00"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("PDF generation failed");
    }
}
