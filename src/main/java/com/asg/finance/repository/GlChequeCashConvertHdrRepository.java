package com.asg.finance.repository;

import com.asg.finance.entity.GlChequeCashConvertHdrEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;



@Repository
public interface GlChequeCashConvertHdrRepository extends JpaRepository<GlChequeCashConvertHdrEntity, Long> {

    GlChequeCashConvertHdrEntity findByTransactionPoid(Long transactionPoid);

    void deleteByTransactionPoid(Long transactionPoid);

}
