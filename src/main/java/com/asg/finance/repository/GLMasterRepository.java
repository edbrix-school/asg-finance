package com.asg.finance.repository;

import com.asg.finance.entity.GLMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GLMasterRepository extends JpaRepository<GLMaster, Long> {
    Optional<GLMaster> findByGlPoid(Long poid);

    boolean existsByGlCodeIgnoreCase(String glCode);

    boolean existsByGlPoid(Long glPoid);

    List<GLMaster> findByGlPoidIn(List<Long> glPoids);

    List<GLMaster> findByGlPoidIn(Collection<Long> glPoids);
}
