package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.dto.GlAgeingMasterDtlDto;
import com.asg.finance.dto.GlAgeingMasterDto;
import com.asg.finance.dto.GlAgeingMasterResponseDto;
import com.asg.finance.entity.GlAgeingMasterEntity;
import com.asg.finance.entity.GlAgeingMasterDtlEntity;
import com.asg.finance.entity.GlobalLogSummary;
import com.asg.finance.repository.GlAgeingMasterRepository;
import com.asg.finance.repository.GlAgeingMasterDtlRepository;
import com.asg.finance.repository.GlobalLogSummaryRepository;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.GlAgeingMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
public class GlAgeingMasterServiceImpl implements GlAgeingMasterService {

    private final GlAgeingMasterRepository ageingMasterRepository;
    private final GlAgeingMasterDtlRepository ageingMasterDtlRepository;
    private final LoggingService loggingService;
    private final GlobalLogSummaryRepository globalLogSummaryRepository;


    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String LOV_NAME = "GL_AGEING_TYPES";

    @Autowired
    private DocumentDeleteService documentDeleteService;
    @Autowired
    private DocumentSearchService documentService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public GlAgeingMasterResponseDto createAgeingMaster(GlAgeingMasterDto ageingMasterDto) {
        // Validate unique description
        if (ageingMasterRepository.existsByDescription(ageingMasterDto.getDescription())) {
            throw new RuntimeException("Ageing description already exists: " + ageingMasterDto.getDescription());
        }

        // Validate ageing details
        validateAgeingDetails(ageingMasterDto.getAgeingDetails());

        // Save master record
        GlAgeingMasterEntity masterEntity = saveAgeingMaster(ageingMasterDto);

        // Save detail records
        saveAgeingDetails(ageingMasterDto.getAgeingDetails(), masterEntity);

        String docId = UserContext.getDocumentId();
        String docKeyPoid = masterEntity.getAgeingPoid().toString();
        String createdMessage = String.format("Created - - DOC:%s KEY:%s", docId, docKeyPoid);
        GlobalLogSummary headerLog = createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, createdMessage);
        globalLogSummaryRepository.save(headerLog);

