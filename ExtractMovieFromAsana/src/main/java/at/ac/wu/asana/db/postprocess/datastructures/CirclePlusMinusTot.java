package at.ac.wu.asana.db.postprocess.datastructures;

/**
 * @author saimir
 *
 */
public class CirclePlusMinusTot implements Comparable<CirclePlusMinusTot> {
	
	public String circleId;
	public String circleName;
	public String ym;
	public int plus;
	public int minus;
	public int tot;
	public int mods;
	public int totPlusesThisCirclePrevMonth;
	public int totMinusesThisCirclesPrevMonth;
	public int totModsThisCirclePrevMonth;
	public int totThisCirclePreviousMonth;
	public int totAllCirclesPlusesPrevMonth;
	public int totAllCirclesMinusesPrevMonth;
	public int totAllCirclesModsPrevMonth;
	public int totAllCirclesPrevMonth;
	
	public CirclePlusMinusTot() {
		super();
		ym = "";
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
	
	public int getMods() {
		return mods;
	}
	public void setMods(int mods) {
		this.mods = mods;
	}
		
	public int getTotAllCirclesPreviousMonth() {
		return totThisCirclePreviousMonth;
	}
	public void setTotAllCirclesPreviousMonth(int totAllCirclesPreviousMonth) {
		this.totThisCirclePreviousMonth = totAllCirclesPreviousMonth;
	}
	public int getTotPlusesAllCirclesPrevMonth() {
		return totPlusesThisCirclePrevMonth;
	}
	public void setTotPlusesAllCirclesPrevMonth(int totPlusesAllCirclesPrevMonth) {
		this.totPlusesThisCirclePrevMonth = totPlusesAllCirclesPrevMonth;
	}
	public int getTotMinusesAllCirclesPrevMonth() {
		return totMinusesThisCirclesPrevMonth;
	}
	public void setTotMinusesAllCirclesPrevMonth(int totMinusesAllCirclesPrevMonth) {
		this.totMinusesThisCirclesPrevMonth = totMinusesAllCirclesPrevMonth;
	}
	
	public int getTotPlusesThisCirclePrevMonth() {
		return totPlusesThisCirclePrevMonth;
	}
	public void setTotPlusesThisCirclePrevMonth(int totPlusesThisCirclePrevMonth) {
		this.totPlusesThisCirclePrevMonth = totPlusesThisCirclePrevMonth;
	}
	public int getTotMinusesThisCirclesPrevMonth() {
		return totMinusesThisCirclesPrevMonth;
	}
	public void setTotMinusesThisCirclesPrevMonth(int totMinusesThisCirclesPrevMonth) {
		this.totMinusesThisCirclesPrevMonth = totMinusesThisCirclesPrevMonth;
	}
	public int getTotModsThisCirclePrevMonth() {
		return totModsThisCirclePrevMonth;
	}
	public void setTotModsThisCirclePrevMonth(int totModsThisCirclePrevMonth) {
		this.totModsThisCirclePrevMonth = totModsThisCirclePrevMonth;
	}
	public int getTotThisCirclesPreviousMonth() {
		return totThisCirclePreviousMonth;
	}
	public void setTotThisCirclesPreviousMonth(int totThisCirclesPreviousMonth) {
		this.totThisCirclePreviousMonth = totThisCirclesPreviousMonth;
	}
	public int getTotAllCirclesPrevMonth() {
		return totAllCirclesPrevMonth;
	}
	public void setTotAllCirclesPrevMonth(int totAllCirclesPrevMonth) {
		this.totAllCirclesPrevMonth = totAllCirclesPrevMonth;
	}
	public int getTotAllCirclesPlusesPrevMonth() {
		return totAllCirclesPlusesPrevMonth;
	}
	public void setTotAllCirclesPlusesPrevMonth(int totAllCirclesPlusesPrevMonth) {
		this.totAllCirclesPlusesPrevMonth = totAllCirclesPlusesPrevMonth;
	}
	public int getTotAllCirclesMinusesPrevMonth() {
		return totAllCirclesMinusesPrevMonth;
	}
	public void setTotAllCirclesMinusesPrevMonth(int totAllCirclesMinusesPrevMonth) {
		this.totAllCirclesMinusesPrevMonth = totAllCirclesMinusesPrevMonth;
	}
	public int getTotAllCirclesModsPrevMonth() {
		return totAllCirclesModsPrevMonth;
	}
	public void setTotAllCirclesModsPrevMonth(int totAllCirclesModsPrevMonth) {
		this.totAllCirclesModsPrevMonth = totAllCirclesModsPrevMonth;
	}
	public int compareTo(CirclePlusMinusTot o) {
		int res = this.ym.compareTo(o.ym);
		if(res==0)
			res = circleId.compareTo(o.circleId);
		return res;
	}
		
	@Override
	public String toString() {
		return "CirclePlusMinusTot [circleId=" + circleId + ", circleName=" + circleName + ", wk=" + ym + ", plus="
				+ plus + ", minus=" + minus + ", tot=" + tot + ", mods=" + mods + ", totPlusesThisCirclePrevMonth="
				+ totPlusesThisCirclePrevMonth + ", totMinusesThisCirclesPrevMonth=" + totMinusesThisCirclesPrevMonth
				+ ", totModsThisCirclePrevMonth=" + totModsThisCirclePrevMonth + ", totThisCirclesPreviousMonth="
				+ totThisCirclePreviousMonth + ", totAllCirclesPlusesPrevMonth=" + totAllCirclesPlusesPrevMonth
				+ ", totAllCirclesMinusesPrevMonth=" + totAllCirclesMinusesPrevMonth + ", totAllCirclesModsPrevMonth="
				+ totAllCirclesModsPrevMonth + ", totAllCirclesPrevMonth=" + totAllCirclesPrevMonth + "]";
	}
	
	public static String[] csvHeader() {
		return new String[] {"circleId", "circleName", "ym", "plus", "minus", "tot", "modifications",
				"totPlusesThisCirclePrevMonth", "totMinusesThisCirclesPrevMonth", "totModsThisCirclePrevMonth",
				"totThisCirclesPreviousMonth",
				"totAllCirclesPlusesPrevMonth", "totAllCirclesMinusesPrevMonth", "totAllCirclesModsPrevMonth",
				"totAllCirclesPrevMonth"};
	}
	
	public String[] csvRow() {
		return new String[] {circleId, circleName, ym, ""+plus, ""+minus, ""+tot, ""+mods
				, ""+totPlusesThisCirclePrevMonth, ""+totMinusesThisCirclesPrevMonth, 
				""+totModsThisCirclePrevMonth, ""+totThisCirclePreviousMonth,
				""+totAllCirclesPlusesPrevMonth, ""+totAllCirclesMinusesPrevMonth, ""+totAllCirclesModsPrevMonth,
				""+totAllCirclesPrevMonth};
	}

}
