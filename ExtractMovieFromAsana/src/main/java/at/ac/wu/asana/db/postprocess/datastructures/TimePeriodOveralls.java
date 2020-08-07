package at.ac.wu.asana.db.postprocess.datastructures;

public class TimePeriodOveralls implements Comparable<TimePeriodOveralls> {

	public String timePeriod;
	public int births;
	public int deaths;
	public int modifications;
	public int delta;
	public int tot;

	public String getTimePeriod() {
		return timePeriod;
	}

	public void setTimePeriod(String timePeriod) {
		this.timePeriod = timePeriod;
	}

	public int getBirths() {
		return births;
	}
	public void setBirths(int births) {
		this.births = births;
	}

	public int getDeaths() {
		return deaths;
	}

	public void setDeaths(int deaths) {
		this.deaths = deaths;
	}

	public int getModifications() {
		return modifications;
	}

	public void setModifications(int modifications) {
		this.modifications = modifications;
	}

	public int getDelta() {
		return delta;
	}

	public void setDelta(int delta) {
		this.delta = delta;
	}

	public int getTot() {
		return tot;
	}

	public void setTot(int tot) {
		this.tot = tot;
	}

	@Override
	public String toString() {
		return "YmOveralls [ym=" + timePeriod + ", births=" + births + ", deaths=" + deaths + ", modifications=" + modifications
				+ "]";
	}
	
	public String[] toCSVRow(String timePeriod) {
		return new String[] {timePeriod, ""+births,""+deaths , ""+modifications, ""+delta, ""+tot};
	}
	
	public static String[] csvHeader() {
		return new String[] {"ym", "births","deaths" , "modifications", "delta", "tot"};
	}
	
	public static String[] csvHeader(String timePeriod) {
		return new String[] {timePeriod, "births","deaths" , "modifications", "delta", "tot"};
	}

	public int compareTo(TimePeriodOveralls o) {
		return this.getTimePeriod().compareTo(o.getTimePeriod());
	}

}
