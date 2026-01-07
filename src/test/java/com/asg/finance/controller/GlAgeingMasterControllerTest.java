//package com.asg.finance.controller;
//
//import com.asg.finance.service.GlAgeingMasterService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.Collections;
//import java.util.Map;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(controllers = GlAgeingMasterController.class,
//        excludeAutoConfiguration = {
//                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
//                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
//        })
//@Slf4j
//class GlAgeingMasterControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private GlAgeingMasterService ageingMasterService;
//
//    @MockBean
//    private PermissionService permissionService;
//
//    @MockBean
//    private JwtUtil jwtUtil;
//
//    @MockBean
//    private DocIdApiMappingProperties docIdApiMappingProperties;
//
//    @MockBean
//    private RBACInterceptor rbacInterceptor;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @BeforeEach
//    void setUp() throws Exception {
//        when(docIdApiMappingProperties.getValidDocIdFormatRegexp()).thenReturn("\\d{3}-\\d{3}");
//        when(docIdApiMappingProperties.getMappings()).thenReturn(Map.of("/api/v1/ageing/**", "400-003"));
//        when(docIdApiMappingProperties.getSpecial()).thenReturn("/api/v1/global/searchable-fields/**");
//        when(rbacInterceptor.preHandle(any(), any(), any())).thenReturn(true);
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void createAgeingMaster_Success() throws Exception {
//        GlAgeingMasterDto testDto = createTestDto();
//        GlAgeingMasterResponseDto responseDto = new GlAgeingMasterResponseDto();
//        responseDto.setStatus("success");
//        responseDto.setAgeingPoid(1L);
//
//        when(ageingMasterService.createAgeingMaster(any(GlAgeingMasterDto.class)))
//                .thenReturn(responseDto);
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(post("/api/v1/ageing")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(testDto)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Ageing Master created successfully"))
//                .andExpect(jsonPath("$.result.data.status").value("success"))
//                .andExpect(jsonPath("$.result.data.ageingPoid").value(1));
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void createAgeingMaster_BadRequest() throws Exception {
//        GlAgeingMasterDto testDto = createTestDto();
//
//        when(ageingMasterService.createAgeingMaster(any(GlAgeingMasterDto.class)))
//                .thenThrow(new RuntimeException("Description already exists"));
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(post("/api/v1/ageing")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(testDto)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Description already exists"));
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void getAgeingMasterDetails_Success() throws Exception {
//        GlAgeingMasterDto testDto = createTestDto();
//
//        when(ageingMasterService.fetchAgeingMaster(anyLong()))
//                .thenReturn(testDto);
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(get("/api/v1/ageing/1")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Ageing Master Details fetched successfully"))
//                .andExpect(jsonPath("$.result.data.description").value("Test Ageing"))
//                .andExpect(jsonPath("$.result.data.groupPoid").value(1));
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void getAgeingMasterDetails_NotFound() throws Exception {
//        when(ageingMasterService.fetchAgeingMaster(anyLong()))
//                .thenThrow(new ResourceNotFoundException("AgeingMaster", "ageingPoid", 999L));
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(get("/api/v1/ageing/999")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("AgeingMaster not found with ageingPoid : '999'"));
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void updateAgeingMaster_Success() throws Exception {
//        GlAgeingMasterDto testDto = createTestDto();
//
//        when(ageingMasterService.updateAgeingMaster(anyLong(), any(GlAgeingMasterDto.class)))
//                .thenReturn(testDto);
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(put("/api/v1/ageing/1")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "EDIT")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(testDto)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Ageing Master updated successfully"));
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void updateAgeingMaster_BadRequest() throws Exception {
//        GlAgeingMasterDto testDto = createTestDto();
//
//        when(ageingMasterService.updateAgeingMaster(anyLong(), any(GlAgeingMasterDto.class)))
//                .thenThrow(new RuntimeException("Invalid data"));
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(put("/api/v1/ageing/1")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "EDIT")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(testDto)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Invalid data"));
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void softDeleteAgeingMaster_Success() throws Exception {
//        doNothing().when(ageingMasterService).softDeleteAgeingMaster(anyLong());
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(delete("/api/v1/ageing/1")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "DELETE"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Ageing Master has been soft deleted successfully"));
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void softDeleteAgeingMaster_NotFound() throws Exception {
//        doThrow(new ResourceNotFoundException("AgeingMaster", "ageingPoid", 999L))
//                .when(ageingMasterService).softDeleteAgeingMaster(anyLong());
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(delete("/api/v1/ageing/999")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "DELETE"))
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Ageing Master not found with ID: 999"));
//    }
//
//    // Edge Cases
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void createAgeingMaster_ServiceException() throws Exception {
//        when(ageingMasterService.createAgeingMaster(any(GlAgeingMasterDto.class)))
//                .thenThrow(new RuntimeException("Service unavailable"));
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(post("/api/v1/ageing")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(createTestDto())))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Service unavailable"));
//    }
//
//
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void createAgeingMaster_InvalidJson() throws Exception {
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(post("/api/v1/ageing")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("invalid json"))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void updateAgeingMaster_NotFound() throws Exception {
//        when(ageingMasterService.updateAgeingMaster(anyLong(), any(GlAgeingMasterDto.class)))
//                .thenThrow(new ResourceNotFoundException("AgeingMaster", "ageingPoid", 999L));
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(put("/api/v1/ageing/999")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "EDIT")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(createTestDto())))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false));
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void updateAgeingMaster_InternalServerError() throws Exception {
//        when(ageingMasterService.updateAgeingMaster(anyLong(), any(GlAgeingMasterDto.class)))
//                .thenThrow(new RuntimeException("Database error"));
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(put("/api/v1/ageing/1")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "EDIT")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(createTestDto())))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Database error"));
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void softDeleteAgeingMaster_InternalServerError() throws Exception {
//        doThrow(new RuntimeException("Database error"))
//                .when(ageingMasterService).softDeleteAgeingMaster(anyLong());
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(delete("/api/v1/ageing/1")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "DELETE"))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Failed to delete Ageing Master: Database error"));
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void createAgeingMaster_NullDescription() throws Exception {
//        GlAgeingMasterDto testDto = createTestDto();
//        testDto.setDescription(null);
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(post("/api/v1/ageing")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(testDto)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void createAgeingMaster_EmptyAgeingDetails() throws Exception {
//        GlAgeingMasterDto testDto = createTestDto();
//        testDto.setAgeingDetails(Collections.emptyList());
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(post("/api/v1/ageing")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "CREATE")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(testDto)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void getAgeingMasterDetails_ZeroId() throws Exception {
//        when(ageingMasterService.fetchAgeingMaster(0L))
//                .thenThrow(new ResourceNotFoundException("AgeingMaster", "ageingPoid", 0L));
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(get("/api/v1/ageing/0")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "VIEW"))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    @WithMockUser(username = "Admin", roles = {"ADMIN"})
//    void updateAgeingMaster_NegativeId() throws Exception {
//        when(ageingMasterService.updateAgeingMaster(eq(-1L), any(GlAgeingMasterDto.class)))
//                .thenThrow(new ResourceNotFoundException("AgeingMaster", "ageingPoid", -1L));
//        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
//
//        mockMvc.perform(put("/api/v1/ageing/-1")
//                        .param("documentId", "400-003")
//                        .param("actionRequested", "EDIT")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(createTestDto())))
//                .andExpect(status().isBadRequest());
//    }
//
//    private GlAgeingMasterDto createTestDto() {
//        GlAgeingMasterDto dto = new GlAgeingMasterDto();
//        dto.setDescription("Test Ageing");
//        dto.setGroupPoid(1L);
//        dto.setDescription2("Test Description");
//        dto.setAgeingBreakupType("MONTHLY");
//        dto.setSeqno(1);
//        dto.setActive(true);
//
//        GlAgeingMasterDtlDto detailDto = new GlAgeingMasterDtlDto();
//        detailDto.setBreakupTitle("0-30 Days");
//        detailDto.setBreakupFrom(0);
//        detailDto.setBreakupTo(30);
//        dto.setAgeingDetails(Collections.singletonList(detailDto));
//
//        return dto;
//    }
//}