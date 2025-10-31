package dto;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * 秘書（secretaries）テーブルのDTO
 */
public class SecretaryDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private UUID id;
	private String secretaryCode;
	private String mail;
	private String password;
	
	private UUID secretaryRankId;
	private SecretaryRankDTO secretaryRankDTO;
	private boolean isPmSecretary;
	
	private String name;
	private String nameRuby;
	private String phone;
	private String postalCode;
	private String address1;
	private String address2;
	private String building;
	
	private Timestamp createdAt;
	private Timestamp updatedAt;
	private Timestamp deletedAt;
	private Timestamp lastLoginAt;

    private String bankName;
    private String bankBranch;
    private String bankType;
    private String bankAccount;
    private String bankOwner;
	
	
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public String getSecretaryCode() {
		return secretaryCode;
	}
	public void setSecretaryCode(String secretaryCode) {
		this.secretaryCode = secretaryCode;
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
	public SecretaryRankDTO getSecretaryRankDTO() {
		return secretaryRankDTO;
	}
	public void setSecretaryRankDTO(SecretaryRankDTO secretaryRankDTO) {
		this.secretaryRankDTO = secretaryRankDTO;
	}
	public boolean isPmSecretary() {
		return isPmSecretary;
	}
	public void setPmSecretary(boolean isPmSecretary) {
		this.isPmSecretary = isPmSecretary;
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
	public String getPostalCode() {
		return postalCode;
	}
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}
	public String getAddress1() {
		return address1;
	}
	public void setAddress1(String address1) {
		this.address1 = address1;
	}
	public String getAddress2() {
		return address2;
	}
	public void setAddress2(String address2) {
		this.address2 = address2;
	}
	public String getBuilding() {
		return building;
	}
	public void setBuilding(String building) {
		this.building = building;
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
	public UUID getSecretaryRankId() {
		return secretaryRankId;
	}
	public void setSecretaryRankId(UUID secretaryRankId) {
		this.secretaryRankId = secretaryRankId;
	}
	
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