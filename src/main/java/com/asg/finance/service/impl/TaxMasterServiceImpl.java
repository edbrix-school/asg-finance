package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.finance.entity.GLMaster;
import com.asg.finance.entity.TaxMaster;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.finance.repository.TaxMasterRepository;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.finance.dto.GlLedgerDTO;
import com.asg.finance.dto.TaxMasterRequestDTO;
import com.asg.finance.dto.TaxMasterResponseDTO;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.TaxMasterService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class TaxMasterServiceImpl implements TaxMasterService {
    private final TaxMasterRepository repository;
    private final GLMasterRepository glMasterRepository;
    private final LoggingService loggingService;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;

    public TaxMasterResponseDTO createTaxMaster(TaxMasterRequestDTO request) {
        if (repository.existsByTaxCode(request.getTaxCode())) {
            throw new ValidationException("Tax Code already exists: " + request.getTaxCode());
        }

        validateRequest(request);
        TaxMaster entity = TaxMaster.builder()
                .taxCode(request.getTaxCode())
                .taxName(request.getTaxName())
                .taxName2(request.getTaxName2())
                .percentage(request.getPercentage())
                .taxType(request.getTaxType())
                .glType(request.getGlType())
                .glLedgerPoid(request.getGlLedgerPoid())
                .taxCategory(request.getTaxCategory())
                .active(request.getActive())
                .seqNo(request.getSeqNo())
                .deleted("N")
                .groupPoid(request.getGroupPoid() != null ? request.getGroupPoid() : 1L)
                .build();

        TaxMaster saved = repository.save(entity);
        
        // Log the creation
        String key = saved.getTaxPoid().toString();
        String docId = UserContext.getDocumentId();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);
        return getTaxMasterResponseDTO(saved);
    }

    public TaxMasterResponseDTO updateTaxMaster(Long taxPoid, TaxMasterRequestDTO request) {
        TaxMaster existing = repository.findByTaxPoid(taxPoid)
                .orElseThrow(() -> new ValidationException("Tax Master not found with id: " + taxPoid));

        // Create a copy of the existing entity for logging
        TaxMaster oldEntity = new TaxMaster();
        BeanUtils.copyProperties(existing, oldEntity);

        if (!existing.getTaxCode().equals(request.getTaxCode()) &&
                repository.existsByTaxCodeAndTaxPoidNot(request.getTaxCode(),taxPoid )) {
            throw new ValidationException("Tax Code already exists: " + request.getTaxCode());
        }

        validateRequest(request);
        existing.setTaxCode(request.getTaxCode());
        existing.setTaxName(request.getTaxName());
        existing.setTaxName2(request.getTaxName2());
        existing.setPercentage(request.getPercentage());
        existing.setTaxType(request.getTaxType());
        existing.setGlType(request.getGlType());
        existing.setGlLedgerPoid(request.getGlLedgerPoid());
        existing.setTaxCategory(request.getTaxCategory());
        existing.setActive(request.getActive());
        existing.setSeqNo(request.getSeqNo());

        TaxMaster updated = repository.save(existing);
        
        // Log the update
        String key = updated.getTaxPoid().toString();
        String docId = UserContext.getDocumentId();
        loggingService.logChanges(oldEntity, updated, TaxMaster.class,
                docId, key, LogDetailsEnum.MODIFIED, "TAX_POID");

        return getTaxMasterResponseDTO(updated);
    }

    private void validateRequest(TaxMasterRequestDTO request) {
        if (!request.getTaxType().matches("INPUT_VAT|OUTPUT_VAT")) {
            throw new ValidationException("Invalid Tax Type. Allowed: INPUT_VAT, OUTPUT_VAT");
        }
        if (!request.getGlType().matches("DR|CR")) {
            throw new ValidationException("Invalid GL Type. Allowed: DR, CR");
        }
    }

    private TaxMasterResponseDTO getTaxMasterResponseDTO(TaxMaster taxMaster) {
        GLMaster glMasterRecord = glMasterRepository.findByGlPoid(taxMaster.getGlLedgerPoid())
                .orElseThrow(() -> new ResourceNotFoundException("GL Master", "glPoid", taxMaster.getGlLedgerPoid()));

        GlLedgerDTO glLedgerDTO = new GlLedgerDTO(
                glMasterRecord.getGlPoid(),
                glMasterRecord.getGlCode(),
                glMasterRecord.getGlDescription()
        );

        TaxMasterResponseDTO taxMasterResponseDTO = new TaxMasterResponseDTO();
        BeanUtils.copyProperties(taxMaster, taxMasterResponseDTO);
        taxMasterResponseDTO.setGlLedger(glLedgerDTO);
        taxMasterResponseDTO.setGlLedgerPoid(glMasterRecord.getGlPoid());
        return taxMasterResponseDTO;
    }

    public TaxMasterResponseDTO getTaxMasterById(Long taxPoid) {
        if (!repository.existsByTaxPoid(taxPoid)) {
            throw new ResourceNotFoundException("Tax Master", "taxMasterPoid", taxPoid);
        }

        TaxMaster taxMaster = repository.findByTaxPoid(taxPoid)
                .orElseThrow(() -> new ResourceNotFoundException("TaxPoid", "taxPoid", taxPoid));

        return getTaxMasterResponseDTO(taxMaster);
    }

    public TaxMasterDto getTaxMasterDtoById(Long taxPoid) {
        TaxMaster taxMaster = repository.findByTaxPoid(taxPoid)
                .orElseThrow(() -> new ResourceNotFoundException("TaxMaster", "taxPoid", taxPoid));
        return mapToDto(taxMaster);
    }

    public List<TaxMasterDto> getTaxMasterDtosByIds(List<Long> taxPoids) {
        List<TaxMaster> taxMasters = repository.findByTaxPoidIn(taxPoids.stream()
                .collect(java.util.stream.Collectors.toSet()));
        return taxMasters.stream()
                .map(this::mapToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    private com.asg.common.lib.dto.TaxMasterDto mapToDto(TaxMaster entity) {
        return com.asg.common.lib.dto.TaxMasterDto.builder()
                .taxPoid(entity.getTaxPoid())
                .taxCode(entity.getTaxCode())
                .taxName(entity.getTaxName())
                .taxName2(entity.getTaxName2())
                .percentage(entity.getPercentage())
                .taxType(entity.getTaxType())
                .glType(entity.getGlType())
                .glLedgerPoid(entity.getGlLedgerPoid())
                .taxCategory(entity.getTaxCategory())
                .seqNo(entity.getSeqNo())
                .build();
    }

    @Transactional
    public void softDeleteTaxMaster(Long taxPoid, DeleteReasonDto deleteReasonDto) {
        TaxMaster existingTaxRecord = repository.findByTaxPoid(taxPoid)
                .orElseThrow(() -> new ResourceNotFoundException("TaxMaster", "taxPoid", taxPoid));

        documentDeleteService.deleteDocument(
                taxPoid,
                "GLOBAL_TAX_MASTER",
                "TAX_POID",
                deleteReasonDto,
                ObjectUtils.isNotEmpty(existingTaxRecord.getCreatedDate()) ? existingTaxRecord.getCreatedDate().toLocalDate() : null
        );
    }

    @Override
    public Map<String, Object> listTaxMaster(String documentId, FilterRequestDto request, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(documentId, filters, operator, pageable, isDeleted,
                "TAX_CODE",   // label
                "TAX_POID");
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

}

