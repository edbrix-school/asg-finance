package com.asg.finance.journalvoucher.service.impl;

import com.asg.common.lib.dto.*;

import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
import com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.*;
import com.asg.finance.dto.*;
import com.asg.finance.entity.*;
import com.asg.finance.repository.*;
import com.asg.finance.repository.master.FixedAssetRepository;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.finance.service.impl.JournalVoucherServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Pageable;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalVoucherServiceImplTest {

    @Mock private GlJournalVoucherHdrRepository glJournalVoucherHdrRepository;
    @Mock private GlJournalVoucherDtlRepository glJournalVoucherDtlRepository;
    @Mock private GlJournalVoucherAssetDtlRepository glJournalVoucherAssetDtlRepository;
    @Mock private GlJournalFaCapitalizationRepository glJournalFaCapitalizationRepository;
    @Mock private FixedAssetRepository fixedAssetRepository;
    @Mock private GLMasterRepository glMasterRepository;
    @Mock private CostCenterBreakupService costCenterBreakupService;
    @Mock private BillwiseBreakupService billwiseBreakupService;
    @Mock private LovDataService lovDataService;
    @Mock private DocumentSearchService documentService;
    @Mock private DocumentDeleteService documentDeleteService;
    @Mock private PrintService printService;
    @Mock private DataSource dataSource;
    @Mock private LoggingService loggingService;
    @Mock private ApplicationContext applicationContext;

    @InjectMocks
    private JournalVoucherServiceImpl journalVoucherService;

    private MockedStatic<UserContext> mockedUserContext;
    private static final String DOC_ID = "400-100";

    @BeforeEach
    void setUp() {
        mockedUserContext = mockStatic(UserContext.class);
        mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
        mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
        mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
        mockedUserContext.when(UserContext::getDocumentId).thenReturn(DOC_ID);
        
        lenient().when(applicationContext.getBean(JournalVoucherServiceImpl.class)).thenReturn(journalVoucherService);
    }

    @AfterEach
    void tearDown() {
        mockedUserContext.close();
    }

    @Test
    void createJournalVoucher_General_Success() {
        JournalVoucherRequest request = buildGeneralRequest();
        GlJournalVoucherHdr hdr = new GlJournalVoucherHdr();
        hdr.setTransactionPoid(100L);
        hdr.setDocRef("JV-001");

        when(glJournalVoucherHdrRepository.save(any())).thenReturn(hdr);
        when(glJournalVoucherDtlRepository.getMaxDetRowIdByTransactionPoid(100L)).thenReturn(0L);

        JournalVoucherResponse response = journalVoucherService.createJournalVoucher(request, DOC_ID);

        assertNotNull(response);
        assertEquals(100L, response.getTransactionPoid());
        assertEquals("JV-001", response.getDocRef());
        verify(glJournalVoucherHdrRepository).save(any());
        verify(glJournalVoucherDtlRepository, atLeastOnce()).save(any());
        verify(loggingService, atLeastOnce()).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());
    }

    @Test
    void createJournalVoucher_AssetDisposal_Success() {
        JournalVoucherRequest request = buildAssetDisposalRequest();
        GlJournalVoucherHdr hdr = new GlJournalVoucherHdr();
        hdr.setTransactionPoid(101L);
        hdr.setDocRef("JV-010");

        when(glJournalVoucherHdrRepository.save(any())).thenReturn(hdr);
        when(fixedAssetRepository.existsById(anyLong())).thenReturn(true);
        when(glJournalVoucherHdrRepository.fetchAssetDepreciationDetails(anyLong()))
                .thenReturn(Collections.singletonList(new JournalVoucherAssetDetailDto()));

        JournalVoucherResponse response = journalVoucherService.createJournalVoucher(request, DOC_ID);

        assertNotNull(response);
        verify(glJournalVoucherAssetDtlRepository, atLeastOnce()).save(any());
        verify(glJournalVoucherHdrRepository, times(2)).updateAssetDetail(101L);
    }

    @Test
    void createJournalVoucher_AssetCapitalization_Success() {
        JournalVoucherRequest request = buildAssetCapitalizationRequest();
        GlJournalVoucherHdr hdr = new GlJournalVoucherHdr();
        hdr.setTransactionPoid(102L);

        when(glJournalVoucherHdrRepository.save(any())).thenReturn(hdr);
        when(fixedAssetRepository.existsById(anyLong())).thenReturn(true);
        when(glJournalVoucherHdrRepository.fetchFixedAssetDetails(anyLong()))
                .thenReturn(Collections.singletonList(new JournalVoucherCapitalizationDto()));
        
        GLMaster glMaster = new GLMaster();
        glMaster.setControlAcNature("FIXED_ASSET");
        when(glMasterRepository.findByGlPoid(anyLong())).thenReturn(Optional.of(glMaster));

        JournalVoucherResponse response = journalVoucherService.createJournalVoucher(request, DOC_ID);

        assertNotNull(response);
        verify(glJournalFaCapitalizationRepository, atLeastOnce()).save(any());
    }

    @Test
    void updateJournalVoucher_General_Success() {
        Long poid = 100L;
        JournalVoucherRequest request = buildGeneralRequest();
        GlJournalVoucherHdr existing = new GlJournalVoucherHdr();
        existing.setTransactionPoid(poid);
        existing.setRefType("GENERAL");

        when(glJournalVoucherHdrRepository.findByTransactionPoid(poid)).thenReturn(Optional.of(existing));
        when(glJournalVoucherHdrRepository.save(any())).thenReturn(existing);

        JournalVoucherResponse response = journalVoucherService.updateJournalVoucher(poid, request, DOC_ID);

        assertNotNull(response);
        verify(glJournalVoucherHdrRepository).save(existing);
    }

    @Test
    void getJournalVoucherById_General_Success() {
        Long poid = 100L;
        GlJournalVoucherHdr hdr = new GlJournalVoucherHdr();
        hdr.setTransactionPoid(poid);
        hdr.setRefType("GENERAL");
        hdr.setMultiCompany("N");

        when(glJournalVoucherHdrRepository.findByTransactionPoid(poid)).thenReturn(Optional.of(hdr));
        when(glJournalVoucherDtlRepository.findByTransactionPoid(poid)).thenReturn(new ArrayList<>());

        JournalVoucherDetailResponse response = journalVoucherService.getJournalVoucherById(poid);

        assertNotNull(response);
        assertEquals("GENERAL", response.getRefType());
    }

    @Test
    void deleteJournalVoucher_Success() {
        Long poid = 100L;
        GlJournalVoucherHdr hdr = new GlJournalVoucherHdr();
        hdr.setTransactionPoid(poid);
        hdr.setTransactionDate(LocalDate.now());

        when(glJournalVoucherHdrRepository.findByTransactionPoid(poid)).thenReturn(Optional.of(hdr));

        journalVoucherService.deleteJournalVoucher(poid, new DeleteReasonDto());

        verify(documentDeleteService).deleteDocument(eq(poid), anyString(), anyString(), any(), any());
    }

    @Test
    void deleteJournalVoucher_ThrowsException_IfPosted() {
        Long poid = 100L;
        GlJournalVoucherHdr hdr = new GlJournalVoucherHdr();
        hdr.setPostedFromDocId("POSTED");

        when(glJournalVoucherHdrRepository.findByTransactionPoid(poid)).thenReturn(Optional.of(hdr));

        assertThrows(IllegalStateException.class, () -> journalVoucherService.deleteJournalVoucher(poid, new DeleteReasonDto()));
    }

    @Test
    void listJournalVouchers_Success() {
        FilterRequestDto filters = new FilterRequestDto("AND", "N", new ArrayList<>());
        Pageable pageable = mock(Pageable.class);
        RawSearchResult raw = new RawSearchResult(new ArrayList<>(), new HashMap<>(), 0L);

        when(documentService.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(raw);

        Map<String, Object> result = journalVoucherService.listJournalVouchers(DOC_ID, filters, null, null, pageable);

        assertNotNull(result);
    }

    @Test
    void validateJournalVoucher_ThrowsException_FutureDate() {
        JournalVoucherRequest request = buildGeneralRequest();
        request.setTransactionDate(LocalDate.now().plusDays(1));

        assertThrows(IllegalArgumentException.class, () -> journalVoucherService.createJournalVoucher(request, DOC_ID));
    }

    @Test
    void createJournalVoucher_ThrowsException_MismatchBalance() {
        JournalVoucherRequest request = buildGeneralRequest();
        request.getGlDetails().get(0).setDrAmt(BigDecimal.valueOf(2000)); // Header amount is 1000

        assertThrows(IllegalArgumentException.class, () -> journalVoucherService.createJournalVoucher(request, DOC_ID));
    }

    @Test
    void createJournalVoucher_WithBreakups_Success() {
        JournalVoucherRequest request = buildGeneralRequest();
        request.getGlDetails().get(0).setCostCenterBreakup(Collections.singletonList(
                CostCenterBreakupPopupRequestDto.builder().costGroup("DEPT").costPoid("P1").amount(BigDecimal.valueOf(1000)).actionType("isCreated").build()
        ));
        request.getGlDetails().get(1).setBillWiseBreakup(Collections.singletonList(
                BillwiseBreakupPopupRequestDto.builder().billRef("B1").amount(BigDecimal.valueOf(1000)).type("Cr").actionType("isCreated").build()
        ));

        GlJournalVoucherHdr hdr = new GlJournalVoucherHdr();
        hdr.setTransactionPoid(100L);
        when(glJournalVoucherHdrRepository.save(any())).thenReturn(hdr);

        journalVoucherService.createJournalVoucher(request, DOC_ID);

        verify(costCenterBreakupService, atLeastOnce()).saveCostCenterBreakups(anyList());
        verify(billwiseBreakupService, atLeastOnce()).insertBillwiseBreakup(anyList());
    }

    @Test
    void updateJournalVoucher_WithActionTypes_Success() {
        Long poid = 100L;
        JournalVoucherRequest request = buildGeneralRequest();
        request.getGlDetails().get(0).setActionType("ISCREATED");
        request.getGlDetails().get(1).setActionType("ISUPDATED");
        request.getGlDetails().get(1).setDetRowId(2L);

        GlJournalVoucherHdr existing = new GlJournalVoucherHdr();
        existing.setTransactionPoid(poid);
        existing.setRefType("GENERAL");

        when(glJournalVoucherHdrRepository.findByTransactionPoid(poid)).thenReturn(Optional.of(existing));
        when(glJournalVoucherDtlRepository.findById(any())).thenReturn(Optional.of(new GlJournalVoucherDtl()));
        when(glJournalVoucherHdrRepository.save(any())).thenReturn(existing);

        journalVoucherService.updateJournalVoucher(poid, request, DOC_ID);

        verify(glJournalVoucherDtlRepository, atLeastOnce()).save(any());
    }

    @Test
    void updateJournalVoucher_DeleteDetail_Success() {
        Long poid = 100L;
        JournalVoucherRequest request = buildGeneralRequest();
        request.getGlDetails().get(0).setActionType("ISDELETED");
        request.getGlDetails().get(0).setDetRowId(1L);

        GlJournalVoucherHdr existing = new GlJournalVoucherHdr();
        existing.setTransactionPoid(poid);
        existing.setRefType("GENERAL");

        when(glJournalVoucherHdrRepository.findByTransactionPoid(poid)).thenReturn(Optional.of(existing));

        journalVoucherService.updateJournalVoucher(poid, request, DOC_ID);

        verify(glJournalVoucherDtlRepository, atLeastOnce()).deleteById(any());
    }

    @Test
    void getAssetDepreciationDetails_Success() {
        Long poid = 123L;
        when(glJournalVoucherHdrRepository.fetchAssetDepreciationDetails(poid))
                .thenReturn(Collections.singletonList(new JournalVoucherAssetDetailDto()));

        JournalVoucherAssetDetailDto result = journalVoucherService.getAssetDepreciationDetails(poid);

        assertNotNull(result);
    }

    @Test
    void getAssetCapitalizationDetails_Success() {
        Long poid = 1234L;
        when(glJournalVoucherHdrRepository.fetchFixedAssetDetails(poid))
                .thenReturn(Collections.singletonList(new JournalVoucherCapitalizationDto()));

        JournalVoucherCapitalizationDto result = journalVoucherService.getAssetCapitalizationDetails(poid);

        assertNotNull(result);
    }

    @Test
    void getAssetDepreciationDetails_ThrowsException_NotFound() {
        when(glJournalVoucherHdrRepository.fetchAssetDepreciationDetails(any())).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> journalVoucherService.getAssetDepreciationDetails(1L));
    }

    @Test
    void createJournalVoucher_MultiCompany_Success() {
        JournalVoucherRequest request = buildGeneralRequest();
        request.setMultiCompany(true);
        request.getGlDetails().get(0).setCompanyPoid(2L);
        request.getGlDetails().get(1).setCompanyPoid(3L);

        GlJournalVoucherHdr hdr = new GlJournalVoucherHdr();
        hdr.setTransactionPoid(100L);
        when(glJournalVoucherHdrRepository.save(any())).thenReturn(hdr);

        journalVoucherService.createJournalVoucher(request, DOC_ID);

        verify(glJournalVoucherHdrRepository).save(argThat(h -> "Y".equals(h.getMultiCompany())));
    }

    @Test
    void validateJournalVoucher_ThrowsException_InvalidRefType() {
        JournalVoucherRequest request = buildGeneralRequest();
        request.setRefType("INVALID");
        assertThrows(IllegalArgumentException.class, () -> journalVoucherService.createJournalVoucher(request, DOC_ID));
    }

    @Test
    void validateJournalVoucher_ThrowsException_MismatchHeaderAmount() {
        JournalVoucherRequest request = buildGeneralRequest();
        request.setAmount(BigDecimal.valueOf(5000)); // GL details sum to 1000
        request.setBhdAmount(BigDecimal.valueOf(5000));
        assertThrows(IllegalArgumentException.class, () -> journalVoucherService.createJournalVoucher(request, DOC_ID));
    }
    @Test
    void getJournalVoucherById_Complex_Success() {
        Long poid = 100L;
        GlJournalVoucherHdr hdr = new GlJournalVoucherHdr();
        hdr.setTransactionPoid(poid);
        hdr.setRefType("GENERAL");
        hdr.setMultiCompany("Y");

        GlJournalVoucherDtl dtl = new GlJournalVoucherDtl();
        dtl.setDetRowId(1L);
        dtl.setGlPoid(1L);
        dtl.setDrAmt(BigDecimal.valueOf(1000));
        dtl.setCrAmt(BigDecimal.ZERO);

        when(glJournalVoucherHdrRepository.findByTransactionPoid(poid)).thenReturn(Optional.of(hdr));
        when(glJournalVoucherDtlRepository.findByTransactionPoid(poid)).thenReturn(Collections.singletonList(dtl));
        
        GlVoucherCostCenterBreakupResponseDto ccResponse = new GlVoucherCostCenterBreakupResponseDto();
        CostCenterBreakupResponseDto ccDto = new CostCenterBreakupResponseDto();
        ccDto.setMainDetRowId(1L);
        ccResponse.setCostBreakupList(Collections.singletonList(ccDto));
        when(costCenterBreakupService.loadCostCenterData(any(), any(), any(), any(), any())).thenReturn(ccResponse);

        GlVoucherLoadBillwiseBreakupResponseDto bwResponse = new GlVoucherLoadBillwiseBreakupResponseDto();
        LoadBillwiseBreakupResponseDto bwDto = new LoadBillwiseBreakupResponseDto();
        bwDto.setMainDetRowId(1L);
        bwResponse.setLoadBillwiseBreakupResponseDtoList(Collections.singletonList(bwDto));
        when(billwiseBreakupService.loadBillwiseBreakup(any(), any(), any(), any())).thenReturn(bwResponse);

        JournalVoucherDetailResponse response = journalVoucherService.getJournalVoucherById(poid);

        assertNotNull(response);
        assertEquals(1, response.getGlDetails().size());
        assertEquals(1, response.getGlDetails().get(0).getCostCenterBreakup().size());
        assertEquals(1, response.getGlDetails().get(0).getBillWiseBreakup().size());
    }

    @Test
    void updateJournalVoucher_AssetDisposal_Success() {
        Long poid = 101L;
        JournalVoucherRequest request = buildAssetDisposalRequest();
        request.getAssetDetails().get(0).setDetRowId(1L);
        request.getAssetDetails().get(0).setActionType("ISUPDATED");

        GlJournalVoucherHdr existing = new GlJournalVoucherHdr();
        existing.setTransactionPoid(poid);
        existing.setRefType("ASSET_DISPOSAL");

        when(glJournalVoucherHdrRepository.findByTransactionPoid(poid)).thenReturn(Optional.of(existing));
        when(glJournalVoucherAssetDtlRepository.findById(any())).thenReturn(Optional.of(new GlJournalVoucherAssetDtl()));
        when(glJournalVoucherHdrRepository.save(any())).thenReturn(existing);
        when(fixedAssetRepository.existsById(anyLong())).thenReturn(true);
        when(glJournalVoucherHdrRepository.fetchAssetDepreciationDetails(anyLong()))
                .thenReturn(Collections.singletonList(new JournalVoucherAssetDetailDto()));

        journalVoucherService.updateJournalVoucher(poid, request, DOC_ID);

        verify(glJournalVoucherAssetDtlRepository).save(any());
    }

    @Test
    void updateJournalVoucher_AssetCapitalization_Success() {
        Long poid = 102L;
        JournalVoucherRequest request = buildAssetCapitalizationRequest();
        request.getAssetCapitalization().get(0).setDetRowId(1L);
        request.getAssetCapitalization().get(0).setActionType("ISUPDATED");

        GlJournalVoucherHdr existing = new GlJournalVoucherHdr();
        existing.setTransactionPoid(poid);
        existing.setRefType("ASSET_CAPITALIZATION");

        when(glJournalVoucherHdrRepository.findByTransactionPoid(poid)).thenReturn(Optional.of(existing));
        when(glJournalFaCapitalizationRepository.findById(any())).thenReturn(Optional.of(new GlJournalFaCapitalization()));
        when(glJournalVoucherHdrRepository.save(any())).thenReturn(existing);
        when(fixedAssetRepository.existsById(anyLong())).thenReturn(true);
        when(glJournalVoucherHdrRepository.fetchFixedAssetDetails(anyLong()))
                .thenReturn(Collections.singletonList(new JournalVoucherCapitalizationDto()));
        
        GLMaster glMaster = new GLMaster();
        glMaster.setControlAcNature("FIXED_ASSET");
        when(glMasterRepository.findByGlPoid(anyLong())).thenReturn(Optional.of(glMaster));

        journalVoucherService.updateJournalVoucher(poid, request, DOC_ID);

        verify(glJournalFaCapitalizationRepository).save(any());
    }

    @Test
    void print_Success() throws Exception {
        Long poid = 100L;
        when(printService.buildBaseParams(eq(poid), any())).thenReturn(new HashMap<>());
        when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[0]);

        byte[] result = journalVoucherService.print(poid);

        assertNotNull(result);
        verify(printService).fillReportToPdf(any(), any(), any());
    }

    private JournalVoucherRequest buildGeneralRequest() {
        return JournalVoucherRequest.builder()
                .transactionDate(LocalDate.now())
                .refType("GENERAL")
                .amount(BigDecimal.valueOf(1000))
                .bhdAmount(BigDecimal.valueOf(1000))
                .postingNarration("Test")
                .confidentialRemarks("Secret")
                .glDetails(Arrays.asList(
                        JournalVoucherGlDetailDto.builder().glPoid(1L).type("Dr").drAmt(BigDecimal.valueOf(1000)).crAmt(BigDecimal.ZERO).build(),
                        JournalVoucherGlDetailDto.builder().glPoid(2L).type("Cr").drAmt(BigDecimal.ZERO).crAmt(BigDecimal.valueOf(1000)).build()
                ))
                .build();
    }

    private JournalVoucherRequest buildAssetDisposalRequest() {
        return JournalVoucherRequest.builder()
                .transactionDate(LocalDate.now())
                .refType("ASSET_DISPOSAL")
                .amount(BigDecimal.valueOf(5000))
                .bhdAmount(BigDecimal.valueOf(5000))
                .postingNarration("Disposal")
                .confidentialRemarks("Secret")
                .wdvAccountGl(3001L)
                .assetDetails(Collections.singletonList(
                        JournalVoucherAssetDetailDto.builder().faPoid(123L).process("DISPOSAL").build()
                ))
                .build();
    }

    private JournalVoucherRequest buildAssetCapitalizationRequest() {
        return JournalVoucherRequest.builder()
                .transactionDate(LocalDate.now())
                .refType("ASSET_CAPITALIZATION")
                .amount(BigDecimal.valueOf(25000))
                .bhdAmount(BigDecimal.valueOf(25000))
                .postingNarration("Cap")
                .confidentialRemarks("Secret")
                .assetCapitalization(Collections.singletonList(
                        JournalVoucherCapitalizationDto.builder().faPoid(1234L).assetValue(BigDecimal.valueOf(25000)).build()
                ))
                .glDetails(Arrays.asList(
                        JournalVoucherGlDetailDto.builder().glPoid(5001L).type("Dr").drAmt(BigDecimal.valueOf(25000)).crAmt(BigDecimal.ZERO).build(),
                        JournalVoucherGlDetailDto.builder().glPoid(2001L).type("Cr").drAmt(BigDecimal.ZERO).crAmt(BigDecimal.valueOf(25000)).build()
                ))
                .build();
    }
}
