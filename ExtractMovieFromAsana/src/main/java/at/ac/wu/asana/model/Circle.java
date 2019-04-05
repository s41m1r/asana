package at.ac.wu.asana.model;

import com.google.api.client.util.DateTime;

public class Circle {
	String id;
	String name;
	String parentId;
	String parentName;
	DateTime creationTime;
	DateTime deletionTime;
	String matchingProjectName;
	
	public Circle() {
	}

	public Circle(String id, String name, DateTime creationTime, DateTime deletionTime) {
		super();
		this.id = id;
		this.name = name;
		this.creationTime = creationTime;
		this.deletionTime = deletionTime;
	}
	
	public Circle(String id, String name, String parentId, String parentName) {
		super();
		this.id = id;
		this.name = name;
		this.parentId = parentId;
		this.parentName = parentName;
	}

	public Circle(String id, String name) {
		super();
		this.id = id;
		this.name = name;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DateTime getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(DateTime creationTime) {
		this.creationTime = creationTime;
	}

	public DateTime getDeletionTime() {
		return deletionTime;
	}

	public void setDeletionTime(DateTime deletionTime) {
		this.deletionTime = deletionTime;
	}

	public String getMatchingProjectName() {
		return matchingProjectName;
	}

	public void setMatchingProjectName(String matchingProjectName) {
		this.matchingProjectName = matchingProjectName;
	}

	@Override
	public String toString() {
		return "Circle [id=" + id + ", name=" + name + "]";
	}
	
	public boolean matches(String name) {
		if(!name.toLowerCase().endsWith("roles"))
			return false;
		if(!name.startsWith("☺"))
			return false;
		String[] tokens = this.name.split(" ");
		int nummatches = 0;
		for (String t : tokens) {
			if(name.contains(t))
				nummatches++;
		}
		if(nummatches == 0)
			return false;
		
		return true;
	}
}
