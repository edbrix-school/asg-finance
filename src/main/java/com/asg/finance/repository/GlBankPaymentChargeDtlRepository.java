package com.asg.finance.repository;


import com.asg.finance.entity.GlBankPaymentChargeDtlEntity;
import com.asg.finance.entity.key.GlBankPaymentChargeDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlBankPaymentChargeDtlRepository extends JpaRepository<GlBankPaymentChargeDtlEntity, GlBankPaymentChargeDtlId> {

    @Query("SELECT e FROM GlBankPaymentChargeDtlEntity e WHERE e.transactionPoid = :transactionPoid")
    List<GlBankPaymentChargeDtlEntity> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

}