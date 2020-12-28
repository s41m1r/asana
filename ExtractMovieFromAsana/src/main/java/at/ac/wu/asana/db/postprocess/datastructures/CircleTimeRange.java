package at.ac.wu.asana.db.postprocess.datastructures;

import java.time.LocalDateTime;

public class CircleTimeRange {
	private String circleId;
	private String circleName;
	private LocalDateTime start;
	private LocalDateTime end;
	
	public CircleTimeRange(String circleId, String circleName, LocalDateTime start, LocalDateTime end) {
		super();
		this.circleId = circleId;
		this.circleName = circleName;
		this.start = start;
		this.end = end;
	}

	public CircleTimeRange(String circleId, String circleName) {
		super();
		this.circleId = circleId;
		this.circleName = circleName;
	}

	public String getCircleId() {
		return circleId;
	}
	public void setCircleId(String circleId) {
		this.circleId = circleId;
	}
	public String getCircleName() {
		return circleName;
	}
	public void setCircleName(String circleName) {
		this.circleName = circleName;
	}
	public LocalDateTime getStart() {
		return start;
	}
	public void setStart(LocalDateTime start) {
		this.start = start;
	}
	public LocalDateTime getEnd() {
		return end;
	}
	public void setEnd(LocalDateTime end) {
		this.end = end;
	}
	@Override
	public String toString() {
		return "CircleTimeRange [circleId=" + circleId + ", circleName=" + circleName + ", start=" + start + ", end="
				+ end + "]";
	}
	
}
