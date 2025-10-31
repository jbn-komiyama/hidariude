package domain;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * システム管理者（system_admins）テーブルのドメインモデル
 */
public class SystemAdmin implements Serializable{
	private static final long serialVersionUID = 1L;
	
    private UUID id;
    private String mail;
    private String password;
    private String name;
    private String nameRuby;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;
    private Date lastLoginAt;
    
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
	public Date getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	public Date getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	public Date getDeletedAt() {
		return deletedAt;
	}
	public void setDeletedAt(Date deletedAt) {
		this.deletedAt = deletedAt;
	}
	public Date getLastLoginAt() {
		return lastLoginAt;
	}
	public void setLastLoginAt(Date lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}

   
}