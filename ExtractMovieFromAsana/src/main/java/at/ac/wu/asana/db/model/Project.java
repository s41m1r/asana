package at.ac.wu.asana.db.model;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class Project {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	public Long id;
	
	public String name;
	
	public String workspaceId;
	public String workspaceName;

	
	@OneToMany(mappedBy="project")
	public Collection<Task> tasks;
	
		
	public Project() {
		super();
		this.tasks = new ArrayList<Task>();
	}
	
	public Project(com.asana.models.Project asanaProject, Long pId){
		this.id = pId;
		this.name = asanaProject.name;
	}
	
	public Project(Long id, String name, String workspaceId, Collection<Task> tasks) {
		super();
		this.id = id;
		this.name = name;
		this.workspaceId = workspaceId;
		this.tasks = tasks;
	}


	@Override
	public String toString() {
		return "Project [id=" + id + ", name=" + name + ", workspaceId=" + workspaceId + "]";
	}
	
}
