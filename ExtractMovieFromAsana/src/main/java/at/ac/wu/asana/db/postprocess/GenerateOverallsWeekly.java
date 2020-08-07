package at.ac.wu.asana.db.postprocess;

import java.util.List;
import java.util.Map;

import at.ac.wu.asana.db.io.ReadFromDB;
import at.ac.wu.asana.db.postprocess.datastructures.TimePeriodOveralls;
import at.ac.wu.asana.model.StructuralDataChange;
import at.ac.wu.asana.util.PrintoutUtils;

public class GenerateOverallsWeekly {

	public static void main(String[] args) {
		Map<String, List<StructuralDataChange>> weeklyChanges = ReadFromDB.getWeeklyChanges();

		List<TimePeriodOveralls> wkOveralls = GenerateOverallCountsMonthly.getOverallCount(weeklyChanges);
		String outFile = "overallsWeekly.csv";
		
		PrintoutUtils.writeOverallsToCSV(wkOveralls, outFile);
		System.out.println("Result written to "+outFile); 
	}

}
