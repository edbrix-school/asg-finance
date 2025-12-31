package com.asg.finance.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.asg.common.lib.client.ParameterServiceClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleTypes;

@Component
@RequiredArgsConstructor
@Slf4j
public class HsbcPaymentClient {

    private final DataSource dataSource;
    private final PgpHelper pgpHelper;
    private final ParameterServiceClient parameterServiceClient;

    public void processPendingPayments() throws Exception {
        log.info("Starting HSBC payment processing");
        
        String sql = "SELECT PAYMENT_ID FROM HSBC_API_XML_DATA WHERE STATUS = 'CREATED'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String paymentId = rs.getString("PAYMENT_ID");
                try {
                    processPayment(paymentId);
                } catch (Exception e) {
                    log.error("Failed to process payment {}: {}", paymentId, e.getMessage(), e);
                }
            }
        }
    }

    private void processPayment(String paymentId) throws Exception {
        String apiUrl = parameterServiceClient.findParameterValueByName("HSBC_URL_FOR_BULK_PAYMENT_API").orElseThrow();
        String profileId = parameterServiceClient.findParameterValueByName("HSBC_PROFILE_ID_FOR_API").orElseThrow();
        String clientSecret = parameterServiceClient.findParameterValueByName("HSBC_CLIENT_SECRET_API").orElseThrow();
        String bankPublicKeyPath = parameterServiceClient.findParameterValueByName("HSBC_PUBLIC_KEY_FOR_API").orElseThrow();
        String asgPrivateKeyPath = parameterServiceClient.findParameterValueByName("HSBC_ASG_PRIV_KEY_FOR_API").orElseThrow();
        String secretKey = parameterServiceClient.findParameterValueByName("HSBC_SECRET_KEY_API").orElseThrow();
        
        String paymentXml = getPaymentXml(paymentId);
        
        PGPSecretKey secretKeyObj = pgpHelper.readSecretKeyFromFile(asgPrivateKeyPath);
        PGPPublicKey bankPublicKey = pgpHelper.readPublicKeyFromFile(bankPublicKeyPath);
        
        ByteArrayOutputStream encryptedData = new ByteArrayOutputStream();
        pgpHelper.encryptAndSign(encryptedData, new ByteArrayInputStream(paymentXml.getBytes("UTF-8")), 
                                 bankPublicKey, pgpHelper.extractPrivateKey(secretKeyObj, secretKey.toCharArray()));
        
        String encryptedBase64 = Base64.toBase64String(encryptedData.toByteArray());
        String payload = "{\"paymentRequestBase64\":\"" + encryptedBase64 + "\"}";
        
        String response = executeHttpPost(apiUrl, payload, profileId, clientSecret);
        processPaymentResponse(response, paymentId, bankPublicKey, secretKeyObj, secretKey, "PAYMENT_INITIATION");
    }

    private String getPaymentXml(String paymentId) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_RET_HSBC_API_PAYMNT_XML(?)}")) {
            
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    if (paymentId.equals(rs.getString("TRANSACTION_POID"))) {
                        return rs.getString("XML_DATA");
                    }
                }
                throw new SQLException("No XML data found for payment: " + paymentId);
            }
        }
    }

    private String executeHttpPost(String url, String payload, String profileId, String clientSecret) throws Exception {
        var sslContext = new SSLContextBuilder().loadTrustMaterial(null, (chain, authType) -> true).build();
        var sslFactory = new SSLConnectionSocketFactory(sslContext);
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(sslFactory).build();
        
        try (CloseableHttpClient client = HttpClients.custom().setConnectionManager(connectionManager).build()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("x-hsbc-client-secret", clientSecret);
            post.setHeader("x-hsbc-profile-id", profileId);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(payload));
            
            try (CloseableHttpResponse response = client.execute(post)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    private void processPaymentResponse(String response, String paymentId, PGPPublicKey bankPublicKey, 
                                       PGPSecretKey secretKeyObj, String secretKey, String responseType) throws Exception {
        JSONObject jsonResponse = JSON.parseObject(response);
        String encryptedResponse = jsonResponse.getString("responseBase64");
        
        if (encryptedResponse != null && !encryptedResponse.trim().isEmpty()) {
            byte[] decryptedData = pgpHelper.decryptAndVerify(Base64.decode(encryptedResponse), 
                                                              pgpHelper.extractPrivateKey(secretKeyObj, secretKey.toCharArray()), 
                                                              bankPublicKey);
            String decryptedResponse = new String(decryptedData);
            updatePaymentStatus(paymentId, jsonResponse, decryptedResponse, responseType);
        } else {
            updatePaymentStatus(paymentId, jsonResponse, jsonResponse.getString("statusDesc"), responseType);
        }
    }

    private void updatePaymentStatus(String paymentId, JSONObject jsonResponse, String decodedResponse, String responseType) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_UPDATE_HSBC_PAYMENT_STATUS(?,?,?,?,?,?,?,?,?,?)}")) {
            
            stmt.setString(1, paymentId);
            stmt.setString(2, jsonResponse.getString("clientProfileId"));
            stmt.setString(3, jsonResponse.getString("processStatus"));
            stmt.setString(4, jsonResponse.getString("responseRefId"));
            stmt.setString(5, jsonResponse.getString("hsbcResponseStatus"));
            stmt.setString(6, jsonResponse.getString("statusCode"));
            stmt.setString(7, jsonResponse.getString("statusDesc"));
            stmt.setString(8, decodedResponse);
            stmt.setString(9, responseType);
            stmt.registerOutParameter(10, OracleTypes.VARCHAR);
            stmt.execute();
            
            String status = stmt.getString(10);
            log.info("Updated payment {} with status {}", paymentId, status);
        }
    }

    public void checkPaymentStatus() throws Exception {
        log.info("Checking HSBC payment status for pending payments");
        
        String sql = "SELECT PAYMENT_ID, TRANSACTION_REF FROM HSBC_API_XML_DATA WHERE STATUS = 'PDNG'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String paymentId = rs.getString("PAYMENT_ID");
                String transactionRef = rs.getString("TRANSACTION_REF");
                try {
                    queryPaymentStatus(paymentId, transactionRef);
                } catch (Exception e) {
                    log.error("Failed to query status for payment {}: {}", paymentId, e.getMessage(), e);
                }
            }
        }
    }

    private void queryPaymentStatus(String paymentId, String transactionRef) throws Exception {
        String apiUrl = parameterServiceClient.findParameterValueByName("HSBC_URL_FOR_PAYMENT_STATUS_API").orElseThrow();
        String profileId = parameterServiceClient.findParameterValueByName("HSBC_PROFILE_ID_FOR_API").orElseThrow();
        String clientSecret = parameterServiceClient.findParameterValueByName("HSBC_CLIENT_SECRET_API").orElseThrow();
        String bankPublicKeyPath = parameterServiceClient.findParameterValueByName("HSBC_PUBLIC_KEY_FOR_API").orElseThrow();
        String asgPrivateKeyPath = parameterServiceClient.findParameterValueByName("HSBC_ASG_PRIV_KEY_FOR_API").orElseThrow();
        String secretKey = parameterServiceClient.findParameterValueByName("HSBC_SECRET_KEY_API").orElseThrow();
        
        String statusXml = getPaymentStatusXml(paymentId, transactionRef);
        
        PGPSecretKey secretKeyObj = pgpHelper.readSecretKeyFromFile(asgPrivateKeyPath);
        PGPPublicKey bankPublicKey = pgpHelper.readPublicKeyFromFile(bankPublicKeyPath);
        
        ByteArrayOutputStream encryptedData = new ByteArrayOutputStream();
        pgpHelper.encryptAndSign(encryptedData, new ByteArrayInputStream(statusXml.getBytes("UTF-8")), 
                                 bankPublicKey, pgpHelper.extractPrivateKey(secretKeyObj, secretKey.toCharArray()));
        
        String encryptedBase64 = Base64.toBase64String(encryptedData.toByteArray());
        String payload = "{\"statusRequestBase64\":\"" + encryptedBase64 + "\"}";
        
        String response = executeHttpPost(apiUrl, payload, profileId, clientSecret);
        processPaymentResponse(response, paymentId, bankPublicKey, secretKeyObj, secretKey, "PAYMENT_ENQUIRY");
    }

    private String getPaymentStatusXml(String paymentId, String transactionRef) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_HSBC_API_PYMT_STAT_XML(?)}")) {
            
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                if (rs.next()) {
                    return rs.getString("xml_output");
                }
                throw new SQLException("No XML data found for payment status");
            }
        }
    }

    public void checkBalanceMismatch() throws SQLException {
        log.info("Checking HSBC balance mismatch");
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_HSBC_BAL_MISMATCH_ALERT()}")) {
            stmt.execute();
            log.info("Balance mismatch check completed");
        }
    }

    public void checkLedgerMismatch() throws SQLException {
        log.info("Checking HSBC ledger mismatch");
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_HSBC_LEDGER_MISMATCH_ALERT()}")) {
            stmt.execute();
            log.info("Ledger mismatch check completed");
        }
    }
}
