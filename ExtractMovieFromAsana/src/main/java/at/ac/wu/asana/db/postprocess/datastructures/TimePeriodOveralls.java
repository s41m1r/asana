package at.ac.wu.asana.db.postprocess.datastructures;

public class TimePeriodOveralls implements Comparable<TimePeriodOveralls> {

	public String timePeriod;
	public int births;
	public int deaths;
	public int modifications;

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


	@Override
	public String toString() {
		return "YmOveralls [ym=" + timePeriod + ", births=" + births + ", deaths=" + deaths + ", modifications=" + modifications
				+ "]";
	}
	
	public String[] toCSVRow(String timePeriod) {
		return new String[] {timePeriod, ""+births,""+deaths , ""+modifications};
	}
	
	public static String[] csvHeader() {
		return new String[] {"ym", "births","deaths" , "modifications"};
	}
	
	public static String[] csvHeader(String timePeriod) {
		return new String[] {timePeriod, "births","deaths" , "modifications"};
	}

	public int compareTo(TimePeriodOveralls o) {
		return this.getTimePeriod().compareTo(o.getTimePeriod());
	}

}
