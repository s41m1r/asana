package at.ac.wu.asana.db.postprocess;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.api.client.util.DateTime;
import com.google.common.collect.Sets;
import com.opencsv.CSVWriter;

import at.ac.wu.asana.csv.WriteUtils;
import at.ac.wu.asana.db.io.ReadFromDB;
import at.ac.wu.asana.model.AsanaActions;
import at.ac.wu.asana.model.StructuralDataChange;
import at.ac.wu.asana.util.GeneralUtils;

public class PostProcessFromDB {
	static String[] authoritativeList = new String[]{
			"7746376637805",
			"7749914219827",
			"7963718816247",
			"11347525454570",
			"11348115733592",
			"11348115733601",
			"11350833325340",
			"11555199602299",
			"11626921109046",
			"12530878841888",
			"13169100426325",
			"29007443412107",
			"47872397062455",
			"61971534223290",
			"79667185218012",
			"163654573139013",
			"236886514207498",
			"388515769387194",
			"389549960603898",
			"404651189519209",
			"560994092069672",
			"561311958443380",
			"824769296181501",
	"1133031362168396"};

	static String[] authoritativeListNames = new String[] {
			"☺ Sales Roles",
			"☺ Infrastructure Roles",
			"☺ Alignment Roles",
			"☺ Organisations Roles",
			"☺ Marketplace Roles",
			"☺ Demand Roles",
			"☺ Providers Roles",
			"☺ Smooth Operations Roles",
			"☺Business Intelligence Roles",
			"☺ Go Sales Roles",
			"☺ Rainmakers Roles",
			"☺ Go Customer Roles",
			"☺ Finance Roles",
			"☺ Product Roles",
			"☺ Marketing Roles",
			"☺ Evangelism Roles",
			"☺ Marketplace DE roles",
			"☺ Users Roles",
			"☺ Providers roles",
			"☺ Germany Roles",
			"☺ People Roles",
			"☺ Office Roles",
			"☺ Customer Success Roles",
	"☺ Springest Academy Roles"};

	static Map<String, String> subCirclesOf = new HashMap<String, String>();
	static Map<String, String> mapTaskCurrentCircle = new HashMap<String, String>();
	static Map<String, TreeSet<TimestampCircle>> circlesOfTasks = new HashMap<String, TreeSet<TimestampCircle>>();
	static Map<String, ArrayList<StructuralDataChange>> mapTaskStories = new LinkedHashMap<String, ArrayList<StructuralDataChange>>();

	public static void main(String[] args) throws IOException {

		long start = System.currentTimeMillis();
		subCirclesOf.put("404651189519209", "236886514207498");
		subCirclesOf.put("11555199602299", "560994092069672");
		subCirclesOf.put("11555199602299", "561311958443380");
		subCirclesOf.put("11347525454570", "824769296181501");
		subCirclesOf.put("11555199602299", "1133031362168396");

		// transfer account/purpose to father
		Map<String, List<StructuralDataChange>> allParents = getParents();
		Map<String, List<StructuralDataChange>> allChildren = getChildren();

		fixChildAsRoleProblem(allParents, allChildren);

		markChangeToAccountabilityPurpose(allChildren);
		cleanup(allChildren);
		cleanup(allParents);

		transferToFather(allParents, allChildren);
		createSubrolesEvtsOnFather(allParents,allChildren);

		Map<String, List<StructuralDataChange>> allEvents = new LinkedHashMap<String, List<StructuralDataChange>>();
		allEvents.putAll(allParents);
		allEvents.putAll(allChildren);

		setDesignRole(allEvents);
		setCurrentAssignee(allEvents);
		
		System.out.println("Events after setCurrentAssignee " +countEvents(allEvents));
		
		Map<String, List<StructuralDataChange>> allEvents2 = setCurrentCircles(allEvents);
		
		System.out.println("Events after setCurrentCircles " +countEvents(allEvents2));
		
		fixCompletedAndRemoveLastModify(allEvents2);

		//		checkIfNullTimestamp(allEvents);
		Map<String, List<StructuralDataChange>> allEventsNoDup = removeDuplicateTasks(allEvents2);
		
		List<StructuralDataChange> uniqueEvents = removeDups(allEvents2);	
		System.out.println("There are unique events = "+uniqueEvents.size());
		
		System.out.println("Events after removeDuplicateTasks = " +countEvents(allEventsNoDup));

//		printHistoryOfTask("11555199602323", allEventsNoDup); 

		String outfile = "Springest-filtered.csv";
		WriteUtils.writeListOfChangesWithCircleToCSV(uniqueEvents, outfile);
//		WriteUtils.writeMapOfChangesWithCircleToCSV2(allEventsNoDup, outfile);

		//		checkIfNoDesigned(allEvents);

		System.out.println("Done in "+(System.currentTimeMillis()-start)/1000+" sec.");
		System.out.println("Wrote on file "+outfile);
	}