        return GlAgeingMasterResponseDto.builder()
                .status("success")
                .ageingPoid(masterEntity.getAgeingPoid())
                .build();
    }

    @Override
    public GlAgeingMasterDto fetchAgeingMaster(Long ageingPoid) {
        GlAgeingMasterEntity masterEntity = ageingMasterRepository.findByAgeingPoid(ageingPoid);
        if (masterEntity == null) {
            throw new ResourceNotFoundException("Ageing Master", "ageingPoid", ageingPoid);
        }

        GlAgeingMasterDto dto = new GlAgeingMasterDto();
        BeanUtils.copyProperties(masterEntity, dto);

        // Convert active flag from String to Boolean
        dto.setActive("Y".equals(masterEntity.getActive()));

        // Fetch and set ageing details via relationship-aware repository
        List<GlAgeingMasterDtlEntity> detailEntities = ageingMasterDtlRepository.findByAgeingMaster_AgeingPoid(ageingPoid);
        dto.setAgeingDetails(detailEntities.stream()
                .map(this::convertDetailEntityToDto)
                .collect(Collectors.toList()));

        return dto;
    }

    @Override
    @Transactional
    public GlAgeingMasterDto updateAgeingMaster(Long ageingPoid, GlAgeingMasterDto ageingMasterDto) {
        GlAgeingMasterEntity existingEntity = ageingMasterRepository.findByAgeingPoid(ageingPoid);
        if (existingEntity == null) {
            throw new ResourceNotFoundException("Ageing Master", "ageingPoid", ageingPoid);
        }

        // Create a copy of the old entity for logging
        GlAgeingMasterEntity oldEntity = new GlAgeingMasterEntity();
        BeanUtils.copyProperties(existingEntity, oldEntity);

        // Validate unique description (excluding current record)
        if (ageingMasterRepository.existsByDescriptionAndAgeingPoidNot(
                ageingMasterDto.getDescription(), ageingPoid)) {
            throw new RuntimeException("Ageing description already exists: " + ageingMasterDto.getDescription());
        }

        // Validate ageing details
        validateAgeingDetails(ageingMasterDto.getAgeingDetails());

        // Update master record
        updateAgeingMasterFields(existingEntity, ageingMasterDto);
        existingEntity.setLastModifiedBy(getCurrentUser());
        existingEntity.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
        ageingMasterRepository.save(existingEntity);

        // Update detail records (use entity relationship)
        //updateAgeingDetails(ageingMasterDto.getAgeingDetails(), existingEntity);
        updateAgeingMastersChildDetails(ageingMasterDto.getAgeingDetails(), ageingPoid);

        String docId = UserContext.getDocumentId();
        String docKeyPoid = ageingPoid.toString();
        String modifiedMessage = String.format("Modified - - DOC:%s KEY:%s", docId, docKeyPoid);
        GlobalLogSummary headerUpdateLog = createSummaryLogEntry(LogDetailsEnum.MODIFIED, docId, docKeyPoid, modifiedMessage);
        globalLogSummaryRepository.save(headerUpdateLog);
        
        List<LogRequestDto<GlAgeingMasterEntity>> headerLogRequests = new ArrayList<>();
        String logDetail = String.format("KeyId = AGEING_POID:%s", ageingPoid);
        headerLogRequests.add(new LogRequestDto<>(oldEntity, existingEntity, GlAgeingMasterEntity.class, docId, docKeyPoid, logDetail));
        if (!headerLogRequests.isEmpty()) {
            loggingService.createLogBatch(headerLogRequests);
        }

        return fetchAgeingMaster(ageingPoid);
    }

    private GlAgeingMasterEntity saveAgeingMaster(GlAgeingMasterDto dto) {
        GlAgeingMasterEntity entity = GlAgeingMasterEntity.builder()
                .groupPoid(UserContext.getGroupPoid())
                .description(dto.getDescription())
                .description2(dto.getDescription2())
                .ageingBreakupType(dto.getAgeingBreakupType())
                .seqno(dto.getSeqno())
                .active(dto.getActive() ? "Y" : "N")
                .deleted("N")
                .createdBy(getCurrentUser())
                .createdDate(Timestamp.valueOf(LocalDateTime.now()))
                .lastModifiedBy(getCurrentUser())
                .lastModifiedDate(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        return ageingMasterRepository.save(entity);
    }

    private void saveAgeingDetails(List<GlAgeingMasterDtlDto> detailDtos, GlAgeingMasterEntity masterEntity) {
        List<GlAgeingMasterDtlEntity> detailEntities = new ArrayList<>();

        for (GlAgeingMasterDtlDto dto : detailDtos) {

            GlAgeingMasterDtlEntity entity = GlAgeingMasterDtlEntity.builder()
                    .ageingMaster(masterEntity)
                    .ageingPoid(masterEntity.getAgeingPoid())
                    .detRowId(dto.getDetRowId())
                    .breakupTitle(dto.getBreakupTitle())
                    .breakupFrom(dto.getBreakupFrom())
                    .breakupTo(dto.getBreakupTo())
                    .createdBy(getCurrentUser())
                    .createdDate(Timestamp.valueOf(LocalDateTime.now()))
                    .lastModifiedBy(getCurrentUser())
                    .lastModifiedDate(Timestamp.valueOf(LocalDateTime.now()))
                    .build();

            detailEntities.add(entity);
        }

        ageingMasterDtlRepository.saveAll(detailEntities);
    }

    private void updateAgeingMasterFields(GlAgeingMasterEntity entity, GlAgeingMasterDto dto) {
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setDescription(dto.getDescription());
        entity.setDescription2(dto.getDescription2());
        entity.setAgeingBreakupType(dto.getAgeingBreakupType());
        entity.setSeqno(dto.getSeqno());
        entity.setActive(dto.getActive() ? "Y" : "N");
    }


    private GlAgeingMasterDtlDto convertDetailEntityToDto(GlAgeingMasterDtlEntity entity) {
        return GlAgeingMasterDtlDto.builder()
                .detRowId(entity.getDetRowId())
                .breakupTitle(entity.getBreakupTitle())
                .breakupFrom(entity.getBreakupFrom())
                .breakupTo(entity.getBreakupTo())
                .build();
    }

    /**
     * Validates the ageing details list to ensure that it is not empty and
     * that the breakupFrom value is not greater than the breakupTo value for each detail.
     * Additionally, this method will validate that there are no overlapping ageing ranges.
     *
     * @param ageingDetails the list of ageing details to be validated
     * @throws RuntimeException if the ageing details list is empty or if there are any overlapping ageing ranges
     */
    private void validateAgeingDetails(List<GlAgeingMasterDtlDto> ageingDetails) {
        if (ageingDetails == null || ageingDetails.isEmpty()) {
            throw new RuntimeException("Ageing details cannot be empty");
        }

        // Validate that breakupFrom <= breakupTo for each detail (skip if null)
        for (GlAgeingMasterDtlDto detail : ageingDetails) {
            if (detail.getBreakupFrom() != null && detail.getBreakupTo() != null) {
                if (detail.getBreakupFrom() > detail.getBreakupTo()) {
                    throw new RuntimeException("Breakup 'from' value cannot be greater than 'to' value for: " + detail.getBreakupTitle());
                }
            }
        }

        // Validate no overlapping ranges (optional business rule)
        validateNoOverlappingRanges(ageingDetails);
    }

    private void validateNoOverlappingRanges(List<GlAgeingMasterDtlDto> ageingDetails) {
        for (int i = 0; i < ageingDetails.size(); i++) {
            for (int j = i + 1; j < ageingDetails.size(); j++) {
                GlAgeingMasterDtlDto detail1 = ageingDetails.get(i);
                GlAgeingMasterDtlDto detail2 = ageingDetails.get(j);

                // Skip validation if any value is null
                if (detail1.getBreakupFrom() != null && detail1.getBreakupTo() != null &&
                    detail2.getBreakupFrom() != null && detail2.getBreakupTo() != null) {
                    if (rangesOverlap(detail1.getBreakupFrom(), detail1.getBreakupTo(),
                            detail2.getBreakupFrom(), detail2.getBreakupTo())) {
                        throw new RuntimeException("Overlapping ageing ranges found between '" +
                                detail1.getBreakupTitle() + "' and '" + detail2.getBreakupTitle() + "'");
                    }
                }
            }
        }
    }

    private boolean rangesOverlap(Integer from1, Integer to1, Integer from2, Integer to2) {
        return Math.max(from1, from2) <= Math.min(to1, to2);
    }


    @Override
    @Transactional
    public void softDeleteAgeingMaster(Long ageingPoid, DeleteReasonDto deleteReasonDto) {
        // Fetch the AgeingMaster entity to validate existence and get transaction date
        GlAgeingMasterEntity ageing = ageingMasterRepository.findById(ageingPoid)
                .orElseThrow(() -> new ResourceNotFoundException("AgeingMaster", "ageingPoid", ageingPoid));

        // Use DocumentDeleteService for consistent soft delete handling
        documentDeleteService.deleteDocument(
                ageingPoid,
                "GL_AGEING_MASTER",
                "AGEING_POID",
                deleteReasonDto,
                null
        );
    }
    @Override
    public Map<String, Object> listAgeingMasters(String documentId, FilterRequestDto request, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(
                documentId,
                filters,
                operator,
                pageable,
                isDeleted,
                "DESCRIPTION",   // original label col (used only for query)
                "AGEING_POID"    // value / ID column
        );

        // ✅ Prepare Page
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    public void updateAgeingMastersChildDetails(List<GlAgeingMasterDtlDto> ageingDetails, Long ageingPoid) {
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = ageingPoid.toString();

        List<GlAgeingMasterDtlEntity> toSave = new ArrayList<>();
        List<GlAgeingMasterDtlEntity> newlyCreatedEntities = new ArrayList<>(); 
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<GlAgeingMasterDtlEntity>> logRequests = new ArrayList<>();
        List<GlobalLogSummary> summaryLogs = new ArrayList<>();

        // Get existing records for deletion logging
        List<GlAgeingMasterDtlEntity> existingDetails = ageingMasterDtlRepository.findByAgeingMaster_AgeingPoid(ageingPoid);
        Map<Long, GlAgeingMasterDtlEntity> existingMap = existingDetails.stream()
                .collect(Collectors.toMap(GlAgeingMasterDtlEntity::getDetRowId, Function.identity(), (first, second) -> first));

        // Group operations by action
        for (GlAgeingMasterDtlDto charge : ageingDetails) {
            String actionType = charge.getActionType() == null ? "NOCHANGE" : charge.getActionType().toUpperCase();
            switch (actionType) {
                case "ISCREATED":
                
                    GlAgeingMasterDtlEntity newEntity = GlAgeingMasterDtlEntity.builder()
                            .ageingPoid(ageingPoid)
                            .detRowId(charge.getDetRowId())
                            .breakupTitle(charge.getBreakupTitle())
                            .breakupFrom(charge.getBreakupFrom())
                            .breakupTo(charge.getBreakupTo())
                            .createdBy(currentUser)
                            .createdDate(Timestamp.valueOf(now))
                            .lastModifiedBy(currentUser)
                            .lastModifiedDate(Timestamp.valueOf(now))
                            .build();
                    toSave.add(newEntity);
                    newlyCreatedEntities.add(newEntity);
                    break;

                case "ISUPDATED":
                    GlAgeingMasterDtlEntity existingCharge = ageingMasterDtlRepository
                            .findByAgeingPoidAndDetRowId(ageingPoid, charge.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("Ageing not found", "detRowId", charge.getDetRowId()));
                    
                    GlAgeingMasterDtlEntity oldCharge = new GlAgeingMasterDtlEntity();
                    BeanUtils.copyProperties(existingCharge, oldCharge);
                    oldCharge.setDetRowId(existingCharge.getDetRowId());
                    oldCharge.setAgeingPoid(existingCharge.getAgeingPoid());
                    oldCharge.setAgeingMaster(existingCharge.getAgeingMaster());
                    
                    existingCharge.setBreakupTitle(charge.getBreakupTitle());
                    existingCharge.setBreakupFrom(charge.getBreakupFrom());
                    existingCharge.setBreakupTo(charge.getBreakupTo());
                    existingCharge.setLastModifiedBy(currentUser);
                    existingCharge.setLastModifiedDate(Timestamp.valueOf(now));
                    toSave.add(existingCharge);
                    
                    String logDetail = String.format("KeyId = AGEING_POID:%s DET_ROW_ID:%s", oldCharge.getAgeingPoid(), oldCharge.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldCharge, existingCharge, GlAgeingMasterDtlEntity.class, docId, docKeyPoid, logDetail));
                    
                    String updateSummaryMessage = String.format("Modified - - DOC:%s KEY:%s", docId, docKeyPoid);
                    GlobalLogSummary updateSummaryLog = createSummaryLogEntry(LogDetailsEnum.MODIFIED, docId, docKeyPoid, updateSummaryMessage);
                    summaryLogs.add(updateSummaryLog);
                    break;

                case "ISDELETED":
                    toDelete.add(charge.getDetRowId());
                    GlAgeingMasterDtlEntity oldEntityForDelete = existingMap.get(charge.getDetRowId());
                    if (oldEntityForDelete != null) {
                        String deletedRecordString = String.format("detRowId:%s, ageingPoid:%s, breakupTitle:%s, breakupFrom:%s, breakupTo:%s",
                                oldEntityForDelete.getDetRowId(), oldEntityForDelete.getAgeingPoid(), oldEntityForDelete.getBreakupTitle(),
                                oldEntityForDelete.getBreakupFrom(), oldEntityForDelete.getBreakupTo());
                        String deleteSummaryMessage = String.format("Row Deleted %s", deletedRecordString);
                        GlobalLogSummary deleteSummaryLog = createSummaryLogEntry(LogDetailsEnum.DELETED, docId, docKeyPoid, deleteSummaryMessage);
                        summaryLogs.add(deleteSummaryLog);
                    }
                    break;
                    
                case "NOCHANGE":
                    break;
            }
        }
        // Batch operations
        if (!toSave.isEmpty()) {
            List<GlAgeingMasterDtlEntity> savedEntities = ageingMasterDtlRepository.saveAll(toSave);
            
            for (GlAgeingMasterDtlEntity newlyCreated : newlyCreatedEntities) {
                GlAgeingMasterDtlEntity savedEntity = savedEntities.stream()
                        .filter(saved ->
                                Objects.equals(saved.getAgeingPoid(), newlyCreated.getAgeingPoid()) &&
                                        Objects.equals(saved.getBreakupTitle(), newlyCreated.getBreakupTitle()) &&
                                        Objects.equals(saved.getBreakupFrom(), newlyCreated.getBreakupFrom()) &&
                                        Objects.equals(saved.getBreakupTo(), newlyCreated.getBreakupTo())
                        )

                        .findFirst()
                        .orElse(null);
                
                if (savedEntity != null && savedEntity.getDetRowId() != null) {
                    String summaryMessage = String.format("Row Created on Ageing Master Detail with DetRowId: %s", savedEntity.getDetRowId());
                    GlobalLogSummary summaryLog = createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, summaryMessage);
                    summaryLogs.add(summaryLog);
                }
            }
        }
        if (!toDelete.isEmpty()) {
            ageingMasterDtlRepository.deleteByAgeingPoidAndDetRowIdIn(ageingPoid, toDelete);
        }
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
        if (!summaryLogs.isEmpty()) {
            globalLogSummaryRepository.saveAll(summaryLogs);
        }
    }
    
   
    private GlobalLogSummary createSummaryLogEntry(LogDetailsEnum logDetailsEnum, String docId, String docKeyPoid, String customMessage) {
        GlobalLogSummary summary = new GlobalLogSummary();
        summary.setLogUserPoid(UserContext.getUserPoid());
        summary.setLogDateTime(new Timestamp(System.currentTimeMillis()));
        summary.setLogDocId(docId);
        summary.setLogDocKeyPoid(docKeyPoid);
        summary.setLogDetails(customMessage);
        return summary;
    }

}
