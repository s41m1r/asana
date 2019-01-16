package at.ac.wu.asana.tryout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.criterion.CountProjection;

import com.asana.Client;
import com.asana.models.Project;
import com.asana.models.Story;
import com.asana.models.Task;
import com.asana.models.Workspace;
import com.asana.requests.CollectionRequest;
import com.google.api.client.util.Sleeper;
import com.opencsv.CSVWriter;

/**
 * APIs
 * 
 * 		Erik Strauss 	0/7506773dd733d4efc682cd23d5949372
 *		Saimir 			0/db1da020bc43e507a079b852e3f01360
 *
 */
public class App 
{
	final static String PAT_SAIMIR = "0/db1da020bc43e507a079b852e3f01360";
	final static String PAT_ERIK = "0/7506773dd733d4efc682cd23d5949372";

	final static String WSPACE = "Springest"; //InfoBiz
	//	final static String WSPACE = "InfoBiz"; //InfoBiz

	public static void main( String[] args ) throws IOException
	{
		
//		Character c = 9786;
//		
//		System.out.println(c);

		//				getInfoBizEvents();
		//				extractAsanaInfo();
		//		Date now = new Date();
//		collectProjects();
		collectTasks();
//		collectStories();

		
		//		SimpleDateFormat dateOnly = new SimpleDateFormat("dd/MM/yyyy");
		//		SimpleDateFormat timeOnly = new SimpleDateFormat("HH:mm:ss");
		//		System.out.println(dateOnly.format(now));
		//		System.out.println(timeOnly.format(now));

	}
	private static void collectStories() {
		List<String> allTasks = getAllSmileyTaskIds("tasks/allTasks.csv");
		List<String> allTaskNames = getAllSmileyTaskNames("tasks/allTasks.csv");
		Iterator<String> taskName = allTaskNames.iterator();
		Client client = Client.accessToken(PAT_ERIK);
		client.options.put("page_size", 100);
		client.options.put("max_retries", 100);
		client.options.put("poll_interval", 10);
		int count = 0;
		
		System.out.println("Going to print "+allTasks.size()+ " stories.");
		for (String taskId : allTasks) {
			System.out.println("Printing stories of "+taskId);
			count++;
			if(count%50==0)
				System.out.println(100.0*count/allTasks.size()+" completed so far.");
			String tname = taskName.next();
			printAllStoriesOfTask(taskId.substring(1, taskId.length()-1), tname.substring(1, tname.length()-1), client);
		}		
	}
	private static List<String> getAllSmileyTaskNames(String filename) {
		List<String> ids = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			String p;
			while((p=br.readLine())!=null){
				Character c = 9786;
				if(p.split(",")[1].charAt(1)==c)
					ids.add(p.split(",")[3].trim()); //structure is projId,projName,taskId,taskName
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ids;
	}
	
	private static void printAllStoriesOfTask(String taskId, String taskName,  Client client) {
		try {
			if(new File("stories/"+taskId+".csv").exists()){
				System.out.println("Already exists. Skipped.");
				return;
			}
			
			CSVWriter pr = new CSVWriter(new FileWriter("stories/"+taskId+".csv"));
			List<Story> stories = new ArrayList<Story>(); 
			Iterable<Story> storiesIt = null;
			try{
				storiesIt = client.stories.findByTask(taskId).execute();
			}
			catch (SocketTimeoutException e) {
				System.out.println(e.getMessage());
				System.out.println("Retrying");
				client = Client.accessToken(PAT_ERIK);
				storiesIt = client.stories.findByTask(taskId).execute();
			}
//			String header = "ProjectId,"
//					+ "ProjectName,"
//					+ "TaskId,"
//					+ "taskName,"
//					+ "task.createdAt,"
//					+ "task.modifiedAt,"
//					+ "task.assignedTo,task.assigneeStatus,task.assignee,task.completedAt,"
//					+ "task.completed,task.parents,pathToHere";
//			pr.writeNext(header.split(","));
			for (Story s : storiesIt) {
				stories.add(s);
			}
			
			printStories(stories, pr, taskId, taskName);
			
			pr.flush();
			pr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void printStories(List<Story> stories, CSVWriter pr, String taskId, String taskName) {
		for (Story story : stories) {
			pr.writeNext(new String[]{
					story.id,
					story.type,
					(new Timestamp(story.createdAt.getValue())).toString(),
					story.createdBy.id,
					story.createdBy.name,
					story.text,
					taskId,
					taskName});
		}
		
	}
	
	private static List<String> getAllSmileyTaskIds(String filename) {
		List<String> ids = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			String p;
			while((p=br.readLine())!=null){
				Character c = 9786;
				if(p.split(",")[1].charAt(1)==c)
					ids.add(p.split(",")[2].trim()); //structure is projId,projName,taskId,taskName
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ids;

	}
	
	private static void collectTasks() {
		List<String> allProjects = getAllProjectIds("onlySmileyProjects.csv");
		List<String> allProjectNames = getAllProjectNames("onlySmileyProjects.csv");
		Iterator<String> it = allProjectNames.iterator();
		for (String p : allProjects) {
			System.out.println("Printing taks of "+p);
			printAllTasksOfProject(p, it.next());
		}
	}
	
	private static void collectSubtasks() {
		List<String> allTasks = getAllSmileyTaskIds("tasks/allTasks.csv");
		List<String> allTaskNames = getAllSmileyTaskNames("tasks/allTasks.csv");
		Iterator<String> taskName = allTaskNames.iterator();
		Client client = Client.accessToken(PAT_ERIK);
		client.options.put("page_size", 100);
		client.options.put("max_retries", 100);
		client.options.put("poll_interval", 10);
		int count = 0;
		
		System.out.println("Going to print "+allTasks.size()+ " stories.");
		for (String taskId : allTasks) {
			System.out.println("Printing subtasks of "+taskId);
			count++;
			if(count%50==0)
				System.out.println(100.0*count/allTasks.size()+" completed so far.");
			String tname = taskName.next();
//			printAllSubtasksOfTask(taskId.substring(1, taskId.length()-1), tname.substring(1, tname.length()-1), client);
		}
	}

	private static List<String> getAllProjectIds(String filename) {
		List<String> ids = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			String p;
			while((p=br.readLine())!=null){
				ids.add(p.split(",")[0].trim());
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ids;
	}
	
	private static List<String> getAllProjectNames(String filename) {
		List<String> names = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			String p;
			while((p=br.readLine())!=null){
				names.add(p.split(",")[1].trim());
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return names;
	}

	private static void printAllTasksOfProject(String p, String pname) {
		try {
			CSVWriter pr = new CSVWriter(new FileWriter("tasks/"+p+"-tasks.csv"));
			
			Client client = Client.accessToken(PAT_ERIK);
			List<Task> tasks = new ArrayList<Task>(); 
			Iterable<Task> taskIt = client.tasks.findByProject(p);
			String header = "ProjectId,"
					+ "ProjectName,"
					+ "TaskId,"
					+ "taskName,"
					+ "task.createdAt,"
					+ "task.modifiedAt,"
					+ "task.assignedTo,task.assigneeStatus,task.assignee,task.completedAt,"
					+ "task.completed,task.parents,pathToHere";
			pr.writeNext(header.split(","));
			for (Task t : taskIt) {
				tasks.add(t);
			}
			
			printTasks(tasks, pr, p, pname, false);
			
			pr.flush();
			pr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void printTasks(List<Task> tasks, CSVWriter pr, String pid, String pname, boolean isSubtask){
		for (Task task : tasks) {
			pr.writeNext(new String[]{pid,pname,task.id,task.name,isSubtask+""});
//					+
//					","+task.createdAt + 
//					","+task.modifiedAt + 
//					","+task.assigneeStatus + 
//					","+ task.assignee +
//					","+task.completedAt + 
//					","+task.completed + 
//					",:"+StructuralDataChange.getPath(task)
			
		}
	}

	private static void collectProjects() {
		Client client = Client.accessToken(PAT_ERIK);
		Workspace workspace = null;

		Iterable<Workspace> workspaces = client.workspaces.findAll();

		for (Workspace wspace : workspaces) {
			if (wspace.name.equals("Springest")) {
				workspace = wspace;
				break;
			}
		}

		System.out.println("Workspace id:"+workspace.id+ " name:"+ workspace.name);

		Iterable<Project> projects = client.projects.findByWorkspace(workspace.id);
		int count=0;
		Iterator<Project> it= projects.iterator();
		try {
			PrintWriter pr = new PrintWriter(new FileOutputStream("allProjectIds.txt"));
			PrintWriter pr2 = new PrintWriter(new FileOutputStream("allProjectNames.txt"));
			PrintWriter pr3 = new PrintWriter(new FileOutputStream("onlySmileyProjects.csv"));
	
			Character c = 9786; //smiley character
			while (it.hasNext()) {
				Project p = it.next();
				pr.println(p.id);
				pr2.println(p.name);
				if(p.name.contains(c+"")){
					pr3.println(p.id+ ","+p.name);
//					System.out.println(p.id);
				}
				count++;
			}
			pr.flush();
			pr2.flush();
			pr3.flush();
			pr.close(); pr2.close(); pr3.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		System.out.println("There are "+ count+ " projects.");
	}

	/**
	 * 
	 */
	private static void extractAsanaInfo() {
		Client client = Client.accessToken(PAT_ERIK);
		//		client.options.put("page_size"	 100);

		//	 	printAllWorkspaces(client); 
		Workspace workspace = null;

		Iterable<Workspace> workspaces = client.workspaces.findAll();

		for (Workspace wspace : workspaces) {
			if (wspace.name.equals("Springest")) {
				workspace = wspace;
				break;
			}
		}

		System.out.println("Workspace id:"+workspace.id+ " name:"+ workspace.name);

		Iterable<Project> projects = client.projects.findByWorkspace(workspace.id);

		Project theProject = null;

		for (Project project : projects) {
			//			if(project!=null)
			//				System.out.println("Project name "+project.name);
			if(project.name.contains("Organisations Roles")){
				theProject = project;

				//				char[] chars = project.name.toCharArray();
				//				
				//				for (char c : chars) {
				//					System.out.println("Character: "+c+" Unicode:"+(int)c);
				//				}

				break;
			}
		}

		//		if(theProject == null){
		//			System.err.println("Project Rep Link (Organisations) not found.");
		//			System.exit(-1);
		//		}
		int i = 0;
		for (Project project : projects) {
			if(i++ > 5) break;

			theProject = project;

			CollectionRequest<Task> tasks = client.tasks.findByProject(theProject.id);

			try {
				PrintWriter rolesFileWriter = new PrintWriter(new FileOutputStream(
						"/home/saimir/ownCloud/Holacracy/API/StructuralDataChanges.csv"));

				System.out.println("=== Circle (Project): "+theProject.name);

				rolesFileWriter.println("=== Circle (Project): "+theProject.name);
				CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
				String[] header = new String[]{"Timestamp", "Role", "Actor", "Action", "RoleId", "ActionId"};

				for (Task task : tasks) {//these are the roles
					if(!task.name.contains("Rep Link (Organisations)"))
						continue;

					System.out.println("Task id:\t"+task.id+ 
							"	 name:"+task.name+
							"	 task.createdAt:"+task.createdAt + 
							"	 task.modifiedAt:"+task.modifiedAt + 
							"	 task.assignee.name:"+task.assigneeStatus + 
							"	 task.completedAt:"+task.completedAt
							);

					//					rolesFileWriter.println("Task id:\t"+task.id+ 
					//							"\tname:"+task.name+
					//							"\ttask.createdAt:"+task.createdAt + 
					//							"\ttask.modifiedAt:"+task.modifiedAt + 
					//							"\ttask.assignee.name:"+task.assigneeStatus + 
					//							"\ttask.completedAt:"+task.completedAt
					//							);

					CollectionRequest<Story> stories = client.stories.findByTask(task.id);
					for (Story story : stories) {
						String action = "";

						//						if(story.type.startsWith("comment"))
						//							action = "change";
						//						else 
						//							if(story.text.startsWith("added"))
						//								action = "create";
						//							else if(story.text.startsWith(""))

						//					csvWriter.writeNext(new String[]{
						//							"Timestamp", 
						//							"Role", 
						//							"Actor", 
						//							"Action", 
						//							"RoleId", 
						//							story.id
						//							});

						//								System.out.println("Story id:"+story.id + 
						//										"	 type:"+ story.type + 
						//										"	 text:"+ story.text +
						//										"	 createdAt:"+ story.createdAt +
						//										"	 createdBy:"+ story.createdBy.name
						//										);
						rolesFileWriter.println("Story id:"+story.id + 
								"\ttype:"+ story.type + 
								"\ttext:"+ story.text +
								"\tcreatedAt:"+ story.createdAt +
								"\tcreatedBy:"+ story.createdBy.name
								);

					}			
				}
				rolesFileWriter.flush();
				rolesFileWriter.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} 
		}
	}

	static void printAllWorkspaces(Client client){
		Iterable<Workspace> workspaces = client.workspaces.findAll();
		for (Workspace workspace: workspaces) {
			System.out.println("Workspace: " + workspace.name);
		}
	}

	static void getInfoBizEvents(){
		Client client = Client.accessToken(PAT_SAIMIR);
		//		client.options.put("page_size"	 100);

		//	 	printAllWorkspaces(client);

		Workspace workspace = null;

		Iterable<Workspace> workspaces = client.workspaces.findAll();

		for (Workspace wspace : workspaces) {
			if (wspace.name.equals("InfoBiz")) {
				workspace = wspace;
				break;
			}
		}

		System.out.println("Workspace id:"+workspace.id+ " name:"+ workspace.name);

		Iterable<Project> projects = client.projects.findByWorkspace(workspace.id);

		Project theProject = null;

		//		for (Project project : projects) {
		//			if(project!=null)
		//				System.out.println("Project name "+project.name);
		//			if(project.name.contains("Org. Structures and Roles")){
		//				theProject = project;
		//				break;
		//			}
		//		}

		//		if(theProject == null){
		//			System.err.println("Project Rep Link (Organisations) not found.");
		//			System.exit(-1);
		//		}

		for (Project project : projects) {
			theProject = project;
			System.out.println("=== Circle (Project): "+theProject.name);

			CollectionRequest<Task> taks = client.tasks.findByProject(theProject.id);
			//		CollectionRequest<Task> taks = client.tasks.findByProject("11351056125206");

			for (Task task : taks) {
				//			if(!task.name.contains("Rep Link (Org"))
				//				continue;

				System.out.println("Task id:\t"+task.id+ 
						"\tname:"+task.name+
						"\ttask.createdAt:"+task.createdAt + 
						"\ttask.modifiedAt:"+task.modifiedAt + 
						"\ttask.assignedTo:"+task.assigneeStatus + "," + task.assignee +
						"\ttask.completedAt:"+task.completedAt + 
						"\ttask.completed:"+task.completed + 
						"\ttask.parent:"+task.parent
						);

				try {
					for(Task t : client.tasks.subtasks(task.id).execute()){
						System.out.println("Subtask id:\t"+t.id+ 
								"\tname:"+t.name+
								"\ttask.createdAt:"+t.createdAt + 
								"\ttask.modifiedAt:"+t.modifiedAt + 
								"\ttask.assignedTo:"+t.assigneeStatus + "," + t.assignee +
								"\ttask.completedAt:"+t.completedAt + 
								"\ttask.completed:"+t.completed + 
								"\ttask.parent:"+t.parent
								);
						CollectionRequest<Story> stories = client.stories.findByTask(t.id);

						for (Story story : stories) {
							System.out.println("****Story id:\t"+story.id + 
									"\ttype:"+ story.type + 
									"\ttext:"+ story.text +
									"\tcreatedAt:"+ story.createdAt +
									"\tcreatedBy:"+ story.createdBy.name +
									"\ttarget:"+ story.target
									);
						}
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


				CollectionRequest<Story> stories = client.stories.findByTask(task.id);
				for (Story story : stories) {
					System.out.println("Story id:\t"+story.id + 
							"\ttype:"+ story.type + 
							"\ttext:"+ story.text +
							"\tcreatedAt:"+ story.createdAt +
							"\tcreatedBy:"+ story.createdBy.name +
							"\ttarget:"+ story.target
							);
				}
			}
		}
	}

}
