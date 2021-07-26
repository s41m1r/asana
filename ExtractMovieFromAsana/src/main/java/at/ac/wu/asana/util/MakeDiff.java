package at.ac.wu.asana.util;

import java.util.ArrayList;
import java.util.List;

import at.ac.wu.asana.csv.ReadInfoFromCSV;
import at.ac.wu.asana.csv.WriteUtils;
import at.ac.wu.asana.model.StructuralDataChange;

public class MakeDiff {
	
	public static void main(String[] args) {
		String file1 = args[0];
		String file2 = args[1];
		
		List<String[]> data1 = ReadInfoFromCSV.readAll(file1);
		List<String[]> data2 = ReadInfoFromCSV.readAll(file2); 
		
		List<String[]> differences = makeDiff(data1, data2);
		String[] header = StructuralDataChange.csvHeaderMappe2();
		WriteUtils.writeList(differences, "differences_"+file1+"_vs_"+file2+".csv", header);
	}

	private static List<String[]> makeDiff(List<String[]> data1, List<String[]> data2) {
		List<String[]> res = new ArrayList<String[]>();
		
		if(data1.size() != data2.size())
			System.err.println("data1.size() = "+data1.size() + " != data2.size() ="+data2.size());
		
		for (int i = 0; i < data1.size(); i++) {
			String[] row = data1.get(i);
			boolean differentRow = false;
			for (int j = 0; j < row.length; j++) {
				String cellD1 = row[j];
				String cellD2 = data2.get(i)[j];
				if(!cellD1.equals(cellD2)) {
					row[j] = String.join("-->", row[j].toUpperCase(), data2.get(i)[j].toUpperCase());
					differentRow = true;
				}
			}
			if(differentRow)
				res.add(row);
		}
		
		return res;
	}
}
