package com.asg.finance.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.dto.PettyCashUserRoleRequestDto;
import com.asg.finance.dto.PettyCashUserroleResponseDto;
import com.asg.finance.exceptions.GlobalExceptionHandler;
import com.asg.finance.service.PettyCashUserRoleService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class PettyCashUserRoleControllerTest {
    private MockMvc mockMvc;

    @Mock
    private PettyCashUserRoleService pettyCashUserRoleService;

    @InjectMocks
    private PettyCashUserRoleController pettyCashUserRoleController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PettyCashUserRoleRequestDto requestDto;
    private PettyCashUserroleResponseDto responseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pettyCashUserRoleController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        requestDto = new PettyCashUserRoleRequestDto();
        requestDto.setRefTypePoid(1001L);
        requestDto.setRefType("PC-ROLE");
        requestDto.setDescription("Petty Cash Approver");
        requestDto.setUserRolePoid(List.of("101", "102"));
        requestDto.setActive("Y");
        requestDto.setSeqno(1);

        responseDto = new PettyCashUserroleResponseDto();
        responseDto.setRefTypePoid(1001L);
        responseDto.setRefType("PC-ROLE");
        responseDto.setDescription("Petty Cash Approver");
        responseDto.setUserRolePoid(List.of("101", "102"));
        responseDto.setActive("Y");
        responseDto.setSeqno(1);
    }

    @Test
    void createPettyCashUserRole_ShouldCreateSuccessfully() throws Exception {
        when(pettyCashUserRoleService.createPettyCashUserRole(any(PettyCashUserRoleRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/v1/petty-cash-user-role")
                        .param("documentId", "doc-001")
                        .param("actionRequested", "create")
                        .param("userPoid", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Petty Cash User Role created successfully")))
                .andExpect(jsonPath("$.result.data.refTypePoid", is(1001)));

        verify(pettyCashUserRoleService, times(1)).createPettyCashUserRole(any(PettyCashUserRoleRequestDto.class));
    }

    @Test
    void createPettyCashUserRole_ShouldReturnInternalServerError_WhenValidationFails() throws Exception {
        // Add glPoid so DTO matches current service expectations
        requestDto.setGlPoid(List.of("201", "202"));

        when(pettyCashUserRoleService.createPettyCashUserRole(any()))
                .thenThrow(new ValidationException("Invalid role type"));

        mockMvc.perform(post("/v1/petty-cash-user-role")
                        .param("documentId", "doc-001")
                        .param("actionRequested", "create")
                        .param("userPoid", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError()) // matches controller's 500
                .andExpect(jsonPath("$.message", is("Invalid role type"))); // matches your response JSON

        verify(pettyCashUserRoleService, times(1)).createPettyCashUserRole(any());
    }


    @Test
    void updatePettyCashUserRole_ShouldUpdateSuccessfully() throws Exception {
        when(pettyCashUserRoleService.updatePettyCashUserRole(eq(1001L), any(PettyCashUserRoleRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/v1/petty-cash-user-role/{refTypePoid}", 1001L)
                        .param("documentId", "doc-002")
                        .param("actionRequested", "update")
                        .param("userPoid", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Petty Cash User Role updated successfully")))
                .andExpect(jsonPath("$.result.data.refTypePoid", is(1001)));

        verify(pettyCashUserRoleService, times(1))
                .updatePettyCashUserRole(eq(1001L), any(PettyCashUserRoleRequestDto.class));
    }

    @Test
    void getPettyCashUserRole_ShouldReturnSuccessfully() throws Exception {
        when(pettyCashUserRoleService.getPettyCashUserRole(1001L)).thenReturn(responseDto);

        mockMvc.perform(get("/v1/petty-cash-user-role/{refTypePoid}", 1001L)
                        .param("documentId", "doc-003")
                        .param("actionRequested", "view"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Petty Cash User Role fetched successfully")))
                .andExpect(jsonPath("$.result.data.refType", is("PC-ROLE")));

        verify(pettyCashUserRoleService, times(1)).getPettyCashUserRole(1001L);
    }

    @Test
    void softDeletePettyCashUserRole_ShouldDeleteSuccessfully() throws Exception {
        doNothing().when(pettyCashUserRoleService).softDeletePettyCashUserRole(1001L);

        mockMvc.perform(delete("/v1/petty-cash-user-role/{refTypePoid}", 1001L)
                        .param("documentId", "doc-004")
                        .param("actionRequested", "delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Petty Cash User Role has been soft deleted successfully")));

        verify(pettyCashUserRoleService, times(1)).softDeletePettyCashUserRole(1001L);
    }

    @Test
    void listPettyCashUserRole_ShouldReturnFilteredListSuccessfully() throws Exception {
        // Mock response from service
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("records", List.of(
                Map.of("USER_ROLE", "ADMIN", "DESCRIPTION", "Admin Role"),
                Map.of("USER_ROLE", "USER", "DESCRIPTION", "Normal User")
        ));
        mockResponse.put("totalCount", 2);

        when(pettyCashUserRoleService.listPettyCashUserRole(
                isNull(), any(FilterRequestDto.class), any(Pageable.class)))
                .thenReturn(mockResponse);

        // JSON filter request
        String filterRequestJson = """
        {
          "operator": "AND",
          "isDeleted": "N",
          "filters": [
             { "searchField": "GLOBALSEARCH", "searchValue": "User" }
          ]
        }
        """;

        // Perform MockMvc POST request with Pageable parameters
        mockMvc.perform(post("/v1/petty-cash-user-role/list")
                        .param("documentId", "DOC123")
                        .param("actionRequested", "VIEW")
                        .param("page", "0")        // Pageable page
                        .param("size", "10")       // Pageable size
                        .param("sort", "USER_ROLE,asc") // Optional sort
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestJson))
                .andDo(result -> System.out.println("Response: " + result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result.data.totalCount", is(2)))
                .andExpect(jsonPath("$.result.data.records", hasSize(2)))
                .andExpect(jsonPath("$.result.data.records[0].USER_ROLE", is("ADMIN")))
                .andExpect(jsonPath("$.result.data.records[1].DESCRIPTION", is("Normal User")));

        // Verify service was called once
        verify(pettyCashUserRoleService, times(1))
                .listPettyCashUserRole(isNull(), any(FilterRequestDto.class), any(Pageable.class));
    }



}
