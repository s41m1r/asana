package at.ac.wu.asana.viz;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import at.ac.wu.asana.db.utils.DatabaseConnector;
import at.ac.wu.asana.model.AsanaActions;
import at.ac.wu.asana.model.StructuralDataChange;

public class VisualizeBasedOnCircle {
	
	static Set<String> allTaskIds = new HashSet<String>();

	
	public static void main(String[] args) {
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

		Graph graph = new SingleGraph("Asana graph");

		graph.addAttribute("ui.stylesheet", 
				"url('file:///home/saimir/asana/ExtractMovieFromAsana/src/resources/styleSheetAsanaMinimal.css')");
		graph.setStrict(false);
		graph.setAutoCreate(true);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");

		Viewer viewer = graph.display();
		final View view = viewer.getDefaultView();

		Node master = graph.addNode("864733919245"); // Springest center
				
		allTaskIds.add(master.getId());

		//		viewer.disableAutoLayout();

		master.addAttribute("ui.class", "master");
		master.addAttribute("ui.label", "Springest");

		SpriteManager sManager = new SpriteManager(graph);
		Sprite s1 = sManager.addSprite("S1");
		s1.setPosition(Units.PX, 100, 10, 0);
		s1.setAttribute("ui.label", "");
		
		int delay = 500; //ms
		addNodesStepByStep(graph, master, s1, delay);
	}

	private static void addNodesStepByStep(Graph graph, Node master, Sprite s1, int delay) {
		SessionFactory sf = DatabaseConnector.getSessionFactory("asana_manual5");
		org.hibernate.Session session = sf.openSession();

		addAllCircles(session, graph, master);

		Query queryAllDates = session.createSQLQuery("SELECT DISTINCT date "
				+ "FROM `SpringestWithCircle` "
//				+ "WHERE projectId <> '7963718816247' "
				+ "ORDER BY date");
		Query queryEvents = session.createSQLQuery("SELECT * FROM `SpringestWithCircle` "
				+ "WHERE date =:date "
//				+ "AND projectId <> '7963718816247'"
				+ "ORDER by timestamp ASC");

		List<String> result = (List<String>) queryAllDates.list();		
		System.out.println("Retrieved "+result.size()+ " days of history.");
//		s1.setAttribute("ui.label", "Days: "+ result.size());
		sleep(delay*3);

		int i = 1;
		for (String d : result) {
			queryEvents.setDate("date", Date.valueOf(d));

			List<Object> events = queryEvents.list();
			s1.setAttribute("ui.label", Date.valueOf(d) + ", day "+i+"/"+result.size()
					+ ", evts: "+events.size());
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
			for (Node n : nodesAdded) {
				if(n.getAttribute("ui.class").equals("modified")) {
					n.setAttribute("ui.style", "size: 10px;");
					n.setAttribute("ui.style", "fill-color: black;");
					n.setAttribute("ui.style", "stroke-mode: plain;");
					sleep(100);            
					n.setAttribute("ui.style", "size: 3px;");
					n.setAttribute("ui.style", "fill-color: blue;");
					n.setAttribute("ui.style", "stroke-mode: none;");
//					sleep(500);	
					allTaskIds.add(n.getId());
				}
			}
//			Collection<Node> nodes = graph.getNodeSet();
//			for (Node n : nodes) {
//				if(n.getInDegree() == 0)
//					graph.removeNode(n);
//			}
//			sleep(500);
		}
		sf.close();
	}

