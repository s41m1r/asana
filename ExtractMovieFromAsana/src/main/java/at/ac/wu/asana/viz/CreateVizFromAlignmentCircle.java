package at.ac.wu.asana.viz;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.Viewer;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import at.ac.wu.asana.db.utils.DatabaseConnector;
import at.ac.wu.asana.model.Circle;
import at.ac.wu.asana.model.StructuralDataChange;

public class CreateVizFromAlignmentCircle {

	static Set<Circle> allYingYangCircles = new HashSet<Circle>();
	static Set<Circle> allSmileyCircles = new HashSet<Circle>();
	static Map<String, List<StructuralDataChange>> allEventsByDate = new HashMap<String, List<StructuralDataChange>>();
	static Map<Circle,Circle> mapSmileyToYinYang = new HashMap<Circle, Circle>();
	static Set<String> seenProjectIds = new HashSet<String>();
	static final int SLEEP = 50;

	static Logger logger = Logger.getLogger(CreateVizFromAlignmentCircle.class.getName());

	public static void main(String[] args) {
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

		Graph graph = new SingleGraph("Asana graph");

		graph.addAttribute("ui.stylesheet", 
				"url('file:///home/saimir/asana/ExtractMovieFromAsana/src/resources/styleSheetAsanaMinimal.css')");
		graph.setStrict(false);
		graph.setAutoCreate(true);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");

		// reading the data
		SessionFactory sf = DatabaseConnector.getSessionFactory("asana_manual2");
		org.hibernate.Session session = sf.openSession();

		String queryStringYinYang = "SELECT * FROM yinAndYang";
		logger.info("Reading circles with ☯ (Yin and Yang)");
		allYingYangCircles = readCircleFromDB(queryStringYinYang, session);
		logger.info("Read "+ allYingYangCircles.size()+" records");

		String queryStringSmiley = "SELECT * FROM `smileyCircles`";
		logger.info("Reading circles with ☺ (Smiley)");
		allSmileyCircles = readCircleFromDB(queryStringSmiley, session);
		logger.info("Read "+ allSmileyCircles.size()+" records");

		allEventsByDate = readAllEventsByDate(session);

		sf.close();

		//		Set<String> keys = allEventsByDate.keySet();
		//		for (String day : keys) {
		//			System.out.println(day + ": "+allEventsByDate.get(day).size()+" events.");
		//		}
		//
		//		System.out.println(keys.size() + " dates in total.");

		mapSmileyToYinYang = matchCircles(allSmileyCircles,allYingYangCircles);

		//		Set<Circle> circles = mapCircleCircle.keySet();
		//		for (Circle c : circles) {
		//			System.out.println(c.getName() + ": "+mapCircleCircle.get(c).getName());
		//		}

		Viewer viewer = graph.display();

		Node master = graph.addNode("☯ Alignment"); // Springest center
		//
		////		allTaskIds.add(master.getId());
		//
		//		//		viewer.disableAutoLayout();
		//
		master.addAttribute("ui.class", "circleClass");
		master.addAttribute("ui.label", "☯ Alignment");
				
		////		aliveCircles.add("☯ Alignment");
		
		//
		addCirclesToGraph(mapSmileyToYinYang, master, graph);
		logger.info("Added all circles found.");

		SpriteManager sManager = new SpriteManager(graph);
		Sprite s1 = sManager.addSprite("S1");
		s1.setPosition(Units.PX, 100, 10, 0);
		s1.setAttribute("ui.label", "");

		//		viewer.enableAutoLayout();
		logger.info("Start animation");
		Instant startAnimation = Instant.now();
		animate(graph, master, s1);
		//		viewer.close();
		logger.info("Animation finished in "+Duration.between(startAnimation, Instant.now()));
	}

