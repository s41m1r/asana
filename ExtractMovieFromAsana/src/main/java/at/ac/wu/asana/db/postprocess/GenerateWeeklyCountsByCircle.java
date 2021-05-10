package at.ac.wu.asana.db.postprocess;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.opencsv.CSVWriter;

import at.ac.wu.asana.db.io.ReadFromDB;
import at.ac.wu.asana.db.postprocess.datastructures.CircleCountsWeekly;
import at.ac.wu.asana.db.postprocess.datastructures.TimePeriodOveralls;
import at.ac.wu.asana.model.CirclesLives;
import at.ac.wu.asana.model.StructuralDataChange;
import at.ac.wu.asana.util.GeneralUtils;
import at.ac.wu.asana.util.PrintoutUtils;

public class GenerateWeeklyCountsByCircle {

	public static void main(String[] args) {

		Instant start = Instant.now();
		String outFile = "circlesWKCounts.csv";
				
		Map<String, List<StructuralDataChange>> weeklyChanges = ReadFromDB.getWeeklyChanges("asana_manual901");
		CirclesLives circlesLives = new CirclesLives();
		circlesLives.init();
				
		weeklyChanges = cutFromBirthOn(weeklyChanges, circlesLives.firstBirthday());
		
		System.out.println("Before: "+GeneralUtils.countEntriesMap(weeklyChanges));
		filterChangesByCircleLives(weeklyChanges, circlesLives);
		System.out.println("After: "+GeneralUtils.countEntriesMap(weeklyChanges));

		List<TimePeriodOveralls> wkOveralls = GenerateOverallCountsMonthly.getOverallCount(weeklyChanges);
		//		
		PrintoutUtils.writeOverallsToCSV(wkOveralls, "wkOveralls.csv", "week");
		
		Map<String, List<CircleCountsWeekly>> circlesWeeklyCounts = getCircleCountsByWeek(weeklyChanges);
		Map<String, List<CircleCountsWeekly>> filteredCirclesWeeklyCounts = filterWeeklyCountsByCircleLives(circlesWeeklyCounts, circlesLives);
		
		System.out.println("Before: "+GeneralUtils.countEntriesMap(circlesWeeklyCounts)+
				" after: "+GeneralUtils.countEntriesMap(filteredCirclesWeeklyCounts));

		setTotWeekly(filteredCirclesWeeklyCounts, wkOveralls);
		setTotCircleWeekly(filteredCirclesWeeklyCounts);
		
		writeMapToCSV(filteredCirclesWeeklyCounts, outFile);

		System.out.println("Done in "+Duration.between(start, Instant.now()));

	}

	private static Map<String, List<StructuralDataChange>> cutFromBirthOn(
			Map<String, List<StructuralDataChange>> weeklyChanges, LocalDate firstBirthday) {
		Map<String, List<StructuralDataChange>> res = new LinkedHashMap<String, List<StructuralDataChange>>();
		Set<Entry<String,List<StructuralDataChange>>> entries = weeklyChanges.entrySet();
		
		int weekBirth = firstBirthday.get(WeekFields.of(Locale.ITALY).weekOfWeekBasedYear());
		int yearBirth = firstBirthday.getYear();
		String firstBirthdayString = (String.valueOf(weekBirth).length()==1)? yearBirth+"0"+weekBirth : yearBirth+""+weekBirth;  
		
		for (Entry<String, List<StructuralDataChange>> entry : entries) {
			String currentWeek = entry.getKey();
			if(currentWeek.compareTo(firstBirthdayString)>=0)
				res.put(entry.getKey(), entry.getValue());
		}
		
		return res;
	}

	private static Map<String, List<CircleCountsWeekly>> filterWeeklyCountsByCircleLives(Map<String, List<CircleCountsWeekly>> circlesWeeklyCounts,
			CirclesLives circlesLives) {
		Map<String, List<CircleCountsWeekly>> res = new LinkedHashMap<String, List<CircleCountsWeekly>>();
		for (String week : circlesWeeklyCounts.keySet()) {
			
			
			List<CircleCountsWeekly> counts = new ArrayList<CircleCountsWeekly>();
			for (CircleCountsWeekly weeklyCount : circlesWeeklyCounts.get(week)) {
				String cId = weeklyCount.getCircleId();
				
				if(cId.equals("0")) {
					counts.add(weeklyCount);
					continue;
				}
				
				if(isAliveThisWeek(cId, circlesLives, week)) {	
					counts.add(weeklyCount);
				}
			}
			res.put(week,counts);
		}
		return res;
	}

