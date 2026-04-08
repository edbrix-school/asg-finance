package com.asg.finance.bankreconciliation.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GlBankEntity;
import com.asg.finance.repository.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

@ExtendWith(MockitoExtension.class)
class BankReconciliationRepositoryImplTest {

    @Mock
    private EntityManager em;

    @Mock
    private GlBankRepository bankRepository;

    @Mock
    private StoredProcedureQuery sp;

    @Mock
    private Session session;

    @Mock
    private Connection connection;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private BankReconciliationRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(repository, "em", em);
        lenient().when(em.createStoredProcedureQuery(anyString())).thenReturn(sp);
        lenient().when(em.unwrap(Session.class)).thenReturn(session);
    }
    
    @Test
    void testCallReconcileView_Success() {
        // Arrange
        Long groupPoid = 1L;
        Long companyPoid = 2L;
        Long bankPoid = 3L;
        LocalDate dateFrom = LocalDate.of(2023, 1, 1);
        LocalDate dateTill = LocalDate.of(2023, 12, 31);
        String chequeNo = "CHQ001";
        String reconcileCheque = "Y";
        String brType = "MANUAL";
        
        Object[] row = {1L, 2L, "DOC001", "DOC001_1", 100L, Date.valueOf("2023-06-15"),
                       "REF001", "CHQ001", 1L, "Test narration", 2L, 3L, 
                       new BigDecimal("1000.00"), new BigDecimal("500.00")};
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);
        
        when(sp.getResultList()).thenReturn(rows);
        
        // Act
        List<BankReconciliationResponse> result = repository.callReconcileView(
            groupPoid, companyPoid, bankPoid, dateFrom, dateTill, chequeNo, reconcileCheque, brType);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        BankReconciliationResponse response = result.get(0);
        assertEquals(1L, response.getTransactionGroupPoid());
        assertEquals(2L, response.getTransactionCompanyPoid());
        assertEquals("DOC001", response.getDocId());
        
        verify(sp, times(8)).registerStoredProcedureParameter(anyString(), any(Class.class), eq(ParameterMode.IN));
        verify(sp).execute();
    }
    
    @Test
    void testCallReconcileView_WithNullDates() {
        // Arrange
        Object[] row = {null, null, null, null, null, null, null, null, null, null, null, null, null, null};
        List<Object[]> rows2 = new ArrayList<>();
        rows2.add(row);
        when(sp.getResultList()).thenReturn(rows2);
        
        // Act
        List<BankReconciliationResponse> result = repository.callReconcileView(
            1L, 2L, 3L, null, null, "CHQ001", "Y", "MANUAL");
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testGetBankPoid_Success() {
        // Arrange
        Long glPoid = 1L;
        when(sp.getOutputParameterValue("P_BANK")).thenReturn("Test Bank");
        when(sp.getOutputParameterValue("P_COMPANY")).thenReturn("Test Company");
        
        // Act
        BankRenconciliationBankInfoDTO result = repository.getBankPoid(glPoid);
        
        // Assert
        assertNotNull(result);
        assertEquals("Test Bank", result.getBank());
        assertEquals("Test Company", result.getCompany());
        
        verify(sp).registerStoredProcedureParameter("P_GL_POID", Long.class, ParameterMode.IN);
        verify(sp).execute();
    }
    
    @Test
    void testSaveReconciliation_Success() throws Exception {
        // Arrange
        BankReconciliationRequest request = createBankReconciliationRequest();
        List<BankReconciliationRequest> requests = Arrays.asList(request);
        
        when(session.doReturningWork(any())).thenReturn(true); // existsByTransactionPoid
        when(sp.getOutputParameterValue("P_RESULT")).thenReturn("Success");
        
        // Act
        String result = repository.saveReconciliation(requests);
        
        // Assert
        assertEquals("Successfully Updated.", result);
        verify(sp).execute();
    }
    
    @Test
    void testSaveReconciliation_NullRequest() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> repository.saveReconciliation(null));
        assertThrows(ValidationException.class, () -> repository.saveReconciliation(Collections.emptyList()));
    }
    
    @Test
    void testSaveReconciliation_TransactionNotExists() throws Exception {
        // Arrange
        BankReconciliationRequest request = createBankReconciliationRequest();
        List<BankReconciliationRequest> requests = Arrays.asList(request);
        
        when(session.doReturningWork(any())).thenReturn(false); // existsByTransactionPoid returns false
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> repository.saveReconciliation(requests));
    }
    
    @Test
    void testSaveReconciliation_DocRefNotExists() throws Exception {
        // Arrange
        BankReconciliationRequest request = createBankReconciliationRequest();
        List<BankReconciliationRequest> requests = Arrays.asList(request);
        
        when(session.doReturningWork(any()))
            .thenReturn(true)  // existsByTransactionPoid returns true
            .thenReturn(false); // existsByDocref returns false
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> repository.saveReconciliation(requests));
    }
    
    @Test
    void testSaveReconciliation_ErrorResult() throws Exception {
        // Arrange
        BankReconciliationRequest request = createBankReconciliationRequest();
        List<BankReconciliationRequest> requests = Arrays.asList(request);
        
        when(session.doReturningWork(any())).thenReturn(true);
        when(sp.getOutputParameterValue("P_RESULT")).thenReturn("Error: Something went wrong");
        
        // Act
        String result = repository.saveReconciliation(requests);
        
        // Assert
        assertEquals("Error: Something went wrong", result);
    }
    
    @Test
    void testHoldCheque_Success() throws Exception {
        // Arrange
        BankReconcHoldAndUholdRequest request = createHoldUnholdRequest();
        List<BankReconcHoldAndUholdRequest> requests = Arrays.asList(request);
        
        when(session.doReturningWork(any())).thenReturn(true);
        when(sp.getOutputParameterValue("P_RESULT")).thenReturn("Success");
        
        // Act
        String result = repository.holdCheque(requests);
        
        // Assert
        assertEquals("1 cheques hold successfully", result);
        verify(sp).execute();
    }
    
    @Test
    void testHoldCheque_TransactionNotExists() throws Exception {
        // Arrange
        BankReconcHoldAndUholdRequest request = createHoldUnholdRequest();
        List<BankReconcHoldAndUholdRequest> requests = Arrays.asList(request);
        
        when(session.doReturningWork(any())).thenReturn(false);
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> repository.holdCheque(requests));
    }
    
    @Test
    void testHoldCheque_ErrorResult() throws Exception {
        // Arrange
        BankReconcHoldAndUholdRequest request = createHoldUnholdRequest();
        List<BankReconcHoldAndUholdRequest> requests = Arrays.asList(request);
        
        when(session.doReturningWork(any())).thenReturn(true);
        when(sp.getOutputParameterValue("P_RESULT")).thenReturn("Error: Hold failed");
        
        // Act
        String result = repository.holdCheque(requests);
        
        // Assert
        assertEquals("Error: Hold failed", result);
    }
    
    @Test
    void testUnholdCheque_Success() throws Exception {
        // Arrange
        BankReconcHoldAndUholdRequest request = createHoldUnholdRequest();
        List<BankReconcHoldAndUholdRequest> requests = Arrays.asList(request);
        
        when(session.doReturningWork(any())).thenAnswer(invocation -> {
            // Mock the doReturningWork behavior for unhold
            return "2 cheques unheld successfully";
        });
        
        // Act
        String result = repository.unholdCheque(requests);
        
        // Assert
        assertEquals("2 cheques unheld successfully", result);
    }
    
    @Test
    void testUnholdCheque_EmptyList() {
        // Act
        String result = repository.unholdCheque(Collections.emptyList());
        
        // Assert
        assertEquals("No cheques to unhold", result);
    }
    
    @Test
    void testUnholdCheque_NullList() {
        // Act
        String result = repository.unholdCheque(null);
        
        // Assert
        assertEquals("No cheques to unhold", result);
    }
    
    @Test
    void testUpdateStatementDate_Success() {
        // Arrange
        Long companyPoid = 1L;
        Long postedBy = 2L;
        Long bankPoid = 3L;
        LocalDate statementDate = LocalDate.of(2023, 12, 31);
        
        GlBankEntity bankEntity = new GlBankEntity();
        when(bankRepository.findByBankPoid(bankPoid)).thenReturn(bankEntity);
        when(sp.getOutputParameterValue(5)).thenReturn("Success");
        
        // Act
        String result = repository.updateStatementDate(companyPoid, postedBy, bankPoid, statementDate);
        
        // Assert
        assertEquals("Success", result);
        verify(sp).execute();
    }
    
    @Test
    void testUpdateStatementDate_BankNotExists() {
        // Arrange
        Long bankPoid = 3L;
        when(bankRepository.findByBankPoid(bankPoid)).thenReturn(null);
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
            () -> repository.updateStatementDate(1L, 2L, bankPoid, LocalDate.now()));
    }
    
    @Test
    void testUpdateStatementDate_NullDate() {
        // Arrange
        GlBankEntity bankEntity = new GlBankEntity();
        when(bankRepository.findByBankPoid(3L)).thenReturn(bankEntity);
        when(sp.getOutputParameterValue(5)).thenReturn("Success");
        
        // Act
        String result = repository.updateStatementDate(1L, 2L, 3L, null);
        
        // Assert
        assertEquals("Success", result);
    }
    
    @Test
    void testPollAutoRefresh_Success() {
        // Arrange
        String userId = "user123";
        Long companyPoid = 1L;
        String loginUrl = "http://localhost:8080";
        
        // Act
        String result = repository.pollAutoRefresh(userId, companyPoid, loginUrl);
        
        // Assert
        assertEquals("SUCCESS", result);
        verify(sp).execute();
    }
    
    @Test
    void testRevertReconciliation_Success() {
        // Arrange
        String docId = "DOC001";
        String transactionPoid = "100";
        Long loginUserPoid = 1L;
        Long loginGroupPoid = 2L;
        Long loginCompanyPoid = 3L;
        String mailAlert = "Y";
        
        when(sp.getOutputParameterValue("P_STATUS")).thenReturn("Success");
        
        // Act
        String result = repository.revertReconciliation(docId, transactionPoid, 
            loginUserPoid, loginGroupPoid, loginCompanyPoid, mailAlert);
        
        // Assert
        assertEquals("Success", result);
        verify(sp).execute();
    }
    
    @Test
    void testRevertReconciliation_NullMailAlert() {
        // Arrange
        when(sp.getOutputParameterValue("P_STATUS")).thenReturn("Success");
        
        // Act
        String result = repository.revertReconciliation("DOC001", "100", 1L, 2L, 3L, null);
        
        // Assert
        assertEquals("Success", result);
        verify(sp).setParameter("P_MAIL_ALERT", "Y");
    }
    
    @Test
    void testGetBankReconcileReport_Success() {
        // Arrange
        BankReconcileReportRequest request = createReportRequest();
        
        Object[] row = {1L, 2L, "DOC001", 100L, Date.valueOf("2023-06-15"), 
                       "REF001", "CHQ001", 1L, "Test narration", 2L, 3L, 
                       new BigDecimal("1000.00"), new BigDecimal("500.00"), 
                       Date.valueOf("2023-06-20"), "CLEARED", "DOC001_1"};
        List<Object[]> reportRows = new ArrayList<>();
        reportRows.add(row);
        
        when(sp.getResultList()).thenReturn(reportRows);
        when(sp.getOutputParameterValue("OUTDATA1")).thenReturn("10000.00");
        when(sp.getOutputParameterValue("OUTDATA2")).thenReturn("15000.00");
        when(sp.getOutputParameterValue("OUTDATA3")).thenReturn("5000.00");
        when(sp.getOutputParameterValue("OUTDATA4")).thenReturn("3000.00");
        when(sp.getOutputParameterValue("OUTDATA5")).thenReturn("2000.00");
        when(sp.getOutputParameterValue("OUTDATA6")).thenReturn("1000.00");
        when(sp.getOutputParameterValue("OUTDATA7")).thenReturn("500.00");
        
        // Act
        BankReconcileReportResponse result = repository.getBankReconcileReport(request);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getReportData().size());
        assertEquals("10000.00", result.getOpeningBalance());
        assertEquals("15000.00", result.getClosingBalance());
        verify(sp).execute();
    }
    
    @Test
    void testGetBankReconcileReport_WithChequeTypeAndFilter() {
        // Arrange
        BankReconcileReportRequest request = createReportRequest();
        request.setChequeType("ISSUED");
        request.setChequeFilter("CLEARED");
        
        Object[] row = {1L, 2L, "DOC001", 100L, Date.valueOf("2023-06-15"), 
                       "REF001", "CHQ001", 1L, "Test narration", 2L, 3L, 
                       new BigDecimal("1000.00"), Date.valueOf("2023-06-20"), "CLEARED", "DOC001_1"};
        List<Object[]> filteredRows = new ArrayList<>();
        filteredRows.add(row);
        when(sp.getResultList()).thenReturn(filteredRows);
        
        // Act
        BankReconcileReportResponse result = repository.getBankReconcileReport(request);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getReportData().size());
    }
    
    @Test
    void testMapRowToDtoView_AllFieldsNull() {
        // Arrange
        Object[] row = new Object[14];
        List<Object[]> rows3 = new ArrayList<>();
        rows3.add(row);
        when(sp.getResultList()).thenReturn(rows3);
        
        // Act
        List<BankReconciliationResponse> result = repository.callReconcileView(
            1L, 2L, 3L, null, null, null, null, null);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        BankReconciliationResponse response = result.get(0);
        assertNull(response.getTransactionGroupPoid());
        assertNull(response.getDocId());
    }
    
    @Test
    void testToLong_VariousTypes() {
        // This tests the private toLong method through public methods
        Object[] row = {
            new BigDecimal("123"),  // BigDecimal
            456,                    // Integer
            "789",                 // String
            "  ",                  // Blank string
            null                    // null
        };
        
        List<Object[]> rows4 = new ArrayList<>();
        rows4.add(row);
        when(sp.getResultList()).thenReturn(rows4);
        
        // Act
        List<BankReconciliationResponse> result = repository.callReconcileView(
            1L, 2L, 3L, null, null, null, null, null);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testGetBigDecimal_VariousTypes() {
        // index 12 = crAmt (BigDecimal), index 13 = drAmt (Double as Number)
        Object[] row = {null, null, null, null, null, null, null, null, null, null, null, null,
            new BigDecimal("100.50"),  // crAmt at index 12
            123.45                     // drAmt at index 13
        };
        
        List<Object[]> rows5 = new ArrayList<>();
        rows5.add(row);
        when(sp.getResultList()).thenReturn(rows5);
        
        // Act
        List<BankReconciliationResponse> result = repository.callReconcileView(
            1L, 2L, 3L, null, null, null, null, null);
        
        // Assert
        assertNotNull(result);
        BankReconciliationResponse response = result.get(0);
        assertEquals(new BigDecimal("100.50"), response.getCrAmt());
        assertEquals(BigDecimal.valueOf(123.45), response.getDrAmt());
    }
    
    @Test
    void testGetDate_VariousTypes() {
        // transactionDate is at index 5; indices 0-4 are Long/Long/String/String/Long
        Object[] row = {1L, 2L, null, null, 100L,
            new java.sql.Timestamp(System.currentTimeMillis()), // transactionDate at index 5
            null, null, null, null, null, null, null, null
        };
        
        List<Object[]> rows6 = new ArrayList<>();
        rows6.add(row);
        when(sp.getResultList()).thenReturn(rows6);
        
        // Act
        List<BankReconciliationResponse> result = repository.callReconcileView(
            1L, 2L, 3L, null, null, null, null, null);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.get(0).getTransactionDate());
    }
    
    @Test
    void testGetDate_InvalidString() {
        // transactionDate is at index 5; indices 0,1,4 are Long fields
        Object[] row = {1L, 2L, null, null, 100L,
            "invalid-date", // transactionDate at index 5 — unparseable, returns null
            null, null, null, null, null, null, null, null
        };
        
        List<Object[]> rows7 = new ArrayList<>();
        rows7.add(row);
        when(sp.getResultList()).thenReturn(rows7);
        
        // Act
        List<BankReconciliationResponse> result = repository.callReconcileView(
            1L, 2L, 3L, null, null, null, null, null);
        
        // Assert
        assertNotNull(result);
        assertNull(result.get(0).getTransactionDate());
    }
    
    @Test
    void testGetDate_UnsupportedType() {
        // transactionDate is at index 5; Integer is unsupported by getDate() → IllegalArgumentException
        Object[] row = {1L, 2L, null, null, 100L,
            123, // Integer at index 5 — unsupported date type
            null, null, null, null, null, null, null, null
        };
        
        List<Object[]> rows8 = new ArrayList<>();
        rows8.add(row);
        when(sp.getResultList()).thenReturn(rows8);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            repository.callReconcileView(1L, 2L, 3L, null, null, null, null, null));
    }
    
    private BankReconciliationRequest createBankReconciliationRequest() {
        BankReconciliationRequest request = new BankReconciliationRequest();
        request.setTransactionGroupPoid(1L);
        request.setTransactionCompanyPoid(2L);
        request.setDocId("DOC001");
        request.setTransactionPoid(100L);
        request.setTransactionDate(LocalDate.of(2023, 6, 15));
        request.setDocRef("REF001");
        request.setChequeRef("CHQ001");
        request.setDetRowId(1L);
        request.setNarration("Test narration");
        request.setGlCompanyPoid(2L);
        request.setGlPoid(3L);
        request.setDrAmt(1000.0);
        request.setCrAmt(500.0);
        request.setPostedBy(1L);
        request.setClearanceDate(LocalDate.of(2023, 6, 20));
        request.setUserAuto("AUTO");
        return request;
    }
    
    private BankReconcHoldAndUholdRequest createHoldUnholdRequest() {
        BankReconcHoldAndUholdRequest request = new BankReconcHoldAndUholdRequest();
        request.setTransactionGroupPoid(1L);
        request.setTransactionCompanyPoid(2L);
        request.setDocId("400-107");
        request.setTransactionPoid(100L);
        request.setUserPoid(1L);
        request.setDocRef("REF001");
        return request;
    }
    
    private BankReconcileReportRequest createReportRequest() {
        BankReconcileReportRequest request = new BankReconcileReportRequest();
        request.setGroupPoid(1L);
        request.setCompanyPoid(2L);
        request.setBankPoid(3L);
        request.setDateFrom(LocalDate.of(2023, 1, 1));
        request.setDateTill(LocalDate.of(2023, 12, 31));
        request.setChequeNo("CHQ001");
        request.setReconcileCheque("Y");
        request.setBrType("MANUAL");
        return request;
    }
}