	private static void animate(Graph graph, Node master, Sprite s1) {

		Set<String> dates = allEventsByDate.keySet();
		int i = 0;
		List<Node> restore = new ArrayList<Node>();

		for (String date : dates) {

			for (Node node : restore) {
				node.setAttribute("ui.class", node.getAttribute("oldClass"));
			}
			restore = new ArrayList<Node>();

			List<StructuralDataChange> eventsInDate = allEventsByDate.get(date);
			s1.setAttribute("ui.label", date + " # "+(1+i)+"/"+dates.size()
			+ ", evts: "+eventsInDate.size());

			for (StructuralDataChange sdc : eventsInDate) {
				String parentName = sdc.getParentTaskName();
				
				String taskname = sdc.getTaskName();
				boolean parentIsYinYang = StructuralDataChange.isYinAndYang(parentName);
				//				boolean parentIsSmiley = StructuralDataChange.isSmiley(parentName);
				boolean projectIsSmiley = StructuralDataChange.isSmiley(sdc.getProjectName());
				boolean thisTaskIsYingYang = StructuralDataChange.isYinAndYang(taskname);
				Edge ed = null;
				int typeOfChange = typeOfChange(sdc);

				//				System.out.println(taskname + 
				//						","+parentName+
				//						","+sdc.getProjectName()+
				//						","+sdc.getRawDataText()+
				//						","+typeOfChange+
				//						","+sdc.getTypeOfChangeDescription());

				if(sdc.getIsSubtask()){					
					if(parentIsYinYang) { //this is a subtask of a circle
						if(inMappingYinYang(parentName) || graph.getNode(parentName)!=null) { // the circle is already drawn
							//connect to parent who is already drawn
							
							if(StructuralDataChange.isYinAndYang(taskname)) { // this subtask is a sub-circle
								// draw or remove?
								drawChange(graph, restore, sdc.getTaskName(), parentName, "circleClass",null,
										typeOfChange);
								seenProjectIds.add(sdc.getTaskId());
							}	
							else {// this a role of the circle (parent is circle but this node is not one)
								drawChange(graph, restore, sdc.getTaskId(), parentName, "role", sdc.getTaskName(), typeOfChange);
							}
						}
					}	
				}
				else { // this task is not a subtask
					//this task is a sub circle i.e. it's project is a smiley project
					if(thisTaskIsYingYang) {
						if(projectIsSmiley && inMappingSmiley(sdc.getProjectName())) {
							parentName = getYinYangFromSmileyName(sdc.getProjectName());
							if(inMappingYinYang(parentName))
								//								ed = drawEdge(graph, sdc.getTaskName(), parentName, "circleClass");
								drawChange(graph, restore, sdc.getTaskName(), parentName, "circleClass", null, typeOfChange);
						}
					}
					else {//this task is a role of a circle (draw if the circle is known)
						if(projectIsSmiley && inMappingSmiley(sdc.getProjectName())){
							parentName = getYinYangFromSmileyName(sdc.getProjectName());
							if(inMappingYinYang(parentName))
								//								ed = drawEdge(graph, sdc.getTaskId(), parentName, "role", sdc.getTaskName());
								drawChange(graph, restore, sdc.getTaskId(), parentName, "role", sdc.getTaskName(), typeOfChange);
						}
					}					
				}
			}
			i++;
			sleep(SLEEP);
		}
	}

	private static void drawChange(Graph graph, List<Node> restore, String child, String parentName, String uiClass, String taskName,
			int typeOfChange) {
		Edge ed;
		switch (typeOfChange) {
		case 1: // draw
			ed = drawEdge(graph, child, parentName, uiClass, taskName);
			break;
		case 2: // blink
			Node n = graph.getNode(child);
			if(n!=null) {
				n.addAttribute("ui.class", "modified");
				n.addAttribute("oldClass", uiClass);
				restore.add(n);
			}		
			break;

		case 3: // remove	
			System.err.println("Deleting "+child);
			Node n1 = graph.getNode(child);
			if(n1!=null) {
				n1.addAttribute("ui.class", "deleted");
				n1.addAttribute("oldClass", uiClass);
				graph.removeNode(n1);
				removeOrphans(graph);
			}	
			break;
		default:
			break;
		}
	}


	private static void removeOrphans(Graph graph) {
		Collection<Node> nodes = graph.getNodeSet();
		for (Iterator<Node> iterator = nodes.iterator(); iterator.hasNext();) {
			Node node = (Node) iterator.next();
			if(node.getOutDegree() == 0)
				iterator.remove();
		}
	}

	private static int typeOfChange(StructuralDataChange sdc) {
		int res = 0; //1- draw; 2- blink; 3-remove
		switch (sdc.getTypeOfChange()) {
		case 12: 
			res=1;
			break;
		case 6:
		case 2:
		case 1:
			res=2;
			break;
		case 14:
			res=3;

		default:
			break;
		}
		return res;
	}

