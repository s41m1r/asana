package at.ac.wu.asana.tryout;

import java.util.List;
import java.util.Map;

import at.ac.wu.asana.csv.ReadCircleParentLocations;
import at.ac.wu.asana.db.postprocess.datastructures.CircleTimeRange;

public class TestCircleTimeRange {

	public static void main(String[] args) {
		String fileName = "/home/saimir/ownCloud/PhD/Collaborations/Waldemar/Springest/Data/Data Extracted from DB/circleDependencies/"
				+ "circleParents-3.csv";
		//<Circle id, circle history (c, t_start, t_end)>
		Map<String, List<CircleTimeRange>> map = ReadCircleParentLocations.readFromFile(fileName); 
		
		for (String cid : map.keySet()) {
			System.out.println(cid+" - "+map.get(cid));
		}
	}
}
