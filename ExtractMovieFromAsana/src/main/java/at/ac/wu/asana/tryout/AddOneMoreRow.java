package at.ac.wu.asana.tryout;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.api.client.util.DateTime;
import com.google.common.collect.Sets;
import com.opencsv.CSVWriter;

import at.ac.wu.asana.db.io.ReadFromDB;
import at.ac.wu.asana.model.AsanaActions;
import at.ac.wu.asana.model.StructuralDataChange;
import at.ac.wu.asana.util.GeneralUtils;

public class AddOneMoreRow {

	static class TimestampCircle implements Comparable<TimestampCircle> {
		DateTime timestamp;
		List<String> circle;
		List<String> circleId;

		public TimestampCircle() {
		}

		public TimestampCircle(DateTime timestamp, List<String> circle, List<String> circleId) {
			super();
			this.timestamp = timestamp;
			this.circle = circle;
			this.circleId = circleId;
		}

		public TimestampCircle(DateTime timestamp, List<String> circle) {
			super();
			this.timestamp = timestamp;
			this.circle = circle;
		}

		public int compareTo(TimestampCircle o) {
			if(this.timestamp.getValue() < o.timestamp.getValue())
				return -1;
			else 
				if(this.timestamp.getValue() > o.timestamp.getValue())
					return 1;
				else return 0;
		}
	}

	static String[] authoritativeList = new String[]{
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
	"824769296181501"};

	static String[] authoritativeListNames = new String[] {
			"☺ Sales Roles",
			"☺ Infrastructure Roles",
			"☺ Alignment Roles",
			"☺ Organisations Roles",
			"☺ Marketplace Roles",
			"☺ Demand Roles",
			"☺ Providers Roles",
			"☺ Smooth Ops Roles",
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
	"☺ Customer Success Roles"};

	static Map<String, String> subCirclesOf = new HashMap<String, String>();
	static Map<String, String> mapTaskCurrentCircle = new HashMap<String, String>();
	static Map<String, TreeSet<TimestampCircle>> circlesOfTasks = new HashMap<String, TreeSet<AddOneMoreRow.TimestampCircle>>();

	public static void main(String[] args) throws IOException {

		//		addTwoMoreRows();
		long start = System.currentTimeMillis();
		subCirclesOf.put("404651189519209", "236886514207498");
		subCirclesOf.put("11555199602299", "560994092069672");
		subCirclesOf.put("11555199602299", "561311958443380");
		subCirclesOf.put("11347525454570", "824769296181501");
		fixCircles();
		//		fixSubcircles()
		System.out.println("Done in "+(System.currentTimeMillis()-start)/1000+" sec.");
	}

	private static void fixCircles() throws IOException {
		String sql = "SELECT * FROM `Springest` ORDER BY taskId, `timestamp`;";

		List<StructuralDataChange> events = ReadFromDB.readFromDB("asana_manual4", sql);
		System.out.println("Read "+events.size()+" events.");		
		String csv = "outCircles.csv";

		PrintWriter rolesFileWriter = new PrintWriter(
				new OutputStreamWriter(
						new FileOutputStream(csv), StandardCharsets.UTF_8) );

		CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
		String[] header = StructuralDataChange.csvHeaderCircle();

		csvWriter.writeNext(header);
		List<String> circles = new ArrayList<String>();
		String lastTaskId = null;
		int lastTypeOfChange = -1;
		boolean migration = false;
		TreeSet<TimestampCircle> treeSet = new TreeSet<AddOneMoreRow.TimestampCircle>();
		List<String> unionCircles = new ArrayList<String>();
		boolean becameIndependent = false;
		int timesAddedToCircle = 0;

		for (StructuralDataChange sdc : events) {
			String currTaskId = sdc.getTaskId();
			if(!currTaskId.equals(lastTaskId)) {
				lastTaskId = currTaskId;
				circles = new ArrayList<String>();
				lastTypeOfChange = -1;
				migration = false;
				becameIndependent = false;
				timesAddedToCircle = 0;
			}
			
			if(sdc.getParentTaskId()!=null && !sdc.getParentTask().isEmpty()) { // this is a subtask and must inherit all fathers circles
				List<String> circlesFather = new ArrayList<String>();
				String parentId = sdc.getParentTaskId();
				TreeSet<TimestampCircle> fathersHistory = circlesOfTasks.get(parentId);
				circlesFather = getCirclesAtTime(fathersHistory, sdc.getStoryCreatedAt());
				if(!becameIndependent)
					circles = GeneralUtils.union(circles, circlesFather);
			}
			
			if(sdc.getTypeOfChange()==12) { 
				sdc.setTypeOfChange(AsanaActions.DESIGN_ROLE);
				sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.DESIGN_ROLE));
			}
			
