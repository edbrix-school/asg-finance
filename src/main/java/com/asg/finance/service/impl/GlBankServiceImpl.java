package com.asg.finance.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.finance.entity.GlBankEntity;
import com.asg.finance.entity.TaxMaster;
import com.asg.finance.repository.*;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.finance.dto.GlBankChequeDtlDto;
import com.asg.finance.dto.GlBankCommissionDtlDto;
import com.asg.finance.dto.GlBankDto;
import com.asg.finance.entity.GlBankChequeDtlEntity;
import com.asg.finance.entity.GlBankCommissionDtlEntity;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.GlBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

@Service
@Slf4j
@RequiredArgsConstructor
public class GlBankServiceImpl implements GlBankService {

    private final GlBankRepository bankRepository;

    private final GlBankChequeDtlRepository chequeDtlRepository;

    private final GlBankCommissionDtlRepository commissionDtlRepository;

    private final DocumentSearchService documentService;

    private final TaxMasterRepository taxMasterRepository;

    private final GLMasterRepository glMasterRepository;

    private final LovDataService lovService;

    private final DocumentDeleteService documentDeleteService;
    
    private final LoggingService loggingService;


    @Override
    public GlBankDto fetchGlBank(Long bankPoid) {
        GlBankEntity bankEntity = bankRepository.findByBankPoid(bankPoid);
        if (bankEntity == null) {
            throw new ResourceNotFoundException("Bank", "bankPoid", bankPoid);
        }
        GlBankDto bankDto = new GlBankDto();
        BeanUtils.copyProperties(bankEntity, bankDto);
        bankDto.setCompanyPoid(Long.valueOf(bankEntity.getCompanyPoid()));
        bankDto.setCreatedBy(bankEntity.getCreatedBy());
        bankDto.setCreatedDate(bankEntity.getCreatedDate());
        if (StringUtils.isNotBlank(bankEntity.getCompanyPoid())) {
            bankDto.setCompanyDet(lovService.getDetailsByPoidAndLovName(Long.valueOf(bankEntity.getCompanyPoid()), "COMPANY"));
        }
        if (bankEntity.getCurrencyCode() != null) {
            bankDto.setCurrencyDet(lovService.getDetailsByCodeAndLovName(bankEntity.getCurrencyCode(), "CURRENCY"));
        }
        bankDto.setChequeDetails(chequeDtlRepository.findByBankPoid(bankPoid).stream().map(this::convertGlBankChequeDtlEntityToGlBankChequeDtlDto).collect(Collectors.toList()));
        bankDto.setCommissionDetails(commissionDtlRepository.findByBankPoid(bankPoid).stream().map(this::convertGlBankCommissionDtlEntityToGlBankCommissionDtlDto).collect(Collectors.toList()));

        return bankDto;
    }