	private static void filterChangesByCircleLives(Map<String, List<StructuralDataChange>> weeklyChanges,
			CirclesLives circlesLives) {
		
		List<String> weeks = new ArrayList<String>(weeklyChanges.keySet());
		for (String week : weeks) {
			List<StructuralDataChange> changes = weeklyChanges.get(week);
//			if(changes.isEmpty()) {
//				weeklyChanges.remove(week);
//				System.out.println("Removed week "+week+" because it has no events.");
//			}
			List<StructuralDataChange> filteredChanges = filterChangesBasedOnCircleLives(changes,circlesLives);
			System.out.println("In week "+week+" removed " +(changes.size()-filteredChanges.size())+" events");
//			changes.clear();
//			changes.addAll(filteredChanges);
			changes.retainAll(filteredChanges);
		}
	}

	private static List<StructuralDataChange> filterChangesBasedOnCircleLives(List<StructuralDataChange> changes,
			CirclesLives circlesLives) {
		List<StructuralDataChange> res = new ArrayList<StructuralDataChange>();
		
		for (StructuralDataChange change : changes) {
			String circleId = change.getAccordingToCircle();
			LocalDate birth = circlesLives.getBirthOf(circleId);
			LocalDate death = circlesLives.getDeathOf(circleId);
			LocalDateTime timeOfThisChange = change.getDateTime();
			
			if(!timeOfThisChange.toLocalDate().isBefore(birth)) {
				if(death==null) {
					res.add(change);
				}
				else {
					if(!timeOfThisChange.toLocalDate().isAfter(death)) {
						res.add(change);
					}
				}
			}
		}
		return res;
	}

	private static void setTotCircleWeekly(Map<String, List<CircleCountsWeekly>> circlesWeeklyCounts) {
		List<String> weeks = new ArrayList<String>(circlesWeeklyCounts.keySet());
		String prevWeek = null;
		for (String wk : weeks) {
			List<CircleCountsWeekly> circleCountsWeeklies = circlesWeeklyCounts.get(wk);
			for (CircleCountsWeekly countsThisWk : circleCountsWeeklies) {
				countsThisWk.setTot(countUntilWeek(countsThisWk.circleId, wk, circlesWeeklyCounts, 0));
				countsThisWk.setTotThisCirclesPreviousMonth(countUntilWeek(countsThisWk.circleId, prevWeek, circlesWeeklyCounts, 0));
				countsThisWk.setTotPlusesThisCirclePrevMonth(countWeek(countsThisWk.circleId, prevWeek, circlesWeeklyCounts,1));
				countsThisWk.setTotMinusesThisCirclesPrevMonth(countWeek(countsThisWk.circleId, prevWeek, circlesWeeklyCounts,2));
				countsThisWk.setTotModsThisCirclePrevMonth(countWeek(countsThisWk.circleId, prevWeek, circlesWeeklyCounts,3));
				countsThisWk.setTotAllCirclesPlusesPrevMonth(countWeek(prevWeek, circlesWeeklyCounts,1));
				countsThisWk.setTotAllCirclesMinusesPrevMonth(countWeek(prevWeek, circlesWeeklyCounts,2));
				countsThisWk.setTotAllCirclesModsPrevMonth(countWeek(prevWeek, circlesWeeklyCounts,3));
			}
			prevWeek = wk;
		}

	}

