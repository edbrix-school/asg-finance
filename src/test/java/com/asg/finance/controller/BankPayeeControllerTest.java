//package com.asg.finance.controller;
//
//import com.asg.finance.dto.BankPayeeRequest;
//import com.asg.finance.dto.BankPayeeResponse;
//import com.asg.finance.exceptions.GlobalExceptionHandler;
//import com.asg.finance.service.IBankPayeeService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.doThrow;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(MockitoExtension.class)
//class BankPayeeControllerTest {
//
//    @Mock
//    private IBankPayeeService bankPayeeService;
//
//    @InjectMocks
//    private BankPayeeController bankPayeeController;
//
//    private MockMvc mockMvc;
//    private ObjectMapper objectMapper;
//    private BankPayeeRequest request;
//    private BankPayeeResponse response;
//
//    @BeforeEach
//    void setUp() {
//        mockMvc = MockMvcBuilders.standaloneSetup(bankPayeeController)
//                .setControllerAdvice(new GlobalExceptionHandler())
//                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
//                .build();
//        objectMapper = new ObjectMapper();
//
//        request = new BankPayeeRequest();
//        request.setPayingName("Manoj");
//        request.setPayingName2("Manoj Kumar");
//        request.setRemarks("Test remark");
//        request.setActive("Y");
//
//        response = new BankPayeeResponse();
//        response.setPoid(1204L);
//        response.setPayingName("Manoj");
//        response.setPayingName2("Manoj Kumar");
//        response.setActive("Y");
//        response.setRemarks("Test remark");
//    }
//
//    @Test
//    void testCreatePayee_Success() throws Exception {
//        when(bankPayeeService.createPayee(any(BankPayeeRequest.class))).thenReturn(response);
//
//        mockMvc.perform(post("/v1/bank-payees")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.result.data.payingName").value("Manoj"))
//                .andExpect(jsonPath("$.result.data.active").value("Y"));
//    }
//
//    @Test
//    void testCreatePayee_ValidationError() throws Exception {
//        BankPayeeRequest invalidRequest = new BankPayeeRequest();
//
//        mockMvc.perform(post("/v1/bank-payees")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(invalidRequest)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false));
//    }
//
//    @Test
//    void testCreatePayee_ServiceException() throws Exception {
//        when(bankPayeeService.createPayee(any(BankPayeeRequest.class)))
//                .thenThrow(new RuntimeException("Service error"));
//
//        mockMvc.perform(post("/v1/bank-payees")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success").value(false));
//    }
//
//    @Test
//    void testGetPayeeById_Success() throws Exception {
//        when(bankPayeeService.getPayeeById(1204L)).thenReturn(response);
//
//        mockMvc.perform(get("/v1/bank-payees/1204")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.result.data.payingName").value("Manoj"))
//                .andExpect(jsonPath("$.result.data.poid").value(1204));
//    }
//
//    @Test
//    void testGetPayeeById_NotFound() throws Exception {
//        when(bankPayeeService.getPayeeById(9999L)).thenReturn(null);
//
//        mockMvc.perform(get("/v1/bank-payees/9999")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.result").value(""));
//    }
//
//    @Test
//    void testGetPayeeById_ServiceException() throws Exception {
//        when(bankPayeeService.getPayeeById(1204L)).thenThrow(new RuntimeException("Service error"));
//
//        mockMvc.perform(get("/v1/bank-payees/1204")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success").value(false));
//    }
//
//    @Test
//    void testUpdatePayee_Success() throws Exception {
//        BankPayeeResponse updatedResponse = new BankPayeeResponse();
//        updatedResponse.setPoid(1204L);
//        updatedResponse.setPayingName("Manoj Updated");
//        updatedResponse.setActive("Y");
//
//        when(bankPayeeService.updatePayee(eq(1204L), any(BankPayeeRequest.class)))
//                .thenReturn(updatedResponse);
//
//        mockMvc.perform(put("/v1/bank-payees/1204")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "EDIT")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.result.data.payingName").value("Manoj Updated"))
//                .andExpect(jsonPath("$.result.data.active").value("Y"));
//    }
//
//    @Test
//    void testUpdatePayee_ValidationError() throws Exception {
//        BankPayeeRequest invalidRequest = new BankPayeeRequest();
//
//        mockMvc.perform(put("/v1/bank-payees/1204")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "EDIT")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(invalidRequest)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false));
//    }
//
//    @Test
//    void testUpdatePayee_ServiceException() throws Exception {
//        when(bankPayeeService.updatePayee(eq(1204L), any(BankPayeeRequest.class)))
//                .thenThrow(new RuntimeException("Update failed"));
//
//        mockMvc.perform(put("/v1/bank-payees/1204")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "EDIT")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success").value(false));
//    }
//
//    @Test
//    void testDeletePayee_Success() throws Exception {
//        doNothing().when(bankPayeeService).softDeleteBypPayingPoid(1204L);
//
//        mockMvc.perform(delete("/v1/bank-payees/1204")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "DELETE"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message").value("Bank Payee has been soft deleted successfully"));
//    }
//
//    @Test
//    void testDeletePayee_ServiceException() throws Exception {
//        doThrow(new RuntimeException("Delete failed")).when(bankPayeeService).softDeleteBypPayingPoid(1204L);
//
//        mockMvc.perform(delete("/v1/bank-payees/1204")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "DELETE"))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success").value(false));
//    }
//
//    @Test
//    void testCreatePayee_MissingDocumentId() throws Exception {
//        mockMvc.perform(post("/v1/bank-payees")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void testCreatePayee_MissingActionRequested() throws Exception {
//        mockMvc.perform(post("/v1/bank-payees")
//                        .param("documentId", "800-320")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void testGetPayeeById_InvalidId() throws Exception {
//        mockMvc.perform(get("/v1/bank-payees/invalid")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testUpdatePayee_InvalidId() throws Exception {
//        mockMvc.perform(put("/v1/bank-payees/invalid")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "EDIT")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testDeletePayee_InvalidId() throws Exception {
//        mockMvc.perform(delete("/v1/bank-payees/invalid")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "DELETE"))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testCreatePayee_EmptyRequestBody() throws Exception {
//        mockMvc.perform(post("/v1/bank-payees")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(""))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testCreatePayee_InvalidJson() throws Exception {
//        mockMvc.perform(post("/v1/bank-payees")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{invalid json}"))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testGetPayeeById_ZeroId() throws Exception {
//        when(bankPayeeService.getPayeeById(0L)).thenReturn(null);
//
//        mockMvc.perform(get("/v1/bank-payees/0")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.result").value(""));
//    }
//
//    @Test
//    void testGetPayeeById_NegativeId() throws Exception {
//        when(bankPayeeService.getPayeeById(-1L)).thenReturn(null);
//
//        mockMvc.perform(get("/v1/bank-payees/-1")
//                        .param("documentId", "800-320")
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.result").value(""));
//    }
//}