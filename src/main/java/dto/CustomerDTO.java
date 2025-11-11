package dto;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 顧客（customers）テーブルのDTO
 */
public class CustomerDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private UUID customerId;
	private List<AssignmentDTO> assignments = new ArrayList<>();
	private String companyCode;
	private String companyName;
	private String mail;
	private String phone;
	private String postalCode;
	private String address1;
	private String address2;
	private String building;
	private UUID primaryContactId;
    private List<CustomerContactDTO> customerContacts = new ArrayList<>();
	private Timestamp createdAt;
	private Timestamp updatedAt;
	private Timestamp deletedAt;
	
	public UUID getId() {
		return customerId;
	}
	public void setId(UUID id) {
		this.customerId = customerId;
	}
	public String getCompanyCode() {
		return companyCode;
	}
	public void setCompanyCode(String companyCode) {
		this.companyCode = companyCode;
	}
	public String getCompanyName() {
		return companyName;
	}
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}
	public String getMail() {
		return mail;
	}
	public void setMail(String mail) {
		this.mail = mail;
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
	public UUID getPrimaryContactId() {
		return primaryContactId;
	}
	public void setPrimaryContactId(UUID primaryContactId) {
		this.primaryContactId = primaryContactId;
	}
	public List<CustomerContactDTO> getCustomerContacts() { 
		return customerContacts; 
		}
    public void setCustomerContacts(List<CustomerContactDTO> list) { 
    	this.customerContacts = list; 
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
	
	public List<AssignmentDTO> getAssignmentDTOs() { return assignments; }
    public void setAssignmentDTOs(List<AssignmentDTO> assignments) { this.assignments = assignments; }
    public void addAssignmentDTO(AssignmentDTO a) { if (a != null) this.assignments.add(a); }
}

