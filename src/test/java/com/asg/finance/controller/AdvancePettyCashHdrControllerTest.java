package com.asg.finance.controller;

import com.asg.finance.dto.AdvancePettyCashHdrRequestDTO;
import com.asg.finance.dto.AdvancePettyCashHdrResponseDTO;
import com.asg.finance.service.AdvancePettyCashHdrServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//@WebMvcTest(AdvancePettyCashHdrController.class)
@SpringBootTest
@AutoConfigureMockMvc
class AdvancePettyCashHdrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdvancePettyCashHdrServiceImpl service;

    @Autowired
    private ObjectMapper objectMapper;

    private AdvancePettyCashHdrRequestDTO requestDTO;
    private AdvancePettyCashHdrResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = AdvancePettyCashHdrRequestDTO.builder()
                .transactionDate(LocalDate.now())
                .pettyCashGlPoid(1001L)
                .payingTo("John Doe")
                .iouAmount(BigDecimal.valueOf(500))
                .narration("Test advance")
                .status("OPEN")
                .build();

        responseDTO = AdvancePettyCashHdrResponseDTO.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.now())
                .docRef("ASGIOU001")
                .pettyCashGlPoid(1001L)
                .payingTo("John Doe")
                .iouAmount(BigDecimal.valueOf(500))
                .status("OPEN")
                .build();
    }

    @Test
    void createAdvancePettyCash_Success() throws Exception {
        when(service.createAdvancePettyCash(any(AdvancePettyCashHdrRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/v1/advance-petty-cash")

                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Advance Petty Cash created successfully"))
                .andExpect(jsonPath("$.result.data.transactionPoid").value(1))
                .andExpect(jsonPath("$.result.data.payingTo").value("John Doe"));
    }

    @Test
    void createAdvancePettyCash_ValidationError() throws Exception {
        requestDTO.setPayingTo(""); // Invalid empty value

        mockMvc.perform(post("/v1/advance-petty-cash")

                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAdvancePettyCash_Success() throws Exception {
        when(service.updateAdvancePettyCash(eq(1L), any(AdvancePettyCashHdrRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/v1/advance-petty-cash/1")

                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Advance Petty Cash updated successfully"));
    }

    @Test
    void getAdvancePettyCashById_Success() throws Exception {
        when(service.getAdvancePettyCashById(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/v1/advance-petty-cash/1")
                     )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Advance Petty Cash fetched successfully"))
                .andExpect(jsonPath("$.result.data.transactionPoid").value(1));
    }

    @Test
    void softDeleteAdvancePettyCash_Success() throws Exception {
        mockMvc.perform(delete("/v1/advance-petty-cash/1")
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Advance Petty Cash has been soft deleted successfully"));
    }

    @Test
    void createAdvancePettyCash_MissingRequiredParams() throws Exception {
        mockMvc.perform(post("/v1/advance-petty-cash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());
    }
}
