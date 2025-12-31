package com.asg.finance.repository;


import com.asg.finance.entity.GlBankPaymentItemDtlEntity;
import com.asg.finance.entity.key.GlBankPaymentItemDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlBankPaymentItemDtlRepository extends JpaRepository<GlBankPaymentItemDtlEntity, GlBankPaymentItemDtlId> {

    @Query("SELECT e FROM GlBankPaymentItemDtlEntity e WHERE e.transactionPoid = :transactionPoid")
    List<GlBankPaymentItemDtlEntity> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

}