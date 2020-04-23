package at.ac.wu.asana.db.postprocess.datastructures;

public class CircleTot {
	public String circleId;
	public Integer tot;
	@Override
	public String toString() {
		return "CircleTot [circleId=" + circleId + ", tot=" + tot + "]";
	}
	public CircleTot(String circleId, Integer tot) {
		super();
		this.circleId = circleId;
		this.tot = tot;
	}
	
}
