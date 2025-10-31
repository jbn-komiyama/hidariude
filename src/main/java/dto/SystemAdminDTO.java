package dto;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * システム管理者（system_admins）テーブルのDTO
 */
public class SystemAdminDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	
    private UUID id;
    private String mail;
    private String password;
    private String name;
    private String nameRuby;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;
    private Timestamp lastLoginAt;
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
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

