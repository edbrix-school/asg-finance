package com.asg.finance.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.request.DocReleaseLockRequestDto;
import com.asg.finance.dto.GLMasterRequestDto;
import com.asg.finance.dto.GLMasterResponseDto;
import com.asg.finance.dto.GlMasterTreeNodeDto;
import com.asg.finance.service.GLMasterService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GLMasterControllerTest {

    @Mock
    private GLMasterService glMasterService;

    @InjectMocks
    private GLMasterController glMasterController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(glMasterController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createGLMaster_WithValidData_ReturnsSuccess() throws Exception {
        GLMasterRequestDto request = new GLMasterRequestDto();
        request.setGlCode("1001");
        request.setDescription("Test GL");

        GLMasterResponseDto response = new GLMasterResponseDto();
        response.setGlPoid(1L);
        response.setGlCode("1001");

        when(glMasterService.createGLMaster(any(GLMasterRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/v1/gl-master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.data.glPoid").value(1L))
                .andExpect(jsonPath("$.result.data.glCode").value("1001"));
    }

    @Test
    void getGLMaster_WithValidId_ReturnsGLMaster() throws Exception {
        GLMasterResponseDto response = new GLMasterResponseDto();
        response.setGlPoid(1L);
        response.setGlCode("1001");

        when(glMasterService.getGLMaster(1L)).thenReturn(response);

        mockMvc.perform(get("/v1/gl-master/1")
                       )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.data.glPoid").value(1L))
                .andExpect(jsonPath("$.result.data.glCode").value("1001"));
    }

    @Test
    void updateGLMaster_WithValidData_ReturnsUpdatedGLMaster() throws Exception {
        GLMasterRequestDto request = new GLMasterRequestDto();
        request.setGlCode("1001");
        request.setDescription("Updated GL");

        GLMasterResponseDto response = new GLMasterResponseDto();
        response.setGlPoid(1L);
        response.setGlCode("1001");

        when(glMasterService.updateGLMaster(eq(1L), any(GLMasterRequestDto.class))).thenReturn(response);

        mockMvc.perform(put("/v1/gl-master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.data.glPoid").value(1L));
    }

    @Test
    void deleteGLMaster_WithValidId_ReturnsSuccess() throws Exception {
        mockMvc.perform(delete("/v1/gl-master/1")
                      )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("GL Master has been soft deleted successfully"));
    }

    @Test
    void getGlMasterTree_WithValidParams_ReturnsTree() throws Exception {
        List<GlMasterTreeNodeDto> treeItems = Arrays.asList(
                new GlMasterTreeNodeDto()
        );

        when(glMasterService.getGlMasterTree(anyString(), anyString(), any())).thenReturn(treeItems);

        mockMvc.perform(get("/v1/gl-master/tree")
                        .param("filterValue", "1000"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getGlMasterTree_WithEmptyResult_ReturnsEmptyMessage() throws Exception {
        when(glMasterService.getGlMasterTree(anyString(), anyString(), any())).thenReturn(new ArrayList<GlMasterTreeNodeDto>());

        mockMvc.perform(get("/v1/gl-master/tree")
                        .param("filterValue", "nonexistent"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void releaseLock_WithValidRequest_ReturnsSuccess() throws Exception {
        DocReleaseLockRequestDto request = new DocReleaseLockRequestDto();
        request.setLoginGroupPoid(1L);
        request.setLoginCompanyPoid(1L);
        request.setLoginUserPoid("100");

        when(glMasterService.acquireLock(any(DocReleaseLockRequestDto.class))).thenReturn("SUCCESS");

        mockMvc.perform(post("/v1/gl-master/release-lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lock released successfully"));
    }

    @Test
    void acquireLock_WithValidRequest_ReturnsSuccess() throws Exception {
        DocReleaseLockRequestDto request = new DocReleaseLockRequestDto();
        request.setLoginGroupPoid(1L);
        request.setLoginCompanyPoid(1L);
        request.setLoginUserPoid("100");

        when(glMasterService.acquireLock(any(DocReleaseLockRequestDto.class))).thenReturn("LOCK_ACQUIRED");

        mockMvc.perform(post("/v1/gl-master/acquire-lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lock acquired successfully"));
    }

    @Test
    void listOfRecordsWithGenericSearch_WithValidRequest_ReturnsResults() throws Exception {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("totalCount", 2);
        mockResponse.put("records", Arrays.asList("Record1", "Record2"));

        FilterRequestDto filterDto = new FilterRequestDto("AND", "false", Collections.emptyList());

        when(glMasterService.listOfRecordsAndGenericSearch(isNull(), any(FilterRequestDto.class), any(Pageable.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/v1/gl-master/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("GL Master list fetched successfully"));
    }

    @Test
    void getGlMasterList_WithNullParent_ReturnsMainGroups() throws Exception {
        List<GLMasterResponseDto> listItems = Arrays.asList(
                new GLMasterResponseDto()
        );

        when(glMasterService.getGlMasterList(anyString(), anyString(), isNull())).thenReturn(listItems);

        mockMvc.perform(get("/v1/gl-master/list")
                  )
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getGlMasterList_WithParentPoid_ReturnsChildren() throws Exception {
        List<GLMasterResponseDto> listItems = Arrays.asList(
                new GLMasterResponseDto()
        );

        when(glMasterService.getGlMasterList(anyString(), anyString(), eq(1L))).thenReturn(listItems);

        mockMvc.perform(get("/v1/gl-master/list")
                        .param("parentPoid", "1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getGlMasterList_WithEmptyResult_ReturnsEmptyMessage() throws Exception {
        when(glMasterService.getGlMasterList(anyString(), anyString(), any())).thenReturn(new ArrayList<GLMasterResponseDto>());

        mockMvc.perform(get("/v1/gl-master/list")
                        .param("parentPoid", "999"))
                .andExpect(status().isInternalServerError());
    }
}