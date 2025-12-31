package com.asg.finance.repository;

import com.asg.finance.entity.GlFavAcMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for GL_FAV_AC_MASTER table
 * Key Favorite Account Master repository for CRUD operations
 */
@Repository
public interface GlFavAcMasterRepository extends JpaRepository<GlFavAcMaster, Long>, JpaSpecificationExecutor<GlFavAcMaster> {
    
    Optional<GlFavAcMaster> findByFavAcPoid(Long favAcPoid);
    
    Optional<GlFavAcMaster> findByFavAcCodeAndGroupPoid(String favAcCode, Long groupPoid);
    
    Optional<GlFavAcMaster> findByDescriptionAndGroupPoid(String description, Long groupPoid);
    
    List<GlFavAcMaster> findByGroupPoidAndDeletedOrderBySeqNoAsc(Long groupPoid, String deleted);
    
    List<GlFavAcMaster> findByGroupPoidAndActiveAndDeletedOrderBySeqNoAsc(Long groupPoid, String active, String deleted);
}

