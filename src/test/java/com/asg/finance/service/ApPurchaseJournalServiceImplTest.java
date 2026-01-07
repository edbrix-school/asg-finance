//package com.asg.finance.service;
//
//import com.asg.common.lib.dto.FilterRequestDto;
//import com.asg.common.lib.dto.RawSearchResult;
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.service.DocumentSearchService;
//import com.asg.finance.dto.ApPurchaseInvoiceAssetDtlDto;
//import com.asg.finance.dto.ApPurchaseInvoiceGlDtlDto;
//import com.asg.finance.dto.ApPurchaseInvoiceHdrDto;
//import com.asg.finance.dto.ApPurchaseInvoiceItemDtlDto;
//import com.asg.finance.entity.ApPurchaseInvoiceAssetDtlEntity;
//import com.asg.finance.entity.ApPurchaseInvoiceGlDtlEntity;
//import com.asg.finance.entity.ApPurchaseInvoiceHdrEntity;
//import com.asg.finance.entity.ApPurchaseInvoiceItemDtlEntity;
//import com.asg.finance.repository.ApPurchaseInvoiceAssetDtlRepository;
//import com.asg.finance.repository.ApPurchaseInvoiceGlDtlRepository;
//import com.asg.finance.repository.ApPurchaseInvoiceHdrRepository;
//import com.asg.finance.repository.ApPurchaseInvoiceItemDtlRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class ApPurchaseJournalServiceImplTest {
//
//    @Mock private ApPurchaseInvoiceHdrRepository hdrRepo;
//    @Mock private ApPurchaseInvoiceItemDtlRepository itemRepo;
//    @Mock private ApPurchaseInvoiceGlDtlRepository glRepo;
//    @Mock private ApPurchaseInvoiceAssetDtlRepository assetRepo;
//    //@Mock private LovService lovService;
//    @Mock private DocumentSearchService documentService;
//
//    @InjectMocks
//    private ApPurchaseJournalServiceImpl service;
//
//    private ApPurchaseInvoiceHdrEntity sampleHdr;
//
//    @BeforeEach
//    void setup() {
//        sampleHdr = new ApPurchaseInvoiceHdrEntity();
//        sampleHdr.setTransactionPoid(71031L);
//        sampleHdr.setTransactionDate(LocalDate.now());
//        sampleHdr.setGroupPoid(1L);
//        sampleHdr.setDocRef("ASG-1");
//        sampleHdr.setCompanyPoid(1L);
//        sampleHdr.setSupplierPoid(2L);
//        sampleHdr.setLocationPoid(3L);
//        sampleHdr.setDeleted("N");
//    }
//
//    @Test
//    @DisplayName("fetchApPurchaseInvoiceHdr returns DTO when found")
//    void fetchHdr_success() {
//        when(hdrRepo.findByTransactionPoid(71031L)).thenReturn(sampleHdr);
//        when(itemRepo.findByIdTransactionPoid(71031L)).thenReturn(List.of());
//        when(glRepo.findByIdTransactionPoid(71031L)).thenReturn(List.of());
//        when(assetRepo.findByIdTransactionPoid(71031L)).thenReturn(List.of());
//
//        ApPurchaseInvoiceHdrDto dto = service.fetchApPurchaseInvoiceHdr(71031L);
//        assertNotNull(dto);
//        assertEquals(71031L, dto.getTransactionPoid());
//    }
//
//    @Test
//    @DisplayName("fetchApPurchaseInvoiceHdr throws when not found")
//    void fetchHdr_notFound() {
//        when(hdrRepo.findByTransactionPoid(999L)).thenReturn(null);
//        assertThrows(ResourceNotFoundException.class, () -> service.fetchApPurchaseInvoiceHdr(999L));
//    }
//
//    @Test
//    @DisplayName("softDeleteApPurchaseInvoice marks deleted and deletes child rows")
//    void softDelete_success() {
//        ApPurchaseInvoiceHdrEntity existing = new ApPurchaseInvoiceHdrEntity();
//        existing.setTransactionPoid(700L);
//        when(hdrRepo.findById(700L)).thenReturn(Optional.of(existing));
//        when(hdrRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
//        when(hdrRepo.findByTransactionPoid(700L)).thenReturn(existing);
//        when(itemRepo.findByIdTransactionPoid(700L)).thenReturn(List.of());
//        when(glRepo.findByIdTransactionPoid(700L)).thenReturn(List.of());
//        when(assetRepo.findByIdTransactionPoid(700L)).thenReturn(List.of());
//
//        ApPurchaseInvoiceHdrDto dto = service.softDeleteApPurchaseInvoice(700L, "tester");
//        assertNotNull(dto);
//        verify(itemRepo).deleteByIdTransactionPoid(700L);
//        verify(glRepo).deleteByIdTransactionPoid(700L);
//        verify(assetRepo).deleteByIdTransactionPoid(700L);
//        verify(hdrRepo).save(argThat(e -> "Y".equals(((ApPurchaseInvoiceHdrEntity)e).getDeleted())));
//    }
//
//    @Test
//    @DisplayName("listOfRecordsAndGenericSearch delegates to DocumentService and wraps page")
//    void listSearch_success() {
//        Pageable pageable = PageRequest.of(0, 10);
//        FilterRequestDto req = new FilterRequestDto("AND", "N", List.of());
//
//        when(documentService.resolveOperator(any())).thenReturn("AND");
//        when(documentService.resolveIsDeleted(any())).thenReturn("N");
//        when(documentService.resolveFilters(any())).thenReturn(List.of());
//
//        RawSearchResult raw = new RawSearchResult(List.of(), Map.of(), 0L);
//        when(documentService.search(eq("200-103"), anyList(), eq("AND"), eq(pageable), eq("N"), any(), any()))
//                .thenReturn(raw);
//
//        Map<String, Object> result = service.listOfRecordsAndGenericSearch("200-103", req, pageable);
//        assertNotNull(result);
//        assertTrue(result.containsKey("content"));
//        assertTrue(result.containsKey("totalElements"));
//    }
//
//    @Test
//    @DisplayName("createApPurchaseInvoice saves header and detail rows")
//    void create_withDetails_savesChildren() {
//        // Arrange header save
//        ApPurchaseInvoiceHdrDto in = new ApPurchaseInvoiceHdrDto();
//        in.setGroupPoid(1L);
//        in.setCompanyPoid(1L);
//        in.setSupplierPoid(2L);
//        in.setLocationPoid(3L);
//        in.setDocRef("ASG-NEW");
//
//        ApPurchaseInvoiceItemDtlDto id1 = new ApPurchaseInvoiceItemDtlDto();
//        id1.setStockPoid(11L);
//        id1.setStockUnitPoid(21L);
//        id1.setPoQty(1L);
//        in.setItemDtls(List.of(id1));
//
//
//        ApPurchaseInvoiceGlDtlDto gd1 = new ApPurchaseInvoiceGlDtlDto();
//        gd1.setCompanyPoid(1L);
//        gd1.setGlPoid(100L);
//        in.setGlDtls(List.of(gd1));
//
//
//        ApPurchaseInvoiceAssetDtlDto ad1 = new ApPurchaseInvoiceAssetDtlDto();
//        ad1.setFaCode("FA1");
//        in.setAssetDtls(List.of(ad1));
//
//        ApPurchaseInvoiceHdrEntity saved = new ApPurchaseInvoiceHdrEntity();
//        saved.setTransactionPoid(555L);
//        when(hdrRepo.save(any())).thenReturn(saved);
//        when(hdrRepo.findByTransactionPoid(555L)).thenReturn(saved);
//        when(itemRepo.findByIdTransactionPoid(555L)).thenReturn(List.of());
//        when(glRepo.findByIdTransactionPoid(555L)).thenReturn(List.of());
//        when(assetRepo.findByIdTransactionPoid(555L)).thenReturn(List.of());
//
//        when(itemRepo.findMaxDetRowIdByTransactionPoid(555L)).thenReturn(0L);
//        when(glRepo.findMaxDetRowIdByTransactionPoid(555L)).thenReturn(0L);
//        when(assetRepo.findMaxDetRowIdByTransactionPoid(555L)).thenReturn(1L);
//
//
//        ApPurchaseInvoiceHdrDto out = service.createApPurchaseInvoice(in);
//
//
//        assertNotNull(out);
//        ArgumentCaptor<List<ApPurchaseInvoiceItemDtlEntity>> itemCaptor = ArgumentCaptor.forClass(List.class);
//        verify(itemRepo).saveAll(itemCaptor.capture());
//        assertEquals(1, itemCaptor.getValue().size());
//
//        ArgumentCaptor<List<ApPurchaseInvoiceGlDtlEntity>> glCaptor = ArgumentCaptor.forClass(List.class);
//        verify(glRepo).saveAll(glCaptor.capture());
//        assertEquals(1, glCaptor.getValue().size());
//
//        ArgumentCaptor<List<ApPurchaseInvoiceAssetDtlEntity>> assetCaptor = ArgumentCaptor.forClass(List.class);
//        verify(assetRepo).saveAll(assetCaptor.capture());
//        assertEquals(1, assetCaptor.getValue().size());
//    }
//
//    @Test
//    @DisplayName("updateApPurchaseInvoice saves detail rows with provided detRowIds")
//    void update_savesChildren() {
//        Long tx = 777L;
//        ApPurchaseInvoiceHdrEntity existing = new ApPurchaseInvoiceHdrEntity();
//        existing.setTransactionPoid(tx);
//        when(hdrRepo.findById(tx)).thenReturn(Optional.of(existing));
//        when(hdrRepo.save(any())).thenReturn(existing);
//
//        ApPurchaseInvoiceHdrDto in = new ApPurchaseInvoiceHdrDto();
//
//        ApPurchaseInvoiceItemDtlDto item = new ApPurchaseInvoiceItemDtlDto();
//        item.setDetRowId(1L);
//        in.setItemDtls(List.of(item));
//
//        ApPurchaseInvoiceGlDtlDto gl = new ApPurchaseInvoiceGlDtlDto();
//        gl.setDetRowId(2L);
//        in.setGlDtls(List.of(gl));
//
//        ApPurchaseInvoiceAssetDtlDto asset = new ApPurchaseInvoiceAssetDtlDto();
//        asset.setDetRowId(3L);
//        in.setAssetDtls(List.of(asset));
//
//        when(hdrRepo.findByTransactionPoid(tx)).thenReturn(existing);
//        when(itemRepo.findByIdTransactionPoid(tx)).thenReturn(List.of());
//        when(glRepo.findByIdTransactionPoid(tx)).thenReturn(List.of());
//        when(assetRepo.findByIdTransactionPoid(tx)).thenReturn(List.of());
//
//        ApPurchaseInvoiceHdrDto out = service.updateApPurchaseInvoice(tx, in);
//        assertNotNull(out);
//
//        verify(itemRepo, atLeastOnce()).save(any(ApPurchaseInvoiceItemDtlEntity.class));
//        verify(glRepo, atLeastOnce()).save(any(ApPurchaseInvoiceGlDtlEntity.class));
//        verify(assetRepo, atLeastOnce()).save(any(ApPurchaseInvoiceAssetDtlEntity.class));
//    }
//}
//
