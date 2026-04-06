package com.asg.finance.pdcchqbatch.repository;

import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.PayGlBreakupCheckResponseDto;
import com.asg.finance.dto.PdcBankPostingProcRequest;
import com.asg.finance.dto.PdcBatchCreationExcelProcRequest;
import com.asg.finance.dto.PdcBatchCreationProcRequest;
import com.asg.finance.dto.PdcBatchCreationProcResponse;
import com.asg.finance.repository.PdcBatchCreationRepositoryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdcBatchCreationRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private StoredProcedureQuery storedProcedureQuery;

    private PdcBatchCreationRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new PdcBatchCreationRepositoryImpl(entityManager);
    }

    @Test
    void checkPayGlBreakup_Success() {
        when(entityManager.createStoredProcedureQuery("PROC_PDC_PAY_GL_BREAKUP_CHECK")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);
        when(storedProcedureQuery.getOutputParameterValue("P_RESULT")).thenReturn("BILL_WISE");
        when(storedProcedureQuery.getOutputParameterValue("P_COST_GROUP")).thenReturn("CG-1");

        PayGlBreakupCheckResponseDto result = repository.checkPayGlBreakup(1L, 2L, 3L, 101L);

        assertEquals("BILL_WISE", result.getResult());
        assertEquals("CG-1", result.getCostGroup());
    }

    @Test
    void checkPayGlBreakup_ExceptionWrapped() {
        when(entityManager.createStoredProcedureQuery("PROC_PDC_PAY_GL_BREAKUP_CHECK"))
                .thenThrow(new RuntimeException("db error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> repository.checkPayGlBreakup(1L, 2L, 3L, 101L));

        assertTrue(exception.getMessage().contains("Error executing PROC_PDC_PAY_GL_BREAKUP_CHECK"));
    }

    @Test
    void runBatchCreation_Success() {
        PdcBatchCreationProcRequest request = PdcBatchCreationProcRequest.builder()
                .transactionPoid(999L)
                .noOfCheques(2)
                .chequeAmount(1500.0)
                .startChequeNo("100001")
                .startDate(LocalDate.of(2026, 4, 1))
                .prePrinted("Y")
                .narration("test")
                .billRef("BILL-1")
                .build();

        when(entityManager.createStoredProcedureQuery("PROC_PDC_CHQ_BATCH_CREATION")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.getOutputParameterValue("P_STATUS")).thenReturn("SUCCESS");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(2L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(3L);

            PdcBatchCreationProcResponse result = repository.runBatchCreation(request);

            assertEquals("SUCCESS", result.getStatus());
            verify(entityManager).flush();
            verify(entityManager).clear();
        }
    }

    @Test
    void runBankPosting_Success() {
        PdcBankPostingProcRequest request = PdcBankPostingProcRequest.builder()
                .transactionPoid(999L)
                .payGlPoid(101L)
                .payingTo("Vendor A")
                .bankPoid(202L)
                .noOfChqs(2L)
                .chequeAmount(1500.0)
                .startChequeNo("100001")
                .startDate("01-APR-2026")
                .build();

        when(entityManager.createStoredProcedureQuery("PROC_PDC_CHQ_BANK_POSTING")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.getOutputParameterValue("P_STATUS")).thenReturn("SUCCESS");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(2L);
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(3L);

            PdcBatchCreationProcResponse result = repository.runBankPosting(request);

            assertEquals("SUCCESS", result.getStatus());
        }
    }

    @Test
    void runBatchCreationXL_Success() {
        PdcBatchCreationExcelProcRequest request = new PdcBatchCreationExcelProcRequest();
        request.setTransactionPoid(999L);
        request.setNoOfCheques(2);
        request.setChequeAmount(BigDecimal.valueOf(1500));
        request.setStartChequeNo("100001");
        request.setStartDate(LocalDate.of(2026, 4, 1));
        request.setPrePrinted("Y");
        request.setNarration("test");
        request.setBillRef("BILL-1");

        when(entityManager.createStoredProcedureQuery("PROC_PDC_CHQ_BATCH_CREATION_XL")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.getOutputParameterValue("P_STATUS")).thenReturn("SUCCESS");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(2L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(3L);

            PdcBatchCreationProcResponse result = repository.runBatchCreationXL(request);

            assertEquals("SUCCESS", result.getStatus());
            verify(entityManager, times(1)).flush();
            verify(entityManager, times(1)).clear();
        }
    }
}

