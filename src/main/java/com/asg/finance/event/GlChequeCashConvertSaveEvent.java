package com.asg.finance.event;

import org.springframework.context.ApplicationEvent;
import com.asg.finance.entity.GlChequeCashConvertHdrEntity;

public class GlChequeCashConvertSaveEvent extends ApplicationEvent {
    private final GlChequeCashConvertHdrEntity entity;
    private final Long groupPoid;
    private final Long companyPoid;
    private final Long userPoid;
    private final String currentUser;

    public GlChequeCashConvertSaveEvent(Object source, GlChequeCashConvertHdrEntity entity, Long groupPoid, Long companyPoid, Long userPoid, String currentUser) {
        super(source);
        this.entity = entity;
        this.groupPoid = groupPoid;
        this.companyPoid = companyPoid;
        this.userPoid = userPoid;
        this.currentUser = currentUser;
    }

    public GlChequeCashConvertSaveEvent(GlChequeCashConvertHdrEntity entity, Long groupPoid, Long companyPoid, Long userPoid, String currentUser) {
        super(entity);
        this.entity = entity;
        this.groupPoid = groupPoid;
        this.companyPoid = companyPoid;
        this.userPoid = userPoid;
        this.currentUser = currentUser;
    }

    public GlChequeCashConvertHdrEntity getEntity() {
        return entity;
    }

    public Long getGroupPoid() {
        return groupPoid;
    }

    public Long getCompanyPoid() {
        return companyPoid;
    }
    
    public Long getUserPoid() {
        return userPoid;
    }

    public String getCurrentUser() {
        return currentUser;
    }
}
