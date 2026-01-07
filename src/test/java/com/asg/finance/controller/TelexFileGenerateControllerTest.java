//package com.asg.finance.controller;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.finance.dto.TelexFileDtlDto;
//import com.asg.finance.dto.TelexFileGenerateRequestDto;
//import com.asg.finance.dto.TelexFileGenerateResponseDto;
//import com.asg.finance.exceptions.GlobalExceptionHandler;
//import com.asg.finance.service.TelexFileGenerateService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.hamcrest.Matchers.hasSize;
//import static org.hamcrest.Matchers.is;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//public class TelexFileGenerateControllerTest {
//
//    private MockMvc mockMvc;
//
//    @Mock
//    private TelexFileGenerateService service;
//
//    @InjectMocks
//    private TelexFileGenerateController controller;
//
//    private ObjectMapper objectMapper;
//    private TelexFileGenerateRequestDto requestDto;
//    private TelexFileGenerateResponseDto responseDto;
//
//    @BeforeEach
//    void setUp() {
//        objectMapper = new ObjectMapper()
//                .registerModule(new JavaTimeModule())
//                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//
//        mockMvc = MockMvcBuilders.standaloneSetup(controller)
//                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
//                .setControllerAdvice(new GlobalExceptionHandler())
//                .build();
//
//        requestDto = TelexFileGenerateRequestDto.builder()
//                .bankPoid(25L)
//                .bankList("Y")
//                .transactionDate(LocalDate.now())
//                .remarks("Test telex transfer")
//                .approvalOnly(false)
//                .suppressBalanceCheck(true)
//                .details(List.of(
//                        TelexFileDtlDto.builder()
//                                .debitTransactionPoid(1001L)
//                                .debitCompanyPoid(10L)
//                                .debitAmount(BigDecimal.valueOf(5000.00))
//                                .debitCurrencyCode("AED")
//                                .build()
//                ))
//                .build();
//
//        responseDto = TelexFileGenerateResponseDto.builder()
//                .transactionPoid(1L)
//                .groupPoid(5L)
//                .companyPoid(10L)
//                .bankPoid(25L)
//                .docRef("TLX-001")
//                .remarks("Test telex transfer")
//                .build();
//    }
//
//    @Test
//    void createTelexFile_ShouldCreateSuccessfully() throws Exception {
//        when(service.createTelexFile(any(TelexFileGenerateRequestDto.class))).thenReturn(responseDto);
//
//        mockMvc.perform(post("/api/v1/telex-file-generate")
//                        .param("documentId", "100-153")
//                        .param("actionRequested", "create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Telex File created successfully")))
//                .andExpect(jsonPath("$.result.data.transactionPoid", is(1)));
//
//        verify(service).createTelexFile(any(TelexFileGenerateRequestDto.class));
//    }
//
//    @Test
//    void updateTelexFile_ShouldUpdateSuccessfully() throws Exception {
//        when(service.updateTelexFile(eq(1L), any(TelexFileGenerateRequestDto.class))).thenReturn(responseDto);
//
//        mockMvc.perform(put("/api/v1/telex-file-generate/{transactionPoid}", 1L)
//                        .param("documentId", "100-153")
//                        .param("actionRequested", "update")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Telex File updated successfully")));
//
//        verify(service).updateTelexFile(eq(1L), any(TelexFileGenerateRequestDto.class));
//    }
//
//    @Test
//    void getTelexFileById_ShouldReturnSuccessfully() throws Exception {
//        when(service.getTelexFileById(1L)).thenReturn(responseDto);
//
//        mockMvc.perform(get("/api/v1/telex-file-generate/{transactionPoid}", 1L)
//                        .param("documentId", "100-153")
//                        .param("actionRequested", "view"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message", is("Telex File fetched successfully")))
//                .andExpect(jsonPath("$.result.data.docRef", is("TLX-001")));
//
//        verify(service).getTelexFileById(1L);
//    }
//
//    @Test
//    void softDeleteTelexFile_ShouldDeleteSuccessfully() throws Exception {
//        doNothing().when(service).softDeleteTelexFile(1L);
//
//        mockMvc.perform(delete("/api/v1/telex-file-generate/{transactionPoid}", 1L)
//                        .param("documentId", "100-153")
//                        .param("actionRequested", "delete"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message", is("Telex File has been soft deleted successfully")));
//
//        verify(service).softDeleteTelexFile(1L);
//    }
//
//    @Test
//    void listTelexFiles_ShouldReturnFilteredListSuccessfully() throws Exception {
//        Map<String, Object> mockResponse = new HashMap<>();
//        mockResponse.put("records", List.of(
//                Map.of("TRANSACTION_POID", 1L, "DOC_REF", "TLX-001"),
//                Map.of("TRANSACTION_POID", 2L, "DOC_REF", "TLX-002")
//        ));
//        mockResponse.put("totalCount", 2);
//
//        when(service.listTelexFiles(eq("100-153"), any(FilterRequestDto.class), any(), any(), any(Pageable.class)))
//                .thenReturn(mockResponse);
//
//        String filterRequestJson = """
//        {
//          "operator": "AND",
//          "isDeleted": "N",
//          "filters": [
//            { "searchField": "GLOBALSEARCH", "searchValue": "Bank" }
//          ]
//        }
//        """;
//
//        mockMvc.perform(post("/api/v1/telex-file-generate/list")
//                        .param("documentId", "100-153")
//                        .param("actionRequested", "view")
//                        .param("page", "0")
//                        .param("size", "10")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(filterRequestJson))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.result.data.totalCount", is(2)))
//                .andExpect(jsonPath("$.result.data.records", hasSize(2)));
//
//        verify(service).listTelexFiles(eq("100-153"), any(FilterRequestDto.class), any(), any(), any(Pageable.class));
//    }
//
//    @Test
//    void loadTelexData_ShouldReturnSuccessfully() throws Exception {
//        List<TelexFileDtlDto> mockData = List.of(
//                TelexFileDtlDto.builder()
//                        .debitTransactionPoid(1001L)
//                        .debitAmount(BigDecimal.valueOf(5000))
//                        .build()
//        );
//
//        when(service.loadTelexTransferData("Y")).thenReturn(mockData);
//
//        mockMvc.perform(get("/api/v1/telex-file-generate/load-telex-data")
//                        .param("bankPoid", "25")
//                        .param("bankList", "Y")
//                        .param("documentId", "100-153")
//                        .param("actionRequested", "view"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Telex transfer data loaded successfully")));
//
//        verify(service).loadTelexTransferData("Y");
//    }
//
//    @Test
//    void regenerateTelexFile_ShouldRegenerateSuccessfully() throws Exception {
//        when(service.regenerateTelexFile(1L)).thenReturn("SUCCESS");
//
//        mockMvc.perform(post("/api/v1/telex-file-generate/{debitVoucherPoid}/regenerate", 1L)
//                        .param("documentId", "100-153")
//                        .param("actionRequested", "regenerate"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Telex file regenerated successfully")));
//
//        verify(service).regenerateTelexFile(1L);
//    }
//}
//
