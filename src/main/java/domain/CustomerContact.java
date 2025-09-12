package domain;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * 顧客担当者（customer_contacts）のドメイン
 */
public class CustomerContact implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- columns ---
    private UUID id;
    private String mail;
    private String password;

    // FK: customers.id（必要に応じて Customer をぶら下げるなら customer も持たせる）
    private UUID customerId;
    private Customer customer; 

    private String name;
    private String nameRuby;
    private String phone;
    private String department;
    private boolean isPrimary;

    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;
    private Date lastLoginAt;

    // --- getters / setters ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameRuby() { return nameRuby; }
    public void setNameRuby(String nameRuby) { this.nameRuby = nameRuby; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean isPrimary) { this.isPrimary = isPrimary; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }

    public Date getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Date lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}