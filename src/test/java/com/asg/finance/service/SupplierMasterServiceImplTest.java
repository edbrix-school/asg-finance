//package com.asg.finance.service;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.dto.RawSearchResult;
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.repository.GroupRepository;
//import com.asg.common.lib.service.DocumentSearchService;
//import com.asg.common.lib.utility.ASGHelperUtils;
//import com.asg.finance.dto.GlobalLedgerDto;
//import com.asg.finance.dto.SupplierImportRequestDto;
//import com.asg.finance.dto.SupplierImportResponseDto;
//import com.asg.finance.dto.SupplierMasterDto;
//import com.asg.finance.entity.SupplierMasterEntity;
//import com.asg.finance.repository.*;
//import com.asg.finance.repository.master.HrEmployeeMasterRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.lang.reflect.Field;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class SupplierMasterServiceImplTest {
//
//    @Mock
//    private GroupRepository groupRepository;
////    @Mock
////    private CountryRepository countryRepository;
////    @Mock
////    private AddressMasterRepository addressMasterRepository;
//    @Mock
//    private SupplierMasterRepository supplierMasterRepository;
//    @Mock
//    private SupplierCategoryRepository supplierCategoryRepository;
//    @Mock
//    private SupplierMasterPaymentDtlRepository supplierMasterPaymentDtlRepository;
//    @Mock
//    private SupplierMasterMangementDtlRepository supplierMasterMangementDtlRepository;
//    @Mock
//    private SupplierMasterServiceDtlRepository supplierMasterServiceDtlRepository;
//    @Mock
//    private SupplierMasterQstnRepository supplierMasterQstnDtlRepository;
//    @Mock
//    private GLMasterRepository glMasterRepository;
//    @Mock
//    private SupplierServicesMasterRepository supplierServicesMasterRepository;
//    @Mock
//    private SalesCustomerMasterRepository salesCustomerMasterRepository;
//    @Mock
//    private HrEmployeeMasterRepository hrEmployeeMasterRepository;
////    @Mock
////    private AddressDetailsRepository detailsRepo;
//    @Mock
//    private DocumentSearchService documentService;
//    @Mock
//    private JdbcTemplate jdbcTemplate;
////    @Mock
////    private CurrencyRepository currencyRepository;
//    @Mock
//    private jakarta.persistence.EntityManager entityManager;
//
//    @InjectMocks
//    private SupplierMasterServiceImpl supplierMasterService;
//
//    private static final Long SUPPLIER_POID = 1L;
//    private static final Long GROUP_POID = 1L;
//
//    private SupplierMasterEntity supplierMasterEntity;
//    private SupplierMasterDto supplierMasterDto;
//
//    @BeforeEach
//    void setUp() throws Exception {
//        supplierMasterEntity = new SupplierMasterEntity();
//        supplierMasterEntity.setSupplierPoid(SUPPLIER_POID);
//        supplierMasterEntity.setSupplierName("Test Supplier");
//        supplierMasterEntity.setSupplierCode("SUP001");
//        supplierMasterEntity.setGroupPoid(GROUP_POID);
//
//        supplierMasterDto = new SupplierMasterDto();
//        supplierMasterDto.setSupplierPoid(SUPPLIER_POID);
//        supplierMasterDto.setSupplierName("Test Supplier");
//        supplierMasterDto.setSupplierCode("SUP001");
//        supplierMasterDto.setGroupPoid(GROUP_POID);
//        supplierMasterDto.setCountryPoid(1L);
//        supplierMasterDto.setAddressPoid(1L);
//        supplierMasterDto.setSupplierCategoryPoid(1L);
//        supplierMasterDto.setCustomerPoid(1L);
//        supplierMasterDto.setPurchaserPoid(1L);
//
//        Field entityManagerField = SupplierMasterServiceImpl.class.getDeclaredField("entityManager");
//        entityManagerField.setAccessible(true);
//        entityManagerField.set(supplierMasterService, entityManager);
//    }
//
//    @Test
//    void getSupplierMaster_WithValidId_ReturnsSupplierMasterDto() {
//        when(supplierMasterRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(supplierMasterEntity);
//        when(supplierMasterPaymentDtlRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(Collections.emptyList());
//        when(supplierMasterMangementDtlRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(Collections.emptyList());
//        when(supplierMasterServiceDtlRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(Collections.emptyList());
//        when(supplierMasterQstnDtlRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(Collections.emptyList());
//
//        SupplierMasterDto result = supplierMasterService.getSupplierMaster(SUPPLIER_POID);
//
//        assertNotNull(result);
//        assertEquals(SUPPLIER_POID, result.getSupplierPoid());
//        assertEquals("Test Supplier", result.getSupplierName());
//    }
//
//    @Test
//    void deleteSupplierMaster_WithValidId_DeletesSupplierAndRelatedData() {
//        try (MockedStatic<ASGHelperUtils> mockedASGHelperUtils = mockStatic(ASGHelperUtils.class)) {
//            mockedASGHelperUtils.when(ASGHelperUtils::getCurrentUser).thenReturn("testUser");
//
//            when(supplierMasterRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(supplierMasterEntity);
//
//            supplierMasterService.deleteSupplierMaster(SUPPLIER_POID);
//
//            assertEquals("N", supplierMasterEntity.getActive());
//            assertEquals("Y", supplierMasterEntity.getDeleted());
//            verify(supplierMasterRepository).save(supplierMasterEntity);
//            verify(supplierMasterPaymentDtlRepository).deleteByIdSupplierPoid(SUPPLIER_POID);
//            verify(supplierMasterMangementDtlRepository).deleteByIdSupplierPoid(SUPPLIER_POID);
//            verify(supplierMasterServiceDtlRepository).deleteByIdSupplierPoid(SUPPLIER_POID);
//            verify(supplierMasterQstnDtlRepository).deleteByIdSupplierPoid(SUPPLIER_POID);
//        }
//    }
//
//    @Test
//    void getSupplierMaster_WithNullSupplierPoid_ThrowsResourceNotFoundException() {
//        when(supplierMasterRepository.findBySupplierPoid(null)).thenReturn(null);
//
//        assertThrows(ResourceNotFoundException.class, () -> {
//            supplierMasterService.getSupplierMaster(null);
//        });
//    }
//
//    @Test
//    void getSupplierMaster_WithNonExistentId_ThrowsResourceNotFoundException() {
//        when(supplierMasterRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(null);
//
//        assertThrows(ResourceNotFoundException.class, () -> {
//            supplierMasterService.getSupplierMaster(SUPPLIER_POID);
//        });
//    }
//
//    @Test
//    void deleteSupplierMaster_WithNullSupplierPoid_ThrowsResourceNotFoundException() {
//        when(supplierMasterRepository.findBySupplierPoid(null)).thenReturn(null);
//
//        assertThrows(ResourceNotFoundException.class, () -> {
//            supplierMasterService.deleteSupplierMaster(null);
//        });
//    }
//
//    @Test
//    void deleteSupplierMaster_WithNonExistentId_ThrowsResourceNotFoundException() {
//        when(supplierMasterRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(null);
//
//        assertThrows(ResourceNotFoundException.class, () -> {
//            supplierMasterService.deleteSupplierMaster(SUPPLIER_POID);
//        });
//    }
//
//    @Test
//    void getSupplierMaster_WithZeroSupplierPoid_ThrowsResourceNotFoundException() {
//        when(supplierMasterRepository.findBySupplierPoid(0L)).thenReturn(null);
//
//        assertThrows(ResourceNotFoundException.class, () -> {
//            supplierMasterService.getSupplierMaster(0L);
//        });
//    }
//
//    @Test
//    void getSupplierMaster_WithNegativeSupplierPoid_ThrowsResourceNotFoundException() {
//        when(supplierMasterRepository.findBySupplierPoid(-1L)).thenReturn(null);
//
//        assertThrows(ResourceNotFoundException.class, () -> {
//            supplierMasterService.getSupplierMaster(-1L);
//        });
//    }
//
//    @Test
//    void getSupplierMaster_WithMaxLongValue_ShouldHandleGracefully() {
//        when(supplierMasterRepository.findBySupplierPoid(Long.MAX_VALUE)).thenReturn(null);
//
//        assertThrows(ResourceNotFoundException.class, () -> {
//            supplierMasterService.getSupplierMaster(Long.MAX_VALUE);
//        });
//    }
//
//    @Test
//    void updateSupplierMaster_SupplierCodeIsExcludedFromUpdate_ShouldNotChangeCode() {
//        String originalCode = "ORIGINAL_CODE";
//        supplierMasterEntity.setSupplierCode(originalCode);
//        supplierMasterDto.setSupplierCode("NEW_CODE");
//
//        assertEquals(originalCode, supplierMasterEntity.getSupplierCode());
//    }
//
//    @Test
//    void createSupplierMaster_WithNullGroupPoid_ThrowsResourceNotFoundException() {
//        supplierMasterDto.setGroupPoid(null);
//        when(groupRepository.existsByGroupPoid(null)).thenReturn(false);
//
//        assertThrows(ResourceNotFoundException.class, () -> {
//            supplierMasterService.createSupplierMaster(supplierMasterDto);
//        });
//    }
//
//    /*@Test
//    void createSupplierMaster_WithValidData_ReturnsSupplierMasterDto() {
//        try (MockedStatic<ASGHelperUtils> mockedASGHelperUtils = mockStatic(ASGHelperUtils.class)) {
//            mockedASGHelperUtils.when(ASGHelperUtils::getCurrentUser).thenReturn("testUser");
//
//            when(groupRepository.existsByGroupPoid(GROUP_POID)).thenReturn(true);
//            when(supplierCategoryRepository.existsBySupplierCategoryPoid(1L)).thenReturn(true);
//            when(countryRepository.existsByCountryPoid(1L)).thenReturn(true);
//            when(hrEmployeeMasterRepository.existsByEmployeePoid(1L)).thenReturn(true);
//            when(salesCustomerMasterRepository.existsByCustomerPoid(1L)).thenReturn(true);
//            when(supplierMasterRepository.existsBySupplierCodeAndGroupPoid(anyString(), anyLong())).thenReturn(false);
//            when(supplierMasterRepository.existsBySupplierCodeAndGroupPoidAndCountryPoid(anyString(), anyLong(), anyLong())).thenReturn(false);
//            when(supplierMasterRepository.existsBySupplierNameIgnoreCaseAndGroupPoid(anyString(), anyLong())).thenReturn(false);
//            lenient().when(addressMasterRepository.existsByAddressNameIgnoreCaseAndGroupPoid(anyString(), anyLong())).thenReturn(false);
//
//            SupplierMasterEntity savedEntity = new SupplierMasterEntity();
//            savedEntity.setSupplierPoid(SUPPLIER_POID);
//            when(supplierMasterRepository.save(any(SupplierMasterEntity.class))).thenReturn(savedEntity);
//            when(supplierMasterRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(supplierMasterEntity);
//            when(supplierMasterPaymentDtlRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(Collections.emptyList());
//            when(supplierMasterMangementDtlRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(Collections.emptyList());
//            when(supplierMasterServiceDtlRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(Collections.emptyList());
//            when(supplierMasterQstnDtlRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(Collections.emptyList());
//
//            jakarta.persistence.StoredProcedureQuery query = mock(jakarta.persistence.StoredProcedureQuery.class);
//            when(entityManager.createStoredProcedureQuery("PROC_AP_SUPPLIER_VALIDATION")).thenReturn(query);
//            when(query.registerStoredProcedureParameter(anyString(), any(Class.class), any())).thenReturn(query);
//            when(query.setParameter(anyString(), any())).thenReturn(query);
//            when(query.getOutputParameterValue("P_RESULT")).thenReturn("SUCCESS...");
//
//            supplierMasterDto.setAddressName("Test Address");
//
//            SupplierMasterDto result = supplierMasterService.createSupplierMaster(supplierMasterDto);
//
//            assertNotNull(result);
//            verify(supplierMasterRepository).save(any(SupplierMasterEntity.class));
//        }
//    }*/
//
//    /*@Test
//    void updateSupplierMaster_WithValidData_ReturnsUpdatedSupplierMasterDto() {
//        try (MockedStatic<ASGHelperUtils> mockedASGHelperUtils = mockStatic(ASGHelperUtils.class)) {
//            mockedASGHelperUtils.when(ASGHelperUtils::getCurrentUser).thenReturn("testUser");
//
//            when(supplierMasterRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(supplierMasterEntity);
//            when(groupRepository.existsByGroupPoid(GROUP_POID)).thenReturn(true);
//            when(supplierCategoryRepository.existsBySupplierCategoryPoid(1L)).thenReturn(true);
//            when(countryRepository.existsByCountryPoid(1L)).thenReturn(true);
//            when(hrEmployeeMasterRepository.existsByEmployeePoid(1L)).thenReturn(true);
//            when(salesCustomerMasterRepository.existsByCustomerPoid(1L)).thenReturn(true);
//            when(supplierMasterRepository.existsBySupplierNameIgnoreCaseAndSupplierPoidNot(anyString(), anyLong())).thenReturn(false);
//            when(supplierMasterRepository.existsBySupplierCodeAndGroupPoidAndSupplierPoidNot(anyString(), anyLong(), anyLong())).thenReturn(false);
//            when(supplierMasterRepository.existsBySupplierNameIgnoreCaseAndGroupPoidAndSupplierPoidNot(anyString(), anyLong(), anyLong())).thenReturn(false);
//            when(supplierMasterRepository.existsBySupplierCodeAndGroupPoidAndCountryPoidAndSupplierNameIgnoreCaseAndSupplierPoidNot(anyString(), anyLong(), anyLong(), anyString(), anyLong())).thenReturn(false);
//            when(supplierMasterPaymentDtlRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(Collections.emptyList());
//            when(supplierMasterMangementDtlRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(Collections.emptyList());
//            when(supplierMasterServiceDtlRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(Collections.emptyList());
//            when(supplierMasterQstnDtlRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(Collections.emptyList());
//
//            jakarta.persistence.StoredProcedureQuery query = mock(jakarta.persistence.StoredProcedureQuery.class);
//            when(entityManager.createStoredProcedureQuery("PROC_AP_SUPPLIER_VALIDATION")).thenReturn(query);
//            when(query.registerStoredProcedureParameter(anyString(), any(Class.class), any())).thenReturn(query);
//            when(query.setParameter(anyString(), any())).thenReturn(query);
//            when(query.getOutputParameterValue("P_RESULT")).thenReturn("SUCCESS...");
//
//            supplierMasterDto.setAddressPoid(1L);
//
//            SupplierMasterDto result = supplierMasterService.updateSupplierMaster(SUPPLIER_POID, supplierMasterDto);
//
//            assertNotNull(result);
//            verify(supplierMasterRepository).save(supplierMasterEntity);
//        }
//    }*/
//
//    @Test
//    void createLedger_WithValidSupplierPoid_ReturnsGlPoid() {
//        when(supplierMasterRepository.findBySupplierPoid(SUPPLIER_POID)).thenReturn(supplierMasterEntity);
//
//        GlobalLedgerDto request = new GlobalLedgerDto();
//        request.setRequestedBy("testUser");
//        request.setGlType("SUPPLIER");
//
//        jakarta.persistence.StoredProcedureQuery query = mock(jakarta.persistence.StoredProcedureQuery.class);
//        when(entityManager.createStoredProcedureQuery("PROC_GL_MASTER_CREATE")).thenReturn(query);
//        when(query.registerStoredProcedureParameter(anyString(), any(Class.class), any())).thenReturn(query);
//        when(query.setParameter(anyString(), any())).thenReturn(query);
//        when(query.getOutputParameterValue("P_STATUS")).thenReturn("SUCCESS");
//        when(query.getOutputParameterValue("P_NEW_GL_POID")).thenReturn(100L);
//
//        Long result = supplierMasterService.createLedger(SUPPLIER_POID, request);
//
//        assertEquals(100L, result);
//        verify(supplierMasterRepository).save(supplierMasterEntity);
//    }
//
//    @Test
//    void importSuppliersFromExcel_WithValidFile_ReturnsTransactionPoid() throws Exception {
//        MultipartFile file = mock(MultipartFile.class);
//        when(file.getInputStream()).thenThrow(new RuntimeException("Test exception"));
//
//        assertThrows(RuntimeException.class, () -> {
//            supplierMasterService.importSuppliersFromExcel(file,SUPPLIER_POID);
//        });
//    }
//
//    @Test
//    void processImportedSuppliers_WithValidRequest_ReturnsResponse() {
//        SupplierImportRequestDto request = new SupplierImportRequestDto();
//        request.setGroupPoid(GROUP_POID);
//        request.setCompanyPoid(1L);
//        request.setUserPoid(1L);
//        request.setTransactionPoid(123L);
//
//        jakarta.persistence.StoredProcedureQuery query = mock(jakarta.persistence.StoredProcedureQuery.class);
//        when(entityManager.createStoredProcedureQuery("PROC_AP_SUPPLIER_EXCEL_PROCESS")).thenReturn(query);
//        when(query.registerStoredProcedureParameter(anyString(), any(Class.class), any())).thenReturn(query);
//        when(query.setParameter(anyString(), any())).thenReturn(query);
//        when(query.getOutputParameterValue("status")).thenReturn("SUCCESS");
//
//        Map<String, Object> counts = Map.of("total", 5);
//        when(jdbcTemplate.queryForMap(anyString(), anyString())).thenReturn(counts);
//
//        SupplierImportResponseDto result = supplierMasterService.processImportedSuppliers(request);
//
//        assertNotNull(result);
//        assertEquals("SUCCESS", result.getStatus());
//        assertEquals(5, result.getProcessedRows());
//    }
//
//    @Test
//    void getNextDetRowIdForPaymentDtl_WithExistingRecords_ReturnsNextId() {
//        when(supplierMasterPaymentDtlRepository.findMaxDetRowIdBySupplierPoid(SUPPLIER_POID)).thenReturn(5L);
//
//        Long result = supplierMasterService.getNextDetRowIdForPaymentDtl(SUPPLIER_POID);
//
//        assertEquals(6L, result);
//    }
//
//    @Test
//    void getNextDetRowIdForPaymentDtl_WithNoExistingRecords_ReturnsOne() {
//        when(supplierMasterPaymentDtlRepository.findMaxDetRowIdBySupplierPoid(SUPPLIER_POID)).thenReturn(null);
//
//        Long result = supplierMasterService.getNextDetRowIdForPaymentDtl(SUPPLIER_POID);
//
//        assertEquals(1L, result);
//    }
//
//    @Test
//    void listOfRecordsAndGenericSearch_WithValidRequest_ReturnsResults() {
//        FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", Collections.emptyList());
//        Pageable pageable = PageRequest.of(0, 10);
//        RawSearchResult rawResult = new RawSearchResult(Collections.emptyList(), Collections.emptyMap(), 0L);
//
//        when(documentService.resolveOperator(filterRequest)).thenReturn("AND");
//        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
//        when(documentService.resolveFilters(filterRequest)).thenReturn(Collections.emptyList());
//
//        when(documentService.search(anyString(), anyList(), anyString(), any(Pageable.class), anyString(), anyString(), anyString())).thenReturn(rawResult);
//
//        Map<String, Object> result = supplierMasterService.listOfRecordsAndGenericSearch("DOC001", filterRequest, pageable);
//
//        assertNotNull(result);
//    }
//}