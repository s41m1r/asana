package at.ac.wu.asana.db.postprocess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import at.ac.wu.asana.csv.ReadInfoFromCSV;
import at.ac.wu.asana.csv.WriteUtils;
import at.ac.wu.asana.db.io.ReadFromDB;
import at.ac.wu.asana.db.postprocess.datastructures.AuthoritativeList;
import at.ac.wu.asana.db.postprocess.datastructures.CircleTimeRange;
import at.ac.wu.asana.db.postprocess.datastructures.TimestampCircle;
import at.ac.wu.asana.model.AsanaActions;
import at.ac.wu.asana.model.StructuralDataChange;
import at.ac.wu.asana.util.GeneralUtils;

public class PostProcessFromDB {

	static Map<String, String> subCirclesOf = new HashMap<String, String>();
	static Map<String, String> mapTaskCurrentCircle = new HashMap<String, String>();
	static Map<String, TreeSet<TimestampCircle>> circlesOfTasks = new HashMap<String, TreeSet<TimestampCircle>>();
	static Map<String, ArrayList<StructuralDataChange>> mapTaskStories = new LinkedHashMap<String, ArrayList<StructuralDataChange>>();

	public static void main(String[] args) throws IOException {

		final String VERSION = "-test";

		long start = System.currentTimeMillis();
		//parent, child
		subCirclesOf.put("404651189519209", "236886514207498");
		subCirclesOf.put("11555199602299", "560994092069672");
		subCirclesOf.put("11555199602299", "561311958443380");
		subCirclesOf.put("11347525454570", "824769296181501");
		subCirclesOf.put("11555199602299", "1133031362168396");
		subCirclesOf.put("1181577597127617", "1183127380297106");
		subCirclesOf.put("1181577597127617", "1158107169298919");
		subCirclesOf.put("1160287303922155", "1160716728801873");

		Map<String, List<StructuralDataChange>> allParents = getParents(); //23488
		Map<String, List<StructuralDataChange>> allChildren = getChildren(); //39945

		Integer parentsSize = allParents.values().stream().mapToInt(List::size).sum();
		Integer childrenSize = allChildren.values().stream().mapToInt(List::size).sum();

		System.out.println("Checksum "+ (parentsSize + childrenSize) + ", DB (63433)");
		assertEquals(63433, parentsSize + childrenSize);

		// allProjects-2021-04-21-merged 
		// allProjects-2021-05-25
		String filename = "auxFiles/allProjects-2021-05-28.txt"; // csv must have no quotes
		List<String> projects = getAllProjectNames(filename);
		Set<String> forceToChild = getWeirdTasks();

		//		isPresent("13078448643099", allParents.keySet());
		//		isPresent("13078448643099", allChildren.keySet());

		Map<String, List<StructuralDataChange>> merge = new LinkedHashMap<String, List<StructuralDataChange>>();

		//		int clearedChildren = cleanup(allChildren);
		//		int clearedParents =  cleanup(allParents);

		merge.putAll(allParents);
		merge.putAll(allChildren);

		int allSize = merge.values().stream().mapToInt(List::size).sum();
		assertEquals(63433, allSize);
		
		Map<String, List<StructuralDataChange>> allNoSep = filterOutSeparator(merge); // filters out 6732 events

		allSize = allNoSep.values().stream().mapToInt(List::size).sum();
		assertEquals(63433, allSize+6732);
		//		assertTrue(allNoSep.containsKey("472476427713571"));
		//		Map<String, List<StructuralDataChange>> allDup = filterOutSubtasksWithNoEvent(allNoSep); // filters out 248 events
		//		assertFalse(allNoSep.containsKey("472476427713571"));


		allSize = allNoSep.values().stream().mapToInt(List::size).sum();
		assertEquals(63433, allSize + 6732); 

		Map<String, List<StructuralDataChange>> dup = getAllDuplicatedTasks(allNoSep); 
		//		Map<String, List<StructuralDataChange>> all = removeDesignDerivedDupli(dup, a);

		Map<String, List<StructuralDataChange>> all = cleanUpDesignDupli(allNoSep, dup); // filters out 1381 events
		allSize = all.values().stream().mapToInt(List::size).sum();

		assertEquals(63433, allSize + 6732 +  1381);

		duplicateTaskToDesignRole(all, dup);

		setRemoveSubRole(all, projects);
		setComment(all);

		manualFixForCode99(all);
		
		Map<String, String> dictionary = getAllTaskIdName(all);
			
		//		fixChildAsRoleProblem(allParents, allChildren);
		Set<String> allOrphanIds = fixOrphans(allParents, allChildren);
		
		String manAnnoAssiFile = "auxFiles/assigned-the-task-complete.csv";
		integrateManuallyAnnotatedCurrentAssignee(allParents, manAnnoAssiFile, false);
		integrateManuallyAnnotatedCurrentAssignee(allChildren, manAnnoAssiFile, false);
		includeManuallyAnnotatedCreatedByName(allParents);
		includeManuallyAnnotatedCreatedByName(allChildren);

		fillAssignee(allParents);
		fillAssignee(allChildren);

		String neverAssiButUnassign = "auxFiles/manuallyFixUnassigned.csv";
		integrateManuallyAnnotatedCurrentAssignee(all, neverAssiButUnassign, true);	

		//		List<String> downgradedRolesEvents = getIdsOfChildrenOlderThanFather(allParents,allChildren);		
		//		fixDowngradedRoles(allParents, allChildren, downgradedRolesEvents);
		
		int lastModRemoved = fixCompletedAndRemoveLastModify(all);
		allSize = all.values().stream().mapToInt(List::size).sum();

		assertEquals(63433, allSize + 6732 + 1381 + lastModRemoved);
		
		int removedCode14Events = removeCode14(all);
		Logger.getLogger(PostProcessFromDB.class.getName()).info("Removed "+removedCode14Events+" events with code 14 (AsanaActions.COMPLETE_ROLE)");
		
		setRoleType(all);
		
		setAliveStatus(all);
		
		Set<String> tasksNeverAddedOrRemoved = getTasksNeverAddedOrRemoved(all);
	
		setDynamicHierarchy(all, allOrphanIds, forceToChild, tasksNeverAddedOrRemoved);
		setDynamicHierarchyDuplicated(all, forceToChild, projects);
		
		List<String> allTasksWhoChangedFather = getAllTasksWhoChangedFather(all, projects);
		Set<String> allTasksWhoseFatherIsLast = getAllTasksAddedAndNotRemoved(all);

		allTasksWhoseFatherIsLast.removeAll(allTasksWhoChangedFather);

		List<String[]> matchedTasks = new ArrayList<String[]>();
				
		assignDynamicFather(
				all,dictionary, 
				allTasksWhoChangedFather, matchedTasks, 
				allTasksWhoseFatherIsLast, forceToChild);
		
		//		List<String[]> subsetEvents = addMatchedParents(dictionary,matchedTasks);
			
		manualFixHierarchy(allParents, allChildren);

		fillDynamicFather(all);

		deleteParentTaskIdFromDynamicParent(all);	

		//check that we really delete codes 14 and 15

		//		WriteUtils.writeList(subsetEvents, "/home/saimir/ownCloud/PhD/Collaborations/"
		//				+ "Waldemar/Springest/Data/Data Extracted from DB/Dataset with Manually Extracted Users/"
		//				+ "testing/subset-events"+VERSION+".csv");	

		WriteUtils.writeMapWithDynamic(all, "/home/saimir/ownCloud/PhD/Collaborations/"
				+ "Waldemar/Springest/Data/Data Extracted from DB/Dataset with Manually Extracted Users/"
				+ "testing/out"+VERSION+".csv");
		//
		//		WriteUtils.writeMap(matchedParents, "/home/saimir/ownCloud/PhD/Collaborations/" + 
		//				"Waldemar/Springest/Data/Data Extracted from DB/Dataset with Manually Extracted Users/" + 
		//				"testing/matched-parents"+VERSION+".csv");
		//
		//		WriteUtils.writeMap(dictionary, "/home/saimir/ownCloud/PhD/Collaborations/" + 
		//				"Waldemar/Springest/Data/Data Extracted from DB/Dataset with Manually Extracted Users/" + 
		//				"testing/dictionary"+VERSION+".csv");

		//		Set<String[]> m = new HashSet<String[]>(matchedTasks);
		//		WriteUtils.writeList(new ArrayList<String[]>(m), "/home/saimir/ownCloud/PhD/Collaborations/" + 
		//				"Waldemar/Springest/Data/Data Extracted from DB/Dataset with Manually Extracted Users/" + 
		//				"testing/matched-tasks"+VERSION+".csv");

		//		 all good until here
		
		
		fixCodingAccordingToAuthoritativeList(all, projects);
		
		setTextOfDesignRole(all);
	
		setDesignRole(all);

		allSize = all.values().stream().mapToInt(List::size).sum();

		assertEquals(63433, allSize + 6732 + 1381 + removedCode14Events + lastModRemoved);

		Map<String, List<StructuralDataChange>> allEvents = setCurrentCircles(all);		

		assertEquals(allSize, allEvents.values().stream().mapToInt(List::size).sum());

		allSize = allEvents.values().stream().mapToInt(List::size).sum();
		assertEquals(63433, allSize + 6732 + 1381 + removedCode14Events + lastModRemoved);
		
		Map<String,List<StructuralDataChange>> allEvents2 = sortHistory(dynamicChildToParent(allEvents)); // 0 events are being skipped 
		
		setCircleInheritanceFromParent(allEvents2);

//		assertEquals(47231, allEvents2.values().stream().mapToInt(List::size).sum());

		allSize = allEvents2.values().stream().mapToInt(List::size).sum();
		assertEquals(63433, allSize + 6732 + 1381 + removedCode14Events + lastModRemoved);

		fixTimeShift(allEvents2);// convert from UTC to Local time zone

		setRoleExtractionIntegration(allEvents2, forceToChild);

		//		checkIfNullTimestamp(allEvents);
		//		Map<String, List<StructuralDataChange>> allEventsNoDup = removeDuplicateTasks(allEvents2); // 1804 events are removed because of same timestamp (duplicate)

		//		System.out.println("Events after removeDuplicateTasks = " +countEvents(allEventsNoDup));
		//		printHistoryOfTask("44330423794233", allEventsNoDup); 	

		fillCurrentAssignees(allEvents2);

		setYinYangAsCircleChange(allEvents2);
		//		manualFixCode17(allEventsNoDup);

		allSize = allEvents2.values().stream().mapToInt(List::size).sum();
		System.out.println("Size before remove dups: "+allSize);
		List<StructuralDataChange> uniqueEvents = removeDups(allEvents2);	//there are 1850 duplicates
		System.out.println("There are unique events = "+uniqueEvents.size()); 

		allSize = uniqueEvents.size();
		assertEquals(63433, allSize + 6732 + 1381 + removedCode14Events + lastModRemoved + 1800);

		fixChangeRoleName(uniqueEvents);
		//		addCompletedEvent(uniqueEvents)

		fillTypeOfChangeNew(uniqueEvents);

		Map<String, Set<String>> dict = uniformAssignees(uniqueEvents);

		Map<String, String> revDict = addCurrentAssigneeId(uniqueEvents,dict);
		setMergedCurrentAssgineeIds(uniqueEvents,revDict);

		String circleEvents = "auxFiles/circleParents-3.csv";
		addSecondDegreeCircle(uniqueEvents, circleEvents);

		setIgnoreEvent(uniqueEvents);
		
		setCompleteAsAlive(uniqueEvents);
		setEventFromOrphan(uniqueEvents, allOrphanIds);
		setOrphansToIgnore(uniqueEvents, allOrphanIds);
		removeOrphanLabelFromSubtask(uniqueEvents);

		addIgnoreToChild(uniqueEvents);
		fixCode5problems(uniqueEvents); 

		fixCode15Problems(uniqueEvents);

		String code20createCircle = "auxFiles/code20CreateCircle.csv";
		manuallySetCode20(uniqueEvents, code20createCircle, revDict);

		//		printHistoryOfTask("11555199602323", allEventsNoDup);

		String outfile = "Springest-filtered.csv";
		//		WriteUtils.writeListOfChangesWithCircleToCSV(uniqueEvents, outfile);
		allSize = uniqueEvents.size();
		assertEquals(63433, allSize + 6732 + 1381 + removedCode14Events + lastModRemoved + 1800);
		
		outfile = outfile.replace("filtered", "Mappe1");
		WriteUtils.writeMappeToCSV(uniqueEvents, outfile);

		//		WriteUtils.writeMapOfChangesWithCircleToCSV2(allEventsNoDup, outfile);

		//		checkIfNoDesigned(allEvents);
		
		List<StructuralDataChange> l = uniqueEvents.stream().filter(
				e -> e.getRawDataText().trim().equals("[EVENT FROM SUB-TASK]")).collect(Collectors.toList());
		
		WriteUtils.writeMappeToCSV(l, "test.csv");

		System.out.println("Done in "+(System.currentTimeMillis()-start)/1000+" sec.");
		System.out.println("Wrote on file "+outfile);
	}

	private static void setCircleInheritanceFromParent(Map<String, List<StructuralDataChange>> allEvents2) {
		for (String k : allEvents2.keySet()) {
			String curC = "NO CIRCLE";
			for (StructuralDataChange sdc : allEvents2.get(k)) {
				if(Timestamp.valueOf(sdc.getStoryCreatedAt()).toString().contains("27:48.109"))
					System.out.println("debug setCircleInheritanceFromParent");
					
				if(sdc.getChildId()==null && sdc.getGrandChildId()==null && 
						(sdc.getTypeOfChange()==AsanaActions.ADD_TO_CIRCLE || 
						sdc.getTypeOfChange() == AsanaActions.CREATE_ROLE || 
						sdc.getTypeOfChange() == AsanaActions.REMOVE_FROM_CIRCLE ||
						sdc.getTypeOfChange() == AsanaActions.ROLE_EXTRACTION || 
						sdc.getTypeOfChange() == AsanaActions.REMOVE_SUB_ROLE) ) { // any circle change at parent 
					curC = sdc.getCircle();
				}	
				sdc.setCircleInheritedFromParent(curC);
			}		
		}
	}

	private static void removeOrphanLabelFromSubtask(List<StructuralDataChange> uniqueEvents) {
		uniqueEvents.stream().filter(e -> e.getRawDataText().contains("[EVENT FROM ORPHAN]") && 
				e.getRawDataText().contains("[EVENT FROM SUB-TASK]")).
		forEach(e -> {
			String rdt = e.getRawDataText();
			String newRdt = rdt.replace("[EVENT FROM ORPHAN] ", "");
			e.setRawDataText(newRdt);
		});		
		
	}

	private static Set<String> getTasksNeverAddedOrRemoved(Map<String, List<StructuralDataChange>> all) {
		Set<String> res = new HashSet<String>();
		for (String k : all.keySet()) {
			boolean addRem = false;
			for (StructuralDataChange e : all.get(k)) {
				if(e.getRawDataText().contains("added subtask to task") || e.getRawDataText().contains("removed from")) {
					addRem = true;
					break;
				}
			}
			if(!addRem)
				res.add(k);
		}
		return res;
	}

	private static void manualFixCode17(Map<String, List<StructuralDataChange>> allEventsNoDup) {
		String[] allEvents = new String[] {
				"2019-07-01 15:52:42.749",
				"2019-09-10 17:14:23.949",
				"2020-07-13 11:50:01.359",
				"2020-07-13 11:50:12.608"
		};

		for (String e : allEvents) {
			StructuralDataChange sdc = getSDCAt(allEventsNoDup, e);
			setChange(sdc, AsanaActions.IGNORE_EVENT);
			setChangeNew(sdc, AsanaActions.IGNORE_EVENT);
		}	
	}

