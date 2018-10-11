package at.ac.wu.asana.tryout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.asana.models.Story;
import com.asana.models.Task;
import com.asana.models.User;
import com.google.api.client.util.DateTime;

public class StructuralDataChange {	
	private DateTime storyCreatedAt;
	private String role;
	private String actor;
	private String assigneeId;
	private String assigneeName;
	private String action;
	private String circle; //location
	private String pathToHere;
	private String taskId;
	private String taskName;
	private String storyId;
	private String workspaceId;
	private Boolean isSubtask;
	private String workspaceName;
	private String projectName;
	private String parentTaskName;
	private String rawDataText;
	private String storyType;
	private String newAssignee;
	private String projectId;
	private DateTime createdAt;
	private DateTime modifiedAt;
	private DateTime completedAt;
	private String storyCreatedById;
	private String storyCreatedByName;
	private String parentTaskId;
	
	/**
	 * 
	 * @param task
	 * @param story
	 * @param me = client
	 */
	public StructuralDataChange(Task task, Story story) {
		storyCreatedAt = story.createdAt;
		storyId = story.id;
		taskId = task.id;
		taskName = task.name;
		pathToHere = getPath(task);
		setStoryCreatedById(story.createdBy.id);
		setStoryCreatedByName(story.createdBy.name);
		if(task.assignee!=null){
			assigneeId = task.assignee.id;
			assigneeName = task.assignee.name;
		}
		isSubtask = (task.parent!=null);
		if(isSubtask){
			parentTaskName = task.parent.name;
			setParentTaskId(task.parent.id);
		}
		rawDataText = story.text;
		storyType = story.type; 
	}

	/**
	 * 
	 * @param task
	 * @param story
	 * @param me = client
	 */
	public StructuralDataChange(Task task, Story story, String me) {
		storyCreatedAt = story.createdAt;
		storyId = story.id;
		taskId = task.id;
		pathToHere = getPath(task);
		setActionAndAssignee(story.text, story.type, me);
		actor = story.createdBy.name;
		isSubtask = (task.parent!=null);
		taskName = task.name;
		if(isSubtask)
			parentTaskName = task.parent.name;
		rawDataText = story.text;
		setMessageType(story.type); 
	}
	
	public static String[] csvHeader(){
		return new String[]{
				"timestamp",
				"createdById", 
				"createdByName",
				"projectName",
				"taskId", 
				"taskName",
				"messageType",
				"storyEventId",
				"projectId",
				"workspaceId",
				"workspaceName",
				"isSubtask",
				"parentTaskId",
				"parentTaskName",
				"pathToHere",
				"rawDataText",
				"date", 
				"time"
				};
	}

	public String[] csvRow(){
		return new String[]{ 
				storyCreatedAt.toString(),
				storyCreatedById,
				storyCreatedByName,
				projectName,
				taskId,
				taskName,
				storyType,
				storyId,
				projectId,				
				workspaceId,
				workspaceName,
				isSubtask.toString(),
				parentTaskId,
				parentTaskName,
				pathToHere,
				rawDataText,
				new SimpleDateFormat("yyyy-MM-dd").format(new Date(storyCreatedAt.getValue())),
				new SimpleDateFormat("hh:mm:ss.SSS").format(new Date(storyCreatedAt.getValue())),};
	}

	public String getAction() {
		return action;
	}

	public String getActor() {
		return actor;
	}

	public String getAssigneeId() {
		return assigneeId;
	}

	public String getAssigneeName() {
		return assigneeName;
	}

	public String getCircle() {
		return circle;
	}
	
	public DateTime getCompletedAt() {
		return completedAt;
	}

	public DateTime getCreatedAt() {
		return createdAt;
	}

	public String getDate(){
		return DateFormat.getInstance().format(storyCreatedAt);
	}

	public DateTime getDateTime() {
		return storyCreatedAt;
	}

	public String getEventId() {
		return storyId;
	}

	public Boolean getIsSubtask() {
		return isSubtask;
	}

	public String getMessageType() {
		return storyType;
	}
	
	public DateTime getModifiedAt() {
		return modifiedAt;
	}

	public String getNewAssignee() {
		return newAssignee;
	}

	public String getParentTask() {
		return parentTaskName;
	}

