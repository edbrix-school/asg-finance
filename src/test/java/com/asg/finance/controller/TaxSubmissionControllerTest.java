package com.asg.finance.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.dto.CreateTaxSubmissionRequest;
import com.asg.finance.dto.LoadVatDetailsResponse;
import com.asg.finance.dto.SubmitTaxSubmissionRequest;
import com.asg.finance.dto.SubmitTaxSubmissionResponse;
import com.asg.finance.dto.TaxSubmissionResponse;
import com.asg.finance.dto.UpdateTaxSubmissionRequest;
import com.asg.finance.dto.ValidatePeriodRequest;
import com.asg.finance.dto.ValidatePeriodResponse;
import com.asg.finance.service.TaxSubmissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxSubmissionControllerTest {

    @Mock
    private TaxSubmissionService taxSubmissionService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private TaxSubmissionController controller;

    @Test
    void createTaxSubmission_ReturnsOk() {
        CreateTaxSubmissionRequest request = new CreateTaxSubmissionRequest();
        TaxSubmissionResponse response = new TaxSubmissionResponse();
        response.setTransactionPoid(100L);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserId).thenReturn("tester");
            when(taxSubmissionService.createTaxSubmission(request)).thenReturn(response);

            ResponseEntity<?> result = controller.createTaxSubmission(request);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            verify(taxSubmissionService).createTaxSubmission(request);
        }
    }

    @Test
    void getTaxSubmissionById_ReturnsOkAndLogsView() {
        TaxSubmissionResponse response = new TaxSubmissionResponse();

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getDocumentId).thenReturn("400-118");
            when(taxSubmissionService.getTaxSubmissionById(10L)).thenReturn(response);

            ResponseEntity<?> result = controller.getTaxSubmissionById(10L);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            verify(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), any(), any());
        }
    }

    @Test
    void updateTaxSubmission_ReturnsOk() {
        UpdateTaxSubmissionRequest request = new UpdateTaxSubmissionRequest();
        request.setPeriodFrom(LocalDateTime.now());
        request.setPeriodTo(LocalDateTime.now());

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            when(taxSubmissionService.updateTaxSubmission(11L, request)).thenReturn(new TaxSubmissionResponse());

            ResponseEntity<?> result = controller.updateTaxSubmission(11L, request);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            verify(taxSubmissionService).updateTaxSubmission(11L, request);
        }
    }

    @Test
    void deleteTaxSubmission_ReturnsOk() {
        DeleteReasonDto reasonDto = new DeleteReasonDto();

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);

            ResponseEntity<?> result = controller.deleteTaxSubmission(12L, reasonDto);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            verify(taxSubmissionService).deleteTaxSubmission(12L, reasonDto);
        }
    }

    @Test
    void listTaxSubmission_ReturnsOkWhenServiceSucceeds() {
        FilterRequestDto filters = new FilterRequestDto("OR", "N", List.of(new FilterDto("DOC_REF", "TS")));
        when(taxSubmissionService.listTaxSubmission(any(), any(), any(), any()))
                .thenReturn(Map.of("content", List.of()));

        ResponseEntity<?> result = controller.listTaxSubmission(
                PageRequest.of(0, 10),
                filters,
                LocalDate.now().minusDays(1),
                LocalDate.now());

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void listTaxSubmission_ReturnsInternalServerErrorWhenServiceThrows() {
        when(taxSubmissionService.listTaxSubmission(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> result = controller.listTaxSubmission(
                PageRequest.of(0, 10),
                null,
                null,
                null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertInstanceOf(Map.class, result.getBody());
    }

    @Test
    void loadVatDetails_ReturnsOk() {
        LoadVatDetailsResponse response = new LoadVatDetailsResponse();
        response.setDetails(List.of());
        when(taxSubmissionService.loadVatDetails(13L)).thenReturn(response);

        ResponseEntity<?> result = controller.loadVatDetails(13L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void submitTaxSubmission_ReturnsOk() {
        SubmitTaxSubmissionRequest request = new SubmitTaxSubmissionRequest("SUBMIT", "ok");
        SubmitTaxSubmissionResponse response = new SubmitTaxSubmissionResponse("SUBMITTED", "SUBMITTED", "done");
        when(taxSubmissionService.submitTaxSubmission(14L, request)).thenReturn(response);

        ResponseEntity<?> result = controller.submitTaxSubmission(14L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void runAfterSave_ReturnsOk() {
        when(taxSubmissionService.runAfterSave(anyLong())).thenReturn(new TaxSubmissionResponse());

        ResponseEntity<?> result = controller.runAfterSave(15L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void validatePeriod_ReturnsOk() {
        ValidatePeriodRequest request = new ValidatePeriodRequest(1L, LocalDateTime.now(), LocalDateTime.now());
        ValidatePeriodResponse response = new ValidatePeriodResponse(true, "ok", List.of());
        when(taxSubmissionService.validatePeriod(request)).thenReturn(response);

        ResponseEntity<?> result = controller.validatePeriod(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody() instanceof Map);
    }
}
