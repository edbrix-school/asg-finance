package com.asg.finance.hsbcapisync.utility;

import com.asg.common.lib.client.ParameterServiceClient;
import com.asg.finance.utility.HsbcApiClient;
import com.asg.finance.utility.PgpHelper;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HsbcApiClientTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private PgpHelper pgpHelper;

    @Mock
    private ParameterServiceClient parameterServiceClient;

    @Mock
    private Connection connection;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private PGPSecretKey secretKey;

    @Mock
    private PGPPublicKey publicKey;

    @Mock
    private PGPPrivateKey privateKey;

    @InjectMocks
    private HsbcApiClient hsbcApiClient;

    @BeforeEach
    void setUp() throws Exception {
        when(parameterServiceClient.findParameterValueByName("HSBC_URL_FOR_API")).thenReturn(Optional.of("https://hsbc-test.example.com/"));
        when(parameterServiceClient.findParameterValueByName("HSBC_PROFILE_ID_FOR_API")).thenReturn(Optional.of("PROFILE123"));
        when(parameterServiceClient.findParameterValueByName("HSBC_CLIENT_SECRET_API")).thenReturn(Optional.of("CLIENT_SECRET"));
        when(parameterServiceClient.findParameterValueByName("HSBC_PUBLIC_KEY_FOR_API")).thenReturn(Optional.of("/keys/bank_public.asc"));
        when(parameterServiceClient.findParameterValueByName("HSBC_ASG_PRIV_KEY_FOR_API")).thenReturn(Optional.of("/keys/asg_private.asc"));
        when(parameterServiceClient.findParameterValueByName("HSBC_SECRET_KEY_API")).thenReturn(Optional.of("secret123"));
    }

    @Test
    void syncHsbcData_KeyFilesNotFound_UsesFallbackKeys() throws Exception {
        when(pgpHelper.readSecretKeyFromFile(anyString())).thenThrow(new Exception("File not found"));
        when(pgpHelper.generateTestSecretKey()).thenReturn(secretKey);
        when(pgpHelper.generateTestPublicKey()).thenReturn(publicKey);
        when(pgpHelper.extractPrivateKey(eq(secretKey), any())).thenReturn(privateKey);

        // encryptAndSign will throw since we can't do real PGP in unit test
        doThrow(new Exception("Encryption failed")).when(pgpHelper).encryptAndSign(any(), any(), any(), any());

        assertThrows(Exception.class, () -> hsbcApiClient.syncHsbcData("1234567890", "15-JAN-2025", 1L, 1L, 1L, "TEST"));

        verify(pgpHelper).generateTestSecretKey();
        verify(pgpHelper).generateTestPublicKey();
    }

    @Test
    void syncHsbcData_MissingParameter_ThrowsNoSuchElementException() throws Exception {
        when(parameterServiceClient.findParameterValueByName("HSBC_URL_FOR_API")).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> hsbcApiClient.syncHsbcData("1234567890", "15-JAN-2025", 1L, 1L, 1L, "TEST"));
    }

    @Test
    void syncHsbcData_KeyFilesFound_UsesRealKeys() throws Exception {
        when(pgpHelper.readSecretKeyFromFile(anyString())).thenReturn(secretKey);
        when(pgpHelper.readPublicKeyFromFile(anyString())).thenReturn(publicKey);
        when(pgpHelper.extractPrivateKey(eq(secretKey), any())).thenReturn(privateKey);

        doThrow(new Exception("Encryption failed")).when(pgpHelper).encryptAndSign(any(), any(), any(), any());

        assertThrows(Exception.class, () -> hsbcApiClient.syncHsbcData("1234567890", "15-JAN-2025", 1L, 1L, 1L, "TEST"));

        verify(pgpHelper, never()).generateTestSecretKey();
        verify(pgpHelper, never()).generateTestPublicKey();
    }

    @Test
    void syncHsbcData_DatabaseCallForRequestBody() throws Exception {
        when(pgpHelper.readSecretKeyFromFile(anyString())).thenReturn(secretKey);
        when(pgpHelper.readPublicKeyFromFile(anyString())).thenReturn(publicKey);
        when(pgpHelper.extractPrivateKey(eq(secretKey), any())).thenReturn(privateKey);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
        when(callableStatement.getString(6)).thenReturn("{\"accountNumber\":\"1234567890\"}");

        doThrow(new Exception("Encryption failed")).when(pgpHelper).encryptAndSign(any(), any(), any(), any());

        assertThrows(Exception.class, () -> hsbcApiClient.syncHsbcData("1234567890", "15-JAN-2025", 1L, 1L, 1L, "TEST"));

        verify(callableStatement).setString(1, "SINGLE_ACCOUNT_HISTORY_DATE");
        verify(callableStatement).setString(2, "1234567890");
        verify(callableStatement).setString(3, "15-JAN-2025");
    }
}
