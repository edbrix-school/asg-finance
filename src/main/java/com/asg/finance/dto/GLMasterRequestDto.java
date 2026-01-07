package com.asg.finance.dto;

import lombok.Data;

import java.util.List;

@Data
public class GLMasterRequestDto {
    private String glCode;
    private String description;
    private String description2;
    private String type;            // GL_TYPE
    private Long subOf;              // parent GL_POID
    private String accountType;
    private String controlAcType;
    private String costGroup;
    private Boolean interCompany;
    private Long interCompanyId;
    private String remarks;
    private Integer seqNo;
    private Boolean active;
    private Boolean isKeyFavorite;
    private Boolean billWise;
    private Boolean prepaymentLedger;

    private List<PaymentDetailsDto> paymentDetails;
    private List<CompanyDetailsDto> companyDetails;
}

