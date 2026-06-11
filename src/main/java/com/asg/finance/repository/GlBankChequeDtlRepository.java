package com.asg.finance.repository;

import com.asg.finance.entity.GlBankChequeDtlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlBankChequeDtlRepository extends JpaRepository<GlBankChequeDtlEntity, Long> {
    List<GlBankChequeDtlEntity> findByBankPoid(Long bankPoid);

    Optional<GlBankChequeDtlEntity> findByBankPoidAndDetRowId(Long bankPoid, Long detRowId);

    @Query("SELECT COALESCE(MAX(g.detRowId), 0) FROM GlBankChequeDtlEntity g WHERE g.bankPoid = :bankPoid")
    Long findMaxDetRowIdByBankPoid(@Param("bankPoid") Long bankPoid);

    void deleteByBankPoid(Long bankPoid);
}