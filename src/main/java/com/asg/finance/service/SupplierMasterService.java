package com.asg.finance.service;

import com.asg.finance.dto.GlobalLedgerDto;
import com.asg.finance.dto.SupplierMasterDto;
import com.asg.finance.dto.SupplierImportRequestDto;
import com.asg.finance.dto.SupplierImportResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface SupplierMasterService {
    SupplierMasterDto getSupplierMaster(Long supplierPoid);

    void deleteSupplierMaster(Long supplierPoid);

    SupplierMasterDto updateSupplierMaster(Long supplierPoid, SupplierMasterDto supplierMasterDto);

    SupplierMasterDto createSupplierMaster(SupplierMasterDto supplierMasterDto);

    Long createLedger(Long supplierPoid, GlobalLedgerDto request);

    Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, Pageable pageable);

    Long importSuppliersFromExcel(MultipartFile file, Long supplierPoid);

    SupplierImportResponseDto processImportedSuppliers(SupplierImportRequestDto request);
}