	private static List<StructuralDataChange> removeDups(Map<String, List<StructuralDataChange>> allEvents2) {
		Set<StructuralDataChange> uniqueEvents = new TreeSet<StructuralDataChange>();
		Set<String> taskIds = allEvents2.keySet();
		for (String tId : taskIds) {
			uniqueEvents.addAll(allEvents2.get(tId));
		}
		return new ArrayList<StructuralDataChange>(uniqueEvents);
	}

	public static String countEvents(Map<String, List<StructuralDataChange>> allEvents) {
		// TODO Auto-generated method stub
		int tot = 0;
		for (String key : allEvents.keySet()) {
			for(StructuralDataChange sdc : allEvents.get(key))
				tot++;
		}
		return tot+"";
	}

	private static Map<String, List<StructuralDataChange>> removeDuplicateTasks(Map<String, List<StructuralDataChange>> allEvents2) {
		Map<Long, StructuralDataChange> datetimeTask = new HashMap<Long, StructuralDataChange>();
		Map<String, List<StructuralDataChange>> res = new TreeMap<String, List<StructuralDataChange>>();
		int c = 0; int tot = 0;
		for (String taskId : allEvents2.keySet()) {
			List<StructuralDataChange> changes = allEvents2.get(taskId);
			for (StructuralDataChange sdc : changes) {
				tot++;
				if(!datetimeTask.containsKey(new Long(sdc.getStoryCreatedAt().getValue())))
					datetimeTask.put(new Long(sdc.getStoryCreatedAt().getValue()), sdc);
				else {
//					System.out.println("Found 2 events at the same time!!!");
					c++;
					StructuralDataChange existing = datetimeTask.get(sdc.getStoryCreatedAt().getValue());
//					System.out.println("Here they are.");
//					System.out.println(Arrays.asList(existing.csvRowCircle()));
//					System.out.println(Arrays.asList(sdc.csvRowCircle()));
					if(existing.getCircleIds()==null || existing.getCircleIds().isEmpty() || 
							existing.getCircle().equals("NO CIRCLE")) {
						//							existing = sdc;
						datetimeTask.remove(new Long(sdc.getStoryCreatedAt().getValue()));
						datetimeTask.put(new Long(sdc.getStoryCreatedAt().getValue()), sdc);
//						System.out.println("Keeping "+Arrays.asList(sdc.csvRowCircle()));
					}
				}
			}
		}
		
		int tot2=0;

		for (String taskId : allEvents2.keySet()) {
			List<StructuralDataChange> changes = allEvents2.get(taskId);
			List<StructuralDataChange> newChanges = new ArrayList<StructuralDataChange>();
			for (StructuralDataChange sdc : changes) {
				StructuralDataChange change = datetimeTask.get(sdc.getStoryCreatedAt().getValue());
				newChanges.add(change);
				tot2++;
			}
			res.put(taskId, newChanges);
		}
		System.out.println("Events at same time="+c);
		System.out.println("Total events="+tot);
		System.out.println("Total events after removing dups ="+tot2);
		return res;
	}

