package com.asg.finance.controller;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.DebitNoteHeaderDto;
import com.asg.finance.service.DebitNoteService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DebitNoteController.class)
class DebitNoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DebitNoteService debitNoteService;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------
    @Test
    void testCreateDebitNote() throws Exception {

        DebitNoteHeaderDto request = new DebitNoteHeaderDto();
        request.setPartyType("SUPPLIER");
        request.setPartyPoid(100L);

        DebitNoteHeaderDto response = new DebitNoteHeaderDto();
        response.setTransactionPoid(2001L);

        Mockito.when(debitNoteService.createDebitNote(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/debit-note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Debit Note created successfully"))
                .andExpect(jsonPath("$.data.transactionPoid").value(2001L));
    }

    // ---------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------
    @Test
    void testUpdateDebitNote() throws Exception {

        DebitNoteHeaderDto request = new DebitNoteHeaderDto();
        request.setPartyType("CUSTOMER");

        DebitNoteHeaderDto response = new DebitNoteHeaderDto();
        response.setTransactionPoid(1001L);

        Mockito.when(debitNoteService.updateDebitNote(anyLong(), any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/debit-note/1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Debit Note updated successfully"))
                .andExpect(jsonPath("$.data.transactionPoid").value(1001L));
    }

    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------
    @Test
    void testDeleteDebitNote() throws Exception {

        Mockito.doNothing().when(debitNoteService).deleteDebitNote(1001L);

        mockMvc.perform(delete("/api/v1/debit-note/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Debit Note deleted successfully"));
    }

    // ---------------------------------------------------------------
    // GET
    // ---------------------------------------------------------------
    @Test
    void testGetDebitNote() throws Exception {

        DebitNoteHeaderDto response = new DebitNoteHeaderDto();
        response.setTransactionPoid(1001L);

        Mockito.when(debitNoteService.getDebitNote(1001L))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/debit-note/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Debit Note details fetched successfully"))
                .andExpect(jsonPath("$.data.transactionPoid").value(1001L));
    }

    // ---------------------------------------------------------------
    // LIST
    // ---------------------------------------------------------------
    @Test
    void testListDebitNotes() throws Exception {

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("total", 1);

        Mockito.when(debitNoteService.listDebitNotes(any(),null,null, any()))
                .thenReturn(mockResult);

        // Build filter DTO using the RECORD constructor
        List<FilterDto> filterList = List.of(
                new FilterDto("PARTY_TYPE", "SUPPLIER")
        );

        FilterRequestDto filterRequest = new FilterRequestDto(
                "AND",
                "N",
                filterList
        );

        mockMvc.perform(post("/api/v1/debit-note/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Debit Notes fetched successfully"))
                .andExpect(jsonPath("$.data.total").value(1));
    }


    // ---------------------------------------------------------------
    // LOAD FDA CHARGES
    // ---------------------------------------------------------------
    @Test
    void testLoadFdaCharges() throws Exception {

        Map<String, Object> mockResponse = Map.of("charge", 100);

        Mockito.when(debitNoteService.loadFdaCharges(500L)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/debit-note/fda/500/charges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("FDA charges loaded successfully"))
                .andExpect(jsonPath("$.data.charge").value(100));
    }

    // ---------------------------------------------------------------
    // GET TAX
    // ---------------------------------------------------------------
    @Test
    void testGetChargeTax() throws Exception {

        Map<String, Object> mockResponse = Map.of("tax", "5%");

        Mockito.when(debitNoteService.getChargeTax(10L, "SUPPLIER", 100L)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/debit-note/tax/10?partyType=SUPPLIER&partyPoid=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tax details fetched successfully"))
                .andExpect(jsonPath("$.data.tax").value("5%"));
    }

    // ---------------------------------------------------------------
    // UPDATE COST AMOUNT
    // ---------------------------------------------------------------
    @Test
    void testUpdateCostAmount() throws Exception {

        Mockito.doNothing().when(debitNoteService).updateCostAmount(anyLong());

        mockMvc.perform(post("/api/v1/debit-note/500/update-cost-amount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cost amount updated successfully"));
    }

    // ---------------------------------------------------------------
    // CHECK SAIL DATE
    // ---------------------------------------------------------------
    @Test
    void testCheckSailDate() throws Exception {

        Map<String, Object> mockResponse = Map.of("valid", true);

        Mockito.when(debitNoteService.checkSailDate(100L)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/debit-note/fda/100/sail-date-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sail date validation completed"))
                .andExpect(jsonPath("$.data.valid").value(true));
    }

    // ---------------------------------------------------------------
    // PARTY DEFAULTS
    // ---------------------------------------------------------------
    @Test
    void testGetPartyDefaults() throws Exception {

        Map<String, Object> mockResponse = Map.of("creditPeriod", 10);

        Mockito.when(debitNoteService.getPartyDefaults(100L, "SUPPLIER"))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/debit-note/party-defaults/100?partyType=SUPPLIER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Party default values loaded"))
                .andExpect(jsonPath("$.data.creditPeriod").value(10));
    }
}


