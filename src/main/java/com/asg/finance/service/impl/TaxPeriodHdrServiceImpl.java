package com.asg.finance.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.finance.entity.StockMasterEntity;
import com.asg.finance.entity.TaxMaster;
import com.asg.finance.repository.TaxMasterRepository;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.finance.dto.*;
import com.asg.finance.entity.master.ShipChargeEntity;
import com.asg.finance.entity.master.ShipChargeGroupEntity;
import com.asg.finance.entity.master.StockCategoryMasterEntity;
import com.asg.finance.repository.StockMasterRepository;
import com.asg.finance.repository.master.ShipChargeGroupRepository;
import com.asg.finance.repository.master.ShipChargeRepository;
import com.asg.finance.repository.master.StockCategoryMasterRepository;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.finance.service.TaxPeriodHdrService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.enums.LogDetailsEnum;
import net.minidev.json.writer.BeansMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.asg.finance.entity.GlobalTaxPeriodChargeDtlEntity;
import com.asg.finance.entity.GlobalTaxPeriodStockDtlEntity;
import com.asg.finance.entity.TaxPeriodHdr;
import com.asg.finance.repository.GlobalTaxPeriodChargeDtlRepository;
import com.asg.finance.repository.GlobalTaxPeriodStockDtlRepository;
import com.asg.finance.repository.TaxPeriodHdrRepository;
import com.asg.common.lib.security.util.UserContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

@Service
@RequiredArgsConstructor
public class TaxPeriodHdrServiceImpl implements TaxPeriodHdrService {
    private final TaxPeriodHdrRepository taxPeriodHdrRepository;
    private final GlobalTaxPeriodChargeDtlRepository globalTaxPeriodChargeDtlRepository;
    private final GlobalTaxPeriodStockDtlRepository globalTaxPeriodStockDtlRepository;
    private final ShipChargeRepository shipChargeRepository;
    private final ShipChargeGroupRepository shipChargeGroupRepository;
    private final TaxMasterRepository taxMasterRepository;
    private final StockMasterRepository stockMasterRepository;
    private final StockCategoryMasterRepository stockCategoryMasterRepository;
    private final EntityManager entityManager;
    private final DocumentSearchService documentService;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;

