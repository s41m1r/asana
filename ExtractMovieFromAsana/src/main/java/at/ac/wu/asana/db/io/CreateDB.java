package at.ac.wu.asana.db.io;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;

import com.asana.Client;
import com.asana.models.Project;
import com.asana.models.Story;
import com.asana.models.Task;
import com.asana.models.Workspace;
import com.asana.requests.CollectionRequest;

import at.ac.wu.asana.db.utils.DatabaseConnector;
import at.ac.wu.asana.tryout.ExtractStructuralDataChanges;
import at.ac.wu.asana.tryout.StructuralDataChange;

public class CreateDB {
	
	static Map<at.ac.wu.asana.db.model.Task, at.ac.wu.asana.db.model.Task> taskParentMap = 
			new HashMap<at.ac.wu.asana.db.model.Task, at.ac.wu.asana.db.model.Task>();
	
	static Map<at.ac.wu.asana.db.model.Story, at.ac.wu.asana.db.model.Task> storyTaskMap = 
			new HashMap<at.ac.wu.asana.db.model.Story, at.ac.wu.asana.db.model.Task>();
	
	static Map<at.ac.wu.asana.db.model.Task, at.ac.wu.asana.db.model.Project> taskProjectMap = 
			new HashMap<at.ac.wu.asana.db.model.Task, at.ac.wu.asana.db.model.Project>();
	

	static Options opts = new Options();

	public static void main(String[] args) {

		//		test("asana");
		populate(args);

	}
	

	static void test(String dbname){
		SessionFactory sessionFactory;

		System.out.println("Attempt to connect to "+dbname);

		if(dbname!=null)
			sessionFactory = DatabaseConnector.getSessionFactory(dbname);
		else
			sessionFactory = DatabaseConnector.getSessionFactory();
		StatelessSession session = sessionFactory.openStatelessSession();
		Transaction tx = session.beginTransaction();

		tx.commit();
		session.close();
		//		System.out.println(100*(users.size()+commits.size()+files.size()+renames.size())/total+"% done");
		DatabaseConnector.shutdown();
	}

	static void populate(String[] args){
		SessionFactory sessionFactory;
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
	    String dbname = line.getOptionValue("db");


		System.out.println("Extraction started with parameters "+"\npat:"+pat+""
				+ "\nws:" +ws + "," 
				+ "\ncsv:" +csv + ","
				+ "\np:" +p + ","
				+ "\nr:" +r + ","
				+ "\ndb:" +dbname + ","
				+ "\nots:"+ ots
				);
		
		System.out.println("Attempt to connect to "+dbname);

		if(dbname!=null)
			sessionFactory = DatabaseConnector.getSessionFactory(dbname);
		else
			sessionFactory = DatabaseConnector.getSessionFactory("asana");
		
		StatelessSession session = sessionFactory.openStatelessSession();
		

		asanaChangesToDB(pat,ws,csv,p,r,(ots!=null), session);
		session.close();
		//		System.out.println(100*(users.size()+commits.size()+files.size()+renames.size())/total+"% done");
		DatabaseConnector.shutdown();
		System.out.println("All done in "+ ExtractStructuralDataChanges.getElapsedTime(System.currentTimeMillis(), start));
	}


