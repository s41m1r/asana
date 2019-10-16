package at.ac.wu.asana.tryout;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.asana.Client;
import com.asana.models.Project;
import com.asana.models.Story;
import com.asana.models.Task;

public class TryTaskType {
	public static void main(String[] args) throws IOException {
		String pat = "0/7506773dd733d4efc682cd23d5949372";
		Client client = Client.accessToken(pat);
		client.options.put("page_size", 100);
		client.options.put("max_retries", 100);
		client.options.put("archived", true);
		
		Task tasksIt = client.tasks.findById("274615754119628").query("resource_subtype", "default_task").
				option("fields",
						Arrays.asList(
								"created_at", "name", "completed",
								"tags","completed_at", "notes", 
								"modified_at", "parent", "parent.name", 
								"assignee", "assignee.name", "include_archived", 
								"resource_subtype", "projects.name", "memberships.project.name"))
								.execute();
//		
		Task task = tasksIt;
		System.out.println(task.resourceSubtype+" - "+task.resourceType+
				" - "+task.name+" - "+task.projects+" "+task.memberships + 
				"- parent="+task.parent.gid + " "+task.parent.name);
		
		List<Story> stories = client.stories.findByTask("428286298228869").execute();
		
		for (Story story : stories) {
			System.out.println(story.gid 
					+","+ story.resourceType
					+","+ story.resourceSubtype
					+","+ story.target
					+","+ story.text
					+","+ story.createdBy.name
					);
		}
		
//		List<Task> tasks = client.tasks.findByProject("388515769387194").execute();
//		System.out.println("I found "+tasks.size()+" tasks.");
		
//		for (Task task : tasksIt) {
//			System.out.println(task.resourceSubtype+" - "+task.resourceType+
//					" - "+task.name+" - "+task.projects+" "+task.memberships);
//		}
	}
}