	private static Map<String, List<StructuralDataChange>> cleanUpDesignDupli(Map<String, List<StructuralDataChange>> all, Map<String, List<StructuralDataChange>> dup) {
		Map<String, List<StructuralDataChange>> filtered = new LinkedHashMap<String, List<StructuralDataChange>>();
		int removed = 0;
		Set<StructuralDataChange> removeCand = new HashSet<StructuralDataChange>();
		for (String k : all.keySet()) {
			for (StructuralDataChange e : all.get(k)) {
				if(dup.containsKey(e.getTaskId()) && e.getMessageType().equals("derived")) {//duplicated and derived
					//					all.get(k).remove(e);
					removed++;
					removeCand.add(e);
				}
			}
		}

		for (String k : all.keySet()) {
			for (StructuralDataChange e : all.get(k)) {
				if(!removeCand.contains(e)) {
					if(!filtered.containsKey(k)) {
						filtered.put(k, new ArrayList<StructuralDataChange>());
					}
					filtered.get(k).add(e);
				}
			}

		}
		System.out.println("Removed "+removed+ " derived events from duplicated tasks");
		return filtered;
	}

	private static Map<String, List<StructuralDataChange>> removeDesignDerivedDupli(Map<String, List<StructuralDataChange>> all,
			Map<String, List<StructuralDataChange>> dupli) {
		Map<String, List<StructuralDataChange>> dup = getAllDuplicatedTasks(all);
		Map<String, List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>();

		for (String k : all.keySet()) {
			List<StructuralDataChange> history = dup.get(k);
			List<StructuralDataChange> newHist = new ArrayList<StructuralDataChange>();
			if(!dup.containsKey(k)) {
				newHist = history;
			}
			else { // it is duplicated, only pick event that are not derived
				for(StructuralDataChange e : history) {
					if(!e.getMessageType().equals("derived")) {
						newHist.add(e);
					}
				}
			}
			res.put(k, newHist);
		}
		return res;
	}


	private static String getEventAt(List<StructuralDataChange> uniqueEvents, String searchTS) {
		String row[] = null;
		for(StructuralDataChange sdc : uniqueEvents) {
			if(Timestamp.valueOf(sdc.getStoryCreatedAt()).toString().equals(searchTS)){
				row = sdc.csvRowMappe1();
			}
		}
		return Arrays.toString(row);
	}

	private static void fixCode15Problems(List<StructuralDataChange> uniqueEvents) {
		uniqueEvents.stream().filter(e -> e.getTypeOfChangeNew() == AsanaActions.CHANGE_ACCOUNTABILITY_PURPOSE && 
				e.getTypeOfChange() != AsanaActions.COMMENT && 
				e.getTypeOfChange() != AsanaActions.IGNORE_EVENT)
		.forEach(e -> setChange(e, AsanaActions.CHANGE_ACCOUNTABILITY_PURPOSE));
	}

	private static void fixCode5problems(List<StructuralDataChange> uniqueEvents) {
		String[] tss = new String[] {"2019-07-01 15:54:08.268",	"2019-07-01 15:54:30.192"};
		List<String> tssList = Lists.newArrayList(tss);
		uniqueEvents.stream().filter(
				e -> tssList.contains(
						Timestamp.valueOf(e.getStoryCreatedAt()).toString()))
		.forEach(e -> setChange(e, AsanaActions.CHANGE_ACCOUNTABILITY_PURPOSE));
	}

	private static void addIgnoreToChild(List<StructuralDataChange> uniqueEvents) {
		uniqueEvents.stream().filter(
				e -> e.getTypeOfChangeNew() == AsanaActions.IGNORE_EVENT &&
				e.getTypeOfChangeOriginal() == AsanaActions.ROLE_INTEGRATION)
		.forEach(e -> {
			setChange(e, AsanaActions.IGNORE_EVENT);
		});
	}

	private static void setOrphansToIgnore(List<StructuralDataChange> uniqueEvents, Set<String> allOrphanIds) {
		uniqueEvents.stream().filter(
				e -> (e.getTypeOfChange() == AsanaActions.ROLE_INTEGRATION ||
				e.getTypeOfChange() == AsanaActions.ROLE_EXTRACTION) || 
				(e.getTypeOfChange() == AsanaActions.ADD_SUB_ROLE &&
				e.getTypeOfChangeNew() == AsanaActions.ADD_SUB_ROLE)
				).
		forEach(e -> {
			if(//e.getChildId()==null || e.getChildId().isEmpty()
					allOrphanIds.contains(e.getTaskId())) {
				setChangeNew(e, AsanaActions.IGNORE_EVENT);
				setChange(e, AsanaActions.IGNORE_EVENT);
			}
			else {
				setChangeNew(e, e.getTypeOfChangeOriginal());
			}
		});
	}

	private static void setEventFromOrphan(List<StructuralDataChange> uniqueEvents, Set<String> allOrphanIds) {
		uniqueEvents.stream().filter(e -> allOrphanIds.contains(e.getTaskId())).
		forEach(e -> {
			String rdt = e.getRawDataText();
			String newRdt = String.join(" ", "[EVENT FROM ORPHAN]", rdt);
			e.setRawDataText(newRdt);
		});		
	}

	private static void setCompleteAsAlive(List<StructuralDataChange> uniqueEvents) {
		uniqueEvents.stream().filter(e -> e.getTypeOfChange() == AsanaActions.DELETE_OR_MARK_COMPLETE ||
				e.getTypeOfChange() == AsanaActions.DELETE_CIRCLE).
		forEach(e -> e.setAliveStatus("alive"));
	}

	private static void setIgnoreEvent(List<StructuralDataChange> uniqueEvents) {
		for (StructuralDataChange sdc : uniqueEvents) {
			if(sdc.getTypeOfChange() == AsanaActions.IGNORE_OR_DELETE || 
					sdc.getTypeOfChange() == AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK ||
					sdc.getTypeOfChangeNew() == AsanaActions.IGNORE_OR_DELETE ||
					sdc.getTypeOfChangeNew() == AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK) {
				setChange(sdc, AsanaActions.IGNORE_EVENT);
				setChangeNew(sdc, AsanaActions.IGNORE_EVENT);
			}
		}
	}

	private static void setRoleExtractionIntegration(Map<String, List<StructuralDataChange>> allEvents2, Set<String> forceToChild) {
		//		List<String> taksEverInCircle = tasksEverInCircle(allEvents2);		
		for (String tId : allEvents2.keySet()) {			
			for (StructuralDataChange e : allEvents2.get(tId)) {
				if(!e.getCircleIds().isEmpty() &&
						!forceToChild.contains(e.getChildId())) { // it is in a circle && the child was not a weird task
					if(e.getTypeOfChange() == AsanaActions.ADD_SUB_ROLE) {
						if(e.getChildId()!=null && !e.getChildId().isEmpty()) { // it is a child 
							setChange(e, AsanaActions.ROLE_INTEGRATION);
							setChangeNew(e, AsanaActions.CHANGE_SUB_ROLE);
							if(isCircle(e.getTaskName(), e.getTaskId())) // ying&yang tasks
								setChangeNew(e, AsanaActions.CIRCLE_CHANGE);
						}
						else { // it is at parent level
							setChange(e, AsanaActions.ROLE_INTEGRATION);
							setChangeNew(e, AsanaActions.ROLE_INTEGRATION);
							if(isCircle(e.getTaskName(), e.getTaskId())) 
								setChangeNew(e, AsanaActions.CIRCLE_CHANGE);
						}

					}
					if(e.getTypeOfChange() == AsanaActions.REMOVE_SUB_ROLE) {
						if(e.getChildId()!=null && !e.getChildId().isEmpty()) { // it is a child
							setChange(e, AsanaActions.ROLE_EXTRACTION);
							setChangeNew(e, AsanaActions.CHANGE_SUB_ROLE);
							if(isCircle(e.getTaskName(), e.getTaskId())) // ying&yang tasks
								setChangeNew(e, AsanaActions.CIRCLE_CHANGE);
						}
						else { // it is a parent
							setChange(e, AsanaActions.ROLE_EXTRACTION);
							setChangeNew(e, AsanaActions.ROLE_EXTRACTION);
							if(isCircle(e.getTaskName(), e.getTaskId()))
								setChangeNew(e, AsanaActions.CIRCLE_CHANGE);
						}
					}
				}
			}
		}
	}

	private static boolean isInCircle(StructuralDataChange e, List<StructuralDataChange> list) {
		// Design role, and create_role should have been already set
		for (StructuralDataChange sdc : Lists.reverse(list)) {
			if(sdc.getTypeOfChange() == AsanaActions.ADD_TO_CIRCLE || 
					sdc.getTypeOfChange() == AsanaActions.CREATE_ROLE) 
				return true;// scanning the history backwards, if it is a circle, it must have been added and not removed
			if(sdc.getTypeOfChange() == AsanaActions.REMOVE_FROM_CIRCLE)
				return false;
		}
		return false;
	}

	private static List<String> tasksEverInCircle(
			Map<String, List<StructuralDataChange>> allEvents2) {
		List<String> res = new ArrayList<String>();
		for (String tId : allEvents2.keySet()) {
			for(StructuralDataChange e : allEvents2.get(tId)) {
				if(e.getTypeOfChange() == AsanaActions.ADD_TO_CIRCLE
						|| e.getTypeOfChange() == AsanaActions.CREATE_ROLE) {
					res.add(tId);
					break;
				}
			}
		}
		return res;
	}

