package com.asg.finance.utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.sql.DataSource;

import com.asg.common.lib.client.ParameterServiceClient;
import com.asg.common.lib.exception.AsgException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
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

    private static final String REQUEST_TYPE       = "SINGLE_ACCOUNT_HISTORY_DATE";
    private static final String TXN_URL_SEGMENT    = "transactions";
    private static final String AMOUNT_FIELD       = "amount";
    private static final String CURRENCY_FIELD     = "currency";

    private final DataSource dataSource;
    private final PgpHelper pgpHelper;
    private final ParameterServiceClient parameterServiceClient;

    // -------------------------------------------------------------------------
    // Sync context — groups session-level params to avoid long method signatures
    // -------------------------------------------------------------------------
    private static class SyncContext {
        final long groupPoid;
        final long companyPoid;
        final long userPoid;
        final String calledFrom;

        SyncContext(long groupPoid, long companyPoid, long userPoid, String calledFrom) {
            this.groupPoid  = groupPoid;
            this.companyPoid = companyPoid;
            this.userPoid   = userPoid;
            this.calledFrom = calledFrom;
        }
    }

    // -------------------------------------------------------------------------
    // writeLog — mirrors legacy WriteLog:
    //   • logs via SLF4J at INFO level
    //   • appends to daily file: logs/hsbc_dd-MMM-yyyy.log
    // -------------------------------------------------------------------------
    private void writeLog(String calledFrom, String message) {
        String logLine = "[" + calledFrom + "] " + message;
        log.info(logLine);
        appendToLogFile(logLine);
    }

    private void writeLog(String calledFrom, String message, Throwable t) {
        String logLine = "[" + calledFrom + "] " + message;
        log.info(logLine);
        log.error(logLine, t);
        appendToLogFile(logLine + " | ERROR: " + t.getMessage());
    }

    private void appendToLogFile(String logLine) {
        try {
            String dateStamp = new SimpleDateFormat("dd-MMM-yyyy").format(new Date());
            String timestamp = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss").format(new Date());
            File logDir = new File("logs");
            if (!logDir.exists() && !logDir.mkdirs()) {
                log.warn("Could not create logs directory");
                return;
            }
            try (FileWriter writer = new FileWriter("logs/hsbc_" + dateStamp + ".log", true)) {
                writer.write(timestamp + "    " + logLine);
                writer.write(System.lineSeparator());
            }
        } catch (IOException e) {
            log.warn("Failed to write to HSBC log file: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void syncAllAccounts(String date, long groupPoid, long companyPoid, long userPoid, String calledFrom) throws SQLException {
        SyncContext ctx = new SyncContext(groupPoid, companyPoid, userPoid, calledFrom);

        String accountsParam = parameterServiceClient.findParameterValueByName("HSBC_ACCOUNTS_FOR_API")
                .orElseThrow(() -> new AsgException("Missing parameter: HSBC_ACCOUNTS_FOR_API", 400));

        String[] accounts = accountsParam.contains(",") ? accountsParam.split(",") : new String[]{accountsParam};

        for (String account : accounts) {
            String accountNumber = account.trim();
            if (!accountNumber.isEmpty()) {
                writeLog(ctx.calledFrom, "Starting sync for account: " + accountNumber + ", date: " + date);
                try {
                    syncHsbcData(accountNumber, date, ctx);
                } catch (Exception e) {
                    writeLog(ctx.calledFrom, "Failed to sync account: " + accountNumber, e);
                }
            }
        }

        writeLog(ctx.calledFrom, "All accounts processed. Starting reconciliation for date: " + date);
        performStmtReconcile(date, date);
        writeLog(ctx.calledFrom, "Reconciliation completed for date: " + date);
    }

    public void syncHsbcData(String accountNumber, String date, long groupPoid, long companyPoid, long userPoid, String calledFrom) throws Exception {
        syncHsbcData(accountNumber, date, new SyncContext(groupPoid, companyPoid, userPoid, calledFrom));
    }

    private void syncHsbcData(String accountNumber, String date, SyncContext ctx) throws Exception {
        writeLog(ctx.calledFrom, "syncHsbcData started - account: " + accountNumber + ", date: " + date);

        String apiUrl        = parameterServiceClient.findParameterValueByName("HSBC_URL_FOR_API").orElseThrow(() -> new AsgException("Missing parameter: HSBC_URL_FOR_API", 400));
        String profileId     = parameterServiceClient.findParameterValueByName("HSBC_PROFILE_ID_FOR_API").orElseThrow(() -> new AsgException("Missing parameter: HSBC_PROFILE_ID_FOR_API", 400));
        String clientSecret  = parameterServiceClient.findParameterValueByName("HSBC_CLIENT_SECRET_API").orElseThrow(() -> new AsgException("Missing parameter: HSBC_CLIENT_SECRET_API", 400));
        String bankPubPath   = parameterServiceClient.findParameterValueByName("HSBC_PUBLIC_KEY_FOR_API").orElseThrow(() -> new AsgException("Missing parameter: HSBC_PUBLIC_KEY_FOR_API", 400));
        String asgPrivPath   = parameterServiceClient.findParameterValueByName("HSBC_ASG_PRIV_KEY_FOR_API").orElseThrow(() -> new AsgException("Missing parameter: HSBC_ASG_PRIV_KEY_FOR_API", 400));
        String secretKey     = parameterServiceClient.findParameterValueByName("HSBC_SECRET_KEY_API").orElseThrow(() -> new AsgException("Missing parameter: HSBC_SECRET_KEY_API", 400));

        String requestBody = getApiRequestBody(accountNumber, date);
        if (requestBody == null) {
            throw new AsgException("API request body returned null for account: " + accountNumber, 400);
        }
        writeLog(ctx.calledFrom, "API request body retrieved for account: " + accountNumber);

        PGPSecretKey secretKeyObj;
        PGPPublicKey bankPublicKey;

        try {
            secretKeyObj  = pgpHelper.readSecretKeyFromFile(asgPrivPath);
            bankPublicKey = pgpHelper.readPublicKeyFromFile(bankPubPath);
            writeLog(ctx.calledFrom, "PGP keys loaded successfully");
        } catch (Exception e) {
            writeLog(ctx.calledFrom, "Key files not found, using fallback for local testing: " + e.getMessage());
            secretKeyObj  = pgpHelper.generateTestSecretKey();
            bankPublicKey = pgpHelper.generateTestPublicKey();
            secretKey     = "test";
        }

        ByteArrayOutputStream encryptedData = new ByteArrayOutputStream();
        pgpHelper.encryptAndSign(encryptedData,
                new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8)),
                bankPublicKey, pgpHelper.extractPrivateKey(secretKeyObj, secretKey.toCharArray()));
        writeLog(ctx.calledFrom, "Request payload encrypted and signed successfully");

        String encryptedBase64 = Base64.toBase64String(encryptedData.toByteArray());
        String payload = "{\"transactionsRequestBase64\":\"" + encryptedBase64 + "\"}";

        String response = executeHttpPost(apiUrl + TXN_URL_SEGMENT, payload, profileId, clientSecret);
        writeLog(ctx.calledFrom, "API response received for account: " + accountNumber);

        processResponse(response, accountNumber, bankPublicKey, secretKeyObj, secretKey, ctx);
        writeLog(ctx.calledFrom, "syncHsbcData completed - account: " + accountNumber);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String getApiRequestBody(String accountNumber, String date) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_RET_HSBC_API_REQUEST_BODY(?,?,?,?,?,?)}")) {

            stmt.setString(1, REQUEST_TYPE);
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

    private String executeHttpPost(String url, String payload, String profileId, String clientSecret) throws IOException {
        try {
            var sslContext = new SSLContextBuilder().loadTrustMaterial(null, (chain, authType) -> true).build();
            var sslFactory = new SSLConnectionSocketFactory(sslContext);
            var connectionManager = PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(sslFactory).build();

            try (CloseableHttpClient client = HttpClients.custom().setConnectionManager(connectionManager).build()) {
                HttpPost post = new HttpPost(url);
                post.setHeader("x-hsbc-client-secret", clientSecret);
                post.setHeader("x-hsbc-profile-id", profileId);
                post.setHeader("Content-Type", "application/json");
                post.setEntity(new StringEntity(payload));
                return client.execute(post, response -> EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            throw new IOException("HTTP POST failed: " + e.getMessage(), e);
        }
    }

    private void processResponse(String response, String accountNumber, PGPPublicKey bankPublicKey,
                                  PGPSecretKey secretKeyObj, String secretKey, SyncContext ctx) throws Exception {
        JSONObject jsonResponse = JSON.parseObject(response);
        String statusCode       = jsonResponse.getString("statusCode");
        String error            = jsonResponse.getString("error");
        String encryptedResponse = jsonResponse.getString("reportBase64");

        if ("RJCT".equals(statusCode) || "RJCT".equals(error) || encryptedResponse == null || encryptedResponse.trim().isEmpty()) {
            String statusDesc = jsonResponse.getString("statusDesc");
            if (statusDesc == null) statusDesc = jsonResponse.getString("description");
            writeLog(ctx.calledFrom, "HSBC API request rejected - Status: " + statusCode + ", Description: " + statusDesc);
            throw new AsgException("HSBC API request failed: " + statusDesc, 400);
        }

        byte[] decryptedData = pgpHelper.decryptAndVerify(Base64.decode(encryptedResponse),
                pgpHelper.extractPrivateKey(secretKeyObj, secretKey.toCharArray()), bankPublicKey);
        writeLog(ctx.calledFrom, "Response decrypted successfully for account: " + accountNumber);

        JSONObject data = JSON.parseObject(new String(decryptedData, StandardCharsets.UTF_8));
        saveBalances(data, accountNumber, ctx);
        saveTransactions(data, accountNumber, ctx);
    }

    private void saveBalances(JSONObject data, String accountNumber, SyncContext ctx) throws SQLException {
        JSONArray balances = data.getJSONObject(TXN_URL_SEGMENT).getJSONArray("balance");
        if (balances == null) {
            writeLog(ctx.calledFrom, "No balance records found for account: " + accountNumber);
            return;
        }

        writeLog(ctx.calledFrom, "Saving " + balances.size() + " balance record(s) for account: " + accountNumber);

        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_UPLOAD_HSBC_API_RAW_BAL(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}")) {

            // Invariant params — set once outside the loop
            stmt.setLong(1, ctx.groupPoid);
            stmt.setLong(2, ctx.companyPoid);
            stmt.setLong(3, ctx.userPoid);
            stmt.setString(4, REQUEST_TYPE);
            stmt.setString(5, accountNumber);
            stmt.registerOutParameter(14, OracleTypes.VARCHAR);

            for (int i = 0; i < balances.size(); i++) {
                JSONObject bal = balances.getJSONObject(i);

                String creditAmount = null;
                String creditCurrency = null;
                JSONArray creditLineArray = bal.getJSONArray("creditLine");
                if (creditLineArray != null && !creditLineArray.isEmpty()) {
                    JSONObject creditLineAmount = creditLineArray.getJSONObject(0).getJSONObject(AMOUNT_FIELD);
                    if (creditLineAmount != null) {
                        creditAmount   = creditLineAmount.getString(AMOUNT_FIELD);
                        creditCurrency = creditLineAmount.getString(CURRENCY_FIELD);
                    }
                }

                stmt.setString(6,  bal.getString("dateTime"));
                stmt.setString(7,  bal.getJSONObject(AMOUNT_FIELD).getString(AMOUNT_FIELD));
                stmt.setString(8,  bal.getJSONObject(AMOUNT_FIELD).getString(CURRENCY_FIELD));
                stmt.setString(9,  creditAmount);
                stmt.setString(10, creditCurrency);
                stmt.setString(11, bal.getString("type"));
                stmt.setString(12, bal.getString("creditDebitIndicator"));
                stmt.setString(13, i == 0 ? "FIRST_CALL" : "REPEAT_CALL");
                stmt.execute();

                writeLog(ctx.calledFrom, "Balance record " + (i + 1) + " saved - status: " + stmt.getString(14));
            }
        }
    }

    public void performStmtReconcile(String fromDate, String toDate) throws SQLException {
        String filterParameter = "BOOKING_DATETIME=" + fromDate + ";BOOKING_DATETIME2=" + toDate + ";BANK_PREFIX=HSBC;";
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_HSBC_API_AUTO_RECONCILE(?,?,?,?,?,?,?)}")) {

            stmt.setObject(1, null);
            stmt.setObject(2, null);
            stmt.setObject(3, null);
            stmt.setString(4, null);
            stmt.setString(5, filterParameter);
            stmt.setString(6, null);
            stmt.registerOutParameter(7, OracleTypes.VARCHAR);
            stmt.execute();

            log.info("performStmtReconcile result: {}", stmt.getString(7));
        }
    }

    private void saveTransactions(JSONObject data, String accountNumber, SyncContext ctx) throws SQLException {
        JSONArray transactions = data.getJSONObject(TXN_URL_SEGMENT).getJSONArray("transaction");
        if (transactions == null) {
            writeLog(ctx.calledFrom, "No transaction records found for account: " + accountNumber);
            return;
        }

        writeLog(ctx.calledFrom, "Saving " + transactions.size() + " transaction record(s) for account: " + accountNumber);

        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_UPLOAD_HSBC_API_RAW_TRN(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}")) {

            // Invariant params — set once outside the loop
            stmt.setLong(1, ctx.groupPoid);
            stmt.setLong(2, ctx.companyPoid);
            stmt.setLong(3, ctx.userPoid);
            stmt.setString(4, REQUEST_TYPE);
            stmt.setString(5, accountNumber);
            stmt.registerOutParameter(20, OracleTypes.VARCHAR);

            for (int i = 0; i < transactions.size(); i++) {
                JSONObject trn             = transactions.getJSONObject(i).getJSONObject("items");
                JSONObject bankTxnCode     = trn.getJSONObject("bankTransactionCode");
                JSONObject proprietaryCode = trn.getJSONObject("proprietaryBankTransactionCode");

                stmt.setString(6,  trn.getString("statementReference"));
                stmt.setString(7,  trn.getString("transactionReference"));
                stmt.setString(8,  trn.getString("transactionStatus"));
                stmt.setString(9,  trn.getString("bookingDateTime"));
                stmt.setString(10, trn.getJSONObject("transactionAmount").getString(AMOUNT_FIELD));
                stmt.setString(11, trn.getJSONObject("transactionAmount").getString(CURRENCY_FIELD));
                stmt.setString(12, bankTxnCode     != null ? bankTxnCode.getString("code")     : null);
                stmt.setString(13, bankTxnCode     != null ? bankTxnCode.getString("subcode")  : null);
                stmt.setString(14, proprietaryCode != null ? proprietaryCode.getString("code")   : null);
                stmt.setString(15, proprietaryCode != null ? proprietaryCode.getString("issuer") : null);
                stmt.setString(16, trn.getString("valueDateTime"));
                stmt.setString(17, trn.getString("transactionInformation"));
                stmt.setString(18, trn.getString("creditDebitIndicator"));
                stmt.setString(19, i == 0 ? "FIRST_CALL" : "REPEAT_CALL");
                stmt.execute();

                writeLog(ctx.calledFrom, "Transaction record " + (i + 1) + " saved - status: " + stmt.getString(20));
            }
        }
    }
}
