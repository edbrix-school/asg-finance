package com.asg.finance.repository;

import com.asg.finance.entity.GlBankDepositVoucherHdr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GlBankDepositVoucherHdrRepository extends JpaRepository<GlBankDepositVoucherHdr, Long>, BankDepositVoucherProcRepository {
    Optional<GlBankDepositVoucherHdr> findByTransactionPoid(Long transactionPoid);
}