	public String getPathToHere() {
		return pathToHere;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getRawDataText() {
		return rawDataText;
	}

	public String getRole() {
		return role;
	}

	public String getTaskId() {
		return taskId;
	}

	public String getTaskName() {
		return taskName;
	}

	
	public String getWorkspaceId() {
		return workspaceId;
	}
	
	public String getWorkspaceName() {
		return workspaceName;
	}

	private String parseAction(String text) {
		if(text.startsWith("added"))
			return AsanaActions.ADD;
		else if(text.startsWith("assigned"))
				return AsanaActions.ASSIGN;
		else if(text.startsWith("completed") | 
				text.startsWith("marked this task complete"))
			return AsanaActions.COMPLETE;
		else if(text.startsWith("change"))
			return AsanaActions.CHANGE;
		else if(text.startsWith("removed"))
			return AsanaActions.REMOVE;
		else if(text.startsWith("unassigned"))
			return AsanaActions.UNASSIGN;
		else if(text.startsWith("liked"))
			return AsanaActions.LIKE;
		else if(text.startsWith("marked incomplete"))
			return AsanaActions.INCOMPLETE;
//		else if(text.startsWith("marked"))
//			return AsanaActions.CHANGE;
//		else if(text.startsWith("unmarked")) //date change?
//			return AsanaActions.CHANGE;
		else if(text.startsWith("duplicate")) //date change?
			return AsanaActions.DUPLICATE;
		else if(text.startsWith("move")) 
			return AsanaActions.MOVE;
		else if(text.startsWith("attached")) //date change?
			return AsanaActions.ATTACH;
		else if(text.startsWith("set"))
			return AsanaActions.SET;
//		else throw new RuntimeException("Unkown text:"+text);
		else {
			System.err.println("Unkown text:"+text + ". I will use "+text.split(" ")[0].trim());
			return text.split(" ")[0].trim();
		}
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
			action = AsanaActions.COMMENT;
		}
			
		else throw new RuntimeException("I found a new action type!"+type);
		
	}

	public void setActor(String actor) {
		this.actor = actor;
	}

	public void setAssigneeId(String assigneeId) {
		this.assigneeId = assigneeId;
	}

	public void setAssigneeName(String assigneeName) {
		this.assigneeName = assigneeName;
	}

	public void setCircle(String circle) {
		this.circle = circle;
	}

	public void setCompletedAt(DateTime completedAt) {
		this.completedAt = completedAt;
	}
	
	public void setCreatedAt(DateTime createdAt) {
		this.createdAt = createdAt;
	}
	
	public void setDateTime(DateTime createdAt) {
		this.storyCreatedAt = createdAt;
	}

	public void setEventId(String eventId) {
		this.storyId = eventId;
	}

	public void setIsSubtask(Boolean isSubtask) {
		this.isSubtask = isSubtask;
	}
	
	public void setMessageType(String messageType) {
		this.storyType = messageType;
	}

	public void setModifiedAt(DateTime modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	public void setNewAssignee(String newAssignee) {
		this.newAssignee = newAssignee;
	}

	public void setNewAssignee(User assignee) {
		if(assignee!=null)
		this.assigneeId = assignee.id;
		this.assigneeName = assignee.name;
	}

	public void setParentTask(String parentTask) {
		this.parentTaskName = parentTask;
	}

	public void setPathToHere(String pathToHere) {
		this.pathToHere = pathToHere;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public void setRawDataText(String rawDataText) {
		this.rawDataText = rawDataText;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}
	
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}

	public void setWorkspaceName(String workspaceName) {
		this.workspaceName = workspaceName;
	}

	public static String getPath(Task task) {
		String path = "";
		if(task.parent!=null)
			path += getPath(task.parent) + "/" + task.parent.name + "/" + task.name;
		return path;
	}

	public String getStoryCreatedById() {
		return storyCreatedById;
	}

	public void setStoryCreatedById(String storyCreatedById) {
		this.storyCreatedById = storyCreatedById;
	}

	public String getStoryCreatedByName() {
		return storyCreatedByName;
	}

	public void setStoryCreatedByName(String storyCreatedByName) {
		this.storyCreatedByName = storyCreatedByName;
	}

	public String getParentTaskId() {
		return parentTaskId;
	}

	public void setParentTaskId(String parentTaskId) {
		this.parentTaskId = parentTaskId;
	}

}
