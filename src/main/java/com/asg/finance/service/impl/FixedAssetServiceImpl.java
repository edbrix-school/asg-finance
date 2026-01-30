package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;

import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.masters.*;
import com.asg.finance.entity.AssetLocation;
import com.asg.finance.entity.FixedAssetCategory;
import com.asg.finance.entity.SupplierMasterEntity;
import com.asg.finance.entity.master.FixedAsset;

import com.asg.finance.entity.master.HrEmployeeMaster;
import com.asg.finance.repository.AssetLocationMasterRepository;
import com.asg.finance.repository.FixedAssetCategoryRepository;
import com.asg.finance.repository.SupplierMasterRepository;
import com.asg.finance.repository.master.FixedAssetRepository;

import com.asg.finance.repository.master.HrEmployeeMasterRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.FixedAssetService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleTypes;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FixedAssetServiceImpl implements FixedAssetService {

    private final FixedAssetRepository repository;
    private final DataSource dataSource;
    @Autowired
    DocumentDeleteService documentDeleteService;
    @Autowired
    AssetLocationMasterRepository locationMasterRepository;

    @Autowired
    FixedAssetCategoryRepository fixedAssetCategoryRepository;

    @Autowired
    HrEmployeeMasterRepository hrEmployeeMasterRepository;

    @Autowired
    SupplierMasterRepository supplierMasterRepository;

    @Autowired
    DocumentSearchService documentService;

    @Autowired
    LovDataService lovService;
    
    @Autowired
    LoggingService loggingService;

    public FixedAssetResponseDto createFixedAsset(FixedAssetRequestDto requestDto) {

       /* if (repository.existsByFaCode(requestDto.getFaCode())) {
            throw new IllegalArgumentException("FA Code must be unique");
        }*/
        if (repository.existsByFaDescription(requestDto.getFaDescription())) {
            throw new IllegalArgumentException("FA Description must be unique");
        }
        FixedAsset entity = convertFromFixedAssetDtoToFixedAssetEntity(requestDto);
        FixedAsset savedEntity = repository.save(entity);
        
        // Log the creation
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), savedEntity.getFaPoid().toString());
        
        return convertFromFixedAssetEntityToFixedAssetDto(savedEntity);
    }

    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    private Map<String, Object> fetchFixedAssetPjDetails(Long faPoid) {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_FIXED_ASSET_PJ_DETAILS(?, ?, ?, ?, ?)}")) {
            
            stmt.setLong(1, UserContext.getGroupPoid());
            stmt.setLong(2, UserContext.getCompanyPoid());
            stmt.setLong(3, UserContext.getUserPoid());
            stmt.setLong(4, faPoid);
            stmt.registerOutParameter(5, OracleTypes.CURSOR);
            stmt.execute();
            
            try (ResultSet rs = (ResultSet) stmt.getObject(5)) {
                if (rs != null && rs.next()) {
                    result.put("DOC_REF", rs.getObject("DOC_REF"));
                    result.put("TRANSACTION_POID", rs.getObject("TRANSACTION_POID") != null ? rs.getLong("TRANSACTION_POID") : null);
                    result.put("COMPANY_POID", rs.getObject("COMPANY_POID") != null ? rs.getLong("COMPANY_POID") : null);
                    result.put("INV_NO", rs.getObject("INV_NO"));
                }
            }
        } catch (Exception e) {
            log.error("Error fetching PJ details for FA: {}", faPoid, e);
        }
        return result;
    }

    private FixedAsset convertFromFixedAssetDtoToFixedAssetEntity(FixedAssetRequestDto requestDto) {
        FixedAsset entity = new FixedAsset();
        entity.setGroupPoid(requestDto.getGroupPoid());
        entity.setFaCode(requestDto.getFaCode());
        entity.setFaDescription(requestDto.getFaDescription());
        entity.setFaDescription2(requestDto.getFaDescription2());
        entity.setAssetType(requestDto.getAssetType());
        entity.setFaCategoryPoid(requestDto.getFaCategoryPoid());
        entity.setLocationPoid(requestDto.getLocationPoid());
        entity.setCompanyPoid(requestDto.getCompanyPoid());
        entity.setBarcode(requestDto.getBarcode());
        entity.setRemarks(requestDto.getRemarks());
        entity.setActive(StringUtils.isBlank(requestDto.getActive()) ? "Y" : requestDto.getActive());
        entity.setDeleted("N");
        entity.setMailAlert(requestDto.getMailAlert());
        entity.setVerified(requestDto.getVerified());
        entity.setVerifiedDate(requestDto.getVerifiedDate());
        entity.setAssetValueDate(requestDto.getAssetValueDate());
        entity.setFaType(requestDto.getFaType());
        entity.setFaParentPoid(requestDto.getFaParentPoid());
        entity.setSeqNo(requestDto.getSeqNo());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setCreatedBy(getCurrentUser());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
        //GeneralInfoDto
        if(requestDto.getGeneralInfoDto()!=null) {
            entity.setFaOwner(requestDto.getGeneralInfoDto().getFaOwner());
            entity.setEmployeePoid(requestDto.getGeneralInfoDto().getEmployeePoid());
            entity.setModelNo(requestDto.getGeneralInfoDto().getModelNo());
            entity.setSerialNo(requestDto.getGeneralInfoDto().getSerialNo());
            entity.setFaColor(requestDto.getGeneralInfoDto().getFaColor());
            entity.setFaSize(requestDto.getGeneralInfoDto().getFaSize());
            entity.setCountryOfOrigin(requestDto.getGeneralInfoDto().getCountryOfOrgin());
            entity.setSupplierPoid(requestDto.getGeneralInfoDto().getSupplierPoid());
            entity.setPurchaseDate(requestDto.getGeneralInfoDto().getPurchaseDate());
            entity.setInvoiceNo(requestDto.getGeneralInfoDto().getInvoiceNo());
            entity.setWarrantyPeriod(requestDto.getGeneralInfoDto().getWarrantyPeriod());
            entity.setMaintenanceContract(requestDto.getGeneralInfoDto().getMaintenaceContract());
            entity.setAmcSupplier(requestDto.getGeneralInfoDto().getAmcSupplier());
            entity.setContractExpiry(requestDto.getGeneralInfoDto().getContractExpiry());
            entity.setGrossValue(requestDto.getGeneralInfoDto().getGrossValue());
            entity.setBrand(requestDto.getGeneralInfoDto().getBrand());
            entity.setMakeDate(requestDto.getGeneralInfoDto().getMakeDate());
            entity.setSoftwareDetails(requestDto.getGeneralInfoDto().getSoftwareDetails());
            entity.setSystemSpecifications(requestDto.getGeneralInfoDto().getSystemSpecifications());
        }
        //InformationAssetDetailsDto
        if(requestDto.getInformationAssetDetailsDto()!=null) {
        entity.setTypeOfInformationAsset(requestDto.getInformationAssetDetailsDto().getTypeOfInformationAsset());
        entity.setPersonalData(requestDto.getInformationAssetDetailsDto().getPersonalData());
        entity.setPersonalSensitiveData(requestDto.getInformationAssetDetailsDto().getPersonalSensitiveData());
        entity.setSensitiveCustomerData(requestDto.getInformationAssetDetailsDto().getSensitiveCustomerData());
        entity.setAssetClassification(requestDto.getInformationAssetDetailsDto().getAssetClassification());
        entity.setIntegrity(requestDto.getInformationAssetDetailsDto().getIntegrity());
        entity.setAvailability(requestDto.getInformationAssetDetailsDto().getAvailability());
        entity.setDataRetentionPeriod(requestDto.getInformationAssetDetailsDto().getDataRetentionPeriod());
        }
        //OpeningDetailsDto
        if(requestDto.getOpeningDetailsDto()!=null) {
            entity.setOpeningAsset(requestDto.getOpeningDetailsDto().getOpeningAsset());
            entity.setOpeningAssetValue(requestDto.getOpeningDetailsDto().getOpeningAssetValue());
            entity.setAccumulatedDepreciation(requestDto.getOpeningDetailsDto().getAccDepreciatedAmt());
            entity.setWdValue(requestDto.getOpeningDetailsDto().getWdvValue());
        }
        //DepreciationDetailsDto
        if(requestDto.getDepreciationDetailsDto()!=null) {
            entity.setDepreciable("Y");
            entity.setDepreciationMethod(requestDto.getDepreciationDetailsDto().getDepreciationMethod());
            entity.setDepreciationStartDate(requestDto.getDepreciationDetailsDto().getDepreciationStartDate());
            entity.setDepreciationPercent(requestDto.getDepreciationDetailsDto().getDepreciationPercent());
            entity.setAssetLife(requestDto.getDepreciationDetailsDto().getAssetLife());
            entity.setScrapValue(requestDto.getDepreciationDetailsDto().getScrapValue());
            entity.setScrapDate(requestDto.getDepreciationDetailsDto().getScrapDate());
            entity.setFaGlAccount(requestDto.getDepreciationDetailsDto().getFaGlAccount());
            entity.setFaAccumulationAccount(requestDto.getDepreciationDetailsDto().getFaAccumulationAc());
            entity.setFaDepreciationAccount(requestDto.getDepreciationDetailsDto().getFaDepreciationAc());
            entity.setCostCenter(requestDto.getDepreciationDetailsDto().getCostCenter());
        }

        if(validateVehicleDetailsIfPresent(requestDto.getVehicleDetailsDto())){
            if(requestDto.getVehicleDetailsDto()!=null) {
                entity.setVehicleProductionYear(requestDto.getVehicleDetailsDto().getVehicleProductionYear());
                entity.setRegistrationNo(requestDto.getVehicleDetailsDto().getRegistrationNo());
                entity.setModelNo(requestDto.getVehicleDetailsDto().getModelNo());
                entity.setEngineChassisNo(requestDto.getVehicleDetailsDto().getEngineChassisNo());
                entity.setVehicleType(requestDto.getVehicleDetailsDto().getVehicleType());
                entity.setInsuranceCoverage(requestDto.getVehicleDetailsDto().getInsuranceCoverage());
                entity.setInsuranceRenewalDate(requestDto.getVehicleDetailsDto().getInsuranceRenewalDate());
                entity.setInsuranceCompany(requestDto.getVehicleDetailsDto().getInsuranceCompany());
                entity.setInsurancePolicyNo(requestDto.getVehicleDetailsDto().getInsurancePolicyNo());
                entity.setInsuranceAmount(requestDto.getVehicleDetailsDto().getInsuranceAmount());
            }
        }

        return entity;
    }

    private FixedAssetResponseDto convertFromFixedAssetEntityToFixedAssetDto(FixedAsset savedEntity) {

        DetailsDto locationPoidDet = null;
        if (savedEntity.getLocationPoid() != null) {
            AssetLocation defaultLocationEntity = locationMasterRepository.findByLocationPoid(savedEntity.getLocationPoid()).orElse(null);
            if (defaultLocationEntity != null) {
                locationPoidDet = new DetailsDto(defaultLocationEntity.getLocationPoid(), defaultLocationEntity.getLocationCode(),
                        defaultLocationEntity.getLocationCode(), defaultLocationEntity.getLocationPoid(), defaultLocationEntity.getDescription(), defaultLocationEntity.getSeqNo());
            }
        }

        DetailsDto faCategoyPoidDet = null;
        if (savedEntity.getFaCategoryPoid() != null) {
            FixedAssetCategory fixedAssetCategory = fixedAssetCategoryRepository.findByFaCategoryPoid(savedEntity.getFaCategoryPoid()).orElse(null);
            if (fixedAssetCategory != null) {
                faCategoyPoidDet = new DetailsDto(fixedAssetCategory.getFaCategoryPoid(), fixedAssetCategory.getFaCategoryCode(),
                        fixedAssetCategory.getFaCategoryDescription(), fixedAssetCategory.getFaCategoryPoid(), fixedAssetCategory.getFaCategoryDescription(), fixedAssetCategory.getSeqNo());
            }
        }

        DetailsDto companyPoidDet = null;
        if (savedEntity.getCompanyPoid() != null) {
            LovGetListDto lovGetListDto = lovService.getDetailsByPoidAndLovName(savedEntity.getCompanyPoid(), "COMPANY");
            if (lovGetListDto != null) {
                companyPoidDet = new DetailsDto(lovGetListDto.getPoid(), lovGetListDto.getCode(),
                        lovGetListDto.getLabel(), lovGetListDto.getValue(), lovGetListDto.getDescription(), lovGetListDto.getSeqNo());
            }
        }

        DetailsDto supplierPoidDet = null;
        if (savedEntity.getSupplierPoid() != null) {
            SupplierMasterEntity supplierMasterEntity = supplierMasterRepository.findBySupplierPoid(savedEntity.getSupplierPoid());
            if (supplierMasterEntity != null) {
                supplierPoidDet = new DetailsDto(supplierMasterEntity.getSupplierPoid(), supplierMasterEntity.getSupplierCode(),
                        supplierMasterEntity.getSupplierName(), supplierMasterEntity.getSupplierPoid(), supplierMasterEntity.getSupplierName(),
                        supplierMasterEntity.getSeqNo() == null ? null : Math.toIntExact(supplierMasterEntity.getSeqNo()));
            }
        }

        DetailsDto employeePoidDet = null;
        if (savedEntity.getEmployeePoid() != null) {
            HrEmployeeMaster hrEmployeeMaster = hrEmployeeMasterRepository.findByEmployeePoid(savedEntity.getEmployeePoid()).orElse(null);
            if (hrEmployeeMaster != null) {
                employeePoidDet = new DetailsDto(hrEmployeeMaster.getEmployeePoid(), hrEmployeeMaster.getEmployeeCode(),
                        hrEmployeeMaster.getEmployeeName(), hrEmployeeMaster.getEmployeePoid(), hrEmployeeMaster.getEmployeeName(),
                        hrEmployeeMaster.getSeqNo() == null ? null : Math.toIntExact(hrEmployeeMaster.getSeqNo()));
            }
        }

        // GeneralInfoDto mapping
        Map<String, Object> pjDetails = fetchFixedAssetPjDetails(savedEntity.getFaPoid());
        GeneralInfoDto generalInfoDto = GeneralInfoDto.builder()
                .faOwner(savedEntity.getFaOwner())
                .employeePoid(savedEntity.getEmployeePoid())
                .modelNo(savedEntity.getModelNo())
                .serialNo(savedEntity.getSerialNo())
                .faColor(savedEntity.getFaColor())
                .faSize(savedEntity.getFaSize())
                .supplierPoidDet(supplierPoidDet)
                .employeePoidDet(employeePoidDet)
                .countryOfOrgin(savedEntity.getCountryOfOrigin())
                .supplierPoid(savedEntity.getSupplierPoid())
                .brand(savedEntity.getBrand())
                .makeDate(savedEntity.getMakeDate())
                .systemSpecifications(savedEntity.getSystemSpecifications())
                .purchaseDate(savedEntity.getPurchaseDate())
                .invoiceNo(savedEntity.getInvoiceNo())
                .warrantyPeriod(savedEntity.getWarrantyPeriod())
                .maintenaceContract(savedEntity.getMaintenanceContract())
                .amcSupplier(savedEntity.getAmcSupplier())
                .contractExpiry(savedEntity.getContractExpiry())
                .grossValue(savedEntity.getGrossValue())
                .softwareDetails(savedEntity.getSoftwareDetails())
                .pjDocRef((String) pjDetails.getOrDefault("DOC_REF", null))
                .pjTransactionPoid((Long) pjDetails.getOrDefault("TRANSACTION_POID", null))
                .pjCompanyPoid((Long) pjDetails.getOrDefault("COMPANY_POID", null))
                .pjInvNo((String) pjDetails.getOrDefault("INV_NO", null))
                .build();

                 //VehicleDetailsDto mapping
                 VehicleDetailsDto vehicleDetailsDto = VehicleDetailsDto.builder()
                .vehicleProductionYear(savedEntity.getVehicleProductionYear())
                .registrationNo(savedEntity.getRegistrationNo())
                         .modelNo(savedEntity.getModelNo())

                 .engineChassisNo(savedEntity.getEngineChassisNo())
                .vehicleType(savedEntity.getVehicleType())
                .insuranceCoverage(savedEntity.getInsuranceCoverage())
                .insuranceRenewalDate(savedEntity.getInsuranceRenewalDate())
                .insuranceCompany(savedEntity.getInsuranceCompany())
                .insurancePolicyNo(savedEntity.getInsurancePolicyNo())
                .insuranceAmount(savedEntity.getInsuranceAmount())
                .build();


        // InformationAssetDetailsDto mapping
        InformationAssetDetailsDto informationAssetDetailsDto = InformationAssetDetailsDto.builder()
                .typeOfInformationAsset(savedEntity.getTypeOfInformationAsset())
                .personalData(savedEntity.getPersonalData())
                .personalSensitiveData(savedEntity.getPersonalSensitiveData())
                .sensitiveCustomerData(savedEntity.getSensitiveCustomerData())
                .assetClassification(savedEntity.getAssetClassification())
                .integrity(savedEntity.getIntegrity())
                .availability(savedEntity.getAvailability())
                .dataRetentionPeriod(savedEntity.getDataRetentionPeriod())
                .build();

        // OpeningDetailsDto mapping
        OpeningDetailsDto openingDetailsDto = OpeningDetailsDto.builder()
        		.openingAsset(savedEntity.getOpeningAsset())
                .openingAssetValue(savedEntity.getOpeningAssetValue())
                .accDepreciatedAmt(savedEntity.getAccumulatedDepreciation())
                .wdvValue(savedEntity.getWdValue())
                .build();

        // DepreciationDetailsDto mapping
        DepreciationDetailsDto depreciationDetailsDto = DepreciationDetailsDto.builder()
                .depreciable(savedEntity.getDepreciable())
                .depreciationMethod(savedEntity.getDepreciationMethod())
                .depreciationStartDate(savedEntity.getDepreciationStartDate())
                .depreciationPercent(savedEntity.getDepreciationPercent())
                .assetLife(savedEntity.getAssetLife())
                .scrapValue(savedEntity.getScrapValue())
                .scrapDate(savedEntity.getScrapDate())
                .faGlAccount(savedEntity.getFaGlAccount())
                .faAccumulationAc(savedEntity.getFaAccumulationAccount())
                .faDepreciationAc(savedEntity.getFaDepreciationAccount())
                .costCenter(savedEntity.getCostCenter())
                .build();

        // Main DTO mapping
        return FixedAssetResponseDto.builder()
                .faPoid(savedEntity.getFaPoid())
                .groupPoid(savedEntity.getGroupPoid())
                .faCode(savedEntity.getFaCode())
                .faDescription(savedEntity.getFaDescription())
                .faDescription2(savedEntity.getFaDescription2())
                .assetType(savedEntity.getAssetType())
                .faCategoryPoid(savedEntity.getFaCategoryPoid())
                .faCategoryPoidDet(faCategoyPoidDet)
                .locationPoidDet(locationPoidDet)
               // .supplierPoidDet(supplierPoidDet)
                .locationPoid(savedEntity.getLocationPoid())
                .companyPoid(savedEntity.getCompanyPoid())
                .companyPoidDet(companyPoidDet)
                //.employeePoidDet(employeePoidDet)
                .barcode(savedEntity.getBarcode())
                .remarks(savedEntity.getRemarks())
                .active(savedEntity.getActive())
                .deleted(savedEntity.getDeleted())
                .mailAlert(savedEntity.getMailAlert())
                .verified(savedEntity.getVerified())
                .verifiedDate(savedEntity.getVerifiedDate())
                .assetValueDate(savedEntity.getAssetValueDate())
                .faType(savedEntity.getFaType())
                .faParentPoid(savedEntity.getFaParentPoid())
                .seqNo(savedEntity.getSeqNo())
                .createdBy(savedEntity.getCreatedBy())
                .createdDate(savedEntity.getCreatedDate())

                // Nested DTOs
                .generalInfoDto(generalInfoDto)
                .informationAssetDetailsDto(informationAssetDetailsDto)
                .openingDetailsDto(openingDetailsDto)
                .depreciationDetailsDto(depreciationDetailsDto)
                .vehicleDetailsDto(vehicleDetailsDto)
                .build();
    }

    private boolean validateVehicleDetailsIfPresent(VehicleDetailsDto vehicleDetails) {
        if (vehicleDetails == null) return true;

        if (isBlank(vehicleDetails.getVehicleType())) {
            throw new ValidationException("Vehicle Type is required");
        }
        if (vehicleDetails.getVehicleProductionYear() == null) {
            throw new ValidationException("Vehicle Production Year is required");
        }
        if (vehicleDetails.getRegistrationNo() == null) {
            throw new ValidationException("Registration Year is required");
        }
        if (isBlank(vehicleDetails.getModelNo())) {
            throw new ValidationException("Model No is required");
        }
        if (isBlank(vehicleDetails.getEngineChassisNo())) {
            throw new ValidationException("Engine Chassis No is required");
        }
        return true;
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public FixedAssetResponseDto updateFixedAsset(Long faPoid, FixedAssetRequestDto requestDto) {
        FixedAsset existingEntity = repository.findById(faPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed Asset not found for id: ", "faPoid", faPoid));

        // Create a copy of the old entity for logging
        FixedAsset oldEntity = new FixedAsset();
        BeanUtils.copyProperties(existingEntity, oldEntity);

        if (repository.existsByFaCodeAndFaPoidNot(requestDto.getFaCode(), faPoid)) {
            throw new ValidationException("FA Code must be unique" + faPoid);
        }
        if (repository.existsByFaDescriptionAndFaPoidNot(requestDto.getFaDescription(), faPoid)) {
            throw new ValidationException("FA Description must be unique" + faPoid);
        }

        FixedAsset updatedEntity = convertFromFixedAssetDtoToFixedAssetEntity(requestDto);

        updatedEntity.setFaPoid(existingEntity.getFaPoid());
        updatedEntity.setLastModifiedBy(getCurrentUser());
        updatedEntity.setLastModifiedDate(LocalDateTime.now());
        FixedAsset savedEntity = repository.save(updatedEntity);
        
        // Log the update
        loggingService.logChanges(oldEntity, savedEntity, FixedAsset.class, UserContext.getDocumentId(), faPoid.toString(), LogDetailsEnum.MODIFIED, "FA_POID");
        
        return convertFromFixedAssetEntityToFixedAssetDto(savedEntity);
    }

    public FixedAssetResponseDto getFixedAssetById(Long faPoid) {
        FixedAsset entity = repository.findById(faPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed Asset not found for id: ", "faPoid", faPoid));

        return convertFromFixedAssetEntityToFixedAssetDto(entity);
    }

    @Transactional
    public void softDeleteFixedAsset(Long faPoid, DeleteReasonDto deleteReasonDto) {
        FixedAsset existing = repository.findByFaPoid(faPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed Asset not found with ID: ", "faPoid", faPoid));
        
        documentDeleteService.deleteDocument(
                faPoid,
                "FIXED_ASSET_MASTER",
                "FA_POID",
                deleteReasonDto,
                null
        );
    }

    @Transactional
    public List<Long> createMultipleCopies(Long faPoid, int noOfCopies) {
        FixedAsset original = repository.findByFaPoid(faPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed Asset", "faPoid", faPoid));

        List<FixedAsset> copies = new ArrayList<>();

        for (int i = 0; i < noOfCopies; i++) {
            FixedAsset copy = new FixedAsset();
            BeanUtils.copyProperties(original, copy, "faPoid", "faCode", "createdDate", "createdBy", "batchCreationRef");

            copy.setBatchCreationRef(original.getFaCode());

            copy.setCreatedBy(getCurrentUser());
            copy.setCreatedDate(LocalDateTime.now());
            copy.setLastModifiedBy(getCurrentUser());
            copy.setLastModifiedDate(LocalDateTime.now());
            copy.setDeleted("N");
            copy.setActive("Y");
            copies.add(copy);
        }

        List<FixedAsset> savedCopies = repository.saveAll(copies);
        return savedCopies.stream().map(FixedAsset::getFaPoid).toList();
    }

    @Override
    public Map<String, Object> listFixedAssetCategories(String documentId, FilterRequestDto request, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(documentId, filters, operator, pageable, isDeleted,
                "CATEGORY_CODE",
                "CATEGORY_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

}
