package at.ac.wu.asana.tryout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.asana.models.Story;
import com.asana.models.Task;
import com.google.api.client.util.DateTime;

public class StructuralDataChange {
	
	public static String getPath(Task task) {
		String path = "";
		if(task.parent!=null)
			path += getPath(task.parent) + "/" + task.parent.name + "/" + task.name;
		return path;
	}
	
	private DateTime dateTime;
	private String role;
	private String actor;
	private String action;
	private String circle; //location
	private String pathToHere;
	private String taskId;
	private String taskName;
	private String eventId;
	private String workspaceId;
	private Boolean isSubtask;
//	private Boolean isProbableRole;
	private String workspaceName;
	private String projectName;
	private String parentTask;
	private String rawDataText;
	private String messageType;

	public String getParentTask() {
		return parentTask;
	}

	public void setParentTask(String parentTask) {
		this.parentTask = parentTask;
	}

	public String getWorkspaceName() {
		return workspaceName;
	}

	public void setWorkspaceName(String workspaceName) {
		this.workspaceName = workspaceName;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	private String newAssignee;

	private String projectId;
	/**
	 * 
	 * @param task
	 * @param story
	 * @param me = client
	 */
	public StructuralDataChange(Task task, Story story, String me) {
		dateTime = story.createdAt;
		eventId = story.id;
		taskId = task.id;
		pathToHere = getPath(task);
		setActionAndAssignee(story.text, story.type, me);
		actor = story.createdBy.name;
		isSubtask = (task.parent!=null);
		taskName = task.name;
		if(isSubtask)
			parentTask = task.parent.name;
		rawDataText = story.text;
		setMessageType(story.type); 
	}
	
	public static String[] csvHeader(){
		return new String[]{"date", "time", "role",
				"actor","action", "taskId", 
				"taskName",
				"newAssignee",
				"eventId",
				"projectId",
				"projectName",
				"workspaceId",
				"workspaceName",
				"isSubtask",
				"parentTask",
				"pathToHere",
				"rawDataText",
				"messageType",
				"timestamp"};
	}

	public String[] csvRow(){
		return new String[]{ 
				new SimpleDateFormat("yyyy-MM-dd").format(new Date(dateTime.getValue())),
				new SimpleDateFormat("hh:mm:ss.SSS").format(new Date(dateTime.getValue())),
				role,
				actor,
				action,
				taskId,
				taskName,
				newAssignee,
				eventId,
				projectId,
				projectName,				
				workspaceId,
				workspaceName,
				isSubtask.toString(),
				parentTask,
				pathToHere,
				rawDataText,
				messageType,
				dateTime.toString()};
	}

	public String getAction() {
		return action;
	}

	public String getActor() {
		return actor;
	}

	public String getCircle() {
		return circle;
	}

	public String getDate(){
		return DateFormat.getInstance().format(dateTime);
	}

	public DateTime getDateTime() {
		return dateTime;
	}

	public String getEventId() {
		return eventId;
	}

	public Boolean getIsSubtask() {
		return isSubtask;
	}

	public String getNewAssignee() {
		return newAssignee;
	}

	
	public String getPathToHere() {
		return pathToHere;
	}
	
	public String getProjectId() {
		return projectId;
	}

	public String getRole() {
		return role;
	}

	public String getTaskId() {
		return taskId;
	}

	public String getWorkspaceId() {
		return workspaceId;
	}

	private String parseAction(String text) {
		if(text.startsWith("added"))
			return AsanaActions.CREATE;
		else if(text.startsWith("assigned"))
				return AsanaActions.ASSIGN;
		else if(text.startsWith("completed") | 
				text.startsWith("marked this task complete"))
			return AsanaActions.COMPLETE;//delete role?
		else if(text.startsWith("change"))
			return AsanaActions.CHANGE;
		else if(text.startsWith("removed"))
			return AsanaActions.REMOVE;
		else if(text.startsWith("unassigned"))
			return AsanaActions.UNASSIGN;
		else if(text.startsWith("liked"))
			return AsanaActions.LIKE;
//		else if(text.startsWith("marked incomplete"))
//			return AsanaActions.INCOMPLETE;
		else if(text.startsWith("marked"))
			return AsanaActions.CHANGE;
		else if(text.startsWith("unmarked")) //date change?
			return AsanaActions.CHANGE;
		else if(text.startsWith("duplicate")) //date change?
			return AsanaActions.CHANGE;
		else if(text.startsWith("move")) //date change?
			return AsanaActions.MOVE;
		else throw new RuntimeException("Unkown text:"+text);
	}

	private String parseAssignee(String text) {
		if(text.startsWith("assigned to")){
			String[] split = text.split("assigned to");
			return split[1].trim();
		}
		return null;
	}

	public void setAction(String action) {
		this.action = action;
	}

	private void setActionAndAssignee(String text, String type, String me) {
		if(type.equals("system")){
			action = parseAction(text);
			newAssignee = parseAssignee(text);
			if(newAssignee!=null && newAssignee.equals("you"))
				newAssignee=""+me;
		}
		else if(type.equals("comment")){
			action = AsanaActions.CHANGE;
		}
			
		else throw new RuntimeException("I found a new action type!"+type);
		
	}

	public void setActor(String actor) {
		this.actor = actor;
	}

	public void setCircle(String circle) {
		this.circle = circle;
	}
	
	public void setDateTime(DateTime createdAt) {
		this.dateTime = createdAt;
	}
	
	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public void setIsSubtask(Boolean isSubtask) {
		this.isSubtask = isSubtask;
	}

	public void setNewAssignee(String newAssignee) {
		this.newAssignee = newAssignee;
	}
	
	public void setPathToHere(String pathToHere) {
		this.pathToHere = pathToHere;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getRawDataText() {
		return rawDataText;
	}

	public void setRawDataText(String rawDataText) {
		this.rawDataText = rawDataText;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

}