	/**
	 * 
	 * @param prevWeek
	 * @param circlesWeeklyCounts
	 * @param what – 0 totals of this circle, 1 total births of this circle, 2 total deaths of this circle, 
	 * 3 total modifications of this circle
	 * @return
	 */
	private static int countWeek(String circleId, String prevWeek, Map<String, List<CircleCountsWeekly>> circlesWeeklyCounts, int what) {
		int res = 0;
		if(prevWeek != null) {
			Set<String> weeks = circlesWeeklyCounts.keySet();
			for (String week : weeks) {
				if(week.compareTo(prevWeek) == 0) {
					List<CircleCountsWeekly> weeklies = circlesWeeklyCounts.get(week);
					for (CircleCountsWeekly ccw : weeklies) {
						if(ccw.getCircleId().equals(circleId)) {
							switch (what) {
							case 0:
								res+=ccw.getBirths()-ccw.getDeaths();
								break;
							case 1: 
								res+=ccw.getBirths();
								break;
							case 2:
								res+=ccw.getDeaths();
								break;
							case 3:
								res+=ccw.getModifications();
								break;
							default:
								break;
							}	
						}
					}
				}
			}
		}
		return res;

	}

	/**
	 * 
	 * @param prevWeek
	 * @param circlesWeeklyCounts
	 * @param what – 0 totals of this circle, 1 total births of this circle, 2 total deaths of this circle, 
	 * 3 total modifications of this circle
	 * @return
	 */
	private static int countWeek(String prevWeek,
			Map<String, List<CircleCountsWeekly>> circlesWeeklyCounts, int what) {
		
		int res = 0;
		if(prevWeek != null) {
			Set<String> weeks = circlesWeeklyCounts.keySet();
			for (String week : weeks) {
				if(week.compareTo(prevWeek) == 0) {
					List<CircleCountsWeekly> weeklies = circlesWeeklyCounts.get(week);
					for (CircleCountsWeekly ccw : weeklies) {
						switch (what) {
						case 0:
							res+=ccw.getBirths()-ccw.getDeaths();
							break;
						case 1: 
							res+=ccw.getBirths();
							break;
						case 2:
							res+=ccw.getDeaths();
							break;
						case 3:
							res+=ccw.getModifications();
							break;
						default:
							break;
						}	
					}
				}
			}
		}
		return res;
	}

	/**
	 * 
	 * @param prevWeek
	 * @param circlesWeeklyCounts
	 * @param what – 0 totals of this circle, 1 total births of this circle, 2 total deaths of this circle, 
	 * 3 total modifications of this circle
	 * @return
	 */

	private static int countUntilWeek(String prevWeek, Map<String, List<CircleCountsWeekly>> circlesWeeklyCounts,
			int what) {
		int res = 0;
		if(prevWeek != null) {
			Set<String> weeks = circlesWeeklyCounts.keySet();
			for (String week : weeks) {
				if(week.compareTo(prevWeek) < 0) {
					List<CircleCountsWeekly> weeklies = circlesWeeklyCounts.get(week);
					for (CircleCountsWeekly ccw : weeklies) {
						switch (what) {
						case 0:
							res+=ccw.getBirths()-ccw.getDeaths();
							break;
						case 1: 
							res+=ccw.getBirths();
							break;
						case 2:
							res+=ccw.getDeaths();
							break;
						case 3:
							res+=ccw.getModifications();
							break;
						default:
							break;
						}	
					}
				}
			}
		}
		return res;
	}

	/**
	 * 
	 * @param circleId
	 * @param wk
	 * @param circlesWeeklyCounts
	 * @param what – 0 totals of this circle, 1 total births of this circle, 2 total deaths of this circle, 
	 * 3 total modifications of this circle
	 * @return
	 */
	private static int countUntilWeek(String circleId, String wk, Map<String, List<CircleCountsWeekly>> circlesWeeklyCounts, int what) {
		int res = 0;
		if(wk != null) {
			Set<String> weeks = circlesWeeklyCounts.keySet();
			for (String week : weeks) {
				if(week.compareTo(wk) <= 0) {
					List<CircleCountsWeekly> weeklies = circlesWeeklyCounts.get(week);
					for (CircleCountsWeekly ccw : weeklies) {
						if(ccw.getCircleId().equals(circleId)) {
							switch (what) {
							case 0:
								res+=ccw.getBirths()-ccw.getDeaths();
								break;
							case 1: 
								res+=ccw.getBirths();
								break;
							case 2:
								res+=ccw.getDeaths();
								break;
							case 3:
								res+=ccw.getModifications();
								break;
							default:
								break;
							}	
						}
					}
				}
			}
		}
		return res;
	}

