package com.asg.finance.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.ImcoDepositRefundRequestDTO;
import com.asg.finance.dto.ImcoDepositRefundResponseDTO;
import com.asg.finance.exceptions.GlobalExceptionHandler;
import com.asg.finance.service.ImcoDepositRefundService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ImcoDepositRefundControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ImcoDepositRefundService service;

    @InjectMocks
    private ImcoDepositRefundController controller;

    private ObjectMapper objectMapper;
    private ImcoDepositRefundRequestDTO requestDto;
    private ImcoDepositRefundResponseDTO responseDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();


        requestDto = ImcoDepositRefundRequestDTO.builder()
                .transactionDate(LocalDate.now())
                .groupPoid(1L)
                .companyPoid(1L)
                .docRef("DOC-001")
                .remarks("Refund for cancelled shipment")
                .grandTotal(BigDecimal.valueOf(10000))
                .blNumber("BL-12345")
                .receiptNum("RCP-789")
                .payingTo("John Doe")
                .chequeRefundDetails(List.of(
                        ImcoDepositRefundRequestDTO.ChequeRefundDetailDTO.builder()
                                .detRowId(1L)
                                .choPoid(101L)
                                .choDate(LocalDate.now())
                                .refDocId("REF-123")
                                .refDocPoid(202L)
                                .pymtType("CHEQUE")
                                .chqCardno("CHQ12345")
                                .chqDate(LocalDate.now())
                                .bankPoid(303L)
                                .addressPoid(404L)
                                .chqAcName("John Doe")
                                .chqAcNo("AC123456789")
                                .amount(BigDecimal.valueOf(5000))
                                .status("ACTIVE")
                                .remarks("Partial refund")
                                .oldRcpvno("OLD-RCP-001")
                                .rcpDate(LocalDate.now())
                                .refDocRef("REF-DOC-123")
                                .choDocId("CHQ-REF")
                                .paymentMainPoid(505L)
                                .build()
                ))
                .chequeBillDetails(List.of(
                        ImcoDepositRefundRequestDTO.ChequeBillDetailDTO.builder()
                                .detRowId(1L)
                                .billRef("BILL-001")
                                .billAmount(BigDecimal.valueOf(5000))
                                .remarks("Bill adjustment")
                                .build()
                ))
                .build();

        responseDto = new ImcoDepositRefundResponseDTO();
        responseDto.setPoid(1L);
        responseDto.setDocRef("DOC-001");
        responseDto.setRemarks("Refund for cancelled shipment");
        responseDto.setGrandTotal(BigDecimal.valueOf(10000));
    }

    @Test
    void createImcoDepositRefund_ShouldCreateSuccessfully() throws Exception {
        when(service.createImcoDepositRefund(any(ImcoDepositRefundRequestDTO.class))).thenReturn(responseDto);

        mockMvc.perform(post("/v1/imco-deposit-refund")
                        .param("userPoid", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("IMCO Deposit Refund created successfully")))
                .andExpect(jsonPath("$.result.data.poid", is(1)));

        verify(service).createImcoDepositRefund(any(ImcoDepositRefundRequestDTO.class));
    }

    @Test
    void createImcoDepositRefund_ShouldReturnInternalServerError_WhenValidationFails() throws Exception {
        when(service.createImcoDepositRefund(any())).thenThrow(new ValidationException("Invalid refund details"));

        mockMvc.perform(post("/v1/imco-deposit-refund")
                        .param("userPoid", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid refund details")));

        verify(service).createImcoDepositRefund(any());
    }

    @Test
    void getImcoDepositRefundById_ShouldReturnSuccessfully() throws Exception {
        when(service.getImcoDepositRefundById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/v1/imco-deposit-refund/{transactionPoid}", 1L)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("IMCO Deposit Refund fetched successfully")))
                .andExpect(jsonPath("$.result.data.docRef", is("DOC-001")));

        verify(service).getImcoDepositRefundById(1L);
    }



    @Test
    void softDeleteImcoDepositRefund_ShouldDeleteSuccessfully() throws Exception {
        doNothing().when(service).softDeleteImcoDepositRefund(1L);

        mockMvc.perform(delete("/v1/imco-deposit-refund/{transactionPoid}", 1L)
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("IMCO Deposit Refund has been soft deleted successfully")));

        verify(service).softDeleteImcoDepositRefund(1L);
    }

    @Test
    void listImcoDepositRefund_ShouldReturnFilteredListSuccessfully() throws Exception {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("400-108");

            Pageable pageable = PageRequest.of(0, 10);
            Map<String, Object> mockResponse = new HashMap<>();
            mockResponse.put("records", List.of(
                    Map.of("TRANSACTION_POID", 1L, "DOC_REF", "DOC-001"),
                    Map.of("TRANSACTION_POID", 2L, "DOC_REF", "DOC-002")
            ));
            mockResponse.put("totalCount", 2);

            when(service.listImcoDepositRefund(eq("400-108"), any(FilterRequestDto.class), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(mockResponse);

            String filterRequestJson = """
            {
              "operator": "AND",
              "isDeleted": "N",
              "filters": [
                { "searchField": "GLOBALSEARCH", "searchValue": "Refund" }
              ]
            }
            """;

            mockMvc.perform(post("/v1/imco-deposit-refund/list")
                            .param("documentId", "400-108")
                            .param("actionRequested", "view")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(filterRequestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.result.data.totalCount", is(2)))
                    .andExpect(jsonPath("$.result.data.records", hasSize(2)))
                    .andExpect(jsonPath("$.result.data.records[0].DOC_REF", is("DOC-001")));

            verify(service).listImcoDepositRefund(eq("400-108"), any(FilterRequestDto.class), isNull(), isNull(), any(Pageable.class));
        }
    }

    @Test
    void getChequeDetails_ShouldReturnSuccessfully() throws Exception {
        com.asg.finance.dto.ImcoRefundLoadResponseDto mockResponse = new com.asg.finance.dto.ImcoRefundLoadResponseDto();
        mockResponse.setCheques(List.of());
        mockResponse.setBills(List.of());
        mockResponse.setPayingTo("John Doe");

        when(service.getChequeDetails(eq(1L), eq("RCP-789"))).thenReturn(mockResponse);

        mockMvc.perform(get("/v1/imco-deposit-refund/cheque-details")
                        .param("receiptPoid", "1")
                        .param("receiptNumber", "RCP-789")
                       )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("IMCO Cheque and Bill details fetched successfully")))
                .andExpect(jsonPath("$.result.data.payingTo", is("John Doe")));

        verify(service).getChequeDetails(1L, "RCP-789");
    }

    /*@Test
    void getGlPostingDetails_ShouldReturnSuccessfully() throws Exception {
        com.asg.dto.GlPostingViewResponseDto mockResponse = new com.asg.dto.GlPostingViewResponseDto();
        mockResponse.setLedgerEntries(List.of());
        mockResponse.setBillwiseBreakup(List.of());
        mockResponse.setCostBreakup(List.of());
        mockResponse.setVatBreakup(List.of());

        when(service.getGlPostingDetails(1L, 1L, "400-108", 1L)).thenReturn(mockResponse);

        mockMvc.perform(get("/v1/imco-deposit-refund/gl-posting")
                        .param("groupPoid", "1")
                        .param("companyPoid", "1")
                        .param("documentId", "400-108")
                        .param("transactionPoid", "1")
                        .param("actionRequested", "VIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("IMCO GL Posting details fetched successfully")));

        verify(service).getGlPostingDetails(1L, 1L, "400-108", 1L);
    }*/
}
