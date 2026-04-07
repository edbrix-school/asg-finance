package com.asg.finance.appurchasecn.repository;

import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.repository.ApPurchaseCnProcRepositoryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ApPurchaseCnProcRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private StoredProcedureQuery storedProcedureQuery;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSetMetaData metaData;

    private ApPurchaseCnProcRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ApPurchaseCnProcRepositoryImpl();
        ReflectionTestUtils.setField(repository, "entityManager", entityManager);
    }

    @Test
    void getPjRefDetails_Success() throws Exception {
        when(entityManager.createStoredProcedureQuery("PROC_AP_CN_PJ_REF_DETAILS")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyString(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);
        when(storedProcedureQuery.getOutputParameterValue("P_RESULT")).thenReturn("SUCCESS");
        when(storedProcedureQuery.getOutputParameterValue("P_PJ_REF_TYPE")).thenReturn("GENERAL");
        when(storedProcedureQuery.getOutputParameterValue("P_PJ_REF_DETAILS")).thenReturn("DETAIL");
        when(storedProcedureQuery.getOutputParameterValue("OUTDATA")).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnName(1)).thenReturn("GL_POID");
        when(metaData.getColumnName(2)).thenReturn("TYPE");
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn(100L);
        when(resultSet.getObject(2)).thenReturn("DR");

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(2L);
            mockedStatic.when(UserContext::getUserPoid).thenReturn(3L);

            Map<String, Object> result = repository.getPjRefDetails(99L);

            assertEquals("GENERAL", result.get("pjRefType"));
            assertEquals("DETAIL", result.get("pjRefDetails"));
            List<Map<String, Object>> lineItems = (List<Map<String, Object>>) result.get("lineItems");
            assertEquals(1, lineItems.size());
            assertEquals(100L, lineItems.get(0).get("GL_POID"));
        }
    }

    @Test
    void getPjRefDetails_ErrorResult() {
        when(entityManager.createStoredProcedureQuery("PROC_AP_CN_PJ_REF_DETAILS")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyString(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);
        when(storedProcedureQuery.getOutputParameterValue("P_RESULT")).thenReturn("ERROR: invalid");

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(2L);
            mockedStatic.when(UserContext::getUserPoid).thenReturn(3L);

            assertThrows(ValidationException.class, () -> repository.getPjRefDetails(99L));
        }
    }

    @Test
    void getPartyDetails_Success() throws Exception {
        when(entityManager.createStoredProcedureQuery("PROC_AP_CN_PJ_PARTY_DTLS")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyString(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);
        when(storedProcedureQuery.getOutputParameterValue("P_RESULT")).thenReturn("SUCCESS");
        when(storedProcedureQuery.getOutputParameterValue("OUTDATA")).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnName(1)).thenReturn("PARTY_NAME");
        when(metaData.getColumnName(2)).thenReturn("TIN");
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn("Supplier A");
        when(resultSet.getObject(2)).thenReturn("TIN123");

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(2L);
            mockedStatic.when(UserContext::getUserPoid).thenReturn(3L);

            Map<String, Object> result = repository.getPartyDetails("SUPPLIER", 10L);

            assertEquals("Supplier A", result.get("PARTY_NAME"));
            assertEquals("TIN123", result.get("TIN"));
        }
    }

    @Test
    void getPartyDetails_EmptyCursor() throws Exception {
        when(entityManager.createStoredProcedureQuery("PROC_AP_CN_PJ_PARTY_DTLS")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyString(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);
        when(storedProcedureQuery.getOutputParameterValue("P_RESULT")).thenReturn("SUCCESS");
        when(storedProcedureQuery.getOutputParameterValue("OUTDATA")).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(resultSet.next()).thenReturn(false);

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(2L);
            mockedStatic.when(UserContext::getUserPoid).thenReturn(3L);

            Map<String, Object> result = repository.getPartyDetails("SUPPLIER", 10L);

            assertTrue(result.isEmpty());
        }
    }

    @Test
    void beforeSaveValidation_SuccessWithNullIds() {
        when(entityManager.createStoredProcedureQuery("PROC_AP_CN_PJ_BEFORE_SAVE")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyString(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);
        when(storedProcedureQuery.getOutputParameterValue("P_RESULT")).thenReturn("SUCCESS");

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(2L);
            mockedStatic.when(UserContext::getUserPoid).thenReturn(3L);

            repository.beforeSaveValidation("SUPPLIER", null, "GENERAL", null);
        }
    }

    @Test
    void beforeSaveValidation_ErrorResult() {
        when(entityManager.createStoredProcedureQuery("PROC_AP_CN_PJ_BEFORE_SAVE")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.setParameter(anyString(), any())).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.execute()).thenReturn(true);
        when(storedProcedureQuery.getOutputParameterValue("P_RESULT")).thenReturn("ERROR: blocked");

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(2L);
            mockedStatic.when(UserContext::getUserPoid).thenReturn(3L);

            assertThrows(ValidationException.class, () -> repository.beforeSaveValidation("SUPPLIER", 11L, "GENERAL", 99L));
        }
    }

    @Test
    void parseResultSet_ExceptionWrapped() throws Exception {
        when(resultSet.getMetaData()).thenThrow(new RuntimeException("cursor failed"));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> ReflectionTestUtils.invokeMethod(repository, "parseResultSet", resultSet));

        assertTrue(exception.getMessage().contains("Error parsing procedure result"));
    }

    @Test
    void parseResultSet_NullCursorReturnsEmptyList() {
        List<Map<String, Object>> result = ReflectionTestUtils.invokeMethod(repository, "parseResultSet", new Object[]{null});
        assertFalse(result.iterator().hasNext());
    }
}
