package com.asg.finance.controller;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.CreditNoteGLDetailDto;
import com.asg.finance.dto.CreditNoteHeaderDto;
import com.asg.finance.dto.UniversalChargeDetailDto;
import com.asg.finance.exceptions.GlobalExceptionHandler;
import com.asg.finance.service.CreditNoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CreditNoteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreditNoteService creditNoteService;

    @InjectMocks
    private CreditNoteController creditNoteController;

    private ObjectMapper objectMapper;
    private CreditNoteHeaderDto creditNoteHeaderDto;
    private List<UniversalChargeDetailDto> chargeDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(creditNoteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        objectMapper = mapper;

        creditNoteHeaderDto = buildCreditNoteHeaderDto();
        chargeDetails = List.of(buildChargeDetailDto());
    }

    @Test
    void createCreditNote_ShouldReturnSuccessResponse() throws Exception {
        when(creditNoteService.createCreditNote(any(CreditNoteHeaderDto.class))).thenReturn(creditNoteHeaderDto);

        mockMvc.perform(post("/v1/credit-note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditNoteHeaderDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Credit note created successfully"))
                .andExpect(jsonPath("$.result.data.partyType").value("CUSTOMER"));

        verify(creditNoteService).createCreditNote(any(CreditNoteHeaderDto.class));
    }

    @Test
    void createCreditNote_WithInvalidPayload_ShouldReturnBadRequest() throws Exception {
        CreditNoteHeaderDto invalidDto = new CreditNoteHeaderDto();

        mockMvc.perform(post("/v1/credit-note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(creditNoteService, never()).createCreditNote(any(CreditNoteHeaderDto.class));
    }

    @Test
    void createCreditNote_WhenServiceThrows_ShouldReturnInternalServerError() throws Exception {
        when(creditNoteService.createCreditNote(any(CreditNoteHeaderDto.class)))
                .thenThrow(new RuntimeException("DB failure"));

        mockMvc.perform(post("/v1/credit-note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditNoteHeaderDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        verify(creditNoteService).createCreditNote(any(CreditNoteHeaderDto.class));
    }

    @Test
    void getCreditNoteById_ShouldReturnCreditNote() throws Exception {
        when(creditNoteService.getCreditNoteById(1L)).thenReturn(creditNoteHeaderDto);

        mockMvc.perform(get("/v1/credit-note/{transactionPoid}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Credit note fetched successfully"))
                .andExpect(jsonPath("$.result.data.partyType").value("CUSTOMER"));

        verify(creditNoteService).getCreditNoteById(1L);
    }

    @Test
    void getCreditNoteById_WhenServiceThrows_ShouldReturnInternalServerError() throws Exception {
        when(creditNoteService.getCreditNoteById(1L)).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(get("/v1/credit-note/{transactionPoid}", 1L))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        verify(creditNoteService).getCreditNoteById(1L);
    }

    @Test
    void getCreditNoteList_ShouldReturnPagedResponse() throws Exception {
        Map<String, Object> pagedResult = Map.of(
                "content", List.of(Map.of("transactionPoid", 1L)),
                "totalElements", 1
        );
        when(creditNoteService.listCreditNotes(anyString(), any(), isNull(), isNull(), any(Pageable.class))).thenReturn(pagedResult);

        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of(new FilterDto("PARTY_TYPE", "CUSTOMER")));

        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("300-111");

            mockMvc.perform(post("/v1/credit-note/list")
                            .param("page", "0")
                            .param("size", "20")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filters)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Credit note list fetched successfully"))
                    .andExpect(jsonPath("$.result.data.totalElements").value(1))
                    .andExpect(jsonPath("$.result.data.content[0].transactionPoid").value(1));
        }

        verify(creditNoteService).listCreditNotes(anyString(), any(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void updateCreditNote_ShouldReturnUpdatedEntity() throws Exception {
        when(creditNoteService.updateCreditNote(eq(1L), any(CreditNoteHeaderDto.class))).thenReturn(creditNoteHeaderDto);

        mockMvc.perform(put("/v1/credit-note/{transactionPoid}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditNoteHeaderDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Credit note updated successfully"))
                .andExpect(jsonPath("$.result.data.partyType").value("CUSTOMER"));

        verify(creditNoteService).updateCreditNote(eq(1L), any(CreditNoteHeaderDto.class));
    }

    @Test
    void deleteCreditNote_ShouldReturnSuccess() throws Exception {
        doNothing().when(creditNoteService).deleteCreditNote(1L);

        mockMvc.perform(delete("/v1/credit-note/{transactionPoid}", 1L)
                        .param("documentId", "300-111")
                        .param("actionRequested", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Credit note deleted successfully"));

        verify(creditNoteService).deleteCreditNote(1L);
    }

    @Test
    void getFFInvoiceCharges_ShouldReturnChargeDetails() throws Exception {
        when(creditNoteService.getFFInvoiceCharges(34L, 24L)).thenReturn(chargeDetails);

        mockMvc.perform(get("/v1/credit-note/ref/ff/{refNo}", 34L)
                        .param("partyPoid", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("FF Invoice charges fetched successfully"));

        verify(creditNoteService).getFFInvoiceCharges(34L, 24L);
    }

    @Test
    void getSHInvoiceCharges_ShouldReturnChargeDetails() throws Exception {
        when(creditNoteService.getSHInvoiceCharges(34L, 10L)).thenReturn(chargeDetails);

        mockMvc.perform(get("/v1/credit-note/ref/sh/{refNo}", 34L)
                        .param("partyPoid", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Shipping invoice charges fetched successfully"));

        verify(creditNoteService).getSHInvoiceCharges(34L, 10L);
    }

    @Test
    void getDNInvoiceCharges_ShouldReturnChargeDetails() throws Exception {
        when(creditNoteService.getDNInvoiceCharges(34L, 15L)).thenReturn(chargeDetails);

        mockMvc.perform(get("/v1/credit-note/ref/dn/{refNo}", 34L)
                        .param("partyPoid", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("DN invoice charges fetched successfully"));

        verify(creditNoteService).getDNInvoiceCharges(34L, 15L);
    }

    @Test
    void getFDADetails_ShouldReturnChargeDetails() throws Exception {
        when(creditNoteService.getFDADetails(34L, 20L)).thenReturn(chargeDetails);

        mockMvc.perform(get("/v1/credit-note/ref/fda/{fdaRef}", 34L)
                        .param("partyPoid", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("FDA details fetched successfully"));

        verify(creditNoteService).getFDADetails(34L, 20L);
    }

    private CreditNoteHeaderDto buildCreditNoteHeaderDto() {
        CreditNoteHeaderDto dto = new CreditNoteHeaderDto();
        dto.setPartyType("CUSTOMER");
        dto.setPartyPoid(34L);
        dto.setRefType("FF_INVOICE");
        dto.setFfInvoicePoid(34L);
        dto.setPostingNarration("Credit note test");
        dto.setTransactionDate(LocalDate.now());
        dto.setCurrencyCode("BHD");
        dto.setCurrencyRate(BigDecimal.ONE);
//        dto.setAmount(BigDecimal.valueOf(100));
        dto.setBhdAmount(BigDecimal.valueOf(100));
        dto.setCreditPeriod(30L);
        dto.setDueDate(LocalDate.now().plusDays(30));
        dto.setTinNumber("TIN123");
        dto.setBillRefType("AGAINST");
        dto.setPrintableRemarks(true);
        dto.setIssueType("Y");
        dto.setMultiCompany(false);
        dto.setRemarks("Test remarks");
        dto.setGrandTotal(BigDecimal.valueOf(100));
        dto.setBankPoid(1L);
        dto.setChargeDetails(List.of(buildChargeDetailDto()));
        dto.setGlDetails(List.of(buildGlDetailDto()));
        return dto;
    }

    private UniversalChargeDetailDto buildChargeDetailDto() {
        UniversalChargeDetailDto detailDto = new UniversalChargeDetailDto();
        detailDto.setChargePoid(34L);
        detailDto.setChargeAmount(BigDecimal.valueOf(100));
        detailDto.setChargeCostAmount(BigDecimal.valueOf(100));
        detailDto.setPdaAmount(BigDecimal.ZERO);
        detailDto.setTaxPoid(34L);
        detailDto.setTaxPercentage(BigDecimal.ZERO);
        detailDto.setTaxAmount(BigDecimal.ZERO);
        detailDto.setTotalAmount(BigDecimal.valueOf(100));
        detailDto.setRemarks("Test charge");
        detailDto.setIssueInvoice("Y");
        detailDto.setSelected(true);
        return detailDto;
    }

    private CreditNoteGLDetailDto buildGlDetailDto() {
        CreditNoteGLDetailDto glDetailDto = new CreditNoteGLDetailDto();
        glDetailDto.setCompanyPoid(1L);
        glDetailDto.setType("DR");
        glDetailDto.setGlPoid(4001L);
        glDetailDto.setDrAmt(BigDecimal.valueOf(100));
        glDetailDto.setCrAmt(BigDecimal.ZERO);
        glDetailDto.setTaxPoid(34L);
        glDetailDto.setTaxPercentage(BigDecimal.ZERO);
        glDetailDto.setTaxAmount(BigDecimal.ZERO);
        glDetailDto.setTotalAmount(BigDecimal.valueOf(100));
        glDetailDto.setRemarks("Test GL entry");
        return glDetailDto;
    }
}