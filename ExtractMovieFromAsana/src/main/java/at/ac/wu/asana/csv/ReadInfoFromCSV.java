package at.ac.wu.asana.csv;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;

import at.ac.wu.asana.db.postprocess.datastructures.AuthoritativeList;
import at.ac.wu.asana.db.postprocess.datastructures.CircleTimeRange;

public abstract class ReadInfoFromCSV {
	
	public static Map<String, List<CircleTimeRange>> readParentLocations(String fileName){
		Map<String, List<CircleTimeRange>> map = new HashMap<String, List<CircleTimeRange>>();

		try {
			CSVReader reader = new CSVReader(new FileReader(fileName));
			reader.readNext();
			List<String[]> rows = reader.readAll();
			
			map = createCircleTimeRangeObjects(rows); 
			
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CsvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}
	
	public static List<CircleTimeRange> readCircleLives(String fileName){
		
		List<CircleTimeRange> res = new ArrayList<CircleTimeRange>();

		try {
			CSVReader reader = new CSVReader(new FileReader(fileName));
			reader.readNext(); //skip header
			List<String[]> rows = reader.readAll();
			res = createListOfCirclesLives(rows); 
			
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
	
	private static List<CircleTimeRange> createListOfCirclesLives(List<String[]> rows) {
		List<CircleTimeRange> res = new ArrayList<CircleTimeRange>();
		for (String[] row : rows) {
			String circleId = row[0];
			String circleName = row[1];
			String startR = row[2];
			String endR = row[3];

			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("M/d/yyyy");

			LocalDateTime start = LocalDate.parse(startR, dtf).atStartOfDay();
			LocalDateTime end = endR.equals("")? null: LocalDate.parse(endR, dtf).atStartOfDay();
			
			CircleTimeRange range = new CircleTimeRange(circleId, circleName, start, end);
			
			res.add(range);
					
		}
		return res;
	}

	private static Map<String, List<CircleTimeRange>> createCircleTimeRangeObjects(List<String[]> rows) {
		Map<String, List<CircleTimeRange>> map = new HashMap<String, List<CircleTimeRange>>();
		
		for (String[] row : rows) {
			String circleId = row[4].trim();
			String parentId = row[3].trim();
			String time = row[0].trim();
			String event = row[2].trim();
			
			String parentName = lookupId(parentId);
			
			CircleTimeRange ctr = null;
			List<CircleTimeRange> ranges = null;
			
			if(map.containsKey(circleId)) {
				ranges = map.get(circleId);
				ctr = getFather(ranges, parentId); // find last father  
				
				if(ctr == null || ctr.getEnd()!=null) {
					ctr = new CircleTimeRange(parentId, parentName);
					ranges.add(ctr);
				}
			}
			else {
				ranges = new ArrayList<CircleTimeRange>();
				ctr = new CircleTimeRange(parentId, parentName);
				ranges.add(ctr);
			}
			
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("M/d/yyyy");
			
			LocalDateTime t = LocalDate.parse(time, dtf).atStartOfDay();
			
			if(event.contains("creation") || event.contains("added")) {
				ctr.setStart(t);
			}
			else {
				ctr.setEnd(t);
			}
			
			map.put(circleId, ranges);			
		}
		
		return map;
	}
	
	private static CircleTimeRange getFather(List<CircleTimeRange> ranges, String parentId) {
		for (CircleTimeRange circleTimeRange : Lists.reverse(ranges)) {
			if(circleTimeRange.getCircleId().equals(parentId))
				return circleTimeRange;
		}
		return null;
	}

	public static String lookupId(String currentCircleId) {
		
		if(currentCircleId.equals("0"))
			return "root";
		
		boolean found = false;
		int i = 0;
		for (; i < AuthoritativeList.authoritativeList.length; i++) {
			if(currentCircleId.equals(AuthoritativeList.authoritativeList[i])) {
				found = true;
				break;
			}
		}
		
		if(found)
			return AuthoritativeList.authoritativeListNames[i];

		return null;
	}
	
	public static List<String> getColumn(int columnNR, String filename){
		List<String> res = new ArrayList<String>();
		System.out.println("Reading file "+filename);
		try {
		    FileInputStream fis = new FileInputStream(filename);
		    InputStreamReader is = new InputStreamReader(fis);
			CSVReader reader = new CSVReader(is);
			reader.readNext(); //skip header
	
			String[] row;
			while((row = reader.readNext()) !=null) {
				res.add(row[columnNR].trim());
			}
			reader.close();
			
			System.out.println("Read "+reader.getRecordsRead()+" lines");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CsvValidationException e) {
			e.printStackTrace();
		} catch (CsvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}

	public static List<String[]> readAll(String filename){
		List<String[]> res = new ArrayList<String[]>();
		System.out.println("Reading file "+filename);
		try {
			CSVReader reader = new CSVReader(new FileReader(filename));
			reader.readNext(); //skip header
			res = reader.readAll();
			reader.close();
			System.out.println("Read "+res.size()+" lines");
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
