package at.ac.wu.asana.csv;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.opencsv.CSVWriter;

import at.ac.wu.asana.model.StructuralDataChange;

public abstract class WriteUtils {

	public static void writeMapOfChangesToCSV(Map<String, List<StructuralDataChange>> taskChanges, String csv) {
		PrintWriter rolesFileWriter;
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(csv), StandardCharsets.UTF_8) );

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = StructuralDataChange.csvHeader();
			csvWriter.writeNext(header);
			for (String taskId : taskChanges.keySet()) {
				List<StructuralDataChange> changes = taskChanges.get(taskId);
				for (StructuralDataChange change : changes) {
					csvWriter.writeNext(change.csvRow());
				}
			}
			csvWriter.flush();
			csvWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeListOfChangesWithCircleToCSV(List<StructuralDataChange> taskChanges, String csv) {
		PrintWriter rolesFileWriter;
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(csv), StandardCharsets.UTF_8));

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = StructuralDataChange.csvHeaderCircle();
			csvWriter.writeNext(header);
			for (StructuralDataChange change : taskChanges) {
				csvWriter.writeNext(change.csvRowCircle());
			}
			csvWriter.flush();
			csvWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeMapOfChangesWithCircleToCSV(Map<String, List<StructuralDataChange>> taskChanges, String csv) {
		PrintWriter rolesFileWriter;
		//		PostProcessFromDB.printHistoryOfTask("7745109865138", taskChanges); 
		int count=0;
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(csv), StandardCharsets.UTF_8));

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = StructuralDataChange.csvHeaderCircle();
			csvWriter.writeNext(header);
			for (String taskId : taskChanges.keySet()) {
				List<StructuralDataChange> changes = taskChanges.get(taskId);
				for (StructuralDataChange change : changes) {
					if(change.getTaskId().equals("7745109865138"))
						count++;
					csvWriter.writeNext(change.csvRowCircle());
				}
			}
			csvWriter.flush();
			csvWriter.close();
			System.out.println("Wrote "+count+" about task 7745109865138");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeMapOfChangesWithCircleToCSV2(Map<String, List<StructuralDataChange>> taskChanges, String csv) {
		PrintWriter rolesFileWriter;
		//		PostProcessFromDB.printHistoryOfTask("7745109865138", taskChanges); 
		int count=0;
		List<String[]> lines = new ArrayList<String[]>();
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(csv), StandardCharsets.UTF_8));

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = StructuralDataChange.csvHeaderCircle();
			lines.add(header);
			Set<String> tasks = taskChanges.keySet();
			for (String taskId : tasks) {
				TreeSet<StructuralDataChange> changes = new TreeSet<StructuralDataChange>(taskChanges.get(taskId));
				if(taskId.equals("7745109865138"))
					System.out.println("taskId has history size "+changes.size());
				for (StructuralDataChange sdc : changes) {
					if(sdc.getTaskId().equals("7745109865138")) {
						count++;
						System.out.println("Writing (under taskId="+taskId+"): "+Arrays.asList(sdc.csvRowCircle()));
					}
					lines.add(sdc.csvRowCircle());
				}
			}
			csvWriter.writeAll(lines);
			csvWriter.flush();
			csvWriter.close();
			System.out.println("Wrote "+count+" about task 7745109865138");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
