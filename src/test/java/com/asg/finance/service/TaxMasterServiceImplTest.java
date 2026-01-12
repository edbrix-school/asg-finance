package com.asg.finance.service;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.dto.TaxMasterRequestDTO;
import com.asg.finance.dto.TaxMasterResponseDTO;
import com.asg.finance.entity.GLMaster;
import com.asg.finance.entity.TaxMaster;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.finance.repository.TaxMasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxMasterServiceImplTest {

    @Mock
    private TaxMasterRepository repository;

    @Mock
    private GLMasterRepository glMasterRepository;

    @InjectMocks
    private TaxMasterServiceImpl service;

    private TaxMasterRequestDTO requestDTO;
    private TaxMaster taxMaster;
    private GLMaster glMaster;

    @BeforeEach
    void setup() {
        requestDTO = new TaxMasterRequestDTO();
        requestDTO.setTaxCode("TAX001");
        requestDTO.setTaxName("VAT");
        requestDTO.setTaxName2("VAT Second");
        requestDTO.setPercentage(5.0);
        requestDTO.setTaxType("INPUT_VAT");
        requestDTO.setGlType("DR");
        requestDTO.setGlLedgerPoid(100L);
        requestDTO.setTaxCategory("OS");
        requestDTO.setActive("Y");
        requestDTO.setSeqNo(1);
        requestDTO.setGroupPoid(1L);

        taxMaster = TaxMaster.builder()
                .taxPoid(1L)
                .taxCode("TAX001")
                .taxName("VAT")
                .percentage(5.0)
                .taxType("INPUT_VAT")
                .glType("DR")
                .glLedgerPoid(100L)
                .taxCategory("STANDARD")
                .active("Y")
                .seqNo(1)
                .deleted("N")
                .groupPoid(1L)
                .build();

        glMaster = new GLMaster();
        glMaster.setGlPoid(100L);
        glMaster.setGlCode("GL001");
        glMaster.setGlDescription("GL Description");
    }

    @Test
    void createTaxMaster_shouldCreateSuccessfully() {

        when(repository.existsByTaxCode("TAX001")).thenReturn(false);
        when(repository.save(any())).thenReturn(taxMaster);
        when(glMasterRepository.findByGlPoid(100L)).thenReturn(Optional.of(glMaster)); // 👈 Important fix

        TaxMasterResponseDTO result = service.createTaxMaster(requestDTO);

        assertNotNull(result);
        assertEquals("TAX001", result.getTaxCode());
        assertEquals("GL001", result.getGlLedger().getCode());
        verify(repository).save(any());
        verify(glMasterRepository).findByGlPoid(100L);
    }


    @Test
    void createTaxMaster_shouldThrow_whenTaxCodeExists() {
        when(repository.existsByTaxCode("TAX001")).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.createTaxMaster(requestDTO));
    }

    @Test
    void createTaxMaster_shouldThrow_whenInvalidTaxType() {
        requestDTO.setTaxType("INVALID");

        assertThrows(ValidationException.class, () -> service.createTaxMaster(requestDTO));
    }


    @Test
    void createTaxMaster_shouldThrow_whenInvalidGlType() {
        requestDTO.setGlType("INVALID");

        assertThrows(ValidationException.class, () -> service.createTaxMaster(requestDTO));
    }


    @Test
    void updateTaxMaster_shouldUpdateSuccessfully() {
        TaxMaster existing = TaxMaster.builder()
                .taxPoid(1L)
                .taxCode("OLD_CODE")
                .build();

        requestDTO.setTaxCode("NEW_CODE");

        when(repository.findByTaxPoid(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByTaxCodeAndTaxPoidNot("NEW_CODE", 1L)).thenReturn(false);
        when(glMasterRepository.findByGlPoid(100L)).thenReturn(Optional.of(glMaster));
        when(repository.save(any())).thenReturn(existing);

        TaxMasterResponseDTO result = service.updateTaxMaster(1L, requestDTO);

        assertNotNull(result);
        assertEquals("NEW_CODE", result.getTaxCode());
    }

    @Test
    void updateTaxMaster_shouldThrow_whenTaxNotFound() {
        when(repository.findByTaxPoid(1L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> service.updateTaxMaster(1L, requestDTO));
    }

    @Test
    void updateTaxMaster_shouldThrow_whenTaxCodeExists() {

        TaxMaster existing = TaxMaster.builder()
                .taxPoid(1L)
                .taxType("INPUT_VAT")
                .taxCode("DUPLICATE_CODE")
                .build();

        TaxMasterRequestDTO requestDTO = new TaxMasterRequestDTO();
        requestDTO.setTaxCode("TAX001");
        requestDTO.setTaxType("VAT");
        requestDTO.setTaxName("Standard VAT");

        when(repository.findByTaxPoid(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByTaxCodeAndTaxPoidNot("TAX001", 1L)).thenReturn(true);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.updateTaxMaster(1L, requestDTO)
        );

        assertEquals("Tax Code already exists: TAX001", exception.getMessage());
    }

    @Test
    void getTaxMasterById_shouldReturnDTO() {
        when(repository.existsByTaxPoid(1L)).thenReturn(true);
        when(repository.findByTaxPoid(1L)).thenReturn(Optional.of(taxMaster));
        when(glMasterRepository.findByGlPoid(100L)).thenReturn(Optional.of(glMaster));

        TaxMasterResponseDTO result = service.getTaxMasterById(1L);

        assertNotNull(result);
        assertEquals("TAX001", result.getTaxCode());
        assertEquals("GL001", result.getGlLedger().getCode());
    }

    @Test
    void getTaxMasterById_shouldThrow_whenTaxNotFound() {
        when(repository.existsByTaxPoid(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.getTaxMasterById(1L));
    }

    @Test
    void getTaxMasterById_shouldThrow_whenGLNotFound() {
        when(repository.existsByTaxPoid(1L)).thenReturn(true);
        when(repository.findByTaxPoid(1L)).thenReturn(Optional.of(taxMaster));
        when(glMasterRepository.findByGlPoid(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getTaxMasterById(1L));
    }

    @Test
    void softDeleteTaxMaster_shouldMarkAsDeleted() {
        when(repository.findByTaxPoid(1L)).thenReturn(Optional.of(taxMaster));

        service.softDeleteTaxMaster(1L);

        assertEquals("Y", taxMaster.getDeleted());
        assertEquals("N", taxMaster.getActive());
        verify(repository).save(taxMaster);
    }

    @Test
    void softDeleteTaxMaster_shouldThrow_whenTaxNotFound() {
        when(repository.findByTaxPoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.softDeleteTaxMaster(1L));
    }

}