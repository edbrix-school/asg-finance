//package com.asg.finance.service;
//
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.finance.dto.PettyCashUserRoleRequestDto;
//import com.asg.finance.dto.PettyCashUserroleResponseDto;
//import com.asg.finance.entity.PettyCashUserroleMaster;
//import com.asg.finance.repository.PettyCashUserroleMasterRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class PettyCashUserRoleServiceImplTest {
//
//    @Mock
//    private PettyCashUserroleMasterRepository repository;
//
////    @Mock
////    private RoleRepository roleRepository;
//
//    @InjectMocks
//    private PettyCashUserRoleServiceImpl service;
//
//    private PettyCashUserRoleRequestDto requestDto;
//    private PettyCashUserroleMaster savedEntity;
//
//    @BeforeEach
//    void setUp() {
//        requestDto = new PettyCashUserRoleRequestDto();
//        requestDto.setRefTypePoid(1L);
//        requestDto.setRefType("TEST_REF");
//        requestDto.setDescription("Test Description");
//        requestDto.setUserRolePoid(List.of("1001", "1002"));
//        requestDto.setActive("Y");
//        requestDto.setSeqno(1);
//
//        savedEntity = new PettyCashUserroleMaster();
//        savedEntity.setRefTypePoid(1L);
//        savedEntity.setRefType("TEST_REF");
//        savedEntity.setDescription("Test Description");
//        savedEntity.setUserRolePoid("1001;1002");
//        savedEntity.setActive("Y");
//        savedEntity.setDeleted("N");
//        savedEntity.setSeqNo(1);
//        savedEntity.setCreatedBy("SYSTEM");
//        //savedEntity.setCreatedDate(LocalDateTime.now());
//    }
//
////    @Test
////    void testCreatePettyCashUserRole() {
////        RoleEntity role = new RoleEntity();
////        role.setUserRolePoid(1001L);
////        role.setUserRoleName("Admin");
////        role.setActive("Y");
////        when(repository.save(any(PettyCashUserroleMaster.class))).thenReturn(savedEntity);
////        when(roleRepository.findByUserRolePoid(1001L)).thenReturn(role);
////        when(roleRepository.findByUserRolePoid(1002L)).thenReturn(role);
////
////        PettyCashUserroleResponseDto response = service.createPettyCashUserRole(requestDto);
////
////        assertNotNull(response);
////        assertEquals("TEST_REF", response.getRefType());
////        assertEquals("Y", response.getActive());
////        assertEquals(List.of("1001", "1002"), response.getUserRolePoid());
////        verify(repository, times(1)).save(any(PettyCashUserroleMaster.class));
////    }
////
////    @Test
////    void testUpdatePettyCashUserRole_Success() {
////        RoleEntity role = new RoleEntity();
////        role.setUserRolePoid(1001L);
////        role.setUserRoleName("Admin");
////        role.setActive("Y");
////        when(repository.findById(1L)).thenReturn(Optional.of(savedEntity));
////        when(repository.save(any(PettyCashUserroleMaster.class))).thenReturn(savedEntity);
////        when(roleRepository.findByUserRolePoid(1001L)).thenReturn(role);
////        when(roleRepository.findByUserRolePoid(1002L)).thenReturn(role);
////
////        PettyCashUserroleResponseDto result = service.updatePettyCashUserRole(1L, requestDto);
////
////        assertNotNull(result);
////        assertEquals("TEST_REF", result.getRefType());
////        verify(repository, times(1)).save(any(PettyCashUserroleMaster.class));
////    }
////
////    @Test
////    void testUpdatePettyCashUserRole_NotFound() {
////        when(repository.findById(1L)).thenReturn(Optional.empty());
////        assertThrows(ResourceNotFoundException.class, () -> service.updatePettyCashUserRole(1L, requestDto));
////    }
////
////    @Test
////    void testGetPettyCashUserRole_Success() {
////        RoleEntity role = new RoleEntity();
////        role.setUserRolePoid(1001L);
////        role.setUserRoleName("Admin");
////        role.setActive("Y");
////        when(repository.findById(1L)).thenReturn(Optional.of(savedEntity));
////        when(roleRepository.findByUserRolePoid(1001L)).thenReturn(role);
////        when(roleRepository.findByUserRolePoid(1002L)).thenReturn(role);
////
////        PettyCashUserroleResponseDto result = service.getPettyCashUserRole(1L);
////
////        assertNotNull(result);
////        assertEquals("TEST_REF", result.getRefType());
////    }
////
////    @Test
////    void testGetPettyCashUserRole_NotFound() {
////        when(repository.findById(1L)).thenReturn(Optional.empty());
////        assertThrows(ResourceNotFoundException.class, () -> service.getPettyCashUserRole(1L));
////    }
//
//    @Test
//    void testSoftDeletePettyCashUserRole_Success() {
//        Long refTypePoid = 1L;
//        PettyCashUserroleMaster entity = new PettyCashUserroleMaster();
//        entity.setRefTypePoid(refTypePoid);
//        entity.setDeleted("N");
//        entity.setActive("Y");
//        when(repository.findById(refTypePoid)).thenReturn(Optional.of(entity));
//        when(repository.save(any(PettyCashUserroleMaster.class))).thenReturn(entity);
//        service.softDeletePettyCashUserRole(refTypePoid);
//        assertEquals("Y", entity.getDeleted());
//        assertEquals("N", entity.getActive());
//        assertNotNull(entity.getLastModifiedDate());
//        assertNotNull(entity.getLastModifiedBy());
//        verify(repository, times(1)).findById(refTypePoid);
//        verify(repository, times(1)).save(entity);
//    }
//
//    @Test
//    void testSoftDeletePettyCashUserRole_NotFound() {
//        Long refTypePoid = 1L;
//        when(repository.findById(refTypePoid)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> service.softDeletePettyCashUserRole(refTypePoid));
//        verify(repository, times(1)).findById(refTypePoid);
//        verify(repository, never()).save(any());
//    }
//
//
//}
//
