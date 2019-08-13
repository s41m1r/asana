package at.ac.wu.asana.tryout;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.asana.Client;
import com.asana.models.Task;

public class TryTaskType {
	public static void main(String[] args) throws IOException {
		String pat = "0/7506773dd733d4efc682cd23d5949372";
		Client client = Client.accessToken(pat);
		client.options.put("page_size", 100);
		client.options.put("max_retries", 100);
		client.options.put("archived", true);
		
		List<Task> tasksIt = client.tasks.findByProject("12530878841888").query("resource_subtype", "default_task").
				option("fields",
						Arrays.asList(
								"created_at", "name", "completed",
								"tags","completed_at", "notes", 
								"modified_at", "parent", "parent.name", 
								"assignee", "assignee.name", "include_archived", 
								"resource_subtype"))
								.execute();
		
		for (Task task : tasksIt) {
			System.out.println(task.resourceSubtype+" - "+task.resourceType+
					" - "+task.name+" - "+task.customFields+" ");
		}
	}
}
