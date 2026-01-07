//package com.asg.finance.service;
//
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.exception.ValidationException;
//import com.asg.finance.dto.FixedAssetCategoryRequestDto;
//import com.asg.finance.dto.FixedAssetCategoryResponseDto;
//import com.asg.finance.entity.CostCenter;
//import com.asg.finance.entity.FixedAssetCategory;
//import com.asg.finance.entity.GLMaster;
//import com.asg.finance.repository.CostCenterRepository;
//import com.asg.finance.repository.FixedAssetCategoryRepository;
//import com.asg.finance.repository.GLMasterRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class FixedAssetCategoryServiceImplTest {
//    @InjectMocks
//    private FixedAssetCategoryServiceImpl service;
//
//    @Mock
//    private FixedAssetCategoryRepository repository;
//
////    @Mock
////    private RoleRepository roleRepository;
//
//    @Mock
//    private CostCenterRepository costCenterRepository;
//
//    @Mock
//    private GLMasterRepository glMasterRepository;
//
//    private FixedAssetCategoryRequestDto requestDto;
//    private FixedAssetCategory entity;
//
//    @BeforeEach
//    void setUp() {
//        requestDto = new FixedAssetCategoryRequestDto();
//        requestDto.setFaCategoryDescription("Description 1");
//        requestDto.setFaCategoryDescription2("Desc 2");
//        requestDto.setAssetType("ASSET_TYPE_1");
//        requestDto.setFaGlAccount(1L);
//        requestDto.setFaAccumulationAccount(1L);
//        requestDto.setFaDepreciationAccount(1L);
//        requestDto.setCostCenter(1L);
//        requestDto.setUserRolePoid(List.of("1001;1002"));
//        requestDto.setActive("Y");
//        requestDto.setGroupPoid(10L);
//        requestDto.setSeqNo(1);
//
//        entity = new FixedAssetCategory();
//        entity.setFaCategoryPoid(1L);
//        entity.setFaCategoryCode("FAC001");
//        entity.setFaCategoryDescription("Description 1");
//        entity.setUserRolePoid("1;2");
//        entity.setActive("Y");
//        entity.setDeleted("N");
//    }
//
//    @Test
//    void testCreateFixedAssetCategory_Success() {
//        when(repository.existsByFaCategoryCodeIgnoreCase("FAC001")).thenReturn(false);
//        when(repository.existsByFaCategoryDescriptionIgnoreCase("Description 1")).thenReturn(false);
//        requestDto.setFaGlAccount(1001L);
//        requestDto.setFaAccumulationAccount(1002L);
//        requestDto.setFaDepreciationAccount(1003L);
//        requestDto.setCostCenter(2001L);
//        entity.setFaGlAccount("1001");
//        entity.setFaAccumulationAccount("1002");
//        entity.setFaDepreciationAccount("1003");
//        entity.setCostCenter("2001");
//
//        when(repository.save(any(FixedAssetCategory.class))).thenReturn(entity);
//        CostCenter mockCostCenter = new CostCenter();
//        mockCostCenter.setCostCenterPoid(2001L);
//        mockCostCenter.setCostCenterCode("CC2001");
//        mockCostCenter.setCostCenterDescription("Main Cost Center");
//        mockCostCenter.setSeqNo(1);
//        when(costCenterRepository.findByCostCenterPoid(2001L)).thenReturn(mockCostCenter);
//        GLMaster gl1 = new GLMaster();
//        gl1.setGlPoid(1001L);
//        gl1.setGlCode("GL1001");
//        gl1.setGlDescription("Main Account");
//        gl1.setSeqno(1);
//
//        GLMaster gl2 = new GLMaster();
//        gl2.setGlPoid(1002L);
//        gl2.setGlCode("GL1002");
//        gl2.setGlDescription("Accumulation");
//        gl2.setSeqno(2);
//
//        GLMaster gl3 = new GLMaster();
//        gl3.setGlPoid(1003L);
//        gl3.setGlCode("GL1003");
//        gl3.setGlDescription("Depreciation");
//        gl3.setSeqno(3);
//
//        when(glMasterRepository.findByGlPoidIn(anySet())).thenReturn(List.of(gl1, gl2, gl3));
//        FixedAssetCategoryResponseDto response = service.createFixedAssetCategory(requestDto);
//        assertNotNull(response);
//        assertEquals("FAC001", response.getFaCategoryCode());
//        assertEquals(1001L, response.getFaGlAccount());
//        assertEquals(2001L, response.getCostCenter());
//    }
//
//
//    @Test
//    void testCreateFixedAssetCategory_DuplicateCode() {
//        when(repository.existsByFaCategoryCodeIgnoreCase("FAC001")).thenReturn(true);
//
//        ValidationException ex = assertThrows(ValidationException.class, () ->
//                service.createFixedAssetCategory(requestDto));
//        assertTrue(ex.getMessage().contains("FA Category Code already exists"));
//    }
//
//    @Test
//    void testCreateFixedAssetCategory_DuplicateDescription() {
//        when(repository.existsByFaCategoryCodeIgnoreCase("FAC001")).thenReturn(false);
//        when(repository.existsByFaCategoryDescriptionIgnoreCase("Description 1")).thenReturn(true);
//
//        ValidationException ex = assertThrows(ValidationException.class, () ->
//                service.createFixedAssetCategory(requestDto));
//        assertTrue(ex.getMessage().contains("FA Category Description already exists"));
//    }
//
//    @Test
//    void testUpdateFixedAssetCategory_Success() {
//        requestDto.setFaCategoryDescription("Description 1");
//        requestDto.setFaGlAccount(1001L);
//        requestDto.setFaAccumulationAccount(1002L);
//        requestDto.setFaDepreciationAccount(1003L);
//        requestDto.setCostCenter(2001L);
//        requestDto.setActive("Y");
//        requestDto.setSeqNo(1);
//        FixedAssetCategory existing = new FixedAssetCategory();
//        existing.setFaCategoryPoid(1L);
//        existing.setFaCategoryCode("FAC001");
//        existing.setFaCategoryDescription("Old Desc");
//        existing.setFaGlAccount("1001");
//        existing.setFaAccumulationAccount("1002");
//        existing.setFaDepreciationAccount("1003");
//        existing.setCostCenter("2001");
//        existing.setActive("Y");
//        existing.setDeleted("N");
//        when(repository.findById(1L)).thenReturn(Optional.of(existing));
//        when(repository.existsByFaCategoryCodeIgnoreCaseAndFaCategoryPoidNot("FAC001", 1L)).thenReturn(false);
//        when(repository.existsByFaCategoryDescriptionIgnoreCaseAndFaCategoryPoidNot("Description 1", 1L)).thenReturn(false);
//        when(repository.save(any(FixedAssetCategory.class))).thenReturn(existing);
//        CostCenter mockCostCenter = new CostCenter();
//        mockCostCenter.setCostCenterPoid(2001L);
//        mockCostCenter.setCostCenterCode("CC2001");
//        mockCostCenter.setCostCenterDescription("Main Cost Center");
//        mockCostCenter.setSeqNo(1);
//        when(costCenterRepository.findByCostCenterPoid(2001L)).thenReturn(mockCostCenter);
//        GLMaster gl1 = new GLMaster();
//        gl1.setGlPoid(1001L);
//        gl1.setGlCode("GL1001");
//        gl1.setGlDescription("Main Account");
//        gl1.setSeqno(1);
//
//        GLMaster gl2 = new GLMaster();
//        gl2.setGlPoid(1002L);
//        gl2.setGlCode("GL1002");
//        gl2.setGlDescription("Accumulation Account");
//        gl2.setSeqno(2);
//
//        GLMaster gl3 = new GLMaster();
//        gl3.setGlPoid(1003L);
//        gl3.setGlCode("GL1003");
//        gl3.setGlDescription("Depreciation Account");
//        gl3.setSeqno(3);
//
//        when(glMasterRepository.findByGlPoidIn(anySet())).thenReturn(List.of(gl1, gl2, gl3));
//        FixedAssetCategoryResponseDto response = service.updateFixedAssetCategory(1L, requestDto);
//        assertNotNull(response);
//        assertEquals("FAC001", response.getFaCategoryCode());
//        assertEquals("Description 1", response.getFaCategoryDescription());
//        assertEquals(1001L, response.getFaGlAccount());
//        assertEquals(2001L, response.getCostCenter());
//
//    }
//
//
//    @Test
//    void testUpdateFixedAssetCategory_NotFound() {
//        when(repository.findById(999L)).thenReturn(Optional.empty());
//
//        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
//                service.updateFixedAssetCategory(999L, requestDto));
//        assertTrue(ex.getMessage().contains("Fixed Asset Category  not found with ID"));
//    }
//
//    @Test
//    void testUpdateFixedAssetCategory_DuplicateCode() {
//        FixedAssetCategory existing = new FixedAssetCategory();
//        existing.setFaCategoryPoid(1L);
//        existing.setFaCategoryCode("FAC001");
//
//        when(repository.findById(1L)).thenReturn(Optional.of(existing));
//        when(repository.existsByFaCategoryCodeIgnoreCaseAndFaCategoryPoidNot("FAC001", 1L)).thenReturn(true);
//
//        ValidationException ex = assertThrows(ValidationException.class, () ->
//                service.updateFixedAssetCategory(1L, requestDto));
//        assertTrue(ex.getMessage().contains("FA Category Code already exists"));
//    }
//
//   /* @Test
//    void testGetFixedAssetCategory_Success() {
//        FixedAssetCategory entity = new FixedAssetCategory();
//        entity.setFaCategoryPoid(1L);
//        entity.setFaCategoryCode("FAC001");
//        entity.setFaCategoryDescription("Description 1");
//        entity.setFaCategoryDescription2("Desc 2");
//        entity.setAssetType("ASSET_TYPE_1");
//        entity.setFaGlAccount("1001");
//        entity.setFaAccumulationAccount("1002");
//        entity.setFaDepreciationAccount("1003");
//        entity.setCostCenter("2001");
//        entity.setUserRolePoid("1;2");
//        entity.setActive("Y");
//        entity.setDeleted("N");
//        RoleEntity role1 = new RoleEntity();
//        role1.setUserRolePoid(1L);
//        role1.setUserRoleName("Admin");
//        role1.setActive("Y");
//
//        RoleEntity role2 = new RoleEntity();
//        role2.setUserRolePoid(2L);
//        role2.setUserRoleName("Approver");
//        role2.setActive("Y");
//        GLMaster gl1 = new GLMaster();
//        gl1.setGlPoid(1001L);
//        gl1.setGlCode("GL1001");
//        gl1.setGlDescription("Main Account");
//        gl1.setSeqno(1);
//
//        GLMaster gl2 = new GLMaster();
//        gl2.setGlPoid(1002L);
//        gl2.setGlCode("GL1002");
//        gl2.setGlDescription("Accumulation Account");
//        gl2.setSeqno(2);
//
//        GLMaster gl3 = new GLMaster();
//        gl3.setGlPoid(1003L);
//        gl3.setGlCode("GL1003");
//        gl3.setGlDescription("Depreciation Account");
//        gl3.setSeqno(3);
//        CostCenter costCenter = new CostCenter();
//        costCenter.setCostCenterPoid(2001L);
//        costCenter.setCostCenterCode("CC2001");
//        costCenter.setCostCenterDescription("Main Cost Center");
//        costCenter.setSeqNo(1);
//        when(repository.findById(1L)).thenReturn(Optional.of(entity));
//        when(roleRepository.findByUserRolePoid(1L)).thenReturn(role1);
//        when(roleRepository.findByUserRolePoid(2L)).thenReturn(role2);
//        when(glMasterRepository.findByGlPoidIn(anySet())).thenReturn(List.of(gl1, gl2, gl3));
//        when(costCenterRepository.findByCostCenterPoid(2001L)).thenReturn(costCenter);
//        FixedAssetCategoryResponseDto response = service.getFixedAssetCategory(1L);
//        assertNotNull(response);
//        assertEquals("FAC001", response.getFaCategoryCode());
//        assertEquals("Description 1", response.getFaCategoryDescription());
//        assertEquals(1001L, response.getFaGlAccount());
//        assertEquals(2, response.getUserRolesPoidDet().size());
//        verify(repository).findById(1L);
//        verify(roleRepository, times(2)).findByUserRolePoid(anyLong());
//        verify(glMasterRepository).findByGlPoidIn(anySet());
//        verify(costCenterRepository).findByCostCenterPoid(2001L);
//    }*/
//
//
//    @Test
//    void testGetFixedAssetCategory_NotFound() {
//        when(repository.findById(999L)).thenReturn(Optional.empty());
//
//        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
//                service.getFixedAssetCategory(999L));
//        assertTrue(ex.getMessage().contains("Fixed Asset Category not found with ID"));
//    }
//
//    @Test
//    void testSoftDeleteFixedAssetCategory_Success() {
//        FixedAssetCategory existing = new FixedAssetCategory();
//        existing.setFaCategoryPoid(1L);
//        existing.setDeleted("N");
//        existing.setActive("Y");
//
//        when(repository.findById(1L)).thenReturn(Optional.of(existing));
//        when(repository.save(any(FixedAssetCategory.class))).thenReturn(existing);
//
//        assertDoesNotThrow(() -> service.softDeleteFixedAssetCategory(1L));
//        assertEquals("Y", existing.getDeleted());
//        assertEquals("N", existing.getActive());
//    }
//
//    @Test
//    void testSoftDeleteFixedAssetCategory_NotFound() {
//        when(repository.findById(999L)).thenReturn(Optional.empty());
//
//        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
//                service.softDeleteFixedAssetCategory(999L));
//        assertTrue(ex.getMessage().contains("Fixed Asset Category not found with ID"));
//    }
//
//}
//
