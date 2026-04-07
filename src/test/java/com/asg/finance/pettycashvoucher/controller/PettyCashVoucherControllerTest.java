package com.asg.finance.pettycashvoucher.controller;

import com.asg.common.lib.service.LoggingService;
import com.asg.finance.controller.PettyCashVoucherController;
import com.asg.finance.dto.PettyRefTypeResponse;
import com.asg.finance.dto.PettyCashFromGenrlPoDto;
import com.asg.finance.dto.PettyCashFromGrnDto;
import com.asg.finance.service.PettyCashVoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("PettyCashVoucherController Tests")
class PettyCashVoucherControllerTest {

    @Mock
    private PettyCashVoucherService pettyCashVoucherService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private PettyCashVoucherController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ===================================================================
    // GET /v1/petty-cash-voucher/load-from-grn
    // ===================================================================
    @Nested
    @DisplayName("load-from-grn endpoint")
    class LoadFromGrnEndpointTests {

        @Test
        @DisplayName("returns 200 with data for valid params")
        void validParams_returns200() throws Exception {
            List<PettyCashFromGrnDto> data = List.of(
                    PettyCashFromGrnDto.builder().transactionPoid(1L).grandTotal(new BigDecimal("300")).build()
            );
            when(pettyCashVoucherService.loadPettyCashFromGrn(
                    eq(1L), eq(2L), eq(3L), eq("2024-01-01"), eq("123")))
                    .thenReturn(PettyRefTypeResponse.<PettyCashFromGrnDto>builder()
                            .responseList(data).build());

            mockMvc.perform(get("/v1/petty-cash-voucher/load-from-grn")
                            .param("groupPoid", "1")
                            .param("companyPoid", "2")
                            .param("userPoid", "3")
                            .param("transactionDate", "2024-01-01")
                            .param("grnSupplierPoid", "123"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 200 with empty list when no GRN rows found")
        void noRows_returns200WithEmptyList() throws Exception {
            when(pettyCashVoucherService.loadPettyCashFromGrn(
                    any(), any(), any(), any(), any()))
                    .thenReturn(PettyRefTypeResponse.<PettyCashFromGrnDto>builder()
                            .responseList(Collections.emptyList()).build());

            mockMvc.perform(get("/v1/petty-cash-voucher/load-from-grn")
                            .param("groupPoid", "1")
                            .param("companyPoid", "2")
                            .param("userPoid", "3")
                            .param("transactionDate", "2024-01-01")
                            .param("grnSupplierPoid", "999"))
                    .andExpect(status().isOk());
        }
    }

    // ===================================================================
    // GET /v1/petty-cash-voucher/load-from-completed-po
    // ===================================================================
    @Nested
    @DisplayName("load-from-completed-po endpoint")
    class LoadFromCompletedPoEndpointTests {

        @Test
        @DisplayName("returns 200 with data for valid params")
        void validParams_returns200() throws Exception {
            List<PettyCashFromGenrlPoDto> data = List.of(
                    PettyCashFromGenrlPoDto.builder().stockPoid(10L).total(new BigDecimal("500")).build()
            );
            when(pettyCashVoucherService.loadPettyCashFromCompletedPo(
                    eq(1L), eq(2L), eq(3L), eq("PO-456")))
                    .thenReturn(PettyRefTypeResponse.<PettyCashFromGenrlPoDto>builder()
                            .responseList(data).build());

            mockMvc.perform(get("/v1/petty-cash-voucher/load-from-completed-po")
                            .param("groupPoid", "1")
                            .param("companyPoid", "2")
                            .param("userPoid", "3")
                            .param("poPoid", "PO-456"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 200 with empty list when no PO rows found")
        void noRows_returns200WithEmptyList() throws Exception {
            when(pettyCashVoucherService.loadPettyCashFromCompletedPo(
                    any(), any(), any(), any()))
                    .thenReturn(PettyRefTypeResponse.<PettyCashFromGenrlPoDto>builder()
                            .responseList(Collections.emptyList()).build());

            mockMvc.perform(get("/v1/petty-cash-voucher/load-from-completed-po")
                            .param("groupPoid", "1")
                            .param("companyPoid", "2")
                            .param("userPoid", "3")
                            .param("poPoid", "PO-000"))
                    .andExpect(status().isOk());
        }
    }

    // ===================================================================
    // GET /v1/petty-cash-voucher/allowed-ref-types
    // ===================================================================
    @Nested
    @DisplayName("allowed-ref-types endpoint")
    class AllowedRefTypesEndpointTests {

        @Test
        @DisplayName("returns 200 with ref type list")
        void returns200WithRefTypes() throws Exception {
            when(pettyCashVoucherService.getAllowedRefTypes(1L))
                    .thenReturn(List.of("GENERAL", "FF JOBS", "FDA JOBS"));

            mockMvc.perform(get("/v1/petty-cash-voucher/allowed-ref-types")
                            .param("userPoid", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 200 with empty list when user has no allowed ref types")
        void returns200WithEmptyList() throws Exception {
            when(pettyCashVoucherService.getAllowedRefTypes(99L))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/v1/petty-cash-voucher/allowed-ref-types")
                            .param("userPoid", "99"))
                    .andExpect(status().isOk());
        }
    }
}