	private static void setTotWeekly(Map<String, List<CircleCountsWeekly>> circlesWeeklyCounts,
			List<TimePeriodOveralls> wkOveralls) {
		Map<String, TimePeriodOveralls> weeklyOveralls = toMap(wkOveralls);
		Set<String> weeks = new TreeSet<String>(circlesWeeklyCounts.keySet());
		java.util.Iterator<String> iterator = weeks.iterator();
		String prevWeek = iterator.next();
		while(iterator.hasNext()) {
			String thisWeek = iterator.next();
//			TimePeriodOveralls overallsPrevWeek = weeklyOveralls.get(prevWeek);
			List<CircleCountsWeekly> circleCountsWeeklies = circlesWeeklyCounts.get(thisWeek);
			for (CircleCountsWeekly circleCountsWeekly : circleCountsWeeklies) {
				circleCountsWeekly.setTotAllCirclesPreviousWeek(sumUntilWeek(weeklyOveralls, prevWeek));
			}
			prevWeek=thisWeek;
		}
	}

	private static int sumUntilWeek(Map<String, TimePeriodOveralls> weeklyOveralls, String week) {
		int tot = 0;
		SortedSet<String> weeks = new TreeSet<String>(weeklyOveralls.keySet());
		for (String w : weeks) {
			if(w.compareTo(week) <= 0 ) {
				tot += weeklyOveralls.get(w).getBirths() - weeklyOveralls.get(w).getDeaths();
			}
		}
		return tot;
	}

	private static Map<String, TimePeriodOveralls> toMap(List<TimePeriodOveralls> wkOveralls) {
		Map<String, TimePeriodOveralls> res = new TreeMap<String, TimePeriodOveralls>();

		for (TimePeriodOveralls timePeriodOveralls : wkOveralls) {
			res.put(timePeriodOveralls.getTimePeriod(), timePeriodOveralls);
		}

		return res;
	}

