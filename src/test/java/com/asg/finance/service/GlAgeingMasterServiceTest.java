package com.asg.finance.service;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.finance.dto.GlAgeingMasterDtlDto;
import com.asg.finance.dto.GlAgeingMasterDto;
import com.asg.finance.dto.GlAgeingMasterResponseDto;
import com.asg.finance.entity.GlAgeingMasterDtlEntity;
import com.asg.finance.entity.GlAgeingMasterEntity;
import com.asg.finance.repository.GlAgeingMasterDtlRepository;
import com.asg.finance.repository.GlAgeingMasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlAgeingMasterServiceTest {

    @Mock
    private GlAgeingMasterRepository ageingMasterRepository;

    @Mock
    private GlAgeingMasterDtlRepository ageingMasterDtlRepository;

    @InjectMocks
    private GlAgeingMasterServiceImpl ageingMasterService;

    private GlAgeingMasterEntity testMasterEntity;
    private GlAgeingMasterDto testMasterDto;

    @BeforeEach
    void setUp() {
        // Set up test data
        testMasterEntity = new GlAgeingMasterEntity();
        testMasterEntity.setAgeingPoid(1L);
        testMasterEntity.setDescription("Test Ageing");
        testMasterEntity.setActive("Y");
        testMasterEntity.setGroupPoid(1L);
        testMasterEntity.setDescription2("Test Description");
        testMasterEntity.setAgeingBreakupType("MONTHLY");
        testMasterEntity.setSeqno(1);

        testMasterDto = new GlAgeingMasterDto();
        testMasterDto.setDescription("Test Ageing");
        testMasterDto.setActive(true);
        testMasterDto.setGroupPoid(1L);
        testMasterDto.setDescription2("Test Description");
        testMasterDto.setAgeingBreakupType("MONTHLY");
        testMasterDto.setSeqno(1);

        GlAgeingMasterDtlDto detailDto = new GlAgeingMasterDtlDto();
        detailDto.setBreakupTitle("0-30 Days");
        detailDto.setBreakupFrom(0);
        detailDto.setBreakupTo(30);
        testMasterDto.setAgeingDetails(Arrays.asList(detailDto));
    }

    @Test
    void testWithMockedStatic() {
        try (MockedStatic<ASGHelperUtils> utilities = Mockito.mockStatic(ASGHelperUtils.class)) {
            utilities.when(ASGHelperUtils::getCurrentUser).thenReturn("testuser");

            // Your test code here
            when(ageingMasterRepository.existsByDescription(anyString())).thenReturn(false);
            when(ageingMasterRepository.save(any(GlAgeingMasterEntity.class))).thenReturn(testMasterEntity);
            when(ageingMasterDtlRepository.saveAll(anyList())).thenReturn(Arrays.asList(new GlAgeingMasterDtlEntity()));

            // Act
            GlAgeingMasterResponseDto response = ageingMasterService.createAgeingMaster(testMasterDto);

            // Assert
            assertNotNull(response);
            assertEquals("success", response.getStatus());
        }
    }

    @Test
    void createAgeingMaster_Success() {
        try (MockedStatic<ASGHelperUtils> utilities = Mockito.mockStatic(ASGHelperUtils.class)) {
            // Arrange
            utilities.when(ASGHelperUtils::getCurrentUser).thenReturn("testuser");
            when(ageingMasterRepository.existsByDescription(anyString())).thenReturn(false);
            when(ageingMasterRepository.save(any(GlAgeingMasterEntity.class))).thenReturn(testMasterEntity);
            when(ageingMasterDtlRepository.saveAll(anyList())).thenReturn(Arrays.asList(new GlAgeingMasterDtlEntity()));

            // Act
            GlAgeingMasterResponseDto response = ageingMasterService.createAgeingMaster(testMasterDto);

            // Assert
            assertNotNull(response);
            assertEquals("success", response.getStatus());
            verify(ageingMasterRepository, times(1)).save(any(GlAgeingMasterEntity.class));
            verify(ageingMasterDtlRepository, times(1)).saveAll(anyList());
        }
    }

    @Test
    void createAgeingMaster_DuplicateDescription_ThrowsException() {
        try (MockedStatic<ASGHelperUtils> utilities = Mockito.mockStatic(ASGHelperUtils.class)) {
            // Arrange
            utilities.when(ASGHelperUtils::getCurrentUser).thenReturn("testuser");
            when(ageingMasterRepository.existsByDescription(anyString())).thenReturn(true);

            // Act & Assert
            assertThrows(RuntimeException.class, () -> {
                ageingMasterService.createAgeingMaster(testMasterDto);
            });
        }
    }

    @Test
    void fetchAgeingMaster_Success() {
        try (MockedStatic<ASGHelperUtils> utilities = Mockito.mockStatic(ASGHelperUtils.class)) {
            // Arrange
            utilities.when(ASGHelperUtils::getCurrentUser).thenReturn("testuser");
            when(ageingMasterRepository.findByAgeingPoid(anyLong())).thenReturn(testMasterEntity);
            when(ageingMasterDtlRepository.findByAgeingMaster_AgeingPoid(anyLong()))
                    .thenReturn(Arrays.asList(new GlAgeingMasterDtlEntity()));

            // Act
            GlAgeingMasterDto result = ageingMasterService.fetchAgeingMaster(1L);

            // Assert
            assertNotNull(result);
            assertEquals("Test Ageing", result.getDescription());
            verify(ageingMasterRepository, times(1)).findByAgeingPoid(1L);
        }
    }

    @Test
    void fetchAgeingMaster_NotFound_ThrowsException() {
        try (MockedStatic<ASGHelperUtils> utilities = Mockito.mockStatic(ASGHelperUtils.class)) {
            // Arrange
            utilities.when(ASGHelperUtils::getCurrentUser).thenReturn("testuser");
            when(ageingMasterRepository.findByAgeingPoid(anyLong())).thenReturn(null);

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> {
                ageingMasterService.fetchAgeingMaster(999L);
            });
        }
    }

    @Test
    void updateAgeingMaster_Success() {
        try (MockedStatic<ASGHelperUtils> utilities = Mockito.mockStatic(ASGHelperUtils.class)) {
            // Arrange
            utilities.when(ASGHelperUtils::getCurrentUser).thenReturn("testuser");
            when(ageingMasterRepository.findByAgeingPoid(anyLong())).thenReturn(testMasterEntity);
            when(ageingMasterRepository.existsByDescriptionAndAgeingPoidNot(anyString(), anyLong())).thenReturn(false);
            when(ageingMasterRepository.save(any(GlAgeingMasterEntity.class))).thenReturn(testMasterEntity);
            when(ageingMasterDtlRepository.findByAgeingMaster_AgeingPoid(anyLong()))
                    .thenReturn(Arrays.asList(new GlAgeingMasterDtlEntity()));

            // Act
            GlAgeingMasterDto result = ageingMasterService.updateAgeingMaster(1L, testMasterDto);

            // Assert
            assertNotNull(result);
            verify(ageingMasterRepository, times(1)).save(any(GlAgeingMasterEntity.class));
        }
    }

    @Test
    void updateAgeingMaster_NotFound_ThrowsException() {
        try (MockedStatic<ASGHelperUtils> utilities = Mockito.mockStatic(ASGHelperUtils.class)) {
            // Arrange
            utilities.when(ASGHelperUtils::getCurrentUser).thenReturn("testuser");
            when(ageingMasterRepository.findByAgeingPoid(anyLong())).thenReturn(null);

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> {
                ageingMasterService.updateAgeingMaster(999L, testMasterDto);
            });
        }
    }
}

