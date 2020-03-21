package at.ac.wu.asana.db.postprocess.datastructures;

import java.time.LocalDateTime;
import java.util.List;

public class TimestampCircle implements Comparable<TimestampCircle> {
	public LocalDateTime timestamp;
	public List<String> circle;
	public List<String> circleId;

	public TimestampCircle() {
	}

	public TimestampCircle(LocalDateTime timestamp, List<String> circle, List<String> circleId) {
		super();
		this.timestamp = timestamp;
		this.circle = circle;
		this.circleId = circleId;
	}

	public TimestampCircle(LocalDateTime timestamp, List<String> circle) {
		super();
		this.timestamp = timestamp;
		this.circle = circle;
	}

	public int compareTo(TimestampCircle o) {
		return this.timestamp.compareTo(o.timestamp);
	}
}