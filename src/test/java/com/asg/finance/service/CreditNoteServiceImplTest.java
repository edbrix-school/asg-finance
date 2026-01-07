//package com.asg.finance.service;
//
//import com.asg.common.lib.dto.FilterDto;
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.dto.LovGetListDto;
//import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
//import com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto;
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.security.util.UserContext;
//import com.asg.common.lib.utility.ASGHelperUtils;
//import com.asg.finance.dto.*;
//import com.asg.finance.entity.ArCreditNoteChargeDtl;
//import com.asg.finance.entity.ArCreditNoteDtl;
//import com.asg.finance.entity.ArCreditNoteHdr;
//import com.asg.finance.repository.ArCreditNoteChargeDtlRepository;
//import com.asg.finance.repository.ArCreditNoteDtlRepository;
//import com.asg.finance.repository.ArCreditNoteHdrRepository;
//import jakarta.persistence.EntityManager;
//import org.junit.jupiter.api.AfterEach;
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
//import javax.sql.DataSource;
//import java.math.BigDecimal;
//import java.sql.*;
//import java.time.Instant;
//import java.time.LocalDate;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class CreditNoteServiceImplTest {
//
//    @Mock
//    private ArCreditNoteHdrRepository creditNoteHdrRepository;
//
//    @Mock
//    private ArCreditNoteDtlRepository creditNoteDtlRepository;
//
//    @Mock
//    private ArCreditNoteChargeDtlRepository creditNoteChargeDtlRepository;
//
//    @Mock
//    private DataSource dataSource;
//
//    @Mock
//    private EntityManager entityManager;
//
//    @Mock
//    private ChargeLovService chargeLovService;
//
//    @Mock
//    private BillwiseBreakupService billwiseBreakupService;
//
//    @Mock
//    private CostCenterBreakupService costCenterBreakupService;
//
//    @InjectMocks
//    private CreditNoteServiceImpl creditNoteService;
//
//    private MockedStatic<UserContext> userContextMock;
//    private MockedStatic<ASGHelperUtils> asgHelperUtilsMock;
//    private CreditNoteHeaderDto creditNoteDto;
//    private ArCreditNoteHdr savedHeader;
//    private ArCreditNoteDtl glDetail;
//    private ArCreditNoteChargeDtl chargeDetail;
//    //private AuthenticationDetails authDetails;
//
//    @BeforeEach
//    void setUp() {
//        userContextMock = mockStatic(UserContext.class);
//        asgHelperUtilsMock = mockStatic(ASGHelperUtils.class);
//
////        authDetails = AuthenticationDetails.builder()
////                .loggedInUserName("TESTUSER")
////                .loggedInUserPoid(1L)
////                .loggedInGroupPoid(1L)
////                .loggedInCompanyPoid(3L)
////                .build();
//
////        userContextMock.when(UserContext::getCurrentUser).thenReturn(authDetails);
////        userContextMock.when(UserContext::getUserGroupPoid).thenReturn(1L);
////        userContextMock.when(UserContext::getUserCompanyPoid).thenReturn(3L);
////        userContextMock.when(UserContext::getUserPoid).thenReturn(1L);
//
//        asgHelperUtilsMock.when(ASGHelperUtils::getCurrentUser).thenReturn("TESTUSER");
//
////        creditNoteDto = createSampleCreditNoteDto();
////        savedHeader = createSampleHeader();
////        glDetail = createSampleGLDetail();
////        chargeDetail = createSampleChargeDetail();
//    }
//
//    @AfterEach
//    void tearDown() {
//        if (userContextMock != null) {
//            userContextMock.close();
//        }
//        if (asgHelperUtilsMock != null) {
//            asgHelperUtilsMock.close();
//        }
//    }
//
//    @Test
//    void createCreditNote_GeneralType_Success() throws SQLException {
//        creditNoteDto.setRefType("GENERAL");
//        creditNoteDto.setPartyType("SUPPLIER");
//
//        when(creditNoteHdrRepository.saveAndFlush(any(ArCreditNoteHdr.class))).thenReturn(savedHeader);
//        when(creditNoteHdrRepository.findById(anyLong())).thenReturn(Optional.of(savedHeader));
////        when(entityManager.refresh(any())).thenReturn(null);
//        when(creditNoteDtlRepository.findByTransactionPoidOrderByDetRowId(anyLong()))
//                .thenReturn(Collections.singletonList(glDetail));
//        when(creditNoteChargeDtlRepository.findByTransactionPoidOrderByDetRowId(anyLong()))
//                .thenReturn(Collections.singletonList(chargeDetail));
//
//        mockDataSourceForValidation();
//
//        CreditNoteHeaderDto result = creditNoteService.createCreditNote(creditNoteDto);
//
//        assertNotNull(result);
//        assertEquals(savedHeader.getTransactionPoid(), result.getTransactionPoid());
//        verify(creditNoteHdrRepository, times(1)).saveAndFlush(any(ArCreditNoteHdr.class));
//        verify(creditNoteDtlRepository, atLeastOnce()).saveAndFlush(any(ArCreditNoteDtl.class));
//    }
//
//    @Test
//    void createCreditNote_WithBillwiseBreakup_Success() throws SQLException {
//        creditNoteDto.setRefType("GENERAL");
//        CreditNoteGLDetailDto glDto = createSampleGLDetailDto();
//        BillwiseBreakupPopupRequestDto billwiseDto = new BillwiseBreakupPopupRequestDto();
//        billwiseDto.setBillRefType("INVOICE");
//        billwiseDto.setBillRef("INV-001");
//        billwiseDto.setType("DR");
//        billwiseDto.setAmount(BigDecimal.valueOf(1000));
//        glDto.setBreakupList(Collections.singletonList(billwiseDto));
//        creditNoteDto.setGlDetails(Collections.singletonList(glDto));
//
//        when(creditNoteHdrRepository.saveAndFlush(any(ArCreditNoteHdr.class))).thenReturn(savedHeader);
//        when(creditNoteHdrRepository.findById(anyLong())).thenReturn(Optional.of(savedHeader));
////        when(entityManager.refresh(any())).thenReturn(null);
//        when(creditNoteDtlRepository.findByTransactionPoidOrderByDetRowId(anyLong()))
//                .thenReturn(Collections.singletonList(glDetail));
//        when(creditNoteChargeDtlRepository.findByTransactionPoidOrderByDetRowId(anyLong()))
//                .thenReturn(Collections.emptyList());
//
//        mockDataSourceForValidation();
//        doNothing().when(billwiseBreakupService).insertBillwiseBreakup(anyList());
//
//        CreditNoteHeaderDto result = creditNoteService.createCreditNote(creditNoteDto);
//
//        assertNotNull(result);
//        verify(billwiseBreakupService, times(1)).insertBillwiseBreakup(anyList());
//    }
//
//    @Test
//    void createCreditNote_WithCostCenterBreakup_Success() throws SQLException {
//        creditNoteDto.setRefType("GENERAL");
//        CreditNoteGLDetailDto glDto = createSampleGLDetailDto();
//        CostCenterBreakupPopupRequestDto costCenterDto = new CostCenterBreakupPopupRequestDto();
//        costCenterDto.setCostGroup("GROUP1");
//        costCenterDto.setCostPoid("COST001");
//        costCenterDto.setAmount(BigDecimal.valueOf(500));
//        glDto.setCostCenterList(Collections.singletonList(costCenterDto));
//        creditNoteDto.setGlDetails(Collections.singletonList(glDto));
//
//        when(creditNoteHdrRepository.saveAndFlush(any(ArCreditNoteHdr.class))).thenReturn(savedHeader);
//        when(creditNoteHdrRepository.findById(anyLong())).thenReturn(Optional.of(savedHeader));
////        when(entityManager.refresh(any())).thenReturn(null);
//        when(creditNoteDtlRepository.findByTransactionPoidOrderByDetRowId(anyLong()))
//                .thenReturn(Collections.singletonList(glDetail));
//        when(creditNoteChargeDtlRepository.findByTransactionPoidOrderByDetRowId(anyLong()))
//                .thenReturn(Collections.emptyList());
//
//        mockDataSourceForValidation();
//        doNothing().when(costCenterBreakupService).saveCostCenterBreakups(anyList());
//
//        CreditNoteHeaderDto result = creditNoteService.createCreditNote(creditNoteDto);
//
//        assertNotNull(result);
//        verify(costCenterBreakupService, times(1)).saveCostCenterBreakups(anyList());
//    }
//
//    @Test
//    void createCreditNote_ValidationFailure_MissingPartyType() {
//        creditNoteDto.setPartyType(null);
//
//        assertThrows(RuntimeException.class, () -> creditNoteService.createCreditNote(creditNoteDto));
//    }
//
//    @Test
//    void createCreditNote_ValidationFailure_MissingRefType() {
//        creditNoteDto.setRefType(null);
//
//        assertThrows(RuntimeException.class, () -> creditNoteService.createCreditNote(creditNoteDto));
//    }
//
//    @Test
//    void getCreditNoteById_Success() {
//        Long transactionPoid = 1L;
//
//        when(creditNoteHdrRepository.findByTransactionPoidAndDeleted(transactionPoid, "N"))
//                .thenReturn(Optional.of(savedHeader));
//        when(creditNoteDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid))
//                .thenReturn(Collections.singletonList(glDetail));
//        when(creditNoteChargeDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid))
//                .thenReturn(Collections.singletonList(chargeDetail));
//
//        GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse = new GlVoucherLoadBillwiseBreakupResponseDto();
//        billwiseResponse.setLoadBillwiseBreakupResponseDtoList(Collections.emptyList());
//        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
//                .thenReturn(billwiseResponse);
//
//        GlVoucherCostCenterBreakupResponseDto costCenterResponse = new GlVoucherCostCenterBreakupResponseDto();
//        costCenterResponse.setCostBreakupList(Collections.emptyList());
//        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
//                .thenReturn(costCenterResponse);
//
//        CreditNoteHeaderDto result = creditNoteService.getCreditNoteById(transactionPoid);
//
//        assertNotNull(result);
//        assertEquals(transactionPoid, result.getTransactionPoid());
//        verify(creditNoteHdrRepository, times(1)).findByTransactionPoidAndDeleted(transactionPoid, "N");
//    }
//
//    @Test
//    void getCreditNoteById_NotFound_ThrowsException() {
//        Long transactionPoid = 999L;
//
//        when(creditNoteHdrRepository.findByTransactionPoidAndDeleted(transactionPoid, "N"))
//                .thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> creditNoteService.getCreditNoteById(transactionPoid));
//    }
//
//    @Test
//    void updateCreditNote_Success() throws SQLException {
//        Long transactionPoid = 1L;
//        creditNoteDto.setRefType("GENERAL");
//
//        when(creditNoteHdrRepository.findByTransactionPoidAndDeleted(transactionPoid, "N"))
//                .thenReturn(Optional.of(savedHeader));
//        when(creditNoteHdrRepository.save(any(ArCreditNoteHdr.class))).thenReturn(savedHeader);
//        when(creditNoteDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid))
//                .thenReturn(Collections.singletonList(glDetail));
//        when(creditNoteChargeDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid))
//                .thenReturn(Collections.singletonList(chargeDetail));
//
//        mockDataSourceForValidation();
//        doNothing().when(creditNoteDtlRepository).deleteByTransactionPoid(anyLong());
//        doNothing().when(creditNoteChargeDtlRepository).deleteByTransactionPoid(anyLong());
//
//        GlVoucherLoadBillwiseBreakupResponseDto billwiseResponse = new GlVoucherLoadBillwiseBreakupResponseDto();
//        billwiseResponse.setLoadBillwiseBreakupResponseDtoList(Collections.emptyList());
//        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
//                .thenReturn(billwiseResponse);
//
//        GlVoucherCostCenterBreakupResponseDto costCenterResponse = new GlVoucherCostCenterBreakupResponseDto();
//        costCenterResponse.setCostBreakupList(Collections.emptyList());
//        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
//                .thenReturn(costCenterResponse);
//
//        CreditNoteHeaderDto result = creditNoteService.updateCreditNote(transactionPoid, creditNoteDto);
//
//        assertNotNull(result);
//        verify(creditNoteHdrRepository, times(1)).save(any(ArCreditNoteHdr.class));
//        verify(creditNoteDtlRepository, times(1)).deleteByTransactionPoid(transactionPoid);
//    }
//
//    @Test
//    void updateCreditNote_NotFound_ThrowsException() {
//        Long transactionPoid = 999L;
//
//        when(creditNoteHdrRepository.findByTransactionPoidAndDeleted(transactionPoid, "N"))
//                .thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> creditNoteService.updateCreditNote(transactionPoid, creditNoteDto));
//    }
//
//    @Test
//    void deleteCreditNote_Success() throws SQLException {
//        Long transactionPoid = 1L;
//
//        when(creditNoteHdrRepository.findByTransactionPoidAndDeleted(transactionPoid, "N"))
//                .thenReturn(Optional.of(savedHeader));
//        when(creditNoteHdrRepository.save(any(ArCreditNoteHdr.class))).thenReturn(savedHeader);
//
//        mockDataSourceForDeleteValidation();
//
//        creditNoteService.deleteCreditNote(transactionPoid);
//
//        verify(creditNoteHdrRepository, times(1)).save(argThat(header -> "Y".equals(header.getDeleted())));
//    }
//
//    @Test
//    void listCreditNotes_Success() {
//        String docId = "300-111";
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", Collections.emptyList());
//        Pageable pageable = PageRequest.of(0, 10);
//
//        when(creditNoteHdrRepository.findByDeletedOrderByTransactionPoidDesc("N"))
//                .thenReturn(Collections.singletonList(savedHeader));
//
//        Map<String, Object> result = creditNoteService.listCreditNotes(docId, filters,null,null, pageable);
//
//        assertNotNull(result);
//        assertTrue(result.containsKey("content"));
//        assertTrue(result.containsKey("totalElements"));
//        assertTrue(result.containsKey("totalPages"));
//    }
//
//    @Test
//    void listCreditNotes_WithDateFilter_Success() {
//        String docId = "300-111";
//        FilterDto dateFilter = new FilterDto("TRANSACTION_DATE", ">=2024-01-01");
//        FilterRequestDto filters = new FilterRequestDto("AND", "N", Collections.singletonList(dateFilter));
//        Pageable pageable = PageRequest.of(0, 10);
//
//        when(creditNoteHdrRepository.findByDeletedOrderByTransactionPoidDesc("N"))
//                .thenReturn(Collections.singletonList(savedHeader));
//
//        Map<String, Object> result = creditNoteService.listCreditNotes(docId, filters,null,null, pageable);
//
//        assertNotNull(result);
//    }
//
//    @Test
//    void saveBillwiseForGl_Success() {
//        Long transactionPoid = 1L;
//        String docId = "300-111";
//        CreditNoteGLDetailDto glDto = createSampleGLDetailDto();
//        glDto.setDetRowId(1L);
//
//        BillwiseBreakupPopupRequestDto billwiseDto = new BillwiseBreakupPopupRequestDto();
//        billwiseDto.setBillRefType("INVOICE");
//        billwiseDto.setBillRef("INV-001");
//        billwiseDto.setType("DR");
//        billwiseDto.setAmount(BigDecimal.valueOf(1000));
//        glDto.setBreakupList(Collections.singletonList(billwiseDto));
//
//        List<CreditNoteGLDetailDto> glDetails = Collections.singletonList(glDto);
//        doNothing().when(billwiseBreakupService).insertBillwiseBreakup(anyList());
//
//        creditNoteService.saveBillwiseForGl(transactionPoid, glDetails, docId);
//
//        verify(billwiseBreakupService, times(1)).insertBillwiseBreakup(anyList());
//    }
//
//    @Test
//    void saveBillwiseForGl_EmptyList_NoAction() {
//        Long transactionPoid = 1L;
//        String docId = "300-111";
//
//        creditNoteService.saveBillwiseForGl(transactionPoid, Collections.emptyList(), docId);
//
//        verify(billwiseBreakupService, never()).insertBillwiseBreakup(anyList());
//    }
//
//    @Test
//    void getBillwiseForGl_Success() {
//        Long transactionPoid = 1L;
//        Long detRowId = 1L;
//        String docId = "300-111";
//
//        LoadBillwiseBreakupResponseDto loadDto = new LoadBillwiseBreakupResponseDto();
//        loadDto.setMainDetRowId(detRowId);
//        loadDto.setBillDetRowId(1L);
//        loadDto.setBillRefType("INVOICE");
//        loadDto.setBillRef("INV-001");
//        loadDto.setDrAmt(BigDecimal.valueOf(1000));
//        loadDto.setCrAmt(BigDecimal.ZERO);
//
//        GlVoucherLoadBillwiseBreakupResponseDto response = new GlVoucherLoadBillwiseBreakupResponseDto();
//        response.setLoadBillwiseBreakupResponseDtoList(Collections.singletonList(loadDto));
//
//        when(billwiseBreakupService.loadBillwiseBreakup(anyLong(), anyLong(), anyString(), anyLong()))
//                .thenReturn(response);
//
//        List<BillwiseBreakupDtoCreditNotePopUp> result = creditNoteService.getBillwiseForGl(transactionPoid, detRowId, docId);
//
//        assertNotNull(result);
//        assertFalse(result.isEmpty());
//    }
//
//    @Test
//    void saveCostCenterForGl_Success() {
//        Long transactionPoid = 1L;
//        String docId = "300-111";
//        CreditNoteGLDetailDto glDto = createSampleGLDetailDto();
//        glDto.setDetRowId(1L);
//
//        CostCenterBreakupPopupRequestDto costCenterDto = new CostCenterBreakupPopupRequestDto();
//        costCenterDto.setCostGroup("GROUP1");
//        costCenterDto.setCostPoid("COST001");
//        costCenterDto.setAmount(BigDecimal.valueOf(500));
//        glDto.setCostCenterList(Collections.singletonList(costCenterDto));
//
//        List<CreditNoteGLDetailDto> glDetails = Collections.singletonList(glDto);
//        doNothing().when(costCenterBreakupService).saveCostCenterBreakups(anyList());
//
//        creditNoteService.saveCostCenterForGl(transactionPoid, glDetails, docId);
//
//        verify(costCenterBreakupService, times(1)).saveCostCenterBreakups(anyList());
//    }
//
//    @Test
//    void saveCostCenterForGl_EmptyList_NoAction() {
//        Long transactionPoid = 1L;
//        String docId = "300-111";
//
//        creditNoteService.saveCostCenterForGl(transactionPoid, Collections.emptyList(), docId);
//
//        verify(costCenterBreakupService, never()).saveCostCenterBreakups(anyList());
//    }
//
//    @Test
//    void getCostCenterForGl_Success() {
//        Long transactionPoid = 1L;
//        Long detRowId = 1L;
//        String docId = "300-111";
//
//        CostCenterBreakupResponseDto costDto = new CostCenterBreakupResponseDto();
//        costDto.setMainDetRowId(detRowId);
//        costDto.setCostDetRowId(1L);
//        costDto.setCostGroup("GROUP1");
//        costDto.setCostPoid("COST001");
//        costDto.setAmount(500L);
//        costDto.setDescription("Test Cost Center");
//
//        GlVoucherCostCenterBreakupResponseDto response = new GlVoucherCostCenterBreakupResponseDto();
//        response.setCostBreakupList(Collections.singletonList(costDto));
//
//        when(costCenterBreakupService.loadCostCenterData(anyString(), anyLong(), anyLong(), anyLong(), anyLong()))
//                .thenReturn(response);
//
//        List<CostCenterBreakupDto> result = creditNoteService.getCostCenterForGl(transactionPoid, detRowId, docId);
//
//        assertNotNull(result);
//        assertFalse(result.isEmpty());
//    }
//
//    @Test
//    void getFFInvoiceCharges_Success() throws SQLException {
//        Long refNo = 123L;
//        Connection connection = mock(Connection.class);
//        CallableStatement callableStatement = mock(CallableStatement.class);
//        ResultSet resultSet = mock(ResultSet.class);
//
//        when(dataSource.getConnection()).thenReturn(connection);
//        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
//        when(callableStatement.execute()).thenReturn(true);
//        when(callableStatement.getObject(anyInt())).thenReturn(resultSet);
//        when(resultSet.next()).thenReturn(true, false);
//        when(resultSet.getString("CHARGE_CODE")).thenReturn("CHG001");
//        when(resultSet.getBigDecimal("CHARGE_AMOUNT")).thenReturn(BigDecimal.valueOf(1000));
//        when(resultSet.getBigDecimal("TAX_AMOUNT")).thenReturn(BigDecimal.valueOf(100));
//        when(resultSet.getBigDecimal("TOTAL_AMOUNT")).thenReturn(BigDecimal.valueOf(1100));
//        when(resultSet.getBigDecimal("CHARGE_COST_AMOUNT")).thenReturn(BigDecimal.valueOf(900));
//        when(resultSet.getBigDecimal("FF_AMOUNT")).thenReturn(BigDecimal.valueOf(100));
//        when(resultSet.getString("REF_DOC_ID")).thenReturn("DOC001");
//        when(resultSet.getLong("REF_DOC_POID")).thenReturn(1L);
//
//        List<UniversalChargeDetailDto> result = creditNoteService.getFFInvoiceCharges(refNo, 1L);
//
//        assertNotNull(result);
//        assertFalse(result.isEmpty());
//    }
//
//    @Test
//    void getSHInvoiceCharges_Success() throws SQLException {
//        Long refNo = 123L;
//        Long partyPoid = 1L;
//        Connection connection = mock(Connection.class);
//        CallableStatement callableStatement = mock(CallableStatement.class);
//        ResultSet resultSet = mock(ResultSet.class);
//
//        when(dataSource.getConnection()).thenReturn(connection);
//        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
//        when(callableStatement.execute()).thenReturn(true);
//        when(callableStatement.getObject(anyInt())).thenReturn(resultSet);
//        when(resultSet.next()).thenReturn(true, false);
//        when(resultSet.getString("CHARGE_CODE")).thenReturn("CHG001");
//        when(resultSet.getBigDecimal("CHARGE_AMOUNT")).thenReturn(BigDecimal.valueOf(1000));
//        when(resultSet.getBigDecimal("TAX_AMOUNT")).thenReturn(BigDecimal.valueOf(100));
//        when(resultSet.getBigDecimal("TOTAL_AMOUNT")).thenReturn(BigDecimal.valueOf(1100));
//        when(resultSet.getBigDecimal("CHARGE_COST_AMOUNT")).thenReturn(BigDecimal.valueOf(900));
//        when(resultSet.getString("REF_DOC_ID")).thenReturn("DOC001");
//        when(resultSet.getLong("REF_DOC_POID")).thenReturn(1L);
//
//        List<UniversalChargeDetailDto> result = creditNoteService.getSHInvoiceCharges(refNo, partyPoid);
//
//        assertNotNull(result);
//        assertFalse(result.isEmpty());
//    }
//
////    private CreditNoteHeaderDto createSampleCreditNoteDto() {
////        CreditNoteHeaderDto dto = new CreditNoteHeaderDto();
////        dto.setPartyType("SUPPLIER");
////        dto.setRefType("GENERAL");
////        dto.setPostingNarration("Test Narration");
////        dto.setGrandTotal(BigDecimal.valueOf(1000));
////        dto.setCurrencyCode("USD");
////        dto.setCurrencyRate(BigDecimal.ONE);
////        dto.setCreditPeriod(30L);
////        LovGetListDto party = new LovGetListDto(1L);
////        dto.setPartyDet(party);
////        return dto;
////    }
//
//    private ArCreditNoteHdr createSampleHeader() {
//        return ArCreditNoteHdr.builder()
//                .transactionPoid(1L)
//                .docRef("CN-001")
//                .transactionDate(LocalDate.now())
//                .groupPoid(1L)
//                .companyPoid(3L)
//                .partyType("SUPPLIER")
//                .partyPoid(1L)
//                .refType("GENERAL")
//                .currencyCode("USD")
//                .currencyRate(BigDecimal.ONE)
//                .grandTotal(BigDecimal.valueOf(1000))
//                .postingNarration("Test Narration")
//                .deleted("N")
//                .createdBy("TESTUSER")
//                .createdDate(Timestamp.from(Instant.now()))
//                .build();
//    }
//
//    private ArCreditNoteDtl createSampleGLDetail() {
//        return ArCreditNoteDtl.builder()
//                .transactionPoid(1L)
//                .detRowId(1L)
//                .type("DR")
//                .companyPoid(3L)
//                .glPoid(100L)
//                .drAmt(BigDecimal.valueOf(1000))
//                .crAmt(BigDecimal.ZERO)
//                .taxPoid(1L)
//                .taxPercentage(BigDecimal.valueOf(5))
//                .taxAmount(BigDecimal.valueOf(50))
//                .totalAmount(BigDecimal.valueOf(1050))
//                .remarks("Test GL Detail")
//                .createdBy("TESTUSER")
//                .createdDate(Timestamp.from(Instant.now()))
//                .build();
//    }
//
//    private ArCreditNoteChargeDtl createSampleChargeDetail() {
//        ArCreditNoteChargeDtl charge = new ArCreditNoteChargeDtl();
//        charge.setTransactionPoid(1L);
//        charge.setDetRowId(1L);
//        charge.setChargePoid(1L);
//        charge.setChargeAmount(BigDecimal.valueOf(500));
//        charge.setTaxAmount(BigDecimal.valueOf(25));
//        charge.setTotalAmount(BigDecimal.valueOf(525));
//        charge.setRemarks("Test Charge");
//        charge.setCreatedBy("TESTUSER");
//        charge.setCreatedDate(Timestamp.from(Instant.now()));
//        return charge;
//    }
//
//    private CreditNoteGLDetailDto createSampleGLDetailDto() {
//        CreditNoteGLDetailDto dto = new CreditNoteGLDetailDto();
//        dto.setDetRowId(1L);
//        dto.setType("DR");
//        dto.setGlPoid(100L);
//        dto.setGlPoid(456L);
//        dto.setDrAmt(BigDecimal.valueOf(1000));
//        dto.setCrAmt(BigDecimal.ZERO);
//        dto.setTaxPoid(2343L);
//        dto.setTaxPoid(1L);
//        dto.setTaxPercentage(BigDecimal.valueOf(5));
//        dto.setTaxAmount(BigDecimal.valueOf(50));
//        dto.setTotalAmount(BigDecimal.valueOf(1050));
//        dto.setRemarks("Test GL Detail");
//        return dto;
//    }
//
//    private void mockDataSourceForValidation() throws SQLException {
//        Connection connection = mock(Connection.class);
//        CallableStatement callableStatement = mock(CallableStatement.class);
//        PreparedStatement preparedStatement = mock(PreparedStatement.class);
//        ResultSet resultSet = mock(ResultSet.class);
//
//        when(dataSource.getConnection()).thenReturn(connection);
//        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
//        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
//        when(callableStatement.execute()).thenReturn(true);
//        when(callableStatement.getString(anyInt())).thenReturn("SUCCESS");
//        when(preparedStatement.executeQuery()).thenReturn(resultSet);
//        when(resultSet.next()).thenReturn(false);
//    }
//
//    private void mockDataSourceForDeleteValidation() throws SQLException {
//        Connection connection = mock(Connection.class);
//        CallableStatement callableStatement = mock(CallableStatement.class);
//
//        when(dataSource.getConnection()).thenReturn(connection);
//        when(connection.prepareCall(anyString())).thenReturn(callableStatement);
//        when(callableStatement.execute()).thenReturn(true);
//    }
//}
//
//
