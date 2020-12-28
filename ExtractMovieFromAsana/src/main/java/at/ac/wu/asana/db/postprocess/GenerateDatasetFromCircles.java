package at.ac.wu.asana.db.postprocess;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;

import at.ac.wu.asana.csv.WriteUtils;
import at.ac.wu.asana.db.io.ReadFromDB;
import at.ac.wu.asana.db.postprocess.datastructures.AuthoritativeList;
import at.ac.wu.asana.model.StructuralDataChange;

public class GenerateDatasetFromCircles {
	
	public static void main(String[] args) {
		
		String path = "/home/saimir/ownCloud/PhD/Collaborations/Waldemar/ISR/Data/Mess-by-Saimir-Do-Not-Enter/";
//		String outFile = "datasetFromCirlce.csv";
		
		Map<String, List<StructuralDataChange>> dataset = new TreeMap<String, List<StructuralDataChange>>();
		
		Instant start = Instant.now();
		
		List<StructuralDataChange> allChanges = ReadFromDB.readFromDBNoSort(
				"asana_manual9", 
				"SELECT * FROM SpringestRaw"
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
		String[] addZero = ArrayUtils.addAll(new String[] {"0"}, AuthoritativeList.authoritativeList);
		return addZero;
	}

	public static String[] initCircleNames() {
		String[] addZero = ArrayUtils.addAll(new String[] {"NO CIRCLE"}, AuthoritativeList.authoritativeListNames);
		return addZero;
	}
		
}
