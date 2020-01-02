package at.ac.wu.asana.db.postprocess;

public class CircleTotsWithOthers extends CirclePlusMinusTot {
	Integer totMonth;
	Integer totMonthMinusThis;

	public static String[] csvHeader() {
		return new String[] {"circleId", "circleName", "ym", "plus", "minus", "tot", "totMonth", "totMonthMinusThis" };
	}
	
	public String[] csvRow() {
		return new String[] {circleId, circleName, ym, ""+plus, ""+minus, ""+tot, totMonth+"", totMonthMinusThis+""};
	}

	@Override
	public String toString() {
		return "CircleTotsWithOthers [totMonth=" + totMonth + ", totMonthMinusThis=" + totMonthMinusThis + ", circleId="
				+ circleId + ", circleName=" + circleName + ", ym=" + ym + ", plus=" + plus + ", minus=" + minus
				+ ", tot=" + tot + "]";
	}
	
}
