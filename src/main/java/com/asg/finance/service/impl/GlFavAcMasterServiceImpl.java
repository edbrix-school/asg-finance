package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.entity.Company;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.client.RoleServiceClient;
import com.asg.finance.entity.GLMaster;
import com.asg.finance.repository.GLMasterRepository;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.*;
import com.asg.finance.entity.GlFavAcMaster;
import com.asg.finance.entity.GlFavAcMasterGlAcDtl;
import com.asg.finance.entity.GlFavAcMasterUserRoleDtl;
import com.asg.finance.entity.GlobalLogSummary;
import com.asg.finance.repository.GlFavAcMasterGlAcDtlRepository;
import com.asg.finance.repository.GlFavAcMasterRepository;
import com.asg.finance.repository.GlFavAcMasterUserRoleDtlRepository;
import com.asg.finance.repository.GlobalLogSummaryRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.GlFavAcMasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service implementation for Key Favorite Account Master operations
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GlFavAcMasterServiceImpl implements GlFavAcMasterService {

    private final GlFavAcMasterRepository masterRepository;
    private final GlFavAcMasterGlAcDtlRepository glAcDtlRepository;
    private final GlFavAcMasterUserRoleDtlRepository userRoleDtlRepository;
    private final RoleServiceClient roleServiceClient;
    private final DocumentSearchService documentService;
    private final GLMasterRepository glMasterRepository;
    private final LovDataService lovService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    
    @PersistenceContext
    private EntityManager entityManager;
    private final GlobalLogSummaryRepository globalLogSummaryRepository;

    @Override
    public GlFavAcMasterResponse createFavoriteAccount(GlFavAcMasterRequest request) {
        // Validate uniqueness of Fav Ac Code
        if (masterRepository.findByFavAcCodeAndGroupPoid(request.getFavAcCode(), UserContext.getGroupPoid()).isPresent()) {
            throw new ValidationException("Fav Ac Code already exists: " + request.getFavAcCode());
        }

        // Validate uniqueness of Description
        if (masterRepository.findByDescriptionAndGroupPoid(request.getDescription(), UserContext.getGroupPoid()).isPresent()) {
            throw new ValidationException("Description already exists: " + request.getDescription());
        }

        // Validate at least one GL account is mapped
        if (request.getGlAccounts() == null || request.getGlAccounts().isEmpty()) {
            throw new ValidationException("At least one GL account must be mapped");
        }

        // Validate Company and View Category are mandatory for all GL accounts
        for (GlAccountDetailRequest glAccount : request.getGlAccounts()) {
            if (glAccount.getCompanyPoId() == null) {
                throw new ValidationException("Company is mandatory for all GL accounts");
            }
            if (glAccount.getViewCategoryPoid() == null || glAccount.getViewCategoryPoid().isBlank()) {
                throw new ValidationException("View Category is mandatory for all GL accounts");
            }
        }

        // Validate GL Account should not be duplicated
        validateNoDuplicateGlAccounts(request.getGlAccounts());

        if (request.getUserRoles() != null && !request.getUserRoles().isEmpty()) {
            List<Long> userRolePoids = request.getUserRoles().stream()
                    .map(UserRoleDetailRequest::getUserRolePoid)
                    .filter(rolePoid -> rolePoid != null && rolePoid != 0)
                    .collect(Collectors.toList());
            validateUserRolesExist(userRolePoids);
        }

        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        // Create master record (BaseEntity handles audit fields)
        GlFavAcMaster master = GlFavAcMaster.builder()
                .groupPoid(UserContext.getGroupPoid())
                .favAcCode(request.getFavAcCode())
                .description(request.getDescription())
                .description2(request.getDescription2())
                .active(request.getActive() != null ? request.getActive() : "Y")
                .seqNo(request.getSeqNo())
                .deleted("N")
                .build();

        GlFavAcMaster savedMaster = masterRepository.save(master);
        
        List<GlobalLogSummary> detailSummaryLogs = new ArrayList<>();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = savedMaster.getFavAcPoid().toString();

        if (request.getGlAccounts() != null && !request.getGlAccounts().isEmpty()) {
            for (GlAccountDetailRequest glAccountRequest : request.getGlAccounts()) {
                String rawAction = glAccountRequest.getActionType();
                String action = (rawAction == null || rawAction.trim().isEmpty())
                        ? "ISCREATED"  
                        : rawAction.trim().toUpperCase();
                
                action = switch (action) {
                    case "ISCREATED", "CREATED", "NEW" -> "ISCREATED";
                    case "ISUPDATED", "UPDATED" -> "ISUPDATED";
                    case "ISDELETED", "DELETED" -> "ISDELETED";
                    default -> "NOCHANGES";
                };
                
                switch (action) {
                    case "NOCHANGES" -> {
                        continue;
                    }
                    case "ISDELETED" -> {
                        continue;
                    }
                    case "ISCREATED" -> {
                        if (glAccountRequest.getDetRowId() != null) {
                            Optional<GlFavAcMasterGlAcDtl> existingEntityOpt = glAcDtlRepository
                                    .findByFavAcPoidAndDetRowId(savedMaster.getFavAcPoid(), glAccountRequest.getDetRowId());
                            
                            if (existingEntityOpt.isPresent()) {
                                GlFavAcMasterGlAcDtl existingEntity = existingEntityOpt.get();
                                existingEntity.setGlPoid(glAccountRequest.getGlAccountPoId());
                                existingEntity.setCompany(glAccountRequest.getCompanyPoId());
                                existingEntity.setViewCategory(glAccountRequest.getViewCategoryPoid());
                                existingEntity.setRemarks(glAccountRequest.getRemarks());
                                existingEntity.setSeqNo(glAccountRequest.getSeqNo());
                                existingEntity.setLastModifiedBy(currentUser);
                                existingEntity.setLastModifiedDate(now);
                                glAcDtlRepository.save(existingEntity);

                                String createSummaryMessage = String.format(
                                        "Row Created on Favorite Account Master GL Account Detail with DetRowId: %s",
                                        existingEntity.getDetRowId());
                                detailSummaryLogs.add(createSummaryLogEntry(
                                        LogDetailsEnum.CREATED, docId, docKeyPoid, createSummaryMessage, now));
                            } else {
                                GlFavAcMasterGlAcDtl glAcDtl = GlFavAcMasterGlAcDtl.builder()
                                        .favAcPoid(savedMaster.getFavAcPoid())
                                        .detRowId(glAccountRequest.getDetRowId())
                                        .glPoid(glAccountRequest.getGlAccountPoId())
                                        .company(glAccountRequest.getCompanyPoId())
                                        .viewCategory(glAccountRequest.getViewCategoryPoid())
                                        .remarks(glAccountRequest.getRemarks())
                                        .seqNo(glAccountRequest.getSeqNo())
                                        .createdBy(currentUser)
                                        .createdDate(now)
                                        .lastModifiedBy(currentUser)
                                        .lastModifiedDate(now)
                                        .build();
                                glAcDtlRepository.save(glAcDtl);

                                String createSummaryMessage = String.format(
                                        "Row Created on Favorite Account Master GL Account Detail  with DetRowId: %s",
                                        glAcDtl.getDetRowId());
                                detailSummaryLogs.add(createSummaryLogEntry(
                                        LogDetailsEnum.CREATED, docId, docKeyPoid, createSummaryMessage, now));
                            }
                        } else {
                            throw new ValidationException("DetRowId is required for create operation");
                        }
                    }
                    case "ISUPDATED" -> {
                        if (glAccountRequest.getDetRowId() == null) {
                            throw new ValidationException("DetRowId is required for update operation");
                        }
                        GlFavAcMasterGlAcDtl existingGlAcDtl = glAcDtlRepository
                                .findByFavAcPoidAndDetRowId(savedMaster.getFavAcPoid(), glAccountRequest.getDetRowId())
                                .orElse(null);
                        
                        if (existingGlAcDtl != null) {
                            existingGlAcDtl.setGlPoid(glAccountRequest.getGlAccountPoId());
                            existingGlAcDtl.setCompany(glAccountRequest.getCompanyPoId());
                            existingGlAcDtl.setViewCategory(glAccountRequest.getViewCategoryPoid());
                            existingGlAcDtl.setRemarks(glAccountRequest.getRemarks());
                            existingGlAcDtl.setSeqNo(glAccountRequest.getSeqNo());
                            existingGlAcDtl.setLastModifiedBy(currentUser);
                            existingGlAcDtl.setLastModifiedDate(now);
                            glAcDtlRepository.save(existingGlAcDtl);

                            String createSummaryMessage = String.format(
                                    "Row Created on Favorite Account Master GL Account Detail with DetRowId: %s",
                                    existingGlAcDtl.getDetRowId());
                            detailSummaryLogs.add(createSummaryLogEntry(
                                    LogDetailsEnum.CREATED, docId, docKeyPoid, createSummaryMessage, now));
                        } else {
                            GlFavAcMasterGlAcDtl glAcDtl = GlFavAcMasterGlAcDtl.builder()
                                    .favAcPoid(savedMaster.getFavAcPoid())
                                    .detRowId(glAccountRequest.getDetRowId())
                                    .glPoid(glAccountRequest.getGlAccountPoId())
                                    .company(glAccountRequest.getCompanyPoId())
                                    .viewCategory(glAccountRequest.getViewCategoryPoid())
                                    .remarks(glAccountRequest.getRemarks())
                                    .seqNo(glAccountRequest.getSeqNo())
                                    .createdBy(currentUser)
                                    .createdDate(now)
                                    .lastModifiedBy(currentUser)
                                    .lastModifiedDate(now)
                                    .build();
                            glAcDtlRepository.save(glAcDtl);

                            String createSummaryMessage = String.format(
                                    "Row Created on Favorite Account Master GL Account Detail with DetRowId: %s",
                                    glAcDtl.getDetRowId());
                            detailSummaryLogs.add(createSummaryLogEntry(
                                    LogDetailsEnum.CREATED, docId, docKeyPoid, createSummaryMessage, now));
                        }
                    }
                }
                            }
        }

        // Create user role detail records
        if (request.getUserRoles() != null && !request.getUserRoles().isEmpty()) {
            for (UserRoleDetailRequest userRoleRequest : request.getUserRoles()) {
                String rawAction = userRoleRequest.getActionType();
                String action = (rawAction == null || rawAction.trim().isEmpty())
                        ? "ISCREATED"  
                        : rawAction.trim().toUpperCase();
                
                action = switch (action) {
                    case "ISCREATED", "CREATED", "NEW" -> "ISCREATED";
                    case "ISUPDATED", "UPDATED" -> "ISUPDATED";
                    case "ISDELETED", "DELETED" -> "ISDELETED";
                    default -> "NOCHANGES";
                };
                
                switch (action) {
                    case "NOCHANGES" -> {
                        continue;
                    }
                    case "ISDELETED" -> {
                        continue;
                    }
                    case "ISCREATED" -> {
                        if (userRoleRequest.getDetRowId() != null) {
                            Optional<GlFavAcMasterUserRoleDtl> existingEntityOpt = userRoleDtlRepository
                                    .findByFavAcPoidAndDetRowId(savedMaster.getFavAcPoid(), userRoleRequest.getDetRowId());
                            
                            if (existingEntityOpt.isPresent()) {
                                GlFavAcMasterUserRoleDtl existingEntity = existingEntityOpt.get();
                                existingEntity.setUserRolePoid(userRoleRequest.getUserRolePoid());
                                existingEntity.setLastModifiedBy(currentUser);
                                existingEntity.setLastModifiedDate(now);
                                userRoleDtlRepository.save(existingEntity);

                                String createSummaryMessage = String.format(
                                        "Row Created on Favorite Account Master User Role Detail with DetRowId: %s",
                                        existingEntity.getDetRowId());
                                detailSummaryLogs.add(createSummaryLogEntry(
                                        LogDetailsEnum.CREATED, docId, docKeyPoid, createSummaryMessage, now));
                            } else {
                                GlFavAcMasterUserRoleDtl userRoleDtl = GlFavAcMasterUserRoleDtl.builder()
                                        .favAcPoid(savedMaster.getFavAcPoid())
                                        .detRowId(userRoleRequest.getDetRowId()) // Frontend provides detRowId
                                        .userRolePoid(userRoleRequest.getUserRolePoid())
                                        .createdBy(currentUser)
                                        .createdDate(now)
                                        .lastModifiedBy(currentUser)
                                        .lastModifiedDate(now)
                                        .build();
                                userRoleDtlRepository.save(userRoleDtl);

                                String createSummaryMessage = String.format(
                                        "Row Created on Favorite Account Master User Role Detail with DetRowId: %s",
                                        userRoleDtl.getDetRowId());
                                detailSummaryLogs.add(createSummaryLogEntry(
                                        LogDetailsEnum.CREATED, docId, docKeyPoid, createSummaryMessage, now));
                            }
                        } else {
                            throw new ValidationException("DetRowId is required for create operation");
                        }
                    }
                    case "ISUPDATED" -> {
                        if (userRoleRequest.getDetRowId() == null) {
                            throw new ValidationException("DetRowId is required for update operation");
                        }
                        GlFavAcMasterUserRoleDtl existingUserRoleDtl = userRoleDtlRepository
                                .findByFavAcPoidAndDetRowId(savedMaster.getFavAcPoid(), userRoleRequest.getDetRowId())
                                .orElse(null);
                        
                        if (existingUserRoleDtl != null) {
                            existingUserRoleDtl.setUserRolePoid(userRoleRequest.getUserRolePoid());
                            existingUserRoleDtl.setLastModifiedBy(currentUser);
                            existingUserRoleDtl.setLastModifiedDate(now);
                            userRoleDtlRepository.save(existingUserRoleDtl);

                            String createSummaryMessage = String.format(
                                    "Row Created on Favorite Account Master User Role Detail with DetRowId: %s",
                                    existingUserRoleDtl.getDetRowId());
                            detailSummaryLogs.add(createSummaryLogEntry(
                                    LogDetailsEnum.CREATED, docId, docKeyPoid, createSummaryMessage, now));
                        } else {
                            GlFavAcMasterUserRoleDtl userRoleDtl = GlFavAcMasterUserRoleDtl.builder()
                                    .favAcPoid(savedMaster.getFavAcPoid())
                                    .detRowId(userRoleRequest.getDetRowId())
                                    .userRolePoid(userRoleRequest.getUserRolePoid())
                                    .createdBy(currentUser)
                                    .createdDate(now)
                                    .lastModifiedBy(currentUser)
                                    .lastModifiedDate(now)
                                    .build();
                            userRoleDtlRepository.save(userRoleDtl);

                            String createSummaryMessage = String.format(
                                    "Row Created on Favorite Account Master User Role Detail with DetRowId: %s",
                                    userRoleDtl.getDetRowId());
                            detailSummaryLogs.add(createSummaryLogEntry(
                                    LogDetailsEnum.CREATED, docId, docKeyPoid, createSummaryMessage, now));
                        }
                    }
                }
                
            }
        }

        if (!detailSummaryLogs.isEmpty()) {
            globalLogSummaryRepository.saveAll(detailSummaryLogs);
        }

        String createdMessage = String.format("Created - - DOC:%s KEY:%s", docId, docKeyPoid);
        GlobalLogSummary headerLog = createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, createdMessage, now);
        globalLogSummaryRepository.save(headerLog);

        return getFavoriteAccountById(savedMaster.getFavAcPoid());
    }

    @Override
    public GlFavAcMasterResponse updateFavoriteAccount(Long favAcPoid, GlFavAcMasterRequest request) {
        GlFavAcMaster existing = masterRepository.findByFavAcPoid(favAcPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite Account Master", "favAcPoid", favAcPoid));

        // Validate uniqueness of Fav Ac Code (excluding current record)
        masterRepository.findByFavAcCodeAndGroupPoid(request.getFavAcCode(), UserContext.getGroupPoid())
                .ifPresent(master -> {
                    if (!master.getFavAcPoid().equals(favAcPoid)) {
                        throw new ValidationException("Fav Ac Code already exists: " + request.getFavAcCode());
                    }
                });

        // Validate uniqueness of Description (excluding current record)
        masterRepository.findByDescriptionAndGroupPoid(request.getDescription(), UserContext.getGroupPoid())
                .ifPresent(master -> {
                    if (!master.getFavAcPoid().equals(favAcPoid)) {
                        throw new ValidationException("Description already exists: " + request.getDescription());
                    }
                });

        // Validate at least one GL account is mapped
        if (request.getGlAccounts() == null || request.getGlAccounts().isEmpty()) {
            throw new ValidationException("At least one GL account must be mapped");
        }

        // Validate Company and View Category are mandatory for all GL accounts
        for (GlAccountDetailRequest glAccount : request.getGlAccounts()) {
            if (glAccount.getCompanyPoId() == null) {
                throw new ValidationException("Company is mandatory for all GL accounts");
            }
            if (glAccount.getViewCategoryPoid() == null || glAccount.getViewCategoryPoid().isBlank()) {
                throw new ValidationException("View Category is mandatory for all GL accounts");
            }
        }

        // Validate GL Account should not be duplicated
        validateNoDuplicateGlAccounts(request.getGlAccounts());

        if (request.getUserRoles() != null && !request.getUserRoles().isEmpty()) {
            List<Long> userRolePoids = request.getUserRoles().stream()
                    .map(UserRoleDetailRequest::getUserRolePoid)
                    .filter(rolePoid -> rolePoid != null && rolePoid != 0)
                    .collect(Collectors.toList());
            validateUserRolesExist(userRolePoids);
        }

        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        // Create copy of old entity for logging
        GlFavAcMaster oldEntity = new GlFavAcMaster();
        BeanUtils.copyProperties(existing, oldEntity);

        // Update master record (BaseEntity handles audit fields)
        existing.setFavAcCode(request.getFavAcCode());
        existing.setDescription(request.getDescription());
        existing.setDescription2(request.getDescription2());
        existing.setActive(request.getActive());
        existing.setSeqNo(request.getSeqNo());

        masterRepository.save(existing);

        List<GlFavAcMasterGlAcDtl> oldGlAcDtls = glAcDtlRepository.findByFavAcPoidOrderByDetRowId(favAcPoid);
        Map<Long, GlFavAcMasterGlAcDtl> oldGlAcMap = oldGlAcDtls.stream()
                .collect(Collectors.toMap(GlFavAcMasterGlAcDtl::getDetRowId, Function.identity(), (first, second) -> first));
        
        List<GlFavAcMasterGlAcDtl> toSave = new ArrayList<>();
        List<GlFavAcMasterGlAcDtl> toDelete = new ArrayList<>();
        List<LogRequestDto<GlFavAcMasterGlAcDtl>> glAcLogRequests = new ArrayList<>();
        List<GlobalLogSummary> glAcSummaryLogs = new ArrayList<>();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = favAcPoid.toString();
        
        if (request.getGlAccounts() != null && !request.getGlAccounts().isEmpty()) {
            for (GlAccountDetailRequest glAccountRequest : request.getGlAccounts()) {
                String actionTypeStr = glAccountRequest.getActionType();
                if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                    if (glAccountRequest.getDetRowId() == null || glAccountRequest.getDetRowId() == 0) {
                        actionTypeStr = "isCreated";
                    } else {
                        actionTypeStr = "isUpdated";
                    }
                }
                String actionType = actionTypeStr.toUpperCase();
                
                switch (actionType) {
                    case "ISCREATED":
                        GlFavAcMasterGlAcDtl newGlAcDtl = GlFavAcMasterGlAcDtl.builder()
                                .favAcPoid(favAcPoid)
                                .detRowId(glAccountRequest.getDetRowId()) 
                                .glPoid(glAccountRequest.getGlAccountPoId())
                                .company(glAccountRequest.getCompanyPoId())
                                .viewCategory(glAccountRequest.getViewCategoryPoid())
                                .remarks(glAccountRequest.getRemarks())
                                .seqNo(glAccountRequest.getSeqNo())
                                .createdBy(currentUser)
                                .createdDate(now)
                                .lastModifiedBy(currentUser)
                                .lastModifiedDate(now)
                                .build();
                        toSave.add(newGlAcDtl);
                        
                        String createSummaryMessage = String.format("Row Created on Favorite Account Master Detail with DetRowId: %s", glAccountRequest.getDetRowId());
                        GlobalLogSummary createSummaryLog = createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, createSummaryMessage);
                        glAcSummaryLogs.add(createSummaryLog);
                        break;
                        
                    case "ISUPDATED":
                        if (glAccountRequest.getDetRowId() == null) {
                            throw new ValidationException("DetRowId is required for update operation");
                        }
                        GlFavAcMasterGlAcDtl existingGlAcDtl = glAcDtlRepository
                                .findByFavAcPoidAndDetRowId(favAcPoid, glAccountRequest.getDetRowId())
                                .orElse(null);
                        
                        if (existingGlAcDtl == null) {
                            GlFavAcMasterGlAcDtl newGlAcDtlFromUpdate = GlFavAcMasterGlAcDtl.builder()
                                    .favAcPoid(favAcPoid)
                                    .detRowId(glAccountRequest.getDetRowId()) 
                                    .glPoid(glAccountRequest.getGlAccountPoId())
                                    .company(glAccountRequest.getCompanyPoId())
                                    .viewCategory(glAccountRequest.getViewCategoryPoid())
                                    .remarks(glAccountRequest.getRemarks())
                                    .seqNo(glAccountRequest.getSeqNo())
                                    .createdBy(currentUser)
                                    .createdDate(now)
                                    .lastModifiedBy(currentUser)
                                    .lastModifiedDate(now)
                                    .build();
                            toSave.add(newGlAcDtlFromUpdate);
                            
                            String createSummaryMessageFromUpdate = String.format("Row Created on Favorite Account Master Detail with DetRowId: %s", glAccountRequest.getDetRowId());
                            GlobalLogSummary createSummaryLogFromUpdate = createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, createSummaryMessageFromUpdate);
                            glAcSummaryLogs.add(createSummaryLogFromUpdate);
                            break;
                        }
                        
                        GlFavAcMasterGlAcDtl oldGlAcDtl = new GlFavAcMasterGlAcDtl();
                        BeanUtils.copyProperties(existingGlAcDtl, oldGlAcDtl);
                        oldGlAcDtl.setDetRowId(existingGlAcDtl.getDetRowId());
                        oldGlAcDtl.setFavAcPoid(existingGlAcDtl.getFavAcPoid());
                        
                        existingGlAcDtl.setGlPoid(glAccountRequest.getGlAccountPoId());
                        existingGlAcDtl.setCompany(glAccountRequest.getCompanyPoId());
                        existingGlAcDtl.setViewCategory(glAccountRequest.getViewCategoryPoid());
                        existingGlAcDtl.setRemarks(glAccountRequest.getRemarks());
                        existingGlAcDtl.setSeqNo(glAccountRequest.getSeqNo());
                        existingGlAcDtl.setLastModifiedBy(currentUser);
                        existingGlAcDtl.setLastModifiedDate(now);
                        toSave.add(existingGlAcDtl);
                        
                        String logDetail = String.format("KeyId = FAV_AC_POID:%s DET_ROW_ID:%s", 
                            oldGlAcDtl.getFavAcPoid(), oldGlAcDtl.getDetRowId());
                        glAcLogRequests.add(new LogRequestDto<>(oldGlAcDtl, existingGlAcDtl, GlFavAcMasterGlAcDtl.class, 
                            docId, docKeyPoid, logDetail));
                        break;
                        
                    case "ISDELETED":
                        if (glAccountRequest.getDetRowId() == null) {
                            throw new ValidationException("DetRowId is required for delete operation");
                        }
                        GlFavAcMasterGlAcDtl glAcDtlToDelete = glAcDtlRepository
                                .findByFavAcPoidAndDetRowId(favAcPoid, glAccountRequest.getDetRowId())
                                .orElse(null);
                        if (glAcDtlToDelete != null) {
                            toDelete.add(glAcDtlToDelete);
                            
                            String deletedRecordString = String.format("detRowId:%s, favAcPoid:%s, glPoid:%s, company:%s, viewCategory:%s, remarks:%s, seqNo:%s",
                                    glAcDtlToDelete.getDetRowId(), glAcDtlToDelete.getFavAcPoid(), glAcDtlToDelete.getGlPoid(),
                                    glAcDtlToDelete.getCompany(), glAcDtlToDelete.getViewCategory(), glAcDtlToDelete.getRemarks(), glAcDtlToDelete.getSeqNo());
                            String deleteSummaryMessage = String.format("Row Deleted %s", deletedRecordString);
                            GlobalLogSummary deleteSummaryLog = createSummaryLogEntry(LogDetailsEnum.DELETED, docId, docKeyPoid, deleteSummaryMessage);
                            glAcSummaryLogs.add(deleteSummaryLog);
                        }
                        break;
                        
                    case "NOCHANGES", "NOCHANGE":
                        if (glAccountRequest.getDetRowId() != null) {
                            GlFavAcMasterGlAcDtl unchangedGlAcDtl = oldGlAcMap.get(glAccountRequest.getDetRowId());
                            if (unchangedGlAcDtl != null) {
                                toSave.add(unchangedGlAcDtl);
                            }
                        }
                        break;
                        
                    default:
                        if (glAccountRequest.getDetRowId() != null) {
                            GlFavAcMasterGlAcDtl defaultGlAcDtl = oldGlAcMap.get(glAccountRequest.getDetRowId());
                            if (defaultGlAcDtl != null) {
                                toSave.add(defaultGlAcDtl);
                            }
                        }
                        break;
                }
            }
        }
        
        if (!toSave.isEmpty()) {
            glAcDtlRepository.saveAll(toSave);
        }
        
        if (!toDelete.isEmpty()) {
            glAcDtlRepository.deleteAll(toDelete);
        }

        List<GlFavAcMasterUserRoleDtl> oldUserRoleDtls = userRoleDtlRepository.findByFavAcPoidOrderByDetRowId(favAcPoid);
        Map<Long, GlFavAcMasterUserRoleDtl> oldUserRoleMap = oldUserRoleDtls.stream()
                .collect(Collectors.toMap(GlFavAcMasterUserRoleDtl::getDetRowId, Function.identity(), (first, second) -> first));
        
        List<GlFavAcMasterUserRoleDtl> userRoleToSave = new ArrayList<>();
        List<GlFavAcMasterUserRoleDtl> userRoleToDelete = new ArrayList<>();
        List<LogRequestDto<GlFavAcMasterUserRoleDtl>> userRoleLogRequests = new ArrayList<>();
        List<GlobalLogSummary> userRoleSummaryLogs = new ArrayList<>();
        
        if (request.getUserRoles() != null && !request.getUserRoles().isEmpty()) {
            for (UserRoleDetailRequest userRoleRequest : request.getUserRoles()) {
                String actionTypeStr = userRoleRequest.getActionType();
                if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                    if (userRoleRequest.getDetRowId() == null || userRoleRequest.getDetRowId() == 0) {
                        actionTypeStr = "isCreated";
                    } else {
                        actionTypeStr = "isUpdated";
                    }
                }
                String actionType = actionTypeStr.toUpperCase();
                
                switch (actionType) {
                    case "ISCREATED":
                        GlFavAcMasterUserRoleDtl newUserRoleDtl = GlFavAcMasterUserRoleDtl.builder()
                                .favAcPoid(favAcPoid)
                                .detRowId(userRoleRequest.getDetRowId()) // Frontend provides detRowId
                                .userRolePoid(userRoleRequest.getUserRolePoid())
                                .createdBy(currentUser)
                                .createdDate(now)
                                .lastModifiedBy(currentUser)
                                .lastModifiedDate(now)
                                .build();
                        userRoleToSave.add(newUserRoleDtl);
                        
                        String createSummaryMessage = String.format("Row Created on Favorite Account Master User Role Detail with DetRowId: %s", userRoleRequest.getDetRowId());
                        GlobalLogSummary createSummaryLog = createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, createSummaryMessage);
                        userRoleSummaryLogs.add(createSummaryLog);
                        break;
                        
                    case "ISUPDATED":
                        if (userRoleRequest.getDetRowId() == null) {
                            throw new ValidationException("DetRowId is required for update operation");
                        }
                        GlFavAcMasterUserRoleDtl existingUserRoleDtl = userRoleDtlRepository
                                .findByFavAcPoidAndDetRowId(favAcPoid, userRoleRequest.getDetRowId())
                                .orElse(null);
                        
                        if (existingUserRoleDtl == null) {
                            GlFavAcMasterUserRoleDtl newUserRoleDtlFromUpdate = GlFavAcMasterUserRoleDtl.builder()
                                    .favAcPoid(favAcPoid)
                                    .detRowId(userRoleRequest.getDetRowId())
                                    .userRolePoid(userRoleRequest.getUserRolePoid())
                                    .createdBy(currentUser)
                                    .createdDate(now)
                                    .lastModifiedBy(currentUser)
                                    .lastModifiedDate(now)
                                    .build();
                            userRoleToSave.add(newUserRoleDtlFromUpdate);
                            
                            String createSummaryMessageFromUpdate = String.format("Row Created on Favorite Account Master User Role Detail with DetRowId: %s", userRoleRequest.getDetRowId());
                            GlobalLogSummary createSummaryLogFromUpdate = createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, createSummaryMessageFromUpdate);
                            userRoleSummaryLogs.add(createSummaryLogFromUpdate);
                            break;
                        }
                        
                        GlFavAcMasterUserRoleDtl oldUserRoleDtl = new GlFavAcMasterUserRoleDtl();
                        BeanUtils.copyProperties(existingUserRoleDtl, oldUserRoleDtl);
                        oldUserRoleDtl.setDetRowId(existingUserRoleDtl.getDetRowId());
                        oldUserRoleDtl.setFavAcPoid(existingUserRoleDtl.getFavAcPoid());
                        
                        existingUserRoleDtl.setUserRolePoid(userRoleRequest.getUserRolePoid());
                        existingUserRoleDtl.setLastModifiedBy(currentUser);
                        existingUserRoleDtl.setLastModifiedDate(now);
                        userRoleToSave.add(existingUserRoleDtl);
                        
                        String logDetail = String.format("KeyId = FAV_AC_POID:%s DET_ROW_ID:%s", 
                            oldUserRoleDtl.getFavAcPoid(), oldUserRoleDtl.getDetRowId());
                        userRoleLogRequests.add(new LogRequestDto<>(oldUserRoleDtl, existingUserRoleDtl, GlFavAcMasterUserRoleDtl.class, 
                            docId, docKeyPoid, logDetail));
                        break;
                        
                    case "ISDELETED":
                        if (userRoleRequest.getDetRowId() == null) {
                            throw new ValidationException("DetRowId is required for delete operation");
                        }
                        GlFavAcMasterUserRoleDtl userRoleDtlToDelete = userRoleDtlRepository
                                .findByFavAcPoidAndDetRowId(favAcPoid, userRoleRequest.getDetRowId())
                                .orElse(null);
                        if (userRoleDtlToDelete != null) {
                            userRoleToDelete.add(userRoleDtlToDelete);
                            
                            String deletedRecordString = String.format("detRowId:%s, favAcPoid:%s, userRolePoid:%s",
                                    userRoleDtlToDelete.getDetRowId(), userRoleDtlToDelete.getFavAcPoid(), userRoleDtlToDelete.getUserRolePoid());
                            String deleteSummaryMessage = String.format("Row Deleted %s", deletedRecordString);
                            GlobalLogSummary deleteSummaryLog = createSummaryLogEntry(LogDetailsEnum.DELETED, docId, docKeyPoid, deleteSummaryMessage);
                            userRoleSummaryLogs.add(deleteSummaryLog);
                        }
                        break;
                        
                    case "NOCHANGES", "NOCHANGE":
                        if (userRoleRequest.getDetRowId() != null) {
                            GlFavAcMasterUserRoleDtl unchangedUserRoleDtl = oldUserRoleMap.get(userRoleRequest.getDetRowId());
                            if (unchangedUserRoleDtl != null) {
                                userRoleToSave.add(unchangedUserRoleDtl);
                            }
                        }
                        break;
                        
                    default:
                        if (userRoleRequest.getDetRowId() != null) {
                            GlFavAcMasterUserRoleDtl defaultUserRoleDtl = oldUserRoleMap.get(userRoleRequest.getDetRowId());
                            if (defaultUserRoleDtl != null) {
                                userRoleToSave.add(defaultUserRoleDtl);
                            }
                        }
                        break;
                }
            }
        }
        
        for (GlFavAcMasterUserRoleDtl oldUserRoleDtl : oldUserRoleDtls) {
            boolean stillExists = request.getUserRoles() != null && 
                    request.getUserRoles().stream()
                            .anyMatch(ur -> ur.getDetRowId() != null && ur.getDetRowId().equals(oldUserRoleDtl.getDetRowId()));
            if (!stillExists) {
                userRoleToDelete.add(oldUserRoleDtl);
                
                String deletedRecordString = String.format("detRowId:%s, favAcPoid:%s, userRolePoid:%s",
                        oldUserRoleDtl.getDetRowId(), oldUserRoleDtl.getFavAcPoid(), oldUserRoleDtl.getUserRolePoid());
                String deleteSummaryMessage = String.format("Row Deleted %s", deletedRecordString);
                GlobalLogSummary deleteSummaryLog = createSummaryLogEntry(LogDetailsEnum.DELETED, docId, docKeyPoid, deleteSummaryMessage);
                userRoleSummaryLogs.add(deleteSummaryLog);
            }
        }
        
        if (!userRoleToSave.isEmpty()) {
            userRoleDtlRepository.saveAll(userRoleToSave);
        }
        
        if (!userRoleToDelete.isEmpty()) {
            userRoleDtlRepository.deleteAll(userRoleToDelete);
        }

        
        String modifiedMessage = String.format("Modified - - DOC:%s KEY:%s", docId, docKeyPoid);
        GlobalLogSummary headerUpdateLog = createSummaryLogEntry(LogDetailsEnum.MODIFIED, docId, docKeyPoid, modifiedMessage, now);
        globalLogSummaryRepository.save(headerUpdateLog);
        
        List<LogRequestDto<GlFavAcMaster>> headerLogRequests = new ArrayList<>();
        String logDetail = String.format("KeyId = FAV_AC_POID:%s", favAcPoid);
        headerLogRequests.add(new LogRequestDto<>(oldEntity, existing, GlFavAcMaster.class, docId, docKeyPoid, logDetail));
        if (!headerLogRequests.isEmpty()) {
            loggingService.createLogBatch(headerLogRequests);
        }
        
      
        if (!glAcLogRequests.isEmpty()) {
            loggingService.createLogBatch(glAcLogRequests);
        }
        if (!userRoleLogRequests.isEmpty()) {
            loggingService.createLogBatch(userRoleLogRequests);
        }
        
        if (!glAcSummaryLogs.isEmpty()) {
            globalLogSummaryRepository.saveAll(glAcSummaryLogs);
        }
        
        if (!userRoleSummaryLogs.isEmpty()) {
            globalLogSummaryRepository.saveAll(userRoleSummaryLogs);
        }

        return getFavoriteAccountById(favAcPoid);
    }

    /**
     * Get a Key Favorite Account Master by ID (internal use)
     */
    public GlFavAcMasterResponse getFavoriteAccountById(Long favAcPoid) {
        GlFavAcMaster master = masterRepository.findByFavAcPoid(favAcPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite Account Master", "favAcPoid", favAcPoid));

        GlFavAcMasterResponse response = new GlFavAcMasterResponse();
        BeanUtils.copyProperties(master, response);

        List<GlFavAcMasterGlAcDtl> glAcDtls = glAcDtlRepository.findByFavAcPoidOrderByDetRowId(favAcPoid);
        List<GlAccountDetailResponse> glAccountResponses = glAcDtls.stream()
                .map(this::mapToGlAccountDetailResponse)
                .sorted(Comparator.comparing(GlAccountDetailResponse::getDetRowId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        setCompanyDetails(glAccountResponses);
        setViewCategoryDetails(glAccountResponses);
        response.setGlAccounts(glAccountResponses);

        List<GlFavAcMasterUserRoleDtl> userRoleDtls = userRoleDtlRepository.findByFavAcPoidOrderByDetRowId(favAcPoid);
        List<UserRoleDetailResponse> userRoleResponses = userRoleDtls.stream()
                .map(this::mapToUserRoleDetailResponse)
                .sorted(Comparator.comparing(UserRoleDetailResponse::getDetRowId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        response.setUserRoles(userRoleResponses);

        List<Long> glPoids = glAccountResponses.stream()
                .map(GlAccountDetailResponse::getGlPoid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // Only query database if there are GL POIDs to look up
        List<GLMaster> glMasters = glPoids.isEmpty() ? Collections.emptyList() 
                : glMasterRepository.findByGlPoidIn(glPoids);

        Map<Long, GLMaster> glMap = glMasters.stream()
                .collect(Collectors.toMap(GLMaster::getGlPoid, Function.identity()));

        glAccountResponses.forEach(glAccountDetailResponse -> {
            Long glPoid = glAccountDetailResponse.getGlPoid();
            GLMaster glRecord = glMap.get(glPoid);

            if (glRecord != null) {
                GlMasterDto glMasterDto = GlMasterDto.builder()
                        .glPoid(glRecord.getGlPoid())
                        .groupPoid(glRecord.getGroupPoid())
                        .glCode(glRecord.getGlCode())
                        .glDescription(glRecord.getGlDescription())
                        .glDescription2(glRecord.getGlDescription2())
                        .glType(glRecord.getGlType())
                        .groupGlPoid(glRecord.getGroupGlPoid())
                        .controlAcNature(glRecord.getControlAcNature())
                        .costGroup(glRecord.getCostGroup())
                        .billwise(glRecord.getBillwise())
                        .prepaymentLedger(glRecord.getPrepaymentLedger())
                        .interCompanyAc(glRecord.getInterCompanyAc())
                        .interCompanyPoid(glRecord.getInterCompanyPoid())
                        .remarks(glRecord.getRemarks())
                        .seqno(glRecord.getSeqno())
                        .active(glRecord.getActive())
                        .groupCodeOld(glRecord.getGroupCodeOld())
                        .glAcType(glRecord.getGlAcType())
                        .deleted(glRecord.getDeleted())
                        .amountLimit(glRecord.getAmountLimit())
                        .amountRol(glRecord.getAmountRol())
                        .oldOrgCode(glRecord.getOldOrgCode())
                        .oldOriginalCode(glRecord.getOldOriginalCode())
                        .oldModCode(glRecord.getOldModCode())
                        .build();
                glAccountDetailResponse.setGlDetails(glMasterDto);
            } else {
                glAccountDetailResponse.setGlDetails(null);
            }

        });
        response.setGlAccounts(glAccountResponses);


        List<Long> rolePoids = userRoleResponses.stream()
                .map(UserRoleDetailResponse::getUserRolePoid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // Only query external service if there are role POIDs to look up
        List<RoleDto> roleDtos = rolePoids.isEmpty() ? Collections.emptyList() 
                : roleServiceClient.findByUserRolePoidIn(rolePoids);

        Map<Long, RoleDto> roleMap = roleDtos.stream()
                .collect(Collectors.toMap(RoleDto::getUserRolePoid, Function.identity()));

        userRoleResponses.forEach(userRoleDetailResponse -> {
            Long poid = userRoleDetailResponse.getUserRolePoid();
            RoleDto roleDto = roleMap.get(poid);

            if (roleDto != null) {
                UserRoleDto.builder().userRolePoId(roleDto.getUserRolePoid()).userRoleName(roleDto.getUserRoleName()).userRoleId(roleDto.getUserRoleId()).build();
                userRoleDetailResponse.setUserRoleDetail(UserRoleDto.builder().userRolePoId(roleDto.getUserRolePoid()).userRoleName(roleDto.getUserRoleName()).userRoleId(roleDto.getUserRoleId()).build());
            } else {
                userRoleDetailResponse.setUserRoleDetail(null);
            }
        });

        return response;
    }

    /**
     * Validate that GL accounts are not duplicated within the same request
     */
    private void validateNoDuplicateGlAccounts(List<GlAccountDetailRequest> glAccounts) {
        Set<Long> glPoids = new HashSet<>();
        for (GlAccountDetailRequest glAccount : glAccounts) {
            if (!glPoids.add(glAccount.getGlAccountPoId())) {
                throw new ValidationException("GL Account should not be duplicated for the same Favorite Account Group");
            }
        }
    }

    /**
     * Filter out null and zero values from userRolePoids list.
     * Returns null if the input is null, or an empty list if all values are filtered out.
     * This allows [0] or [null] to be treated as empty (not mandatory).
     */
    private List<Long> filterValidUserRolePoids(List<Long> userRolePoids) {
        if (userRolePoids == null) {
            return null;
        }
        List<Long> filtered = userRolePoids.stream()
                .filter(poid -> poid != null && poid != 0)
                .collect(Collectors.toList());
        return filtered.isEmpty() ? null : filtered;
    }

    /**
     * Validate that user roles exist in the database.
     * If userRolePoids is null or empty, validation is skipped (e.g., when userRolePoids: [0] or []).
     */
    private void validateUserRolesExist(List<Long> userRolePoids) {
        if (userRolePoids == null || userRolePoids.isEmpty()) {
            return;
        }
        
        List<RoleDto> userRoles = roleServiceClient.findByUserRolePoidIn(userRolePoids);
        Set<Long> existingRoleIds = userRoles.stream()
                .map(RoleDto::getUserRolePoid)
                .collect(Collectors.toSet());

        for (Long userRolePoid : userRolePoids) {
            if (!existingRoleIds.contains(userRolePoid)) {
                throw new ValidationException("User Role does not exist: " + userRolePoid);
            }
        }
    }

    /**
     * Get current user from security context
     */
    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    /**
     * Map entity to GL Account Detail Response
     */
    private GlAccountDetailResponse mapToGlAccountDetailResponse(GlFavAcMasterGlAcDtl entity) {
        GlAccountDetailResponse response = new GlAccountDetailResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }

    /**
     * Map entity to User Role Detail Response
     */
    private UserRoleDetailResponse mapToUserRoleDetailResponse(GlFavAcMasterUserRoleDtl entity) {
        UserRoleDetailResponse response = new UserRoleDetailResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }

    @Override
    @Transactional
    public void softDeleteFavoriteAccount(Long favAcPoid, DeleteReasonDto deleteReasonDto) {
        GlFavAcMaster existing = masterRepository.findByFavAcPoid(favAcPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite Account Master", "favAcPoid", favAcPoid));

        // Use DocumentDeleteService for consistent soft delete handling
        documentDeleteService.deleteDocument(
                favAcPoid,
                "GL_FAV_AC_MASTER",
                "FAV_AC_POID",
                deleteReasonDto,
                null
        );
    }

    public Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, Pageable pageable) {

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "FAC_AC_POID",
                "DESCRIPTION");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private void setCompanyDetails(List<GlAccountDetailResponse> glAccountResponses) {
        Map<String, Object> listValue = lovService.getLovList(
                "", 0L, 0L, 0L,
                "COMPANY",
                0, 0, // pageSize 0 = all
                "", ""
        );

        if (listValue != null) {

            // ✅ Correct key is usually "data" (lowercase), not "Data"
            @SuppressWarnings("unchecked")
            List<LovGetListDto> lovGetListDtos = (List<LovGetListDto>) listValue.get("data");

            if (lovGetListDtos != null) {

                // ✅ Convert LovGetListDto -> CompanyDto map
                Map<Long, Company> companyMap = lovGetListDtos.stream()
                        .collect(Collectors.toMap(
                                LovGetListDto::getPoid,
                                lov -> {
                                    Company company = new Company();
                                    company.setCompanyPoid(lov.getPoid());
                                    company.setCompanyCode(lov.getCode());
                                    company.setCompanyName(lov.getDescription()); // or use getLabel()
                                    return company;
                                }
                        ));

                // ✅ Assign companyDetails into glAccountResponses
                glAccountResponses = glAccountResponses.stream()
                        .peek(gl -> gl.setCompanyDetails(companyMap.get(gl.getCompany()))) // gl.getCompany() must be companyId
                        .collect(Collectors.toList());
            }
        }
    }


    @SuppressWarnings("unchecked")
    private void setViewCategoryDetails(List<GlAccountDetailResponse> glAccountResponses) {
        Map<String, Object> listValue = lovService.getLovList(
                "", 0L, 0L, 0L,
                "FAV_VIEW_CATEGORY",
                0, 0,
                "", ""
        );

        if (listValue == null) return;

        List<LovGetListDto> lovGetListDtos = (List<LovGetListDto>) listValue.get("data");
        if (lovGetListDtos == null || lovGetListDtos.isEmpty()) return;

        // Build map of POID → LOV object
        Map<Long, LovGetListDto> lovMap = lovGetListDtos.stream()
                .collect(Collectors.toMap(LovGetListDto::getPoid, Function.identity()));

        // Update existing list in-place (don’t reassign)
        for (GlAccountDetailResponse gl : glAccountResponses) {
            if (gl.getViewCategory() != null) {
                try {
                    Long viewCategoryId = Long.valueOf(gl.getViewCategory());
                    LovGetListDto lovDto = lovMap.get(viewCategoryId);
                    gl.setViewCategoryDetails(lovDto);
                } catch (NumberFormatException e) {
                    // In case viewCategory is not a number
                    gl.setViewCategoryDetails(null);
                }
            } else {
                gl.setViewCategoryDetails(null);
            }
        }
    }
    
 
    private GlobalLogSummary createSummaryLogEntry(LogDetailsEnum logDetailsEnum, String docId, String docKeyPoid, String customMessage) {
        return createSummaryLogEntry(logDetailsEnum, docId, docKeyPoid, customMessage, null);
    }

    private GlobalLogSummary createSummaryLogEntry(LogDetailsEnum logDetailsEnum, String docId, String docKeyPoid, String customMessage, LocalDateTime logDateTime) {
        GlobalLogSummary summary = new GlobalLogSummary();
        summary.setLogUserPoid(UserContext.getUserPoid());
        summary.setLogDateTime(logDateTime != null ? java.sql.Timestamp.valueOf(logDateTime) : java.sql.Timestamp.valueOf(LocalDateTime.now()));
        summary.setLogDocId(docId);
        summary.setLogDocKeyPoid(docKeyPoid);
        summary.setLogDetails(customMessage);
        return summary;
    }


}

