package at.ac.wu.asana.db.postprocess;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import at.ac.wu.asana.csv.WriteUtils;
import at.ac.wu.asana.db.io.ReadFromDB;
import at.ac.wu.asana.model.StructuralDataChange;

public class GenerateDatasetFromCircles {
	
	public static void main(String[] args) {
		
		String path = "/home/saimir/ownCloud/PhD/Collaborations/Waldemar/ISR/Data/Mess-by-Saimir-Do-Not-Enter/";
//		String outFile = "datasetFromCirlce.csv";
		
		Map<String, List<StructuralDataChange>> dataset = new TreeMap<String, List<StructuralDataChange>>();
		
		Instant start = Instant.now();
		
		List<StructuralDataChange> allChanges = ReadFromDB.readFromDBNoSort(
				"asana_manual5", 
				"SELECT * FROM SpringestWithCircle"
				);
		Instant dataRead = Instant.now();
		
		System.out.println("Raw data read in "+Duration.between(start, dataRead));
		System.out.println("Raw data has "+allChanges.size()+" events.");
		System.out.println("Creating dataset by circle");
		
//		Set<String> circleIds = new HashSet<String>(Arrays.asList(initCircleIds()));
//		Set<String> circleNames = new HashSet<String>(Arrays.asList(initCircleNames()));
		
		for(StructuralDataChange sdc : allChanges) {
			String[] cids = sdc.getCircleIds().split(",");
			for (String cid : cids) {
				if(!dataset.containsKey(cid))
					dataset.put(cid, new ArrayList<StructuralDataChange>());
				dataset.get(cid).add(sdc);
			}
		}
		
		Instant dataTurned = Instant.now();
		System.out.println("Done in "+Duration.between(dataRead, dataTurned));
//		System.out.println("Writing output to file "+outFile);
		
		Instant startWriting = Instant.now();
		boolean extraColumnsProjectName = true;
		
		for (String project : dataset.keySet()) {
			String filename = path+project+".csv";
			if(project.equals(""))
				filename = path+"0.csv";
			WriteUtils.writeListOfChangesWithCircleToCSV(dataset.get(project), filename, extraColumnsProjectName, project);
		}

//		int recordsWritten = WriteUtils.writeMapWithCircleToCsv(dataset, outFile);
		
		System.out.println("Done in "+Duration.between(startWriting, Instant.now()));
//		System.out.println("Records written: "+recordsWritten);
		System.out.println("Total time "+Duration.between(start, Instant.now()));


	}

	public static String[] initCircleIds() {
		return new String[]{
				"0",
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
				"824769296181501",
		"1133031362168396"};
	}

	public static String[] initCircleNames() {
		return new String[] {
				"NO CIRCLE",
				"☺ Sales Roles",
				"☺ Infrastructure Roles",
				"☺ Alignment Roles",
				"☺ Organisations Roles",
				"☺ Marketplace Roles",
				"☺ Demand Roles",
				"☺ Providers Roles",
				"☺ Smooth Operations Roles",
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
				"☺ Customer Success Roles",
		"☺ Springest Academy Roles"};
	}
}
