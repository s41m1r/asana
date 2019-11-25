package at.ac.wu.asana.csv;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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
	
	public static void writeMapOfChangesWithCircleToCSV(Map<String, List<StructuralDataChange>> taskChanges, String csv) {
		PrintWriter rolesFileWriter;
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(csv), StandardCharsets.UTF_8) );

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

}
