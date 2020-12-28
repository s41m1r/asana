package at.ac.wu.asana.csv;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.opencsv.CSVReader;

import at.ac.wu.asana.db.postprocess.datastructures.AuthoritativeList;
import at.ac.wu.asana.db.postprocess.datastructures.CircleTimeRange;

public abstract class ReadCircleParentLocations {
	
	public static Map<String, List<CircleTimeRange>> readFromFile(String fileName){
		Map<String, List<CircleTimeRange>> map = new HashMap<String, List<CircleTimeRange>>();

		try {
			CSVReader reader = new CSVReader(new FileReader(fileName));
			reader.readNext();
			List<String[]> rows = reader.readAll();
			System.out.println("Read "+reader.getLinesRead()+" lines.");
			
			map = createCircleTimeRangeObjects(rows); 
			
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
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
//				ctr = ranges.get(ranges.size()-1);
				//get the event about father
				if (circleId.equals("47872397062455")) {
					System.out.println("Butta!");
				}
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
}
