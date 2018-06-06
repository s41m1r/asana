package at.ac.wu.asana.tryout;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.asana.Client;
import com.asana.models.Project;
import com.asana.models.Story;
import com.asana.models.Task;
import com.asana.models.Workspace;
import com.asana.requests.CollectionRequest;
import com.opencsv.CSVWriter;

public class ExtractStructuralDataChanges {

	static List<StructuralDataChange> structuralDataChanges = new ArrayList<StructuralDataChange>();
	static Options opts = new Options();

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		
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
	    }

//		asanaChangesToCSV(App.PAT_SAIMIR, "InfoBiz", "");
	    String pat = line.getOptionValue("pat");
		String ws = line.getOptionValue("ws");
		String csv = line.getOptionValue("csv");
		String p = line.getOptionValue("p");
		String r = line.getOptionValue("r");
		String ots = line.getOptionValue("ots");
		
		
	    System.out.println("Extraction started with parameters "+"\npat:"+pat+""
	    		+ "\nws:" +ws + "," 
	    		+ "\ncsv:" +csv + ","
	    		+ "\np:" +p + ","
	    		+ "\nr:" +r + ","
	    		+ "\nots:"+ ots
	    		);
	    
		asanaChangesToCSV(pat,ws,csv,p,r,(ots!=null));
		
		System.out.println("All done in "+ getElapsedTime(System.currentTimeMillis(), start));
	}

	private static Options initOpts() {
		Option csvOutFile   = new Option("csv", "csv", true, "use given file for csv logging");
		Option pat = new Option("pat", "personalAccessToken", true, "use given personalAccessToken");
		Option wspace = new Option("ws", "workspace", true, "use given workspace name");
		Option project = new Option("p", "project", true, "use given project name");
		Option role = new Option("r", "role", true, "use given role name");
		Option onlyTaskStories = new Option("ots", "onlyTaskStories", false, "use given role name");
		
		opts.addOption(wspace).
		addOption(csvOutFile).
		addOption(pat).
		addOption(project).
		addOption(role).addOption(onlyTaskStories);
		
		return opts;
	}

	public static void asanaChangesToCSV(String pat, String workspaceName, 
			String csvOutFile, String specificProject, String specificTask, Boolean onlyTaskStories){
		Client client = Client.accessToken(pat);
		Workspace workspace = null;
		Iterable<Workspace> workspaces = client.workspaces.findAll();
		for (Workspace wspace : workspaces) {
			if (wspace.name.equals(workspaceName)) {
				workspace = wspace;
				break;
			}
		}

		Iterable<Project> projects = client.projects.findByWorkspace(workspace.id);

		try {
			PrintWriter rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(csvOutFile), StandardCharsets.UTF_8) );

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = StructuralDataChange.csvHeader();

			csvWriter.writeNext(header);
			
			System.out.print("Looking for "+ specificProject + " ... ");
			boolean foundSpecificProject = false;
			for (Project project : projects) {
				
				if(specificProject != null){
					if(!project.name.contains(specificProject)){
						continue;
					}
				}
				System.out.println("Found ("+project.name+")");
				System.out.println("Retrieving all the tasks and subtasks.");
				List<Task> tasks = client.tasks.findByProject(project.id).execute();
				List<Task> allTasksAndSubtasks = null;
				
				if(onlyTaskStories){
					allTasksAndSubtasks = tasks;
				}
				else{
					allTasksAndSubtasks = getAllNestedSubtasks(client, tasks);
				}
				
				System.out.println("Scanning "+project.name+ " containing "+allTasksAndSubtasks.size()+ " tasks and subtasks.");
//				List<StructuralDataChange> changes = new ArrayList<StructuralDataChange>();
				for (Task task : allTasksAndSubtasks) {//find the stories and create the StructuralDataChanges
					if(specificTask!=null){
						if(!task.name.contains(specificTask))
							continue;
					}
					CollectionRequest<Story> stories = client.stories.findByTask(task.id);
					System.out.println("Extracting stories (events) of "+task.name);
					for (Story story : stories) {
						StructuralDataChange change = new StructuralDataChange(task, story, client.users.me().execute().name.trim());
						change.setProjectId(project.id);
						change.setWorkspaceId(workspace.id);
						change.setRole(task.name);
						change.setProjectId(project.id);
						change.setProjectName(project.name);
						change.setWorkspaceId(workspace.id);
						change.setWorkspaceName(workspace.name);
						csvWriter.writeNext(change.csvRow());
					}
				}
			}
			csvWriter.flush();
			csvWriter.close();
			if(!foundSpecificProject){
				System.out.println("Not found.");
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static List<Task> getAllNestedSubtasks(Client client, List<Task> roots){//recursive
		List<Task> allTasks = new ArrayList<Task>();
		try {
			allTasks.addAll(roots);
			for (Task task : roots) {
				List<Task> subtasks = client.tasks.subtasks(task.id).execute();
				for (Task t : subtasks) {
					t.parent = task;
				}
				allTasks.addAll(getAllNestedSubtasks(client, subtasks));				
			}
		} catch (IOException e) {
			e.printStackTrace();
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
			System.out.println("Task id:\t"+task.id+ 
					"\tname:"+task.name+
					"\ttask.createdAt:"+task.createdAt + 
					"\ttask.modifiedAt:"+task.modifiedAt + 
					"\ttask.assignedTo:"+task.assigneeStatus + "," + task.assignee +
					"\ttask.completedAt:"+task.completedAt + 
					"\ttask.completed:"+task.completed + 
					"\ttask.parents:"+StructuralDataChange.getPath(task)
					);
		}
	}
	
	/**
	 * @param startTime
	 */
	static String getElapsedTime(long time, long startTime) {
		long elapsed = time-startTime;
		long second = (elapsed / 1000) % 60;
		long minute = (elapsed / (1000 * 60)) % 60;
		long hour = (elapsed / (1000 * 60 * 60)) % 24;
		long day = (elapsed / (1000 * 60 * 60 * 24));
		
		return String.format("%03d %02d:%02d:%02d.%03d [days h:m:s.msec]", day, hour, minute, second, elapsed%1000);
	}
}
