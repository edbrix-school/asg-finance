package com.asg.finance.service.impl;

import com.asg.common.lib.dto.request.DocReleaseLockRequestDto;
import com.asg.finance.dto.GlLedgerDTO;
import com.asg.finance.service.GLMasterCustomService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;

@Slf4j
@Service
public class GLMasterCustomServiceImpl implements GLMasterCustomService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Override
    public String acquireLock(DocReleaseLockRequestDto request)  {
        String status = null;

        String sql = "{ call PROC_GLOB_DOC_ACQUIRE_LOCK(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection connection = dataSource.getConnection();
             CallableStatement cs = connection.prepareCall(sql)) {

            cs.setLong(1, request.getLoginGroupPoid());
            cs.setLong(2, request.getLoginCompanyPoid());
            cs.setLong(3, Long.parseLong(request.getLoginUserPoid()));
            cs.setString(4, request.getDocId());

            cs.setLong(5, request.getDocPoidValue());

            cs.setString(6, request.getUserId());

            cs.registerOutParameter(7, java.sql.Types.VARCHAR);

            cs.execute();

            status = cs.getString(7);
        }
        catch (Exception e) {
            log.error(" error : {}", e.getMessage());
        }
        return status;
    }

    @Override
    public String releaseLock(DocReleaseLockRequestDto request)  {
        String status = null;

        String sql = "{ call PROC_GLOB_DOC_RELEASE_LOCK(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection connection = dataSource.getConnection();
             CallableStatement cs = connection.prepareCall(sql)) {

            cs.setLong(1, request.getLoginGroupPoid());
            cs.setLong(2, request.getLoginCompanyPoid());
            cs.setLong(3, Long.parseLong(request.getLoginUserPoid()));
            cs.setString(4, request.getDocId());

            cs.setLong(5, request.getDocPoidValue());

            cs.setString(6, request.getUserId());

            cs.registerOutParameter(7, java.sql.Types.VARCHAR);

            cs.execute();

            status = cs.getString(7);
        }
        catch (Exception e) {
            log.error(" error : {}", e.getMessage());
        }
        return status;
    }
}
