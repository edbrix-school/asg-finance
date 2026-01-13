package com.asg.finance.service.impl;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.dto.ApPaymentRequestHdrRequestDto;
import com.asg.finance.dto.ApPaymentRequestHdrResponseDto;
import com.asg.finance.dto.ApPaymentRequestMapper;
import com.asg.finance.entity.ApPaymentRequestDtl;
import com.asg.finance.entity.ApPaymentRequestHdr;
import com.asg.finance.repository.ApPaymentRequestCustomRepository;
import com.asg.finance.repository.ApPaymentRequestDtlRepository;
import com.asg.finance.repository.ApPaymentRequestHdrRepository;
import com.asg.finance.service.ApPaymentRequestService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Transactional
public class ApPaymentRequestServiceImpl implements ApPaymentRequestService {

    private final ApPaymentRequestHdrRepository hdrRepository;
    private final ApPaymentRequestDtlRepository dtlRepository;
    private final DocumentSearchService documentService;
    private final ApPaymentRequestCustomRepository aapPaymentRequestCustomRepository;

    @Override
    public ApPaymentRequestHdrResponseDto create(ApPaymentRequestHdrRequestDto requestDto) {
        ApPaymentRequestHdr hdr =
                ApPaymentRequestMapper.toEntity(requestDto, null);

        hdr.setCreatedBy(getCurrentUser());
        hdr.setCreatedDate(LocalDateTime.now());

        // 🔑 ID GENERATED HERE
        hdr = hdrRepository.save(hdr);

        Long transactionPoid = hdr.getTransactionPoid();

        List<ApPaymentRequestDtl> details =
                requestDto.getDetails().stream()
                        .map(d -> ApPaymentRequestMapper
                                .toDtlEntity(transactionPoid, d))
                        .collect(Collectors.toList());

        dtlRepository.saveAll(details);

        return ApPaymentRequestMapper.toResponse(hdr, details);
    }

    @Override
    public ApPaymentRequestHdrResponseDto update(
            Long transactionPoid,
            ApPaymentRequestHdrRequestDto requestDto
    ) {
        ApPaymentRequestHdr hdr = hdrRepository
                .findById(transactionPoid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment request not found","transactionPoid",transactionPoid));

        hdr.setDocRef(requestDto.getDocRef());
        hdr.setRefType(requestDto.getRefType());
        hdr.setCurrencyCode(requestDto.getCurrencyCode());
        hdr.setCurrencyRate(requestDto.getCurrencyRate());
        hdr.setPayeePoid(requestDto.getPayeePoid());
        hdr.setRequestedBy(requestDto.getRequestedBy());
        hdr.setRemarks(requestDto.getRemarks());
        hdr.setTotalAmount(requestDto.getTotalAmount());
        hdr.setLastModifiedBy(getCurrentUser());
        hdr.setLastModifiedDate(LocalDateTime.now());


        hdrRepository.save(hdr);

        /* Replace details */
        dtlRepository.deleteByIdTransactionPoid(transactionPoid);

        List<ApPaymentRequestDtl> details =
                requestDto.getDetails().stream()
                        .map(d -> ApPaymentRequestMapper
                                .toDtlEntity(transactionPoid, d))
                        .toList();

        dtlRepository.saveAll(details);

        return ApPaymentRequestMapper.toResponse(hdr, details);
    }

    @Override
    @Transactional
    public ApPaymentRequestHdrResponseDto findById(Long transactionPoid) {

        ApPaymentRequestHdr hdr = hdrRepository.findById(transactionPoid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment request not found","transactionPoid",transactionPoid));

        List<ApPaymentRequestDtl> details =
                dtlRepository.findByIdTransactionPoid(transactionPoid);

        return ApPaymentRequestMapper.toResponse(hdr, details);
    }

    @Override
    public void delete(Long transactionPoid) {

        ApPaymentRequestHdr hdr = hdrRepository
                .findById(transactionPoid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment request not found","transactionPoid",transactionPoid));

        hdr.setDeleted("Y");
        hdr.setCreatedBy(getCurrentUser());
        hdr.setCreatedDate(LocalDateTime.now());
        hdr.setLastModifiedBy(getCurrentUser());
        hdr.setLastModifiedDate(LocalDateTime.now());

        hdrRepository.save(hdr);
    }

    @Override
    public Map<String, Object> listPaymentRequest(String documentId, FilterRequestDto request, Pageable pageable, LocalDate periodFrom, LocalDate periodTo) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveDateFilters(request,"TRANSACTION_DATE", periodFrom, periodTo);

        RawSearchResult raw = documentService.search(documentId, filters, operator, pageable, isDeleted,
                "DOC_REF",
                "TRANSACTION_POID");
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public Map<String, Object> createFromPo(String poPoid) {

        return aapPaymentRequestCustomRepository.createFromPo(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                poPoid
        );
    }

    @Override
    public Map<String, Object> createFromFf(String ffPoid) {

        return aapPaymentRequestCustomRepository.createFromFf(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                ffPoid
        );
    }

    @Override
    public Map<String, Object> createFromFda(String fdaPoid) {

        return aapPaymentRequestCustomRepository.createFromFda(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                fdaPoid
        );
    }

    @Override
    public Map<String, Object> createFromMta(String poPoid) {

        return aapPaymentRequestCustomRepository.createFromMta(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                poPoid
        );

    }


}
