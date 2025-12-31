package com.asg.finance.repository;

import com.asg.finance.entity.GlBankDebitHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlBankDebitHdrRepository extends JpaRepository<GlBankDebitHdr, Long> {

    @Query("SELECT h FROM GlBankDebitHdr h WHERE h.transactionPoid = :transactionPoid AND (h.deleted IS NULL OR h.deleted != 'Y')")
    Optional<GlBankDebitHdr> findByTransactionPoidAndNotDeleted(@Param("transactionPoid") Long transactionPoid);

    boolean existsByDocRefIgnoreCaseAndTransactionPoidNot(String docRef, Long transactionPoid);
    boolean existsByDocRefIgnoreCase(String docRef);

    Optional<GlBankDebitHdr> findByTransactionPoid(Long transactionPoid);
}