	private static void fixChildAsRoleProblem(Map<String, List<StructuralDataChange>> allParents,
			Map<String, List<StructuralDataChange>> allChildren) {
		List<String> l = new ArrayList<String>();

		/*	7744437203132
			7747031589070
			62330621684448
			62330621684446
			62330621684442
			11449993727825
			7747031589074
			7747031589068
			13881300096790
			12042956870836
			688592025469493
			688592025469492
			688592025469491
		 * */
		l.add("7744437203132");
		l.add("7747031589070");
		l.add("62330621684448");
		l.add("62330621684446");
		l.add("62330621684442");
		l.add("11449993727825");
		l.add("7747031589074");
		l.add("7747031589068");
		l.add("13881300096790");
		l.add("12042956870836");
		l.add("688592025469493");
		l.add("688592025469492");
		l.add("688592025469491");

		for (String id : l) {
			allParents.put(id, allChildren.get(id));
			allChildren.remove(id);
		}

	}

	private static void checkIfNullTimestamp(Map<String, List<StructuralDataChange>> taskChanges) {
		for (String taskId : taskChanges.keySet()) {
			List<StructuralDataChange> changes = taskChanges.get(taskId);
			for (StructuralDataChange change : changes) {
				if(change.getStoryCreatedAt()==null) {
					System.err.println("FOUND!");
					System.exit(-1);
				}
			}
		}
	}

	private static void removeDuplicates(Map<String, List<StructuralDataChange>> allEvents) {
		// TODO Auto-generated method stub

	}

	private static void fixCompletedAndRemoveLastModify(Map<String, List<StructuralDataChange>> allEvents) {
		//		Map<String, List<StructuralDataChange>> map = new HashMap<String, List<StructuralDataChange>>(allEvents);
		//		Set<String> allKeys = new HashSet<String>(map.keySet());
		//		for(String k : allKeys) {
		//			for(StructuralDataChange sdc : map.get(k)) {
		//				if(sdc.getTypeOfChange()==14 && sdc.getTaskCompletedAt()==null) {
		//					allEvents.get(sdc.getTaskId()).remove(sdc);
		//				}
		//			}
		//		}
		//		
		Map<String, List<StructuralDataChange>> res = new HashMap<String, List<StructuralDataChange>>(allEvents);
		for(String k : allEvents.keySet()) {
			List<StructuralDataChange> evts = new ArrayList<StructuralDataChange>();
			for(StructuralDataChange sdc : allEvents.get(k)) {
				if((sdc.getTypeOfChange()==14 && sdc.getTaskCompletedAt()==null) || 
						sdc.getTypeOfChange()==13) {
					continue;
				}
				else if(sdc.getTypeOfChange()==14) {
					sdc.setStoryCreatedAt(sdc.getTaskCompletedAt());
				}
				evts.add(sdc);
			}
			res.put(k, evts);
		}
		allEvents.clear();
		allEvents.putAll(res);
	}

	private static void checkIfNoDesigned(Map<String, List<StructuralDataChange>> allEvents) {
		// TODO Auto-generated method stub
		Set<String> taskIds = allEvents.keySet();
		System.out.println("Going to check "+taskIds.size()+" tasks.");
		for (String string : taskIds) {
			List<StructuralDataChange> changes = new ArrayList<StructuralDataChange>();
			if(changes.size()==0)
				continue;
			StructuralDataChange firstTask = changes.get(0);
			if(firstTask.getTypeOfChange()!=12) {
				System.out.println("Task "+firstTask.getTaskId()+" "+firstTask.getTaskName()+" does not have a 12 code.");
				System.out.println(changes);
			}
		}

	}

