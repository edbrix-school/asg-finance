package com.asg.finance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

@Data
public class GLMasterRequestDto {
    private String glCode;
    private String description;
    private String description2;
    private String type;            // GL_TYPE
    @JsonAlias({"groupGlPoid"})
    private Long subOf;
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

