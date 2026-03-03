package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.finance.dto.SupplierCategoryDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.entity.SupplierCategoryEntity;
import com.asg.finance.repository.SupplierCategoryRepository;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.SupplierCategoryService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierCategoryServiceImpl implements SupplierCategoryService {

    private final SupplierCategoryRepository supplierCategoriesRepository;
    private final DocumentSearchService documentService;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;

    @PersistenceContext
    private EntityManager entityManager;


    @Transactional
    @Override
    public void softDeleteSupplierCategory(Long supplierCategoryPoid, DeleteReasonDto deleteReasonDto) {
        supplierCategoriesRepository.findById(supplierCategoryPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier Category", "supplierCategoryPoid", supplierCategoryPoid));

        documentDeleteService.deleteDocument(
                supplierCategoryPoid,
                "AP_SUPPLIER_CATEGORY_MASTER",
                "SUPPLIER_CATEGORY_POID",
                deleteReasonDto,
                null
        );
    }

    private SupplierCategoryDto mapToDto(SupplierCategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        log.info("entity: {}", entity);
        SupplierCategoryDto dto = new SupplierCategoryDto();
        dto.setSupplierCategoryPoid(entity.getSupplierCategoryPoid());
        dto.setGroupPoid(entity.getGroupPoid());
        dto.setSupplierCategoryCode(entity.getSupplierCategoryCode());
        dto.setSupplierCategoryName(entity.getSupplierCategoryName());
        dto.setSupplierCategoryName2(entity.getSupplierCategoryName2());
        dto.setActive(entity.getActive());
        dto.setSeqNo(String.valueOf(entity.getSequenceNumber()));
        dto.setGeneralRemarks(entity.getGeneralRemarks());
        dto.setDeleted(entity.getDeleted());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());

        log.info("dto: {}", dto);
        return dto;
    }

    @Override
    public SupplierCategoryDto getSupplierCategoryById(Long supplierCategoryPoid) {

        SupplierCategoryEntity entity = supplierCategoriesRepository.findById(supplierCategoryPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier Category", "supplierCategoryPoid", supplierCategoryPoid));

        return mapToDto(entity);
    }

    @Transactional
    @Override
    public SupplierCategoryDto updateSupplierCategory(Long supplierCategoryPoid, SupplierCategoryDto supplierCategoryDto) {
        SupplierCategoryEntity entity = supplierCategoriesRepository.findById(supplierCategoryPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier Category", "supplierCategoryPoid", supplierCategoryPoid));

        // Create a copy of the existing entity for logging
        SupplierCategoryEntity oldEntity = new SupplierCategoryEntity();
        BeanUtils.copyProperties(entity, oldEntity);

        if (StringUtils.isNotBlank(supplierCategoryDto.getSupplierCategoryName()) && !supplierCategoryDto.getSupplierCategoryName().equals(entity.getSupplierCategoryName())) {
            if (supplierCategoriesRepository.existsBySupplierCategoryNameAndSupplierCategoryPoidNot(supplierCategoryDto.getSupplierCategoryName(), supplierCategoryPoid)) {
                throw new ResourceAlreadyExistsException("Supplier Category Name", supplierCategoryDto.getSupplierCategoryName());
            }
        }

        if (StringUtils.isNotBlank(supplierCategoryDto.getSupplierCategoryName())) {
            entity.setSupplierCategoryName(supplierCategoryDto.getSupplierCategoryName());
        }

        entity.setSupplierCategoryName2(supplierCategoryDto.getSupplierCategoryName2());

        if (StringUtils.isNotBlank(supplierCategoryDto.getSeqNo())) {

            String seq = supplierCategoryDto.getSeqNo().trim();

            if (!seq.matches("\\d{1,5}")) {
                throw new IllegalArgumentException("Sequence Number must be up to 5 digits only.");
            }

            entity.setSequenceNumber(Integer.valueOf(seq));
        } else {
            entity.setSequenceNumber(null);
        }

        entity.setActive(StringUtils.isNotBlank(supplierCategoryDto.getActive()) ? supplierCategoryDto.getActive() : "Y");


        SupplierCategoryEntity updatedEntity = supplierCategoriesRepository.save(entity);
        
        // Log the update
        String key = updatedEntity.getSupplierCategoryPoid().toString();
        String docId = UserContext.getDocumentId();
        loggingService.logChanges(oldEntity, updatedEntity, SupplierCategoryEntity.class, 
                docId, key, LogDetailsEnum.MODIFIED, "SUPPLIER_CATEGORY_POID");

        return mapToDto(updatedEntity);
    }

    @Transactional
    @Override
    public SupplierCategoryDto createSupplierCategory(SupplierCategoryDto supplierCategoryDto) {
log.info("SupplierCategoryDto: {}", supplierCategoryDto);
        if (supplierCategoriesRepository.existsBySupplierCategoryCode(supplierCategoryDto.getSupplierCategoryCode())) {
            throw new ResourceAlreadyExistsException("Supplier Category Code", supplierCategoryDto.getSupplierCategoryCode());
        }

        if (supplierCategoriesRepository.existsBySupplierCategoryName(supplierCategoryDto.getSupplierCategoryName())) {
            throw new ResourceAlreadyExistsException("Supplier Category Name", supplierCategoryDto.getSupplierCategoryName());
        }

        SupplierCategoryEntity entity = new SupplierCategoryEntity();
//        entity.setSupplierCategoryCode(supplierCategoryDto.getSupplierCategoryCode());
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setSupplierCategoryName(supplierCategoryDto.getSupplierCategoryName());
        entity.setSupplierCategoryName2(supplierCategoryDto.getSupplierCategoryName2());
        entity.setSequenceNumber(StringUtils.isNotBlank(supplierCategoryDto.getSeqNo())
                ? Integer.valueOf(supplierCategoryDto.getSeqNo()) : null);
        entity.setActive(StringUtils.isNotBlank(supplierCategoryDto.getActive()) ? supplierCategoryDto.getActive() : "Y");
        entity.setDeleted("N");

        SupplierCategoryEntity savedEntity = supplierCategoriesRepository.save(entity);

        entityManager.flush();
        entityManager.refresh(savedEntity);
        
        // Log the creation
        String key = savedEntity.getSupplierCategoryPoid().toString();
        String docId = UserContext.getDocumentId();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);
        
        log.info("savedEntity: {}", savedEntity);
        return mapToDto(savedEntity);
    }

    public Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, Pageable pageable) {

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "SUPPLIER_CATEGORY_NAME",   // label
                "SUPPLIER_CATEGORY_POID");    // value);

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }
}




