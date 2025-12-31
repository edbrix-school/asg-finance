package com.asg.finance.repository;

import com.asg.finance.entity.GlBankDepositVoucherDtl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GlBankDepositVoucherDtlRepository extends JpaRepository<GlBankDepositVoucherDtl, Long> {
    List<GlBankDepositVoucherDtl> findByTransactionPoid(Long transactionPoid);
    void deleteByTransactionPoid(Long transactionPoid);
}
