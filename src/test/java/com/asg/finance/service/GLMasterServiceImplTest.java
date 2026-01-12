package com.asg.finance.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.DocReleaseLockRequestDto;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.finance.dto.GLMasterResponseDto;
import com.asg.finance.dto.GlMasterTreeRequest;
import com.asg.finance.entity.GLMasterEntity;
import com.asg.finance.repository.GLMasterTreeViewRepository;
import com.asg.finance.repository.GLMastersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.sql.SQLException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GLMasterServiceImplTest {

    @Mock
    private GLMastersRepository glMastersRepository;

    @Mock
    private GLMasterTreeViewRepository treeViewRepository;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private GLMasterCustomService glMasterCustomService;

    @InjectMocks
    private GLMasterServiceImpl service;

    private DocReleaseLockRequestDto request;


    private GLMasterEntity mainGroup;
    private GLMasterEntity childLedger;

    @BeforeEach
    void setUp() {
        request = new DocReleaseLockRequestDto();
        request.setLoginGroupPoid(1L);
        request.setLoginCompanyPoid(1L);
        request.setLoginUserPoid("100");
        request.setDocId("400-001");
        request.setDocPoidValue(123L);
        request.setUserId("testUser");
        mainGroup = new GLMasterEntity();
        mainGroup.setGlPoid(1000L);
        mainGroup.setGlCode("1000");
        mainGroup.setDescription("ASSETS");
        mainGroup.setType("MAIN_GROUP");
        mainGroup.setAccountType("Asset");
        mainGroup.setActiveFlag("Y");
        mainGroup.setDeletedFlag("N");

        childLedger = new GLMasterEntity();
        childLedger.setGlPoid(1010L);
        childLedger.setGlCode("1010");
        childLedger.setDescription("CASH");
        childLedger.setType("LEDGER");
        childLedger.setAccountType("Asset");
        childLedger.setGroupGlPoid(1000L);
        childLedger.setActiveFlag("Y");
        childLedger.setDeletedFlag("N");
    }

    @Test
    void getGlMasterList_returnsMainGroups_whenParentIsNull() {
        when(glMastersRepository.findMainGroups(eq(false), isNull())).thenReturn(List.of(mainGroup));

        List<GLMasterResponseDto> result = service.getGlMasterList("400-001", "VIEW", null);

        assertThat(result).hasSize(1);
        GLMasterResponseDto dto = result.get(0);
        assertThat(dto.getGlPoid()).isEqualTo(1000L);
        assertThat(dto.getGlCode()).isEqualTo("1000");
        assertThat(dto.getDescription()).isEqualTo("ASSETS");
        assertThat(dto.getType()).isEqualTo("MAIN_GROUP");
        assertThat(dto.getParentPoid()).isNull();
        assertThat(dto.getLevel()).isEqualTo(0);
        assertThat(dto.getDeleted()).isFalse();
        assertThat(dto.getActive()).isTrue();
    }

    @Test
    void getGlMasterList_returnsChildren_whenParentProvided() {
        when(glMastersRepository.findDirectChildren(eq(1000L), eq(false), isNull()))
                .thenReturn(List.of(childLedger));

        List<GLMasterResponseDto> result = service.getGlMasterList("400-001", "VIEW", 1000L);

        assertThat(result).hasSize(1);
        GLMasterResponseDto dto = result.get(0);
        assertThat(dto.getGlPoid()).isEqualTo(1010L);
        assertThat(dto.getParentPoid()).isEqualTo(1000L);
        assertThat(dto.getLevel()).isEqualTo(1);
        assertThat(dto.getType()).isEqualTo("LEDGER");
    }

    @Test
    void getGlMasterTree_buildsHierarchy_fromProcedureData() {
        GlMasterTreeRequest req = GlMasterTreeRequest.builder()
                .includeDeleted(false)
                .build();

        // Mock procedure records: one root (MAIN_GROUP) and one child (SUB_GROUP)
        Map<String, Object> root = new HashMap<>();
        root.put("POID", 2000L);
        root.put("PARENT_POID", null);
        root.put("DESCRIPTION", "REVENUE ACCOUNTS (4000)");
        root.put("GL_TYPE", "MAIN_GROUP");
        root.put("ITEM_TYPE", "ITEM");
        root.put("DELETED", "N");
        root.put("ACTIVE", "Y");
        root.put("LVL", 1);

        Map<String, Object> child = new HashMap<>();
        child.put("POID", 2100L);
        child.put("PARENT_POID", 2000L);
        child.put("DESCRIPTION", "TURNOVER (4010)");
        child.put("GL_TYPE", "SUB_GROUP");
        child.put("ITEM_TYPE", "ITEM");
        child.put("DELETED", "N");
        child.put("ACTIVE", "Y");
        child.put("LVL", 2);

        try {
            when(treeViewRepository.callGlMasterTreeViewProcedure(eq("400-001"), eq("VIEW"), any()))
                    .thenReturn(List.of(root, child));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        var tree = service.getGlMasterTree("400-001", "VIEW", req);

        assertThat(tree).hasSize(1);
        var rootNode = tree.get(0);
        assertThat(rootNode.getGlPoid()).isEqualTo(2000L);
        assertThat(rootNode.getGlCode()).isEqualTo("4000");
        assertThat(rootNode.getType()).isEqualTo("MAIN_GROUP");
        assertThat(rootNode.getLevel()).isEqualTo(0);
        assertThat(rootNode.getChildren()).hasSize(1);

        var childNode = rootNode.getChildren().get(0);
        assertThat(childNode.getGlPoid()).isEqualTo(2100L);
        assertThat(childNode.getParentPoid()).isEqualTo(2000L);
        assertThat(childNode.getGlCode()).isEqualTo("4010");
        assertThat(childNode.getType()).isEqualTo("SUB_GROUP");
        assertThat(childNode.getLevel()).isEqualTo(1);
    }

    @Test
    void acquireLock_ShouldReturnSuccess_WhenRequestIsValid() {
        // Arrange
        String expectedStatus = "LOCK_ACQUIRED";
        when(glMasterCustomService.acquireLock(any(DocReleaseLockRequestDto.class))).thenReturn(expectedStatus);

        // Act
        String result = service.acquireLock(request);

        // Assert
        assertEquals(expectedStatus, result);
        verify(glMasterCustomService, times(1)).acquireLock(any(DocReleaseLockRequestDto.class));
    }

    @Test
    void acquireLock_ShouldReturnError_WhenRepositoryThrowsException() {
        // Arrange
        when(glMasterCustomService.acquireLock(any(DocReleaseLockRequestDto.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> service.acquireLock(request));
        verify(glMasterCustomService, times(1)).acquireLock(any(DocReleaseLockRequestDto.class));
    }

    @Test
    void releaseLock_ShouldReturnSuccess_WhenRequestIsValid() {
        // Arrange
        String expectedStatus = "LOCK_RELEASED";
        when(glMasterCustomService.releaseLock(any(DocReleaseLockRequestDto.class))).thenReturn(expectedStatus);

        // Act
        String result = service.releaseLock(request);

        // Assert
        assertEquals(expectedStatus, result);
        verify(glMasterCustomService, times(1)).releaseLock(any(DocReleaseLockRequestDto.class));
    }

    @Test
    void releaseLock_ShouldReturnError_WhenRepositoryThrowsException() {
        // Arrange
        when(glMasterCustomService.releaseLock(any(DocReleaseLockRequestDto.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> service.releaseLock(request));
        verify(glMasterCustomService, times(1)).releaseLock(any(DocReleaseLockRequestDto.class));
    }

    @Test
    void acquireLock_ShouldPassCorrectParametersToRepository() {
        // Arrange
        String expectedStatus = "LOCK_ACQUIRED";
        when(glMasterCustomService.acquireLock(any(DocReleaseLockRequestDto.class))).thenReturn(expectedStatus);

        // Act
        String result = service.acquireLock(request);

        // Assert
        assertEquals(expectedStatus, result);
        verify(glMasterCustomService).acquireLock(argThat(
                req -> req != null &&
                        req.getLoginGroupPoid().equals(1L) &&
                        req.getLoginCompanyPoid().equals(1L) &&
                        "100".equals(req.getLoginUserPoid()) &&
                        "400-001".equals(req.getDocId()) &&
                        req.getDocPoidValue().equals(123L) &&
                        "testUser".equals(req.getUserId())
        ));
    }
    @Test
    void listOfRecordsAndGenericSearch_ShouldReturnPaginatedResults_WhenFiltersProvided() {

        FilterRequestDto request = new FilterRequestDto("AND", "false", List.of(
                new FilterDto("GL_CODE", "1000")
        ));

        Pageable pageable = PageRequest.of(0, 10);
        List<Map<String, Object>> mockRecords = List.of(
                Map.of("GL_POID", 1L, "GL_DESCRIPTION", "Test GL 1"),
                Map.of("GL_POID", 2L, "GL_DESCRIPTION", "Test GL 2")
        );

        when(documentService.resolveOperator(any(FilterRequestDto.class))).thenReturn("AND");
        when(documentService.resolveIsDeleted(any(FilterRequestDto.class))).thenReturn("N");

        when(documentService.resolveFilters(any(FilterRequestDto.class))).thenAnswer(invocation -> {
            FilterRequestDto req = invocation.getArgument(0);
            return req.filters();
        });

        when(documentService.search(
                eq("400-001"),
                argThat(filters -> filters != null && !filters.isEmpty() &&
                        filters.get(0).searchField().equals("GL_CODE")),
                eq("AND"),
                eq(pageable),
                eq("N"),
                eq("GL_POID"),
                eq("GL_DESCRIPTION")
        )).thenReturn(new RawSearchResult(
                mockRecords,
                Map.of("GL_POID", "NUMBER", "GL_DESCRIPTION", "VARCHAR2"),
                2L
        ));


        Map<String, Object> result = service.listOfRecordsAndGenericSearch("400-001", request, pageable);

        assertNotNull(result);

        assertTrue(result.containsKey("content"), "Result should contain 'content' key");
        assertTrue(result.containsKey("displayFields"), "Result should contain 'displayFields' key");
        assertTrue(result.containsKey("pageNumber"), "Result should contain 'pageNumber' key");
        assertTrue(result.containsKey("pageSize"), "Result should contain 'pageSize' key");
        assertTrue(result.containsKey("totalElements"), "Result should contain 'totalElements' key");
        assertTrue(result.containsKey("totalPages"), "Result should contain 'totalPages' key");
        assertTrue(result.containsKey("last"), "Result should contain 'last' key");


        List<?> content = (List<?>) result.get("content");
        assertNotNull(content, "Content should not be null");


        assertEquals(2L, ((Number) result.get("totalElements")).longValue(), "Total elements should be 2");
        assertEquals(2, content.size(), "Content size should be 2 as we're returning all records on the first page");
        assertEquals(1, ((Number) result.get("totalPages")).intValue(), "Total pages should be 1");
        assertEquals(0, ((Number) result.get("pageNumber")).intValue(), "Current page number should be 0");
        assertEquals(10, ((Number) result.get("pageSize")).intValue(), "Page size should be 10");
        assertTrue((Boolean) result.get("last"), "This should be the last page since we only have one page of results");

        @SuppressWarnings("unchecked")
        Map<String, String> displayFields = (Map<String, String>) result.get("displayFields");
        assertNotNull(displayFields, "Display fields should not be null");
        assertEquals("NUMBER", displayFields.get("GL_POID"), "GL_POID type should be NUMBER");
        assertEquals("VARCHAR2", displayFields.get("GL_DESCRIPTION"), "GL_DESCRIPTION type should be VARCHAR2");


        Map<?, ?> firstItem = (Map<?, ?>) content.get(0);
        assertNotNull(firstItem, "First item should not be null");
        assertEquals(1L, firstItem.get("GL_POID"), "First item's GL_POID should be 1");
        assertEquals("Test GL 1", firstItem.get("GL_DESCRIPTION"), "First item's description should match");
    }

    @Test
    void listOfRecordsAndGenericSearch_ShouldReturnEmptyResults_WhenNoDataFound() {

        FilterRequestDto request = new FilterRequestDto("AND", "false", List.of());
        Pageable pageable = PageRequest.of(0, 10);

        when(documentService.search(
                eq("400-001"),
                anyList(),
                any(),
                eq(pageable),
                any(),
                eq("GL_POID"),
                eq("GL_DESCRIPTION")
        )).thenReturn(new RawSearchResult(Collections.emptyList(), Collections.emptyMap(), 0L));


        Map<String, Object> result = service.listOfRecordsAndGenericSearch("400-001", request, pageable);

        assertNotNull(result);
        assertEquals(0L, ((Number) result.get("totalElements")).longValue());
        assertTrue(((List<?>) result.get("content")).isEmpty());
    }


    @Test
    void listOfRecordsAndGenericSearch_ShouldIncludeDeletedRecords_WhenRequested() {

        FilterRequestDto request = new FilterRequestDto("AND", "true", List.of());
        Pageable pageable = PageRequest.of(0, 10);

        List<Map<String, Object>> mockRecords = List.of(
                Map.of("GL_POID", 1L, "GL_DESCRIPTION", "Test GL 1"),
                Map.of("GL_POID", 2L, "GL_DESCRIPTION", "Test GL 2")
        );


        when(documentService.resolveOperator(any(FilterRequestDto.class))).thenReturn("AND");
        when(documentService.resolveIsDeleted(any(FilterRequestDto.class))).thenReturn("Y"); // Should be "Y" for deleted records

        when(documentService.resolveFilters(any(FilterRequestDto.class))).thenReturn(Collections.emptyList());

        // Mock the search method to return a valid RawSearchResult
        when(documentService.search(
                eq("400-001"),
                eq(Collections.emptyList()),
                eq("AND"),
                eq(pageable),
                eq("Y"), // Should be "Y" for deleted records
                eq("GL_POID"),
                eq("GL_DESCRIPTION")
        )).thenReturn(new RawSearchResult(
                mockRecords,
                Map.of("GL_POID", "NUMBER", "GL_DESCRIPTION", "VARCHAR2"),
                2L
        ));


        Map<String, Object> result = service.listOfRecordsAndGenericSearch("400-001", request, pageable);

        assertNotNull(result);
        assertNotNull(result.get("content"));
        assertEquals(2, ((List<?>) result.get("content")).size());
    }

}


