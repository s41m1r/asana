package at.ac.wu.asana.db.postprocess;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.SessionFactory;

import com.opencsv.CSVWriter;

import at.ac.wu.asana.db.io.ReadFromDB;
import at.ac.wu.asana.db.postprocess.datastructures.CircleCounts;
import at.ac.wu.asana.db.postprocess.datastructures.CirclePlusMinusTot;
import at.ac.wu.asana.db.postprocess.datastructures.CircleTot;
import at.ac.wu.asana.db.postprocess.datastructures.YMCircleList;
import at.ac.wu.asana.db.postprocess.datastructures.YMTaskList;
import at.ac.wu.asana.db.utils.DatabaseConnector;
import at.ac.wu.asana.model.StructuralDataChange;

public class CountByCircleWeekly {
	static String[] authoritativeList = new String[]{
			"0",
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
			"NO CIRCLE",
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

	static Map<String, Integer> ymPluses = new HashMap<String, Integer>();
	static Map<String, Integer> ymMinuses= new HashMap<String, Integer>();
	static Map<String, Integer> ymMods = new HashMap<String, Integer>();
	static Map<String, Integer> ymTasks = new HashMap<String, Integer>();
	static Map<String, Integer> ymTots = new HashMap<String, Integer>();

	static List<String> allYW;

	public static void main(String[] args) {

		List<CirclePlusMinusTot> circlePlusMinusTots = getWeeklyCountByCircle();

		printCirclesPlusMinusTot(circlePlusMinusTots, "totalsWeeklyByCirclePlusMinusTot.csv");

		//		printTotalsMonthly2(mapCircleToYMandTasks, "totalsMonthlyByCircle.csv");

	}

	static List<CirclePlusMinusTot> getWeeklyCountByCircle() {
		Map<String, List<CircleCounts>> ywCircleCounts = new TreeMap<String, List<CircleCounts>>();
		Map<String, List<StructuralDataChange>> ywChanges = new LinkedHashMap<String, List<StructuralDataChange>>();

		String dbname = "asana_manual5";
		String queryAllYW = "SELECT * FROM allYW";

		List<String> allYW = ReadFromDB.readAllTimePeriod(dbname, queryAllYW);

//		System.out.println(allYM);

		String queryAllInYW = "SELECT * FROM `SpringestWithCircle` "
				+ "WHERE YEARWEEK(`timestamp`) =:ym" 
				//				+ "AND typeOfChange IN (12,4,5,14)"
				+ "";
		//		date =:date

		// read the data
		SessionFactory sf = DatabaseConnector.getSessionFactory(dbname);
		org.hibernate.Session session = sf.openSession();
		for (String yw : allYW) {
			List<StructuralDataChange> changes = ReadFromDB.readChangesByYM(session, dbname, queryAllInYW, yw);
			ywChanges.put(yw, changes);
		}
		session.flush();
		session.close();
		sf.close();

		Set<String> yms = ywChanges.keySet();
		for (String ym : yms) {
			
			List<CircleCounts> counts = getTotRolesInCircle(ywChanges.get(ym));
			ywCircleCounts.put(ym, counts);
			
//			if(ym.equals("201404")) {
//				CircleCounts cc = counts.get(23);
//				System.out.println("DEBUG: "+Arrays.asList(cc.toCSVRow("201404")));
//			}
		}		

		Map<String,List<YMCircleList>> mapTaskToYMandCircles = new HashMap<String, List<YMCircleList>>();
		fillInMap(mapTaskToYMandCircles,ywChanges);
		//				printMap(mapTaskToYMandCircles);

		Map<String,List<YMTaskList>> mapCircleToYMandTasks = new TreeMap<String, List<YMTaskList>>();
		fillInCircleMap(mapCircleToYMandTasks, mapTaskToYMandCircles);
		//		printMapCircle(mapCircleToYMandTasks);

		//		String outFile = "Roles-in-circle-by-ym.csv";
		//
		//		writeMapToCSV(ymCircleCounts, outFile);

		List<CirclePlusMinusTot> circlePlusMinusTots = getListFromMap(mapCircleToYMandTasks, ywChanges);
		circlePlusMinusTots = expandByPadding(circlePlusMinusTots, mapCircleToYMandTasks);

		return circlePlusMinusTots;
	}

//	private static boolean contains(String element, Map<String, List<CircleCounts>> ymCircleCounts) {
//		for(String key: ymCircleCounts.keySet()) {
//			for(CircleCounts cc : ymCircleCounts.get(key)) {
//				if(cc.circleId.equals(element)) {
//					System.out.println("FOUND!!!");
//					return true;
//				}
//			}
//		}
//		return false;
//	}

	private static List<CirclePlusMinusTot> expandByPadding(List<CirclePlusMinusTot> circlePlusMinusTots, Map<String, List<YMTaskList>> mapCircleToYMandTasks) {
		List<CirclePlusMinusTot> res = new ArrayList<CirclePlusMinusTot>();
		List<String> circles = getAllCircles(circlePlusMinusTots);
		allYW = getAllYM(circlePlusMinusTots);
		Collections.sort(allYW);
		Collections.sort(circles);
		//		Map<String, String> yearCircle = getMapYearCircle(circlePlusMinusTots);

		//do the cross-product
		for (int i = 0; i < allYW.size(); i++) {
			for (int j = 0; j < circles.size(); j++) {
				List<YMTaskList> ymTaskLists = mapCircleToYMandTasks.get(circles.get(j));
				CirclePlusMinusTot cpmt = null;
				if(containsYM(allYW.get(i), ymTaskLists)) {
					cpmt = getEntryFrom(circles.get(j), allYW.get(i), circlePlusMinusTots);
				}
				else {
					cpmt = createZeroValue(circles.get(j), allYW.get(i));
					if(i>0) {
						CirclePlusMinusTot cpmt2 = getEntryFrom(circles.get(j), allYW.get(i-1), res);
						if(cpmt2!=null)
							cpmt.tot = cpmt2.tot;
					}
						
				}
				res.add(cpmt);
			}
		}
		Collections.sort(res);
		return res;
	}

	private static CirclePlusMinusTot getEntryFrom(String circle, String ym,
			List<CirclePlusMinusTot> circlePlusMinusTots) {
		for (CirclePlusMinusTot circlePlusMinusTot : circlePlusMinusTots) {
			if(circlePlusMinusTot.ym.equals(ym) && 
					circlePlusMinusTot.circleId.equals(circle))
				return circlePlusMinusTot;
		}
		return null;
	}


	private static boolean containsYM(String ym, List<YMTaskList> ymTaskLists) {
		for (YMTaskList ymTaskList : ymTaskLists) {
			if(ymTaskList.ym.equals(ym))
				return true;
		}
		return false;
	}

	private static CirclePlusMinusTot createZeroValue(String circle, String ym) {
		// TODO Auto-generated method stub
		CirclePlusMinusTot circlePlusMinusTot = new CirclePlusMinusTot();
		circlePlusMinusTot.setCircleId(circle);
		circlePlusMinusTot.setCircleName(getCircleNameFromID(circle));
		circlePlusMinusTot.setYm(ym);
		circlePlusMinusTot.setMinus(0);
		circlePlusMinusTot.setPlus(0);
		if(!ym.equals(allYW.get(0))) {
			circlePlusMinusTot.setTotAllCirclesPlusesPrevMonth(ymPluses.get(getPreviousMonth(ym, allYW)));
			circlePlusMinusTot.setTotAllCirclesMinusesPrevMonth(ymMinuses.get(getPreviousMonth(ym, allYW)));
			circlePlusMinusTot.setTotAllCirclesModsPrevMonth(ymMods.get(getPreviousMonth(ym, allYW)));
			circlePlusMinusTot.setTotAllCirclesPrevMonth(ymTasks.get(getPreviousMonth(ym, allYW)));
		}
		return circlePlusMinusTot;
	}

	private static int getTotalFromPreviousYM(int i, String circleId, List<String> allYM, List<CirclePlusMinusTot> circlePlusMinusTots) {
		int tot = 0;
		CirclePlusMinusTot previous = findPrevious(circlePlusMinusTots, i, circleId);
		if(previous!=null)
			tot = previous.getTot();
		return tot;
	}

	private static CirclePlusMinusTot findPrevious(List<CirclePlusMinusTot> circlePlusMinusTots, int i,
			String circleId) {
		for (int j = i-1; j > 0; j--) {
			if(circlePlusMinusTots.get(j).circleId.equals(circleId))
				return circlePlusMinusTots.get(j);
		}
		return null;
	}

	private static void contains(String circleId, List<CirclePlusMinusTot> circlePlusMinusTots) {
		// TODO Auto-generated method stub

	}

	private static boolean lookup(String ym, String circleId, List<CirclePlusMinusTot> circlePlusMinusTots) {
		for (CirclePlusMinusTot circlePlusMinusTot : circlePlusMinusTots) {
			if(circlePlusMinusTot.ym.equals(ym) && circlePlusMinusTot.circleId.equals(circleId))
				return true;
		}
		return false;
	}


	private static Map<String, String> getMapYearCircle(List<CirclePlusMinusTot> circlePlusMinusTots) {
		Map<String, String> mapYearCircle = new HashMap<String, String>();
		for (CirclePlusMinusTot circlePlusMinusTot : circlePlusMinusTots) {
			mapYearCircle.put(circlePlusMinusTot.ym, circlePlusMinusTot.circleId);
		}
		return mapYearCircle;
	}

	private static List<String> getAllYM(List<CirclePlusMinusTot> circlePlusMinusTots) {
		Set<String> res = new TreeSet<String>();
		for (CirclePlusMinusTot circlePlusMinusTot : circlePlusMinusTots) {
			res.add(circlePlusMinusTot.ym);
		}
		return new ArrayList<String>(res);
	}

	private static List<String> getAllCircles(List<CirclePlusMinusTot> circlePlusMinusTots) {
		Set<String> res = new TreeSet<String>();
		for (CirclePlusMinusTot circlePlusMinusTot : circlePlusMinusTots) {
			res.add(circlePlusMinusTot.circleId);
		}
		return new ArrayList<String>(res);
	}

	private static void printCirclesPlusMinusTot(List<CirclePlusMinusTot> circlePlusMinusTots, String outFile) {
		PrintWriter rolesFileWriter;

		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(outFile), StandardCharsets.UTF_8) );

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = CirclePlusMinusTot.csvHeader();
			csvWriter.writeNext(header);

