package com.asg.finance.service;

import com.asg.common.lib.dto.request.BillwiseBreakupRequestDto;
import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.GlVoucherPendingBillwiseBreakupResponseDto;
import com.asg.finance.repository.BillwiseBreakupDtlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillwiseBreakupServiceImplTest {

    @Mock
    private BillwiseBreakupDtlRepository billwiseBreakupDtlRepository;

    @InjectMocks
    private BillwiseBreakupServiceImpl billwiseBreakupService;

    private BillwiseBreakupRequestDto requestDto;
    private GlVoucherLoadBillwiseBreakupResponseDto loadResponse;
    private GlVoucherPendingBillwiseBreakupResponseDto pendingResponse;

    @BeforeEach
    void setUp() {
        requestDto = createSampleRequestDto();
        loadResponse = new GlVoucherLoadBillwiseBreakupResponseDto();
        pendingResponse = new GlVoucherPendingBillwiseBreakupResponseDto();
    }

    @Test
    void loadBillwiseBreakup_Success() {
        when(billwiseBreakupDtlRepository.loadBillwiseBreakup(any(), any(), any(), any()))
                .thenReturn(loadResponse);

        GlVoucherLoadBillwiseBreakupResponseDto result = billwiseBreakupService.loadBillwiseBreakup(
                1L, 1L, "DOC001", 1L);

        assertNotNull(result);
        assertEquals(loadResponse, result);
        verify(billwiseBreakupDtlRepository).loadBillwiseBreakup(1L, 1L, "DOC001", 1L);
    }

    @Test
    void showPendingBillwiseBreakup_Success() {
        Date asOnDate = new Date();
        when(billwiseBreakupDtlRepository.showPendingBillwiseBreakup(any(), any(), any(), any()))
                .thenReturn(pendingResponse);

        GlVoucherPendingBillwiseBreakupResponseDto result = billwiseBreakupService.showPendingBillwiseBreakup(
                1L, 1L, 1L, asOnDate);

        assertNotNull(result);
        assertEquals(pendingResponse, result);
        verify(billwiseBreakupDtlRepository).showPendingBillwiseBreakup(1L, 1L, 1L, asOnDate);
    }

    @Test
    void insertBillwiseBreakup_Success() {
        List<BillwiseBreakupRequestDto> breakupList = List.of(requestDto);

        billwiseBreakupService.insertBillwiseBreakup(breakupList);

        verify(billwiseBreakupDtlRepository).insertBillwiseBreakup(breakupList);
    }

    @Test
    void insertBillwiseBreakup_NullList_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> billwiseBreakupService.insertBillwiseBreakup(null));

        assertEquals("No billwise breakup entries provided", exception.getMessage());
        verify(billwiseBreakupDtlRepository, never()).insertBillwiseBreakup(any());
    }

    @Test
    void insertBillwiseBreakup_EmptyList_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> billwiseBreakupService.insertBillwiseBreakup(Collections.emptyList()));

        assertEquals("No billwise breakup entries provided", exception.getMessage());
        verify(billwiseBreakupDtlRepository, never()).insertBillwiseBreakup(any());
    }

    @Test
    void insertBillwiseBreakup_MissingGroupPoid_ThrowsException() {
        requestDto.setGroupPoid(null);
        List<BillwiseBreakupRequestDto> breakupList = List.of(requestDto);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> billwiseBreakupService.insertBillwiseBreakup(breakupList));

        assertEquals("Missing mandatory fields in billwise breakup data", exception.getMessage());
        verify(billwiseBreakupDtlRepository, never()).insertBillwiseBreakup(any());
    }

    @Test
    void insertBillwiseBreakup_MissingCompanyPoid_ThrowsException() {
        requestDto.setCompanyPoid(null);
        List<BillwiseBreakupRequestDto> breakupList = List.of(requestDto);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> billwiseBreakupService.insertBillwiseBreakup(breakupList));

        assertEquals("Missing mandatory fields in billwise breakup data", exception.getMessage());
        verify(billwiseBreakupDtlRepository, never()).insertBillwiseBreakup(any());
    }

    @Test
    void insertBillwiseBreakup_MissingDocId_ThrowsException() {
        requestDto.setDocId(null);
        List<BillwiseBreakupRequestDto> breakupList = List.of(requestDto);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> billwiseBreakupService.insertBillwiseBreakup(breakupList));

        assertEquals("Missing mandatory fields in billwise breakup data", exception.getMessage());
        verify(billwiseBreakupDtlRepository, never()).insertBillwiseBreakup(any());
    }

    @Test
    void insertBillwiseBreakup_MissingTransactionPoid_ThrowsException() {
        requestDto.setTransactionPoid(null);
        List<BillwiseBreakupRequestDto> breakupList = List.of(requestDto);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> billwiseBreakupService.insertBillwiseBreakup(breakupList));

        assertEquals("Missing mandatory fields in billwise breakup data", exception.getMessage());
        verify(billwiseBreakupDtlRepository, never()).insertBillwiseBreakup(any());
    }

    @Test
    void deleteBillwiseBreakup_Success() {
        billwiseBreakupService.deleteBillwiseBreakup(1L, 1L, "DOC001", 1L, 1L);

        verify(billwiseBreakupDtlRepository).deleteBillwiseBreakup(1L, 1L, "DOC001", 1L, 1L);
    }

    @Test
    void updateBillwiseBreakups_Success() {
        List<BillwiseBreakupRequestDto> request = List.of(requestDto);

        billwiseBreakupService.updateBillwiseBreakups(request, 1L);

        verify(billwiseBreakupDtlRepository).deleteBillwiseBreakup(
                requestDto.getGroupPoid(),
                requestDto.getCompanyPoid(),
                requestDto.getDocId(),
                requestDto.getTransactionPoid(),
                requestDto.getLoginUserPoid()
        );
        verify(billwiseBreakupDtlRepository).insertBillwiseBreakup(request);
    }

    @Test
    void updateBillwiseBreakups_NullList_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> billwiseBreakupService.updateBillwiseBreakups(null, 1L));

        assertEquals("No cost center breakup entries provided", exception.getMessage());
        verify(billwiseBreakupDtlRepository, never()).deleteBillwiseBreakup(any(), any(), any(), any(), any());
        verify(billwiseBreakupDtlRepository, never()).insertBillwiseBreakup(any());
    }

    @Test
    void updateBillwiseBreakups_EmptyList_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> billwiseBreakupService.updateBillwiseBreakups(Collections.emptyList(), 1L));

        assertEquals("No cost center breakup entries provided", exception.getMessage());
        verify(billwiseBreakupDtlRepository, never()).deleteBillwiseBreakup(any(), any(), any(), any(), any());
        verify(billwiseBreakupDtlRepository, never()).insertBillwiseBreakup(any());
    }

    @Test
    void updateBillwiseBreakups_MissingMandatoryFields_ThrowsException() {
        requestDto.setGroupPoid(null);
        List<BillwiseBreakupRequestDto> request = List.of(requestDto);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> billwiseBreakupService.updateBillwiseBreakups(request, 1L));

        assertEquals("Missing mandatory fields in breakup data", exception.getMessage());
        verify(billwiseBreakupDtlRepository, never()).deleteBillwiseBreakup(any(), any(), any(), any(), any());
        verify(billwiseBreakupDtlRepository, never()).insertBillwiseBreakup(any());
    }

    private BillwiseBreakupRequestDto createSampleRequestDto() {
        BillwiseBreakupRequestDto dto = new BillwiseBreakupRequestDto();
        dto.setGroupPoid(1L);
        dto.setCompanyPoid(1L);
        dto.setDocId("DOC001");
        dto.setTransactionPoid(1L);
        dto.setLoginUserPoid(1L);
        return dto;
    }
}
