package at.ac.wu.asana.test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.opencsv.CSVWriter;

import at.ac.wu.asana.db.io.ReadFromDB;
import at.ac.wu.asana.model.AsanaActions;
import at.ac.wu.asana.model.StructuralDataChange;

public class AddOneMoreRow {

	static String[] authoritativeList = new String[]{
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
			"824769296181501"};

	static String[] authoritativeListNames = new String[] {
			"☺ Sales Roles",
			"☺ Infrastructure Roles",
			"☺ Alignment Roles",
			"☺ Organisations Roles",
			"☺ Marketplace Roles",
			"☺ Demand Roles",
			"☺ Providers Roles",
			"☺ Smooth Ops Roles",
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
			"☺ Customer Success Roles"};


	static Map<String, String> mapTaskCurrentCircle = new HashMap<String, String>();

	public static void main(String[] args) throws IOException {

		//		addTwoMoreRows();
		long start = System.currentTimeMillis();
		fixCircles();
		System.out.println("Done in "+(System.currentTimeMillis()-start)/1000+" sec.");
	}

	private static void fixCircles() throws IOException {
		List<StructuralDataChange> events = ReadFromDB.readFromDB("asana_manual3");
		System.out.println("Read "+events.size()+" events.");		
		String csv = "outCircles.csv";

		PrintWriter rolesFileWriter = new PrintWriter(
				new OutputStreamWriter(
						new FileOutputStream(csv), StandardCharsets.UTF_8) );

		CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
		String[] header = StructuralDataChange.csvHeaderCircle();

		csvWriter.writeNext(header);

//		String circle = null;
		List<String> circles = new ArrayList<String>();
		String lastTaskId = null;

		for (StructuralDataChange sdc : events) {
			String currTaskId = sdc.getTaskId();
			if(!currTaskId.equals(lastTaskId)) {
				lastTaskId = currTaskId;
//				circle = null;
				circles = new ArrayList<String>();
			}
			if(sdc.getTypeOfChange()==AsanaActions.ADD_TO_CIRCLE) {
				String curCircle = sdc.getRawDataText().replaceAll("added to ", "").trim();
				int i = lookup(curCircle); // if -1 then it is not a circle
				if(i!=-1) {
//					circle = curCircle;
					if(!circles.contains(curCircle))
						circles.add(curCircle);
				}
//				else {
////					circle = null;
////					circles = new ArrayList<String>();
//				}
			}
			if(sdc.getTypeOfChange()==AsanaActions.REMOVE_FROM_CIRCLE) {
				String curCircle = sdc.getRawDataText().replaceAll("removed from ", "").trim();
				int i = lookup(curCircle); // if -1 then it is not a circle
				if(i!=-1) {
//					circle = curCircle;
					if(circles.contains(curCircle))
						circles.add(curCircle);
				}
//				else {
////					circle = null;
//					circles = new ArrayList<String>();
//				}
			}
			sdc.setCircle(commaSeparate(circles));
			csvWriter.writeNext(sdc.csvRowCircle());
		}
		csvWriter.flush();
		csvWriter.close();
	}

	private static String commaSeparate(List<String> circles) {
//		if(circles.isEmpty())
//			return "NO CIRCLE";
		String res = "";
		for (int i = 0; i < circles.size()-1; i++) {
			res += circles.get(i) + ",";
		}
		res+=circles.get(circles.size()-1);
		return res;
	}

	private static void addTwoMoreRows() throws FileNotFoundException, IOException {
		Set<String> authListNames = Sets.newHashSet(authoritativeListNames);

		List<StructuralDataChange> events = ReadFromDB.readFromDB("asana_manual3");
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


			if(sdc.getTaskId().equals("7817799957722"))
				System.out.println("qui");

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
						currentCircleId = authoritativeList[i];

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
		for (; i < authoritativeListNames.length; i++) {
			if(currentCircleName.equals(authoritativeListNames[i])) {
				found = true;
				break;
			}
		}
		if(found)
			return i;

		return -1;
	}
}