			for (CirclePlusMinusTot circlePlusMinusTot : circlePlusMinusTots) {
				csvWriter.writeNext(circlePlusMinusTot.csvRow());
			}

			csvWriter.flush();
			csvWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private static List<CirclePlusMinusTot> getListFromMap(Map<String, List<YMTaskList>> mapCircleToYMandTasks, Map<String, List<StructuralDataChange>> ymChanges) {
		List<CirclePlusMinusTot> res = new ArrayList<CirclePlusMinusTot>();
		Set<String> circles = mapCircleToYMandTasks.keySet();
		for (String circle : circles) {
			List<YMTaskList> ymTaskLists = mapCircleToYMandTasks.get(circle);
			for (int i = 0; i < ymTaskLists.size(); i++) {
				CirclePlusMinusTot circlePlusMinusTot = new CirclePlusMinusTot();
				circlePlusMinusTot.setCircleId(circle);
				circlePlusMinusTot.setCircleName(getCircleNameFromID(circle));
				circlePlusMinusTot.setYm(ymTaskLists.get(i).ym);
				circlePlusMinusTot.setTot(ymTaskLists.get(i).taskIds.size());
				circlePlusMinusTot.setMods(countMods(ymTaskLists.get(i), ymChanges));
				if(i>0) {
					circlePlusMinusTot.setMinus(countMissing(ymTaskLists.get(i-1).taskIds,ymTaskLists.get(i).taskIds));
					circlePlusMinusTot.setPlus(countMissing(ymTaskLists.get(i).taskIds,ymTaskLists.get(i-1).taskIds));
					circlePlusMinusTot.setTotPlusesThisCirclePrevMonth(count(res,mapCircleToYMandTasks, ymTaskLists.get(i-1).ym, 1));
					circlePlusMinusTot.setTotMinusesThisCirclesPrevMonth(count(res, mapCircleToYMandTasks, ymTaskLists.get(i-1).ym, 2));
					circlePlusMinusTot.setTotModsThisCirclePrevMonth(count(res, mapCircleToYMandTasks, ymTaskLists.get(i-1).ym, 3));
					circlePlusMinusTot.setTotThisCirclesPreviousMonth(count(res, mapCircleToYMandTasks, ymTaskLists.get(i-1).ym, 4));
				}
				if(i==0)
					circlePlusMinusTot.setPlus(circlePlusMinusTot.getTot());

				res.add(circlePlusMinusTot);
			}
		}
		Collections.sort(res);
		List<String> allYM = new ArrayList<String>(ymChanges.keySet());
		Collections.sort(allYM);
		countTotPrevMonth(res, allYM);
		return res;
	}

