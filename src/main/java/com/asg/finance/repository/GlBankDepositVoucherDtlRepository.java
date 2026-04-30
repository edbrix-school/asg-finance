package com.asg.finance.repository;

import com.asg.finance.entity.GlBankDepositVoucherDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GlBankDepositVoucherDtlRepository extends JpaRepository<GlBankDepositVoucherDtl, Long> {
    List<GlBankDepositVoucherDtl> findByTransactionPoid(Long transactionPoid);

    List<GlBankDepositVoucherDtl> findByTransactionPoidOrderByChqSeqNumAsc(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM GlBankDepositVoucherDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowIdByTransactionPoid(
            @org.springframework.data.repository.query.Param("transactionPoid") Long transactionPoid);
}