//package com.asg.finance.service;
//
//import com.asg.common.lib.dto.FilterDto;
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.dto.RawSearchResult;
//import com.asg.common.lib.security.util.UserContext;
//import com.asg.common.lib.service.DocumentSearchService;
//import com.asg.finance.dto.PettyCashRequestDto;
//import com.asg.finance.dto.PettyCashResponseDto;
//import com.asg.finance.entity.GlPettyCashPaymentHdr;
//import com.asg.finance.repository.*;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class PettyCashVoucherServiceImplTest {
//
//    @Mock private GlPettyCashPaymentHdrRepository glPettyCashPaymentHdrRepository;
//    @Mock private GlPettyCashPaymentDtlRepository glPettyCashPaymentDtlRepository;
//    @Mock private GLPettyCashItemDtlRepository glPettyCashItemDtlRepository;
//    @Mock private GlPettyCashChargeDtlRepository glPettyCashChargeDtlRepository;
//    @Mock private DocumentSearchService documentService;
//    @Mock private PettyCashLoadByRefTypeRepository pettyCashLoadByRefTypeRepository;
//    @Mock private  PettyCashPaymentVoucherCustomRepository pettyCashPaymentVoucherCustomRepository;
//
//    @InjectMocks
//    private PettyCashVoucherServiceImpl pettyCashVoucherService;
//
//    private PettyCashRequestDto requestDto;
//    private GlPettyCashPaymentHdr savedHeader;
//    private Long loginGroupPoid = 1L;
//    private Long loginCompanyPoid = 1L;
//    private Long loginUserPoid = 1L;
//
//    @BeforeEach
//    void setUp() {
//        requestDto = createSampleRequestDto();
//        savedHeader = createSampleHeader();
//    }
//
//    @Test
//    void createPettyCash_GeneralType_Success() {
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
//            mockedUserContext.when(UserContext::getUserId).thenReturn("123");
//
//            requestDto.setRefType("GENERAL");
//            when(glPettyCashPaymentHdrRepository.save(any())).thenReturn(savedHeader);
//            when(glPettyCashPaymentDtlRepository.saveAll(any())).thenReturn(Collections.emptyList());
//
//            PettyCashResponseDto result = pettyCashVoucherService.createPettyCash(
//                    requestDto, any());
//
//            assertNotNull(result);
//            assertEquals(savedHeader.getTransactionPoid(), result.getTransactionPoid());
//            verify(pettyCashPaymentVoucherCustomRepository).validateGlVouchers(any(), any(), any(), any(), any(), any(), any());
//            verify(pettyCashPaymentVoucherCustomRepository).validateBeforeSave(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
//        }
//    }
//
//    @Test
//    void createPettyCash_MTQRFQType_Success() {
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
//            mockedUserContext.when(UserContext::getUserId).thenReturn("123");
//
//            requestDto.setRefType("MTQ RFQ");
//            when(glPettyCashPaymentHdrRepository.save(any())).thenReturn(savedHeader);
//            when(glPettyCashItemDtlRepository.saveAll(any())).thenReturn(Collections.emptyList());
//
//            PettyCashResponseDto result = pettyCashVoucherService.createPettyCash(
//                    requestDto, any());
//
//            assertNotNull(result);
//        }
//    }
//
//
//
//    @Test
//    void createPettyCash_InvalidRefType_ThrowsException() {
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
//            mockedUserContext.when(UserContext::getUserId).thenReturn("123");
//
//            requestDto.setRefType("INVALID");
//            when(glPettyCashPaymentHdrRepository.save(any())).thenReturn(savedHeader);
//
//            assertThrows(RuntimeException.class, () ->
//                    pettyCashVoucherService.createPettyCash(requestDto, any()));
//        }
//    }
//
//    @Test
//    void updatePettyCash_Success() {
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
//            mockedUserContext.when(UserContext::getUserId).thenReturn("123");
//
//            Long transactionPoid = 1L;
//            requestDto.setRefType("GENERAL");
//
//            when(glPettyCashPaymentHdrRepository.findByTransactionPoid(transactionPoid))
//                    .thenReturn(Optional.of(savedHeader));
//            when(glPettyCashPaymentHdrRepository.save(any())).thenReturn(savedHeader);
//            when(glPettyCashPaymentDtlRepository.findByTransactionPoid(transactionPoid))
//                    .thenReturn(Collections.emptyList());
//            when(glPettyCashPaymentDtlRepository.saveAll(any())).thenReturn(Collections.emptyList());
//
//            PettyCashResponseDto result = pettyCashVoucherService.updatePettyCash(
//                    transactionPoid, requestDto, any());
//
//            assertNotNull(result);
//            verify(pettyCashPaymentVoucherCustomRepository).getOldJobReferences(any(), any(), any(), any(), any(), any(), any());
//            verify(pettyCashPaymentVoucherCustomRepository).validateJobBeforeSave(any(), any(), any(), any(), any(), any(), any());
//        }
//    }
//
//    @Test
//    void updatePettyCash_NotFound_ThrowsException() {
//        Long transactionPoid = 1L;
//        when(glPettyCashPaymentHdrRepository.findByTransactionPoid(transactionPoid))
//                .thenReturn(Optional.empty());
//
//        assertThrows(RuntimeException.class, () ->
//                pettyCashVoucherService.updatePettyCash(transactionPoid, requestDto, any()));
//    }
//
//    @Test
//    void deletePettyCashVoucher_Success() {
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
//            mockedUserContext.when(UserContext::getUserId).thenReturn("123");
//
//            Long transactionPoid = 1L;
//            String docId = "DOC001";
//            String refType = "GENERAL";
//            String refPoid = "1";
//
//            when(glPettyCashPaymentHdrRepository.findByTransactionPoid(transactionPoid))
//                    .thenReturn(Optional.of(savedHeader));
//            when(glPettyCashPaymentHdrRepository.save(any())).thenReturn(savedHeader);
//
//            pettyCashVoucherService.deletePettyCashVoucher(
//                    transactionPoid, docId, refType);
//
//            verify(pettyCashPaymentVoucherCustomRepository).validateVoucherBeforeDelete(any(), any(), any(), any(), any(), any());
//            verify(glPettyCashPaymentHdrRepository).save(argThat(header -> "Y".equals(header.getDeleted())));
//        }
//    }
//
//    @Test
//    void listPettyCashVoucher_Success() {
//        String documentId = "DOC001";
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());
//        Pageable pageable = PageRequest.of(0, 10);
//        LocalDate startDate = LocalDate.of(2024, 1, 1);
//        LocalDate endDate = LocalDate.of(2024, 12, 31);
//        List<FilterDto> dateFilters = Collections.emptyList();
//
//        when(documentService.resolveOperator(filters)).thenReturn("AND");
//        when(documentService.resolveIsDeleted(filters)).thenReturn("N");
//        when(documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate, endDate))
//                .thenReturn(dateFilters);
//        when(documentService.search(any(), any(), any(), any(), any(), any(), any()))
//                .thenReturn(new RawSearchResult(Collections.emptyList(), Collections.emptyMap(), 0L));
//
//        Map<String, Object> result = pettyCashVoucherService.listPettyCashVoucher(documentId, filters,startDate, endDate, pageable);
//
//        assertNotNull(result);
//        verify(documentService).search(eq(documentId), any(), eq("AND"), eq(pageable), eq("N"), eq("REF_TYPE"), eq("TRANSACTION_POID"));
//    }
//
//    private PettyCashRequestDto createSampleRequestDto() {
//        PettyCashRequestDto dto = new PettyCashRequestDto();
//        dto.setTransactionDate(new Date());
//        dto.setCurrencyCode("USD");
//        dto.setCurrencyRate(BigDecimal.ONE);
//        dto.setAmount(BigDecimal.valueOf(1000));
//        dto.setPayingTo("Test Payee");
//        dto.setNarration("Test narration");
//        dto.setRefType("GENERAL");
//        dto.setDocId("DOC001");
//        dto.setGlPettyCashPaymentDtlRequestDtos(Collections.emptyList());
//        dto.setGlPettyCashChargeDtlRequestDtos(Collections.emptyList());
//        dto.setGlPettyCashItemDtlRequestDtos(Collections.emptyList());
//        return dto;
//    }
//
//    private GlPettyCashPaymentHdr createSampleHeader() {
//        return GlPettyCashPaymentHdr.builder()
//                .transactionPoid(1L)
//                .docRef("PC001")
//                .transactionDate(new Date())
//                .groupPoid(loginGroupPoid)
//                .companyPoid(loginCompanyPoid)
//                .currencyCode("USD")
//                .amount(BigDecimal.valueOf(1000))
//                .payingTo("Test Payee")
//                .refType("GENERAL")
//                .createdBy("1")
//                .createdDate(new Date())
//                .build();
//    }
//}