	private static void countTotPrevMonth(List<CirclePlusMinusTot> res, List<String> allYM) {
		for (CirclePlusMinusTot circlePlusMinusTot : res) {

			String ym = circlePlusMinusTot.ym;

			if(!ymPluses.containsKey(ym)){
				ymPluses.put(ym, 0);
			}

			if(!ymMinuses.containsKey(ym)){
				ymMinuses.put(ym, 0);
			}

			if(!ymMods.containsKey(ym)){
				ymMods.put(ym, 0);
			}

			if(!ymTasks.containsKey(ym)){
				ymTasks.put(ym, 0);
			}

			ymPluses.put(ym, ymPluses.get(ym) + circlePlusMinusTot.getPlus());
			ymMinuses.put(ym, ymMinuses.get(ym) + circlePlusMinusTot.getMinus());
			ymMods.put(ym, ymMods.get(ym) + circlePlusMinusTot.getMods());
			ymTasks.put(ym, ymTasks.get(ym) + circlePlusMinusTot.getTot());
		}

		for (CirclePlusMinusTot circlePlusMinusTot : res) {
			String currentMonth = circlePlusMinusTot.ym;
			String previousMonth = getPreviousMonth(currentMonth, allYM);
			if(previousMonth!=null) {
				circlePlusMinusTot.setTotAllCirclesPlusesPrevMonth(ymPluses.get(previousMonth));
				circlePlusMinusTot.setTotAllCirclesMinusesPrevMonth(ymMinuses.get(previousMonth));
				circlePlusMinusTot.setTotAllCirclesModsPrevMonth(ymMods.get(previousMonth));
				circlePlusMinusTot.setTotAllCirclesPrevMonth(ymTasks.get(previousMonth));
			}
		}

	}

