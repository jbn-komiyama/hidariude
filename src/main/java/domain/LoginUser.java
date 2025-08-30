package domain;

import java.io.Serializable;
import java.util.UUID;
public class LoginUser implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private int authority;
	private Secretary secretary;
	private SystemAdmin systemAdmin;
	private Customer customer;
	
	public int getAuthority() {
		return authority;
	}
	public void setAuthority(int authority) {
		this.authority = authority;
	}
	public Secretary getSecretary() {
		return secretary;
	}
	public void setSecretary(Secretary secretary) {
		this.secretary = secretary;
	}
	public SystemAdmin getSystemAdmin() {
		return systemAdmin;
	}
	public void setSystemAdmin(SystemAdmin systemAdmin) {
		this.systemAdmin = systemAdmin;
	}
	public Customer getCustomer() {
		return customer;
	}
	public void setCustomer(Customer customer) {
		this.customer = customer;
	}
	
}
