package at.ac.wu.asana.model;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

import com.asana.models.Story;
import com.asana.models.Tag;
import com.asana.models.Task;
import com.asana.models.User;
import com.google.api.client.util.DateTime;

public class StructuralDataChange {	
	
	final Logger logger = Logger.getLogger(this.getClass().getName());
	
	private DateTime storyCreatedAt;
	private DateTime taskCreatedAt;
	private DateTime taskModifiedAt;
	private DateTime taskCompletedAt;
	private boolean isRole;
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
	private boolean isSubtask;
	private String workspaceName;
	private String projectName;
	private String parentTaskName;
	private String rawDataText;
	private String messageType;
	private String newAssignee;
	private String projectId;
	private DateTime createdAt;
	private DateTime modifiedAt;
	private DateTime completedAt;
	private String storyCreatedById;
	private String storyCreatedByName;
	private String parentTaskId;
	private String taskNotes;
	private String taskTags;
	private int typeOfChange;
	private String typeOfChangeDescription;
	private boolean isCircle;
	
	public StructuralDataChange() {
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
		taskName = task.name;
		taskCreatedAt = task.createdAt;
		taskCompletedAt = task.completedAt;
		taskModifiedAt = task.modifiedAt;
		setActionAndAssignee(story.text, story.type, me);
		setTaskTags(extractTaskTags(task.tags));
		setTaskNotes(task.notes);
//		pathToHere = getPath(task);
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
		setRole(taskName);
		this.isCircle = isCircle(task.name);
		rawDataText = story.text;
		messageType = story.type;
		typeOfChange = typeOfChange(story.text, messageType);
		typeOfChangeDescription = AsanaActions.codeToString(typeOfChange);
	}
	
	private void setRole(String taskName2) {
		if(messageType == null)
			return;
		if(messageType.equals(AsanaActions.codeToString(AsanaActions.CREATE_ROLE)) ||
				messageType.equals(AsanaActions.codeToString(AsanaActions.LAST_MODIFY_ROLE)) ||
				messageType.equals(AsanaActions.codeToString(AsanaActions.COMPLETE_ROLE)))
			this.isRole = false;
		
		if(taskName2 == null || taskName2.isEmpty())
			this.isRole = false;
		
		else this.isRole = true;
	}

	private String extractTaskTags(Collection<Tag> tags) {
		String res = "";
		for (Tag tag : tags) {
			res+= " "+tag.name;
		}
		return res;
	}
	/**
	 * 
	 * @param task
	 * @param story
	 * @param me = client
	 */
//	public StructuralDataChange(Task task, Story story, String me) {
//		storyCreatedAt = story.createdAt;
//		storyId = story.id;
//		taskId = task.id;
//		pathToHere = getPath(task);
//		setActionAndAssignee(story.text, story.type, me);
//		actor = story.createdBy.name;
//		isSubtask = (task.parent!=null);
//		taskName = task.name;
//		if(isSubtask)
//			parentTaskName = task.parent.name;
//		rawDataText = story.text;
//		setMessageType(story.type); 
//	}
	
	public StructuralDataChange(Task task, DateTime eventTimestamp, int typeOfChange) {
		storyCreatedAt = eventTimestamp;
		taskId = task.id;
		taskName = task.name;
		taskCreatedAt = task.createdAt;
		taskCompletedAt = task.completedAt;
		taskModifiedAt = task.modifiedAt;
//		pathToHere = getPath(task);
		
		// at some point also set action
		this.typeOfChange = typeOfChange;
		this.typeOfChangeDescription = AsanaActions.codeToString(typeOfChange);
		
		if(task.assignee!=null){
			assigneeId = task.assignee.id;
			assigneeName = task.assignee.name;
		}
		isSubtask = (task.parent!=null);
		if(isSubtask){
			parentTaskName = task.parent.name;
			setParentTaskId(task.parent.id);
		}
		setRole(isRole());
		setTaskTags(extractTaskTags(task.tags));
		setTaskNotes(task.notes);
	}
	
	public static String[] csvHeader(){
		return new String[]{
				"timestamp",
				"taskId",
				"parentTaskId",
				"taskName",
				"rawDataText",
				"messageType",
				"typeOfChange",
				"typeOfChangeDescription",
				"isRole",
				"taskCreatedAt", 
				"createdByName",
				"projectName",
				"isCicle",
				"createdById",
				"assigneeId",
				"assigneeName",
				"eventId",
				"projectId",
				"workspaceId",
				"workspaceName",
				"isSubtask",
				"parentTaskName",
//				"pathToHere",
				"date", 
				"time",
				"taskCompletedAt",
				"taskModifiedAt",
				"taskNotes"
				};
	}

