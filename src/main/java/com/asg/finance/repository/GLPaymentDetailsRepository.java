package com.asg.finance.repository;

import com.asg.finance.entity.GLPaymentDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GLPaymentDetailsRepository extends JpaRepository<GLPaymentDetailsEntity, GLPaymentDetailsEntity.CompositeKey> {
    Optional<GLPaymentDetailsEntity> findByGlMaster_GlPoid(Long glPoid);
    Optional<GLPaymentDetailsEntity> findByGlPoidAndId(Long glPoid, Long id);
    void deleteByGlPoidAndIdIn(Long glPoid, List<Long> id);
    List<GLPaymentDetailsEntity> findAllByGlMaster_GlPoid(Long glPoid);
}