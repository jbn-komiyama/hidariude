package domain;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class Secretary  implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID id;
    private String secretaryCode;
    private String mail;
    private String password;
    private UUID secretaryRankId;
    private boolean pmSecretary;
    private String name;
    private String nameRuby;
    private String phone;
    private String postalCode;
    private String address1;
    private String address2;
    private String building;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;
    private Date lastLoginAt;
    private String bankName;
    private String bankBranch;
    private String bankType;
    private String bankAccount;
    private String bankOwner;

    
	// relation
    private SecretaryRank secretaryRank;

    public Secretary() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSecretaryCode() { return secretaryCode; }
    public void setSecretaryCode(String secretaryCode) { this.secretaryCode = secretaryCode; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public UUID getSecretaryRankId() { return secretaryRankId; }
    public void setSecretaryRankId(UUID secretaryRankId) { this.secretaryRankId = secretaryRankId; }

    public boolean isPmSecretary() { return pmSecretary; }
    public void setPmSecretary(boolean pmSecretary) { this.pmSecretary = pmSecretary; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameRuby() { return nameRuby; }
    public void setNameRuby(String nameRuby) { this.nameRuby = nameRuby; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getAddress1() { return address1; }
    public void setAddress1(String address1) { this.address1 = address1; }

    public String getAddress2() { return address2; }
    public void setAddress2(String address2) { this.address2 = address2; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }

    public Date getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Date lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public SecretaryRank getSecretaryRank() { return secretaryRank; }
    public void setSecretaryRank(SecretaryRank secretaryRank) { this.secretaryRank = secretaryRank; }

    public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getBankBranch() {
		return bankBranch;
	}

	public void setBankBranch(String bankBranch) {
		this.bankBranch = bankBranch;
	}

	public String getBankType() {
		return bankType;
	}

	public void setBankType(String bankType) {
		this.bankType = bankType;
	}

	public String getBankAccount() {
		return bankAccount;
	}

	public void setBankAccount(String bankAccount) {
		this.bankAccount = bankAccount;
	}

	public String getBankOwner() {
		return bankOwner;
	}

	public void setBankOwner(String bankOwner) {
		this.bankOwner = bankOwner;
	}
}