	private static void setComment(Map<String, List<StructuralDataChange>> all) {
		for(String k : all.keySet()) {
			for (StructuralDataChange e : all.get(k)) {
				if(e.getMessageType().equals("comment")) {
					e.setTypeOfChange(AsanaActions.COMMENT);
					e.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.COMMENT));
				}
			}
		}
	}

	/**
	 * Rule: If rawDataText = removed from T and T is a task (no project, no circle),
	 *  then code it as remove sub role
	 * @param all
	 * @param dictionary 
	 * @param projects 
	 * @param forceToChild 
	 */
	private static void setRemoveSubRole(Map<String, List<StructuralDataChange>> all, List<String> projects) {
		for (String k : all.keySet()) {
			List<StructuralDataChange> history = all.get(k);
			for (StructuralDataChange e : history) {
				String rawDataText = e.getRawDataText();
				if(rawDataText.startsWith("removed from") && 
						!containsCircleName(rawDataText) && 
						!isProject(stripProjectName(rawDataText), projects)) {
					int toc = AsanaActions.REMOVE_SUB_ROLE;
					e.setTypeOfChange(toc);
					e.setTypeOfChangeDescription(AsanaActions.codeToString(toc));
				}
			}
		}
	}

	private static void manualFixForCode99(Map<String, List<StructuralDataChange>> all) {
		StructuralDataChange sdc = getSDCAt(all, "2014-04-10 10:33:09.169");
		String rdt = sdc.getRawDataText();
		String newText = rdt.replace("😍  Smooth Operations Roles", "☺ Smooth Operations Roles");
		sdc.setRawDataText(newText);
	}

	private static int removeCode14(Map<String, List<StructuralDataChange>> all) {
		int removedEvents = 0;
		for (String k : all.keySet()) {
			if(k.equals("1124479127191211"))
				System.out.println("removeCode14 debug");
			for (Iterator<StructuralDataChange> it = all.get(k).iterator(); it.hasNext();) {
				if(it.next().getTypeOfChange() == AsanaActions.COMPLETE_ROLE) {
					it.remove();
					removedEvents++;
				}
			}
		}
		return removedEvents;
	}

	private static Map<String, List<StructuralDataChange>> duplicateTaskToDesignRole(Map<String, List<StructuralDataChange>> all, Map<String, List<StructuralDataChange>> dup) {
		for (String k : dup.keySet()) {
			dup.get(k).stream().filter(e -> e.getRawDataText().startsWith("duplicated task from"))
			.forEach(e -> {
				e.setTypeOfChange(AsanaActions.DESIGN_ROLE);
				e.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.DESIGN_ROLE));
			});
		}
		return dup;
	}

	private static void setMergedCurrentAssgineeIds(List<StructuralDataChange> uniqueEvents,
			Map<String, String> revDict) {
		for(StructuralDataChange e : uniqueEvents) {
			String ma = e.getMergedCurrentAssignees();
			Set<String> aset = new LinkedHashSet<String>();
			if(ma!=null && !ma.isEmpty()) {
				String[] asgs = ma.split(",");
				for (String s : asgs) {
					//					if(dict.containsKey(s.trim())) {
					//						
					//					}
					aset.add(revDict.get(s));
				}
			}
			e.setMergedCurrentAssigneeIds(setToString(aset));
		}

	}

	private static Map<String, String> addCurrentAssigneeId(List<StructuralDataChange> uniqueEvents, Map<String, Set<String>> dict) {
		// reverse the dict (name is key)
		Map<String, String> revDict = new HashMap<String, String>();
		for (String k : dict.keySet()) {
			Set<String> vals = dict.get(k);
			for (String v : vals) {
				revDict.put(v, k);
			}
		}

		for (StructuralDataChange change : uniqueEvents) {
			if(revDict.containsKey(change.getCurrentAssignee()))
				change.setCurrentAssigneeId(revDict.get(change.getCurrentAssignee()));
		}

		return revDict;
	}

	/**
	 * We create a dictionary of all assignee names, so we can uniform them based on their ids (unique)
	 * 
	 * @param uniqueEvents
	 * @return 
	 */
	private static Map<String, Set<String>> uniformAssignees(List<StructuralDataChange> uniqueEvents) {
		Map<String, Set<String>> mapIdName = new HashMap<String, Set<String>>();

		for (StructuralDataChange e : uniqueEvents) {
			if(!mapIdName.containsKey(e.getStoryCreatedById())) {
				mapIdName.put(e.getStoryCreatedById(), new HashSet<String>());
			}
			if(!mapIdName.containsKey(e.getAssigneeId())) {
				mapIdName.put(e.getLastAssigneeId(), new HashSet<String>());
			}	
			mapIdName.get(e.getStoryCreatedById()).add(e.getStoryCreatedByName());
			mapIdName.get(e.getLastAssigneeId()).add(e.getLastAssigneeName());
		}


		for(StructuralDataChange e : uniqueEvents) { // set the first name in the list
			if(mapIdName.containsKey(e.getLastAssigneeId())) {
				e.setLastAssigneeName(mapIdName.get(e.getLastAssigneeId()).toArray(new String[] {})[0]);
			}
			if(mapIdName.containsKey(e.getStoryCreatedById())) {
				e.setStoryCreatedByName(mapIdName.get(e.getStoryCreatedById()).toArray(new String[] {})[0]);
			}			
		}

		return mapIdName;
	}

	private static Map<String, List<StructuralDataChange>> sortHistory(
			Map<String, List<StructuralDataChange>> map) {
		Map<String, List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>();
		for (String k : map.keySet()) {
			List<StructuralDataChange> history = new ArrayList<StructuralDataChange>(map.get(k));
			Collections.sort(history);
			res.put(k,history);
		}
		return res;
	}

	private static void fillCurrentAssignees(Map<String, List<StructuralDataChange>> allEventsNoDup) {
		for (String k : allEventsNoDup.keySet()) {
			Set<String> currentAssignees = new HashSet<String>();

			for (StructuralDataChange sdc : allEventsNoDup.get(k)) {
				if(!sdc.getRoleType().equals("accountability/purpose")) {
					if(sdc.getTypeOfChangeOriginal()==AsanaActions.ASSIGN_TO_ACTOR) {
						String assi = sdc.getCurrentAssignee();
						currentAssignees.add(assi.trim());
					}

					if(sdc.getTypeOfChangeOriginal() == AsanaActions.UNASSIGN_FROM_ACTOR) {
						String assi = sdc.getRawDataText().replace("unassigned from ", "").trim();
						currentAssignees.remove(assi);
					}
					sdc.setMergedCurrentAssignees(setToString(currentAssignees));
				}
			}
		}
	}

	private static void fillTypeOfChangeNew(List<StructuralDataChange> uniqueEvents) {
		uniqueEvents.stream().filter(e -> e.getTypeOfChangeDescriptionNew()==null).forEach(e -> {
			e.setTypeOfChangeDescriptionNew(e.getTypeOfChangeDescriptionOriginal());
			e.setTypeOfChangeNew(e.getTypeOfChangeOriginal());
		});
	}

	private static void setAliveStatus(Map<String, List<StructuralDataChange>> all) {
		for(String k: all.keySet()) {
			List<StructuralDataChange> taskHistory = new LinkedList<StructuralDataChange>(all.get(k));
			java.util.Collections.sort(taskHistory);
			String status = "alive";
			for (StructuralDataChange sdc : taskHistory) {
				if(sdc.getTypeOfChange() == AsanaActions.DELETE_OR_MARK_COMPLETE) {
					status = ""+"dead";
				}
				if(sdc.getTypeOfChange() == AsanaActions.REVIVE_OR_MARK_INCOMPLETE) {
					status = "alive";
				}
				sdc.setAliveStatus(status);
			}
		}
	}

	private static String getEventAt(Map<String, List<StructuralDataChange>> allEvents, String searchTS) {
		String row[] = null;

		for (String k : allEvents.keySet()) {
			for(StructuralDataChange sdc : allEvents.get(k)) {
				if(Timestamp.valueOf(sdc.getStoryCreatedAt()).toString().equals(searchTS)){
					row = sdc.csvRowDynamic();
				}
			}
		}
		return Arrays.toString(row);
	}

	private static StructuralDataChange getSDCAt(Map<String, List<StructuralDataChange>> allEvents, String searchTS) {
		StructuralDataChange res = null;

		for (String k : allEvents.keySet()) {
			for(StructuralDataChange sdc : allEvents.get(k)) {
				if(Timestamp.valueOf(sdc.getStoryCreatedAt()).toString().equals(searchTS)){
					res = sdc;
					break;
				}
			}
		}
		return res;
	}

	private static void deleteParentTaskIdFromDynamicParent(Map<String, List<StructuralDataChange>> all) {
		for (String k : all.keySet()) {
			for (StructuralDataChange sdc : all.get(k)) {
				if(sdc.getDynamicHierarchy().equals("parent")) {
					sdc.setParentTaskId("");
				}
			}
		}
	}

	private static Map<String, List<StructuralDataChange>> dynamicChildToParent(Map<String, List<StructuralDataChange>> all) {

		Map<String, List<StructuralDataChange>> allChildrenDynamic = getSubMapWithHierarchy(all, "child");
		Map<String, List<StructuralDataChange>> allParentsDynamic = getSubMapWithHierarchy(all, "parent");

		int nAll = all.values().stream().mapToInt(List::size).sum();

		int nChild0 = allChildrenDynamic.values().stream().mapToInt(List::size).sum();
		int nParen0 = allParentsDynamic.values().stream().mapToInt(List::size).sum();
		assertEquals(nAll, nChild0+nParen0);

		Logger.getLogger(PostProcessFromDB.class.getName()).info("Begin "+nAll);

		int promoted = promoteOrphans(allChildrenDynamic, allParentsDynamic);

		int nChild1 = allChildrenDynamic.values().stream().mapToInt(List::size).sum();
		int nParen1 = allParentsDynamic.values().stream().mapToInt(List::size).sum();

		assertEquals(nChild0, nChild1 + promoted);
		assertEquals(nParen0, nParen1 - promoted);
		assertEquals(nAll, nChild1+nParen1);

		Map<String, List<StructuralDataChange>> grandChildren = getGrandChildren(allChildrenDynamic);
		int nGran = grandChildren.values().stream().mapToInt(List::size).sum();

		int grandChildEvents = removeOverlappingEvents(grandChildren, allChildrenDynamic);

		int nChild2 = allChildrenDynamic.values().stream().mapToInt(List::size).sum();
		int nGran2 = grandChildren.values().stream().mapToInt(List::size).sum();

		assertEquals(nGran,nGran2);
		assertEquals(nChild1, nChild2+grandChildEvents);
		assertEquals(nChild1+nGran, nChild2+nGran2+grandChildEvents);
		assertEquals(nAll, nChild2+grandChildEvents+promoted+nParen1-promoted); //assertEquals(nAll, nChild0+nParen0);

		int gcB = grandChildren.size();
		int chB = allChildrenDynamic.size();
		int pB = allParentsDynamic.size();

		//		WriteUtils.writeMapWithDynamic(allChildrenDynamic, "children.csv");
		//		WriteUtils.writeMapWithDynamic(allParentsDynamic, "parents.csv");
		//		WriteUtils.writeMapWithDynamic(grandChildren, "grandchildren.csv");

		System.out.println("grandChildren -> allChildrenDynamic");
		assignHierarchy(grandChildren, "grandchild");
		int movedGrandCh = moveToDynamicParent(grandChildren, allChildrenDynamic);
		//		WriteUtils.writeMapWithDynamic(allChildrenDynamic, "children-after.csv");

		int nGran3 = grandChildren.values().stream().mapToInt(List::size).sum();
		assertEquals(nGran3, 0);

		int nChild3 = allChildrenDynamic.values().stream().mapToInt(List::size).sum();
		assertEquals(nChild3, nChild2+movedGrandCh);		

		assignHierarchy(allChildrenDynamic, "child");
		System.out.println("allChildrenDynamic -> allParentsDynamic");
		int movedChildren = moveToDynamicParent(allChildrenDynamic, allParentsDynamic);
		int nParen2 = allParentsDynamic.values().stream().mapToInt(List::size).sum();
		int nChild4 = allChildrenDynamic.values().stream().mapToInt(List::size).sum();

		assertEquals(nParen2, movedChildren+nParen1);
		assertEquals(nChild4, 0);

		Logger.getLogger(PostProcessFromDB.class.getName()).info("End "+nParen2+". Skipped "+(nAll-nParen2) + " derived events during integration.");
		//		
		//		System.out.println("grandchildren before "+gcB+" after "+grandChildren.size());
		//		System.out.println("children before "+chB +  " after "+allChildrenDynamic.size());
		//		System.out.println("parents before "+pB +  " after "+allParentsDynamic.size());

		return allParentsDynamic;
	}


	/** 
	 * 
	 * @param grandChildren – timestamps to remove
	 * @param allChildrenDynamic – where to removed them from
	 */
	private static int removeOverlappingEvents(Map<String, List<StructuralDataChange>> grandChildren,
			Map<String, List<StructuralDataChange>> allChildrenDynamic) {
		int removed = 0;
		for (String k : grandChildren.keySet()) {
			if(allChildrenDynamic.containsKey(k)) {
				removed+=allChildrenDynamic.get(k).size();
				allChildrenDynamic.remove(k);
			}
		}
		return removed;
	}

	private static void assignHierarchy(Map<String, List<StructuralDataChange>> map, String hierarchy) {
		for(String k : map.keySet()) {
			for (StructuralDataChange sdc : map.get(k)) {
				switch (hierarchy) {
				case "grandchild":
					sdc.setGrandChildId(sdc.getTaskId());
					sdc.setGrandChildName(sdc.getTaskName());
					break;

				case "child":
					sdc.setChildId(sdc.getTaskId());
					sdc.setChildName(sdc.getTaskName());
					break;

				default:
					break;
				}
			}
		}
	}

	private static Map<String, List<StructuralDataChange>> getGrandChildren(
			Map<String, List<StructuralDataChange>> allChildrenDynamic) {
		Map<String, List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>();
		for (String k : allChildrenDynamic.keySet()) {
			for(StructuralDataChange e : allChildrenDynamic.get(k)) {
				if(allChildrenDynamic.containsKey(e.getParentTaskId())) {
					if(!res.containsKey(k))
						res.put(k, new ArrayList<StructuralDataChange>());
					res.get(k).add(e);
				}
			}
		}
		return res;
	}

	private static int moveToDynamicParent(Map<String, List<StructuralDataChange>> children,
			Map<String, List<StructuralDataChange>> parents) {
		List<String> keys = new ArrayList<String>(children.keySet());
		Set<String> childrenAdded = new HashSet<String>();
		int moved = 0;

		for (String childId : keys) {			
			List<StructuralDataChange> childHistory = children.get(childId);

			for (StructuralDataChange event : childHistory) {
				boolean ok = false;
				String parentTaskId = event.getParentTaskId();
				List<StructuralDataChange> parentsEvents = parents.get(parentTaskId);

				if(parentsEvents!=null) {
					boolean parentIsCircle = false;
					if(isCircle(parentsEvents.get(0).getTaskName(), parentsEvents.get(0).getTaskId()))
						parentIsCircle=true;

					if(parentIsCircle) {
						ok = addToParentCircle(event, parentsEvents);
					}

					else { // regular task
						ok = addToParentTask(event, parentsEvents);
					}

					childrenAdded.add(childId);
					if(ok) 
						moved++;
				}
				else {
					System.err.println("task "+childId+": parent id "+parentTaskId+" not found");
				}
			}
		}
		children.keySet().removeAll(childrenAdded);
		return moved;
	}

	private static boolean isCircle(String taskName, String taskId) {
		if(taskName.contains("☯") &&
				!taskId.equals("1183137058154065") &&
				!taskId.equals("1158107169298928") &&
				!taskId.equals("389611570378671"))
			return true;
		return false;
	}

	private static boolean addToParentTask(StructuralDataChange event, List<StructuralDataChange> parentsEvents) {
		int newChange = -999;

		switch (event.getTypeOfChange()) {
		case AsanaActions.IGNORE_OR_DELETE:
			newChange = AsanaActions.IGNORE_OR_DELETE;
			break;
		case AsanaActions.COMMENT:
			newChange = AsanaActions.COMMENT;
			break;
		case AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK:
			newChange = AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK;
			break;
		case AsanaActions.ASSIGN_TO_ACTOR:
			newChange = AsanaActions.ASSIGN_TO_ACTOR;
			break;
		case AsanaActions.UNASSIGN_FROM_ACTOR:
			newChange = AsanaActions.UNASSIGN_FROM_ACTOR;
			break;
		case AsanaActions.ADD_SUB_ROLE:
			newChange = AsanaActions.ADD_SUB_ROLE;
			break;
		case AsanaActions.REMOVE_SUB_ROLE:
			newChange = AsanaActions.REMOVE_SUB_ROLE;
			break;

		default:
			if(!event.getRoleType().equals("accountability/purpose"))
				newChange = AsanaActions.CHANGE_SUB_ROLE;
			break;
		}

		if(event.getRoleType().equals("accountability/purpose") && 
				!event.getMessageType().equals("comment")) {
			newChange = AsanaActions.CHANGE_ACCOUNTABILITY_PURPOSE;
			if(parentsEvents.get(0).getParentTaskId().isEmpty() && 
					event.getGrandChildId()!=null)
				newChange = AsanaActions.CHANGE_SUB_ROLE;
		}

		return addToParent("[EVENT FROM SUB-TASK] ", event, parentsEvents, newChange);

	}

	private static boolean addToParentCircle(StructuralDataChange event, List<StructuralDataChange> parentsEvents) {
		int newChange = -9999;
		switch (event.getTypeOfChange()) {
		case AsanaActions.IGNORE_OR_DELETE:
			newChange = AsanaActions.IGNORE_OR_DELETE;
			break;
		case AsanaActions.COMMENT:
			newChange = AsanaActions.COMMENT;
			break;
		case AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK:
			newChange = AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK;
			break;
		case AsanaActions.ASSIGN_TO_ACTOR:
			newChange = AsanaActions.ASSIGN_TO_ACTOR;
			break;
		case AsanaActions.UNASSIGN_FROM_ACTOR:
			newChange = AsanaActions.UNASSIGN_FROM_ACTOR;
			break;
		case AsanaActions.ADD_SUB_ROLE:
			newChange = AsanaActions.ADD_SUB_ROLE;
			break;
		case AsanaActions.REMOVE_SUB_ROLE:
			newChange = AsanaActions.REMOVE_SUB_ROLE;
			break;

		default:
			newChange = AsanaActions.CIRCLE_CHANGE;
			break;
		}

		if(event.getRoleType().equals("accountability/purpose") && 
				!event.getMessageType().equals("comment")) {
			newChange = AsanaActions.CHANGE_ACCOUNTABILITY_PURPOSE;
			if(parentsEvents.get(0).getParentTaskId().isEmpty() && 
					event.getGrandChildId()!=null)
				newChange = AsanaActions.CIRCLE_CHANGE;
		}

		return addToParent("[EVENT FROM SUB-TASK] ", event, parentsEvents, newChange);
	}

	private static int promoteOrphans(Map<String, List<StructuralDataChange>> allChildrenDynamic,
			Map<String, List<StructuralDataChange>> allParentsDynamic) {
		int promoted = 0;
		List<String> keys = new ArrayList<String>(allChildrenDynamic.keySet());
		List<String> idsAddedToParent = new ArrayList<String>();
		boolean add = false;
		for (String k : keys) {
			//			if(k.equals("8172983842889"))
			//				System.out.println("debug getSubMapWithHierarchy");
			add = false;
			for(StructuralDataChange sdc : allChildrenDynamic.get(k)) {
				if(sdc.getDynamicHierarchy().equals("child") && 
						(sdc.getParentTaskId() == null  || sdc.getParentTaskId().isEmpty())) {
					//promote 
					sdc.setDynamicHierarchy("parent");
					sdc.setParentTaskId("");
					add = true;
				}
			}
			if(add) {
				if(allParentsDynamic.containsKey(k)) {
					allParentsDynamic.get(k).addAll(allChildrenDynamic.get(k));
				}
				else {
					allParentsDynamic.put(k, allChildrenDynamic.get(k));
				}
				idsAddedToParent.add(k);
				promoted += allChildrenDynamic.get(k).size();
			}
		}
		allChildrenDynamic.keySet().removeAll(idsAddedToParent);
		return promoted;
	}

	private static Map<String, List<StructuralDataChange>> getSubMapWithHierarchy(
			Map<String, List<StructuralDataChange>> all, String hierarchy) {
		Map<String, List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>();
		for (String k : all.keySet()) {
			for(StructuralDataChange sdc : all.get(k))
				if(sdc.getDynamicHierarchy().equals(hierarchy)) {
					if(!res.containsKey(k))
						res.put(k, new ArrayList<StructuralDataChange>());
					res.get(k).add(sdc);
				}
		}
		return res;
	}

	private static void dynamicSubRoleToFather(Map<String, List<StructuralDataChange>> allParents,
			Map<String, List<StructuralDataChange>> allChildren) {
		// TODO Auto-generated method stub

	}

	private static void manualFixHierarchy(Map<String, List<StructuralDataChange>> allParents, Map<String, List<StructuralDataChange>> allChildren) {
		String filename = "historyOf-1173980377019858.csv";
		List<String[]> fixed = ReadInfoFromCSV.readAll(filename);
		List<StructuralDataChange> toFix = allChildren.get("1173980377019858");

		List<StructuralDataChange> addToParent = new ArrayList<StructuralDataChange>();

		for (String[] row : fixed) {
			String timestamp = row[0].trim();
			if(row[7].equals("derived"))
				continue;
			for (StructuralDataChange sdc : toFix) {
				if(!sdc.getMessageType().equals("derived") &&
						Timestamp.valueOf(sdc.getStoryCreatedAt()).toString().equals(timestamp)) {
					sdc.setParentTaskId(row[2].trim());
					sdc.setDynamicHierarchy(row[4]);
					sdc.setDynamicParentName(row[5].trim());
					if(row[4].trim().equals("parent")) {
						sdc.setRoleType("role");
						addToParent.add(sdc);
					}
					break;
				}
			}
		}

		for (StructuralDataChange e : addToParent) {
			if(!allParents.containsKey(e.getTaskId())) {
				allParents.put(e.getTaskId(), new ArrayList<StructuralDataChange>());
			}
			allParents.get(e.getTaskId()).add(e);
			allChildren.get(e.getTaskId()).remove(e);
		}

	}

	private static void fillDynamicFather(Map<String, List<StructuralDataChange>> all) {		
		for (String taskId : all.keySet()) {
			List<StructuralDataChange> history = all.get(taskId);
			for (StructuralDataChange sdc : history) {
				if(sdc.getDynamicHierarchy().equals("child")) {
					if(!sdc.getParentTaskId().isEmpty() && 
							sdc.getDynamicParentName()!=null && 
							sdc.getDynamicParentName().isEmpty()) { // they have parent id but dynamic field is empty

						sdc.setDynamicParentName(sdc.getParentTaskName());
					}
				}
			}
		}	
	}

	private static void fixParentNames(Map<String, List<StructuralDataChange>> all, Map<String, String> dictionary) {
		for (String taskId : all.keySet()) {
			for(StructuralDataChange e: all.get(taskId)) {
				if(!dictionary.containsKey(e.getTaskId())) {
					String curNameOfParent = e.getParentTaskName();
					String dictName = dictionary.get(e.getTaskId());
					if(dictName.contains(curNameOfParent))
						e.setParentTaskName(dictName);
					else {
						System.err.println("Parent name different! Check " + e.getTaskId());
					}
				}
				else {
					System.err.println("Parent task unknown. Check "+e.getTaskId());
				}
			}
		}
	}

	/** 
	 * These tasks must be forced to "child"
	 * @return
	 */
	private static Set<String> getWeirdTasks() {
		String path = "auxFiles/weirdTasks.txt";
		Set<String> list = new HashSet<String>();

		try (BufferedReader br = Files.newBufferedReader(Paths.get(path))) {

			//br returns as stream and convert it into a List
			list = br.lines().map(String::trim).collect(Collectors.toSet());

		} catch (IOException e) {
			e.printStackTrace();
		}

		return list;
	}

	private static Set<String> getAllTaskNames(Map<String, List<StructuralDataChange>> all) {
		Set<String> res = new HashSet<String>();
		for (String k : all.keySet()) {
			List<StructuralDataChange> history = all.get(k);
			if(history.size()>0)
				res.add(history.get(0).getTaskName());
		}
		return res;
	}

	private static Set<String> tasksStillSubtaskAfterRemoved(
			Map<String, List<StructuralDataChange>> allChildren, Set<String> allTaskTames) {
		Set<String> changes = new HashSet<String>();
		for(String k : allChildren.keySet()) {
			boolean foundRemovedFrom = false;
			boolean foundAddedSubtask = false;
			int indexOfRemovedFrom = -1;
			List<StructuralDataChange> history = allChildren.get(k);
			List<StructuralDataChange> historyReverse = Lists.reverse(history);
			for(int i=0; i<historyReverse.size() && !foundRemovedFrom; i++) {//look backwards
				StructuralDataChange sdc = historyReverse.get(i);
				if(sdc.getRawDataText().startsWith("removed from")) {
					String[] n = sdc.getRawDataText().split("removed from");
					String name = "";
					if(n.length==2)
						name = sdc.getRawDataText().split("removed from")[1].trim();
					else {
						System.err.println("Problem with "+sdc.getTaskId()+ " "+sdc.getRawDataText());
					}
					if(allTaskTames.contains(name)) {
						foundRemovedFrom = true;
						indexOfRemovedFrom = i;
					}
				}
			}

			for(int i=history.size()-indexOfRemovedFrom; i!=-1 && i<history.size() && !foundAddedSubtask; i++) {
				if(history.get(i).getRawDataText().startsWith("added subtask to task"))
					foundAddedSubtask=true;
			}

			if(foundRemovedFrom && !foundAddedSubtask)
				changes.add(k);
		}
		return changes;
	}

	private static Set<String> getAllTasksAddedAndNotRemoved(Map<String, List<StructuralDataChange>> all) {
		Set<String> res = new HashSet<String>();
		for (String taskId : all.keySet()) {
			List<StructuralDataChange> history = all.get(taskId);
			ListIterator<StructuralDataChange> list = history.listIterator(history.size());
			boolean found = false;
			while (list.hasPrevious() && !found) {
				StructuralDataChange e = list.previous();
				if(e.getRawDataText().startsWith("added subtask to task")) {
					res.add(e.getTaskId());
					found = true;
				}
				else {
					if(e.getRawDataText().startsWith("removed from"))
						found = true;
				}
			}
		}		
		return res;
	}

	private static List<String[]> filterSubsetEvents(List<String[]> subsetEvents) {
		Set<String> seen = new HashSet<String>();
		Set<String> seenFathers = new HashSet<String>();
		Set<String> seenAssignedFather = new HashSet<String>();

		List<String[]> res = new ArrayList<String[]>();
		for (String[] e : subsetEvents) {
			boolean notAssignedFather = false;
			boolean notSeenFatherSet = false;
			boolean notseenId = false;
			if(!seen.contains(e[0])) {
				seen.add(e[0]);
				notseenId = true;
			} 
			if(!seenFathers.contains(e[4])) {
				seenFathers.add(e[4]);
				notSeenFatherSet=true;
			}

			if(!seenAssignedFather.contains(e[2])) {
				seenAssignedFather.add(e[2]);
				notAssignedFather=true;
			}
			if(notseenId || notSeenFatherSet || notAssignedFather)
				res.add(e);
		}
		return res;
	}

	private static List<String[]> addMatchedParents(Map<String, String> dictionary, List<String[]> matchedTasks) {
		Set<String[]> res = new HashSet<String[]>();
		for (String[] task : matchedTasks) {
			String pn = task[3];
			List<String> allPossibleParents = reverseLookUp2(pn, dictionary);
			res.add(new String[] {task[0], task[1], task[2], task[3], allPossibleParents.toString()});
		}
		return new ArrayList<String[]>(res);
	}

	private static List<String> getAllTasksWhoChangedFather(Map<String, List<StructuralDataChange>> all, List<String> projects) {
		Set<String> res = new HashSet<String>();

		//		try {
		//			PrintWriter writer = new PrintWriter(new FileWriter("projs.txt"));
		//			for (String p : projects) {
		//				writer.write(p);
		//			}
		//			writer.flush();
		//			writer.close();
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		}

		for (String taskId : all.keySet()) {
			List<StructuralDataChange> history = all.get(taskId);

			if(moreThanOneFather(history))
				res.add(taskId);

			if(addedRemovedFromSameParentTask(history)) {
				res.add(taskId);
			}

			if(duplicatedRemovedFrom(history, projects)) {
				res.add(taskId);
			}
		}
		return new ArrayList<String>(res);
	}

	private static List<String> getAllProjectNames(String filename) {
		//		int columnNR = 1;
		//		List<String> res = ReadInfoFromCSV.getColumn(columnNR,filename);
		List<String> res = GeneralUtils.readFromTextFile(filename);
		System.out.println("Got "+res.size() + " project names");
		return res;
	}

	private static boolean duplicatedRemovedFrom(List<StructuralDataChange> history, List<String> projects) {
		// TODO Auto-generated method stub
		// duplicated task from -> removed from NOT PROJECT
		boolean dupFound = false;
		for (StructuralDataChange change : history) {
			if(change.getRawDataText().startsWith("duplicated task from"))
				dupFound = true;

			if(dupFound && 
					change.getRawDataText().startsWith("removed from") && 
					removedFromNotProject(change.getRawDataText(), projects))
				return true;
		}
		return false;
	}

	private static boolean removedFromNotProject(String rawDataText, List<String> projects) {
		String name = rawDataText.split("removed from")[1].trim();	
		for (String p : projects) {
			if(name.equals(p))
				return false;
		}
		return true;
	}

	private static boolean addedRemovedFromSameParentTask(List<StructuralDataChange> history) {
		// added subtask to task PARENT -> removed from PARENT
		String parent = "";
		for(StructuralDataChange e : history) {
			if(e.getRawDataText().startsWith("added subtask to task")) {
				String[] tokens = e.getRawDataText().split("added subtask to task");
				if(tokens.length>1)
					parent=tokens[1].trim();				
			}

			if(e.getRawDataText().startsWith("removed from")) {
				String tname = e.getRawDataText().split("removed from")[1].trim();
				if(tname.equals(parent))
					return true;
			}
		}
		return false;
	}

	//	private static List<String[]> getAllIdsWithMatchedParentTask(Map<String, List<StructuralDataChange>> all,
	//			Map<String, String> moreParents) {
	//		Map<String, List<StructuralDataChange>> res = new HashMap<String, List<StructuralDataChange>>();
	//		Set<String> tasksWithMoreThanOneFather = new HashSet<String>();
	//
	//		Set<String[]> res2 = new HashSet<String[]>();
	//
	//		Set<String> keys = all.keySet();
	//		
	////		filter all those that were matched
	//		for (String k : keys) {
	//			List<StructuralDataChange> changes = all.get(k);
	//			if(wasMatched(changes)) {
	//				res.put(k, changes);
	//			}
	//		}
	//		
	//		//filter for the ones who have had more than one father		
	//		for (String k : keys) {
	//			List<StructuralDataChange> changes = all.get(k);
	//			if(moreThanOneFather(changes)) {
	//				tasksWithMoreThanOneFather.add(k);
	//			}
	//		}
	//		
	//		// now we can further filter
	//		for (String k : tasksWithMoreThanOneFather) {
	//			List<StructuralDataChange> changes = res.get(k);
	//			if(changes!=null && changes.size()>0)
	//			for (StructuralDataChange e : changes) {
	//				String[] row = null;
	//				if(e.getParentTaskId().startsWith("*") && moreParents.containsKey(e.getDynamicParentName().trim())) {
	//					row = new String[] {e.getTaskId(), e.getTaskName(), e.getParentTaskId().substring(1), e.getDynamicParentName(), moreParents.get(e.getDynamicParentName().trim())};
	//					res2.add(row);
	//				}
	//			}
	//		}		
	//		
	//		return new ArrayList<String[]>(res2);
	//	}

	private static boolean moreThanOneFather(List<StructuralDataChange> changes) {
		int added = 0;

		Set<StructuralDataChange> theChanges = getUniqueTaskIdTimestamp(changes); //to eliminate duplicates

		for (StructuralDataChange change : theChanges) {
			if(change.getRawDataText().startsWith("added subtask to task"))
				added++;
		}
		if(added>=2)
			return true;

		return false;
	}

	private static Set<StructuralDataChange> getUniqueTaskIdTimestamp(List<StructuralDataChange> changes) {
		Set<LocalDateTime> seenTss = new HashSet<LocalDateTime>();
		Set<StructuralDataChange> res = new HashSet<StructuralDataChange>();

		for (StructuralDataChange e : changes) {
			if(e.getMessageType().equals("derived"))
				continue;
			if(!seenTss.contains(e.getStoryCreatedAt())) {
				res.add(e);
				seenTss.add(e.getStoryCreatedAt());
			}
		}		
		return res;
	}



	//	private static List<String> getDifferentParents(List<StructuralDataChange> changes) {
	//		List<String> parentsMatched = new ArrayList<String>();
	//		for (StructuralDataChange sdc : changes) {
	//			if(sdc.getParentTaskId().startsWith("*") && !parentsMatched.contains(sdc.getParentTaskId()))
	//				parentsMatched.add(sdc.getParentTaskId());
	//		}
	//		return parentsMatched;
	//	}

	//	private static boolean wasMatched(List<StructuralDataChange> changes) {
	//		for (StructuralDataChange structuralDataChange : changes) {
	//			if(structuralDataChange.getParentTaskId().startsWith("*"))
	//				return true;
	//		}
	//		return false;
	//	}

	private static Map<String, String> getTasksWithMoreFathers(Map<String, String> taskIdNameMap,
			Map<String, String> matched) {
		Map<String, String> nameToIds = new HashMap<String, String>();
		for (Entry<String,String> entry : matched.entrySet()) {
			List<String> matches = reverseLookUp2(entry.getValue(), taskIdNameMap);
			if(matches.size()>1)
				nameToIds.put(entry.getValue(), matches.toString());
		}
		return nameToIds;
	}

	private static Map<String, String> getTasksWithMoreFathers2(Map<String, String> taskIdNameMap,
			Map<String, String> matched) {
		Map<String, String> nameToIds = new HashMap<String, String>();
		Set<String> keys = nameToIds.keySet();

		for (Entry<String,String> entry : matched.entrySet()) {
			List<String> matches = reverseLookUp2(entry.getValue(), taskIdNameMap);
			if(matches.size()>1)
				nameToIds.put(entry.getValue(), matches.toString());
		}
		return nameToIds;
	}

	private static boolean wasEverFather(String id, Map<String, List<StructuralDataChange>> all) {
		List<StructuralDataChange> history = all.get(id);
		if(history==null)
			return false;
		for (StructuralDataChange e : history) {
			if(e.getDynamicHierarchy().equals("parent"))
				return true;
		}
		return false;
	}


	private static void fixRoleIntegrationExtraction(Map<String, List<StructuralDataChange>> all) {
		Set<String> keys = all.keySet();
		for (String k : keys) {
			List<StructuralDataChange> values = all.get(k);
			boolean parent = false;
			boolean child = false;

			for (StructuralDataChange sdc : values) {				
				if(sdc.getDynamicHierarchy().equals("parent")){
					parent = true;
					if(child) { // it went from child to parent
						if(sdc.getTypeOfChange() == AsanaActions.ADD_TO_CIRCLE) { // it is in a circle
							sdc.setTypeOfChange(AsanaActions.ROLE_EXTRACTION);
							sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.ROLE_EXTRACTION));
							child = false;
							parent=false;
						}
					}
				}

				if(sdc.getDynamicHierarchy().equals("child")) {
					child = true;
					if(parent) { // it was a parent before (at some point)
						if(sdc.getTypeOfChange() == AsanaActions.REMOVE_FROM_CIRCLE) { // it is in a circle
							sdc.setTypeOfChange(AsanaActions.ROLE_INTEGRATION);
							sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.ROLE_INTEGRATION));
							parent = false;
							child = false;
						}
					}
				}
			}
		}
	}


	private static Map<String, String> assignDynamicFather(Map<String, List<StructuralDataChange>> all, Map<String, String> taskIdNameMap, List<String> allTasksWhoChangedFather, List<String[]> matchedTasks, Set<String> allTasksWhoseFatherIsLast, Set<String> forceToChild) {
		Map<String, String> matched = new HashMap<String, String>();

		//		Set<String> exactlyOnce = getOccurenceExactly("added subtask to task", 1, all);
		////		add condition that it must have an asana parent task id
		//		Set<String> doNotChange = haveAsanaParentId(exactlyOnce,all);

		List<String> rest = new ArrayList<String>(all.keySet());
		allTasksWhoChangedFather.removeAll(allTasksWhoseFatherIsLast);
		rest.retainAll(allTasksWhoChangedFather); 

		for (String k : rest) {
			
			if(!forceToChild.contains(k)) {
				for (StructuralDataChange e : all.get(k)) {
					if(e.getDynamicHierarchy().equals("child")) {
						String lookedUpID = reverseLookUp(e.getDynamicParentName(),taskIdNameMap);
						// here we have to mark it as orphan
						if(lookedUpID.isEmpty()) {
							if(e.getDynamicParentName()!=null && 
									e.getRawDataText().contains(e.getDynamicParentName())) {
								setChange(e, AsanaActions.IGNORE_EVENT);
								setChangeNew(e, AsanaActions.IGNORE_EVENT);
							}
							e.setRawDataText(String.join(" ", "[EVENT FROM ORPHAN]",e.getRawDataText()));
						}
						e.setParentTaskId(lookedUpID);
						matched.put(lookedUpID,e.getDynamicParentName());
						matchedTasks.add(new String[] {e.getTaskId(), e.getTaskName(),lookedUpID,e.getDynamicParentName()});
					}
				}
			}
		}
		return matched;
	}

	private static Set<String> haveAsanaParentId(Set<String> exactlyOnce, Map<String, List<StructuralDataChange>> all) {
		Set<String> res = new HashSet<String>();
		for (String k : exactlyOnce) {
			for(StructuralDataChange e : all.get(k)) {
				if(e.getParentTaskId()!=null && !e.getParentTaskId().isEmpty()) {
					res.add(k);
					break;
				}
			}
		}
		return res;
	}

	private static Set<String> getOccurenceExactly(String string, int times, Map<String, List<StructuralDataChange>> all) {
		Set<String> res = new HashSet<String>();
		for (String k : all.keySet()) {
			int f = 0;
			for (StructuralDataChange e : all.get(k)) {
				if(e.getRawDataText().startsWith(string))
					f++;
			}
			if(f==times)
				res.add(k);
		}
		return res;
	}

	private static String reverseLookUp(String dynamicParentName, Map<String, String> taskIdNameMap) {
		String res = "";
		for (String k : taskIdNameMap.keySet()) {
			if(taskIdNameMap.get(k).equals(dynamicParentName))
				res=k;
		}
		return res;
	}

	private static List<String> reverseLookUp2(String dynamicParentName, Map<String, String> taskIdNameMap) {
		List<String> res = new ArrayList<String>();
		for (String k : taskIdNameMap.keySet()) {
			if(taskIdNameMap.get(k).equals(dynamicParentName))
				res.add(k);
		}
		return res;
	}

	private static Map<String, String> getAllTaskIdName(Map<String, List<StructuralDataChange>> all) {
		Map<String, String> res = new HashMap<String, String>();
		for (String tId : all.keySet()) {
			if(all.get(tId)!=null && all.get(tId).size()>0)
				res.put(tId, all.get(tId).get(0).getTaskName().trim());			
		}
		return res;
	}

	private static Map<String, String> getAllParentTaskIdName(Map<String, List<StructuralDataChange>> all) {
		Map<String, String> res = new HashMap<String, String>();
		for (String tId : all.keySet()) {
			if(all.get(tId)!=null && all.get(tId).size()>0 && !all.get(tId).get(0).getParentTaskId().isEmpty())
				res.put(all.get(tId).get(0).getParentTaskId(), all.get(tId).get(0).getParentTaskName());			
		}
		return res;
	}

	private static Map<String, List<StructuralDataChange>> getAllDuplicatedTasks(
			Map<String, List<StructuralDataChange>> all) {

		Map<String, List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>();

		for(String taskId : all.keySet()) {
			List<StructuralDataChange> history = all.get(taskId);
			boolean hasDup = history.stream().anyMatch(e-> e.getRawDataText().startsWith("duplicated task from"));
			if(hasDup)
				res.put(taskId, history);
		}
		return res;
	}

	private static void setDynamicHierarchyDuplicated(Map<String, List<StructuralDataChange>> all, Set<String> forceToChild, List<String> projects) {
		Map<String, List<StructuralDataChange>> allDup = getAllDuplicatedTasks(all); //getAllDuplicatedRemovedAddToCircle(all);
		Map<String, List<StructuralDataChange>> allNoChange = getAllNoChange(allDup);
		allNoChange.entrySet().stream().forEach(e -> e.getValue().forEach(v -> v.setDynamicHierarchy("child")));

		Set<String> duplicatedAsParent = getAllDupAsParent(allDup);

		Map<String, List<StructuralDataChange>> allDupChange = new LinkedHashMap<String, List<StructuralDataChange>>();
		for (String k : allDup.keySet()) {	
			if(!allNoChange.containsKey(k) && !duplicatedAsParent.contains(k)) {
				allDupChange.put(k, allDup.get(k));
			}
		}

		for (String k : allDupChange.keySet()) {			
			String hierarchy = "child";
			String parentName = "";
			String lastParent = "NO PARENT";
			boolean parentDerived = false;
			boolean everAdded = false;

			for(StructuralDataChange sdc: allDupChange.get(k)) {

				if(forceToChild.contains(k)) {
					hierarchy = "child";
					parentName = sdc.getParentTaskName();
				}

				else if(sdc.getRawDataText().startsWith("removed from") && 
						sdc.getTypeOfChange()!=AsanaActions.REMOVE_FROM_CIRCLE &&
						!projects.contains(stripProjectName(sdc.getRawDataText()))) {
					if(sdc.getRawDataText().contains(lastParent) || !everAdded) {
						hierarchy = "parent"; 
						parentName="";
					}					
				}
				else if(sdc.getRawDataText().startsWith("added subtask to task")) {
					hierarchy = "child";
					String[] pn = sdc.getRawDataText().split("added subtask to task");
					String name = "";
					if(pn.length>1){
						name=pn[1];
					}
					parentName=name.trim();
					lastParent = "" + parentName;
					parentDerived = true;
					everAdded = true;
				}

				sdc.setDynamicHierarchy(hierarchy);

				if(hierarchy.equals("child")) {
					if(!parentDerived)
						parentName = sdc.getParentTaskName();
					sdc.setDynamicParentName(parentName);
				}	
			}
		}
	}


	private static Set<String> getAllDupAsParent(Map<String, List<StructuralDataChange>> allDup) {
		Set<String> res = new HashSet<String>();
		for (String tId : allDup.keySet()) {
			List<StructuralDataChange> changes = allDup.get(tId);
			if(changes.get(0).getParentTaskId().isEmpty())
				res.add(tId);
		}
		return res;
	}

	private static Map<String, List<StructuralDataChange>> getAllNoChange(
			Map<String, List<StructuralDataChange>> allDup) {
		Map<String, List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>();

		for(String taskId : allDup.keySet()) {
			List<StructuralDataChange> history = allDup.get(taskId);
			boolean nodup = false;
			boolean noaddedsubtasktotask = false;
			for (StructuralDataChange e : history) {
				if(e.getRawDataText().startsWith("duplicated task from"))
					nodup = true;
				if(e.getRawDataText().startsWith("added subtask to task"))
					noaddedsubtasktotask = true;
			}

			if(nodup && noaddedsubtasktotask) {
				res.put(taskId, history);
			}
		}
		return res;

	}

	private static Map<String,List<StructuralDataChange>> getAllDuplicatedRemovedAddToCircle(Map<String, List<StructuralDataChange>> all) {
		Map<String,List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>();
		List<String> ids = getAllTaskIdsStartingWith("duplicated task", all); 
		for (String k : ids) {
			List<StructuralDataChange> history = all.get(k);
			int removed = 0;
			int addedToCircle = 0;
			for (StructuralDataChange event : history) {
				//				if(event.getTypeOfChange()==AsanaActions.ADD_TO_CIRCLE)
				//					addedToCircle++;
				if(event.getTypeOfChange()==AsanaActions.REMOVE_FROM_CIRCLE)
					removed++;
			}
			if(addedToCircle>0 && removed>0)
				res.put(k,history);
		}
		return res;
	}

	private static Map<String,List<StructuralDataChange>> getAllDuplicatedAddSubTask(Map<String, List<StructuralDataChange>> all) {
		Map<String,List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>();
		List<String> ids = getAllTaskIdsStartingWith("duplicated task", all);
		boolean add = false;
		for (String k : ids) {
			List<StructuralDataChange> history = all.get(k);
			add = false;
			for (StructuralDataChange event : history) {
				if(event.getRawDataText().startsWith("added subtask to task"))
					add = true;
			}
			if(add)
				res.put(k, history);
		}
		return res;
	}

	private static List<String> getAllTaskIdsStartingWith(String string, Map<String, List<StructuralDataChange>> all) {
		List<String> res = new ArrayList<String>();
		Set<String> ids = all.keySet();
		for (String k : ids) {
			List<StructuralDataChange> changes = all.get(k);
			StructuralDataChange first = null;
			for (StructuralDataChange sdc : changes) {
				first = sdc;
				if(!sdc.getMessageType().equals("derived"))
					break;
			}
			if(first!=null && first.getRawDataText().startsWith(string))
				res.add(k);
		}
		return res;
	}

	//	private static void fillDownDynamicHierarchy(Map<String, List<StructuralDataChange>> all) {
	//		for (String id : all.keySet()) {
	//			String current="";
	//			for (StructuralDataChange sdc : all.get(id)) {
	//				if(sdc.getDynamicHierarchy()!=null && !sdc.getDynamicHierarchy().isEmpty()) {
	//					current=""+sdc.getDynamicHierarchy();
	//				}
	//				else {
	//					sdc.setDynamicHierarchy(current);
	//				}
	//			}
	//		}
	//	}

	private static void setDynamicHierarchy(Map<String, List<StructuralDataChange>> all, Set<String> allOrphanIds, Set<String> forceToChild, Set<String> tasksNeverAddedOrRemoved) {
		Set<String> keySet = all.keySet();
		for (String k : keySet) {
			List<StructuralDataChange> events = all.get(k);
			String hierarchy = "parent";
			String parentName = "";
			String lastParent = "NO PARENT";
			for (StructuralDataChange e : events) {
				String rawDataText = e.getRawDataText().trim();
				
				if(forceToChild.contains(k) || tasksNeverAddedOrRemoved.contains(k)) {
					hierarchy = "child";
					parentName = e.getParentTaskName();
				}

				else if(rawDataText.startsWith("added subtask to task") && 
						!allOrphanIds.contains(e.getTaskId())) { // does not apply to orphan	
					hierarchy = "child";
					String[] pn = rawDataText.split("added subtask to task");
					String name = "";
					if(pn.length>1){
						name=pn[1];
					}
					parentName = name.trim();
					lastParent = ""+parentName;
				}
				else if(rawDataText.startsWith("removed from") && 
						e.getTypeOfChange()!=AsanaActions.REMOVE_FROM_CIRCLE) {
					if(rawDataText.contains(lastParent)) { //it has to be removed from the last parent
						hierarchy = "parent";
						parentName = "";
					}
				}

				if(hierarchy.equals("child")) {
					e.setDynamicParentName(parentName);
				}
				e.setDynamicHierarchy(hierarchy);
			}
		}		
	}

	private static void getAllWithCodeMoreThanOnce(Map<String, List<StructuralDataChange>> all) {
		Map<String, List<StructuralDataChange>> tsks = allTasksWithCodeMoreThanOnce(all, AsanaActions.ADD_SUB_ROLE, 7777777);

		WriteUtils.writeMapOfChangesWithCircleToCSV(tsks, "add-sub-role-more-than-1-before-moving-to-father.csv");

		tsks = allTasksWithCodeMoreThanOnce(all, AsanaActions.ADD_SUB_ROLE, AsanaActions.ADD_TO_CIRCLE);

		WriteUtils.writeMapOfChangesWithCircleToCSV(tsks, "either-add-sub-role-add-to-circle-more-than-1-before-moving-to-father.csv");

		tsks = allTasksWithBothCodes(all, AsanaActions.ADD_SUB_ROLE, AsanaActions.ADD_TO_CIRCLE);
		WriteUtils.writeMapOfChangesWithCircleToCSV(tsks, "both-add-sub-role-and-add-to-circle-at-least-1-before-moving-to-father.csv");
	}

	private static Map<String, List<StructuralDataChange>> allTasksWithBothCodes(
			Map<String, List<StructuralDataChange>> all, int addSubRole, int addToCircle) {
		Map<String, List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>();

		Map<String, List<StructuralDataChange>> temp = new LinkedHashMap<String, List<StructuralDataChange>>();

		temp.putAll(all);

		Map<String, List<StructuralDataChange>> temp2 = removeDuplicateTasks(temp);

		for (String task : temp2.keySet()) {
			int found1 = 0;
			int found2 = 0;
			List<StructuralDataChange> history = temp2.get(task);
			for (StructuralDataChange event : history) {
				if(event.getTypeOfChange()==addSubRole)
					found1++;
				if(event.getTypeOfChange()==addToCircle)
					found2++;

			}
			if(found1>1 && found2>1) {
				List<StructuralDataChange> hist = new ArrayList<StructuralDataChange>();
				//				if(res.containsKey(task)) {
				//					hist.addAll(res.get(task));
				//				}
				hist.addAll(history);
				res.put(task, hist);
			}
		}
		return res;
	}

	private static Map<String, List<StructuralDataChange>> allTasksWithCodeMoreThanOnce(
			Map<String, List<StructuralDataChange>> allEvents, int code1, int code2) {
		Map<String, List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>();
		Map<String, List<StructuralDataChange>> temp = new LinkedHashMap<String, List<StructuralDataChange>>();

		temp.putAll(allEvents);

		Map<String, List<StructuralDataChange>> temp2 = removeDuplicateTasks(temp);


		for (String task : temp2.keySet()) {
			int found = 0;
			Set<StructuralDataChange> history = new HashSet<StructuralDataChange>(temp2.get(task));

			for (StructuralDataChange event : history) {
				if(event.getTypeOfChange()==code1 || event.getTypeOfChange()==code2)
					found++;
			}
			if(found>1 && !res.containsKey(task))
				res.put(task, new ArrayList<StructuralDataChange>(history));
		}
		return res;
	} 

	private static void fixDowngradedRoles(Map<String, List<StructuralDataChange>> allParents,
			Map<String, List<StructuralDataChange>> allChildren, List<String> downgradedRoles) {

		for (String childId : downgradedRoles) {

			List<StructuralDataChange> childHistory = allChildren.get(childId);
			//			List<StructuralDataChange> extractionEvents = getExtractionEvents(childHistory);
			//			List<StructuralDataChange> integrationEvents = getIntegrationEvents(childHistory, allParents);
			//			Collections.sort(extractionEvents);
			//			ColfixOrphanslections.sort(integrationEvents);
			//			
			//			StructuralDataChange firstExtracted = extractionEvents.get(0);
			//			StructuralDataChange firstIntegrated = integrationEvents.get(0);
			//			
			int last = 0; // -1 extraction, 1 integration
			//			
			//			
			for (StructuralDataChange event : childHistory) {

				if(event.getTypeOfChange()==AsanaActions.ADD_SUB_ROLE) { // then it is role integration
					last = 1;
					event.setTypeOfChange(AsanaActions.ROLE_INTEGRATION);
					event.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.ROLE_INTEGRATION));
				}
				if(event.getTypeOfChange()==AsanaActions.ADD_TO_CIRCLE) { // then it is role extraction
					last = -1;
					event.setTypeOfChange(AsanaActions.ROLE_EXTRACTION);
					event.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.ROLE_EXTRACTION));
				}

				//				if(last == -1) {
				//					event.setParentTaskId("");
				//					event.setParentTaskName("");
				//				}
			}		
		}
	}


	private static List<StructuralDataChange> getIntegrationEvents(List<StructuralDataChange> history, Map<String, List<StructuralDataChange>> allParents) {
		List<StructuralDataChange> res = new ArrayList<StructuralDataChange>();
		for(StructuralDataChange sdc : history) {
			if(sdc.getTypeOfChange()==AsanaActions.ADD_SUB_ROLE && allParents.containsKey(sdc.getParentTaskId()))
				res.add(sdc);
		}
		return res;
	}


	private static List<StructuralDataChange> getExtractionEvents(List<StructuralDataChange> history) {
		List<StructuralDataChange> res = new ArrayList<StructuralDataChange>();
		for(StructuralDataChange sdc : history) {
			if(sdc.getTypeOfChange()==AsanaActions.ADD_TO_CIRCLE)
				res.add(sdc);
		}
		return res;
	}


	private static Set<String> fixOrphans(Map<String, List<StructuralDataChange>> allParents,
			Map<String, List<StructuralDataChange>> allChildren) {
		List<StructuralDataChange> orphans = new ArrayList<StructuralDataChange>();
		Set<String> allOrphanIds = new HashSet<String>(); 

		for(String k: allChildren.keySet()) {
			StructuralDataChange child = allChildren.get(k).get(0);

			if(allChildren.containsKey(child.getParentTaskId())) // it is a grandchild
				continue;

			if(!allParents.containsKey(child.getParentTaskId())) {
				orphans.add(child);
			}

		}

		System.out.println("Found "+orphans.size()+ " orphans");

		for (StructuralDataChange orphan : orphans) { // transfer
			List<StructuralDataChange> history = allChildren.remove(orphan.getTaskId());
			for (StructuralDataChange sdc : history) {
				sdc.setParentTaskId("");
				sdc.setParentTaskName("");
			}
			allParents.put(orphan.getTaskId(), history);
			allOrphanIds.add(orphan.getTaskId());
		}
		return allOrphanIds;
	}

	private static List<String> getIdsOfChildrenOlderThanFather(Map<String, List<StructuralDataChange>> allParents,
			Map<String, List<StructuralDataChange>> allChildren) {

		List<String> problematicKids = new ArrayList<String>();

		for(String k: allChildren.keySet()) {
			StructuralDataChange child = allChildren.get(k).get(0);
			if(allChildren.containsKey(child.getParentTaskId()))
				continue;

			if(!allParents.containsKey(child.getParentTaskId())) {
				System.out.println("Could not find parent of this task: "+child.getTaskId()+ " "+child.getTaskName());
				continue;
			}

			StructuralDataChange father = allParents.get(child.getParentTaskId()).get(0);

			if(wasEverAssigned(allChildren.get(child.getTaskId())) && 
					child.getStoryCreatedAt().isBefore(father.getStoryCreatedAt())) {
				problematicKids.add(child.getTaskId());
			}
		}

		//		WriteUtils.writeListOfChangesWithCircleToCSV(problematicKids, "/home/saimir/Downloads/badkids.csv");
		return problematicKids;
	}

	private static void setRoleType(Map<String, List<StructuralDataChange>> allEvents2) {
		Set<String> keys = allEvents2.keySet();
		for (String taskId : keys) {

			List<StructuralDataChange> taskHistory = allEvents2.get(taskId);
			//			if(taskId.equals("201798023170258"))
			//				System.out.println("Debug setRoleType");
			for (StructuralDataChange sdc : taskHistory) {
				if(sdc.getParentTaskId().isEmpty()) { // then it is a role
					sdc.setRoleType("role");
				}
				else { // it is a sub-task
					if(wasEverAssigned(taskHistory))
						sdc.setRoleType("sub-role");
					else {
						sdc.setRoleType("accountability/purpose");
						sdc.setChangeAccountabilityPurpose(true);
						int toc = AsanaActions.CHANGE_ACCOUNTABILITY_PURPOSE;
						if(sdc.getMessageType().equals("comment"))
							toc = AsanaActions.COMMENT;
						else if(sdc.getTypeOfChange() == AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK)
							toc = AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK;
						else if(sdc.getTypeOfChange() == AsanaActions.IGNORE_OR_DELETE)
							toc = AsanaActions.IGNORE_OR_DELETE;
						else if(sdc.getTypeOfChange() == AsanaActions.CREATE_ROLE) // it should be the earliest event in the history
							sdc.setRawDataText("created this task");
						sdc.setTypeOfChange(toc);
						sdc.setTypeOfChangeDescription(AsanaActions.codeToString(toc));
					}
				}
			}
		}
	}

	private static boolean wasEverAssigned(List<StructuralDataChange> taskHistory) {
		for (StructuralDataChange sdc : taskHistory) {
			if(sdc.getTypeOfChange()==AsanaActions.ASSIGN_TO_ACTOR || sdc.getTypeOfChange()==AsanaActions.UNASSIGN_FROM_ACTOR)
				return true;
		}
		return false;
	}

	private static void includeManuallyAnnotatedCreatedByName(Map<String, List<StructuralDataChange>> allEvents) {
		// read list of userId, userName
		String usersFile = "/home/saimir/ownCloud/PhD/Collaborations/Waldemar/Springest/Data/"
				+ "Data Extracted from DB/extracted20201127-only-smooth-ops/Saimir-Manual-Annotation/"
				+ "all-users-for-createdByName.csv";

		List<String[]> users = readUsersFile(usersFile);

		Set<String> tasks = allEvents.keySet();
		for (String task : tasks) {
			List<StructuralDataChange> events = allEvents.get(task);
			for (StructuralDataChange sdc : events) {
				String cbi = sdc.getStoryCreatedById();
				String name = lookUpName(cbi, users);
				if(name!=null)
					sdc.setStoryCreatedByName(name);
			}
		}
	}

	private static String lookUpName(String cbi, List<String[]> users) {
		for (String[] user : users) {
			String id = user[0].trim();
			String name = user[1].trim();
			if(cbi.equals(id))
				return name;
		}
		return null;
	}

	private static List<String[]> readUsersFile(String usersFile) {
		List<String[]> res = null;
		try {
			CSVReader reader = new CSVReader(new FileReader(usersFile));
			reader.readNext();
			res = reader.readAll();
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CsvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}

	private static void checkEffectiveness(Map<String, List<StructuralDataChange>> allEvents) {
		List<StructuralDataChange> events = allEvents.get("99912941734884");
		for (StructuralDataChange sdc : events) {
			DateTimeFormatter formatter = new DateTimeFormatterBuilder()
					.appendPattern("yyyy-MM-dd HH:mm:ss")
					.appendFraction(ChronoField.MILLI_OF_SECOND, 1, 3, true) // min 2 max 3
					.toFormatter();
			LocalDateTime rowTime = LocalDateTime.parse("2016-04-06 11:37:21.032", formatter);
			if(sdc.getStoryCreatedAt().equals(rowTime)) {
				System.out.println("Current assignee = "+sdc.getCurrentAssignee());
			}
		}

	}

	private static List<StructuralDataChange> integrateManuallyAnnotatedCurrentAssignee(Map<String, List<StructuralDataChange>> allEvents, String fileName, boolean converToUTC) {
		List<StructuralDataChange> changed = new ArrayList<StructuralDataChange>();
		CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(fileName));
			reader.readNext();
			List<String[]> rows = reader.readAll();
			//			System.out.println("Read "+reader.getLinesRead()+" lines.");
			reader.close();
			int found = 0;
			for (String[] row : rows) {

				String taskID = row[1].trim();
				List<StructuralDataChange> eventsOfTask = allEvents.get(taskID);
				if(eventsOfTask==null)
					continue;
				for (StructuralDataChange structuralDataChange : eventsOfTask) {
					//					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS[S]");
					DateTimeFormatter formatter = new DateTimeFormatterBuilder()
							.appendPattern("yyyy-MM-dd HH:mm:ss")
							.appendFraction(ChronoField.MILLI_OF_SECOND, 1, 3, true) // min 2 max 3
							.toFormatter();
					LocalDateTime rowTime = LocalDateTime.parse(row[0], formatter);

					boolean same = false;

					ZonedDateTime ldtZoned = rowTime.atZone(ZoneId.systemDefault());
					ZonedDateTime utcZoned = ldtZoned.withZoneSameInstant(ZoneId.of("UTC"));

					if(converToUTC) {
						same = structuralDataChange.getStoryCreatedAt().equals(utcZoned.toLocalDateTime());
						//						if(structuralDataChange.getStoryCreatedAt().toString().contains("54:29.038"))
						//							System.out.println("debug integrateManuallyAnnotatedCurrentAssignee");
					}
					else {
						same = structuralDataChange.getStoryCreatedAt().equals(rowTime);
					}

					if(same) {
						found++;
						structuralDataChange.setCurrentAssignee(""+row[14]);
						changed.add(structuralDataChange);
						//						System.out.println("Found " + rowTime + " "+ row[1] + " " + row[3] + " " +
						//						structuralDataChange.getTaskId() + " "+ 
						//						structuralDataChange.getTaskName() + " " + structuralDataChange.getStoryCreatedAt());
					}
				}
			}
			System.out.println("Integrated "+found+ " manual annotations of the current assignee.");		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CsvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return changed;
	}


	private static List<StructuralDataChange> manuallySetCode20(List<StructuralDataChange> uniqueEvents, String fileName, Map<String, String> revDict) {
		List<StructuralDataChange> changed = new ArrayList<StructuralDataChange>();
		CSVReader reader;
		List<String[]> rows = null;

		try {
			reader = new CSVReader(new FileReader(fileName));
			reader.readNext();
			rows = reader.readAll();
			//			System.out.println("Read "+reader.getLinesRead()+" lines.");
			reader.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CsvException e) {
			e.printStackTrace();
		}

		List<String> timestamps = rows.stream().map(e -> e[0].trim()).collect(Collectors.toList());


		List<StructuralDataChange> subsetEvents = uniqueEvents.stream().filter(
				e -> timestamps.contains(Timestamp.valueOf(e.getStoryCreatedAt()).toString()))
				.collect(Collectors.toList());

		int found = subsetEvents.size();
		for (StructuralDataChange sdc : subsetEvents) {

			String timestamp = Timestamp.valueOf(sdc.getStoryCreatedAt()).toString();

			for(String[] row : rows) {
				if(row[0].trim().equals(timestamp)) {
					String name = row[10].trim();
					sdc.setStoryCreatedByName(name);
					sdc.setStoryCreatedById(revDict.get(name));
				}
			}
		}

		System.out.println("Manually set "+found+ " code 20 entries.");		

		return changed;
	}

	private static void addSecondDegreeCircle(List<StructuralDataChange> uniqueEvents, String fileName) {
		Map<String, List<CircleTimeRange>> map = ReadInfoFromCSV.readParentLocations(fileName);
		for (StructuralDataChange sdc : uniqueEvents) {
			String[] circleIds = sdc.getCircleIds().split(",");
			String secondDegreeIds = "";
			String secondDegreeNames = "";
			for (String circleId : circleIds) {
				if(circleId.equals(""))
					break;
				LocalDateTime time = sdc.getStoryCreatedAt();
				List<CircleTimeRange> circleTimeRanges = map.get(circleId);
				if(circleTimeRanges != null) {
					//					if(circleId.equals("11626921109046") && time.isEqual(LocalDateTime.parse("2015-08-31T08:30:20.204"))) {
					//						System.err.println("butta!");
					//					}
					String cid = getParentCircleAtTime(time,circleTimeRanges);
					if(cid == null) {
						//						System.err.println("butta!");
						secondDegreeIds = "";
						secondDegreeNames = "";
					}
					else {
						secondDegreeIds += ","+cid+"_"+circleId;
						secondDegreeNames += ","+AuthoritativeList.lookupId(cid)+"_"+AuthoritativeList.lookupId(circleId);
					}
				}				
			}
			sdc.setSecondDegreeCircleRelationshipId(secondDegreeIds.startsWith(",")?secondDegreeIds.substring(1):secondDegreeIds);
			sdc.setSecondDegreeCircleRelationshipName(secondDegreeNames.startsWith(",")?secondDegreeNames.substring(1):secondDegreeNames);
		}
	}

	private static String getParentCircleAtTime(LocalDateTime time, List<CircleTimeRange> circleTimeRanges) {
		for (CircleTimeRange circleTimeRange : circleTimeRanges) {
			if(!time.toLocalDate().isBefore(circleTimeRange.getStart().toLocalDate()) && 
					(circleTimeRange.getEnd() == null || !time.toLocalDate().isAfter(circleTimeRange.getEnd().toLocalDate())))
				return circleTimeRange.getCircleId();
		}
		return null;
	}

	private static void fixUnassigned(Map<String, List<StructuralDataChange>> allEvents) {
		for (String k : allEvents.keySet()) {
			boolean unassigned = false;
			for (StructuralDataChange change : allEvents.get(k)) {

				if(change.getTypeOfChange() == AsanaActions.ASSIGN_TO_ACTOR) {
					unassigned = false;
				}

				if(unassigned || change.getTypeOfChange() == AsanaActions.UNASSIGN_FROM_ACTOR) {
					unassigned = true;
					change.setCurrentAssignee("");
				}
			}
		}
	}

	private static void fixCodingAccordingToAuthoritativeList(Map<String, List<StructuralDataChange>> a, List<String> projects) {
		Set<String> keys = a.keySet();

		for (String k : keys) {
			List<StructuralDataChange> allChanges = a.get(k);
			for (StructuralDataChange sdc : allChanges) {
				
				
				int typeOfChange = sdc.getTypeOfChange(); // set this!
//				if(Timestamp.valueOf(sdc.getStoryCreatedAt()).toString().contains("27:48.109") 
//						//						&& sdc.getRawDataText().contains("added to ☺ Marketplace (NL/BE/UK/SE/COM) Roles")
//						) {
//					System.out.println("debug fixCodingAccordingToAuthoritativeList");
//				}
				if(sdc.getTypeOfChange() == AsanaActions.COMMENT)
					typeOfChange = AsanaActions.COMMENT;
				else if(containsCircleName(sdc.getRawDataText()))
					//					typeOfChange = recodeAddRemoveToCircleWithAuthoritativeList(sdc);
					typeOfChange = setAddRemoveCircle(sdc);
				else if(isProject(stripProjectName(sdc.getRawDataText()),projects))
					typeOfChange = setAddRemoveToIgnoreDelete(sdc);
				else if(sdc.getTypeOfChange()==AsanaActions.REMOVE_SUB_ROLE)
					typeOfChange = sdc.getTypeOfChange();

				String typeOfChangeDescription = AsanaActions.codeToString(typeOfChange);
				sdc.setTypeOfChange(typeOfChange);
				sdc.setTypeOfChangeDescription(typeOfChangeDescription);
			}
		}
	}

	/**
	 * Only considers prefixes 'added to' and 'removed from'
	 * @param rawDataText
	 * @return
	 */
	private static String stripProjectName(String rawDataText) {
		String pname = rawDataText.replace("added to", "").replace("removed from", "").trim();
		return pname;
	}

	private static int setAddRemoveToIgnoreDelete(StructuralDataChange sdc) {
		//we know that it already contains a project name
		if(sdc.getRawDataText().contains("removed from"))
			return AsanaActions.IGNORE_OR_DELETE;
		if(sdc.getRawDataText().contains("added to"))
			return AsanaActions.IGNORE_OR_DELETE;

		return sdc.getTypeOfChange();
	}

	private static boolean isProject(String rawDataText, List<String> projects) {

		for(String projectName : projects) {
			//			if(projectName.contains("Impraise setup and launched to test"))
			//				System.out.println("debug isProject");
			if(rawDataText.equals(projectName.trim()))
				return true;
		}
		return false;
	}

	private static int recodeAddRemoveToCircleWithAuthoritativeList(StructuralDataChange sdc) {
		//		if(sdc.getTypeOfChange() == AsanaActions.UNCLEAR_OR_CONFLICT_WITH_CODEBOOK) {
		if(sdc.getRawDataText().contains("added to") && 
				containsCircleName(sdc.getRawDataText()))
			return AsanaActions.ADD_TO_CIRCLE;

		else if(sdc.getRawDataText().contains("removed from") && 
				containsCircleName(sdc.getRawDataText()))
			return AsanaActions.REMOVE_FROM_CIRCLE;
		//		}
		return sdc.getTypeOfChange();
	}

	private static int setAddRemoveCircle(StructuralDataChange sdc) {

		if(sdc.getRawDataText().contains("added to"))
			return AsanaActions.ADD_TO_CIRCLE;

		if(sdc.getRawDataText().contains("removed from"))
			return AsanaActions.REMOVE_FROM_CIRCLE;

		return sdc.getTypeOfChange();
	}

	private static boolean containsCircleName(String text) {
		for(String circleName : AuthoritativeList.authoritativeListNames)
			if(text.contains(circleName))
				return true;
		return false;
	}

	private static Map<String, List<StructuralDataChange>> toMap(List<StructuralDataChange> uniqueEvents) {
		Map<String, List<StructuralDataChange>> res = new HashMap<String, List<StructuralDataChange>>();
		for (StructuralDataChange structuralDataChange : uniqueEvents) {
			if(!res.containsKey(structuralDataChange.getTaskId()))
				res.put(structuralDataChange.getTaskId(), new ArrayList<StructuralDataChange>());
			res.get(structuralDataChange.getTaskId()).add(structuralDataChange);
		}
		return res;
	}

	private static boolean isPresent(String id, Set<String> ids) {
		for (String s : ids) {
			if(s.equals(id)) {
				System.out.println("Present!");
				return true;
			}
		}
		System.out.println("Not present ******************************** ");
		return false;
	}

	private static void addCompletedEvent(List<StructuralDataChange> uniqueEvents) {
		Map<String, List<StructuralDataChange>> uniqueEventsMap = toMap(uniqueEvents);
		int i = 0;
		for (String k : uniqueEventsMap.keySet()) {

			boolean found = false;
			List<StructuralDataChange> changes = uniqueEventsMap.get(k);
			for(StructuralDataChange sdc: changes) {
				if(sdc.getTypeOfChange()==AsanaActions.COMPLETE_ROLE) {
					found=true;
					break;
				}
			}
			if(!found && changes.size()>0 && changes.get(0).getTaskCompletedAt()!=null) {
				StructuralDataChange sdc = changes.get(changes.size()-1).makeCopy();
				sdc.setStoryCreatedAt(changes.get(changes.size()-1).getTaskCompletedAt());
				sdc.setTypeOfChange(AsanaActions.COMPLETE_ROLE);
				sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.COMPLETE_ROLE));
				sdc.setMessageType("derived");
				System.out.println("Set: "+Arrays.toString(sdc.csvRow()));
				uniqueEvents.add(sdc);
				i++;
			}
		}
		System.out.println("Set "+i+" final events");
	}

	private static void fixChangeRoleName(List<StructuralDataChange> uniqueEvents) {
		int eventsChanged = 0;
		for (StructuralDataChange structuralDataChange : uniqueEvents) {
			if(structuralDataChange.getRawDataText().equals("removed the name")) {
				structuralDataChange.setTypeOfChange(AsanaActions.CHANGE_NAME_OF_ROLE);
				structuralDataChange.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.CHANGE_NAME_OF_ROLE));
				eventsChanged++;
			}
		}
		System.out.println("Fixed problem with 'removed the name'. Events changed: "+eventsChanged);
	}

	private static void setYinYangAsCircleChange(Map<String, List<StructuralDataChange>> allEvents) {
		for (String k : allEvents.keySet()) {
			for(StructuralDataChange sdc: allEvents.get(k)) {
				if(isCircle(sdc.getTaskName(),sdc.getTaskId())) { // this means it is yin&yang

					//					if(Timestamp.valueOf(sdc.getStoryCreatedAt()).toString().equals("2016-01-05 13:29:58.88"))
					//						System.out.println("debug YinYangAsCircleChange");

					if(!sdc.getMessageType().equals("derived") && 
							//							sdc.getTypeOfChange()!=AsanaActions.ADD_TO_CIRCLE
							//							&& sdc.getTypeOfChange()!=AsanaActions.REMOVE_FROM_CIRCLE
							//							&& sdc.getTypeOfChange()!=AsanaActions.CREATE_ROLE
							//							&& sdc.getTypeOfChange()!=AsanaActions.COMMENT 
							//							&& sdc.getTypeOfChange() != AsanaActions.CIRCLE_CHANGE
							//							&& sdc.getTypeOfChange() != AsanaActions.DESIGN_ROLE
							//							&& sdc.getTypeOfChange() != AsanaActions.DELETE_OR_MARK_COMPLETE
							//							&& sdc.getTypeOfChange() != AsanaActions.CHANGE_ACCOUNTABILITY_PURPOSE
							sdc.getTypeOfChange() == AsanaActions.CHANGE_SUB_ROLE
							&& (sdc.getChildId()==null || sdc.getChildId().isEmpty())) {
						sdc.setTypeOfChange(AsanaActions.CIRCLE_CHANGE);
						sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.CIRCLE_CHANGE));
					}
					else if(sdc.getTypeOfChange()==AsanaActions.CREATE_ROLE) {
						//						sdc.setTypeOfChange(AsanaActions.CREATE_CIRCLE);
						//						sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.CREATE_CIRCLE));
						setChange(sdc, AsanaActions.ADD_TO_CIRCLE);
					}
					else if(sdc.getTypeOfChange()==AsanaActions.DESIGN_ROLE) {
						sdc.setTypeOfChange(AsanaActions.DESIGN_CIRCLE);
						sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.DESIGN_CIRCLE));
					}
					else if(sdc.getTypeOfChange()==AsanaActions.DELETE_OR_MARK_COMPLETE && 
							(sdc.getChildId()==null || sdc.getChildId().isEmpty())) { // only if it is not a child (a child keeps its original code)
						sdc.setTypeOfChange(AsanaActions.DELETE_CIRCLE);
						sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.DELETE_CIRCLE));
					}						
				}
			}
		}
	}

	private static void fixTimeShift(Map<String, List<StructuralDataChange>> allEvents) {		
		for (String k : allEvents.keySet()) {
			for(StructuralDataChange sdc: allEvents.get(k)) {
				//				if(sdc.getTaskId().equals("99912941734884"))
				//					System.out.println("Delete me!");

				ZonedDateTime sdt = sdc.getStoryCreatedAt().atZone(ZoneId.of("UTC"));
				sdc.setStoryCreatedAt(sdt.withZoneSameInstant(ZoneId.of("Europe/Paris")).toLocalDateTime());

				if(sdc.getTaskCompletedAt()!=null) {
					ZonedDateTime zdt = sdc.getTaskCompletedAt().atZone(ZoneId.of("UTC"));
					sdc.setTaskCompletedAt(zdt.withZoneSameInstant(ZoneId.of("Europe/Paris")).toLocalDateTime());
				}
				if(sdc.getModifiedAt() != null) {
					ZonedDateTime zmt = sdc.getModifiedAt().atZone(ZoneId.of("UTC"));
					sdc.setTaskModifiedAt(zmt.withZoneSameInstant(ZoneId.of("Europe/Paris")).toLocalDateTime());
				}
			}
		}
	}

	private static void hasCode14(Map<String, List<StructuralDataChange>> allEvents) {
		int fourteen = 0;
		for (String key : allEvents.keySet()) {
			for(StructuralDataChange sdc : allEvents.get(key)) {
				if(sdc.getTypeOfChange()==AsanaActions.COMPLETE_ROLE)
					fourteen++;
			}
		}
		System.out.println("There are "+fourteen+" entries with code: "+AsanaActions.COMPLETE_ROLE);
	}

	private static List<StructuralDataChange> removeDups(Map<String, List<StructuralDataChange>> allEvents2) {
		Set<StructuralDataChange> uniqueEvents = new TreeSet<StructuralDataChange>();
		Set<String> taskIds = allEvents2.keySet();
		for (String tId : taskIds) {
			uniqueEvents.addAll(allEvents2.get(tId));
		}
		Logger.getLogger(PostProcessFromDB.class.getName()).info(
				"Removed "+(allEvents2.values().stream().mapToInt(List::size).sum() - uniqueEvents.size())+" duplicates");
		return new LinkedList<StructuralDataChange>(uniqueEvents);
	}

	public static String countEvents(Map<String, List<StructuralDataChange>> allEvents) {
		int tot = 0;
		for (String key : allEvents.keySet()) {
			tot+=allEvents.get(key).size();
		}
		return tot+"";
	}

	private static Map<String, List<StructuralDataChange>> removeDuplicateTasks(Map<String, List<StructuralDataChange>> allEvents2) {
		Map<LocalDateTime, StructuralDataChange> datetimeTask = new HashMap<LocalDateTime, StructuralDataChange>();
		Map<String, List<StructuralDataChange>> res = new TreeMap<String, List<StructuralDataChange>>();
		int tot = 0;

		int size0 = allEvents2.values().stream().mapToInt(List::size).sum();

		for (String taskId : allEvents2.keySet()) {
			List<StructuralDataChange> changes = allEvents2.get(taskId);
			for (StructuralDataChange sdc : changes) {
				if(!datetimeTask.containsKey(sdc.getStoryCreatedAt()))
					datetimeTask.put(sdc.getStoryCreatedAt(), sdc);
				else {
					StructuralDataChange existing = datetimeTask.get(sdc.getStoryCreatedAt());
					if(existing.getTaskId().equals(sdc.getTaskId()) && 
							(existing.getCircleIds()==null || existing.getCircleIds().isEmpty() || 
							existing.getCircle().equals("NO CIRCLE"))) {
						datetimeTask.put(sdc.getStoryCreatedAt(), sdc.makeCopy());
						tot++;
					}
				}
			}
		}

		for (String taskId : allEvents2.keySet()) {
			List<StructuralDataChange> changes = allEvents2.get(taskId);
			List<StructuralDataChange> newChanges = new ArrayList<StructuralDataChange>();
			for (StructuralDataChange sdc : changes) {
				StructuralDataChange change = datetimeTask.get(sdc.getStoryCreatedAt());
				newChanges.add(change);
			}
			res.put(taskId, newChanges);
		}

		int size1 = allEvents2.values().stream().mapToInt(List::size).sum();

		Logger.getLogger(Class.class.getName()).info("Found "+tot+ " events with same timestamp.");
		Logger.getLogger(Class.class.getName()).info("Begin: "+size0+ ". End: "+size1);
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


	private static int fixCompletedAndRemoveLastModify(Map<String, List<StructuralDataChange>> allEvents) {
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
		int lastModCnt = 0;
		for(String k : allEvents.keySet()) {
			List<StructuralDataChange> evts = new ArrayList<StructuralDataChange>();
			for(StructuralDataChange sdc : allEvents.get(k)) {
				if((sdc.getTypeOfChange()==AsanaActions.COMPLETE_ROLE && sdc.getTaskCompletedAt()==null) || 
						sdc.getTypeOfChange()==AsanaActions.LAST_MODIFY_ROLE) {
					lastModCnt++;
					continue;
				}
				else if(sdc.getTypeOfChange()==AsanaActions.COMPLETE_ROLE) {
					sdc.setStoryCreatedAt(sdc.getTaskCompletedAt());
				}
				evts.add(sdc);
			}
			res.put(k, evts);
		}
		System.out.println("Skipped "+lastModCnt+" LAST_MODIFY_ROLE events.");
		allEvents.clear();
		allEvents.putAll(res);
		return lastModCnt;
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
	
	private static void setTextOfDesignRole(Map<String, List<StructuralDataChange>> allEvents) {
		for(String k : allEvents.keySet()) {
			for(StructuralDataChange sdc : allEvents.get(k)) {
				if(sdc.getTypeOfChange()==15) {//CREATE_ROLE -> DESIGN_ROLE
					sdc.setRawDataText("created this task");
				}
			}
		}
	}

	private static Map<String, List<StructuralDataChange>> setCurrentCircles(Map<String, List<StructuralDataChange>> allEvents) {
		Map<String, List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>();
		Set<String> taskIds = allEvents.keySet();
		int neverAdded = 0;
		for (String taskId : taskIds) {
			//			if(taskId.equals("1158107169298928"))
			//				System.out.println("HERE!");
			boolean firstTimeAddedToCircle = true;
			List<String> circles = new ArrayList<String>();
			List<StructuralDataChange> taskHistory = new LinkedList<StructuralDataChange>(allEvents.get(taskId));
			java.util.Collections.sort(taskHistory);
			List<StructuralDataChange> newList = new LinkedList<StructuralDataChange>();

			for (StructuralDataChange sdc : taskHistory) {

				if(sdc.getTypeOfChange()==AsanaActions.ADD_TO_CIRCLE || 
						sdc.getTypeOfChange() == AsanaActions.DESIGN_ROLE) {
					String curCircle = sdc.getRawDataText().replaceAll("\\[EVENT FROM SUB-TASK\\]","").replaceAll("added to ", "").trim();
					int i = lookup(curCircle); // if -1 then it is not a circle
					if(i!=-1) {
						if(firstTimeAddedToCircle ) {
							setChange(sdc, AsanaActions.CREATE_ROLE);
							circles.add(curCircle);
						}
						else {
							List<String> newCirc = new ArrayList<String>();
							newCirc.add(curCircle);
							circles = GeneralUtils.union(circles, newCirc);
						}
						firstTimeAddedToCircle = false;
					}
					else {
						if(sdc.getMessageType().equals("comment"))
							setChange(sdc, AsanaActions.COMMENT);
					}
				}

				if(sdc.getTypeOfChange()==AsanaActions.REMOVE_FROM_CIRCLE) {
					String curCircle = sdc.getRawDataText().replaceAll("\\[EVENT FROM SUB-TASK\\]","").replaceAll("removed from ", "").trim();
					int i = lookup(curCircle); // if -1 then it is not a circle
					if(i!=-1) { 
						if(circles.contains(curCircle))
							circles.remove(curCircle);
						else {
							neverAdded++;
							//							System.err.println("This task "+sdc.getTaskId()+" "+sdc.getTaskName()
							//							+" was never added to "+curCircle);
						}
					}
					else {
						if(sdc.getMessageType().equals("comment"))
							setChange(sdc, AsanaActions.COMMENT);
					}
				}
				sdc.setCircle(commaSeparate(circles));
				sdc.setCircleIds(commaSeparateIds(circles));
				newList.add(sdc.makeCopy());
			}
			res.put(taskId, newList);
		}
		assertTrue(allEvents.size() == res.size());
		System.err.println("I found "+neverAdded+" tasks that were removed from a circle without having been added before.");
		return res;
	}

	private static void setChange(StructuralDataChange sdc, int code) {
		sdc.setTypeOfChange(code);
		sdc.setTypeOfChangeDescription(AsanaActions.codeToString(code));
	}

	private static void setChangeNew(StructuralDataChange sdc, int code) {
		sdc.setTypeOfChangeNew(code);
		sdc.setTypeOfChangeDescriptionNew(AsanaActions.codeToString(code));
	}

	public static void printHistoryOfTask(String taskId, Map<String, List<StructuralDataChange>> allEvents) {
		System.out.println("History of task "+taskId+" Size="+allEvents.get(taskId).size());
		for (StructuralDataChange sdc : allEvents.get(taskId)) {
			System.out.println(sdc.getStoryCreatedAt() +";"+sdc.getTaskId()+
					";"+sdc.getRawDataText()+";"+sdc.getCircle()+";"+sdc.getCircle());
		}
	}

	private static void fillAssignee(Map<String, List<StructuralDataChange>> allEvents) {
		Set<String> keys = allEvents.keySet();
		for (String key : keys) {
			String curAssignee = "";
			List<StructuralDataChange> changes = allEvents.get(key);			
			Collections.sort(changes);
			for (StructuralDataChange sdc : changes) { 
				if(sdc.getTypeOfChange() == AsanaActions.ASSIGN_TO_ACTOR)
					curAssignee=""+sdc.getCurrentAssignee();
				if(sdc.getTypeOfChange() == AsanaActions.UNASSIGN_FROM_ACTOR)
					curAssignee = "";
				//
				//				if(sdc.getTaskId().equals("847280364167627") && sdc.getDateTime().toLocalDate().equals(LocalDate.of(2020, 1, 2))) {
				//					System.out.println("debug fillAssignee()");
				//				}

				sdc.setCurrentAssignee(""+curAssignee);
			}
		}
	}

	private static void subroleToFather(Map<String, List<StructuralDataChange>> allParents, Map<String, List<StructuralDataChange>> allChildren) {
		Set<String> children = allChildren.keySet();
		List<String> addedToFather = new ArrayList<String>();
		for (String k : children) {
			List<StructuralDataChange> childStories = allChildren.get(k);

			for (StructuralDataChange sdc : childStories) {

				if(sdc.getTypeOfChange()==AsanaActions.ADD_SUB_ROLE ||
						sdc.getTypeOfChange()==AsanaActions.ROLE_EXTRACTION ||
						sdc.getTypeOfChange()==AsanaActions.ROLE_INTEGRATION ||
						sdc.getRoleType().equals("sub-role")) {
					List<StructuralDataChange> parentsEvents = null;
					parentsEvents = lookUpParent(allParents, allChildren, sdc.getParentTaskId());

					if(parentsEvents!=null) {
						Collections.sort(parentsEvents);

						sdc.setEventId(sdc.getTaskId());
						addedToFather.add(sdc.getTaskId());
						StructuralDataChange fatherEvent = getFatherEventAt(parentsEvents,sdc.getStoryCreatedAt());

						if(fatherEvent!=null && !fatherEvent.getCurrentAssignee().isEmpty() && !sdc.getCurrentAssignee().trim().isEmpty()) {

							String fatherAssignee = fatherEvent.getCurrentAssignee().trim();
							String curAssign = sdc.getCurrentAssignee().trim();
							String setString = "";

							Set<String> aSet = new LinkedHashSet<String>(Arrays.asList(fatherAssignee.split(",")));
							aSet.add(curAssign);

							for (String s : aSet) {
								setString+=","+s.trim();
							}
							if(setString.endsWith(","))
								setString = setString.substring(0, setString.length()-1);

							sdc.setCurrentAssignee(setString.substring(1));

						}

						switch (sdc.getTypeOfChange()) {
						case AsanaActions.ROLE_EXTRACTION:
							addToFather("[EVENT FROM SUB-TASK] ",sdc, parentsEvents, AsanaActions.ROLE_EXTRACTION);
							break;

						case AsanaActions.ROLE_INTEGRATION:
							addToFather("[EVENT FROM SUB-TASK] ",sdc, parentsEvents, AsanaActions.ROLE_INTEGRATION);
							break;

						default:
							addToFather("[EVENT FROM SUB-TASK] ",sdc, parentsEvents, AsanaActions.CHANGE_SUB_ROLE);
							break;
						}
					}
				}
			}
		}
		for (String id : addedToFather) {
			allChildren.remove(id);
		}
	}

	private static StructuralDataChange getFatherEventAt(List<StructuralDataChange> parentsEvents,
			LocalDateTime storyCreatedAt) {
		StructuralDataChange res = parentsEvents.get(0); 
		for (StructuralDataChange sdc : parentsEvents) {
			if(!res.getStoryCreatedAt().isAfter(storyCreatedAt)) // as long as it is before or at same time 
				res = sdc;
		}
		return res;
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

	private static Map<String, List<StructuralDataChange>> filterOutSubtasksWithNoEvent(Map<String, List<StructuralDataChange>> events) {

		Map<String, List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>();

		int tot = 0;

		Integer beginSize = events.values().stream().mapToInt(List::size).sum();

		Set<String> removeKeys = new HashSet<String>();
		Set<StructuralDataChange> removeEvents = new HashSet<StructuralDataChange>();

		for (String key : events.keySet()) {	
			List<StructuralDataChange> changes = new ArrayList<StructuralDataChange>(events.get(key));
			for (StructuralDataChange sdc : changes) {
				if(sdc.getIsSubtask() && sdc.getMessageType().equals("derived")) {
					removeEvents.add(sdc);
					tot++;
					if(events.get(key).size() == 0)
						removeKeys.add(key);
				}
			}
		}

		for(String k : events.keySet()) { // populate new map
			if(!removeKeys.contains(k)) {
				for (StructuralDataChange e : events.get(k)) {
					if(!removeEvents.contains(e)) {
						res.put(k, events.get(k));
					}
				}
			}
		}

		Integer endSize = res.values().stream().mapToInt(List::size).sum();

		System.out.println("Cleanup. Begin: "+beginSize + " End: "+endSize +
				". Filtered out "+(beginSize-endSize)+ " events with empty history.");

		return res;
	}

	private static int cleanup(Map<String, List<StructuralDataChange>> events) {

		Integer beginSize = events.values().stream().mapToInt(List::size).sum();

		Set<String> keys = new TreeSet<String>(events.keySet());
		int tot = 0;

		List<String> removeKeys = new ArrayList<String>();

		for (String key : events.keySet()) {	
			List<StructuralDataChange> changes = new ArrayList<StructuralDataChange>(events.get(key));

			for (StructuralDataChange sdc : changes) {
				if(sdc.getIsSubtask() && sdc.getMessageType().equals("derived")) {
					events.get(key).remove(sdc);
					tot++;
					if(events.get(key).size() == 0)
						removeKeys.add(key);
				}
			}
		}

		for (String k : removeKeys) { //the all have size 0
			events.remove(k);
		}

		Integer endSize = events.values().stream().mapToInt(List::size).sum();

		System.out.println("Cleanup. Begin: "+beginSize + " End: "+endSize);
		System.out.println("Test: "+ events.containsKey("472476427713571"));

		return tot;
	}

	private static Map<String, List<StructuralDataChange>> filterOutSeparator(Map<String, List<StructuralDataChange>> events) {
		Map<String, List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>();
		int c = 0;
		for (String k : events.keySet()) {
			if(!events.get(k).get(0).isRenderedAsSeparator()) {
				res.put(k, events.get(k));
			}
			else {
				c+=events.get(k).size();
			}
		}
		System.out.println("Filtered out "+c+ " events from separators.");
		return res;
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
				//				if(sdc.getTaskId().equals("11341931869295")) {
				//					changeAccPurpFound = true;
				//				}
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

	private static void accountabilityPurposeToFather(Map<String, List<StructuralDataChange>> allParents,
			Map<String, List<StructuralDataChange>> allChildren) {
		Set<String> children = allChildren.keySet();
		List<StructuralDataChange> addedToFather = new ArrayList<StructuralDataChange>();
		for (String child : children) {
			List<StructuralDataChange> changesOfChild = allChildren.get(child);
			for (StructuralDataChange sdc : changesOfChild) {

				if(sdc.isChangeAccountabilityPurpose() || sdc.getRoleType().equals("accountability/purpose")) {
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
						addToFather("[EVENT FROM PURPOSE/ACCOUNTABILITY SUBTASK] ",sdc, parentsEvents, AsanaActions.CHANGE_ACCOUNTABILITY_PURPOSE);
						addedToFather.add(sdc.makeCopy());
					}		
				}
			}
		}
		for (StructuralDataChange sdc : addedToFather) {
			allChildren.remove(sdc.getTaskId());
		}
	}

	private static void addToFather(String prefix, StructuralDataChange sdc, List<StructuralDataChange> parentsEvents, int code) {
		if(sdc.getMessageType().equals("derived"))
			return;

		//		sdc.setEventId(sdc.getTaskId());
		int i = (parentsEvents.size() > 1)? 1 : 0;  // to avoid picking a "derived" event in the father
		sdc.setParentTaskId(parentsEvents.get(i).getParentTaskId());
		sdc.setParentTaskName(parentsEvents.get(i).getDynamicParentName());
		sdc.setEventId(sdc.getTaskId());
		sdc.setTaskId(parentsEvents.get(i).getTaskId());
		sdc.setTaskName(parentsEvents.get(i).getTaskName());
		//		sdc.setCircleIds(parentsEvents.get(1).getCircleIds());
		//		sdc.setCircle(parentsEvents.get(1).getCircle());
		sdc.setRawDataText(prefix+sdc.getRawDataText());
		sdc.setTypeOfChange(code);
		sdc.setTypeOfChangeDescription(AsanaActions.codeToString(code));
		sdc.setTaskCreatedAt(parentsEvents.get(i).getTaskCreatedAt());
		sdc.setTaskCompletedAt(parentsEvents.get(i).getTaskCompletedAt());
		sdc.setRoleType("role");						
		parentsEvents.add(sdc.makeCopy());
	}

	private static boolean addToParent(String prefix, StructuralDataChange sdc, List<StructuralDataChange> parentsEvents, int code) {
		//		if(sdc.getMessageType().equals("derived"))
		//			return false;

		//		sdc.setEventId(sdc.getTaskId());
		int i = (parentsEvents.size() > 1)? 1 : 0;  // to avoid picking a "derived" event in the father
		sdc.setParentTaskId(parentsEvents.get(i).getParentTaskId());
		sdc.setParentTaskName(parentsEvents.get(i).getDynamicParentName());
		sdc.setEventId(sdc.getTaskId());
		sdc.setTaskId(parentsEvents.get(i).getTaskId());
		sdc.setTaskName(parentsEvents.get(i).getTaskName());
		//		sdc.setCircleIds(parentsEvents.get(1).getCircleIds());
		//		sdc.setCircle(parentsEvents.get(1).getCircle());
		sdc.setRawDataText(prefix+sdc.getRawDataText());
		sdc.setTypeOfChangeNew(code);
		sdc.setTypeOfChangeDescriptionNew(AsanaActions.codeToString(code));
		sdc.setTaskCreatedAt(parentsEvents.get(i).getTaskCreatedAt());
		sdc.setTaskCompletedAt(parentsEvents.get(i).getTaskCompletedAt());
		String roleType = parentsEvents.get(i).getRoleType();
		sdc.setRoleType(roleType);
		sdc.setDynamicHierarchy(parentsEvents.get(i).getDynamicHierarchy());
		sdc.setDynamicParentName(parentsEvents.get(i).getDynamicParentName());
		parentsEvents.add(sdc.makeCopy());
		return true;
	}

	private static String setToString(Set<String> allAssigness) {
		String res = "";

		for (String s : allAssigness) {
			if(s!=null && !s.isEmpty())
				res+=","+s;
		}
		if(res.length()>0)
			return res.substring(1);
		return res;
	}

	private static String getParentAssignessAtTime(LocalDateTime t,
			List<StructuralDataChange> parentsEvents) {

		Set<String> parents = new HashSet<String>();
		//		List<StructuralDataChange> sorted = new ArrayList<StructuralDataChange>(parentsEvents);
		//		Collections.sort(sorted);
		StructuralDataChange parent = null;
		for (StructuralDataChange sdc : parentsEvents) {
			if(sdc.getStoryCreatedAt().isAfter(t))
				break;

			else {
				parents.add(sdc.getCurrentAssignee());
				parent = sdc;
			}
		}

		if(parent==null)
			return "";

		return parent.getCurrentAssignee();
	}

	public static Map<String, List<StructuralDataChange>> getChildren() {
		String sql = "SELECT * FROM `SpringestRaw` WHERE parentTaskId<>'' ORDER BY `timestamp` ASC";
		Map<String, List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>(); 
		return getFromDB(sql, res);
	}

	/**
	 * Returns the history of the parents only
	 * @return
	 */
	public static Map<String, List<StructuralDataChange>> getParents() {
		String sql = "SELECT * FROM `SpringestRaw` WHERE parentTaskId='' ORDER BY `timestamp` ASC";

		Map<String, List<StructuralDataChange>> parents = new LinkedHashMap<String, List<StructuralDataChange>>(); 
		return getFromDB(sql, parents);
	}

	public static Map<String, List<StructuralDataChange>> getDuplicatedTasks() {
		String sql = "SELECT DISTINCT taskId FROM `SpringestRaw` WHERE rawDataText LIKE \"%duplicated task from%\"";

		Map<String, List<StructuralDataChange>> dups = new LinkedHashMap<String, List<StructuralDataChange>>(); 
		return getFromDB(sql, dups);
	}


	private static Map<String, List<StructuralDataChange>> getFromDB(String sql,
			Map<String, List<StructuralDataChange>> parents) {
		List<StructuralDataChange> events = ReadFromDB.readFromDBNoSortSimple("asana_manual901", sql);

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

	private static List<String> getCirclesAtTime(TreeSet<TimestampCircle> fathersHistory, LocalDateTime localDateTime) {
		List<String> circlesAtTime = new ArrayList<String>();
		if(fathersHistory==null)
			return circlesAtTime;
		TimestampCircle fathersLastEvent = null;
		long diff = Long.MAX_VALUE;
		long lastDiff = diff;
		for (TimestampCircle timestampCircle : fathersHistory) {
			Duration duration = Duration.between(localDateTime, timestampCircle.timestamp);
			if(duration.getNano()>0) {
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
			int idx = lookup(c);
			circleIds+=AuthoritativeList.authoritativeList[idx]+",";
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
		Set<String> authListNames = Sets.newHashSet(AuthoritativeList.authoritativeListNames);

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
						currentCircleId = AuthoritativeList.authoritativeList[i];

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
		for (; i < AuthoritativeList.authoritativeListNames.length; i++) {
			if(currentCircleName.trim().equals(AuthoritativeList.authoritativeListNames[i].trim())) {
				found = true;
				break;
			}
		}		
		if(currentCircleName.contains("Smooth Operations Roles")) //smooth operations uses 2 different smileys
			return 24;

		if(found)
			return i;

		return -1;
	}
}
