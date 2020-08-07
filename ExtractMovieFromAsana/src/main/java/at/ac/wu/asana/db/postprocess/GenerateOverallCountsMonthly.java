package at.ac.wu.asana.db.postprocess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.SessionFactory;

import at.ac.wu.asana.db.io.ReadFromDB;
import at.ac.wu.asana.db.postprocess.datastructures.TimePeriodOveralls;
import at.ac.wu.asana.db.utils.DatabaseConnector;
import at.ac.wu.asana.model.StructuralDataChange;
import at.ac.wu.asana.util.PrintoutUtils;

public class GenerateOverallCountsMonthly {

	public static void main(String[] args) {

		Map<String, List<StructuralDataChange>> ymChanges = getMonthlyChanges();

		List<TimePeriodOveralls> ymOveralls = getOverallCount(ymChanges);
		String outFile = "overallsMonthly.csv";
		
		PrintoutUtils.writeOverallsToCSV(ymOveralls, outFile);
		System.out.println("Result written to "+outFile);

	}

	public static List<TimePeriodOveralls> getOverallCount(Map<String, List<StructuralDataChange>> ymChanges) {
		/* 1 = alive; -1 = dead;  */
		Map<String, Integer> taskStatus = new HashMap<String, Integer>(); 
		// if it is dead, and a code 7 comes, do not count it. Likewise for code 8 and 12

		List<TimePeriodOveralls> ymOveralls = new ArrayList<TimePeriodOveralls>();
		Set<String> keys = new TreeSet<String>(ymChanges.keySet());

		for (String ym : keys) {
			List<StructuralDataChange> changes = ymChanges.get(ym);
			TimePeriodOveralls overalls = new TimePeriodOveralls();
			overalls.setTimePeriod(ym);
			for (StructuralDataChange change : changes) {
				String taskId = change.getTaskId();

				if(!taskStatus.containsKey(taskId))
					taskStatus.put(taskId, 0);

				int toc = change.getTypeOfChange();
				
				switch (toc) {
				case 15:
				case 4:
					if(taskStatus.get(taskId)!=1) {
						overalls.births++;
					}
					taskStatus.put(taskId, 1);
					break;
//				case 8:
//					if(taskStatus.get(taskId)!=1) {
//						overalls.births++;
//					}
//					taskStatus.put(taskId, 1);
//					break;
				case 5:
				case 7:
					if(taskStatus.get(taskId)!=-1) {
						overalls.deaths++;
					}
					taskStatus.put(taskId, -1);
//				case 7:
//					if(taskStatus.get(taskId)!=-1) {
//						overalls.deaths++;
//					}
//					taskStatus.put(taskId, -1);
					break;

				case 9:
				case 11:
				case 2:
				case 1:
				case 3:
//				case 6:
				case 111:
					overalls.modifications++;
					break;

				default:
					break;
				}				
			}
			ymOveralls.add(overalls);
		}
		
		Collections.sort(ymOveralls);
		
		for (TimePeriodOveralls timePeriodOveralls : ymOveralls) {
			timePeriodOveralls.delta = timePeriodOveralls.births-timePeriodOveralls.deaths;
		}
		
		for (int i=0; i<ymOveralls.size();i++) {			
			TimePeriodOveralls periodCount = ymOveralls.get(i);
			int tot = 0;
			for (int j = 0; j <= i; j++) {
				tot+=ymOveralls.get(j).getDelta();
			}
			periodCount.setTot(tot);
		}
		
		return ymOveralls;
	}

	private static Map<String, List<StructuralDataChange>> getMonthlyChanges() {
		Map<String, List<StructuralDataChange>> ymChanges = new LinkedHashMap<String, List<StructuralDataChange>>();

		String dbname = "asana_manual5";
		String queryAllYM = "SELECT * FROM allYM";

		List<String> allYM = ReadFromDB.readAllTimePeriod(dbname, queryAllYM);

		String queryAllInYM = "SELECT * FROM `SpringestWithCircle` "
				+ "WHERE EXTRACT( YEAR_MONTH FROM `date` ) =:ym "
				//				+ "AND typeOfChange IN (12,4,5,14)"
				+ "";
		//		date =:date

		// read the data
		SessionFactory sf = DatabaseConnector.getSessionFactory(dbname);
		org.hibernate.Session session = sf.openSession();
		for (String ym : allYM) {
			List<StructuralDataChange> changes = ReadFromDB.readChangesByYM(session, dbname, queryAllInYM, ym);
			ymChanges.put(ym, changes);
		}
		session.flush();
		session.close();
		sf.close();

		return ymChanges;
	}
}
