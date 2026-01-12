package com.asg.finance.controller;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.CostCenterListResponseDto;
import com.asg.finance.dto.CostCenterRequestDTO;
import com.asg.finance.exceptions.GlobalExceptionHandler;
import com.asg.finance.service.CostCenterServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CostCenterControllerTest {

    @Mock
    private CostCenterServiceImpl costCenterServiceImpl;

    @InjectMocks
    private CostCenterController costCenterController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private CostCenterRequestDTO costCenterDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(costCenterController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        objectMapper = new ObjectMapper();

        costCenterDto = new CostCenterRequestDTO();
        costCenterDto.setCostCenterCode("CC1001");
        costCenterDto.setCostCenterDescription("CC10012");
        /*costCenterDto.setMisGroup("MG1");
        costCenterDto.setCostCenterGroupTypePoid(1L);
        costCenterDto.setCostGroupType("Test");*/
    }

    @Test
    void testCreateCostCenter_ValidationError() throws Exception {
        mockMvc.perform(post("/v1/cost-center")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(costCenterDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(costCenterServiceImpl, never()).createCostCenter(any(CostCenterRequestDTO.class));
    }

    @Test
    void testGetCostCenterById_Success() throws Exception {
        when(costCenterServiceImpl.getCostCenterById(12345L)).thenReturn(costCenterDto);

        mockMvc.perform(get("/v1/cost-center/12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.data.costCenterCode").value("CC1001"))
                .andExpect(jsonPath("$.result.data.costCenterDescription").value("CC10012"));

        verify(costCenterServiceImpl, times(1)).getCostCenterById(12345L);
    }

    @Test
    void testGetCostCenterById_ServiceException() throws Exception {
        when(costCenterServiceImpl.getCostCenterById(12345L))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/v1/cost-center/12345"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        verify(costCenterServiceImpl, times(1)).getCostCenterById(12345L);
    }

    @Test
    void testUpdateCostCenter_ValidationError() throws Exception {
        mockMvc.perform(put("/v1/cost-center/update/12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(costCenterDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(costCenterServiceImpl, never()).updateCostCenter(eq(12345L), any(CostCenterRequestDTO.class));
    }

    @Test
    void testSoftDeleteCostCenter_Success() throws Exception {
        doNothing().when(costCenterServiceImpl).softDeleteCountry(12345L);

        mockMvc.perform(delete("/v1/cost-center/12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cost Center has been soft deleted successfully"));

        verify(costCenterServiceImpl, times(1)).softDeleteCountry(12345L);
    }

    @Test
    void testSoftDeleteCostCenter_ServiceException() throws Exception {
        doThrow(new RuntimeException("Delete failed")).when(costCenterServiceImpl).softDeleteCountry(12345L);

        mockMvc.perform(delete("/v1/cost-center/12345"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        verify(costCenterServiceImpl, times(1)).softDeleteCountry(12345L);
    }

    @Test
    void testListCostCenter_Success() throws Exception {
        CostCenterListResponseDto testData = new CostCenterListResponseDto();
        testData.setCostCenterCode("CC001");
        testData.setCostCenterDescription("Test Cost Center");
        
        when(costCenterServiceImpl.getCostCenterList(eq("400-002"), eq("VIEW"), isNull()))
                .thenReturn(List.of(testData));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("400-002");
            mockedUserContext.when(UserContext::getActionRequested).thenReturn("VIEW");

            mockMvc.perform(get("/v1/cost-center/list"))
                    .andDo(result -> System.out.println("Response: " + result.getResponse().getContentAsString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        verify(costCenterServiceImpl, times(1)).getCostCenterList(eq("400-002"), eq("VIEW"), isNull());
    }

    @Test
    void testListCostCenter_ServiceException() throws Exception {
        when(costCenterServiceImpl.getCostCenterList(eq("400-002"), eq("VIEW"), isNull()))
                .thenThrow(new RuntimeException("Service error"));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("400-002");
            mockedUserContext.when(UserContext::getActionRequested).thenReturn("VIEW");

            mockMvc.perform(get("/v1/cost-center/list"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false));
        }

        verify(costCenterServiceImpl, times(1)).getCostCenterList(eq("400-002"), eq("VIEW"), isNull());
    }

    @Test
    void testCreateCostCenter_MissingDocumentId() throws Exception {
        mockMvc.perform(post("/v1/cost-center")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(costCenterDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testCreateCostCenter_InvalidJson() throws Exception {
        mockMvc.perform(post("/v1/cost-center")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetCostCenterById_InvalidId() throws Exception {
        mockMvc.perform(get("/v1/cost-center/invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateCostCenter_MissingActionRequested() throws Exception {
        mockMvc.perform(post("/v1/cost-center")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(costCenterDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateCostCenter_EmptyRequestBody() throws Exception {
        mockMvc.perform(post("/v1/cost-center")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateCostCenter_InvalidId() throws Exception {
        mockMvc.perform(put("/v1/cost-center/update/invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(costCenterDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSoftDeleteCostCenter_InvalidId() throws Exception {
        mockMvc.perform(delete("/v1/cost-center/invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testListCostCenter_EmptyFilters() throws Exception {
        when(costCenterServiceImpl.getCostCenterList(eq("400-002"), eq("VIEW"), isNull()))
                .thenReturn(List.of());

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("400-002");
            mockedUserContext.when(UserContext::getActionRequested).thenReturn("VIEW");

            mockMvc.perform(get("/v1/cost-center/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Test
    void testCreateCostCenter_UnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/v1/cost-center")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andExpect(status().isInternalServerError());
    }
}