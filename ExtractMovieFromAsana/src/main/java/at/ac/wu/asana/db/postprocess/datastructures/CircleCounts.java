package at.ac.wu.asana.db.postprocess.datastructures;

public class CircleCounts implements Comparable<CircleCounts>{
	public String circleId;
	public String circleName;
	public int births;
	public int deaths;
	public int modifications;
	public int migrations;
	public int totEvents;
	public int totDerivedEvents;
	public int totOtherEvents;
	public int totRolesInCircle;
	public long totalRolesInCircleUntilThisMonth;
	
	
	
	public CircleCounts() {
		super();
	}

	public CircleCounts(String circleId, String circleName, int births, int deaths, int modifications, int migrations,
			int totEvents, int totDerivedEvents, int totOtherEvents, int totRolesInCircle) {
		super();
		this.circleId = circleId;
		this.circleName = circleName;
		this.births = births;
		this.deaths = deaths;
		this.modifications = modifications;
		this.migrations = migrations;
		this.totEvents = totEvents;
		this.totDerivedEvents = totDerivedEvents;
		this.totOtherEvents = totOtherEvents;
		this.totRolesInCircle = totRolesInCircle;
	}

	public CircleCounts(String circleId, String circleName, int births, int deaths, int modifications, int migrations,
			int totEvents, int totDerivedEvents, int totOtherEvents) {
		super();
		this.circleId = circleId;
		this.circleName = circleName;
		this.births = births;
		this.deaths = deaths;
		this.modifications = modifications;
		this.migrations = migrations;
		this.totEvents = totEvents;
		this.totDerivedEvents = totDerivedEvents;
		this.totOtherEvents = totOtherEvents;
	}

	public CircleCounts(int births, int deaths, int modifications, int migrations) {
		super();
		this.births = births;
		this.deaths = deaths;
		this.modifications = modifications;
		this.migrations = migrations;
	}
	
	public CircleCounts(String circleId, String circleName, int births, int deaths, int modifications, int migrations,
			int totEvents, int totDerivedEvents) {
		super();
		this.circleId = circleId;
		this.circleName = circleName;
		this.births = births;
		this.deaths = deaths;
		this.modifications = modifications;
		this.migrations = migrations;
		this.totEvents = totEvents;
		this.totDerivedEvents = totDerivedEvents;
	}

	
	public long getTotalRolesInCircleUntilThisMonth() {
		return totalRolesInCircleUntilThisMonth;
	}

	public void setTotalRolesInCircleUntilThisMonth(long totalRolesInCircleUntilThisMonth) {
		this.totalRolesInCircleUntilThisMonth = totalRolesInCircleUntilThisMonth;
	}

	public int getTotOtherEvents() {
		return totOtherEvents;
	}

	public void setTotOtherEvents(int totOtherEvents) {
		this.totOtherEvents = totOtherEvents;
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
	public int getMigrations() {
		return migrations;
	}
	public void setMigrations(int migrations) {
		this.migrations = migrations;
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

	public int getTotEvents() {
		return totEvents;
	}

	public void setTotEvents(int totEvents) {
		this.totEvents = totEvents;
	}

	public int getTotDerivedEvents() {
		return totDerivedEvents;
	}

	public void setTotDerivedEvents(int totDerivedEvents) {
		this.totDerivedEvents = totDerivedEvents;
	}

	public int getTotRolesInCircle() {
		return totRolesInCircle;
	}

	public void setTotRolesInCircle(int totRolesInCircle) {
		this.totRolesInCircle = totRolesInCircle;
	}

	@Override
	public String toString() {
		return "CircleCounts [circleId=" + circleId + ", circleName=" + circleName + ", births=" + births + ", deaths="
				+ deaths + ", modifications=" + modifications + ", migrations=" + migrations + ", totEvents="
				+ totEvents + ", totDerivedEvents=" + totDerivedEvents + ", totOtherEvents=" + totOtherEvents
				+ ", totRolesInCircle=" + totRolesInCircle + ", totalRolesInCircleUntilThisMonth="
				+ totalRolesInCircleUntilThisMonth + "]";
	}

	public String[] toCSVRow(String k) {
		return new String[] {k, circleId , circleName , ""+births ,""+deaths , ""+modifications
				,""+migrations ,""+totEvents ,""+totDerivedEvents
				,""+totOtherEvents,""+totRolesInCircle, ""+totalRolesInCircleUntilThisMonth};
	}

	public int compareTo(CircleCounts o) {
		if(this == o)
			return 0;
		if(o!=null)
			return this.circleId.compareTo(o.circleId);
		return 0;
	}
}
