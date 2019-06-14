package at.ac.wu.asana.test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
			"13169100426325","11626921109046","824769296181501","163654573139013",
			"47872397062455","404651189519209","29007443412107","12530878841888",
			"79667185218012","236886514207498","561311958443380","11347525454570",
			"560994092069672","61971534223290","389549960603898","11350833325340",
			"7746376637805","11555199602299","388515769387194"};
	
	static String[] authoritativeListNames = new String[] {
			"☺ Rainmakers Roles","☺Business Intelligence Roles","☺ Customer Success Roles",
			"☺ Evangelism Roles","☺ Finance Roles","☺ Germany Roles",
			"☺ Go Customer Roles","☺ Go Sales Roles","☺ Marketing Roles",
			"☺ Marketplace DE roles","☺ Office Roles","☺ Organisations Roles",
			"☺ People Roles","☺ Product Roles","☺ Providers roles","☺ Providers Roles",
			"☺ Sales Roles","☺ Smooth Ops Roles","☺ Users Roles"
	};
	
	
	static Map<String, String> mapTaskCurrentCircle = new HashMap<String, String>();
	
	public static void main(String[] args) throws IOException {
		
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
