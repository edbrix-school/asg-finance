package com.asg.finance.repository;

import com.asg.finance.entity.AdvancePettyCashHdr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdvancePettyCashHdrRepository extends JpaRepository<AdvancePettyCashHdr, Long> {
    Optional<AdvancePettyCashHdr> findByTransactionPoid(Long transactionPoid);
    boolean existsByDocRef(String docRef);
    boolean existsByDocRefAndTransactionPoidNot(String docRef, Long transactionPoid);
    boolean existsByTransactionPoid(Long transactionPoid);
}