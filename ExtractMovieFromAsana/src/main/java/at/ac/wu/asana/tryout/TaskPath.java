package at.ac.wu.asana.tryout;

public class TaskPath {
	public String taskId;
	public String path;
	
	public TaskPath() {
		// TODO Auto-generated constructor stub
	}

	public TaskPath(String taskId, String path) {
		super();
		this.taskId = taskId;
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}
}
