package at.ac.wu.asana.viz;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.graphstream.algorithm.Toolkit;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.hibernate.SessionFactory;

import com.opencsv.CSVWriter;

import at.ac.wu.asana.db.io.ReadFromDB;
import at.ac.wu.asana.db.postprocess.datastructures.YMCircleList;
import at.ac.wu.asana.db.postprocess.datastructures.YMTaskList;
import at.ac.wu.asana.db.utils.DatabaseConnector;
import at.ac.wu.asana.model.AuthoritativeList;
import at.ac.wu.asana.model.StructuralDataChange;

public class GenerateFromTaskList {

	static Map<String, List<StructuralDataChange>> dailyChanges = new LinkedHashMap<String, List<StructuralDataChange>>();
	static Set<String> deadCircles = new HashSet<String>();
	static Set<String> subCircles = new HashSet<String>();
	static Graph g = null;
	static Node master = null;
	static Sprite s = null;
	static Map<String, String> circleDeathDate = new HashMap<String, String>();

	static List<String[]> graphMeasures = new ArrayList<String[]>();


	public static void main(String[] args) { 
		Instant startTime = Instant.now();
		System.out.println("Loading data ... ");
		initDailyChanges();
		System.out.println("Retrieved "+dailyChanges.size()+" days.");
		System.out.println("Generating data structures ... ");
		Map<String,List<YMCircleList>> mapTaskToYMandCircles = new HashMap<String, List<YMCircleList>>();
		fillInMap(mapTaskToYMandCircles,dailyChanges);
		System.out.println("Mapping of tasks to circles done.");

		Map<String,List<YMTaskList>> mapCircleToYMandTasks = new TreeMap<String, List<YMTaskList>>();
		fillInCircleMap(mapCircleToYMandTasks, mapTaskToYMandCircles);
		System.out.println("Mapping circles to tasks done.");

		System.out.println("Setting circle death dates.");
		circleDeathDate = setCircleDeathDates();

		System.out.println("Initializing graphstream ...");

		int delay = 0;	

		g = initGraph();
		master = initCenter();
		s = initSprite();
		g.display();

		System.out.println("Done.");

		System.out.println("Starting animation.");

		graphMeasures.add(new String[] {
				"averageDegree", 
				"max.degree",
				"min.degree",
				"degree-Avg-Deviation",
				"avg-ClusteringCoefficient",
				"minClustertingCoefficient",
				"maxClustertingCoefficient",
				"density(g)",
				"diameter(g)", 
				"isConnected(g)",
				"nrCommunities"});
		
		animate(mapCircleToYMandTasks, mapTaskToYMandCircles, delay);

		writeCSV("graphMeasures.csv");

		System.out.println("Finished in "+Duration.between(startTime, Instant.now()));		

	}

