package at.ac.wu.asana.csv;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.asana.Client;
import com.asana.models.Project;
import com.asana.models.Story;
import com.asana.models.Task;
import com.asana.models.Workspace;
import com.google.api.client.util.DateTime;
import com.opencsv.CSVWriter;

import at.ac.wu.asana.db.utils.DatabaseConnector;
import at.ac.wu.asana.model.AsanaActions;
import at.ac.wu.asana.model.StructuralDataChange;

public class ExtractStructuralDataChanges {

	static List<StructuralDataChange> structuralDataChanges = new ArrayList<StructuralDataChange>();
	static Options opts = new Options();
	static Logger logger = Logger.getLogger("Extraction");	

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		FileHandler fh;
		try {
			fh = new FileHandler("Extraction.log");
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();  
			fh.setFormatter(formatter);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}    

		CommandLineParser commandLineParser = new DefaultParser();
		Options options = initOpts();
		CommandLine line = null;

		try {
			// parse the command line arguments
			line = commandLineParser.parse(options, args);
		}
		catch(ParseException exp ) {
			// oops, something went wrong
			System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
			logger.warning("Parsing failed.  Reason: " + exp.getMessage());
		}

		//		asanaChangesToCSV(App.PAT_SAIMIR, "InfoBiz", "");
		String pat = line.getOptionValue("pat");
		String ws = line.getOptionValue("ws");
		String csv = line.getOptionValue("csv");
		String p = line.getOptionValue("p");
		String pid = line.getOptionValue("pid");
		String r = line.getOptionValue("r");
		Boolean ots = line.hasOption("ots");
		Boolean os = line.hasOption("os");

		String info = "Extraction started with parameters "+"\npat:"+pat+""
				+ "\nws:" +ws + "," 
				+ "\ncsv:" +csv + ","
				+ "\np:" +p + ","
				+ "\npid:" +pid + ","
				+ "\nr:" +r + ","
				+ "\nots:"+ ots
				+ "\nos:"+ os;
		logger.info(info); 

		//		asanaChangesToCSV(pat,ws,csv,p,r,ots,os);
		if(pid!=null)
			extractTasksFromPID(pat, ws, csv, pid, r, ots, os);
		
		else{
			parseRawDataText(pat,ws,csv,p,r,ots,os);
		}

