package com.asg.finance.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.asg.common.lib.service.GlobalParameterService;
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
    private final GlobalParameterService globalParameterService;

    public void processPendingPayments() throws Exception {
        log.info("Starting HSBC payment processing");

        String apiUrl = globalParameterService.getParameterValue("HSBC_URL_FOR_BULK_PAYMENT_API", "GROUP", "1", "");
        String profileId = globalParameterService.getParameterValue("HSBC_PROFILE_ID_FOR_API", "GROUP", "1", "");
        String clientSecret = globalParameterService.getParameterValue("HSBC_CLIENT_SECRET_API", "GROUP", "1", "");
        String bankPublicKeyPath = globalParameterService.getParameterValue("HSBC_PUBLIC_KEY_FOR_API", "GROUP", "1", "");
        String asgPrivateKeyPath = globalParameterService.getParameterValue("HSBC_ASG_PRIV_KEY_FOR_API", "GROUP", "1", "");
        String secretKey = globalParameterService.getParameterValue("HSBC_SECRET_KEY_API", "GROUP", "1", "");

        PGPSecretKey secretKeyObj = pgpHelper.readSecretKeyFromFile(asgPrivateKeyPath);
        PGPPublicKey bankPublicKey = pgpHelper.readPublicKeyFromFile(bankPublicKeyPath);

        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_RET_HSBC_API_PAYMNT_XML(?)}")) {

            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();

            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    String paymentXml = rs.getString("XML_DATA");
                    String transactionPoid = rs.getString("TRANSACTION_POID");
                    try {
                        processPayment(conn, transactionPoid, paymentXml, apiUrl, profileId, clientSecret,
                                bankPublicKey, secretKeyObj, secretKey);
                    } catch (Exception e) {
                        log.error("Failed to process payment {}: {}", transactionPoid, e.getMessage(), e);
                    }
                }
            }
        }
    }

    private void processPayment(Connection conn, String transactionPoid, String paymentXml,
                                 String apiUrl, String profileId, String clientSecret,
                                 PGPPublicKey bankPublicKey, PGPSecretKey secretKeyObj, String secretKey) throws Exception {
        ByteArrayOutputStream encryptedData = new ByteArrayOutputStream();
        pgpHelper.encryptAndSign(encryptedData, new ByteArrayInputStream(paymentXml.getBytes("UTF-8")),
                bankPublicKey, pgpHelper.extractPrivateKey(secretKeyObj, secretKey.toCharArray()));

        String encryptedBase64 = Base64.toBase64String(encryptedData.toByteArray());
        String payload = "{\"paymentBase64\":\"" + encryptedBase64 + "\"}";

        String response = executeHttpPost(apiUrl, payload, profileId, clientSecret);

        JSONObject obj = JSON.parseObject(response);
        String statusCode = obj.getString("statusCode");
        String statusDesc = obj.getString("statusDesc");
        String responseRefId = obj.getString("referenceId");
        String hsbcResponseStatus = "COMPLETED";
        String extractedResponseStr = null;

        String responseVal = obj.getString("responseBase64");
        if (responseVal != null && !responseVal.trim().isEmpty() && !"PDNG".equalsIgnoreCase(statusCode)) {
            extractedResponseStr = new String(Base64.decode(responseVal));
        }

        updatePaymentStatus(conn, transactionPoid, profileId, "Y", responseRefId,
                hsbcResponseStatus, statusCode, statusDesc, extractedResponseStr, "PAYMENT_INITIATION");
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

    private void updatePaymentStatus(Connection conn, String transactionPoid, String profileId,
                                      String processStatus, String responseRefId, String hsbcResponseStatus,
                                      String statusCode, String statusDesc, String decodedResponse,
                                      String responseType) throws SQLException {
        try (CallableStatement stmt = conn.prepareCall("{call PROC_UPDATE_HSBC_PAYMENT_STATUS(?,?,?,?,?,?,?,?,?,?)}")) {
            stmt.setString(1, transactionPoid);
            stmt.setString(2, profileId);
            stmt.setString(3, processStatus);
            stmt.setString(4, responseRefId);
            stmt.setString(5, hsbcResponseStatus);
            stmt.setString(6, statusCode);
            stmt.setString(7, statusDesc);
            stmt.setString(8, decodedResponse);
            stmt.setString(9, responseType);
            stmt.registerOutParameter(10, OracleTypes.VARCHAR);
            stmt.execute();

            String status = stmt.getString(10);
            log.info("Updated payment {} with status {}", transactionPoid, status);
        }
    }

    public void checkPaymentStatus() throws Exception {
        log.info("Checking HSBC payment status for pending payments");

        String apiUrl = globalParameterService.getParameterValue("HSBC_URL_FOR_PAYMENT_STATUS_API", "GROUP", "1", "");
        String profileId = globalParameterService.getParameterValue("HSBC_PROFILE_ID_FOR_API", "GROUP", "1", "");
        String clientSecret = globalParameterService.getParameterValue("HSBC_CLIENT_SECRET_API", "GROUP", "1", "");
        String bankPublicKeyPath = globalParameterService.getParameterValue("HSBC_PUBLIC_KEY_FOR_API", "GROUP", "1", "");
        String asgPrivateKeyPath = globalParameterService.getParameterValue("HSBC_ASG_PRIV_KEY_FOR_API", "GROUP", "1", "");
        String secretKey = globalParameterService.getParameterValue("HSBC_SECRET_KEY_API", "GROUP", "1", "");

        PGPSecretKey secretKeyObj = pgpHelper.readSecretKeyFromFile(asgPrivateKeyPath);
        PGPPublicKey bankPublicKey = pgpHelper.readPublicKeyFromFile(bankPublicKeyPath);

        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_HSBC_API_PYMT_STAT_XML(?)}")) {

            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();

            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    String statusXml = rs.getString("XML_OUTPUT");
                    String transactionPoid = rs.getString("TRANSACTION_POID");
                    try {
                        queryPaymentStatus(conn, transactionPoid, statusXml, apiUrl, profileId, clientSecret,
                                bankPublicKey, secretKeyObj, secretKey);
                    } catch (Exception e) {
                        log.error("Failed to query status for payment {}: {}", transactionPoid, e.getMessage(), e);
                    }
                }
            }
        }
    }

    private void queryPaymentStatus(Connection conn, String transactionPoid, String statusXml,
                                     String apiUrl, String profileId, String clientSecret,
                                     PGPPublicKey bankPublicKey, PGPSecretKey secretKeyObj, String secretKey) throws Exception {
        ByteArrayOutputStream encryptedData = new ByteArrayOutputStream();
        pgpHelper.encryptAndSign(encryptedData, new ByteArrayInputStream(statusXml.getBytes("UTF-8")),
                bankPublicKey, pgpHelper.extractPrivateKey(secretKeyObj, secretKey.toCharArray()));

        String encryptedBase64 = Base64.toBase64String(encryptedData.toByteArray());
        String payload = "{\"paymentEnquiryBase64\":\"" + encryptedBase64 + "\"}";

        String response = executeHttpPost(apiUrl, payload, profileId, clientSecret);

        JSONObject obj = JSON.parseObject(response);
        String statusCode = obj.getString("statusCode");
        String statusDesc = obj.getString("statusDesc");
        String hsbcResponseStatus = "COMPLETED";
        String extractedResponseStr = null;

        String responseVal = obj.getString("responseBase64");
        if (responseVal != null && !responseVal.trim().isEmpty() && !"PDNG".equalsIgnoreCase(statusCode)) {
            extractedResponseStr = new String(Base64.decode(responseVal));
            hsbcResponseStatus = "PENDING";
        }

        // params 2, 3, 4 are null for enquiry (matching legacy)
        updatePaymentStatus(conn, transactionPoid, null, null, null,
                hsbcResponseStatus, statusCode, statusDesc, extractedResponseStr, "PAYMENT_ENQUIRY");
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
