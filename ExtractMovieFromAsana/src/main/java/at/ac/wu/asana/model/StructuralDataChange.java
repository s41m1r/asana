package at.ac.wu.asana.model;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.logging.Logger;

import com.asana.models.Project;
import com.asana.models.Story;
import com.asana.models.Tag;
import com.asana.models.Task;
import com.asana.models.User;
import com.asana.models.Workspace;
import com.google.api.client.util.DateTime;

public class StructuralDataChange implements Comparable<StructuralDataChange> {	

	final Logger logger = Logger.getLogger(this.getClass().getName());

	private LocalDateTime storyCreatedAt;
	private LocalDateTime taskCreatedAt;
	private LocalDateTime taskModifiedAt;
	private LocalDateTime taskCompletedAt;
	private boolean isRole;
	private String actor;
	private String lastAssigneeId;
	private String lastAssigneeName;
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
	private String currentAssignee;
	private String projectId;
	private LocalDateTime createdAt;
	private LocalDateTime modifiedAt;
	private LocalDateTime completedAt;
	private String storyCreatedById;
	private String storyCreatedByName;
	private String parentTaskId;
	private String taskNotes;
	private String taskTags;
	private int typeOfChange;
	private String typeOfChangeDescription;
	private boolean isCircle;
	private boolean migration;
	private boolean isRenderedAsSeparator;
	private boolean isChangeAccountabilityPurpose;

	private String circleIds;

	private String parentCircle;
	
	private String accordingToCircle;

	public StructuralDataChange() {
	}

	/**
	 * 
	 * @param task
	 * @param story
	 * @param me = client
	 */
	//	public StructuralDataChange(Task task, Story story, String me) {
	//		storyCreatedAt = story.createdAt;
	//		storyId = story.gid;
	//		taskId = task.gid;
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
		DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
		storyCreatedAt = LocalDateTime.parse(eventTimestamp.toString(), formatter);
		taskId = task.gid;
		taskName = task.name;
		taskCreatedAt = LocalDateTime.parse(task.createdAt.toString(), formatter);
		taskCompletedAt = LocalDateTime.parse(task.completedAt.toString(), formatter);
		taskModifiedAt = LocalDateTime.parse(task.modifiedAt.toString(), formatter);
		createdAt=taskCreatedAt;
		completedAt=taskCompletedAt;
		modifiedAt=taskModifiedAt;
		//		pathToHere = getPath(task);

		// at some point also set action
		this.typeOfChange = typeOfChange;
		this.typeOfChangeDescription = AsanaActions.codeToString(typeOfChange);

