package com.asg.finance.repository;

import com.asg.finance.entity.ArGenReceiptChargesDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArGenReceiptChargesDtlRepository extends JpaRepository<ArGenReceiptChargesDtl, Long> {

    /**
     * Find all charges details for a given receipt transaction
     */
    List<ArGenReceiptChargesDtl> findByReceiptHdr_TransactionPoid(Long transactionPoid);

    /**
     * Find charge detail by transaction POID and detail row ID
     */
    Optional<ArGenReceiptChargesDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    /**
     * Delete all charges details for a given receipt transaction
     */
    @Modifying
    @Query("DELETE FROM ArGenReceiptChargesDtl d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    /**
     * Delete charge details by transaction POID and detail row IDs
     */
    @Modifying
    @Query("DELETE FROM ArGenReceiptChargesDtl d WHERE d.transactionPoid = :transactionPoid AND d.detRowId IN :detRowIds")
    void deleteByTransactionPoidAndDetRowIdIn(@Param("transactionPoid") Long transactionPoid, @Param("detRowIds") List<Long> detRowIds);

    long countByTransactionPoid(Long transactionPoid);
}

