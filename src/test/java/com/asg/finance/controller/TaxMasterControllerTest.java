//package com.asg.finance.controller;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.exception.ValidationException;
//import com.asg.finance.dto.GlLedgerDTO;
//import com.asg.finance.dto.TaxMasterRequestDTO;
//import com.asg.finance.dto.TaxMasterResponseDTO;
//import com.asg.finance.exceptions.GlobalExceptionHandler;
//import com.asg.finance.service.TaxMasterServiceImpl;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(MockitoExtension.class)
//class TaxMasterControllerTest {
//
//    private MockMvc mockMvc;
//
//    @Mock
//    private TaxMasterServiceImpl taxMasterServiceImpl;
//
//    @InjectMocks
//    private TaxMasterController taxMasterController;
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    private TaxMasterRequestDTO requestDto;
//    private TaxMasterResponseDTO responseDto;
//
//    @BeforeEach
//    void setUp() {
//        mockMvc = MockMvcBuilders.standaloneSetup(taxMasterController)
//                .setControllerAdvice(new GlobalExceptionHandler())
//                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
//                .build();
//
//        requestDto = TaxMasterRequestDTO.builder()
//                .taxCode("TAX123")
//                .taxName("VAT")
//                .taxName2("Value Added Tax")
//                .percentage(18.0)
//                .taxType("INPUT_VAT")
//                .glType("DR")
//                .glLedgerPoid(100L)
//                .taxCategory("Standard")
//                .active("Y")
//                .seqNo(1)
//                .groupPoid(1L)
//                .build();
//
//        GlLedgerDTO glLedger = GlLedgerDTO.builder()
//                .poid(100L)
//                .code("GL100")
//                .description("Sales GL")
//                .build();
//
//        responseDto = TaxMasterResponseDTO.builder()
//                .taxPoid(2001L)
//                .taxCode("TAX123")
//                .taxName("VAT")
//                .taxName2("Value Added Tax")
//                .percentage(18.0)
//                .taxType("INPUT_VAT")
//                .glType("DR")
//                .glLedgerPoid(100L)
//                .glLedger(glLedger)
//                .taxCategory("Standard")
//                .active("Y")
//                .seqNo(1)
//                .groupPoid(1L)
//                .build();
//    }
//
//    @Test
//    void testCreateTaxMaster_Success() throws Exception {
//
//        when(taxMasterServiceImpl.createTaxMaster(any(TaxMasterRequestDTO.class)))
//                .thenReturn(responseDto);
//
//        mockMvc.perform(post("/v1/tax-master")
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "create")
//                        .param("userPoid", "101")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isOk())
//                .andExpect(content().string(
//                        org.hamcrest.Matchers.containsString("2001")
//                ))
//                .andExpect(content().string(
//                        org.hamcrest.Matchers.containsString("TAX123")
//                ))
//                .andExpect(content().string(
//                        org.hamcrest.Matchers.containsString("VAT")
//                ));
//
//        verify(taxMasterServiceImpl, times(1))
//                .createTaxMaster(any(TaxMasterRequestDTO.class));
//    }
//
//    @Test
//    void testCreateTaxMaster_ValidationException() throws Exception {
//
//        when(taxMasterServiceImpl.createTaxMaster(any(TaxMasterRequestDTO.class)))
//                .thenThrow(new ValidationException("Invalid Tax Type"));
//
//        mockMvc.perform(post("/v1/tax-master")
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "create")
//                        .param("userPoid", "101")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isInternalServerError())
//                .andExpect(content().string(
//                        org.hamcrest.Matchers.containsString("Invalid Tax Type")
//                ));
//
//        verify(taxMasterServiceImpl, times(1))
//                .createTaxMaster(any(TaxMasterRequestDTO.class));
//    }
//
//
//    @Test
//    void testUpdateTaxMaster_Success() throws Exception {
//
//        when(taxMasterServiceImpl.updateTaxMaster(eq(2001L), any(TaxMasterRequestDTO.class)))
//                .thenReturn(responseDto);
//
//        mockMvc.perform(put("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "update")
//                        .param("userPoid", "101")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isOk())
//                .andExpect(content().string(
//                        org.hamcrest.Matchers.containsString("2001")
//                ))
//                .andExpect(content().string(
//                        org.hamcrest.Matchers.containsString("TAX123")
//                ));
//
//        verify(taxMasterServiceImpl, times(1))
//                .updateTaxMaster(eq(2001L), any(TaxMasterRequestDTO.class));
//    }
//
//
//    @Test
//    void testGetTaxMaster_Success() throws Exception {
//
//        when(taxMasterServiceImpl.getTaxMasterById(2001L))
//                .thenReturn(responseDto);
//
//        mockMvc.perform(get("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "view"))
//                .andExpect(status().isOk())
//                .andExpect(content().string(
//                        org.hamcrest.Matchers.containsString("TAX123")
//                ));
//
//        verify(taxMasterServiceImpl, times(1))
//                .getTaxMasterById(2001L);
//    }
//
//
//    @Test
//    void testSoftDeleteTaxMaster_Success() throws Exception {
//
//        doNothing().when(taxMasterServiceImpl).softDeleteTaxMaster(2001L);
//
//        mockMvc.perform(delete("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "delete"))
//                .andExpect(status().isOk())
//                .andExpect(content().string(
//                        org.hamcrest.Matchers.containsString(
//                                "Tax Master deleted successfully"
//                        )
//                ));
//
//        verify(taxMasterServiceImpl, times(1))
//                .softDeleteTaxMaster(2001L);
//    }
//
//
//    @Test
//    void testListTaxMaster_Success() throws Exception {
//        Map<String, Object> data = new HashMap<>();
//        data.put("content", List.of(responseDto));
//        data.put("totalElements", 1);
//
//        when(taxMasterServiceImpl.listTaxMaster(anyString(), any(FilterRequestDto.class), any(Pageable.class)))
//                .thenReturn(data);
//
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());
//
//        mockMvc.perform(post("/v1/tax-master/list")
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "VIEW")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(filters)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.result.data.totalElements").value(1));
//
//        verify(taxMasterServiceImpl, times(1)).listTaxMaster(anyString(), any(FilterRequestDto.class), any(Pageable.class));
//    }
//
//    @Test
//    void testCreateTaxMaster_MissingDocumentId() throws Exception {
//        mockMvc.perform(post("/v1/tax-master")
//                        .param("actionRequested", "create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testUpdateTaxMaster_ServiceException() throws Exception {
//
//        when(taxMasterServiceImpl.updateTaxMaster(eq(2001L), any(TaxMasterRequestDTO.class)))
//                .thenThrow(new RuntimeException("Update failed"));
//
//        mockMvc.perform(put("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "update")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isInternalServerError())
//                .andExpect(content().string(
//                        org.hamcrest.Matchers.containsString("Update failed")
//                ));
//
//        verify(taxMasterServiceImpl, times(1))
//                .updateTaxMaster(eq(2001L), any(TaxMasterRequestDTO.class));
//    }
//
//
//    @Test
//    void testGetTaxMaster_ServiceException() throws Exception {
//
//        when(taxMasterServiceImpl.getTaxMasterById(2001L))
//                .thenThrow(new RuntimeException("Service error"));
//
//        mockMvc.perform(get("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "view"))
//                .andExpect(status().isInternalServerError())
//                .andExpect(content().string(
//                        org.hamcrest.Matchers.containsString("Service error")
//                ));
//
//        verify(taxMasterServiceImpl, times(1))
//                .getTaxMasterById(2001L);
//    }
//
//
//    @Test
//    void testListTaxMaster_ServiceException() throws Exception {
//
//        when(taxMasterServiceImpl.listTaxMaster(
//                anyString(), any(FilterRequestDto.class), any(Pageable.class)))
//                .thenThrow(new RuntimeException("List failed"));
//
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());
//
//        mockMvc.perform(post("/v1/tax-master/list")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(filters)))
//                .andExpect(status().isInternalServerError())
//                .andExpect(content().string(
//                        org.hamcrest.Matchers.containsString("List failed")
//                ));
//
//        verify(taxMasterServiceImpl, times(1))
//                .listTaxMaster(anyString(), any(FilterRequestDto.class), any(Pageable.class));
//    }
//
//
//    @Test
//    void testCreateTaxMaster_MissingActionRequested() throws Exception {
//        mockMvc.perform(post("/v1/tax-master")
//                        .param("documentId", "doc-123")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testCreateTaxMaster_InvalidJson() throws Exception {
//        mockMvc.perform(post("/v1/tax-master")
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{invalid json}"))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testCreateTaxMaster_EmptyRequestBody() throws Exception {
//        mockMvc.perform(post("/v1/tax-master")
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(""))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testCreateTaxMaster_NullRequestBody() throws Exception {
//        mockMvc.perform(post("/v1/tax-master")
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "create")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testCreateTaxMaster_UnsupportedMediaType() throws Exception {
//        mockMvc.perform(post("/v1/tax-master")
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "create")
//                        .contentType(MediaType.TEXT_PLAIN)
//                        .content("plain text"))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testUpdateTaxMaster_MissingDocumentId() throws Exception {
//        mockMvc.perform(put("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("actionRequested", "update")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testUpdateTaxMaster_MissingActionRequested() throws Exception {
//        mockMvc.perform(put("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("documentId", "doc-123")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testUpdateTaxMaster_InvalidId() throws Exception {
//        mockMvc.perform(put("/v1/tax-master/{taxPoid}", "invalid")
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "update")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testUpdateTaxMaster_NegativeId() throws Exception {
//        when(taxMasterServiceImpl.updateTaxMaster(eq(-1L), any(TaxMasterRequestDTO.class)))
//                .thenReturn(responseDto);
//
//        mockMvc.perform(put("/v1/tax-master/{taxPoid}", -1L)
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "update")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void testUpdateTaxMaster_ZeroId() throws Exception {
//        when(taxMasterServiceImpl.updateTaxMaster(eq(0L), any(TaxMasterRequestDTO.class)))
//                .thenReturn(responseDto);
//
//        mockMvc.perform(put("/v1/tax-master/{taxPoid}", 0L)
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "update")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void testUpdateTaxMaster_EmptyRequestBody() throws Exception {
//        mockMvc.perform(put("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "update")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(""))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testUpdateTaxMaster_ValidationException() throws Exception {
//        when(taxMasterServiceImpl.updateTaxMaster(eq(2001L), any(TaxMasterRequestDTO.class)))
//                .thenThrow(new ValidationException("Validation failed"));
//
//        mockMvc.perform(put("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "update")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testGetTaxMaster_MissingDocumentId() throws Exception {
//        mockMvc.perform(get("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("actionRequested", "view"))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testGetTaxMaster_MissingActionRequested() throws Exception {
//        mockMvc.perform(get("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("documentId", "doc-123"))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testGetTaxMaster_InvalidId() throws Exception {
//        mockMvc.perform(get("/v1/tax-master/{taxPoid}", "invalid")
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "view"))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testGetTaxMaster_NegativeId() throws Exception {
//        when(taxMasterServiceImpl.getTaxMasterById(-1L)).thenReturn(responseDto);
//
//        mockMvc.perform(get("/v1/tax-master/{taxPoid}", -1L)
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "view"))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void testGetTaxMaster_ZeroId() throws Exception {
//        when(taxMasterServiceImpl.getTaxMasterById(0L)).thenReturn(responseDto);
//
//        mockMvc.perform(get("/v1/tax-master/{taxPoid}", 0L)
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "view"))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void testGetTaxMaster_EmptyDocumentId() throws Exception {
//        when(taxMasterServiceImpl.getTaxMasterById(2001L)).thenReturn(responseDto);
//
//        mockMvc.perform(get("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("documentId", "")
//                        .param("actionRequested", "view"))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void testGetTaxMaster_EmptyActionRequested() throws Exception {
//        when(taxMasterServiceImpl.getTaxMasterById(2001L)).thenReturn(responseDto);
//
//        mockMvc.perform(get("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", ""))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void testSoftDeleteTaxMaster_MissingDocumentId() throws Exception {
//        mockMvc.perform(delete("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("actionRequested", "delete"))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testSoftDeleteTaxMaster_MissingActionRequested() throws Exception {
//        mockMvc.perform(delete("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("documentId", "doc-123"))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testSoftDeleteTaxMaster_InvalidId() throws Exception {
//        mockMvc.perform(delete("/v1/tax-master/{taxPoid}", "invalid")
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "delete"))
//                .andExpect(status().isBadRequest())
//                .andExpect(content().string(org.hamcrest.Matchers.containsString("invalid")));
//    }
//
//
//    @Test
//    void testSoftDeleteTaxMaster_ServiceException() throws Exception {
//        doThrow(new RuntimeException("Delete failed")).when(taxMasterServiceImpl).softDeleteTaxMaster(2001L);
//
//        mockMvc.perform(delete("/v1/tax-master/{taxPoid}", 2001L)
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "delete"))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success").value(false));
//    }
//
//    @Test
//    void testListTaxMaster_MissingDocumentId() throws Exception {
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());
//
//        mockMvc.perform(post("/v1/tax-master/list")
//                        .param("actionRequested", "VIEW")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(filters)))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testListTaxMaster_MissingActionRequested() throws Exception {
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());
//
//        mockMvc.perform(post("/v1/tax-master/list")
//                        .param("documentId", "doc-123")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(filters)))
//                .andExpect(status().isInternalServerError());
//    }
//
//    @Test
//    void testListTaxMaster_InvalidJson() throws Exception {
//        mockMvc.perform(post("/v1/tax-master/list")
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "VIEW")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{invalid json}"))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void testListTaxMaster_NullRequestBody() throws Exception {
//        Map<String, Object> data = new HashMap<>();
//        data.put("content", List.of());
//        data.put("totalElements", 0);
//
//        when(taxMasterServiceImpl.listTaxMaster(anyString(), isNull(), any(Pageable.class)))
//                .thenReturn(data);
//
//        mockMvc.perform(post("/v1/tax-master/list")
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "VIEW")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void testListTaxMaster_EmptyFilters() throws Exception {
//        Map<String, Object> data = new HashMap<>();
//        data.put("content", List.of());
//        data.put("totalElements", 0);
//
//        when(taxMasterServiceImpl.listTaxMaster(anyString(), any(FilterRequestDto.class), any(Pageable.class)))
//                .thenReturn(data);
//
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());
//
//        mockMvc.perform(post("/v1/tax-master/list")
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "VIEW")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(filters)))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void testCreateTaxMaster_EmptyDocumentId() throws Exception {
//        when(taxMasterServiceImpl.createTaxMaster(any(TaxMasterRequestDTO.class)))
//                .thenReturn(responseDto);
//
//        mockMvc.perform(post("/v1/tax-master")
//                        .param("documentId", "")
//                        .param("actionRequested", "create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void testCreateTaxMaster_EmptyActionRequested() throws Exception {
//        when(taxMasterServiceImpl.createTaxMaster(any(TaxMasterRequestDTO.class)))
//                .thenReturn(responseDto);
//
//        mockMvc.perform(post("/v1/tax-master")
//                        .param("documentId", "doc-123")
//                        .param("actionRequested", "")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isOk());
//    }
//}
