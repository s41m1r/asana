package at.ac.wu.asana.db.postprocess;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.SessionFactory;

import com.opencsv.CSVWriter;

import at.ac.wu.asana.db.io.ReadFromDB;
import at.ac.wu.asana.db.postprocess.datastructures.YmOveralls;
import at.ac.wu.asana.db.utils.DatabaseConnector;
import at.ac.wu.asana.model.StructuralDataChange;

public class GenerateOverallCountsMonthly {

	public static void main(String[] args) {

		Map<String, List<StructuralDataChange>> ymChanges = getMonthlyChanges();

		List<YmOveralls> ymOveralls = getOverallCount(ymChanges);
		String outFile = "overallsMonthly.csv";
		
		writeOverallsToCSV(ymOveralls, outFile);
		System.out.println("Result written to "+outFile);

	}

	private static void writeOverallsToCSV(List<YmOveralls> ymOveralls, String outFile) {
		PrintWriter rolesFileWriter;
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(outFile), StandardCharsets.UTF_8));

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = YmOveralls.csvHeader();
			csvWriter.writeNext(header);
			for (YmOveralls change : ymOveralls) {
				csvWriter.writeNext(change.toCSVRow(change.ym));
			}
			csvWriter.flush();
			csvWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static List<YmOveralls> getOverallCount(Map<String, List<StructuralDataChange>> ymChanges) {
		/* 1 = alive; -1 = dead;  */
		Map<String, Integer> taskStatus = new HashMap<String, Integer>(); 
		// if it is dead, and a code 7 comes, do not count it. Likewise for code 8 and 12

		List<YmOveralls> ymOveralls = new ArrayList<YmOveralls>();
		Set<String> keys = new TreeSet<String>(ymChanges.keySet());

		for (String ym : keys) {
			List<StructuralDataChange> changes = ymChanges.get(ym);
			YmOveralls overalls = new YmOveralls();
			overalls.setYm(ym);
			for (StructuralDataChange change : changes) {
				String taskId = change.getTaskId();

				if(!taskStatus.containsKey(taskId))
					taskStatus.put(taskId, 0);

				int toc = change.getTypeOfChange();

				switch (toc) {
				case 12:
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
				case 14:
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
				case 15:
				case 3:
				case 6:
				case 5:
				case 111:
				case 4:
					overalls.modifications++;
					break;

				default:
					break;
				}
			}
			ymOveralls.add(overalls);

		}
		return ymOveralls;
	}

	private static Map<String, List<StructuralDataChange>> getMonthlyChanges() {
		Map<String, List<StructuralDataChange>> ymChanges = new LinkedHashMap<String, List<StructuralDataChange>>();

		String dbname = "asana_manual5";
		String queryAllYM = "SELECT * FROM allYM";

		List<String> allYM = ReadFromDB.readAllYM(dbname, queryAllYM);

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