	private static void sleep(int i) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}

	private static Edge drawEdge(Graph graph, String child, String parentName, String uiClass, String taskName) {
		Edge ed;

		if(parentName.equals(child)) 
			return null; //avoid self loops

		ed = graph.addEdge(parentName+child, parentName, child);
		ed.getTargetNode().addAttribute("ui.class", uiClass);
		if(taskName==null)
			ed.getTargetNode().addAttribute("ui.label", child);
		else
			ed.getTargetNode().addAttribute("ui.label", taskName);
		return ed;
	}

	private static String getYinYangFromSmileyName(String projectName) {
		Collection<Circle> keys = mapSmileyToYinYang.keySet();
		Circle c = null;
		for (Circle circle : keys) {
			if(circle.getName().equals(projectName)) {
				c = circle;
				break;
			}
		}
		if(c!=null) {
			Circle yy = mapSmileyToYinYang.get(c);
			return yy.getName();
		}
		return null;
	}

	private static Edge drawEdge(Graph graph, String child, String parentName, String uiClass) {
		Edge ed;
		ed = graph.addEdge(parentName+child, parentName, child);
		ed.getTargetNode().addAttribute("ui.class", uiClass);
		ed.getTargetNode().addAttribute("ui.label", child);
		return ed;
	}

	private static boolean inMappingSmiley(String parentName) {
		Set<Circle> circles = mapSmileyToYinYang.keySet();
		for (Circle c : circles) {
			if(c.getName().equals(parentName))
				return true;
		}
		return false;
	}

	private static boolean inMappingYinYang(String parentName) {
		Collection<Circle> collection =  mapSmileyToYinYang.values();
		for (Circle c : collection) {
			if(c.getName().equals(parentName))
				return true;
		}
		return false;
	}

	private static void addCirclesToGraph(Map<Circle, Circle> mapCircleCircle, Node master, Graph graph) {
		Set<Circle> circles = mapCircleCircle.keySet();
		for (Circle c : circles) {
			if(c.getName().contains("Alignment"))
				continue;
			Circle yinYang = mapCircleCircle.get(c);
			Edge ed = graph.addEdge(master.getId()+yinYang.getName(), master.getId(), yinYang.getName());
			ed.getTargetNode().addAttribute("ui.class", "circleClass");
			ed.getTargetNode().addAttribute("ui.label", yinYang.getName());
		}
	}

	private static Map<String, List<StructuralDataChange>> readAllEventsByDate(Session session) {

		Map<String, List<StructuralDataChange>> allEvents = new TreeMap<String, List<StructuralDataChange>>();
		Query queryAllDates = session.createSQLQuery(""
				+ "SELECT DISTINCT date FROM `Springest` "
				+ "ORDER BY date ASC");
		Query queryEvents = session.createSQLQuery(""
				+ "SELECT * FROM `Springest` "
				+ "WHERE date =:date "
				+ "ORDER by date ASC");

		List<String> result = (List<String>) queryAllDates.list();		
		logger.info("Retrieving "+result.size()+ " days of history.");
		long start = System.currentTimeMillis();
		
		for (String d : result) {
			queryEvents.setDate("date", Date.valueOf(d));
			List<Object> events = queryEvents.list();
			List<StructuralDataChange> changeEvents = new ArrayList<StructuralDataChange>();
			for (Object e : events) {
				Object[] row = (Object[]) e;
				String[] str = toStrObjArray(row);
				StructuralDataChange sdc = StructuralDataChange.fromString(str);
				//				if(sdc.getMessageType().equals("system"))
				changeEvents.add(sdc);
			}
			if(changeEvents.size()>0) //avoid recording dates with 0 events
				allEvents.put(d, changeEvents);
		}
		Duration d = Duration.ofMillis(System.currentTimeMillis()-start);
		logger.info("Finished reading in " + d);
		return allEvents;
	}

	private static Map<Circle, Circle> matchCircles(Set<Circle> allSmileyCircles2, Set<Circle> allYingYangCircles2) {
		Map<Circle, Circle> res = new HashMap<Circle, Circle>();
		for (Circle smileyCircle : allSmileyCircles2) {
			String name = smileyCircle.getName();
			for (Circle yingYangCircle : allYingYangCircles2) {
				if(matches(name, yingYangCircle.getName())) {
					res.put(smileyCircle, yingYangCircle);
				}				
				if(name.equals("☺ Smooth Ops Roles") && 
						yingYangCircle.getName().contains("Smooth Operations"))
					res.put(smileyCircle, yingYangCircle);
			}
			if(name.equals("☺ Alignment Roles"))
				res.put(new Circle("7963718816247","☺ Alignment Roles"),
						new Circle("7777777777777","☯ Alignment"));
		}
		return res;
	}

	private static boolean matches(String smiley, String yinYang) {
		String s1 = smiley.substring(1, smiley.length());
		String s2 = yinYang.substring(1, yinYang.length());

		s1 = s1.replace("Roles", "").replace("roles", "");
		s1 = s1.trim();

		s2 = s2.trim();

		if(s1.equals(s2))
			return true;

		return false;
	}

	private static Set<Circle> readCircleFromDB(String queryString, Session session) {
		Set<Circle> res = new HashSet<Circle>();
		Query queryProjects = session.createSQLQuery(queryString);
		List<Object> events = (List<Object>) queryProjects.list();

		for (Object e : events) {
			Object[] row = (Object[]) e;
			String[] str = toStrObjArray(row);
			System.out.println();
			Circle c = new Circle(str[1], str[0]);

			res.add(c);
		}
		return res;
	}

	private static String[] toStrObjArray(Object[] row) {
		String[] res = new String[row.length];
		for(int i=0; i<row.length; i++)
			res[i] = row[i].toString();
		return res;
	}

}