	private static void setDesignRole(Map<String, List<StructuralDataChange>> allEvents) {
		for(String k : allEvents.keySet()) {
			for(StructuralDataChange sdc : allEvents.get(k)) {
				if(sdc.getTypeOfChange()==15) {//CREATE_ROLE -> DESIGN_ROLE
					sdc.setTypeOfChange(AsanaActions.DESIGN_ROLE);
					sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.DESIGN_ROLE));
				}
			}
		}
	}

	private static Map<String, List<StructuralDataChange>> setCurrentCircles(Map<String, List<StructuralDataChange>> allEvents) {
		Map<String, List<StructuralDataChange>> res = new TreeMap<String, List<StructuralDataChange>>();
		Set<String> taskIds = allEvents.keySet();
		for (String taskId : taskIds) {
			
			boolean firstTimeAddedToCircle = true;
			List<String> circles = new ArrayList<String>();
			List<StructuralDataChange> taskHistory = new LinkedList<StructuralDataChange>(allEvents.get(taskId));
			java.util.Collections.sort(taskHistory, new Comparator<StructuralDataChange>() {
				public int compare(StructuralDataChange o1, StructuralDataChange o2) {
					return o1.getStoryCreatedAt().toStringRfc3339().compareTo(o2.getStoryCreatedAt().toStringRfc3339());
				}
			} );
			SortedSet<StructuralDataChange> sortedSDC = new TreeSet<StructuralDataChange>();

			for (StructuralDataChange sdc : taskHistory) {

				StructuralDataChange sdc2 = sdc;

				if(sdc2.getTypeOfChange()==AsanaActions.ADD_TO_CIRCLE) {
					String curCircle = sdc2.getRawDataText().replaceAll("added to ", "").trim();
					int i = lookup(curCircle); // if -1 then it is not a circle
					if(i!=-1) {
						if(firstTimeAddedToCircle) {
							setChange(sdc2, AsanaActions.CREATE_ROLE);
							circles.add(curCircle);
						}
						else {
							List<String> newCirc = new ArrayList<String>();
							newCirc.add(curCircle);
							circles = GeneralUtils.union(circles, newCirc);
						}
						firstTimeAddedToCircle = false;
					}
				}

				if(sdc2.getTypeOfChange()==AsanaActions.REMOVE_FROM_CIRCLE) {
					String curCircle = sdc2.getRawDataText().replaceAll("removed from ", "").trim();
					int i = lookup(curCircle); // if -1 then it is not a circle
					if(i!=-1) { 
						if(circles.contains(curCircle))
							circles.remove(curCircle);
						else {
							System.err.println("This task "+sdc2.getTaskId()+" "+sdc2.getTaskName()
							+" was never added to "+curCircle);
						}
					}
					else {
						setChange(sdc2, AsanaActions.IGNORE_OR_DELETE);
					}
				}
				sdc2.setCircle(commaSeparate(circles));
				sdc2.setCircleIds(commaSeparateIds(circles));
				sortedSDC.add(sdc2);
			}
			res.put(taskId, new LinkedList<StructuralDataChange>(sortedSDC));
		}
		return res;
	}

	private static void setChange(StructuralDataChange sdc, int code) {
		sdc.setTypeOfChange(code);
		sdc.setTypeOfChangeDescription(AsanaActions.codeToString(code));
	}
	
	public static void printHistoryOfTask(String taskId, Map<String, List<StructuralDataChange>> allEvents) {
		System.out.println("History of task "+taskId+" Size="+allEvents.get(taskId).size());
		for (StructuralDataChange sdc : allEvents.get(taskId)) {
			System.out.println(sdc.getStoryCreatedAt() +";"+sdc.getTaskId()+
					";"+sdc.getRawDataText()+";"+sdc.getCircle()+";"+sdc.getCircle());
		}
	}

	private static void setCurrentAssignee(Map<String, List<StructuralDataChange>> allEvents) {
		Set<String> keys = allEvents.keySet();
		for (String key : keys) {
			String curAssignee = "";
			List<StructuralDataChange> changes = allEvents.get(key);
			for (StructuralDataChange sdc : changes) { 
				if(sdc.getCurrentAssignee()!=null && !sdc.getCurrentAssignee().isEmpty())
					curAssignee=""+sdc.getCurrentAssignee();

				sdc.setCurrentAssignee(""+curAssignee);
			}
		}
	}

	private static void createSubrolesEvtsOnFather(Map<String, List<StructuralDataChange>> allParents, Map<String, List<StructuralDataChange>> allChildren) {
		Set<String> children = allChildren.keySet();
		for (String k : children) {
			List<StructuralDataChange> childStories = allChildren.get(k);
			for (StructuralDataChange sdc : childStories) {
				if(sdc.getTypeOfChange()==AsanaActions.ADD_SUB_ROLE) {
					List<StructuralDataChange> parentsEvents = null;
					parentsEvents = lookUpParent(allParents, allChildren, sdc.getParentTaskId());
					if(parentsEvents!=null)
						addToFather(sdc, parentsEvents, AsanaActions.CHANGE_SUB_ROLE);
				}
			}
		}
	}

	/**
	 * 
	 * @param allParents
	 * @param allChildren
	 * @param parentTaskId
	 * @return null if it finds nothing or the list of stories of the closest ancestor
	 */
	private static List<StructuralDataChange> lookUpParent(Map<String, List<StructuralDataChange>> allParents,
			Map<String, List<StructuralDataChange>> allChildren, String parentTaskId) {

		List<StructuralDataChange> parentsEvents = allParents.get(parentTaskId);

		String curPparent = parentTaskId;

		if(allParents.containsKey(curPparent))
			parentsEvents = allParents.get(curPparent);

		else {
			if(allChildren.containsKey(curPparent)) {
				parentsEvents = allChildren.get(curPparent);
				if(parentsEvents.get(0).getParentTaskId() == null)
					System.err.println("Error: this child should have a parent."+parentsEvents.get(0).getParentTaskId());
				else {
					curPparent = parentsEvents.get(0).getParentTaskId();
				}
			}
			parentsEvents = allParents.get(curPparent);
		}
		return parentsEvents;
	}

	private static void cleanup(Map<String, List<StructuralDataChange>> allParents) {
		Set<String> keys = new TreeSet<String>(allParents.keySet());
		for (String key : keys) {
			List<StructuralDataChange> changes = new ArrayList<StructuralDataChange>(allParents.get(key));
			for (StructuralDataChange sdc : changes) {
				if(sdc.isRenderedAsSeparator()) {
					//					allParents.get(key).remove(sdc);
					allParents.remove(key);
					//					System.out.println("Removed task : "+Arrays.asList(sdc.csvRow()));
				}
				if(sdc.isChangeAccountabilityPurpose() && 
						sdc.getMessageType().equals("derived")) {
					if(allParents.containsKey(key)) {
						allParents.get(key).remove(sdc);
						//					allParents.remove(sdc.getTaskId());
						//						System.out.println("Removed derived task : "+Arrays.asList(sdc.csvRow()));
					}
				}
			}
		}
	}

	private static void markChangeToAccountabilityPurpose(Map<String, List<StructuralDataChange>> allChildren) {
		Set<String> keys = allChildren.keySet();
		String lastParentId = "";
		boolean changeAccPurpFound = false;
		for (String childId : keys) {
			List<StructuralDataChange> changes = allChildren.get(childId);
			String currParentId = changes.get(0).getParentTaskId();
			if(!currParentId.equals(lastParentId)) {
				changeAccPurpFound = false;
				lastParentId=currParentId;
			}
			for (StructuralDataChange sdc : changes) {
				//				if(sdc.getParentTaskId().equals("12685694210861"))
				//					Sys-tem.out.println("qui");
				if(sdc.getTaskName().toLowerCase().startsWith("purpose") || 
						sdc.getTaskName().toLowerCase().startsWith("accountabilit")) {
					changeAccPurpFound = true;
				}
				if(sdc.getTaskName().toLowerCase().startsWith("assignee"))
					changeAccPurpFound = false;
				if(changeAccPurpFound) {
					sdc.setChangeAccountabilityPurpose(true);
				}
			}
		}
	}

	private static void transferToFather(Map<String, List<StructuralDataChange>> allParents,
			Map<String, List<StructuralDataChange>> allChildren) {
		Set<String> children = allChildren.keySet();
		List<StructuralDataChange> addedToFather = new ArrayList<StructuralDataChange>();
		for (String child : children) {
			List<StructuralDataChange> changesOfChild = allChildren.get(child);

			for (StructuralDataChange sdc : changesOfChild) {
				if(sdc.isChangeAccountabilityPurpose()) {
					String fatherId = sdc.getParentTaskId();
					//					if(sdc.getTaskId().equals("847322767816828"))
					//						System.out.println("Delete me.");

					boolean isGrandchild = false;

					if(allChildren.containsKey(fatherId)) {
						isGrandchild = true;
						//						System.out.println("Found grandchild!");
					}

					List<StructuralDataChange> parentsEvents = (!isGrandchild)? allParents.get(fatherId): allChildren.get(fatherId);

					if(parentsEvents!=null) {
						addToFather(sdc, parentsEvents, AsanaActions.CHANGE_ACCOUNTABILITY_PURPOSE);
						addedToFather.add(sdc);
					}		
				}
			}
		}
		for (StructuralDataChange sdc : addedToFather) {
			allChildren.remove(sdc.getTaskId());
		}
	}

	private static void addToFather(StructuralDataChange sdc, List<StructuralDataChange> parentsEvents, int code) {
		if(sdc.getMessageType().equals("derived"))
			return;
		//		sdc.setEventId(sdc.getTaskId());		
		sdc.setParentTaskId(parentsEvents.get(1).getParentTaskId());
		sdc.setParentTaskName(parentsEvents.get(1).getParentTaskName());
		sdc.setTaskId(parentsEvents.get(1).getTaskId());
		sdc.setTaskName(parentsEvents.get(1).getTaskName());
		//		sdc.setCircleIds(parentsEvents.get(1).getCircleIds());
		//		sdc.setCircle(parentsEvents.get(1).getCircle());
		sdc.setRawDataText("[EVENT FROM PURPOSE/ACCOUNTABILITY SUBTASK] "+sdc.getRawDataText());
		sdc.setTypeOfChange(code);
		sdc.setTypeOfChangeDescription(AsanaActions.codeToString(code));
		sdc.setTaskCompletedAt(parentsEvents.get(1).getTaskCompletedAt());
		parentsEvents.add(sdc);
	}

	private static Map<String, List<StructuralDataChange>> getChildren() {
		String sql = "SELECT * FROM `SpringestRaw` WHERE parentTaskId<>''";
		Map<String, List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>(); 
		return getFromDB(sql, res);
	}

	/**
	 * Returns the history of the parents only
	 * @return
	 */
	private static Map<String, List<StructuralDataChange>> getParents() {
		String sql = "SELECT * FROM `SpringestRaw` WHERE parentTaskId=''";

		Map<String, List<StructuralDataChange>> parents = new LinkedHashMap<String, List<StructuralDataChange>>(); 
		return getFromDB(sql, parents);
	}

	private static Map<String, List<StructuralDataChange>> getFromDB(String sql,
			Map<String, List<StructuralDataChange>> parents) {
		List<StructuralDataChange> events = ReadFromDB.readFromDBNoSort("asana_manual5", sql);

		for (StructuralDataChange sdc : events) {
			if(parents.containsKey(sdc.getTaskId())) {
				List<StructuralDataChange> historyOfParent = parents.get(sdc.getTaskId());
				historyOfParent.add(sdc);
			}
			else {
				ArrayList<StructuralDataChange> historyOfParent = new ArrayList<StructuralDataChange>();
				historyOfParent.add(sdc);
				parents.put(sdc.getTaskId(), historyOfParent);
			}
		}

		return parents;
	}

	/**
	 * 
	 * @throws IOException
	 */
	private static void fixCircles() throws IOException {
		String sql = "SELECT * FROM `SpringestRaw` ORDER BY taskId, `timestamp`;";

		List<StructuralDataChange> events = ReadFromDB.readFromDB("asana_manual5", sql);
		System.out.println("Read "+events.size()+" events.");		
		String csv = "outCircles.csv";

		PrintWriter rolesFileWriter = new PrintWriter(
				new OutputStreamWriter(
						new FileOutputStream(csv), StandardCharsets.UTF_8) );

		CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
		String[] header = StructuralDataChange.csvHeaderCircle();

		csvWriter.writeNext(header);
		List<String> circles = new ArrayList<String>();
		String lastTaskId = null;
		int lastTypeOfChange = -1;
		boolean migration = false;
		TreeSet<TimestampCircle> treeSet = new TreeSet<TimestampCircle>();
		List<String> unionCircles = new ArrayList<String>();
		boolean becameIndependent = false;
		int timesAddedToCircle = 0;
		boolean assigneeFound = false;
		boolean purpAccFound = false;
		StructuralDataChange lastParent = events.get(0);

		for (StructuralDataChange sdc : events) {

			String currTaskId = sdc.getTaskId();
			if(!currTaskId.equals(lastTaskId)) { //new parent
				lastTaskId = currTaskId;
				circles = new ArrayList<String>();
				lastTypeOfChange = -1;
				migration = false;
				becameIndependent = false;
				timesAddedToCircle = 0;
				assigneeFound = false;
				purpAccFound = false;
				lastParent = sdc;
			}

			if(sdc.getParentTaskId()!=null && !sdc.getParentTask().isEmpty()) { // this is a subtask and must inherit all fathers circles
				List<String> circlesFather = new ArrayList<String>();
				String parentId = sdc.getParentTaskId();
				TreeSet<TimestampCircle> fathersHistory = circlesOfTasks.get(parentId);
				circlesFather = getCirclesAtTime(fathersHistory, sdc.getStoryCreatedAt());

				if(!becameIndependent)
					circles = GeneralUtils.union(circles, circlesFather);

			}

			if(sdc.getTypeOfChange()==12) { 
				sdc.setTypeOfChange(AsanaActions.DESIGN_ROLE);
				sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.DESIGN_ROLE));
			}

			if(sdc.getTypeOfChange()==AsanaActions.ADD_TO_CIRCLE) {
				String curCircle = sdc.getRawDataText().replaceAll("added to ", "").trim();
				int i = lookup(curCircle); // if -1 then it is not a circle
				if(i!=-1) {
					if(timesAddedToCircle==0) {
						setChange(sdc, AsanaActions.CREATE_ROLE);
					}
					timesAddedToCircle++;
					if(!circles.contains(curCircle)) {
						circles.add(curCircle);
						TreeSet<TimestampCircle> ts = new TreeSet<TimestampCircle>();
						List<String> copyOfCircles = new ArrayList<String>();
						copyOfCircles.addAll(circles);		
						ts.add(new TimestampCircle(sdc.getStoryCreatedAt(), copyOfCircles));
						//						circlesOfTasks.put(currTaskId, ts);
						if(circlesOfTasks.get(currTaskId)==null)
							circlesOfTasks.put(currTaskId, ts);
						else
							circlesOfTasks.get(currTaskId).addAll(ts);

						if(lastTypeOfChange==AsanaActions.REMOVE_FROM_CIRCLE)
							migration=true; // if the task is the same and last action was a remove from circle, then we have a migration

						lastTypeOfChange=AsanaActions.ADD_TO_CIRCLE;
					}
				}
				else {
					sdc.setTypeOfChange(AsanaActions.IGNORE_OR_DELETE);
					sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.IGNORE_OR_DELETE));
				}
			}
			if(sdc.getTypeOfChange()==AsanaActions.REMOVE_FROM_CIRCLE) {
				String curCircle = sdc.getRawDataText().replaceAll("removed from ", "").trim();
				int i = lookup(curCircle); // if -1 then it is not a circle
				if(i!=-1) {
					if(circles.contains(curCircle)) {
						circles.remove(curCircle);
						lastTypeOfChange=AsanaActions.REMOVE_FROM_CIRCLE;
					}

					TreeSet<TimestampCircle> fathersHistory = circlesOfTasks.get(sdc.getParentTaskId());
					List<String> circlesFather = getCirclesAtTime(fathersHistory, sdc.getStoryCreatedAt());
					if(GeneralUtils.intersection(circles, circlesFather).isEmpty())
						becameIndependent = true;
				}
				else {
					sdc.setTypeOfChange(AsanaActions.IGNORE_OR_DELETE);
					sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.IGNORE_OR_DELETE));
				}
			}
			sdc.setCircle(commaSeparate(circles));
			sdc.setCircleIds(commaSeparateIds(circles));
			csvWriter.writeNext(sdc.csvRowCircle());
		}
		csvWriter.flush();
		csvWriter.close();
	}

	private static List<String> getCirclesAtTime(TreeSet<TimestampCircle> fathersHistory, DateTime currentTime) {
		List<String> circlesAtTime = new ArrayList<String>();
		if(fathersHistory==null)
			return circlesAtTime;
		TimestampCircle fathersLastEvent = null;
		long diff = Long.MAX_VALUE;
		long lastDiff = diff;
		for (TimestampCircle timestampCircle : fathersHistory) {
			diff=currentTime.getValue()-timestampCircle.timestamp.getValue();
			if(diff>0) {
				fathersLastEvent = timestampCircle;
				lastDiff=diff;
			}
			else
				break;
		}

		if(fathersLastEvent!=null)
			circlesAtTime.addAll(fathersLastEvent.circle);

		return circlesAtTime;
	}

	private static String commaSeparateIds(List<String> circles) {
		String circleIds = "";
		boolean hit = false;
		for (String c : circles) {
			if(c.equals("11555199602299"))
				System.out.println("FOUNDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
			int idx = lookup(c);
			circleIds+=authoritativeList[idx]+",";
			hit=true;
		}
		if(hit)
			circleIds = ""+circleIds.substring(0, circleIds.length()-1);

		return circleIds;
	}

	private static String commaSeparate(List<String> circles) {
		if(circles.isEmpty())
			return "NO CIRCLE";
		String res = "";
		for (int i = 0; i < circles.size()-1; i++) {
			res += circles.get(i) + ",";
		}
		res+=circles.get(circles.size()-1);
		return res;
	}

	private static void addTwoMoreRows() throws FileNotFoundException, IOException {
		Set<String> authListNames = Sets.newHashSet(authoritativeListNames);

		List<StructuralDataChange> events = ReadFromDB.readFromDB("asana_manual4", null);
		System.out.println("Read "+events.size()+" events.");		
		String csv = "out.csv";

		PrintWriter rolesFileWriter = new PrintWriter(
				new OutputStreamWriter(
						new FileOutputStream(csv), StandardCharsets.UTF_8) );

		CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
		String[] header = StructuralDataChange.csvHeaderCircle();

		csvWriter.writeNext(header);

		for (StructuralDataChange sdc : events) {

			String lastCircle = null;
			String currentCircleName = null;
			String currentCircleId = sdc.getProjectId();
			sdc.setCircle(sdc.getProjectName());
			sdc.setMigration(false);

			if(mapTaskCurrentCircle.containsKey(sdc.getTaskId())) { // already seen
				lastCircle = mapTaskCurrentCircle.get(sdc.getTaskId());

				if(sdc.getTypeOfChange()==AsanaActions.ADD_TO_CIRCLE) {
					currentCircleName = sdc.getRawDataText().replaceAll("added to ", "").trim();
					//					if(authListNames.contains(currentCircleName)) {
					//						System.out.println(sdc.getTaskName()+"," + sdc.getRawDataText() +
					//								" is contained in "+ authListNames);
					//					}
					int i = lookup(currentCircleName); // if -1 then it is not a circle

					if(i!=-1)
						currentCircleId = authoritativeList[i];

					if(!lastCircle.equals(currentCircleId) && i!=-1) {
						sdc.setMigration(true);
						sdc.setCircle(currentCircleName);
						mapTaskCurrentCircle.put(sdc.getTaskId(), currentCircleId);
					}
					else { // still in the same circle
						sdc.setMigration(false);
						sdc.setCircle(sdc.getProjectName());
					}
				}
			}

			mapTaskCurrentCircle.put(sdc.getTaskId(), currentCircleId);			
			csvWriter.writeNext(sdc.csvRowCircle());
		}

		csvWriter.flush();
		csvWriter.close();
	}

	private static int lookup(String currentCircleName) {
		boolean found = false;
		int i = 0;
		for (; i < authoritativeListNames.length; i++) {
			if(currentCircleName.equals(authoritativeListNames[i])) {
				found = true;
				break;
			}
		}
		if(currentCircleName.equals("☺ Smooth Ops Roles"))
			return 7;
		if(found)
			return i;

		return -1;
	}
}
