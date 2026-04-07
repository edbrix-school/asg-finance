package com.asg.finance.service.impl;

import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.dto.ChequePrintBatchRequest;
import com.asg.finance.dto.ChequePrintBatchResponse;
import com.asg.finance.dto.ChequePrintPendingRequest;
import com.asg.finance.dto.ChequePrintStockRequest;
import com.asg.finance.dto.PendingChequeResponse;
import com.asg.finance.repository.ChequePrintingRepository;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChequePrintingServiceImplTest {

    @Mock
    private ChequePrintingRepository repo;

    @Mock
    private PrintService printService;

    @Mock
    private DataSource dataSource;

    @Test
    void getPendingCheques_DelegatesToRepository() {
        ChequePrintingServiceImpl service = new ChequePrintingServiceImpl(repo, printService, dataSource);
        when(repo.fetchPendingCheques()).thenReturn(List.of(new PendingChequeResponse()));

        List<PendingChequeResponse> result = service.getPendingCheques();

        assertEquals(1, result.size());
        verify(repo).fetchPendingCheques();
    }

    @Test
    void getChequeStock_DelegatesToRepository() {
        ChequePrintingServiceImpl service = new ChequePrintingServiceImpl(repo, printService, dataSource);
        when(repo.fetchChequeStock("HSBC", "NOT_SIGNED")).thenReturn(List.of());

        List<?> result = service.getChequeStock("HSBC", "NOT_SIGNED");

        assertEquals(0, result.size());
        verify(repo).fetchChequeStock("HSBC", "NOT_SIGNED");
    }

    @Test
    void print_ThrowsWhenStocksMissing() {
        ChequePrintingServiceImpl service = new ChequePrintingServiceImpl(repo, printService, dataSource);
        ChequePrintBatchRequest request = new ChequePrintBatchRequest();
        request.setPendingCheques(List.of());
        request.setStocks(List.of());

        assertThrows(ValidationException.class, () -> service.print(request));
    }

    @Test
    void print_ThrowsWhenNoChequeSelected() {
        ChequePrintingServiceImpl service = new ChequePrintingServiceImpl(repo, printService, dataSource);
        ChequePrintBatchRequest request = new ChequePrintBatchRequest();

        ChequePrintStockRequest stock = new ChequePrintStockRequest();
        stock.setCompanyPoid(1L);
        stock.setBankPoid(2L);
        stock.setStockType("NOT_SIGNED");
        stock.setSelected(true);
        request.setStocks(List.of(stock));

        ChequePrintPendingRequest pending = new ChequePrintPendingRequest();
        pending.setTransactionPoid(10L);
        pending.setCompanyPoid(1L);
        pending.setSelected(false);
        request.setPendingCheques(List.of(pending));

        assertThrows(ValidationException.class, () -> service.print(request));
    }

    @Test
    void print_ThrowsWhenAnyStockUnselected() {
        ChequePrintingServiceImpl service = new ChequePrintingServiceImpl(repo, printService, dataSource);
        ChequePrintBatchRequest request = new ChequePrintBatchRequest();

        ChequePrintStockRequest stock1 = new ChequePrintStockRequest();
        stock1.setCompanyPoid(1L);
        stock1.setBankPoid(2L);
        stock1.setStockType("NOT_SIGNED");
        stock1.setSelected(true);
        ChequePrintStockRequest stock2 = new ChequePrintStockRequest();
        stock2.setCompanyPoid(2L);
        stock2.setBankPoid(3L);
        stock2.setStockType("NOT_SIGNED");
        stock2.setSelected(false);
        request.setStocks(List.of(stock1, stock2));

        ChequePrintPendingRequest pending = new ChequePrintPendingRequest();
        pending.setTransactionPoid(10L);
        pending.setCompanyPoid(1L);
        pending.setSelected(true);
        request.setPendingCheques(List.of(pending));

        assertThrows(ValidationException.class, () -> service.print(request));
    }

    @Test
    void print_ThrowsWhenNoMatchingStockForSelectedCheque() {
        ChequePrintingServiceImpl service = new ChequePrintingServiceImpl(repo, printService, dataSource);
        ChequePrintBatchRequest request = new ChequePrintBatchRequest();

        ChequePrintStockRequest stock = new ChequePrintStockRequest();
        stock.setCompanyPoid(2L);
        stock.setBankPoid(2L);
        stock.setStockType("NOT_SIGNED");
        stock.setSelected(true);
        request.setStocks(List.of(stock));

        ChequePrintPendingRequest pending = new ChequePrintPendingRequest();
        pending.setTransactionPoid(10L);
        pending.setCompanyPoid(1L);
        pending.setSelected(true);
        request.setPendingCheques(List.of(pending));

        assertThrows(ValidationException.class, () -> service.print(request));
    }

    @Test
    void print_Success_PrintsAndReturnsCounts() throws Exception {
        ChequePrintingServiceImpl service = new ChequePrintingServiceImpl(repo, printService, dataSource);
        ChequePrintBatchRequest request = new ChequePrintBatchRequest();
        request.setSuppressBalanceCheck("N");

        ChequePrintStockRequest stock = new ChequePrintStockRequest();
        stock.setCompanyPoid(1L);
        stock.setBankPoid(2L);
        stock.setStockType("NOT_SIGNED");
        stock.setSelected(true);
        request.setStocks(List.of(stock));

        ChequePrintPendingRequest pending = new ChequePrintPendingRequest();
        pending.setTransactionPoid(10L);
        pending.setCompanyPoid(1L);
        pending.setPvNo("PV-10");
        pending.setAccountPayee("Y");
        pending.setSelected(true);
        request.setPendingCheques(List.of(pending));

        JasperReport report = mock(JasperReport.class);
        when(repo.validateBeforeChequePrint(anyLong(), anyLong(), anyString(), anyLong(), anyString(), anyLong(), anyString()))
                .thenReturn(Map.of("result", "SUCCESS", "nextChequeNumber", "1001", "defaultPrinter", "ignored"));
        when(printService.buildBaseParams(10L, "400-151")).thenReturn(new java.util.HashMap<>());
        when(printService.load("Finance/BankPayments/BankPaymentVoucher.jrxml")).thenReturn(report);
        when(repo.afterChequePrint(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn("SUCCESS");

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserPoid).thenReturn(999L);
            userContext.when(UserContext::getUserName).thenReturn("tester");
            userContext.when(UserContext::getDocumentId).thenReturn("400-151");

            ChequePrintBatchResponse result = service.print(request);

            assertEquals(1, result.getRequestedCount());
            assertEquals(1, result.getPrintedCount());
            assertEquals(0, result.getSkippedCount());
            verify(printService).printReportToPrinter(eq(report), anyMap(), eq(dataSource), eq("ignored"), anyInt());
        }
    }

    @Test
    void print_ThrowsWhenAfterPrintReturnsError() throws Exception {
        ChequePrintingServiceImpl service = new ChequePrintingServiceImpl(repo, printService, dataSource);
        ChequePrintBatchRequest request = new ChequePrintBatchRequest();
        request.setSuppressBalanceCheck("N");

        ChequePrintStockRequest stock = new ChequePrintStockRequest();
        stock.setCompanyPoid(1L);
        stock.setBankPoid(2L);
        stock.setStockType("NOT_SIGNED");
        stock.setSelected(true);
        request.setStocks(List.of(stock));

        ChequePrintPendingRequest pending = new ChequePrintPendingRequest();
        pending.setTransactionPoid(11L);
        pending.setCompanyPoid(1L);
        pending.setPvNo("PV-11");
        pending.setAccountPayee("Y");
        pending.setSelected(true);
        request.setPendingCheques(List.of(pending));

        JasperReport report = mock(JasperReport.class);
        when(repo.validateBeforeChequePrint(anyLong(), anyLong(), anyString(), anyLong(), anyString(), anyLong(), anyString()))
                .thenReturn(Map.of("result", "SUCCESS", "nextChequeNumber", "1002", "defaultPrinter", "ignored"));
        when(printService.buildBaseParams(11L, "400-151")).thenReturn(new java.util.HashMap<>());
        when(printService.load("Finance/BankPayments/BankPaymentVoucher.jrxml")).thenReturn(report);
        when(repo.afterChequePrint(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn("ERROR : after print failure");

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserPoid).thenReturn(999L);
            userContext.when(UserContext::getUserName).thenReturn("tester");
            userContext.when(UserContext::getDocumentId).thenReturn("400-151");

            assertThrows(ValidationException.class, () -> service.print(request));
        }
    }

    @Test
    void print_ReturnsMessageWhenBeforeStatusIsInfo() throws Exception {
        ChequePrintingServiceImpl service = new ChequePrintingServiceImpl(repo, printService, dataSource);
        ChequePrintBatchRequest request = new ChequePrintBatchRequest();
        request.setSuppressBalanceCheck("N");

        ChequePrintStockRequest stock = new ChequePrintStockRequest();
        stock.setCompanyPoid(1L);
        stock.setBankPoid(2L);
        stock.setStockType("NOT_SIGNED");
        stock.setSelected(true);
        request.setStocks(List.of(stock));

        ChequePrintPendingRequest pending = new ChequePrintPendingRequest();
        pending.setTransactionPoid(12L);
        pending.setCompanyPoid(1L);
        pending.setPvNo("PV-12");
        pending.setAccountPayee("Y");
        pending.setSelected(true);
        request.setPendingCheques(List.of(pending));

        when(repo.validateBeforeChequePrint(anyLong(), anyLong(), anyString(), anyLong(), anyString(), anyLong(), anyString()))
                .thenReturn(Map.of("result", "INFO: already printed", "nextChequeNumber", "", "defaultPrinter", ""));

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserPoid).thenReturn(999L);
            userContext.when(UserContext::getUserName).thenReturn("tester");
            userContext.when(UserContext::getDocumentId).thenReturn("400-151");

            ChequePrintBatchResponse result = service.print(request);
            assertEquals(0, result.getPrintedCount());
            assertTrue(result.getMessages().getFirst().contains("INFO:"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ERROR : Insufficient balance.", "PENDING"})
    void print_ThrowsWhenBeforeStatusInvalid(String beforeStatus) {
        ChequePrintingServiceImpl service = new ChequePrintingServiceImpl(repo, printService, dataSource);
        ChequePrintBatchRequest request = new ChequePrintBatchRequest();
        request.setSuppressBalanceCheck("N");

        ChequePrintStockRequest stock = new ChequePrintStockRequest();
        stock.setCompanyPoid(1L);
        stock.setBankPoid(2L);
        stock.setStockType("NOT_SIGNED");
        stock.setSelected(true);
        request.setStocks(List.of(stock));

        ChequePrintPendingRequest pending = new ChequePrintPendingRequest();
        pending.setTransactionPoid(13L);
        pending.setCompanyPoid(1L);
        pending.setSelected(true);
        request.setPendingCheques(List.of(pending));

        when(repo.validateBeforeChequePrint(anyLong(), anyLong(), anyString(), anyLong(), anyString(), anyLong(), anyString()))
                .thenReturn(Map.of("result", beforeStatus));

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserPoid).thenReturn(999L);
            userContext.when(UserContext::getUserName).thenReturn("tester");
            userContext.when(UserContext::getDocumentId).thenReturn("400-151");

            assertThrows(ValidationException.class, () -> service.print(request));
        }
    }

    @Test
    void print_ReturnsMessageWhenAfterStatusInfo() throws Exception {
        ChequePrintingServiceImpl service = new ChequePrintingServiceImpl(repo, printService, dataSource);
        ChequePrintBatchRequest request = new ChequePrintBatchRequest();
        request.setSuppressBalanceCheck("N");

        ChequePrintStockRequest stock = new ChequePrintStockRequest();
        stock.setCompanyPoid(1L);
        stock.setBankPoid(2L);
        stock.setStockType("NOT_SIGNED");
        stock.setSelected(true);
        request.setStocks(List.of(stock));

        ChequePrintPendingRequest pending = new ChequePrintPendingRequest();
        pending.setTransactionPoid(16L);
        pending.setCompanyPoid(1L);
        pending.setPvNo("PV-16");
        pending.setAccountPayee("Y");
        pending.setSelected(true);
        request.setPendingCheques(List.of(pending));

        JasperReport report = mock(JasperReport.class);
        when(repo.validateBeforeChequePrint(anyLong(), anyLong(), anyString(), anyLong(), anyString(), anyLong(), anyString()))
                .thenReturn(Map.of("result", "SUCCESS", "nextChequeNumber", "1003", "defaultPrinter", "ignored"));
        when(printService.buildBaseParams(16L, "400-151")).thenReturn(new java.util.HashMap<>());
        when(printService.load("Finance/BankPayments/BankPaymentVoucher.jrxml")).thenReturn(report);
        when(repo.afterChequePrint(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn("INFO: printed with warning");

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserPoid).thenReturn(999L);
            userContext.when(UserContext::getUserName).thenReturn("tester");
            userContext.when(UserContext::getDocumentId).thenReturn("400-151");

            ChequePrintBatchResponse result = service.print(request);
            assertEquals(0, result.getPrintedCount());
            assertEquals(1, result.getMessages().size());
        }
    }
}

