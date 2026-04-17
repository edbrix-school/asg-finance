package com.asg.finance.event;

import com.asg.finance.dto.PettyCashRequestBase;
import org.springframework.context.ApplicationEvent;

public class PettyCashVoucherSaveEvent extends ApplicationEvent {

    private final PettyCashRequestBase requestDto;
    private final Long transactionPoid;
    private final Long groupPoid;
    private final Long companyPoid;
    private final Long userPoid;
    private final String docId;
    private final String oldRefType;
    private final String oldRefPoid;

    public PettyCashVoucherSaveEvent(Object source, PettyCashRequestBase requestDto,
                                     Long transactionPoid, Long groupPoid,
                                     Long companyPoid, Long userPoid, String docId,
                                     String oldRefType, String oldRefPoid) {
        super(source);
        this.requestDto = requestDto;
        this.transactionPoid = transactionPoid;
        this.groupPoid = groupPoid;
        this.companyPoid = companyPoid;
        this.userPoid = userPoid;
        this.docId = docId;
        this.oldRefType = oldRefType;
        this.oldRefPoid = oldRefPoid;
    }

    public PettyCashRequestBase getRequestDto() { return requestDto; }
    public Long getTransactionPoid() { return transactionPoid; }
    public Long getGroupPoid() { return groupPoid; }
    public Long getCompanyPoid() { return companyPoid; }
    public Long getUserPoid() { return userPoid; }
    public String getDocId() { return docId; }
    public String getOldRefType() { return oldRefType; }
    public String getOldRefPoid() { return oldRefPoid; }
}
