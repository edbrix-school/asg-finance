package com.asg.finance.repository;

import com.asg.finance.entity.ArGenReceiptAdvanceDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArGenReceiptAdvanceDtlRepository extends JpaRepository<ArGenReceiptAdvanceDtl, Long> {

    /**
     * Find all advance details for a given receipt transaction
     */
    List<ArGenReceiptAdvanceDtl> findByReceiptHdr_TransactionPoid(Long transactionPoid);

    /**
     * Find advance detail by transaction POID and detail row ID
     */
    Optional<ArGenReceiptAdvanceDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    /**
     * Delete all advance details for a given receipt transaction
     */
    @Modifying
    @Query("DELETE FROM ArGenReceiptAdvanceDtl d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    /**
     * Delete advance details by transaction POID and detail row IDs
     */
    @Modifying
    @Query("DELETE FROM ArGenReceiptAdvanceDtl d WHERE d.transactionPoid = :transactionPoid AND d.detRowId IN :detRowIds")
    void deleteByTransactionPoidAndDetRowIdIn(@Param("transactionPoid") Long transactionPoid, @Param("detRowIds") List<Long> detRowIds);

    long countByTransactionPoid(Long transactionPoid);
}

