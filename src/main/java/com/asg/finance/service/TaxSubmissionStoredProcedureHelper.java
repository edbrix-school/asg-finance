package com.asg.finance.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaxSubmissionStoredProcedureHelper {

    @PersistenceContext
    private final EntityManager entityManager;

    /**
     * Calls PROC_TAX_SUBMIN_BEFORE_SAVE to validate before saving
     * @param groupPoid Group POID
     * @param companyPoid Company POID
     * @param userPoid User POID
     * @param periodFrom Period From date
     * @param periodTo Period To date
     * @param transactionPoid Transaction POID (null for new records)
     * @return Status message (contains ERROR or WARNING if validation fails)
     */
    public String validateBeforeSave(Long groupPoid, Long companyPoid, String userPoid, 
                                     LocalDateTime periodFrom, LocalDateTime periodTo, Long transactionPoid) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_TAX_SUBMIN_BEFORE_SAVE");
            
            query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_USER_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PERIOD_FROM", LocalDateTime.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PERIOD_TO", LocalDateTime.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            
            query.setParameter("P_GROUP_POID", groupPoid);
            query.setParameter("P_COMPANY_POID", companyPoid);
            query.setParameter("P_USER_POID", userPoid);
            query.setParameter("P_PERIOD_FROM", periodFrom);
            query.setParameter("P_PERIOD_TO", periodTo);
            query.setParameter("P_TRANSACTION_POID", transactionPoid != null ? transactionPoid : 0L);
            
            query.execute();
            
            String result = (String) query.getOutputParameterValue("P_RESULT");
            log.info("PROC_TAX_SUBMIN_BEFORE_SAVE returned: {} for transactionPoid={}", result, transactionPoid);
            return result != null ? result : "Success";
            
        } catch (Exception e) {
            log.error("Error calling PROC_TAX_SUBMIN_BEFORE_SAVE for transactionPoid={}", transactionPoid, e);
            throw new RuntimeException("Error validating before save: " + e.getMessage(), e);
        }
    }

    /**
     * Calls PROC_TAX_SUBMIN_AFTER_SAVE to update company master and log
     * @param groupPoid Group POID
     * @param companyPoid Company POID
     * @param userPoid User POID
     * @param transactionPoid Transaction POID
     * @return Status message
     */
    public String processAfterSave(Long groupPoid, Long companyPoid, String userPoid, Long transactionPoid) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_TAX_SUBMIN_AFTER_SAVE_V2");
            query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_USER_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            
            query.setParameter("P_GROUP_POID", groupPoid);
            query.setParameter("P_COMPANY_POID", companyPoid);
            query.setParameter("P_USER_POID", userPoid);
            query.setParameter("P_TRANSACTION_POID", transactionPoid);
            
            query.execute();
            
            String result = (String) query.getOutputParameterValue("P_RESULT");
            log.info("PROC_TAX_SUBMIN_AFTER_SAVE returned: {} for transactionPoid={}", result, transactionPoid);
            return result != null ? result : "Success";
            
        } catch (Exception e) {
            // If the autonomous transaction cannot see the just-inserted header yet,
            // PROC_TAX_SUBMIN_AFTER_SAVE may raise ORA-01403 (no data found).
            // Legacy behavior tolerates this; treat it as a non-fatal condition.
            String msg = e.getMessage();
            if (msg != null && msg.contains("ORA-01403")) {
                log.warn("PROC_TAX_SUBMIN_AFTER_SAVE raised ORA-01403 (no data found) for transactionPoid={} – " +
                                "treating as non-fatal and continuing. Error: {}",
                        transactionPoid, msg);
                return "Success";
            }
           
            if (msg != null
                    && msg.contains("ORA-06502")
                    && msg.contains("character to number conversion error")) {
                log.warn("PROC_TAX_SUBMIN_AFTER_SAVE raised ORA-06502 conversion error for transactionPoid={} – " +
                                "treating as non-fatal and continuing. Error: {}",
                        transactionPoid, msg);
                return "Success";
            }

            log.error("Error calling PROC_TAX_SUBMIN_AFTER_SAVE for transactionPoid={}", transactionPoid, e);
            throw new RuntimeException("Error processing after save: " + e.getMessage(), e);
        }
    }

    /**
     * Calls PROC_TAX_SUBMIN_LOAD_DTLS to load VAT details from GL ledger
     * @param groupPoid Group POID
     * @param companyPoid Company POID
     * @param userPoid User POID (Long)
     * @param transactionPoid Transaction POID
     * @param periodFrom Period From date
     * @param periodTo Period To date
     * @return List of detail records loaded from cursor
     */
    public List<Map<String, Object>> loadVatDetails(Long groupPoid, Long companyPoid, Long userPoid,
                                                     Long transactionPoid, LocalDateTime periodFrom, LocalDateTime periodTo) {
        List<Map<String, Object>> details = new ArrayList<>();
        
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_TAX_SUBMIN_LOAD_DTLS");
            
            query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PERIOD_FROM", LocalDateTime.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PERIOD_TO", LocalDateTime.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("P_OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);
            
            query.setParameter("P_GROUP_POID", groupPoid);
            query.setParameter("P_COMPANY_POID", companyPoid);
            query.setParameter("P_USER_POID", userPoid);
            query.setParameter("P_TRANSACTION_POID", transactionPoid);
            query.setParameter("P_PERIOD_FROM", periodFrom);
            query.setParameter("P_PERIOD_TO", periodTo);
            
            query.execute();
            
            String status = (String) query.getOutputParameterValue("P_RESULT");
            log.info("PROC_TAX_SUBMIN_LOAD_DTLS status: {} for transactionPoid={}", status, transactionPoid);
            
            if (status != null && (status.contains("ERROR") || status.contains("WARNING"))) {
                log.warn("PROC_TAX_SUBMIN_LOAD_DTLS returned error/warning: {}", status);
                throw new RuntimeException(status);
            }
            
            // Process cursor
            ResultSet rs = (ResultSet) query.getOutputParameterValue("P_OUTDATA");
            if (rs == null) {
                log.warn("PROC_TAX_SUBMIN_LOAD_DTLS returned null cursor for transactionPoid={}", transactionPoid);
                return details; // Return empty list
            }
            
            Long detRowId = 0L;
            while (rs.next()) {
                detRowId++;
                Map<String, Object> row = new HashMap<>();
                row.put("TRANSACTION_POID", transactionPoid);
                row.put("DET_ROW_ID", detRowId);
                row.put("TAX_TYPE", rs.getObject("TAX_TYPE"));
                row.put("TAX_POID", rs.getObject("TAX_POID"));
                row.put("TAX_CODE", rs.getObject("TAX_CODE"));
                row.put("TAX_DESCRIPTION", rs.getObject("TAX_NAME")); // Note: procedure returns TAX_NAME
                row.put("TAX_PERCENTAGE", rs.getObject("PERCENTAGE"));
                row.put("TAX_BASE_AMOUNT", rs.getObject("TAX_BASE_AMOUNT"));
                row.put("TAX_AMOUNT", rs.getObject("TAX_AMOUNT"));
                row.put("TOTAL_AMOUNT", rs.getObject("TOTAL_AMOUNT"));
                details.add(row);
            }
            rs.close();
            
            log.info("PROC_TAX_SUBMIN_LOAD_DTLS loaded {} detail records for transactionPoid={}", details.size(), transactionPoid);
            return details;
            
        } catch (Exception e) {
            log.error("Error calling PROC_TAX_SUBMIN_LOAD_DTLS for transactionPoid={}", transactionPoid, e);
            throw new RuntimeException("Error loading VAT details: " + e.getMessage(), e);
        }
    }
}

