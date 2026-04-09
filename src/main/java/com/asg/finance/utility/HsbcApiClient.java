package com.asg.finance.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.asg.common.lib.client.ParameterServiceClient;
import com.asg.common.lib.exception.AsgException;
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
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleTypes;

@Component
@RequiredArgsConstructor
@Slf4j
public class HsbcApiClient {

    private final DataSource dataSource;
    private final PgpHelper pgpHelper;
    private final ParameterServiceClient parameterServiceClient;

    public void syncHsbcData(String accountNumber, String date) throws Exception {
        String apiUrl = parameterServiceClient.findParameterValueByName("HSBC_URL_FOR_API").orElseThrow(() -> new AsgException("Missing parameter: HSBC_URL_FOR_API", 400));
        String profileId = parameterServiceClient.findParameterValueByName("HSBC_PROFILE_ID_FOR_API").orElseThrow(() -> new AsgException("Missing parameter: HSBC_PROFILE_ID_FOR_API", 400));
        String clientSecret = parameterServiceClient.findParameterValueByName("HSBC_CLIENT_SECRET_API").orElseThrow(() -> new AsgException("Missing parameter: HSBC_CLIENT_SECRET_API", 400));
        String bankPublicKeyPath = parameterServiceClient.findParameterValueByName("HSBC_PUBLIC_KEY_FOR_API").orElseThrow(() -> new AsgException("Missing parameter: HSBC_PUBLIC_KEY_FOR_API", 400));
        String asgPrivateKeyPath = parameterServiceClient.findParameterValueByName("HSBC_ASG_PRIV_KEY_FOR_API").orElseThrow(() -> new AsgException("Missing parameter: HSBC_ASG_PRIV_KEY_FOR_API", 400));
        String secretKey = parameterServiceClient.findParameterValueByName("HSBC_SECRET_KEY_API").orElseThrow(() -> new AsgException("Missing parameter: HSBC_SECRET_KEY_API", 400));
        
        String requestBody = getApiRequestBody(accountNumber, date);
        
        PGPSecretKey secretKeyObj;
        PGPPublicKey bankPublicKey;
        
        try {
            secretKeyObj = pgpHelper.readSecretKeyFromFile(asgPrivateKeyPath);
            bankPublicKey = pgpHelper.readPublicKeyFromFile(bankPublicKeyPath);
        } catch (Exception e) {
            log.warn("Key files not found, using fallback for local testing: {}", e.getMessage());
            secretKeyObj = pgpHelper.generateTestSecretKey();
            bankPublicKey = pgpHelper.generateTestPublicKey();
            secretKey = "test"; // Use test password for generated keys
        }
        
        ByteArrayOutputStream encryptedData = new ByteArrayOutputStream();
        pgpHelper.encryptAndSign(encryptedData, new ByteArrayInputStream(requestBody.getBytes("UTF-8")), 
                                 bankPublicKey, pgpHelper.extractPrivateKey(secretKeyObj, secretKey.toCharArray()));
        
        String encryptedBase64 = Base64.toBase64String(encryptedData.toByteArray());
        String payload = "{\"transactionsRequestBase64\":\"" + encryptedBase64 + "\"}";
        
        String response = executeHttpPost(apiUrl + "transactions", payload, profileId, clientSecret);
        processResponse(response, accountNumber, bankPublicKey, secretKeyObj, secretKey);
    }

