package at.ac.wu.asana.tryout;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.asana.Client;
import com.asana.models.CustomFieldSetting;
import com.asana.models.Project;
import com.asana.models.Team;
import com.asana.models.User;
import com.asana.models.Workspace;
import com.google.api.client.util.DateTime;
import com.opencsv.CSVWriter;

import at.ac.wu.asana.csv.ExtractStructuralDataChanges;

public class ExtractAllProjects {

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		System.out.println(new Date() + " Extraction started.");
		extractProjects(args);
		System.out.println("All done in "+ ExtractStructuralDataChanges.getElapsedTime(System.currentTimeMillis(), start));
	}

	/**
	 * @param args
	 */
	public static void extractProjects(String[] args) {
		CommandLineParser lineParser = new DefaultParser();
		CommandLine cmd = null;
		Options opts = new Options();
		opts.addOption(new Option("csv", true, "the output file produced"))
				.addOption(new Option("pat", true, "the personal access token"))
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

		Iterable<Project> projects = client.projects.findByWorkspace(workspace.id);
		String filename = cmd.getOptionValue("csv");
		CSVWriter writer = null;
		try {
			writer = new CSVWriter(new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(filename), StandardCharsets.UTF_8)));
			String[] header = new String[]{
					"id",
					"color",
					"createdAt",
					"customFieldSettings",
					"followers",
					"isArchived",
					"isPublic",
					"members",
					"modifiedAt",
					"name",
					"notes",
					"owner",
					"team",
					"workspace"
					};
			
			writer.writeNext(header);

			for (Project project : projects) {
				String[] row = getCSVRow(
						project.id,
						project.color,
						project.createdAt,
						project.customFieldSettings,
						project.followers,
						project.isArchived,
						project.isPublic,
						project.members,
						project.modifiedAt,
						project.name,
						project.notes,
						project.owner,
						project.team,
						project.workspace
						);
				
				System.out.println(project.id+" "+project.name);
				writer.writeNext(row);
			}
			
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String[] getCSVRow(String id, String color, DateTime createdAt,
			Collection<CustomFieldSetting> customFieldSettings, 
			Collection<User> followers, boolean isArchived,
			boolean isPublic, Collection<User> members, 
			DateTime modifiedAt, String name, String notes, User owner,
			Team team, Workspace workspace) {
		String[] result = new String[14];
		
		result[0] = id;
		result[1] = color+"";
		result[2] = createdAt+"";
		result[3] = toStringCustomFields(customFieldSettings);
		result[4] = toStringUsersColletion(followers);
		result[5] = isArchived+"";
		result[6] = isPublic + "";
		result[7] = toStringUsersColletion(members);
		result[8] = modifiedAt+"";
		result[9] = name+"";
		result[10] = notes+"";
		result[11] = toStringUser(owner);
		result[12] = (team==null)? null : "["+ team.id+"—"+team.name+"]";
		result[13] = (workspace==null)? null : "["+ workspace.id+"—"+workspace.name+"]";
				
		return result;
	}

	private static String toStringUser(User owner) {
		if(owner == null)
			return null;
		return "["+ owner.id+"—"+owner.name+"]";
	}

	private static String toStringUsersColletion(Collection<User> followers) {
		if(followers == null)
			return null;
		String res = "[";
		String sep = "–";
		String seprow = "\\";
		for (User user : followers) {
			if(user == null)
				return null;
			res += user.id + sep + user.name;
			res += seprow;
		}
		return res+"]";
	}

	private static String toStringCustomFields(Collection<CustomFieldSetting> customFieldSettings) {
		if(customFieldSettings==null)
			return null;
		String res = "[";
		String sep = "–";
		String seprow = "\\";
		for (CustomFieldSetting customFieldSetting : customFieldSettings) {
			if(customFieldSetting==null)
				return null;
			res += customFieldSetting.id + sep + customFieldSetting.isImportant;
			res += seprow;
		}
		return res+"]";
	}

}