    @Override
    @Transactional
    public TaxPeriodHdrResponseDto createTaxPeriodHdr(TaxPeriodHdrRequestDto request) {
        validatePeriodDates(request.getPeriodFrom(), request.getPeriodTo());
        validatePeriodOverlap(request.getPeriodFrom(), request.getPeriodTo());
        TaxPeriodHdr entity = convertFromTaxPeriodHdrDtoToTaxPeriodHdrEntity(request);
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        TaxPeriodHdr taxPeriodHdr = taxPeriodHdrRepository.save(entity);
        String currentUser = getCurrentUser();
        Long transactionPoid = taxPeriodHdr.getTransactionPoid();
        String key = taxPeriodHdr.getTransactionPoid().toString();
        String docId = UserContext.getDocumentId();
        if (request.getCharges() != null && !request.getCharges().isEmpty()) {
            List<GlobalTaxPeriodChargeDtlEntity> chargeEntities = request.getCharges().stream()
                    .map(dto -> GlobalTaxPeriodChargeDtlEntity.builder()
                            .transactionPoid(transactionPoid)
                            .chargePoid(dto.getChargePoid())
                            .chargeCatPoid(dto.getChargeCatPoid())
                            .outputTaxPoid(dto.getOutputTaxPoid())
                            .inputTaxPoid(dto.getInputTaxPoid())
                            .detRowId(dto.getDetRowId())
                            .remarks(dto.getRemarks())
                            .createdBy(currentUser)
                            .createdDate(LocalDateTime.now())
                            .lastModifiedBy(currentUser)
                            .lastModifiedDate(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());

            globalTaxPeriodChargeDtlRepository.saveAll(chargeEntities);

            chargeEntities.forEach(chargeDtlEntity -> {
                String logDetail = String.format("Row Created on Tax Charge with detRowId: %s", chargeDtlEntity.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), docId , logDetail);
            });
        }

        if (request.getStocks() != null && !request.getStocks().isEmpty()) {
           List<GlobalTaxPeriodStockDtlEntity> stockDtlEntities = request.getStocks().stream()
                   .map(dto -> GlobalTaxPeriodStockDtlEntity.builder()
                           .transactionPoid(transactionPoid)
                           .stockPoid(dto.getStockPoid())
                           .stockCatPoid(dto.getStockCatPoid())
                           .taxPoid(dto.getOutputTaxPoid())
                           .inputTaxPoid(dto.getInputTaxPoid())
                           .detRowId(dto.getDetRowId())
                           .remarks(dto.getRemarks())
                           .createdBy(currentUser)
                           .createdDate(LocalDateTime.now())
                           .lastModifiedBy(currentUser)
                           .lastModifiedDate(LocalDateTime.now())
                           .build())
                   .collect(Collectors.toList());
           globalTaxPeriodStockDtlRepository.saveAll(stockDtlEntities);
           stockDtlEntities.forEach(stockDtlEntity -> {
                String logDetail = String.format("Row Created on Tax stock with detRowId: %s", stockDtlEntity.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), docId , logDetail);
            });
        }

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);


        return convertFromTaxPeriodHdrEntityToTaxPeriodHdrDto(taxPeriodHdr);
    }

    @Override
    @Transactional
    public TaxPeriodHdrResponseDto updateTaxPeriodHdr(Long transactionPoid, TaxPeriodHdrRequestDto request) {
        validatePeriodDates(request.getPeriodFrom(), request.getPeriodTo());
        validatePeriodOverlapForUpdate(request.getPeriodFrom(), request.getPeriodTo(), transactionPoid);
        TaxPeriodHdr existingEntity = taxPeriodHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Period not found for id: ", "transactionPoid", transactionPoid));

        // Create a copy of the existing entity for logging
        TaxPeriodHdr oldEntity = new TaxPeriodHdr();
        BeanUtils.copyProperties(existingEntity, oldEntity);

        TaxPeriodHdr updatedEntity = convertFromTaxPeriodHdrDtoToTaxPeriodHdrEntity(request);

        updatedEntity.setDocRef(existingEntity.getDocRef());
        updatedEntity.setTransactionPoid(existingEntity.getTransactionPoid());
        updatedEntity.setTransactionDate(LocalDate.now());
        TaxPeriodHdr savedEntity = taxPeriodHdrRepository.save(updatedEntity);
        //Update Stocks & Charges
        if (request.getCharges() != null && !request.getCharges().isEmpty()) {
            updateTaxPeriodCharges(request.getCharges(), savedEntity.getTransactionPoid());
        }
        if (request.getStocks() != null && !request.getStocks().isEmpty()) {
            updateTaxPeriodStocks(request.getStocks(), savedEntity.getTransactionPoid());
        }
        
        // Log the update
        String key = savedEntity.getTransactionPoid().toString();
        String docId = UserContext.getDocumentId();
        loggingService.logChanges(oldEntity, savedEntity, TaxPeriodHdr.class, 
                docId, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
        
        return convertFromTaxPeriodHdrEntityToTaxPeriodHdrDto(savedEntity);
    }

    public TaxPeriodHdrResponseDto getTaxPeriodHdrById(Long transactionPoid) {
        TaxPeriodHdr savedEntity = taxPeriodHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Period not found for id: ", "transactionPoid", transactionPoid));
        return convertFromTaxPeriodHdrEntityToTaxPeriodHdrDto(savedEntity);
    }

    @Transactional
    public void softDeleteTaxPeriodHdr(Long transactionPoid, DeleteReasonDto reasonDto) {
        TaxPeriodHdr savedEntity = taxPeriodHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Period not found with ID: ", "transactionPoid", transactionPoid));
        savedEntity.setDeleted("Y");
        savedEntity.setLastModifiedDate(LocalDateTime.now());
        savedEntity.setLastModifiedBy(getCurrentUser());
        taxPeriodHdrRepository.save(savedEntity);

        documentDeleteService.deleteDocument(
                transactionPoid,
                "GLOBAL_TAX_PERIOD_HDR",
                "TRANSACTION_POID",
                reasonDto,
                savedEntity.getTransactionDate()
        );
    }


    private TaxPeriodHdr convertFromTaxPeriodHdrDtoToTaxPeriodHdrEntity(TaxPeriodHdrRequestDto request) {
        TaxPeriodHdr taxPeriodHdr = new TaxPeriodHdr();
        taxPeriodHdr.setGroupPoid(UserContext.getGroupPoid());
        taxPeriodHdr.setCompanyPoid(UserContext.getCompanyPoid());
        taxPeriodHdr.setTransactionDate(LocalDate.now());
        taxPeriodHdr.setDescription(request.getDescription());
        taxPeriodHdr.setPeriodFrom(request.getPeriodFrom());
        taxPeriodHdr.setPeriodTo(request.getPeriodTo());
        taxPeriodHdr.setLastModifiedBy(getCurrentUser());
        taxPeriodHdr.setLastModifiedDate(LocalDateTime.now());
        taxPeriodHdr.setDeleted("N");
        return taxPeriodHdr;
    }

    private TaxPeriodHdrResponseDto convertFromTaxPeriodHdrEntityToTaxPeriodHdrDto(TaxPeriodHdr taxPeriodHdr) {
        TaxPeriodHdrResponseDto responseDto = new TaxPeriodHdrResponseDto();
        responseDto.setTransactionPoid(taxPeriodHdr.getTransactionPoid());
        responseDto.setTransactionDate(LocalDate.now());
        responseDto.setGroupPoid(taxPeriodHdr.getGroupPoid());
        responseDto.setCompanyPoid(taxPeriodHdr.getCompanyPoid());
        responseDto.setDocRef(taxPeriodHdr.getDocRef());
        responseDto.setDescription(taxPeriodHdr.getDescription());
        responseDto.setPeriodFrom(taxPeriodHdr.getPeriodFrom());
        responseDto.setPeriodTo(taxPeriodHdr.getPeriodTo());
        responseDto.setDeleted(taxPeriodHdr.getDeleted());
        responseDto.setCreatedBy(taxPeriodHdr.getCreatedBy());
        responseDto.setCreatedDate(taxPeriodHdr.getCreatedDate());
        responseDto.setModifiedBy(taxPeriodHdr.getLastModifiedBy());
        responseDto.setModifiedDate(taxPeriodHdr.getLastModifiedDate());
        return responseDto;
    }


    private String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    @Override
    public Page<TaxPeriodChargeDtlResponseDto> getTaxPeriodCharges(Long transactionPoid, Pageable pageable) {
        Page<GlobalTaxPeriodChargeDtlEntity> chargeEntities = globalTaxPeriodChargeDtlRepository
                .findByTransactionPoid(transactionPoid, pageable);
        
        // Collect all unique IDs
        Set<Long> chargePoids = chargeEntities.getContent().stream()
                .map(GlobalTaxPeriodChargeDtlEntity::getChargePoid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Set<Long> chargeCatPoids = chargeEntities.getContent().stream()
                .map(GlobalTaxPeriodChargeDtlEntity::getChargeCatPoid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Set<Long> taxPoids = chargeEntities.getContent().stream()
                .flatMap(entity -> Stream.of(entity.getOutputTaxPoid(), entity.getInputTaxPoid()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // Batch fetch all details
        Map<Long, ShipChargeEntity> shipChargeMap = shipChargeRepository.findByChargePoidIn(chargePoids)
                .stream().collect(Collectors.toMap(ShipChargeEntity::getChargePoid, Function.identity()));
        
        Map<Long, ShipChargeGroupEntity> shipChargeGroupMap = shipChargeGroupRepository.findByChargeGroupPoidIn(chargeCatPoids)
                .stream().collect(Collectors.toMap(ShipChargeGroupEntity::getChargeGroupPoid, Function.identity()));
        
        Map<Long, TaxMaster> taxMasterMap = taxMasterRepository.findByTaxPoidIn(taxPoids)
                .stream().collect(Collectors.toMap(TaxMaster::getTaxPoid, Function.identity()));
        
        return chargeEntities.map(entity -> convertToChargeDto(entity, shipChargeMap, shipChargeGroupMap, taxMasterMap));
    }

    private TaxPeriodChargeDtlResponseDto convertToChargeDto(GlobalTaxPeriodChargeDtlEntity entity,
                                                             Map<Long, ShipChargeEntity> shipChargeMap,
                                                             Map<Long, ShipChargeGroupEntity> shipChargeGroupMap,
                                                             Map<Long, TaxMaster> taxMasterMap) {

        DetailsDto chargePoidDet = null;
        if (entity.getChargePoid() != null) {
            ShipChargeEntity shipChargeEntity = shipChargeMap.get(entity.getChargePoid());
            if (shipChargeEntity != null) {
                chargePoidDet = new DetailsDto(
                        shipChargeEntity.getChargePoid(),
                        shipChargeEntity.getChargeCode(),
                        shipChargeEntity.getChargeCode(),
                        shipChargeEntity.getChargePoid(),
                        shipChargeEntity.getChargeName().concat(" - ").concat(shipChargeEntity.getDivisionCode()),
                        shipChargeEntity.getSeqNo()
                );
            }
        }

        DetailsDto chargeCatPoidDet = null;
        if(entity.getChargeCatPoid() != null) {
            ShipChargeGroupEntity shipChargeGroupEntity = shipChargeGroupMap.get(entity.getChargeCatPoid());
            if (shipChargeGroupEntity != null) {
                chargeCatPoidDet = new DetailsDto(
                        shipChargeGroupEntity.getChargeGroupPoid(),
                        shipChargeGroupEntity.getChargeGroupCode(),
                        shipChargeGroupEntity.getChargeGroupName(),
                        shipChargeGroupEntity.getChargeGroupPoid(),
                        shipChargeGroupEntity.getChargeGroupName(),
                        shipChargeGroupEntity.getSeqNo()
                );
            }
        }

        DetailsDto outputTaxPoidDet = null;
        if(entity.getOutputTaxPoid() != null) {
           TaxMaster taxMaster = taxMasterMap.get(entity.getOutputTaxPoid());
           if (taxMaster != null) {
               outputTaxPoidDet = new DetailsDto(
                       taxMaster.getTaxPoid(),
                       taxMaster.getTaxCode(),
                       taxMaster.getTaxName(),
                       taxMaster.getTaxPoid(),
                       taxMaster.getTaxName(),
                       taxMaster.getSeqNo()
               );
           }
        }

        DetailsDto inputTaxPoidDet = null;
        if(entity.getInputTaxPoid() != null) {
            TaxMaster taxMaster = taxMasterMap.get(entity.getInputTaxPoid());
            if (taxMaster != null) {
                inputTaxPoidDet = new DetailsDto(
                        taxMaster.getTaxPoid(),
                        taxMaster.getTaxCode(),
                        taxMaster.getTaxName(),
                        taxMaster.getTaxPoid(),
                        taxMaster.getTaxName(),
                        taxMaster.getSeqNo()
                );
            }
        }

        return TaxPeriodChargeDtlResponseDto.builder()
                .detRowId(entity.getDetRowId())
                .chargePoid(entity.getChargePoid())
                .chargePoidDet(chargePoidDet)
                .chargeCatPoid(entity.getChargeCatPoid())
                .chargeCatPoidDet(chargeCatPoidDet)
                .outputTaxPoid(entity.getOutputTaxPoid())
                .outputTaxPoidDet(outputTaxPoidDet)
                .inputTaxPoid(entity.getInputTaxPoid())
                .inputTaxPoidDet(inputTaxPoidDet)
                .remarks(entity.getRemarks())
                .build();
    }

    @Override
    public Page<TaxPeriodStockDtlResponseDto> getTaxPeriodStocks(Long transactionPoid, Pageable pageable) {
       Page<GlobalTaxPeriodStockDtlEntity> stockDtlEntities = globalTaxPeriodStockDtlRepository
               .findByTransactionPoid(transactionPoid, pageable);
       
        // Collect all unique IDs
        Set<Long> stockPoids = stockDtlEntities.getContent().stream()
                .map(GlobalTaxPeriodStockDtlEntity::getStockPoid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Set<Long> stockCatPoids = stockDtlEntities.getContent().stream()
                .map(GlobalTaxPeriodStockDtlEntity::getStockCatPoid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Set<Long> taxPoids = stockDtlEntities.getContent().stream()
                .flatMap(entity -> Stream.of(entity.getTaxPoid(), entity.getInputTaxPoid()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // Batch fetch all details
        Map<Long, StockMasterEntity> stockMasterMap = stockMasterRepository.findByStockPoidIn(stockPoids)
                .stream().collect(Collectors.toMap(StockMasterEntity::getStockPoid, Function.identity()));
        
        Map<Long, StockCategoryMasterEntity> stockCategoryMap = stockCategoryMasterRepository.findByCategoryPoidIn(stockCatPoids)
                .stream().collect(Collectors.toMap(StockCategoryMasterEntity::getCategoryPoid, Function.identity()));
        
        Map<Long, TaxMaster> taxMasterMap = taxMasterRepository.findByTaxPoidIn(taxPoids)
                .stream().collect(Collectors.toMap(TaxMaster::getTaxPoid, Function.identity()));
        
        return stockDtlEntities.map(entity -> convertToStockDto(entity, stockMasterMap, stockCategoryMap, taxMasterMap));
    }

    private TaxPeriodStockDtlResponseDto convertToStockDto(GlobalTaxPeriodStockDtlEntity entity,
                                                           Map<Long, StockMasterEntity> stockMasterMap,
                                                           Map<Long, StockCategoryMasterEntity> stockCategoryMap,
                                                           Map<Long, TaxMaster> taxMasterMap) {
        DetailsDto stockPoidDet = null;
        if(entity.getStockPoid() != null) {
            StockMasterEntity stockMasterEntity = stockMasterMap.get(entity.getStockPoid());
            if (stockMasterEntity != null) {
                stockPoidDet = new DetailsDto(
                        stockMasterEntity.getStockPoid(),
                        stockMasterEntity.getStockCode(),
                        stockMasterEntity.getStockName(),
                        stockMasterEntity.getStockPoid(),
                        stockMasterEntity.getStockName(),
                        stockMasterEntity.getSeqNo()
                );
            }
        }

        DetailsDto stockCatPoidDet = null;
        if(entity.getStockCatPoid() != null) {
            StockCategoryMasterEntity stockCategoryMasterEntity = stockCategoryMap.get(entity.getStockCatPoid());
            if (stockCategoryMasterEntity != null) {
                stockCatPoidDet = new DetailsDto(
                        stockCategoryMasterEntity.getCategoryPoid(),
                        stockCategoryMasterEntity.getCategoryCode(),
                        stockCategoryMasterEntity.getCategoryName(),
                        stockCategoryMasterEntity.getCategoryPoid(),
                        stockCategoryMasterEntity.getCategoryName(),
                        stockCategoryMasterEntity.getSeqNo()
                );
            }
        }

        DetailsDto taxPoidDet = null;
        if(entity.getTaxPoid() != null) {
            TaxMaster taxMaster = taxMasterMap.get(entity.getTaxPoid());
            if (taxMaster != null) {
                taxPoidDet = new DetailsDto(
                        taxMaster.getTaxPoid(),
                        taxMaster.getTaxCode(),
                        taxMaster.getTaxName(),
                        taxMaster.getTaxPoid(),
                        taxMaster.getTaxName(),
                        taxMaster.getSeqNo()
                );
            }
        }

        DetailsDto inputTaxPoidDet = null;
        if(entity.getInputTaxPoid() != null) {
            TaxMaster taxMaster = taxMasterMap.get(entity.getInputTaxPoid());
            if (taxMaster != null) {
                inputTaxPoidDet = new DetailsDto(
                        taxMaster.getTaxPoid(),
                        taxMaster.getTaxCode(),
                        taxMaster.getTaxName(),
                        taxMaster.getTaxPoid(),
                        taxMaster.getTaxName(),
                        taxMaster.getSeqNo()
                );
            }
        }

        return TaxPeriodStockDtlResponseDto.builder()
                .detRowId(entity.getDetRowId())
                .stockCatPoid(entity.getStockCatPoid())
                .stockCatPoidDet(stockCatPoidDet)
                .stockPoid(entity.getStockPoid())
                .stockPoidDet(stockPoidDet)
                .outputTaxPoid(entity.getTaxPoid())
                .outputTaxPoidDet(taxPoidDet)
                .inputTaxPoid(entity.getInputTaxPoid())
                .inputTaxPoidDet(inputTaxPoidDet)
                .remarks(entity.getRemarks())
                .build();
    }

    @Override
    public String copyTaxPeriod(Long transactionPoid, Long companyPoid, Long userPoid) {
        if (!taxPeriodHdrRepository.existsById(transactionPoid)) {
            throw new ResourceNotFoundException("Tax Period not found for id: ", "transactionPoid", transactionPoid);
        }
        
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_TAX_PERIOD_COPY")
                .registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);
        
        query.setParameter("P_COMPANY_POID", companyPoid);
        query.setParameter("P_USER_POID", userPoid);
        query.setParameter("P_TRANSACTION_POID", transactionPoid);
        
        query.execute();
        
        return (String) query.getOutputParameterValue("P_STATUS");
    }

    public void updateTaxPeriodCharges(List<TaxPeriodChargeDtlRequestDto> charges, Long transactionPoid) {
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        
        List<GlobalTaxPeriodChargeDtlEntity> toSave = new ArrayList<>();
        List<GlobalTaxPeriodChargeDtlEntity> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<GlobalTaxPeriodChargeDtlEntity>> logRequests = new ArrayList<>();
        
        // Group operations by action
        for (TaxPeriodChargeDtlRequestDto charge : charges) {
            switch (charge.getAction().toUpperCase()) {
                case "ISCREATED":
                    toSave.add(GlobalTaxPeriodChargeDtlEntity.builder()
                            .transactionPoid(transactionPoid)
                            .chargePoid(charge.getChargePoid())
                            .chargeCatPoid(charge.getChargeCatPoid())
                            .outputTaxPoid(charge.getOutputTaxPoid())
                            .inputTaxPoid(charge.getInputTaxPoid())
                            .detRowId(charge.getDetRowId())
                            .remarks(charge.getRemarks())
                            .createdBy(currentUser)
                            .createdDate(now)
                            .lastModifiedBy(currentUser)
                            .lastModifiedDate(now)
                            .build());
                    break;
                    
                case "ISUPDATED":
                    GlobalTaxPeriodChargeDtlEntity existingCharge = globalTaxPeriodChargeDtlRepository
                            .findByTransactionPoidAndDetRowId(transactionPoid, charge.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("Charge not found", "detRowId", charge.getDetRowId()));
                    
                    GlobalTaxPeriodChargeDtlEntity oldCharge = new GlobalTaxPeriodChargeDtlEntity();
                    BeanUtils.copyProperties(existingCharge, oldCharge);
                    
                    existingCharge.setChargePoid(charge.getChargePoid());
                    existingCharge.setChargeCatPoid(charge.getChargeCatPoid());
                    existingCharge.setOutputTaxPoid(charge.getOutputTaxPoid());
                    existingCharge.setInputTaxPoid(charge.getInputTaxPoid());
                    existingCharge.setRemarks(charge.getRemarks());
                    existingCharge.setLastModifiedBy(currentUser);
                    existingCharge.setLastModifiedDate(now);
                    toUpdate.add(existingCharge);
                    
                    String logDetailForUpdate = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", oldCharge.getTransactionPoid() ,charge.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldCharge, existingCharge, GlobalTaxPeriodChargeDtlEntity.class, docId, docKeyPoid, logDetailForUpdate));
                    break;
                    
                case "ISDELETED":
                    toDelete.add(charge.getDetRowId());
                    loggingService.logDelete(charge, docId, docKeyPoid);
                    break;
            }
        }
        
        // Batch operations
        if (!toSave.isEmpty()) {
            globalTaxPeriodChargeDtlRepository.saveAll(toSave);
            toSave.forEach(e -> {
                String logDetailForCreated = String.format("Row Created on Tax Charge with detRowId: %s", e.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), UserContext.getDocumentId(), logDetailForCreated);
            });
        }

        if (!toUpdate.isEmpty()) {
            globalTaxPeriodChargeDtlRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }

        if (!toDelete.isEmpty()) {
            globalTaxPeriodChargeDtlRepository.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
        }

    }

    private void updateTaxPeriodStocks(List<TaxPeriodStockDtlRequestDto> stocks, Long transactionPoid) {
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        
        List<GlobalTaxPeriodStockDtlEntity> toSave = new ArrayList<>();
        List<GlobalTaxPeriodStockDtlEntity> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<GlobalTaxPeriodStockDtlEntity>> logRequests = new ArrayList<>();

        // Group operations by action
        for (TaxPeriodStockDtlRequestDto stock : stocks) {
            switch (stock.getAction().toUpperCase()) {
                case "ISCREATED":
                    toSave.add(GlobalTaxPeriodStockDtlEntity.builder()
                            .transactionPoid(transactionPoid)
                            .stockPoid(stock.getStockPoid())
                            .stockCatPoid(stock.getStockCatPoid())
                            .taxPoid(stock.getOutputTaxPoid())
                            .inputTaxPoid(stock.getInputTaxPoid())
                            .detRowId(stock.getDetRowId())
                            .remarks(stock.getRemarks())
                            .createdBy(currentUser)
                            .createdDate(now)
                            .lastModifiedBy(currentUser)
                            .lastModifiedDate(now)
                            .build());
                    break;

                case "ISUPDATED":
                    GlobalTaxPeriodStockDtlEntity existingStock = globalTaxPeriodStockDtlRepository
                            .findByTransactionPoidAndDetRowId(transactionPoid, stock.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("Stock not found", "detRowId", stock.getDetRowId()));

                    GlobalTaxPeriodStockDtlEntity oldStock = new GlobalTaxPeriodStockDtlEntity();
                    BeanUtils.copyProperties(existingStock, oldStock);

                    existingStock.setStockPoid(stock.getStockPoid());
                    existingStock.setStockCatPoid(stock.getStockCatPoid());
                    existingStock.setTaxPoid(stock.getOutputTaxPoid());
                    existingStock.setInputTaxPoid(stock.getInputTaxPoid());
                    existingStock.setRemarks(stock.getRemarks());
                    existingStock.setLastModifiedBy(currentUser);
                    existingStock.setLastModifiedDate(now);
                    toUpdate.add(existingStock);

                    String logDetail = String.format("KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s",oldStock.getTransactionPoid() ,stock.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldStock, existingStock, GlobalTaxPeriodStockDtlEntity.class, docId, docKeyPoid, logDetail));
                    break;

                case "ISDELETED":
                    toDelete.add(stock.getDetRowId());
                    loggingService.logDelete(stock, docId, docKeyPoid);
                    break;
            }
        }

        // Save operations first
        if (!toSave.isEmpty()) {
            globalTaxPeriodStockDtlRepository.saveAll(toSave);
            toSave.forEach(e ->
            {
                String logDetailForCreated = String.format("Row Created on Tax Stock with detRowId: %s", e.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), UserContext.getDocumentId(), logDetailForCreated);
            });
        }

        if (!toUpdate.isEmpty()) {
            globalTaxPeriodStockDtlRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
        if (!toDelete.isEmpty()) {
            globalTaxPeriodStockDtlRepository.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
        }
    }

    @Override
    public Map<String, Object> listTaxPeriod(String documentId, FilterRequestDto request, Pageable pageable,LocalDate periodFrom, LocalDate periodTo) {
        if((periodFrom == null && periodTo != null) || (periodFrom != null && periodTo == null)) {
            throw new ValidationException("Both startDate and endDate should be specified or both dates should be empty.");
        }
        validatePeriodDates(periodFrom, periodTo);
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveDateFilters(request,"TRANSACTION_DATE",periodFrom, periodTo);

        RawSearchResult raw = documentService.search(documentId, filters, operator, pageable, isDeleted,
                "TRANSACTION_POID",
                "DOC_REF");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private void validatePeriodDates(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("Period From must not be after Period To");
        }
    }

    private void validatePeriodOverlap(LocalDate periodFrom, LocalDate periodTo) {
        if (taxPeriodHdrRepository.existsOverlappingPeriod(periodFrom, periodTo)) {
            throw new ValidationException("Tax period overlaps with existing period");
        }
    }

    private void validatePeriodOverlapForUpdate(LocalDate periodFrom, LocalDate periodTo, Long transactionPoid) {
        if (taxPeriodHdrRepository.existsOverlappingPeriodExcluding(periodFrom, periodTo, transactionPoid)) {
            throw new ValidationException("Tax period overlaps with existing period");
        }
    }
}
