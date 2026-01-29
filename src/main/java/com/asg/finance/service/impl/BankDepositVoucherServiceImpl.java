package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.dto.BankDepositVoucherDtlDto;
import com.asg.finance.dto.BankDepositVoucherRequestDto;
import com.asg.finance.dto.BankDepositVoucherResponseDto;
import com.asg.finance.entity.GlBankDepositVoucherDtl;
import com.asg.finance.entity.GlBankDepositVoucherHdr;
import com.asg.finance.repository.GlBankDepositVoucherDtlRepository;
import com.asg.finance.repository.GlBankDepositVoucherHdrRepository;
import com.asg.finance.service.BankDepositVoucherService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankDepositVoucherServiceImpl implements BankDepositVoucherService {

    private final GlBankDepositVoucherHdrRepository hdrRepository;
    private final GlBankDepositVoucherDtlRepository dtlRepository;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final PrintService printService;
    private final DataSource dataSource;
    private final LovDataService lovService;
    private final LoggingService loggingService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public BankDepositVoucherResponseDto createBankDepositVoucher(BankDepositVoucherRequestDto request) {
        Long transactionPoid = createHeaderAndDetails(request);
        callUpdatePaymentProcedure(transactionPoid, request.getGroupPoid(), request.getCompanyPoid(), request.getType());
        
        // Get the saved entity for logging
        GlBankDepositVoucherHdr savedEntity = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Deposit Voucher", "transactionPoid", transactionPoid));
        
        // Log the creation
        String key = transactionPoid.toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), key);
        
        return getBankDepositVoucherById(transactionPoid);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Long createHeaderAndDetails(BankDepositVoucherRequestDto request) {
        hdrRepository.callBeforeSaveValidation(request.getCompanyPoid(), request.getBankPoid());

        GlBankDepositVoucherHdr hdr = GlBankDepositVoucherHdr.builder()
                .transactionDate(LocalDate.now())
                .groupPoid(request.getGroupPoid())
                .companyPoid(request.getCompanyPoid())
                .bankPoid(request.getBankPoid())
                .postingNarration(request.getPostingNarration())
                .remarks(request.getRemarks())
                .grandTotal(calculateGrandTotal(request.getDetails()))
                .refType(request.getType())
                .bankFilter(request.getBankFilter())
                .groupPosting(request.getGroupPosting() != null && request.getGroupPosting() ? "Y" : "N")
                .createdBy(getCurrentUser())
                .createdDate(LocalDateTime.now())
                .lastModifiedBy(getCurrentUser())
                .lastModifiedDate(LocalDateTime.now())
                .deleted("N")
                .build();

        GlBankDepositVoucherHdr savedHdr = hdrRepository.save(hdr);

        if (request.getDetails() != null) {
            String docId = UserContext.getDocumentId();
            String key = savedHdr.getTransactionPoid().toString();
            List<GlBankDepositVoucherDtl> details = new ArrayList<>();
            for (int i = 0; i < request.getDetails().size(); i++) {
                BankDepositVoucherDtlDto dto = request.getDetails().get(i);
                GlBankDepositVoucherDtl detail = convertToDetailEntity(dto, savedHdr.getTransactionPoid());
                detail.setDetRowId((long) (i + 1));
                details.add(detail);
            }
            dtlRepository.saveAll(details);
            
            details.forEach(detail -> {
                String logDetail = String.format("Row Created on Bank Deposit Voucher Detail with detRowId: %s", detail.getDetRowId());
                loggingService.createLogSummaryEntry(docId, key, logDetail);
            });
        }
        hdrRepository.flush();
        return savedHdr.getTransactionPoid();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void callUpdatePaymentProcedure(Long transactionPoid, Long groupPoid, Long companyPoid, String paymentType) {
        callMarkPaymentsCompleted(transactionPoid, groupPoid, companyPoid, paymentType);

    }

    private void callMarkPaymentsCompleted(Long transactionPoid, Long groupPoid, Long companyPoid, String paymentType) {
        List<GlBankDepositVoucherDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);
        hdrRepository.markPaymentsCompleted(transactionPoid, groupPoid, companyPoid, paymentType);
    }

    @Override
    @Transactional
    public BankDepositVoucherResponseDto updateBankDepositVoucher(Long transactionPoid, BankDepositVoucherRequestDto request) {
        GlBankDepositVoucherHdr hdr = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Deposit Voucher", "transactionPoid", transactionPoid));

        // Create a copy of the existing entity for logging
        GlBankDepositVoucherHdr oldEntity = new GlBankDepositVoucherHdr();
        BeanUtils.copyProperties(hdr, oldEntity);

        List<GlBankDepositVoucherDtl> existingDetails = dtlRepository.findByTransactionPoid(transactionPoid);
        for (GlBankDepositVoucherDtl detail : existingDetails) {
            hdrRepository.callChequeStatusValidation(detail.getRefDocRef(), detail.getRefDocPoid());
        }

        hdrRepository.callBeforeSaveValidation(request.getCompanyPoid(), request.getBankPoid());

        hdr.setTransactionDate(LocalDate.now());
        hdr.setBankPoid(request.getBankPoid());
        hdr.setPostingNarration(request.getPostingNarration());
        hdr.setRemarks(request.getRemarks());
        hdr.setGrandTotal(calculateGrandTotal(request.getDetails()));
        hdr.setRefType(request.getType());
        hdr.setBankFilter(request.getBankFilter());
        hdr.setGroupPosting(request.getGroupPosting() != null && request.getGroupPosting() ? "Y" : "N");
        hdr.setLastModifiedBy(getCurrentUser());
        hdr.setLastModifiedDate(LocalDateTime.now());

        hdrRepository.save(hdr);

        dtlRepository.deleteByTransactionPoid(transactionPoid);

        if (request.getDetails() != null) {
            List<GlBankDepositVoucherDtl> details = new ArrayList<>();
            for (int i = 0; i < request.getDetails().size(); i++) {
                BankDepositVoucherDtlDto dto = request.getDetails().get(i);
                GlBankDepositVoucherDtl detail = convertToDetailEntity(dto, transactionPoid);
                detail.setDetRowId((long) (i + 1));
                details.add(detail);
            }
            dtlRepository.saveAll(details);
        }
        callMarkPaymentsCompleted(transactionPoid, hdr.getGroupPoid(), hdr.getCompanyPoid(), request.getType());

        // Log the update
        String key = transactionPoid.toString();
        loggingService.logChanges(oldEntity, hdr, GlBankDepositVoucherHdr.class, 
                UserContext.getDocumentId(), key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
        return getBankDepositVoucherById(transactionPoid);
    }

    @Override
    public BankDepositVoucherResponseDto getBankDepositVoucherById(Long transactionPoid) {
        GlBankDepositVoucherHdr hdr = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Deposit Voucher", "transactionPoid", transactionPoid));

        List<GlBankDepositVoucherDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);

        return BankDepositVoucherResponseDto.builder()
                .transactionPoid(hdr.getTransactionPoid())
                .transactionDate(hdr.getTransactionDate())
                .docRef(hdr.getDocRef())
                .bankPoid(hdr.getBankPoid())
                .postingNarration(hdr.getPostingNarration())
                .remarks(hdr.getRemarks())
                .grandTotal(hdr.getGrandTotal())
                .groupPosting(hdr.getGroupPosting())
                .createdBy(hdr.getCreatedBy())
                .createdDate(hdr.getCreatedDate())
                .lastModifiedBy(hdr.getLastModifiedBy())
                .lastModifiedDate(hdr.getLastModifiedDate())
                .refType(hdr.getRefType())
                .bankFilter(hdr.getBankFilter())
                .details(details.stream().map(this::convertToDetailDto).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public void softDeleteBankDepositVoucher(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        GlBankDepositVoucherHdr hdr = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Bank Deposit Voucher", "transactionPoid", transactionPoid));

        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_BANK_DEPOSIT_VOUCHER_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                hdr.getTransactionDate()
        );
    }

    @Override
    public Map<String, Object> listBankDepositVouchers(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "DOC_REF", "TRANSACTION_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public List<BankDepositVoucherDtlDto> loadPendingPayments(Long bankPoid, String type, String bankFilter) {
        return hdrRepository.loadPendingPayments(bankPoid, type, bankFilter);
    }

    private BigDecimal calculateGrandTotal(List<BankDepositVoucherDtlDto> details) {
        if (details == null) return BigDecimal.ZERO;
        return details.stream()
                .map(BankDepositVoucherDtlDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private GlBankDepositVoucherDtl convertToDetailEntity(BankDepositVoucherDtlDto dto, Long transactionPoid) {
        return GlBankDepositVoucherDtl.builder()
                .transactionPoid(transactionPoid)
                .bankPoid(dto.getBankPoid())
                .pymtType(dto.getPymtType())
                .refDocPoid(dto.getRefDocPoid())
                .refDocRef(dto.getRefDocRef())
                .refDocId(dto.getRefDocId())
                .rcpDate(dto.getRcpDate())
                .chqAcName(dto.getChqAcName())
                .chqAcNo(dto.getChqAcNo())
                .chqCardNo(dto.getChqCardNo())
                .chqDate(dto.getChqDate())
                .amount(dto.getAmount())
                .remarks(dto.getRemarks())
                .selected(dto.getSelected() != null && dto.getSelected().length() == 1 ? dto.getSelected() : "Y")
                .chqSeqNum(dto.getChqSeqNum())
                .paymentMainPoid(dto.getPaymentMainPoid())
                .build();
    }

    private BankDepositVoucherDtlDto convertToDetailDto(GlBankDepositVoucherDtl entity) {
        return BankDepositVoucherDtlDto.builder()
                .detRowId(entity.getDetRowId())
                .bankDet(setBankDet(entity))
                .bankPoid(entity.getBankPoid())
                .pymtType(entity.getPymtType())
                .refDocPoid(entity.getRefDocPoid())
                .refDocRef(entity.getRefDocRef())
                .refDocId(entity.getRefDocId())
                .rcpDate(entity.getRcpDate())
                .chqAcName(entity.getChqAcName())
                .chqAcNo(entity.getChqAcNo())
                .chqCardNo(entity.getChqCardNo())
                .chqDate(entity.getChqDate())
                .amount(entity.getAmount())
                .remarks(entity.getRemarks())
                .selected(entity.getSelected())
                .chqSeqNum(entity.getChqSeqNum())
                .paymentMainPoid(entity.getPaymentMainPoid())
                .build();
    }

    private LovGetListDto setBankDet(GlBankDepositVoucherDtl entity) {
        if (entity.getBankPoid() != null) {
            return lovService.getDetailsByPoidAndLovName(entity.getBankPoid(),"BANK_MASTER_FOR_BDV");
        }
        return  null;
    }

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "400-109");
        params.put("SUB_CHEQUE", printService.load("Finance/GL/BankDepositVoucherPymt_subreport1.jrxml"));
        params.put("SUB_CASH", printService.load("Finance/GL/BankDepositVoucherGL_subreport1.jrxml"));
        JasperReport mainReport = printService.load("Finance/GL/BankDepositVoucherReport.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }
}
