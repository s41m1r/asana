package at.ac.wu.asana.tryout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.asana.Client;
import com.asana.models.Project;
import com.asana.models.Workspace;
import com.asana.requests.CollectionRequest;

import at.ac.wu.asana.csv.ExtractStructuralDataChanges;

public class DownloadAllCiclesAndSubcircles {
	
	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		System.out.println(new Date() + " Extraction started.");
		
//		-pat "0/db1da020bc43e507a079b852e3f01360" -ws "InfoBiz" -p "Org. Structures and Roles"
//		-pat "0/7506773dd733d4efc682cd23d5949372" -ws "Springest" -p "☺ Organisations Roles"
		String pat = "0/7506773dd733d4efc682cd23d5949372";
		String ws = "Springest";
		String mainProject ="☺ Organisations Roles";
		
		List<Project> circles = extractAllCirclesAndSubcircles(pat,ws,mainProject);
		
		for (Project project : circles) {
			System.out.println(project.name + " "+project.createdAt);
		}
		
		System.out.println("All done in "+ ExtractStructuralDataChanges.getElapsedTime(System.currentTimeMillis(), start));
	}

	private static List<com.asana.models.Project> extractAllCirclesAndSubcircles(String pat, String mainProject, String ws) {
		Client client = Client.accessToken(pat);
		client.options.put("page_size", 100);
		client.options.put("max_retries", 100);
		//		client.options.put("poll_interval", 10);
		
		Iterable<Workspace> workspaces = client.workspaces.findAll();
		Workspace workspace = null;

		for (Workspace wspace : workspaces) {
			if (wspace.name.equals(mainProject)) {
				workspace = wspace;
				break;
			}
		}
		
		CollectionRequest<com.asana.models.Project> projects =  client.projects.findByWorkspace(workspace.id).
				option("fields",
				Arrays.asList(
						"created_at", "name", "completed",
						"tags","completed_at", "notes", 
						"modified_at", "parent", "parent.name", 
						"assignee", "assignee.name"));
		List<com.asana.models.Project> res = new ArrayList<com.asana.models.Project>();
		
		for (com.asana.models.Project p : projects) {
			if(isCircle(p.name))
				res.add(p);
		}
		
		System.out.println("Found: "+workspace.name);
		
		return res;
	}
	
	private static boolean isCircle(String input) {
		return startsWithSmiley(input) && endsWithRoles(input);
	}

	private static boolean startsWithSmiley(String input) {
		return input.startsWith("☺");
	}

	private static boolean endsWithRoles(String input) {
		return input.toLowerCase().endsWith("roles");
	}

}
