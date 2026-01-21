package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.dto.BankPayeeRequest;
import com.asg.finance.dto.BankPayeeResponse;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.entity.BankPayee;
import com.asg.finance.repository.BankPayeeRepository;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.IBankPayeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BankPayeeServiceImpl implements IBankPayeeService {

    @Autowired
    private BankPayeeRepository repository;

    @Autowired
    private DocumentSearchService documentService;

    @Autowired
    private LoggingService loggingService;

    @Autowired
    private DocumentDeleteService documentDeleteService;

    @Transactional
    public BankPayeeResponse createPayee(BankPayeeRequest request) {
        repository.findByPayingName(request.getPayingName()).ifPresent(p -> {
            throw new ResourceAlreadyExistsException("Paying Name", request.getPayingName());
        });
        // validate active flag
        if (request.getActive() != null && !request.getActive().matches("Y|N")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active must be Y or N");
        }
        BankPayee payee = new BankPayee();
        payee.setPayingName(request.getPayingName());
        payee.setPayingName2(request.getPayingName2());
        payee.setRemarks(request.getRemarks());
        payee.setActive(request.getActive() != null ? request.getActive() : "N");
        payee.setDeleted("N");
        payee.setSeqNo(request.getSeqNo());
        payee.setCreatedBy(ASGHelperUtils.getCurrentUser());
        payee.setCreatedDate(LocalDateTime.now());

        BankPayee saved = repository.save(payee);

        // Log the creation
        String key = saved.getPayingPoid().toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), key);

        BankPayeeResponse response = new BankPayeeResponse();
        response.setPoid(saved.getPayingPoid());
        response.setPayingName(saved.getPayingName());
        response.setPayingName2(saved.getPayingName2());
        response.setRemarks(saved.getRemarks());
        response.setActive(saved.getActive());
        response.setSeqNo(saved.getSeqNo());
        response.setCreatedBy(saved.getCreatedBy());
        response.setCreatedDate(saved.getCreatedDate());

        return response;
    }
    @Transactional(readOnly = true)
    public BankPayeeResponse getPayeeById(Long payingPoid) {
        BankPayee payee = repository.findByPayingPoid(payingPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Bankpay Master","payingPoid ",payingPoid));
        BankPayeeResponse response = new BankPayeeResponse();
        response.setPoid(payee.getPayingPoid());
        response.setPayingName(payee.getPayingName());
        response.setPayingName2(payee.getPayingName2());
        response.setRemarks(payee.getRemarks());
        response.setActive(payee.getActive());
        response.setSeqNo(payee.getSeqNo());
        response.setCreatedBy(payee.getCreatedBy());
        response.setCreatedDate(payee.getCreatedDate());
        return response;
    }

    public void softDeleteBypPayingPoid(Long payingPoid, DeleteReasonDto deleteReasonDto) {
        BankPayee entity = repository.findByPayingPoidAndDeleted(payingPoid , "N")
                .orElseThrow(() -> new RuntimeException("Payee with ID " + payingPoid + " not found"));
        
        documentDeleteService.deleteDocument(
                payingPoid,
                "GL_PAYING_TO_MASTER",
                "PAYING_POID",
                deleteReasonDto,
                null
        );
    }

    @Override
    @Transactional
    public BankPayeeResponse updatePayee(Long payingPoid, BankPayeeRequest request) {
        BankPayee entity = repository.findByPayingPoidAndDeleted(payingPoid, "N")
                .orElseThrow(() -> new RuntimeException("Payee not found"));

        // Create a copy of the existing entity for logging
        BankPayee oldEntity = new BankPayee();
        BeanUtils.copyProperties(entity, oldEntity);

        if (request.getPayingName() != null &&
                !request.getPayingName().equalsIgnoreCase(entity.getPayingName())) {

            boolean exists = repository.existsByPayingNameIgnoreCaseAndDeleted(request.getPayingName(), "N");
            if (exists) {
                throw new IllegalArgumentException("Payee Name already exists");
            }
            entity.setPayingName(request.getPayingName());
        }

        entity.setPayingName2(request.getPayingName2());

        entity.setRemarks(request.getRemarks());


        if (request.getActive() != null) {
            if (!request.getActive().matches("Y|N")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active must be Y or N");
            }
            entity.setActive(request.getActive());
        }

        entity.setSeqNo(request.getSeqNo());

        entity.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());

        repository.save(entity);

        // Log the update
        String key = entity.getPayingPoid().toString();
        loggingService.logChanges(oldEntity, entity, BankPayee.class, 
                UserContext.getDocumentId(), key, LogDetailsEnum.MODIFIED, "PAYING_POID");

        BankPayeeResponse response = new BankPayeeResponse();
        response.setPoid(entity.getPayingPoid());
        response.setPayingName(entity.getPayingName());
        response.setPayingName2(entity.getPayingName2());
        response.setRemarks(entity.getRemarks());
        response.setActive(entity.getActive());
        response.setActive("Y".equalsIgnoreCase(entity.getActive()) ? "Y" : "N");
        response.setSeqNo(entity.getSeqNo());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedDate(entity.getCreatedDate());

        return response;
    }

    @Override
    public Map<String, Object> listPayees(String documentId, FilterRequestDto request, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(
                documentId,
                filters,
                operator,
                pageable,
                isDeleted,
                "PAYING_NAME",  // label
                "PAYING_POID"   // value
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }



}
