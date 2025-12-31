package com.asg.finance.repository;

import com.asg.finance.dto.GlMasterTreeRequest;
import oracle.jdbc.OracleTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class for GL Master Tree View operations
 * Handles database procedure calls for tree structure retrieval
 */
@Repository
public class GLMasterTreeViewRepository {

    private static final Logger log = LoggerFactory.getLogger(GLMasterTreeViewRepository.class);

    @Autowired
    private DataSource dataSource;

    /**
     * Call the GL Master tree view procedure
     *
     * @param documentId Document ID
     * @param actionRequested Action requested
     * @param request Tree request parameters
     * @return List of tree view data records
     * @throws SQLException if database operation fails
     */
    public List<Map<String, Object>> callGlMasterTreeViewProcedure(String documentId, String actionRequested, GlMasterTreeRequest request) throws SQLException {
        String sql = "{ call PROC_GLOB_DOC_TREEVIEW_LOAD(?, ?, ?, ?, ?, ?, ?, ?, ?) }";

        List<Map<String, Object>> treeViewData = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            // Set input parameters
            cs.setLong(1, request.getGroupPoid() != null ? request.getGroupPoid() : 1L);           // P_LOGIN_GROUP_POID
            cs.setLong(2, request.getCompanyPoid() != null ? request.getCompanyPoid() : 1L);       // P_LOGIN_COMPANY_POID
            cs.setLong(3, request.getUserPoid() != null ? request.getUserPoid() : 100L);           // P_LOGIN_USER_POID
            cs.setString(4, documentId);                                                           // P_DOC_ID
            cs.setString(5, null);                                            // P_FILTER_FIELD1
            cs.setString(6, request.getFilterValue());                                            // P_FILTER_VALUE1
            cs.setString(7, null);                                            // P_FILTER_FIELD2
            cs.setString(8, null);                                            // P_FILTER_VALUE2

            // Register output cursor parameter
            cs.registerOutParameter(9, OracleTypes.CURSOR);              // OUTDATA

            log.debug("Calling PROC_GLOB_DOC_TREEVIEW_LOAD for GL Master tree with documentId: {}, actionRequested: {}", 
                     documentId, actionRequested);
            cs.execute();

            // Get cursor results
            try (ResultSet rs = (ResultSet) cs.getObject(9)) {
                if (rs != null) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    log.debug("Procedure returned {} columns: {}", columnCount, 
                            java.util.Arrays.toString(java.util.stream.IntStream.range(1, columnCount + 1)
                                    .mapToObj(i -> {
                                        try {
                                            return metaData.getColumnName(i);
                                        } catch (SQLException e) {
                                            return "Unknown";
                                        }
                                    }).toArray()));
                    
                    while (rs.next()) {
                        Map<String, Object> record = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            record.put(columnName, value);
                        }
                        treeViewData.add(record);

                        // Log first few records for debugging
                        if (treeViewData.size() <= 3) {
                            log.debug("Record {}: LVL={}, POID={}, DESCRIPTION={}, PARENT_POID={}, ITEM_TYPE={}, GL_TYPE={}",
                                    treeViewData.size(),
                                    record.get("LVL"),
                                    record.get("POID"),
                                    record.get("DESCRIPTION"),
                                    record.get("PARENT_POID"),
                                    record.get("ITEM_TYPE"),
                                    record.get("GL_TYPE"));
                        }
                    }
                }
            }

            log.debug("PROC_GLOB_DOC_TREEVIEW_LOAD returned {} records for GL Master tree", treeViewData.size());

        } catch (SQLException e) {
            log.error("Error while calling stored procedure PROC_GLOB_DOC_TREEVIEW_LOAD", e);
            throw new SQLException("Error while calling PROC_GLOB_DOC_TREEVIEW_LOAD", e);
        }

        return treeViewData;
    }
}