package at.ac.wu.asana.db.postprocess.datastructures;

import java.util.ArrayList;
import java.util.List;

public class YMCircleList {
	public String ym;
	public List<String> circleIds;
	public List<String> circles;
	
	public YMCircleList(String ym, List<String> circleIds, List<String> circles, String[] authoritativeList, String[] authoritativeListNames) {
		super();
		this.ym = ym;
		this.circleIds = circleIds;
		this.circles = circles;
		
		for (int i = 0; i < circles.size(); i++) {
			if(circles.get(i).equals("NO CIRCLE"))
				circleIds.set(i, "0");			
		}
		
		for (int i = 0; i < this.circleIds.size(); i++) {
			if(circleIds.get(i).isEmpty()) {
				String circleName = circles.get(i);
				int pos = 0;
				boolean found = false;
				String id = "";
				for(;!found && pos<authoritativeListNames.length;pos++) {
					if(authoritativeListNames[pos].equals(circleName)) {
						found = true;
					}
				}
				if(found)
					circleIds.set(i, id);
			}
				
		}
	}

	public YMCircleList(String ym, ArrayList<String> circleIds) {
		this.ym = ym;
		this.circleIds = circleIds;
	}

	@Override
	public String toString() {
		return "YMCircleList [ym=" + ym + ", circleIds=" + circleIds + ", circles=" + circles + "]";
	}
	
	
}
