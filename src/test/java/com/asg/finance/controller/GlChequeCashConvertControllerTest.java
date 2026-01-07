//package com.asg.finance.controller;
//
//import com.asg.finance.dto.GlChequeCashConvertHdrDto;
//import com.asg.finance.dto.GlChequeConversionLoadResponseDto;
//import com.asg.finance.exceptions.GlobalExceptionHandler;
//import com.asg.finance.service.GlChequeCashConvertService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.util.HashMap;
//import java.util.Arrays;
//import java.util.Map;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//
//@ExtendWith(MockitoExtension.class)
//class GlChequeCashConvertControllerTest {
//
//    private MockMvc mockMvc;
//
//    @Mock
//    private GlChequeCashConvertService service;
//
//    @InjectMocks
//    private GlChequeCashConvertController glChequeCashConvertController;
//
//    private GlChequeCashConvertHdrDto mockDto;
//
//    @BeforeEach
//    void setUp() {
//        mockMvc = MockMvcBuilders.standaloneSetup(glChequeCashConvertController)
//                .setControllerAdvice(new GlobalExceptionHandler())
//                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
//                .build();
//        mockDto = new GlChequeCashConvertHdrDto();
//        mockDto.setDocRef("ASG78");
//        mockDto.setPostingNarration("Cheque conversion for cash");
//        mockDto.setCash(2000L);
//    }
//
//    @Test
//    void testLoadGlChequeConversionGet_Success() throws Exception {
//        GlChequeConversionLoadResponseDto row = new GlChequeConversionLoadResponseDto();
//        row.setPaymentMainPoid(7002001L);
//        row.setAmount(1000.0);
//        row.setVoucherType("CHQ");
//
//        Mockito.when(service.loadGlChequeConversion(eq("716380"), eq("0100000007343"), eq("CHEQUE_TO_CHEQUE")))
//                .thenReturn(Arrays.asList(row));
//
//        mockMvc.perform(get("/api/v1/gl-cheque-cash-conversion/load")
//                        .param("documentId", "400-110")
//                        .param("actionRequested", "VIEW")
//                        .param("chequeNumber", "716380")
//                        .param("chequeAccNumber", "0100000007343")
//                        .param("type", "CHEQUE_TO_CHEQUE"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message").value("GL Cheque Cash Conversion load fetched successfully"))
//                .andExpect(jsonPath("$.result.data").isArray())
//                .andExpect(jsonPath("$.result.data[0].paymentMainPoid").value(7002001))
//                .andExpect(jsonPath("$.result.data[0].amount").value(1000.0))
//                .andExpect(jsonPath("$.result.data[0].voucherType").value("CHQ"));
//    }
//
//    @Test
//    void testGetGlChequeCashConvert() throws Exception {
//        Mockito.when(service.getGlChequeCashConvert(201L)).thenReturn(mockDto);
//
//        mockMvc.perform(get("/api/v1/gl-cheque-cash-conversion/{transactionPoid}", 201)
//                        .param("documentId", "400-110")
//                        .param("actionRequested", "VIEW")
//                        .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message").value("GL Cheque Cash Convert Records fetched successfully"))
//                .andExpect(jsonPath("$.result.data.docRef").value("ASG78"))
//                .andExpect(jsonPath("$.result.data.postingNarration").value("Cheque conversion for cash"))
//                .andExpect(jsonPath("$.result.data.cash").value(2000));
//    }
//
//    @Test
//    void testDeleteGlChequeCashConvert() throws Exception {
//        Mockito.doNothing().when(service).softDeleteByTransactionPoid(301L);
//
//        mockMvc.perform(delete("/api/v1/gl-cheque-cash-conversion/{transactionPoid}", 301)
//                        .param("documentId", "400-110")
//                        .param("actionRequested", "DELETE"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message").value("GL Cheque Cash Convert Record deleted successfully"));
//    }
//
//    @Test
//    void testGetGlChequeCashConvert_MissingDocumentId_ReturnsBadRequest() throws Exception {
//        mockMvc.perform(get("/api/v1/gl-cheque-cash-conversion/{transactionPoid}", 201)
//
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testDeleteGlChequeCashConvert_MissingParams_ReturnsBadRequest() throws Exception {
//        mockMvc.perform(delete("/api/v1/gl-cheque-cash-conversion/{transactionPoid}", 301)
//
//                )
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testListOfRecordsWithGenericSearch() throws Exception {
//        Map<String, Object> mockResponse = new HashMap<>();
//        mockResponse.put("totalRecords", 1);
//        mockResponse.put("records", new Object[]{mockDto});
//
//        Mockito.doReturn(mockResponse)
//                .when(service)
//                .listOfRecordsAndGenericSearch(anyString(), any(), any());
//
//        String requestBody = """
//                {
//                  "operator": "AND",
//                  "isDeleted": "N",
//                  "filters": [
//                    {"searchField": "TRANSACTION_POID", "searchValue": "301"}
//                  ]
//                }
//                """;
//
//        mockMvc.perform(post("/api/v1/gl-cheque-cash-conversion/list")
//                        .param("documentId", "400-110")
//                        .param("actionRequested", "VIEW")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestBody))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message").value("GL Cheque Cash Convert list fetched successfully"))
//                .andExpect(jsonPath("$.result.data.totalRecords").value(1));
//    }
//
//    @Test
//    void testListOfRecordsWithGenericSearch_NullBodyAndPagination() throws Exception {
//        Map<String, Object> mockResponse = new HashMap<>();
//        mockResponse.put("totalRecords", 0);
//        mockResponse.put("records", new Object[]{});
//
//        Mockito.doReturn(mockResponse)
//                .when(service)
//                .listOfRecordsAndGenericSearch(anyString(), any(), any());
//
//        mockMvc.perform(post("/api/v1/gl-cheque-cash-conversion/list")
//                        .param("documentId", "400-110")
//                        .param("actionRequested", "VIEW")
//                        .param("page", "0")
//                        .param("size", "5")
//                        .param("sort", "transactionPoid,desc")
//                        .contentType(MediaType.APPLICATION_JSON)) // no body
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message").value("GL Cheque Cash Convert list fetched successfully"));
//    }
//
//    @Test
//    void testCreateGlChequeCashConvert() throws Exception {
//        Mockito.when(service.createGlChequeCashConvert(org.mockito.ArgumentMatchers.any(GlChequeCashConvertHdrDto.class)))
//                .thenReturn(mockDto);
//
//        String requestBody = """
//                {
//                  "groupPoid": 1,
//                  "companyPoid": 1,
//                  "docRef": "ASG78",
//                  "postingNarration": "Cheque conversion for cash",
//                  "cash": 2000,
//                  "createdBy": "MOHAMMED"
//                }
//                """;
//
//        mockMvc.perform(post("/api/v1/gl-cheque-cash-conversion")
//                        .param("documentId", "400-110")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestBody))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message").value("GL Cheque Cash Convert created successfully"))
//                .andExpect(jsonPath("$.result.data.docRef").value("ASG78"));
//    }
//
//    @Test
//    void testCreateGlChequeCashConvert_MissingParams_ReturnsBadRequest() throws Exception {
//        String requestBody = """
//                {
//                  "groupPoid": 1,
//                  "companyPoid": 1
//                }
//                """;
//
//        mockMvc.perform(post("/api/v1/gl-cheque-cash-conversion")
//
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestBody))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testCreateGlChequeCashConvert_EmptyBody_ReturnsBadRequest() throws Exception {
//        mockMvc.perform(post("/api/v1/gl-cheque-cash-conversion")
//                        .param("documentId", "400-110")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(""))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testUpdateGlChequeCashConvert() throws Exception {
//        Mockito.when(service.updateGlChequeCashConvert(eq(101L), org.mockito.ArgumentMatchers.any(GlChequeCashConvertHdrDto.class)))
//                .thenReturn(mockDto);
//
//        String requestBody = """
//                {
//                  "groupPoid": 1,
//                  "companyPoid": 1,
//                  "docRef": "ASG78",
//                  "postingNarration": "Cheque conversion for cash",
//                  "cash": 2000,
//                  "createdBy": "MOHAMMED"
//                }
//                """;
//
//        mockMvc.perform(put("/api/v1/gl-cheque-cash-conversion/{transactionPoid}", 101)
//                        .param("documentId", "400-110")
//                        .param("actionRequested", "UPDATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestBody))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message").value("GLCheque Cash Conversion updated successfully"))
//                .andExpect(jsonPath("$.result.data.docRef").value("ASG78"));
//    }
//
//
//}
//
