//package com.asg.finance.controller;
//
//import com.asg.finance.dto.ApPurchaseInvoiceHdrDto;
//import com.asg.finance.service.ApPurchaseServiceJournal;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.HashMap;
//import java.util.Map;
//import static org.mockito.ArgumentMatchers.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.FilterType;
//
//@WebMvcTest(controllers = ApPurchaseJournalController.class,
//        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.asg\\.security\\..*"))
//@AutoConfigureMockMvc(addFilters = false)
//class ApPurchaseJournalControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private ApPurchaseServiceJournal service;
//
//
//    @Test
//    @DisplayName("GET /v1/ap-purchase-journal/{transactionPoid} returns 200")
//    void getApPurchaseInvoiceHdr_success() throws Exception {
//        Long txPoid = 71031L;
//        Mockito.when(service.fetchApPurchaseInvoiceHdr(eq(txPoid))).thenReturn(new ApPurchaseInvoiceHdrDto());
//
//        mockMvc.perform(get("/v1/ap-purchase-journal/{transactionPoid}", txPoid)
//                   )
//                .andExpect(status().isOk());
//
//        Mockito.verify(service).fetchApPurchaseInvoiceHdr(eq(txPoid));
//    }
//
//    @Test
//    @DisplayName("POST /v1/ap-purchase-journal/{transactionPoid} returns 200")
//    void createApPurchaseInvoice_success() throws Exception {
//        ApPurchaseInvoiceHdrDto req = new ApPurchaseInvoiceHdrDto();
//        Mockito.when(service.createApPurchaseInvoice(any(ApPurchaseInvoiceHdrDto.class), anyString())).thenReturn(new ApPurchaseInvoiceHdrDto());
//
//        mockMvc.perform(post("/v1/ap-purchase-journal/{transactionPoid}", 0)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req)))
//                .andExpect(status().isOk());
//
//        Mockito.verify(service).createApPurchaseInvoice(any(ApPurchaseInvoiceHdrDto.class), eq("200-103"));
//    }
//
//    @Test
//    @DisplayName("PUT /v1/ap-purchase-journal/{transactionPoid} returns 200")
//    void updateApPurchaseInvoice_success() throws Exception {
//        Long txPoid = 71031L;
//        ApPurchaseInvoiceHdrDto req = new ApPurchaseInvoiceHdrDto();
//        Mockito.when(service.updateApPurchaseInvoice(eq(txPoid), any(ApPurchaseInvoiceHdrDto.class))).thenReturn(new ApPurchaseInvoiceHdrDto());
//
//        mockMvc.perform(put("/v1/ap-purchase-journal/{transactionPoid}", txPoid)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req)))
//                .andExpect(status().isOk());
//
//        Mockito.verify(service).updateApPurchaseInvoice(eq(txPoid), any(ApPurchaseInvoiceHdrDto.class));
//    }
//
//    @Test
//    @DisplayName("DELETE /v1/ap-purchase-journal/{transactionPoid} returns 200")
//    void softDeleteApPurchaseInvoice_success() throws Exception {
//        Long txPoid = 71031L;
//        Mockito.when(service.softDeleteApPurchaseInvoice(eq(txPoid), anyString()))
//                .thenReturn(new ApPurchaseInvoiceHdrDto());
//
//        mockMvc.perform(delete("/v1/ap-purchase-journal/{transactionPoid}", txPoid)
//                       )
//                .andExpect(status().isOk());
//
//        Mockito.verify(service).softDeleteApPurchaseInvoice(eq(txPoid), eq("200-103"));
//    }
//
//    @Test
//    @DisplayName("POST /v1/ap-purchase-journal/list returns 200")
//    void listOfRecordsWithGenericSearch_success() throws Exception {
//        Map<String, Object> pagePayload = new HashMap<>();
//        pagePayload.put("content", java.util.List.of());
//        pagePayload.put("totalElements", 0);
//        Mockito.when(service.listOfRecordsAndGenericSearch(anyString(), any(), any(), any(), any())).thenReturn(pagePayload);
//
//        String body = "{\n" +
//                "  \"operator\": \"AND\",\n" +
//                "  \"isDeleted\": \"N\",\n" +
//                "  \"filters\": [{ \"searchField\": \"DOC_REF\", \"searchValue\": \"ASG54210\" }]\n" +
//                "}";
//
//        mockMvc.perform(post("/v1/ap-purchase-journal/list")
//                        .param("page", "0")
//                        .param("size", "10")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().isOk());
//
//        Mockito.verify(service).listOfRecordsAndGenericSearch(eq("200-103"), any(), any(), any(), any());
//    }
//}
//
