package com.asg.finance.controller;

import com.asg.finance.dto.ChequePrintBatchRequest;
import com.asg.finance.dto.ChequePrintBatchResponse;
import com.asg.finance.dto.ChequeStockResponse;
import com.asg.finance.dto.PendingChequeResponse;
import com.asg.finance.service.ChequePrintingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChequePrintingControllerTest {

    @Mock
    private ChequePrintingService chequePrintingService;

    @InjectMocks
    private ChequePrintingController controller;

    @Test
    void pendingCheques_ReturnsOk() {
        PendingChequeResponse response = new PendingChequeResponse();
        response.setTransactionPoid(100L);
        when(chequePrintingService.getPendingCheques()).thenReturn(List.of(response));

        ResponseEntity<?> result = controller.pendingCheques();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(chequePrintingService).getPendingCheques();
    }

    @Test
    void chequeStock_ReturnsOk() {
        ChequeStockResponse response = new ChequeStockResponse();
        response.setBank("HSBC");
        when(chequePrintingService.getChequeStock("HSBC", "NOT_SIGNED")).thenReturn(List.of(response));

        ResponseEntity<?> result = controller.chequeStock("HSBC", "NOT_SIGNED");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(chequePrintingService).getChequeStock("HSBC", "NOT_SIGNED");
    }

    @Test
    void print_ReturnsOkWhenServiceSucceeds() throws Exception {
        ChequePrintBatchRequest request = new ChequePrintBatchRequest();
        ChequePrintBatchResponse response = new ChequePrintBatchResponse();
        response.setPrintedCount(1);
        when(chequePrintingService.print(request)).thenReturn(response);

        ResponseEntity<?> result = controller.print(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(chequePrintingService).print(request);
    }

    @Test
    void print_ReturnsErrorWhenServiceThrows() throws Exception {
        ChequePrintBatchRequest request = new ChequePrintBatchRequest();
        when(chequePrintingService.print(request)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> result = controller.print(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        verify(chequePrintingService).print(request);
    }
}

