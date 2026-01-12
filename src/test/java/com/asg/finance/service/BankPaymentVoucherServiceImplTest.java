//package com.asg.finance.service;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.dto.LovGetListDto;
//import com.asg.common.lib.dto.RawSearchResult;
//import com.asg.common.lib.exception.ValidationException;
//import com.asg.common.lib.security.util.UserContext;
//import com.asg.common.lib.service.DocumentSearchService;
//import com.asg.common.lib.service.LovDataService;
//import com.asg.finance.dto.BankPaymentChargeDetailRequest;
//import com.asg.finance.dto.BankPaymentVoucherRequest;
//import com.asg.finance.dto.BankPaymentVoucherResponse;
//import com.asg.finance.entity.GLPaymentVoucherHDREntity;
//import com.asg.finance.entity.GlBankPaymentChargeDtlEntity;
//import com.asg.finance.repository.*;
//import org.junit.jupiter.api.AfterEach;
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
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//public class BankPaymentVoucherServiceImplTest {
//
//    private MockedStatic<UserContext> userContextMock;
//
//    @Mock
//    private BankPaymentVoucherRepository paymentVoucherRepository;
//
//    @Mock
//    private BankPaymentVoucherDetailsRepository paymentVoucherDetailsRepository;
//
//    @Mock
//    private GlBankPaymentChargeDtlRepository chargeDtlRepository;
//
//    @Mock
//    private GlBankPaymentItemDtlRepository itemRepository;
//
//    @Mock
//    private BankPaymentVoucherSpRepository spRepository;
//
//    @Mock
//    private BankPaymentLoadDataRepository loadDataRepository;
//
//    @Mock
//    private DocumentSearchService documentService;
//
//    @Mock
//    private LovDataService lovService;
//
//    @InjectMocks
//    private BankPaymentVoucherServiceImpl voucherService;
//
//    private BankPaymentVoucherRequest request;
//    private GLPaymentVoucherHDREntity header;
//    private GlBankPaymentChargeDtlEntity chargeDetail;
//    private AuthenticationDetails authDetails;
//
//    @BeforeEach
//    void setUp() {
//        userContextMock = mockStatic(UserContext.class);
//
//        authDetails = AuthenticationDetails.builder()
//                .loggedInUserName("TESTUSER")
//                .loggedInUserPoid(1L)
//                .loggedInGroupPoid(1L)
//                .loggedInCompanyPoid(1L)
//                .build();
//
//        userContextMock.when(UserContext::getCurrentUser).thenReturn(authDetails);
//        userContextMock.when(UserContext::getUserGroupPoid).thenReturn(1L);
//        userContextMock.when(UserContext::getUserCompanyPoid).thenReturn(1L);
//
//        header = new GLPaymentVoucherHDREntity();
//        header.setTransactionPoid(1L);
//        header.setGroupPoid(1L);
//        header.setCompanyPoid(1L);
//        header.setBankPoid(100L);
//        header.setRefType("FDA JOBS");
//        header.setFdaRef(1122L);
//        header.setPayGlPoid(500L);
//        header.setPayingTo("XYZ Logistics");
//        header.setDocRef("BPV-1");
//        header.setDeleted("N");
//        header.setCreatedBy("SYSTEM");
//        header.setCreatedDate(LocalDateTime.now());
//
//        chargeDetail = new GlBankPaymentChargeDtlEntity();
//        chargeDetail.setTransactionPoid(1L);
//        chargeDetail.setDetRowId(1L);
//        chargeDetail.setChargePoid(1L);
//        chargeDetail.setChargeAmount(20000L);
//        chargeDetail.setDescription("Freight Charge");
//
//        BankPaymentChargeDetailRequest chargeRequest = new BankPaymentChargeDetailRequest();
//        chargeRequest.setDetRowId(1L);
//        chargeRequest.setChargePoid(1L);
//        chargeRequest.setChargeAmount(20000L);
//        chargeRequest.setDescription("Freight Charge");
//        chargeRequest.setSelected(true);
//
//        request = new BankPaymentVoucherRequest();
//        request.setBankPoid(100L);
//        request.setRefType("FDA JOBS");
//        request.setFdaRefId(1122L);
//        request.setPayGlPoid(500L);
//        request.setPayingTo("XYZ Logistics");
//        request.setChequeNo("CHQ987654");
//        request.setChequeDate("2025-11-12");
//        request.setRemarks("Payment for freight");
//        request.setAccountPayee(true);
//        request.setChargeDetailRequests(List.of(chargeRequest));
//    }
//
//    @AfterEach
//    void tearDown() {
//        if (userContextMock != null) {
//            userContextMock.close();
//        }
//    }
//
//    @Test
//    void testGetVoucherById_Success() {
//        when(paymentVoucherRepository.findById(1L)).thenReturn(Optional.of(header));
//        when(chargeDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(chargeDetail));
//        when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(new LovGetListDto());
//
//        BankPaymentVoucherResponse response = voucherService.getVoucherById(1L,"400-107");
//
//        assertNotNull(response);
//        assertEquals(1L, response.getTransactionPoid());
//        assertEquals("FDA JOBS", response.getRefType());
//        verify(paymentVoucherRepository, times(1)).findById(1L);
//    }
//
//    @Test
//    void testGetVoucherById_NotFound() {
//        when(paymentVoucherRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ValidationException.class, () -> voucherService.getVoucherById(1L,"400-107"));
//    }
//
//    @Test
//    void testCreateBankPaymentVoucher_Success() {
//        when(paymentVoucherRepository.save(any(GLPaymentVoucherHDREntity.class))).thenReturn(header);
//        when(paymentVoucherRepository.findById(1L)).thenReturn(Optional.of(header));
//        when(chargeDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(chargeDetail));
//        when(chargeDtlRepository.saveAll(anyList())).thenReturn(List.of(chargeDetail));
//        when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(new LovGetListDto());
//
//        BankPaymentVoucherResponse response = voucherService.createBankPaymentVoucher(request, anyString());
//
//        assertNotNull(response);
//        assertEquals("FDA JOBS", response.getRefType());
//        verify(paymentVoucherRepository, atLeast(2)).save(any(GLPaymentVoucherHDREntity.class));
//    }
//
//    @Test
//    void testCreateBankPaymentVoucher_ValidationFailure_MissingFdaRefId() {
//        request.setFdaRefId(null);
//
//        assertThrows(ValidationException.class, () -> voucherService.createBankPaymentVoucher(request, anyString()));
//    }
//
//    @Test
//    void testSoftDeleteVoucher_Success() {
//        when(paymentVoucherRepository.findById(1L)).thenReturn(Optional.of(header));
//        when(paymentVoucherRepository.save(any(GLPaymentVoucherHDREntity.class))).thenReturn(header);
//        when(chargeDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(chargeDetail));
//        when(spRepository.validateVoucherStatus(anyLong(), any(), anyLong(), anyString(), anyString(), anyString())).thenReturn("SUCCESS");
//        when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(new LovGetListDto());
//
//        BankPaymentVoucherResponse response = voucherService.softDeleteVoucher(1L);
//
//        assertNotNull(response);
//        assertEquals("Y", header.getDeleted());
//        verify(paymentVoucherRepository, times(1)).save(header);
//    }
//
//    @Test
//    void testListBankPaymentVouchers_Success() {
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", Collections.emptyList());
//        Pageable pageable = PageRequest.of(0, 10);
//
//        when(documentService.resolveOperator(any())).thenReturn("AND");
//        when(documentService.resolveIsDeleted(any())).thenReturn("N");
//        when(documentService.resolveDateFilters(any(), anyString(), any(), any())).thenReturn(Collections.emptyList());
//
//        RawSearchResult rawSearchResult = new RawSearchResult(
//                List.of(Map.of("TRANSACTION_POID", 1L, "DOC_REF", "BPV-1")),
//                Map.of("TRANSACTION_POID", "DOC_REF"),
//                1L
//        );
//
//        when(documentService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
//                .thenReturn(rawSearchResult);
//
//        Map<String, Object> result = voucherService.listBankPaymentVouchers("400-107", filters, null, null, pageable);
//
//        assertNotNull(result);
//        verify(documentService, times(1))
//                .search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString());
//    }
//
//    @Test
//    void testGetBankBalance_Success() {
//        Map<String, Object> balance = Map.of("bankBalance", 100000, "availableBalance", 95000);
//        when(spRepository.getBankBalance(100L)).thenReturn(balance);
//
//        Map<String, Object> result = voucherService.getBankBalance(100L);
//
//        assertNotNull(result);
//        assertEquals(100000, result.get("bankBalance"));
//        verify(spRepository, times(1)).getBankBalance(100L);
//    }
//
//    @Test
//    void testValidateChequePrint_Success() {
//        when(paymentVoucherRepository.findById(1L)).thenReturn(Optional.of(header));
//        when(spRepository.validateBeforeChequePrint(anyLong(), any(), anyLong(), anyLong(), anyString(), anyLong(), anyString()))
//                .thenReturn(Map.of("result", "SUCCESS"));
//
//        voucherService.validateChequePrint(1L);
//
//        verify(spRepository, times(1)).validateBeforeChequePrint(anyLong(), any(), anyLong(), anyLong(), anyString(), anyLong(), anyString());
//    }
//
//    @Test
//    void testMarkChequePrinted_Success() {
//        when(paymentVoucherRepository.findById(1L)).thenReturn(Optional.of(header));
//
//        voucherService.markChequePrinted(1L);
//
//        verify(spRepository, times(1)).afterChequePrint(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString(), any());
//    }
//
//    @Test
//    void testReleaseCheque_Success() {
//        when(paymentVoucherRepository.findById(1L)).thenReturn(Optional.of(header));
//
//        voucherService.releaseCheque(1L, "John Doe", "+971501234567");
//
//        verify(spRepository, times(1)).releaseCheque(anyLong(), anyString(), anyLong(), anyLong(), eq("John Doe"), eq("+971501234567"));
//    }
//
//    @Test
//    void testUnReleaseCheque_Success() {
//        when(paymentVoucherRepository.findById(1L)).thenReturn(Optional.of(header));
//
//        voucherService.unReleaseCheque(1L);
//
//        verify(spRepository, times(1)).unReleaseCheque(anyLong(), anyString(), anyLong(), anyLong());
//    }
//
//    @Test
//    void testResetChequeStatus_Success() {
//        when(paymentVoucherRepository.findById(1L)).thenReturn(Optional.of(header));
//
//        voucherService.resetChequeStatus(1L);
//
//        verify(spRepository, times(1)).resetChequeStatus(anyLong(), anyLong(), any(), anyLong());
//    }
//
//    @Test
//    void testRevertReconciliation_Success() {
//        when(paymentVoucherRepository.findById(1L)).thenReturn(Optional.of(header));
//
//        voucherService.revertReconciliation(1L, "Test comment");
//
//        verify(spRepository, times(1)).revertReconciliation(anyLong(), anyLong(), any(), anyString(), anyString(), anyString());
//    }
//}
//
