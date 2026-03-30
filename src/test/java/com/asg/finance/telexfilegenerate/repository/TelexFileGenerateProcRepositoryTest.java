package com.asg.finance.telexfilegenerate.repository;

import com.asg.finance.dto.TelexFileDtlDto;
import com.asg.finance.repository.TelexFileGenerateProcRepositoryImpl;
import com.asg.common.lib.service.LoggingService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class TelexFileGenerateProcRepositoryTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private LoggingService loggingService;

    @Mock
    private Query query;

    @Mock
    private StoredProcedureQuery storedProcedureQuery;

    private TelexFileGenerateProcRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new TelexFileGenerateProcRepositoryImpl(loggingService);
        ReflectionTestUtils.setField(repository, "entityManager", entityManager);
    }

    @Test
    void loadTelexTransferData_Success() {
        Object[] row = new Object[]{
                new BigDecimal(1001),
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 28, 0, 0)),
                new BigDecimal(1),
                "BD-2025-001",
                "ABC Suppliers Ltd",
                "1",
                "Payment for invoice",
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 29, 0, 0)),
                "USD",
                new BigDecimal("3.75"),
                new BigDecimal("10000.00"),
                new BigDecimal("37500.00"),
                "N",
                "N",
                "TARGET_DOC_ID=400-111,DOC_KEY_POID=1001",
                "OUR"
        };

        List<Object[]> resultList = new ArrayList<>();
        resultList.add(row);

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        doReturn(resultList).when(query).getResultList();

        List<TelexFileDtlDto> result = repository.loadTelexTransferData("Y");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1001L, result.get(0).getDebitTransactionPoid());
        assertEquals("BD-2025-001", result.get(0).getDebitDocRef());
        assertEquals("USD", result.get(0).getDebitCurrencyCode());
        verify(entityManager, times(1)).createNativeQuery(anyString());
    }

    @Test
    void loadTelexTransferData_EmptyResult() {
        List<Object[]> emptyList = new ArrayList<>();
        
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        doReturn(emptyList).when(query).getResultList();

        List<TelexFileDtlDto> result = repository.loadTelexTransferData("Y");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void regenerateTelexFile_Success() {
        when(entityManager.createStoredProcedureQuery("PROC_GL_BANK_DEBIT_FILE_REGENT_V2")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyInt(), any(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyInt(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);
        when(storedProcedureQuery.getOutputParameterValue(5)).thenReturn("SUCCESS");

        String result = repository.regenerateTelexFile(1L, 1L, 1L, 1001L);

        assertEquals("SUCCESS: Bank telex file removed", result);
        verify(entityManager, times(1)).createStoredProcedureQuery("PROC_GL_BANK_DEBIT_FILE_REGENT_V2");
    }

    @Test
    void regenerateTelexFile_Error() {
        when(entityManager.createStoredProcedureQuery("PROC_GL_BANK_DEBIT_FILE_REGENT_V2")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyInt(), any(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyInt(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);
        when(storedProcedureQuery.getOutputParameterValue(5)).thenReturn("ERROR: Failed");

        String result = repository.regenerateTelexFile(1L, 1L, 1L, 1001L);

        assertEquals("ERROR: Failed", result);
    }

    @Test
    void regenerateTelexFile_Exception() {
        when(entityManager.createStoredProcedureQuery("PROC_GL_BANK_DEBIT_FILE_REGENT_V2"))
                .thenThrow(new RuntimeException("Database error"));

        String result = repository.regenerateTelexFile(1L, 1L, 1L, 1001L);

        assertTrue(result.startsWith("ERROR:"));
    }

    @Test
    void createBankFileBatchNbb_Success() {
        when(entityManager.createStoredProcedureQuery("PROC_BANK_FILE_CREATE_BCH_NBB")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyInt(), any(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyInt(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);
        when(storedProcedureQuery.getOutputParameterValue(3)).thenReturn("SUCCESS");

        String result = repository.createBankFileBatchNbb(42418L, 1L, 1L);

        assertEquals("SUCCESS", result);
    }

    @Test
    void createBankFileBatchNbb_Exception() {
        when(entityManager.createStoredProcedureQuery("PROC_BANK_FILE_CREATE_BCH_NBB"))
                .thenThrow(new RuntimeException("Database error"));

        String result = repository.createBankFileBatchNbb(42418L, 1L, 1L);

        assertTrue(result.startsWith("ERROR:"));
    }

    @Test
    void checkOverdraft_Success() {
        when(entityManager.createStoredProcedureQuery("PROC_BANK_FILE_CHECK_OD")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyInt(), any(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyInt(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);
        when(storedProcedureQuery.getOutputParameterValue(2)).thenReturn("OK");

        String result = repository.checkOverdraft(42418L);

        assertEquals("OK", result);
    }

    @Test
    void checkOverdraft_Exception() {
        when(entityManager.createStoredProcedureQuery("PROC_BANK_FILE_CHECK_OD"))
                .thenThrow(new RuntimeException("Database error"));

        String result = repository.checkOverdraft(42418L);

        assertNull(result);
    }

    @Test
    void linkBankApproval_Success() {
        when(entityManager.createStoredProcedureQuery("PROC_DYNRPT_LINK_BANKAPPROVAL")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyInt(), any(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyInt(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);
        when(storedProcedureQuery.getOutputParameterValue(7)).thenReturn("SUCCESS");

        String result = repository.linkBankApproval(1L, 1L, 1L, "400-111", "xx", "400-111-1001");

        assertEquals("SUCCESS", result);
    }

    @Test
    void linkBankApproval_Exception() {
        when(entityManager.createStoredProcedureQuery("PROC_DYNRPT_LINK_BANKAPPROVAL"))
                .thenThrow(new RuntimeException("Database error"));

        String result = repository.linkBankApproval(1L, 1L, 1L, "400-111", "xx", "400-111-1001");

        assertNull(result);
    }

    @Test
    void getPayingTo_Success() {
        List<String> resultList = new ArrayList<>();
        resultList.add("EMP-001");
        
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        doReturn(resultList).when(query).getResultList();

        String result = repository.getPayingTo("BD-2025-001");

        assertEquals("EMP-001", result);
    }

    @Test
    void getPayingTo_EmptyResult() {
        List<String> emptyList = new ArrayList<>();
        
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        doReturn(emptyList).when(query).getResultList();

        String result = repository.getPayingTo("BD-2025-001");

        assertNull(result);
    }

    @Test
    void getPayingTo_Exception() {
        when(entityManager.createNativeQuery(anyString())).thenThrow(new RuntimeException("Database error"));

        String result = repository.getPayingTo("BD-2025-001");

        assertNull(result);
        verify(entityManager, times(1)).createNativeQuery(anyString());
    }

    @Test
    void checkEmployeeIban_True() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(new BigDecimal(1));

        boolean result = repository.checkEmployeeIban("BD-2025-001");

        assertTrue(result);
    }

    @Test
    void checkEmployeeIban_False() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(new BigDecimal(0));

        boolean result = repository.checkEmployeeIban("BD-2025-001");

        assertFalse(result);
    }

    @Test
    void checkEmployeeIban_Exception() {
        when(entityManager.createNativeQuery(anyString())).thenThrow(new RuntimeException("Database error"));

        boolean result = repository.checkEmployeeIban("BD-2025-001");

        assertFalse(result);
        verify(entityManager, times(1)).createNativeQuery(anyString());
    }

    @Test
    void getBeneficiaryDetails_Success() {
        Object[] row = new Object[]{"BEN-001", new BigDecimal(1), "BH", "ACC-001", "BANK-001"};
        List<Object[]> resultList = new ArrayList<>();
        resultList.add(row);

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        doReturn(resultList).when(query).getResultList();

        Map<String, String> result = repository.getBeneficiaryDetails(1001L);

        assertNotNull(result);
        assertEquals("BEN-001", result.get("BENEFICIARY_ID"));
        assertEquals("BH", result.get("BENEFICIARY_COUNTRY"));
    }

    @Test
    void getBeneficiaryDetails_EmptyResult() {
        List<Object[]> emptyList = new ArrayList<>();
        
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        doReturn(emptyList).when(query).getResultList();

        Map<String, String> result = repository.getBeneficiaryDetails(1001L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getBeneficiaryDetails_Exception() {
        when(entityManager.createNativeQuery(anyString())).thenThrow(new RuntimeException("Database error"));

        Map<String, String> result = repository.getBeneficiaryDetails(1001L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(entityManager, times(1)).createNativeQuery(anyString());
    }

    @Test
    void getTtChargeType_Success() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn("OUR");

        String result = repository.getTtChargeType(1001L);

        assertEquals("OUR", result);
    }

    @Test
    void getTtChargeType_Exception() {
        when(entityManager.createNativeQuery(anyString())).thenThrow(new RuntimeException("Database error"));

        String result = repository.getTtChargeType(1001L);

        assertNull(result);
        verify(entityManager, times(1)).createNativeQuery(anyString());
    }

    @Test
    void getCompanyDetails_Success() {
        Object[] row = new Object[]{"Test Company", "Test Address"};

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);

        Map<String, String> result = repository.getCompanyDetails(1L);

        assertNotNull(result);
        assertEquals("Test Company", result.get("COMPANY_NAME"));
        assertEquals("Test Address", result.get("ADDRESS"));
    }

    @Test
    void getCompanyDetails_Exception() {
        when(entityManager.createNativeQuery(anyString())).thenThrow(new RuntimeException("Database error"));

        Map<String, String> result = repository.getCompanyDetails(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(entityManager, times(1)).createNativeQuery(anyString());
    }

    @Test
    void getCountryCode_Success() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn("BH");

        String result = repository.getCountryCode(1001L);

        assertEquals("BH", result);
    }

    @Test
    void getCountryCode_Exception() {
        when(entityManager.createNativeQuery(anyString())).thenThrow(new RuntimeException("Database error"));

        String result = repository.getCountryCode(1001L);

        assertEquals("BH", result);
        verify(entityManager, times(1)).createNativeQuery(anyString());
    }

    @Test
    void createBankFilePayment_Success() {
        when(entityManager.createStoredProcedureQuery("PROC_BANK_FILE_CREATE_PAYMENT")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyInt(), any(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyInt(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);

        repository.createBankFilePayment(1L, 1001L, 1L, "Company", "Address", "BH", "REF", "OUR", "BH", 42418L, 1);

        verify(entityManager, times(1)).createStoredProcedureQuery("PROC_BANK_FILE_CREATE_PAYMENT");
    }

    @Test
    void createBankFilePayment_Exception() {
        when(entityManager.createStoredProcedureQuery("PROC_BANK_FILE_CREATE_PAYMENT"))
                .thenThrow(new RuntimeException("Database error"));

        // Should not throw exception, just log it
        repository.createBankFilePayment(1L, 1001L, 1L, "Company", "Address", "BH", "REF", "OUR", "BH", 42418L, 1);

        verify(entityManager, times(1)).createStoredProcedureQuery("PROC_BANK_FILE_CREATE_PAYMENT");
    }

    @Test
    void generateHsbcApiXml_Success() {
        when(entityManager.createStoredProcedureQuery("PROC_HSBC_API_GENERATE_XML_V2")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyInt(), any(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyInt(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);

        repository.generateHsbcApiXml(1L, 1001L, 1L, "Company", "Address", "BH", "REF", "OUR", "BH", 42418L, 1, 1L);

        verify(entityManager, times(1)).createStoredProcedureQuery("PROC_HSBC_API_GENERATE_XML_V2");
    }

    @Test
    void generateHsbcApiXml_Exception() {
        when(entityManager.createStoredProcedureQuery("PROC_HSBC_API_GENERATE_XML_V2"))
                .thenThrow(new RuntimeException("Database error"));

        // Should not throw exception, just log it
        repository.generateHsbcApiXml(1L, 1001L, 1L, "Company", "Address", "BH", "REF", "OUR", "BH", 42418L, 1, 1L);

        verify(entityManager, times(1)).createStoredProcedureQuery("PROC_HSBC_API_GENERATE_XML_V2");
    }

    @Test
    void createBankFilePaymentAub_Success() {
        when(entityManager.createStoredProcedureQuery("PROC_BANK_FILE_CREATE_PMT_AUB")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyInt(), any(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyInt(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);

        repository.createBankFilePaymentAub(1L, 1001L, 1L, "Company", "Address", "BH", "REF", "OUR", "BH", 42418L, 1);

        verify(entityManager, times(1)).createStoredProcedureQuery("PROC_BANK_FILE_CREATE_PMT_AUB");
    }

    @Test
    void createBankFilePaymentAub_Exception() {
        when(entityManager.createStoredProcedureQuery("PROC_BANK_FILE_CREATE_PMT_AUB"))
                .thenThrow(new RuntimeException("Database error"));

        // Should not throw exception, just log it
        repository.createBankFilePaymentAub(1L, 1001L, 1L, "Company", "Address", "BH", "REF", "OUR", "BH", 42418L, 1);

        verify(entityManager, times(1)).createStoredProcedureQuery("PROC_BANK_FILE_CREATE_PMT_AUB");
    }

    @Test
    void getCompanyCode_Success() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn("ASG");

        String result = repository.getCompanyCode(1L);

        assertEquals("ASG", result);
    }

    @Test
    void getCompanyCode_Exception() {
        when(entityManager.createNativeQuery(anyString())).thenThrow(new RuntimeException("Database error"));

        String result = repository.getCompanyCode(1L);

        assertNull(result);
        verify(entityManager, times(1)).createNativeQuery(anyString());
    }
}
