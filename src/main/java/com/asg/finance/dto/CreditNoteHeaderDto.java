package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import com.asg.finance.validation.ValidCreditNote;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
@ValidCreditNote
public class CreditNoteHeaderDto {
    private Long transactionPoid;
    private LocalDate transactionDate;
    private String docRef;

    @NotBlank(message = "Party Type is required")
    private String partyType;

    @NotNull(message = "Party Poid is required")
    private Long partyPoid;

    @NotBlank(message = "Reference type is mandatory")
    private String refType;

    private String currencyCode;
    private BigDecimal currencyRate;
    private BigDecimal bhdAmount;
    /**
     * Reference-specific field(s)
     * When Ref Type is General or Custom
     * private String voyageRef;
     *
     * As per SRS not needed.
     */

    @NotBlank(message = "Posting narration required")
    private String postingNarration;

    private Long creditPeriod;
    private LocalDate dueDate;
    private String tinNumber;

    private String billRefType;
    private Boolean printableRemarks;
    private String issueType;
    private Boolean multiCompany;

    /**
     * Reference-specific field(s)
     * When Ref Type is SH_INVOICE
     */
    private Long shInvoicePoid;



    /**
     * Reference-specific field(s)
     * When Ref Type is FF_INVOICE
     */
    private Long ffInvoicePoid;

    /**
     * Reference-specific field(s)
     * When Ref Type is DN_INVOICE
     */
    private Long dnInvoicePoid;
    private Long fdaRefPoid;
    private Boolean fdaDirect; // Checkbox for DN ref type

    private String dnFdaReference; // Text field for FDA ref type


    private String remarks;
    private BigDecimal grandTotal;

    /**
     * Auditing Related Fields
     */
    private String createdBy;
    private Timestamp createdDate;
    private String lastModifiedBy;
    private Timestamp lastModifiedDate;

    @Valid
    private List<CreditNoteGLDetailDto> glDetails;
    @Valid
    private List<UniversalChargeDetailDto> chargeDetails;

    private Long bankPoid;

    // Used for Response
    private LovGetListDto partyTypeDet;
    private LovGetListDto partyDet;
}