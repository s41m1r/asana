package at.ac.wu.asana.db.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

@Entity
public class Task {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	public Integer id;
	
	public String asanaId;
	
	public String name;
	
	@ManyToOne
	public Project project;
	
	@ManyToOne()
	public Task parent;
	
	@OneToMany(mappedBy="task", cascade=CascadeType.ALL)
	public Collection<Story> storyLines;
	
	public Task() {
		storyLines = new ArrayList<>();
	}
	
	public Task(com.asana.models.Task apiTask) {
		asanaId = apiTask.gid;
		name = apiTask.name;
		
		storyLines = new ArrayList<>();
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((asanaId == null) ? 0 : asanaId.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Task other = (Task) obj;
		if (asanaId == null) {
			if (other.asanaId != null)
				return false;
		} else if (!asanaId.equals(other.asanaId))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Task [id=" + id + ", name=" + name + ", project=" + project + "]";
	}

}