		if(task.assignee!=null){
			lastAssigneeId = task.assignee.gid;
			lastAssigneeName = task.assignee.name;
		}
		isSubtask = (task.parent!=null);
		if(isSubtask){
			setParentTaskName(task.parent.name);
			setParentTaskId(task.parent.gid);
		}
		setRole(isRole());
		setTaskTags(extractTaskTags(task.tags));
		setTaskNotes(task.notes);
		isRenderedAsSeparator=task.isRenderedAsSeparator;
	}

	/**
	 * 
	 * @param task
	 * @param story
	 * @param me = client
	 */
	public StructuralDataChange(Task task, Story story, String me) {
		DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
		storyCreatedAt = LocalDateTime.parse(task.createdAt.toString(), formatter);		
		storyId = story.gid;
		taskId = task.gid;
		taskName = task.name;
		taskCreatedAt = LocalDateTime.parse(task.createdAt.toString(), formatter);
		taskCompletedAt = LocalDateTime.parse(task.completedAt.toString(), formatter);
		taskModifiedAt = LocalDateTime.parse(task.modifiedAt.toString(), formatter);
		setActionAndAssignee(story.text, story.type, me);
		setTaskTags(extractTaskTags(task.tags));
		setTaskNotes(task.notes);
		//		pathToHere = getPath(task);
		setStoryCreatedById(story.createdBy.gid);
		setStoryCreatedByName(story.createdBy.name);
		if(task.assignee!=null){
			lastAssigneeId = task.assignee.gid;
			lastAssigneeName = task.assignee.name;
		}
		isSubtask = (task.parent!=null);
		if(isSubtask){
			parentTaskName = task.parent.name;
			setParentTaskId(task.parent.gid);
		}
		setRole(taskName);
		this.isCircle = isCircle(task.name);
		rawDataText = story.text;
		messageType = story.type;
		typeOfChange = typeOfChangeFromCodingScheme(story.text, messageType);
		typeOfChangeDescription = AsanaActions.codeToString(typeOfChange);
		isRenderedAsSeparator=task.isRenderedAsSeparator;
	}

	
	public String getAccordingToCircle() {
		return accordingToCircle;
	}

	public void setAccordingToCircle(String accordingToCircle) {
		this.accordingToCircle = accordingToCircle;
	}

	public String[] csvRow(){
		return new String[]{ 
				Timestamp.valueOf(storyCreatedAt).toString(),
				taskId,
				parentTaskId,
				taskName,
				rawDataText,
				messageType,
				typeOfChange+"",
				typeOfChangeDescription,
				isRole+"",
				taskCreatedAt.toString(),
				storyCreatedByName,				
				projectName,
				isCircle+"",
				storyCreatedById,
				currentAssignee,
				lastAssigneeId,
				lastAssigneeName,
				storyId,
				projectId,				
				workspaceId,
				workspaceName,
				isSubtask+"",
				isRenderedAsSeparator+"",
				parentTaskName,
				storyCreatedAt.toLocalDate().toString(),
				storyCreatedAt.toLocalTime().toString(),
				((taskCompletedAt!= null)? taskCompletedAt.toString():""),
				((taskModifiedAt != null)? taskModifiedAt.toString(): ""),
				taskNotes
		};
	}
	
	public String[] csvRowCircle(){
		return new String[]{ 
				Timestamp.valueOf(storyCreatedAt).toString(),
				taskId,
				parentTaskId,
				taskName,
				rawDataText,
				messageType,
				typeOfChange+"",
				typeOfChangeDescription,
				isRole+"",
				Timestamp.valueOf(taskCreatedAt).toString(),
				storyCreatedByName,				
				projectName,
				isCircle+"",
				storyCreatedById,
				currentAssignee,
				lastAssigneeId,
				lastAssigneeName,
				storyId,
				projectId,				
				workspaceId,
				workspaceName,
				isSubtask+"",
				isRenderedAsSeparator+"",
				parentTaskName,
				storyCreatedAt.toLocalDate().toString(),
				storyCreatedAt.toLocalTime().toString(),
				((taskCompletedAt!= null)? taskCompletedAt.toString():""),
				((taskModifiedAt != null)? taskModifiedAt.toString(): ""),
				taskNotes,
				circle,
				circleIds,
				migration+"",
				parentCircle,
				accordingToCircle
		};
	}

	private String extractTaskTags(Collection<Tag> tags) {
		String res = "";
		if(tags!=null) {
			for (Tag tag : tags) {
				res+= " "+tag.name;
			}
		}
		return res;
	}
	public String getAction() {
		return action;
	}
	public String getActor() {
		return actor;
	}

	public String getAssigneeId() {
		return lastAssigneeId;
	}

	public String getAssigneeName() {
		return lastAssigneeName;
	}

	public String getCircle() {
		return circle;
	}
	public LocalDateTime getCompletedAt() {
		return completedAt;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public String getDate(){
		return DateFormat.getInstance().format(storyCreatedAt);
	}
	public LocalDateTime getDateTime() {
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
	public LocalDateTime getModifiedAt() {
		return modifiedAt;
	}

	public String getCircleIds() {
		return circleIds;
	}

	public String getCurrentAssignee() {
		return currentAssignee;
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

	public LocalDateTime getStoryCreatedAt() {
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

	public LocalDateTime getTaskCompletedAt() {
		return taskCompletedAt;
	}

	public LocalDateTime getTaskCreatedAt() {
		return taskCreatedAt;
	}

	public String getTaskId() {
		return taskId;
	}

	public LocalDateTime getTaskModifiedAt() {
		return taskModifiedAt;
	}

	public String getTaskName() {
		return taskName;
	}

	public String getTaskNotes() {
		return taskNotes;
	}

	public String getTaskTags() {
		return taskTags;
	}

	public int getTypeOfChange() {
		return typeOfChange;
	}

	public String getTypeOfChangeDescription() {
		return typeOfChangeDescription;
	}

	public String getWorkspaceId() {
		return workspaceId;
	}

	public String getWorkspaceName() {
		return workspaceName;
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

	public boolean isMigration() {
		return migration;
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

	public String parseCurrentAssignee() {
		if(this.rawDataText.startsWith("assigned to")){
			String[] split = this.rawDataText.split("assigned to");
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
			currentAssignee = parseAssignee(text);
			if(currentAssignee!=null && currentAssignee.equals("you"))
				currentAssignee=""+me;
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
		this.lastAssigneeId = assigneeId;
	}

	public void setAssigneeName(String assigneeName) {
		this.lastAssigneeName = assigneeName;
	}


	public void setCircle(boolean isCircle) {
		this.isCircle = isCircle;
	}

	public void setCircle(String circle) {
		this.circle = circle;
	}

	public void setCircleIds(String commaSeparateIds) {
		circleIds=""+commaSeparateIds;
	}

	public void setCompletedAt(LocalDateTime completedAt) {
		this.completedAt = completedAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public void setDateTime(LocalDateTime createdAt) {
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

	public void setMigration(boolean migration) {
		this.migration = migration;
	}

	public void setModifiedAt(LocalDateTime modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	public void setCurrentAssignee(String newAssignee) {
		this.currentAssignee = newAssignee;
	}

	public void setNewAssignee(User assignee) {
		if(assignee!=null)
			this.lastAssigneeId = assignee.gid;
		this.lastAssigneeName = assignee.name;
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

	public void setRole(boolean isRole) {
		this.isRole = isRole;
	}

	public void setRole(Boolean role) {
		this.isRole = role;
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

	public void setStoryCreatedAt(LocalDateTime storyCreatedAt) {
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

	public void setTaskCompletedAt(LocalDateTime taskCompletedAt) {
		this.taskCompletedAt = taskCompletedAt;
	}

	public void setTaskCreatedAt(LocalDateTime taskCreatedAt) {
		this.taskCreatedAt = taskCreatedAt;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public void setTaskModifiedAt(LocalDateTime taskModifiedAt) {
		this.taskModifiedAt = taskModifiedAt;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public void setTaskNotes(String taskNotes) {
		this.taskNotes = ""+taskNotes;
	}
	public void setTaskTags(String taskTags) {
		this.taskTags = taskTags;
	}
	public void setTypeOfChange(int typeOfChange) {
		this.typeOfChange = typeOfChange;
	}
	public void setTypeOfChangeDescription(String typeOfChangeDescription) {
		this.typeOfChangeDescription = typeOfChangeDescription;
	}
	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}

	public void setWorkspaceName(String workspaceName) {
		this.workspaceName = workspaceName;
	}

	private int typeOfChangeFromCodingScheme(String text, String messageType) {

		if(text.startsWith("added subtask to task"))
			return AsanaActions.ADD_SUB_ROLE;

		else if(text.startsWith("assigned to") ||
				text.startsWith("assigned the task"))
			return AsanaActions.ASSIGN_TO_ACTOR;

		else if(text.startsWith("unassigned from") ||
				text.startsWith("unassigned the task"))
			return AsanaActions.UNASSIGN_FROM_ACTOR;

		else if(text.startsWith("added to ☺") && 
				text.toLowerCase().endsWith("roles"))
			return AsanaActions.ADD_TO_CIRCLE;

		else if(text.startsWith("removed from ☺") && 
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
				text.startsWith("added the name") ||
				text.equals("removed the name")) 
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

		else if(text.startsWith("added subtask to task") ||
				text.startsWith("added the description") ||
				text.startsWith("changed the description") ||
				text.startsWith("removed from") ||
				text.startsWith("removed the description") ||
				text.startsWith("changed the name to") ||
				text.startsWith("duplicated task from") ||
				text.startsWith("marked this task complete"))
			return AsanaActions.CHANGE_ACCOUNTABILITY_PURPOSE;

		else {
			logger.warning("Unkown text: "+text + 
					". I will use "+
					AsanaActions.codeToString(AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK) +
					" = "+AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK);
			return AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK;
		}
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
				"currentAssignee",
				"lastAssigneeId",
				"lastAssigneeName",
				"eventId",
				"projectId",
				"workspaceId",
				"workspaceName",
				"isSubtask",
				"isRenderedAsSeparator",
				"parentTaskName",
				//				"pathToHere",
				"date", 
				"time",
				"taskCompletedAt",
				"taskModifiedAt",
				"taskNotes"
		};
	}

	public static String[] csvHeaderCircle(){
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
				"currentAssignee",
				"lastAssigneeId",
				"lastAssigneeName",
				"eventId",
				"projectId",
				"workspaceId",
				"workspaceName",
				"isSubtask",
				"isRenderedAsSeparator",
				"parentTaskName",
				//				"pathToHere",
				"date", 
				"time",
				"taskCompletedAt",
				"taskModifiedAt",
				"taskNotes",
				"circle",
				"circleIds",
				"migration",
				"parentCircle",
				"accordingToCircle"
		};
	}

	public static StructuralDataChange fromString(String[] row){
		StructuralDataChange sdc = new StructuralDataChange();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		String timestamp = row[0];
		if(row[0].length()==22)
			timestamp+="0";
		if(row[0].length()==21)
			timestamp+="00";
		sdc.storyCreatedAt = LocalDateTime.parse(timestamp, formatter);
		sdc.createdAt = LocalDateTime.parse(timestamp, formatter);
		sdc.taskId = row[1];	
		sdc.parentTaskId = row[2];
		sdc.taskName = row[3];
		sdc.rawDataText = row[4];
		sdc.messageType = row[5];
		sdc.typeOfChange = Integer.parseInt(row[6]);
		sdc.typeOfChangeDescription = row[7];
		sdc.isRole = Boolean.parseBoolean(row[8]);
		sdc.taskCreatedAt = LocalDateTime.parse(timestamp, formatter);
		sdc.storyCreatedByName = row[10];
		sdc.projectName = row[11];
		sdc.isCircle = Boolean.parseBoolean(row[12]);
		sdc.storyCreatedById = row[13];
		sdc.currentAssignee = row[14];
		sdc.lastAssigneeId = row[15];
		sdc.lastAssigneeName = row[16];
		sdc.storyId = row[17];
		sdc.projectId = row[18];				
		sdc.workspaceId = row[19];
		sdc.workspaceName = row[20];
		sdc.isSubtask = Boolean.parseBoolean(row[21]);
		sdc.isRenderedAsSeparator = Boolean.parseBoolean(row[22]);
		sdc.parentTaskName = row[23];
		sdc.taskCompletedAt = (!row[26].equals(""))? parseDateTime(row[26]):null;
		sdc.taskModifiedAt = parseDateTime(row[27]);
		sdc.taskNotes = row[28];

		if(row.length>29) {
			//			System.out.println("Here length="+row.length);
			sdc.setCircle(row[29]);
			sdc.setCircleIds(row[30]);
		}
		if(row.length>=34)
			sdc.setAccordingToCircle(row[33]);

		return sdc;
	}

	private static LocalDateTime parseDateTime(String string) {
		DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
		LocalDateTime res = null;
		try {
			res = LocalDateTime.parse(string, formatter);
		} catch (java.time.format.DateTimeParseException e) {
			try {
				formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
				res = LocalDateTime.parse(string, formatter);
			} catch (Exception e2) {
				System.err.println("Could not parse date-time. "+e2);
			}
		}
		return res;
	}

	public static String getPath(Task task) {
		String path = "";
		if(task.parent!=null)
			path += getPath(task.parent) + "/" + task.parent.name + "/" + task.name;
		return path;
	}

	public static boolean isCircle(String name) {
		if(name.contains("☺") && name.toLowerCase().endsWith("roles"))
			return true;
		else 
			return false;
	}

	public static boolean isSmiley(String parentName) {
		return parentName.startsWith("☺");
	}

	public static boolean isYinAndYang(String taskname) {
		if(taskname.startsWith("☯"))
			return true;
		return false;
	}

	public static StructuralDataChange parseFromText(Task task, Story story, String me) {
		StructuralDataChange dataChange = new StructuralDataChange();
		DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
		dataChange.storyCreatedAt = LocalDateTime.parse(story.createdAt.toString(), formatter);
		dataChange.storyId = story.gid;
		dataChange.taskId = task.gid;
		dataChange.taskName = task.name;
		dataChange.taskCreatedAt = LocalDateTime.parse(task.createdAt.toString(), formatter);
		dataChange.taskCompletedAt = LocalDateTime.parse(task.completedAt.toString(), formatter);
		dataChange.taskModifiedAt = LocalDateTime.parse(task.modifiedAt.toString(), formatter);
		dataChange.setActionAndAssignee(story.text, story.type, me);
		dataChange.setTaskTags(dataChange.extractTaskTags(task.tags));
		dataChange.setTaskNotes(task.notes);
		//		pathToHere = getPath(task);
		dataChange.setStoryCreatedById(story.createdBy.gid);
		dataChange.setStoryCreatedByName(story.createdBy.name);
		if(task.assignee!=null){
			dataChange.lastAssigneeId = task.assignee.gid;
			dataChange.lastAssigneeName = task.assignee.name;
		}
		dataChange.isSubtask = (task.parent!=null);
		if(dataChange.isSubtask){
			dataChange.parentTaskName = task.parent.name;
			dataChange.setParentTaskId(task.parent.gid);
		}
		dataChange.setRole(dataChange.taskName);
		dataChange.isCircle = isCircle(task.name);
		dataChange.rawDataText = story.text;
		dataChange.messageType = story.type;
		dataChange.typeOfChange = dataChange.typeOfChangeFromCodingScheme(story.text, dataChange.messageType);
		dataChange.typeOfChangeDescription = AsanaActions.codeToString(dataChange.typeOfChange);
		if(story.resourceSubtype.equals("assigned")) {
			String assignee = dataChange.parseAssignee(story.text);
			if(assignee!= null && assignee.equals("you"))
				assignee = me;
			dataChange.currentAssignee = assignee;
		}
		dataChange.isRenderedAsSeparator=task.isRenderedAsSeparator;
		return dataChange;
	}

	public boolean isRenderedAsSeparator() {
		return isRenderedAsSeparator;
	}

	public void setRenderedAsSeparator(boolean isRenderedAsSeparator) {
		this.isRenderedAsSeparator = isRenderedAsSeparator;
	}

	public boolean isChangeAccountabilityPurpose() {
		return isChangeAccountabilityPurpose;
	}

	public void setChangeAccountabilityPurpose(boolean isChangeAccountabilityPurpose) {
		this.isChangeAccountabilityPurpose = isChangeAccountabilityPurpose;
	}

	public int compareTo(StructuralDataChange o) {		
		return this.storyCreatedAt.compareTo(o.getStoryCreatedAt());
	}

	public StructuralDataChange makeCopy() {
		StructuralDataChange sdc = new StructuralDataChange();
		sdc.storyCreatedAt = LocalDateTime.from(this.storyCreatedAt);
		sdc.taskCreatedAt = LocalDateTime.from(this.taskCreatedAt); 
		sdc.taskModifiedAt = LocalDateTime.from(this.taskModifiedAt);
		if(this.taskCompletedAt!=null)
				sdc.taskCompletedAt = LocalDateTime.from(this.taskCompletedAt);
		sdc.isRole = this.isRole;
		sdc.actor = ""+ this.actor;
		sdc.lastAssigneeId = ""+this.lastAssigneeId;
		sdc.lastAssigneeName = ""+this.lastAssigneeName;
		sdc.action = "" + this.action;
		sdc.circle = this.circle +""; //location
		sdc.pathToHere = this.pathToHere+"";
		sdc.taskId = this.taskId +"";
		sdc.taskName = this.taskName + "";
		sdc.storyId = this.storyId +"";
		sdc.workspaceId = this.workspaceId+"";
		sdc.isSubtask = this.isSubtask;
		sdc.workspaceName = this.workspaceName+"";
		sdc.projectName = this.projectName + "";
		sdc.rawDataText = this.rawDataText +"";
		sdc.messageType = this.messageType + "";
		sdc.currentAssignee = this.currentAssignee + "";
		sdc.projectId = this.projectId + "";
		sdc.createdAt = LocalDateTime.from(this.createdAt);
		if(sdc.modifiedAt!=null)
			sdc.modifiedAt = LocalDateTime.from(this.modifiedAt);
		if(sdc.completedAt!=null)
			sdc.completedAt = LocalDateTime.from(this.completedAt);
		sdc.storyCreatedById = this.storyCreatedById + "";
		sdc.storyCreatedByName = this.storyCreatedByName + "";
		sdc.parentTaskId = this.parentTaskId + "";
		sdc.taskNotes = this.taskNotes + "";
		sdc.taskTags = this.taskTags + "";
		sdc.typeOfChange = this.typeOfChange;
		sdc.typeOfChangeDescription = this.typeOfChangeDescription + "";
		sdc.isCircle = this.isCircle;
		sdc.migration = this.migration;
		sdc.isRenderedAsSeparator = this.isRenderedAsSeparator;
		sdc.isChangeAccountabilityPurpose = this.isChangeAccountabilityPurpose;
		sdc.circleIds = this.circleIds + "";
		sdc.parentCircle = this.parentCircle + "";
		return sdc;
	}

	@Override
	public int hashCode() {
		return 7*storyCreatedAt.hashCode() + 17*taskId.hashCode() + 31*rawDataText.hashCode() * 41*projectId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		 // If the object is compared with itself then return true   
        if (obj == this) { 
            return true; 
        } 
  
        /* Check if o is an instance of Complex or not 
          "null instanceof [type]" also returns false */
        if (!(obj instanceof StructuralDataChange)) { 
            return false; 
        } 
          
        // typecast o to Complex so that we can compare data members  
        StructuralDataChange sdc = (StructuralDataChange) obj; 
          
		return this.storyCreatedAt.equals(sdc.storyCreatedAt) && this.taskId.equals(sdc.taskId) 
				&& this.typeOfChange == sdc.typeOfChange
				;
	}

	public static StructuralDataChange createDerivedEvent(Task task, Project project, Workspace workspace,
			DateTime timestamp, int action) {
		StructuralDataChange chTask = new StructuralDataChange(task, timestamp, action);
		chTask.setProjectId(project.gid);
		chTask.setWorkspaceId(workspace.gid);
		chTask.setProjectId(project.gid);
		chTask.setProjectName(project.name);
		chTask.setWorkspaceId(workspace.gid);
		chTask.setWorkspaceName(workspace.name);
		chTask.setMessageType("derived");
		return chTask;
	}
	
	

}
