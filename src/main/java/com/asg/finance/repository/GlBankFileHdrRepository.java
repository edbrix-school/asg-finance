package com.asg.finance.repository;

import com.asg.finance.entity.GlBankFileHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlBankFileHdrRepository extends JpaRepository<GlBankFileHdr, Long> {
    Optional<GlBankFileHdr> findByTransactionPoid(Long transactionPoid);
}