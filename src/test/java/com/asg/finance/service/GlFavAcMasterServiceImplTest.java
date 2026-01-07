//package com.asg.finance.service;
//
//import com.asg.common.lib.dto.FilterDto;
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.dto.RawSearchResult;
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.exception.ValidationException;
//import com.asg.common.lib.security.util.UserContext;
//import com.asg.common.lib.service.DocumentSearchService;
//import com.asg.common.lib.service.LovDataService;
//import com.asg.finance.dto.GlAccountDetailRequest;
//import com.asg.finance.dto.GlFavAcMasterRequest;
//import com.asg.finance.dto.GlFavAcMasterResponse;
//import com.asg.finance.entity.GlFavAcMaster;
//import com.asg.finance.entity.GlFavAcMasterGlAcDtl;
//import com.asg.finance.entity.GlFavAcMasterUserRoleDtl;
//import com.asg.finance.repository.GLMasterRepository;
//import com.asg.finance.repository.GlFavAcMasterGlAcDtlRepository;
//import com.asg.finance.repository.GlFavAcMasterRepository;
//import com.asg.finance.repository.GlFavAcMasterUserRoleDtlRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.sql.Timestamp;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//class GlFavAcMasterServiceImplTest {
//
//    @Mock
//    private GlFavAcMasterRepository masterRepository;
//
//    @Mock
//    private GlFavAcMasterGlAcDtlRepository glAcDtlRepository;
//
//    @Mock
//    private GlFavAcMasterUserRoleDtlRepository userRoleDtlRepository;
//
////    @Mock
////    private RoleRepository roleRepository;
//
//    @Mock
//    private DocumentSearchService documentService;
//
//    @Mock
//    private GLMasterRepository glMasterRepository;
//
//    @Mock
//    private LovDataService lovService;
//
//    @InjectMocks
//    private GlFavAcMasterServiceImpl service;
//
//    private Pageable pageable;
//    private FilterRequestDto filterRequestDto;
//
//    private GlFavAcMasterRequest validRequest;
//    private GlFavAcMaster savedMaster;
//    private GlAccountDetailRequest glAccountDetail;
//    private final Long favAcPoid = 101L;
//
//    @BeforeEach
//    void setUp() {
//        pageable = PageRequest.of(0, 10);
//        filterRequestDto = new FilterRequestDto("AND", "N", Collections.emptyList());
//        glAccountDetail = GlAccountDetailRequest.builder()
//                .glAccountPoId(10010001L)
//                .companyPoId(1L)
//                .viewCategoryPoid("BANK_ACC")
//                .seqNo(1L)
//                .remarks("Primary bank account")
//                .build();
//
//        validRequest = GlFavAcMasterRequest.builder()
//                .favAcCode("BANKS")
//                .description("Bank Accounts Group")
//                .description2("For treasury reporting")
//                .seqNo(1)
//                .active("Y")
//                .groupPoid(1L)
//                .userRolePoids(Arrays.asList(1L, 2L))
//                .glAccounts(Collections.singletonList(glAccountDetail))
//                .build();
//
//        savedMaster = GlFavAcMaster.builder()
//                .favAcPoid(favAcPoid)
//                .groupPoid(1L)
//                .favAcCode("BANKS")
//                .description("Bank Accounts Group")
//                .description2("For treasury reporting")
//                .active("Y")
//                .seqNo(1)
//                .createdBy("SYSTEM")
//                .createdDate(new Timestamp(System.currentTimeMillis()))
//                .lastModifiedBy("SYSTEM")
//                .lastModifiedDate(new Timestamp(System.currentTimeMillis()))
//                .deleted("N")
//                .build();
//
//        // Mock LovService calls
//        Map<String, Object> lovResponse = new HashMap<>();
//        lovResponse.put("data", Collections.emptyList());
//        when(lovService.getLovList(anyString(), anyLong(), anyLong(), anyLong(), anyString(), anyInt(), anyInt(), anyString(), anyString()))
//                .thenReturn(lovResponse);
//    }
//
//    @Test
//    void createFavoriteAccount_WithValidRequest_ShouldCreateSuccessfully() {
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
//            mockedUserContext.when(UserContext::getUserId).thenReturn("testUser");
//
//            when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.empty());
//            when(masterRepository.findByDescriptionAndGroupPoid("Bank Accounts Group", 1L)).thenReturn(Optional.empty());
//            when(roleRepository.existsByUserRolePoid(1L)).thenReturn(true);
//            when(roleRepository.existsByUserRolePoid(2L)).thenReturn(true);
//            when(masterRepository.save(any(GlFavAcMaster.class))).thenReturn(savedMaster);
//            when(masterRepository.findByFavAcPoid(favAcPoid)).thenReturn(Optional.of(savedMaster));
//            when(glAcDtlRepository.findByFavAcPoidOrderBySeqNo(favAcPoid)).thenReturn(Collections.emptyList());
//            when(userRoleDtlRepository.findByFavAcPoid(favAcPoid)).thenReturn(Collections.emptyList());
//            when(glMasterRepository.findByGlPoidIn(anyList())).thenReturn(Collections.emptyList());
//
//            GlFavAcMasterResponse response = service.createFavoriteAccount(validRequest);
//
//            assertNotNull(response);
//            assertEquals(favAcPoid, response.getFavAcPoid());
//            assertEquals("BANKS", response.getFavAcCode());
//            assertEquals("Bank Accounts Group", response.getDescription());
//            assertEquals("Y", response.getActive());
//
//            verify(masterRepository).save(any(GlFavAcMaster.class));
//            verify(glAcDtlRepository).save(any(GlFavAcMasterGlAcDtl.class));
//            verify(userRoleDtlRepository, times(2)).save(any(GlFavAcMasterUserRoleDtl.class));
//        }
//    }
//
//    @Test
//    void createFavoriteAccount_WithDuplicateFavAcCode_ShouldThrowValidationException() {
//        when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.of(savedMaster));
//
//        ValidationException exception = assertThrows(ValidationException.class, () ->
//                service.createFavoriteAccount(validRequest)
//        );
//
//        assertEquals("Fav Ac Code already exists: BANKS", exception.getMessage());
//        verify(masterRepository, never()).save(any(GlFavAcMaster.class));
//    }
//
//    @Test
//    void createFavoriteAccount_WithDuplicateDescription_ShouldThrowValidationException() {
//        when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.empty());
//        when(masterRepository.findByDescriptionAndGroupPoid("Bank Accounts Group", 1L)).thenReturn(Optional.of(savedMaster));
//
//        ValidationException exception = assertThrows(ValidationException.class, () ->
//                service.createFavoriteAccount(validRequest)
//        );
//
//        assertEquals("Description already exists: Bank Accounts Group", exception.getMessage());
//        verify(masterRepository, never()).save(any(GlFavAcMaster.class));
//    }
//
//    @Test
//    void createFavoriteAccount_WithNullGlAccounts_ShouldThrowValidationException() {
//        validRequest.setGlAccounts(null);
//
//        ValidationException exception = assertThrows(ValidationException.class, () ->
//                service.createFavoriteAccount(validRequest)
//        );
//
//        assertEquals("At least one GL account must be mapped", exception.getMessage());
//        verify(masterRepository, never()).save(any(GlFavAcMaster.class));
//    }
//
//    @Test
//    void createFavoriteAccount_WithEmptyGlAccounts_ShouldThrowValidationException() {
//        validRequest.setGlAccounts(Collections.emptyList());
//
//        ValidationException exception = assertThrows(ValidationException.class, () ->
//                service.createFavoriteAccount(validRequest)
//        );
//
//        assertEquals("At least one GL account must be mapped", exception.getMessage());
//        verify(masterRepository, never()).save(any(GlFavAcMaster.class));
//    }
//
//    @Test
//    void createFavoriteAccount_WithMissingCompany_ShouldThrowValidationException() {
//        glAccountDetail.setCompanyPoId(null);
//        validRequest.setGlAccounts(Collections.singletonList(glAccountDetail));
//
//        when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.empty());
//        when(masterRepository.findByDescriptionAndGroupPoid("Bank Accounts Group", 1L)).thenReturn(Optional.empty());
//
//        ValidationException exception = assertThrows(ValidationException.class, () ->
//                service.createFavoriteAccount(validRequest)
//        );
//
//        assertEquals("Company is mandatory for all GL accounts", exception.getMessage());
//        verify(masterRepository, never()).save(any(GlFavAcMaster.class));
//    }
//
//    @Test
//    void createFavoriteAccount_WithMissingViewCategory_ShouldThrowValidationException() {
//        glAccountDetail.setViewCategoryPoid(null);
//        validRequest.setGlAccounts(Collections.singletonList(glAccountDetail));
//
//        when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.empty());
//        when(masterRepository.findByDescriptionAndGroupPoid("Bank Accounts Group", 1L)).thenReturn(Optional.empty());
//
//        ValidationException exception = assertThrows(ValidationException.class, () ->
//                service.createFavoriteAccount(validRequest)
//        );
//
//        assertEquals("View Category is mandatory for all GL accounts", exception.getMessage());
//        verify(masterRepository, never()).save(any(GlFavAcMaster.class));
//    }
//
//    @Test
//    void createFavoriteAccount_WithBlankViewCategory_ShouldThrowValidationException() {
//        glAccountDetail.setViewCategoryPoid("");
//        validRequest.setGlAccounts(Collections.singletonList(glAccountDetail));
//
//        when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.empty());
//        when(masterRepository.findByDescriptionAndGroupPoid("Bank Accounts Group", 1L)).thenReturn(Optional.empty());
//
//        ValidationException exception = assertThrows(ValidationException.class, () ->
//                service.createFavoriteAccount(validRequest)
//        );
//
//        assertEquals("View Category is mandatory for all GL accounts", exception.getMessage());
//        verify(masterRepository, never()).save(any(GlFavAcMaster.class));
//    }
//
//    @Test
//    void createFavoriteAccount_WithDuplicateGlAccounts_ShouldThrowValidationException() {
//        GlAccountDetailRequest duplicate = GlAccountDetailRequest.builder()
//                .glAccountPoId(10010001L)
//                .companyPoId(1L)
//                .viewCategoryPoid("BANK_ACC")
//                .seqNo(2L)
//                .build();
//
//        validRequest.setGlAccounts(Arrays.asList(glAccountDetail, duplicate));
//
//        when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.empty());
//        when(masterRepository.findByDescriptionAndGroupPoid("Bank Accounts Group", 1L)).thenReturn(Optional.empty());
//
//        ValidationException exception = assertThrows(ValidationException.class, () ->
//                service.createFavoriteAccount(validRequest)
//        );
//
//        assertEquals("GL Account should not be duplicated for the same Favorite Account Group", exception.getMessage());
//        verify(masterRepository, never()).save(any(GlFavAcMaster.class));
//    }
//
//    @Test
//    void createFavoriteAccount_WithNonExistentUserRole_ShouldThrowValidationException() {
//        when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.empty());
//        when(masterRepository.findByDescriptionAndGroupPoid("Bank Accounts Group", 1L)).thenReturn(Optional.empty());
//        when(roleRepository.existsByUserRolePoid(1L)).thenReturn(true);
//        when(roleRepository.existsByUserRolePoid(2L)).thenReturn(false);
//
//        ValidationException exception = assertThrows(ValidationException.class, () ->
//                service.createFavoriteAccount(validRequest)
//        );
//
//        assertEquals("User Role does not exist: 2", exception.getMessage());
//        verify(masterRepository, never()).save(any(GlFavAcMaster.class));
//    }
//
//    @Test
//    void createFavoriteAccount_WithNullUserRoles_ShouldCreateSuccessfully() {
//        validRequest.setUserRolePoids(null);
//
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
//            mockedUserContext.when(UserContext::getUserId).thenReturn("testUser");
//
//            when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.empty());
//            when(masterRepository.findByDescriptionAndGroupPoid("Bank Accounts Group", 1L)).thenReturn(Optional.empty());
//            when(masterRepository.save(any(GlFavAcMaster.class))).thenReturn(savedMaster);
//            when(masterRepository.findByFavAcPoid(favAcPoid)).thenReturn(Optional.of(savedMaster));
//            when(glAcDtlRepository.findByFavAcPoidOrderBySeqNo(favAcPoid)).thenReturn(Collections.emptyList());
//            when(userRoleDtlRepository.findByFavAcPoid(favAcPoid)).thenReturn(Collections.emptyList());
//            when(glMasterRepository.findByGlPoidIn(anyList())).thenReturn(Collections.emptyList());
//
//            GlFavAcMasterResponse response = service.createFavoriteAccount(validRequest);
//
//            assertNotNull(response);
//            verify(userRoleDtlRepository, never()).save(any(GlFavAcMasterUserRoleDtl.class));
//        }
//    }
//
//    @Test
//    void createFavoriteAccount_WithEmptyUserRoles_ShouldCreateSuccessfully() {
//        validRequest.setUserRolePoids(Collections.emptyList());
//
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
//            mockedUserContext.when(UserContext::getUserId).thenReturn("testUser");
//
//            when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.empty());
//            when(masterRepository.findByDescriptionAndGroupPoid("Bank Accounts Group", 1L)).thenReturn(Optional.empty());
//            when(masterRepository.save(any(GlFavAcMaster.class))).thenReturn(savedMaster);
//            when(masterRepository.findByFavAcPoid(favAcPoid)).thenReturn(Optional.of(savedMaster));
//            when(glAcDtlRepository.findByFavAcPoidOrderBySeqNo(favAcPoid)).thenReturn(Collections.emptyList());
//            when(userRoleDtlRepository.findByFavAcPoid(favAcPoid)).thenReturn(Collections.emptyList());
//            when(glMasterRepository.findByGlPoidIn(anyList())).thenReturn(Collections.emptyList());
//
//            GlFavAcMasterResponse response = service.createFavoriteAccount(validRequest);
//
//            assertNotNull(response);
//            verify(userRoleDtlRepository, never()).save(any(GlFavAcMasterUserRoleDtl.class));
//        }
//    }
//
//    @Test
//    void updateFavoriteAccount_WithValidRequest_ShouldUpdateSuccessfully() {
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
//            mockedUserContext.when(UserContext::getUserId).thenReturn("testUser");
//
//            when(masterRepository.findByFavAcPoid(favAcPoid)).thenReturn(Optional.of(savedMaster));
//            when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.of(savedMaster));
//            when(masterRepository.findByDescriptionAndGroupPoid("Bank Accounts Group", 1L)).thenReturn(Optional.of(savedMaster));
//            when(roleRepository.existsByUserRolePoid(1L)).thenReturn(true);
//            when(roleRepository.existsByUserRolePoid(2L)).thenReturn(true);
//            when(masterRepository.save(any(GlFavAcMaster.class))).thenReturn(savedMaster);
//            when(glAcDtlRepository.findByFavAcPoidOrderBySeqNo(favAcPoid)).thenReturn(Collections.emptyList());
//            when(userRoleDtlRepository.findByFavAcPoid(favAcPoid)).thenReturn(Collections.emptyList());
//            when(glMasterRepository.findByGlPoidIn(anyList())).thenReturn(Collections.emptyList());
//
//            GlFavAcMasterResponse response = service.updateFavoriteAccount(favAcPoid, validRequest);
//
//            assertNotNull(response);
//            assertEquals(favAcPoid, response.getFavAcPoid());
//
//            verify(masterRepository).save(any(GlFavAcMaster.class));
//            verify(glAcDtlRepository).deleteByFavAcPoid(favAcPoid);
//            verify(userRoleDtlRepository).deleteByFavAcPoid(favAcPoid);
//            verify(glAcDtlRepository).save(any(GlFavAcMasterGlAcDtl.class));
//            verify(userRoleDtlRepository, times(2)).save(any(GlFavAcMasterUserRoleDtl.class));
//        }
//    }
//
//    @Test
//    void updateFavoriteAccount_WithNonExistentId_ShouldThrowResourceNotFoundException() {
//        when(masterRepository.findByFavAcPoid(999L)).thenReturn(Optional.empty());
//
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
//                service.updateFavoriteAccount(999L, validRequest)
//        );
//
//        assertEquals("Favorite Account Master not found with favAcPoid : '999'", exception.getMessage());
//        verify(masterRepository, never()).save(any(GlFavAcMaster.class));
//    }
//
//    @Test
//    void updateFavoriteAccount_WithDuplicateFavAcCode_ShouldThrowValidationException() {
//        GlFavAcMaster otherMaster = GlFavAcMaster.builder().favAcPoid(999L).build();
//
//        when(masterRepository.findByFavAcPoid(favAcPoid)).thenReturn(Optional.of(savedMaster));
//        when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.of(otherMaster));
//
//        ValidationException exception = assertThrows(ValidationException.class, () ->
//                service.updateFavoriteAccount(favAcPoid, validRequest)
//        );
//
//        assertEquals("Fav Ac Code already exists: BANKS", exception.getMessage());
//        verify(masterRepository, never()).save(any(GlFavAcMaster.class));
//    }
//
//    @Test
//    void updateFavoriteAccount_WithDuplicateDescription_ShouldThrowValidationException() {
//        GlFavAcMaster otherMaster = GlFavAcMaster.builder().favAcPoid(999L).build();
//
//        when(masterRepository.findByFavAcPoid(favAcPoid)).thenReturn(Optional.of(savedMaster));
//        when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.of(savedMaster));
//        when(masterRepository.findByDescriptionAndGroupPoid("Bank Accounts Group", 1L)).thenReturn(Optional.of(otherMaster));
//
//        ValidationException exception = assertThrows(ValidationException.class, () ->
//                service.updateFavoriteAccount(favAcPoid, validRequest)
//        );
//
//        assertEquals("Description already exists: Bank Accounts Group", exception.getMessage());
//        verify(masterRepository, never()).save(any(GlFavAcMaster.class));
//    }
//
//    @Test
//    void getFavoriteAccountById_WithValidId_ShouldReturnResponse() {
//        when(masterRepository.findByFavAcPoid(favAcPoid)).thenReturn(Optional.of(savedMaster));
//        when(glAcDtlRepository.findByFavAcPoidOrderBySeqNo(favAcPoid)).thenReturn(Collections.emptyList());
//        when(userRoleDtlRepository.findByFavAcPoid(favAcPoid)).thenReturn(Collections.emptyList());
//        when(glMasterRepository.findByGlPoidIn(anyList())).thenReturn(Collections.emptyList());
//
//        GlFavAcMasterResponse response = service.getFavoriteAccountById(favAcPoid);
//
//        assertNotNull(response);
//        assertEquals(favAcPoid, response.getFavAcPoid());
//        assertEquals("BANKS", response.getFavAcCode());
//        assertEquals("Bank Accounts Group", response.getDescription());
//    }
//
//    @Test
//    void getFavoriteAccountById_WithNonExistentId_ShouldThrowResourceNotFoundException() {
//        when(masterRepository.findByFavAcPoid(999L)).thenReturn(Optional.empty());
//
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
//                service.getFavoriteAccountById(999L)
//        );
//
//        assertEquals("Favorite Account Master not found with favAcPoid : '999'", exception.getMessage());
//    }
//
//    @Test
//    void createFavoriteAccount_WithNullUserContext_ShouldUseSystemUser() {
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
//            mockedUserContext.when(UserContext::getUserId).thenReturn(null);
//
//            when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.empty());
//            when(masterRepository.findByDescriptionAndGroupPoid("Bank Accounts Group", 1L)).thenReturn(Optional.empty());
//            when(roleRepository.existsByUserRolePoid(1L)).thenReturn(true);
//            when(roleRepository.existsByUserRolePoid(2L)).thenReturn(true);
//            when(masterRepository.save(any(GlFavAcMaster.class))).thenReturn(savedMaster);
//            when(masterRepository.findByFavAcPoid(favAcPoid)).thenReturn(Optional.of(savedMaster));
//            when(glAcDtlRepository.findByFavAcPoidOrderBySeqNo(favAcPoid)).thenReturn(Collections.emptyList());
//            when(userRoleDtlRepository.findByFavAcPoid(favAcPoid)).thenReturn(Collections.emptyList());
//            when(glMasterRepository.findByGlPoidIn(anyList())).thenReturn(Collections.emptyList());
//
//            GlFavAcMasterResponse response = service.createFavoriteAccount(validRequest);
//
//            assertNotNull(response);
//            verify(masterRepository).save(argThat(master -> "SYSTEM".equals(master.getCreatedBy())));
//        }
//    }
//
//    @Test
//    void createFavoriteAccount_WithNullActiveField_ShouldDefaultToY() {
//        validRequest.setActive(null);
//
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
//            mockedUserContext.when(UserContext::getUserId).thenReturn("testUser");
//
//            when(masterRepository.findByFavAcCodeAndGroupPoid("BANKS", 1L)).thenReturn(Optional.empty());
//            when(masterRepository.findByDescriptionAndGroupPoid("Bank Accounts Group", 1L)).thenReturn(Optional.empty());
//            when(roleRepository.existsByUserRolePoid(1L)).thenReturn(true);
//            when(roleRepository.existsByUserRolePoid(2L)).thenReturn(true);
//            when(masterRepository.save(any(GlFavAcMaster.class))).thenReturn(savedMaster);
//            when(masterRepository.findByFavAcPoid(favAcPoid)).thenReturn(Optional.of(savedMaster));
//            when(glAcDtlRepository.findByFavAcPoidOrderBySeqNo(favAcPoid)).thenReturn(Collections.emptyList());
//            when(userRoleDtlRepository.findByFavAcPoid(favAcPoid)).thenReturn(Collections.emptyList());
//            when(glMasterRepository.findByGlPoidIn(anyList())).thenReturn(Collections.emptyList());
//
//            GlFavAcMasterResponse response = service.createFavoriteAccount(validRequest);
//
//            assertNotNull(response);
//            verify(masterRepository).save(argThat(master -> "Y".equals(master.getActive())));
//        }
//    }
//
//    @Test
//    void testListOfRecordsAndGenericSearch_ReturnsPaginatedMap() {
//        // Arrange
//        String docId = "400-004";
//        String operator = "AND";
//        String isDeleted = "N";
//        List<FilterDto> filters = Collections.singletonList(new FilterDto("BANK_NAME", "SBI"));
//
//        Map<String, Object> record1 = new HashMap<>();
//        record1.put("FAC_AC_POID", 101);
//        record1.put("DESCRIPTION", "Savings Account");
//
//        List<Map<String, Object>> records = Collections.singletonList(record1);
//        List<String> displayFields = Arrays.asList("FAC_AC_POID", "DESCRIPTION");
//
//        Map<String, String> displayFieldsMap = new HashMap<>();
//        displayFields.forEach(field -> displayFieldsMap.put(field, field));
//        RawSearchResult rawResult = new RawSearchResult(records, displayFieldsMap, 1L);
//
//        when(documentService.resolveOperator(filterRequestDto)).thenReturn(operator);
//        when(documentService.resolveIsDeleted(filterRequestDto)).thenReturn(isDeleted);
//        when(documentService.resolveFilters(filterRequestDto)).thenReturn(filters);
//
//        when(documentService.search(eq(docId), eq(filters), eq(operator), eq(pageable), eq(isDeleted),
//                any(), any())).thenReturn(rawResult);
//
//        Map<String, Object> result = service.listOfRecordsAndGenericSearch(docId, filterRequestDto, pageable);
//
//        assertNotNull(result);
//        assertTrue(result.containsKey("content"), "Response should contain 'content' key");
//        assertEquals(1, ((List<?>) result.get("content")).size(),
//                "Expected one record in content");
//        verify(documentService, times(1)).search(eq(docId), eq(filters), eq(operator), eq(pageable), eq(isDeleted),
//                any(), any());
//    }
//
//    @Test
//    void testListOfRecordsAndGenericSearch_WhenNoRecords_ReturnsEmptyMap() {
//        when(documentService.resolveOperator(any())).thenReturn("AND");
//        when(documentService.resolveIsDeleted(any())).thenReturn("N");
//        when(documentService.resolveFilters(any())).thenReturn(Collections.emptyList());
//
//        when(documentService.search(anyString(), anyList(), anyString(), any(Pageable.class), anyString(), any(), any()))
//                .thenReturn(new RawSearchResult(Collections.emptyList(), Collections.emptyMap(), 0L));
//
//        Map<String, Object> result = service.listOfRecordsAndGenericSearch("400-004", new FilterRequestDto("AND", "N", Collections.emptyList()), pageable);
//
//        assertNotNull(result);
//        verify(documentService, times(1)).search(anyString(), anyList(), anyString(), any(Pageable.class), anyString(), any(), any());
//    }
//}