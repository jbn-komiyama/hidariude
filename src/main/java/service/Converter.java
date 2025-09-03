// Converter.java
package service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import domain.Assignment;
import domain.Customer;
import domain.CustomerContact;
import domain.Secretary;
import domain.SecretaryRank;
import domain.Task;
import domain.TaskRank;
import dto.AssignmentDTO;
import dto.CustomerContactDTO;
import dto.CustomerDTO;
import dto.SecretaryDTO;
import dto.SecretaryRankDTO;
import dto.TaskDTO;
import dto.TaskRankDTO;

/**
 * DTO -> Domain 変換クラス。
 * 基本方針:
 *  - 引数が null の場合は null を返す
 *  - 日付系はユーティリティ（ts2date / sql2date）に集約
 */
public class Converter {

    // -------------------------
    // Customer
    // -------------------------
	public Customer toDomain(CustomerDTO dto) {
        if (dto == null) return null;

		Customer s = new Customer();
        s.setId(dto.getId());
        s.setCompanyCode(dto.getCompanyCode());
        s.setCompanyName(dto.getCompanyName());
        s.setMail(dto.getMail());
        s.setPhone(dto.getPhone());
        s.setPostalCode(dto.getPostalCode());
        s.setAddress1(dto.getAddress1());
        s.setAddress2(dto.getAddress2());
        s.setBuilding(dto.getBuilding());
        s.setPrimaryContactId(dto.getPrimaryContactId());
        s.setCreatedAt(ts2date(dto.getCreatedAt()));
        s.setUpdatedAt(ts2date(dto.getUpdatedAt()));
        s.setDeletedAt(ts2date(dto.getDeletedAt()));

        List<CustomerContact> list = new ArrayList<>();
        if (dto.getCustomerContacts() != null) {
            for (CustomerContactDTO c : dto.getCustomerContacts()) {
                list.add(toDomain(c));
            }
        }
        s.setCustomerContacts(list);

        return s;
	}

	public CustomerContact toDomain(CustomerContactDTO c) {
	    if (c == null) return null;
	    CustomerContact customerContact = new CustomerContact();
	    customerContact.setId(c.getId());
	    customerContact.setMail(c.getMail());
	    customerContact.setPassword(c.getPassword());
	    if (c.getCustomerDTO() != null) {
	        customerContact.setCustomerId(c.getCustomerDTO().getId());
	    }
	    customerContact.setName(c.getName());
	    customerContact.setNameRuby(c.getNameRuby());
	    customerContact.setPhone(c.getPhone());
	    customerContact.setDepartment(c.getDepartment());
	    customerContact.setPrimary(c.isPrimary());
	    customerContact.setCreatedAt(ts2date(c.getCreatedAt()));
	    customerContact.setUpdatedAt(ts2date(c.getUpdatedAt()));
	    customerContact.setDeletedAt(ts2date(c.getDeletedAt()));
	    customerContact.setLastLoginAt(ts2date(c.getLastLoginAt()));
	    return customerContact;
	}

    // -------------------------
    // Secretary / Rank
    // -------------------------
	public Secretary toDomain(SecretaryDTO dto) {
        if (dto == null) return null;

		Secretary s = new Secretary();
        s.setId(dto.getId());
        s.setSecretaryCode(dto.getSecretaryCode());
        s.setMail(dto.getMail());
        s.setPassword(dto.getPassword());
        s.setPmSecretary(dto.isPmSecretary());
        s.setName(dto.getName());
        s.setNameRuby(dto.getNameRuby());
        s.setPhone(dto.getPhone());
        s.setPostalCode(dto.getPostalCode());
        s.setAddress1(dto.getAddress1());
        s.setAddress2(dto.getAddress2());
        s.setBuilding(dto.getBuilding());
        s.setCreatedAt(ts2date(dto.getCreatedAt()));
        s.setUpdatedAt(ts2date(dto.getUpdatedAt()));
        s.setDeletedAt(ts2date(dto.getDeletedAt()));
        s.setLastLoginAt(ts2date(dto.getLastLoginAt()));

        s.setSecretaryRank(toDomain(dto.getSecretaryRankDTO()));
        return s;
	}

	public SecretaryRank toDomain(SecretaryRankDTO r) {
        if (r == null) return null;
        SecretaryRank rank = new SecretaryRank();
        rank.setId(r.getId());
        rank.setRankName(r.getRankName());
        rank.setDescription(r.getDescription());
        rank.setIncreaseBasePayCustomer(r.getIncreaseBasePayCustomer());
        rank.setIncreaseBasePaySecretary(r.getIncreaseBasePaySecretary());
        rank.setCreatedAt(ts2date(r.getCreatedAt()));
        rank.setUpdatedAt(ts2date(r.getUpdatedAt()));
        rank.setDeletedAt(ts2date(r.getDeletedAt()));
        return rank;
    }