		//		System.out.println("All done in "+ getElapsedTime(System.currentTimeMillis(), start));
		logger.info("All done in "+ getElapsedTime(System.currentTimeMillis(), start));
		logger.info("Output stored in:  "+ csv);
	}

	private static void parseRawDataText(String pat, String ws, String csv, String p, String r, Boolean ots,
			Boolean os) {
		Client client = Client.accessToken(pat);
		client.options.put("page_size", 100);
		client.options.put("max_retries", 100);
		//		client.options.put("poll_interval", 10);

		Workspace workspace = null;
		Iterable<Workspace> workspaces = client.workspaces.findAll();
		for (Workspace wspace : workspaces) {
			if (wspace.name.equals(ws)) {
				workspace = wspace;
				break;
			}
		}

		Iterable<Project> projects = client.projects.findByWorkspace(workspace.id);

		try {
			String me = client.users.me().execute().name.trim();
			PrintWriter rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(csv), StandardCharsets.UTF_8) );

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = StructuralDataChange.csvHeader();

			csvWriter.writeNext(header);

			for (Project project : projects) {
				if(p != null){
					logger.info("Looking for "+ p + " ... ");
					if(!project.name.contains(p)){
						continue;
					}
				}
				logger.info("Found project: "+project.name);
				logger.info("Retrieving all the tasks and subtasks.");

				List<Task> tasksIt = client.tasks.findByProject(project.id).
						option("fields",
								Arrays.asList(
										"created_at", "name", "completed",
										"tags","completed_at", "notes", 
										"modified_at", "parent", "parent.name", 
										"assignee", "assignee.name", "include_archived")).execute();
				List<Task> tasks = new ArrayList<Task>();
				for (Task task : tasksIt) {
					tasks.add(task);
				}

				List<Task> allTasksAndSubtasks = null;

				if(ots){
					allTasksAndSubtasks = tasks;
				}
				else{
					allTasksAndSubtasks = getAllNestedSubtasks(client, tasks);
				}

				logger.info("Scanning "+project.name+ " containing "+allTasksAndSubtasks.size()+ " tasks and subtasks.");
				for (Task task : allTasksAndSubtasks) {//find the stories and create the StructuralDataChanges
					if(r!=null){
						if(!task.name.contains(r))
							continue;
					}
					List<Story> stories = client.stories.findByTask(task.id).option("fields", 
							Arrays.asList(
									"created_at", "created_by","created_by.name",
									"type", "target", "text")).execute();
					if(stories == null){
						logger.severe("Stories of "+task.name+ "is "+stories);
						continue;
					}

					logger.info("Extracting stories (events) of "+task.name);
					/*
					 * Here we can define a new event to be inserted about only Tasks creation. 
					 * But what about the other fields?
					 */					
					addCSVRow(project, workspace, task, csvWriter, task.createdAt, AsanaActions.CREATE_ROLE);

					for (Story story : stories) {
						try{
							//							StructuralDataChange change = new StructuralDataChange(task, story, me);
							//							StructuralDataChange change = new StructuralDataChange(task, story);
							StructuralDataChange change = StructuralDataChange.parseFromText(task,story,me);
							change.setProjectId(project.id);
							change.setWorkspaceId(workspace.id);
							change.setProjectId(project.id);
							change.setProjectName(project.name);
							change.setWorkspaceId(workspace.id);
							change.setWorkspaceName(workspace.name);
							csvWriter.writeNext(change.csvRow());
						}
						catch (NullPointerException e) {
							System.err.println("Story: "+story);
							logger.info("Problem in Story: "+story);
							e.printStackTrace();
						}
					}
					addCSVRow(project, workspace, task, csvWriter, task.completedAt, AsanaActions.COMPLETE_ROLE);
					addCSVRow(project, workspace, task, csvWriter, task.modifiedAt, AsanaActions.LAST_MODIFY_ROLE);
				}
			}
			csvWriter.flush();
			csvWriter.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Options initOpts() {
		Option csvOutFile   = new Option("csv", "csv", true, "use given file for csv logging");
		Option pat = new Option("pat", "personalAccessToken", true, "use given personalAccessToken");
		Option wspace = new Option("ws", "workspace", true, "use given workspace name");
		Option project = new Option("p", "project", true, "use given project name");
		Option projectId = new Option("pid", "projectId", true, "use given project id");
		Option role = new Option("r", "role", true, "use given role name");
		Option onlyTaskStories = new Option("ots", "onlyTaskStories", false, "do not retrieve subtasks");
		Option onlySmileys = new Option("os", "onlySmileys", true, "only extract projects/tasks with smileys (roles)");

		opts.addOption(wspace).
		addOption(csvOutFile).
		addOption(pat).
		addOption(project).
		addOption(projectId).
		addOption(role).addOption(onlyTaskStories).addOption(onlySmileys);

		return opts;
	}

	public static void asanaChangesToCSV(String pat, String workspaceName, 
			String csvOutFile, String specificProject, String specificTask, Boolean onlyTaskStories, boolean onlySmileys){
		Client client = Client.accessToken(pat);
		client.options.put("page_size", 100);
		client.options.put("max_retries", 100);
		//		client.options.put("poll_interval", 10);

		Workspace workspace = null;
		Iterable<Workspace> workspaces = client.workspaces.findAll();
		for (Workspace wspace : workspaces) {
			if (wspace.name.equals(workspaceName)) {
				workspace = wspace;
				break;
			}
		}

		Iterable<Project> projects = client.projects.findByWorkspace(workspace.id);

		//		SessionFactory sessionFactory = DatabaseConnector.getSessionFactory("asana_manual2");
		//		
		//		Session session = sessionFactory.openSession();
		//		
		//		Query query = session.createSQLQuery("CREATE TABLE IF NOT EXISTS `out2` (\n" + 
		//				"	timestamp TIMESTAMP NULL, \n" + 
		//				"	`taskId` DECIMAL(38, 0) NOT NULL, \n" + 
		//				"	`parentTaskId` DECIMAL(38, 0), \n" + 
		//				"	`taskName` VARCHAR(207), \n" + 
		//				"	`rawDataText` VARCHAR(511), \n" + 
		//				"	`messageType` VARCHAR(7) NOT NULL, \n" + 
		//				"	`typeOfChange` DECIMAL(38, 0) NOT NULL, \n" + 
		//				"	`typeOfChangeDescription` VARCHAR(33) NOT NULL, \n" + 
		//				"	`isRole` BOOL NOT NULL, \n" + 
		//				"	`taskCreatedAt` TIMESTAMP NULL, \n" + 
		//				"	`createdByName` VARCHAR(19), \n" + 
		//				"	`projectName` VARCHAR(21) NOT NULL, \n" + 
		//				"	`isCicle` BOOL NOT NULL, \n" + 
		//				"	`createdById` DECIMAL(38, 0), \n" + 
		//				"	`assigneeId` DECIMAL(38, 0), \n" + 
		//				"	`assigneeName` VARCHAR(17), \n" + 
		//				"	`eventId` DECIMAL(38, 0), \n" + 
		//				"	`projectId` DECIMAL(38, 0) NOT NULL, \n" + 
		//				"	`workspaceId` DECIMAL(38, 0) NOT NULL, \n" + 
		//				"	`workspaceName` VARCHAR(9) NOT NULL, \n" + 
		//				"	`isSubtask` BOOL NOT NULL, \n" + 
		//				"	`parentTaskName` VARCHAR(63), \n" + 
		//				"	date DATE NOT NULL, \n" + 
		//				"	time DATETIME NOT NULL, \n" + 
		//				"	`taskCompletedAt` TIMESTAMP NULL, \n" + 
		//				"	`taskModifiedAt` TIMESTAMP NULL, \n" + 
		//				"	`taskNotes` VARCHAR(370), \n" + 
		//				"	CHECK (`isRole` IN (0, 1)), \n" + 
		//				"	CHECK (`isCicle` IN (0, 1)), \n" + 
		//				"	CHECK (`isSubtask` IN (0, 1))\n" + 
		//				");");
		//		query.list();


		try {
			String me = client.users.me().execute().name.trim();
			PrintWriter rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(csvOutFile), StandardCharsets.UTF_8) );

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = StructuralDataChange.csvHeader();

			csvWriter.writeNext(header);

			for (Project project : projects) {

				if(specificProject != null){
					logger.info("Looking for "+ specificProject + " ... ");
					if(!project.name.contains(specificProject)){
						continue;
					}
				}
				logger.info("Found project: "+project.name);
				logger.info("Retrieving all the tasks and subtasks.");

				List<Task> tasksIt = client.tasks.findByProject(project.id).
						option("fields",
								Arrays.asList(
										"created_at", "name", "completed",
										"tags","completed_at", "notes", 
										"modified_at", "parent", "parent.name", 
										"assignee", "assignee.name", "include_archived")).execute();
				List<Task> tasks = new ArrayList<Task>();
				for (Task task : tasksIt) {
					tasks.add(task);

					//					System.out.println("Assignee Status:"+task.assigneeStatus + 
					//							" id: "+task.id+" name:"+ task.name + " notes: "+
					//							task.notes + " completed: "+task.completedAt + 
					//							" assignee: "+((task.assignee!=null)? 
					//									("id="+task.assignee.id+" name="+task.assignee.name) : task.assignee)
					//							);		
				}

				List<Task> allTasksAndSubtasks = null;

				if(onlyTaskStories){
					allTasksAndSubtasks = tasks;
				}
				else{
					allTasksAndSubtasks = getAllNestedSubtasks(client, tasks);
				}

				logger.info("Scanning "+project.name+ " containing "+allTasksAndSubtasks.size()+ " tasks and subtasks.");
				for (Task task : allTasksAndSubtasks) {//find the stories and create the StructuralDataChanges
					if(specificTask!=null){
						if(!task.name.contains(specificTask))
							continue;
					}
					List<Story> stories = client.stories.findByTask(task.id).option("fields", 
							Arrays.asList(
									"created_at", "created_by","created_by.name",
									"type", "target", "text")).execute();
					if(stories == null){
						logger.severe("Stories of "+task.name+ "is "+stories);
						continue;
					}

					logger.info("Extracting stories (events) of "+task.name);
					/*
					 * Here we can define a new event to be inserted about only Tasks creation. 
					 * But what about the other fields?
					 */					
					addCSVRow(project, workspace, task, csvWriter, task.createdAt, AsanaActions.CREATE_ROLE);

					for (Story story : stories) {
						try{
							StructuralDataChange change = new StructuralDataChange(task, story, me);
							//							StructuralDataChange change = new StructuralDataChange(task, story);
							change.setProjectId(project.id);
							change.setWorkspaceId(workspace.id);
							change.setProjectId(project.id);
							change.setProjectName(project.name);
							change.setWorkspaceId(workspace.id);
							change.setWorkspaceName(workspace.name);
							csvWriter.writeNext(change.csvRow());
						}
						catch (NullPointerException e) {
							System.err.println("Story: "+story);
							logger.info("Problem in Story: "+story);
							e.printStackTrace();
						}
					}
					addCSVRow(project, workspace, task, csvWriter, task.completedAt, AsanaActions.COMPLETE_ROLE);
					addCSVRow(project, workspace, task, csvWriter, task.modifiedAt, AsanaActions.LAST_MODIFY_ROLE);
				}
			}
			csvWriter.flush();
			csvWriter.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	//	private static boolean containsSmiley(String name) {
	//		if(name!=null){
	//			char[] chars = name.toCharArray();
	//			for (int c : chars) {
	//				if(c==9786) //smiley character ☺
	//					return true;
	//			}
	//		}
	//		return false;
	//	}

	public static void addCSVRow(Project project, Workspace workspace, Task task, 
			CSVWriter csvWriter, DateTime eventTimestamp, int action) {

		if(eventTimestamp == null) 
			return;
		StructuralDataChange chTask = new StructuralDataChange(task, eventTimestamp, action);
		chTask.setProjectId(project.id);
		chTask.setWorkspaceId(workspace.id);
		chTask.setProjectId(project.id);
		chTask.setProjectName(project.name);
		chTask.setWorkspaceId(workspace.id);
		chTask.setWorkspaceName(workspace.name);
		chTask.setMessageType("derived");
		csvWriter.writeNext(chTask.csvRow());
	}

	public static List<Task> getAllNestedSubtasks(Client client, List<Task> roots){//recursive
		List<Task> allTasks = new ArrayList<Task>();
		allTasks.addAll(roots);
		for (Task task : roots) {
			List<Task> subtasks = null;
			try {
				//				subtasks = client.tasks.subtasks(task.id).execute();
				logger.info("Getting subtasks of "+task.name);
				subtasks = client.tasks.subtasks(task.id).option("fields", 
						Arrays.asList(
								"created_at", "name", "completed",
								"tags","completed_at",
								"modified_at", "parent", "parent.name")).execute();
			} catch (IOException e) {
				e.printStackTrace();
			}

			for (Task t : subtasks) {
				t.parent = task;
			}
			allTasks.addAll(getAllNestedSubtasks(client, subtasks));				
		}
		return allTasks;
	}

	//	public static List<StructuralDataChange> getAllStructuralDataChanges(Client client, List<Task> allTasks, String path){
	//		List<StructuralDataChange> list = new ArrayList<StructuralDataChange>();
	//		for (Task task : allTasks) {
	//			CollectionRequest<Story> stories = client.stories.findByTask(task.id);
	//			for (Story story : stories) {
	//				StructuralDataChange structuralDataChange = new StructuralDataChange();
	//				structuralDataChange.setDateTime(story.createdAt);
	//				structuralDataChange.setPathToHere(task.id);
	//			}
	//		}
	//		return list;
	//	}

	public static void printTasks(List<Task> tasks){
		for (Task task : tasks) {
			System.out.print("Task id:\t"+task.id+ 
					"\tname:"+task.name+
					"\ttask.createdAt:"+task.createdAt + 
					"\ttask.modifiedAt:"+task.modifiedAt + 
					"\ttask.assignedTo:"+task.assigneeStatus + "," + task.assignee +
					"\ttask.completedAt:"+task.completedAt + 
					"\ttask.completed:"+task.completed + 
					"\ttask.dueAt:"+task.dueAt +
					"\ttask.dueOn:"+task.dueOn +
					"\ttask.tags:"+task.tags
					);
			if(task.parent !=null){
				List<Task> p = new ArrayList<Task>();
				p.add(task.parent);
				printTasks(p);
			}
			System.out.println();
		}
	}

	/**
	 * @param startTime
	 */
	public static String getElapsedTime(long time, long startTime) {
		long elapsed = time-startTime;
		long second = (elapsed / 1000) % 60;
		long minute = (elapsed / (1000 * 60)) % 60;
		long hour = (elapsed / (1000 * 60 * 60)) % 24;
		long day = (elapsed / (1000 * 60 * 60 * 24));

		return String.format("%03d %02d:%02d:%02d.%03d [days h:m:s.msec]", day, hour, minute, second, elapsed%1000);
	}

	private static void extractTasksFromPID(String pat, String ws, String csv, String pid, String r, Boolean ots,
			Boolean os) {

		Client client = Client.accessToken(pat);
		client.options.put("page_size", 100);
		client.options.put("max_retries", 100);
		//		client.options.put("poll_interval", 10);
		try {
			String me = client.users.me().execute().name.trim();
			String wsid = null;
			boolean found = false;
			
			for(Workspace w : client.workspaces.findAll()) {
				if(w.name.equals(ws)) {
					wsid = w.id;
					found = true;
					break;
				}
			}
			
			if(!found) {
				System.err.println("Workspace not found.");
				System.exit(-1);
			}
			Workspace workspace = client.workspaces.findById(wsid).execute();
			Project project = client.projects.findById(pid).execute();

			PrintWriter rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(csv), StandardCharsets.UTF_8) );

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = StructuralDataChange.csvHeader();

			csvWriter.writeNext(header);

			logger.info("Found project: "+project.name);
			logger.info("Retrieving all the tasks and subtasks.");

			List<Task> tasksIt = client.tasks.findByProject(project.id).
					option("fields",
							Arrays.asList(
									"created_at", "name", "completed",
									"tags","completed_at", "notes", 
									"modified_at", "parent", "parent.name", 
									"assignee", "assignee.name", "include_archived")).execute();
			List<Task> tasks = new ArrayList<Task>();
			for (Task task : tasksIt) {
				tasks.add(task);
			}

			List<Task> allTasksAndSubtasks = null;

			if(ots){
				allTasksAndSubtasks = tasks;
			}
			else{
				allTasksAndSubtasks = getAllNestedSubtasks(client, tasks);
			}

			logger.info("Scanning "+project.name+ " containing "+allTasksAndSubtasks.size()+ " tasks and subtasks.");
			for (Task task : allTasksAndSubtasks) {//find the stories and create the StructuralDataChanges
				if(r!=null){
					if(!task.name.contains(r))
						continue;
				}
				List<Story> stories = client.stories.findByTask(task.id).option("fields", 
						Arrays.asList(
								"created_at", "created_by","created_by.name",
								"type", "target", "text")).execute();
				if(stories == null){
					logger.severe("Stories of "+task.name+ "is "+stories);
					continue;
				}

				logger.info("Extracting stories (events) of "+task.name);
				/*
				 * Here we can define a new event to be inserted about only Tasks creation. 
				 * But what about the other fields?
				 */					
				addCSVRow(project, workspace, task, csvWriter, task.createdAt, AsanaActions.CREATE_ROLE);

				for (Story story : stories) {
					try{
						//							StructuralDataChange change = new StructuralDataChange(task, story, me);
						//							StructuralDataChange change = new StructuralDataChange(task, story);
						StructuralDataChange change = StructuralDataChange.parseFromText(task,story,me);
						change.setProjectId(project.id);
						change.setWorkspaceId(workspace.id);
						change.setProjectId(project.id);
						change.setProjectName(project.name);
						change.setWorkspaceId(workspace.id);
						change.setWorkspaceName(workspace.name);
						csvWriter.writeNext(change.csvRow());
					}
					catch (NullPointerException e) {
						System.err.println("Story: "+story);
						logger.info("Problem in Story: "+story);
						e.printStackTrace();
					}
				}
				addCSVRow(project, workspace, task, csvWriter, task.completedAt, AsanaActions.COMPLETE_ROLE);
				addCSVRow(project, workspace, task, csvWriter, task.modifiedAt, AsanaActions.LAST_MODIFY_ROLE);
			}
			csvWriter.flush();
			csvWriter.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
}
