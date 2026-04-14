package com.asg.finance.appurchasecn.service.impl;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.dto.ApPurchaseCnGlDtlDto;
import com.asg.finance.dto.ApPurchaseCnHdrDto;
import com.asg.finance.entity.ApPurchaseCnGlDtl;
import com.asg.finance.entity.ApPurchaseCnHdr;
import com.asg.finance.exception.DataAccessException;
import com.asg.finance.repository.ApPurchaseCnChargeDtlRepository;
import com.asg.finance.repository.ApPurchaseCnGlDtlRepository;
import com.asg.finance.repository.ApPurchaseCnHdrRepository;
import com.asg.finance.repository.ApPurchaseCnItemDtlRepository;
import com.asg.finance.repository.ApPurchaseCnProcRepository;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.finance.repository.TaxMasterRepository;
import com.asg.finance.service.BillwiseBreakupService;
import com.asg.finance.service.CostCenterBreakupService;
import com.asg.finance.service.impl.ApPurchaseCnServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import net.sf.jasperreports.engine.JasperReport;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApPurchaseCnServiceImplTest {

    @Mock private ApPurchaseCnHdrRepository hdrRepository;
    @Mock private ApPurchaseCnItemDtlRepository itemDtlRepository;
    @Mock private ApPurchaseCnChargeDtlRepository chargeDtlRepository;
    @Mock private ApPurchaseCnGlDtlRepository glDtlRepository;
    @Mock private ApPurchaseCnProcRepository procRepository;
    @Mock private BillwiseBreakupService billwiseBreakupService;
    @Mock private CostCenterBreakupService costCenterBreakupService;
    @Mock private GLMasterRepository glMasterRepository;
    @Mock private TaxMasterRepository taxMasterRepository;
    @Mock private PrintService printService;
    @Mock private DataSource dataSource;
    @Mock private DocumentSearchService documentSearchService;
    @Mock private LoggingService loggingService;
    @Mock private DocumentDeleteService documentDeleteService;
    @Mock private LovDataService lovService;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private ApPurchaseCnServiceImpl service;

    private ApPurchaseCnHdrDto headerDto;
    private ApPurchaseCnHdr headerEntity;

    @BeforeEach
    void setUp() {
        headerDto = new ApPurchaseCnHdrDto();
        headerDto.setTransactionPoid(100L);
        headerDto.setTransactionDate(LocalDate.of(2026, 4, 3));
        headerDto.setPartyType("SUPPLIER");
        headerDto.setSupplierPoid(101L);
        headerDto.setCurrencyCode("BHD");
        headerDto.setCurrencyRate(BigDecimal.ONE);
        headerDto.setSupplierCnAmount(new BigDecimal("100.00"));
        headerDto.setRefType("GENERAL");
        headerDto.setNarration("Test Narration");
        
        // Add GL details for GENERAL reference type
        ApPurchaseCnGlDtlDto glDetail = new ApPurchaseCnGlDtlDto();
        glDetail.setDetRowId(1L);
        glDetail.setType("DR");
        glDetail.setCompanyPoid(1L);
        glDetail.setGlPoid(1001L);
        glDetail.setDrAmount(new BigDecimal("100"));
        glDetail.setCrAmount(BigDecimal.ZERO);
        glDetail.setTaxPoid(101L);
        glDetail.setTaxPercentage(BigDecimal.ZERO);
        glDetail.setTaxAmount(BigDecimal.ZERO);
        glDetail.setTotalAmount(new BigDecimal("100"));
        glDetail.setActionType("isCreated");
        
        List<ApPurchaseCnGlDtlDto> glDetails = new ArrayList<>();
        glDetails.add(glDetail);
        headerDto.setGlDetails(glDetails);

        headerEntity = ApPurchaseCnHdr.builder()
                .transactionPoid(100L)
                .transactionDate(headerDto.getTransactionDate())
                .docRef("CN-001")
                .groupPoid(1L)
                .companyPoid(1L)
                .currencyCode("BHD")
                .currencyRate(BigDecimal.ONE)
                .supplierPoid(101L)
                .refType("GENERAL")
                .narration("Test Narration")
                .partyType("SUPPLIER")
                .build();
    }

    @Test
    void create_Success() {
        when(hdrRepository.save(any(ApPurchaseCnHdr.class))).thenAnswer(invocation -> {
            ApPurchaseCnHdr hdr = invocation.getArgument(0);
            hdr.setTransactionPoid(100L);
            return hdr;
        });
        doNothing().when(procRepository).beforeSaveValidation(anyString(), anyLong(), anyString(), any());

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getUserId).thenReturn("user");

            DataAccessException exception = assertThrows(DataAccessException.class, () -> service.create(headerDto));
            assertThat(exception.getMessage()).contains("Failed to create supplier credit note");
        }
    }

    @Test
    void create_ExceptionWrapped() {
        doNothing().when(procRepository).beforeSaveValidation(anyString(), anyLong(), anyString(), any());
        when(hdrRepository.save(any(ApPurchaseCnHdr.class))).thenThrow(new RuntimeException("Database error"));

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getUserId).thenReturn("user");

            DataAccessException exception = assertThrows(DataAccessException.class, () -> service.create(headerDto));
            assertThat(exception.getMessage()).contains("Failed to create supplier credit note");
        }
    }

    @Test
    void getById_Success() {
        when(hdrRepository.findById(100L)).thenReturn(Optional.of(headerEntity));
        when(itemDtlRepository.findByTransactionPoid(100L)).thenReturn(Collections.emptyList());
        when(chargeDtlRepository.findByTransactionPoid(100L)).thenReturn(Collections.emptyList());
        when(glDtlRepository.findByTransactionPoid(100L)).thenReturn(Collections.emptyList());

        ApPurchaseCnHdrDto result = service.getById(100L);

        assertThat(result).isNotNull();
        assertThat(result.getTransactionPoid()).isEqualTo(100L);
        assertThat(result.getDocRef()).isEqualTo("CN-001");
        verify(hdrRepository).findById(100L);
    }

    @Test
    void getById_NotFound() {
        when(hdrRepository.findById(100L)).thenReturn(Optional.empty());

        DataAccessException exception = assertThrows(DataAccessException.class, () -> service.getById(100L));
        assertThat(exception.getMessage()).contains("Failed to fetch supplier credit note");
    }

    @Test
    void update_Success() {
        ApPurchaseCnHdrDto updateDto = new ApPurchaseCnHdrDto();
        updateDto.setTransactionDate(LocalDate.of(2026, 4, 4));
        updateDto.setPartyType("SUPPLIER");
        updateDto.setCurrencyCode("BHD");
        updateDto.setCurrencyRate(BigDecimal.ONE);
        updateDto.setSupplierCnAmount(new BigDecimal("100"));
        updateDto.setRefType("GENERAL");
        updateDto.setNarration("Updated");
        
        // Add GL details for GENERAL reference type
        ApPurchaseCnGlDtlDto glDetail = new ApPurchaseCnGlDtlDto();
        glDetail.setDetRowId(1L);
        glDetail.setType("DR");
        glDetail.setCompanyPoid(1L);
        glDetail.setGlPoid(1001L);
        glDetail.setDrAmount(new BigDecimal("100"));
        glDetail.setCrAmount(BigDecimal.ZERO);
        glDetail.setTaxPoid(101L);
        glDetail.setTaxPercentage(BigDecimal.ZERO);
        glDetail.setTaxAmount(BigDecimal.ZERO);
        glDetail.setTotalAmount(new BigDecimal("100"));
        glDetail.setActionType("isCreated");
        
        List<ApPurchaseCnGlDtlDto> glDetails = new ArrayList<>();
        glDetails.add(glDetail);
        updateDto.setGlDetails(glDetails);

        when(hdrRepository.findById(100L)).thenReturn(Optional.of(headerEntity));
        when(hdrRepository.save(any(ApPurchaseCnHdr.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemDtlRepository.findByTransactionPoid(100L)).thenReturn(Collections.emptyList());
        when(chargeDtlRepository.findByTransactionPoid(100L)).thenReturn(Collections.emptyList());
        when(glDtlRepository.findByTransactionPoid(100L)).thenReturn(Collections.emptyList());
        when(glDtlRepository.save(any())).thenAnswer(invocation -> {
            ApPurchaseCnGlDtl gl = invocation.getArgument(0);
            if (gl.getDetRowId() == null) {
                gl.setDetRowId(1L);
            }
            return gl;
        });
        when(glMasterRepository.existsByGlPoid(1001L)).thenReturn(true);
        when(taxMasterRepository.existsByTaxPoid(101L)).thenReturn(true);

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getUserId).thenReturn("user");
            mockedStatic.when(UserContext::getDocumentId).thenReturn("200-103");

            ApPurchaseCnHdrDto result = service.update(100L, updateDto);

            assertThat(result.getTransactionPoid()).isEqualTo(100L);
            verify(hdrRepository).save(any(ApPurchaseCnHdr.class));
        }
    }

    @Test
    void delete_Success() {
        when(hdrRepository.findById(100L)).thenReturn(Optional.of(headerEntity));

        service.delete(100L, null);

        verify(documentDeleteService).deleteDocument(100L, "AP_PURCHASE_CN_HDR", "TRANSACTION_POID", null, headerEntity.getTransactionDate());
    }

    @Test
    void list_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Map<String, Object> result = new HashMap<>();
        result.put("totalElements", 1);

        when(documentSearchService.resolveOperator(any())).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(any())).thenReturn("N");
        when(documentSearchService.resolveDateFilters(any(), anyString(), any(), any())).thenReturn(Collections.emptyList());
        when(documentSearchService.search(anyString(), any(), anyString(), any(Pageable.class), anyString(), anyString(), anyString()))
                .thenReturn(new com.asg.common.lib.dto.RawSearchResult(Collections.emptyList(), Collections.emptyMap(), 1L));

        Map<String, Object> listResult = service.list("200-103", new FilterRequestDto("AND", "N", List.of()), LocalDate.now(), LocalDate.now(), pageable);

        assertThat(listResult).isNotNull();
        verify(documentSearchService).search(anyString(), any(), anyString(), any(Pageable.class), anyString(), anyString(), anyString());
    }

    @Test
    void getPjRefDetails_Success() {
        Map<String, Object> procResult = new HashMap<>();
        procResult.put("pjRefType", "GENERAL");
        procResult.put("lineItems", Collections.emptyList());

        when(procRepository.getPjRefDetails(77L)).thenReturn(procResult);

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getUserPoid).thenReturn(1L);

            Map<String, Object> result = service.getPjRefDetails(77L);

            assertThat(result.get("pjRefType")).isEqualTo("GENERAL");
            verify(procRepository).getPjRefDetails(77L);
        }
    }

    @Test
    void getPartyDetails_Success() {
        when(procRepository.getPartyDetails("SUPPLIER", 11L)).thenReturn(Map.of("NAME", "Test Supplier"));

        Map<String, Object> result = service.getPartyDetails("SUPPLIER", 11L);

        assertThat(result.get("NAME")).isEqualTo("Test Supplier");
        verify(procRepository).getPartyDetails("SUPPLIER", 11L);
    }

    @Test
    void getPartyDetails_Exception() {
        when(procRepository.getPartyDetails("SUPPLIER", 12L)).thenThrow(new RuntimeException("Database error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.getPartyDetails("SUPPLIER", 12L));
        assertThat(exception.getMessage()).contains("Database error");
    }

    @Test
    void update_NotFound() {
        when(hdrRepository.findById(100L)).thenReturn(Optional.empty());

        DataAccessException exception = assertThrows(DataAccessException.class, () -> service.update(100L, headerDto));
        assertThat(exception.getMessage()).contains("Failed to update supplier credit note");
    }

    @Test
    void update_Exception() {
        when(hdrRepository.findById(100L)).thenReturn(Optional.of(headerEntity));
        when(hdrRepository.save(any(ApPurchaseCnHdr.class))).thenThrow(new RuntimeException("Database error"));

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getUserId).thenReturn("user");

            DataAccessException exception = assertThrows(DataAccessException.class, () -> service.update(100L, headerDto));
            assertThat(exception.getMessage()).contains("Failed to update supplier credit note");
        }
    }

    @Test
    void delete_NotFound() {
        when(hdrRepository.findById(100L)).thenReturn(Optional.empty());

        DataAccessException exception = assertThrows(DataAccessException.class, () -> service.delete(100L, null));
        assertThat(exception.getMessage()).contains("Failed to delete supplier credit note");
    }

    @Test
    void delete_Exception() {
        when(hdrRepository.findById(100L)).thenReturn(Optional.of(headerEntity));
        when(documentDeleteService.deleteDocument(anyLong(), anyString(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("Delete failed"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.delete(100L, null));
        assertThat(exception.getMessage()).contains("Delete failed");
    }

    @Test
    void getPjRefDetails_Exception() {
        when(procRepository.getPjRefDetails(77L)).thenThrow(new RuntimeException("Database error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.getPjRefDetails(77L));
        assertThat(exception.getMessage()).contains("Database error");
    }

    @Test
    void create_ValidationException_NoRefType() {
        headerDto.setRefType(null);
        doNothing().when(procRepository).beforeSaveValidation(anyString(), anyLong(), any(), any());
        when(hdrRepository.save(any(ApPurchaseCnHdr.class))).thenReturn(headerEntity);

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getUserId).thenReturn("user");

            DataAccessException exception = assertThrows(DataAccessException.class, () -> service.create(headerDto));
            assertThat(exception.getMessage()).contains("Failed to create supplier credit note");
        }
    }

    @Test
    void create_ValidationException_GeneralNoGlDetails() {
        headerDto.setRefType("GENERAL");
        headerDto.setGlDetails(null);
        doNothing().when(procRepository).beforeSaveValidation(anyString(), anyLong(), anyString(), any());
        when(hdrRepository.save(any(ApPurchaseCnHdr.class))).thenReturn(headerEntity);

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getUserId).thenReturn("user");

            DataAccessException exception = assertThrows(DataAccessException.class, () -> service.create(headerDto));
            assertThat(exception.getMessage()).contains("Failed to create supplier credit note");
        }
    }

    @Test
    void create_ValidationException_FFNoChargeDetails() {
        headerDto.setRefType("FF");
        headerDto.setGlDetails(null);
        headerDto.setChargeDetails(null);
        doNothing().when(procRepository).beforeSaveValidation(anyString(), anyLong(), anyString(), any());
        when(hdrRepository.save(any(ApPurchaseCnHdr.class))).thenReturn(headerEntity);

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getUserId).thenReturn("user");

            DataAccessException exception = assertThrows(DataAccessException.class, () -> service.create(headerDto));
            assertThat(exception.getMessage()).contains("Failed to create supplier credit note");
        }
    }

    @Test
    void create_ValidationException_FDANoChargeDetails() {
        headerDto.setRefType("FDA");
        headerDto.setGlDetails(null);
        headerDto.setChargeDetails(null);
        doNothing().when(procRepository).beforeSaveValidation(anyString(), anyLong(), anyString(), any());
        when(hdrRepository.save(any(ApPurchaseCnHdr.class))).thenReturn(headerEntity);

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedStatic.when(UserContext::getUserId).thenReturn("user");

            DataAccessException exception = assertThrows(DataAccessException.class, () -> service.create(headerDto));
            assertThat(exception.getMessage()).contains("Failed to create supplier credit note");
        }
    }

    @Test
    void create_UserContextNull() {
        when(hdrRepository.save(any(ApPurchaseCnHdr.class))).thenAnswer(invocation -> {
            ApPurchaseCnHdr hdr = invocation.getArgument(0);
            hdr.setTransactionPoid(100L);
            return hdr;
        });
        doNothing().when(procRepository).beforeSaveValidation(anyString(), anyLong(), anyString(), any());

        try (MockedStatic<UserContext> mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getGroupPoid).thenReturn(null);
            mockedStatic.when(UserContext::getCompanyPoid).thenReturn(null);
            mockedStatic.when(UserContext::getUserId).thenReturn("user");

            DataAccessException exception = assertThrows(DataAccessException.class, () -> service.create(headerDto));
            assertThat(exception.getMessage()).contains("Failed to create supplier credit note");
        }
    }

    @Test
    void getById_Exception() {
        when(hdrRepository.findById(100L)).thenThrow(new RuntimeException("Database error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.getById(100L));
        assertThat(exception.getMessage()).contains("Database error");
    }

    @Test
    void list_Exception() {
        when(documentSearchService.resolveOperator(any())).thenThrow(new RuntimeException("Search error"));

        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> service.list("200-103", null, null, null, PageRequest.of(0, 10)));
        assertThat(exception.getMessage()).contains("Search error");
    }

    @Test
    void print_Success() throws Exception {
        Long transactionPoid = 100L;
        byte[] expectedPdf = "PDF content".getBytes();
        Map<String, Object> baseParams = new HashMap<>();
        baseParams.put("transactionPoid", transactionPoid);
        
        JasperReport glSubreport = org.mockito.Mockito.mock(JasperReport.class);
        JasperReport chargeSubreport = org.mockito.Mockito.mock(JasperReport.class);
        JasperReport mainReport = org.mockito.Mockito.mock(JasperReport.class);
        
        when(printService.buildBaseParams(transactionPoid, "200-103")).thenReturn(baseParams);
        when(printService.load("Finance/AP/PurchaseInvoiceReportGlSubreport1.jrxml")).thenReturn(glSubreport);
        when(printService.load("Finance/AP/PurchaseInvoiceChargeSubReport.jrxml")).thenReturn(chargeSubreport);
        when(printService.load("Finance/AP/PurchaseReturnNote.jrxml")).thenReturn(mainReport);
        when(printService.fillReportToPdf(eq(mainReport), any(Map.class), eq(dataSource))).thenReturn(expectedPdf);
        
        byte[] result = service.print(transactionPoid);
        
        assertThat(result).isEqualTo(expectedPdf);
        verify(printService).buildBaseParams(transactionPoid, "200-103");
        verify(printService).load("Finance/AP/PurchaseInvoiceReportGlSubreport1.jrxml");
        verify(printService).load("Finance/AP/PurchaseInvoiceChargeSubReport.jrxml");
        verify(printService).load("Finance/AP/PurchaseReturnNote.jrxml");
        verify(printService).fillReportToPdf(eq(mainReport), any(Map.class), eq(dataSource));
    }
    
    @Test
    void print_BuildBaseParamsException() throws Exception {
        Long transactionPoid = 100L;
        
        when(printService.buildBaseParams(transactionPoid, "200-103"))
                .thenThrow(new RuntimeException("Failed to build base parameters"));
        
        DataAccessException exception = assertThrows(DataAccessException.class, () -> service.print(transactionPoid));
        
        assertThat(exception.getMessage()).contains("Failed to generate PDF for transaction: " + transactionPoid);
    }
    
    @Test
    void print_LoadSubreportException() throws Exception {
        Long transactionPoid = 100L;
        Map<String, Object> baseParams = new HashMap<>();
        
        when(printService.buildBaseParams(transactionPoid, "200-103")).thenReturn(baseParams);
        when(printService.load("Finance/AP/PurchaseInvoiceReportGlSubreport1.jrxml"))
                .thenThrow(new RuntimeException("Failed to load GL subreport"));
        
        DataAccessException exception = assertThrows(DataAccessException.class, () -> service.print(transactionPoid));
        
        assertThat(exception.getMessage()).contains("Failed to generate PDF for transaction: " + transactionPoid);
    }
    
    @Test
    void print_LoadMainReportException() throws Exception {
        Long transactionPoid = 100L;
        Map<String, Object> baseParams = new HashMap<>();
        
        JasperReport glSubreport = org.mockito.Mockito.mock(JasperReport.class);
        JasperReport chargeSubreport = org.mockito.Mockito.mock(JasperReport.class);
        
        when(printService.buildBaseParams(transactionPoid, "200-103")).thenReturn(baseParams);
        when(printService.load("Finance/AP/PurchaseInvoiceReportGlSubreport1.jrxml")).thenReturn(glSubreport);
        when(printService.load("Finance/AP/PurchaseInvoiceChargeSubReport.jrxml")).thenReturn(chargeSubreport);
        when(printService.load("Finance/AP/PurchaseReturnNote.jrxml"))
                .thenThrow(new RuntimeException("Failed to load main report"));
        
        DataAccessException exception = assertThrows(DataAccessException.class, () -> service.print(transactionPoid));
        
        assertThat(exception.getMessage()).contains("Failed to generate PDF for transaction: " + transactionPoid);
    }
    
    @Test
    void print_FillReportToPdfException() throws Exception {
        Long transactionPoid = 100L;
        Map<String, Object> baseParams = new HashMap<>();
        
        JasperReport glSubreport = org.mockito.Mockito.mock(JasperReport.class);
        JasperReport chargeSubreport = org.mockito.Mockito.mock(JasperReport.class);
        JasperReport mainReport = org.mockito.Mockito.mock(JasperReport.class);
        
        when(printService.buildBaseParams(transactionPoid, "200-103")).thenReturn(baseParams);
        when(printService.load("Finance/AP/PurchaseInvoiceReportGlSubreport1.jrxml")).thenReturn(glSubreport);
        when(printService.load("Finance/AP/PurchaseInvoiceChargeSubReport.jrxml")).thenReturn(chargeSubreport);
        when(printService.load("Finance/AP/PurchaseReturnNote.jrxml")).thenReturn(mainReport);
        when(printService.fillReportToPdf(eq(mainReport), any(Map.class), eq(dataSource)))
                .thenThrow(new RuntimeException("Failed to fill report to PDF"));
        
        DataAccessException exception = assertThrows(DataAccessException.class, () -> service.print(transactionPoid));
        
        assertThat(exception.getMessage()).contains("Failed to generate PDF for transaction: " + transactionPoid);
    }
}