    // -------------------------
    // Assignment / TaskRank
    // -------------------------
	public Assignment toDomain(AssignmentDTO dto) {
		if (dto == null) return null;

        Assignment assignment = new Assignment();
        assignment.setId(dto.getAssignmentId());
        assignment.setCustomerId(dto.getAssignmentCustomerId());
        assignment.setSecretaryId(dto.getAssignmentSecretaryId());
        assignment.setTaskRankId(dto.getTaskRankId());
        assignment.setTargetYearMonth(dto.getTargetYearMonth());
        assignment.setBasePayCustomer(dto.getBasePayCustomer());
        assignment.setBasePaySecretary(dto.getBasePaySecretary());
        assignment.setIncreaseBasePayCustomer(dto.getIncreaseBasePayCustomer());
        assignment.setIncreaseBasePaySecretary(dto.getIncreaseBasePaySecretary());
        assignment.setCustomerBasedIncentiveForCustomer(dto.getCustomerBasedIncentiveForCustomer());
        assignment.setCustomerBasedIncentiveForSecretary(dto.getCustomerBasedIncentiveForSecretary());
        assignment.setStatus(dto.getAssignmentStatus());
        assignment.setCreatedBy(dto.getAssignmentCreatedBy());
        assignment.setCreatedAt(dto.getAssignmentCreatedAt() == null ? null : dto.getAssignmentCreatedAt().toLocalDateTime());
        assignment.setUpdatedAt(dto.getAssignmentUpdatedAt() == null ? null : dto.getAssignmentUpdatedAt().toLocalDateTime());
        assignment.setDeletedAt(dto.getAssignmentDeletedAt() == null ? null : dto.getAssignmentDeletedAt().toLocalDateTime());
        assignment.setTaskRankName(dto.getTaskRankName());

        // Customer (summary)
        Customer customer = new Customer();
        customer.setId(dto.getCustomerId());
        customer.setCompanyCode(dto.getCustomerCompanyCode());
        customer.setCompanyName(dto.getCustomerCompanyName());
        assignment.setCustomer(customer);

        // Secretary (summary)
        Secretary secretary = new Secretary();
        secretary.setId(dto.getSecretaryId());
        secretary.setName(dto.getSecretaryName());
        SecretaryRank sr = new SecretaryRank();
        sr.setId(dto.getSecretaryRankId());
        sr.setRankName(dto.getSecretaryRankName());
        secretary.setSecretaryRank(sr);
        assignment.setSecretary(secretary);

        return assignment;
	}

	public TaskRank toDomain(TaskRankDTO dto) {
		if (dto == null) return null;

	    TaskRank taskRank = new TaskRank();
	    taskRank.setId(dto.getId());
	    taskRank.setRankName(dto.getRankName());
	    taskRank.setBasePayCustomer(dto.getBasePayCustomer());
	    taskRank.setBasePaySecretary(dto.getBasePaySecretary());
	    taskRank.setCreatedAt(dto.getCreatedAt() == null ? null : new java.util.Date(dto.getCreatedAt().getTime()));
	    taskRank.setUpdatedAt(dto.getUpdatedAt() == null ? null : new java.util.Date(dto.getUpdatedAt().getTime()));
	    taskRank.setDeletedAt(dto.getDeletedAt() == null ? null : new java.util.Date(dto.getDeletedAt().getTime()));
	    return taskRank;
	}

    // -------------------------
    // ★ Task (NEW)
    // -------------------------
    public Task toDomain(TaskDTO dto) {
        if (dto == null) return null;

        Task t = new Task();
        t.setId(dto.getId());

        // assignments.* を含んだ AssignmentDTO を詰め替え
        if (dto.getAssignment() != null) {
            t.setAssignment(toDomain(dto.getAssignment()));
        }

        t.setWorkDate(sql2date(dto.getWorkDate()));
        t.setStartTime(ts2date(dto.getStartTime()));
        t.setEndTime(ts2date(dto.getEndTime()));
        t.setWorkMinute(dto.getWorkMinute());
        t.setWorkContent(dto.getWorkContent());
        t.setApprovedAt(ts2date(dto.getApprovedAt()));

        if (dto.getApprovedBy() != null) {
            t.setApprovedBy(toDomain(dto.getApprovedBy())); // idのみでもOK
        }

        // ※ 月次請求/サマリは Domain クラス定義に合わせて必要なら後で拡張
        t.setCreatedAt(ts2date(dto.getCreatedAt()));
        t.setUpdatedAt(ts2date(dto.getUpdatedAt()));
        t.setDeletedAt(ts2date(dto.getDeletedAt()));
        return t;
    }

    // -------------------------
    // Utilities
    // -------------------------
	private Date ts2date(java.sql.Timestamp ts) {
        return ts == null ? null : new Date(ts.getTime());
    }

    private Date sql2date(java.sql.Date d) {
        return d == null ? null : new Date(d.getTime());
    }
}
