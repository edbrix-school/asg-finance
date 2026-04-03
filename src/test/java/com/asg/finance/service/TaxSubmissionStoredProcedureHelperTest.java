package com.asg.finance.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaxSubmissionStoredProcedureHelperTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private StoredProcedureQuery query;

    private TaxSubmissionStoredProcedureHelper helper;

    @BeforeEach
    void setUp() {
        helper = new TaxSubmissionStoredProcedureHelper(entityManager);
        when(query.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @Test
    void validateBeforeSave_ReturnsOutputValue() {
        when(entityManager.createStoredProcedureQuery("PROC_TAX_SUBMIN_BEFORE_SAVE")).thenReturn(query);
        when(query.getOutputParameterValue("P_RESULT")).thenReturn("OK");

        String result = helper.validateBeforeSave(1L, 2L, "user", LocalDateTime.now(), LocalDateTime.now(), 10L);

        assertEquals("OK", result);
    }

    @Test
    void validateBeforeSave_ReturnsSuccessWhenOutputNull() {
        when(entityManager.createStoredProcedureQuery("PROC_TAX_SUBMIN_BEFORE_SAVE")).thenReturn(query);
        when(query.getOutputParameterValue("P_RESULT")).thenReturn(null);

        String result = helper.validateBeforeSave(1L, 2L, "user", LocalDateTime.now(), LocalDateTime.now(), null);

        assertEquals("Success", result);
    }

    @Test
    void validateBeforeSave_ThrowsWrappedException() {
        when(entityManager.createStoredProcedureQuery("PROC_TAX_SUBMIN_BEFORE_SAVE"))
                .thenThrow(new RuntimeException("failed"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> helper.validateBeforeSave(1L, 2L, "user", LocalDateTime.now(), LocalDateTime.now(), 10L));

        assertTrue(ex.getMessage().contains("Error validating before save"));
    }

    @Test
    void processAfterSave_ReturnsSuccessWhenOra01403() {
        when(entityManager.createStoredProcedureQuery("PROC_TAX_SUBMIN_AFTER_SAVE")).thenReturn(query);
        when(query.execute()).thenThrow(new RuntimeException("ORA-01403: no data found"));

        String result = helper.processAfterSave(1L, 2L, "user", 10L);

        assertEquals("Success", result);
    }

    @Test
    void processAfterSave_ThrowsWrappedExceptionForOtherErrors() {
        when(entityManager.createStoredProcedureQuery("PROC_TAX_SUBMIN_AFTER_SAVE")).thenReturn(query);
        when(query.execute()).thenThrow(new RuntimeException("broken"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> helper.processAfterSave(1L, 2L, "user", 10L));

        assertTrue(ex.getMessage().contains("Error processing after save"));
    }

    @Test
    void loadVatDetails_ReturnsMappedRows() throws Exception {
        ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);

        when(entityManager.createStoredProcedureQuery("PROC_TAX_SUBMIN_LOAD_DTLS")).thenReturn(query);
        when(query.getOutputParameterValue("P_RESULT")).thenReturn("SUCCESS");
        when(query.getOutputParameterValue("P_OUTDATA")).thenReturn(rs);

        when(rs.next()).thenReturn(true, false);
        when(rs.getObject("TAX_TYPE")).thenReturn("VAT");
        when(rs.getObject("TAX_POID")).thenReturn(99L);
        when(rs.getObject("TAX_CODE")).thenReturn("VAT-01");
        when(rs.getObject("TAX_NAME")).thenReturn("Value Added Tax");
        when(rs.getObject("PERCENTAGE")).thenReturn("5");
        when(rs.getObject("TAX_BASE_AMOUNT")).thenReturn(new BigDecimal("100.00"));
        when(rs.getObject("TAX_AMOUNT")).thenReturn(new BigDecimal("5.00"));
        when(rs.getObject("TOTAL_AMOUNT")).thenReturn(new BigDecimal("105.00"));

        List<Map<String, Object>> result = helper.loadVatDetails(1L, 2L, 3L, 10L,
                LocalDateTime.now().minusDays(30), LocalDateTime.now());

        assertEquals(1, result.size());
        assertEquals("VAT", result.get(0).get("TAX_TYPE"));
        assertEquals(1L, result.get(0).get("DET_ROW_ID"));
    }

    @Test
    void loadVatDetails_ReturnsEmptyWhenCursorNull() {
        when(entityManager.createStoredProcedureQuery("PROC_TAX_SUBMIN_LOAD_DTLS")).thenReturn(query);
        when(query.getOutputParameterValue("P_RESULT")).thenReturn("SUCCESS");
        when(query.getOutputParameterValue("P_OUTDATA")).thenReturn(null);

        List<Map<String, Object>> result = helper.loadVatDetails(1L, 2L, 3L, 10L,
                LocalDateTime.now().minusDays(30), LocalDateTime.now());

        assertTrue(result.isEmpty());
    }

    @Test
    void loadVatDetails_ThrowsWhenStatusContainsError() {
        when(entityManager.createStoredProcedureQuery("PROC_TAX_SUBMIN_LOAD_DTLS")).thenReturn(query);
        when(query.getOutputParameterValue("P_RESULT")).thenReturn("ERROR: bad data");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> helper.loadVatDetails(1L, 2L, 3L, 10L,
                        LocalDateTime.now().minusDays(30), LocalDateTime.now()));

        assertTrue(ex.getMessage().contains("Error loading VAT details"));
    }
}