	private static void writeMapToCSV(Map<String, List<CircleCountsWeekly>> circlesWeeklyCounts, String outFile) {
		PrintWriter rolesFileWriter;
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(outFile), StandardCharsets.UTF_8) );

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = CircleCountsWeekly.csvHeader();
			csvWriter.writeNext(header);
			for (String k : circlesWeeklyCounts.keySet()) {
				List<CircleCountsWeekly> events = circlesWeeklyCounts.get(k);
				java.util.Collections.sort(events);
				for (CircleCountsWeekly count : events) {
					csvWriter.writeNext(count.csvRow());
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

	private static Map<String, List<CircleCountsWeekly>> getCircleCountsByWeek(
			Map<String, List<StructuralDataChange>> weeklyChanges) {

		Map<String, List<CircleCountsWeekly>> res = new TreeMap<String, List<CircleCountsWeekly>>();

		Set<String> weeks = weeklyChanges.keySet();
		String[] circleIds = GenerateDatasetFromCircles.initCircleIds();
		String[] circleNames = GenerateDatasetFromCircles.initCircleNames();
		
		CirclesLives lives = new CirclesLives();
		lives.init();

		for (String week : weeks) {
			List<StructuralDataChange> eventsOfWeek = weeklyChanges.get(week);
			List<CircleCountsWeekly> ccs = new ArrayList<CircleCountsWeekly>();

			for (StructuralDataChange change : eventsOfWeek) {
				CircleCountsWeekly cc = new CircleCountsWeekly();
				String circleId = change.getAccordingToCircle();			
				String circleName = getCircleNameFromId(circleId, circleIds, circleNames);

				cc.setCircleId(circleId);
				cc.setCircleName(circleName);
				cc.setWeek(week+"");
				
				cc.setAge(computeAge(circleId, lives, week));

				if(contains(ccs, circleId)) {
					cc = get(circleId, ccs); 
				}
				else {				
					ccs.add(cc);
				}	

				setTypeOfChange(change, cc);	
			}
			addMissingCircles(ccs,circleIds,circleNames, week,lives);
			//			Collections.sort(ccs);
			res.put(week, ccs);
		}

		return res;
	}

	private static void addMissingCircles(List<CircleCountsWeekly> ccs, String[] circleIds, String[] circleNames, String week, CirclesLives lives) {
		Set<String> considered = getCircleIdsSet(ccs);
		for (int i = 0; i < circleIds.length; i++) {
			if(!considered.contains(circleIds[i]) && isAliveThisWeek(circleIds[i],lives,week)) {
				CircleCountsWeekly cc = new CircleCountsWeekly();
				cc.setCircleId(circleIds[i]);
				cc.setCircleName(circleNames[i]);
				cc.setWeek(week);
				cc.setAge(computeAge(circleIds[i], lives, week));
				ccs.add(cc);
			}
		}
	}

	private static boolean isAliveThisWeek(String circleId, CirclesLives lives, String week) {
		if(circleId.equals("0"))
			return true;
		
		LocalDate birthThisCircle = lives.getBirthOf(circleId);
		LocalDate deathThisCircle = lives.getDeathOf(circleId);
		int weekBirth = birthThisCircle.get(WeekFields.of(Locale.ITALY).weekOfWeekBasedYear());
		int yearBirth = birthThisCircle.getYear();
		int weekDeath = (deathThisCircle==null)? 53 : deathThisCircle.get(WeekFields.of(Locale.ITALY).weekOfWeekBasedYear());
		int yearDeath = (deathThisCircle==null)? 2022: deathThisCircle.getYear();
		
		int thisWeek = Integer.parseInt(week);
		int birthWeek = Integer.parseInt(yearBirth+""+((String.valueOf(weekBirth).length()==1)? "0"+weekBirth:weekBirth));
		int deathWeek = Integer.parseInt(yearDeath+""+((String.valueOf(weekDeath).length()==1)? "0"+weekDeath:weekDeath));
		
		return thisWeek>=birthWeek && thisWeek<=deathWeek;
	}

	private static Set<String> getCircleIdsSet(List<CircleCountsWeekly> ccs) {
		Set<String> res = new HashSet<String>();
		for (CircleCountsWeekly circleCounts : ccs) {
			res.add(circleCounts.getCircleId());
		}
		return res;
	}

	private static void setTypeOfChange(StructuralDataChange change, CircleCountsWeekly cc) {
		int toc = change.getTypeOfChange();
		switch (toc) {
		case 15:
		case 4:
			if(concernsThisCircle(change,cc))
				cc.setBirths(cc.getBirths()+1);
			break;
		case 5:
			if(concernsThisCircle(change,cc))
				cc.setDeaths(cc.getDeaths()+1); // removed from circle
			break;
		case 7:
			cc.setDeaths(cc.getDeaths()+1);	 // completed this task		
			break;

		case 9:
		case 11:
		case 2:
		case 1:
		case 3:
//		case 6:
		case 111:
			cc.setModifications(cc.getModifications()+1);
			break;

		default:
			break;
		}
	}

	private static boolean concernsThisCircle(StructuralDataChange change, CircleCountsWeekly cc) {
		if(change.getRawDataText().contains(cc.getCircleName()))
			return true;
		return false;
	}

	private static CircleCountsWeekly get(String circleId, List<CircleCountsWeekly> ccs) {
		for (CircleCountsWeekly circleCounts : ccs) {
			if(circleCounts.getCircleId().equals(circleId))
				return circleCounts;
		}
		return null;
	}

	private static boolean contains(List<CircleCountsWeekly> ccs, String circleId) {
		for (CircleCountsWeekly circleCounts : ccs) {
			if(circleCounts.getCircleId().equals(circleId))
				return true;
		}
		return false;
	}

	public static void writeToCSV(Map<String, List<TimePeriodOveralls>> circlesWeeklyCounts, String outFile) {
		PrintWriter output;
		String[] circleIds = GenerateDatasetFromCircles.initCircleIds();
		String[] circleNames = GenerateDatasetFromCircles.initCircleNames();
		try {
			output = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(outFile), StandardCharsets.UTF_8));

			CSVWriter csvWriter = new CSVWriter(output);
			String[] header = TimePeriodOveralls.csvHeader("yearWeek");
			List<String> h = new ArrayList<String>();
			h.add("circle");
			h.addAll(Arrays.asList(header));
			csvWriter.writeNext(h.toArray(new String[0]));
			for(String k: circlesWeeklyCounts.keySet()) {
				SortedSet<TimePeriodOveralls> tpoList =  new TreeSet<TimePeriodOveralls>(circlesWeeklyCounts.get(k));
				for (TimePeriodOveralls tpo : tpoList) {
					List<String> r = new ArrayList<String>();
					String circleName = getCircleNameFromId(k, circleIds, circleNames);
					r.add(circleName);
					r.addAll(Arrays.asList(tpo.toCSVRow(tpo.getTimePeriod())));
					csvWriter.writeNext(r.toArray(new String[0]));
				}
			}
			csvWriter.flush();
			csvWriter.close();
		}catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static String getCircleNameFromId(String k, String[] circleIds, String[] circleNames) {
		for (int i = 0; i < circleIds.length; i++) {
			if(circleIds[i].equals(k))
				return circleNames[i];
		}
		return null;
	}

	public static Map<String, List<TimePeriodOveralls>> getWeeklyCountsByCircle(Map<String, List<StructuralDataChange>> weeklyChanges) {
		Map<String, List<TimePeriodOveralls>> circlesWeeklyCounts = new TreeMap<String, List<TimePeriodOveralls>>();
		String[] allCircleIds = GenerateDatasetFromCircles.initCircleIds();
		for (int i = 0; i < allCircleIds.length; i++) {
			circlesWeeklyCounts.put(allCircleIds[i], new ArrayList<TimePeriodOveralls>());
		}
		for(String week:  weeklyChanges.keySet()) {
			Map<String, TimePeriodOveralls> circleCountsThisWeek = new TreeMap<String, TimePeriodOveralls>();
			List<StructuralDataChange> wkChanges = weeklyChanges.get(week);
			TimePeriodOveralls overallsThisWeek = new TimePeriodOveralls();
			overallsThisWeek.setTimePeriod(week);

			for (int i = 0; i < allCircleIds.length; i++) {
				circleCountsThisWeek.put(allCircleIds[i], overallsThisWeek);
				circlesWeeklyCounts.get(allCircleIds[i]).add(overallsThisWeek);
			}
			for(StructuralDataChange change : wkChanges) {
				String circle = change.getAccordingToCircle();
				//				if(!circlesWeeklyCounts.containsKey(circle))
				//					circlesWeeklyCounts.put(circle, new ArrayList<TimePeriodOveralls>());
				int toc = change.getTypeOfChange();
				switch (toc) {
				case 15:
				case 4:
					circleCountsThisWeek.get(circle).setBirths(circleCountsThisWeek.get(circle).getBirths()+1);
					break;
				case 5:
				case 7:
					circleCountsThisWeek.get(circle).setDeaths(circleCountsThisWeek.get(circle).getDeaths()+1);
					break;

				case 9:
				case 11:
				case 2:
				case 1:
				case 3:
//				case 6:
				case 111:
					circleCountsThisWeek.get(circle).setModifications(circleCountsThisWeek.get(circle).getModifications()+1);
					break;

				default:
					break;
				}
			}
			for(String circleChanged : circleCountsThisWeek.keySet()) {
				circlesWeeklyCounts.get(circleChanged).add(circleCountsThisWeek.get(circleChanged));
			}
		}
		return circlesWeeklyCounts;
	}
	
	public static long computeAge(String circleId, CirclesLives circlesLives, String week) {

		WeekFields weekFields = WeekFields.of(Locale.ITALY);
		LocalDate thisWeekDate = LocalDate.now().withYear(Integer.valueOf(week.substring(0,4)))
				.with(weekFields.weekOfYear(),Integer.valueOf(week.substring(4,6)))
				.with(weekFields.dayOfWeek(), 2);
		
		if(!circlesLives.isInit())
			circlesLives.init();
		
		LocalDate birth = circlesLives.getBirthOf(circleId);
		LocalDate birthWeek = birth.with(weekFields.dayOfWeek(),2);

		return ChronoUnit.WEEKS.between(birthWeek, thisWeekDate);
	}

}
