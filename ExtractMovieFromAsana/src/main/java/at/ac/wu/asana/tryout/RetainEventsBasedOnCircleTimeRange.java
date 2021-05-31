package at.ac.wu.asana.tryout;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import at.ac.wu.asana.csv.ReadInfoFromCSV;
import at.ac.wu.asana.db.postprocess.datastructures.CircleTimeRange;

public class RetainEventsBasedOnCircleTimeRange {

	public static void main(String[] args) {
		
		String overallFile = "/home/saimir/ownCloud/PhD/Collaborations/Waldemar/Springest/Data/"
				+ "Data Extracted from DB/Dataset with Manually Extracted Users/Springest-manually-fixed-V01.csv";
		
		String cutFile = "/home/saimir/ownCloud/PhD/Collaborations/Waldemar/Springest/Data/"
				+ "Data Extracted from DB/Dataset with Manually Extracted Users/Springest-manually-fixed-V01-cut.csv";
		
		String circleBirthDeath = "/home/saimir/ownCloud/PhD/Collaborations/Waldemar/Springest/Data/"
				+ "Data Extracted from DB/circleDependencies/circlesLives.csv";
		
		List<String> csvHeader = new ArrayList<String>();
		List<String[]> allRows = readAll(overallFile, csvHeader);
		List<String[]> retainedRows = new ArrayList<String[]>();
		List<CircleTimeRange> circleLives = ReadInfoFromCSV.readCircleLives(circleBirthDeath);
		
		DateTimeFormatter overaAll = new DateTimeFormatterBuilder()
				  .appendPattern("yyyy-MM-dd HH:mm:ss")
				  .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 3, true) // min 2 max 3
				  .toFormatter();
		
		for (String[] row : allRows) {
			String tsValue = row[0].trim();
			LocalDateTime tsEvent = LocalDateTime.parse(tsValue, overaAll);
			String tsProjectId = row[18].trim();
//			String tsProjectName = row[11].trim();
			
			if(row[1].trim().equals("11378875694427"))
				System.out.println("Debug");
			
			CircleTimeRange ctr = getCircle(circleLives, tsProjectId);
			
			if(tsEvent.isBefore(ctr.getStart()))
				continue;
			
			if(ctr.getEnd()!=null && tsEvent.isAfter(ctr.getEnd()))
				continue;
			
			retainedRows.add(row);			
		}
		
		writeCSV(retainedRows, cutFile, csvHeader);
	}

	private static void writeCSV(List<String[]> retainedRows, String cutFile, List<String> csvHeader) {
		CSVWriter writer;
		try {
			writer = new CSVWriter(new FileWriter(cutFile));
			writer.writeNext(csvHeader.toArray(new String[0]));
			writer.writeAll(retainedRows);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static CircleTimeRange getCircle(List<CircleTimeRange> circleLives, String tsProjectId) {
		for (CircleTimeRange ctr : circleLives) {
			if(ctr.getCircleId().equals(tsProjectId))
				return ctr;
		}
		return null;
	}

	private static List<String[]> readAll(String overallFile, List<String> csvHeader) {
		List<String[]> res = null;
		try {
			CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(overallFile)));
			List<String> l = Arrays.asList(reader.readNext());
			csvHeader.addAll(l);

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

}
