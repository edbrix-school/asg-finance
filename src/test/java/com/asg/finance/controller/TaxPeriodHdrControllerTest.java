//package com.asg.finance.controller;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.exception.ValidationException;
//import com.asg.finance.dto.TaxPeriodChargeDtlResponseDto;
//import com.asg.finance.dto.TaxPeriodHdrRequestDto;
//import com.asg.finance.dto.TaxPeriodHdrResponseDto;
//import com.asg.finance.dto.TaxPeriodStockDtlResponseDto;
//import com.asg.finance.exceptions.GlobalExceptionHandler;
//import com.asg.finance.service.TaxPeriodHdrService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.time.LocalDate;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.hamcrest.Matchers.hasSize;
//import static org.hamcrest.Matchers.is;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//public class TaxPeriodHdrControllerTest {
//
//    private MockMvc mockMvc;
//
//    @Mock
//    private TaxPeriodHdrService taxPeriodHdrService;
//
//    @InjectMocks
//    private TaxPeriodHdrController taxPeriodHdrController;
//
//    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
//
//    private TaxPeriodHdrRequestDto requestDto;
//    private TaxPeriodHdrResponseDto responseDto;
//
//    @BeforeEach
//    void setUp() {
//        mockMvc = MockMvcBuilders.standaloneSetup(taxPeriodHdrController)
//                .setControllerAdvice(new GlobalExceptionHandler())
//                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
//                .build();
//
//        requestDto = TaxPeriodHdrRequestDto.builder()
//                .description("Test Tax Period")
//                .periodFrom(LocalDate.now())
//                .periodTo(LocalDate.now().plusDays(30))
//                .groupPoid(1L)
//                .companyPoid(1L)
//                .build();
//
//        responseDto = TaxPeriodHdrResponseDto.builder()
//                .transactionPoid(1L)
//                .description("Test Tax Period")
//                .periodFrom(LocalDate.now())
//                .periodTo(LocalDate.now().plusDays(30))
//                .groupPoid(1L)
//                .companyPoid(1L)
//                .deleted("N")
//                .build();
//    }
//
//    @Test
//    void createTaxPeriodHdr_ShouldCreateSuccessfully() throws Exception {
//        when(taxPeriodHdrService.createTaxPeriodHdr(any(TaxPeriodHdrRequestDto.class)))
//                .thenReturn(responseDto);
//
//        mockMvc.perform(post("/api/v1/tax-perioad")
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "create")
//                        .param("userPoid", "101")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Tax Period created successfully")))
//                .andExpect(jsonPath("$.result.data.transactionPoid", is(1)));
//
//        verify(taxPeriodHdrService).createTaxPeriodHdr(any(TaxPeriodHdrRequestDto.class));
//    }
//
//    @Test
//    void createTaxPeriodHdr_ShouldReturnInternalServerError_WhenValidationFails() throws Exception {
//        when(taxPeriodHdrService.createTaxPeriodHdr(any()))
//                .thenThrow(new ValidationException("Validation failed"));
//
//        mockMvc.perform(post("/api/v1/tax-perioad")
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "create")
//                        .param("userPoid", "101")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success", is(false)))
//                .andExpect(jsonPath("$.message", is("Validation failed")));
//
//        verify(taxPeriodHdrService).createTaxPeriodHdr(any());
//    }
//
//    @Test
//    void createTaxPeriodHdr_ShouldReturnInternalServerError_WhenExceptionOccurs() throws Exception {
//        when(taxPeriodHdrService.createTaxPeriodHdr(any()))
//                .thenThrow(new RuntimeException("Database connection failed"));
//
//        mockMvc.perform(post("/api/v1/tax-perioad")
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "create")
//                        .param("userPoid", "101")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success", is(false)));
//
//        verify(taxPeriodHdrService).createTaxPeriodHdr(any());
//    }
//
//    @Test
//    void updateTaxPeriodHdr_ShouldUpdateSuccessfully() throws Exception {
//        when(taxPeriodHdrService.updateTaxPeriodHdr(eq(1L), any(TaxPeriodHdrRequestDto.class)))
//                .thenReturn(responseDto);
//
//        mockMvc.perform(put("/api/v1/tax-perioad/{transactionPoid}", 1L)
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "update")
//                        .param("userPoid", "101")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message", is("Tax Period updated successfully")))
//                .andExpect(jsonPath("$.result.data.transactionPoid", is(1)));
//
//        verify(taxPeriodHdrService).updateTaxPeriodHdr(eq(1L), any(TaxPeriodHdrRequestDto.class));
//    }
//
//    @Test
//    void updateTaxPeriodHdr_ShouldReturnInternalServerError_WhenValidationFails() throws Exception {
//        when(taxPeriodHdrService.updateTaxPeriodHdr(eq(1L), any(TaxPeriodHdrRequestDto.class)))
//                .thenThrow(new ValidationException("Period dates are invalid"));
//
//        mockMvc.perform(put("/api/v1/tax-perioad/{transactionPoid}", 1L)
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "update")
//                        .param("userPoid", "101")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success", is(false)))
//                .andExpect(jsonPath("$.message", is("Period dates are invalid")));
//
//        verify(taxPeriodHdrService).updateTaxPeriodHdr(eq(1L), any(TaxPeriodHdrRequestDto.class));
//    }
//
//    @Test
//    void getTaxPeriodHdrById_ShouldReturnSuccessfully() throws Exception {
//        when(taxPeriodHdrService.getTaxPeriodHdrById(1L)).thenReturn(responseDto);
//
//        mockMvc.perform(get("/api/v1/tax-perioad/{transactionPoid}", 1L)
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "view"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message", is("Tax Period fetched successfully")))
//                .andExpect(jsonPath("$.result.data.description", is("Test Tax Period")));
//
//        verify(taxPeriodHdrService).getTaxPeriodHdrById(1L);
//    }
//
//    @Test
//    void softDeleteTaxPeriodHdr_ShouldDeleteSuccessfully() throws Exception {
//        doNothing().when(taxPeriodHdrService).softDeleteTaxPeriodHdr(1L);
//
//        mockMvc.perform(delete("/api/v1/tax-perioad/{transactionPoid}", 1L)
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "delete"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message", is("Tax Period has been soft deleted successfully")));
//
//        verify(taxPeriodHdrService).softDeleteTaxPeriodHdr(1L);
//    }
//
//
//    @Test
//    void copyTaxPeriod_ShouldCopySuccessfully() throws Exception {
//        when(taxPeriodHdrService.copyTaxPeriod(1L, 1L, 1L)).thenReturn("SUCCESS");
//
//        mockMvc.perform(post("/api/v1/tax-perioad/{transactionPoid}/copy", 1L)
//                        .param("companyPoid", "1")
//                        .param("userPoid", "1")
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "copy"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message", is("SUCCESS")));
//
//        verify(taxPeriodHdrService).copyTaxPeriod(1L, 1L, 1L);
//    }
//
//    @Test
//    void copyTaxPeriod_ShouldReturnInternalServerError_WhenExceptionOccurs() throws Exception {
//        when(taxPeriodHdrService.copyTaxPeriod(1L, 1L, 1L))
//                .thenThrow(new RuntimeException("Copy operation failed"));
//
//        mockMvc.perform(post("/api/v1/tax-perioad/{transactionPoid}/copy", 1L)
//                        .param("companyPoid", "1")
//                        .param("userPoid", "1")
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "copy"))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success", is(false)));
//
//        verify(taxPeriodHdrService).copyTaxPeriod(1L, 1L, 1L);
//    }
//
//    @Test
//    void getTaxPeriodCharges_ShouldReturnSuccessfully() throws Exception {
//
//        TaxPeriodChargeDtlResponseDto chargeDto = new TaxPeriodChargeDtlResponseDto();
//        chargeDto.setDetRowId(1L);
//        chargeDto.setRemarks("Test Charge");
//
//
//        Pageable pageable = PageRequest.of(0, 25);
//
//
//        Page<TaxPeriodChargeDtlResponseDto> pageResult = new PageImpl<>(List.of(chargeDto), pageable, 1);
//
//
//        when(taxPeriodHdrService.getTaxPeriodCharges(eq(1L), any(Pageable.class)))
//                .thenReturn(pageResult);
//
//
//        mockMvc.perform(get("/api/v1/tax-perioad/{transactionPoid}/charges", 1L)
//                        .param("page", "0")
//                        .param("size", "25")
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "view"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message", is("Transaction Poid charges fetched successfully")))
//                .andExpect(jsonPath("$.result.data.content[0].remarks", is("Test Charge")));
//
//
//        verify(taxPeriodHdrService).getTaxPeriodCharges(eq(1L), any(Pageable.class));
//    }
//
//
//    @Test
//    void getTaxPeriodStocks_ShouldReturnSuccessfully() throws Exception {
//
//        TaxPeriodStockDtlResponseDto stockDto = TaxPeriodStockDtlResponseDto.builder()
//                .detRowId(1L)
//                .remarks("Test Stock")
//                .build();
//
//
//        Pageable pageable = PageRequest.of(0, 25);
//
//
//        Page<TaxPeriodStockDtlResponseDto> pageResult = new PageImpl<>(List.of(stockDto), pageable, 1);
//
//
//        when(taxPeriodHdrService.getTaxPeriodStocks(eq(1L), any(Pageable.class)))
//                .thenReturn(pageResult);
//
//
//        mockMvc.perform(get("/api/v1/tax-perioad/{transactionPoid}/stocks", 1L)
//                        .param("page", "0")
//                        .param("size", "25")
//                        .param("documentId", "600-001")
//                        .param("actionRequested", "view"))
//                .andDo(print()) // optional: prints the response to console
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message", is("Transaction Poid stocks fetched successfully")))
//                .andExpect(jsonPath("$.result.data.content[0].remarks", is("Test Stock")));
//
//
//        verify(taxPeriodHdrService).getTaxPeriodStocks(eq(1L), any(Pageable.class));
//    }
//
//
//    @Test
//    void listTaxPeriod_ShouldReturnSuccessfully() throws Exception {
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Map<String, Object> mockResponse = new HashMap<>();
//        mockResponse.put("records", List.of(
//                Map.of("TRANSACTION_POID", 1L, "DOC_REF", "TP-001"),
//                Map.of("TRANSACTION_POID", 2L, "DOC_REF", "TP-002")
//        ));
//        mockResponse.put("totalCount", 2);
//
//        when(taxPeriodHdrService.listTaxPeriod(eq("400-011"), any(FilterRequestDto.class), any(Pageable.class), null, null))
//                .thenReturn(mockResponse);
//
//        FilterRequestDto filterRequest = new FilterRequestDto(
//                "AND",
//                "N",
//                List.of()
//        );
//
//        mockMvc.perform(post("/api/v1/tax-perioad/list")
//                        .param("documentId", "400-011")
//                        .param("actionRequested", "VIEW")
//                        .param("page", "0")
//                        .param("size", "10")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(filterRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.result.data.totalCount", is(2)))
//                .andExpect(jsonPath("$.result.data.records", hasSize(2)))
//                .andExpect(jsonPath("$.result.data.records[0].DOC_REF", is("TP-001")));
//
//        verify(taxPeriodHdrService).listTaxPeriod(eq("400-011"), any(FilterRequestDto.class), any(Pageable.class), null, null);
//    }
//
//
//
//
//}