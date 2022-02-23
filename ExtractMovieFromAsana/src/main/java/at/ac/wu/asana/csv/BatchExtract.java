package at.ac.wu.asana.csv;

import com.asana.Client;
import com.asana.models.Project;
import com.asana.models.Workspace;
import com.asana.requests.ItemRequest;
import com.google.gson.JsonElement;
import com.google.protobuf.Option;

import scala.util.parsing.combinator.testing.Str;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.TCKind;

public class BatchExtract {

    final static Logger logger = Logger.getLogger("BatchExtraction");

    public static void main(String[] args) {

        String projectsFile = args[0];
        String pat = args[1];
        String ws = args[2];

        List<String[]> all = ReadInfoFromCSV.readAll(projectsFile);


        List<String> gids = new ArrayList<String>();//ReadInfoFromCSV.getColumn(0, projectsFile);
//        List<String> projectNames = new ArrayList<String>();//ReadInfoFromCSV.getColumn(9, projectsFile);

        for (String[] row: all
        ) {
            gids.add(row[0]);
//            projectNames.add(row[9]);
        }


        String tasksPerProjectCSV = "tasksPerProject.csv";

        Client client = initAsana(pat);

        computeTasksPerProject(client, ws, gids, tasksPerProjectCSV);


//        for (String gid :
//                gids) {
//            ExtractStructuralDataChanges.extractTasksFromPID2(pat, ws, gid+".csv", gid, null, false, false);
//        }
    }

    private static Client initAsana(String pat) {
        Client client = Client.accessToken(pat);
        client.options.put("page_size", 100);
        client.options.put("max_retries", 100);
        //		client.options.put("opt_fields", "resource_subtype");
        client.headers.put("asana-disable", "string_ids,new_sections");
        //		client.options.put("poll_interval", 10);

        return client;
    }

    private static void computeTasksPerProject(Client client, String ws, List<String> gids, String outFile) {

        try {
            boolean found = false;

            for (Workspace w : client.workspaces.findAll()) {
                if (w.name.equals(ws)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                logger.severe("Workspace not found.");
                System.exit(-1);
            }

            Map<String, String> mapGidName = new HashMap<String, String>();

            LinkedHashMap<String, Integer> tasksPerProject = new LinkedHashMap<String, Integer>();

            for (String gid : gids) {
            	Project project;
            	try {
            		project = client.projects.findById(gid).execute();
            	}
            	catch (com.asana.errors.NotFoundError e) {
            		logger.log(Level.SEVERE, "Project not found for "+gid);
            		continue;
				}
                
                Integer tCount = 0;
                ItemRequest<JsonElement> request = client.projects.getTaskCountsForProject(gid);
                request.query.put("opt_fields", Arrays.asList(
                		"num_tasks","num_incomplete_tasks","num_completed_tasks"));
                JsonElement element = request.execute();
                if(!element.isJsonNull())
                	tCount = element.getAsJsonObject().get("num_tasks").getAsInt();
                
                logger.info("Found "+tCount+" tasks in project " + project.name);
               
                mapGidName.put(gid, project.name);
                tasksPerProject.put(gid, tCount);
            }

            WriteUtils.writeTaskCounts(mapGidName, tasksPerProject, outFile);

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


}
