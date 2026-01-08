package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.TelexFileDtlDto;
import com.asg.finance.dto.TelexFileGenerateRequestDto;
import com.asg.finance.dto.TelexFileGenerateResponseDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.entity.GlBankDebitHdr;
import com.asg.finance.entity.GlBankFileDtl;
import com.asg.finance.entity.GlBankFileHdr;
import com.asg.finance.repository.GlBankDebitHdrRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.repository.TelexFileGenerateProcRepository;
import com.asg.finance.repository.GlBankFileDtlRepository;
import com.asg.finance.repository.GlBankFileHdrRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.BankFileBatchService;
import com.asg.finance.service.TelexFileGenerateService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TelexFileGenerateServiceImpl implements TelexFileGenerateService {

    private final GlBankFileHdrRepository hdrRepository;
    private final GlBankFileDtlRepository dtlRepository;
    private final TelexFileGenerateProcRepository procRepository;
    private final DocumentSearchService documentService;
    private final LovDataService lovService;
    private final GlBankDebitHdrRepository glBankDebitHdrRepository;
    private final BankFileBatchService bankFileBatchService;
    private final DocumentDeleteService documentDeleteService;

    @Override
    @Transactional
    public TelexFileGenerateResponseDto createTelexFile(TelexFileGenerateRequestDto request) {
        try {
            GlBankFileHdr hdr = new GlBankFileHdr();
            hdr.setTransactionDate(request.getTransactionDate());
            hdr.setGroupPoid(UserContext.getGroupPoid());
            hdr.setCompanyPoid(UserContext.getCompanyPoid());
            hdr.setBankPoid(request.getBankPoid());
            hdr.setBankList(request.getBankList() != null ? request.getBankList() : "Y");
            hdr.setLongNarration(request.getRemarks());
            hdr.setOnlyApproval(request.isApprovalOnly() ? "Y" : "N");
            hdr.setTtSuppressBalanceCheck(request.isSuppressBalanceCheck() ? "Y" : "N");
            hdr.setCreatedBy(getCurrentUser());
            hdr.setCreatedDate(LocalDateTime.now());
            hdr.setLastModifiedBy(getCurrentUser());
            hdr.setLastModifiedDate(LocalDateTime.now());
            hdr.setDeleted("N");

            GlBankFileHdr savedHdr = hdrRepository.saveAndFlush(hdr);

            if (request.getDetails() != null && !request.getDetails().isEmpty()) {
                List<GlBankFileDtl> details = new ArrayList<>();
                for (int i = 0; i < request.getDetails().size(); i++) {
                    TelexFileDtlDto dto = request.getDetails().get(i);
                    GlBankFileDtl detail = convertToDetailEntity(dto, savedHdr.getTransactionPoid());
                    detail.setDetRowId((long) (i + 1));
                    details.add(detail);
                }
                dtlRepository.saveAll(details);
                dtlRepository.flush();
            }

            Long userId = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;
            Long transactionPoid = savedHdr.getTransactionPoid();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    processFileAsync(transactionPoid, userId);
                }
            });
            
            return getTelexFileById(transactionPoid);
        } catch (Exception e) {
            String errorMessage = extractTriggerErrorMessage(e);
            throw new ValidationException(errorMessage);
        }
    }

    @Override
    @Transactional
    public TelexFileGenerateResponseDto updateTelexFile(Long transactionPoid, TelexFileGenerateRequestDto request) {
        try {
            GlBankFileHdr hdr = hdrRepository.findByTransactionPoid(transactionPoid)
                    .orElseThrow(() -> new ResourceNotFoundException("Telex File", "transactionPoid", transactionPoid));

            hdr.setTransactionDate(request.getTransactionDate());
            hdr.setBankPoid(request.getBankPoid());
            hdr.setBankList(request.getBankList() != null ? request.getBankList() : "Y");
            hdr.setLongNarration(request.getRemarks());
            hdr.setOnlyApproval(request.isApprovalOnly() ? "Y" : "N");
            hdr.setTtSuppressBalanceCheck(request.isSuppressBalanceCheck() ? "Y" : "N");
            hdr.setLastModifiedBy(getCurrentUser());
            hdr.setLastModifiedDate(LocalDateTime.now());

            hdrRepository.saveAndFlush(hdr);

            dtlRepository.deleteByTransactionPoid(transactionPoid);

            if (request.getDetails() != null && !request.getDetails().isEmpty()) {
                List<GlBankFileDtl> details = new ArrayList<>();
                for (int i = 0; i < request.getDetails().size(); i++) {
                    TelexFileDtlDto dto = request.getDetails().get(i);
                    GlBankFileDtl detail = convertToDetailEntity(dto, transactionPoid);
                    detail.setDetRowId((long) (i + 1));
                    details.add(detail);
                }
                dtlRepository.saveAll(details);
                dtlRepository.flush();
            }

            Long userId = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;
            
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    processFileAsync(transactionPoid, userId);
                }
            });
            
            return getTelexFileById(transactionPoid);
        } catch (Exception e) {
            String errorMessage = extractTriggerErrorMessage(e);
            throw new ValidationException(errorMessage);
        }
    }

    @Override
    public TelexFileGenerateResponseDto getTelexFileById(Long transactionPoid) {
        GlBankFileHdr hdr = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Telex File", "transactionPoid", transactionPoid));

        List<GlBankFileDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);

        return convertToResponseDto(hdr, details);
    }

    @Override
    @Transactional
    public void softDeleteTelexFile(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        try {
            GlBankFileHdr hdr = hdrRepository.findByTransactionPoid(transactionPoid)
                    .orElseThrow(() -> new ResourceNotFoundException("Telex File", "transactionPoid", transactionPoid));

            documentDeleteService.deleteDocument(
                    transactionPoid,
                    "GL_BANK_DEBIT_HDR",
                    "TRANSACTION_POID",
                    deleteReasonDto.getDeleteReason(),
                    hdr.getTransactionDate()
            );

            hdr.setDeleted("Y");
            hdr.setLastModifiedBy(getCurrentUser());
            hdr.setLastModifiedDate(LocalDateTime.now());
            hdrRepository.saveAndFlush(hdr);
        } catch (Exception e) {
            String errorMessage = extractTriggerErrorMessage(e);
            throw new ValidationException(errorMessage);
        }
    }

    @Override
    public Map<String, Object> listTelexFiles(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "DOC_REF", "TRANSACTION_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public List<TelexFileDtlDto> loadTelexTransferData(String bankList) {
        return procRepository.loadTelexTransferData(bankList);
    }

    @Override
    @Transactional
    public String regenerateTelexFile(Long debitVoucherPoid) {
        glBankDebitHdrRepository.findByTransactionPoid(debitVoucherPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Telex File", "transactionPoid", debitVoucherPoid));

        Long userId = UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;
        return procRepository.regenerateTelexFile(UserContext.getGroupPoid(), UserContext.getCompanyPoid(),
                userId, debitVoucherPoid);
    }

    private GlBankFileDtl convertToDetailEntity(TelexFileDtlDto dto, Long transactionPoid) {
        return GlBankFileDtl.builder()
                .transactionPoid(transactionPoid)
                .debitTransactionPoid(dto.getDebitTransactionPoid())
                .debitTransactionDate(dto.getDebitTransactionDate())
                .debitCompanyPoid(dto.getDebitCompanyPoid())
                .debitDocRef(dto.getDebitDocRef())
                .debitPayingToName(dto.getDebitPayingToName())
                .debitPayingType(dto.getDebitPayingType())
                .debitLongNarration(dto.getDebitLongNarration())
                .debitTtDate(dto.getDebitTtDate())
                .debitCurrencyCode(dto.getDebitCurrencyCode())
                .debitCurrencyRate(dto.getDebitCurrencyRate())
                .debitCurrencyAmt(dto.getDebitCurrencyAmt())
                .debitAmount(dto.getDebitAmount())
                .deleted(dto.getDeleted() != null ? dto.getDeleted() : "N")
                .selected(dto.getSelected() != null ? dto.getSelected() : "N")
                .drilldownLinkInfo(dto.getDrilldownLinkInfo())
                .debitTtChargeType(dto.getDebitTtChargeType())
                .createdBy(getCurrentUser())
                .createdDate(LocalDateTime.now())
                .lastModifiedBy(getCurrentUser())
                .lastModifiedDate(LocalDateTime.now())
                .build();
    }

    private TelexFileGenerateResponseDto convertToResponseDto(GlBankFileHdr hdr, List<GlBankFileDtl> details) {
        return TelexFileGenerateResponseDto.builder()
                .transactionPoid(hdr.getTransactionPoid())
                .transactionDate(hdr.getTransactionDate())
                .groupPoid(hdr.getGroupPoid())
                .groupDet(lovService.getDetailsByPoidAndLovName(hdr.getGroupPoid(), "GROUP"))
                .companyPoid(hdr.getCompanyPoid())
                .companyDet(lovService.getDetailsByPoidAndLovName(hdr.getCompanyPoid(), "COMPANY"))
                .docRef(hdr.getDocRef())
                .bankPoid(hdr.getBankPoid())
                .bankDet(lovService.getDetailsByCodeAndLovName(hdr.getBankList(), "BANK_PAYMENT_FILE"))
                .bankList(hdr.getBankList())
                .remarks(hdr.getLongNarration())
                .approvalOnly("Y".equals(hdr.getOnlyApproval()))
                .suppressBalanceCheck("Y".equals(hdr.getTtSuppressBalanceCheck()))
                .createdBy(hdr.getCreatedBy())
                .createdDate(hdr.getCreatedDate())
                .lastModifiedBy(hdr.getLastModifiedBy())
                .lastModifiedDate(hdr.getLastModifiedDate())
                .details(details.stream().map(this::convertToDetailDto).collect(Collectors.toList()))
                .build();
    }

    private TelexFileDtlDto convertToDetailDto(GlBankFileDtl entity) {
        return TelexFileDtlDto.builder()
                .detRowId(entity.getDetRowId())
                .debitTransactionPoid(entity.getDebitTransactionPoid())
                .debitTransactionDate(entity.getDebitTransactionDate())
                .debitCompanyPoid(entity.getDebitCompanyPoid())
                .debitCompanyDet(lovService.getDetailsByPoidAndLovName(entity.getDebitCompanyPoid(), "COMPANY"))
                .debitDocRef(entity.getDebitDocRef())
                .debitPayingToName(entity.getDebitPayingToName())
                .debitPayingType(entity.getDebitPayingType())
                .debitLongNarration(entity.getDebitLongNarration())
                .debitTtDate(entity.getDebitTtDate())
                .debitCurrencyCode(entity.getDebitCurrencyCode())
                .debitCurrencyRate(entity.getDebitCurrencyRate())
                .debitCurrencyAmt(entity.getDebitCurrencyAmt())
                .debitAmount(entity.getDebitAmount())
                .deleted(entity.getDeleted())
                .selected(entity.getSelected())
                .drilldownLinkInfo(entity.getDrilldownLinkInfo())
                .debitTtChargeType(entity.getDebitTtChargeType())
                .build();
    }

    private String getCurrentUser() {
        Long userPoid = UserContext.getUserPoid();
        return userPoid != null ? userPoid.toString() : "SYSTEM";
    }

    @Async
    public void processFileAsync(Long transactionPoid, Long userId) {
        try {
            bankFileBatchService.createBankFileBatch(transactionPoid, userId);
        } catch (Exception e) {
            // Log error but don't throw - async method
        }
    }

    private String extractTriggerErrorMessage(Exception e) {
        String errorMessage = e.getMessage();
        if (errorMessage != null && errorMessage.contains("Changes allowed only within current Financial Period")) {
            return "Changes allowed only within current Financial Period";
        } else if (errorMessage != null && errorMessage.contains("Transaction date can not update")) {
            return "Transaction date cannot be updated";
        } else if (errorMessage != null && errorMessage.contains("Changes allowed only within current Transaction Period")) {
            return "Changes allowed only within current Transaction Period";
        }
        return errorMessage != null ? errorMessage : "Database operation failed";
    }
}