	private static void writeCSV(String csv) {

		PrintWriter rolesFileWriter;
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(csv), StandardCharsets.UTF_8) );

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			for (String[] row : graphMeasures) {
				csvWriter.writeNext(row);
			}
			csvWriter.flush();
			csvWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Map<String, String> setCircleDeathDates() {
		Map<String, String> circleDeathDate = new HashMap<String, String>();
		circleDeathDate.put(AuthoritativeList.authoritativeList[AuthoritativeList.lookup("☺Business Intelligence Roles")], "2015-03-24");
		circleDeathDate.put(AuthoritativeList.authoritativeList[AuthoritativeList.lookup("☺ Demand Roles")], "2014-09-30");
		circleDeathDate.put(AuthoritativeList.authoritativeList[AuthoritativeList.lookup("☺ Marketplace DE roles")], "2017-09-18");
		circleDeathDate.put(AuthoritativeList.authoritativeList[AuthoritativeList.lookup("☺ Go Customer Roles")], "2016-01-12");
		circleDeathDate.put(AuthoritativeList.authoritativeList[AuthoritativeList.lookup("☺ Go Sales Roles")], "2014-05-29");
		circleDeathDate.put(AuthoritativeList.authoritativeList[AuthoritativeList.lookup("☺ Infrastructure Roles")], "2018-06-28");
		circleDeathDate.put(AuthoritativeList.authoritativeList[AuthoritativeList.lookup("☺ Marketplace Roles")], "2018-06-05");
		circleDeathDate.put(AuthoritativeList.authoritativeList[AuthoritativeList.lookup("☺ Providers Roles")], "2016-01-11");
		circleDeathDate.put(AuthoritativeList.authoritativeList[AuthoritativeList.lookup("☺ Rainmakers Roles")], "2015-06-09");
		circleDeathDate.put(AuthoritativeList.authoritativeList[AuthoritativeList.lookup("☺ Sales Roles")], "2014-09-01");
		circleDeathDate.put(AuthoritativeList.authoritativeList[AuthoritativeList.lookup("☺ Office Roles")], "2018-02-22");
		circleDeathDate.put(AuthoritativeList.authoritativeList[AuthoritativeList.lookup("☺ People Roles")], "2018-02-22");

		return circleDeathDate;
	}

	private static Sprite initSprite() {

		SpriteManager sManager = new SpriteManager(g);
		s = sManager.addSprite("S1");
		s.setPosition(Units.PX, 100, 10, 0);
		s.setAttribute("ui.label", "");

		return s;
	}

	private static Node initCenter() {
		master = g.addNode("864733919245"); // Springest center

		//		viewer.disableAutoLayout();

		master.addAttribute("ui.class", "master");
		master.addAttribute("ui.label", "Springest");
		master.addAttribute("ui.layout", "frozen");
		master.addAttribute("xy", 0.0, 1.0);
		master.addAttribute("layout.force",0.6);
		master.addAttribute("layout.stabilization-limit",0.6);
		master.addAttribute("layout.weight",0.6);
		return master;
	}


	private static void animate(Map<String, List<YMTaskList>> mapCircleToYMandTasks,
			Map<String, List<YMCircleList>> mapTaskToYMandCircles, int delay) {

		Set<String> allDays = dailyChanges.keySet();

		int dayCnt = 1;
		for (String day : allDays) {
			List<StructuralDataChange> changes = dailyChanges.get(day);
			updateSprite(day, dayCnt++, allDays.size(), changes.size());

			Set<String> circleIdsDrawnToday = new HashSet<String>();
			Set<String> circleIdsDiedToday = new HashSet<String>();

			for (StructuralDataChange change : changes) {

				String taskId = change.getTaskId();
				List<String> circleIdSet = new ArrayList<String>(getSetOfCircleIds(taskId, changes));
				List<String> circleSet = new ArrayList<String>(getSetOfCircles(taskId, changes));

				if(circleIdSet.size()==1 && circleIdSet.get(0).equals("")) // this task is in no circles in this day
					break;

				for (int i=0; i<circleIdSet.size(); i++) {

					if(circleIdSet.get(i).equals(""))// NO CIRCLE
						continue;

					if(!circleIdsDrawnToday.contains(circleIdSet.get(i))) {
						drawCircle(circleIdSet.get(i), circleSet.get(i));
						circleIdsDrawnToday.add(circleIdSet.get(i));
					}
					if(circleDeathDate.containsKey(circleIdSet.get(i))) {
						if(circleDeathDate.get(circleIdSet.get(i)).equals(day)){
							if(!circleIdsDiedToday.contains(circleIdSet.get(i))) {
								System.out.println("Today "+day+" dies circle "+circleSet.get(i));
								deadCircles.add(circleIdSet.get(i));
								removeCircle(circleIdSet.get(i));
								circleIdsDiedToday.add(circleIdSet.get(i));
							}
						}
					}

					// draw tasks belonging to circles in this day
					//					List<String> setOfCircleIdsToday = getCirclesIdsToday(taskId, mapTaskToYMandCircles, day);
					//					List<String> setOfCirclesToday = getCirclesToday(taskId, mapTaskToYMandCircles, day);
					String[] circleIds = change.getCircleIds().split(",");
					for (String c : circleIds) {
						if(!deadCircles.contains(c))
							drawRole(taskId, change.getTaskName(), circleIds, change.getCircle().split(","));
					}
				}
			}
			sleep(delay);
			if(dayCnt%14==0) {
				//				g.addAttribute("ui.screenshot", "/home/saimir/ownCloud/PhD/Collaborations/Waldemar/Springest/Movies/frames/screenshot"+dayCnt/7+".png");
			}
			double[] clCoefs = Toolkit.clusteringCoefficients(g);
			Arrays.sort(clCoefs);
			String[] row = new String[] {
					Toolkit.averageDegree(g)+"",
					Toolkit.degreeMap(g).get(0)+"", //max degree
					Toolkit.degreeMap(g).get(Toolkit.degreeMap(g).size()-1)+"", // min degree
					Toolkit.degreeAverageDeviation(g)+"", 
					Toolkit.averageClusteringCoefficient(g)+"", 
					clCoefs[0]+"", // min clustering coef
					clCoefs[clCoefs.length-1]+"", // max clustering coef
					Toolkit.density(g)+"", 
					Toolkit.diameter(g)+"", 
					Toolkit.isConnected(g)+"",
					Toolkit.communities(g, null).size()+""
					};
			graphMeasures.add(row);
		}
	}

	private static List<String> getCirclesToday(String taskId, Map<String, List<YMCircleList>> mapTaskToYMandCircles,
			String day) {
		if(mapTaskToYMandCircles.containsKey(taskId)) {
			List<YMCircleList> ymCircleLists = mapTaskToYMandCircles.get(taskId);
			for (YMCircleList ymCircleList : ymCircleLists) {
				if(ymCircleList.ym.equals(day)) {
					return ymCircleList.circles;
				}
			}
		}
		return null;
	}


	private static List<String> getCirclesIdsToday(String taskId, Map<String, List<YMCircleList>> mapTaskToYMandCircles, String day) {
		if(mapTaskToYMandCircles.containsKey(taskId)) {
			List<YMCircleList> ymCircleLists = mapTaskToYMandCircles.get(taskId);
			for (YMCircleList ymCircleList : ymCircleLists) {
				if(ymCircleList.ym.equals(day)) {
					return ymCircleList.circleIds;
				}
			}
		}
		return null;
	}


	private static void drawRole(String taskId, String name, String[] circleIds, String[] circleNames) {
		if(circleIds.length==1 && circleIds[0].equals("")) //no circle
			return;

		String att = "";
		for (String c : circleIds) {
			att+=c;
		}
		for (int i=0; i<circleIds.length; i++) {
			String circleId = circleIds[i];
			if(circleNames[i].equals("NO CIRCLE"))
				continue;
			Edge e = g.addEdge(circleId+taskId, circleId, taskId);
			e.addAttribute("layout.force",0.3);
			e.addAttribute("layout.stabilization-limit",0.5);
			e.addAttribute("layout.weight",0.5);
			Node n = e.getTargetNode();
			n.addAttribute("circleIds", att);
			n.addAttribute("ui.class", "role");
			n.addAttribute("ui.label", taskId+":"+name);
			n.addAttribute("layout.force",0.3);
			n.addAttribute("layout.stabilization-limit",0.5);
			n.addAttribute("layout.weight",0.5);
		}
	}

	private static void removeCircle(String circleId) {
		//		String father = master.getId();
		//		if(isSubcircle(circleId))
		//			father = getFather(circleId);
		List<Node> nodes = new ArrayList<Node>(g.getNodeSet());

		for (int i = 0; i < nodes.size(); i++) {
			//			System.out.println("Attributes:"+nodes.get(i).getAttribute("circleIds"));
			String attr = nodes.get(i).getAttribute("circleIds");
			if(attr!=null && attr.contains(circleId)) {
				String newAttr = attr.replace(circleId, "");
				nodes.get(i).setAttribute("circleIds", newAttr);
				if(newAttr.equals(""))
					g.removeNode(nodes.get(i));
			}
		}
		g.removeNode(circleId);
	}

	private static void updateSprite(String day, int dayCnt, int allDays, int nChanges) {
		s.setAttribute("ui.label", day + ", day "+dayCnt+"/"+allDays
				+ ", evts: "+nChanges);
	}

	private static void drawCircle(String circleId, String circleName) {

		if(circleName.equals("NO CIRCLE"))
			System.out.println("Here");

		if(deadCircles.contains(circleId))
			return;
		String father = master.getId();
		if(isSubcircle(circleId))
			father=getFather(circleId);

		Edge ed = g.addEdge(father+circleId, father, circleId);
		Node n = ed.getTargetNode();
		n.addAttribute("ui.class", "circleClass");
		List<String> att = new ArrayList<String>();
		att.add(circleId);
		n.addAttribute("circleIds", circleId);
		n.addAttribute("ui.label", circleName);
	}

	private static Graph initGraph() {
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

		g = new SingleGraph("Asana graph");

		g.addAttribute("ui.stylesheet", 
				"url('file:///home/saimir/asana/ExtractMovieFromAsana/src/resources/styleSheetAsanaMinimal.css')");
		g.setStrict(false);
		g.setAutoCreate(true);
		g.addAttribute("ui.quality");
		g.addAttribute("ui.antialias");
		g.addAttribute("layout.force",0.2);
		g.addAttribute("layout.stabilization-limit",0.4);
		g.addAttribute("layout.weight",0.4);

		return g;
	}

	private static void fillInCircleMap(Map<String, List<YMTaskList>> mapCircleToYMandTasks,
			Map<String, List<YMCircleList>> mapTaskToYMandCircles) {
		//		Set<String> tasks = mapTaskToYMandCircles.keySet();
		//		Set<String> allCircles = getAllCircles(mapTaskToYMandCircles);
		//		Set<String> allYM = getAllYM(mapTaskToYMandCircles);

		Set<String> theTasks = mapTaskToYMandCircles.keySet();
		for (String t : theTasks) {
			List<YMCircleList> ymCircleList = mapTaskToYMandCircles.get(t);
			for (YMCircleList ymListCirc : ymCircleList) {
				List<String> circs = ymListCirc.circleIds;
				for (String c : circs) {

					if(!mapCircleToYMandTasks.containsKey(c)) {
						List<YMTaskList> list = new ArrayList<YMTaskList>();
						List<String> tids = new ArrayList<String>();
						tids.add(t);
						YMTaskList taskList = new YMTaskList(ymListCirc.ym, tids);
						list.add(taskList);
						Collections.sort(list);
						mapCircleToYMandTasks.put(c, list);
					}

					else { // circle is already there

						List<YMTaskList> ymTaskLists = mapCircleToYMandTasks.get(c);
						Collections.sort(ymTaskLists);
						int i = lookupYM(ymListCirc.ym, ymTaskLists);
						if(i!=-1) {
							YMTaskList ymTaskList = ymTaskLists.get(i);
							ymTaskList.taskIds.add(t);
						}
						else { // ym is not there
							List<String> tasks = new ArrayList<String>();
							tasks.add(t);
							YMTaskList ymTaskList = new YMTaskList(ymListCirc.ym,tasks);
							ymTaskLists.add(ymTaskList);
							Collections.sort(ymTaskLists);
						}
					}
				}
			}
		}
	}

	private static int lookupYM(String ym, List<YMTaskList> ymTaskLists) {
		boolean found = false;
		int pos = 0;
		for (int i = 0; !found && i < ymTaskLists.size(); i++) {
			if(ymTaskLists.get(i).ym.equals(ym)) {
				found=true;
				pos = i;
			}
		}
		if(!found)
			return -1;
		return pos;
	}

	public static <K, V> void printMap(Map<K, V> map) {
		for (Map.Entry<K, V> entry : map.entrySet()) {          
			System.out.println( entry.getKey() + " " + entry.getValue() );
		}
	}

	static void initDailyChanges() {
		dailyChanges = new LinkedHashMap<String, List<StructuralDataChange>>();

		String dbname = "asana_manual5";
		String allDaysQuery = "SELECT DISTINCT `date` " + 
				"			 FROM `SpringestWithCircle` " + 
				//				"			  WHERE `date` > '2013-09-30' " + 
				"			  ORDER BY `date`";

		List<String> allDays = ReadFromDB.readAll(dbname, allDaysQuery);

		//		System.out.println(allYM);

		String queryAllInDay = "SELECT * FROM `SpringestWithCircle` "
				+ "WHERE `date` =:day "
				//				+ "AND typeOfChange IN (12,4,5,14)"
				+ "";
		//		date =:date

		// read the data
		SessionFactory sf = DatabaseConnector.getSessionFactory(dbname);
		org.hibernate.Session session = sf.openSession();
		for (String day : allDays) {
			List<StructuralDataChange> changes = ReadFromDB.readDailyChanges(session, dbname, queryAllInDay, day);
			dailyChanges.put(day, changes);
		}
		session.flush();
		session.close();
		sf.close();

	}

	private static void fillInMap(Map<String, List<YMCircleList>> mapTaskToYMandCircles,
			Map<String, List<StructuralDataChange>> ymChanges) {
		Set<String> yms = ymChanges.keySet();
		for (String ym : yms) {
			Set<String> taskIds = getTasksInChanges(ymChanges.get(ym));
			for (String taskId : taskIds) {
				Set<String> circleIdSet = getSetOfCircleIds(taskId, ymChanges.get(ym));
				Set<String> circleSet = getSetOfCircles(taskId, ymChanges.get(ym));
				YMCircleList ymCircleList = new YMCircleList(ym, new ArrayList<String>(circleIdSet),
						new ArrayList<String>(circleSet), AuthoritativeList.authoritativeList, AuthoritativeList.authoritativeListNames);
				if(!mapTaskToYMandCircles.containsKey(taskId)) {
					mapTaskToYMandCircles.put(taskId, new ArrayList<YMCircleList>());
				}
				mapTaskToYMandCircles.get(taskId).add(ymCircleList);
			}
		}		
	}

	private static Set<String> getTasksInChanges(List<StructuralDataChange> list) {
		Set<String> tasksUnique = new HashSet<String>();
		for (StructuralDataChange structuralDataChange : list) {
			tasksUnique.add(structuralDataChange.getTaskId());
		}
		return tasksUnique;
	}

	private static Set<String> getSetOfCircleIds(String taskId, List<StructuralDataChange> list) {
		Set<String> circles = new LinkedHashSet<String>();
		for (StructuralDataChange change : list) {
			if(change.getTaskId().equals(taskId)) {
				String[] circleIds = change.getCircleIds().split(",");
				for (String cid : circleIds) {
					circles.add(cid);
				}
			}
		}
		return circles;
	}

	private static Set<String> getSetOfCircles(String taskId, List<StructuralDataChange> list) {
		Set<String> circles = new LinkedHashSet<String>();
		for (StructuralDataChange change : list) {
			if(change.getTaskId().equals(taskId)) {
				String[] circleIds = change.getCircle().split(",");
				for (String cid : circleIds) {
					circles.add(cid);
				}
			}
		}
		return circles;
	}

	private static String getFather(String circleId) {
		if(circleId.equals("1133031362168396") || circleId.equals("560994092069672")
				|| circleId.equals("561311958443380"))
			return "11555199602299";
		if(circleId.equals("824769296181501"))
			return "11347525454570";
		if(circleId.equals("236886514207498"))
			return "404651189519209";
		return null;
	}

	private static boolean isSubcircle(String circleId) {
		if(circleId.equals("1133031362168396") || circleId.equals("824769296181501") || 
				circleId.equals("560994092069672") || circleId.equals("561311958443380") ||
				circleId.equals("236886514207498"))
			return true;
		return false;
	}

	private static void sleep(int time) {
		try { Thread.sleep(time); } catch (Exception e) {}
	}
}
