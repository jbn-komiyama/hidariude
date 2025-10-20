package dto;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.UUID;

public class CustomerContactDTO implements Serializable {
	private static final long serialVersionUID = 1L;

	private UUID id;
	private String mail;
	private String password;
	private UUID customerId;
	private CustomerDTO customerDTO;
	private String name;
	private String nameRuby;
	private String phone;
	private String department;
	private boolean isPrimary;
	private Timestamp createdAt;
	private Timestamp updatedAt;
	private Timestamp deletedAt;
	private Timestamp lastLoginAt;

	public UUID getCustomerId() {
		return customerId;
	}

	public void setCustomerId(UUID customerId) {
		this.customerId = customerId;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public CustomerDTO getCustomerDTO() {
		return customerDTO;
	}

	public void setCustomerDTO(CustomerDTO customerDTO) {
		this.customerDTO = customerDTO;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNameRuby() {
		return nameRuby;
	}

	public void setNameRuby(String nameRuby) {
		this.nameRuby = nameRuby;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public boolean isPrimary() {
		return isPrimary;
	}

	public void setPrimary(boolean primary) {
		isPrimary = primary;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public Timestamp getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Timestamp getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(Timestamp deletedAt) {
		this.deletedAt = deletedAt;
	}

	public Timestamp getLastLoginAt() {
		return lastLoginAt;
	}

	public void setLastLoginAt(Timestamp lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}

}
