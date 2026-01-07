//package com.asg.finance.service;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.dto.RawSearchResult;
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.service.DocumentSearchService;
//import com.asg.finance.dto.BankDepositVoucherDtlDto;
//import com.asg.finance.dto.BankDepositVoucherRequestDto;
//import com.asg.finance.dto.BankDepositVoucherResponseDto;
//import com.asg.finance.entity.GlBankDepositVoucherDtl;
//import com.asg.finance.entity.GlBankDepositVoucherHdr;
//import com.asg.finance.repository.GlBankDepositVoucherDtlRepository;
//import com.asg.finance.repository.GlBankDepositVoucherHdrRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//public class BankDepositVoucherServiceImplTest {
//
//    @Mock
//    private GlBankDepositVoucherHdrRepository hdrRepository;
//
//    @Mock
//    private GlBankDepositVoucherDtlRepository dtlRepository;
//
//    @Mock
//    private DocumentSearchService documentService;
//
//    @InjectMocks
//    private BankDepositVoucherServiceImpl service;
//
//    private BankDepositVoucherRequestDto requestDto;
//    private GlBankDepositVoucherHdr header;
//    private GlBankDepositVoucherDtl detail;
//
//    @BeforeEach
//    void setUp() {
//        header = GlBankDepositVoucherHdr.builder()
//                .transactionPoid(69664L)
//                .transactionDate(LocalDate.now())
//                .groupPoid(1L)
//                .companyPoid(1L)
//                .docRef("ASG23132")
//                .bankPoid(61L)
//                .postingNarration("Bank deposit for cheques received")
//                .remarks("Monthly cheque deposit")
//                .grandTotal(BigDecimal.valueOf(74.89))
//                .refType("CHEQUE")
//                .bankFilter("ALL")
//                .groupPosting("N")
//                .deleted("N")
//                .createdBy("SYSTEM")
//                .createdDate(LocalDateTime.now())
//                .lastModifiedBy("SYSTEM")
//                .lastModifiedDate(LocalDateTime.now())
//                .build();
//
//        detail = GlBankDepositVoucherDtl.builder()
//                .transactionPoid(69664L)
//                .detRowId(1L)
//                .bankPoid(61L)
//                .pymtType("CHEQUE")
//                .refDocPoid(2085L)
//                .refDocRef("ASGDR858626")
//                .chqAcName("BABASONS")
//                .chqAcNo("2002623964080")
//                .chqCardNo("034588")
//                .chqDate(LocalDate.of(2016, 4, 7))
//                .amount(BigDecimal.valueOf(74.89))
//                .remarks("Cheque deposit")
//                .selected("Y")
//                .chqSeqNum(1)
//                .paymentMainPoid(768393L)
//                .build();
//
//        requestDto = BankDepositVoucherRequestDto.builder()
//                .bankPoid(61L)
//                .type("CHEQUE")
//                .bankFilter("ALL")
//                .groupPosting(false)
//                .postingNarration("Bank deposit for cheques received")
//                .companyPoid(1L)
//                .groupPoid(1L)
//                .remarks("Monthly cheque deposit")
//                .details(List.of(
//                        BankDepositVoucherDtlDto.builder()
//                                .bankPoid(61L)
//                                .pymtType("CHEQUE")
//                                .refDocPoid(2085L)
//                                .refDocRef("ASGDR858626")
//                                .chqAcName("BABASONS")
//                                .chqAcNo("2002623964080")
//                                .chqCardNo("034588")
//                                .chqDate(LocalDate.of(2016, 4, 7))
//                                .amount(BigDecimal.valueOf(74.89))
//                                .remarks("Cheque deposit")
//                                .chqSeqNum(1)
//                                .paymentMainPoid(768393L)
//                                .build()
//                ))
//                .build();
//    }
//
//    @Test
//    void testGetBankDepositVoucherById_Success() {
//        when(hdrRepository.findByTransactionPoid(69664L)).thenReturn(Optional.of(header));
//        when(dtlRepository.findByTransactionPoid(69664L)).thenReturn(List.of(detail));
//
//        BankDepositVoucherResponseDto response = service.getBankDepositVoucherById(69664L);
//
//        assertNotNull(response);
//        assertEquals(69664L, response.getTransactionPoid());
//        assertEquals("ASG23132", response.getDocRef());
//        assertEquals(1, response.getDetails().size());
//        verify(hdrRepository, times(1)).findByTransactionPoid(69664L);
//    }
//
//    @Test
//    void testGetBankDepositVoucherById_NotFound() {
//        when(hdrRepository.findByTransactionPoid(69664L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> service.getBankDepositVoucherById(69664L));
//    }
//
//    @Test
//    void testSoftDeleteBankDepositVoucher_Success() {
//        when(hdrRepository.findByTransactionPoid(69664L)).thenReturn(Optional.of(header));
//        when(hdrRepository.save(any(GlBankDepositVoucherHdr.class))).thenReturn(header);
//
//        service.softDeleteBankDepositVoucher(69664L);
//
//        assertEquals("Y", header.getDeleted());
//        verify(hdrRepository, times(1)).save(header);
//    }
//
//    @Test
//    void testSoftDeleteBankDepositVoucher_NotFound() {
//        when(hdrRepository.findByTransactionPoid(69664L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> service.softDeleteBankDepositVoucher(69664L));
//    }
//
//    @Test
//    void testListBankDepositVouchers_Success() {
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", Collections.emptyList());
//        Pageable pageable = PageRequest.of(0, 10);
//
//        when(documentService.resolveOperator(any())).thenReturn("AND");
//        when(documentService.resolveIsDeleted(any())).thenReturn("N");
//        when(documentService.resolveDateFilters(any(), anyString(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
//
//        RawSearchResult rawSearchResult = new RawSearchResult(
//                List.of(Map.of("TRANSACTION_POID", 69664L, "DOC_REF", "ASG23132")),
//                Map.of("TRANSACTION_POID", "DOC_REF"),
//                1L
//        );
//
//        when(documentService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
//                .thenReturn(rawSearchResult);
//
//        Map<String, Object> result = service.listBankDepositVouchers("400-109", filters, null, null, pageable);
//
//        assertNotNull(result);
//        verify(documentService, times(1))
//                .search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString());
//    }
//
//    @Test
//    void testLoadPendingPayments_Success() {
//        List<BankDepositVoucherDtlDto> mockPayments = List.of(
//                BankDepositVoucherDtlDto.builder()
//                        .paymentMainPoid(768393L)
//                        .refDocPoid(2085L)
//                        .refDocRef("ASGDR858626")
//                        .amount(BigDecimal.valueOf(74.89))
//                        .build()
//        );
//
//        when(hdrRepository.loadPendingPayments(61L, "CHEQUE", "ALL")).thenReturn(mockPayments);
//
//        List<BankDepositVoucherDtlDto> result = service.loadPendingPayments(61L, "CHEQUE", "ALL");
//
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertEquals(768393L, result.get(0).getPaymentMainPoid());
//        verify(hdrRepository, times(1)).loadPendingPayments(61L, "CHEQUE", "ALL");
//    }
//
//    @Test
//    void testCreateBankDepositVoucher_NullGroupPoid() {
//        BankDepositVoucherRequestDto requestWithNullGroup = BankDepositVoucherRequestDto.builder()
//                .bankPoid(61L)
//                .type("CHEQUE")
//                .bankFilter("ALL")
//                .groupPosting(false)
//                .postingNarration("Test")
//                .companyPoid(1L)
//                .groupPoid(null)
//                .remarks("Test")
//                .details(List.of())
//                .build();
//
//        when(hdrRepository.save(any(GlBankDepositVoucherHdr.class))).thenReturn(header);
//        when(hdrRepository.findByTransactionPoid(anyLong())).thenReturn(Optional.of(header));
//        when(dtlRepository.findByTransactionPoid(anyLong())).thenReturn(List.of());
//
//        BankDepositVoucherResponseDto response = service.createBankDepositVoucher(requestWithNullGroup);
//
//        assertNotNull(response);
//        verify(hdrRepository, times(1)).save(any(GlBankDepositVoucherHdr.class));
//    }
//
//    @Test
//    void testCreateBankDepositVoucher_GroupPostingTrue() {
//        requestDto.setGroupPosting(true);
//
//        when(hdrRepository.save(any(GlBankDepositVoucherHdr.class))).thenReturn(header);
//        when(hdrRepository.findByTransactionPoid(anyLong())).thenReturn(Optional.of(header));
//        when(dtlRepository.findByTransactionPoid(anyLong())).thenReturn(List.of(detail));
//
//        BankDepositVoucherResponseDto response = service.createBankDepositVoucher(requestDto);
//
//        assertNotNull(response);
//        verify(hdrRepository, times(1)).save(any(GlBankDepositVoucherHdr.class));
//    }
//
//    @Test
//    void testUpdateBankDepositVoucher_Success() {
//        when(hdrRepository.findByTransactionPoid(69664L)).thenReturn(Optional.of(header));
//        when(dtlRepository.findByTransactionPoid(69664L)).thenReturn(List.of(detail));
//        when(hdrRepository.save(any(GlBankDepositVoucherHdr.class))).thenReturn(header);
//        when(dtlRepository.saveAll(anyList())).thenReturn(List.of(detail));
//        doNothing().when(dtlRepository).deleteByTransactionPoid(69664L);
//
//        BankDepositVoucherResponseDto response = service.updateBankDepositVoucher(69664L, requestDto);
//
//        assertNotNull(response);
//        verify(hdrRepository, times(1)).save(any(GlBankDepositVoucherHdr.class));
//        verify(dtlRepository, times(1)).deleteByTransactionPoid(69664L);
//        verify(dtlRepository, times(1)).saveAll(anyList());
//    }
//
//    @Test
//    void testUpdateBankDepositVoucher_NotFound() {
//        when(hdrRepository.findByTransactionPoid(69664L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class,
//                () -> service.updateBankDepositVoucher(69664L, requestDto));
//    }
//
//    @Test
//    void testCalculateGrandTotal_EmptyDetails() {
//        requestDto.setDetails(null);
//
//        when(hdrRepository.save(any(GlBankDepositVoucherHdr.class))).thenReturn(header);
//        when(hdrRepository.findByTransactionPoid(anyLong())).thenReturn(Optional.of(header));
//        when(dtlRepository.findByTransactionPoid(anyLong())).thenReturn(List.of());
//
//        BankDepositVoucherResponseDto response = service.createBankDepositVoucher(requestDto);
//
//        assertNotNull(response);
//        verify(hdrRepository, times(1)).save(any(GlBankDepositVoucherHdr.class));
//    }
//
//    @Test
//    void testConvertToDetailEntity_SelectedFieldConversion() {
//        BankDepositVoucherDtlDto dto = BankDepositVoucherDtlDto.builder()
//                .selected("Y")
//                .amount(BigDecimal.valueOf(100))
//                .build();
//
//        requestDto.setDetails(List.of(dto));
//
//        when(hdrRepository.save(any(GlBankDepositVoucherHdr.class))).thenReturn(header);
//        when(hdrRepository.findByTransactionPoid(anyLong())).thenReturn(Optional.of(header));
//        when(dtlRepository.saveAll(anyList())).thenReturn(List.of(detail));
//        when(dtlRepository.findByTransactionPoid(anyLong())).thenReturn(List.of(detail));
//
//        BankDepositVoucherResponseDto response = service.createBankDepositVoucher(requestDto);
//
//        assertNotNull(response);
//        verify(dtlRepository, times(1)).saveAll(anyList());
//    }
//}
//
