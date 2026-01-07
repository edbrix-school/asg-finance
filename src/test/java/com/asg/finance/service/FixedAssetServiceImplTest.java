//package com.asg.finance.service;
//
//import com.asg.common.lib.entity.Company;
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.exception.ValidationException;
//import com.asg.common.lib.service.LovDataService;
//import com.asg.finance.dto.masters.FixedAssetRequestDto;
//import com.asg.finance.dto.masters.FixedAssetResponseDto;
//import com.asg.finance.dto.masters.VehicleDetailsDto;
//import com.asg.finance.entity.AssetLocation;
//import com.asg.finance.entity.FixedAssetCategory;
//import com.asg.finance.entity.SupplierMasterEntity;
//import com.asg.finance.entity.master.FixedAsset;
//import com.asg.finance.entity.master.HrEmployeeMaster;
//import com.asg.finance.repository.AssetLocationMasterRepository;
//import com.asg.finance.repository.FixedAssetCategoryRepository;
//import com.asg.finance.repository.SupplierMasterRepository;
//import com.asg.finance.repository.master.FixedAssetRepository;
//import com.asg.finance.repository.master.HrEmployeeMasterRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class FixedAssetServiceImplTest {
//
//    @Mock
//    private FixedAssetRepository repository;
//    @Mock
//    private AssetLocationMasterRepository locationMasterRepository;
//    @Mock
//    private FixedAssetCategoryRepository fixedAssetCategoryRepository;
////    @Mock
////    private CompanyRepository companyRepository;
//
//    private LovDataService lovDataService;
//
//    @Mock
//    private HrEmployeeMasterRepository hrEmployeeMasterRepository;
//    @Mock
//    private SupplierMasterRepository supplierMasterRepository;
//
//    private FixedAssetServiceImpl service;
//
//    private FixedAssetRequestDto requestDto;
//    private FixedAsset fixedAsset;
//
//    @BeforeEach
//    void setUp() {
//        service = new FixedAssetServiceImpl(repository);
//        ReflectionTestUtils.setField(service, "locationMasterRepository", locationMasterRepository);
//        ReflectionTestUtils.setField(service, "fixedAssetCategoryRepository", fixedAssetCategoryRepository);
//        ReflectionTestUtils.setField(service, "companyRepository", companyRepository);
//        ReflectionTestUtils.setField(service, "hrEmployeeMasterRepository", hrEmployeeMasterRepository);
//        ReflectionTestUtils.setField(service, "supplierMasterRepository", supplierMasterRepository);
//
//        requestDto = new FixedAssetRequestDto();
//        requestDto.setFaCode("FA001");
//        requestDto.setFaDescription("Test Asset");
//        requestDto.setAssetType("EQUIPMENT");
//        requestDto.setFaCategoryPoid(1L);
//        requestDto.setLocationPoid(1L);
//        requestDto.setCompanyPoid(1L);
//        requestDto.setActive("Y");
//
//        fixedAsset = new FixedAsset();
//        fixedAsset.setFaPoid(1L);
//        fixedAsset.setFaCode("FA001");
//        fixedAsset.setFaDescription("Test Asset");
//        fixedAsset.setLocationPoid(1L);
//        fixedAsset.setFaCategoryPoid(1L);
//        fixedAsset.setCompanyPoid(1L);
//        fixedAsset.setSupplierPoid(1L);
//        fixedAsset.setEmployeePoid(1L);
//        fixedAsset.setCreatedBy("SYSTEM");
//        fixedAsset.setCreatedDate(LocalDateTime.now());
//        fixedAsset.setAssetType("EQUIPMENT");
//        fixedAsset.setActive("Y");
//        fixedAsset.setDeleted("N");
//    }
//
////    @Test
////    void createFixedAsset_Success() {
////        when(repository.existsByFaDescription(anyString())).thenReturn(false);
////        when(repository.save(any(FixedAsset.class))).thenReturn(fixedAsset);
////        mockRepositoryResponses();
////
////        FixedAssetResponseDto result = service.createFixedAsset(requestDto);
////
////        assertNotNull(result);
////        assertEquals("FA001", result.getFaCode());
////        assertEquals("Test Asset", result.getFaDescription());
////        verify(repository, times(1)).save(any(FixedAsset.class));
////    }
//
//    @Test
//    void createFixedAsset_DuplicateDescription_ThrowsException() {
//        when(repository.existsByFaDescription(anyString())).thenReturn(true);
//
//        assertThrows(IllegalArgumentException.class, () -> service.createFixedAsset(requestDto));
//    }
//
//    @Test
//    void validateVehicleDetails_InvalidType_ThrowsException() {
//        VehicleDetailsDto vehicleDto = new VehicleDetailsDto();
//        vehicleDto.setVehicleType("");
//        requestDto.setVehicleDetailsDto(vehicleDto);
//
//        when(repository.existsByFaDescription(anyString())).thenReturn(false);
//
//        assertThrows(ValidationException.class, () -> service.createFixedAsset(requestDto));
//    }
//
////    @Test
////    void updateFixedAsset_Success() {
////        when(repository.findById(1L)).thenReturn(Optional.of(fixedAsset));
////        when(repository.existsByFaCodeAndFaPoidNot(anyString(), anyLong())).thenReturn(false);
////        when(repository.existsByFaDescriptionAndFaPoidNot(anyString(), anyLong())).thenReturn(false);
////        when(repository.save(any(FixedAsset.class))).thenReturn(fixedAsset);
////        mockRepositoryResponses();
////
////        FixedAssetResponseDto result = service.updateFixedAsset(1L, requestDto);
////
////        assertNotNull(result);
////        assertEquals("FA001", result.getFaCode());
////        verify(repository, times(1)).save(any(FixedAsset.class));
////    }
//
//    @Test
//    void updateFixedAsset_NotFound_ThrowsException() {
//        when(repository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> service.updateFixedAsset(1L, requestDto));
//    }
//
////    @Test
////    void getFixedAssetById_Success() {
////        when(repository.findById(1L)).thenReturn(Optional.of(fixedAsset));
////        mockRepositoryResponses();
////
////        FixedAssetResponseDto result = service.getFixedAssetById(1L);
////
////        assertNotNull(result);
////        assertEquals(1L, result.getFaPoid());
////        assertEquals("FA001", result.getFaCode());
////        assertEquals("Test Asset", result.getFaDescription());
////    }
//
//    @Test
//    void getFixedAssetById_NotFound_ThrowsException() {
//        when(repository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> service.getFixedAssetById(1L));
//    }
//
//    @Test
//    void softDeleteFixedAsset_Success() {
//        when(repository.findByFaPoid(1L)).thenReturn(Optional.of(fixedAsset));
//        when(repository.save(any(FixedAsset.class))).thenReturn(fixedAsset);
//
//        service.softDeleteFixedAsset(1L);
//
//        assertEquals("Y", fixedAsset.getDeleted());
//        assertEquals("N", fixedAsset.getActive());
//        assertNotNull(fixedAsset.getLastModifiedDate());
//        assertNotNull(fixedAsset.getLastModifiedBy());
//        verify(repository, times(1)).findByFaPoid(1L);
//        verify(repository, times(1)).save(fixedAsset);
//    }
//
//    @Test
//    void createMultipleCopies_Success() {
//        when(repository.findByFaPoid(1L)).thenReturn(Optional.of(fixedAsset));
//        when(repository.saveAll(anyList())).thenReturn(List.of(fixedAsset));
//
//        List<Long> result = service.createMultipleCopies(1L, 2);
//
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        verify(repository, times(1)).saveAll(anyList());
//    }
//
////    private void mockRepositoryResponses() {
////        AssetLocation location = new AssetLocation();
////        location.setLocationPoid(1L);
////        location.setLocationCode("LOC001");
////        location.setDescription("Test Location");
////        location.setSeqNo(1);
////        when(locationMasterRepository.findByLocationPoid(anyLong())).thenReturn(Optional.of(location));
////
////        FixedAssetCategory category = new FixedAssetCategory();
////        category.setFaCategoryPoid(1L);
////        category.setFaCategoryCode("CAT001");
////        category.setFaCategoryDescription("Test Category");
////        category.setSeqNo(1);
////        when(fixedAssetCategoryRepository.findByFaCategoryPoid(anyLong())).thenReturn(Optional.of(category));
////
////        Company company = new Company();
////        company.setCompanyPoid(1L);
////        company.setCompanyCode("COMP001");
////        company.setLabel("Test Company");
////        company.setValue(1L);
////        company.setSeqNo(1);
////        when(companyRepository.findByCompanyPoid(anyLong())).thenReturn(company);
////
////        SupplierMasterEntity supplier = new SupplierMasterEntity();
////        supplier.setSupplierPoid(1L);
////        supplier.setSupplierCode("SUP001");
////        supplier.setSupplierName("Test Supplier");
////        supplier.setSeqNo(1L);
////        when(supplierMasterRepository.findBySupplierPoid(anyLong())).thenReturn(supplier);
////
////        HrEmployeeMaster employee = new HrEmployeeMaster();
////        employee.setEmployeePoid(1L);
////        employee.setEmployeeCode("EMP001");
////        employee.setEmployeeName("Test Employee");
////        employee.setSeqNo(1);
////        when(hrEmployeeMasterRepository.findByEmployeePoid(anyLong())).thenReturn(Optional.of(employee));
////    }
//}
