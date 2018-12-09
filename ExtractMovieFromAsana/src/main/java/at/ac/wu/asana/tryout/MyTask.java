package at.ac.wu.asana.tryout;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.asana.models.Task;

public class MyTask extends Task {
	
	List<Task> parentsList;

	public MyTask() {
		super();
		parentsList = new ArrayList<Task>();
	}
	
	public MyTask(List<Task> parentsList) {
		super();
		this.parentsList = parentsList;
	}
	
	public void addParent(Task parent){
		parentsList.add(parent);
	}

	public List<Task> getParentsList() {
		return parentsList;
	}

	public void setParentsList(List<Task> parentsList) {
		this.parentsList = parentsList;
	}
	
	public String getParentsPath(){
		String path = "";
		Stack<Task> parentsReverse = new Stack<Task>();
		parentsReverse.addAll(parentsList);
		path = parentsReverse.pop().name;
		while(!parentsReverse.isEmpty()){
			path += "/" + parentsReverse.pop().name;
		}
		return path;
	}
	
}
