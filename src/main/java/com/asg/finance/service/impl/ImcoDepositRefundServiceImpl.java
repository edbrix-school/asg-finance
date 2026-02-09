package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DetailsDto;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.response.GlPostingViewResponseDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.finance.dto.ImcoDepositRefundRequestDTO;
import com.asg.finance.dto.ImcoDepositRefundResponseDTO;
import com.asg.finance.dto.ImcoRefundLoadResponseDto;
import com.asg.finance.entity.GlobalLogSummary;
import com.asg.finance.entity.GlImcoChequeBillDtl;
import com.asg.finance.entity.GlImcoChequeRefundDtl;
import com.asg.finance.entity.GlImcoChequeRefundHdr;
import com.asg.finance.repository.GlobalLogSummaryRepository;
import com.asg.finance.repository.*;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.ImcoDepositRefundService;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImcoDepositRefundServiceImpl implements ImcoDepositRefundService {
    
    private final GlImcoChequeRefundHdrRepository hdrRepository;
    private final GlImcoChequeRefundDtlRepository dtlRepository;
    private final GlImcoChequeBillDtlRepository billDtlRepository;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final ImcoChequeDetailsRepository repository;
    private final ImcoDepositRefundRepository depositRefundRepository;
    private final ImcoSaveRefundRepository imcoSaveRefundRepository;
    private final LovDataService lovService;
    private final PrintService printService;
    private final DataSource dataSource;
    private final LoggingService loggingService;
    private final GlobalLogSummaryRepository globalLogSummaryRepository;

    
    @Override
    @Transactional
    public ImcoDepositRefundResponseDTO createImcoDepositRefund(ImcoDepositRefundRequestDTO request) {
        validateRequest(request);

        try {
            GlImcoChequeRefundHdr header = GlImcoChequeRefundHdr.builder()
                    .transactionDate(request.getTransactionDate())
                    .groupPoid(UserContext.getGroupPoid())
                    .companyPoid(UserContext.getCompanyPoid())
                    .docRef(request.getDocRef())
                    .remarks(request.getRemarks())
                    .grandTotal(request.getGrandTotal())
                    .blNumber(request.getBlNumber())
                    .receiptNum(request.getReceiptNum())
                    .payingTo(request.getPayingTo())
                    .createdBy(getCurrentUser())
                    .createdDate(LocalDateTime.now())
                    .lastModifiedBy(getCurrentUser())
                    .lastModifiedDate(LocalDateTime.now())
                    .deleted("N")
                    .build();

            final GlImcoChequeRefundHdr savedHeader = hdrRepository.save(header);
            final Long hdrPoid = savedHeader.getTransactionPoid();

            List<GlImcoChequeRefundDtl> refundDetails = request.getChequeRefundDetails().stream()
                    .map(dto -> GlImcoChequeRefundDtl.builder()
                            .transactionPoid(hdrPoid)
                            .detRowId(dto.getDetRowId() != null ? dto.getDetRowId() : 1)
                            .choPoid(dto.getChoPoid())
                            .choDate(dto.getChoDate())
                            .refDocId(dto.getRefDocId())
                            .refDocPoid(dto.getRefDocPoid())
                            .pymtType(dto.getPymtType())
                            .chqCardno(dto.getChqCardno())
                            .chqDate(dto.getChqDate())
                            .bankPoid(dto.getBankPoid())
                            .addressPoid(dto.getAddressPoid())
                            .chqAcName(dto.getChqAcName())
                            .chqAcNo(dto.getChqAcNo())
                            .amount(dto.getAmount())
                            .status(dto.getStatus())
                            .remarks(dto.getRemarks())
                            .createdBy(getCurrentUser())
                            .createdDate(LocalDateTime.now())
                            .lastModifiedBy(getCurrentUser())
                            .lastModifiedDate(LocalDateTime.now())
                            .oldRcpvno(dto.getOldRcpvno())
                            .rcpDate(dto.getRcpDate())
                            .refDocRef(dto.getRefDocRef())
                            .choDocId(dto.getChoDocId())
                            .paymentMainPoid(dto.getPaymentMainPoid())
                            .build())
                    .collect(Collectors.toList());

            List<GlImcoChequeRefundDtl> savedRefundDetails = dtlRepository.saveAll(refundDetails);
            
            savedRefundDetails.forEach(refundDetail -> {
                String logDetail = String.format("Row Created on Cheque Refund with detRowId: %s", refundDetail.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), hdrPoid.toString(), logDetail);
            });

            List<GlImcoChequeBillDtl> billDetails = request.getChequeBillDetails().stream()
                    .map(dto -> GlImcoChequeBillDtl.builder()
                            .transactionPoid(hdrPoid)
                            .detRowId(dto.getDetRowId() != null ? dto.getDetRowId() : 1)
                            .billRef(dto.getBillRef())
                            .billAmount(dto.getBillAmount())
                            .remarks(dto.getRemarks())
                            .createdBy(getCurrentUser())
                            .createdDate(LocalDateTime.now())
                            .lastModifiedBy(getCurrentUser())
                            .lastModifiedDate(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());

            List<GlImcoChequeBillDtl> savedBillDetails = billDtlRepository.saveAll(billDetails);
            
            savedBillDetails.forEach(billDetail -> {
                String logDetail = String.format("Row Created on Cheque Bill with detRowId: %s", billDetail.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), hdrPoid.toString(), logDetail);
            });

            callAfterSaveProcedure(savedHeader);

            String docId = UserContext.getDocumentId();
            String docKeyPoid = savedHeader.getTransactionPoid().toString();
            Timestamp now = new Timestamp(System.currentTimeMillis());
            String createdMessage = String.format("Created - - DOC:%s KEY:%s", docId, docKeyPoid);
            GlobalLogSummary headerLog = createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, createdMessage, now);
            globalLogSummaryRepository.save(headerLog);

            return buildResponse(savedHeader, refundDetails, billDetails);
        }catch (Exception ex) {
            String errorMessage = extractTriggerErrorMessage(ex);
            throw new ValidationException(errorMessage);
        }
    }

    private void validateRequest(ImcoDepositRefundRequestDTO request) {
        BigDecimal totalBillAmount = request.getChequeBillDetails().stream()
                .map(ImcoDepositRefundRequestDTO.ChequeBillDetailDTO::getBillAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalRefundAmount = request.getChequeRefundDetails().stream()
                .map(ImcoDepositRefundRequestDTO.ChequeRefundDetailDTO::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = totalBillAmount.add(totalRefundAmount);
        if (total.compareTo(BigDecimal.ZERO) != 0) {
            throw new ValidationException("Total Bill Amount (BillAmt) does not match the Receipt Amount (ReceiptAmt).");
        }
    }

    
    private ImcoDepositRefundResponseDTO buildResponse(GlImcoChequeRefundHdr header, 
                                                     List<GlImcoChequeRefundDtl> refundDetails,
                                                     List<GlImcoChequeBillDtl> billDetails) {
        DetailsDto receiptNumDtl = new DetailsDto(
                null,
                header.getReceiptNum(),
                header.getReceiptNum(),
                null,
                header.getReceiptNum(),
                null
        );
        return ImcoDepositRefundResponseDTO.builder()
                .poid(header.getTransactionPoid())
                .transactionDate(header.getTransactionDate())
                .groupPoid(header.getGroupPoid())
                .companyPoid(header.getCompanyPoid())
                .docRef(header.getDocRef())
                .remarks(header.getRemarks())
                .grandTotal(header.getGrandTotal())
                .blNumber(header.getBlNumber())
                .receiptNum(header.getReceiptNum())
                .receiptNumDtl(receiptNumDtl)
                .payingTo(header.getPayingTo())
                .createdBy(header.getCreatedBy())
                .createdDate(header.getCreatedDate())
                .lastModifiedBy(header.getLastModifiedBy())
                .lastModifiedDate(header.getLastModifiedDate())
                .chequeRefundDetails(refundDetails.stream()
                        .map(this::mapToRefundDetailResponse)
                        .collect(Collectors.toList()))
                .chequeBillDetails(billDetails.stream()
                        .map(this::mapToBillDetailResponse)
                        .collect(Collectors.toList()))
                .build();
    }
    
    private ImcoDepositRefundResponseDTO.ChequeRefundDetailResponseDTO mapToRefundDetailResponse(GlImcoChequeRefundDtl entity) {
        return ImcoDepositRefundResponseDTO.ChequeRefundDetailResponseDTO.builder()
                .poid(entity.getTransactionPoid())
                .detRowId(entity.getDetRowId())
                .choPoid(entity.getChoPoid())
                .choDate(entity.getChoDate())
                .refDocId(entity.getRefDocId())
                .refDocPoid(entity.getRefDocPoid())
                .pymtType(entity.getPymtType())
                .chqCardno(entity.getChqCardno())
                .chqDate(entity.getChqDate())
                .bankPoid(entity.getBankPoid())
                .addressPoid(entity.getAddressPoid())
                .chqAcName(entity.getChqAcName())
                .chqAcNo(entity.getChqAcNo())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .oldRcpvno(entity.getOldRcpvno())
                .rcpDate(entity.getRcpDate())
                .refDocRef(entity.getRefDocRef())
                .choDocId(entity.getChoDocId())
                .paymentMainPoid(entity.getPaymentMainPoid())
                .build();
    }
    
    private ImcoDepositRefundResponseDTO.ChequeBillDetailResponseDTO mapToBillDetailResponse(GlImcoChequeBillDtl entity) {
        return ImcoDepositRefundResponseDTO.ChequeBillDetailResponseDTO.builder()
                .poid(entity.getTransactionPoid())
                .detRowId(entity.getDetRowId())
                .billRef(entity.getBillRef())
                .billAmount(entity.getBillAmount())
                .remarks(entity.getRemarks())
                .build();
    }
    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    public ImcoDepositRefundResponseDTO getImcoDepositRefundById(Long transactionPoid) {
        GlImcoChequeRefundHdr header = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Refund header not found for ID: ", "transactionPoid",  transactionPoid));
        List<GlImcoChequeRefundDtl> refundDetails = dtlRepository.findByTransactionPoid(transactionPoid);
        List<GlImcoChequeBillDtl> billDetails = billDtlRepository.findByTransactionPoid(transactionPoid);
        return buildResponse(header, refundDetails, billDetails);
    }

    @Override
    @Transactional
    public void softDeleteImcoDepositRefund(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        GlImcoChequeRefundHdr existing = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Refund header not found for ID: ", "transactionPoid", transactionPoid));
        
        documentDeleteService.deleteDocument(
                transactionPoid,
                "GL_IMCO_CHEQUE_REFUND_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                existing.getTransactionDate()
        );
    }

    @Override
    public Map<String, Object> listImcoDepositRefund(String documentId, FilterRequestDto request, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(documentId, filters, operator, pageable, isDeleted,
                "TRANSACTION_POID",
                "DOC_REF");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    public ImcoRefundLoadResponseDto getChequeDetails(Long receiptPoid, String receiptNumber) throws SQLException {
        if (receiptPoid == null) {
            throw new IllegalArgumentException("Receipt number cannot be null");
        }

        if (!isValidReceipt(receiptPoid, receiptNumber)) {
            throw new ResourceNotFoundException("Not found: ","Receipt number or Receipt number", String.format("%s, %s", receiptNumber, receiptPoid));
        }

        ImcoRefundLoadResponseDto result = repository.fetchChequeAndBillDetails(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                getCurrentUser(),
                receiptPoid,
                receiptNumber
        );
        return result;
    }

    private boolean isValidReceipt(Long receiptPoid, String receiptNumber) {
        return depositRefundRepository.isValidReceipt(receiptPoid, receiptNumber, UserContext.getCompanyPoid());
    }

    public GlPostingViewResponseDto getGlPostingDetails(String docId, Long transactionPoid) {

       hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Refund header not found for ID: ", "transactionPoid",  transactionPoid));

        GlPostingViewResponseDto result = depositRefundRepository.fetchGlPostingDetails(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                docId,
                transactionPoid
        );

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void callAfterSaveProcedure(GlImcoChequeRefundHdr header) {
        String payingTo = imcoSaveRefundRepository.callImcoRefundLoadProcedure(
                header.getGroupPoid(),
                header.getCompanyPoid(),
                getCurrentUser(),
                header.getReceiptNum(),
                header.getBlNumber()
        );
        if (payingTo != null && !payingTo.equals(header.getPayingTo())) {
            header.setPayingTo(payingTo);
            hdrRepository.save(header);
        }
    }

    private String extractTriggerErrorMessage(Exception e) {
        if (e == null || e.getMessage() == null) {
            return "Unknown error occurred while saving data.";
        }

        String message = e.getMessage();
        if (message.contains("ORA-20001")) {
            return "Changes allowed only within the current Financial Period.";
        }
        if (message.contains("ORA-20002") && message.toLowerCase().contains("transaction date")) {
            return "Transaction date cannot be updated outside the valid period.";
        }
        if (message.contains("ORA-04088")) {
            return "Error occurred while executing database trigger.";
        }

        return "Database validation failed: " + (message != null ? message : "Unknown error");
    }

    private GlobalLogSummary createSummaryLogEntry(LogDetailsEnum logDetailsEnum, String docId, String docKeyPoid, String customMessage, Timestamp logDateTime) {
        GlobalLogSummary summary = new GlobalLogSummary();
        summary.setLogUserPoid(UserContext.getUserPoid());
        summary.setLogDateTime(logDateTime != null ? logDateTime : new Timestamp(System.currentTimeMillis()));
        summary.setLogDocId(docId);
        summary.setLogDocKeyPoid(docKeyPoid);
        summary.setLogDetails(customMessage);
        return summary;
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "400-108");
        JasperReport mainReport = printService.load("Finance/GL/IMCO_Refund_Receipt.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

}