    private String getApiRequestBody(String accountNumber, String date) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_RET_HSBC_API_REQUEST_BODY(?,?,?,?,?,?)}")) {
            
            stmt.setString(1, "SINGLE_ACCOUNT_HISTORY_DATE");
            stmt.setString(2, accountNumber);
            stmt.setString(3, date);
            stmt.setDate(4, null);
            stmt.setDate(5, null);
            stmt.registerOutParameter(6, OracleTypes.VARCHAR);
            stmt.execute();

            String apiRequestBody = stmt.getString(6);
            if (apiRequestBody != null && apiRequestBody.contains("ERROR")) {
                throw new AsgException("HSBC API request body error: " + apiRequestBody, 400);
            }
            return apiRequestBody;
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

    private void processResponse(String response, String accountNumber, PGPPublicKey bankPublicKey, 
                                  PGPSecretKey secretKeyObj, String secretKey) throws Exception {
        JSONObject jsonResponse = JSON.parseObject(response);
        String statusCode = jsonResponse.getString("statusCode");
        String error = jsonResponse.getString("error");
        String encryptedResponse = jsonResponse.getString("reportBase64");
        
        if ("RJCT".equals(statusCode) || "RJCT".equals(error) || encryptedResponse == null || encryptedResponse.trim().isEmpty()) {
            String statusDesc = jsonResponse.getString("statusDesc");
            if (statusDesc == null) {
                statusDesc = jsonResponse.getString("description");
            }
            log.error("HSBC API request rejected - Status: {}, Description: {}, Response: {}", statusCode, statusDesc, response);
            throw new Exception("HSBC API request failed: " + statusDesc);
        }
        
        byte[] decryptedData = pgpHelper.decryptAndVerify(Base64.decode(encryptedResponse), 
                                                          pgpHelper.extractPrivateKey(secretKeyObj, secretKey.toCharArray()), 
                                                          bankPublicKey);
        
        JSONObject data = JSON.parseObject(new String(decryptedData));
        saveBalances(data, accountNumber);
        saveTransactions(data, accountNumber);
    }

    private void saveBalances(JSONObject data, String accountNumber) throws SQLException {
        JSONArray balances = data.getJSONObject("transactions").getJSONArray("balance");
        if (balances == null) return;
        
        for (int i = 0; i < balances.size(); i++) {
            JSONObject bal = balances.getJSONObject(i);
            try (Connection conn = dataSource.getConnection();
                 CallableStatement stmt = conn.prepareCall("{call PROC_UPLOAD_HSBC_API_RAW_BAL(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}")) {
                
                stmt.setString(4, "SINGLE_ACCOUNT_HISTORY_DATE");
                stmt.setString(5, accountNumber);
                stmt.setString(6, bal.getString("dateTime"));
                stmt.setString(7, bal.getJSONObject("amount").getString("amount"));
                stmt.setString(8, bal.getJSONObject("amount").getString("currency"));
                stmt.setString(11, bal.getString("type"));
                stmt.setString(12, bal.getString("creditDebitIndicator"));
                stmt.setString(13, i == 0 ? "FIRST_CALL" : "REPEAT_CALL");
                stmt.registerOutParameter(14, OracleTypes.VARCHAR);
                stmt.execute();
            }
        }
    }

    private void saveTransactions(JSONObject data, String accountNumber) throws SQLException {
        JSONArray transactions = data.getJSONObject("transactions").getJSONArray("transaction");
        if (transactions == null) return;
        
        for (int i = 0; i < transactions.size(); i++) {
            JSONObject trn = transactions.getJSONObject(i).getJSONObject("items");
            try (Connection conn = dataSource.getConnection();
                 CallableStatement stmt = conn.prepareCall("{call PROC_UPLOAD_HSBC_API_RAW_TRN(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}")) {
                
                stmt.setString(4, "SINGLE_ACCOUNT_HISTORY_DATE");
                stmt.setString(5, accountNumber);
                stmt.setString(6, trn.getString("statementReference"));
                stmt.setString(7, trn.getString("transactionReference"));
                stmt.setString(8, trn.getString("transactionStatus"));
                stmt.setString(9, trn.getString("bookingDateTime"));
                stmt.setString(10, trn.getJSONObject("transactionAmount").getString("amount"));
                stmt.setString(11, trn.getJSONObject("transactionAmount").getString("currency"));
                stmt.setString(16, trn.getString("valueDateTime"));
                stmt.setString(17, trn.getString("transactionInformation"));
                stmt.setString(18, trn.getString("creditDebitIndicator"));
                stmt.setString(19, i == 0 ? "FIRST_CALL" : "REPEAT_CALL");
                stmt.registerOutParameter(20, OracleTypes.VARCHAR);
                stmt.execute();
            }
        }
    }
}