	public String[] csvRow(){
		return new String[]{ 
				new Timestamp(storyCreatedAt.getValue()).toString(),
				taskId,
				parentTaskId,
				taskName,
				rawDataText,
				messageType,
				typeOfChange+"",
				typeOfChangeDescription,
				isRole+"",
				new Timestamp(taskCreatedAt.getValue()).toString(),
				storyCreatedByName,				
				projectName,
				isCircle+"",
				storyCreatedById,
				assigneeId,
				assigneeName,
				storyId,
				projectId,				
				workspaceId,
				workspaceName,
				isSubtask+"",
				parentTaskName,
				new SimpleDateFormat("yyyy-MM-dd").format(new Date(storyCreatedAt.getValue())),
				new SimpleDateFormat("hh:mm:ss.SSS").format(new Date(storyCreatedAt.getValue())),
				((taskCompletedAt!= null)? taskCompletedAt.toString():""),
				((taskModifiedAt != null)? taskModifiedAt.toString(): ""),
				taskNotes
				};
	}
	
	public static StructuralDataChange fromString(String[] row){
		StructuralDataChange sdc = new StructuralDataChange();
				
		sdc.storyCreatedAt = DateTime.parseRfc3339(row[0].replace(' ', 'T'));
		sdc.createdAt = DateTime.parseRfc3339(row[0].replace(' ', 'T'));
		sdc.taskId = row[1];	
		sdc.parentTaskId = row[2];
		sdc.taskName = row[3];
		sdc.rawDataText = row[4];
		sdc.messageType = row[5];
		sdc.typeOfChange = Integer.parseInt(row[6]);
		sdc.typeOfChangeDescription = row[7];
		sdc.isRole = Boolean.parseBoolean(row[8]);
		sdc.taskCreatedAt = DateTime.parseRfc3339(row[9].replace(' ', 'T'));
		sdc.storyCreatedByName = row[10];
		sdc.projectName = row[11];
		sdc.isCircle = Boolean.parseBoolean(row[12]);
		sdc.storyCreatedById = row[13];
		sdc.assigneeId = row[14];
		sdc.assigneeName = row[15];
		sdc.storyId = row[16];
		sdc.projectId = row[17];				
		sdc.workspaceId = row[18];
		sdc.workspaceName = row[19];
		sdc.isSubtask = Boolean.parseBoolean(row[20]);
		sdc.parentTaskName = row[21];
		
		return sdc;
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
		return messageType;
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

	public String getParentTaskId() {
		return parentTaskId;
	}

	public String getParentTaskName() {
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

	public DateTime getStoryCreatedAt() {
		return storyCreatedAt;
	}

	public String getStoryCreatedById() {
		return storyCreatedById;
	}

	public String getStoryCreatedByName() {
		return storyCreatedByName;
	}

	public String getStoryId() {
		return storyId;
	}

	public String getStoryType() {
		return messageType;
	}
	
	public DateTime getTaskCompletedAt() {
		return taskCompletedAt;
	}

	public DateTime getTaskCreatedAt() {
		return taskCreatedAt;
	}

	public String getTaskId() {
		return taskId;
	}

	public DateTime getTaskModifiedAt() {
		return taskModifiedAt;
	}

	public String getTaskName() {
		return taskName;
	}

	public int getTypeOfChange() {
		return typeOfChange;
	}

	public void setTypeOfChange(int typeOfChange) {
		this.typeOfChange = typeOfChange;
	}

	public String getTypeOfChangeDescription() {
		return typeOfChangeDescription;
	}

	public void setTypeOfChangeDescription(String typeOfChangeDescription) {
		this.typeOfChangeDescription = typeOfChangeDescription;
	}

	public String getWorkspaceId() {
		return workspaceId;
	}

	public String getWorkspaceName() {
		return workspaceName;
	}

	/**
	 * if taskName is empty --> not a role
	 * @return 1 if it is role, 0 otherwise
	 */
	public Boolean isRole() {
		if(taskName==null || taskName.isEmpty())
			return false;
		else 
			return true;
	}

	public void setRole(Boolean role) {
		this.isRole = role;
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
	
	private int typeOfChange(String text, String messageType) {
		if(text.startsWith("added subtask to task") ||
				text.startsWith("added the description") ||
				text.startsWith("changed the description") ||
				text.startsWith("removed from") ||
				text.startsWith("removed the description") ||
				text.startsWith("changed the name to") ||
				text.startsWith("duplicated task from") ||
				text.startsWith("marked this task complete"))
			return AsanaActions.CHANGE_ACCOUNTABILITY_PURPOSE;
		
		else if(text.startsWith("added subtask to task"))
				return AsanaActions.ADD_SUB_ROLE;
		
		else if(text.startsWith("assigned to") ||
				text.startsWith("assigned the task"))
			return AsanaActions.ASSIGN_TO_ACTOR;
		
		else if(text.startsWith("unassigned from") ||
				text.startsWith("unassigned the task"))
			return AsanaActions.UNASSIGN_FROM_ACTOR;
		
		else if(text.startsWith("added to") && 
				text.toLowerCase().endsWith("roles"))
			return AsanaActions.ADD_TO_CIRCLE;
		
		else if(text.startsWith("removed from") && 
				text.toLowerCase().endsWith("roles"))
			return AsanaActions.REMOVE_FROM_CIRCLE;
		
		else if(text.startsWith("liked") || 
				messageType.equals("comment"))
			return AsanaActions.COMMENT;
		
		else if(text.startsWith("completed this task") ||
				text.startsWith("marked this task complete"))
			return AsanaActions.DETELE_OR_MARK_COMPLETE;
		
		else if(text.startsWith("marked incomplete") ||
				text.startsWith("marked this task incomplete"))
			return AsanaActions.REVIVE_OR_MARK_INCOMPLETE;

		else if(text.startsWith("changed the name to") ||
				text.startsWith("added the name")) 
			return AsanaActions.CHANGE_NAME_OF_ROLE;
		
		else if(text.startsWith("duplicated task from") || 
				text.matches("added.*follower") ||
				(text.contains("moved into") && !text.toLowerCase().endsWith("roles")) ||
				text.startsWith("moved from") ||
				(text.startsWith("removed from") && !text.toLowerCase().endsWith("roles")) ||
				text.startsWith("marked today") ||
				text.startsWith("unmarked today") || 
				text.startsWith("removed the due date")
				) 
			return AsanaActions.IGNORE_OR_DELETE;

		else {
			logger.warning("Unkown text: "+text + 
					". I will use "+
					AsanaActions.codeToString(AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK) +
					" = "+AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK);
			return AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK;
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
			action = AsanaActions.MAKE_COMMENT;
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
		this.messageType = messageType;
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
	
	public void setParentTaskId(String parentTaskId) {
		this.parentTaskId = parentTaskId;
	}

	public void setParentTaskName(String parentTaskName) {
		this.parentTaskName = parentTaskName;
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

	public void setStoryCreatedAt(DateTime storyCreatedAt) {
		this.storyCreatedAt = storyCreatedAt;
	}

	public void setStoryCreatedById(String storyCreatedById) {
		this.storyCreatedById = storyCreatedById;
	}

	public void setStoryCreatedByName(String storyCreatedByName) {
		this.storyCreatedByName = storyCreatedByName;
	}

	public void setStoryId(String storyId) {
		this.storyId = storyId;
	}
	
	public void setStoryType(String storyType) {
		this.messageType = storyType;
	}

	public void setTaskCompletedAt(DateTime taskCompletedAt) {
		this.taskCompletedAt = taskCompletedAt;
	}

	public void setTaskCreatedAt(DateTime taskCreatedAt) {
		this.taskCreatedAt = taskCreatedAt;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public void setTaskModifiedAt(DateTime taskModifiedAt) {
		this.taskModifiedAt = taskModifiedAt;
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
	public String getTaskNotes() {
		return taskNotes;
	}
	public void setTaskNotes(String taskNotes) {
		this.taskNotes = taskNotes;
	}
	public String getTaskTags() {
		return taskTags;
	}
	public void setTaskTags(String taskTags) {
		this.taskTags = taskTags;
	}

	public void setRole(boolean isRole) {
		this.isRole = isRole;
	}
	
	/**
	 * 
	 * 1- it is a project && 2- It starts with a smiley and ends with roles 
	 * 
	 * @return
	 */
	public boolean isCircle() {
		if(projectName.contains("☺") && projectName.toLowerCase().endsWith("roles"))
			return true;
		else 
			return false;
	}
	
	public static boolean isCircle(String name) {
		if(name.contains("☺") && name.toLowerCase().endsWith("roles"))
			return true;
		else 
			return false;
	}

	public void setCircle(boolean isCircle) {
		this.isCircle = isCircle;
	}

}
