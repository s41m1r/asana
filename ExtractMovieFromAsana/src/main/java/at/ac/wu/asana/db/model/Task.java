package at.ac.wu.asana.db.model;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
public class Task {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	public Integer id;
	
	public String asanaId;
	
	public String name;
	
	@ManyToOne
	public Project project;
	
	@ManyToOne
	public Task parent;
	
	@OneToMany(mappedBy="task")
	public Collection<Story> storyLines;
	
	public Task() {
		storyLines = new ArrayList<Story>();
	}
	
	public Task(com.asana.models.Task apiTask) {
		asanaId = apiTask.id;
		name = apiTask.name;
		
		storyLines = new ArrayList<Story>();
	}

	@Override
	public String toString() {
		return "Task [id=" + id + ", name=" + name + ", project=" + project + "]";
	}

}
