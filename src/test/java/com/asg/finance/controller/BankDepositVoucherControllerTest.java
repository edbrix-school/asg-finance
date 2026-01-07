package com.asg.finance.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.dto.BankDepositVoucherDtlDto;
import com.asg.finance.dto.BankDepositVoucherRequestDto;
import com.asg.finance.dto.BankDepositVoucherResponseDto;
import com.asg.finance.exceptions.GlobalExceptionHandler;
import com.asg.finance.service.BankDepositVoucherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
public class BankDepositVoucherControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BankDepositVoucherService service;

    @InjectMocks
    private BankDepositVoucherController controller;

    private ObjectMapper objectMapper;
    private BankDepositVoucherRequestDto requestDto;
    private BankDepositVoucherResponseDto responseDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        requestDto = BankDepositVoucherRequestDto.builder()
                .bankPoid(61L)
                .type("CHEQUE")
                .bankFilter("ALL")
                .groupPosting(false)
                .postingNarration("Bank deposit for cheques received")
                .companyPoid(1L)
                .groupPoid(1L)
                .remarks("Monthly cheque deposit")
                .details(List.of(
                        BankDepositVoucherDtlDto.builder()
                                .bankPoid(61L)
                                .pymtType("CHEQUE")
                                .refDocPoid(2085L)
                                .refDocRef("ASGDR858626")
                                .chqAcName("BABASONS")
                                .chqAcNo("2002623964080")
                                .chqCardNo("034588")
                                .chqDate(LocalDate.of(2016, 4, 7))
                                .amount(BigDecimal.valueOf(74.89))
                                .remarks("Cheque deposit")
                                .chqSeqNum(1)
                                .paymentMainPoid(768393L)
                                .build()
                ))
                .build();

        responseDto = BankDepositVoucherResponseDto.builder()
                .transactionPoid(69664L)
                .transactionDate(LocalDate.now())
                .docRef("ASG23132")
                .bankPoid(61L)
                .postingNarration("Bank deposit for cheques received")
                .remarks("Monthly cheque deposit")
                .grandTotal(BigDecimal.valueOf(74.89))
                .build();
    }

    @Test
    void createBankDepositVoucher_ShouldCreateSuccessfully() throws Exception {
        when(service.createBankDepositVoucher(any(BankDepositVoucherRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/v1/bank-deposit-voucher")
                        .param("documentId", "400-109")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Bank Deposit Voucher created successfully")))
                .andExpect(jsonPath("$.result.data.transactionPoid", is(69664)))
                .andExpect(jsonPath("$.result.data.docRef", is("ASG23132")));

        verify(service).createBankDepositVoucher(any(BankDepositVoucherRequestDto.class));
    }

    @Test
    void createBankDepositVoucher_ShouldReturnInternalServerError_WhenValidationFails() throws Exception {
        when(service.createBankDepositVoucher(any())).thenThrow(new ValidationException("Invalid bank deposit details"));

        mockMvc.perform(post("/v1/bank-deposit-voucher")
                        .param("documentId", "400-109")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid bank deposit details")));

        verify(service).createBankDepositVoucher(any());
    }

    @Test
    void getBankDepositVoucherById_ShouldReturnSuccessfully() throws Exception {
        when(service.getBankDepositVoucherById(69664L)).thenReturn(responseDto);

        mockMvc.perform(get("/v1/bank-deposit-voucher/{transactionPoid}", 69664L)
                        .param("documentId", "400-109")
                        .param("actionRequested", "view"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Bank Deposit Voucher fetched successfully")))
                .andExpect(jsonPath("$.result.data.docRef", is("ASG23132")));

        verify(service).getBankDepositVoucherById(69664L);
    }

    @Test
    void updateBankDepositVoucher_ShouldUpdateSuccessfully() throws Exception {
        when(service.updateBankDepositVoucher(eq(69664L), any(BankDepositVoucherRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/v1/bank-deposit-voucher/{transactionPoid}", 69664L)
                        .param("documentId", "400-109")
                        .param("actionRequested", "update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Bank Deposit Voucher updated successfully")));

        verify(service).updateBankDepositVoucher(eq(69664L), any(BankDepositVoucherRequestDto.class));
    }

    @Test
    void softDeleteBankDepositVoucher_ShouldDeleteSuccessfully() throws Exception {
        doNothing().when(service).softDeleteBankDepositVoucher(69664L);

        mockMvc.perform(delete("/v1/bank-deposit-voucher/{transactionPoid}", 69664L)
                        .param("documentId", "400-109")
                        .param("actionRequested", "delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Bank Deposit Voucher has been soft deleted successfully")));

        verify(service).softDeleteBankDepositVoucher(69664L);
    }

    @Test
    void listBankDepositVouchers_ShouldReturnFilteredListSuccessfully() throws Exception {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("content", List.of(
                Map.of("TRANSACTION_POID", 69664L, "DOC_REF", "ASG23132"),
                Map.of("TRANSACTION_POID", 69665L, "DOC_REF", "ASG23133")
        ));
        mockResponse.put("totalElements", 2);

        when(service.listBankDepositVouchers(isNull(), any(FilterRequestDto.class), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(mockResponse);

        String filterRequestJson = """
        {
          "operator": "AND",
          "isDeleted": "N",
          "filters": [
            { "searchField": "GLOBALSEARCH", "searchValue": "ASG" }
          ]
        }
        """;

        mockMvc.perform(post("/v1/bank-deposit-voucher/list")
                        .param("documentId", "400-109")
                        .param("actionRequested", "view")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(service).listBankDepositVouchers(isNull(), any(FilterRequestDto.class), isNull(), isNull(), any(Pageable.class));
    }

}

