package at.ac.wu.asana.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVWriter;

import at.ac.wu.asana.db.postprocess.datastructures.TimePeriodOveralls;

public abstract class PrintoutUtils {
	
	public static void writeOverallsToCSV(List<TimePeriodOveralls> ymOveralls, String outFile) {
		PrintWriter rolesFileWriter;
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(outFile), StandardCharsets.UTF_8));

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = TimePeriodOveralls.csvHeader("yearWeek");
			csvWriter.writeNext(header);
			for (TimePeriodOveralls change : ymOveralls) {
				csvWriter.writeNext(change.toCSVRow(change.timePeriod));
			}
			csvWriter.flush();
			csvWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	public static void writeOverallsToCSV(List<TimePeriodOveralls> ymOveralls, String outFile, String timeperiod) {
		PrintWriter rolesFileWriter;
		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(outFile), StandardCharsets.UTF_8));

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = (timeperiod!=null)? TimePeriodOveralls.csvHeader(timeperiod) : TimePeriodOveralls.csvHeader();
			csvWriter.writeNext(header);
			for (TimePeriodOveralls change : ymOveralls) {
				csvWriter.writeNext(change.toCSVRow(change.timePeriod));
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