    @Override
    @Transactional
    public GlBankDto updateGlBank(Long bankPoid, GlBankDto glBankDto) {
        GlBankEntity bankEntity = bankRepository.findByBankPoid(bankPoid);
        if (bankEntity == null) {
            throw new ResourceNotFoundException("Bank", "bankPoid", bankPoid);
        }

        // Create a copy of the old entity for logging
        GlBankEntity oldEntity = new GlBankEntity();
        BeanUtils.copyProperties(bankEntity, oldEntity);

        boolean existsByBankCode = bankRepository.existsByBankCodeIgnoreCaseAndBankPoidNot(glBankDto.getBankCode(), bankPoid);

        if (existsByBankCode) {
            throw new ResourceAlreadyExistsException("Bank Code", glBankDto.getBankCode());
        }

        boolean existsByBankDescription = bankRepository.existsByBankDescriptionIgnoreCaseAndBankPoidNot(glBankDto.getBankDescription(), bankPoid);

        if (existsByBankDescription) {
            throw new ResourceAlreadyExistsException("Bank Description", glBankDto.getBankDescription());
        }

        boolean existsByBankAccountNo = bankRepository.existsByBankAccountNoIgnoreCaseAndBankPoidNot(glBankDto.getBankAccountNo(), bankPoid);

        if (existsByBankAccountNo) {
            throw new ResourceAlreadyExistsException("Bank AccountNo", glBankDto.getBankAccountNo());
        }

        if(lovService.getDetailsByPoidAndLovName(glBankDto.getCompanyPoid(), "COMPANY").getCode() == null) {
            throw new ResourceNotFoundException("Company", "companyPoid", glBankDto.getCompanyPoid());
        }

        if (lovService.getLovItemByCodeFast(glBankDto.getCurrencyCode(), "CURRENCY").getPoid() == null) {
            throw new ResourceNotFoundException("Currency", "currencyCode", glBankDto.getCurrencyCode());
        }

        if (!glMasterRepository.existsByGlPoid(glBankDto.getGlPoid())) {
            throw new ResourceNotFoundException("Gl Master", "glPoid", glBankDto.getGlPoid());
        }

        updateBankFields(bankEntity, glBankDto);
        bankEntity.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
        GlBankEntity updatedGlBankEntity = bankRepository.save(bankEntity);
        List<GlBankChequeDtlEntity> updatedChequeDtlEntities = glBankDto.getChequeDetails() != null && !glBankDto.getChequeDetails().isEmpty() ? processChequeDetails(bankPoid, glBankDto.getChequeDetails()) : new ArrayList<>();

        List<GlBankCommissionDtlEntity> updatedCommissionDtlEntities = glBankDto.getCommissionDetails() != null && !glBankDto.getCommissionDetails().isEmpty() ? processCommissionDetails(bankPoid, glBankDto.getCommissionDetails()) : new ArrayList<>();

        // Log the update
        loggingService.logChanges(oldEntity, updatedGlBankEntity, GlBankEntity.class, UserContext.getDocumentId(), bankPoid.toString(), LogDetailsEnum.MODIFIED, "BANK_POID");

        return convertGlBankEntityToGlBankDto(updatedGlBankEntity, updatedChequeDtlEntities, updatedCommissionDtlEntities);
    }

