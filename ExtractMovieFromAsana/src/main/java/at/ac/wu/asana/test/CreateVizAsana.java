package at.ac.wu.asana.test;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.Viewer;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import at.ac.wu.asana.db.utils.DatabaseConnector;
import at.ac.wu.asana.model.AsanaActions;
import at.ac.wu.asana.model.StructuralDataChange;

public class CreateVizAsana {

	public static void main(String[] args) {
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

		Graph graph = new MultiGraph("Asana graph");
		
		graph.addAttribute("ui.stylesheet", "url('file:///home/saimir/asana/ExtractMovieFromAsana/src/resources/styleSheetAsanaMinimal.css')");
		graph.setStrict(false);
		graph.setAutoCreate(true);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");

		Viewer viewer = graph.display();
		Node master = graph.addNode("864733919245"); // Springest center
		
		viewer.disableAutoLayout();

		master.addAttribute("ui.class", "master");
		master.addAttribute("ui.label", "Springest");
		
		viewer.enableAutoLayout();
		
		addNodesStepByStep(graph, master);
		
		SpriteManager sManager = new SpriteManager(graph);
		Sprite s1 = sManager.addSprite("S1");
		s1.setPosition(0, 0, 0);
		s1.attachToNode(master.getId());
		s1.setAttribute("ui.label", "End of movie");		
	}

	private static void addNodesStepByStep(Graph graph, Node master) {

		SessionFactory sf = DatabaseConnector.getSessionFactory("asana_manual2");
		org.hibernate.Session session = sf.openSession();
		
		addAllCircles(session, graph, master);
		
		Query queryAllDates = session.createSQLQuery("SELECT DISTINCT date FROM `TABLE 2` ORDER BY date");
		Query queryEvents = session.createSQLQuery("SELECT * FROM `TABLE 2` WHERE date =:date ORDER by timestamp ASC");

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
//			List<Node> nodesAdded = 
					addToGraph(graph, changeEvents);
					connectSameCircleNodes(graph);
			sleep(1000);
//			for (Node node : nodesAdded) {
//				node.addAttribute("ui.style", "fill-color: rgb(155,0,0);");
//			}
		}
		sf.close();
	}

	private static void connectSameCircleNodes(Graph graph) {
		Collection<Node> nodes = graph.getNodeSet();
		Map<Object, List<Node>> circleNodeMap = new java.util.HashMap<Object, List<Node>>();		
		for (Node n : nodes) {
			if(circleNodeMap.containsKey(n.getAttribute("circle"))) {
				circleNodeMap.get(n.getAttribute("circle")).add(n);
			}
			else {
				List<Node> l = new ArrayList<Node>();
				l.add(n);
				circleNodeMap.put(n.getAttribute("circle"), l);
			}
		}
		Set<Object> keys = circleNodeMap.keySet();
		for (Object k : keys) {
			List<Node> list = circleNodeMap.get(k);
			for(int i=0; i<list.size()-1; i++) {
				for(int j=i+1; j<list.size(); j++) {
					Node n1 = list.get(i);
					Node n2 = list.get(j);
					Edge e = graph.addEdge(n1.getId()+n2.getId(), n1.getId(), n2.getId());
					e.addAttribute("ui.class", "sameCircle");
				}
			}
		}
		
	}

	private static void addAllCircles(Session session, Graph graph, Node master) {
		// TODO Auto-generated method stub SELECT DISTINCT projectName FROM `Springest` WHERE 1 
		Query queryProjects = session.createSQLQuery("SELECT DISTINCT projectId, projectName FROM `Springest` WHERE 1");
		List projects = queryProjects.list();
		
		for (Object object : projects) {
			Object[] row = (Object[]) object;
			String[] str = toStrObjArray(row);
			
			Edge e = graph.addEdge(master.getId()+str[0], master.getId(), str[0]);
			Node n = e.getTargetNode();
			n.addAttribute("ui.class", "circleClass");
			n.addAttribute("ui.label", str[1]);
		}
		
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
			case AsanaActions.ADD_SUB_ROLE:
			case AsanaActions.ADD_TO_CIRCLE:
			case AsanaActions.ASSIGN_TO_ACTOR:
			case AsanaActions.CREATE_ROLE:
				n.addAttribute("ui.class", "added");
				break;
			case AsanaActions.REVIVE_OR_MARK_INCOMPLETE:
//				if(e.getIsSubtask()) {
//					org.graphstream.graph.Edge ed = 
//							graph.addEdge(e.getParentTaskId()+e.getTaskId(), e.getParentTaskId(), e.getTaskId());
//					nodesAdded.add(ed.getNode0());
//					nodesAdded.add(ed.getNode1());
//				}
//				else {
//					org.graphstream.graph.Edge ed =
//							graph.addEdge(e.getProjectId()+e.getTaskId(), e.getProjectId(), e.getTaskId());
//					nodesAdded.add(ed.getNode0());
//					nodesAdded.add(ed.getNode1());
//				}
//				n.addAttribute("ui.class", "role");
				break;
			case AsanaActions.COMPLETE_ROLE:
			case AsanaActions.DETELE_OR_MARK_COMPLETE:
			case AsanaActions.REMOVE_FROM_CIRCLE:
//				sleep(1000);
				Node n2 = graph.removeNode(n);
				n.addAttribute("ui.class", "unemphasize");
				sleep(100);
				break;
			default:
				break;
			}
			sleep(500);
		}
		return nodesAdded;
	}

	private static void sleep(int time) {
		try { Thread.sleep(time); } catch (Exception e) {}
	}
}
