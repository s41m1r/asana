package at.ac.wu.asana.viz.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

import com.google.api.client.util.DateTime;
import com.opencsv.CSVReader;

import at.ac.wu.asana.csv.model.StructuralDataChange;

public class CreateVizEasy {
	
	public static void main(String[] args) {
		
		List<String> projects = readFromFile("/home/saimir/ownCloud/PhD/Collaborations/Waldemar/API/allSmileys.txt");
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

		Graph graph = new MultiGraph("Asana graph");
		
		graph.addAttribute("ui.stylesheet", "url('file:///home/saimir/eclipse-workspace/ExtractMovieFromAsana/src/resources/styleSheetAsana.css')");
        graph.setStrict(false);
		graph.setAutoCreate(true);
//		graph.addAttribute("ui.quality");
//		graph.addAttribute("ui.antialias");
        
		Node master = graph.addNode("InfoBiz");
		
		master.addAttribute("ui.class", "circleClass");
		master.addAttribute("ui.label", "Organisations Roles");
        
        graph.display();
		
//		addAllProjects(graph, master, projects);
		
		String csv = "/home/saimir/ownCloud/PhD/Collaborations/Waldemar/API/Organisations Roles 8.csv";
		
		addNodesStepByStep(graph, master, csv);
	}

	private static void addNodesStepByStep(Graph graph, Node master, String csv) {
		
		try {
			CSVReader csvReader = new CSVReader(new InputStreamReader(new FileInputStream(csv), "UTF-16"));
			
			List<String[]> list = csvReader.readAll();
			list.remove(0); //the header
			
			List<StructuralDataChange> events = null;
			while(list!=null && (events = getEventsOfNextDate(list))!=null) {
				addToGraph(graph,master,events);
				sleep();
			}
			csvReader.close();
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void addToGraph(Graph graph, Node master, List<StructuralDataChange> events) {
		for (StructuralDataChange e : events) {	
			if(e.getIsSubtask()) {
				graph.addEdge(e.getParentTaskId()+e.getTaskId(), e.getParentTaskId(), e.getTaskId());
			}
			else {
				graph.addEdge(master.getId()+e.getTaskId(), master.getId(), e.getTaskId());
			}
			Node n = graph.getNode(e.getTaskId());
			
			if(e.isRole()) {
				n.addAttribute("ui.class", "role");
//				n.addAttribute("ui.label", e.getTaskName());
			}
			else {
				n.addAttribute("ui.label", e.getTaskName());
			}
		}
	}

	private static List<StructuralDataChange> getEventsOfNextDate(List<String[]> list) {
		
//		timestamp	role id (taskId)	role id of parent (parentTaskId)	role name (taskName)	rawDataText	messageType	typeOfChange	typeOfChange-Description	isRole	roleCreatedAt (taskCreatedAt)	actor (createdByName)	circle (projectName)	isCicle	createdById	assigneeId	assigneeName	eventId	projectId	workspaceId	workspaceName	isSubtask	parentTaskName	date	time	taskCompletedAt	taskModifiedAt	taskNotes
		List<StructuralDataChange> res = new ArrayList<StructuralDataChange>();
		System.out.println("Attempting to parse date: "+list.get(0)[0]);
		DateTime thisDate = DateTime.parseRfc3339(list.get(0)[0]);
		Date referenceDate = new Date(thisDate.getValue());
		Calendar reference =  Calendar.getInstance();
		reference.setTime(referenceDate);
		for (int i = 0; list !=null && i < list.size(); i++) {
			DateTime dateTime = DateTime.parseRfc3339(list.get(i)[0]);
			Date now = new Date(dateTime.getValue());
			Calendar nowCal = Calendar.getInstance();
			nowCal.setTime(now);
			
			if(nowCal.get(Calendar.DATE) == reference.get(Calendar.DATE) && 
					nowCal.get(Calendar.MONTH) == reference.get(Calendar.MONTH) &&
					nowCal.get(Calendar.YEAR) == reference.get(Calendar.YEAR)
					) {
				res.add(StructuralDataChange.fromString(list.get(i)));
			}
			else 
				break;
		
			list.remove(i);
		}
		return res;				
	}

	private static void addAllProjects(Graph graph, Node master, List<String> projects) {		
		for (String string : projects) {
			graph.addEdge(string+master.getId(), string, master.getId());
		}
	}

	private static List<String> readFromFile(String string) {
		List<String> list = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(string)));
			String line = null;
			while( (line = br.readLine())!= null ) {
				list.add(line);
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}
	
	protected static void sleep() {
		try { Thread.sleep(200); } catch (Exception e) {}
	}
}
