package com.asg.finance.repository;

import com.asg.finance.entity.GlBankCommissionDtlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlBankCommissionDtlRepository extends JpaRepository<GlBankCommissionDtlEntity, Long> {
    List<GlBankCommissionDtlEntity> findByBankPoid(Long bankPoid);

    Optional<GlBankCommissionDtlEntity> findByBankPoidAndDetRowId(Long bankPoid, Long detRowId);

    void deleteByBankPoid(Long bankPoid);
}