package domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AssignmentGroup implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private Secretary secretary;
	
	private List<Assignment> assignments = new ArrayList<>();
	
	
	public Secretary getSecretary() { 
		return secretary; 
	}
	  
	public void setSecretary(Secretary secretary) { 
		this.secretary = secretary; 
	}
	public List<Assignment> getAssignments() { 
		return assignments; 
	}
	public void setAssignments(List<Assignment> assignments) { 
		this.assignments = assignments; 
	}
}
