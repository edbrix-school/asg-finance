package com.asg.finance.repository;

import com.asg.finance.entity.SupplierMasterServiceDtlEntity;
import com.asg.finance.entity.key.SupplierMasterServiceDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierMasterServiceDtlRepository extends JpaRepository<SupplierMasterServiceDtlEntity, SupplierMasterServiceDtlKey> {
    @Query("SELECT s FROM SupplierMasterServiceDtlEntity s WHERE s.id.supplierPoid = :supplierPoid")
    List<SupplierMasterServiceDtlEntity> findBySupplierPoid(@Param("supplierPoid") Long supplierPoid);

    void deleteByIdSupplierPoid(Long supplierPoid);

    @Query("SELECT s FROM SupplierMasterServiceDtlEntity s WHERE s.id.supplierPoid = :supplierPoid AND s.id.detRowId = :detRowId")
    SupplierMasterServiceDtlEntity findBySupplierPoidAndDetRowId(@Param("supplierPoid") Long supplierPoid, @Param("detRowId") Long detRowId);

    @Query("SELECT COALESCE(MAX(s.id.detRowId), 0) FROM SupplierMasterServiceDtlEntity s WHERE s.id.supplierPoid = :supplierPoid")
    Long findMaxDetRowIdBySupplierPoid(@Param("supplierPoid") Long supplierPoid);
}