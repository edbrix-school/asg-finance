package com.asg.finance.repository;

import com.asg.finance.entity.GLMasterCompanyDtlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GLMasterCompanyDtlRepository extends JpaRepository<GLMasterCompanyDtlEntity, GLMasterCompanyDtlEntity.CompositeKey> {

    List<GLMasterCompanyDtlEntity> findByGlPoid(Long glPoid);
    Optional<GLMasterCompanyDtlEntity> findByGlPoidAndId(Long glPoid, Long id);
    void deleteByGlPoidAndIdIn(Long glPoid, List<Long> id);
}
