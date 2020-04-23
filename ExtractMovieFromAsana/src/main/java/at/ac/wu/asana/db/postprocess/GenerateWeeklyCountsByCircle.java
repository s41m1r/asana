package at.ac.wu.asana.db.postprocess;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.opencsv.CSVWriter;

import at.ac.wu.asana.db.io.ReadFromDB;
import at.ac.wu.asana.db.postprocess.datastructures.CircleCountsWeekly;
import at.ac.wu.asana.db.postprocess.datastructures.TimePeriodOveralls;
import at.ac.wu.asana.model.StructuralDataChange;

public class GenerateWeeklyCountsByCircle {

	public static void main(String[] args) {

		Instant start = Instant.now();
		String outFile = "circlesWKCounts.csv";
		Map<String, List<StructuralDataChange>> weeklyChanges = ReadFromDB.getWeeklyChanges("asana_manual6");

		List<TimePeriodOveralls> wkOveralls = GenerateOverallCountsMonthly.getOverallCount(weeklyChanges);
		//		
		//		PrintoutUtils.writeOverallsToCSV(wkOveralls, outFile, "week");

		Map<String, List<CircleCountsWeekly>> circlesWeeklyCounts = getCircleCountsByWeek(weeklyChanges);
		setTotWeekly(circlesWeeklyCounts, wkOveralls);
		setTotCircleWeekly(circlesWeeklyCounts);

		writeMapToCSV(circlesWeeklyCounts, outFile);

		System.out.println("Done in "+Duration.between(start, Instant.now()));

	}

	private static void setTotCircleWeekly(Map<String, List<CircleCountsWeekly>> circlesWeeklyCounts) {
		List<String> weeks = new ArrayList<String>(circlesWeeklyCounts.keySet());
		String prevWeek = null;
		for (String wk : weeks) {
			List<CircleCountsWeekly> circleCountsWeeklies = circlesWeeklyCounts.get(wk);
			for (CircleCountsWeekly countsThisWk : circleCountsWeeklies) {
				countsThisWk.setTot(countUntilWeek(countsThisWk.circleId, wk, circlesWeeklyCounts, 0));
				countsThisWk.setTotPlusesThisCirclePrevMonth(countWeek(countsThisWk.circleId, prevWeek, circlesWeeklyCounts,1));
				countsThisWk.setTotMinusesThisCirclesPrevMonth(countWeek(countsThisWk.circleId, prevWeek, circlesWeeklyCounts,2));
				countsThisWk.setTotModsThisCirclePrevMonth(countWeek(countsThisWk.circleId, prevWeek, circlesWeeklyCounts,3));
				countsThisWk.setTotAllCirclesPrevMonth(countUntilWeek(prevWeek, circlesWeeklyCounts,0));
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
				if(week.compareTo(wk) < 0) {
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
		for(String week: circlesWeeklyCounts.keySet()) {
			List<CircleCountsWeekly> thisWeek = circlesWeeklyCounts.get(week);
			for (CircleCountsWeekly circleCountsWeekly : thisWeek) {
				int currentTot = sumUntilWeek(weeklyOveralls, week);
				circleCountsWeekly.setTotAllCirclesPreviousWeek(currentTot);
			}
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

		for (String week : weeks) {
			List<StructuralDataChange> eventsOfWeek = weeklyChanges.get(week);
			if(eventsOfWeek.isEmpty())
				continue;
			List<CircleCountsWeekly> ccs = new ArrayList<CircleCountsWeekly>();
			for (StructuralDataChange change : eventsOfWeek) {
				CircleCountsWeekly cc = new CircleCountsWeekly();
				String circleId = change.getAccordingToCircle();			
				String circleName = getCircleNameFromId(circleId, circleIds, circleNames);

				cc.setCircleId(circleId);
				cc.setCircleName(circleName);
				cc.setWeek(week+"");

				if(contains(ccs, circleId)) {
					cc = get(circleId, ccs); 
				}
				else {				
					ccs.add(cc);
				}	

				setTypeOfChange(change, cc);	
			}
			addMissingCircles(ccs,circleIds,circleNames, week);
			//			Collections.sort(ccs);
			res.put(week, ccs);
		}

		return res;
	}

	private static void addMissingCircles(List<CircleCountsWeekly> ccs, String[] circleIds, String[] circleNames, String week) {
		Set<String> considered = getCircleIdsSet(ccs);
		for (int i = 0; i < circleIds.length; i++) {
			if(!considered.contains(circleIds[i])) {
				CircleCountsWeekly cc = new CircleCountsWeekly();
				cc.setCircleId(circleIds[i]);
				cc.setCircleName(circleNames[i]);
				cc.setWeek(week);
				ccs.add(cc);
			}
		}
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
			cc.setBirths(cc.getBirths()+1);
			break;
		case 5:
		case 7:
			cc.setDeaths(cc.getDeaths()+1);
			break;

		case 9:
		case 11:
		case 2:
		case 1:
		case 3:
		case 6:
		case 111:
			cc.setModifications(cc.getModifications()+1);
			break;

		default:
			break;
		}
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

	private static void writeToCSV(Map<String, List<TimePeriodOveralls>> circlesWeeklyCounts, String outFile) {
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

	private static Map<String, List<TimePeriodOveralls>> getWeeklyCountsByCircle(Map<String, List<StructuralDataChange>> weeklyChanges) {
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
				case 6:
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

}
