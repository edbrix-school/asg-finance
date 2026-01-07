package com.asg.finance.controller;

import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.dto.FixedAssetCategoryRequestDto;
import com.asg.finance.dto.FixedAssetCategoryResponseDto;
import com.asg.finance.exceptions.GlobalExceptionHandler;
import com.asg.finance.service.FixedAssetCategoryService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FixedAssetCategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FixedAssetCategoryService fixedAssetCategoryService;

    @InjectMocks
    private FixedAssetCategoryController fixedAssetCategoryController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private FixedAssetCategoryRequestDto requestDto;
    private FixedAssetCategoryResponseDto responseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fixedAssetCategoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        requestDto = new FixedAssetCategoryRequestDto();
        requestDto.setFaCategoryDescription("Office Equipment");
        requestDto.setFaCategoryDescription2("Office Equip Desc");
        requestDto.setAssetType("PHYSICAL");
        requestDto.setFaGlAccount(1L);
        requestDto.setFaAccumulationAccount(1L);
        requestDto.setFaDepreciationAccount(1L);
        requestDto.setCostCenter(1L);
        requestDto.setUserRolePoid(List.of("1001;1002"));
        requestDto.setActive("Y");
        requestDto.setSeqNo(1);
        requestDto.setGroupPoid(100L);

        responseDto = new FixedAssetCategoryResponseDto();
        responseDto.setFaCategoryPoid(1L);
        responseDto.setFaCategoryCode("FAC001");
        responseDto.setFaCategoryDescription("Office Equipment");
        responseDto.setAssetType("PHYSICAL");
        responseDto.setActive("Y");
        responseDto.setUserRolePoid(List.of("1001;1002"));
        responseDto.setSeqNo(1);
    }

    @Test
    void createFixedAssetCategory_ShouldCreateSuccessfully() throws Exception {
        when(fixedAssetCategoryService.createFixedAssetCategory(any())).thenReturn(responseDto);

        mockMvc.perform(post("/v1/asset-category")
                        .param("documentId", "800-000")
                        .param("actionRequested", "create")
                        .param("userPoid", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Fixed Asset Category created successfully")))
                .andExpect(jsonPath("$.result.data.faCategoryCode", is("FAC001")));

        verify(fixedAssetCategoryService, times(1)).createFixedAssetCategory(any());
    }

    @Test
    void createFixedAssetCategory_ShouldReturnBadRequest_OnValidationException() throws Exception {
        when(fixedAssetCategoryService.createFixedAssetCategory(any()))
                .thenThrow(new ValidationException("Duplicate Code"));

        mockMvc.perform(post("/v1/asset-category")
                        .param("documentId", "800-000")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Duplicate Code")));

        verify(fixedAssetCategoryService, times(1)).createFixedAssetCategory(any());
    }

    @Test
    void createFixedAssetCategory_ShouldReturnInternalServerError_OnException() throws Exception {
        when(fixedAssetCategoryService.createFixedAssetCategory(any()))
                .thenThrow(new RuntimeException("Something went wrong"));

        mockMvc.perform(post("/v1/asset-category")
                        .param("documentId", "800-000")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Failed To Create Fixed Asset Category")));

        verify(fixedAssetCategoryService, times(1)).createFixedAssetCategory(any());
    }

    @Test
    void updateFixedAssetCategory_ShouldUpdateSuccessfully() throws Exception {
        when(fixedAssetCategoryService.updateFixedAssetCategory(eq(1L), any())).thenReturn(responseDto);

        mockMvc.perform(put("/v1/asset-category/1")
                        .param("documentId", "TAX-001")
                        .param("actionRequested", "update")
                        .param("userPoid", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Fixed Asset Category updated successfully")))
                .andExpect(jsonPath("$.result.data.faCategoryPoid", is(1)));

        verify(fixedAssetCategoryService, times(1)).updateFixedAssetCategory(eq(1L), any());
    }

    @Test
    void updateFixedAssetCategory_ShouldReturnInternalServerError_OnException() throws Exception {
        when(fixedAssetCategoryService.updateFixedAssetCategory(eq(1L), any()))
                .thenThrow(new RuntimeException("Update failed"));

        mockMvc.perform(put("/v1/asset-category/1")
                        .param("documentId", "TAX-001")
                        .param("actionRequested", "update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Failed to update Fixed Asset Category")));

        verify(fixedAssetCategoryService, times(1)).updateFixedAssetCategory(eq(1L), any());
    }

    @Test
    void updateFixedAssetCategory_ShouldReturnBadRequest_OnValidationException() throws Exception {
        when(fixedAssetCategoryService.updateFixedAssetCategory(eq(1L), any()))
                .thenThrow(new ValidationException("Code already exists"));

        mockMvc.perform(put("/v1/asset-category/1")
                        .param("documentId", "TAX-001")
                        .param("actionRequested", "update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Code already exists")));

        verify(fixedAssetCategoryService, times(1)).updateFixedAssetCategory(eq(1L), any());
    }

    @Test
    void getFixedAssetCategory_ShouldReturnCategory() throws Exception {
        when(fixedAssetCategoryService.getFixedAssetCategory(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/v1/asset-category/1")
                        .param("documentId", "800-320")
                        .param("actionRequested", "view"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Fixed Asset Category fetched successfully")))
                .andExpect(jsonPath("$.result.data.faCategoryPoid", is(1)));

        verify(fixedAssetCategoryService, times(1)).getFixedAssetCategory(1L);
    }

    @Test
    void softDeleteFixedAssetCategory_ShouldDeleteSuccessfully() throws Exception {
        doNothing().when(fixedAssetCategoryService).softDeleteFixedAssetCategory(1L);

        mockMvc.perform(delete("/v1/asset-category/1")
                        .param("documentId", "800-320")
                        .param("actionRequested", "delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Fixed Asset Category has been soft deleted successfully")));

        verify(fixedAssetCategoryService, times(1)).softDeleteFixedAssetCategory(1L);
    }



    // Edge Cases
    @Test
    void createFixedAssetCategory_WithSpecialCharacters_ShouldCreateSuccessfully() throws Exception {
        requestDto.setFaCategoryDescription("Special & Characters <>");
        responseDto.setFaCategoryCode("FAC@#$%");

        when(fixedAssetCategoryService.createFixedAssetCategory(any())).thenReturn(responseDto);

        mockMvc.perform(post("/v1/asset-category")
                        .param("documentId", "800-000")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(fixedAssetCategoryService, times(1)).createFixedAssetCategory(any());
    }

    @Test
    void createFixedAssetCategory_WithUnicodeCharacters_ShouldCreateSuccessfully() throws Exception {
        requestDto.setFaCategoryDescription("Unicode 测试 العربية");
        responseDto.setFaCategoryCode("FAC测试");

        when(fixedAssetCategoryService.createFixedAssetCategory(any())).thenReturn(responseDto);

        mockMvc.perform(post("/v1/asset-category")
                        .param("documentId", "800-000")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(fixedAssetCategoryService, times(1)).createFixedAssetCategory(any());
    }

    @Test
    void createFixedAssetCategory_WithMaxLengthValues_ShouldCreateSuccessfully() throws Exception {
        String maxLengthCode = "A".repeat(50);
        String maxLengthDesc = "B".repeat(255);

        requestDto.setFaCategoryDescription(maxLengthDesc);
        responseDto.setFaCategoryCode(maxLengthCode);

        when(fixedAssetCategoryService.createFixedAssetCategory(any())).thenReturn(responseDto);

        mockMvc.perform(post("/v1/asset-category")
                        .param("documentId", "800-000")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(fixedAssetCategoryService, times(1)).createFixedAssetCategory(any());
    }

    @Test
    void createFixedAssetCategory_WithNullOptionalFields_ShouldCreateSuccessfully() throws Exception {
        requestDto.setFaCategoryDescription2(null);
        requestDto.setUserRolePoid(null);
        requestDto.setSeqNo(null);

        when(fixedAssetCategoryService.createFixedAssetCategory(any())).thenReturn(responseDto);

        mockMvc.perform(post("/v1/asset-category")
                        .param("documentId", "800-000")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(fixedAssetCategoryService, times(1)).createFixedAssetCategory(any());
    }

    @Test
    void createFixedAssetCategory_WithInvalidJSON_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/asset-category")
                        .param("documentId", "800-000")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFixedAssetCategory_WithMissingDocumentId_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/v1/asset-category")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    void createFixedAssetCategory_WithMissingActionRequested_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/v1/asset-category")
                        .param("documentId", "800-000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    void updateFixedAssetCategory_WithNegativeId_ShouldCallService() throws Exception {
        when(fixedAssetCategoryService.updateFixedAssetCategory(eq(-1L), any())).thenReturn(responseDto);

        mockMvc.perform(put("/v1/asset-category/-1")
                        .param("documentId", "TAX-001")
                        .param("actionRequested", "update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        verify(fixedAssetCategoryService, times(1)).updateFixedAssetCategory(eq(-1L), any());
    }

    @Test
    void updateFixedAssetCategory_WithZeroId_ShouldCallService() throws Exception {
        when(fixedAssetCategoryService.updateFixedAssetCategory(eq(0L), any())).thenReturn(responseDto);

        mockMvc.perform(put("/v1/asset-category/0")
                        .param("documentId", "TAX-001")
                        .param("actionRequested", "update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        verify(fixedAssetCategoryService, times(1)).updateFixedAssetCategory(eq(0L), any());
    }

    @Test
    void getFixedAssetCategory_WithNegativeId_ShouldCallService() throws Exception {
        when(fixedAssetCategoryService.getFixedAssetCategory(-1L)).thenReturn(responseDto);

        mockMvc.perform(get("/v1/asset-category/-1")
                        .param("documentId", "800-320")
                        .param("actionRequested", "view"))
                .andExpect(status().isOk());

        verify(fixedAssetCategoryService, times(1)).getFixedAssetCategory(-1L);
    }

    @Test
    void softDeleteFixedAssetCategory_WithNegativeId_ShouldCallService() throws Exception {
        doNothing().when(fixedAssetCategoryService).softDeleteFixedAssetCategory(-1L);

        mockMvc.perform(delete("/v1/asset-category/-1")
                        .param("documentId", "800-320")
                        .param("actionRequested", "delete"))
                .andExpect(status().isOk());

        verify(fixedAssetCategoryService, times(1)).softDeleteFixedAssetCategory(-1L);
    }







    @Test
    void createFixedAssetCategory_WithInactiveStatus_ShouldCreateSuccessfully() throws Exception {
        requestDto.setActive("N");
        responseDto.setActive("N");

        when(fixedAssetCategoryService.createFixedAssetCategory(any())).thenReturn(responseDto);

        mockMvc.perform(post("/v1/asset-category")
                        .param("documentId", "800-000")
                        .param("actionRequested", "create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result.data.active", is("N")));

        verify(fixedAssetCategoryService, times(1)).createFixedAssetCategory(any());
    }
}