package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.entity.Company;
import com.asg.common.lib.exception.ResourceNotFoundException;
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
import com.asg.finance.repository.GlFavAcMasterGlAcDtlRepository;
import com.asg.finance.repository.GlFavAcMasterRepository;
import com.asg.finance.repository.GlFavAcMasterUserRoleDtlRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.GlFavAcMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service implementation for Key Favorite Account Master operations
 */
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

        // Validate user roles exist (if provided)
        if (request.getUserRolePoids() != null && !request.getUserRolePoids().isEmpty()) {
            validateUserRolesExist(request.getUserRolePoids());
        }

        String currentUser = getCurrentUser();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Create master record
        GlFavAcMaster master = GlFavAcMaster.builder()
                .groupPoid(UserContext.getGroupPoid())
                .favAcCode(request.getFavAcCode())
                .description(request.getDescription())
                .description2(request.getDescription2())
                .active(request.getActive() != null ? request.getActive() : "Y")
                .seqNo(request.getSeqNo())
                .createdBy(currentUser)
                .createdDate(now)
                .lastModifiedBy(currentUser)
                .lastModifiedDate(now)
                .deleted("N")
                .build();

        GlFavAcMaster savedMaster = masterRepository.save(master);

        // Create GL account detail records
        if (request.getGlAccounts() != null && !request.getGlAccounts().isEmpty()) {
            for (GlAccountDetailRequest glAccountRequest : request.getGlAccounts()) {
                GlFavAcMasterGlAcDtl glAcDtl = GlFavAcMasterGlAcDtl.builder()
                        .favAcPoid(savedMaster.getFavAcPoid())
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
            }
        }

        // Create user role detail records
        if (request.getUserRolePoids() != null && !request.getUserRolePoids().isEmpty()) {
            for (Long userRolePoid : request.getUserRolePoids()) {
                GlFavAcMasterUserRoleDtl userRoleDtl = GlFavAcMasterUserRoleDtl.builder()
                        .favAcPoid(savedMaster.getFavAcPoid())
                        .userRolePoid(userRolePoid)
                        .createdBy(currentUser)
                        .createdDate(now)
                        .lastModifiedBy(currentUser)
                        .lastModifiedDate(now)
                        .build();
                userRoleDtlRepository.save(userRoleDtl);
            }
        }

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

        // Validate user roles exist (if provided)
        if (request.getUserRolePoids() != null && !request.getUserRolePoids().isEmpty()) {
            validateUserRolesExist(request.getUserRolePoids());
        }

        String currentUser = getCurrentUser();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Update master record
        existing.setFavAcCode(request.getFavAcCode());
        existing.setDescription(request.getDescription());
        existing.setDescription2(request.getDescription2());
        existing.setActive(request.getActive());
        existing.setSeqNo(request.getSeqNo());
        existing.setLastModifiedBy(currentUser);
        existing.setLastModifiedDate(now);

        masterRepository.save(existing);

        // Delete existing GL account details and recreate
        glAcDtlRepository.deleteByFavAcPoid(favAcPoid);
        glAcDtlRepository.flush(); // Ensure deletes are committed before inserts
        
        if (request.getGlAccounts() != null && !request.getGlAccounts().isEmpty()) {
            for (GlAccountDetailRequest glAccountRequest : request.getGlAccounts()) {
                GlFavAcMasterGlAcDtl glAcDtl = GlFavAcMasterGlAcDtl.builder()
                        .favAcPoid(favAcPoid)
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
            }
        }

        // Delete existing user role details and recreate
        userRoleDtlRepository.deleteByFavAcPoid(favAcPoid);
        userRoleDtlRepository.flush(); // Ensure deletes are committed before inserts
        
        if (request.getUserRolePoids() != null && !request.getUserRolePoids().isEmpty()) {
            for (Long userRolePoid : request.getUserRolePoids()) {
                GlFavAcMasterUserRoleDtl userRoleDtl = GlFavAcMasterUserRoleDtl.builder()
                        .favAcPoid(favAcPoid)
                        .userRolePoid(userRolePoid)
                        .createdBy(currentUser)
                        .createdDate(now)
                        .lastModifiedBy(currentUser)
                        .lastModifiedDate(now)
                        .build();
                userRoleDtlRepository.save(userRoleDtl);
            }
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

        // Fetch and set GL account details
        List<GlFavAcMasterGlAcDtl> glAcDtls = glAcDtlRepository.findByFavAcPoidOrderBySeqNo(favAcPoid);
        List<GlAccountDetailResponse> glAccountResponses = glAcDtls.stream()
                .map(this::mapToGlAccountDetailResponse)
                .collect(Collectors.toList());
        setCompanyDetails(glAccountResponses);
        setViewCategoryDetails(glAccountResponses);
        response.setGlAccounts(glAccountResponses);

        List<GlFavAcMasterUserRoleDtl> userRoleDtls = userRoleDtlRepository.findByFavAcPoid(favAcPoid);
        List<UserRoleDetailResponse> userRoleResponses = userRoleDtls.stream()
                .map(this::mapToUserRoleDetailResponse)
                .collect(Collectors.toList());
        response.setUserRoles(userRoleResponses);

        List<Long> glPoids = glAccountResponses.stream()
                .map(GlAccountDetailResponse::getGlPoid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<GLMaster> glMasters = glMasterRepository.findByGlPoidIn(glPoids);

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

        List<RoleDto> roleDtos = roleServiceClient.findByUserRolePoidIn(rolePoids);

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
     * Validate that user roles exist in the database
     */
    private void validateUserRolesExist(List<Long> userRolePoids) {

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
    public void softDeleteFavoriteAccount(Long favAcPoid, com.asg.common.lib.dto.DeleteReasonDto deleteReasonDto) {
        GlFavAcMaster existing = masterRepository.findByFavAcPoid(favAcPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite Account Master", "favAcPoid", favAcPoid));

        // Use DocumentDeleteService for consistent soft delete handling
        documentDeleteService.deleteDocument(
                favAcPoid,
                "GL_FAV_AC_MASTER",
                "FAV_AC_POID",
                deleteReasonDto.getDeleteReason(),
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


}

