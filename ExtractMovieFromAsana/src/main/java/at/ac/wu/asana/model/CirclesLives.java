package at.ac.wu.asana.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import at.ac.wu.asana.util.DateRange;

public final class CirclesLives {
	
	private Map<String, DateRange> lifeOf = new HashMap<String, DateRange>();
	private boolean isInit = false;
		
	public void init() {
		this.isInit = true;
		
		DateRange range = new DateRange(LocalDate.parse("2013-09-16"), null);
		lifeOf.put("0", range); //NO CIRCLE
		
		range = new DateRange(LocalDate.parse("2013-10-01"), null);
		lifeOf.put("7963718816247", range); //Alignment
		
		range = new DateRange(LocalDate.parse("2014-04-14"), LocalDate.parse("2015-03-24"));
		lifeOf.put("11626921109046", range); //Business Intelligence
		
		range = new DateRange(LocalDate.parse("2014-04-01"), LocalDate.parse("2014-09-30"));
		lifeOf.put("11348115733601", range); //Demand
		
		range = new DateRange(LocalDate.parse("2016-08-04"), null);
		lifeOf.put("163654573139013", range); //Evangelism
		
		range = new DateRange(LocalDate.parse("2015-09-02"), null);
		lifeOf.put("47872397062455", range); //Finance
		
		range = new DateRange(LocalDate.parse("2013-10-01"), null);
		lifeOf.put("7963718816247", range); //Alignment
		
		range = new DateRange(LocalDate.parse("2017-08-09"), null);
		lifeOf.put("404651189519209", range); //Germany
		
		range = new DateRange(LocalDate.parse("2017-01-03"), LocalDate.parse("2017-09-18"));
		lifeOf.put("236886514207498", range); //Germany_Marketplace DE
		
		range = new DateRange(LocalDate.parse("2015-03-11"), LocalDate.parse("2016-01-12"));
		lifeOf.put("29007443412107", range); //Go Customers
		
		range = new DateRange(LocalDate.parse("2014-05-22"), LocalDate.parse("2014-05-29"));
		lifeOf.put("12530878841888", range); //Go Sales
		
		range = new DateRange(LocalDate.parse("2013-09-16"), LocalDate.parse("2018-06-28"));
		lifeOf.put("7749914219827", range); //Infrastructure
		
		range = new DateRange(LocalDate.parse("2016-01-12"), null);
		lifeOf.put("79667185218012", range); //Marketing
		
		range = new DateRange(LocalDate.parse("2014-04-01"), LocalDate.parse("2018-06-05"));
		lifeOf.put("11348115733592", range); //Marketplace (non-German)
		
		range = new DateRange(LocalDate.parse("2014-04-01"), null);
		lifeOf.put("11347525454570", range); //Organisations
		
		range = new DateRange(LocalDate.parse("2018-09-17"), null);
		lifeOf.put("824769296181501", range); //Organisations_Customer Success
		
		range = new DateRange(LocalDate.parse("2015-10-29"), null);
		lifeOf.put("61971534223290", range); //Product
		
		range = new DateRange(LocalDate.parse("2014-04-01"), LocalDate.parse("2016-01-11"));
		lifeOf.put("11350833325340", range); //Providers (old version)
		
		range = new DateRange(LocalDate.parse("2015-10-29"), null);
		lifeOf.put("389549960603898", range); //Product
		
		range = new DateRange(LocalDate.parse("2017-07-20"), null);
		lifeOf.put("389549960603898", range); //Providers
		
		range = new DateRange(LocalDate.parse("2014-06-17"), LocalDate.parse("2015-06-09"));
		lifeOf.put("13169100426325", range); //Rainmakers
		
		range = new DateRange(LocalDate.parse("2013-09-16"), LocalDate.parse("2014-09-01"));
		lifeOf.put("7746376637805", range); //Sales
		
		range = new DateRange(LocalDate.parse("2014-04-10"), null);
		lifeOf.put("11555199602299", range); //Smooth Operations
		
		range = new DateRange(LocalDate.parse("2018-02-15"), LocalDate.parse("2018-02-22"));
		lifeOf.put("561311958443380", range); //Smooth Operations_Office
		
		range = new DateRange(LocalDate.parse("2018-02-15"), LocalDate.parse("2018-02-22"));
		lifeOf.put("560994092069672", range); //Smooth Operations_People
		
		range = new DateRange(LocalDate.parse("2018-02-15"), LocalDate.parse("2018-02-22"));
		lifeOf.put("560994092069672", range); //Smooth Operations_People
		
		range = new DateRange(LocalDate.parse("2019-07-26"), null);
		lifeOf.put("1133031362168396", range); //Smooth Operations_Springest Academy
		
		range = new DateRange(LocalDate.parse("2017-07-19"), null);
		lifeOf.put("388515769387194", range); //Users
		
	}

	public Map<String, DateRange> getLifeOf() {
		return lifeOf;
	}
	
	public DateRange getLifeOf(String circleId) {
		return lifeOf.get(circleId);
	}
	
	public LocalDate getBirthOf(String circleId) {
		return lifeOf.get(circleId).getStartDate();
	}
	
	public LocalDate getDeathOf(String circleId) {
		return lifeOf.get(circleId).getEndDate();
	}

	public void setLifeOf(Map<String, DateRange> lifeOf) {
		this.lifeOf = lifeOf;
	}

	public boolean isInit() {
		return isInit;
	}
	
	public LocalDate firstBirthday() {
		LocalDate birthday = LocalDate.now();
		Set<Entry<String,DateRange>> entries = this.lifeOf.entrySet();
		for (Entry<String, DateRange> entry : entries) {
			LocalDate birthCircle = entry.getValue().startDate;
			if(birthCircle.isBefore(birthday))
				birthday=birthCircle;
		}
		return LocalDate.from(birthday);
	}
}
