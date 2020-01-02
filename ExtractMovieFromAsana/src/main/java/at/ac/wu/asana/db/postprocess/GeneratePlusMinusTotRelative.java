package at.ac.wu.asana.db.postprocess;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.opencsv.CSVWriter;

public class GeneratePlusMinusTotRelative {
	
	public static void main(String[] args) {
		List<CirclePlusMinusTot> circlePlusMinusTots = CountByCircle.getMonthlyCountByCircle();
		
		Map<String, Integer> ymToTotal = getYMTotal(circlePlusMinusTots);
		
		writeStringIntMaptoCSV(ymToTotal, "totalsOfEachMonth.csv");
		
	}

	private static void writeStringIntMaptoCSV(Map<String, Integer> ymToTotal, String outFile) {
		PrintWriter rolesFileWriter;

		try {
			rolesFileWriter = new PrintWriter(
					new OutputStreamWriter(
							new FileOutputStream(outFile), StandardCharsets.UTF_8) );

			CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
			String[] header = new String[] {"ym", "tot"};
			csvWriter.writeNext(header);

			for (String	ym : ymToTotal.keySet()) {
				csvWriter.writeNext(new String[] {ym, ymToTotal.get(ym).toString()});
			}

			csvWriter.flush();
			csvWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	private static Map<String, Integer> getYMTotal(List<CirclePlusMinusTot> circlePlusMinusTots) {
		Map<String, Integer> ymToTotal = new TreeMap<String, Integer>();
		for (CirclePlusMinusTot circlePlusMinusTot : circlePlusMinusTots) {
			if(!ymToTotal.containsKey(circlePlusMinusTot.ym)) {
				ymToTotal.put(circlePlusMinusTot.ym, 0);
			}			
			ymToTotal.put(circlePlusMinusTot.ym, ymToTotal.get(circlePlusMinusTot.ym) + circlePlusMinusTot.tot);
		}
		return ymToTotal;
	}
}
