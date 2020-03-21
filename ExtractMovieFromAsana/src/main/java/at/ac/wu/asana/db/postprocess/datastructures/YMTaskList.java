package at.ac.wu.asana.db.postprocess.datastructures;

import java.util.List;

public class YMTaskList implements Comparable<YMTaskList> {
	public String ym;
	public List<String> taskIds;
	public YMTaskList(String ym, List<String> taskIds) {
		super();
		this.ym = ym;
		this.taskIds = taskIds;
	}
	@Override
	public String toString() {
		return "YMTaskList [ym=" + ym + ", taskIds=" + taskIds + "]";
	}
	
	public int compareTo(YMTaskList o) {
		return this.ym.compareTo(o.ym);
	}	
}
