package at.ac.wu.asana.db.postprocess;

import java.util.List;

import com.google.api.client.util.DateTime;

public class TimestampCircle implements Comparable<TimestampCircle> {
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