	private static String getPreviousMonth(String currentMonth, List<String> allYM) {
		String prevMonth = null;
		for (String current : allYM) {
			if(current.equals(currentMonth))
				break;
			prevMonth = current;
		}
		return prevMonth;
	}

	private static int count(List<CirclePlusMinusTot> res, Map<String, List<YMTaskList>> mapCircleToYMandTasks, String ym, int which) {
		int result = 0;
		for (CirclePlusMinusTot circlePlusMinusTot : res) {
			if(circlePlusMinusTot.ym.equals(ym)) {
				switch (which) {
				case 1: 
					result+=circlePlusMinusTot.plus;
					break;
				case 2:
					result+=circlePlusMinusTot.minus;
					break;
				case 3:
					result+=circlePlusMinusTot.mods;
					break;
				case 4:
					result+=circlePlusMinusTot.tot;
					break;
				default:
					break;
				}
			}

		}
		return result;
	}

	private static int countMods(YMTaskList ymTaskList, Map<String, List<StructuralDataChange>> ymChanges) {
		int res = 0;
		List<String> taskIds = ymTaskList.taskIds;
		String ym = ymTaskList.ym;

		List<StructuralDataChange> changes = ymChanges.get(ym);

		for (String task : taskIds) {
			for(StructuralDataChange change : changes) {
				if(change.getTaskId().equals(task))
					res+=countTaskChanges(change);
			}
		}
		return res;
	}

	private static int countTaskChanges(StructuralDataChange change) {
		switch (change.getTypeOfChange()) {
		case 9:
		case 11:
		case 111:
		case 2:
		case 1:
		case 15:
		case 3:
		case 6:
		case 5:
		case 4:
			return 1;

		default:
			break;
		}
		return 0;
	}

	/**
	 * Count how many of the first set are NOT in the second
	 * @param taskIds
	 * @param taskIds2
	 * @return
	 */
	private static int countMissing(List<String> taskIds, List<String> taskIds2) {
		int missing = 0;
		for (String taskId : taskIds) {
			if(!taskIds2.contains(taskId))
				missing++;
		}
		return missing;
	}



	private static String getCircleNameFromID(String circle) {
		for (int i = 0; i < authoritativeList.length; i++) {
			if(authoritativeList[i].equals(circle))
				return authoritativeListNames[i];
		}
		if(circle.equals("11555199602299"))
			return "☺ Smooth Ops Roles";
		return null;
	}



