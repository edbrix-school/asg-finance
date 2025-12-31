package com.asg.finance.repository;

import com.asg.finance.entity.SupplierMasterManagementDtlEntity;
import com.asg.finance.entity.key.SupplierMasterMangementDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierMasterMangementDtlRepository extends JpaRepository<SupplierMasterManagementDtlEntity, SupplierMasterMangementDtlKey> {
    @Query("SELECT s FROM SupplierMasterManagementDtlEntity s WHERE s.id.supplierPoid = :supplierPoid")
    List<SupplierMasterManagementDtlEntity> findBySupplierPoid(@Param("supplierPoid") Long supplierPoid);

    void deleteByIdSupplierPoid(Long supplierPoid);

    @Query("SELECT s FROM SupplierMasterManagementDtlEntity s WHERE s.id.supplierPoid = :supplierPoid AND s.id.detRowId = :detRowId")
    SupplierMasterManagementDtlEntity findBySupplierPoidAndDetRowId(@Param("supplierPoid") Long supplierPoid, @Param("detRowId") Long detRowId);

    @Query("SELECT COALESCE(MAX(s.id.detRowId), 0) FROM SupplierMasterManagementDtlEntity s WHERE s.id.supplierPoid = :supplierPoid")
    Long findMaxDetRowIdBySupplierPoid(@Param("supplierPoid") Long supplierPoid);
}