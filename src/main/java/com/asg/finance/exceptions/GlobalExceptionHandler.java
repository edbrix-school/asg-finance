package com.asg.finance.exceptions;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.common.lib.exception.AsgException;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AsgException.class)
	public ResponseEntity<?> handleAsgException(AsgException ex) {
		log.error("AsgException: {}", ex.getMessage());
		return ApiResponse.error(ex.getMessage(), ex.getCode());
	}

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<?> handleValidationException(ValidationException ex) {
		return ApiResponse.badRequest(ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
		return ApiResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<?> handleDateFormatException(MethodArgumentTypeMismatchException ex) {
		if (ex.getRequiredType() == LocalDate.class) {
			String value = ex.getValue() != null ? ex.getValue().toString() : null;
			String param = ex.getPropertyName();

			if (StringUtils.isBlank(value))
				return ApiResponse.badRequest("Date value cannot be empty.");

			if (!value.matches("\\d{4}-\\d{2}-\\d{2}"))
				return ApiResponse.badRequest(
						String.format("Invalid value '%s' for '%s'. Expected format: YYYY-MM-DD.", value, param));

			try {
				String[] parts = value.split("-");
				int year = Integer.parseInt(parts[0]);
				int month = Integer.parseInt(parts[1]);
				int day = Integer.parseInt(parts[2]);

				if (month < 1 || month > 12)
					return ApiResponse
							.badRequest(String.format("Invalid month '%d' in '%s'. Must be 01–12.", month, value));

				int maxDay = YearMonth.of(year, month).lengthOfMonth();
				if (day < 1 || day > maxDay)
					return ApiResponse.badRequest(String.format(
							"Invalid day '%d' for month '%02d' in '%s'. Max days: %d.", day, month, value, maxDay));

				LocalDate.parse(value); // final validation (leap year, etc.)
			} catch (DateTimeException | NumberFormatException e) {
				return ApiResponse
						.badRequest(String.format("Invalid date '%s'. Please provide a valid calendar date.", value));
			}

			return ApiResponse.badRequest(String.format("Invalid date '%s' for parameter '%s'.", value, param));
		}

		return ApiResponse.badRequest(ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		Map<String, Object> errors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

		log.info("Validation errors at {}", request.getRequestURI());
		return ApiResponse.error("Validation error occurred", HttpStatus.BAD_REQUEST.value(), errors);
	}

	@ExceptionHandler(MissingPathVariableException.class)
	public ResponseEntity<?> handleMissingPathVariable(MissingPathVariableException ex) {
		String msg = String.format("Missing path variable: '%s'", ex.getVariableName());
		return ApiResponse.badRequest(msg);
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<?> handleMissingHeader(MissingRequestHeaderException ex) {
		String msg = String.format("Missing Header variable: '%s'", ex.getHeaderName());
		return ApiResponse.badRequest(msg);
	}

	@ExceptionHandler(ResourceAlreadyExistsException.class)
	public ResponseEntity<?> handleResourceAlreadyExists(ResourceAlreadyExistsException ex) {
		return ApiResponse.conflict(ex.getMessage());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<?> handleResourceNotFound(ResourceNotFoundException ex) {
		return ApiResponse.notFound(ex.getMessage());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<?> handleNoResourceFoundException(NoResourceFoundException ex) {
		return ApiResponse.notFound(ex.getMessage());
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<?> handleNoResourceFoundException(HandlerMethodValidationException ex) {
		Map<String, String> errors = ex.getAllErrors().stream().map(error -> (ObjectError) error)
				.collect(Collectors.toMap(error -> {
					String[] codes = error.getCodes();
					if (codes != null && codes.length > 0) {
						String code = codes[0];
						return code.substring(code.lastIndexOf('.') + 1);
					}
					return error.getObjectName();
				}, ObjectError::getDefaultMessage, (existing, replacement) -> existing, LinkedHashMap::new));

		return ApiResponse.error(ex.getMessage(), 400, errors);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleGeneralException(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error at {} ", request.getRequestURI(), ex);
		return ApiResponse.error(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> handleJsonParseErrors(HttpMessageNotReadableException ex) {
		Throwable cause = ex.getMostSpecificCause();
		String message = cause != null ? cause.getMessage() : "Invalid request payload";

		if (message != null && message.contains("BigDecimal")) {
			String fieldName = extractFieldName(ex.getMessage());
			if (fieldName != null) {
				message = fieldName + " format is not correct";
			} else {
				message = "Numeric field format is not correct";
			}
		}

		return ApiResponse.error(message, HttpStatus.BAD_REQUEST.value());
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<?> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
		String msg = "File exceeds the maximum allowed upload size. Please upload a smaller file.";
		log.warn("MaxUploadSizeExceededException: {}", ex.getMessage());
		return ApiResponse.badRequest(msg);
	}

	@ExceptionHandler(InputMismatchException.class)
	public ResponseEntity<?> handleInputMismatchException(InputMismatchException ex) {
		return ApiResponse.badRequest("Invalid input format: " + ex.getMessage());
	}

	private String extractFieldName(String errorMessage) {
		if (errorMessage == null) return null;
		if (errorMessage.contains("[\"") && errorMessage.contains("\"]")) {
			int start = errorMessage.lastIndexOf("[\"");
			int end = errorMessage.indexOf("\"]", start);
			if (start != -1 && end != -1) {
				return errorMessage.substring(start + 2, end);
			}
		}
		return null;
	}
}