	private static void printTotalsMonthly(Map<String, List<YMTaskList>> mapCircleToYMandTasks, String outFile) {
		PrintWriter rolesFileWriter;
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(outFile), StandardCharsets.UTF_8) );

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = new String[]{"ym","circleId","circleName","tot", "plus", "minus"};
			csvWriter.writeNext(header);
			Set<String> allYM = getAllYMFromCirclesMap(mapCircleToYMandTasks);

			for (String ym : allYM) {
				Set<String> circles = new TreeSet<String>(mapCircleToYMandTasks.keySet());
				for(String c : circles) {
					int tot = getTotRoles(c, ym, mapCircleToYMandTasks);
					csvWriter.writeNext(new String[] {ym, c, tot+""});
				}
			}	
			csvWriter.flush();
			csvWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private static void printTotalsMonthly2(Map<String, List<YMTaskList>> mapCircleToYMandTasks, String outFile) {
		PrintWriter rolesFileWriter;
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(outFile), StandardCharsets.UTF_8) );

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = new String[]{"ym","circleId","circleName","tot"};
			csvWriter.writeNext(header);

			for(String circle : mapCircleToYMandTasks.keySet()) {
				for(YMTaskList taskList : mapCircleToYMandTasks.get(circle))
					csvWriter.writeNext(new String[] {taskList.ym, circle.toString(), "", taskList.taskIds.size()+""});
			}

			csvWriter.flush();
			csvWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private static int getTotRoles(String c, String ym, Map<String, List<YMTaskList>> mapCircleToYMandTasks) {
		int tot = 0;
		for (String k : mapCircleToYMandTasks.keySet()) {
			List<YMTaskList> ymTaskLists = mapCircleToYMandTasks.get(k);
			int i = lookupYM(ym, ymTaskLists);
			if(i!=-1) {
				YMTaskList tl = ymTaskLists.get(i);
				tot = tl.taskIds.size();
			}
		}
		return tot;
	}

	private static Set<String> getAllYMFromCirclesMap(Map<String, List<YMTaskList>> mapCircleToYMandTasks) {
		Set<String> allYM = new TreeSet<String>();
		for (String circle : mapCircleToYMandTasks.keySet()) {
			List<YMTaskList> ymTaskLists = mapCircleToYMandTasks.get(circle);
			for (YMTaskList ymTaskList : ymTaskLists) {
				allYM.add(ymTaskList.ym);
			}
		}
		return allYM;
	}

	private static void printMapCircle(Map<String, List<YMTaskList>> mapCircleToYMandTasks) {
		for(String k: mapCircleToYMandTasks.keySet())
			System.out.println(k+";"+mapCircleToYMandTasks.get(k));
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

	private static Set<String> getAllYM(Map<String, List<YMCircleList>> mapTaskToYMandCircles) {
		Set<String> allYM = new HashSet<String>();
		for (String task : mapTaskToYMandCircles.keySet()) {
			List<YMCircleList> ymCircleLists = mapTaskToYMandCircles.get(task);
			for (YMCircleList ymCircleList : ymCircleLists) {
				allYM.add(ymCircleList.ym);
			}
		}
		return allYM;
	}

	private static Set<String> getAllCircles(Map<String, List<YMCircleList>> mapTaskToYMandCircles) {
		Set<String> circles = new HashSet<String>();
		for (String task : mapTaskToYMandCircles.keySet()) {
			List<YMCircleList> circleLists = mapTaskToYMandCircles.get(task);
			for (YMCircleList ymCircleList : circleLists) {
				List<String> circs = ymCircleList.circleIds;
				circles.addAll(circs);
			}
		}
		return circles;
	}

	private static List<YMTaskList> getCirclesOfYm(String task, List<YMCircleList> ymCircles) {
		List<YMTaskList> res = new ArrayList<YMTaskList>();
		for (YMCircleList ymCircleList : ymCircles) {
			for(String circle : ymCircleList.circles) {
				YMTaskList yMTaskList = new YMTaskList(circle, new ArrayList<String>());
				yMTaskList.taskIds.add(task);
			}
		}
		return res;
	}

	private static void printMap(Map<String, List<YMCircleList>> mapTaskToYMandCircles) {
		for (String task : mapTaskToYMandCircles.keySet()) {
			System.out.println(task+";"+mapTaskToYMandCircles.get(task));
		}
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
						new ArrayList<String>(circleSet), authoritativeList, authoritativeListNames);
				if(!mapTaskToYMandCircles.containsKey(taskId)) {
					mapTaskToYMandCircles.put(taskId, new ArrayList<YMCircleList>());
				}
				mapTaskToYMandCircles.get(taskId).add(ymCircleList);
			}
		}		
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

	private static Set<String> getTasksInChanges(List<StructuralDataChange> list) {
		Set<String> tasksUnique = new HashSet<String>();
		for (StructuralDataChange structuralDataChange : list) {
			tasksUnique.add(structuralDataChange.getTaskId());
		}
		return tasksUnique;
	}

	private static List<CircleCounts> getTotRolesInCircle(List<StructuralDataChange> list) {
		List<CircleCounts> res = new ArrayList<CircleCounts>();
		Map<String,CircleCounts> mapCircleCounts = new HashMap<String, CircleCounts>();
		initCounts(mapCircleCounts);
		Map<String,Set<String>> mapTaskCircles = getMapTaskCircles(list);
		countRolesInCircle(mapTaskCircles, mapCircleCounts);
		countPlusMinusInCircle(mapTaskCircles, mapCircleCounts);

		for(String key : mapCircleCounts.keySet()) {
			res.add(mapCircleCounts.get(key));
		}

		return res;
	}

	private static void countPlusMinusInCircle(Map<String, Set<String>> mapTaskCircles,
			Map<String, CircleCounts> mapCircleCounts) {


	}

	private static void countRolesInCircle(Map<String, Set<String>> mapTaskCircles,
			Map<String, CircleCounts> mapCircleCounts) {
		for(String tId: mapTaskCircles.keySet()) {
			Set<String> circleIds = mapTaskCircles.get(tId);
			for (String circle : circleIds) {
				CircleCounts cc = mapCircleCounts.get(circle);
				cc.setTotRolesInCircle(cc.getTotRolesInCircle()+1);
			}
		}
	}

	private static Map<String, Set<String>> getMapTaskCircles(List<StructuralDataChange> list) {
		Map<String, Set<String>> map = new HashMap<String, Set<String>>();
		for (StructuralDataChange sdc : list) {
			String tId = sdc.getTaskId();
			if(!map.containsKey(tId))
				map.put(tId, new HashSet<String>());
			String[] circleIds = sdc.getCircleIds().split(",");
			String[] circleNames = sdc.getCircle().split(",");
			for (int i = 0; i < circleNames.length; i++) {
				if(circleNames[i].equals("NO CIRCLE"))
					circleIds[i]="0";
			}
			for (int i = 0; i < circleIds.length; i++) {
				map.get(tId).add(circleIds[i]);
			}
		}
		return map;
	}

	private static List<CircleCounts> getCountsFromChanges(List<StructuralDataChange> list) {
		List<CircleCounts> res = new ArrayList<CircleCounts>();
		Map<String,CircleCounts> mapCircleCounts = new HashMap<String, CircleCounts>();
		initCounts(mapCircleCounts);
		for (StructuralDataChange sdc : list) {
			String[] circleIds = sdc.getCircleIds().split(",");
			String[] circleNames = sdc.getCircle().split(",");
			for (int i = 0; i < circleNames.length; i++) {
				if(circleNames[i].equals("NO CIRCLE"))
					circleIds[i]="0";
				CircleCounts ccs = mapCircleCounts.get(circleIds[i]);
				ccs.setTotEvents(ccs.getTotEvents()+1);
				switch (sdc.getTypeOfChange()) {
				case 12: ccs.setTotDerivedEvents(ccs.getTotDerivedEvents()+1);
				break;
				case 15: 
				case 4:
				case 8:
					ccs.setBirths(ccs.getBirths()+1);
					if(sdc.getTypeOfChange()==4)
						ccs.setMigrations(ccs.getMigrations()+1);
					break;
				case 5: ccs.setMigrations(ccs.getMigrations()+1);
				case 14:
				case 7:
					ccs.setDeaths(ccs.getDeaths()+1);
					if(sdc.getTypeOfChange()==14)
						ccs.setTotDerivedEvents(ccs.getTotDerivedEvents()+1);						
					break;
				case 2:
				case 6: 
				case 111:
				case 9:
				case 3:
				case 11: 
				case 1:
					ccs.setModifications(ccs.getModifications()+1);
					break;
				case 13:
					ccs.setTotDerivedEvents(ccs.getTotDerivedEvents()+1);
					break;
				default:
					ccs.setTotOtherEvents(ccs.getTotOtherEvents()+1);
					break;
				}				
			}
		}

		for(String key :mapCircleCounts.keySet()) {
			res.add(mapCircleCounts.get(key));
		}

		return res;
	}

	private static void countBirthsDeaths(Map<String, List<CircleCounts>> ymCircleCounts) {
		// TODO Auto-generated method stub
		int births = 0;
		int deaths = 0;

		for(String k : ymCircleCounts.keySet()) {
			List<CircleCounts> counts = ymCircleCounts.get(k);
			for (CircleCounts cc : counts) {
				births+=cc.getBirths();
				deaths+=cc.getDeaths();
			}
		}

		System.out.println("births="+births+", deaths="+deaths);
	}

	private static void setCountsSoFarByCircle(Map<String, List<CircleCounts>> ymCircleCounts) {
		Set<String> days = ymCircleCounts.keySet();
		for (String day : days) {
			List<CircleCounts> dailyCounts = ymCircleCounts.get(day);

			for (CircleCounts countInDay : dailyCounts) {
				String circleId = countInDay.getCircleId();
				int countsUntilDay = getCountsSoFar(day, circleId, ymCircleCounts);
				countInDay.setTotRolesInCircle(countsUntilDay);
			}
		}
	}

	private static int getCountsSoFar(String theDay, String circleId, Map<String, List<CircleCounts>> ymCircleCounts) {
		int totRolesInCircle = 0;
		Set<String> days = ymCircleCounts.keySet();
		for (String day : days) {
			List<CircleCounts> counts = ymCircleCounts.get(day);
			CircleCounts cc = null;
			boolean found = false;
			for (CircleCounts c : counts) {
				if(c.getCircleId().equals(circleId)) {
					cc=c;
					found=true;
					break;
				}
			}
			if(found) {
				totRolesInCircle += cc.getTotRolesInCircle();
			}
			if(day.equals(theDay))
				break;
		}
		return totRolesInCircle;
	}

	private static void setCurrentCount(CircleCounts ccts, CircleTot currentCount) {
		currentCount.tot = ccts.getBirths()-ccts.getDeaths();
	}

	private static CircleCounts lookup(String k, List<CircleCounts> counts) {
		for (CircleCounts circleCounts : counts) {
			if(circleCounts.getCircleId().equals(k))
				return circleCounts;
		}
		return null;
	}

	private static List<CircleCounts> updateCounts(List<CircleCounts> counts, List<CircleCounts> list) {
		List<CircleCounts> res = new ArrayList<CircleCounts>(counts);
		for (CircleCounts cc : list) {
			CircleCounts c = lookup(cc, res);
			cc.setBirths(c.getBirths()+cc.getBirths());
			cc.setDeaths(c.getDeaths()+cc.getDeaths());
			cc.setModifications(c.getModifications()+cc.getModifications());
			cc.setMigrations(c.getMigrations()+cc.getMigrations());
		}
		return res;
	}

	private static CircleCounts lookup(CircleCounts cc, List<CircleCounts> res) {
		for (CircleCounts c : res) {
			if(cc.getCircleId().equals(c.getCircleId()))
				return c;
		}
		return null;
	}

	private static void writeMapToCSV(Map<String, List<CircleCounts>> ymCircleCounts, String outFile) {
		PrintWriter rolesFileWriter;
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(outFile), StandardCharsets.UTF_8) );

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = new String[]{"yw","circleId","circleName","births",
					"deaths", "modifications", "migrations", "totEvents", 
					"totDerivedEvents", "totOtherEvents", "totRolesInCircle", "totalRolesInCircleUntilThisMonth"};
			csvWriter.writeNext(header);
			for (String k : ymCircleCounts.keySet()) {
				List<CircleCounts> events = ymCircleCounts.get(k);
				java.util.Collections.sort(events);
				for (CircleCounts count : events) {
					csvWriter.writeNext(count.toCSVRow(k));
				}
			}
			csvWriter.flush();
			csvWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private static void initCounts(Map<String, CircleCounts> mapCircleCounts) {
		for (int i = 0; i < authoritativeList.length; i++) {
			CircleCounts c = new CircleCounts(authoritativeList[i], authoritativeListNames[i],
					0, 0, 0, 0, 0, 0);
			mapCircleCounts.put(authoritativeList[i], c);
		}
		mapCircleCounts.put("0", new CircleCounts("0", "NO CIRCLE",
				0, 0, 0, 0, 0, 0));
	}

}
