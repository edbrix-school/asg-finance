//package com.asg.finance.service;
//
//import com.asg.common.lib.dto.response.GlVoucherLoadBillwiseBreakupResponseDto;
//import com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto;
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.service.DocumentSearchService;
//import com.asg.finance.dto.*;
//import com.asg.finance.entity.ArDebitNoteChargeDtl;
//import com.asg.finance.entity.ArDebitNoteDtl;
//import com.asg.finance.entity.ArDebitNoteHdr;
//import com.asg.finance.repository.*;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.*;
//
//import java.math.BigDecimal;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class DebitNoteServiceImplTest {
//
//    @InjectMocks
//    private DebitNoteServiceImpl debitNoteService;
//
//    @Mock private ArDebitNoteHdrRepository hdrRepo;
//    @Mock private ArDebitNoteDtlRepository dtlRepo;
//    @Mock private ArDebitNoteChargeDtlRepository chargeRepo;
//    @Mock private SupplierMasterRepository supplierRepo;
//    @Mock private DocumentSearchService documentService;
//    @Mock private DebitNoteCustomRepository customRepo;
//    @Mock private BillwiseBreakupService billwiseBreakupService;
//    @Mock private CostCenterBreakupService costCenterBreakupService;
//
//    @BeforeEach
//    void setup() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    // ---------------------------------------------------------------------
//    // createDebitNote()
//    // ---------------------------------------------------------------------
//    @Test
//    void testCreateDebitNote_success() {
//
//        DebitNoteHeaderDto request = new DebitNoteHeaderDto();
//        request.setRefType("FDA");
//        request.setPartyType("SUPPLIER");
//        request.setPartyPoid(123L);
//        request.setGlDetails(List.of(mockGlDetail()));
//        request.setChargeDetails(List.of(mockChargeDetail()));
//
//        ArDebitNoteHdr savedHdr = new ArDebitNoteHdr();
//        savedHdr.setTransactionPoid(1001L);
//        savedHdr.setRefType("FDA");
//
//        when(hdrRepo.getNextSequenceValue()).thenReturn(1001L);
//        when(hdrRepo.saveAndFlush(any())).thenReturn(savedHdr);
//
//        ArDebitNoteDtl glDtl = new ArDebitNoteDtl();
//        glDtl.setTransactionPoid(1001L);
//        glDtl.setDetRowId(1L);
//        when(dtlRepo.findByTransactionPoid(1001L)).thenReturn(List.of(glDtl));
//
//        ArDebitNoteChargeDtl chDtl = new ArDebitNoteChargeDtl();
//        chDtl.setTransactionPoid(1001L);
//        chDtl.setDetRowId(1L);
//        when(chargeRepo.findByTransactionPoid(1001L)).thenReturn(List.of(chDtl));
//
//        DebitNoteHeaderDto result = debitNoteService.createDebitNote(request);
//
//        assertNotNull(result);
//        assertEquals(1001L, result.getTransactionPoid());
//        verify(dtlRepo, times(1)).save(any());
//        verify(chargeRepo, times(1)).save(any());
//    }
//
//    // ---------------------------------------------------------------------
//    // updateDebitNote()
//    // ---------------------------------------------------------------------
//    @Test
//    void testUpdateDebitNote_success() {
//
//        Long poid = 1001L;
//
//        ArDebitNoteHdr existing = new ArDebitNoteHdr();
//        existing.setTransactionPoid(poid);
//        existing.setRefType("FDA");
//
//        DebitNoteHeaderDto updateDto = new DebitNoteHeaderDto();
//        updateDto.setRefType("FDA");
//        updateDto.setGlDetails(List.of(mockGlDetail()));
//        updateDto.setChargeDetails(List.of(mockChargeDetail()));
//
//        when(hdrRepo.findById(poid)).thenReturn(Optional.of(existing));
//
//        List<ArDebitNoteDtl> glList = List.of(new ArDebitNoteDtl());
//        when(dtlRepo.findByTransactionPoid(poid)).thenReturn(glList);
//
//        List<ArDebitNoteChargeDtl> chList = List.of(new ArDebitNoteChargeDtl());
//        when(chargeRepo.findByTransactionPoid(poid)).thenReturn(chList);
//
//        DebitNoteHeaderDto result = debitNoteService.updateDebitNote(poid, updateDto);
//
//        assertNotNull(result);
//        verify(dtlRepo, times(1)).deleteByTransactionPoid(poid);
//        verify(chargeRepo, times(1)).deleteByTransactionPoid(poid);
//    }
//
//    // ---------------------------------------------------------------------
//    // getDebitNote()
//    // ---------------------------------------------------------------------
//    @Test
//    void testGetDebitNote_success() {
//
//        Long poid = 1001L;
//
//        ArDebitNoteHdr hdr = new ArDebitNoteHdr();
//        hdr.setTransactionPoid(poid);
//        hdr.setRefType("FDA");
//
//        when(hdrRepo.findById(poid)).thenReturn(Optional.of(hdr));
//        when(dtlRepo.findByTransactionPoid(poid)).thenReturn(new ArrayList<>());
//        when(chargeRepo.findByTransactionPoid(poid)).thenReturn(new ArrayList<>());
//
//        DebitNoteHeaderDto dto = debitNoteService.getDebitNote(poid);
//
//        assertNotNull(dto);
//        assertEquals(poid, dto.getTransactionPoid());
//    }
//
//    @Test
//    void testGetDebitNote_notFound() {
//        when(hdrRepo.findById(999L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> debitNoteService.getDebitNote(999L));
//    }
//
//    // ---------------------------------------------------------------------
//    // deleteDebitNote()
//    // ---------------------------------------------------------------------
//    @Test
//    void testDeleteDebitNote() {
//
//        Long poid = 200L;
//
//        ArDebitNoteHdr hdr = new ArDebitNoteHdr();
//        hdr.setTransactionPoid(poid);
//
//        when(hdrRepo.findById(poid)).thenReturn(Optional.of(hdr));
//
//        debitNoteService.deleteDebitNote(poid);
//
//        assertEquals("Y", hdr.getDeleted());
//        verify(hdrRepo, times(1)).save(any());
//    }
//
//    // ---------------------------------------------------------------------
//    // breakup mapping
//    // ---------------------------------------------------------------------
//    @Test
//    void testLoadBreakups_billwiseAndCostCenterMapping() {
//
//        DebitNoteHeaderDto dto = new DebitNoteHeaderDto();
//        DebitNoteGlDetailDto gl = mockGlDetail();
//        gl.setDetRowId(1L);
//        dto.setGlDetails(List.of(gl));
//
//        // Billwise mock
//        LoadBillwiseBreakupResponseDto bw = new LoadBillwiseBreakupResponseDto(
//                1L, 10L, 501L,
//                "BR", "INV-001",
//                new Date(),
//                BigDecimal.valueOf(100),
//                BigDecimal.ZERO,
//                "OK"
//        );
//        GlVoucherLoadBillwiseBreakupResponseDto bwResponse =
//                new GlVoucherLoadBillwiseBreakupResponseDto();
//
//        // Cost center mock
//        CostCenterBreakupResponseDto cc = new CostCenterBreakupResponseDto(
//                1L, 501L, 88L, "CG1", "111", 500L, "Desc"
//        );
//        GlVoucherCostCenterBreakupResponseDto costResponse =
//                new GlVoucherCostCenterBreakupResponseDto();
//        costResponse.setCostBreakupList(List.of(cc));
//
//        when(billwiseBreakupService.loadBillwiseBreakup(any(), any(), any(), any()))
//                .thenReturn(bwResponse);
//
//        when(costCenterBreakupService.loadCostCenterData(any(), any(), any(),any(),any()))
//                .thenReturn(costResponse);
//
//        // call internal method using reflection
//        assertDoesNotThrow(() -> {
//            var m = DebitNoteServiceImpl.class
//                    .getDeclaredMethod("loadBreakups", DebitNoteHeaderDto.class, Long.class);
//            m.setAccessible(true);
//            m.invoke(debitNoteService, dto, 1001L);
//        });
//
//        assertNotNull(dto.getGlDetails().get(0).getBreakupList());
//        assertNotNull(dto.getGlDetails().get(0).getCostCenterList());
//    }
//
//    // ---------------------------------------------------------------------
//    // helper mock methods
//    // ---------------------------------------------------------------------
//    private DebitNoteGlDetailDto mockGlDetail() {
//        DebitNoteGlDetailDto gl = new DebitNoteGlDetailDto();
//        gl.setGlId(501L);
//        gl.setDetRowId(1L);
//        gl.setDebitAmount(BigDecimal.valueOf(100));
//        return gl;
//    }
//
//    private DebitNoteChargeDetailDto mockChargeDetail() {
//        DebitNoteChargeDetailDto ch = new DebitNoteChargeDetailDto();
//        ch.setChargeId(77L);
//        ch.setChargeAmount(BigDecimal.valueOf(50));
//        return ch;
//    }
//
//    // wrapper class used only in testing cost center service
//    static class CostCenterBreakupDtoListWrapper {
//        private List<CostCenterBreakupResponseDto> costBreakupList;
//        public CostCenterBreakupDtoListWrapper(List<CostCenterBreakupResponseDto> list) {
//            this.costBreakupList = list;
//        }
//        public List<CostCenterBreakupResponseDto> getCostBreakupList() { return costBreakupList; }
//    }
//}
//
