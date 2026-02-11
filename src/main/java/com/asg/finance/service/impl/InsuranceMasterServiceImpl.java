package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.client.RoleServiceClient;
import com.asg.finance.dto.masters.*;
import com.asg.finance.entity.GlobalLogDetails;
import com.asg.finance.entity.GlobalLogSummary;
import com.asg.finance.entity.master.*;
import com.asg.finance.repository.GlobalLogDetailsRepository;
import com.asg.finance.repository.GlobalLogSummaryRepository;
import com.asg.finance.repository.master.HrEmployeeMasterRepository;
import com.asg.finance.repository.master.InsuranceMasterRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.InsuranceMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.stream.IntStream;

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
    private final HrEmployeeMasterRepository hrEmployeeMasterRepository;
    private final LovDataService lovService;

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    @Override
    public InsuranceMasterResponseDto getInsuranceMasterById(Long insuranceId) {
        InsuranceMaster insuranceMaster = insuranceMasterRepository.findById(insuranceId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Master", "ID", insuranceId));
        return mapToResponseDto(insuranceMaster);
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
            System.out.println("Creating insurance master for policy: " + request.getPolicyNo());

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
                    .oneTime("N")
                    .description(request.getDescription())
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
            
            // Logging for create operation
            loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), finalSaved.getTransactionPoid().toString());
            
            return mapToResponseDto(finalSaved);
        } catch (Exception e) {
            System.err.println("Error in createInsuranceMaster: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create Insurance Master: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public InsuranceMasterResponseDto updateInsuranceMaster(Long insuranceId, InsuranceMasterRequestDto request) {
        // Preserve existing PJ_REF_POID if already linked


        InsuranceMaster existing = insuranceMasterRepository.findById(insuranceId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Master", "ID", insuranceId));

        // Create copy of old entity for logging
        InsuranceMaster oldEntity = new InsuranceMaster();
        BeanUtils.copyProperties(existing, oldEntity);

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
        existing.setInsuranceAmount(request.getInsuranceAmount());
        existing.setPremiumAmount(request.getPremiumAmount());
        existing.setPaymentFrequency(request.getPaymentFrequency());
        existing.setDescription(request.getDescription());
        existing.setLastModifiedBy(getCurrentUser());
        existing.setLastModifiedDate(LocalDateTime.now());

        // Clear existing child details
        existing.getEmployeeDetails().clear();
        existing.getPropertyDetails().clear();
        existing.getPicDetails().clear();

        // Add updated child details
        buildAndSetChildDetails(request, existing);
        if (existing.getPjRefPoid() != null) {
            existing.setPjRefPoid(existing.getPjRefPoid());
        } else {
            existing.setPjRefPoid(null); // don't allow overwriting or random assignment
        }

        InsuranceMaster updated = insuranceMasterRepository.save(existing);
        
        // Logging for update operation
        loggingService.logChanges(oldEntity, updated, InsuranceMaster.class, UserContext.getDocumentId(), insuranceId.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
        
        return mapToResponseDto(updated);
    }

    private void buildAndSetChildDetails(InsuranceMasterRequestDto request, InsuranceMaster savedParent) {
        try {
            if (request.getEmployeeDetails() != null && !request.getEmployeeDetails().isEmpty()) {
                System.out.println("Building " + request.getEmployeeDetails().size() + " employee details");
                savedParent.setEmployeeDetails(buildEmployeeDetails(request.getEmployeeDetails(), savedParent));
            }
            if (request.getPropertyDetails() != null && !request.getPropertyDetails().isEmpty()) {
                System.out.println("Building " + request.getPropertyDetails().size() + " property details");
                savedParent.setPropertyDetails(buildPropertyDetails(request.getPropertyDetails(), savedParent));
            }

            if (request.getPicDetails() != null && !request.getPicDetails().isEmpty()) {
                System.out.println("Building " + request.getPicDetails().size() + " PIC details");
                savedParent.setPicDetails(buildPicDetails(request.getPicDetails(), savedParent));
            }
        } catch (Exception e) {
            System.err.println("Error building child details: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }



    private List<InsuranceEmployeeDetail> buildEmployeeDetails(List<InsuranceEmployeeDetailRequestDto> dtos, InsuranceMaster parent) {
        if (dtos == null) return new ArrayList<>();
        List<InsuranceEmployeeDetail> result = new ArrayList<>();
        long nextDetRowId = 1;
        
        for (InsuranceEmployeeDetailRequestDto dto : dtos) {
            String action = normalizeAction(dto.getActionType());
            
            if ("ISDELETED".equals(action)) continue;
            
            Long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++;
            
            result.add(InsuranceEmployeeDetail.builder()
                    .transactionPoid(parent.getTransactionPoid())
                    .detRowId(detRowId)
                    .employeePoid(dto.getEmployeePoid())
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
        long nextDetRowId = 1;
        
        for (InsurancePropertyDetailRequestDto dto : dtos) {
            String action = normalizeAction(dto.getActionType());
            
            if ("ISDELETED".equals(action)) continue;
            
            Long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++;
            
            result.add(InsurancePropertyDetail.builder()
                    .transactionPoid(parent.getTransactionPoid())
                    .detRowId(detRowId)
                    .propertyPoid(dto.getPropertyPoid())
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
        long nextDetRowId = 1;
        
        for (InsurancePicDetailRequestDto dto : dtos) {
            String action = normalizeAction(dto.getActionType());
            
            if ("ISDELETED".equals(action)) continue;
            
            DetailsDto roleDetails = getRoleDetails(dto.getRolePoid());
            Long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++;
            
            result.add(InsurancePicDetail.builder()
                    .transactionPoid(parent.getTransactionPoid())
                    .detRowId(detRowId)
                    .rolePoid(roleDetails.poid())
                    .contactType(dto.getContactType())
                    .picPersonPoid(dto.getPicPersonPoid())
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

    private InsuranceMasterResponseDto mapToResponseDto(InsuranceMaster entity) {
        return InsuranceMasterResponseDto.builder()
                .insurancePoid(entity.getTransactionPoid())
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
                .map(e -> {
                    LovGetListDto employeeLov = null;
                    if (e.getEmployeePoid() != null) {
                        try {
                            HrEmployeeMaster employee = hrEmployeeMasterRepository.findByEmployeePoid(e.getEmployeePoid()).orElse(null);
                            if (employee != null) {
                                employeeLov = new LovGetListDto(
                                    employee.getEmployeePoid(),
                                    employee.getEmployeeCode(),
                                    employee.getEmployeeName(),
                                    employee.getEmployeePoid(),
                                    employee.getEmployeeName(),
                                    null,
                                    null
                                );
                            }
                        } catch (Exception ex) {
                            // ignore
                        }
                    }
                    return InsuranceEmployeeDetailResponseDto.builder()
                            .employeeDetailPoid(e.getDetRowId())
                            .employee(employeeLov)
                            .amount(e.getAmount())
                            .remarks(e.getRemarks())
                            .build();
                })
                .toList();
    }

    private List<InsurancePropertyDetailResponseDto> mapPropertyDetails(List<InsurancePropertyDetail> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(e -> {
                    LovGetListDto propertyLov = null;
                    if (e.getPropertyPoid() != null) {
                        propertyLov = getLovByPoid(e.getPropertyPoid(), "INSURANCE_PROPERTY_MASTER");
                    }
                    return InsurancePropertyDetailResponseDto.builder()
                            .propertyDetailPoid(e.getDetRowId())
                            .property(propertyLov)
                            .amount(e.getAmount())
                            .remarks(e.getRemarks())
                            .build();
                })
                .toList();
    }



    private List<InsurancePicDetailResponseDto> mapPicDetails(List<InsurancePicDetail> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(e -> {
                    DetailsDto roleDetails = null;
                    if (e.getRolePoid() != null) {
                        try {
                            roleDetails = getRoleDetails(e.getRolePoid());
                        } catch (Exception ex) {
                            // ignore
                        }
                    }
                    LovGetListDto picPersonLov = null;
                    if (e.getPicPersonPoid() != null) {
                        picPersonLov = getLovByPoid(e.getPicPersonPoid(), "INSURANCE_PROPERTY_PIC");
                    }
                    return InsurancePicDetailResponseDto.builder()
                            .picDetailPoid(e.getDetRowId())
                            .role(roleDetails)
                            .contactType(e.getContactType())
                            .picPerson(picPersonLov)
                            .fromDate(e.getFromDate())
                            .toDate(e.getToDate())
                            .build();
                })
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

    private List<InsuranceRenewalLog> buildRenewalLogs(List<InsuranceRenewalLogRequestDto> dtos, InsuranceMaster parent) {
        if (dtos == null) return new ArrayList<>();

        LocalDate today = LocalDate.now();

        return IntStream.range(0, dtos.size())
                .mapToObj(i -> {
                    InsuranceRenewalLogRequestDto dto = dtos.get(i);

                    // Validate renewal date is not in future
                    if (dto.getRenewalDate().isAfter(today)) {
                        throw new ValidationException("Renewal date cannot be in the future");
                    }

                    return InsuranceRenewalLog.builder()
                            .transactionPoid(parent.getTransactionPoid())
                            .detRowId((long) (i + 1))
                            .renewalDate(dto.getRenewalDate())
                            .fromDate(dto.getFromDate())
                            .expiryDate(dto.getExpiryDate())
                            .createdBy(getCurrentUser())
                            .createdDate(LocalDateTime.now())
                            .lastModifiedBy(getCurrentUser())
                            .lastModifiedDate(LocalDateTime.now())
                            .build();
                })
                .collect(Collectors.toList());
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

    private LovGetListDto getLovByPoid(Long poid, String lovName) {
        if (poid == null) return null;
        try {
            return lovService.getDetailsByPoidAndLovName(poid, lovName);
        } catch (Exception e) {
            return null;
        }
    }

    private LovGetListDto getLovByCode(String code, String lovName) {
        if (code == null) return null;
        try {
            return lovService.getDetailsByCodeAndLovName(code, lovName);
        } catch (Exception e) {
            return null;
        }
    }

    private String getEmployeeLabel(Long employeePoid) {
        if (employeePoid == null) return null;
        try {
            return hrEmployeeMasterRepository.findByEmployeePoid(employeePoid)
                    .map(HrEmployeeMaster::getEmployeeName)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String getPropertyLabel(Long propertyPoid) {
        if (propertyPoid == null) return null;
        try {
            // Property PIC is from LOV table - fetch using document service or return poid as string
            // Since we don't have a direct repository, return the poid for now
            // Frontend should already have the label from the LOV dropdown
            return "Property-" + propertyPoid;
        } catch (Exception e) {
            return null;
        }
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
        if (!Objects.equals(oldE.getPicPersonPoid(), newE.getPicPersonPoid()))
            saveDetailLogRow(docId, docKeyPoid, logDetailsKey, table, "picPersonPoid", oldE.getPicPersonPoid(), newE.getPicPersonPoid(), now);
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

        // Validate insurance can be renewed (must be expired or expiring)
        if (existing.getExpiryDate().isAfter(today)) {
            throw new ValidationException("Insurance can only be renewed on or after expiry date");
        }

        // Archive current details to renewal log
        InsuranceRenewalLog renewalLog = InsuranceRenewalLog.builder()
                .transactionPoid(existing.getTransactionPoid())
                .detRowId((long) (existing.getRenewalLogs().size() + 1))
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

        existing.getRenewalLogs().add(renewalLog);

        // Update with new renewal details
        existing.setFromDate(request.getFromDate());
        existing.setExpiryDate(request.getExpiryDate());
        existing.setInsuranceAmount(request.getInsuranceAmount());
        existing.setPremiumAmount(request.getPremiumAmount());
        existing.setLastModifiedBy(getCurrentUser());
        existing.setLastModifiedDate(LocalDateTime.now());

        InsuranceMaster renewed = insuranceMasterRepository.save(existing);
        return mapToResponseDto(renewed);
    }
}
