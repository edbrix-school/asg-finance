package com.asg.finance.repository;

import com.asg.finance.entity.ArGenReceiptChargesDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArGenReceiptChargesDtlRepository extends JpaRepository<ArGenReceiptChargesDtl, Long> {

    /**
     * Find all charges details for a given receipt transaction
     */
    List<ArGenReceiptChargesDtl> findByReceiptHdr_TransactionPoid(Long transactionPoid);

    /**
     * Delete all charges details for a given receipt transaction
     */
    @Modifying
    @Query("DELETE FROM ArGenReceiptChargesDtl d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    long countByTransactionPoid(Long transactionPoid);
}