	/**
	 * @param args
	 */
	public static void persistProjects(String[] args, StatelessSession session) {
		CommandLineParser lineParser = new DefaultParser();
		CommandLine cmd = null;
		Options opts = new Options();
		opts.addOption(new Option("csv", true, "the output file produced"))
		.addOption(new Option("pat", true, "the personal access token"))
		.addOption(new Option("db", true, "the name of the database to be created"))
		.addOption(new Option("ws", true, "the workspace"));

		try {
			cmd = lineParser.parse(opts, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		Client client = Client.accessToken(cmd.getOptionValue("pat"));
		Workspace workspace = null;

		Iterable<Workspace> workspaces = client.workspaces.findAll();

		for (Workspace wspace : workspaces) {
			if (wspace.name.equals(cmd.getOptionValue("ws"))) {
				workspace = wspace;
				break;
			}
		}

		System.out.println("Workspace id:"+workspace.id+ " name:"+ workspace.name);

		CollectionRequest<Project> projects = client.projects.findByWorkspace(workspace.id);
		Long pId = new Long(0);

		for (Project project : projects) {

			at.ac.wu.asana.db.model.Project dbProject = new at.ac.wu.asana.db.model.Project();
			dbProject.id = pId++;
			dbProject.workspaceId = workspace.id;
			dbProject.name = project.name;

			session.insert(dbProject);

		}
	}

	public static void asanaChangesToDB(String pat, String workspaceName, 
			String csvOutFile, String specificProject, String specificTask, Boolean onlyTaskStories, StatelessSession session){
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


		boolean foundSpecificProject = false;
		Long pId = new Long(0);

		for (Project project : projects) {

			if(specificProject != null){
				System.out.print("Looking for "+ specificProject + " ... ");
				if(!project.name.contains(specificProject)){
					continue;
				}
			}
			System.out.println("Found ("+project.name+")");
			
			at.ac.wu.asana.db.model.Project p = new at.ac.wu.asana.db.model.Project(project, pId);
			p.workspaceId = workspace.id;
			p.workspaceName = workspace.name;
			
			session.insert(p);
			pId++;
			
			
			System.out.println("Retrieving all the tasks and subtasks.");
			List<Task> tasks;
			try {
				tasks = client.tasks.findByProject(project.id).execute();

				List<Task> allTasksAndSubtasks = null;

				if(onlyTaskStories){
					allTasksAndSubtasks = tasks;
				}
				else{
					allTasksAndSubtasks = ExtractStructuralDataChanges.getAllNestedSubtasks(client, tasks);
				}

				System.out.println("Scanning "+project.name+ " containing "+allTasksAndSubtasks.size()+ " tasks and subtasks.");
				//				List<StructuralDataChange> changes = new ArrayList<StructuralDataChange>();
				for (Task task : allTasksAndSubtasks) {//find the stories and create the StructuralDataChanges
					if(specificTask!=null){
						if(!task.name.contains(specificTask))
							continue;
					}

					persistTask(task, p, session);
					
					CollectionRequest<Story> stories = client.stories.findByTask(task.id);
					System.out.println("Extracting stories (events) of "+task.name);
					
					for (Story story : stories) {
						StructuralDataChange change = new StructuralDataChange(task, story, client.users.me().execute().name.trim());
						change.setProjectId(project.id);
						change.setWorkspaceId(workspace.id);
						change.setProjectId(project.id);
						change.setProjectName(project.name);
						change.setWorkspaceId(workspace.id);
						change.setWorkspaceName(workspace.name);
						
						persistStory(change, task, client, session);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(!foundSpecificProject){
			System.out.println("Not found.");
		}

	}

	private static void persistStory(StructuralDataChange change, Task task, Client client, StatelessSession session) {
		at.ac.wu.asana.db.model.Story s = new at.ac.wu.asana.db.model.Story();
		
		s.action = change.getAction();
		s.asanaId = change.getEventId();
		long datetimeValue = change.getDateTime().getValue();
		s.date = new Date(datetimeValue);
		s.time = new Time(datetimeValue);
			
		s.timestamp = new Timestamp(datetimeValue);	
		s.text = change.getRawDataText();
		s.type = change.getMessageType();
		s.user = change.getActor();
		
		at.ac.wu.asana.db.model.Task t = new at.ac.wu.asana.db.model.Task(task);
		
//		if(allTasks.contains(t)){
//			System.out.println("have it! "+t.parent);
//			for (at.ac.wu.asana.db.model.Task task2 : allTasks) {
//				if(task2.equals(t)){
//					s.task = task2;
//				}
//			}
//		}
//		else
			s.task = t;
		
		
		System.out.println("Saving "+ s + " "+s.task);
		
		session.insert(s);
	}

	private static void persistTask(Task task, at.ac.wu.asana.db.model.Project theProject, StatelessSession session) {
		at.ac.wu.asana.db.model.Task t = new at.ac.wu.asana.db.model.Task();

		t.asanaId = task.id;
		t.name = task.name;
		
		t.project = theProject;
		
		if(task.parent!=null){
			t.parent =  new at.ac.wu.asana.db.model.Task(task.parent);
		}

//		allTasks.add(t);
		
		System.out.println("Saving "+ t + " parent: "+t.parent);
		

	}

	private static Options initOpts() {
		Option csvOutFile   = new Option("csv", "csv", true, "use given file for csv logging");
		Option pat = new Option("pat", "personalAccessToken", true, "use given personalAccessToken");
		Option wspace = new Option("ws", "workspace", true, "use given workspace name");
		Option project = new Option("p", "project", true, "use given project name");
		Option role = new Option("r", "role", true, "use given role name");
		Option db = new Option("db", "database", true, "use given database name");
		Option onlyTaskStories = new Option("ots", "onlyTaskStories", false, "use given role name");

		opts.addOption(wspace).
		addOption(csvOutFile).
		addOption(pat).
		addOption(project).
		addOption(role).
		addOption(db).
		addOption(onlyTaskStories);

		return opts;
	}

}
