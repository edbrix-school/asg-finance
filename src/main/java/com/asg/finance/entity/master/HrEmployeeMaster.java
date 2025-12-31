package com.asg.finance.entity.master;

import com.asg.common.lib.entity.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "HR_EMPLOYEE_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrEmployeeMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EMPLOYEE_POID", nullable = false)
    private Long employeePoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "EMPLOYEE_CODE", nullable = false, length = 20)
    private String employeeCode;

    @Column(name = "EMPLOYEE_NAME", nullable = false, length = 100)
    private String employeeName;

    @Column(name = "EMPLOYEE_NAME2", length = 100)
    private String employeeName2;

    @Column(name = "BASEGROUP", length = 20)
    private String baseGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_POID", referencedColumnName = "COMPANY_POID")
    private Company company;

    @Column(name = "LOCATION_POID")
    private Long locationPoid;

    @Lob
    @Column(name = "PHOTO")
    private byte[] photo;

    @Column(name = "JOIN_DATE")
    private LocalDate joinDate;

    @Column(name = "GENDER", length = 20)
    private String gender;

    @Column(name = "MARITAL_STATUS", length = 20)
    private String maritalStatus;

    @Column(name = "DATE_OF_BIRTH")
    private LocalDate dateOfBirth;

    @Column(name = "PRESENT_ADDRESS", length = 500)
    private String presentAddress;

    @Column(name = "PERMANENT_ADDRESS", length = 500)
    private String permanentAddress;

    @Column(name = "POSTAL_ADDRESS", length = 500)
    private String postalAddress;

    @Column(name = "HOME_COUNTRY_PHONE", length = 20)
    private String homeCountryPhone;

    @Column(name = "MOBILE", length = 20)
    private String mobile;

    @Column(name = "PERSONAL_EMAIL", length = 100)
    private String personalEmail;

    @Column(name = "BUSINESS_EMAIL", length = 100)
    private String businessEmail;

    @Column(name = "BLOOD_GROUP", length = 20)
    private String bloodGroup;

    @Column(name = "EMERGENCY_CONTACT_PERSON", length = 100)
    private String emergencyContactPerson;

    @Column(name = "EMERGENCY_CONTACT_NO", length = 20)
    private String emergencyContactNo;

    @Column(name = "SERVICE_START_DATE")
    private LocalDate serviceStartDate;

    @Column(name = "SERVICE_TYPE", length = 20)
    private String serviceType;

    @Column(name = "CONTRACT_START")
    private LocalDate contractStart;

    @Column(name = "CONTRACT_END")
    private LocalDate contractEnd;

    @Column(name = "PROBATION", length = 20)
    private String probation;

    @Column(name = "NOTICE_PERIOD", length = 20)
    private String noticePeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIRECT_SUPERVISOR_POID", referencedColumnName = "EMPLOYEE_POID")
    private HrEmployeeMaster supervisor;

    @Column(name = "LOGIN_USER_POID")
    private Long loginUserPoid;

    @Column(name = "JOB_DESCRIPTION", length = 500)
    private String jobDescription;

    /*@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AIR_SECTOR_POID", referencedColumnName = "AIRSEC_POID")
    private HrAirsectorMaster airSector;*/

    @Column(name = "TICKET_PERIOD", length = 20)
    private String ticketPeriod;

    @Column(name = "NO_OF_TICKETS", length = 20)
    private String noOfTickets;

    @Column(name = "ACCESS_CARD_ISSUED", length = 20)
    private String accessCardIssued;

    @Column(name = "DISCONTINUED", length = 20)
    private String discontinued = "N";

    @Column(name = "DISCONTINUED_DATE")
    private LocalDate discontinuedDate;

    @Column(name = "REASON", length = 200)
    private String reason;

    @Column(name = "OTHER_REASONS", length = 200)
    private String otherReasons;

    @Column(name = "BASIC_SALARY", precision = 20, scale = 3)
    private BigDecimal basicSalary;

    @Column(name = "NET_SALARY", precision = 20, scale = 3)
    private BigDecimal netSalary;

    @Column(name = "CURRENCY_POID")
    private Long currencyPoid;

    @Column(name = "PAYMENT_METHOD", length = 20)
    private String paymentMethod;

    @Column(name = "BANK_POID")
    private Long bankPoid;

    @Column(name = "ACCOUNT_NO", length = 50)
    private String accountNo;

    @Column(name = "IBAN", length = 50)
    private String iban;

    @Column(name = "REGISTERED_SALARY", precision = 20, scale = 3)
    private BigDecimal registeredSalary;

    @Column(name = "LAST_INCREMENT_DATE")
    private LocalDate lastIncrementDate;

    @Column(name = "NEXT_INCREMENT_DATE")
    private LocalDate nextIncrementDate;

    @Column(name = "HOLD_SALARY", precision = 20, scale = 3)
    private BigDecimal holdSalary;

    @Column(name = "HOLD_REASON", length = 200)
    private String holdReason;

    @Column(name = "CR_POID")
    private Long crPoid;

    @Column(name = "PASSPORT_NO", length = 20)
    private String passportNo;

    @Column(name = "ISSUED_DATE")
    private LocalDate issuedDate;

    @Column(name = "EXPIRY_DATE")
    private LocalDate expiryDate;

    @Column(name = "PLACE_OF_ISSUE", length = 20)
    private String placeOfIssue;

    @Column(name = "PASSPORT_POSSESSED_BY", length = 50)
    private String passportPossessedBy;

    @Column(name = "GOSI_NO", length = 50)
    private String gosiNo;

    @Column(name = "GOSI_RETURN_AMOUNT", precision = 20, scale = 3)
    private BigDecimal gosiReturnAmount;

    @Column(name = "CPR_NO", length = 20)
    private String cprNo;

    @Column(name = "CPR_EXPIRY_DATE")
    private LocalDate cprExpiryDate;

    @Column(name = "CPR_OCCUPATION", length = 50)
    private String cprOccupation;

    @Column(name = "MEDICAL_INSURANCE", length = 20)
    private String medicalInsurance;

    @Column(name = "MEDICAL_INSURANCE_EXPIRY_DATE")
    private LocalDate medicalInsuranceExpiryDate;

    @Column(name = "RP_NO", length = 50)
    private String rpNo;

    @Column(name = "RP_START_DATE")
    private LocalDate rpStartDate;

    @Column(name = "RP_EXPIRY_DATE")
    private LocalDate rpExpiryDate;

    @Column(name = "ACTIVE", length = 1)
    private String active = "N";

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "EMPLOYEE_BIOMATRIX_ID", length = 50)
    private String employeeBiomatrixId;

    @Column(name = "OT_APPLICABLE", length = 50)
    private String otApplicable = "1";

    @Column(name = "INSURANCE_NOMINEE", length = 100)
    private String insuranceNominee;

    @Column(name = "INSURANCE_NOMINEE_RELATION", length = 20)
    private String insuranceNomineeRelation;

    @Column(name = "SHIFT_POID")
    private Long shiftPoid;

    @Column(name = "CURRENCY_CODE", length = 10)
    private String currencyCode;

    @Column(name = "DELETED", length = 1)
    private String deleted = "N";

    @Column(name = "DRIVING_LIC_EXP")
    private LocalDate drivingLicExp;

    @Column(name = "NOMINEE_CONTACT_DTL", length = 200)
    private String nomineeContactDtl;

    @Column(name = "DISPLAY_NAME", length = 100)
    private String displayName;

    @Column(name = "EMP_GL_POID")
    private Long empGlPoid;

    @Column(name = "INSU_NOMINEE2", length = 50)
    private String insuNominee2;

    @Column(name = "BANK_REGISTER_EMPLOYEE_NO", length = 100)
    private String bankRegisterEmployeeNo;

    @Column(name = "PROBATION_BENEFITS", length = 1)
    private String probationBenefits = "N";

    @Column(name = "PROBATION_COMPLETED_ON")
    private LocalDate probationCompletedOn;

    @Column(name = "LIFE_INSURANCE", length = 1)
    private String lifeInsurance;

    @Column(name = "ATTENDANCE_FROM_LOG", length = 1)
    private String attendanceFromLog = "N";

    @Column(name = "MANAGEMENT_STAFF", length = 1)
    private String managementStaff = "N";

    @Column(name = "EXT_NO", length = 20)
    private String extNo;

    @Column(name = "ATTENDANCE_LATE_CHECK", length = 1)
    private String attendanceLateCheck = "Y";

    @Column(name = "ATTENDANCE_LUNCH_CHECK", length = 1)
    private String attendanceLunchCheck = "Y";

    @Column(name = "PASSPORT_REMARKS", length = 100)
    private String passportRemarks;

    @Column(name = "PLACE_OF_HIRE", length = 100)
    private String placeOfHire;

    @Column(name = "PETTY_CASH_GL_POID")
    private Long pettyCashGlPoid;

    @Column(name = "ATTENDANCE_CHECK", length = 1)
    private String attendanceCheck = "Y";

    @Column(name = "RECRUITMENT_SOURCE", length = 100)
    private String recruitmentSource;

    @Column(name = "RECRUITMENT_DETAILS", length = 200)
    private String recruitmentDetails;

    @Column(name = "RECRUITMENT_BUDGETED", length = 100)
    private String recruitmentBudgeted;

    @Column(name = "RECRUITMENT_REASON", length = 100)
    private String recruitmentReason;

    @Column(name = "OT1_LIMIT")
    private BigDecimal ot1Limit;

    @Column(name = "OT2_LIMIT")
    private BigDecimal ot2Limit;

    @Column(name = "SHORT_HOURS", length = 1)
    private String shortHours = "N";

    @Column(name = "CODE_OF_CONDUCT_SIGNED", length = 1)
    private String codeOfConductSigned;

    @Column(name = "ORIENTATION", length = 1)
    private String orientation;

    @Column(name = "ORIENTATION_REMARKS", length = 100)
    private String orientationRemarks;

    @Column(name = "FREE_LUNCH", length = 1)
    private String freeLunch;

    @Column(name = "STAFF_ACCOMMODATION", length = 50)
    private String staffAccommodation;

    @Column(name = "ACTUAL_DOB")
    private LocalDate actualDob;
}
