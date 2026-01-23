package com.asg.finance.repository;

import com.asg.finance.entity.GlFavAcMasterGlAcDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for GL_FAV_AC_MASTER_GL_AC_DTL table
 * Repository for GL account detail records
 */
@Repository
public interface GlFavAcMasterGlAcDtlRepository extends JpaRepository<GlFavAcMasterGlAcDtl, GlFavAcMasterGlAcDtl.CompositeKey> {
    
    List<GlFavAcMasterGlAcDtl> findByFavAcPoid(Long favAcPoid);
    
    @Query("SELECT g FROM GlFavAcMasterGlAcDtl g WHERE g.favAcPoid = :favAcPoid ORDER BY g.seqNo ASC")
    List<GlFavAcMasterGlAcDtl> findByFavAcPoidOrderBySeqNo(@Param("favAcPoid") Long favAcPoid);
    
    @Modifying
    @Query("DELETE FROM GlFavAcMasterGlAcDtl g WHERE g.favAcPoid = :favAcPoid")
    void deleteByFavAcPoid(@Param("favAcPoid") Long favAcPoid);
    
    @Query("SELECT g FROM GlFavAcMasterGlAcDtl g WHERE g.favAcPoid = :favAcPoid AND g.detRowId = :detRowId")
    java.util.Optional<GlFavAcMasterGlAcDtl> findByFavAcPoidAndDetRowId(@Param("favAcPoid") Long favAcPoid, @Param("detRowId") Long detRowId);

}

