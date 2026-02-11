package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LoggingService;
import com.asg.finance.client.RoleServiceClient;
import com.asg.finance.dto.masters.*;
import com.asg.finance.entity.GlobalLogDetails;
import com.asg.finance.entity.GlobalLogSummary;
import com.asg.finance.entity.master.*;
import com.asg.finance.repository.GlobalLogDetailsRepository;
import com.asg.finance.repository.GlobalLogSummaryRepository;
import com.asg.finance.repository.master.InsuranceMasterRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.InsuranceMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InsuranceMasterServiceImpl implements InsuranceMasterService {

    private final InsuranceMasterRepository insuranceMasterRepository;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final RoleServiceClient roleServiceClient;
    private final LoggingService loggingService;
    private final GlobalLogSummaryRepository globalLogSummaryRepository;
    private final GlobalLogDetailsRepository globalLogDetailsRepository;

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    @Override
    public InsuranceMasterResponseDto getInsuranceMasterById(Long insuranceId) {
        return mapToResponseDto(insuranceMasterRepository.findById(insuranceId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Master", "ID", insuranceId)));
    }

    @Override
    public Map<String, Object> listInsuranceMasters(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveDateFilters(filters,"TRANSACTION_POID",startDate, endDate);

        RawSearchResult raw = documentService.search(documentId, filterList, operator, pageable, isDeleted,
                "POLICY_NO",
                "TRANSACTION_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public void softDeleteInsuranceMaster(Long insuranceId, DeleteReasonDto deleteReasonDto) {
        InsuranceMaster insuranceMaster = insuranceMasterRepository.findById(insuranceId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Master", "ID", insuranceId));

        if (insuranceMasterRepository.hasPjReference(insuranceId)) {
            throw new ValidationException("Cannot delete — Insurance linked with Purchase Journal Reference");
        }

        documentDeleteService.deleteDocument(
                insuranceId,
                "GLOBAL_INSURANCE_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                null
        );
    }

    @Override
    @Transactional
    public InsuranceMasterResponseDto createInsuranceMaster(InsuranceMasterRequestDto request) {
        try {
            // Validate policy number format
            if (!request.getPolicyNo().matches("^POL[0-9]{8}$")) {
                throw new ValidationException("Policy number must be in format POL12345678");
            }

            // Validate insurance type against LOV
            List<String> validInsuranceTypes = Arrays.asList(
                "VEHICLE_INSURANCE", "MEDICAL_INSURANCE", "PROPERTY_INSURANCE", 
                "TRAVEL_INSURANCE", "PROJECTS_INSURANCE", "RO_RO_INSURANCE", 
                "EQUIPMENT_INSURANCE", "CUSTOMS_CLEARANCE_INSURANCE", "IT_INSURANCE", 
                "LIFE_INSURANCE", "CYBER_SECURITY_INSURANCE", "PROFESSIONAL_INDEMNITY_INSURANCE", 
                "FORWARDING_LOGISTICS_INSURANCE"
            );
            if (!validInsuranceTypes.contains(request.getInsuranceType())) {
                throw new ValidationException("Invalid insurance type. Must be one of: " + String.join(", ", validInsuranceTypes));
            }

            // Validate insurance category
            if (!request.getCategory().matches("^(GROUP|COMPANY|INDIVIDUAL)$")) {
                throw new ValidationException("Insurance category must be GROUP, COMPANY, or INDIVIDUAL");
            }

            // Validate unique policy number per company
            if (insuranceMasterRepository.existsByPolicyNoAndCompanyPoid(request.getPolicyNo(), 1L)) {
                throw new ValidationException("Policy Number must be unique per company");
            }

            // Validate header dates
            if (request.getFromDate() == null || request.getExpiryDate() == null) {
                throw new ValidationException("From Date and Expiry Date cannot be null");
            }

            if (request.getFromDate().isAfter(request.getExpiryDate())) {
                throw new ValidationException("From Date cannot be greater than Expiry Date");
            }

            // Validate PIC dates (if present)
            if (request.getPicDetails() != null) {
                for (InsurancePicDetailRequestDto pic : request.getPicDetails()) {

                    // Validate PIC from ≤ to
                    if (pic.getFromDate().isAfter(pic.getToDate())) {
                        throw new ValidationException("PIC From Date cannot be greater than PIC To Date");
                    }

                    // Validate PIC within insurance period
                    if (pic.getFromDate().isBefore(request.getFromDate()) ||
                            pic.getToDate().isAfter(request.getExpiryDate())) {

                        throw new ValidationException("PIC dates must be within the insurance policy period");
                    }
                }
            }

            InsuranceMaster insuranceMaster = InsuranceMaster.builder()
                    .groupPoid(1L)
                    .companyPoid(1L)
                    .insuranceType(request.getInsuranceType())
                    .insuranceCategory(request.getCategory())
                    .policyNo(request.getPolicyNo())
                    .insuranceProvider(request.getInsuranceProvider())
                    .fromDate(request.getFromDate())
                    .expiryDate(request.getExpiryDate())
                    .currencyPoid(request.getCurrency())
                    .exchangeRate(request.getRate())
                    .insuranceAmount(request.getInsuranceAmount())
                    .premiumAmount(request.getPremiumAmount())
                    .paymentFrequency(request.getPaymentFrequency())
                    .oneTime(request.getOneTime() != null ? request.getOneTime() : "N")
                    .description(request.getDescription())
                    .faPoid(request.getFaPoid())
                    .deleted("N")
                    .createdBy(getCurrentUser())
                    .createdDate(LocalDateTime.now())
                    .lastModifiedBy(getCurrentUser())
                    .lastModifiedDate(LocalDateTime.now())
                    .build();

            insuranceMaster.setPjRefPoid(null);
            InsuranceMaster saved = insuranceMasterRepository.save(insuranceMaster);
            buildAndSetChildDetails(request, saved);
            InsuranceMaster finalSaved = insuranceMasterRepository.save(saved);

            String docId = UserContext.getDocumentId();
            String docKeyPoid = finalSaved.getTransactionPoid().toString();
            Timestamp now = new Timestamp(System.currentTimeMillis());

            String createdMessage = String.format("Created - - DOC:%s KEY:%s", docId, docKeyPoid);
            GlobalLogSummary headerLog = createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid, createdMessage, now);
            globalLogSummaryRepository.save(headerLog);


            return mapToResponseDto(finalSaved);
        } catch (ValidationException e) {
            throw e; // Re-throw validation exceptions without wrapping
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Insurance Master: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public InsuranceMasterResponseDto updateInsuranceMaster(Long insuranceId, InsuranceMasterRequestDto request) {
        InsuranceMaster existing = insuranceMasterRepository.findById(insuranceId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Master", "ID", insuranceId));

        // Create copy of old entity for logging
        InsuranceMaster oldEntity = new InsuranceMaster();
        BeanUtils.copyProperties(existing, oldEntity);

        List<InsuranceEmployeeDetail> oldEmployeeDetails = existing.getEmployeeDetails() != null ? new ArrayList<>(existing.getEmployeeDetails()) : new ArrayList<>();
        List<InsurancePropertyDetail> oldPropertyDetails = existing.getPropertyDetails() != null ? new ArrayList<>(existing.getPropertyDetails()) : new ArrayList<>();
        List<InsurancePicDetail> oldPicDetails = existing.getPicDetails() != null ? new ArrayList<>(existing.getPicDetails()) : new ArrayList<>();
        List<InsuranceVehicleDetail> oldVehicleDetails = existing.getVehicleDetails() != null ? new ArrayList<>(existing.getVehicleDetails()) : new ArrayList<>();

        List<InsuranceEmployeeDetail> oldEmployeeSnapshots = snapshotEmployeeDetails(oldEmployeeDetails);
        List<InsurancePropertyDetail> oldPropertySnapshots = snapshotPropertyDetails(oldPropertyDetails);
        List<InsurancePicDetail> oldPicSnapshots = snapshotPicDetails(oldPicDetails);
        List<InsuranceVehicleDetail> oldVehicleSnapshots = snapshotVehicleDetails(oldVehicleDetails);

        // Validate unique policy number per company (excluding current record)

        // Validate policy number format
        if (!request.getPolicyNo().matches("^POL[0-9]{8}$")) {
            throw new ValidationException("Policy number must be in format POL12345678");
        }

        // Validate insurance type against LOV
        List<String> validInsuranceTypes = Arrays.asList(
            "VEHICLE_INSURANCE", "MEDICAL_INSURANCE", "PROPERTY_INSURANCE", 
            "TRAVEL_INSURANCE", "PROJECTS_INSURANCE", "RO_RO_INSURANCE", 
            "EQUIPMENT_INSURANCE", "CUSTOMS_CLEARANCE_INSURANCE", "IT_INSURANCE", 
            "LIFE_INSURANCE", "CYBER_SECURITY_INSURANCE", "PROFESSIONAL_INDEMNITY_INSURANCE", 
            "FORWARDING_LOGISTICS_INSURANCE"
        );
        if (!validInsuranceTypes.contains(request.getInsuranceType())) {
            throw new ValidationException("Invalid insurance type. Must be one of: " + String.join(", ", validInsuranceTypes));
        }

        // Validate insurance category
        if (!request.getCategory().matches("^(GROUP|COMPANY|INDIVIDUAL)$")) {
            throw new ValidationException("Insurance category must be GROUP, COMPANY, or INDIVIDUAL");
        }

        // Validate unique policy number (excluding current record)
        if (insuranceMasterRepository.existsByPolicyNoAndDeletedAndTransactionPoidNot(
                request.getPolicyNo(), "N", insuranceId)) {
            throw new ValidationException("Policy Number must be unique");

        }

        // Validate header dates
        if (request.getFromDate() == null || request.getExpiryDate() == null) {
            throw new ValidationException("From Date and Expiry Date cannot be null");
        }

        if (request.getFromDate().isAfter(request.getExpiryDate())) {
            throw new ValidationException("From Date cannot be greater than Expiry Date");
        }

        // Validate PIC dates (if present)
        if (request.getPicDetails() != null) {
            for (InsurancePicDetailRequestDto pic : request.getPicDetails()) {

                // Validate PIC from ≤ to
                if (pic.getFromDate().isAfter(pic.getToDate())) {
                    throw new ValidationException("PIC From Date cannot be greater than PIC To Date");
                }

                // Validate PIC within insurance period
                if (pic.getFromDate().isBefore(request.getFromDate()) ||
                        pic.getToDate().isAfter(request.getExpiryDate())) {

                    throw new ValidationException("PIC dates must be within the insurance policy period");
                }
            }
        }



        // Update fields (preserve pjRefPoid if it exists)
        existing.setInsuranceType(request.getInsuranceType());
        existing.setInsuranceCategory(request.getCategory());
        existing.setPolicyNo(request.getPolicyNo());
        existing.setInsuranceProvider(request.getInsuranceProvider());
        existing.setFromDate(request.getFromDate());
        existing.setExpiryDate(request.getExpiryDate());
        existing.setCurrencyPoid(request.getCurrency());
        existing.setExchangeRate(request.getRate());
        existing.setInsuranceAmount(request.getInsuranceAmount());
        existing.setPremiumAmount(request.getPremiumAmount());
        existing.setPaymentFrequency(request.getPaymentFrequency());
        existing.setOneTime(request.getOneTime() != null ? request.getOneTime() : "N");
        existing.setDescription(request.getDescription());
        existing.setFaPoid(request.getFaPoid());
        existing.setLastModifiedBy(getCurrentUser());
        existing.setLastModifiedDate(LocalDateTime.now());

        // Clear and rebuild child details (don't replace collections, modify them)
        existing.getEmployeeDetails().clear();
        existing.getPropertyDetails().clear();
        existing.getPicDetails().clear();
        if (existing.getVehicleDetails() != null) {
            existing.getVehicleDetails().clear();
        }
        
        // Add new child details to existing collections
        if (request.getEmployeeDetails() != null) {
            existing.getEmployeeDetails().addAll(buildEmployeeDetails(request.getEmployeeDetails(), existing));
        }
        if (request.getPropertyDetails() != null) {
            existing.getPropertyDetails().addAll(buildPropertyDetails(request.getPropertyDetails(), existing));
        }
        if (request.getPicDetails() != null) {
            existing.getPicDetails().addAll(buildPicDetails(request.getPicDetails(), existing));
        }
        if (request.getVehicleNumber() != null && !request.getVehicleNumber().isBlank()) {
            if (existing.getVehicleDetails() == null) {
                existing.setVehicleDetails(new ArrayList<>());
            }
            existing.getVehicleDetails().addAll(buildVehicleDetails(request.getVehicleNumber(), request.getInsuranceAmount(), existing));
        }
        if (existing.getPjRefPoid() != null) {
            existing.setPjRefPoid(existing.getPjRefPoid());
        } else {
            existing.setPjRefPoid(null); // don't allow overwriting or random assignment
        }

        InsuranceMaster updated = insuranceMasterRepository.save(existing);

        String docId = UserContext.getDocumentId();
        String docKeyPoid = String.valueOf(insuranceId);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<LogRequestDto<InsuranceMaster>> headerLogRequests = new ArrayList<>();
        List<GlobalLogSummary> subTableSummaryLogs = new ArrayList<>();

        String modifiedMessage = String.format("Modified - - DOC:%s KEY:%s", docId, docKeyPoid);
        GlobalLogSummary headerUpdateLog = createSummaryLogEntry(LogDetailsEnum.MODIFIED, docId, docKeyPoid, modifiedMessage, now);
        globalLogSummaryRepository.save(headerUpdateLog);

        InsuranceMaster oldHeaderOnly = copyHeaderOnlyForLog(oldEntity);
        InsuranceMaster updatedHeaderOnly = copyHeaderOnlyForLog(updated);
        String headerLogDetail = String.format("KeyId = TRANSACTION_POID:%s", docKeyPoid);
        headerLogRequests.add(new LogRequestDto<>(oldHeaderOnly, updatedHeaderOnly, InsuranceMaster.class, docId, docKeyPoid, headerLogDetail));


        Set<Long> newEmpDetRowIds = updated.getEmployeeDetails() != null
                ? updated.getEmployeeDetails().stream().map(InsuranceEmployeeDetail::getDetRowId).collect(Collectors.toSet())
                : Collections.emptySet();
        for (InsuranceEmployeeDetail oldEmp : oldEmployeeDetails) {
            if (!newEmpDetRowIds.contains(oldEmp.getDetRowId())) {
                String deletedRecordString = String.format("detRowId:%s, transactionPoid:%s, employeePoid:%s, amount:%s, remarks:%s",
                        oldEmp.getDetRowId(), oldEmp.getTransactionPoid(), oldEmp.getEmployeePoid(), oldEmp.getAmount(), oldEmp.getRemarks());
                String deleteSummaryMessage = String.format("Row Deleted %s", deletedRecordString);
                subTableSummaryLogs.add(createSummaryLogEntry(LogDetailsEnum.DELETED, docId, docKeyPoid, deleteSummaryMessage, now));
            }
        }
        Set<Long> oldEmpDetRowIds = oldEmployeeDetails.stream().map(InsuranceEmployeeDetail::getDetRowId).collect(Collectors.toSet());
        if (updated.getEmployeeDetails() != null) {
            for (InsuranceEmployeeDetail newEmp : updated.getEmployeeDetails()) {
                if (!oldEmpDetRowIds.contains(newEmp.getDetRowId())) {
                    subTableSummaryLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid,
                            String.format("Row Created on Insurance Master Employee Detail with DetRowId: %s", newEmp.getDetRowId()), now));
                } else {
                    InsuranceEmployeeDetail oldSnapshot = findOldEmployeeByDetRowId(oldEmployeeSnapshots, newEmp.getDetRowId());
                    if (oldSnapshot != null) addDetailLogAndModified(oldSnapshot, newEmp, docId, docKeyPoid, now, subTableSummaryLogs);
                }
            }
        }

        Set<Long> newPropDetRowIds = updated.getPropertyDetails() != null
                ? updated.getPropertyDetails().stream().map(InsurancePropertyDetail::getDetRowId).collect(Collectors.toSet())
                : Collections.emptySet();
        for (InsurancePropertyDetail oldProp : oldPropertyDetails) {
            if (!newPropDetRowIds.contains(oldProp.getDetRowId())) {
                String deletedRecordString = String.format("detRowId:%s, transactionPoid:%s, propertyPoid:%s, amount:%s, remarks:%s",
                        oldProp.getDetRowId(), oldProp.getTransactionPoid(), oldProp.getPropertyPoid(), oldProp.getAmount(), oldProp.getRemarks());
                String deleteSummaryMessage = String.format("Row Deleted %s", deletedRecordString);
                subTableSummaryLogs.add(createSummaryLogEntry(LogDetailsEnum.DELETED, docId, docKeyPoid, deleteSummaryMessage, now));
            }
        }
        Set<Long> oldPropDetRowIds = oldPropertyDetails.stream().map(InsurancePropertyDetail::getDetRowId).collect(Collectors.toSet());
        if (updated.getPropertyDetails() != null) {
            for (InsurancePropertyDetail newProp : updated.getPropertyDetails()) {
                if (!oldPropDetRowIds.contains(newProp.getDetRowId())) {
                    subTableSummaryLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid,
                            String.format("Row Created on Insurance Master Property Detail with DetRowId: %s", newProp.getDetRowId()), now));
                } else {
                    InsurancePropertyDetail oldSnapshot = findOldPropertyByDetRowId(oldPropertySnapshots, newProp.getDetRowId());
                    if (oldSnapshot != null) addDetailLogAndModified(oldSnapshot, newProp, docId, docKeyPoid, now, subTableSummaryLogs);
                }
            }
        }

        Set<Long> newPicDetRowIds = updated.getPicDetails() != null
                ? updated.getPicDetails().stream().map(InsurancePicDetail::getDetRowId).collect(Collectors.toSet())
                : Collections.emptySet();
        for (InsurancePicDetail oldPic : oldPicDetails) {
            if (!newPicDetRowIds.contains(oldPic.getDetRowId())) {
                String deletedRecordString = String.format("detRowId:%s, transactionPoid:%s, rolePoid:%s, contactType:%s, picPerson:%s, fromDate:%s, toDate:%s",
                        oldPic.getDetRowId(), oldPic.getTransactionPoid(), oldPic.getRolePoid(), oldPic.getContactType(),
                        oldPic.getPicPerson(), oldPic.getFromDate(), oldPic.getToDate());
                String deleteSummaryMessage = String.format("Row Deleted %s", deletedRecordString);
                subTableSummaryLogs.add(createSummaryLogEntry(LogDetailsEnum.DELETED, docId, docKeyPoid, deleteSummaryMessage, now));
            }
        }
        Set<Long> oldPicDetRowIds = oldPicDetails.stream().map(InsurancePicDetail::getDetRowId).collect(Collectors.toSet());
        if (updated.getPicDetails() != null) {
            for (InsurancePicDetail newPic : updated.getPicDetails()) {
                if (!oldPicDetRowIds.contains(newPic.getDetRowId())) {
                    subTableSummaryLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid,
                            String.format("Row Created on Insurance Master PIC Detail with DetRowId: %s", newPic.getDetRowId()), now));
                } else {
                    InsurancePicDetail oldSnapshot = findOldPicByDetRowId(oldPicSnapshots, newPic.getDetRowId());
                    if (oldSnapshot != null) addDetailLogAndModified(oldSnapshot, newPic, docId, docKeyPoid, now, subTableSummaryLogs);
                }
            }
        }

        Set<Long> newVehDetRowIds = updated.getVehicleDetails() != null
                ? updated.getVehicleDetails().stream().map(InsuranceVehicleDetail::getDetRowId).collect(Collectors.toSet())
                : Collections.emptySet();
        for (InsuranceVehicleDetail oldVeh : oldVehicleDetails) {
            if (!newVehDetRowIds.contains(oldVeh.getDetRowId())) {
                String deletedRecordString = String.format("detRowId:%s, transactionPoid:%s, amount:%s, remarks:%s",
                        oldVeh.getDetRowId(), oldVeh.getTransactionPoid(), oldVeh.getAmount(), oldVeh.getRemarks());
                String deleteSummaryMessage = String.format("Row Deleted %s", deletedRecordString);
                subTableSummaryLogs.add(createSummaryLogEntry(LogDetailsEnum.DELETED, docId, docKeyPoid, deleteSummaryMessage, now));
            }
        }
        Set<Long> oldVehDetRowIds = oldVehicleDetails.stream().map(InsuranceVehicleDetail::getDetRowId).collect(Collectors.toSet());
        if (updated.getVehicleDetails() != null) {
            for (InsuranceVehicleDetail newVeh : updated.getVehicleDetails()) {
                if (!oldVehDetRowIds.contains(newVeh.getDetRowId())) {
                    subTableSummaryLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid,
                            String.format("Row Created on Insurance Master Vehicle Detail with DetRowId: %s", newVeh.getDetRowId()), now));
                } else {
                    InsuranceVehicleDetail oldSnapshot = findOldVehicleByDetRowId(oldVehicleSnapshots, newVeh.getDetRowId());
                    if (oldSnapshot != null) addDetailLogAndModified(oldSnapshot, newVeh, docId, docKeyPoid, now, subTableSummaryLogs);
                }
            }
        }

        if (!subTableSummaryLogs.isEmpty()) {
            globalLogSummaryRepository.saveAll(subTableSummaryLogs);
        }
        if (!headerLogRequests.isEmpty()) {
            loggingService.createLogBatch(headerLogRequests);
        }
        return mapToResponseDto(updated);
    }

    private void buildAndSetChildDetails(InsuranceMasterRequestDto request, InsuranceMaster savedParent) {
        // Always process lists to handle both additions and deletions
        if (request.getEmployeeDetails() != null) {
            savedParent.setEmployeeDetails(buildEmployeeDetails(request.getEmployeeDetails(), savedParent));
        }
        if (request.getPropertyDetails() != null) {
            savedParent.setPropertyDetails(buildPropertyDetails(request.getPropertyDetails(), savedParent));
        }
        if (request.getPicDetails() != null) {
            savedParent.setPicDetails(buildPicDetails(request.getPicDetails(), savedParent));
        }
        if (request.getVehicleNumber() != null && !request.getVehicleNumber().isBlank()) {
            savedParent.setVehicleDetails(buildVehicleDetails(request.getVehicleNumber(), request.getInsuranceAmount(), savedParent));
        }
    }



    private List<InsuranceEmployeeDetail> buildEmployeeDetails(List<InsuranceEmployeeDetailRequestDto> dtos, InsuranceMaster parent) {
        if (dtos == null) return new ArrayList<>();
        List<InsuranceEmployeeDetail> result = new ArrayList<>();
        
        // Get max existing detRowId
        long maxDetRowId = parent.getEmployeeDetails() != null ? 
            parent.getEmployeeDetails().stream()
                .mapToLong(InsuranceEmployeeDetail::getDetRowId)
                .max().orElse(0L) : 0L;
        long nextDetRowId = maxDetRowId + 1;
        
        for (InsuranceEmployeeDetailRequestDto dto : dtos) {
            String action = normalizeAction(dto.getActionType());
            
            if ("ISDELETED".equals(action)) continue;
            
            // Fix: Action type determines detRowId assignment
            Long detRowId;
            if ("ISCREATED".equals(action)) {
                detRowId = nextDetRowId++; // Always assign new ID for CREATE
            } else if ("ISUPDATED".equals(action)) {
                if (dto.getDetRowId() == null) {
                    throw new ValidationException("DetRowId is required for UPDATE action on Employee Detail");
                }
                detRowId = dto.getDetRowId(); // Must use provided ID for UPDATE
            } else {
                detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++; // Use provided or assign new
            }
            
            result.add(InsuranceEmployeeDetail.builder()
                    .transactionPoid(parent.getTransactionPoid())
                    .detRowId(detRowId)
                    .employeePoid(1L)
                    .amount(dto.getAmount())
                    .remarks(dto.getRemarks())
                    .createdBy(getCurrentUser())
                    .createdDate(LocalDateTime.now())
                    .lastModifiedBy(getCurrentUser())
                    .lastModifiedDate(LocalDateTime.now())
                    .build());
        }
        return result;
    }

    private List<InsurancePropertyDetail> buildPropertyDetails(List<InsurancePropertyDetailRequestDto> dtos, InsuranceMaster parent) {
        if (dtos == null) return new ArrayList<>();
        List<InsurancePropertyDetail> result = new ArrayList<>();
        
        // Get max existing detRowId
        long maxDetRowId = parent.getPropertyDetails() != null ? 
            parent.getPropertyDetails().stream()
                .mapToLong(InsurancePropertyDetail::getDetRowId)
                .max().orElse(0L) : 0L;
        long nextDetRowId = maxDetRowId + 1;
        
        for (InsurancePropertyDetailRequestDto dto : dtos) {
            String action = normalizeAction(dto.getActionType());
            
            if ("ISDELETED".equals(action)) continue;
            
            // Fix: Action type determines detRowId assignment
            Long detRowId;
            if ("ISCREATED".equals(action)) {
                detRowId = nextDetRowId++; // Always assign new ID for CREATE
            } else if ("ISUPDATED".equals(action)) {
                if (dto.getDetRowId() == null) {
                    throw new ValidationException("DetRowId is required for UPDATE action on Property Detail");
                }
                detRowId = dto.getDetRowId(); // Must use provided ID for UPDATE
            } else {
                detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++; // Use provided or assign new
            }
            
            result.add(InsurancePropertyDetail.builder()
                    .transactionPoid(parent.getTransactionPoid())
                    .detRowId(detRowId)
                    .propertyPoid(1L)
                    .amount(dto.getAmount())
                    .remarks(dto.getRemarks())
                    .createdBy(getCurrentUser())
                    .createdDate(LocalDateTime.now())
                    .lastModifiedBy(getCurrentUser())
                    .lastModifiedDate(LocalDateTime.now())
                    .build());
        }
        return result;
    }

    private List<InsurancePicDetail> buildPicDetails(List<InsurancePicDetailRequestDto> dtos, InsuranceMaster parent) {
        if (dtos == null) return new ArrayList<>();
        List<InsurancePicDetail> result = new ArrayList<>();
        
        // Get max existing detRowId
        long maxDetRowId = parent.getPicDetails() != null ? 
            parent.getPicDetails().stream()
                .mapToLong(InsurancePicDetail::getDetRowId)
                .max().orElse(0L) : 0L;
        long nextDetRowId = maxDetRowId + 1;
        
        for (InsurancePicDetailRequestDto dto : dtos) {
            String action = normalizeAction(dto.getActionType());
            
            if ("ISDELETED".equals(action)) continue;
            
            // Fix: Action type determines detRowId assignment
            Long detRowId;
            if ("ISCREATED".equals(action)) {
                detRowId = nextDetRowId++; // Always assign new ID for CREATE
            } else if ("ISUPDATED".equals(action)) {
                if (dto.getDetRowId() == null) {
                    throw new ValidationException("DetRowId is required for UPDATE action on PIC Detail");
                }
                detRowId = dto.getDetRowId(); // Must use provided ID for UPDATE
            } else {
                detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++; // Use provided or assign new
            }
            
            DetailsDto roleDetails = getRoleDetails(dto.getRolePoid());
            
            result.add(InsurancePicDetail.builder()
                    .transactionPoid(parent.getTransactionPoid())
                    .detRowId(detRowId)
                    .rolePoid(roleDetails.poid())
                    .contactType(dto.getContactType())
                    .picPerson(dto.getPicPerson())
                    .fromDate(dto.getFromDate())
                    .toDate(dto.getToDate())
                    .createdBy(getCurrentUser())
                    .createdDate(LocalDateTime.now())
                    .lastModifiedBy(getCurrentUser())
                    .lastModifiedDate(LocalDateTime.now())
                    .build());
        }
        return result;
    }

    private List<InsuranceVehicleDetail> buildVehicleDetails(String vehicleNumber, BigDecimal amount, InsuranceMaster parent) {
        List<InsuranceVehicleDetail> result = new ArrayList<>();
        result.add(InsuranceVehicleDetail.builder()
                .transactionPoid(parent.getTransactionPoid())
                .detRowId(1L)
                .fixedAssetPoid(null)
                .amount(amount)
                .remarks(vehicleNumber)
                .createdBy(getCurrentUser())
                .createdDate(LocalDateTime.now())
                .lastModifiedBy(getCurrentUser())
                .lastModifiedDate(LocalDateTime.now())
                .build());
        return result;
    }

    private InsuranceMasterResponseDto mapToResponseDto(InsuranceMaster entity) {
        return InsuranceMasterResponseDto.builder()
                .insurancePoid(entity.getTransactionPoid())
                .docRef(entity.getDocRef())
                .groupPoid(entity.getGroupPoid())
                .companyPoid(entity.getCompanyPoid())
                .insuranceType(entity.getInsuranceType())
                .insuranceCategory(entity.getInsuranceCategory())
                .policyNo(entity.getPolicyNo())
                .insuranceProvider(entity.getInsuranceProvider())
                .fromDate(entity.getFromDate())
                .expiryDate(entity.getExpiryDate())
                .status(calculateStatus(entity))
                .currency(entity.getCurrencyPoid())
                .rate(entity.getExchangeRate())
                .insuranceAmount(entity.getInsuranceAmount())
                .premiumAmount(entity.getPremiumAmount())
                .paymentFrequency(entity.getPaymentFrequency())
                .oneTime(entity.getOneTime())
                .description(entity.getDescription())
                .faPoid(entity.getFaPoid())
                .pjRefPoid(entity.getPjRefPoid())
                .deleted(entity.getDeleted())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .employeeDetails(mapEmployeeDetails(entity.getEmployeeDetails()))
                .propertyDetails(mapPropertyDetails(entity.getPropertyDetails()))
                .picDetails(mapPicDetails(entity.getPicDetails()))
                .renewalLogs(mapRenewalLogs(entity.getRenewalLogs()))
                .build();
    }

    private String calculateStatus(InsuranceMaster entity) {
        if (entity.getExpiryDate() == null) return "Active";

        LocalDate today = LocalDate.now();
        if (entity.getRenewalLogs() != null && !entity.getRenewalLogs().isEmpty()) {
            return "Renewed";
        }
        if (entity.getExpiryDate().isBefore(today)) {
            return "Expired";
        }
        return "Active";
    }

    private List<InsuranceEmployeeDetailResponseDto> mapEmployeeDetails(List<InsuranceEmployeeDetail> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(e -> InsuranceEmployeeDetailResponseDto.builder()
                        .employeeDetailPoid(e.getDetRowId())
                        .amount(e.getAmount())
                        .remarks(e.getRemarks())
                        .build())
                .toList();
    }

    private List<InsurancePropertyDetailResponseDto> mapPropertyDetails(List<InsurancePropertyDetail> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(e -> InsurancePropertyDetailResponseDto.builder()
                        .propertyDetailPoid(e.getDetRowId())
                       // .sn(e.getDetRowId().intValue())
                        .amount(e.getAmount())
                        .remarks(e.getRemarks())
                        .build())
                .toList();
    }



    private List<InsurancePicDetailResponseDto> mapPicDetails(List<InsurancePicDetail> entities) {
        if (entities == null) return new ArrayList<>();

        return entities.stream()
                .map(e -> InsurancePicDetailResponseDto.builder()
                        .rolePoid(e.getRolePoid())
                        .contactType(e.getContactType())
                        .picPerson(e.getPicPerson())
                        .fromDate(e.getFromDate())
                        .toDate(e.getToDate())
                        .build())
                .toList();
    }

    private List<InsuranceRenewalLogResponseDto> mapRenewalLogs(List<InsuranceRenewalLog> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(e -> InsuranceRenewalLogResponseDto.builder()
                        .detRowId(e.getDetRowId())
                        .renewalDate(e.getRenewalDate())
                        .fromDate(e.getFromDate())
                        .expiryDate(e.getExpiryDate())
                        .insuranceAmount(e.getInsuranceAmount())
                        .premiumAmount(e.getPremiumAmount())
                        .build())
                .toList();
    }

    private String normalizeAction(String actionType) {
        if (actionType == null || actionType.trim().isEmpty()) return "ISCREATED";
        return switch (actionType.trim().toUpperCase()) {
            case "ISCREATED", "CREATED", "NEW" -> "ISCREATED";
            case "ISUPDATED", "UPDATED" -> "ISUPDATED";
            case "ISDELETED", "DELETED" -> "ISDELETED";
            default -> "NOCHANGES";
        };
    }

    public DetailsDto getRoleDetails(Long rolePoid) {
        RoleDto roleDto = roleServiceClient.findById(rolePoid);
        if (roleDto == null) {
            throw new ValidationException("Please select a valid User Role in PIC Details");
        }
        return new DetailsDto(
                roleDto.getUserRolePoid(),     // poid
                roleDto.getUserRoleId(),       // code
                roleDto.getUserRoleName(),     // label
                roleDto.getUserRolePoid(),     // value
                roleDto.getUserRoleName(),    // description
                roleDto.getSeqNo()             // seqNo
        );
    }

    private GlobalLogSummary createSummaryLogEntry(LogDetailsEnum logDetailsEnum, String docId, String docKeyPoid, String customMessage, Timestamp logDateTime) {
        GlobalLogSummary summary = new GlobalLogSummary();
        summary.setLogUserPoid(UserContext.getUserPoid());
        summary.setLogDateTime(logDateTime != null ? logDateTime : new Timestamp(System.currentTimeMillis()));
        summary.setLogDocId(docId);
        summary.setLogDocKeyPoid(docKeyPoid);
        summary.setLogDetails(customMessage);
        return summary;
    }

    private InsuranceEmployeeDetail findOldEmployeeByDetRowId(List<InsuranceEmployeeDetail> list, Long detRowId) {
        if (list == null || detRowId == null) return null;
        return list.stream().filter(e -> detRowId.equals(e.getDetRowId())).findFirst().orElse(null);
    }
    private InsurancePropertyDetail findOldPropertyByDetRowId(List<InsurancePropertyDetail> list, Long detRowId) {
        if (list == null || detRowId == null) return null;
        return list.stream().filter(p -> detRowId.equals(p.getDetRowId())).findFirst().orElse(null);
    }
    private InsurancePicDetail findOldPicByDetRowId(List<InsurancePicDetail> list, Long detRowId) {
        if (list == null || detRowId == null) return null;
        return list.stream().filter(p -> detRowId.equals(p.getDetRowId())).findFirst().orElse(null);
    }
    private InsuranceVehicleDetail findOldVehicleByDetRowId(List<InsuranceVehicleDetail> list, Long detRowId) {
        if (list == null || detRowId == null) return null;
        return list.stream().filter(v -> detRowId.equals(v.getDetRowId())).findFirst().orElse(null);
    }

    private List<InsuranceEmployeeDetail> snapshotEmployeeDetails(List<InsuranceEmployeeDetail> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<InsuranceEmployeeDetail> snapshots = new ArrayList<>();
        for (InsuranceEmployeeDetail e : list) {
            InsuranceEmployeeDetail s = new InsuranceEmployeeDetail();
            BeanUtils.copyProperties(e, s);
            s.setTransactionPoid(e.getTransactionPoid());
            s.setDetRowId(e.getDetRowId());
            s.setInsuranceMaster(null);
            snapshots.add(s);
        }
        return snapshots;
    }
    private List<InsurancePropertyDetail> snapshotPropertyDetails(List<InsurancePropertyDetail> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<InsurancePropertyDetail> snapshots = new ArrayList<>();
        for (InsurancePropertyDetail p : list) {
            InsurancePropertyDetail s = new InsurancePropertyDetail();
            BeanUtils.copyProperties(p, s);
            s.setTransactionPoid(p.getTransactionPoid());
            s.setDetRowId(p.getDetRowId());
            s.setInsuranceMaster(null);
            snapshots.add(s);
        }
        return snapshots;
    }
    private List<InsurancePicDetail> snapshotPicDetails(List<InsurancePicDetail> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<InsurancePicDetail> snapshots = new ArrayList<>();
        for (InsurancePicDetail p : list) {
            InsurancePicDetail s = new InsurancePicDetail();
            BeanUtils.copyProperties(p, s);
            s.setTransactionPoid(p.getTransactionPoid());
            s.setDetRowId(p.getDetRowId());
            s.setInsuranceMaster(null);
            snapshots.add(s);
        }
        return snapshots;
    }
    private List<InsuranceVehicleDetail> snapshotVehicleDetails(List<InsuranceVehicleDetail> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<InsuranceVehicleDetail> snapshots = new ArrayList<>();
        for (InsuranceVehicleDetail v : list) {
            InsuranceVehicleDetail s = new InsuranceVehicleDetail();
            BeanUtils.copyProperties(v, s);
            s.setTransactionPoid(v.getTransactionPoid());
            s.setDetRowId(v.getDetRowId());
            s.setInsuranceMaster(null);
            snapshots.add(s);
        }
        return snapshots;
    }

    private void addDetailLogAndModified(InsuranceEmployeeDetail oldE, InsuranceEmployeeDetail newE, String docId, String docKeyPoid, Timestamp now, List<GlobalLogSummary> summaryLogs) {
        saveEmployeeDetailLogsToGlobalLogDetails(oldE, newE, docId, docKeyPoid, now);
    }
    private void addDetailLogAndModified(InsurancePropertyDetail oldE, InsurancePropertyDetail newE, String docId, String docKeyPoid, Timestamp now, List<GlobalLogSummary> summaryLogs) {
        savePropertyDetailLogsToGlobalLogDetails(oldE, newE, docId, docKeyPoid, now);
    }
    private void addDetailLogAndModified(InsurancePicDetail oldE, InsurancePicDetail newE, String docId, String docKeyPoid, Timestamp now, List<GlobalLogSummary> summaryLogs) {
        savePicDetailLogsToGlobalLogDetails(oldE, newE, docId, docKeyPoid, now);
    }
    private void addDetailLogAndModified(InsuranceVehicleDetail oldE, InsuranceVehicleDetail newE, String docId, String docKeyPoid, Timestamp now, List<GlobalLogSummary> summaryLogs) {
        saveVehicleDetailLogsToGlobalLogDetails(oldE, newE, docId, docKeyPoid, now);
    }

    private static String toLogValue(Object o) {
        if (o == null) return null;
        return o.toString();
    }

    private void saveDetailLogRow(String docId, String docKeyPoid, String logDetailsKey, String logTable, String fieldName, Object oldVal, Object newVal, Timestamp now) {
        GlobalLogDetails d = new GlobalLogDetails();
        d.setLogUserPoid(UserContext.getUserPoid());
        d.setLogDateTime(now);
        d.setLogDocId(docId);
        d.setLogDocKeyPoid(docKeyPoid);
        d.setFieldName(fieldName);
        d.setOldValue(toLogValue(oldVal));
        d.setNewValue(toLogValue(newVal));
        d.setLogDetails(logDetailsKey);
        d.setLogTable(logTable);
        globalLogDetailsRepository.save(d);
    }

    private void saveEmployeeDetailLogsToGlobalLogDetails(InsuranceEmployeeDetail oldE, InsuranceEmployeeDetail newE, String docId, String docKeyPoid, Timestamp now) {
        String logDetailsKey = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, oldE.getDetRowId());
        String table = "GLOBAL_INSURANCE_EMPLOYEE_DTL";
        if (!Objects.equals(oldE.getEmployeePoid(), newE.getEmployeePoid()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "employeePoid", oldE.getEmployeePoid(), newE.getEmployeePoid(), now);
        if (!Objects.equals(oldE.getAmount(), newE.getAmount()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "amount", oldE.getAmount(), newE.getAmount(), now);
        if (!Objects.equals(oldE.getRemarks(), newE.getRemarks()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "remarks", oldE.getRemarks(), newE.getRemarks(), now);
    }

    private void savePropertyDetailLogsToGlobalLogDetails(InsurancePropertyDetail oldE, InsurancePropertyDetail newE, String docId, String docKeyPoid, Timestamp now) {
        String logDetailsKey = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, oldE.getDetRowId());
        String table = "GLOBAL_INSURANCE_PROPERTY_DTL";
        if (!Objects.equals(oldE.getPropertyPoid(), newE.getPropertyPoid()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "propertyPoid", oldE.getPropertyPoid(), newE.getPropertyPoid(), now);
        if (!Objects.equals(oldE.getAmount(), newE.getAmount()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "amount", oldE.getAmount(), newE.getAmount(), now);
        if (!Objects.equals(oldE.getRemarks(), newE.getRemarks()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "remarks", oldE.getRemarks(), newE.getRemarks(), now);
    }

    private void savePicDetailLogsToGlobalLogDetails(InsurancePicDetail oldE, InsurancePicDetail newE, String docId, String docKeyPoid, Timestamp now) {
        String logDetailsKey = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, oldE.getDetRowId());
        String table = "GLOBAL_INSURANCE_PIC_DTL";
        if (!Objects.equals(oldE.getRolePoid(), newE.getRolePoid()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "rolePoid", oldE.getRolePoid(), newE.getRolePoid(), now);
        if (!Objects.equals(oldE.getContactType(), newE.getContactType()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "contactType", oldE.getContactType(), newE.getContactType(), now);
        if (!Objects.equals(oldE.getPicPerson(), newE.getPicPerson()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "picPerson", oldE.getPicPerson(), newE.getPicPerson(), now);
        if (!Objects.equals(oldE.getFromDate(), newE.getFromDate()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "fromDate", oldE.getFromDate(), newE.getFromDate(), now);
        if (!Objects.equals(oldE.getToDate(), newE.getToDate()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "toDate", oldE.getToDate(), newE.getToDate(), now);
    }

    private void saveVehicleDetailLogsToGlobalLogDetails(InsuranceVehicleDetail oldE, InsuranceVehicleDetail newE, String docId, String docKeyPoid, Timestamp now) {
        String logDetailsKey = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s", docKeyPoid, oldE.getDetRowId());
        String table = "GLOBAL_INSURANCE_VEHICLE_DTL";
        if (!Objects.equals(oldE.getFixedAssetPoid(), newE.getFixedAssetPoid()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "fixedAssetPoid", oldE.getFixedAssetPoid(), newE.getFixedAssetPoid(), now);
        if (!Objects.equals(oldE.getAmount(), newE.getAmount()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "amount", oldE.getAmount(), newE.getAmount(), now);
        if (!Objects.equals(oldE.getRemarks(), newE.getRemarks()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "remarks", oldE.getRemarks(), newE.getRemarks(), now);
    }

   
    private InsuranceMaster copyHeaderOnlyForLog(InsuranceMaster source) {
        InsuranceMaster copy = new InsuranceMaster();
        BeanUtils.copyProperties(source, copy);
        copy.setEmployeeDetails(null);
        copy.setPropertyDetails(null);
        copy.setPicDetails(null);
        copy.setVehicleDetails(null);
        copy.setInsuranceDetails(null);
        copy.setRenewalLogs(null);
        return copy;
    }

    @Override
    @Transactional
    public InsuranceMasterResponseDto renewInsurance(Long insuranceId, InsuranceMasterRequestDto request) {
        InsuranceMaster existing = insuranceMasterRepository.findById(insuranceId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Master", "ID", insuranceId));

        LocalDate today = LocalDate.now();

        if (existing.getExpiryDate().isAfter(today)) {
            throw new ValidationException("Insurance can only be renewed on or after expiry date");
        }

        LocalDate oldFromDate = existing.getFromDate();
        LocalDate oldExpiryDate = existing.getExpiryDate();
        BigDecimal oldInsuranceAmount = existing.getInsuranceAmount();
        BigDecimal oldPremiumAmount = existing.getPremiumAmount();

        InsuranceRenewalLog renewalLog = InsuranceRenewalLog.builder()
                .transactionPoid(existing.getTransactionPoid())
                .detRowId(existing.getRenewalLogs() != null ? (long) (existing.getRenewalLogs().size() + 1) : 1L)
                .renewalDate(LocalDate.now())
                .fromDate(existing.getFromDate())
                .expiryDate(existing.getExpiryDate())
                .insuranceAmount(existing.getInsuranceAmount())
                .premiumAmount(existing.getPremiumAmount())
                .createdBy(getCurrentUser())
                .createdDate(LocalDateTime.now())
                .lastModifiedBy(getCurrentUser())
                .lastModifiedDate(LocalDateTime.now())
                .build();

        if (existing.getRenewalLogs() == null) {
            existing.setRenewalLogs(new ArrayList<>());
        }
        existing.getRenewalLogs().add(renewalLog);

        existing.setFromDate(request.getFromDate());
        existing.setExpiryDate(request.getExpiryDate());
        existing.setInsuranceAmount(request.getInsuranceAmount());
        existing.setPremiumAmount(request.getPremiumAmount());
        existing.setLastModifiedBy(getCurrentUser());
        existing.setLastModifiedDate(LocalDateTime.now());

        InsuranceMaster renewed = insuranceMasterRepository.save(existing);

        String docId = UserContext.getDocumentId();
        String docKeyPoid = String.valueOf(insuranceId);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        String renewedMessage = String.format("Modified - - DOC:%s KEY:%s", docId, docKeyPoid);
        globalLogSummaryRepository.save(createSummaryLogEntry(LogDetailsEnum.MODIFIED, docId, docKeyPoid, renewedMessage, now));

        List<GlobalLogSummary> renewalSummaryLogs = new ArrayList<>();
        renewalSummaryLogs.add(createSummaryLogEntry(LogDetailsEnum.CREATED, docId, docKeyPoid,
                String.format("Row Created on Insurance Master Renewal Log with DetRowId: %s", renewalLog.getDetRowId()), now));
        globalLogSummaryRepository.saveAll(renewalSummaryLogs);

        String headerLogDetailsKey = String.format("KeyId = TRANSACTION_POID:%s", docKeyPoid);
        String headerTable = "GLOBAL_INSURANCE_HDR";
        if (!Objects.equals(oldFromDate, request.getFromDate()))
            saveDetailLogRow(docId, docKeyPoid, headerLogDetailsKey, headerTable, "fromDate", oldFromDate, request.getFromDate(), now);
        if (!Objects.equals(oldExpiryDate, request.getExpiryDate()))
            saveDetailLogRow(docId, docKeyPoid, headerLogDetailsKey, headerTable, "expiryDate", oldExpiryDate, request.getExpiryDate(), now);
        if (!Objects.equals(oldInsuranceAmount, request.getInsuranceAmount()))
            saveDetailLogRow(docId, docKeyPoid, headerLogDetailsKey, headerTable, "insuranceAmount", oldInsuranceAmount, request.getInsuranceAmount(), now);
        if (!Objects.equals(oldPremiumAmount, request.getPremiumAmount()))
            saveDetailLogRow(docId, docKeyPoid, headerLogDetailsKey, headerTable, "premiumAmount", oldPremiumAmount, request.getPremiumAmount(), now);

        return mapToResponseDto(renewed);
    }
}
