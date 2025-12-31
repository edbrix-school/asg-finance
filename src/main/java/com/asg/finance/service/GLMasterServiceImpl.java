package com.asg.finance.service;


import com.asg.common.lib.dto.*;

import com.asg.common.lib.dto.request.DocReleaseLockRequestDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.finance.dto.*;

import com.asg.finance.entity.GLMasterCompanyDtlEntity;
import com.asg.finance.entity.GLMasterEntity;
import com.asg.finance.entity.GLPaymentDetailsEntity;
import com.asg.finance.repository.GLMasterCompanyDtlRepository;
import com.asg.finance.repository.GLMasterTreeViewRepository;
import com.asg.finance.repository.GLMastersRepository;
import com.asg.finance.repository.GLPaymentDetailsRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class GLMasterServiceImpl implements GLMasterService {

    private static final Logger log = LoggerFactory.getLogger(GLMasterServiceImpl.class);

    @Autowired
    private GLMastersRepository glMasterRepo;

    @Autowired
    private GLPaymentDetailsRepository payDtlRepo;

    @Autowired
    private GLMasterCompanyDtlRepository companyDtlRepo;

    @Autowired
    private GLMasterTreeViewRepository glMasterTreeViewRepository;

    @Autowired
    private GLMasterCustomService glMasterCustomService;

    @Autowired
    private DocumentSearchService documentService;

    @Autowired
    private LovDataService lovService;

    private String getCurrentUser() {
        return ASGHelperUtils.getCurrentUser(); // dynamically fetch current user
    }


    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Converts active flag from various formats (boolean, String) to database format ("Y" or "N")
     * Handles: null, boolean true/false, String "true"/"false", String "Y"/"N"
     */
    private String convertToActiveFlag(Object active) {
        if (active == null) {
            return "Y"; // Default to active
        }
        
        // Handle boolean
        if (active instanceof Boolean) {
            return ((Boolean) active) ? "Y" : "N";
        }
        
        // Handle String
        if (active instanceof String) {
            String activeStr = ((String) active).trim().toUpperCase();
            if ("TRUE".equals(activeStr) || "Y".equals(activeStr) || "YES".equals(activeStr) || "1".equals(activeStr)) {
                return "Y";
            }
            if ("FALSE".equals(activeStr) || "N".equals(activeStr) || "NO".equals(activeStr) || "0".equals(activeStr)) {
                return "N";
            }
            // If already "Y" or "N", return as is
            if ("Y".equals(activeStr) || "N".equals(activeStr)) {
                return activeStr;
            }
        }
        
        // Default to "Y" if unrecognized format
        return "Y";
    }

    @Override
    public GLMasterResponseDto createGLMaster(GLMasterRequestDto req) {
        // Auto-generate GL code if not provided (database trigger will handle it)
        String glCode = req.getGlCode();
        if (glCode == null || glCode.trim().isEmpty()) {
            glCode = null; // Let trigger generate the code
        }

        if (glCode != null && glMasterRepo.existsByGlCode(glCode)) {
            throw new RuntimeException("GL Code already exists: " + glCode);
        }

        String type = req.getType();
        Long subOf = req.getSubOf();
        if ("LEDGER".equals(type) || "SUB_GROUP".equals(type)) {
            if (subOf == null) {
                throw new RuntimeException("subOf (parent GL_Poid) is required when type = LEDGER or SUB_GROUP");
            }
        }

        String parentType = null;
        if (subOf != null) {
            Optional<GLMasterEntity> parentOpt = glMasterRepo.findById(subOf);
            if (parentOpt.isEmpty()) {
                throw new RuntimeException("Parent GL not found with POID: " + subOf);
            }
            parentType = parentOpt.get().getType();
        }
        if ("LEDGER".equals(parentType)) {
            throw new RuntimeException("Selected Group cannot be LEDGER account");
        }

        GLMasterEntity entity = new GLMasterEntity();
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setGlCode(glCode);
        entity.setDescription(req.getDescription());
        entity.setDescription2(req.getDescription2());
        entity.setType(req.getType());
        entity.setGroupGlPoid(subOf);
        entity.setAccountType(req.getAccountType());
        entity.setControlAcType(req.getControlAcType());
        entity.setCostGroup(req.getCostGroup());
        entity.setInterCompanyFlag(req.getInterCompany() != null && req.getInterCompany() ? "Y" : "N");
        entity.setInterCompanyId(req.getInterCompanyId());
        entity.setRemarks(req.getRemarks());
        entity.setSeqNo(req.getSeqNo());
        entity.setActiveFlag(req.getActive() != null && req.getActive() ? "Y" : "N");
        entity.setBillWiseFlag(req.getBillWise() != null && req.getBillWise() ? "Y" : "N");
        entity.setPrepaymentLedgerFlag(req.getPrepaymentLedger() != null && req.getPrepaymentLedger() ? "Y" : "N");
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(now());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(now());

        entity = glMasterRepo.save(entity);
        glMasterRepo.flush(); // Force immediate database write

        // Get trigger-generated GL_CODE directly from database
        try {
            // Simple workaround: query the database directly for the generated code
            GLMasterEntity refreshedEntity = glMasterRepo.findById(entity.getGlPoid()).orElse(null);
            if (refreshedEntity != null && refreshedEntity.getGlCode() != null) {
                entity.setGlCode(refreshedEntity.getGlCode());
            } else {
                // Fallback: construct expected GL_CODE
                entity.setGlCode(entity.getAccountType() + entity.getGlPoid());
            }
        } catch (Exception e) {
            log.warn("Failed to get generated GL_CODE, using fallback: {}", e.getMessage());
            entity.setGlCode(entity.getAccountType() + entity.getGlPoid());
        }


        if (req.getPaymentDetails() != null && !req.getPaymentDetails().isEmpty()) {
            savePaymentDetails(entity, req.getPaymentDetails());
        }

        if (req.getCompanyDetails() != null && !req.getCompanyDetails().isEmpty()) {
            saveCompanyDetails(entity, req.getCompanyDetails());
        }

        // Company details handling can be added here if needed

        propagateToChildren(entity);

        return toResponseDto(entity);
    }


    @Override
    public GLMasterResponseDto getGLMaster(Long glPoid) {
        GLMasterEntity entity = glMasterRepo.findById(glPoid)
                .filter(e -> !"Y".equalsIgnoreCase(e.getDeletedFlag()))  // Exclude if deleted
                .orElseThrow(() -> new RuntimeException("GL Master not found or deleted: " + glPoid));

        return toResponseDto(entity);
    }

    @Override
    public com.asg.common.lib.dto.GLMasterDto getGLMasterDto(Long glPoid) {
        GLMasterEntity entity = glMasterRepo.findById(glPoid)
                .filter(e -> !"Y".equalsIgnoreCase(e.getDeletedFlag()))
                .orElseThrow(() -> new RuntimeException("GL Master not found or deleted: " + glPoid));
        return mapToDto(entity);
    }

    @Override
    public List<com.asg.common.lib.dto.GLMasterDto> getGLMasterDtos(List<Long> glPoids) {
        List<GLMasterEntity> entities = glMasterRepo.findByGlPoidIn(glPoids);
        return entities.stream()
                .filter(e -> !"Y".equalsIgnoreCase(e.getDeletedFlag()))
                .map(this::mapToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    private com.asg.common.lib.dto.GLMasterDto mapToDto(GLMasterEntity entity) {
        return GLMasterDto.builder()
                .glPoid(entity.getGlPoid())
                .glCode(entity.getGlCode())
                .glDescription(entity.getDescription())
                .glDescription2(entity.getDescription2())
                .glType(entity.getType())
                .controlAcNature(entity.getControlAcType())
                .costGroup(entity.getCostGroup())
                .billwise(entity.getBillWiseFlag())
                .prepaymentLedger(entity.getPrepaymentLedgerFlag())
                .interCompanyAc(entity.getInterCompanyFlag())
                .glAcType(entity.getAccountType())
                .build();
    }


    @Override
    public GLMasterResponseDto updateGLMaster(Long glPoid, GLMasterRequestDto req) {
        GLMasterEntity entity = glMasterRepo.findById(glPoid)
                .orElseThrow(() -> new RuntimeException("GL Master not found: " + glPoid));

        if (glMasterRepo.existsByGlCodeAndGlPoidNot(req.getGlCode(), glPoid)) {
            throw new RuntimeException("GL Code already in use by another: " + req.getGlCode());
        }

        String type = req.getType();
        Long subOf = req.getSubOf();
        if ("LEDGER".equals(type) || "SUB_GROUP".equals(type)) {
            if (subOf == null) {
                throw new RuntimeException("subOf is required for LEDGER or SUB_GROUP");
            }
        }

        String parentType = null;
        if (subOf != null) {
            Optional<GLMasterEntity> parentOpt = glMasterRepo.findById(subOf);
            if (parentOpt.isEmpty()) {
                throw new RuntimeException("Parent GL not found: " + subOf);
            }
            parentType = parentOpt.get().getType();
        }
        if ("LEDGER".equals(parentType)) {
            throw new RuntimeException("Selected parent cannot be LEDGER account");
        }

        entity.setGlCode(req.getGlCode());
        entity.setDescription(req.getDescription());
        entity.setDescription2(req.getDescription2());
        entity.setType(req.getType());
        entity.setGroupGlPoid(subOf);
        entity.setAccountType(req.getAccountType());
        entity.setControlAcType(req.getControlAcType());
        entity.setCostGroup(req.getCostGroup());
        entity.setInterCompanyFlag(req.getInterCompany() != null && req.getInterCompany() ? "Y" : "N");
        entity.setInterCompanyId(req.getInterCompanyId());
        entity.setRemarks(req.getRemarks());
        entity.setSeqNo(req.getSeqNo());
        entity.setActiveFlag(req.getActive() != null && req.getActive() ? "Y" : "N");
        entity.setBillWiseFlag(req.getBillWise() != null && req.getBillWise() ? "Y" : "N");
        entity.setPrepaymentLedgerFlag(req.getPrepaymentLedger() != null && req.getPrepaymentLedger() ? "Y" : "N");
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(now());

        glMasterRepo.save(entity);

        if (req.getPaymentDetails() != null && !req.getPaymentDetails().isEmpty()) {
            updateGlPaymentDetails(req.getPaymentDetails(), glPoid);
        }

        if (req.getCompanyDetails() != null && !req.getCompanyDetails().isEmpty()) {
            updateGlCompanyDetails(req.getCompanyDetails(), glPoid);
        }
        propagateToChildren(entity);

        return toResponseDto(entity);
    }

    private void saveCompanyDetails(GLMasterEntity entity, List<CompanyDetailsDto> companyDetails) {
        // For CREATE: filter out "noChanges" and "isDeleted", only process "isCreated" or null/empty
        List<GLMasterCompanyDtlEntity> entities = companyDetails.stream()
                .filter(cdto -> {
                    String actionTypeStr = cdto.getActionType();
                    if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                        return true; // Default to create if actionType is null/empty
                    }
                    String actionType = actionTypeStr.toUpperCase();
                    // Only process "ISCREATED", skip "NOCHANGES" and "ISDELETED" in CREATE
                    return "ISCREATED".equals(actionType);
                })
                .map(cdto -> {
                    GLMasterCompanyDtlEntity c = new GLMasterCompanyDtlEntity();
                    c.setGlMaster(entity);
                    c.setGlPoid(entity.getGlPoid());
                    c.setCompanyPoid(cdto.getCompanyPoid());
                    c.setRemarks(cdto.getRemarks());
                    c.setCreatedBy(getCurrentUser());
                    c.setCreatedDate(now());
                    c.setLastModifiedBy(getCurrentUser());
                    c.setLastModifiedDate(now());
                    return c;
                }).toList();
        if (!entities.isEmpty()) {
            companyDtlRepo.saveAll(entities);
        }
    }

    private void savePaymentDetails(GLMasterEntity entity, List<PaymentDetailsDto> paymentDetails) {
        // For CREATE: filter out "noChanges" and "isDeleted", only process "isCreated" or null/empty
        List<GLPaymentDetailsEntity> entities = paymentDetails.stream()
                .filter(pdto -> {
                    String actionTypeStr = pdto.getActionType();
                    if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                        return true; // Default to create if actionType is null/empty
                    }
                    String actionType = actionTypeStr.toUpperCase();
                    // Only process "ISCREATED", skip "NOCHANGES" and "ISDELETED" in CREATE
                    return "ISCREATED".equals(actionType);
                })
                .map(pdto -> {
                    GLPaymentDetailsEntity payEntity = new GLPaymentDetailsEntity();
                    payEntity.setGlMaster(entity);
                    payEntity.setGlPoid(entity.getGlPoid());  // Explicitly set composite key component
                    payEntity.setType(pdto.getType());
                    payEntity.setBeneficiaryName(pdto.getBeneficiaryName());
                    payEntity.setAddress(pdto.getAddress());
                    payEntity.setBank(pdto.getBank());
                    payEntity.setBankAddress(pdto.getBankAddress());
                    payEntity.setBeneficiaryCountry(pdto.getBeneficiaryCountry());
                    payEntity.setSwiftCode(pdto.getSwiftCode());
                    payEntity.setAccountNumber(pdto.getAccountNumber());
                    payEntity.setIban(pdto.getIban());
                    payEntity.setIntermediaryBank(pdto.getIntermediaryBank());
                    payEntity.setIntermediaryAcct(pdto.getIntermediaryAcct());
                    payEntity.setBankSwiftCode(pdto.getIntermediarySwiftCode());
                    payEntity.setIntermediaryCountryPoid(pdto.getIntermediaryCountryPoid());
                    payEntity.setActive(convertToActiveFlag(pdto.getActive()));
                    payEntity.setCreatedBy(getCurrentUser());
                    payEntity.setCreatedDate(now());
                    payEntity.setLastModifiedBy(getCurrentUser());
                    payEntity.setLastModifiedDate(now());
                    return payEntity;
                }).toList();
        if (!entities.isEmpty()) {
            payDtlRepo.saveAll(entities);
        }
    }


    @Override
    public void deleteGLMaster(Long glPoid) {
        GLMasterEntity entity = glMasterRepo.findById(glPoid)
                .orElseThrow(() -> new RuntimeException("GL Master not found: " + glPoid));

        // Check if this GL Master has active children
        if (hasActiveChildren(glPoid)) {
            throw new ValidationException("This GL Master cannot be deleted as it has related child records.");
        }

        entity.setActiveFlag("N");   // mark inactive
        entity.setDeletedFlag("Y");  // mark deleted (you need to add this field if not present)
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());

        glMasterRepo.save(entity);
    }

    private boolean hasActiveChildren(Long parentPoid) {
        return glMasterRepo.existsByGroupGlPoidAndDeletedFlag(parentPoid, "N");
    }


    private void propagateToChildren(GLMasterEntity parent) {
        GLMasterEntity probe = new GLMasterEntity();
        probe.setGroupGlPoid(parent.getGlPoid());
        List<GLMasterEntity> children = glMasterRepo.findAll(Example.of(probe));

        if (children.isEmpty()) {
            return;
        }
        for (GLMasterEntity child : children) {
            child.setAccountType(parent.getAccountType());
            child.setControlAcType(parent.getControlAcType());
            child.setCostGroup(parent.getCostGroup());
            child.setBillWiseFlag(parent.getBillWiseFlag());
            child.setLastModifiedBy(getCurrentUser());
            child.setLastModifiedDate(now());
        }
        glMasterRepo.saveAll(children);
    }

    private GLMasterResponseDto toResponseDto(GLMasterEntity entity) {
        GLMasterResponseDto dto = new GLMasterResponseDto();
        dto.setGlPoid(entity.getGlPoid());
        dto.setGlCode(entity.getGlCode());

        dto.setGlCode(entity.getGlCode());
        dto.setDescription(entity.getDescription());
        dto.setDescription2(entity.getDescription2());
        dto.setType(entity.getType());
        dto.setSubOf(entity.getGroupGlPoid());
        if (entity.getGroupGlPoid() != null) {
            dto.setSubOfDet(lovService.getDetailsByPoidAndLovName(entity.getGroupGlPoid(), "GL_MASTER_GROUPS"));
        }
        dto.setAccountType(entity.getAccountType());
        dto.setControlAcType(entity.getControlAcType());
        dto.setCostGroup(entity.getCostGroup());
        dto.setInterCompany("Y".equals(entity.getInterCompanyFlag()));
        dto.setInterCompanyId(entity.getInterCompanyId());
        if (entity.getInterCompanyId() != null) {
            dto.setInterCompanyDet(lovService.getDetailsByPoidAndLovName(entity.getInterCompanyId(), "COMPANY"));
        }
        dto.setRemarks(entity.getRemarks());
        dto.setSeqNo(entity.getSeqNo());
        dto.setActive("Y".equals(entity.getActiveFlag()));
        dto.setBillWise("Y".equals(entity.getBillWiseFlag()));
        dto.setPrepaymentLedger("Y".equals(entity.getPrepaymentLedgerFlag()));

        List<GLPaymentDetailsEntity> paymentDetails = payDtlRepo.findAllByGlMaster_GlPoid(entity.getGlPoid());
        if (!paymentDetails.isEmpty()) {
            dto.setPaymentDetails(paymentDetails.stream().map(pay -> {
                PaymentDetailsDto pdto = new PaymentDetailsDto();
                pdto.setDetRowId(pay.getId());
                pdto.setGlPoid(pay.getGlPoid());
                pdto.setType(pay.getType());
                pdto.setBeneficiaryName(pay.getBeneficiaryName());
                pdto.setAddress(pay.getAddress());
                pdto.setBank(pay.getBank());
                pdto.setBankAddress(pay.getBankAddress());
                pdto.setBeneficiaryCountry(pay.getBeneficiaryCountry());
                // Populate beneficiary country details
                if (pay.getBeneficiaryCountry() != null && pay.getBeneficiaryCountry() > 0) {
                    try {
                        LovGetListDto countryDetail = lovService.getDetailsByPoidAndLovName(pay.getBeneficiaryCountry(), "COUNTRY");
                        CountryInfoDto countryInfo = new CountryInfoDto();
                        countryInfo.setCountryPoid(countryDetail.getPoid());
                        countryInfo.setCountryCode(countryDetail.getCode());
                        countryInfo.setCountryName(countryDetail.getLabel());
                        pdto.setBeneficiaryCountryDetails(countryInfo);
                    } catch (Exception e) {
                        log.warn("Failed to fetch beneficiary country details for ID: {}", pay.getBeneficiaryCountry());
                    }
                }
                pdto.setSwiftCode(pay.getSwiftCode());
                pdto.setAccountNumber(pay.getAccountNumber());
                pdto.setIban(pay.getIban());
                pdto.setIntermediaryBank(pay.getIntermediaryBank());
                pdto.setIntermediaryAcct(pay.getIntermediaryAcct());
                pdto.setIntermediarySwiftCode(pay.getBankSwiftCode());
                pdto.setIntermediaryCountryPoid(pay.getIntermediaryCountryPoid());
                // Populate intermediary country details
                if (pay.getIntermediaryCountryPoid() != null && pay.getIntermediaryCountryPoid() > 0) {
                    try {
                        LovGetListDto countryDetail = lovService.getDetailsByPoidAndLovName(pay.getBeneficiaryCountry(), "COUNTRY");
                        CountryInfoDto countryInfo = new CountryInfoDto();
                        countryInfo.setCountryPoid(countryDetail.getPoid());
                        countryInfo.setCountryCode(countryDetail.getCode());
                        countryInfo.setCountryName(countryDetail.getLabel());
                        pdto.setIntermediaryCountryDetails(countryInfo);
                    } catch (Exception e) {
                        log.warn("Failed to fetch intermediary country details for ID: {}", pay.getIntermediaryCountryPoid());
                    }
                }
                pdto.setActive("Y");
                return pdto;
            }).toList());
        }
        List<GLMasterCompanyDtlEntity> companyDtls = companyDtlRepo.findByGlPoid(entity.getGlPoid());
        if (!companyDtls.isEmpty()) {
            dto.setCompanyDetails(companyDtls.stream().map(cd -> {
                CompanyDetailsDto d = new CompanyDetailsDto();
                d.setDetRowId(cd.getId());
                d.setGlPoid(cd.getGlPoid());
                d.setCompanyPoid(cd.getCompanyPoid());
                if (cd.getCompanyPoid() != null) {
                    d.setCompanyDet(lovService.getDetailsByPoidAndLovName(cd.getCompanyPoid(), "COMPANY"));
                }
                d.setRemarks(cd.getRemarks());
                return d;
            }).toList());
        }
        return dto;
    }

    // Tree functionality methods
    @Override
    public List<GlMasterTreeNodeDto> getGlMasterTree(String documentId, String actionRequested, GlMasterTreeRequest request) {
        try {
            log.info("Fetching GL Master tree for documentId: {}, actionRequested: {}, includeDeleted: {}, groupPoid: {}, companyPoid: {}, userPoid: {}",
                    documentId, actionRequested, request.getIncludeDeleted(),
                    request.getGroupPoid(), request.getCompanyPoid(), request.getUserPoid());

            // Call the stored procedure
            List<Map<String, Object>> rawData = glMasterTreeViewRepository.callGlMasterTreeViewProcedure(documentId, actionRequested, request);

            // Filter records based on includeDeleted flag
            List<Map<String, Object>> filteredData = filterRecords(rawData, request.getIncludeDeleted());

            // Build tree structure
            List<GlMasterTreeNodeDto> treeItems = buildTreeStructure(filteredData);

            log.info("Successfully built GL Master tree with {} root items", treeItems.size());
            return treeItems;

        } catch (Exception e) {
            log.error("Error fetching GL Master tree", e);
            throw new RuntimeException("Error fetching GL Master tree: " + e.getMessage(), e);
        }
    }


    /**
     * Filter records based on includeDeleted flag
     */
    private List<Map<String, Object>> filterRecords(List<Map<String, Object>> records, Boolean includeDeleted) {
        if (includeDeleted == null || !includeDeleted) {
            // Filter out deleted records (only include active records)
            return records.stream()
                    .filter(record -> {
                        String deleted = (String) record.get("DELETED");
                        return !"Y".equals(deleted);
                    })
                    .collect(Collectors.toList());
        }
        // Return all records if includeDeleted is true
        return records;
    }

    /**
     * Build tree structure from flat list of records
     */
    private List<GlMasterTreeNodeDto> buildTreeStructure(List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        // Convert records to tree items
        List<GlMasterTreeNodeDto> allItems = records.stream()
                .map(this::convertToTreeItem)
                .collect(Collectors.toList());

        // Create a map for quick lookup
        Map<Long, GlMasterTreeNodeDto> itemMap = allItems.stream()
                .collect(Collectors.toMap(
                        GlMasterTreeNodeDto::getGlPoid,
                        item -> item,
                        (existing, replacement) -> existing
                ));

        // Build parent-child relationships
        List<GlMasterTreeNodeDto> rootItems = new ArrayList<>();
        for (GlMasterTreeNodeDto item : allItems) {
            Long parentPoid = item.getParentPoid();
            if (parentPoid == null) {
                // This is a root item
                rootItems.add(item);
            } else {
                // This is a child item, find its parent
                GlMasterTreeNodeDto parent = itemMap.get(parentPoid);
                if (parent != null) {
                    parent.getChildren().add(item);
                }
            }
        }

        // Sort items at each level
        sortTreeItems(rootItems);

        // Recalculate levels properly based on actual tree structure
        recalculateLevels(rootItems, 0);

        return rootItems;
    }

    /**
     * Convert database record to GlMasterTreeItem
     */
    private GlMasterTreeNodeDto convertToTreeItem(Map<String, Object> record) {
        // Extract GL code and description from the combined description field
        String description = (String) record.get("DESCRIPTION");
        String glCode = "";
        String glDescription = description;

        if (description != null && description.contains("(") && description.contains(")")) {
            int openParen = description.lastIndexOf("(");
            int closeParen = description.lastIndexOf(")");
            if (openParen > 0 && closeParen > openParen) {
                glCode = description.substring(openParen + 1, closeParen);
                glDescription = description.substring(0, openParen).trim();
            }
        }

        // Determine account type based on GL code or description
        String accountType = determineAccountType(glCode, glDescription);

        // Safely get level with logical null handling
        Integer level = getIntegerValue(record.get("LVL"));
        Long parentPoid = getLongValue(record.get("PARENT_POID"));

        if (level == null) {
            // If LEVEL is null, determine it logically based on parent relationship
            if (parentPoid == null) {
                level = 1; // No parent = root level
            } else {
                level = 2; // Has parent = child level (we'll recalculate properly in tree building)
            }
            log.warn("LEVEL field is null for POID {}, parentPoid: {}, setting to: {}",
                    getLongValue(record.get("POID")), parentPoid, level);
        }

        // Safely get POID with null check
        Long poid = getLongValue(record.get("POID"));
        if (poid == null) {
            poid = 0L; // Default to 0 if null
        }

        // Safely get string fields with null checks
        String glType = (String) record.get("GL_TYPE");
        if (glType == null) {
            glType = "UNKNOWN";
        }

        String itemType = (String) record.get("ITEM_TYPE");
        if (itemType == null) {
            itemType = "ITEM";
        }

        // Create tree node extending GLMasterResponseDto
        GlMasterTreeNodeDto node = new GlMasterTreeNodeDto();
        node.setGlPoid(poid);
        node.setGlCode(glCode);
        node.setDescription(glDescription);
        node.setType(glType);
        node.setAccountType(accountType);
        node.setParentPoid(parentPoid);
        node.setLevel(level - 1);
        node.setDeleted("Y".equals(record.get("DELETED")));
        node.setActive(!"N".equals(record.get("ACTIVE")));
        node.setId("row-" + poid);
        node.setIsExpanded(false);
        node.setIsRowGroup(true);
        node.setChildren(new ArrayList<>());
        return node;
    }

    /**
     * Determine account type based on GL code or description
     */
    private String determineAccountType(String glCode, String glDescription) {
        if (glCode != null && !glCode.isEmpty()) {
            // Determine based on GL code patterns
            if (glCode.startsWith("1")) {
                return "Asset";
            } else if (glCode.startsWith("2")) {
                return "Liability";
            } else if (glCode.startsWith("3")) {
                return "Equity";
            } else if (glCode.startsWith("4")) {
                return "Revenue";
            } else if (glCode.startsWith("5")) {
                return "Expense";
            }
        }

        // Fallback to description-based determination
        if (glDescription != null) {
            String desc = glDescription.toLowerCase();
            if (desc.contains("asset")) {
                return "Asset";
            } else if (desc.contains("liability")) {
                return "Liability";
            } else if (desc.contains("equity")) {
                return "Equity";
            } else if (desc.contains("revenue") || desc.contains("income")) {
                return "Revenue";
            } else if (desc.contains("expense") || desc.contains("cost")) {
                return "Expense";
            }
        }

        return "Other";
    }

    /**
     * Sort tree items recursively
     */
    private void sortTreeItems(List<GlMasterTreeNodeDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        // Sort by GL type priority, then by GL code, then by description
        items.sort((a, b) -> {
            // First, sort by GL type priority
            int typeComparison = Integer.compare(getGlTypePriority(a.getType()), getGlTypePriority(b.getType()));
            if (typeComparison != 0) {
                return typeComparison;
            }

            // Then by GL code
            int codeComparison = compareGlCodes(a.getGlCode(), b.getGlCode());
            if (codeComparison != 0) {
                return codeComparison;
            }

            // Finally by description
            return a.getDescription().compareToIgnoreCase(b.getDescription());
        });

        // Recursively sort children
        for (GlMasterTreeNodeDto item : items) {
            if (item.getChildren() != null && !item.getChildren().isEmpty()) {
                sortTreeItems(item.getChildren());
            }
        }
    }

    /**
     * Recalculate levels based on actual tree structure
     * This ensures levels are correct even if database LEVEL field was null or incorrect
     */
    private void recalculateLevels(List<GlMasterTreeNodeDto> items, int currentLevel) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (GlMasterTreeNodeDto item : items) {
            // Set the correct level based on tree position
            item.setLevel(currentLevel);

            // Recursively set levels for children
            if (item.getChildren() != null && !item.getChildren().isEmpty()) {
                recalculateLevels(item.getChildren(), currentLevel + 1);
            }
        }
    }

    /**
     * Get GL type priority for sorting
     */
    private int getGlTypePriority(String glType) {
        if (glType == null) {
            return 999;
        }
        switch (glType.toUpperCase()) {
            case "MAIN_GROUP":
                return 1;
            case "SUB_GROUP":
                return 2;
            case "LEDGER":
                return 3;
            default:
                return 999;
        }
    }

    /**
     * Compare GL codes for sorting
     */
    private int compareGlCodes(String code1, String code2) {
        if (code1 == null && code2 == null) {
            return 0;
        }
        if (code1 == null) {
            return 1;
        }
        if (code2 == null) {
            return -1;
        }
        return code1.compareTo(code2);
    }

    /**
     * Count total accounts recursively
     */
    private int countTotalAccounts(List<GlMasterTreeNodeDto> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        int count = items.size();
        for (GlMasterTreeNodeDto item : items) {
            count += countTotalAccounts(item.getChildren());
        }
        return count;
    }

    /**
     * Count accounts by type recursively
     */
    private Map<String, Integer> countAccountsByType(List<GlMasterTreeNodeDto> items) {
        Map<String, Integer> counts = new HashMap<>();
        countAccountsByTypeRecursive(items, counts);
        return counts;
    }

    /**
     * Recursive helper for counting accounts by type
     */
    private void countAccountsByTypeRecursive(List<GlMasterTreeNodeDto> items, Map<String, Integer> counts) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (GlMasterTreeNodeDto item : items) {
            String type = item.getType();
            counts.put(type, counts.getOrDefault(type, 0) + 1);
            countAccountsByTypeRecursive(item.getChildren(), counts);
        }
    }

    /**
     * Safely get integer value from object
     */
    private Integer getIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Safely get long value from object
     */
    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String releaseLock(DocReleaseLockRequestDto request) {
        return glMasterCustomService.releaseLock(request);
    }

    @Override
    public String acquireLock(DocReleaseLockRequestDto request) {
        return glMasterCustomService.acquireLock(request);
    }

    public Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, Pageable pageable) {

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "GL_POID",
                "GL_DESCRIPTION");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public List<GLMasterResponseDto> getGlMasterList(String documentId, String actionRequested, Long parentPoid) {
        try {
            log.info("Fetching GL Master list for documentId: {}, actionRequested: {}, parentPoid: {}",
                    documentId, actionRequested, parentPoid);

            // Get data directly from database using repository
            List<GLMasterEntity> entities;

            if (parentPoid == null) {
                // Get main groups (records with no parent)
                entities = glMasterRepo.findMainGroups(
                        false, // Always exclude deleted records
                        null   // No group filtering needed
                );
            } else {
                // Get direct children of the specified parent
                entities = glMasterRepo.findDirectChildren(
                        parentPoid,
                        false, // Always exclude deleted records
                        null   // No group filtering needed
                );
            }

            // Convert entities to list items
            List<GLMasterResponseDto> listItems = convertEntitiesToListItems(entities);

            log.info("Successfully retrieved GL Master list with {} items for parentPoid: {}", listItems.size(), parentPoid);
            return listItems;

        } catch (Exception e) {
            log.error("Error fetching GL Master list", e);
            throw new RuntimeException("Error fetching GL Master list: " + e.getMessage(), e);
        }
    }

    /**
     * Convert entities to list items
     */
    private List<GLMasterResponseDto> convertEntitiesToListItems(List<GLMasterEntity> entities) {
        List<GLMasterResponseDto> listItems = new ArrayList<>();

        for (GLMasterEntity entity : entities) {
            GLMasterResponseDto listItem = convertEntityToListItem(entity);
            if (listItem != null) {
                listItems.add(listItem);
            }
        }

        // Sort the list items by GL code
        sortListItems(listItems);
        return listItems;
    }

    /**
     * Convert a single entity to list item
     */
    private GLMasterResponseDto convertEntityToListItem(GLMasterEntity entity) {
        try {
            if (entity == null || entity.getGlPoid() == null) {
                return null;
            }

            // Determine level based on parent relationship
            Integer level = (entity.getGroupGlPoid() == null) ? 0 : 1;

            GLMasterResponseDto dto = new GLMasterResponseDto();
            dto.setGlPoid(entity.getGlPoid());
            dto.setGlCode(entity.getGlCode());
            dto.setDescription(entity.getDescription());
            dto.setType(entity.getType());
            dto.setAccountType(entity.getAccountType());
            dto.setParentPoid(entity.getGroupGlPoid());
            dto.setLevel(level);
            dto.setActive("Y".equals(entity.getActiveFlag()));
            dto.setDeleted("Y".equals(entity.getDeletedFlag()));
            return dto;

        } catch (Exception e) {
            log.warn("Error converting entity to list item: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Sort list items by GL code
     */
    private void sortListItems(List<GLMasterResponseDto> listItems) {
        listItems.sort((a, b) -> {
            // Sort by GL code (treat as numbers if possible, otherwise as strings)
            String codeA = a.getGlCode() != null ? a.getGlCode() : "";
            String codeB = b.getGlCode() != null ? b.getGlCode() : "";

            try {
                // Try to sort as numbers
                Long numA = Long.parseLong(codeA);
                Long numB = Long.parseLong(codeB);
                return numA.compareTo(numB);
            } catch (NumberFormatException e) {
                // Fall back to string sorting
                return codeA.compareTo(codeB);
            }
        });
    }

    public void updateGlPaymentDetails(List<PaymentDetailsDto> paymentDetails, Long glPoid) {
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        List<GLPaymentDetailsEntity> toSave = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();

        // Group operations by action
        for (PaymentDetailsDto charge : paymentDetails) {
            // Handle null, empty string, or whitespace as "noChanges"
            String actionTypeStr = charge.getActionType();
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                actionTypeStr = "noChanges";
            }
            String actionType = actionTypeStr.toUpperCase();
            switch (actionType) {
                case "ISCREATED":
                    toSave.add(GLPaymentDetailsEntity.builder()
                            .id(charge.getDetRowId())
                            .glPoid(glPoid)
                            .type(charge.getType())
                            .beneficiaryName(charge.getBeneficiaryName())
                                    .address(charge.getAddress())
                                    .bank(charge.getBank())
                                    .bankAddress(charge.getBankAddress())
                                    .beneficiaryCountry(charge.getBeneficiaryCountry())
                                    .swiftCode(charge.getSwiftCode())
                                    .accountNumber(charge.getAccountNumber())
                                    .iban(charge.getIban())
                                    .intermediaryBank(charge.getIntermediaryBank())
                                    .intermediaryAcct(charge.getIntermediaryAcct())
                                    .intermediaryOth(charge.getIntermediaryOth())
                                    .specialInstruction(charge.getSpecialInstruction())
                                    .intermediaryCountryPoid(charge.getIntermediaryCountryPoid())
                            .active(convertToActiveFlag(charge.getActive()))
                            .createdBy(currentUser)
                            .createdDate(now)
                            .lastModifiedBy(currentUser)
                            .lastModifiedDate(now)
                            .build());
                    break;

                case "ISUPDATED":
                    GLPaymentDetailsEntity existingCharge = payDtlRepo
                            .findByGlPoidAndId(glPoid, charge.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("Payment not found", "detRowId", charge.getDetRowId()));
                    existingCharge.setType(charge.getType());
                    existingCharge.setBeneficiaryName(charge.getBeneficiaryName());
                    existingCharge.setAddress(charge.getAddress());
                    existingCharge.setBank(charge.getBank());
                    existingCharge.setBankAddress(charge.getBankAddress());
                    existingCharge.setBeneficiaryCountry(charge.getBeneficiaryCountry());
                    existingCharge.setSwiftCode(charge.getSwiftCode());
                    existingCharge.setAccountNumber(charge.getAccountNumber());
                    existingCharge.setIban(charge.getIban());
                    existingCharge.setIntermediaryBank(charge.getIntermediaryBank());
                    existingCharge.setIntermediaryAcct(charge.getIntermediaryAcct());
                    existingCharge.setIntermediaryOth(charge.getIntermediaryOth());
                    existingCharge.setSpecialInstruction(charge.getSpecialInstruction());
                    existingCharge.setIntermediaryCountryPoid(charge.getIntermediaryCountryPoid());
                    existingCharge.setActive(convertToActiveFlag(charge.getActive()));
                    existingCharge.setLastModifiedBy(currentUser);
                    existingCharge.setLastModifiedDate(now);
                    toSave.add(existingCharge);
                    break;

                case "ISDELETED":
                    toDelete.add(charge.getDetRowId());
                    break;

                case "NOCHANGES":
                    // Do nothing - keep existing record as-is
                    // This record will remain in the database unchanged
                    break;

                default:
                    // If actionType is null or unrecognized, treat as NOCHANGES
                    // This prevents accidental deletion of records
                    log.warn("Unknown actionType '{}' for payment detail (detRowId: {}), treating as NOCHANGES", 
                            charge.getActionType(), charge.getDetRowId());
                    break;
            }
        }

        // Batch operations
        // Only delete records explicitly marked as "ISDELETED"
        // Records with "NOCHANGES" or unknown actionType are skipped (preserved in database)
        if (!toSave.isEmpty()) {
            payDtlRepo.saveAll(toSave);
        }
        if (!toDelete.isEmpty()) {
            payDtlRepo.deleteByGlPoidAndIdIn(glPoid, toDelete);
        }
    }

    public void updateGlCompanyDetails(List<CompanyDetailsDto> companyDetails, Long glPoid) {
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        List<GLMasterCompanyDtlEntity> toSave = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();

        // Group operations by action
        for (CompanyDetailsDto charge : companyDetails) {
            // Handle null, empty string, or whitespace as "noChanges"
            String actionTypeStr = charge.getActionType();
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                actionTypeStr = "noChanges";
            }
            String actionType = actionTypeStr.toUpperCase();
            switch (actionType) {
                case "ISCREATED":
                    toSave.add(GLMasterCompanyDtlEntity.builder()
                            .id(charge.getDetRowId())
                            .glPoid(glPoid)
                            .companyPoid(charge.getCompanyPoid())
                            .remarks(charge.getRemarks())
                            .createdBy(currentUser)
                            .createdDate(now)
                            .lastModifiedBy(currentUser)
                            .lastModifiedDate(now)
                            .build());
                    break;

                case "ISUPDATED":
                    GLMasterCompanyDtlEntity existingCharge = companyDtlRepo
                            .findByGlPoidAndId(glPoid, charge.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("Company not found", "detRowId", charge.getDetRowId()));
                    existingCharge.setCompanyPoid(charge.getCompanyPoid());
                    existingCharge.setRemarks(charge.getRemarks());
                    existingCharge.setLastModifiedBy(currentUser);
                    existingCharge.setLastModifiedDate(now);
                    toSave.add(existingCharge);
                    break;

                case "ISDELETED":
                    toDelete.add(charge.getDetRowId());
                    break;

                case "NOCHANGES":
                    // Do nothing - keep existing record as-is
                    // This record will remain in the database unchanged
                    break;

                default:
                    // If actionType is null or unrecognized, treat as NOCHANGES
                    // This prevents accidental deletion of records
                    log.warn("Unknown actionType '{}' for company detail (detRowId: {}), treating as NOCHANGES", 
                            charge.getActionType(), charge.getDetRowId());
                    break;
            }
        }

        // Batch operations
        if (!toSave.isEmpty()) {
            companyDtlRepo.saveAll(toSave);
        }
        if (!toDelete.isEmpty()) {
            companyDtlRepo.deleteByGlPoidAndIdIn(glPoid, toDelete);
        }
    }
}
