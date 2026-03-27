package com.asg.finance.bankdepositvoucher.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.*;
import com.asg.finance.dto.BankDepositVoucherDtlDto;
import com.asg.finance.dto.BankDepositVoucherRequestDto;
import com.asg.finance.dto.BankDepositVoucherResponseDto;
import com.asg.finance.entity.GlBankDepositVoucherDtl;
import com.asg.finance.entity.GlBankDepositVoucherHdr;
import com.asg.finance.repository.GlBankDepositVoucherDtlRepository;
import com.asg.finance.repository.GlBankDepositVoucherHdrRepository;
import com.asg.finance.service.impl.BankDepositVoucherServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankDepositVoucherServiceImplTest {

    @Mock
    private GlBankDepositVoucherHdrRepository hdrRepository;
    @Mock
    private GlBankDepositVoucherDtlRepository dtlRepository;
    @Mock
    private DocumentSearchService documentService;
    @Mock
    private DocumentDeleteService documentDeleteService;
    @Mock
    private PrintService printService;
    @Mock
    private DataSource dataSource;
    @Mock
    private LovDataService lovService;
    @Mock
    private LoggingService loggingService;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private BankDepositVoucherServiceImpl service;

    private BankDepositVoucherRequestDto requestDto;
    private GlBankDepositVoucherHdr hdrEntity;
    private GlBankDepositVoucherDtl dtlEntity;

    @BeforeEach
    void setUp() {
        requestDto = BankDepositVoucherRequestDto.builder()
                .bankPoid(1L)
                .type("CHQ")
                .bankFilter("ALL")
                .groupPosting(true)
                .postingNarration("Test Narration")
                .remarks("Test Remarks")
                .details(new ArrayList<>())
                .build();

        hdrEntity = GlBankDepositVoucherHdr.builder()
                .transactionPoid(100L)
                .transactionDate(LocalDate.now())
                .bankPoid(1L)
                .docRef("DOC-001")
                .grandTotal(BigDecimal.valueOf(1000))
                .build();

        dtlEntity = GlBankDepositVoucherDtl.builder()
                .transactionPoid(100L)
                .detRowId(1L)
                .bankPoid(1L)
                .amount(BigDecimal.valueOf(1000))
                .build();
    }

    @Test
    void createBankDepositVoucher_Success() {
        requestDto.getDetails().add(BankDepositVoucherDtlDto.builder()
                .bankPoid(1L)
                .amount(BigDecimal.valueOf(1000))
                .pymtType("CHQ")
                .build());

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedConstruction<TransactionTemplate> mockedTx = mockConstruction(TransactionTemplate.class,
                (mock, context) -> {
                    when(mock.execute(any())).thenAnswer(invocation -> {
                        TransactionCallback<?> callback = invocation.getArgument(0);
                        return callback.doInTransaction(null);
                    });
                })) {
            
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(hdrRepository.save(any())).thenReturn(hdrEntity);
            when(hdrRepository.findByTransactionPoid(100L)).thenReturn(Optional.of(hdrEntity));
            when(dtlRepository.findByTransactionPoidOrderByChqSeqNumAsc(100L)).thenReturn(List.of(dtlEntity));

            BankDepositVoucherResponseDto response = service.createBankDepositVoucher(requestDto);

            assertNotNull(response);
            assertEquals(100L, response.getTransactionPoid());
            verify(hdrRepository).callBeforeSaveValidation(anyLong(), anyLong());
            verify(hdrRepository).markPaymentsCompleted(eq(100L), anyString());
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), eq("DOC123"), eq("100"));
        }
    }

    @Test
    void updateBankDepositVoucher_Success() {
        requestDto.getDetails().add(BankDepositVoucherDtlDto.builder()
                .detRowId(1L)
                .actionType("ISUPDATED")
                .bankPoid(1L)
                .amount(BigDecimal.valueOf(1000))
                .pymtType("CHQ")
                .build());
        requestDto.getDetails().add(BankDepositVoucherDtlDto.builder()
                .actionType("ISCREATED")
                .bankPoid(1L)
                .amount(BigDecimal.valueOf(500))
                .pymtType("CHQ")
                .build());
        requestDto.getDetails().add(BankDepositVoucherDtlDto.builder()
                .detRowId(2L)
                .actionType("ISDELETED")
                .amount(BigDecimal.ZERO)
                .build());

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedConstruction<TransactionTemplate> mockedTx = mockConstruction(TransactionTemplate.class,
                (mock, context) -> {
                    doAnswer(invocation -> {
                        java.util.function.Consumer<org.springframework.transaction.TransactionStatus> action = invocation.getArgument(0);
                        action.accept(null);
                        return null;
                    }).when(mock).executeWithoutResult(any());
                    when(mock.execute(any())).thenAnswer(invocation -> {
                        TransactionCallback<?> callback = invocation.getArgument(0);
                        return callback.doInTransaction(null);
                    });
                })) {
            
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(hdrRepository.findByTransactionPoid(100L)).thenReturn(Optional.of(hdrEntity));
            when(dtlRepository.findByTransactionPoid(100L)).thenReturn(List.of(dtlEntity, GlBankDepositVoucherDtl.builder().detRowId(2L).build()));
            when(dtlRepository.findByTransactionPoidOrderByChqSeqNumAsc(100L)).thenReturn(List.of(dtlEntity));

            BankDepositVoucherResponseDto response = service.updateBankDepositVoucher(100L, requestDto);

            assertNotNull(response);
            verify(hdrRepository).markPaymentsCompleted(eq(100L), anyString());
            verify(dtlRepository, times(2)).saveAll(anyList());
            verify(dtlRepository).delete(any());
            verify(loggingService).logChanges(any(), any(), any(), anyString(), anyString(), any(), anyString());
        }
    }

    @Test
    void createBankDepositVoucher_WithNullDetails_Success() {
        requestDto.setDetails(null);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedConstruction<TransactionTemplate> mockedTx = mockConstruction(TransactionTemplate.class,
                (mock, context) -> {
                    when(mock.execute(any())).thenAnswer(invocation -> {
                        TransactionCallback<?> callback = invocation.getArgument(0);
                        return callback.doInTransaction(null);
                    });
                })) {
            
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(hdrRepository.save(any())).thenReturn(hdrEntity);
            when(hdrRepository.findByTransactionPoid(100L)).thenReturn(Optional.of(hdrEntity));
            when(dtlRepository.findByTransactionPoidOrderByChqSeqNumAsc(100L)).thenReturn(new ArrayList<>());

            BankDepositVoucherResponseDto response = service.createBankDepositVoucher(requestDto);

            assertNotNull(response);
            verify(hdrRepository).save(any());
        }
    }

    @Test
    void createBankDepositVoucher_Exception() {
        try (MockedConstruction<TransactionTemplate> mockedTx = mockConstruction(TransactionTemplate.class,
                (mock, context) -> {
                    when(mock.execute(any())).thenThrow(new RuntimeException("DB Error"));
                })) {
            
            assertThrows(RuntimeException.class, () -> service.createBankDepositVoucher(requestDto));
        }
    }

    @Test
    void getBankDepositVoucherById_Success() {
        when(hdrRepository.findByTransactionPoid(100L)).thenReturn(Optional.of(hdrEntity));
        when(dtlRepository.findByTransactionPoidOrderByChqSeqNumAsc(100L)).thenReturn(List.of(dtlEntity));
        when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(new LovGetListDto());

        BankDepositVoucherResponseDto response = service.getBankDepositVoucherById(100L);

        assertNotNull(response);
        assertNotNull(response.getDetails());
        assertEquals(1, response.getDetails().size());
    }

    @Test
    void getBankDepositVoucherById_NotFound() {
        when(hdrRepository.findByTransactionPoid(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getBankDepositVoucherById(999L));
    }

    @Test
    void softDeleteBankDepositVoucher_Success() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Testing");

        when(hdrRepository.findByTransactionPoid(100L)).thenReturn(Optional.of(hdrEntity));

        service.softDeleteBankDepositVoucher(100L, deleteReasonDto);

        verify(documentDeleteService).deleteDocument(eq(100L), eq("GL_BANK_DEPOSIT_VOUCHER_HDR"), eq("TRANSACTION_POID"), eq(deleteReasonDto), any());
    }

    @Test
    void listBankDepositVouchers_Success() {
        RawSearchResult raw = new RawSearchResult(List.of(Map.of("id", 1)), Map.of("id", "ID"), 1L);
        when(documentService.search(anyString(), anyList(), any(), any(), any(), anyString(), anyString())).thenReturn(raw);
        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");

        Map<String, Object> result = service.listBankDepositVouchers("DOC123", new FilterRequestDto("AND", "N", new ArrayList<>()), null, null, Pageable.unpaged());

        assertNotNull(result);
        assertEquals(1L, result.get("totalElements"));
    }

    @Test
    void loadPendingPayments_Success() {
        when(hdrRepository.loadPendingPayments(1L, "CHQ", "ALL")).thenReturn(new ArrayList<>());

        List<BankDepositVoucherDtlDto> result = service.loadPendingPayments(1L, "CHQ", "ALL");

        assertNotNull(result);
        verify(hdrRepository).loadPendingPayments(1L, "CHQ", "ALL");
    }

    @Test
    void print_Success() throws Exception {
        when(printService.buildBaseParams(100L, "400-109")).thenReturn(new HashMap<>());
        when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[0]);

        byte[] result = service.print(100L);

        assertNotNull(result);
        verify(printService, times(3)).load(anyString());
    }
}
