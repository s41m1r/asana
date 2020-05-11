package at.ac.wu.asana.db.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import at.ac.wu.asana.db.utils.DatabaseConnector;
import at.ac.wu.asana.model.CirclesLives;
import at.ac.wu.asana.model.StructuralDataChange;
import at.ac.wu.asana.util.GeneralUtils;

public abstract class ReadFromDB {

	public static Map<String, List<StructuralDataChange>> getWeeklyChanges() {
		Map<String, List<StructuralDataChange>> wkChanges = new LinkedHashMap<String, List<StructuralDataChange>>();
	
		String dbname = "asana_manual5";
		String queryAllYM = "SELECT * FROM allYW";
	
		List<String> allYM = readAllTimePeriod(dbname, queryAllYM);
	
		String queryAllInYM = "SELECT * FROM `SpringestWithCircle` "
				+ "WHERE YEARWEEK(`timestamp`,'1') =:ym "
				//				+ "AND typeOfChange IN (12,4,5,14)"
				+ "";
		//		date =:date
	
		// read the data
		SessionFactory sf = DatabaseConnector.getSessionFactory(dbname);
		org.hibernate.Session session = sf.openSession();
		for (String wk : allYM) {
			List<StructuralDataChange> changes = readChangesByYM(session, dbname, queryAllInYM, wk);
			wkChanges.put(wk, changes);
		}
		session.flush();
		session.close();
		sf.close();
	
		return wkChanges;
	}
	
	public static Map<String, List<StructuralDataChange>> getWeeklyChanges(String dbname) {
		Map<String, List<StructuralDataChange>> wkChanges = new LinkedHashMap<String, List<StructuralDataChange>>();
	
		String queryAllYM = "SELECT * FROM `ayw` ORDER BY `yw`";
	
		List<String> allYM = readAllTimePeriod("asana_manual5", queryAllYM);
	
		String queryAllInYM = "SELECT * FROM `SpringestWithCircle` "
				+ "WHERE YEARWEEK(`timestamp`,'1') =:ym "
				//				+ "AND typeOfChange IN (12,4,5,14)"
				+ "";
		//		date =:date
	
		// read the data
		SessionFactory sf = DatabaseConnector.getSessionFactory(dbname);
		org.hibernate.Session session = sf.openSession();
		for (String wk : allYM) {
			List<StructuralDataChange> changes = readChangesByYM(session, dbname, queryAllInYM, wk);
			wkChanges.put(wk, changes);
		}
		session.flush();
		session.close();
		sf.close();
	
		return wkChanges;
	}

	public static List<String> readAllTimePeriod(String dbname, String sql){

		// read the data
		SessionFactory sf = DatabaseConnector.getSessionFactory(dbname);
		org.hibernate.Session session = sf.openSession();

		List<String> allEvents = new ArrayList<String>();

		Query queryEvents = session.createSQLQuery(sql);

		List<Object> results = queryEvents.list();

		for (Object e : results) {
			allEvents.add(e.toString());
		}

		session.flush();
		session.close();
		sf.close();

		return allEvents;
	}
	
	public static List<String> readAll(String dbname, String sql){

		// read the data
		SessionFactory sf = DatabaseConnector.getSessionFactory(dbname);
		org.hibernate.Session session = sf.openSession();

		List<String> allEvents = new ArrayList<String>();

		Query queryEvents = session.createSQLQuery(sql);

		List<Object> results = queryEvents.list();

		for (Object e : results) {
			allEvents.add(e.toString());
		}

		session.flush();
		session.close();
		sf.close();

		return allEvents;
	}

	public static List<StructuralDataChange> readFromDB(String dbname, String sql){

		// read the data
		SessionFactory sf = DatabaseConnector.getSessionFactory(dbname);
		org.hibernate.Session session = sf.openSession();

		List<StructuralDataChange> allEvents = new ArrayList<StructuralDataChange>();

		Query queryEvents = session.createSQLQuery((sql!=null)? sql: ""
				+ "SELECT * FROM `Springest`"
				+ "ORDER BY taskId, `timestamp`, `parentTaskId`");

		List<Object> results = queryEvents.list();
		List<StructuralDataChange> changeEvents = new ArrayList<StructuralDataChange>();

		for (Object e : results) {
			Object[] row = (Object[]) e;
			String[] str = GeneralUtils.toStrObjArray(row);
			StructuralDataChange sdc = StructuralDataChange.fromString(str);
			allEvents.add(sdc);
		}

		session.flush();
		session.close();
		sf.close();

		return allEvents;
	}

	public static List<StructuralDataChange> readFromDBNoSort(String dbname, String sql){

		// read the data
		SessionFactory sf = DatabaseConnector.getSessionFactory(dbname);
		org.hibernate.Session session = sf.openSession();

		List<StructuralDataChange> allEvents = new ArrayList<StructuralDataChange>();

		Query queryEvents = session.createSQLQuery((sql!=null)? sql: ""
				+ "SELECT * FROM `"+dbname+"`.SpringestRaw");

		List<Object> results = queryEvents.list();

		for (Object e : results) {
			Object[] row = (Object[]) e;
			String[] str = GeneralUtils.toStrObjArray(row);
			StructuralDataChange sdc = StructuralDataChange.fromString(str);
			allEvents.add(sdc);
		}

		session.flush();
		session.close();
		sf.close();

		return allEvents;
	}

	public static List<StructuralDataChange> readChangesByYM(Session session, String dbname, String q, String ym) {		
		List<StructuralDataChange> allEvents = new ArrayList<StructuralDataChange>();
		Query qu = session.createSQLQuery(q);

		qu.setString("ym", ym);

		List<Object> results = qu.list();
		List<StructuralDataChange> changeEvents = new ArrayList<StructuralDataChange>();

		for (Object e : results) {
			Object[] row = (Object[]) e;
			String[] str = GeneralUtils.toStrObjArray(row);
			StructuralDataChange sdc = StructuralDataChange.fromString(str);
			allEvents.add(sdc);
		}

		return allEvents;
	}
	
	public static List<StructuralDataChange> readDailyChanges(Session session, String dbname, String q, String day) {		
		List<StructuralDataChange> allEvents = new ArrayList<StructuralDataChange>();
		Query qu = session.createSQLQuery(q);

		qu.setString("day", day);

		List<Object> results = qu.list();
		List<StructuralDataChange> changeEvents = new ArrayList<StructuralDataChange>();

		for (Object e : results) {
			Object[] row = (Object[]) e;
			String[] str = GeneralUtils.toStrObjArray(row);
			StructuralDataChange sdc = StructuralDataChange.fromString(str);
			allEvents.add(sdc);
		}

		return allEvents;
	}
}
