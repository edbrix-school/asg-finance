//package com.asg.finance.service;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.dto.RawSearchResult;
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.exception.ValidationException;
//import com.asg.common.lib.service.DocumentSearchService;
//import com.asg.finance.dto.ImcoDepositRefundRequestDTO;
//import com.asg.finance.dto.ImcoDepositRefundResponseDTO;
//import com.asg.finance.entity.GlImcoChequeBillDtl;
//import com.asg.finance.entity.GlImcoChequeRefundDtl;
//import com.asg.finance.entity.GlImcoChequeRefundHdr;
//import com.asg.finance.repository.*;
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
//import java.sql.SQLException;
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
//public class ImcoDepositRefundServiceImplTest {
//    @Mock
//    private GlImcoChequeRefundHdrRepository hdrRepository;
//
//    @Mock
//    private GlImcoChequeRefundDtlRepository dtlRepository;
//
//    @Mock
//    private GlImcoChequeBillDtlRepository billDtlRepository;
//
//    @Mock
//    private DocumentSearchService documentService;
//
//    @Mock
//    private ImcoChequeDetailsRepository repository;
//
//    @Mock
//    private ImcoDepositRefundRepository depositRefundRepository;
//
//    @Mock
//    private ImcoSaveRefundRepository imcoSaveRefundRepository;
//
//    @InjectMocks
//    private ImcoDepositRefundServiceImpl refundService;
//
//    private ImcoDepositRefundRequestDTO requestDTO;
//    private GlImcoChequeRefundHdr header;
//    private GlImcoChequeRefundDtl dtl;
//    private GlImcoChequeBillDtl billDtl;
//
//    @BeforeEach
//    void setUp() {
//
//        header = GlImcoChequeRefundHdr.builder()
//                .transactionPoid(1L)
//                .transactionDate(LocalDate.now())
//                .groupPoid(1L)
//                .companyPoid(1L)
//                .docRef("DOC-001")
//                .remarks("Test Remarks")
//                .grandTotal(BigDecimal.valueOf(1000))
//                .receiptNum("RCP001")
//                .payingTo("John Doe")
//                .deleted("N")
//                .createdBy("SYSTEM")
//                .createdDate(LocalDateTime.now())
//                .lastModifiedBy("SYSTEM")
//                .lastModifiedDate(LocalDateTime.now())
//                .build();
//
//        dtl = GlImcoChequeRefundDtl.builder()
//                .transactionPoid(1L)
//                .detRowId(1L)
//                .amount(BigDecimal.valueOf(1000))
//                .remarks("Refund detail")
//                .build();
//
//        billDtl = GlImcoChequeBillDtl.builder()
//                .transactionPoid(1L)
//                .detRowId(1L)
//                .billRef("BILL001")
//                .billAmount(BigDecimal.valueOf(1001))
//                .remarks("Bill detail")
//                .build();
//
//        ImcoDepositRefundRequestDTO.ChequeRefundDetailDTO refundDetailDTO =
//                ImcoDepositRefundRequestDTO.ChequeRefundDetailDTO.builder()
//                        .amount(BigDecimal.valueOf(1000))
//                        .remarks("Refund detail")
//                        .build();
//
//        ImcoDepositRefundRequestDTO.ChequeBillDetailDTO billDetailDTO =
//                ImcoDepositRefundRequestDTO.ChequeBillDetailDTO.builder()
//                        .billRef("BILL001")
//                        .billAmount(BigDecimal.valueOf(1001))
//                        .remarks("Bill detail")
//                        .build();
//
//        requestDTO = ImcoDepositRefundRequestDTO.builder()
//                .transactionDate(LocalDate.now())
//                .groupPoid(1L)
//                .companyPoid(1L)
//                .docRef("DOC-001")
//                .remarks("Test Remarks")
//                .grandTotal(BigDecimal.valueOf(1000))
//                .receiptNum("RCP001")
//                .blNumber("BL001")
//                .chequeRefundDetails(List.of(refundDetailDTO))
//                .chequeBillDetails(List.of(billDetailDTO))
//                .build();
//    }
//
//
//
//    @Test
//    void testGetImcoDepositRefundById_Success() {
//        when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
//        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(dtl));
//        when(billDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(billDtl));
//
//        ImcoDepositRefundResponseDTO response = refundService.getImcoDepositRefundById(1L);
//
//        assertNotNull(response);
//        assertEquals(1L, response.getPoid());
//        assertEquals("DOC-001", response.getDocRef());
//        assertEquals(1, response.getChequeRefundDetails().size());
//        verify(hdrRepository, times(1)).findByTransactionPoid(1L);
//    }
//
//    @Test
//    void testGetImcoDepositRefundById_NotFound() {
//        when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> refundService.getImcoDepositRefundById(1L));
//    }
//
//
//
//    @Test
//    void testSoftDeleteImcoDepositRefund_Success() {
//        when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
//        when(hdrRepository.save(any(GlImcoChequeRefundHdr.class))).thenReturn(header);
//
//        refundService.softDeleteImcoDepositRefund(1L);
//
//        assertEquals("Y", header.getDeleted());
//        verify(hdrRepository, times(1)).save(header);
//    }
//
//    @Test
//    void testSoftDeleteImcoDepositRefund_NotFound() {
//        when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> refundService.softDeleteImcoDepositRefund(1L));
//    }
//
//    @Test
//    void testListImcoDepositRefund_Success() {
//
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", Collections.emptyList());
//        Pageable pageable = PageRequest.of(0, 10);
//
//        when(documentService.resolveOperator(any())).thenReturn("AND");
//        when(documentService.resolveIsDeleted(any())).thenReturn("N");
//        when(documentService.resolveFilters(any())).thenReturn(Collections.emptyList());
//
//        RawSearchResult rawSearchResult = new RawSearchResult(
//                List.of(Map.of("TRANSACTION_POID", 1L, "DOC_REF", "DOC-001")),
//                Map.of("TRANSACTION_POID", "DOC_REF"),
//                1L
//        );
//
//        when(documentService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
//                .thenReturn(rawSearchResult);
//
//        Map<String, Object> result = refundService.listImcoDepositRefund("DOC123", filters, null, null, pageable);
//        assertNotNull(result);
//        verify(documentService, times(1))
//                .search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString());
//    }
//
//
//    @Test
//    void testCreateImcoDepositRefund_Success() {
//        when(hdrRepository.save(any(GlImcoChequeRefundHdr.class))).thenReturn(header);
//        when(dtlRepository.saveAll(anyList())).thenReturn(List.of(dtl));
//        when(billDtlRepository.saveAll(anyList())).thenReturn(List.of(billDtl));
//        when(imcoSaveRefundRepository.callImcoRefundLoadProcedure(1L, 1L, "SYSTEM", "RCP001", "BL001"))
//                .thenReturn("John Doe");
//
//        ImcoDepositRefundResponseDTO response = refundService.createImcoDepositRefund(requestDTO);
//
//        assertNotNull(response);
//        assertEquals("DOC-001", response.getDocRef());
//        verify(hdrRepository, times(1)).save(any(GlImcoChequeRefundHdr.class));
//        verify(dtlRepository, times(1)).saveAll(anyList());
//        verify(billDtlRepository, times(1)).saveAll(anyList());
//    }
//
//    @Test
//    void testCreateImcoDepositRefund_ValidationFailure() {
//        ImcoDepositRefundRequestDTO.ChequeRefundDetailDTO refundDetail =
//                ImcoDepositRefundRequestDTO.ChequeRefundDetailDTO.builder()
//                        .amount(BigDecimal.valueOf(2000))
//                        .build();
//
//        ImcoDepositRefundRequestDTO.ChequeBillDetailDTO billDetail =
//                ImcoDepositRefundRequestDTO.ChequeBillDetailDTO.builder()
//                        .billAmount(BigDecimal.valueOf(1000))
//                        .build();
//
//        ImcoDepositRefundRequestDTO invalidRequest = ImcoDepositRefundRequestDTO.builder()
//                .chequeRefundDetails(List.of(refundDetail))
//                .chequeBillDetails(List.of(billDetail))
//                .build();
//
//        assertThrows(ValidationException.class, () -> refundService.createImcoDepositRefund(invalidRequest));
//    }
//
//    @Test
//    void testCreateImcoDepositRefund_NullGroupAndCompanyPoid() {
//        ImcoDepositRefundRequestDTO requestWithNulls = ImcoDepositRefundRequestDTO.builder()
//                .transactionDate(LocalDate.now())
//                .groupPoid(null)
//                .companyPoid(null)
//                .docRef("DOC-001")
//                .grandTotal(BigDecimal.valueOf(1000))
//                .receiptNum("RCP001")
//                .blNumber("BL001")
//                .chequeRefundDetails(List.of(ImcoDepositRefundRequestDTO.ChequeRefundDetailDTO.builder()
//                        .amount(BigDecimal.valueOf(500)).build()))
//                .chequeBillDetails(List.of(ImcoDepositRefundRequestDTO.ChequeBillDetailDTO.builder()
//                        .billAmount(BigDecimal.valueOf(1000)).build()))
//                .build();
//
//        when(hdrRepository.save(any(GlImcoChequeRefundHdr.class))).thenReturn(header);
//        when(dtlRepository.saveAll(anyList())).thenReturn(List.of(dtl));
//        when(billDtlRepository.saveAll(anyList())).thenReturn(List.of(billDtl));
//        when(imcoSaveRefundRepository.callImcoRefundLoadProcedure(1L, 1L, "SYSTEM", "RCP001", "BL001"))
//                .thenReturn(null);
//
//        ImcoDepositRefundResponseDTO response = refundService.createImcoDepositRefund(requestWithNulls);
//
//        assertNotNull(response);
//        verify(hdrRepository, times(1)).save(any(GlImcoChequeRefundHdr.class));
//    }
//
//    @Test
//    void testCreateImcoDepositRefund_ProcedureUpdatesPayingTo() {
//        when(hdrRepository.save(any(GlImcoChequeRefundHdr.class))).thenReturn(header);
//        when(dtlRepository.saveAll(anyList())).thenReturn(List.of(dtl));
//        when(billDtlRepository.saveAll(anyList())).thenReturn(List.of(billDtl));
//        when(imcoSaveRefundRepository.callImcoRefundLoadProcedure(1L, 1L, "SYSTEM", "RCP001", "BL001"))
//                .thenReturn("Updated Name");
//
//        ImcoDepositRefundResponseDTO response = refundService.createImcoDepositRefund(requestDTO);
//
//        assertNotNull(response);
//        verify(hdrRepository, times(1)).save(any(GlImcoChequeRefundHdr.class));
//    }
//
//    @Test
//    void testCreateImcoDepositRefund_NullDetRowIds() {
//        ImcoDepositRefundRequestDTO.ChequeRefundDetailDTO refundDetailWithNullId =
//                ImcoDepositRefundRequestDTO.ChequeRefundDetailDTO.builder()
//                        .detRowId(null)
//                        .amount(BigDecimal.valueOf(500))
//                        .build();
//
//        ImcoDepositRefundRequestDTO.ChequeBillDetailDTO billDetailWithNullId =
//                ImcoDepositRefundRequestDTO.ChequeBillDetailDTO.builder()
//                        .detRowId(null)
//                        .billAmount(BigDecimal.valueOf(1000))
//                        .build();
//
//        ImcoDepositRefundRequestDTO requestWithNullIds = ImcoDepositRefundRequestDTO.builder()
//                .transactionDate(LocalDate.now())
//                .groupPoid(1L)
//                .companyPoid(1L)
//                .docRef("DOC-001")
//                .grandTotal(BigDecimal.valueOf(1000))
//                .receiptNum("RCP001")
//                .blNumber("BL001")
//                .chequeRefundDetails(List.of(refundDetailWithNullId))
//                .chequeBillDetails(List.of(billDetailWithNullId))
//                .build();
//
//        when(hdrRepository.save(any(GlImcoChequeRefundHdr.class))).thenReturn(header);
//        when(dtlRepository.saveAll(anyList())).thenReturn(List.of(dtl));
//        when(billDtlRepository.saveAll(anyList())).thenReturn(List.of(billDtl));
//        when(imcoSaveRefundRepository.callImcoRefundLoadProcedure(1L, 1L, "SYSTEM", "RCP001", "BL001"))
//                .thenReturn(null);
//
//        ImcoDepositRefundResponseDTO response = refundService.createImcoDepositRefund(requestWithNullIds);
//
//        assertNotNull(response);
//        verify(dtlRepository, times(1)).saveAll(anyList());
//        verify(billDtlRepository, times(1)).saveAll(anyList());
//    }
//
//    @Test
//    void testGetChequeDetails_Success() throws SQLException {
//        com.asg.finance.dto.ImcoRefundLoadResponseDto expectedResult = new com.asg.finance.dto.ImcoRefundLoadResponseDto();
//        when(repository.fetchChequeAndBillDetails(1L, 1L, "SYSTEM", 1L, "RCP001"))
//                .thenReturn(expectedResult);
//
//        com.asg.finance.dto.ImcoRefundLoadResponseDto result = refundService.getChequeDetails(1L, "RCP001");
//
//        assertNotNull(result);
//        assertEquals(expectedResult, result);
//        verify(repository, times(1)).fetchChequeAndBillDetails(1L, 1L, "SYSTEM", 1L, "RCP001");
//    }
//
//    @Test
//    void testGetGlPostingDetails_Success() {
//        com.asg.dto.GlPostingViewResponseDto expectedResult = new com.asg.dto.GlPostingViewResponseDto();
//        when(depositRefundRepository.fetchGlPostingDetails(1L, 1L, "DOC001", 1L))
//                .thenReturn(expectedResult);
//
//        com.asg.dto.GlPostingViewResponseDto result = refundService.getGlPostingDetails(1L, 1L, "DOC001", 1L);
//
//        assertNotNull(result);
//        assertEquals(expectedResult, result);
//        verify(depositRefundRepository, times(1)).fetchGlPostingDetails(1L, 1L, "DOC001", 1L);
//    }
//
//}
//
