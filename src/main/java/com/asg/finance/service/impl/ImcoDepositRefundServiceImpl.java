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
import com.asg.finance.annotation.PerformGlPosting;
import com.asg.finance.dto.ImcoDepositRefundRequestDTO;
import com.asg.finance.dto.ImcoDepositRefundResponseDTO;
import com.asg.finance.dto.ImcoRefundLoadResponseDto;
import com.asg.finance.entity.GlImcoChequeBillDtl;
import com.asg.finance.entity.GlImcoChequeRefundDtl;
import com.asg.finance.entity.GlImcoChequeRefundHdr;
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
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
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
        private final PrintService printService;
        private final DataSource dataSource;
        private final LoggingService loggingService;

        @PersistenceContext
        private EntityManager entityManager;

        @Override
        @Transactional
        @PerformGlPosting
        public ImcoDepositRefundResponseDTO createImcoDepositRefund(ImcoDepositRefundRequestDTO request) {
                validateRequest(request);

                try {
                        GlImcoChequeRefundHdr header = GlImcoChequeRefundHdr.builder()
                                        .transactionDate(request.getTransactionDate())
                                        .groupPoid(UserContext.getGroupPoid())
                                        .companyPoid(UserContext.getCompanyPoid())
                                        .docRef(request.getDocRef())
                                        .remarks(request.getRemarks())
                                        .blNumber(request.getBlNumber())
                                        .receiptNum(request.getReceiptNum())
                                        .payingTo(request.getPayingTo())
                                        .deleted("N")
                                        .build();

                        final GlImcoChequeRefundHdr savedHeader = hdrRepository.save(header);
                        hdrRepository.flush();
                        entityManager.refresh(savedHeader); // Get trigger-generated DOC_REF
                        final Long hdrPoid = savedHeader.getTransactionPoid();

                        // Log header creation FIRST
                        loggingService.createLogSummaryEntry(
                                UserContext.getDocumentId(),
                                hdrPoid.toString(),
                                String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), savedHeader.getDocRef())
                        );

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
                                                        .oldRcpvno(dto.getOldRcpvno())
                                                        .rcpDate(dto.getRcpDate())
                                                        .refDocRef(dto.getRefDocRef())
                                                        .choDocId(dto.getChoDocId())
                                                        .paymentMainPoid(dto.getPaymentMainPoid())
                                                        .build())
                                        .collect(Collectors.toList());

                        List<GlImcoChequeRefundDtl> savedRefundDetails = dtlRepository.saveAll(refundDetails);

                        savedRefundDetails.forEach(refundDetail -> {
                                String logDetail = String.format("Row Created on Cheque Refund with detRowId: %s",
                                                refundDetail.getDetRowId());
                                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), hdrPoid.toString(),
                                                logDetail);
                        });

                        List<GlImcoChequeBillDtl> billDetails = request.getChequeBillDetails().stream()
                                        .map(dto -> GlImcoChequeBillDtl.builder()
                                                        .transactionPoid(hdrPoid)
                                                        .detRowId(dto.getDetRowId() != null ? dto.getDetRowId() : 1)
                                                        .billRef(dto.getBillRef())
                                                        .billAmount(dto.getBillAmount())
                                                        .remarks(dto.getRemarks())
                                                        .build())
                                        .collect(Collectors.toList());

                        List<GlImcoChequeBillDtl> savedBillDetails = billDtlRepository.saveAll(billDetails);

                        savedBillDetails.forEach(billDetail -> {
                                String logDetail = String.format("Row Created on Cheque Bill with detRowId: %s",
                                                billDetail.getDetRowId());
                                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), hdrPoid.toString(),
                                                logDetail);
                        });

                        dtlRepository.flush();
                        billDtlRepository.flush();

                        callAfterSaveProcedure(savedHeader, request.getDocRef());

                        return buildResponse(savedHeader, savedRefundDetails, savedBillDetails);
                } catch (Exception ex) {
                        String errorMessage = extractTriggerErrorMessage(ex);
                        throw new ValidationException(errorMessage);
                }
        }

        private void validateRequest(ImcoDepositRefundRequestDTO request) {
                if (request.getBlNumber() == null || request.getBlNumber().trim().isEmpty()) {
                        throw new ValidationException("BL number not entered, please enter BL number");
                }
                if (request.getReceiptNum() == null || request.getReceiptNum().trim().isEmpty()) {
                        throw new ValidationException("Receipt not entered, please enter receipt number");
                }
                if (hdrRepository.countByReceiptNum(request.getReceiptNum().trim()) > 0) {
                        throw new ValidationException(
                                        "A refund already exists with receipt number " + request.getReceiptNum().trim());
                }

                BigDecimal totalBillAmount = request.getChequeBillDetails().stream()
                                .map(ImcoDepositRefundRequestDTO.ChequeBillDetailDTO::getBillAmount)
                                .filter(amount -> amount != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalRefundAmount = request.getChequeRefundDetails().stream()
                                .map(ImcoDepositRefundRequestDTO.ChequeRefundDetailDTO::getAmount)
                                .filter(amount -> amount != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // ADF logic: BillAmt = sum(-BillAmount), ReceiptAmt = sum(Amount). Match if
                // BillAmt == ReceiptAmt.
                // Spring logic: BillAmt + ReceiptAmt == 0.
                BigDecimal total = totalBillAmount.add(totalRefundAmount);
                if (total.compareTo(BigDecimal.ZERO) != 0) {
                        // Note: In ADF, BillAmt is displayed as positive (multiplied by -1).
                        // To provide a consistent message, we use absolute values or follow the sum
                        // logic.
                        BigDecimal billAmtDisplay = totalBillAmount.negate();
                        throw new ValidationException(
                                        "Total Bill amount (" + billAmtDisplay
                                                        + ") is not matched with receipt Amount (" + totalRefundAmount
                                                        + ")");
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
                                null);
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

        private ImcoDepositRefundResponseDTO.ChequeRefundDetailResponseDTO mapToRefundDetailResponse(
                        GlImcoChequeRefundDtl entity) {
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

        private ImcoDepositRefundResponseDTO.ChequeBillDetailResponseDTO mapToBillDetailResponse(
                        GlImcoChequeBillDtl entity) {
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

        @Override
        public ImcoDepositRefundResponseDTO getImcoDepositRefundById(Long transactionPoid) {
                GlImcoChequeRefundHdr header = hdrRepository.findByTransactionPoid(transactionPoid)
                                .orElseThrow(() -> new ResourceNotFoundException("Refund header not found for ID: ",
                                                "transactionPoid",
                                                transactionPoid));
                List<GlImcoChequeRefundDtl> refundDetails = dtlRepository.findByTransactionPoid(transactionPoid);
                List<GlImcoChequeBillDtl> billDetails = billDtlRepository.findByTransactionPoid(transactionPoid);
                return buildResponse(header, refundDetails, billDetails);
        }

        @Override
        @Transactional
        public void softDeleteImcoDepositRefund(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
                GlImcoChequeRefundHdr existing = hdrRepository.findByTransactionPoid(transactionPoid)
                                .orElseThrow(() -> new ResourceNotFoundException("Refund header not found for ID: ",
                                                "transactionPoid",
                                                transactionPoid));

                documentDeleteService.deleteDocument(
                                transactionPoid,
                                "GL_IMCO_CHEQUE_REFUND_HDR",
                                "TRANSACTION_POID",
                                deleteReasonDto,
                                existing.getTransactionDate());
        }

        @Override
        public Map<String, Object> listImcoDepositRefund(String documentId, FilterRequestDto request,
                        LocalDate startDate,
                        LocalDate endDate, Pageable pageable) {
                String operator = documentService.resolveOperator(request);
                String isDeleted = documentService.resolveIsDeleted(request);
                List<FilterDto> filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", startDate,
                                endDate);

                RawSearchResult raw = documentService.search(documentId, filters, operator, pageable, isDeleted,
                                "TRANSACTION_POID",
                                "DOC_REF");

                Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

                return PaginationUtil.wrapPage(page, raw.displayFields());
        }

        @Override
        public ImcoRefundLoadResponseDto getChequeDetails(Long receiptPoid, String receiptNumber) throws SQLException {
                if (receiptPoid == null) {
                        throw new IllegalArgumentException("Receipt number cannot be null");
                }

                if (!isValidReceipt(receiptPoid, receiptNumber)) {
                        throw new ResourceNotFoundException("Not found: ", "Receipt number or Receipt number",
                                        String.format("%s, %s", receiptNumber, receiptPoid));
                }

                ImcoRefundLoadResponseDto result = repository.fetchChequeAndBillDetails(
                                UserContext.getGroupPoid(),
                                UserContext.getCompanyPoid(),
                                getCurrentUser(),
                                receiptPoid,
                                receiptNumber);
                return result;
        }

        private boolean isValidReceipt(Long receiptPoid, String receiptNumber) {
                return depositRefundRepository.isValidReceipt(receiptPoid, receiptNumber, UserContext.getCompanyPoid());
        }

        @Override
        public GlPostingViewResponseDto getGlPostingDetails(String docId, Long transactionPoid) {

                hdrRepository.findByTransactionPoid(transactionPoid)
                                .orElseThrow(() -> new ResourceNotFoundException("Refund header not found for ID: ",
                                                "transactionPoid",
                                                transactionPoid));

                GlPostingViewResponseDto result = depositRefundRepository.fetchGlPostingDetails(
                                UserContext.getGroupPoid(),
                                UserContext.getCompanyPoid(),
                                docId,
                                transactionPoid);

                return result;
        }

        @Transactional
        protected void callAfterSaveProcedure(GlImcoChequeRefundHdr header, String docRef) {
                String result = imcoSaveRefundRepository.callImcoRefundAfterSave(
                                header.getGroupPoid(),
                                header.getCompanyPoid(),
                                UserContext.getUserPoid(),
                                "400-108",
                                header.getTransactionPoid(),
                                docRef);

                if (result != null && !result.toUpperCase().contains("SUCCESS")) {
                        throw new ValidationException("Some error occured after save procedure " + result);
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


        @Override
        public byte[] print(Long transactionPoid) throws Exception {
                Map<String, Object> params = printService.buildBaseParams(transactionPoid, "400-108");
                JasperReport mainReport = printService.load("Finance/GL/IMCO_Refund_Receipt.jrxml");
                return printService.fillReportToPdf(mainReport, params, dataSource);
        }

}