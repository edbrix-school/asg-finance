//package com.asg.finance.controller;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.finance.dto.GlBankDto;
//import com.asg.finance.entity.GlBankEntity;
//import com.asg.finance.exceptions.GlobalExceptionHandler;
//import com.asg.finance.service.GlBankService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(MockitoExtension.class)
//class GlBankControllerTest {
//
//    @Mock
//    private GlBankService bankService;
//
//    @InjectMocks
//    private GlBankController glBankController;
//
//    private MockMvc mockMvc;
//    private ObjectMapper objectMapper;
//
//    @BeforeEach
//    void setUp() {
//        PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();
//        pageableResolver.setFallbackPageable(PageRequest.of(0, 20));
//
//        mockMvc = MockMvcBuilders.standaloneSetup(glBankController)
//                .setControllerAdvice(new GlobalExceptionHandler())
//                .setCustomArgumentResolvers(pageableResolver)
//                .build();
//
//        objectMapper = new ObjectMapper();
//        objectMapper.findAndRegisterModules();
//    }
//
//    @Test
//    void updateBankMaster_Success() throws Exception {
//        GlBankDto responseDto = new GlBankDto();
//        responseDto.setBankPoid(1L);
//
//        when(bankService.updateGlBank(eq(1L), any(GlBankDto.class))).thenReturn(responseDto);
//
//        String validJson = "{\"bankCode\":\"BANK001\",\"bankDescription\":\"Test Bank\",\"glPoid\":100,\"bankAccountNo\":\"123456789\",\"companyPoid\":1,\"bankPrefix\":\"TB\"}";
//
//        mockMvc.perform(put("/v1/bank-master/1")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "EDIT")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(validJson))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Bank Master updated successfully"));
//
//        verify(bankService).updateGlBank(eq(1L), any(GlBankDto.class));
//    }
//
//    @Test
//    void updateBankMaster_NotFound() throws Exception {
//        when(bankService.updateGlBank(eq(999L), any(GlBankDto.class)))
//                .thenThrow(new ResourceNotFoundException("Bank", "bankPoid", 999L));
//
//        String validJson = "{\"bankCode\":\"BANK001\",\"bankDescription\":\"Test Bank\",\"glPoid\":100,\"bankAccountNo\":\"123456789\",\"companyPoid\":1,\"bankPrefix\":\"TB\"}";
//
//        mockMvc.perform(put("/v1/bank-master/999")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "EDIT")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(validJson))
//                .andExpect(status().isNotFound());
//
//        verify(bankService).updateGlBank(eq(999L), any(GlBankDto.class));
//    }
//
//    @Test
//    void updateBankMaster_MissingRequestBody() throws Exception {
//        mockMvc.perform(put("/v1/bank-master/1")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "EDIT"))
//                .andExpect(status().isBadRequest());
//
//        verify(bankService, never()).updateGlBank(anyLong(), any(GlBankDto.class));
//    }
//
//    @Test
//    void updateBankMaster_MissingRequiredParams() throws Exception {
//        String validJson = "{\"bankCode\":\"BANK001\",\"bankDescription\":\"Test Bank\",\"glPoid\":100,\"bankAccountNo\":\"123456789\",\"companyPoid\":1,\"bankPrefix\":\"TB\"}";
//
//        mockMvc.perform(put("/v1/bank-master/1")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(validJson))
//                .andExpect(status().isInternalServerError());
//
//        verify(bankService, never()).updateGlBank(anyLong(), any(GlBankDto.class));
//    }
//
//    @Test
//    void updateBankMaster_ValidationError() throws Exception {
//        String invalidJson = "{\"bankCode\":\"\",\"bankDescription\":\"Test Bank\",\"glPoid\":100,\"bankAccountNo\":\"123456789\",\"companyPoid\":1,\"bankPrefix\":\"TB\"}";
//
//        mockMvc.perform(put("/v1/bank-master/1")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "EDIT")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(invalidJson))
//                .andExpect(status().isBadRequest());
//
//        verify(bankService, never()).updateGlBank(anyLong(), any(GlBankDto.class));
//    }
//
//    @Test
//    void updateBankMaster_ServiceException() throws Exception {
//        when(bankService.updateGlBank(eq(1L), any(GlBankDto.class)))
//                .thenThrow(new RuntimeException("Database connection failed"));
//
//        String validJson = "{\"bankCode\":\"BANK001\",\"bankDescription\":\"Test Bank\",\"glPoid\":100,\"bankAccountNo\":\"123456789\",\"companyPoid\":1,\"bankPrefix\":\"TB\"}";
//
//        mockMvc.perform(put("/v1/bank-master/1")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "EDIT")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(validJson))
//                .andExpect(status().isInternalServerError());
//
//        verify(bankService).updateGlBank(eq(1L), any(GlBankDto.class));
//    }
//
//    @Test
//    void updateBankMaster_MalformedJson() throws Exception {
//        String malformedJson = "{\"bankCode\":\"BANK001\",\"bankDescription\":\"Test Bank\",\"glPoid\":}";
//
//        mockMvc.perform(put("/v1/bank-master/1")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "EDIT")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(malformedJson))
//                .andExpect(status().isBadRequest());
//
//        verify(bankService, never()).updateGlBank(anyLong(), any(GlBankDto.class));
//    }
//
//    @Test
//    void createNewEntry_Success() throws Exception {
//        GlBankEntity responseEntity = new GlBankEntity();
//        responseEntity.setBankPoid(2L);
//
//        when(bankService.createEntry(any(GlBankDto.class))).thenReturn(responseEntity);
//
//        String validJson = "{\"bankCode\":\"BANK002\",\"bankDescription\":\"New Bank\",\"glPoid\":100,\"bankAccountNo\":\"987654321\",\"companyPoid\":2,\"bankPrefix\":\"NB\"}";
//
//        mockMvc.perform(post("/v1/bank-master/createEntry")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(validJson))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Bank Master Created successfully"));
//
//        verify(bankService).createEntry(any(GlBankDto.class));
//    }
//
//    @Test
//    void createNewEntry_ValidationError() throws Exception {
//        String invalidJson = "{\"bankCode\":\"BANK002\",\"bankDescription\":\"\",\"glPoid\":100,\"bankAccountNo\":\"987654321\",\"companyPoid\":2,\"bankPrefix\":\"NB\"}";
//
//        mockMvc.perform(post("/v1/bank-master/createEntry")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(invalidJson))
//                .andExpect(status().isBadRequest());
//
//        verify(bankService, never()).createEntry(any(GlBankDto.class));
//    }
//
//    @Test
//    void createNewEntry_ServiceException() throws Exception {
//        when(bankService.createEntry(any(GlBankDto.class)))
//                .thenThrow(new RuntimeException("Database error"));
//
//        String validJson = "{\"bankCode\":\"BANK002\",\"bankDescription\":\"New Bank\",\"glPoid\":100,\"bankAccountNo\":\"987654321\",\"companyPoid\":2,\"bankPrefix\":\"NB\"}";
//
//        mockMvc.perform(post("/v1/bank-master/createEntry")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(validJson))
//                .andExpect(status().isBadRequest());
//
//        verify(bankService).createEntry(any(GlBankDto.class));
//    }
//
//    @Test
//    void createNewEntry_MissingRequiredParams() throws Exception {
//        String validJson = "{\"bankCode\":\"BANK002\",\"bankDescription\":\"New Bank\",\"glPoid\":100,\"bankAccountNo\":\"987654321\",\"companyPoid\":2,\"bankPrefix\":\"NB\"}";
//
//        mockMvc.perform(post("/v1/bank-master/createEntry")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(validJson))
//                .andExpect(status().isInternalServerError());
//
//        verify(bankService, never()).createEntry(any(GlBankDto.class));
//    }
//
//    @Test
//    void getBankDetails_Success() throws Exception {
//        GlBankDto responseDto = new GlBankDto();
//        responseDto.setBankPoid(47L);
//
//        when(bankService.fetchGlBank(47L)).thenReturn(responseDto);
//
//        mockMvc.perform(get("/v1/bank-master/47")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Bank Master Fetched successfully"));
//
//        verify(bankService).fetchGlBank(47L);
//    }
//
//    @Test
//    void getBankDetails_NotFound() throws Exception {
//        when(bankService.fetchGlBank(999L))
//                .thenThrow(new ResourceNotFoundException("Bank", "bankPoid", 999L));
//
//        mockMvc.perform(get("/v1/bank-master/999")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isNotFound());
//
//        verify(bankService).fetchGlBank(999L);
//    }
//
//    @Test
//    void getBankDetails_MissingRequiredParams() throws Exception {
//        mockMvc.perform(get("/v1/bank-master/47"))
//                .andExpect(status().isInternalServerError());
//
//        verify(bankService, never()).fetchGlBank(anyLong());
//    }
//
//    @Test
//    void getBankDetails_ServiceException() throws Exception {
//        when(bankService.fetchGlBank(47L))
//                .thenThrow(new RuntimeException("Database connection failed"));
//
//        mockMvc.perform(get("/v1/bank-master/47")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isInternalServerError());
//
//        verify(bankService).fetchGlBank(47L);
//    }
//
//    @Test
//    void softDeleteBankMaster_Success() throws Exception {
//        doNothing().when(bankService).deleteBankMaster(181L);
//
//        mockMvc.perform(delete("/v1/bank-master/181")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "DELETE"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Bank Master soft deleted and related details removed"));
//
//        verify(bankService).deleteBankMaster(181L);
//    }
//
//    @Test
//    void softDeleteBankMaster_NotFound() throws Exception {
//        doThrow(new ResourceNotFoundException("Bank", "bankPoid", 999L))
//                .when(bankService).deleteBankMaster(999L);
//
//        mockMvc.perform(delete("/v1/bank-master/999")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "DELETE"))
//                .andExpect(status().isNotFound());
//
//        verify(bankService).deleteBankMaster(999L);
//    }
//
//    @Test
//    void softDeleteBankMaster_MissingRequiredParams() throws Exception {
//        mockMvc.perform(delete("/v1/bank-master/181"))
//                .andExpect(status().isInternalServerError());
//
//        verify(bankService, never()).deleteBankMaster(anyLong());
//    }
//
//    @Test
//    void softDeleteBankMaster_ServiceException() throws Exception {
//        doThrow(new RuntimeException("Database error"))
//                .when(bankService).deleteBankMaster(181L);
//
//        mockMvc.perform(delete("/v1/bank-master/181")
//                        .param("documentId", "400-006")
//                        .param("actionRequested", "DELETE"))
//                .andExpect(status().isInternalServerError());
//
//        verify(bankService).deleteBankMaster(181L);
//    }
//
//    @Test
//    @DisplayName("List bank records with filters - success")
//    void listOfRecordsWithGenericSearch_WithFilters_ShouldReturnRecords() throws Exception {
//        String documentId = "400-006";
//        String actionRequested = "VIEW";
//
//        String requestBody = """
//                {
//                    "operator": "AND",
//                    "filters": [
//                        {
//                            "searchField": "bankCode",
//                            "searchValue": "BANK001"
//                        }
//                    ]
//                }
//                """;
//
//        Map<String, Object> mockResponse = new HashMap<>();
//        mockResponse.put("content", List.of(
//                Map.of("bankPoid", 1, "bankCode", "BANK001", "bankDescription", "Test Bank")
//        ));
//        mockResponse.put("totalElements", 1);
//        mockResponse.put("totalPages", 1);
//        mockResponse.put("size", 10);
//        mockResponse.put("number", 0);
//
//        when(bankService.listOfRecordsAndGenericSearch(eq(documentId), any(FilterRequestDto.class), any(Pageable.class)))
//                .thenReturn(mockResponse);
//
//        mockMvc.perform(post("/v1/bank-master/list")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestBody)
//                        .param("documentId", documentId)
//                        .param("actionRequested", actionRequested)
//                        .param("page", "0")
//                        .param("size", "10")
//                        .param("sort", "bankCode,asc"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Bank records fetched successfully"))
//                .andExpect(jsonPath("$.result.data.content[0].bankCode").value("BANK001"));
//
//        verify(bankService, times(1))
//                .listOfRecordsAndGenericSearch(eq(documentId), any(FilterRequestDto.class), any(Pageable.class));
//    }
//
//    @Test
//    @DisplayName("List bank records - without filters")
//    void listOfRecordsWithGenericSearch_WithoutFilters_ShouldReturnAllRecords() throws Exception {
//        String documentId = "DOC123";
//        String actionRequested = "VIEW";
//
//        Map<String, Object> mockResponse = new HashMap<>();
//        mockResponse.put("content", List.of(
//                Map.of("BANK_CODE", "BANK1", "BANK_DESCRIPTION", "Bank One"),
//                Map.of("BANK_CODE", "BANK2", "BANK_DESCRIPTION", "Bank Two")
//        ));
//        mockResponse.put("totalElements", 2L);
//        mockResponse.put("totalPages", 1);
//        mockResponse.put("size", 10);
//        mockResponse.put("number", 0);
//        mockResponse.put("last", true);
//        mockResponse.put("first", true);
//        mockResponse.put("empty", false);
//        mockResponse.put("sort", Map.of("sorted", false, "unsorted", true, "empty", false));
//        mockResponse.put("pageable", Map.of("sort", Map.of("sorted", false, "unsorted", true, "empty", false), "pageNumber", 0, "pageSize", 10, "offset", 0, "paged", true, "unpaged", false));
//        mockResponse.put("displayFields", List.of("BANK_CODE", "BANK_DESCRIPTION"));
//
//        when(bankService.listOfRecordsAndGenericSearch(
//                eq(documentId),
//                any(FilterRequestDto.class),
//                any(Pageable.class)))
//                .thenReturn(mockResponse);
//
//        String requestBody = """
//                {
//                    "operator": "AND",
//                    "isDeleted": "N",
//                    "filters": []
//                }
//                """;
//
//        MvcResult result = mockMvc.perform(post("/v1/bank-master/list")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestBody)
//                        .param("documentId", documentId)
//                        .param("actionRequested", actionRequested)
//                        .param("page", "0")
//                        .param("size", "10")
//                        .param("sort", "bankCode,asc")
//                        .characterEncoding("utf-8"))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        verify(bankService, times(1)).listOfRecordsAndGenericSearch(
//                eq(documentId),
//                any(FilterRequestDto.class),
//                any(Pageable.class));
//    }
//
//    @Test
//    @DisplayName("List bank records - missing documentId")
//    void listOfRecordsWithGenericSearch_WhenDocumentIdMissing_ShouldReturnInternalServerError() throws Exception {
//        String actionRequested = "VIEW";
//
//        mockMvc.perform(post("/v1/bank-master/list")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{}")
//                        .param("actionRequested", actionRequested))
//                .andExpect(status().isInternalServerError());
//
//        verifyNoInteractions(bankService);
//    }
//
//    @Test
//    @DisplayName("List bank records - missing actionRequested")
//    void listOfRecordsWithGenericSearch_WhenActionRequestedMissing_ShouldReturnInternalServerError() throws Exception {
//        String documentId = "400-006";
//
//        mockMvc.perform(post("/v1/bank-master/list")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{}")
//                        .param("documentId", documentId))
//                .andExpect(status().isInternalServerError());
//    }
//}