package at.ac.wu.asana.db.io;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import at.ac.wu.asana.db.utils.DatabaseConnector;
import at.ac.wu.asana.model.StructuralDataChange;
import at.ac.wu.asana.util.GeneralUtils;

public abstract class ReadFromDB {

	public static List<String> readAllYM(String dbname, String sql){

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


//	public static List<String> readDates(String string, String queryAllDates) {
//		// TODO Auto-generated method stub
//		List<String> res = new ArrayList<String>();
//		Query queryAllDates = session.createSQLQuery(string);
//		return null;
//	}

}
