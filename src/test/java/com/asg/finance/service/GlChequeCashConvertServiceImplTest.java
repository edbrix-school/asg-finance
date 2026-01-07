//package com.asg.finance.service;
//
//import com.asg.common.lib.dto.FilterDto;
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.dto.LovGetListDto;
//import com.asg.common.lib.dto.RawSearchResult;
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.service.DocumentSearchService;
//import com.asg.common.lib.service.LovDataService;
//import com.asg.finance.dto.GlChequeCashConvertHdrDto;
//import com.asg.finance.dto.GlChequeConversionLoadResponseDto;
//import com.asg.finance.entity.GlChequeCashConvertHdrEntity;
//import com.asg.finance.entity.GlChequeCashConvertInDtlEntity;
//import com.asg.finance.entity.GlChequeCashConvertOutDtlEntity;
//import com.asg.finance.entity.key.GlChequeCashConvertInDtlKey;
//import com.asg.finance.entity.key.GlChequeCashConvertOutDtlKey;
//import com.asg.finance.repository.GlChequeCashConvertHdrRepository;
//import com.asg.finance.repository.GlChequeCashConvertInDtlRepository;
//import com.asg.finance.repository.GlChequeCashConvertOutDtlRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class GlChequeCashConvertServiceImplTest {
//
//    @Mock private GlChequeCashConvertHdrRepository hdrRepo;
//    @Mock private GlChequeCashConvertInDtlRepository inRepo;
//    @Mock private GlChequeCashConvertOutDtlRepository outRepo;
//    @Mock private DocumentSearchService documentService;
//    @Mock private LovDataService lovService;
//    @Mock private com.asg.finance.repository.GlChequeCashConvertRepository glChequeCashConvertRepository;
//
//    @InjectMocks private GlChequeCashConvertServiceImpl service;
//
//    private GlChequeCashConvertHdrEntity hdr;
//
//    @BeforeEach
//    void setup() {
//
//        ReflectionTestUtils.setField(service, "lovService", lovService);
//
//        Mockito.lenient().doReturn(new LovGetListDto())
//                .when(lovService)
//                .getDetailsByPoidAndLovName(org.mockito.ArgumentMatchers.<Long>isNull(), anyString());
//        Mockito.lenient().doReturn(new LovGetListDto())
//                .when(lovService)
//                .getDetailsByPoidAndLovName(anyLong(), anyString());
//        hdr = new GlChequeCashConvertHdrEntity();
//        hdr.setTransactionPoid(201L);
//        hdr.setTransactionDate(LocalDateTime.now());
//        hdr.setGroupPoid(1L);
//        hdr.setCompanyPoid(2L);
//        hdr.setDocRef("ASG7");
//        hdr.setType("CHQ2CASH");
//        hdr.setPostingNarration("Cheque conversion");
//        hdr.setCash(2000L);
//        hdr.setRemarks("note");
//        hdr.setCreatedBy("u1");
//        hdr.setCreatedDate(LocalDateTime.now());
//        hdr.setLastModifiedBy("u1");
//        hdr.setLastModifiedDate(LocalDateTime.now());
//        hdr.setDeleted("N");
//        hdr.setChqAcNo("AX123");
//        hdr.setChqCardNo("9876");
//        hdr.setRoundingAmt(0L);
//    }
//
//    @Test
//    void getGlChequeCashConvert_returnsMappedDto_withDetails() {
//
//        when(hdrRepo.findByTransactionPoid(201L)).thenReturn(hdr);
//
//        GlChequeCashConvertInDtlEntity in = new GlChequeCashConvertInDtlEntity();
//        GlChequeCashConvertInDtlKey inKey = new GlChequeCashConvertInDtlKey();
//        inKey.setTransactionPoid(201L);
//        inKey.setDetRowId(1L);
//        in.setId(inKey);
//        in.setBankPoid(501L);
//        in.setChqAcName("Axis");
//        in.setChqCardNo("9876");
//        in.setChqDate(LocalDate.now());
//        in.setAmount(1000L);
//        in.setRemarks("in");
//        in.setVoucherType("CHQ");
//        in.setChequeCompanyPoid(301L);
//        in.setPaymentMainPoid(401L);
//        in.setLineType("IN");
//        in.setPymtType("CHEQUE");
//        in.setTtBankPoid(100L);
//        in.setTtRef("TT1");
//        in.setCardPoid(900L);
//        in.setCardType("VISA");
//        in.setCreditCardRef("CREF");
//        when(inRepo.findByIdTransactionPoid(201L)).thenReturn(List.of(in));
//
//        GlChequeCashConvertOutDtlEntity out = new GlChequeCashConvertOutDtlEntity();
//        GlChequeCashConvertOutDtlKey outKey = new GlChequeCashConvertOutDtlKey();
//        outKey.setTransactionPoid(201L);
//        outKey.setDetRowId(1L);
//        out.setId(outKey);
//        out.setPaymentMainPoid(772267L);
//        out.setAmount(1000L);
//        out.setRemarks("out");
//        out.setBankPoid(50L);
//        out.setChqAcName("Cash");
//        out.setChqAcNo("CASH");
//        out.setChqCardNo("CNUM");
//        out.setChqDate(LocalDate.now());
//        out.setSelected("Y");
//        out.setVoucherType("CASH");
//        out.setLineType("OUT");
//        when(outRepo.findByIdTransactionPoid(201L)).thenReturn(List.of(out));
//
//        LovGetListDto lovDto = new LovGetListDto();
//        lovDto.setCode("X");
//        when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);
//
//        GlChequeCashConvertHdrDto dto = service.getGlChequeCashConvert(201L);
//        assertThat(dto.getTransactionPoid()).isEqualTo(201L);
//        assertThat(dto.getDocRef()).isEqualTo("ASG7");
//        assertThat(dto.getPostingNarration()).isEqualTo("Cheque conversion");
//        assertThat(dto.getCash()).isEqualTo(2000L);
//        assertThat(dto.getInDtls()).hasSize(1);
//        assertThat(dto.getOutDtls()).hasSize(1);
//        assertThat(dto.getCompanyDet()).isNotNull();
//        verify(hdrRepo).findByTransactionPoid(201L);
//        verify(inRepo).findByIdTransactionPoid(201L);
//        verify(outRepo).findByIdTransactionPoid(201L);
//    }
//
//    @Test
//    void getGlChequeCashConvert_throwsResourceNotFound_whenMissing() {
//        when(hdrRepo.findByTransactionPoid(999L)).thenReturn(null);
//        assertThrows(ResourceNotFoundException.class, () -> service.getGlChequeCashConvert(999L));
//    }
//
//    @Test
//    void softDeleteByTransactionPoid_marksDeleted_andDeletesChildren() {
//        GlChequeCashConvertHdrEntity existing = new GlChequeCashConvertHdrEntity();
//        existing.setTransactionPoid(301L);
//        when(hdrRepo.findById(301L)).thenReturn(Optional.of(existing));
//
//        service.softDeleteByTransactionPoid(301L);
//
//        assertThat(existing.getDeleted()).isEqualTo("Y");
//        assertThat(existing.getLastModifiedDate()).isNotNull();
//        verify(hdrRepo).save(existing);
//        verify(inRepo).deleteByIdTransactionPoid(301L);
//        verify(outRepo).deleteByIdTransactionPoid(301L);
//    }
//
//    @Test
//    void listOfRecordsAndGenericSearch_returnsWrappedPage() {
//        FilterRequestDto req = new FilterRequestDto("AND", "N", List.of(new FilterDto("DOC_REF","ASG7")));
//        Pageable pageable = PageRequest.of(0, 10);
//        List<Map<String,Object>> records = List.of(Map.of("TRANSACTION_POID", 1L, "DOC_REF","ASG7"));
//
//        when(documentService.resolveOperator(any())).thenReturn("AND");
//        when(documentService.resolveIsDeleted(any())).thenReturn("N");
//        when(documentService.resolveFilters(any())).thenAnswer(inv -> ((FilterRequestDto)inv.getArgument(0)).filters());
//
//        when(documentService.search(
//                eq("400-110"), anyList(), eq("AND"), eq(pageable), eq("N"),
//                eq("TRANSACTION_POID"), eq("DOC_REF")
//        )).thenReturn(new RawSearchResult(records, Map.of("TRANSACTION_POID","NUMBER"), 1L));
//
//        Map<String,Object> result = service.listOfRecordsAndGenericSearch("400-110", req, pageable);
//        assertNotNull(result);
//        assertTrue(result.containsKey("content"));
//        assertTrue(result.containsKey("displayFields"));
//        assertEquals(1L, ((Number)result.get("totalElements")).longValue());
//    }
//
//    @Test
//    void createGlChequeCashConvert_savesHeader_andReturnsDto() {
//        GlChequeCashConvertHdrDto input = new GlChequeCashConvertHdrDto();
//        input.setGroupPoid(1L);
//        input.setCompanyPoid(2L);
//        input.setDocRef("ASG7");
//        input.setPostingNarration("Cheque conversion");
//        input.setCash(2000L);
//        input.setCreatedBy("u1");
//        input.setTransactionDate(LocalDateTime.now());
//
//        GlChequeCashConvertHdrEntity saved = new GlChequeCashConvertHdrEntity();
//        saved.setTransactionPoid(777L);
//        when(hdrRepo.save(any(GlChequeCashConvertHdrEntity.class))).thenReturn(saved);
//        when(hdrRepo.findByTransactionPoid(777L)).thenReturn(saved);
//        when(inRepo.findByIdTransactionPoid(777L)).thenReturn(Collections.emptyList());
//        when(outRepo.findByIdTransactionPoid(777L)).thenReturn(Collections.emptyList());
//
//        GlChequeCashConvertHdrDto result = service.createGlChequeCashConvert(input);
//        assertNotNull(result);
//        verify(hdrRepo).save(any(GlChequeCashConvertHdrEntity.class));
//        verify(inRepo, never()).saveAll(anyList());
//        verify(outRepo, never()).saveAll(anyList());
//    }
//
//    @Test
//    void updateGlChequeCashConvert_updatesFields_andReturnsDto() {
//        GlChequeCashConvertHdrEntity existing = new GlChequeCashConvertHdrEntity();
//        existing.setTransactionPoid(101L);
//        when(hdrRepo.findById(101L)).thenReturn(Optional.of(existing));
//
//        GlChequeCashConvertHdrEntity afterSave = new GlChequeCashConvertHdrEntity();
//        afterSave.setTransactionPoid(101L);
//        when(hdrRepo.save(any(GlChequeCashConvertHdrEntity.class))).thenReturn(afterSave);
//
//        when(hdrRepo.findByTransactionPoid(101L)).thenReturn(afterSave);
//        when(inRepo.findByIdTransactionPoid(101L)).thenReturn(Collections.emptyList());
//        when(outRepo.findByIdTransactionPoid(101L)).thenReturn(Collections.emptyList());
//
//
//        GlChequeCashConvertHdrDto update = new GlChequeCashConvertHdrDto();
//        update.setPostingNarration("upd");
//        update.setCash(2500L);
//        update.setRemarks("r");
//        update.setChqAcNo("acc");
//        update.setChqCardNo("card");
//        update.setRoundingAmt(1L);
//        update.setDeleted("N");
//        update.setLastModifiedBy("tester");
//
//        GlChequeCashConvertHdrDto result = service.updateGlChequeCashConvert(101L, update);
//        assertNotNull(result);
//        verify(hdrRepo).save(any(GlChequeCashConvertHdrEntity.class));
//    }
//
//    @Test
//    void loadGlChequeConversion_enrichesBankDet_andReturnsList() {
//        GlChequeConversionLoadResponseDto row = new GlChequeConversionLoadResponseDto();
//        row.setPaymentMainPoid(7002001L);
//        row.setAmount(1000.0);
//        row.setBankPoid(50L);
//        row.setVoucherType("CHQ");
//
//        when(glChequeCashConvertRepository.loadGlChequeConversion(eq("716380"), eq("0100000007343"), eq("CHEQUE_TO_CHEQUE")))
//                .thenReturn(List.of(row));
//
//        LovGetListDto bankLov = new LovGetListDto();
//        bankLov.setCode("BANK-50");
//        when(lovService.getDetailsByPoidAndLovName(50L, "BANK")).thenReturn(bankLov);
//
//        List<GlChequeConversionLoadResponseDto> result = service.loadGlChequeConversion("716380", "0100000007343", "CHEQUE_TO_CHEQUE");
//
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertEquals(7002001L, result.get(0).getPaymentMainPoid());
//        assertEquals(1000.0, result.get(0).getAmount());
//        assertNotNull(result.get(0).getBankDet());
//        assertEquals("BANK-50", result.get(0).getBankDet().getCode());
//        verify(glChequeCashConvertRepository).loadGlChequeConversion(eq("716380"), eq("0100000007343"), eq("CHEQUE_TO_CHEQUE"));
//        verify(lovService).getDetailsByPoidAndLovName(50L, "BANK");
//    }
//
//    @Test
//    void loadGlChequeConversion_emptyList_returnsEmpty() {
//        when(glChequeCashConvertRepository.loadGlChequeConversion(any(), any(), any()))
//                .thenReturn(Collections.emptyList());
//
//        List<GlChequeConversionLoadResponseDto> result = service.loadGlChequeConversion(null, null, "CHEQUE_TO_CASH");
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//        verify(glChequeCashConvertRepository).loadGlChequeConversion(isNull(), isNull(), eq("CHEQUE_TO_CASH"));
//        verify(lovService, never()).getDetailsByPoidAndLovName(anyLong(), anyString());
//    }
//}
//