	private static List<Node> addToGraph(Graph graph, List<StructuralDataChange> events) {
		List<Node> nodesAdded = new ArrayList<Node>();
		for (StructuralDataChange e : events) {
			Node n = graph.getNode(e.getTaskId());
			if(e.getCircle().equals("NO CIRCLE"))
				continue;
			if(n==null) {				
				if(e.getIsSubtask() && allTaskIds.contains(e.getParentTaskId())) {
					org.graphstream.graph.Edge ed = 
							graph.addEdge(e.getParentTaskId()+e.getTaskId(), e.getParentTaskId(), e.getTaskId());
					ed.getTargetNode().addAttribute("name", e.getTaskName());
					nodesAdded.add(ed.getNode1());
				}
				else {
					String[] circles = parseCommaSeparated(e.getCircle());
					String[] circleIds = parseCommaSeparated(e.getCircleIds());
					for (int i = 0; i < circleIds.length; i++) {
						org.graphstream.graph.Edge ed =
								graph.addEdge(circleIds[i]+e.getTaskId(), circleIds[i], e.getTaskId());
						ed.getTargetNode().addAttribute("name", circles[i]);
						nodesAdded.add(ed.getNode1());
					}
				}

				n = graph.getNode(e.getTaskId());
				n.addAttribute("circle", e.getProjectId());
				if(e.getTaskName()!=null)
					n.addAttribute("ui.label", e.getTaskName());

				if(e.isRole()) {
					n.addAttribute("ui.class", "role");
				}

				if(StructuralDataChange.isCircle(e.getTaskName())) {
					n.addAttribute("ui.class", "circleClass");
					if(e.getIsSubtask())
						n.addAttribute("ui.class", "subcircle");
				}
			}
			switch(e.getTypeOfChange()) {
			//			case AsanaActions.ADD_SUB_ROLE:
			//			case AsanaActions.ADD_TO_CIRCLE:
			//			case AsanaActions.ASSIGN_TO_ACTOR:
			case AsanaActions.CREATE_ROLE:
				n.addAttribute("ui.class", "added");
				break;
				//			case AsanaActions.REVIVE_OR_MARK_INCOMPLETE:
				//				break;
			case AsanaActions.COMPLETE_ROLE:
			case AsanaActions.DETELE_OR_MARK_COMPLETE:
				//			case AsanaActions.REMOVE_FROM_CIRCLE:
				n.addAttribute("ui.class", "deleted");
				n.addAttribute("deleted", "true");
				nodesAdded.remove(n);
				allTaskIds.remove(n.getId());
				graph.removeNode(n.getId());
				
//				for(String tId : allTaskIds) {
//					if(graph.getNode(tId)!=null)
//						if(graph.getNode(tId).getDegree()==0)
//							graph.removeNode(tId);
//				}
				
//				removeOrphans(graph);
				
				break;
				//			case AsanaActions.CHANGE_NAME_OF_ROLE:
				////			case AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK:
				//			case AsanaActions.LAST_MODIFY_ROLE:
				//			case AsanaActions.COMMENT:
				//			case AsanaActions.UNASSIGN_FROM_ACTOR:
				//				n.setAttribute("ui.style", "size: 20px;");
				//				sleep(200);
				//				n.setAttribute("ui.style", "size: 15px;");
				//				sleep(200);
				//				n.addAttribute("ui.class", "modified");
				//				break;
			default:
				//				n.addAttribute("ui.class", "deleted");
				//				graph.removeNode(n.getId());
				//				n.setAttribute("ui.style", "size: 15px;");
				//				sleep(200);
				//				n.setAttribute("ui.style", "size: 10px;");
				//				sleep(200);
				n.addAttribute("ui.class", "modified");
				break;
			}
		}
		return nodesAdded;
	}

	private static String[] parseCommaSeparated(String circle) {
		return circle.split(",");
	}

	private static String[] toStrObjArray(Object[] row) {
		String[] res = new String[row.length];
		for(int i=0; i<row.length; i++)
			res[i] = row[i].toString();
		return res;
	}

	private static void addAllCircles(Session session, Graph graph, Node master) {
		Query queryProjects = session.createSQLQuery("SELECT DISTINCT projectId, projectName FROM `SpringestWithCircle` WHERE 1");
		List projects = queryProjects.list();

		for (Object object : projects) {
			Object[] row = (Object[]) object;
			String[] str = toStrObjArray(row);

			Edge e = graph.addEdge(master.getId()+str[0], master.getId(), str[0]);
			Node n = e.getTargetNode();
			n.addAttribute("ui.class", "circleClass");
			n.addAttribute("ui.label", str[1]);

			allTaskIds.add(n.getId());
		}
	}

	private static void sleep(int time) {
		try { Thread.sleep(time); } catch (Exception e) {}
	}
	
	private static void removeOrphans(Graph graph) {
		Collection<Node> nodes = graph.getNodeSet();
		for (Iterator<Node> iterator = nodes.iterator(); iterator.hasNext();) {
			Node node = (Node) iterator.next();
			if(node.getOutDegree() == 0)
				iterator.remove();
		}
	}
}