    @Override
    @Transactional
    public GlBankDto createEntry(GlBankDto bankMasterDto) {
        //Bank Code Bank Description and Bank Account No are required and unique fields
        boolean bankCodeExists = bankRepository.existsByBankCodeIgnoreCase(bankMasterDto.getBankCode());
        if (bankCodeExists) {
            throw new ResourceAlreadyExistsException("Bank Code", bankMasterDto.getBankCode());
        }
        boolean bankDescriptionExists = bankRepository.existsByBankDescriptionIgnoreCase(bankMasterDto.getBankDescription());
        if (bankDescriptionExists) {
            throw new ResourceAlreadyExistsException("Bank Description", bankMasterDto.getBankDescription());
        }
        boolean bankAccountNoExists = bankRepository.existsByBankAccountNoIgnoreCase(bankMasterDto.getBankAccountNo());
        if (bankAccountNoExists) {
            throw new ResourceAlreadyExistsException("Bank Account No", bankMasterDto.getBankAccountNo());
        }

        if(lovService.getDetailsByPoidAndLovName(bankMasterDto.getCompanyPoid(), "COMPANY").getCode() == null) {
            throw new ResourceNotFoundException("Company", "companyPoid", bankMasterDto.getCompanyPoid());
        }

        if (lovService.getLovItemByCodeFast(bankMasterDto.getCurrencyCode(), "CURRENCY").getPoid() == null) {
            throw new ResourceNotFoundException("Currency", "currencyCode", bankMasterDto.getCurrencyCode());
        }

        if (!glMasterRepository.existsByGlPoid(bankMasterDto.getGlPoid())) {
            throw new ResourceNotFoundException("Gl Master", "glPoid", bankMasterDto.getGlPoid());
        }

        //Save details
        GlBankEntity bankMasterData = saveBankDetails(bankMasterDto);

        saveBankChequeDetails(bankMasterDto, bankMasterData);
        saveBankCommisionDetails(bankMasterDto, bankMasterData);        
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), bankMasterData.getBankPoid().toString());
        return fetchGlBank(bankMasterData.getBankPoid());
    }

    private void saveBankCommisionDetails(GlBankDto bankMasterDto, GlBankEntity bankMasterData) {
        List<GlBankCommissionDtlEntity> commissionDetailsEntity = new ArrayList<>();
        bankMasterDto.getCommissionDetails().forEach(commissionDetailsDto -> {
            createNewGlBankCommissionDtlEntity(bankMasterData.getBankPoid(), commissionDetailsDto, commissionDetailsEntity);
        });
        commissionDtlRepository.saveAll(commissionDetailsEntity);
    }

    private void saveBankChequeDetails(GlBankDto bankMasterDto, GlBankEntity bankMasterData) {
        List<GlBankChequeDtlEntity> chequeDetailsListEntity = new ArrayList<>();
        bankMasterDto.getChequeDetails().forEach(chequeDetailsDto -> {
            createNewGlBankChequeDtlEntity(bankMasterData.getBankPoid(), chequeDetailsDto, chequeDetailsListEntity);
        });
        chequeDtlRepository.saveAll(chequeDetailsListEntity);
    }

    private GlBankEntity saveBankDetails(GlBankDto bankMasterDto) {
        GlBankEntity bankMaster = new GlBankEntity();
        BeanUtils.copyProperties(bankMasterDto, bankMaster);
        bankMaster.setGroupPoid(UserContext.getGroupPoid());
        bankMaster.setCompanyPoid(String.valueOf(UserContext.getCompanyPoid()));
        bankMaster.setCreatedBy(getCurrentUser());
        bankMaster.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
        return bankRepository.save(bankMaster);
    }


    private void updateBankFields(GlBankEntity entity, GlBankDto request) {
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setBankCode(request.getBankCode());
        entity.setBankDescription(request.getBankDescription());
        entity.setBankDescription2(request.getBankDescription2());
        entity.setGlPoid(request.getGlPoid());
        entity.setBankAccountNo(request.getBankAccountNo());
        entity.setIban(request.getIban());
        entity.setSwiftCode(request.getSwiftCode());
        entity.setBankAddress(request.getBankAddress());
        entity.setCurrencyCode(request.getCurrencyCode());
        entity.setBuyingRate(request.getBuyingRate());
        entity.setSellingRate(request.getSellingRate());
        entity.setPeriodStart(request.getPeriodStart());
        entity.setPeriodEnd(request.getPeriodEnd());
        entity.setCardCommision(request.getCardCommision());
        entity.setChequePrintingYn(request.getChequePrintingYn());
        entity.setSeqno(request.getSeqno());
        entity.setRemarks(request.getRemarks());
        entity.setActive(request.getActive());
        entity.setDeleted(request.getDeleted());
        entity.setOdLimit(request.getOdLimit());
        entity.setCompanyPoid(String.valueOf(UserContext.getCompanyPoid()));
        entity.setOldGlAccNo(request.getOldGlAccNo());
        entity.setBankPrefix(request.getBankPrefix());
        entity.setOnlineFileTt(request.getOnlineFileTt());
        entity.setBankStatementDate(request.getBankStatementDate());
        entity.setCurrencyRate(request.getCurrencyRate());
        entity.setVatTinNumber(request.getVatTinNumber());
        entity.setAccountType(request.getAccountType());
        entity.setCorrespondantSwiftCode(request.getCorrespondantSwiftCode());
        entity.setCorrespondantBank(request.getCorrespondantBank());
        entity.setEdiBankAccountNo(request.getEdiBankAccountNo());
    }

    private List<GlBankChequeDtlEntity> processChequeDetails(Long bankPoid, List<GlBankChequeDtlDto> glBankChequeDtlDtoList) {
        List<GlBankChequeDtlEntity> entitiesToDelete = new ArrayList<>();
        List<GlBankChequeDtlEntity> entitiesToSave = new ArrayList<>();
        List<GlBankChequeDtlEntity> savedEntities = new ArrayList<>();
        List<LogRequestDto<GlBankChequeDtlEntity>> logRequests = new ArrayList<>();

        for (GlBankChequeDtlDto dto : glBankChequeDtlDtoList) {
            String action = StringUtils.isBlank(dto.getActionType()) ? "" : dto.getActionType().toLowerCase();

            switch (action) {
                case "isdeleted" -> handleDeleteActionForChequeDetails(bankPoid, dto, entitiesToDelete);
                case "iscreated", "isupdated" -> handleCreateOrUpdateChequeDetails(bankPoid, dto, entitiesToSave, logRequests);
                default ->
                        log.warn("Unknown or missing actionType '{}' for cheque detail with detRowId={}", dto.getActionType(), dto.getDetRowId());
            }
        }
        if (!entitiesToDelete.isEmpty()) {
            chequeDtlRepository.deleteAll(entitiesToDelete);
        }
        if (!entitiesToSave.isEmpty()) {
            savedEntities = chequeDtlRepository.saveAll(entitiesToSave);
        }
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
        return savedEntities;
    }

    private void handleDeleteActionForChequeDetails(Long bankPoid, GlBankChequeDtlDto dto, List<GlBankChequeDtlEntity> entitiesToDelete) {
        if (dto.getDetRowId() != null) {
            chequeDtlRepository.findByBankPoidAndDetRowId(bankPoid, dto.getDetRowId())
                    .ifPresentOrElse(
                            entity -> {
                                entitiesToDelete.add(entity);
                                loggingService.logDelete(dto, UserContext.getDocumentId(), bankPoid.toString());
                            },
                            () -> log.warn("No GlBankChequeDtlEntity found for bankPoid={} and detRowId={}, skipping delete.", bankPoid, dto.getDetRowId())
                    );
        } else {
            log.warn("detRowId is null for bankPoid={} in cheque details, skipping delete.", bankPoid);
        }
    }

    private void handleCreateOrUpdateChequeDetails(Long bankPoid, GlBankChequeDtlDto glBankChequeDtlDto, List<GlBankChequeDtlEntity> entitiesToSave, List<LogRequestDto<GlBankChequeDtlEntity>> logRequests) {
        if (glBankChequeDtlDto.getDetRowId() != null) {
            chequeDtlRepository.findByBankPoidAndDetRowId(bankPoid, glBankChequeDtlDto.getDetRowId())
                    .ifPresentOrElse(
                            existingEntity -> {
                                GlBankChequeDtlEntity oldEntity = new GlBankChequeDtlEntity();
                                BeanUtils.copyProperties(existingEntity, oldEntity);
                                updateExistingGlBankChequeDtlEntity(existingEntity, glBankChequeDtlDto, entitiesToSave);
                                logRequests.add(new LogRequestDto<>(oldEntity, existingEntity, GlBankChequeDtlEntity.class, 
                                    UserContext.getDocumentId(), bankPoid.toString(), String.format( "KeyId = BANK_POID:%s DET_ROW_ID:%s", oldEntity.getBankPoid(), glBankChequeDtlDto.getDetRowId())));
                            },
                            () -> createNewGlBankChequeDtlEntity(bankPoid, glBankChequeDtlDto, entitiesToSave)
                    );
        } else {
            createNewGlBankChequeDtlEntity(bankPoid, glBankChequeDtlDto, entitiesToSave);
        }
    }

    private void createNewGlBankChequeDtlEntity(Long bankPoid, GlBankChequeDtlDto dto, List<GlBankChequeDtlEntity> entitiesToSave) {
        GlBankChequeDtlEntity newEntity = new GlBankChequeDtlEntity();

        newEntity.setBankPoid(bankPoid);
        newEntity.setChqSignType(dto.getChqSignType());
        newEntity.setTotalCheques(dto.getTotalCheques());
        newEntity.setStartChqNo(dto.getStartChqNo());
        newEntity.setEndChqNo(dto.getEndChqNo());
        newEntity.setReorderLevel(dto.getReorderLevel());
        newEntity.setCurrentChqNo(dto.getCurrentChqNo());
        newEntity.setDefaultPrinterAddr(dto.getDefaultPrinterAddr());
        newEntity.setDefaultPrinterTray(dto.getDefaultPrinterTray());
        newEntity.setStockFinishedYn(dto.getStockFinishedYn());
        newEntity.setRemarks(dto.getRemarks());
        newEntity.setCreatedBy(getCurrentUser());
        newEntity.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
        newEntity.setLastModifiedBy(getCurrentUser());
        newEntity.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
        newEntity.setLastChqNo(dto.getLastChqNo());
        newEntity.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
        entitiesToSave.add(newEntity);
        
        String logDetail = String.format("Row Created on Bank Cheque Detail with detRowId: %s", dto.getDetRowId());
        loggingService.createLogSummaryEntry(UserContext.getDocumentId(), bankPoid.toString(), logDetail);
    }

    private void updateExistingGlBankChequeDtlEntity(GlBankChequeDtlEntity entity, GlBankChequeDtlDto dto, List<GlBankChequeDtlEntity> entities) {
        entity.setChqSignType(dto.getChqSignType());
        entity.setTotalCheques(dto.getTotalCheques());
        entity.setStartChqNo(dto.getStartChqNo());
        entity.setEndChqNo(dto.getEndChqNo());
        entity.setReorderLevel(dto.getReorderLevel());
        entity.setCurrentChqNo(dto.getCurrentChqNo());
        entity.setDefaultPrinterAddr(dto.getDefaultPrinterAddr());
        entity.setDefaultPrinterTray(dto.getDefaultPrinterTray());
        entity.setStockFinishedYn(dto.getStockFinishedYn());
        entity.setRemarks(dto.getRemarks());
        entity.setLastChqNo(dto.getLastChqNo());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
        entities.add(entity);
    }

    private List<GlBankCommissionDtlEntity> processCommissionDetails(Long bankPoid, List<GlBankCommissionDtlDto> glBankCommissionDtlDtoList) {
        List<GlBankCommissionDtlEntity> entitiesToDelete = new ArrayList<>();
        List<GlBankCommissionDtlEntity> entitiesToSave = new ArrayList<>();
        List<GlBankCommissionDtlEntity> savedEntities = new ArrayList<>();
        List<LogRequestDto<GlBankCommissionDtlEntity>> logRequests = new ArrayList<>();

        for (GlBankCommissionDtlDto dto : glBankCommissionDtlDtoList) {
            String action = StringUtils.isBlank(dto.getActionType()) ? "" : dto.getActionType().toLowerCase();

            switch (action) {
                case "isdeleted" -> handleDeleteActionForCommissionDetails(bankPoid, dto, entitiesToDelete);
                case "iscreated", "isupdated" -> handleCreateOrUpdateCommissionDetails(bankPoid, dto, entitiesToSave, logRequests);
                default ->
                        log.warn("Unknown or missing actionType '{}' for commission detail with detRowId={}", dto.getActionType(), dto.getDetRowId());
            }
        }
        if (!entitiesToDelete.isEmpty()) {
            commissionDtlRepository.deleteAll(entitiesToDelete);
        }
        if (!entitiesToSave.isEmpty()) {
            savedEntities = commissionDtlRepository.saveAll(entitiesToSave);
        }
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
        return savedEntities;
    }

    private void handleDeleteActionForCommissionDetails(Long bankPoid, GlBankCommissionDtlDto dto, List<GlBankCommissionDtlEntity> entitiesToDelete) {
        if (dto.getDetRowId() != null) {
            commissionDtlRepository.findByBankPoidAndDetRowId(bankPoid, dto.getDetRowId())
                    .ifPresentOrElse(
                            entity -> {
                                entitiesToDelete.add(entity);
                                loggingService.logDelete(dto, UserContext.getDocumentId(), bankPoid.toString());
                            },
                            () -> log.warn("No GlBankCommissionDtlEntity found for bankPoid={} and detRowId={}, skipping delete.", bankPoid, dto.getDetRowId())
                    );
        } else {
            log.warn("detRowId is null for bankPoid={} in commission details, skipping delete.", bankPoid);
        }
    }


    private void handleCreateOrUpdateCommissionDetails(Long bankPoid, GlBankCommissionDtlDto glBankCommissionDtlDto, List<GlBankCommissionDtlEntity> entitiesToSave, List<LogRequestDto<GlBankCommissionDtlEntity>> logRequests) {
        if (glBankCommissionDtlDto.getDetRowId() != null) {
            commissionDtlRepository.findByBankPoidAndDetRowId(bankPoid, glBankCommissionDtlDto.getDetRowId())
                    .ifPresentOrElse(
                            existingEntity -> {
                                GlBankCommissionDtlEntity oldEntity = new GlBankCommissionDtlEntity();
                                BeanUtils.copyProperties(existingEntity, oldEntity);
                                updateExistingGlBankCommissionDtlEntity(existingEntity, glBankCommissionDtlDto, entitiesToSave);
                                logRequests.add(new LogRequestDto<>(oldEntity, existingEntity, GlBankCommissionDtlEntity.class, 
                                    UserContext.getDocumentId(), bankPoid.toString(), String.format( "KeyId = BANK_POID:%s DET_ROW_ID:%s", oldEntity.getBankPoid(), glBankCommissionDtlDto.getDetRowId())));
                            },
                            () -> createNewGlBankCommissionDtlEntity(bankPoid, glBankCommissionDtlDto, entitiesToSave)
                    );
        } else {
            createNewGlBankCommissionDtlEntity(bankPoid, glBankCommissionDtlDto, entitiesToSave);
        }
    }

    private void createNewGlBankCommissionDtlEntity(Long bankPoid, GlBankCommissionDtlDto dto, List<GlBankCommissionDtlEntity> entitiesToSave) {
        if (dto.getCommissionGlPoid() != null) {
            boolean existsByGlPoid = glMasterRepository.existsByGlPoid(dto.getCommissionGlPoid());

            if (!existsByGlPoid) {
                throw new ResourceNotFoundException("Commission Gl Master", "commissionGlPoid", dto.getCommissionGlPoid());
            }
        }
        if (dto.getTaxPoid() != null) {
            boolean existsByTaxPoid = taxMasterRepository.existsByTaxPoid(dto.getTaxPoid());

            if (!existsByTaxPoid) {
                throw new ResourceNotFoundException("Tax Master", "taxPoid", dto.getTaxPoid());
            }
        }

        GlBankCommissionDtlEntity newEntity = new GlBankCommissionDtlEntity();
        newEntity.setBankPoid(bankPoid);
        newEntity.setPeriodFrom(dto.getPeriodFrom());
        newEntity.setPeriodTo(dto.getPeriodTo());
        newEntity.setCommissionGlPoid(dto.getCommissionGlPoid());
        newEntity.setCommissionPercent(dto.getCommissionPercent());
        newEntity.setTaxPoid(dto.getTaxPoid());
        newEntity.setTaxPercentage(dto.getTaxPercentage());
        newEntity.setRemarks(dto.getRemarks());
        newEntity.setCreatedBy(getCurrentUser());
        newEntity.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
        newEntity.setLastModifiedBy(getCurrentUser());
        newEntity.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
        newEntity.setCardType(dto.getCardType());
        entitiesToSave.add(newEntity);
        
        String logDetail = String.format("Row Created on Bank Commission Detail with detRowId: %s", dto.getDetRowId());
        loggingService.createLogSummaryEntry(UserContext.getDocumentId(), bankPoid.toString(), logDetail);
    }

    private void updateExistingGlBankCommissionDtlEntity(GlBankCommissionDtlEntity entity, GlBankCommissionDtlDto dto, List<GlBankCommissionDtlEntity> entities) {
        boolean existsByGlPoid = glMasterRepository.existsByGlPoid(dto.getCommissionGlPoid());

        if (!existsByGlPoid) {
            throw new ResourceNotFoundException("Commission Gl Master", "commissionGlPoid", dto.getCommissionGlPoid());
        }

        boolean existsByTaxPoid = taxMasterRepository.existsByTaxPoid(dto.getTaxPoid());

        if (!existsByTaxPoid) {
            throw new ResourceNotFoundException("Tax Master", "taxPoid", dto.getTaxPoid());
        }

        entity.setPeriodFrom(dto.getPeriodFrom());
        entity.setPeriodTo(dto.getPeriodTo());
        entity.setCommissionGlPoid(dto.getCommissionGlPoid());
        entity.setCommissionPercent(dto.getCommissionPercent());
        entity.setTaxPoid(dto.getTaxPoid());
        entity.setTaxPercentage(dto.getTaxPercentage());
        entity.setRemarks(dto.getRemarks());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(Timestamp.valueOf(LocalDateTime.now()));
        entity.setCardType(dto.getCardType());
        entities.add(entity);
    }


    private GlBankDto convertGlBankEntityToGlBankDto(GlBankEntity entity, List<GlBankChequeDtlEntity> glBankChequeDtlEntityList, List<GlBankCommissionDtlEntity> glBankCommissionDtlEntityList) {
        if (entity == null) return null;

        GlBankDto dto = new GlBankDto();
        dto.setBankPoid(entity.getBankPoid());
        dto.setGroupPoid(entity.getGroupPoid());
        dto.setBankCode(entity.getBankCode());
        dto.setBankDescription(entity.getBankDescription());
        dto.setBankDescription2(entity.getBankDescription2());
        dto.setGlPoid(entity.getGlPoid());
        dto.setBankAccountNo(entity.getBankAccountNo());
        dto.setIban(entity.getIban());
        dto.setSwiftCode(entity.getSwiftCode());
        dto.setBankAddress(entity.getBankAddress());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setBuyingRate(entity.getBuyingRate());
        dto.setSellingRate(entity.getSellingRate());
        dto.setPeriodStart(entity.getPeriodStart());
        dto.setPeriodEnd(entity.getPeriodEnd());
        dto.setCardCommision(entity.getCardCommision());
        dto.setChequePrintingYn(entity.getChequePrintingYn());
        dto.setSeqno(entity.getSeqno());
        dto.setRemarks(entity.getRemarks());
        dto.setActive(entity.getActive());
        dto.setDeleted(entity.getDeleted());
        dto.setOdLimit(entity.getOdLimit());
        dto.setCompanyPoid(Long.valueOf(entity.getCompanyPoid()));
        dto.setOldGlAccNo(entity.getOldGlAccNo());
        dto.setBankPrefix(entity.getBankPrefix());
        dto.setOnlineFileTt(entity.getOnlineFileTt());
        dto.setBankStatementDate(entity.getBankStatementDate());
        dto.setCurrencyRate(entity.getCurrencyRate());
        dto.setVatTinNumber(entity.getVatTinNumber());
        dto.setAccountType(entity.getAccountType());
        dto.setCorrespondantSwiftCode(entity.getCorrespondantSwiftCode());
        dto.setCorrespondantBank(entity.getCorrespondantBank());
        dto.setEdiBankAccountNo(entity.getEdiBankAccountNo());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());

        dto.setChequeDetails(convertGlBankChequeDtlEntityListToGlBankChequeDtlDtoList(glBankChequeDtlEntityList));
        dto.setCommissionDetails(convertGlBankCommissionDtlEntityListToGlBankCommissionDtlDtoList(glBankCommissionDtlEntityList));

        return dto;
    }

    private GlBankChequeDtlDto convertGlBankChequeDtlEntityToGlBankChequeDtlDto(GlBankChequeDtlEntity entity) {
        if (entity == null) return null;

        GlBankChequeDtlDto dto = new GlBankChequeDtlDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setChqSignType(entity.getChqSignType());
        dto.setTotalCheques(entity.getTotalCheques());
        dto.setStartChqNo(entity.getStartChqNo());
        dto.setEndChqNo(entity.getEndChqNo());
        dto.setReorderLevel(entity.getReorderLevel());
        dto.setCurrentChqNo(entity.getCurrentChqNo());
        dto.setDefaultPrinterAddr(entity.getDefaultPrinterAddr());
        dto.setDefaultPrinterTray(entity.getDefaultPrinterTray());
        dto.setStockFinishedYn(entity.getStockFinishedYn());
        dto.setRemarks(entity.getRemarks());
        dto.setLastChqNo(entity.getLastChqNo());

        return dto;
    }

    private GlBankCommissionDtlDto convertGlBankCommissionDtlEntityToGlBankCommissionDtlDto(GlBankCommissionDtlEntity entity) {

        log.info("taxPoid : {}, commissionPoid : {}", entity.getTaxPoid(), entity.getCommissionGlPoid());

        GlBankCommissionDtlDto dto = new GlBankCommissionDtlDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setCommissionGlPoid(entity.getCommissionGlPoid());
        dto.setCommissionPercent(entity.getCommissionPercent());
        dto.setPeriodFrom(entity.getPeriodFrom());
        dto.setPeriodTo(entity.getPeriodTo());
        dto.setCommissionPercent(entity.getCommissionPercent());
        dto.setRemarks(entity.getRemarks());
        dto.setCardType(entity.getCardType());
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setTaxPoid(entity.getTaxPoid());

        if (entity.getCommissionGlPoid() != null) {
            dto.setCommissionGlDet(lovService.getDetailsByPoidAndLovName(entity.getCommissionGlPoid(), "BANK_COMMISSION_GL"));
        }

        if (entity.getTaxPoid() != null) {
            dto.setTaxPoidDet(lovService.getDetailsByPoidAndLovName(entity.getTaxPoid(), "BANK_TAX_MASTER"));
        }

        if (entity.getTaxPoid() != null) {
            Optional<TaxMaster> taxMaster = taxMasterRepository.findByTaxPoid(entity.getTaxPoid());
            if (taxMaster.isPresent()) {
                dto.setTaxCode(taxMaster.get().getTaxCode());
                dto.setTaxName(taxMaster.get().getTaxName());
            }
        }
        return dto;
    }

    List<GlBankChequeDtlDto> convertGlBankChequeDtlEntityListToGlBankChequeDtlDtoList(List<GlBankChequeDtlEntity> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream()
                .map(this::convertGlBankChequeDtlEntityToGlBankChequeDtlDto)
                .collect(Collectors.toList());
    }

    List<GlBankCommissionDtlDto> convertGlBankCommissionDtlEntityListToGlBankCommissionDtlDtoList(List<GlBankCommissionDtlEntity> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream()
                .map(this::convertGlBankCommissionDtlEntityToGlBankCommissionDtlDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteBankMaster(Long bankPoid, DeleteReasonDto deleteReasonDto) {
        GlBankEntity bankEntity = bankRepository.findByBankPoid(bankPoid);
        if (bankEntity == null) {
            throw new ResourceNotFoundException("Bank", "bankPoid", bankPoid);
        }

        // Use DocumentDeleteService for consistent soft delete handling
        documentDeleteService.deleteDocument(
                bankPoid,
                "GL_BANK_MASTER",
                "BANK_POID",
                deleteReasonDto,
                null
        );
    }

    public Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, Pageable pageable) {

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "BANK_DESCRIPTION", "BANK_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }
}
