package com.asg.finance.service.impl;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.finance.entity.GLMaster;
import com.asg.common.lib.exception.ValidationException;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.finance.dto.AdvancePettyCashDtlResponseDTO;
import com.asg.finance.dto.AdvancePettyCashHdrRequestDTO;
import com.asg.finance.dto.AdvancePettyCashHdrResponseDTO;
import com.asg.finance.dto.GlLedgerDTO;
import com.asg.finance.entity.AdvancePettyCashHdr;

import com.asg.finance.repository.AdvancePettyCashHdrRepository;
import com.asg.finance.repository.AdvancePettyCashDtlRepository;

import com.asg.finance.entity.AdvancePettyCashDtl;

import com.asg.finance.service.AdvancePettyCashHdrService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdvancePettyCashHdrServiceImpl implements AdvancePettyCashHdrService {
    
    private final AdvancePettyCashHdrRepository repository;
    private final AdvancePettyCashDtlRepository detailRepository;
    private final GLMasterRepository glMasterRepository;
    
    @Autowired
    private DocumentSearchService documentService;

    @Override
    public AdvancePettyCashHdrResponseDTO createAdvancePettyCash(AdvancePettyCashHdrRequestDTO request) {
        validateTransactionDate(request.getTransactionDate());
        validateClosedStatus(request.getStatus(), request.getClosedReason());
        try {
            AdvancePettyCashHdr entity = convertFromDtoToEntity(request);
            AdvancePettyCashHdr saved = repository.save(entity);
            return convertFromEntityToDto(saved);
        } catch (Exception ex) {
            String errorMessage = extractTriggerErrorMessage(ex);
            throw new ValidationException(errorMessage);
        }
    }

    @Override
    public AdvancePettyCashHdrResponseDTO updateAdvancePettyCash(Long transactionPoid, AdvancePettyCashHdrRequestDTO request) {
        AdvancePettyCashHdr existing = repository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Advance Petty Cash not found with ID: ", "transactionPoid", transactionPoid));
        
        if ("CLOSED".equals(existing.getStatus())) {
            throw new ValidationException("Cannot update a closed advance petty cash record");
        }
        
        validateTransactionDate(request.getTransactionDate());
        validateClosedStatus(request.getStatus(), request.getClosedReason());

        existing.setTransactionDate(request.getTransactionDate());
        existing.setPettyCashGlPoid(request.getPettyCashGlPoid());
        existing.setPayingTo(request.getPayingTo());
        existing.setIouAmount(request.getIouAmount());
        existing.setNarration(request.getNarration());
        existing.setStatus(request.getStatus());
        existing.setClosedReason(request.getClosedReason());
        
        if ("CLOSED".equals(request.getStatus())) {
            BigDecimal currentSettled = existing.getSettledAmount() != null ? existing.getSettledAmount() : BigDecimal.ZERO;
            BigDecimal currentBalance = existing.getBalanceAmount() != null ? existing.getBalanceAmount() : BigDecimal.ZERO;
            existing.setSettledAmount(currentSettled.add(currentBalance));
            existing.setBalanceAmount(BigDecimal.ZERO);
        }
        
        try {
            existing.setLastModifiedBy(getCurrentUser());
            existing.setLastModifiedDate(LocalDateTime.now());
            AdvancePettyCashHdr updated = repository.save(existing);
            return convertFromEntityToDto(updated);
        } catch (Exception ex) {
            String errorMessage = extractTriggerErrorMessage(ex);
            throw new ValidationException(errorMessage);
        }
    }

    @Override
    public AdvancePettyCashHdrResponseDTO getAdvancePettyCashById(Long transactionPoid) {
        AdvancePettyCashHdr entity = repository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Advance Petty Cash not found with ID: ", "transactionPoid", transactionPoid));
        return convertFromEntityToDto(entity);
    }

    @Override
    @Transactional
    public void softDeleteAdvancePettyCash(Long transactionPoid) {
        try {
            AdvancePettyCashHdr existing = repository.findByTransactionPoid(transactionPoid)
                    .orElseThrow(() -> new ResourceNotFoundException("Advance Petty Cash not found with ID: ", "transactionPoid", transactionPoid));

            existing.setDeleted("Y");
            existing.setLastModifiedDate(LocalDateTime.now());
            existing.setLastModifiedBy(getCurrentUser());
            repository.save(existing);
        }catch (Exception ex){
            String errorMessage = extractTriggerErrorMessage(ex);
            throw new ValidationException(errorMessage);
        }
    }

    @Override
    public Map<String, Object> listAdvancePettyCash(String documentId, FilterRequestDto request, Pageable pageable, LocalDate periodFrom, LocalDate periodTo) {
        validatePeriodDates(periodFrom, periodTo);
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveDateFilters(request,"TRANSACTION_DATE", periodFrom, periodTo);

        RawSearchResult raw = documentService.search(documentId, filters, operator, pageable, isDeleted,
                "DOC_REF",
                "TRANSACTION_POID");
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private static void validatePeriodDates(LocalDate periodFrom, LocalDate periodTo) {
        if((periodFrom == null && periodTo != null) || (periodFrom != null && periodTo == null)) {
            throw new IllegalArgumentException("Both startDate and endDate should be specified or both dates should be empty.");
        }
        if (periodFrom != null && periodTo != null && periodFrom.isAfter(periodTo)) {
            throw new IllegalArgumentException("Period From must not be after Period To");
        }
    }

    private AdvancePettyCashHdr convertFromDtoToEntity(AdvancePettyCashHdrRequestDTO dto) {
        AdvancePettyCashHdr entity = new AdvancePettyCashHdr();
        entity.setTransactionDate(dto.getTransactionDate());
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setCompanyPoid(UserContext.getCompanyPoid());
        entity.setPettyCashGlPoid(dto.getPettyCashGlPoid());
        entity.setPayingTo(dto.getPayingTo());
        entity.setIouAmount(dto.getIouAmount());
        entity.setNarration(dto.getNarration());
        entity.setStatus(dto.getStatus());
        entity.setClosedReason(dto.getClosedReason());
        
        if ("CLOSED".equals(dto.getStatus())) {
            entity.setSettledAmount(dto.getIouAmount() != null ? dto.getIouAmount() : BigDecimal.ZERO);
            entity.setBalanceAmount(BigDecimal.ZERO);
        } else {
            entity.setSettledAmount(BigDecimal.ZERO);
            entity.setBalanceAmount(dto.getIouAmount() != null ? dto.getIouAmount() : BigDecimal.ZERO);
        }
        entity.setDeleted("N");
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
        return entity;
    }

    private AdvancePettyCashHdrResponseDTO convertFromEntityToDto(AdvancePettyCashHdr entity) {
        AdvancePettyCashHdrResponseDTO dto = new AdvancePettyCashHdrResponseDTO();
        dto.setTransactionPoid(entity.getTransactionPoid());
        dto.setTransactionDate(entity.getTransactionDate());
        dto.setDocRef(entity.getDocRef());
        dto.setPettyCashGlPoid(entity.getPettyCashGlPoid());
        dto.setPayingTo(entity.getPayingTo());
        dto.setIouAmount(entity.getIouAmount());
        dto.setSettledAmount(entity.getSettledAmount());
        dto.setBalanceAmount(entity.getBalanceAmount());
        dto.setNarration(entity.getNarration());
        dto.setStatus(entity.getStatus());
        dto.setClosedReason(entity.getClosedReason());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        
        // Add GL Master details
        if (entity.getPettyCashGlPoid() != null) {
            GLMaster glMaster = glMasterRepository.findByGlPoid(entity.getPettyCashGlPoid()).orElse(null);
            if (glMaster != null) {
                GlLedgerDTO glLedgerDTO = new GlLedgerDTO(
                        glMaster.getGlPoid(),
                        glMaster.getGlCode(),
                        glMaster.getGlDescription()
                );
                dto.setPettyCashGlPoidDet(glLedgerDTO);
            }
        }
        
        // Add detail records if they exist
        List<AdvancePettyCashDtl> details = detailRepository.findByTransactionPoid(entity.getTransactionPoid());
        if (!details.isEmpty()) {
            List<AdvancePettyCashDtlResponseDTO> detailDtos = details.stream()
                    .map(this::convertDetailToDto)
                    .toList();
            dto.setDetails(detailDtos);
        }
        
        return dto;
    }
    
    private AdvancePettyCashDtlResponseDTO convertDetailToDto(AdvancePettyCashDtl detail) {
        return AdvancePettyCashDtlResponseDTO.builder()
                .detRowId(detail.getDetRowId())
                .transactionPoid(detail.getTransactionPoid())
                .documentDate(detail.getPettyCashTrnDate())
                .pettyCashReference(detail.getPettyCashRef())
                .amount(detail.getAmount())
                .remarks(detail.getPettyCashRemarks())
                .build();
    }

    private void validateTransactionDate(LocalDate transactionDate) {
        if (transactionDate != null && transactionDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Transaction date cannot be in future");
        }
    }

    private void validateClosedStatus(String status, String closedReason) {
        if ("CLOSED".equals(status) && (closedReason == null || closedReason.trim().isEmpty())) {
            throw new ValidationException("Closed reason is required when status is CLOSED");
        }
    }

    private String extractTriggerErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("ORA-20001")) {
                return "Changes allowed only within current Financial Period";
            } else if (message.contains("ORA-20002")) {
                if (message.contains("Transaction date can not update")) {
                    return "Transaction date cannot be updated";
                } else {
                    return "Changes allowed only within current Transaction Period";
                }
            }
        }
        return "Database validation failed: " + (message != null ? message : "Unknown error");
    }

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

}