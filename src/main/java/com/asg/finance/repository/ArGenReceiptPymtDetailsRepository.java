package com.asg.finance.repository;

import com.asg.finance.entity.ArGenReceiptPymtDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArGenReceiptPymtDetailsRepository extends JpaRepository<ArGenReceiptPymtDetails, Long> {

    /**
     * Find all payment details for a given receipt transaction
     */
    List<ArGenReceiptPymtDetails> findByReceiptHdr_TransactionPoid(Long transactionPoid);

    /**
     * Delete all payment details for a given receipt transaction
     */
    @Modifying
    @Query("DELETE FROM ArGenReceiptPymtDetails d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    long countByTransactionPoid(Long transactionPoid);
}

