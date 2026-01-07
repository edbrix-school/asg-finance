//package com.asg.finance.service;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.dto.LovGetListDto;
//import com.asg.common.lib.dto.RawSearchResult;
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.security.util.UserContext;
//import com.asg.common.lib.service.DocumentSearchService;
//import com.asg.common.lib.service.LovDataService;
//import com.asg.finance.dto.TelexFileDtlDto;
//import com.asg.finance.dto.TelexFileGenerateRequestDto;
//import com.asg.finance.dto.TelexFileGenerateResponseDto;
//import com.asg.finance.entity.GlBankFileDtl;
//import com.asg.finance.entity.GlBankFileHdr;
//import com.asg.finance.repository.GlBankFileDtlRepository;
//import com.asg.finance.repository.GlBankFileHdrRepository;
//import com.asg.finance.repository.TelexFileGenerateProcRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
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
//public class TelexFileGenerateServiceImplTest {
//
//    @Mock
//    private GlBankFileHdrRepository hdrRepository;
//
//    @Mock
//    private GlBankFileDtlRepository dtlRepository;
//
//    @Mock
//    private TelexFileGenerateProcRepository procRepository;
//
//    @Mock
//    private DocumentSearchService documentService;
//
//    @Mock
//    private LovDataService lovService;
//
//    @InjectMocks
//    private TelexFileGenerateServiceImpl telexService;
//
//    private TelexFileGenerateRequestDto requestDto;
//    private GlBankFileHdr header;
//    private GlBankFileDtl detail;
//
//    @BeforeEach
//    void setUp() {
//        header = new GlBankFileHdr();
//        header.setTransactionPoid(1L);
//        header.setTransactionDate(LocalDate.now());
//        header.setGroupPoid(5L);
//        header.setCompanyPoid(10L);
//        header.setBankPoid(25L);
//        header.setDocRef("TLX-001");
//        header.setBankList("Y");
//        header.setLongNarration("Test narration");
//        header.setDeleted("N");
//        header.setCreatedBy("SYSTEM");
//        header.setCreatedDate(LocalDateTime.now());
//        header.setLastModifiedBy("SYSTEM");
//        header.setLastModifiedDate(LocalDateTime.now());
//
//        detail = GlBankFileDtl.builder()
//                .transactionPoid(1L)
//                .detRowId(1L)
//                .debitTransactionPoid(1001L)
//                .debitCompanyPoid(10L)
//                .debitAmount(BigDecimal.valueOf(5000))
//                .debitCurrencyCode("AED")
//                .deleted("N")
//                .build();
//
//        requestDto = TelexFileGenerateRequestDto.builder()
//                .bankPoid(25L)
//                .bankList("Y")
//                .transactionDate(LocalDate.now())
//                .remarks("Test narration")
//                .approvalOnly(true)
//                .suppressBalanceCheck(false)
//                .details(List.of(
//                        TelexFileDtlDto.builder()
//                                .debitTransactionPoid(1001L)
//                                .debitCompanyPoid(10L)
//                                .debitAmount(BigDecimal.valueOf(5000))
//                                .debitCurrencyCode("AED")
//                                .build()
//                ))
//                .build();
//    }
//
//    @Test
//    void testCreateTelexFile_Success() {
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
////            mockedUserContext.when(UserContext::getUserGroupPoid).thenReturn(5L);
////            mockedUserContext.when(UserContext::getUserCompanyPoid).thenReturn(10L);
//            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
//
//            when(hdrRepository.save(any(GlBankFileHdr.class))).thenReturn(header);
//            when(dtlRepository.saveAll(anyList())).thenReturn(List.of(detail));
//            when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
//            when(dtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(detail));
//            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(new LovGetListDto());
//
//            TelexFileGenerateResponseDto response = telexService.createTelexFile(requestDto);
//
//            assertNotNull(response);
//            assertEquals(1L, response.getTransactionPoid());
//            assertEquals("TLX-001", response.getDocRef());
//            verify(hdrRepository, times(1)).save(any(GlBankFileHdr.class));
//            verify(dtlRepository, times(1)).saveAll(anyList());
//        }
//    }
//
//    @Test
//    void testGetTelexFileById_Success() {
//        when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
//        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(detail));
//        when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(new LovGetListDto());
//
//        TelexFileGenerateResponseDto response = telexService.getTelexFileById(1L);
//
//        assertNotNull(response);
//        assertEquals(1L, response.getTransactionPoid());
//        assertEquals("TLX-001", response.getDocRef());
//        verify(hdrRepository, times(1)).findByTransactionPoid(1L);
//    }
//
//    @Test
//    void testGetTelexFileById_NotFound() {
//        when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> telexService.getTelexFileById(1L));
//    }
//
//    @Test
//    void testUpdateTelexFile_Success() {
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
//            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
//
//            when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
//            when(hdrRepository.save(any(GlBankFileHdr.class))).thenReturn(header);
//            when(dtlRepository.saveAll(anyList())).thenReturn(List.of(detail));
//            when(dtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(detail));
//            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(new LovGetListDto());
//
//            TelexFileGenerateResponseDto response = telexService.updateTelexFile(1L, requestDto);
//
//            assertNotNull(response);
//            assertEquals(1L, response.getTransactionPoid());
//            verify(hdrRepository, times(1)).save(any(GlBankFileHdr.class));
//            verify(dtlRepository, times(1)).deleteByTransactionPoid(1L);
//        }
//    }
//
//    @Test
//    void testSoftDeleteTelexFile_Success() {
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
//            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
//
//            when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
//            when(hdrRepository.save(any(GlBankFileHdr.class))).thenReturn(header);
//
//            telexService.softDeleteTelexFile(1L);
//
//            assertEquals("Y", header.getDeleted());
//            verify(hdrRepository, times(1)).save(header);
//        }
//    }
//
//    @Test
//    void testSoftDeleteTelexFile_NotFound() {
//        when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> telexService.softDeleteTelexFile(1L));
//    }
//
//    @Test
//    void testListTelexFiles_Success() {
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", Collections.emptyList());
//        Pageable pageable = PageRequest.of(0, 10);
//
//        when(documentService.resolveOperator(any())).thenReturn("AND");
//        when(documentService.resolveIsDeleted(any())).thenReturn("N");
//        when(documentService.resolveDateFilters(any(), anyString(), any(), any())).thenReturn(Collections.emptyList());
//
//        RawSearchResult rawSearchResult = new RawSearchResult(
//                List.of(Map.of("TRANSACTION_POID", 1L, "DOC_REF", "TLX-001")),
//                Map.of("TRANSACTION_POID", "DOC_REF"),
//                1L
//        );
//
//        when(documentService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
//                .thenReturn(rawSearchResult);
//
//        Map<String, Object> result = telexService.listTelexFiles("100-153", filters, null, null, pageable);
//
//        assertNotNull(result);
//        verify(documentService, times(1))
//                .search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString());
//    }
//
//    @Test
//    void testLoadTelexTransferData_Success() {
//        List<TelexFileDtlDto> mockData = List.of(
//                TelexFileDtlDto.builder()
//                        .debitTransactionPoid(1001L)
//                        .debitAmount(BigDecimal.valueOf(5000))
//                        .build()
//        );
//
//        when(procRepository.loadTelexTransferData("Y")).thenReturn(mockData);
//
//        List<TelexFileDtlDto> result = telexService.loadTelexTransferData( "Y");
//
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        verify(procRepository, times(1)).loadTelexTransferData("Y");
//    }
//
//    @Test
//    void testRegenerateTelexFile_Success() {
//        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
//            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
//
//            when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
//            when(procRepository.regenerateTelexFile(5L, 10L, 1L, 1L)).thenReturn("SUCCESS");
//
//            String result = telexService.regenerateTelexFile(1L);
//
//            assertEquals("SUCCESS", result);
//            verify(procRepository, times(1)).regenerateTelexFile(5L, 10L, 1L, 1L);
//        }
//    }
//
//    @Test
//    void testValidateAndMarkDeleted_Success() {
//        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(detail));
//        when(procRepository.validateBeneficiaryDetails(1001L)).thenReturn(null);
//        when(dtlRepository.saveAll(anyList())).thenReturn(List.of(detail));
//
//        String result = telexService.validateAndMarkDeleted(1L, List.of(1L));
//
//        assertNull(result);
//        assertEquals("Y", detail.getDeleted());
//        verify(dtlRepository, times(1)).saveAll(anyList());
//    }
//
//    @Test
//    void testValidateAndMarkDeleted_ValidationFails() {
//        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(detail));
//        when(procRepository.validateBeneficiaryDetails(1001L)).thenReturn("Validation error");
//
//        String result = telexService.validateAndMarkDeleted(1L, List.of(1L));
//
//        assertEquals("Validation error", result);
//        verify(dtlRepository, never()).saveAll(anyList());
//    }
//}
//