			if(sdc.getTypeOfChange()==AsanaActions.ADD_TO_CIRCLE) {
				String curCircle = sdc.getRawDataText().replaceAll("added to ", "").trim();
				int i = lookup(curCircle); // if -1 then it is not a circle
				if(i!=-1) {
					if(timesAddedToCircle==0) {
						sdc.setTypeOfChange(AsanaActions.CREATE_ROLE);
						sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.CREATE_ROLE));
					}
					timesAddedToCircle++;
					if(!circles.contains(curCircle)) {
						circles.add(curCircle);
						TreeSet<TimestampCircle> ts = new TreeSet<AddOneMoreRow.TimestampCircle>();
						List<String> copyOfCircles = new ArrayList<String>();
						copyOfCircles.addAll(circles);		
						ts.add(new TimestampCircle(sdc.getStoryCreatedAt(), copyOfCircles));
//						circlesOfTasks.put(currTaskId, ts);
						if(circlesOfTasks.get(currTaskId)==null)
							circlesOfTasks.put(currTaskId, ts);
						else
							circlesOfTasks.get(currTaskId).addAll(ts);

						if(lastTypeOfChange==AsanaActions.REMOVE_FROM_CIRCLE)
							migration=true; // if the task is the same and last action was a remove from circle, then we have a migration

						lastTypeOfChange=AsanaActions.ADD_TO_CIRCLE;
					}
				}
				else {
					sdc.setTypeOfChange(AsanaActions.IGNORE_OR_DELETE);
					sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.IGNORE_OR_DELETE));
				}
			}
			if(sdc.getTypeOfChange()==AsanaActions.REMOVE_FROM_CIRCLE) {
				String curCircle = sdc.getRawDataText().replaceAll("removed from ", "").trim();
				int i = lookup(curCircle); // if -1 then it is not a circle
				if(i!=-1) {
					if(circles.contains(curCircle)) {
						circles.remove(curCircle);
						lastTypeOfChange=AsanaActions.REMOVE_FROM_CIRCLE;
					}
					
					TreeSet<TimestampCircle> fathersHistory = circlesOfTasks.get(sdc.getParentTaskId());
					List<String> circlesFather = getCirclesAtTime(fathersHistory, sdc.getStoryCreatedAt());
					if(GeneralUtils.intersection(circles, circlesFather).isEmpty())
						becameIndependent = true;
				}
				else {
					sdc.setTypeOfChange(AsanaActions.IGNORE_OR_DELETE);
					sdc.setTypeOfChangeDescription(AsanaActions.codeToString(AsanaActions.IGNORE_OR_DELETE));
				}
			}
			sdc.setCircle(commaSeparate(circles));
			sdc.setCircleIds(commaSeparateIds(circles));
			csvWriter.writeNext(sdc.csvRowCircle());
		}
		csvWriter.flush();
		csvWriter.close();
	}

	private static List<String> getCirclesAtTime(TreeSet<TimestampCircle> fathersHistory, DateTime currentTime) {
		List<String> circlesAtTime = new ArrayList<String>();
		if(fathersHistory==null)
			return circlesAtTime;
		TimestampCircle fathersLastEvent = null;
		long diff = Long.MAX_VALUE;
		long lastDiff = diff;
		for (TimestampCircle timestampCircle : fathersHistory) {
			diff=currentTime.getValue()-timestampCircle.timestamp.getValue();
			if(diff>0) {
				fathersLastEvent = timestampCircle;
				lastDiff=diff;
			}
			else
				break;
		}
		
		if(fathersLastEvent!=null)
			circlesAtTime.addAll(fathersLastEvent.circle);
		
		return circlesAtTime;
	}

	private static String commaSeparateIds(List<String> circles) {
		String circleIds = "";
		boolean hit = false;
		for (String c : circles) {
			int idx = lookup(c);
			circleIds+=authoritativeList[idx]+",";
			hit=true;
		}
		if(hit)
			circleIds = ""+circleIds.substring(0, circleIds.length()-1);

		return circleIds;
	}

	private static String commaSeparate(List<String> circles) {
		if(circles.isEmpty())
			return "NO CIRCLE";
		String res = "";
		for (int i = 0; i < circles.size()-1; i++) {
			res += circles.get(i) + ",";
		}
		res+=circles.get(circles.size()-1);
		return res;
	}

	private static void addTwoMoreRows() throws FileNotFoundException, IOException {
		Set<String> authListNames = Sets.newHashSet(authoritativeListNames);

		List<StructuralDataChange> events = ReadFromDB.readFromDB("asana_manual4", null);
		System.out.println("Read "+events.size()+" events.");		
		String csv = "out.csv";

		PrintWriter rolesFileWriter = new PrintWriter(
				new OutputStreamWriter(
						new FileOutputStream(csv), StandardCharsets.UTF_8) );

		CSVWriter csvWriter = new CSVWriter(rolesFileWriter);
		String[] header = StructuralDataChange.csvHeaderCircle();

		csvWriter.writeNext(header);

		for (StructuralDataChange sdc : events) {

			String lastCircle = null;
			String currentCircleName = null;
			String currentCircleId = sdc.getProjectId();
			sdc.setCircle(sdc.getProjectName());
			sdc.setMigration(false);


			if(sdc.getTaskId().equals("7817799957722"))
				System.out.println("qui");

			if(mapTaskCurrentCircle.containsKey(sdc.getTaskId())) { // already seen
				lastCircle = mapTaskCurrentCircle.get(sdc.getTaskId());

				if(sdc.getTypeOfChange()==AsanaActions.ADD_TO_CIRCLE) {
					currentCircleName = sdc.getRawDataText().replaceAll("added to ", "").trim();
					//					if(authListNames.contains(currentCircleName)) {
					//						System.out.println(sdc.getTaskName()+"," + sdc.getRawDataText() +
					//								" is contained in "+ authListNames);
					//					}
					int i = lookup(currentCircleName); // if -1 then it is not a circle

					if(i!=-1)
						currentCircleId = authoritativeList[i];

					if(!lastCircle.equals(currentCircleId) && i!=-1) {
						sdc.setMigration(true);
						sdc.setCircle(currentCircleName);
						mapTaskCurrentCircle.put(sdc.getTaskId(), currentCircleId);
					}
					else { // still in the same circle
						sdc.setMigration(false);
						sdc.setCircle(sdc.getProjectName());
					}
				}
			}

			mapTaskCurrentCircle.put(sdc.getTaskId(), currentCircleId);			
			csvWriter.writeNext(sdc.csvRowCircle());
		}

		csvWriter.flush();
		csvWriter.close();
	}

	private static int lookup(String currentCircleName) {
		boolean found = false;
		int i = 0;
		for (; i < authoritativeListNames.length; i++) {
			if(currentCircleName.equals(authoritativeListNames[i])) {
				found = true;
				break;
			}
		}
		if(found)
			return i;

		return -1;
	}
}
