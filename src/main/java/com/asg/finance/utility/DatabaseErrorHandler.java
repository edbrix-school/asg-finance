package com.asg.finance.utility;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import com.asg.common.lib.exception.ValidationException;

/**
 * Utility class for handling database errors and converting them to user-friendly messages
 */
public class DatabaseErrorHandler {

    private DatabaseErrorHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Handle database exceptions and convert them to ValidationException with user-friendly messages
     * 
     * @param ex The exception to handle
     * @throws ValidationException with user-friendly error message
     */
    public static void handleDatabaseException(Exception ex) {
        // Check if it's a DataIntegrityViolationException
        DataIntegrityViolationException dataEx = findDataIntegrityViolationException(ex);
        if (dataEx != null) {
            throw new ValidationException(extractErrorMessage(dataEx));
        }

        // Check for Oracle errors in the exception message
        String errorMessage = extractErrorMessageFromException(ex);
        if (errorMessage != null) {
            throw new ValidationException(errorMessage);
        }

        // If it's a DataAccessException but no specific error found, throw generic message
        if (ex instanceof DataAccessException) {
            throw new ValidationException("Data validation failed. Please check your input and try again.");
        }

        // For other exceptions, rethrow as ValidationException with original message
        throw new ValidationException("Failed to save data: " + (ex.getMessage() != null ? ex.getMessage() : "Unknown error"));
    }

    /**
     * Find DataIntegrityViolationException in exception chain
     */
    private static DataIntegrityViolationException findDataIntegrityViolationException(Throwable ex) {
        if (ex == null) {
            return null;
        }
        if (ex instanceof DataIntegrityViolationException) {
            return (DataIntegrityViolationException) ex;
        }
        return findDataIntegrityViolationException(ex.getCause());
    }

    /**
     * Extract user-friendly error message from DataIntegrityViolationException
     */
    private static String extractErrorMessage(DataIntegrityViolationException ex) {
        String errorMessage = ex.getMessage();
        if (errorMessage == null) {
            return "Data validation failed. Please check your input and try again.";
        }

        // Handle Oracle PL/SQL buffer too small error (ORA-06502)
        if (errorMessage.contains("ORA-06502")) {
            if (errorMessage.contains("character string buffer too small")) {
                String fieldName = extractFieldNameFromError(errorMessage);
                if (fieldName != null) {
                    return String.format("Field '%s' exceeds maximum allowed length. Please reduce the length of the value.", fieldName);
                }
                return "One or more fields exceed the maximum allowed length. Please check your input and reduce field lengths.";
            }
            return "Data validation failed: Invalid value provided. Please check your input.";
        }

        // Handle unique constraint violations
        if (errorMessage.contains("unique constraint") || errorMessage.contains("UNIQUE") || errorMessage.contains("ORA-00001")) {
            return "A record with this value already exists. Please use a different value.";
        }

        return "Data validation failed. Please check your input and try again.";
    }

    /**
     * Extract error message from any exception that contains Oracle error codes
     * This method checks the exception and its cause chain for Oracle errors
     */
    private static String extractErrorMessageFromException(Throwable ex) {
        if (ex == null) {
            return null;
        }
        
        String errorMessage = ex.getMessage();
        if (errorMessage == null) {
            // Check cause
            return extractErrorMessageFromException(ex.getCause());
        }
        
        // Check for Oracle PL/SQL buffer too small error (ORA-06502)
        if (errorMessage.contains("ORA-06502")) {
            if (errorMessage.contains("character string buffer too small")) {
                // Try to extract field name from error message (generic approach)
                String fieldName = extractFieldNameFromError(errorMessage);
                if (fieldName != null) {
                    return String.format("Field '%s' exceeds maximum allowed length. Please reduce the length of the value.", fieldName);
                }
                return "One or more fields exceed the maximum allowed length. Please check your input and reduce field lengths.";
            }
            return "Data validation failed: Invalid value provided. Please check your input.";
        }
        
        // Handle unique constraint violations
        if (errorMessage.contains("unique constraint") || errorMessage.contains("UNIQUE") || errorMessage.contains("ORA-00001")) {
            return "A record with this value already exists. Please use a different value.";
        }
        
        // Check cause if no match found
        return extractErrorMessageFromException(ex.getCause());
    }

    /**
     * Extract field name from Oracle error message (generic approach)
     * Attempts to find column names in the error message and convert them to readable field names
     */
    private static String extractFieldNameFromError(String errorMessage) {
        // Oracle column names are typically in ALL_UPPERCASE with underscores
        // Look for patterns like: IA_NAME, IA_CODE, etc. (at least 2 uppercase letters/numbers with underscores)
        // Pattern: All uppercase, contains underscore, minimum 3 characters
        java.util.regex.Pattern columnPattern = java.util.regex.Pattern.compile("\\b([A-Z][A-Z0-9]*_[A-Z0-9_]+)\\b");
        java.util.regex.Matcher matcher = columnPattern.matcher(errorMessage);
        
        // Common English words and Oracle keywords to exclude
        java.util.Set<String> excludedWords = java.util.Set.of(
            "ORA", "PL", "SQL", "ERROR", "EXCEPTION", "TRIGGER", "TABLE", "PRODUCTION",
            "COULD", "NOT", "EXECUTE", "STATEMENT", "INSERT", "UPDATE", "DELETE", "SELECT",
            "INTO", "FROM", "WHERE", "VALUES", "SET", "DEFAULT", "LINE", "AT", "DURING"
        );
        
        // Look for column names (must contain underscore and be all uppercase)
        while (matcher.find()) {
            String potentialColumn = matcher.group(1).toUpperCase();
            
            // Must contain at least one underscore and be at least 3 characters
            if (potentialColumn.contains("_") && potentialColumn.length() >= 3) {
                // Skip excluded words and system identifiers
                if (!excludedWords.contains(potentialColumn) && 
                    !potentialColumn.startsWith("ORA-") &&
                    !potentialColumn.contains("TRI") && // Skip trigger names
                    !potentialColumn.matches(".*\\d{4,}.*")) { // Skip things with 4+ consecutive digits
                    
                    // Convert column name to readable format (e.g., IA_NAME -> IA Name)
                    String readableName = potentialColumn.replaceAll("_", " ");
                    // Capitalize first letter of each word, keep rest lowercase
                    String[] words = readableName.split("\\s+");
                    StringBuilder result = new StringBuilder();
                    for (String word : words) {
                        if (result.length() > 0) {
                            result.append(" ");
                        }
                        if (word.length() > 0) {
                            // For acronyms (all caps, short), keep as is, otherwise capitalize first letter
                            if (word.length() <= 2) {
                                result.append(word);
                            } else {
                                result.append(word.substring(0, 1));
                                if (word.length() > 1) {
                                    result.append(word.substring(1).toLowerCase());
                                }
                            }
                        }
                    }
                    return result.toString();
                }
            }
        }
        return null;
    }
}

