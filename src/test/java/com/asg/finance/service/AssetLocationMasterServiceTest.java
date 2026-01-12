package com.asg.finance.service;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.finance.dto.AssetLocationMasterRequestDto;
import com.asg.finance.dto.AssetLocationMasterResponseDto;
import com.asg.finance.entity.AssetLocation;
import com.asg.finance.repository.AssetLocationMasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetLocationMasterServiceTest {

    @Mock
    private AssetLocationMasterRepository repository;
    @InjectMocks
    private AssetLocationMasterServiceImpl service;

    private AssetLocationMasterRequestDto requestDto;
    private AssetLocation entity;

    @BeforeEach
    void setUp() {
        requestDto = AssetLocationMasterRequestDto.builder()
                .locationCode("LOC001")
                .description("Main Warehouse")
                .seqNo(1)
                .groupPoid(1L)
                .active("Y")
                .build();

        entity = new AssetLocation();
        entity.setLocationPoid(1L);
        entity.setLocationCode("LOC001");
        entity.setDescription("Main Warehouse");
        entity.setSeqNo(1);
        entity.setGroupPoid(1L);
        entity.setDeleted("N");
        entity.setActive("Y");
    }


    @Test
    void createAssetLocation_ShouldSaveSuccessfully() {
        when(repository.existsByLocationCode("LOC001")).thenReturn(false);
        when(repository.existsByDescription("Main Warehouse")).thenReturn(false);
        when(repository.save(any(AssetLocation.class))).thenReturn(entity);

        AssetLocationMasterResponseDto responseDto = service.createAssetLocationMaster(requestDto);

        assertEquals(1L, responseDto.getLocationPoid());
        verify(repository).save(any(AssetLocation.class));
    }


    @Test
    void createAssetLocation_ShouldThrowException_WhenDuplicateLocationCode() {
        when(repository.existsByLocationCode("LOC001")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createAssetLocationMaster(requestDto)
        );

        assertEquals("Location Code already exists", ex.getMessage());
        verify(repository, never()).save(any());
    }


    @Test
    void createAssetLocation_ShouldThrowException_WhenDuplicateDescription() {
        when(repository.existsByLocationCode("LOC001")).thenReturn(false);
        when(repository.existsByDescription("Main Warehouse")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createAssetLocationMaster(requestDto)
        );

        assertEquals("Description already exists", ex.getMessage());
        verify(repository, never()).save(any());
    }


    @Test
    void updateAssetLocation_ShouldUpdateSuccessfully() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByLocationCodeAndLocationPoidNot("LOC001", 1L)).thenReturn(false);
        when(repository.existsByDescriptionAndLocationPoidNot("Main Warehouse", 1L)).thenReturn(false);
        when(repository.save(any(AssetLocation.class))).thenReturn(entity);

        AssetLocationMasterResponseDto updated = service.updateAssetLocationMaster(1L, requestDto);

        assertNotNull(updated);
        assertEquals("LOC001", updated.getLocationCode());
        verify(repository).save(any());
    }


    @Test
    void updateAssetLocation_ShouldThrowException_WhenNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateAssetLocationMaster(1L, requestDto)
        );

        assertTrue(ex.getMessage().contains("Asset Location not found"));
        verify(repository, never()).save(any());
    }


    @Test
    void updateAssetLocation_ShouldThrowException_WhenDuplicateCode() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByLocationCodeAndLocationPoidNot("LOC001", 1L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateAssetLocationMaster(1L, requestDto)
        );

        assertEquals("Location Code already exists", ex.getMessage());
        verify(repository, never()).save(any());
    }


    @Test
    void deleteAssetLocation_ShouldSoftDeleteSuccessfully() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.softDeleteAssetLocationMaster(1L);

        assertEquals("Y", entity.getDeleted());
        assertEquals("N", entity.getActive());
        verify(repository).save(entity);
    }

    @Test
    void deleteAssetLocation_ShouldThrowException_WhenNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.softDeleteAssetLocationMaster(1L)
        );

        assertTrue(ex.getMessage().contains("Asset Location not found"));
        verify(repository, never()).save(any());
    }

    @Test
    void getAssetLocationById_ShouldReturnSuccessfully() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        AssetLocationMasterResponseDto result = service.getAssetLocationMasterById(1L);

        assertNotNull(result);
        assertEquals("LOC001", result.getLocationCode());
    }


    @Test
    void getAssetLocationById_ShouldThrowException_WhenNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.getAssetLocationMasterById(1L)
        );

        assertTrue(ex.getMessage().contains("Asset Location not found"));
    }
}
