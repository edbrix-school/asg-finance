//package com.asg.finance.service;
//
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.exception.ValidationException;
//import com.asg.common.lib.service.DocumentSearchService;
//import com.asg.common.lib.service.LovDataService;
//import com.asg.finance.dto.masters.InsuranceMasterRequestDto;
//import com.asg.finance.dto.masters.InsuranceMasterResponseDto;
//import com.asg.finance.dto.masters.InsurancePicDetailRequestDto;
//import com.asg.finance.dto.masters.InsuranceVehicleDetailRequestDto;
//import com.asg.finance.entity.master.InsuranceMaster;
//import com.asg.finance.repository.master.InsuranceMasterRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class InsuranceMasterServiceImplTest {
//
//    @Mock
//    private InsuranceMasterRepository insuranceMasterRepository;
//    @Mock
//    private DocumentSearchService documentService;
////    @Mock
////    private RoleRepository roleRepository;
//    @Mock
//    private LovDataService lovService;
//
//    private InsuranceMasterServiceImpl service;
//
//    private InsuranceMasterRequestDto requestDto;
//    private InsuranceMaster insuranceMaster;
//
//    @BeforeEach
//    void setUp() {
//        service = new InsuranceMasterServiceImpl(insuranceMasterRepository, documentService);
//        ReflectionTestUtils.setField(service, "roleRepository", roleRepository);
//        ReflectionTestUtils.setField(service, "lovService", lovService);
//
//        requestDto = InsuranceMasterRequestDto.builder()
//                .insuranceType("Property Insurance")
//                .category("Property")
//                .policyNo("PROP001")
//                .insuranceProvider("ABC Insurance")
//                .fromDate(LocalDate.of(2025, 1, 1))
//                .expiryDate(LocalDate.of(2025, 12, 31))
//                .insuranceAmount(new BigDecimal("100000"))
//                .premiumAmount(new BigDecimal("5000"))
//                .description("Test Insurance")
//                .build();
//
//        insuranceMaster = InsuranceMaster.builder()
//                .transactionPoid(1L)
//                .groupPoid(1L)
//                .companyPoid(1L)
//                .insuranceType("Property Insurance")
//                .insuranceCategory("Property")
//                .policyNo("PROP001")
//                .insuranceProvider("ABC Insurance")
//                .fromDate(LocalDate.of(2025, 1, 1))
//                .expiryDate(LocalDate.of(2025, 12, 31))
//                .insuranceAmount(new BigDecimal("100000"))
//                .premiumAmount(new BigDecimal("5000"))
//                .description("Test Insurance")
//                .deleted("N")
//                .createdBy("SYSTEM")
//                .createdDate(LocalDateTime.now())
//                .vehicleDetails(new ArrayList<>())
//                .employeeDetails(new ArrayList<>())
//                .propertyDetails(new ArrayList<>())
//                .insuranceDetails(new ArrayList<>())
//                .picDetails(new ArrayList<>())
//                .renewalLogs(new ArrayList<>())
//                .build();
//    }
//
//    @Test
//    void createInsuranceMaster_Success() {
//        when(insuranceMasterRepository.existsByPolicyNoAndCompanyPoid(anyString(), anyLong())).thenReturn(false);
//        when(insuranceMasterRepository.save(any(InsuranceMaster.class))).thenReturn(insuranceMaster);
//
//        InsuranceMasterResponseDto result = service.createInsuranceMaster(requestDto);
//
//        assertNotNull(result);
//        assertEquals("PROP001", result.getPolicyNo());
//        assertEquals("Property Insurance", result.getInsuranceType());
//        assertEquals("Active", result.getStatus());
//        verify(insuranceMasterRepository, times(2)).save(any(InsuranceMaster.class));
//    }
//
//    @Test
//    void createInsuranceMaster_DuplicatePolicyNumber_ThrowsException() {
//        when(insuranceMasterRepository.existsByPolicyNoAndCompanyPoid(anyString(), anyLong())).thenReturn(true);
//
//        assertThrows(RuntimeException.class, () -> service.createInsuranceMaster(requestDto));
//        verify(insuranceMasterRepository, never()).save(any(InsuranceMaster.class));
//    }
//
//    @Test
//    void updateInsuranceMaster_Success() {
//        when(insuranceMasterRepository.findById(1L)).thenReturn(Optional.of(insuranceMaster));
//        when(insuranceMasterRepository.existsByPolicyNoAndCompanyPoidAndTransactionPoidNot(anyString(), anyLong(), anyLong())).thenReturn(false);
//        when(insuranceMasterRepository.save(any(InsuranceMaster.class))).thenReturn(insuranceMaster);
//
//        InsuranceMasterResponseDto result = service.updateInsuranceMaster(1L, requestDto);
//
//        assertNotNull(result);
//        assertEquals("PROP001", result.getPolicyNo());
//        verify(insuranceMasterRepository, times(1)).save(any(InsuranceMaster.class));
//    }
//
//    @Test
//    void updateInsuranceMaster_NotFound_ThrowsException() {
//        when(insuranceMasterRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> service.updateInsuranceMaster(1L, requestDto));
//    }
//
//    @Test
//    void updateInsuranceMaster_DuplicatePolicyNumber_ThrowsException() {
//        when(insuranceMasterRepository.findById(1L)).thenReturn(Optional.of(insuranceMaster));
//        when(insuranceMasterRepository.existsByPolicyNoAndCompanyPoidAndTransactionPoidNot(anyString(), anyLong(), anyLong())).thenReturn(true);
//
//        assertThrows(ValidationException.class, () -> service.updateInsuranceMaster(1L, requestDto));
//    }
//
//    @Test
//    void getInsuranceMasterById_Success() {
//        when(insuranceMasterRepository.findById(1L)).thenReturn(Optional.of(insuranceMaster));
//
//        InsuranceMasterResponseDto result = service.getInsuranceMasterById(1L);
//
//        assertNotNull(result);
//        assertEquals(1L, result.getInsurancePoid());
//        assertEquals("PROP001", result.getPolicyNo());
//        assertEquals("Property Insurance", result.getInsuranceType());
//    }
//
//    @Test
//    void getInsuranceMasterById_NotFound_ThrowsException() {
//        when(insuranceMasterRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> service.getInsuranceMasterById(1L));
//    }
//
//    @Test
//    void softDeleteInsuranceMaster_Success() {
//        when(insuranceMasterRepository.findById(1L)).thenReturn(Optional.of(insuranceMaster));
//        when(insuranceMasterRepository.hasPjReference(1L)).thenReturn(false);
//        when(insuranceMasterRepository.save(any(InsuranceMaster.class))).thenReturn(insuranceMaster);
//
//        service.softDeleteInsuranceMaster(1L);
//
//        assertEquals("Y", insuranceMaster.getDeleted());
//        assertNotNull(insuranceMaster.getLastModifiedDate());
//        assertNotNull(insuranceMaster.getLastModifiedBy());
//        verify(insuranceMasterRepository, times(1)).save(insuranceMaster);
//    }
//
//    @Test
//    void softDeleteInsuranceMaster_LinkedToPJ_ThrowsException() {
//        when(insuranceMasterRepository.findById(1L)).thenReturn(Optional.of(insuranceMaster));
//        when(insuranceMasterRepository.hasPjReference(1L)).thenReturn(true);
//
//        assertThrows(ValidationException.class, () -> service.softDeleteInsuranceMaster(1L));
//        verify(insuranceMasterRepository, never()).save(any(InsuranceMaster.class));
//    }
//
//    @Test
//    void softDeleteInsuranceMaster_NotFound_ThrowsException() {
//        when(insuranceMasterRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> service.softDeleteInsuranceMaster(1L));
//    }
//
//    @Test
//    void createInsuranceMaster_WithChildDetails_Success() {
//        requestDto.setVehicleDetails(List.of(
//                InsuranceVehicleDetailRequestDto.builder()
//                        .amount(new BigDecimal("50000"))
//                        .remarks("Test vehicle")
//                        .build()
//        ));
//
//        requestDto.setPicDetails(List.of(
//                InsurancePicDetailRequestDto.builder()
//                        .rolePoid(1L)
//                        .contactType("Email")
//                        .picPerson("John Doe")
//                        .fromDate(LocalDate.now())
//                        .build()
//        ));
//
//        RoleEntity roleEntity = new RoleEntity();
//        roleEntity.setUserRolePoid(1L);
//        roleEntity.setUserRoleId("ADMIN");
//        roleEntity.setUserRoleName("Administrator");
//        roleEntity.setSeqNo(1);
//        when(roleRepository.findById(1L)).thenReturn(Optional.of(roleEntity));
//
//        when(insuranceMasterRepository.existsByPolicyNoAndCompanyPoid(anyString(), anyLong())).thenReturn(false);
//        when(insuranceMasterRepository.save(any(InsuranceMaster.class))).thenReturn(insuranceMaster);
//
//        InsuranceMasterResponseDto result = service.createInsuranceMaster(requestDto);
//
//        assertNotNull(result);
//        assertEquals("PROP001", result.getPolicyNo());
//        verify(insuranceMasterRepository, times(2)).save(any(InsuranceMaster.class));
//    }
//}
