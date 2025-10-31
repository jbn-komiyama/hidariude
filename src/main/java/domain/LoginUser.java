package domain;

import java.io.Serializable;

/**
 * ログインユーザー情報を保持するドメインモデル
 * 権限に応じて秘書、システム管理者、顧客、顧客担当者のいずれかの情報を保持します
 */
public class LoginUser implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private int authority;
	private Secretary secretary;
	private SystemAdmin systemAdmin;
	private Customer customer;
	private CustomerContact customerContact;
	
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
	public CustomerContact getCustomerContact() {
		return customerContact;
	}
	public void setCustomerContact(CustomerContact customerContact) {
		this.customerContact = customerContact;
	}
	
	public Customer getCustomer() {
		return customer;
	}
	public void setCustomer(Customer customer) {
		this.customer = customer;
	}
	
}
