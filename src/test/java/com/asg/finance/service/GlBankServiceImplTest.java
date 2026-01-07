//package com.asg.finance.service;
//
//import com.asg.common.lib.exception.ResourceAlreadyExistsException;
//import com.asg.common.lib.exception.ResourceNotFoundException;
//import com.asg.common.lib.service.DocumentSearchService;
//import com.asg.common.lib.service.LovDataService;
//import com.asg.finance.dto.GlBankChequeDtlDto;
//import com.asg.finance.dto.GlBankCommissionDtlDto;
//import com.asg.finance.dto.GlBankDto;
//import com.asg.finance.entity.GlBankChequeDtlEntity;
//import com.asg.finance.entity.GlBankCommissionDtlEntity;
//import com.asg.finance.entity.GlBankEntity;
//import com.asg.finance.repository.GlBankChequeDtlRepository;
//import com.asg.finance.repository.GlBankCommissionDtlRepository;
//import com.asg.finance.repository.GlBankRepository;
//import com.asg.finance.repository.TaxMasterRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.math.BigDecimal;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class GlBankServiceImplTest {
//
//    @Mock(lenient = true)
//    private GlBankRepository bankRepository;
//
//    @Mock(lenient = true)
//    private GlBankChequeDtlRepository chequeDtlRepository;
//
//    @Mock(lenient = true)
//    private GlBankCommissionDtlRepository commissionDtlRepository;
//
//    @Mock(lenient = true)
//    private DocumentSearchService documentService;
//
//    @Mock(lenient = true)
//    private TaxMasterRepository taxMasterRepository;
//
//    @Mock(lenient = true)
//    private com.asg.finance.repository.GLMasterRepository glMasterRepository;
//
//    @Mock(lenient = true)
//    private LovDataService lovService;
//
////    @Mock(lenient = true)
////    private CurrencyRepository currencyRepository;
////
////    @Mock(lenient = true)
////    private CompanyRepository companyRepository;
//
//    @InjectMocks
//    private GlBankServiceImpl glBankService;
//
//    private Pageable pageable;
//    private GlBankEntity bankEntity;
//    private GlBankDto bankDto;
//    private GlBankChequeDtlEntity chequeEntity;
//    private GlBankCommissionDtlEntity commissionEntity;
//
//    @BeforeEach
//    void setUp() {
//        pageable = PageRequest.of(0, 10);
//        bankEntity = new GlBankEntity();
//        bankEntity.setBankPoid(1L);
//        bankEntity.setBankCode("BANK001");
//        bankEntity.setBankDescription("Test Bank");
//        bankEntity.setGlPoid(100L);
//        bankEntity.setBankAccountNo("123456789");
//        bankEntity.setCompanyPoid(String.valueOf(1001L));
//        bankEntity.setBankPrefix("TB");
//        bankEntity.setActive("Y");
//
//        bankDto = GlBankDto.builder()
//                .bankPoid(1L)
//                .bankCode("BANK001")
//                .bankDescription("Test Bank")
//                .glPoid(100L)
//                .bankAccountNo("123456789")
//                .companyPoid(1001L)
//                .bankPrefix("TB")
//                .active("Y")
//                .build();
//
//        chequeEntity = new GlBankChequeDtlEntity();
//        chequeEntity.setDetRowId(1L);
//        chequeEntity.setBankPoid(1L);
//        chequeEntity.setChqSignType("SINGLE");
//
//        commissionEntity = new GlBankCommissionDtlEntity();
//        commissionEntity.setDetRowId(1L);
//        commissionEntity.setBankPoid(1L);
//        commissionEntity.setCardType("VISA");
//    }
//
//    @Test
//    void fetchGlBank_Success() {
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        when(chequeDtlRepository.findByBankPoid(1L)).thenReturn(Arrays.asList(chequeEntity));
//        when(commissionDtlRepository.findByBankPoid(1L)).thenReturn(Arrays.asList(commissionEntity));
//        when(lovService.getDetailsByPoidAndLovName(any(), any())).thenReturn(null);
//        when(lovService.getDetailsByCodeAndLovName(any(), any())).thenReturn(null);
//
//        GlBankDto result = glBankService.fetchGlBank(1L);
//
//        assertNotNull(result);
//        assertEquals("Test Bank", result.getBankDescription());
//        verify(bankRepository).findByBankPoid(1L);
//        verify(chequeDtlRepository).findByBankPoid(1L);
//        verify(commissionDtlRepository).findByBankPoid(1L);
//    }
//
//    @Test
//    void fetchGlBank_NotFound() {
//        when(bankRepository.findByBankPoid(999L)).thenReturn(null);
//
//        assertThrows(ResourceNotFoundException.class, () -> glBankService.fetchGlBank(999L));
//        verify(bankRepository).findByBankPoid(999L);
//    }
//
//    @Test
//    void updateGlBank_Success() {
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        when(bankRepository.existsByBankCodeIgnoreCaseAndBankPoidNot("BANK001", 1L)).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCaseAndBankPoidNot("Test Bank", 1L)).thenReturn(false);
//        when(bankRepository.existsByBankAccountNoIgnoreCaseAndBankPoidNot("123456789", 1L)).thenReturn(false);
//        when(currencyRepository.existsByCurrencyCodeIgnoreCase(any())).thenReturn(true);
//        when(companyRepository.existsByCompanyPoid(any())).thenReturn(true);
//        when(glMasterRepository.existsByGlPoid(any())).thenReturn(true);
//        when(bankRepository.save(any(GlBankEntity.class))).thenReturn(bankEntity);
//
//        GlBankDto result = glBankService.updateGlBank(1L, bankDto);
//
//        assertNotNull(result);
//        verify(bankRepository).save(any(GlBankEntity.class));
//    }
//
//    @Test
//    void updateGlBank_NotFound() {
//        when(bankRepository.findByBankPoid(999L)).thenReturn(null);
//
//        assertThrows(ResourceNotFoundException.class, () -> glBankService.updateGlBank(999L, bankDto));
//    }
//
//    @Test
//    void updateGlBank_BankCodeAlreadyExists() {
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        when(bankRepository.existsByBankCodeIgnoreCaseAndBankPoidNot("BANK001", 1L)).thenReturn(true);
//
//        assertThrows(ResourceAlreadyExistsException.class, () -> glBankService.updateGlBank(1L, bankDto));
//    }
//
//    @Test
//    void updateGlBank_BankDescriptionAlreadyExists() {
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        when(bankRepository.existsByBankCodeIgnoreCaseAndBankPoidNot("BANK001", 1L)).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCaseAndBankPoidNot("Test Bank", 1L)).thenReturn(true);
//
//        assertThrows(ResourceAlreadyExistsException.class, () -> glBankService.updateGlBank(1L, bankDto));
//    }
//
//    @Test
//    void updateGlBank_BankAccountNoAlreadyExists() {
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        when(bankRepository.existsByBankCodeIgnoreCaseAndBankPoidNot("BANK001", 1L)).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCaseAndBankPoidNot("Test Bank", 1L)).thenReturn(false);
//        when(bankRepository.existsByBankAccountNoIgnoreCaseAndBankPoidNot("123456789", 1L)).thenReturn(true);
//
//        assertThrows(ResourceAlreadyExistsException.class, () -> glBankService.updateGlBank(1L, bankDto));
//    }
//
//    @Test
//    void createEntry_Success() {
//        when(bankRepository.existsByBankCodeIgnoreCase("BANK001")).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCase("Test Bank")).thenReturn(false);
//        when(bankRepository.existsByBankAccountNoIgnoreCase("123456789")).thenReturn(false);
//        when(currencyRepository.existsByCurrencyCodeIgnoreCase(any())).thenReturn(true);
//        when(companyRepository.existsByCompanyPoid(any())).thenReturn(true);
//        when(glMasterRepository.existsByGlPoid(any())).thenReturn(true);
//        when(bankRepository.save(any(GlBankEntity.class))).thenReturn(bankEntity);
//        when(chequeDtlRepository.saveAll(any())).thenReturn(Arrays.asList(chequeEntity));
//        when(commissionDtlRepository.saveAll(any())).thenReturn(Arrays.asList(commissionEntity));
//        when(taxMasterRepository.existsByTaxPoid(any())).thenReturn(true);
//
//        GlBankChequeDtlDto chequeDto = new GlBankChequeDtlDto();
//        GlBankCommissionDtlDto commissionDto = new GlBankCommissionDtlDto();
//        commissionDto.setTaxPoid(1L);
//        commissionDto.setCommissionGlPoid(1L);
//        commissionDto.setCommissionPercent(BigDecimal.valueOf(2.0));
//
//        bankDto.setChequeDetails(Arrays.asList(chequeDto));
//        bankDto.setCommissionDetails(Arrays.asList(commissionDto));
//
//        GlBankEntity result = glBankService.createEntry(bankDto);
//
//        assertNotNull(result);
//        verify(bankRepository).save(any(GlBankEntity.class));
//        verify(chequeDtlRepository).saveAll(any());
//        verify(commissionDtlRepository).saveAll(any());
//    }
//
//    @Test
//    void createEntry_BankCodeExists() {
//        when(bankRepository.existsByBankCodeIgnoreCase("BANK001")).thenReturn(true);
//
//        assertThrows(RuntimeException.class, () -> glBankService.createEntry(bankDto));
//    }
//
//    @Test
//    void deleteBankMaster_Success() {
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        when(bankRepository.save(any(GlBankEntity.class))).thenReturn(bankEntity);
//
//        glBankService.deleteBankMaster(1L);
//
//        verify(bankRepository).findByBankPoid(1L);
//        verify(bankRepository).save(any(GlBankEntity.class));
//        verify(chequeDtlRepository).deleteByBankPoid(1L);
//        verify(commissionDtlRepository).deleteByBankPoid(1L);
//    }
//
//    @Test
//    void deleteBankMaster_NotFound() {
//        when(bankRepository.findByBankPoid(999L)).thenReturn(null);
//
//        assertThrows(ResourceNotFoundException.class, () -> glBankService.deleteBankMaster(999L));
//    }
//
//    // Edge Cases
//    @Test
//    void fetchGlBank_WithNullBankPoid() {
//        assertThrows(Exception.class, () -> glBankService.fetchGlBank(null));
//    }
//
//    @Test
//    void fetchGlBank_WithZeroBankPoid() {
//        when(bankRepository.findByBankPoid(0L)).thenReturn(null);
//        assertThrows(ResourceNotFoundException.class, () -> glBankService.fetchGlBank(0L));
//    }
//
//    @Test
//    void fetchGlBank_WithNegativeBankPoid() {
//        when(bankRepository.findByBankPoid(-1L)).thenReturn(null);
//        assertThrows(ResourceNotFoundException.class, () -> glBankService.fetchGlBank(-1L));
//    }
//
//    @Test
//    void fetchGlBank_WithEmptyCollections() {
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        when(chequeDtlRepository.findByBankPoid(1L)).thenReturn(Collections.emptyList());
//        when(commissionDtlRepository.findByBankPoid(1L)).thenReturn(Collections.emptyList());
//        when(lovService.getDetailsByPoidAndLovName(any(), any())).thenReturn(null);
//        when(lovService.getDetailsByCodeAndLovName(any(), any())).thenReturn(null);
//
//        GlBankDto result = glBankService.fetchGlBank(1L);
//
//        assertNotNull(result);
//        assertTrue(result.getChequeDetails() == null || result.getChequeDetails().isEmpty());
//        assertTrue(result.getCommissionDetails() == null || result.getCommissionDetails().isEmpty());
//    }
//
//    @Test
//    void updateGlBank_WithNullDto() {
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        assertThrows(Exception.class, () -> glBankService.updateGlBank(1L, null));
//    }
//
//    @Test
//    void updateGlBank_WithNullBankCode() {
//        bankDto.setBankCode(null);
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        assertThrows(Exception.class, () -> glBankService.updateGlBank(1L, bankDto));
//    }
//
//    @Test
//    void updateGlBank_WithEmptyBankCode() {
//        bankDto.setBankCode("");
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        assertThrows(Exception.class, () -> glBankService.updateGlBank(1L, bankDto));
//    }
//
//    @Test
//    void updateGlBank_WithWhitespaceBankCode() {
//        bankDto.setBankCode("   ");
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        assertThrows(Exception.class, () -> glBankService.updateGlBank(1L, bankDto));
//    }
//
//    @Test
//    void updateGlBank_WithInvalidCurrency() {
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        when(bankRepository.existsByBankCodeIgnoreCaseAndBankPoidNot("BANK001", 1L)).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCaseAndBankPoidNot("Test Bank", 1L)).thenReturn(false);
//        when(bankRepository.existsByBankAccountNoIgnoreCaseAndBankPoidNot("123456789", 1L)).thenReturn(false);
//        when(currencyRepository.existsByCurrencyCodeIgnoreCase(any())).thenReturn(false);
//
//        assertThrows(RuntimeException.class, () -> glBankService.updateGlBank(1L, bankDto));
//    }
//
//    @Test
//    void updateGlBank_WithInvalidCompany() {
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        when(bankRepository.existsByBankCodeIgnoreCaseAndBankPoidNot("BANK001", 1L)).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCaseAndBankPoidNot("Test Bank", 1L)).thenReturn(false);
//        when(bankRepository.existsByBankAccountNoIgnoreCaseAndBankPoidNot("123456789", 1L)).thenReturn(false);
//        when(currencyRepository.existsByCurrencyCodeIgnoreCase(any())).thenReturn(true);
//        when(companyRepository.existsByCompanyPoid(any())).thenReturn(false);
//
//        assertThrows(RuntimeException.class, () -> glBankService.updateGlBank(1L, bankDto));
//    }
//
//    @Test
//    void updateGlBank_WithInvalidGlPoid() {
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        when(bankRepository.existsByBankCodeIgnoreCaseAndBankPoidNot("BANK001", 1L)).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCaseAndBankPoidNot("Test Bank", 1L)).thenReturn(false);
//        when(bankRepository.existsByBankAccountNoIgnoreCaseAndBankPoidNot("123456789", 1L)).thenReturn(false);
//        when(currencyRepository.existsByCurrencyCodeIgnoreCase(any())).thenReturn(true);
//        when(companyRepository.existsByCompanyPoid(any())).thenReturn(true);
//        when(glMasterRepository.existsByGlPoid(any())).thenReturn(false);
//
//        assertThrows(RuntimeException.class, () -> glBankService.updateGlBank(1L, bankDto));
//    }
//
//    @Test
//    void createEntry_WithNullDto() {
//        assertThrows(Exception.class, () -> glBankService.createEntry(null));
//    }
//
//    @Test
//    void createEntry_BankDescriptionExists() {
//        when(bankRepository.existsByBankCodeIgnoreCase("BANK001")).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCase("Test Bank")).thenReturn(true);
//
//        assertThrows(RuntimeException.class, () -> glBankService.createEntry(bankDto));
//    }
//
//    @Test
//    void createEntry_BankAccountNoExists() {
//        when(bankRepository.existsByBankCodeIgnoreCase("BANK001")).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCase("Test Bank")).thenReturn(false);
//        when(bankRepository.existsByBankAccountNoIgnoreCase("123456789")).thenReturn(true);
//
//        assertThrows(RuntimeException.class, () -> glBankService.createEntry(bankDto));
//    }
//
//    @Test
//    void createEntry_WithInvalidTaxPoid() {
//        when(bankRepository.existsByBankCodeIgnoreCase("BANK001")).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCase("Test Bank")).thenReturn(false);
//        when(bankRepository.existsByBankAccountNoIgnoreCase("123456789")).thenReturn(false);
//        when(currencyRepository.existsByCurrencyCodeIgnoreCase(any())).thenReturn(true);
//        when(companyRepository.existsByCompanyPoid(any())).thenReturn(true);
//        when(glMasterRepository.existsByGlPoid(any())).thenReturn(true);
//        when(taxMasterRepository.existsByTaxPoid(any())).thenReturn(false);
//
//        GlBankCommissionDtlDto commissionDto = new GlBankCommissionDtlDto();
//        commissionDto.setTaxPoid(999L);
//        bankDto.setCommissionDetails(Arrays.asList(commissionDto));
//
//        assertThrows(RuntimeException.class, () -> glBankService.createEntry(bankDto));
//    }
//
//    @Test
//    void createEntry_WithNullCommissionPercent() {
//        when(bankRepository.existsByBankCodeIgnoreCase("BANK001")).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCase("Test Bank")).thenReturn(false);
//        when(bankRepository.existsByBankAccountNoIgnoreCase("123456789")).thenReturn(false);
//        when(currencyRepository.existsByCurrencyCodeIgnoreCase(any())).thenReturn(true);
//        when(companyRepository.existsByCompanyPoid(any())).thenReturn(true);
//        when(glMasterRepository.existsByGlPoid(any())).thenReturn(true);
//        when(taxMasterRepository.existsByTaxPoid(any())).thenReturn(true);
//
//        GlBankCommissionDtlDto commissionDto = new GlBankCommissionDtlDto();
//        commissionDto.setTaxPoid(1L);
//        commissionDto.setCommissionPercent(null);
//        bankDto.setCommissionDetails(Arrays.asList(commissionDto));
//
//        assertThrows(RuntimeException.class, () -> glBankService.createEntry(bankDto));
//    }
//
//    @Test
//    void createEntry_WithNegativeCommissionPercent() {
//        when(bankRepository.existsByBankCodeIgnoreCase("BANK001")).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCase("Test Bank")).thenReturn(false);
//        when(bankRepository.existsByBankAccountNoIgnoreCase("123456789")).thenReturn(false);
//        when(currencyRepository.existsByCurrencyCodeIgnoreCase(any())).thenReturn(true);
//        when(companyRepository.existsByCompanyPoid(any())).thenReturn(true);
//        when(glMasterRepository.existsByGlPoid(any())).thenReturn(true);
//        when(taxMasterRepository.existsByTaxPoid(any())).thenReturn(true);
//
//        GlBankCommissionDtlDto commissionDto = new GlBankCommissionDtlDto();
//        commissionDto.setTaxPoid(1L);
//        commissionDto.setCommissionPercent(BigDecimal.valueOf(-1.0));
//        bankDto.setCommissionDetails(Arrays.asList(commissionDto));
//
//        assertThrows(RuntimeException.class, () -> glBankService.createEntry(bankDto));
//    }
//
//    @Test
//    void createEntry_WithExcessiveCommissionPercent() {
//        when(bankRepository.existsByBankCodeIgnoreCase("BANK001")).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCase("Test Bank")).thenReturn(false);
//        when(bankRepository.existsByBankAccountNoIgnoreCase("123456789")).thenReturn(false);
//        when(currencyRepository.existsByCurrencyCodeIgnoreCase(any())).thenReturn(true);
//        when(companyRepository.existsByCompanyPoid(any())).thenReturn(true);
//        when(glMasterRepository.existsByGlPoid(any())).thenReturn(true);
//        when(taxMasterRepository.existsByTaxPoid(any())).thenReturn(true);
//
//        GlBankCommissionDtlDto commissionDto = new GlBankCommissionDtlDto();
//        commissionDto.setTaxPoid(1L);
//        commissionDto.setCommissionPercent(BigDecimal.valueOf(101.0));
//        bankDto.setCommissionDetails(Arrays.asList(commissionDto));
//
//        assertThrows(RuntimeException.class, () -> glBankService.createEntry(bankDto));
//    }
//
//    @Test
//    void createEntry_WithEmptyCollections() {
//        when(bankRepository.existsByBankCodeIgnoreCase("BANK001")).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCase("Test Bank")).thenReturn(false);
//        when(bankRepository.existsByBankAccountNoIgnoreCase("123456789")).thenReturn(false);
//        when(currencyRepository.existsByCurrencyCodeIgnoreCase(any())).thenReturn(true);
//        when(companyRepository.existsByCompanyPoid(any())).thenReturn(true);
//        when(glMasterRepository.existsByGlPoid(any())).thenReturn(true);
//        when(bankRepository.save(any(GlBankEntity.class))).thenReturn(bankEntity);
//
//        bankDto.setChequeDetails(Collections.emptyList());
//        bankDto.setCommissionDetails(Collections.emptyList());
//
//        GlBankEntity result = glBankService.createEntry(bankDto);
//
//        assertNotNull(result);
//        verify(bankRepository).save(any(GlBankEntity.class));
//    }
//
//    @Test
//    void deleteBankMaster_WithNullBankPoid() {
//        assertThrows(Exception.class, () -> glBankService.deleteBankMaster(null));
//    }
//
//    @Test
//    void deleteBankMaster_WithZeroBankPoid() {
//        when(bankRepository.findByBankPoid(0L)).thenReturn(null);
//        assertThrows(ResourceNotFoundException.class, () -> glBankService.deleteBankMaster(0L));
//    }
//
//    @Test
//    void deleteBankMaster_WithNegativeBankPoid() {
//        when(bankRepository.findByBankPoid(-1L)).thenReturn(null);
//        assertThrows(ResourceNotFoundException.class, () -> glBankService.deleteBankMaster(-1L));
//    }
//
//    @Test
//    void updateGlBank_WithMaxLengthBankCode() {
//        String maxLengthCode = "A".repeat(20);
//        bankDto.setBankCode(maxLengthCode);
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//        when(bankRepository.existsByBankCodeIgnoreCaseAndBankPoidNot(maxLengthCode, 1L)).thenReturn(false);
//        when(bankRepository.existsByBankDescriptionIgnoreCaseAndBankPoidNot("Test Bank", 1L)).thenReturn(false);
//        when(bankRepository.existsByBankAccountNoIgnoreCaseAndBankPoidNot("123456789", 1L)).thenReturn(false);
//        when(currencyRepository.existsByCurrencyCodeIgnoreCase(any())).thenReturn(true);
//        when(companyRepository.existsByCompanyPoid(any())).thenReturn(true);
//        when(glMasterRepository.existsByGlPoid(any())).thenReturn(true);
//        when(bankRepository.save(any(GlBankEntity.class))).thenReturn(bankEntity);
//
//        GlBankDto result = glBankService.updateGlBank(1L, bankDto);
//
//        assertNotNull(result);
//    }
//
//    @Test
//    void updateGlBank_WithExceedingLengthBankCode() {
//        String exceedingLengthCode = "A".repeat(21);
//        bankDto.setBankCode(exceedingLengthCode);
//        when(bankRepository.findByBankPoid(1L)).thenReturn(bankEntity);
//
//        assertThrows(Exception.class, () -> glBankService.updateGlBank(1L, bankDto));
//    }
//}
