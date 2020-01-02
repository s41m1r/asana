package at.ac.wu.asana.db.postprocess;

public class CirclePlusMinusTot implements Comparable<CirclePlusMinusTot> {
	
	String circleId;
	String circleName;
	String ym;
	int plus;
	int minus;
	int tot;
	
	
	public CirclePlusMinusTot() {
		super();
	}
	public CirclePlusMinusTot(String circleId, String circleName, String ym, int plus, int minus, int tot) {
		super();
		this.circleId = circleId;
		this.circleName = circleName;
		this.ym = ym;
		this.plus = plus;
		this.minus = minus;
		this.tot = tot;
	}
	public String getYm() {
		return ym;
	}
	public void setYm(String ym) {
		this.ym = ym;
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
	public int getPlus() {
		return plus;
	}
	public void setPlus(int plus) {
		this.plus = plus;
	}
	public int getMinus() {
		return minus;
	}
	public void setMinus(int minus) {
		this.minus = minus;
	}
	public int getTot() {
		return tot;
	}
	public void setTot(int tot) {
		this.tot = tot;
	}
	@Override
	public String toString() {
		return "CirclePlusMinusTot [circleId=" + circleId + ", circleName=" + circleName + ", ym=" + ym + ", plus="
				+ plus + ", minus=" + minus + ", tot=" + tot + "]";
	}
	public int compareTo(CirclePlusMinusTot o) {
		// TODO Auto-generated method stub
		return this.ym.compareTo(o.ym);
	}
	
	public static String[] csvHeader() {
		return new String[] {"circleId", "circleName", "ym", "plus", "minus", "tot"};
	}
	
	public String[] csvRow() {
		return new String[] {circleId, circleName, ym, ""+plus, ""+minus, ""+tot};
	}
	
}
