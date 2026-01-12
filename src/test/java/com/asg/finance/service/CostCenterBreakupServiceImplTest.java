package com.asg.finance.service;

import com.asg.finance.dto.CostCenterBreakupRequestDto;
import com.asg.finance.dto.CostCenterBreakupResponseDto;
import com.asg.finance.dto.GlVoucherCostCenterBreakupResponseDto;
import com.asg.finance.repository.CostCenterBreakupDtlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CostCenterBreakupServiceImplTest {

    @Mock
    private CostCenterBreakupDtlRepository costCenterRepository;

    @InjectMocks
    private CostCenterBreakupServiceImpl costCenterBreakupService;

    private CostCenterBreakupRequestDto requestDto;
    private GlVoucherCostCenterBreakupResponseDto responseDto;
    private List<CostCenterBreakupRequestDto> requestList;

    @BeforeEach
    void setUp() {
        requestDto = CostCenterBreakupRequestDto.builder()
                .groupPoid(1L)
                .companyPoid(2L)
                .docId("DOC001")
                .transactionPoid(3L)
                .mainDetRowId(4L)
                .glPoid(5L)
                .costDetRowId(6L)
                .costGroup("ADMIN")
                .costPoid("7")
                .amount(BigDecimal.valueOf(1000))
                .loginUserPoid(8L)
                .build();

        requestList = new ArrayList<>();
        requestList.add(requestDto);

        responseDto = new GlVoucherCostCenterBreakupResponseDto();
        List<CostCenterBreakupResponseDto> breakupList = new ArrayList<>();
        breakupList.add(CostCenterBreakupResponseDto.builder()
                .mainDetRowId(4L)
                .glPoid(5L)
                .costDetRowId(6L)
                .costGroup("ADMIN")
                .costPoid("7")
                .amount(1000L)
                .description("Admin Department")
                .build());
        responseDto.setCostBreakupList(breakupList);
    }

    @Test
    void loadCostCenterData_ShouldReturnResponseDto_WhenValidParameters() {
        when(costCenterRepository.loadCostCenters(1L, 2L, "DOC001", 3L))
                .thenReturn(responseDto);
        GlVoucherCostCenterBreakupResponseDto result = costCenterBreakupService
                .loadCostCenterData("DOC001", 3L, 1L, 2L, 8L);
        assertNotNull(result);
        assertEquals(responseDto, result);
        assertNotNull(result.getCostBreakupList());
        assertEquals(1, result.getCostBreakupList().size());
        verify(costCenterRepository, times(1)).loadCostCenters(1L, 2L, "DOC001", 3L);
    }

    @Test
    void loadCostCenterData_ShouldReturnNull_WhenRepositoryReturnsNull() {
        when(costCenterRepository.loadCostCenters(1L, 2L, "DOC001", 3L))
                .thenReturn(null);
        GlVoucherCostCenterBreakupResponseDto result = costCenterBreakupService
                .loadCostCenterData("DOC001", 3L, 1L, 2L, 8L);
        assertNull(result);
        verify(costCenterRepository, times(1)).loadCostCenters(1L, 2L, "DOC001", 3L);
    }

    @Test
    void deleteCostCenterData_ShouldCallRepositoryDelete_WhenValidParameters() {
        doNothing().when(costCenterRepository).deleteCostCenters(1L, 2L, "DOC001", 3L, 8L);
        costCenterBreakupService.deleteCostCenterData("DOC001", 3L, 1L, 2L, 8L);
        verify(costCenterRepository, times(1)).deleteCostCenters(1L, 2L, "DOC001", 3L, 8L);
    }

    @Test
    void saveCostCenterBreakups_ShouldSaveSuccessfully_WhenValidRequest() {
        doNothing().when(costCenterRepository).insertCostBreakup(requestList);
        assertDoesNotThrow(() -> costCenterBreakupService.saveCostCenterBreakups(requestList));
        verify(costCenterRepository, times(1)).insertCostBreakup(requestList);
    }

    @Test
    void saveCostCenterBreakups_ShouldThrowException_WhenRequestIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> costCenterBreakupService.saveCostCenterBreakups(null));

        assertEquals("No cost center breakup entries provided", exception.getMessage());
        verify(costCenterRepository, never()).insertCostBreakup(any());
    }

    @Test
    void saveCostCenterBreakups_ShouldThrowException_WhenRequestIsEmpty() {
        List<CostCenterBreakupRequestDto> emptyList = new ArrayList<>();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> costCenterBreakupService.saveCostCenterBreakups(emptyList));

        assertEquals("No cost center breakup entries provided", exception.getMessage());
        verify(costCenterRepository, never()).insertCostBreakup(any());
    }

    @Test
    void saveCostCenterBreakups_ShouldThrowException_WhenGroupPoidIsNull() {
        requestDto.setGroupPoid(null);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> costCenterBreakupService.saveCostCenterBreakups(requestList));

        assertEquals("Missing mandatory fields in breakup data", exception.getMessage());
        verify(costCenterRepository, never()).insertCostBreakup(any());
    }

    @Test
    void saveCostCenterBreakups_ShouldThrowException_WhenCompanyPoidIsNull() {
        requestDto.setCompanyPoid(null);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> costCenterBreakupService.saveCostCenterBreakups(requestList));

        assertEquals("Missing mandatory fields in breakup data", exception.getMessage());
        verify(costCenterRepository, never()).insertCostBreakup(any());
    }

    @Test
    void saveCostCenterBreakups_ShouldThrowException_WhenDocIdIsNull() {
        requestDto.setDocId(null);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> costCenterBreakupService.saveCostCenterBreakups(requestList));

        assertEquals("Missing mandatory fields in breakup data", exception.getMessage());
        verify(costCenterRepository, never()).insertCostBreakup(any());
    }

    @Test
    void saveCostCenterBreakups_ShouldThrowException_WhenTransactionPoidIsNull() {
        requestDto.setTransactionPoid(null);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> costCenterBreakupService.saveCostCenterBreakups(requestList));

        assertEquals("Missing mandatory fields in breakup data", exception.getMessage());
        verify(costCenterRepository, never()).insertCostBreakup(any());
    }

    @Test
    void updateCostCenterBreakups_ShouldUpdateSuccessfully_WhenValidRequest() {
        doNothing().when(costCenterRepository).deleteCostCenters(1L, 2L, "DOC001", 3L, 8L);
        doNothing().when(costCenterRepository).insertCostBreakup(requestList);
        assertDoesNotThrow(() -> costCenterBreakupService.updateCostCenterBreakups(requestList, 8L));
        verify(costCenterRepository, times(1)).deleteCostCenters(1L, 2L, "DOC001", 3L, 8L);
        verify(costCenterRepository, times(1)).insertCostBreakup(requestList);
    }

    @Test
    void updateCostCenterBreakups_ShouldThrowException_WhenRequestIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> costCenterBreakupService.updateCostCenterBreakups(null, 8L));

        assertEquals("No cost center breakup entries provided", exception.getMessage());
        verify(costCenterRepository, never()).deleteCostCenters(any(), any(), any(), any(), any());
        verify(costCenterRepository, never()).insertCostBreakup(any());
    }

    @Test
    void updateCostCenterBreakups_ShouldThrowException_WhenRequestIsEmpty() {
        List<CostCenterBreakupRequestDto> emptyList = new ArrayList<>();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> costCenterBreakupService.updateCostCenterBreakups(emptyList, 8L));

        assertEquals("No cost center breakup entries provided", exception.getMessage());
        verify(costCenterRepository, never()).deleteCostCenters(any(), any(), any(), any(), any());
        verify(costCenterRepository, never()).insertCostBreakup(any());
    }

    @Test
    void updateCostCenterBreakups_ShouldThrowException_WhenMandatoryFieldsAreMissing() {
        requestDto.setGroupPoid(null);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> costCenterBreakupService.updateCostCenterBreakups(requestList, 8L));

        assertEquals("Missing mandatory fields in breakup data", exception.getMessage());
        verify(costCenterRepository, never()).deleteCostCenters(any(), any(), any(), any(), any());
        verify(costCenterRepository, never()).insertCostBreakup(any());
    }

    @Test
    void updateCostCenterBreakups_ShouldHandleMultipleEntries_WhenValidRequest() {
        CostCenterBreakupRequestDto secondRequest = CostCenterBreakupRequestDto.builder()
                .groupPoid(1L)
                .companyPoid(2L)
                .docId("DOC001")
                .transactionPoid(3L)
                .mainDetRowId(9L)
                .glPoid(10L)
                .costDetRowId(11L)
                .costGroup("HR")
                .costPoid("12")
                .amount(BigDecimal.valueOf(2000))
                .loginUserPoid(8L)
                .build();

        List<CostCenterBreakupRequestDto> multipleRequests = new ArrayList<>();
        multipleRequests.add(requestDto);
        multipleRequests.add(secondRequest);

        doNothing().when(costCenterRepository).deleteCostCenters(1L, 2L, "DOC001", 3L, 8L);
        doNothing().when(costCenterRepository).insertCostBreakup(multipleRequests);

        assertDoesNotThrow(() -> costCenterBreakupService.updateCostCenterBreakups(multipleRequests, 8L));

        verify(costCenterRepository, times(1)).deleteCostCenters(1L, 2L, "DOC001", 3L, 8L);
        verify(costCenterRepository, times(1)).insertCostBreakup(multipleRequests);
    }
}