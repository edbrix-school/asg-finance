package com.asg.finance.repository;

import com.asg.finance.entity.GlBankChequeDtlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlBankChequeDtlRepository extends JpaRepository<GlBankChequeDtlEntity, Long> {
    List<GlBankChequeDtlEntity> findByBankPoid(Long bankPoid);

    Optional<GlBankChequeDtlEntity> findByBankPoidAndDetRowId(Long bankPoid, Long detRowId);

    void deleteByBankPoid(Long bankPoid);
}