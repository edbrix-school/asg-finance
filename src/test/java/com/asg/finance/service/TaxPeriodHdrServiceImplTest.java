package com.asg.finance.service;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GlobalTaxPeriodChargeDtlEntity;
import com.asg.finance.entity.TaxPeriodHdr;
import com.asg.finance.repository.GlobalTaxPeriodChargeDtlRepository;
import com.asg.finance.repository.GlobalTaxPeriodStockDtlRepository;
import com.asg.finance.repository.TaxPeriodHdrRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxPeriodHdrServiceImplTest {

    @Mock
    private TaxPeriodHdrRepository taxPeriodHdrRepository;

    @Mock
    private GlobalTaxPeriodChargeDtlRepository globalTaxPeriodChargeDtlRepository;

    @Mock
    private GlobalTaxPeriodStockDtlRepository globalTaxPeriodStockDtlRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private com.asg.finance.repository.master.ShipChargeRepository shipChargeRepository;

    @Mock
    private com.asg.finance.repository.master.ShipChargeGroupRepository shipChargeGroupRepository;

    @Mock
    private com.asg.finance.repository.TaxMasterRepository taxMasterRepository;

    /*@Mock
    private com.asg.repository.master.StockMasterRepository stockMasterRepository;*/

    @Mock
    private com.asg.finance.repository.master.StockCategoryMasterRepository stockCategoryMasterRepository;

    @InjectMocks
    private TaxPeriodHdrServiceImpl taxPeriodHdrService;

    private TaxPeriodHdrRequestDto requestDto;
    private TaxPeriodHdr taxPeriodHdr;
    private TaxPeriodChargeDtlRequestDto chargeDto;
    private TaxPeriodStockDtlRequestDto stockDto;

    @BeforeEach
    void setUp() {
        chargeDto = TaxPeriodChargeDtlRequestDto.builder()
                .chargePoid(1L)
                .chargeCatPoid(1L)
                .outputTaxPoid(1L)
                .inputTaxPoid(1L)
                .detRowId(1L)
                .remarks("Test charge")
                .build();

        stockDto = TaxPeriodStockDtlRequestDto.builder()
                .stockPoid(1L)
                .stockCatPoid(1L)
                .outputTaxPoid(1L)
                .inputTaxPoid(1L)
                .detRowId(1L)
                .remarks("Test stock")
                .build();

        requestDto = TaxPeriodHdrRequestDto.builder()
                .description("Test Tax Period")
                .periodFrom(LocalDate.now())
                .periodTo(LocalDate.now().plusDays(30))
                .groupPoid(1L)
                .companyPoid(1L)
                .charges(Arrays.asList(chargeDto))
                .stocks(Arrays.asList(stockDto))
                .build();

        taxPeriodHdr = TaxPeriodHdr.builder()
                .transactionPoid(1L)
                .description("Test Tax Period")
                .periodFrom(LocalDate.now())
                .periodTo(LocalDate.now().plusDays(30))
                .groupPoid(1L)
                .companyPoid(1L)
                .docRef("DOC001")
                .deleted("N")
                .createdBy("TEST_USER")
                .createdDate(LocalDateTime.now())
                .build();
    }

    @Test
    void createTaxPeriodHdr_Success() {
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserId).thenReturn("1");
            when(taxPeriodHdrRepository.save(any(TaxPeriodHdr.class))).thenReturn(taxPeriodHdr);
            when(globalTaxPeriodChargeDtlRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
            when(globalTaxPeriodStockDtlRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

            TaxPeriodHdrResponseDto result = taxPeriodHdrService.createTaxPeriodHdr(requestDto);

            assertNotNull(result);
            assertEquals(taxPeriodHdr.getTransactionPoid(), result.getTransactionPoid());
            assertEquals(taxPeriodHdr.getDescription(), result.getDescription());
            verify(taxPeriodHdrRepository).save(any(TaxPeriodHdr.class));
            verify(globalTaxPeriodChargeDtlRepository).saveAll(anyList());
            verify(globalTaxPeriodStockDtlRepository).saveAll(anyList());
        }
    }

    @Test
    void createTaxPeriodHdr_WithoutChargesAndStocks() {
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserId).thenReturn("1");
            requestDto.setCharges(null);
            requestDto.setStocks(null);
            when(taxPeriodHdrRepository.save(any(TaxPeriodHdr.class))).thenReturn(taxPeriodHdr);

            TaxPeriodHdrResponseDto result = taxPeriodHdrService.createTaxPeriodHdr(requestDto);

            assertNotNull(result);
            verify(taxPeriodHdrRepository).save(any(TaxPeriodHdr.class));
            verify(globalTaxPeriodChargeDtlRepository, never()).saveAll(anyList());
            verify(globalTaxPeriodStockDtlRepository, never()).saveAll(anyList());
        }
    }


    @Test
    void updateTaxPeriodHdr_NotFound() {
        when(taxPeriodHdrRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taxPeriodHdrService.updateTaxPeriodHdr(1L, requestDto)
        );

        assertTrue(exception.getMessage().contains("Tax Period not found for id:"));
        verify(taxPeriodHdrRepository).findById(1L);
        verify(taxPeriodHdrRepository, never()).save(any(TaxPeriodHdr.class));
    }

    @Test
    void getTaxPeriodHdrById_Success() {
        when(taxPeriodHdrRepository.findById(1L)).thenReturn(Optional.of(taxPeriodHdr));

        TaxPeriodHdrResponseDto result = taxPeriodHdrService.getTaxPeriodHdrById(1L);

        assertNotNull(result);
        assertEquals(taxPeriodHdr.getTransactionPoid(), result.getTransactionPoid());
        assertEquals(taxPeriodHdr.getDescription(), result.getDescription());
        verify(taxPeriodHdrRepository).findById(1L);
    }

    @Test
    void getTaxPeriodHdrById_NotFound() {
        when(taxPeriodHdrRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taxPeriodHdrService.getTaxPeriodHdrById(1L)
        );

        assertTrue(exception.getMessage().contains("Tax Period not found for id:"));
        verify(taxPeriodHdrRepository).findById(1L);
    }

    @Test
    void softDeleteTaxPeriodHdr_Success() {
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserId).thenReturn("1");
            when(taxPeriodHdrRepository.findById(1L)).thenReturn(Optional.of(taxPeriodHdr));
            when(taxPeriodHdrRepository.save(any(TaxPeriodHdr.class))).thenReturn(taxPeriodHdr);

            taxPeriodHdrService.softDeleteTaxPeriodHdr(1L);

            verify(taxPeriodHdrRepository).findById(1L);
            verify(taxPeriodHdrRepository).save(argThat(entity -> "Y".equals(entity.getDeleted())));
        }
    }

    @Test
    void softDeleteTaxPeriodHdr_NotFound() {
        when(taxPeriodHdrRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taxPeriodHdrService.softDeleteTaxPeriodHdr(1L)
        );

        assertTrue(exception.getMessage().contains("Tax Period not found with ID:"));
        verify(taxPeriodHdrRepository).findById(1L);
        verify(taxPeriodHdrRepository, never()).save(any(TaxPeriodHdr.class));
    }

    @Test
    void getTaxPeriodCharges_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        GlobalTaxPeriodChargeDtlEntity chargeEntity = GlobalTaxPeriodChargeDtlEntity.builder()
                .transactionPoid(1L)
                .chargePoid(1L)
                .build();
        Page<GlobalTaxPeriodChargeDtlEntity> chargePage = new PageImpl<>(Arrays.asList(chargeEntity));

        when(globalTaxPeriodChargeDtlRepository.findByTransactionPoid(1L, pageable)).thenReturn(chargePage);
        when(shipChargeRepository.findByChargePoidIn(anySet())).thenReturn(Collections.emptyList());
        when(taxMasterRepository.findByTaxPoidIn(anySet())).thenReturn(Collections.emptyList());

        Page<TaxPeriodChargeDtlResponseDto> result = taxPeriodHdrService.getTaxPeriodCharges(1L, pageable);

        assertNotNull(result);
        verify(globalTaxPeriodChargeDtlRepository).findByTransactionPoid(1L, pageable);
    }

    @Test
    void validatePeriodDates_InvalidDates() {
        requestDto.setPeriodFrom(LocalDate.now().plusDays(30));
        requestDto.setPeriodTo(LocalDate.now());

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> taxPeriodHdrService.createTaxPeriodHdr(requestDto)
        );

        assertTrue(exception.getMessage().contains("Period From must not be after Period To"));
    }
}
