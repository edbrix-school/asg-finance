package com.asg.finance.repository;

import com.asg.finance.entity.SupplierMasterQstnDtlEntity;
import com.asg.finance.entity.key.SupplierMasterQstnDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierMasterQstnRepository extends JpaRepository<SupplierMasterQstnDtlEntity, SupplierMasterQstnDtlKey> {
    @Query("SELECT s FROM SupplierMasterQstnDtlEntity s WHERE s.id.supplierPoid = :supplierPoid")
    List<SupplierMasterQstnDtlEntity> findBySupplierPoid(@Param("supplierPoid") Long supplierPoid);

    void deleteByIdSupplierPoid(Long supplierPoid);

    @Query("SELECT s FROM SupplierMasterQstnDtlEntity s WHERE s.id.supplierPoid = :supplierPoid AND s.id.detRowId = :detRowId")
    SupplierMasterQstnDtlEntity findBySupplierPoidAndDetRowId(@Param("supplierPoid") Long supplierPoid, @Param("detRowId") Long detRowId);

    @Query("SELECT COALESCE(MAX(s.id.detRowId), 0) FROM SupplierMasterQstnDtlEntity s WHERE s.id.supplierPoid = :supplierPoid")
    Long findMaxDetRowIdBySupplierPoid(@Param("supplierPoid") Long supplierPoid);
}