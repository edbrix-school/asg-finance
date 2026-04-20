package com.asg.finance.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private static final String GROUP = "GROUP";

    private final DataSource dataSource;
    private final PgpHelper pgpHelper;
    private final GlobalParameterService globalParameterService;

    public void processPendingPayments() throws Exception {
        log.info("Starting HSBC payment processing");

        String apiUrl = globalParameterService.getParameterValue("HSBC_URL_FOR_BULK_PAYMENT_API", GROUP, "1", "");
        String profileId = globalParameterService.getParameterValue("HSBC_PROFILE_ID_FOR_API_PAYMENT", GROUP, "1", "");
        String bankPublicKeyPath = globalParameterService.getParameterValue("HSBC_PUBLIC_KEY_FOR_API_PAYMENT", GROUP, "1", "");
        String asgPrivateKeyPath = globalParameterService.getParameterValue("HSBC_ASG_PRIV_KEY_FOR_API_PAYMENT", GROUP, "1", "");
        String secretKey = globalParameterService.getParameterValue("HSBC_SECRET_KEY_API", GROUP, "1", "");

        Map<String, String> headers = buildHeaders(profileId);

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
                        processPayment(conn, transactionPoid, paymentXml, apiUrl, profileId, headers,
                                bankPublicKey, secretKeyObj, secretKey);
                    } catch (Exception e) {
                        log.error("Failed to process payment {}: {}", transactionPoid, e.getMessage(), e);
                    }
                }
            }
        }
    }

    private Map<String, String> buildHeaders(String profileId) {
        String clientSecret = globalParameterService.getParameterValue("HSBC_CLIENT_SECRET_API_PAYMENT", GROUP, "1", "");
        String contentType = globalParameterService.getParameterValue("HSBC_CONTENT_TYPE_API", GROUP, "1", "");
        String countryCode = globalParameterService.getParameterValue("HSBC_CONUNTRY_CODE_API", GROUP, "1", "");
        String payLoadType = globalParameterService.getParameterValue("HSBC_PAYLOAD_TYPE_API_PAYMENT", GROUP, "1", "");
        String transactionType = globalParameterService.getParameterValue("HSBC_TRANSACTION_TYPE_API_PAYMENT", GROUP, "1", "");

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-hsbc-client-secret", clientSecret.trim());
        headers.put("x-hsbc-profile-id", profileId);
        headers.put("Content-Type", contentType.trim());
        headers.put("countryCode", countryCode.trim());
        headers.put("x-payload-type", payLoadType.trim());
        headers.put("x-trans-type", transactionType.trim());
        return headers;
    }

    private void processPayment(Connection conn, String transactionPoid, String paymentXml,
                                 String apiUrl, String profileId, Map<String, String> headers,
                                 PGPPublicKey bankPublicKey, PGPSecretKey secretKeyObj, String secretKey) throws Exception {
        ByteArrayOutputStream encryptedData = new ByteArrayOutputStream();
        pgpHelper.encryptAndSign(encryptedData, new ByteArrayInputStream(paymentXml.getBytes(StandardCharsets.UTF_8)),
                bankPublicKey, pgpHelper.extractPrivateKey(secretKeyObj, secretKey.toCharArray()));

        String encryptedBase64 = Base64.toBase64String(encryptedData.toByteArray());
        String payload = "{\"paymentBase64\":\"" + encryptedBase64 + "\"}";

        String response = executeHttpPost(apiUrl, payload, headers);

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

    private String executeHttpPost(String url, String payload, Map<String, String> headers) throws Exception {
        System.out.println("==== HSBC CURL REQUEST ====");
        StringBuilder curl = new StringBuilder("curl -X POST '").append(url).append("' \\\n");
        for (Map.Entry<String, String> h : headers.entrySet()) {
            curl.append("  -H '").append(h.getKey()).append(": ").append(h.getValue()).append("' \\\n");
        }
        curl.append("  -d '").append(payload).append("'");
        System.out.println(curl);
        System.out.println("===========================");

        var sslContext = new SSLContextBuilder().loadTrustMaterial(null, (chain, authType) -> true).build();
        var sslFactory = new SSLConnectionSocketFactory(sslContext);
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(sslFactory).build();

        try (CloseableHttpClient client = HttpClients.custom().setConnectionManager(connectionManager).build()) {
            HttpPost post = new HttpPost(url);
            for (Map.Entry<String, String> h : headers.entrySet()) {
                post.setHeader(h.getKey(), h.getValue());
            }
            post.setEntity(new StringEntity(payload));

            try (CloseableHttpResponse response = client.execute(post)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                log.info("HSBC response status: {}, body: {}", statusCode, responseBody);
                return responseBody;
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

        String apiUrl = globalParameterService.getParameterValue("HSBC_URL_FOR_PAYMENT_STATUS_API", GROUP, "1", "");
        String profileId = globalParameterService.getParameterValue("HSBC_PROFILE_ID_FOR_API_PAYMENT", GROUP, "1", "");
        String bankPublicKeyPath = globalParameterService.getParameterValue("HSBC_PUBLIC_KEY_FOR_API_PAYMENT", GROUP, "1", "");
        String asgPrivateKeyPath = globalParameterService.getParameterValue("HSBC_ASG_PRIV_KEY_FOR_API_PAYMENT", GROUP, "1", "");
        String secretKey = globalParameterService.getParameterValue("HSBC_SECRET_KEY_API", GROUP, "1", "");

        Map<String, String> headers = buildHeaders(profileId);

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
                        queryPaymentStatus(conn, transactionPoid, statusXml, apiUrl, headers,
                                bankPublicKey, secretKeyObj, secretKey);
                    } catch (Exception e) {
                        log.error("Failed to query status for payment {}: {}", transactionPoid, e.getMessage(), e);
                    }
                }
            }
        }
    }

    private void queryPaymentStatus(Connection conn, String transactionPoid, String statusXml,
                                     String apiUrl, Map<String, String> headers,
                                     PGPPublicKey bankPublicKey, PGPSecretKey secretKeyObj, String secretKey) throws Exception {
        ByteArrayOutputStream encryptedData = new ByteArrayOutputStream();
        pgpHelper.encryptAndSign(encryptedData, new ByteArrayInputStream(statusXml.getBytes(StandardCharsets.UTF_8)),
                bankPublicKey, pgpHelper.extractPrivateKey(secretKeyObj, secretKey.toCharArray()));

        String encryptedBase64 = Base64.toBase64String(encryptedData.toByteArray());
        String payload = "{\"paymentEnquiryBase64\":\"" + encryptedBase64 + "\"}";

        String response = executeHttpPost(apiUrl, payload, headers);

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
