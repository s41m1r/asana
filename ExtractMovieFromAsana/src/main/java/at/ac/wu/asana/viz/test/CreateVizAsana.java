package at.ac.wu.asana.viz.test;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.hibernate.Query;
import org.hibernate.SessionFactory;

import at.ac.wu.asana.db.utils.DatabaseConnector;
import at.ac.wu.asana.model.AsanaActions;
import at.ac.wu.asana.model.StructuralDataChange;

public class CreateVizAsana {

	public static void main(String[] args) {
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

		Graph graph = new SingleGraph("Asana graph");
		
		graph.addAttribute("ui.stylesheet", "url('file:///home/saimir/asana/ExtractMovieFromAsana/src/resources/styleSheetAsana.css')");
		graph.setStrict(false);
		graph.setAutoCreate(true);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");

		Node master = graph.addNode("11347525454570");

		master.addAttribute("ui.class", "circleClass");
		master.addAttribute("ui.label", "☺ Organisations Roles");

		graph.display();

		addNodesStepByStep(graph, master);
		
		SpriteManager sManager = new SpriteManager(graph);
		Sprite s1 = sManager.addSprite("S1");
		s1.setPosition(1, 1, 0);
		s1.setAttribute("ui.label", "End of movie");		
	}

	private static void addNodesStepByStep(Graph graph, Node master) {

		SessionFactory sf = DatabaseConnector.getSessionFactory("asana_manual2");

		org.hibernate.Session session = sf.openSession();
		Query queryAllDates = session.createSQLQuery("SELECT DISTINCT date FROM `TABLE 3` ORDER BY date ");
		Query queryEvents = session.createSQLQuery("SELECT * FROM `TABLE 3` WHERE date =:date ORDER by timestamp ASC");

		List<String> result = (List<String>) queryAllDates.list();		
		System.out.println("Retrieved "+result.size()+ " days of history.");
		int i = 1;
		for (String d : result) {
			queryEvents.setDate("date", Date.valueOf(d));

			List<Object> events = queryEvents.list();

			System.out.println(events.size()+" in day "+i++);
			List<StructuralDataChange> changeEvents = new ArrayList<StructuralDataChange>();
			for (Object e : events) {
				Object[] row = (Object[]) e;
				String[] str = toStrObjArray(row);
				StructuralDataChange sdc = StructuralDataChange.fromString(str);
				changeEvents.add(sdc);
				//				System.out.println("i="+i+++" "+sdc.getTaskId()+" "+sdc.getTypeOfChangeDescription()+" "+
				//				sdc.getCreatedAt()+" prjId="+sdc.getProjectId());
			}
			List<Node> nodesAdded = addToGraph(graph, changeEvents);
			sleep(1000);
			for (Node node : nodesAdded) {
				node.addAttribute("ui.style", "fill-color: rgb(155,0,0);");
			}
		}
		sf.close();
	}

	private static String[] toStrObjArray(Object[] row) {
		String[] res = new String[row.length];
		for(int i=0; i<row.length; i++)
			res[i] = row[i].toString();
		return res;
	}

	private static List<Node> addToGraph(Graph graph, List<StructuralDataChange> events) {
		List<Node> nodesAdded = new ArrayList<Node>();
		for (StructuralDataChange e : events) {
			Node n = graph.getNode(e.getTaskId());
			if(n==null) {				
				if(e.getIsSubtask()) {
					org.graphstream.graph.Edge ed = 
							graph.addEdge(e.getParentTaskId()+e.getTaskId(), e.getParentTaskId(), e.getTaskId());
					nodesAdded.add(ed.getNode0());
					nodesAdded.add(ed.getNode1());
				}
				else {
					org.graphstream.graph.Edge ed =
							graph.addEdge(e.getProjectId()+e.getTaskId(), e.getProjectId(), e.getTaskId());
					nodesAdded.add(ed.getNode0());
					nodesAdded.add(ed.getNode1());
				}

				n = graph.getNode(e.getTaskId());
				if(e.getTaskName()!=null)
					n.addAttribute("ui.label", e.getTaskName());

				if(e.isRole()) {
					n.addAttribute("ui.class", "role");
				}

				if(StructuralDataChange.isCircle(e.getTaskName())) {
					n.addAttribute("ui.class", "circleClass");
				}
			}
			switch(e.getTypeOfChange()) {
			case AsanaActions.ADD_SUB_ROLE:
			case AsanaActions.ADD_TO_CIRCLE:
			case AsanaActions.ASSIGN_TO_ACTOR:
			case AsanaActions.CREATE_ROLE:
				n.addAttribute("ui.class", "fill-color: green;");
				break;
			case AsanaActions.REVIVE_OR_MARK_INCOMPLETE:
				if(e.getIsSubtask()) {
					org.graphstream.graph.Edge ed = 
							graph.addEdge(e.getParentTaskId()+e.getTaskId(), e.getParentTaskId(), e.getTaskId());
					nodesAdded.add(ed.getNode0());
					nodesAdded.add(ed.getNode1());
				}
				else {
					org.graphstream.graph.Edge ed =
							graph.addEdge(e.getProjectId()+e.getTaskId(), e.getProjectId(), e.getTaskId());
					nodesAdded.add(ed.getNode0());
					nodesAdded.add(ed.getNode1());
				}
				n.addAttribute("ui.class", "role");
				break;
			case AsanaActions.COMPLETE_ROLE:
			case AsanaActions.DETELE_OR_MARK_COMPLETE:
			case AsanaActions.REMOVE_FROM_CIRCLE:
				n.addAttribute("ui.class", "deleted");
//				sleep(1000);
//				graph.removeNode(n);
				break;
			default:
				break;
			}
//			sleep(500);
		}
		return nodesAdded;
	}

	private static void sleep(int time) {
		try { Thread.sleep(time); } catch (Exception e) {}
	}
}
