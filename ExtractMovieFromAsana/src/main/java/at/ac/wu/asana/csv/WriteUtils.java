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
	
	public static void writeListOfChangesWithCircleToCSV(List<StructuralDataChange> taskChanges, String csv, boolean extraColumnProjectName, String circleName) {
		if(!extraColumnProjectName)
			writeListOfChangesWithCircleToCSV(taskChanges, csv);
		else {
			PrintWriter rolesFileWriter;
			try {
				rolesFileWriter = new PrintWriter(
						new OutputStreamWriter(
								new FileOutputStream(csv), StandardCharsets.UTF_8));

				CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
				
				csvWriter.writeNext(StructuralDataChange.csvHeaderCircle());
				for (StructuralDataChange change : taskChanges) {
					change.setAccordingToCircle(circleName);
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
	}

	public static void writeListOfChangesWithCircleToCSV(List<StructuralDataChange> taskChanges, String csv) {
		PrintWriter rolesFileWriter;
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(csv), StandardCharsets.UTF_8));

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = StructuralDataChange.csvHeaderCircleSecondDegree();
			csvWriter.writeNext(header);
			for (StructuralDataChange change : taskChanges) {
				csvWriter.writeNext(change.csvRowCircleSecondDegree());
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
					csvWriter.writeNext(change.csvRowCircle());
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

	public static void writeMapOfChangesWithCircleToCSV2(Map<String, List<StructuralDataChange>> taskChanges, String csv) {
		PrintWriter rolesFileWriter;
		//		PostProcessFromDB.printHistoryOfTask("7745109865138", taskChanges); 
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
				for (StructuralDataChange sdc : changes) {
					lines.add(sdc.csvRowCircle());
				}
			}
			csvWriter.writeAll(lines);
			csvWriter.flush();
			csvWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static int writeMapWithCircleToCsv(Map<String, List<StructuralDataChange>> changes, String csv) {
		int linesWritten = 0;
		PrintWriter rolesFileWriter;
		List<String[]> lines = new ArrayList<String[]>();
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(csv), StandardCharsets.UTF_8));

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = StructuralDataChange.csvHeaderCircle();
			lines.add(header);
			Set<String> circleIds = changes.keySet();
			for (String taskId : circleIds) {
				TreeSet<StructuralDataChange> events = new TreeSet<StructuralDataChange>(changes.get(taskId));
				for (StructuralDataChange sdc : events) {
					lines.add(sdc.csvRowCircle());
					linesWritten++;
				}
			}
			csvWriter.writeAll(lines);
			csvWriter.flush();
			csvWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return linesWritten;
	}
	
	public static int writeMapWithDynamic(Map<String, List<StructuralDataChange>> changes, String csv) {
		int linesWritten = 0;
		PrintWriter rolesFileWriter;
		List<String[]> lines = new ArrayList<String[]>();
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(csv), StandardCharsets.UTF_8));

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = StructuralDataChange.csvHeaderDynamic();
			lines.add(header);
			Set<String> circleIds = changes.keySet();
			for (String taskId : circleIds) {
				List<StructuralDataChange> events = changes.get(taskId);
				for (StructuralDataChange sdc : events) {
					lines.add(sdc.csvRowDynamic());
					linesWritten++;
				}
			}
			csvWriter.writeAll(lines);
			csvWriter.flush();
			csvWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return linesWritten;
	}
	
	
}
