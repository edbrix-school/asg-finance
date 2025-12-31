package com.asg.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class GLMasterCompanyDtlId implements java.io.Serializable {
    @Column(name = "GL_POID")
    private Long glPoid;

    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    // equals & hashCode, getters & setters
}
