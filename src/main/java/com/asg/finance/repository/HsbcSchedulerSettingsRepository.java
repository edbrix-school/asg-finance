package com.asg.finance.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class HsbcSchedulerSettingsRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SELECT_SQL =
            "SELECT ACTION_ID, ACTION_TYPE, DESCRIPTION, ACTIVE " +
            "FROM GLOBAL_SERVER_UTILITY_SETTINGS " +
            "WHERE ACTION_TYPE = ? AND NVL(ACTIVE, 'Y') = 'Y'";

    private static final String UPDATE_SQL =
            "UPDATE GLOBAL_SERVER_UTILITY_SETTINGS " +
            "SET LAST_EXECUTION_TIME = ? " +
            "WHERE ACTION_TYPE = ?";

    /**
     * Returns true if the action type exists and is ACTIVE = 'Y'.
     */
    public boolean isActive(String actionType) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM GLOBAL_SERVER_UTILITY_SETTINGS " +
                    "WHERE ACTION_TYPE = ? AND NVL(ACTIVE, 'Y') = 'Y'",
                    Integer.class, actionType);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Failed to check active status for {}: {}", actionType, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Returns the DESCRIPTION for the action type, used for log messages.
     */
    public Optional<String> getDescription(String actionType) {
        try {
            return jdbcTemplate.query(SELECT_SQL, rs -> {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("DESCRIPTION"));
                }
                return Optional.<String>empty();
            }, actionType);
        } catch (Exception e) {
            log.error("Failed to get description for {}: {}", actionType, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Updates LAST_EXECUTION_TIME before the task runs — matches legacy
     * common.updateExecutiontime(actionId, lastExecutionTimeStr).
     */
    public void updateLastExecutionTime(String actionType, String lastExecutionTimeStr) {
        try {
            jdbcTemplate.update(UPDATE_SQL, lastExecutionTimeStr, actionType);
        } catch (Exception e) {
            log.error("Failed to update LAST_EXECUTION_TIME for {}: {}", actionType, e.getMessage(), e);
        }
    }
}
