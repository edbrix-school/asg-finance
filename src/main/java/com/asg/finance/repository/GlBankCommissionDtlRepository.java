package com.asg.finance.repository;

import com.asg.finance.entity.GlBankCommissionDtlEntity;
import com.asg.finance.entity.key.GlBankCommissionDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlBankCommissionDtlRepository extends JpaRepository<GlBankCommissionDtlEntity, GlBankCommissionDtlKey> {
    List<GlBankCommissionDtlEntity> findByBankPoid(Long bankPoid);

    Optional<GlBankCommissionDtlEntity> findByBankPoidAndDetRowId(Long bankPoid, Long detRowId);

    void deleteByBankPoid(Long bankPoid);
}