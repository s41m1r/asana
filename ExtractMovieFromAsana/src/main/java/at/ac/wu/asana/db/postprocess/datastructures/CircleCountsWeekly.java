package at.ac.wu.asana.db.postprocess.datastructures;

public class CircleCountsWeekly extends CirclePlusMinusTot {
	
	private long age;
	
	public CircleCountsWeekly() {
		super();
		setAge(0);
	}
	
	@Override
	public String toString() {
		return "CirclePlusMinusTot [circleId=" + circleId + ", circleName=" + circleName + ", ym=" + ym + ", births="
				+ plus + ", deaths=" + minus + ", tot=" + tot + ", mods=" + mods + ", totPlusesThisCirclePrevMonth="
				+ totPlusesThisCirclePrevMonth + ", totMinusesThisCirclesPrevMonth=" + totMinusesThisCirclesPrevMonth
				+ ", totModsThisCirclePrevMonth=" + totModsThisCirclePrevMonth + ", totThisCirclesPreviousMonth="
				+ totThisCirclePreviousMonth + ", totAllCirclesPlusesPrevMonth=" + totAllCirclesPlusesPrevMonth
				+ ", totAllCirclesMinusesPrevMonth=" + totAllCirclesMinusesPrevMonth + ", totAllCirclesModsPrevMonth="
				+ totAllCirclesModsPrevMonth + ", totAllCirclesPrevMonth=" + totAllCirclesPrevMonth + "]";
	}
	
	public static String[] csvHeader() {
		return new String[] {"circleId", "circleName", "ym", "births", "deaths", "tot", "modifications",
				"totBirthsThisCirclePrevWk", "totDeathsThisCirclesPrevWk", "totModsThisCirclePrevWk",
				"totAllCirclesThisWk",
				"totAllCirclesBirthsPrevWk", "totAllCirclesDeathsPrevWk", "totAllCirclesModsPrevWk",
				"totAllCirclesPrevWk","age"};
	}
	
	public String[] csvRow() {
		return new String[] {circleId, circleName, ym, ""+plus, ""+minus, ""+tot, ""+mods
				, ""+totPlusesThisCirclePrevMonth, ""+totMinusesThisCirclesPrevMonth, 
				""+totModsThisCirclePrevMonth, ""+totThisCirclePreviousMonth,
				""+totAllCirclesPlusesPrevMonth, ""+totAllCirclesMinusesPrevMonth, ""+totAllCirclesModsPrevMonth,
				""+totAllCirclesPrevMonth,""+age};
	}
	
	public int getBirths() {
		return super.getPlus();
	}
	
	public void setBirths(int b) {
		super.setPlus(b);
	}
	
	public int getDeaths() {
		return super.getMinus();
	}
	
	public void setDeaths(int d) {
		super.setMinus(d);
	}

	public int getModifications() {
		return super.getMods();
	}

	public void setModifications(int i) {
		super.setMods(i);
	}
	
	public void setWeek(String wk) {
		super.setYm(wk+"");
	}

	public void setTotAllCirclesPreviousWeek(int currentTot) {
		super.setTotAllCirclesPreviousMonth(currentTot);
	}

	public long getAge() {
		return age;
	}

	public void setAge(long l) {
		this.age = l;
	}
}
